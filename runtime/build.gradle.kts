plugins {
    id("com.android.library")
    `maven-publish`
}

android {
    compileSdk = 35

    namespace = "com.github.kr328.zloader"

    defaultConfig {
        minSdk = 26

        consumerProguardFiles("consumer-rules.pro")

        @Suppress("UnstableApiUsage")
        externalNativeBuild {
            cmake {
                abiFilters("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
                arguments("-DANDROID_STL=none")
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1+"
        }
    }

    buildFeatures {
        buildConfig = true
        prefab = true
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    compileOnly(libs.androidx.annotation)
}

publishing {
    publications {
        register<MavenPublication>(project.name) {
            artifactId = project.name
            groupId = project.group.toString()
            version = project.version.toString()

            afterEvaluate {
                from(components["release"])
            }
        }
    }
}
