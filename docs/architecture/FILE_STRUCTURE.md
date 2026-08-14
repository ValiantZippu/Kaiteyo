# Repository File Structure Reference

A map of the repository. Build output (`build/`), Gradle caches, `.git/`, and
machine-local files (`local.properties`) are excluded.

## Root

```
Kaiteyo/
├── README.md                 ← project front page (status, downloads, docs map)
├── CONTRIBUTING.md           ← contribution guide (canonical)
├── SECURITY.md               ← security policy & vulnerability reporting
├── CHANGELOG.md              ← release history (also rendered on the website)
├── LICENSE                   ← GPL-3.0
├── AGENTS.md                 ← AI-assistant operating instructions
├── build.gradle.kts          ← root Gradle build (plugin declarations)
├── settings.gradle.kts       ← module list: :app :iosApp :desktopApp :core :mediaGenerator :kjd
├── gradle.properties         ← Gradle/JVM properties (daemon off, 2GB heap, native flags)
├── gradlew / gradlew.bat     ← Gradle wrapper
├── local.properties          ← machine-specific SDK dir (NOT committed)
│
├── core/                     ← shared KMP module (all platforms)
├── desktopApp/               ← desktop app + desktop suite (JVM)
├── app/                      ← Android entry point (flavors googlePlay / fdroid)
├── iosApp/                   ← iOS entry point (Swift host + Compose)
├── kjd/                      ← KJD data platform (standalone JVM module)
├── mediaGenerator/           ← media asset generation utility
├── installer/                ← branded installer subsystem (scripts/configs, no Gradle)
├── website/                  ← static site (Python build; consumes ../docs)
├── buildSrc/                 ← Gradle logic: AppVersion.kt, AppAssets.kt, prepare tasks
├── gradle/                   ← wrapper + libs.versions.toml version catalog
├── fastlane/                 ← mobile app store metadata (screenshots, changelogs)
├── preview_assets/           ← branding assets (logos, banners)
├── scripts/                  ← dev scripts (e.g. capture-window-shell.sh)
├── .github/                  ← CI workflows (build-all, build-release) + FUNDING
└── docs/                     ← documentation (see docs/README.md)
```

## Module: `core/` (shared, all platforms)

**Dependencies:** Compose MPP, Koin, Ktor, SQLDelight, DataStore, kotlinx.*,
AboutLibraries, Wanakana, reorderable.

```
core/
├── build.gradle.kts          ← KMP targets (jvm/android/ios), SQLDelight config
├── consumer-rules.pro        ← Android ProGuard rules
├── credits/libraries/*.json  ← in-app data/library credits (AboutLibraries)
└── src/
    ├── commonMain/
    │   ├── kotlin/ua/syt0r/kanji/
    │   │   ├── presentation/    ← UI: KaiteyoApp, common/ (theme, ui, resources, nav),
    │   │   │                       screen/main/ (shell + feature screens)
    │   │   ├── core/            ← data layer: app_data, user_data, srs (fsrs),
    │   │   │                       statistics, sync, account, transfer, backup,
    │   │   │                       stroke_evaluator, tts, theme_manager, …
    │   │   └── di/              ← Koin modules (AppModule, …)
    │   ├── sqldelight_app_data/ ← AppDataDatabase schemas (Letters.sq, Vocab.sq)
    │   └── sqldelight_user_data/← UserDataDatabase schema + migrations/ (1.sqm … 14.sqm)
    ├── androidMain/          ← Android actuals (drivers, notifications, file access, TTS)
    ├── jvmMain/              ← JVM actuals (desktop file/backup/transfer/TTS)
    ├── iosMain/              ← iOS actuals (native drivers, ZIP codecs, file pickers)
    └── commonTest/           ← shared tests (FSRS, stroke eval, statistics, transfer)
```

## Module: `desktopApp/` (JVM — window shell + desktop suite)

**Dependencies:** `:core`, `:kjd` (patch apply), Ktor server (netty), JNA, VLCJ,
Compose Desktop.

```
desktopApp/
├── build.gradle.kts          ← Compose desktop app config (jpackage targets, icons)
├── mac_icon.icns / windows_icon.ico
├── linux/                    ← AppImage AppDir, flatpak metainfo, snapcraft config
└── src/
    └── jvmMain/
        ├── composeResources/ ← window icon, aboutlibraries.json
        └── kotlin/ua/syt0r/kanji/
            ├── BuildConfig.kt
            ├── desktopApp/   ← entry points: Main.kt (main), SuiteMain.kt
            │                    (desktopSuiteMain), KaiteyoWindow, OnboardingWizard,
            │                    WindowStateStore, NativeWindowDrag
            └── desktop/      ← THE DESKTOP SUITE (JVM-only)
                ├── appstate/     ← AppState, WorkspacePanels
                ├── designsystem/ ← Ds* components + tokens
                ├── engine/       ← dictionary, media, mining, ocr, browser, review, srs,
                │                   sync, transfer, theming, updates, plugins, shortcuts,
                │                   settings, stats, collections, account, api, cli,
                │                   history, jdata
                ├── ui/           ← views per domain
                ├── model/        ← card/library/search models
                └── data/         ← DemoData
```

## Module: `app/` (Android)

```
app/
├── build.gradle.kts          ← flavors, signing resolution, adjustFlavorTasks()
├── proguard-rules.pro
└── src/
    ├── main/                 ← shared: manifest, KaiteyoApplication, DI, resources
    ├── googlePlay/           ← Firebase analytics/crashlytics, billing, review, sponsor
    └── fdroid/               ← Google-free flavor (FdroidMainActivity, no Firebase)
```

## Module: `iosApp/` (iOS)

```
iosApp/
├── build.gradle.kts
├── KaiteyoApp.xcodeproj      ← Xcode project (scheme: KaiteyoApp)
├── KaiteyoApp/               ← Swift: KaiteyoApp.swift, ContentView.swift,
│                                Info.plist, assets, Swift backup/TTS/wanakana helpers
└── src/iosMain/              ← Kotlin iOS actuals (app host, credits, backup base)
```

## Module: `kjd/` (KJD data platform)

```
kjd/
├── build.gradle.kts          ← JVM application (mainClass io.kaiteyo.kjd.cli.KjdCliKt)
├── README.md
└── src/
    ├── main/kotlin/io/kaiteyo/kjd/
    │   ├── api/          ← public JapaneseDatabase API
    │   ├── cli/          ← kjd CLI
    │   ├── db/           ← SQLite schema, writer, migrator, index rebuild
    │   ├── model/        ← canonical entities
    │   ├── normalize/    ← Japanese-aware normalization
    │   ├── parser/       ← KanjiVG, KANJIDIC, JMdict, JmdictFurigana, Tanos, Leeds, yomichan-jlpt-vocab
    │   ├── patch/        ← database diff/patch (incremental updates)
    │   ├── pipeline/     ← build pipeline + release manifest
    │   ├── resolve/      ← entity resolution
    │   ├── search/       ← FTS search
    │   ├── source/       ← source metadata + licenses
    │   └── validate/     ← validation
    └── test/             ← parsers, schema migration, diff/patch, E2E pipeline
```

## Module: `installer/` (branded installer subsystem)

See `installer/README.md` and `installer/docs/`. Key paths: `common/version.json`
(single source of truth), `windows/` (Inno Setup), `macos/` (DMG + notarize),
`linux/` (AppImage/deb/rpm/flatpak/snap), `scripts/` (stage/verify/bump/feed/asset
generators), `templates/` (release notes + update manifest), `assets/` (brand SVGs).

## Directory: `docs/`

See `docs/README.md` for the full map. Top-level areas: architecture (+ decisions),
data, design, branding, features, user-guide, integrations, platform, releases, security,
legal, testing, api, development, contributing, setup, maintenance, planning, roadmap,
guides, troubleshooting, screenshots.

## What breaks if removed

| Path | Effect |
|---|---|
| `core/` | Everything — all shared logic and UI |
| `desktopApp/` | Desktop build + suite |
| `app/` | Android build |
| `iosApp/` | iOS build |
| `kjd/` | Bundled language database generation + desktop patch updates |
| `installer/` | Release packaging (EXE/DMG/AppImage/…, update feeds) |
| `buildSrc/` | Build configuration (versions, assets) |
| `gradle/libs.versions.toml` | Dependency resolution |
| `settings.gradle.kts` | Module discovery |
