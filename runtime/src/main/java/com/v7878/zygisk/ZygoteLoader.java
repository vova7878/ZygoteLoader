package com.v7878.zygisk;

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
    public static final String ALL_PACKAGES = ".all";

    /**
     * Get currently injected package name
     *
     * @return package name
     */
    public static String getPackageName() {
        return EntryPoint.getPackageName();
    }
}
