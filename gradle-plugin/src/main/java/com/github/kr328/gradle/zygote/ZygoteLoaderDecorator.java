package com.github.kr328.gradle.zygote;

import com.android.build.api.variant.ApplicationVariant;
import com.android.build.api.variant.VariantOutput;
import com.github.kr328.gradle.zygote.tasks.ChecksumTask;
import com.github.kr328.gradle.zygote.tasks.CustomizeTask;
import com.github.kr328.gradle.zygote.tasks.PackagesTask;
import com.github.kr328.gradle.zygote.tasks.PropertiesTask;
import com.github.kr328.gradle.zygote.util.StringUtils;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Sync;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Zip;
import org.gradle.api.tasks.bundling.ZipEntryCompression;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public final class ZygoteLoaderDecorator {
    private final Project project;
    private final ZygoteLoaderExtension extension;

    public ZygoteLoaderDecorator(Project project, ZygoteLoaderExtension extension) {
        this.project = project;
        this.extension = extension;
    }

    public void decorateVariant(ApplicationVariant variant) {
        TaskProvider<Task> pack = project.getTasks().named("package" + StringUtils.capitalize(variant.getName()));
        VariantOutput variantOutput = variant.getOutputs().stream().findAny().orElseThrow();

        Map<String, String> moduleProp = new LinkedHashMap<>();

        moduleProp.put("version", variantOutput.getVersionName().getOrElse(""));
        moduleProp.put("versionCode", variantOutput.getVersionCode().getOrElse(0).toString());
        moduleProp.put("minSdkVersion", String.valueOf(variant.getMinSdk().getApiLevel()));
        if (variant.getMaxSdk() != null) {
            moduleProp.put("maxSdkVersion", String.valueOf(variant.getMaxSdk()));
        }

        moduleProp.put("id", extension.getId());
        moduleProp.put("name", extension.getName());
        moduleProp.put("author", extension.getAuthor());
        moduleProp.put("description", extension.getDescription());
        moduleProp.put("entrypoint", extension.getEntrypoint());

        TaskProvider<PropertiesTask> generateModuleProp = project.getTasks().register(
                "generateModuleProp" + StringUtils.capitalize(variant.getName()),
                PropertiesTask.class, task -> {
                    task.getDestinationFile().set(
                            project.getLayout().getBuildDirectory()
                                    .dir("generated/properties/" + variant.getName())
                                    .map(p -> p.file("module.prop"))
                    );
                    task.getProperties().set(moduleProp);
                }
        );

        TaskProvider<PackagesTask> generateInitialPackages = project.getTasks().register(
                "generateInitialPackages" + StringUtils.capitalize(variant.getName()),
                PackagesTask.class, task -> {
                    task.getDestinationDirectory().set(
                            project.getLayout().getBuildDirectory()
                                    .dir("generated/packages/" + variant.getName())
                    );
                    task.getPackages().set(extension.getPackages());
                }
        );

        TaskProvider<Sync> mergeMagisk = project.getTasks().register(
                "mergeMagisk" + StringUtils.capitalize(variant.getName()),
                Sync.class, task -> {
                    task.into(project.getLayout().getBuildDirectory().dir("intermediates/merged_magisk/" + variant.getName()));

                    Provider<File> apkFile = pack.map(p ->
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
                    );

                    Provider<FileCollection> apk = apkFile.map(project::zipTree);

                    task.getInputs().file(apkFile);

                    // module prop
                    task.from(generateModuleProp);

                    // initial packages
                    task.from(generateInitialPackages, sp -> sp.into("packages"));

                    // classes.dex
                    task.from(apk, sp -> sp.include("classes.dex"));

                    // native libraries
                    task.from(apk, sp -> {
                        sp.include("lib/*/*.so");
                        sp.eachFile(file -> {
                            String abi = file.getPath().split("/")[1];
                            file.setPath("zygisk/" + abi + ".so");
                        });
                    });

                    // assets
                    task.from(apk, sp -> {
                        sp.include("assets/**");
                        sp.eachFile(file -> file.setPath(file.getPath().substring("assets/".length())));
                    });
                }
        );
        TaskProvider<ChecksumTask> generateChecksum = project.getTasks().register(
                "generateChecksum" + StringUtils.capitalize(variant.getName()),
                ChecksumTask.class, task -> {
                    task.getDestinationFile().set(
                            project.getLayout().getBuildDirectory()
                                    .dir("generated/checksum/" + variant.getName())
                                    .map(p -> p.file("00-verify-resources.sh"))
                    );
                    task.getRootDirectory().set(mergeMagisk.map(m ->
                            project.getLayout().getProjectDirectory()
                                    .dir(m.getDestinationDir().getAbsolutePath()))
                    );
                }
        );

        TaskProvider<CustomizeTask> generateCustomize = project.getTasks().register(
                "generateCustomize" + StringUtils.capitalize(variant.getName()),
                CustomizeTask.class, task -> {
                    task.getDestinationFile().set(
                            project.getLayout().getBuildDirectory()
                                    .dir("generated/customize/" + variant.getName())
                                    .map(p -> p.file("customize.sh"))
                    );
                    task.getMergedDirectory().set(mergeMagisk.map(m ->
                            project.getLayout().getProjectDirectory()
                                    .dir(m.getDestinationDir().getAbsolutePath()))
                    );
                    task.getChecksumFileName().set(generateChecksum.map(c -> c.getDestinationFile().get().getAsFile().getName()));
                }
        );

        TaskProvider<Zip> zipMagisk = project.getTasks().register(
                "zipMagisk" + StringUtils.capitalize(variant.getName()),
                Zip.class, zip -> {
                    zip.getDestinationDirectory().set(
                            project.getLayout().getBuildDirectory()
                                    .dir("outputs/magisk/" + variant.getFlavorName() + "/" + variant.getBuildType())
                    );

                    zip.getArchiveBaseName().set(project.getName());
                    if (extension.getArchiveName() != null) {
                        zip.getArchiveBaseName().set(extension.getArchiveName());
                    }

                    zip.setIncludeEmptyDirs(false);
                    zip.setEntryCompression(ZipEntryCompression.DEFLATED);
                    zip.setPreserveFileTimestamps(false);

                    zip.from(mergeMagisk);
                    zip.from(generateChecksum, sp -> sp.into("customize.d"));
                    zip.from(generateCustomize);
                }
        );

        project.getTasks().named(
                "assemble" + StringUtils.capitalize(variant.getName()),
                t -> t.dependsOn(zipMagisk)
        );
    }
}
