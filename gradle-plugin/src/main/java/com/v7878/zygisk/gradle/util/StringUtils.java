package com.v7878.zygisk.gradle.util;

import javax.annotation.Nonnull;

public final class StringUtils {
    public static String capitalize(@Nonnull String in) {
        if (in.isEmpty()) {
            return "";
        }

        return Character.toUpperCase(in.charAt(0)) + in.substring(1);
    }
}
