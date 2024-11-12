plugins {
    alias(libs.plugins.java.gradle.plugin)
    alias(libs.plugins.maven.publish)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17

    withSourcesJar()
}

dependencies {
    compileOnly(gradleApi())
    compileOnly(libs.android.gradle)
}

gradlePlugin {
    plugins {
        create("zygote") {
            id = "${rootProject.group}.${rootProject.name}"
            implementationClass = "com.v7878.zygisk.gradle.ZygoteLoaderPlugin"
        }
    }
}

val dynamicSources = layout.buildDirectory.dir("generated/dynamic").get()

sourceSets {
    named("main") {
        java.srcDir(dynamicSources)
    }
}

task("generateDynamicSources") {
    outputs.dir(dynamicSources)

    tasks.withType(JavaCompile::class.java).forEach { it.dependsOn(this) }
    tasks["sourcesJar"].dependsOn(this)

    doFirst {
        val buildConfig = dynamicSources.file(
            "com/v7878/zygisk/gradle/BuildConfig.java"
        ).asFile

        buildConfig.parentFile.mkdirs()

        val runtimeProject = project(":runtime")
        buildConfig.writeText(
            """
            package com.v7878.zygisk.gradle;
            
            public final class BuildConfig {
                public static final String RUNTIME_DEPENDENCY =
                 "${rootProject.group}.${rootProject.name}:${runtimeProject.name}:${runtimeProject.version}";
            }
            """.trimIndent()
        )
    }
}

publishing {
    publications {
        register<MavenPublication>("pluginMaven") {
            artifactId = project.name
            groupId = project.group.toString()
            version = project.version.toString()
        }
    }
}
