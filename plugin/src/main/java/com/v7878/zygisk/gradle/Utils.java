package com.v7878.zygisk.gradle;

import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.file.FileVisitor;

import java.util.function.Consumer;

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
}
