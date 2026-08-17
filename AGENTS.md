# AGENTS.md — Kaiteyo (書いてよ)

Kaiteyo is a premium cross-platform Japanese language learning app (fork of Kanji Dojo).
Kotlin Multiplatform + Compose Multiplatform 1.8.2, targeting Desktop (Windows/macOS/Linux),
Android, and iOS. Desktop is the primary focus.

**Read first before changing anything:**
- `docs/development/AI_CONTEXT.md` — written explicitly for AI assistants (workflow, "never change" list)
- `docs/development/COMMANDS.md` — command library (build/test/release)
- `docs/planning/CURRENT_ISSUES.md` — living bug tracker
- `docs/README.md` — docs map and project principles

---

## Modules

| Module | Role |
|---|---|
| `core/` | **All shared code**: UI (Compose MPP), business logic, data layer. `commonMain` / `jvmMain` / `androidMain` / `iosMain` |
| `desktopApp/` | Thin JVM wrapper: window setup, Koin init, plus the standalone desktop suite (`ua.syt0r.kanji.desktop.*`) |
| `app/` | Android entry point. Flavors: `googlePlay` (Firebase, billing, review) and `fdroid` (google tasks auto-disabled via `adjustFlavorTasks()`) |
| `iosApp/` | iOS entry point (Swift project + Compose host) |
| `kjd/` | **KJD** data platform — standalone JVM module that ingests open datasets and generates the bundled language database; also provides incremental patch apply for desktop |
| `mediaGenerator/` | JVM utility module (javacv + coil) for generating media assets |
| `installer/` | Branded installer subsystem — **not a Gradle module**; scripts/configs (Inno Setup, DMG, AppImage/deb/rpm, update feeds). Version source: `installer/common/version.json` + `buildSrc/AppVersion.kt` |
| `website/` | Static site, **Python build** (`build.py`) — consumes `../docs`; unrelated to the Kotlin build |
| `buildSrc/` | Gradle logic: `AppVersion.kt`, `AppAssets.kt`, asset download/prepare tasks |

## Essential Commands

Run from repo root. Windows: `.\gradlew.bat ...`; macOS/Linux: `./gradlew ...`.

```bash
# Compile / run desktop (the main daily loop)
gradlew :desktopApp:compileKotlinJvm
gradlew :desktopApp:run

# Android
gradlew :app:assembleDebug                    # debug APK (applicationId ua.syt0r.kanji.dev)
gradlew :app:assembleFdroidRelease            # what CI builds

# Tests
gradlew :core:allTests

# Installers (must run on matching host OS)
gradlew :desktopApp:packageMsi    # Windows
gradlew :desktopApp:packageDmg    # macOS
gradlew :desktopApp:packageDeb    # Linux
gradlew :desktopApp:packageDistributionForCurrentOS   # what CI uses

# Regenerate SQLDelight interfaces after changing .sq schemas
gradlew :core:generateCommonMainAppDataDatabaseInterface
gradlew :core:generateCommonMainUserDataDatabaseInterface
```

### Gotchas in the build setup

- **JDK 17 required** (jvmToolchain(17) everywhere). Kotlin compilerOptions pin
  `languageVersion`/`apiVersion` to `KOTLIN_2_1`.
- **Plugin versions are literal in `settings.gradle.kts`** `pluginManagement` (the version
  catalog is not accessible there). Keep them in sync with `gradle/libs.versions.toml`.
- `gradle.properties` sets `org.gradle.daemon=false` and a conservative 2 GB heap
  (8 GB dev machine). Builds are slow; don't launch many Gradle invocations in parallel.
- **iOS targets cannot build on Windows** — expected; `kotlin.native.ignoreDisabledTargets=true` is set.
- Android builds need `ANDROID_HOME` / `ANDROID_SDK_ROOT` and a machine-specific
  `local.properties` (`sdk.dir=...`). Never commit `local.properties`. See `docs/development/COMMANDS.md` for the setup snippet.
- No IDE run configurations ship in the repo (the stale `.run/` configs were removed). Run
  `gradlew :desktopApp:run` from the terminal; add `-Duser.language=ja -Duser.country=JP`
  for the Japanese UI locale.

## Versioning and App Assets

- **Single version source**: `buildSrc/src/main/kotlin/AppVersion.kt`
  (`versionCode`, `versionName`, `desktopAppVersion` — must be 3 numbers). Bump here only.
- **App data asset**: `AppAssets.kt` declares `kanji-dojo-data-base-v15.sql` (AppDataDatabaseVersion = 15)
  and TTS voice files (opus for Android, wav for desktop/iOS). Build tasks download missing assets
  from GitHub releases on first build — **needs network**.
- `core/src/<sourceSet>Main/composeResources/files/` is managed: the prepare task **deletes any
  file not declared in `AppAssets.kt`**. Never drop files there by hand; register them in `AppAssets.kt`.
- Version catalog: `gradle/libs.versions.toml`.

## Architecture

### Package layout (everything under `ua.syt0r.kanji` — do not rename the namespace)

```
core/src/commonMain/kotlin/ua/syt0r/kanji/
├── presentation/
│   ├── KaiteyoApp.kt           # Root composable, theme init
│   ├── ViewModel.kt           # multiplatformViewModel / getMultiplatformViewModel (expect/actual)
│   ├── common/                # Shared UI components, theme/, resources/ (strings, icons)
│   └── screen/main/           # App shell: MainScreen, navigation, feature screens
├── core/                      # Data layer
│   ├── app_data/              # Read-only dictionary DB (SQLDelight AppDataDatabase)
│   ├── user_data/             # Mutable user DB (SQLDelight UserDataDatabase) + migrations
│   ├── srs/                   # FSRS scheduling, SRS managers (fsrs/ subpackage)
│   ├── sync/, backup/, account/, analytics/, theme_manager/, stroke_evaluator/, tts/, ...
└── di/                        # Koin modules
```

### Screen pattern (the most important convention)

Every feature screen follows a 4-file pattern in `screen/main/screen/<feature>/`:

1. `{Feature}ScreenContract.kt` — `interface {Feature}ScreenContract { interface ViewModel ... }`
2. `{Feature}ScreenViewModel.kt` — implements the contract
3. `{Feature}ScreenModule.kt` — Koin module:
   ```kotlin
   val featureScreenModule = module {
       multiplatformViewModel<FeatureScreenContract.ViewModel> { FeatureScreenViewModel(...) }
   }
   ```
4. `{Feature}Screen.kt` / `{Feature}ScreenUI.kt` — composables, obtain VM via
   `getMultiplatformViewModel<Contract.ViewModel>()`

**Register every new screen module in `di/AppModule.kt`** (`screenModules` list), otherwise it won't load.

- `multiplatformViewModel` / `getMultiplatformViewModel` are `expect` in `presentation/ViewModel.kt`
  with platform `actual`s (JVM uses Koin `viewModel`, etc.).
- State: `StateFlow` in ViewModels, `mutableStateOf`/`derivedStateOf` for local UI state,
  `CompositionLocal` for theme propagation.

### Strings (i18n)

Strings are **interface-based, not resource files**: `Strings` interface with `EnglishStrings`
and `JapaneseStrings` implementations. Lookup: `resolveString { someString }`, selected by
`Locale.current.language` ("ja" → Japanese). **Adding a string requires editing both
implementations** (the interface enforces it) plus the interface itself.

### SQLDelight

Two databases defined in `core/build.gradle.kts`:
- `AppDataDatabase` — bundled read-only asset (dictionary/kanji data)
- `UserDataDatabase` — mutable user data, with versioned migrations in
  `commonMain/sqldelight_user_data/migrations/`

Never change schema without explicit request (see AI_CONTEXT "never change" list).

### Desktop

One entry point in `desktopApp`:
- `desktopApp/Main.kt` `main()` — the real app: `startKoin` with `appModules + desktopAppModule`,
  undecorated `Window` wrapped in `KaiteyoWindow`. The standalone suite entry
  (`desktopSuiteMain()` / `SuiteMain.kt`) was retired — there is exactly one shell.

The former suite now lives in `ua.syt0r.kanji.desktop.*` as a JVM-only feature library
folded into shipped destinations via Koin hosts: `DesktopMediaCentreContent` (Media) and
`DesktopGameCentreContent` (Kaiteyo World), each mounting its views with its own `AppState`.
It has its own layered structure:
`engine/` (dictionary, ocr, mining, media, review/srs, sync, search, transfer, theming, history),
`ui/` (views per domain), `designsystem/` (reusable `Ds*` components), `appstate/`, `model/`.
This code is **JVM-only** — it does not exist on Android/iOS.

### Android specifics

- Signing keystore resolved from: `KEYSTORE_PATH` env → `~/.kaiteyo/keystore.jks` → repo-root
  `keystore.jks`; if absent, falls back to debug signing. Release signing secrets come from env vars.
- `adjustFlavorTasks()` in `app/build.gradle.kts` disables GoogleServices/Crashlytics/ArtProfile
  tasks for the `fdroid` flavor (F-Droid reproducible builds). Don't remove it.
- CI (`build-all.yml`): Ubuntu builds `app:assembleFdroidRelease` + `desktopApp/linux/AppImage/make.sh`; Windows/macOS use `packageDistributionForCurrentOS`. Java 17 Temurin.

## Conventions and Style

Full detail in `docs/development/CODING_STANDARDS.md`. Highlights:

- 4-space indent, 120-char lines, no wildcard imports (explicit only)
- Composables PascalCase, `modifier` param last with default `Modifier`
- Modifier order: size → padding → background/clip → clickable → align → graphicsLayer → semantics
- `data class` for state, `sealed class` for hierarchies, `val` over `var`
- Animations use spring physics; see `docs/design/ANIMATION_SYSTEM.md`
- Compose MPP 1.8.2 import rules (from `docs/development/AI_CONTEXT.md`): `animateColorAsState` →
  `androidx.compose.animation`, `animateFloatAsState`/`spring`/`tween` →
  `androidx.compose.animation.core`, `Window`/`WindowState` → `androidx.compose.ui.window`,
  `WindowDraggableArea` → `androidx.compose.foundation.window`
- Tests: `kotlin.test` in `commonTest` (e.g., `core/src/commonTest/.../FsrsSchedulerTest.kt`,
  JUnit platform). No lint/format plugin configured — match surrounding style manually.

## Never Change (from docs/development/AI_CONTEXT.md)

- SRS algorithm logic and core learning logic (reviews, study sessions, card scheduling)
- SQLDelight `.sq` schemas unless explicitly requested
- Package namespace `ua.syt0r.kanji`
- Gradle build configuration unless the build is broken

## Docs Map (progressive disclosure)

`docs/` is organized by topic; `docs/README.md` has the full tree. Most useful:
`architecture/OVERVIEW.md`, `architecture/decisions/` (ADRs), `data/SOURCES.md` (dataset
licenses/attribution), `development/CODING_STANDARDS.md`, `development/AI_CONTEXT.md`,
`development/COMMANDS.md`, `planning/CURRENT_ISSUES.md` (living bug tracker — update it when
you fix issues), `planning/README.md` (status taxonomy), `troubleshooting/README.md` (record
solved issues), `development/DEVELOPMENT_SETUP.md`, `development/VIBE_CODING_GUIDE.md`,
`testing/README.md` (test strategy), `releases/RELEASE_PROCESS.md` (release workflow).

## Definition of Done

1. `gradlew :desktopApp:compileKotlinJvm` passes; no new warnings
2. New screens registered in `di/AppModule.kt`
3. UI follows `design/DESIGN_LANGUAGE.md` / `design/UI_SYSTEM.md`
4. New strings added to both `EnglishStrings` and `JapaneseStrings`
5. Docs updated if behavior changed; `planning/CURRENT_ISSUES.md` updated if an issue was fixed

---

## Desktop Dictionary Engine

Core to the native reading ecosystem. Organized under `ua.syt0r.kanji.desktop.engine.dictionary`.

### DictionaryService (`DictionaryService.kt`)
- App-facing controller owned by `AppState`.
- Holds installed/enabled dictionaries, query, recent searches, favorites.
- Provides lookup (grouped/flat), search history, favorite toggle.
- `importFile` uses `DictionaryImporter` → parses Yomitan-compatible formats (ZIP, folder, JSON).
- `import` returns `Result<InstalledDictionary>`; logs activity.
- `recentSearches` stored as `history.json` (`recentSearches`), `favorites` as `favorites.json`.

### DictionaryRepository (`DictionaryRepository.kt`)
- Owns installed dictionaries, their entries, and on-disk index.
- Separates settings engine from user data.
- `installedDictionaries()` returns prioritized list; `enabledDictionaries()` filters `enabled = true`.
- Search uses `SearchMode` flags (EXACT, PREFIX, KANA, DEINFLECT) with scoring.
- Index built on demand: per-dictionary `*.json` files in `data/index/`.
- `install()` replaces existing dictionary of same id, writes index.

### DictionaryModels (`DictionaryModels.kt`)
- Serializable POJOs shared across desktop, popup, API.
- `DictionaryEntry`: headword, spellings, readings, senses, kanjiSpellings, frequency, JLPT/grade/radical tags.
- `InstalledDictionary`: id, name, revision, format, source/target lang, priority.
- `DictionaryFormat` enum: Yomitan, JmDict, KanjiDic, Frequency, PitchAccent, Grammar, Name, Custom.
- `DictionaryMatch`: entry + source dictionary + score.
- `MinedDictionaryData`: flattened payload for cards.

### DictionaryImporter (`DictionaryImporter.kt`)
- Parses Yomitan‑compatible archives (ZIP) or term documents (JSON array/object).
- `parseIndexMeta` extracts name/revision/format from `index.json` (field: `format`).
- Supports: index folder, ZIP, lone index.json, single term JSON, and `.json` files (treated as JMdict).
- Returns `DictImportBundle` (result summary + parsed entries).

### DictionaryLookupCard (`dictionary/DictionaryLookupCard.kt`)
- UI for searching across all enabled dictionaries.
- Flat list of results grouped by dictionary name.
- Hovering a result opens `DictionaryPopup` at mouse position.

### DictionaryPopup (`dictionary/DictionaryPopup.kt`)
- **The native popup lookup** – triggered on hover/click on Japanese text in the Reading Environment, Browser, Media, etc.
- Shows a result card with: headword, reading(s), definition(s), example, tags (JLPT, radicals), pronunciation button, and action buttons:
  - Create card (mines to card pool via MiningEngine)
  - Edit card (opens card editor)
  - Add tags / flags
  - Suspend / bookmark
  - Copy (headword, reading, definition)
  - Pronunciation (text‑to‑speech)
  - Open full dictionary (switches to DictionaryManagerView)
- Actions feed `MiningEngine` with `MiningPayload` to create cards directly.

## Media Engine (`MediaEngine.kt`)

Jvm‑only (`ua.syt0r.kanji.desktop.engine.media`):
- `MediaBookmark` / `AudioClip` (serializable).
- `MediaDocument` (video/audio/image/PDF/text/web) with file metadata.
- `AudioPlayer` uses Java Sound API (Clip) with pause/seek.
- Subtitle support (SRT/ASS/SSA/VTT) with synchronization.
- Screenshot capture, frame stepping, A‑B repeat, playback speed, timestamp navigation.
- Integrated with dictionary popup (lookup on subtitle text), mining (subtitle mining), and bookmarks.

## Mining Engine (`MiningEngine.kt`)

Unified mining workflow (`ua.syt0r.kanji.desktop.engine.mining`):
- `MiningPayload` – source-agnostic (headword, reading, definition, sentence, screenshot/audio paths, tags, flags, notes, timestamp, source, sourceDetail, deckId, example).
- `Mine()` creates a `DesktopCard` and adds to AppState card pool.
- `mineFromDictionary()` convenience for dictionary matches.
- Sources: Dictionary, Browser, Video/Subtitle, OCR, Clipboard, Reader, Image, Audio, Integrations API.
- Each source builds a `MiningPayload` and calls `mine()`.
- Duplicate protection via `MinedRecord` (idempotent-ish).

## Reading Environment (Native)

Desktop‑only reading workspace (`ua.syt0r.kanji.desktop.ui.reading` or similar).

- Supports local HTML, EPUB (future), TXT, Markdown.
- **Selectable text** → triggers `DictionaryPopup` hover/click lookup.
- **OCR integration** → screenshot/clipboard/DRAG‑&‑DROP OCR results feed DictionaryPopup + MiningEngine.
- **Bookmarks / Highlights** stored as `ReadingBookmark` (`MediaBookmark`-like, but with page offset).
- **Reading history** (`AppState.readingHistory`).

## OCR Integration (`OcrEngine.kt`)

- `OcrEngine` (Jvm) using Tesseract or similar (future).
- Actions: screenshot OCR, image OCR, clipboard OCR, drag‑&‑drop region capture.
- Results feed `DictionaryPopup` (immediate lookup) and `MiningEngine` (sentence mining).
- Stored OCR images + results in `.kaiteyo/ocr/` for history.

## Lightweight Browser (`BrowserView.kt`)

Desktop browser workspace (`ua.syt0r.kanji.desktop.ui.browser`):
- **Purpose**: dictionary lookup, reading Japanese sites, watching supported media, research.
- Not a full browser — focused on study workflow: text selection → dictionary popup → mining → card.
- Uses platform WebView (future) or lightweight HTML renderer.
- Can load local files + remote URLs with study‑friendly features (subtitle extraction, OCR on images).

## Unified Workflow (Native)

1. **Reading (HTML/Markdown/PDF/Text)** → select Japanese text → **DictionaryPopup** → "Create card" → **MiningEngine** → card appears in Review.
2. **Video/Subtitle** → subtitle text appears → hover → **DictionaryPopup** → "Mine subtitle" → **MiningEngine** → sentence card + continue playback.
3. **Screenshot** → OCR → definition appears → **DictionaryPopup** → "Create card from OCR" → card with image attached.
4. **Clipboard** (Japanese text) → **DictionaryPopup** (clipboard lookup) → immediate card creation.
5. **Browser** → select text → same as Reading.

All share: design system (`Ds*` components), theme tokens, modifier order, shortcuts, dialogs, navigation (WorkspaceNav), and persistent state (AppState).

## Design System (`desktop/designsystem/`)

All native UI uses the same token‑based system:
- `DsCard`, `DsButton`, `DsDialog`, `DsType`, `DsSpacing` (4dp grid), `DsRadius`, `DsBadge`.
- `surfaceColors()` defines light/dark mode tokens.
- Modifier order: size → padding → background/clip → clickable → align → graphicsLayer → semantics.
- Hover states (`hoverable` + `collectIsHoveredAsState`) used consistently.

## Shortcuts (`ShortcutRegistry.kt`)

Global shortcut registry (`ua.syt0r.kanji.desktop.engine.shortcuts`):
- Default: `Ctrl+Shift+D` → DictionaryPopup, `Ctrl+Shift+M` → MiningDialog, `Ctrl+Shift+B` → Browser.
- Customizable via `ShortcutsView` (desktop/ui/shortcuts`).
- Persistent in AppState.

## State (`AppState.kt`)

Centralized state (`ua.syt0r.kanji.desktop.appstate`):
- Holds: dictionary service, mining engine, activity log, card pool, recent files, bookmarks, OCR history, browser history.
- All native modules read/write via `AppState` singleton (main entry point: `desktopApp/Main.kt`).
- Activity logging (`ActivityLog.kt`) with categories (Study, Mining, UI, Error).

## Platform Integrations

- **Koin DI**: modules loaded in `desktopApp/Main.kt` (`appModules + desktopAppModule`).
- **AboutLibraries**: credits / open‑source attribution.
- **Version Catalog**: `gradle/libs.versions.toml`.
- **Gradle**: `desktopApp:run` launches the native stack; `:desktopApp:packageDistributionForCurrentOS` builds installers.
- **Git**: branch‑based workflow (`develop` default). PRs opened to `develop`.

## Testing & Build

- JVM compilation (`:desktopApp:compileKotlinJvm`).
- Tests: `:core:allTests` (shared engine), `:desktopApp:test` (desktop suite),
  `:kjd:test` (data platform). See `docs/testing/README.md`.
- Integration tests for dictionary import, mining, and OCR are WIP; UI tests are not yet
  established.

## Future Plugin Architecture

Stubs in `plugin/` (`PluginRegistry`, `PluginMarketplace`) for: dictionary plugins, OCR backends, subtitle extractors.

---

**Remember**: The native stack lives exclusively in `desktopApp`. It implements everything described in the specification (replacing Yomitan/ ASBPlayer). Changes propagate to the rest of the codebase via `AppState` and Koin injections. Follow the 4‑file screen pattern for any new native screens (e.g., `OcrView`, `ReadingView`).
