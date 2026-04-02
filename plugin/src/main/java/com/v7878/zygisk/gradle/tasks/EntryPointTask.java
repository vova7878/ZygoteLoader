package com.v7878.zygisk.gradle.tasks;

import com.v7878.zygisk.gradle.Utils;
import com.v7878.zygisk.gradle.Utils.Mapping;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.nio.file.Files;

public abstract class EntryPointTask extends DefaultTask {
    private static final String ENTRYPOINT = "com.v7878.zygisk.EntryPoint";

    @InputFile
    @Optional
    public abstract RegularFileProperty getMappings();

    @OutputFile
    public abstract RegularFileProperty getDestinationFile();

    @TaskAction
    public void doAction() throws Exception {
        String entrypoint;
        if (getMappings().isPresent()) {
            try (var maps = Utils.mappings(getMappings().get().getAsFile().toPath())) {
                entrypoint = maps
                        .filter(mapping -> ENTRYPOINT.equals(mapping.original()))
                        .map(Mapping::renamed)
                        .findAny().orElse(ENTRYPOINT);
            }
        } else {
            // debug build
            entrypoint = ENTRYPOINT;
        }

        Files.writeString(getDestinationFile().get().getAsFile().toPath(), entrypoint);
    }
}
