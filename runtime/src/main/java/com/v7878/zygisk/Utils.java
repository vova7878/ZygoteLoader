package com.v7878.zygisk;

import android.annotation.SuppressLint;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import dalvik.system.BaseDexClassLoader;

class Utils {
    public static Map<String, String> toMap(String properties) {
        var map = new HashMap<String, String>();

        for (String line : properties.split("\n")) {
            String[] kv = line.split("=", 2);
            if (kv.length != 2)
                continue;

            map.put(kv[0].trim(), kv[1].trim());
        }

        return Collections.unmodifiableMap(map);
    }

    public static String getCurrentInstructionSet() throws ReflectiveOperationException {
        @SuppressLint("DiscouragedPrivateApi")
        class Holder {
            static final Method getCurrentInstructionSet;

            static {
                try {
                    var VMRuntime = Class.forName("dalvik.system.VMRuntime");
                    getCurrentInstructionSet = VMRuntime.getDeclaredMethod("getCurrentInstructionSet");
                    getCurrentInstructionSet.setAccessible(true);
                } catch (Throwable th) {
                    throw new ExceptionInInitializerError(th);
                }
            }
        }
        return (String) Holder.getCurrentInstructionSet.invoke(null);
    }

    public static String getNativeLibraryFolderName() throws ReflectiveOperationException {
        String isa = getCurrentInstructionSet();
        return switch (isa) {
            case "arm" -> "armeabi-v7a";
            case "arm64" -> "arm64-v8a";
            default -> isa;
        };
    }

    public static void addNativePath(BaseDexClassLoader loader, File... libPaths) throws ReflectiveOperationException {
        @SuppressLint("DiscouragedPrivateApi")
        @SuppressWarnings("JavaReflectionMemberAccess")
        class Holder {
            static final Field pathList;
            static final Field nativeElements;
            static final Constructor<?> initElement;

            static {
                try {
                    pathList = BaseDexClassLoader.class.getDeclaredField("pathList");
                    pathList.setAccessible(true);
                    var DexPathList = Class.forName("dalvik.system.DexPathList");
                    nativeElements = DexPathList.getDeclaredField("nativeLibraryPathElements");
                    nativeElements.setAccessible(true);
                    var NativeLibraryElement = Class.forName("dalvik.system.DexPathList$NativeLibraryElement");
                    initElement = NativeLibraryElement.getDeclaredConstructor(File.class);
                    initElement.setAccessible(true);
                } catch (Throwable th) {
                    throw new ExceptionInInitializerError(th);
                }
            }
        }
        Objects.requireNonNull(loader);
        if (libPaths.length == 0) {
            return;
        }
        var pathList = Holder.pathList.get(loader);
        var elements = (Object[]) Holder.nativeElements.get(pathList);
        //noinspection DataFlowIssue
        int oldLength = elements.length;
        elements = Arrays.copyOf(elements, oldLength + libPaths.length);
        for (int i = 0; i < libPaths.length; i++) {
            elements[oldLength + i] = Holder.initElement.newInstance(libPaths[i]);
        }
        Holder.nativeElements.set(pathList, elements);
    }
}
