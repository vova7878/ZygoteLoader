package com.github.kr328.zloader.internal;

import android.util.Log;

import com.github.kr328.zloader.BuildConfig;
import com.v7878.r8.annotations.DoNotObfuscate;
import com.v7878.r8.annotations.DoNotObfuscateType;
import com.v7878.r8.annotations.DoNotShrink;
import com.v7878.r8.annotations.DoNotShrinkType;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
@DoNotObfuscateType
@DoNotShrinkType
public final class Loader {
    private static final String TAG = "ZygoteLoader[Java]";

    private static String dynamicPackagesPath;
    private static String dataDirectory;
    private static String packageName;

    @DoNotObfuscate
    @DoNotShrink
    private static void load(String packageName, ByteBuffer properties) {
        try {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Loading in " + packageName);
            }

            init(packageName, StandardCharsets.UTF_8.decode(properties).toString());
        } catch (Throwable throwable) {
            Log.e(TAG, "doLoad: " + throwable, throwable);
        }
    }

    public static void init(String packageName, String propertiesText) {
        Map<String, String> properties = new HashMap<>();

        for (String line : propertiesText.split("\n")) {
            String[] kv = line.split("=", 2);
            if (kv.length != 2)
                continue;

            properties.put(kv[0].trim(), kv[1].trim());
        }

        String dataDirectory = properties.get("dataDirectory");
        if (dataDirectory == null) {
            Log.e(TAG, "Data directory not found");

            return;
        }

        String entrypointName = properties.get("entrypoint");
        if (entrypointName == null) {
            Log.e(TAG, "Entrypoint not found");

            return;
        }

        Loader.dynamicPackagesPath = dataDirectory + "/packages";
        Loader.dataDirectory = dataDirectory;
        Loader.packageName = packageName;

        try {
            ClassLoader loader = Loader.class.getClassLoader();
            if (loader == null) {
                throw new ClassNotFoundException("ClassLoader of " + Loader.class + " unavailable");
            }

            loader.loadClass(entrypointName).getMethod("main").invoke(null);
        } catch (final ReflectiveOperationException e) {
            Log.e(TAG, "Invoke main of " + entrypointName, e);
        }
    }

    public static String getDynamicPackagesPath() {
        return dynamicPackagesPath;
    }

    public static String getDataDirectory() {
        return dataDirectory;
    }

    public static String getPackageName() {
        return packageName;
    }
}
