package com.v7878.zygisk;

import static java.nio.charset.StandardCharsets.UTF_8;

import android.system.Os;
import android.util.Log;

import com.v7878.r8.annotations.DoNotObfuscate;
import com.v7878.r8.annotations.DoNotObfuscateType;
import com.v7878.r8.annotations.DoNotShrink;
import com.v7878.r8.annotations.DoNotShrinkType;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
@DoNotObfuscateType
@DoNotShrinkType
final class EntryPoint {
    private static final String TAG = "ZygoteLoader[Java]";

    private static String packageName;
    private static String moduleDir;
    private static Map<String, String> properties;
    private static Class<?> entrypoint;

    @DoNotObfuscate
    @DoNotShrink
    private static boolean load(String packageName, int moduleDirFD, ByteBuffer props) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Loading in " + packageName);
        }
        try {
            moduleDir = Os.readlink("proc/self/fd/" + moduleDirFD);
            return init(packageName, UTF_8.decode(props).toString());
        } catch (Throwable throwable) {
            Log.e(TAG, "load", throwable);
            return false;
        }
    }

    private static boolean init(String packageName, String props) {
        Map<String, String> properties = new HashMap<>();

        for (String line : props.split("\n")) {
            String[] kv = line.split("=", 2);
            if (kv.length != 2)
                continue;

            properties.put(kv[0].trim(), kv[1].trim());
        }

        EntryPoint.packageName = packageName;
        EntryPoint.properties = Collections.unmodifiableMap(properties);

        String entrypointName = properties.get("entrypoint");
        if (entrypointName == null) {
            Log.e(TAG, "Entrypoint not found");
            return false;
        }

        try {
            entrypoint = Class.forName(entrypointName);
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "Trying to find class " + entrypointName, e);
            return false;
        }
        return true;
    }

    @DoNotObfuscate
    @DoNotShrink
    private static void preSpecialize() {
        try {
            entrypoint.getMethod("premain").invoke(null);
        } catch (ReflectiveOperationException e) {
            Log.e(TAG, "Invoke premain of " + entrypoint, e);
        }
    }

    @DoNotObfuscate
    @DoNotShrink
    private static void postSpecialize() {
        try {
            entrypoint.getMethod("main").invoke(null);
        } catch (ReflectiveOperationException e) {
            Log.e(TAG, "Invoke main of " + entrypoint, e);
        }
    }

    public static String getModuleDir() {
        return moduleDir;
    }

    public static String getPackageName() {
        return packageName;
    }

    public static Map<String, String> getProperties() {
        return properties;
    }
}
