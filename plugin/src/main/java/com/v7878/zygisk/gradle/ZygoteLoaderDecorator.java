package com.v7878.zygisk.gradle;

import static com.v7878.zygisk.gradle.Utils.computeTaskName;

import com.android.build.api.variant.ApplicationVariant;
import com.android.build.api.variant.VariantOutput;
import com.v7878.zygisk.gradle.tasks.ChecksumTask;
import com.v7878.zygisk.gradle.tasks.CustomizeTask;
import com.v7878.zygisk.gradle.tasks.PackagesTask;
import com.v7878.zygisk.gradle.tasks.PropertiesTask;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.tasks.Sync;
import org.gradle.api.tasks.bundling.Zip;
import org.gradle.api.tasks.bundling.ZipEntryCompression;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class ZygoteLoaderDecorator {
    private static final String ID_PATTERN = "^[a-zA-Z][a-zA-Z0-9._-]+$";
    private static final String ZYGISK_LIB_NAME = "libzygisk_loader.so";

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

        VariantOutput variantOutput = variant.getOutputs().stream().findAny().orElseThrow();

        var buildDir = project.getLayout().getBuildDirectory();

        var generateInitialPackages = project.getTasks().register(
                computeTaskName("generateInitialPackages", variantName),
                PackagesTask.class, task -> {
                    task.getDestinationDirectory().set(
                            buildDir.dir("generated/packages/" + variantName)
                    );
                    task.getPackages().set(extension.getPackages());
                }
        );

        var apk = project.getTasks().named(computeTaskName("package", variantName))
                .map(p -> p.getOutputs().getFiles().getAsFileTree()
                        .matching(f -> f.include("**/*.apk"))
                        .getSingleFile()
                ).map(project::zipTree);

        var hasNativeLibs = apk.map(files -> files
                .matching(f -> f.include("lib/*/*.so"))
                .matching(f -> f.exclude("lib/*/" + ZYGISK_LIB_NAME))
                .getFiles().stream()
                .findAny().isPresent()
        );

        var moduleProps = hasNativeLibs.map(value -> {
            Map<String, String> props = new LinkedHashMap<>();

            props.put("version", variantOutput.getVersionName().getOrElse(""));
            props.put("versionCode", variantOutput.getVersionCode().getOrElse(0).toString());
            props.put("minSdkVersion", String.valueOf(variant.getMinSdk().getApiLevel()));
            if (variant.getMaxSdk() != null) {
                props.put("maxSdkVersion", String.valueOf(variant.getMaxSdk()));
            }

            putProperty(props, "id", true, extension.getId());
            if (!extension.getId().matches(ID_PATTERN)) {
                throw new GradleException(String.format(
                        "module id \"%s\" does not match %s", extension.getId(), ID_PATTERN));
            }
            putProperty(props, "name", true, extension.getName());
            putProperty(props, "author", true, extension.getAuthor());
            putProperty(props, "description", true, extension.getDescription());
            putProperty(props, "entrypoint", true, extension.getEntrypoint());
            putProperty(props, "updateJson", false, extension.getUpdateJson());
            Boolean libs = extension.getAttachNativeLibs();
            if (libs == null) {
                libs = value;
            }
            putProperty(props, "attachNativeLibs", false, Boolean.toString(libs));

            props.putAll(extension.getAdditionalProperties());
            return props;
        });

        var generateModuleProp = project.getTasks().register(
                computeTaskName("generateModuleProp", variantName),
                PropertiesTask.class, task -> {
                    task.getDestinationFile().set(
                            buildDir.dir("generated/properties/" + variantName)
                                    .map(p -> p.file("module.prop"))
                    );
                    task.getProperties().set(moduleProps);
                }
        );

        var mergeMagisk = project.getTasks().register(
                computeTaskName("mergeMagisk", variantName),
                Sync.class, task -> {
                    task.into(buildDir.dir("intermediates/merged_magisk/" + variantName));

                    // module prop
                    task.from(generateModuleProp);

                    // initial packages
                    task.from(generateInitialPackages, sp -> sp.into("packages"));

                    // dex files
                    task.from(apk, sp -> sp.include("classes*.dex"));

                    // native libraries
                    task.from(apk, sp -> {
                        sp.include("lib/*/*.so");
                        sp.eachFile(file -> {
                            String[] parts = file.getPath().split("/");
                            String name = parts[2];
                            // zygisk library is located in special place
                            if (ZYGISK_LIB_NAME.equals(name)) {
                                String abi = parts[1];
                                file.setPath("zygisk/" + abi + ".so");
                            }
                        });
                    });

                    // assets
                    task.from(apk, sp -> {
                        sp.include("assets/**");
                        sp.eachFile(file -> file.setPath(
                                file.getPath().substring("assets/".length())));
                    });
                }
        );

        var generateChecksum = extension.isGenerateChecksums() ? project.getTasks().register(
                computeTaskName("generateChecksum", variantName),
                ChecksumTask.class, task -> {
                    task.getDestinationFile().set(
                            buildDir.dir("generated/checksum/" + variantName)
                                    .map(p -> p.file("00-verify-resources.sh"))
                    );
                    task.getSourceDirectory().from(mergeMagisk);
                }
        ) : null;

        var generateCustomize = project.getTasks().register(
                computeTaskName("generateCustomize", variantName),
                CustomizeTask.class, task -> {
                    task.getDestinationFile().set(
                            buildDir.dir("generated/customize/" + variantName)
                                    .map(p -> p.file("customize.sh"))
                    );
                    task.getCustomizeFiles().addAll(mergeMagisk.map(m ->
                            m.getOutputs().getFiles().getAsFileTree()
                                    .matching(t -> t.include("customize.d/*"))
                                    .getFiles().stream().map(File::getName).toList()
                    ));
                    if (generateChecksum != null) {
                        task.getCustomizeFiles().add(generateChecksum.map((c ->
                                c.getDestinationFile().get().getAsFile().getName())));
                    }
                }
        );

        var zipMagisk = project.getTasks().register(
                computeTaskName("zipMagisk", variantName),
                Zip.class, zip -> {
                    zip.getDestinationDirectory().set(
                            buildDir.dir("outputs/magisk/" + variant.getFlavorName() + "/" + variant.getBuildType())
                    );

                    String name = extension.getArchiveName() != null
                            ? extension.getArchiveName()
                            : project.getName();
                    if (extension.isAddVariantToArchiveName()) {
                        name += "-" + variantName;
                    }

                    zip.getArchiveBaseName().set(name);

                    zip.setIncludeEmptyDirs(false);
                    zip.setEntryCompression(ZipEntryCompression.DEFLATED);
                    zip.setPreserveFileTimestamps(false);

                    zip.from(mergeMagisk);
                    if (generateChecksum != null) {
                        zip.from(generateChecksum, sp -> sp.into("customize.d"));
                    }
                    zip.from(generateCustomize);
                }
        );

        project.getTasks().named(
                computeTaskName("assemble", variantName),
                t -> t.dependsOn(zipMagisk)
        );
    }
}
