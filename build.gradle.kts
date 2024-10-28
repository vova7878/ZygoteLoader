plugins {
    id("com.android.library") version "8.7.1" apply false
}

subprojects {
    plugins.withId("java") {
        extensions.configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11
        }
    }
}

task("clean", type = Delete::class) {
    delete(rootProject.buildDir)
}
