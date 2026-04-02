package com.v7878.zygisk;

import static java.nio.charset.StandardCharsets.UTF_8;

import android.util.Log;

import com.v7878.r8.annotations.DoNotObfuscate;
import com.v7878.r8.annotations.DoNotShrink;
import com.v7878.r8.annotations.DoNotShrinkType;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;

import dalvik.system.BaseDexClassLoader;

@SuppressWarnings("unused")
@DoNotShrinkType
final class EntryPoint {
    private static final String TAG = "ZygoteLoader[Java]";

    private static String packageName;
    private static String processName;
    private static String moduleDir;
    private static Map<String, String> properties;

    @DoNotObfuscate
    @DoNotShrink
    private static boolean load(String packageName, String processName, int moduleDirFD) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Loading in " + packageName);
        }
        moduleDir = "/proc/self/fd/" + moduleDirFD;
        try {
            File props = new File(moduleDir, "module.prop");
            byte[] propsData = Files.readAllBytes(props.toPath());
            return init(packageName, processName, new String(propsData, UTF_8));
        } catch (Throwable throwable) {
            Log.e(TAG, "load", throwable);
            return false;
        }
    }

    private static boolean init(String packageName, String processName, String props) throws Throwable {
        EntryPoint.packageName = packageName;
        EntryPoint.processName = processName;
        EntryPoint.properties = Utils.toMap(props);

        libs:
        {
            String attachNativeLibs = properties.get("attachNativeLibs");
            if (attachNativeLibs == null) {
                Log.e(TAG, "attachNativeLibs flag does not exist and will be treated as false");
                break libs;
            }
            if (Boolean.parseBoolean(attachNativeLibs)) {
                File libFolder = new File(moduleDir, "lib");
                if (libFolder.isDirectory()) {
                    Utils.addNativePath((BaseDexClassLoader) EntryPoint.class.getClassLoader(),
                            new File(libFolder, Utils.getNativeLibraryFolderName()));
                }
            }
        }

        return true;
    }

    @DoNotObfuscate
    @DoNotShrink
    private static void preSpecialize() {
        try {
            Bindings.premain();
        } catch (Throwable th) {
            Log.e(TAG, "Invoke premain", th);
        }
        moduleDir = null;
    }

    @DoNotObfuscate
    @DoNotShrink
    private static void postSpecialize() {
        try {
            Bindings.main();
        } catch (Throwable th) {
            Log.e(TAG, "Invoke main", th);
        }
    }

    public static String getModuleDir() {
        return moduleDir;
    }

    public static String getPackageName() {
        return packageName;
    }

    public static String getProcessName() {
        return processName;
    }

    public static Map<String, String> getProperties() {
        return properties;
    }
}
