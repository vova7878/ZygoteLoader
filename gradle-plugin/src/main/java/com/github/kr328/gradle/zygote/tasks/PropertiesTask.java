package com.github.kr328.gradle.zygote.tasks;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.nio.file.Files;
import java.util.stream.Collectors;

public abstract class PropertiesTask extends DefaultTask {
    private static final String HEADER = "# Generated by ZygoteLoader. DO NOT EDIT.";

    @Input
    public abstract MapProperty<String, String> getProperties();

    @OutputFile
    public abstract RegularFileProperty getDestinationFile();

    @TaskAction
    public void doAction() throws Exception {
        String text = getProperties().get().entrySet().stream()
                .map(kv -> kv.getKey() + "=" + kv.getValue())
                .collect(Collectors.joining("\n", HEADER + "\n", "\n"));

        Files.writeString(getDestinationFile().get().getAsFile().toPath(), text);
    }
}
