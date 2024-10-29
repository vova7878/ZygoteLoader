@file:Suppress("UnstableApiUsage")

rootProject.name = "ZygoteLoader"

include(":runtime")
include(":gradle-plugin")

pluginManagement {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
    versionCatalogs {
        create("libs") {
            val agp = "8.7.1"
            val annotation = "1.7.0"

            library("android-gradle", "com.android.tools.build:gradle:$agp")
            library("androidx-annotation", "androidx.annotation:annotation:$annotation")
        }
    }
}