package com.v7878.zygisk.gradle.tasks;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.nio.file.Files;

public abstract class BindingsTask extends DefaultTask {
    @OutputDirectory
    public abstract DirectoryProperty getDestinationDirectory();

    @Input
    public abstract Property<String> getEntrypoint();

    @TaskAction
    public void doAction() throws Throwable {
        var dst = getDestinationDirectory();
        for (File file : dst.getAsFileTree()) {
            Files.delete(file.toPath());
        }

        var file = dst.file("com/v7878/zygisk/Bindings.java").get().getAsFile().toPath();
        Files.createDirectories(file.getParent());

        Files.writeString(file, String.format("""
                package com.v7878.zygisk;
                
                public final class Bindings {
                    public static void premain() throws Throwable {
                        %s.premain();
                    }
                    public static void main() throws Throwable {
                        %<s.main();
                    }
                }
                """.trim(), getEntrypoint().get())
        );
    }
}
