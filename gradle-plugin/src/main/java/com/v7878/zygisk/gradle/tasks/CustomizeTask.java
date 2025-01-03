package com.v7878.zygisk.gradle.tasks;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public abstract class CustomizeTask extends DefaultTask {
    private static final String HEADER = "# Generated by ZygoteLoader. DO NOT EDIT.";
    private static final String TAIL = "rm -rf $MODPATH/customize.d";

    @InputDirectory
    public abstract DirectoryProperty getMergedDirectory();

    @Input
    public abstract Property<String> getChecksumFileName();

    @OutputFile
    public abstract RegularFileProperty getDestinationFile();

    @TaskAction
    public void doAction() throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append(HEADER).append("\n\n");

        try (var files = Files.list(getMergedDirectory()
                .dir("customize.d").get().getAsFile().toPath())) {
            Stream<String> customizes = Stream.concat(
                    Stream.of(getChecksumFileName().get()),
                    files.map(Path::getFileName)
                            .map(Path::toString)
            ).sorted();

            customizes.forEach(f -> {
                sb.append(String.format("[ -f \"$MODPATH/customize.d/%1$s\" ] || abort \"! Part '%1$s' not found\"", f)).append('\n');
                sb.append(String.format(". \"$MODPATH/customize.d/%1$s\"", f)).append('\n');
            });
        }

        sb.append("\n\n").append(TAIL);

        Files.writeString(getDestinationFile().getAsFile().get().toPath(), sb);
    }
}
