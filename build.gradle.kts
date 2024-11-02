plugins {
    alias(libs.plugins.android.library) apply false
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
