package com.v7878.zygisk;

import static java.nio.charset.StandardCharsets.UTF_8;

import android.util.Log;

import com.v7878.r8.annotations.DoNotObfuscate;
import com.v7878.r8.annotations.DoNotObfuscateType;
import com.v7878.r8.annotations.DoNotShrink;
import com.v7878.r8.annotations.DoNotShrinkType;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;

import dalvik.system.BaseDexClassLoader;

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
    private static boolean load(String packageName, int moduleDirFD) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Loading in " + packageName);
        }
        moduleDir = "/proc/self/fd/" + moduleDirFD;
        try {
            File props = new File(moduleDir, "module.prop");
            byte[] propsData = Files.readAllBytes(props.toPath());
            return init(packageName, new String(propsData, UTF_8));
        } catch (Throwable throwable) {
            Log.e(TAG, "load", throwable);
            return false;
        }
    }

    private static boolean init(String packageName, String props) throws Throwable {
        EntryPoint.packageName = packageName;
        EntryPoint.properties = Utils.toMap(props);

        File libFolder = new File(moduleDir, "lib");
        if (libFolder.isDirectory()) {
            Utils.addNativePath((BaseDexClassLoader) EntryPoint.class.getClassLoader(),
                    new File(libFolder, Utils.getNativeLibraryFolderName()));
        }

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
        moduleDir = null;
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
