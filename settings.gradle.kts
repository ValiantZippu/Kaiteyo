pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }

    val kotlinVersion = "2.1.20"
    val agpVersion = "8.5.2"

    plugins {
        kotlin("multiplatform") version kotlinVersion
        kotlin("android") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion
        kotlin("plugin.compose") version kotlinVersion
        id("com.android.application") version agpVersion
        id("com.android.library") version agpVersion
        id("org.jetbrains.compose") version "1.8.2"
        id("com.google.gms.google-services") version "4.4.2"
        id("com.google.firebase.crashlytics") version "3.0.2"
        id("com.codingfeline.buildkonfig") version "0.13.3"
        id("app.cash.sqldelight") version "2.0.2"
        id("com.mikepenz.aboutlibraries.plugin") version "12.0.0"
    }
}

rootProject.name = "kaiteyo"
include(":app", ":iosApp", ":desktopApp", ":core", ":mediaGenerator")

val localProperties = File(rootDir, "local.properties")
if (localProperties.exists()) {
    localProperties.readLines().forEach { line ->
        if (line.startsWith("sdk.dir=")) {
            val sdkDir = line.removePrefix("sdk.dir=").trim().removeSurrounding("\"")
            System.setProperty("sdk.dir", sdkDir)
            System.setProperty("android.sdk.path", sdkDir)
            System.setProperty("android.sdk.root", sdkDir)
            System.setProperty("android.home", sdkDir)
        }
    }
}
