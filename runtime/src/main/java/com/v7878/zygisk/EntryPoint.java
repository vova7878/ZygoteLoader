package com.v7878.zygisk;

import android.util.Log;

import com.v7878.r8.annotations.DoNotObfuscate;
import com.v7878.r8.annotations.DoNotObfuscateType;
import com.v7878.r8.annotations.DoNotShrink;
import com.v7878.r8.annotations.DoNotShrinkType;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
@DoNotObfuscateType
@DoNotShrinkType
final class EntryPoint {
    private static final String TAG = "ZygoteLoader[Java]";

    private static String packageName;
    private static Map<String, String> moduleProps;

    @DoNotObfuscate
    @DoNotShrink
    private static void load(String packageName, ByteBuffer properties) {
        try {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Loading in " + packageName);
            }

            init(packageName, StandardCharsets.UTF_8.decode(properties).toString());
        } catch (Throwable throwable) {
            Log.e(TAG, "doLoad", throwable);
        }
    }

    private static void init(String packageName, String propertiesText) {
        Map<String, String> properties = new HashMap<>();

        for (String line : propertiesText.split("\n")) {
            String[] kv = line.split("=", 2);
            if (kv.length != 2)
                continue;

            properties.put(kv[0].trim(), kv[1].trim());
        }

        String entrypointName = properties.get("entrypoint");
        if (entrypointName == null) {
            Log.e(TAG, "Entrypoint not found");

            return;
        }

        EntryPoint.packageName = packageName;
        EntryPoint.moduleProps = Collections.unmodifiableMap(properties);

        try {
            Class.forName(entrypointName).getMethod("main").invoke(null);
        } catch (ReflectiveOperationException e) {
            Log.e(TAG, "Invoke main of " + entrypointName, e);
        }
    }

    public static String getPackageName() {
        return packageName;
    }

    public static Map<String, String> getProperties() {
        return moduleProps;
    }
}
