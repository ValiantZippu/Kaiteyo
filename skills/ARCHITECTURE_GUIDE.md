# Architecture Guide

> How the project is organized — folder by folder, module by module.

---

## Top-Level Structure

```
Kaiteyo/
├── core/               Shared Kotlin Multiplatform code
├── desktopApp/         Desktop wrapper + suite
├── app/                Android entry point
├── iosApp/             iOS entry point
├── kjd/                Data platform (database generation)
├── mediaGenerator/     Media asset generator
├── installer/          Installer scripts (not Gradle)
├── website/            Static site (Python)
├── buildSrc/           Gradle build logic
├── brand/              Brand assets pipeline
├── tools/cli/          Developer CLI
├── docs/               Documentation (180+ pages)
├── skills/             AI & contributor skills
├── fastlane/           Mobile metadata & screenshots
└── .github/            CI/CD workflows
```

---

## Module Deep Dive

### `core/` — The Heart

**All shared code lives here.** Kotlin Multiplatform with platform-specific source sets.

```
core/
├── src/
│   ├── commonMain/          Shared across all platforms
│   │   ├── kotlin/ua/syt0r/kanji/
│   │   │   ├── presentation/    UI layer
│   │   │   │   ├── KaiteyoApp.kt           Root composable
│   │   │   │   ├── ViewModel.kt            expect/actual VM factory
│   │   │   │   ├── common/                 Shared UI components
│   │   │   │   │   ├── theme/              Colors, typography, dimensions
│   │   │   │   │   ├── resources/          Strings, icons, brand assets
│   │   │   │   │   ├── nav/                Navigation components
│   │   │   │   │   └── ui/                 Reusable composables
│   │   │   │   └── screen/main/            Main screen + features
│   │   │   │       ├── MainScreen.kt       App shell
│   │   │   │       ├── MainNavigation.kt   Destinations
│   │   │   │       └── screen/<feature>/   Per-feature screens
│   │   │   ├── core/           Data layer
│   │   │   │   ├── app_data/       Dictionary DB (read-only)
│   │   │   │   ├── user_data/      User DB (mutable)
│   │   │   │   ├── srs/           Spaced repetition
│   │   │   │   ├── sync/          Sync engine
│   │   │   │   ├── backup/        Backup/restore
│   │   │   │   ├── account/       User accounts
│   │   │   │   ├── analytics/     Usage analytics
│   │   │   │   ├── theme_manager/ Theme persistence
│   │   │   │   ├── stroke_evaluator/ Writing evaluation
│   │   │   │   └── tts/          Text-to-speech
│   │   │   └── di/               Koin modules
│   │   ├── composeResources/     Compose resources
│   │   └── sqldelight_user_data/ SQLDelight schemas
│   ├── jvmMain/             Desktop-specific code
│   ├── androidMain/         Android-specific code
│   └── iosMain/             iOS-specific code
├── build.gradle.kts         Module build config
└── commonTest/              Shared tests
```

**Key rules:**
- New screens go in `screen/main/screen/<feature>/`
- Follow the 4-file pattern (Contract → ViewModel → Module → UI)
- Register modules in `di/AppModule.kt`
- Add strings to both `EnglishStrings` and `JapaneseStrings`

---

### `desktopApp/` — Desktop Shell + Suite

The desktop app is a **thin wrapper** around `core/` plus a **full feature suite**.

```
desktopApp/
├── src/jvmMain/kotlin/ua/syt0r/kanji/
│   ├── desktopApp/              Wrapper (Main.kt, KaiteyoWindow.kt)
│   │   ├── Main.kt             Entry point, Koin init
│   │   ├── KaiteyoWindow.kt    Borderless window shell
│   │   ├── WindowMessageHandler.kt  Windows API integration
│   │   ├── NativeWindowDrag.kt  Native drag regions
│   │   └── OnboardingWizard.kt  First-run setup
│   └── desktop/                The actual suite
│       ├── appstate/           Central state (AppState)
│       ├── engine/             All desktop engines
│       │   ├── dictionary/     Yomitan-style dictionary
│       │   ├── media/          Video/audio playback
│       │   ├── playback/       Player controls
│       │   ├── mining/         Sentence mining
│       │   ├── learning/       Study features
│       │   ├── statistics/     Stats & analytics
│       │   ├── theming/        Theme management
│       │   ├── settings/       App settings
│       │   ├── activity/       Activity logging
│       │   ├── shortcuts/      Keyboard shortcuts
│       │   ├── sync/           Sync engine
│       │   ├── account/        User accounts
│       │   ├── updates/        Auto-update
│       │   ├── api/            Local HTTP API
│       │   ├── browser/        Web browser
│       │   ├── ocr/            OCR engine
│       │   ├── search/         Search engine
│       │   ├── transfer/       Data transfer
│       │   └── jdata/          Second data platform
│       ├── designsystem/       Desktop-specific design tokens
│       ├── ui/                 All desktop views
│       │   ├── workspace/      Workspace shell & navigation
│       │   ├── DashboardView.kt
│       │   ├── LibraryView.kt
│       │   ├── DictionaryManagerView.kt
│       │   ├── MediaView.kt
│       │   ├── ReviewView.kt
│       │   └── ... (20+ views)
│       └── model/              Desktop data models
└── build.gradle.kts
```

**Key rules:**
- Desktop code is JVM-only — it doesn't exist on Android/iOS
- All views read from `AppState` singleton
- Use `Ds*` design system components
- Follow desktop-specific design tokens

---

### `app/` — Android

```
app/
├── src/
│   ├── googlePlay/    Firebase, billing, review
│   ├── fdroid/        Google-free build
│   └── main/          Shared Android code
├── build.gradle.kts   Flavors, signing, dependencies
└── proguard/          ProGuard rules
```

**Key rules:**
- Never remove `adjustFlavorTasks()` — F-Droid requirement
- Signing keystore: `KEYSTORE_PATH` env → `~/.kaiteyo/keystore.jks` → debug
- Release secrets from env vars only

---

### `kjd/` — Data Platform

Standalone JVM module that generates the bundled language database.

```
kjd/
├── src/main/kotlin/   Generation pipeline
├── sources/           Open datasets (JMDict, KANJIDIC, etc.)
└── build.gradle.kts   Standalone build
```

**Commands:**
```bash
./kjd build --sources-dir sources/
./kjd validate
./kjd search kanji 食
./kjd lookup 食べる
```

---

### `brand/` — Brand Assets

```
brand/
├── source/           Original assets (never edit)
├── processed/        Validated copies
├── generated/        Raster outputs (PNG, ICO, ICNS)
├── manifests/        Asset manifest (assets.json)
└── scripts/          Validation & sync pipeline
```

**Pipeline:** `source/` → validate → `processed/` → render → `generated/` → app resources

---

### `installer/` — Installer Scripts

Not a Gradle module — pure scripts and configs.

```
installer/
├── common/version.json    Version source (must match buildSrc)
├── windows/               Inno Setup configs
├── macos/                 DMG scripts
├── linux/                 AppImage/deb/rpm scripts
└── docs/                  Installer documentation
```

---

### `docs/` — Documentation

180+ pages organized like a documentation site.

```
docs/
├── README.md               Documentation map
├── architecture/           System architecture & ADRs
├── design/                 Design language & system
├── development/            Dev setup & standards
├── features/               Feature specifications
├── planning/               Roadmap & issues
├── platform/               Platform-specific guides
├── product/                Product vision & principles
├── troubleshooting/        Common problems & fixes
├── testing/                Test strategy
├── releases/               Release process
├── data/                   Data sources & attribution
├── game/                   Game mode architecture
├── ai/                     AI agent guides
├── cli/                    CLI documentation
└── ... (many more)
```

---

## Dependency Flow

```
┌─────────────┐
│  desktopApp  │ ─── depends on ──→ ┌──────┐
│  (JVM)       │                     │ core │
└─────────────┘                     │ (KMP)│
                                    └──┬───┘
┌─────────────┐                        │
│   app        │ ─── depends on ──→    │
│  (Android)   │                        │
└─────────────┘                        │
                                    ┌──┴───┐
┌─────────────┐                     │ kjd  │
│  iosApp      │ ─── depends on ──→ │(JVM) │
│  (iOS)       │                     └──────┘
└─────────────┘
```

---

## Data Flow

```
User Action → Screen (Composable) → ViewModel (StateFlow) → UseCase → Repository → Database
     ↑                                                                              │
     └──────────────────────── State update ←───────────────────────────────────────┘
```

---

## DI (Dependency Injection)

Koin modules are organized per-screen:

```kotlin
// In AppModule.kt
val screenModules = listOf(
    homeScreenModule,
    libraryScreenModule,
    reviewScreenModule,
    // ... every feature screen
)
```

Each module provides its ViewModel:
```kotlin
val featureScreenModule = module {
    multiplatformViewModel<FeatureScreenContract.ViewModel> {
        FeatureScreenViewModel(get())
    }
}
```

---

## Database Architecture

Two SQLDelight databases:

| Database | Purpose | Mutability |
|----------|---------|------------|
| `AppDataDatabase` | Dictionary, kanji data | Read-only (bundled asset) |
| `UserDataDatabase` | User progress, cards, settings | Mutable (user data) |

**Never change `.sq` schemas** unless explicitly asked — migrations are versioned.

---

## Build System

```
Gradle
├── settings.gradle.kts        Plugin versions (must match catalog)
├── gradle/libs.versions.toml  Version catalog
├── buildSrc/
│   ├── AppVersion.kt          Version source (single source of truth)
│   └── AppAssets.kt           Asset declarations
├── core/build.gradle.kts      Core module config
├── desktopApp/build.gradle.kt Desktop module config
└── app/build.gradle.kt        Android config (flavors, signing)
```

**Key commands:**
```bash
./gradlew :desktopApp:compileKotlinJvm    # Compile desktop
./gradlew :desktopApp:run                 # Run desktop
./gradlew :core:allTests                  # Run tests
./gradlew :app:assembleDebug              # Android debug
```
