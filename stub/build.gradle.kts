plugins {
    alias(libs.plugins.java.library)
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}
