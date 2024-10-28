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

    productFlavors {
        setFlavorDimensions(mutableListOf("loader"))

        create("riru") {
            dimension = "loader"

            @Suppress("UnstableApiUsage")
            externalNativeBuild {
                cmake {
                    arguments("-DLOADER:STRING=riru")
                }
            }
        }

        create("zygisk") {
            dimension = "loader"

            @Suppress("UnstableApiUsage")
            externalNativeBuild {
                cmake {
                    arguments("-DLOADER:STRING=zygisk")
                }
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
        multipleVariants("all") {
            withSourcesJar()
            withJavadocJar()
            includeBuildTypeValues("debug", "release")
            includeFlavorDimensionAndValues("loader", "riru", "zygisk")
        }
    }
}

dependencies {
    val riruImplementation by configurations

    compileOnly(libs.androidx.annotation)

    riruImplementation(libs.riru.runtime)
}

publishing {
    publications {
        register<MavenPublication>(project.name) {
            artifactId = project.name
            groupId = project.group.toString()
            version = project.version.toString()

            afterEvaluate {
                from(components["all"])
            }
        }
    }
}
