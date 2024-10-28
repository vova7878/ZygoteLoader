plugins {
    `java-gradle-plugin`
    `maven-publish`
}

val dynamicSources = buildDir.resolve("generated/dynamic")

java {
    withSourcesJar()
}

dependencies {
    compileOnly(gradleApi())
    compileOnly(libs.android.gradle)
}

sourceSets {
    named("main") {
        java.srcDir(dynamicSources)
    }
}

gradlePlugin {
    plugins {
        create("zygote") {
            id = "com.github.vova7878.ZygoteLoader"
            implementationClass = "com.github.kr328.gradle.zygote.ZygoteLoaderPlugin"
        }
    }
}

task("generateDynamicSources") {
    inputs.property("moduleGroup", project.group)
    inputs.property("moduleArtifact", project.name)
    inputs.property("moduleVersion", project.version)
    outputs.dir(dynamicSources)
    tasks.withType(JavaCompile::class.java).forEach { it.dependsOn(this) }
    tasks["sourcesJar"].dependsOn(this)

    doFirst {
        val buildConfig = dynamicSources.resolve("com/github/kr328/gradle/zygote/BuildConfig.java")

        buildConfig.parentFile.mkdirs()

        val rp = project(":runtime")
        buildConfig.writeText(
            """
            package com.github.kr328.gradle.zygote;
            
            public final class BuildConfig {
                public static final String RUNTIME_DEPENDENCY = "${rp.group}.ZygoteLoader:${rp.name}:${rp.version}";
            }
            """.trimIndent()
        )
    }
}

publishing {
    publications {
        register<MavenPublication>(project.name) {
            artifactId = project.name
            groupId = project.group.toString()
            version = project.version.toString()

            from(components["java"])
        }
    }
}
