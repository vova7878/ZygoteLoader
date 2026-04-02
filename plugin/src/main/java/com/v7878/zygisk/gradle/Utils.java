package com.v7878.zygisk.gradle;

import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.file.FileVisitor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.stream.Stream;

public final class Utils {
    public static String capitalize(String in) {
        if (in.isEmpty()) {
            return "";
        }

        return Character.toUpperCase(in.charAt(0)) + in.substring(1);
    }

    public static String computeTaskName(String first, String... parts) {
        StringBuilder b = new StringBuilder(first);
        for (String part : parts) {
            b.append(capitalize(part));
        }
        return b.toString();
    }

    public static FileVisitor files(Consumer<FileVisitDetails> visitor) {
        return new FileVisitor() {
            @Override
            public void visitDir(FileVisitDetails dirDetails) {
                // nop
            }

            @Override
            public void visitFile(FileVisitDetails fileDetails) {
                visitor.accept(fileDetails);
            }
        };
    }

    public record Mapping(String original, String renamed) {
    }

    @SuppressWarnings("resource")
    public static Stream<Mapping> mappings(Path path) {
        try {
            return Files.lines(path).mapMulti((line, mapper) -> {
                if (line.startsWith(" ") || line.startsWith("#")) {
                    return;
                }
                if (!line.endsWith(":")) {
                    return;
                }
                line = line.substring(0, line.length() - 1);
                var parts = line.split(" -> ");
                assert parts.length == 2;
                mapper.accept(new Mapping(parts[0], parts[1]));
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
