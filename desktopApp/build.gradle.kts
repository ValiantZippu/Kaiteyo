import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    // @Serializable models (SettingsEngine snapshots, KJD patch feed).
    kotlin("plugin.serialization")
    id("org.jetbrains.compose")
    id("com.mikepenz.aboutlibraries.plugin")
}

kotlin {

    jvm()

    jvmToolchain(17)
    compilerOptions {
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1)
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1)
    }

    sourceSets {

        jvmMain {
            dependencies {
                implementation(compose.components.resources)
                implementation(project(":core"))
                // KJD Japanese data platform — incremental patch apply for the
                // bundled language database (DatabasePatcher / DatabasePatch).
                implementation(project(":kjd"))
                implementation(libs.ktor.server.core)
                implementation(libs.ktor.server.netty)
                // Native OS window dragging on Windows (WM_NCLBUTTONDOWN) and
                // Linux (X11/EWMH). Direct coordinates (not catalog accessors)
                // so the desktop module never depends on generated aliases.
                implementation("net.java.dev.jna:jna:5.14.0")
                implementation("net.java.dev.jna:jna-platform:5.14.0")
                // VLC playback backend (embedded video via VLCJ). GPL-3.0 —
                // compatible with Kaiteyo's GPL-3.0 license. The app degrades
                // gracefully when VLC is not installed.
                implementation(libs.vlcj)
                // Sentry (JVM) — error tracking + performance monitoring
                implementation(libs.sentry.core)
                // Tess4J — Tesseract OCR via JNI (optional, graceful fallback)
                implementation(libs.tess4j)
                // Kuromoji — Japanese morphological analyzer (IPAdic)
                implementation(libs.kuromoji.ipadic)
                // Coil 3 — image loading with caching
                implementation(libs.coil.compose)
            }
        }

        jvmTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.turbine)
                implementation(libs.mockk)
            }
        }

    }

}

val mainClassKt = "ua.syt0r.kanji.desktopApp.MainKt"

compose.desktop {
    application {
        mainClass = mainClassKt
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)

            packageName = "Kaiteyo"
            packageVersion = AppVersion.desktopAppVersion
            vendor = "syt0r"

            modules("jdk.unsupported", "java.sql", "java.net.http")

            windows {
                upgradeUuid = "12c852a8-6e21-41a7-bd47-3bec9ff5c5df"
                iconFile.set(File("windows_icon.ico"))
                menu = true
                shortcut = true
            }

            macOS {
                bundleID = "ua.syt0r.kaiteyo"
                iconFile.set(File("mac_icon.icns"))
            }

            linux {
                val linuxIcon = File("src/jvmMain/composeResources/drawable/windowIcon.png")
                iconFile.set(linuxIcon)
            }

        }
    }
}

compose.resources {
    generateResClass = always
    packageOfResClass = "ua.syt0r.kanji.desktopApp"
}

aboutLibraries {
    configPath = "core/credits"
    excludeFields = arrayOf("generated")
}
