package com.github.kr328.zloader;

import com.github.kr328.zloader.internal.Loader;

/**
 * Utilize to manage ZygoteLoader.
 */
public final class ZygoteLoader {
    /**
     * Package name that indicate currently in system_server
     * <p>
     * {@link #getPackageName}
     */
    public static final String PACKAGE_SYSTEM_SERVER = "android";

    /**
     * Get currently injected package name
     *
     * @return package name
     */
    public static String getPackageName() {
        return Loader.getPackageName();
    }
}
