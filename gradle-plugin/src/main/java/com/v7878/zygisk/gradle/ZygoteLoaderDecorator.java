package com.v7878.zygisk.gradle;

import com.android.build.api.variant.ApplicationVariant;
import com.android.build.api.variant.VariantOutput;
import com.v7878.zygisk.gradle.tasks.ChecksumTask;
import com.v7878.zygisk.gradle.tasks.CustomizeTask;
import com.v7878.zygisk.gradle.tasks.PackagesTask;
import com.v7878.zygisk.gradle.tasks.PropertiesTask;
import com.v7878.zygisk.gradle.util.StringUtils;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.tasks.Sync;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.bundling.Zip;
import org.gradle.api.tasks.bundling.ZipEntryCompression;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public final class ZygoteLoaderDecorator {
    private static final String ID_PATTERN = "^[a-zA-Z][a-zA-Z0-9._-]+$";

    private final Project project;

    public ZygoteLoaderDecorator(Project project) {
        this.project = project;
    }

    private static void putProperty(Map<String, String> props, String name, boolean required, String value) {
        if (required && value == null) throw new GradleException("module " + name + " not set");
        if (value != null) props.put(name, value);
    }

    public void decorateVariant(ApplicationVariant variant) {
        var extension = variant.getExtension(ZygoteLoaderExtension.class);
        Objects.requireNonNull(extension, "ZygoteLoaderExtension is null");

        String variantName = variant.getName();
        String variantNameCap = StringUtils.capitalize(variantName);

        VariantOutput variantOutput = variant.getOutputs().stream().findAny().orElseThrow();

        Map<String, String> moduleProps = new LinkedHashMap<>();

        moduleProps.put("version", variantOutput.getVersionName().getOrElse(""));
        moduleProps.put("versionCode", variantOutput.getVersionCode().getOrElse(0).toString());
        moduleProps.put("minSdkVersion", String.valueOf(variant.getMinSdk().getApiLevel()));
        if (variant.getMaxSdk() != null) {
            moduleProps.put("maxSdkVersion", String.valueOf(variant.getMaxSdk()));
        }

        putProperty(moduleProps, "id", true, extension.getId());
        if (!extension.getId().matches(ID_PATTERN)) {
            throw new GradleException(String.format(
                    "module id \"%s\" does not match %s", extension.getId(), ID_PATTERN));
        }
        putProperty(moduleProps, "name", true, extension.getName());
        putProperty(moduleProps, "author", true, extension.getAuthor());
        putProperty(moduleProps, "description", true, extension.getDescription());
        putProperty(moduleProps, "entrypoint", true, extension.getEntrypoint());
        putProperty(moduleProps, "updateJson", false, extension.getUpdateJson());

        moduleProps.putAll(extension.getAdditionalProperties());

        var buildDir = project.getLayout().getBuildDirectory();
        var projectDir = project.getLayout().getProjectDirectory();

        var generateModuleProp = project.getTasks().register(
                "generateModuleProp" + variantNameCap,
                PropertiesTask.class, task -> {
                    task.getDestinationFile().set(
                            buildDir.dir("generated/properties/" + variantName)
                                    .map(p -> p.file("module.prop"))
                    );
                    task.getProperties().set(moduleProps);
                }
        );

        var generateInitialPackages = project.getTasks().register(
                "generateInitialPackages" + variantNameCap,
                PackagesTask.class, task -> {
                    task.getDestinationDirectory().set(
                            buildDir.dir("generated/packages/" + variantName)
                    );
                    task.getPackages().set(extension.getPackages());
                }
        );

        var apk = project.getTasks().named("package" + variantNameCap).map(p ->
                p.getOutputs().getFiles().getFiles().stream()
                        .flatMap(f -> {
                            if (f.isDirectory()) {
                                return Stream.of(Objects.requireNonNull(f.listFiles()));
                            } else {
                                return Stream.of(f);
                            }
                        })
                        .filter(f -> f.getName().endsWith(".apk"))
                        .findAny()
                        .orElseThrow()
        ).map(project::zipTree);

        var bundleClassesJar = project.getTasks().register(
                "bundle" + variantNameCap + "ToClassesJar",
                Jar.class, jar -> {
                    jar.getDestinationDirectory().set(
                            buildDir.dir("intermediates/module_classes_jar/" + variantName)
                    );

                    jar.getArchiveBaseName().set("classes.jar");

                    jar.setIncludeEmptyDirs(false);
                    jar.setEntryCompression(ZipEntryCompression.STORED);
                    jar.setPreserveFileTimestamps(false);

                    // classesX.dex
                    jar.from(apk, sp -> sp.include("classes*.dex"));
                }
        );

        var mergeMagisk = project.getTasks().register(
                "mergeMagisk" + variantNameCap,
                Sync.class, task -> {
                    task.into(buildDir.dir("intermediates/merged_magisk/" + variantName));

                    // module prop
                    task.from(generateModuleProp);

                    // initial packages
                    task.from(generateInitialPackages, sp -> sp.into("packages"));

                    // classes.jar
                    task.from(bundleClassesJar);

                    // native libraries
                    task.from(apk, sp -> {
                        sp.include("lib/*/*.so");
                        sp.eachFile(file -> {
                            String[] parts = file.getPath().split("/");
                            String name = parts[2];
                            // zygisk library is located in special place
                            if ("libzygisk_loader.so".equals(name)) {
                                String abi = parts[1];
                                file.setPath("zygisk/" + abi + ".so");
                            }
                        });
                    });

                    // assets
                    task.from(apk, sp -> {
                        sp.include("assets/**");
                        sp.eachFile(file -> file.setPath(file.getPath().substring("assets/".length())));
                    });
                }
        );

        var generateChecksum = project.getTasks().register(
                "generateChecksum" + variantNameCap,
                ChecksumTask.class, task -> {
                    task.getDestinationFile().set(
                            buildDir.dir("generated/checksum/" + variantName)
                                    .map(p -> p.file("00-verify-resources.sh"))
                    );
                    task.getRootDirectory().set(mergeMagisk.map(m ->
                            projectDir.dir(m.getDestinationDir().getAbsolutePath()))
                    );
                }
        );

        var generateCustomize = project.getTasks().register(
                "generateCustomize" + variantNameCap,
                CustomizeTask.class, task -> {
                    task.getDestinationFile().set(
                            buildDir.dir("generated/customize/" + variantName)
                                    .map(p -> p.file("customize.sh"))
                    );
                    task.getMergedDirectory().set(mergeMagisk.map(m ->
                            projectDir.dir(m.getDestinationDir().getAbsolutePath()))
                    );
                    task.getChecksumFileName().set(generateChecksum.map(c ->
                            c.getDestinationFile().get().getAsFile().getName()));
                }
        );

        var zipMagisk = project.getTasks().register(
                "zipMagisk" + variantNameCap,
                Zip.class, zip -> {
                    zip.getDestinationDirectory().set(
                            buildDir.dir("outputs/magisk/" + variant.getFlavorName() + "/" + variant.getBuildType())
                    );

                    zip.getArchiveBaseName().set(extension.getArchiveName() != null ?
                            extension.getArchiveName() : project.getName());

                    zip.setIncludeEmptyDirs(false);
                    zip.setEntryCompression(ZipEntryCompression.DEFLATED);
                    zip.setPreserveFileTimestamps(false);

                    zip.from(mergeMagisk);
                    zip.from(generateChecksum, sp -> sp.into("customize.d"));
                    zip.from(generateCustomize);
                }
        );

        project.getTasks().named(
                "assemble" + variantNameCap,
                t -> t.dependsOn(zipMagisk)
        );
    }
}
