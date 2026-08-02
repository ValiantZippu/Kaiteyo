// Plugin versions are declared in settings.gradle.kts (pluginManagement)
// and dependencies in gradle/libs.versions.toml.
// The `apply false` declarations below keep Kotlin core plugins
// (kapt, parcelize, ...) resolvable in module build scripts without versions.
plugins {
    kotlin("multiplatform") apply false
    kotlin("plugin.compose") apply false
    kotlin("plugin.serialization") apply false
    kotlin("android") apply false
    id("com.android.application") apply false
    id("com.android.library") apply false
    id("org.jetbrains.compose") apply false
    alias(libs.plugins.build.config) apply false
    id("com.mikepenz.aboutlibraries.plugin") apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
