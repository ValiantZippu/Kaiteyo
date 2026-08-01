# Kaiteyo — Complete File Structure Reference

This document explains **every file** in the repository. Every file is documented with its purpose, dependencies, and whether developers should modify it.

Total tracked files: ~500+ across 9 root modules.

> **Note:** Build output directories (`build/`), Git internals (`.git/`), Gradle caches (`.gradle/`), and generated files are excluded from this listing as they are not tracked in version control.

## Root Directory

```
Kaiteyo/
├── README.md              ← Project overview, badges, download links
├── CONTRIBUTING.md        ← Contribution guidelines
├── LICENSE                ← GPL-3.0 license
├── build.gradle.kts       ← Root build configuration
├── settings.gradle.kts    ← Module declarations
├── gradle.properties      ← Gradle properties
├── gradlew / gradlew.bat  ← Gradle wrapper scripts
├── .gitignore             ← Git ignore rules
├── keystore.jks           ← Android debug keystore
├── keystore.txt           ← Keystore credentials
│
├── docs/                  ← Project documentation (see below)
├── app/                   ← Android application module
├── core/                  ← Shared Kotlin Multiplatform module
├── desktopApp/            ← Desktop application module
├── iosApp/                ← iOS application wrapper
├── mediaGenerator/        ← Asset generation utility
├── buildSrc/              ← Gradle build logic
├── gradle/                ← Gradle wrapper and version catalog
├── fastlane/              ← Mobile app store metadata
└── preview_assets/        ← Branding and preview assets
```

## Module: `core/` (Shared Library)

**Purpose:** Contains all business logic, data models, UI components, and theme system shared across all platforms.

**Dependencies:** Compose Multiplatform, Koin, SQLDelight, Ktor, DataStore

**Safe to modify:** Yes — this is where most development happens

```
core/
├── build.gradle.kts       ← Module build config
├── consumer-rules.pro     ← Android ProGuard rules
│
├── src/
│   ├── commonMain/        ← Cross-platform code (primary)
│   │   ├── kotlin/ua/syt0r/kanji/
│   │   │   ├── core/          ← Data layer
│   │   │   │   ├── app_data/      ← App database (SQLDelight)
│   │   │   │   ├── user_data/     ← User database (SQLDelight)
│   │   │   │   ├── theme_manager/ ← Theme persistence
│   │   │   │   └── user_data/preferences/ ← DataStore preferences
│   │   │   │
│   │   │   ├── di/           ← Koin dependency injection modules
│   │   │   │
│   │   │   └── presentation/ ← UI layer (Compose Multiplatform)
│   │   │       ├── common/       ← Shared UI components
│   │   │       │   ├── theme/        ← Theme system (Color, Theme, Dimens)
│   │   │       │   ├── resources/    ← Strings, drawables
│   │   │       │   └── ui/           ← Reusable components
│   │   │       │
│   │   │       └── screen/      ← Feature screens
│   │   │           └── main/
│   │   │               └── screen/
│   │   │                   ├── home/     ← Home screen
│   │   │                   ├── settings/ ← Settings/Appearance Studio
│   │   │                   └── credits/  ← Credits screen
│   │   │
│   │   └── sqldelight/    ← SQLDelight schema files
│   │
│   ├── androidMain/       ← Android-specific implementations
│   ├── iosMain/           ← iOS-specific implementations
│   └── jvmMain/           ← Desktop (JVM) specific implementations
│
├── core/                  ← Nested core module (legacy)
│   └── src/
│       ├── androidMain/
│       ├── commonMain/
│       └── jvmMain/
│
└── credits/               ← Library credit data (JSON)
```

### Key Files in `core/`

| File | Purpose | Modify? |
|------|---------|---------|
| `presentation/KanjiDojoApp.kt` | Root composable, theme setup | Yes |
| `presentation/common/theme/Color.kt` | Color definitions, accent schemes | Yes |
| `presentation/common/theme/Theme.kt` | Theme state, composition locals | Yes |
| `presentation/common/theme/Dimens.kt` | Spacing, sizing constants | Yes |
| `presentation/screen/main/screen/settings/AppearanceStudio.kt` | Appearance Studio UI | Yes |
| `di/AppModules.kt` | Koin module definitions | Yes (add new deps) |
| `core/app_data/` | App database (dictionary data) | No (read-only data) |
| `core/user_data/` | User database (study progress) | No (schema changes risky) |

## Module: `desktopApp/` (Desktop Entry Point)

**Purpose:** Thin wrapper that sets up the desktop window and launches the shared UI.

**Dependencies:** core module, Compose Desktop

**Safe to modify:** Yes — desktop-specific UI only

```
desktopApp/
├── build.gradle.kts       ← Desktop build config
├── windows_icon.ico       ← Windows application icon
├── mac_icon.icns          ← macOS application icon
│
├── src/jvmMain/
│   └── kotlin/ua/syt0r/kanji/desktopApp/
│       ├── Main.kt        ← Application entry point, window setup
│       └── TitleBar.kt    ← Custom title bar (may be removed)
│
├── linux/                 ← Linux packaging configs
└── build/                 ← Build outputs
```

### Key Files in `desktopApp/`

| File | Purpose | Modify? |
|------|---------|---------|
| `Main.kt` | Window setup, floating controls, drag region | Yes |
| `TitleBar.kt` | Custom title bar component | Yes (may delete) |
| `build.gradle.kts` | Desktop dependencies, packaging | Rarely |

## Module: `app/` (Android Entry Point)

**Purpose:** Android-specific application setup.

**Dependencies:** core module, Android SDK

**Safe to modify:** Yes — Android-specific code only

```
app/
├── build.gradle.kts       ← Android build config
├── proguard-rules.pro     ← ProGuard rules
│
├── src/
│   ├── main/              ← Shared Android resources
│   │   ├── AndroidManifest.xml
│   │   ├── java/          ← Android activity code
│   │   └── res/           ← Android resources
│   │
│   ├── fdroid/            ← F-Droid build variant
│   └── googlePlay/        ← Google Play build variant
```

## Module: `iosApp/` (iOS Entry Point)

**Purpose:** Swift/SwiftUI project that hosts the Compose Multiplatform UI.

**Dependencies:** core module (via framework)

**Safe to modify:** Yes — iOS-specific code only

```
iosApp/
├── build.gradle.kts       ← iOS build config
├── KanjiDojoApp/          ← Xcode project
│   ├── KanjiDojoApp.swift     ← App entry point
│   ├── ContentView.swift      ← Main SwiftUI view
│   ├── Info.plist             ← App metadata
│   └── Assets.xcassets/       ← iOS assets
└── src/iosMain/           ← Kotlin iOS source
```

## Module: `buildSrc/` (Build Logic)

**Purpose:** Shared Gradle build configuration.

**Dependencies:** Gradle Kotlin DSL

**Safe to modify:** Yes — but affects all modules

```
buildSrc/
├── build.gradle.kts
└── src/main/kotlin/
    ├── AppVersion.kt      ← Version constants
    ├── AppAssets.kt       ← Asset configuration
    └── BuildTools.kt      ← Build task definitions
```

## Module: `mediaGenerator/` (Asset Generation)

**Purpose:** Utility for generating icons, screenshots, and promotional materials.

**Dependencies:** Compose Desktop

**Safe to modify:** Yes — utility only

## Directory: `gradle/`

**Purpose:** Gradle wrapper and dependency management.

```
gradle/
├── wrapper/
│   ├── gradle-wrapper.jar      ← Gradle bootstrap (committed)
│   └── gradle-wrapper.properties ← Gradle version
│
└── libs.versions.toml          ← Version catalog (all dependencies)
```

**Safe to modify:** `libs.versions.toml` — add/update dependencies. Do NOT modify `gradle-wrapper.jar`.

## Directory: `docs/`

**Purpose:** Complete project documentation.

```
docs/
├── 00_START_HERE.md           ← Entry point
├── AI_CONTEXT.md              ← AI assistant context
├── 01_PROJECT_VISION.md       ← Mission and philosophy
├── 02_DESIGN_LANGUAGE.md      ← Design system
├── 03_BRANDING.md             ← Brand guidelines
├── 04_ROADMAP.md              ← Development roadmap
├── 05_FEATURES.md             ← Feature status
├── 06_ARCHITECTURE.md         ← Architecture overview
├── 07_THEME_SYSTEM.md         ← Theme system
├── 08_UI_GUIDELINES.md        ← UI component specs
├── 09_ANIMATION_GUIDELINES.md ← Animation specs
├── 10_TODO.md                 ← Task list
├── 11_TODO.md                 ← (duplicate, legacy)
├── 12_CODING_STANDARDS.md     ← Coding standards
├── 13_ASSETS.md               ← Asset inventory
├── 14_RELEASE_PROCESS.md      ← Release guide
│
├── api/                       ← API documentation
├── branding/                  ← Brand assets
├── decisions/                 ← Architecture Decision Records
├── design/                    ← Design system docs
├── development/               ← Development guides
├── features/                  ← Feature specifications
├── guides/                    ← Developer guides
├── planning/                  ← Project planning
└── Reference/                 ← Reference documentation
```

## Directory: `preview_assets/`

**Purpose:** Branding and preview assets.

| File | Purpose |
|------|---------|
| `kaiteyo_logo.svg` | Primary logo |
| `kaiteyo_icon_simple.svg` | Simplified icon |
| `kaiteyo_banner.svg` | GitHub banner |
| `kaiteyo_wordmark.svg` | Text-only logo |
| `inkscape_icon.svg` | Editable source |

## Directory: `fastlane/`

**Purpose:** Mobile app store metadata (screenshots, descriptions).

**Safe to modify:** Yes — update for new releases.

## What Breaks If Removed

| File/Folder | What Breaks |
|-------------|-------------|
| `core/` | Everything — all logic and UI |
| `desktopApp/` | Desktop build |
| `app/` | Android build |
| `iosApp/` | iOS build |
| `buildSrc/` | Build configuration |
| `gradle/libs.versions.toml` | All dependency resolution |
| `gradle/wrapper/gradle-wrapper.jar` | Gradle build system |
| `settings.gradle.kts` | Module discovery |
| `keystore.jks` | Android release signing |
