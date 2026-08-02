plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    kotlin("plugin.parcelize")
    kotlin("plugin.compose")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("com.mikepenz.aboutlibraries.plugin")
}

adjustFlavorTasks()

kotlin {
    jvmToolchain(17)
}

android {

    namespace = "ua.syt0r.kanji"

    compileSdk = 35
    defaultConfig {
        applicationId = "ua.syt0r.kanji"
        minSdk = 26
        targetSdk = 35
        versionCode = AppVersion.versionCode
        versionName = AppVersion.versionName
    }

    buildTypes {
        val debug = getByName("debug") {
            versionNameSuffix = "-debug"
            applicationIdSuffix = ".dev"
        }

        val release = getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    flavorDimensions += "version"

    productFlavors {
        create("googlePlay") {
            dimension = "version"
        }

        create("fdroid") {
            dimension = "version"
            applicationIdSuffix = ".fdroid"
        }
    }

    buildFeatures {
        compose = true
    }

    // The keystore is kept outside the repository for security.
    // Resolved from (in order): KEYSTORE_PATH env var, the user's ~/.kaiteyo directory,
    // or the repository root (where CI decodes it from the KEYSTORE_BASE64 secret).
    val keystoreFile = run {
        val fromEnv = System.getenv("KEYSTORE_PATH")?.let(::File)?.takeIf { it.exists() }
        val fromUserHome = File(System.getProperty("user.home"), ".kaiteyo/keystore.jks").takeIf { it.exists() }
        fromEnv ?: fromUserHome ?: rootProject.file("keystore.jks")
    }

    val signedBuildSigningConfig = signingConfigs.create("signedBuild") {
        storeFile = keystoreFile
        System.getenv("KEYSTORE_PASS")?.let { storePassword = it }
        System.getenv("SIGN_KEY")?.let { keyAlias = it }
        System.getenv("SIGN_PASS")?.let { keyPassword = it }
    }

    val debugSigningConfig = signingConfigs.getByName("debug")

    buildTypes.forEach {
        it.signingConfig = if (keystoreFile.exists()) {
            signedBuildSigningConfig
        } else {
            debugSigningConfig
        }
    }

    dependenciesInfo {
        // Removes a signing block with encrypted data for reproducible F-Droid builds
        includeInApk = false
    }

}

dependencies {
    implementation(project(":core"))

    "googlePlayImplementation"(platform(libs.firebase.bom))
    "googlePlayImplementation"(libs.firebase.analytics.ktx)
    "googlePlayImplementation"(libs.firebase.crashlytics.ktx)
    "googlePlayImplementation"(libs.billing.ktx)
    "googlePlayImplementation"(libs.review.ktx)
}

aboutLibraries {
    configPath = "core/credits"
    excludeFields = arrayOf("generated")
}

fun adjustFlavorTasks() {
    project.gradle.taskGraph.whenReady {
        allTasks.forEach { task ->

            val isFdroid = task.name.contains("fdroid", ignoreCase = true)

            val isGoogleTask = task.name.contains("GoogleServices", ignoreCase = true) ||
                    task.name.contains("Crashlytics", ignoreCase = true)

            val isArtProfileTask = task.name.contains("ArtProfile", ignoreCase = true)

            if (isFdroid && (isGoogleTask || isArtProfileTask)) {
                println("Disabling f-droid task: ${task.name}")
                task.enabled = false
            }

        }
    }
}
