import com.vanniktech.maven.publish.AndroidMultiVariantLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SourcesJar

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.maven.publish)
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

android {
    enableKotlin = false

    namespace = "com.v7878.zygisk"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        minSdk = 26

        consumerProguardFiles("consumer-rules.pro")

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
    }
}

dependencies {
    compileOnly(project(":stub"))

    implementation(libs.r8.annotations)
}

mavenPublishing {
    publishToMavenCentral(automaticRelease = false)
    configure(
        AndroidMultiVariantLibrary(
            javadocJar = JavadocJar.Empty(),
            sourcesJar = SourcesJar.Sources()
        )
    )

    coordinates(
        groupId = "io.github.vova7878.ZygoteLoader",
        artifactId = "runtime",
        version = project.version.toString()
    )

    pom {
        name.set("ZygoteLoader runtime")
        description.set("A plugin for creating Java-only Zygisk modules")
        inceptionYear.set("2026")
        url.set("https://github.com/vova7878/ZygoteLoader")

        licenses {
            license {
                name.set("MIT")
                url.set("https://opensource.org/license/mit")
                distribution.set("repository")
            }
        }

        developers {
            developer {
                id.set("vova7878")
                name.set("Vladimir Kozelkov")
                url.set("https://github.com/vova7878")
            }
            developer {
                id.set("Kr328")
                url.set("https://github.com/Kr328")
            }
        }

        scm {
            url.set("https://github.com/vova7878/ZygoteLoader")
            connection.set("scm:git:git://github.com/vova7878/ZygoteLoader.git")
            developerConnection.set("scm:git:ssh://git@github.com/vova7878/ZygoteLoader.git")
        }
    }
}
