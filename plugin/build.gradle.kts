import com.vanniktech.maven.publish.GradlePlugin
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SourcesJar

plugins {
    alias(libs.plugins.java.gradle.plugin)
    alias(libs.plugins.maven.publish)
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
    withSourcesJar()
}

dependencies {
    compileOnly(gradleApi())
    compileOnly(libs.android.gradle)
}

gradlePlugin {
    plugins {
        create("zygote") {
            id = "io.github.vova7878.ZygoteLoader"
            implementationClass = "com.v7878.zygisk.gradle.ZygoteLoaderPlugin"
        }
    }
}

val dynamicSources = layout.buildDirectory.dir("generated/dynamic")!!

sourceSets {
    named("main") {
        java.srcDir(dynamicSources)
    }
}

val generator = tasks.register("generateDynamicSources") {
    outputs.dir(dynamicSources)

    doFirst {
        val buildConfig = dynamicSources.get().file(
            "com/v7878/zygisk/gradle/BuildConfig.java"
        ).asFile

        buildConfig.parentFile.mkdirs()

        buildConfig.writeText(
            """
            package com.v7878.zygisk.gradle;
            
            public final class BuildConfig {
                public static final String RUNTIME_DEPENDENCY =
                 "io.github.vova7878.ZygoteLoader:runtime:${rootProject.version}";
            }
            """.trimIndent()
        )
    }
}!!

tasks["compileJava"].dependsOn(generator)
tasks["sourcesJar"].dependsOn(generator)

mavenPublishing {
    publishToMavenCentral(automaticRelease = false)
    signAllPublications()
    configure(
        GradlePlugin(
            javadocJar = JavadocJar.Empty(),
            sourcesJar = SourcesJar.Sources()
        )
    )

    coordinates(
        groupId = "io.github.vova7878",
        artifactId = "ZygoteLoader",
        version = project.version.toString()
    )

    pom {
        name.set("ZygoteLoader")
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
