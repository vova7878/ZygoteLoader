package com.github.kr328.gradle.zygote.tasks;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

public abstract class ChecksumTask extends DefaultTask {
    private static final String VERIFY_HEADER = String.join("\n",
            "# Generated by ZygoteLoader. DO NOT EDIT.",
            "",
            "ui_print \"- Verify module resources\"",
            "",
            "function do_verify() {",
            "    [ -f \"$MODPATH/$1\" ] || abort \"! Module resource '$1' not found\"",
            "",
            "    (echo \"$2  $MODPATH/$1\" | sha256sum -c -s -) || abort \"! Module resource '$1' verify failed\"",
            "",
            "    ui_print \"  '$1' verified\"",
            "}"
    );

    private static final String VERIFY_TAIL = String.join("\n",
            "unset -f do_verify"
    );

    private static String checksum(final Path path) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");

            OutputStream stream = new OutputStream() {
                @Override
                public void write(int i) throws IOException {
                    this.write(new byte[]{(byte) i});
                }

                @Override
                public void write(@Nonnull byte[] b, int off, int len) {
                    sha256.update(b, off, len);
                }
            };

            Files.copy(path, stream);

            StringBuilder sb = new StringBuilder();
            for (final byte b : sha256.digest()) {
                sb.append(String.format("%02x", ((int) b) & 0xff));
            }
            return sb.toString();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    @InputDirectory
    public abstract DirectoryProperty getRootDirectory();

    @OutputFile
    public abstract RegularFileProperty getDestinationFile();

    @TaskAction
    public void doAction() throws Exception {
        Path root = getRootDirectory().getAsFile().get().toPath();

        List<Map.Entry<String, String>> checksums = Files.walk(root)
                .filter(Files::isRegularFile)
                .map(p -> root.relativize(p).toString())
                .filter(p -> !p.startsWith("META-INF"))
                .map(p -> Map.entry(p.replace('\\', '/'), checksum(root.resolve(p))))
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toList());

        StringBuilder sb = new StringBuilder();
        sb.append(VERIFY_HEADER).append("\n\n");

        for (var checksum : checksums) {
            sb.append("do_verify ")
                    .append(checksum.getKey())
                    .append(" ")
                    .append(checksum.getValue())
                    .append('\n');
        }

        sb.append("\n\n").append(VERIFY_TAIL);

        Files.writeString(getDestinationFile().getAsFile().get().toPath(), sb);
    }
}
