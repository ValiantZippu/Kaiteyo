# Kaiteyo — AI Context

This document is written specifically for AI assistants. Read this first before making
any changes. It is grounded in the current repository state (v2.2.1+) — when it
disagrees with the code, the code wins.

## Read order for AI agents

1. `docs/README.md` — the documentation map
2. `docs/engineering/ENGINEERING_STANDARDS.md` — the engineering contract (ADR-0012)
3. `docs/ai/AI_AGENT_GUIDE.md` — binding agent workflow (read/understand/plan/
   implement/validate/document)
4. This file — project context + never-change list
5. `docs/planning/CURRENT_ISSUES.md` — what's broken
6. `docs/planning/TODO.md` + `docs/planning/MASTER_TODO.md` — prioritized tasks
7. `docs/planning/ENGINEERING_AUDIT.md` — audit, dependency map, starting files
8. `docs/architecture/OVERVIEW.md` — module map
9. The subsystem spec under `docs/architecture/` before touching that subsystem

## Project Overview

Kaiteyo is a Compose Multiplatform Japanese language learning app (fork of Kanji Dojo,
fully rebranded). It runs on Desktop (Windows/macOS/Linux — the primary focus),
Android, and iOS.

**Tech stack (pinned):**
- Kotlin 2.1.20, Compose Multiplatform 1.8.2, JDK 17 (`jvmToolchain(17)`; compiler
  `languageVersion`/`apiVersion` pinned to `KOTLIN_2_1`)
- Gradle with version catalog (`gradle/libs.versions.toml`)
- Koin for DI (screen ViewModels via `multiplatformViewModel`)
- SQLDelight 2.x — two databases: `AppDataDatabase` (bundled read-only dictionary) +
  `UserDataDatabase` (mutable user data with versioned migrations)
- Ktor for HTTP; DataStore for preferences; kotlinx.serialization for JSON
- Desktop suite: VLCJ (VLC), mpv JSON-RPC, Java Sound, JNA (native window drag,
  system media keys), Tess4J (OCR), ffmpeg (thumbnails/clips)

## Architecture

### Modules

| Module | Role |
|---|---|
| `core/` | All shared code: UI (Compose MPP), business logic, data layer. `commonMain` / `jvmMain` / `androidMain` / `iosMain` |
| `desktopApp/` | Thin JVM wrapper (`Main.kt`, `KaiteyoWindow.kt`) **plus the whole desktop suite** (`ua.syt0r.kanji.desktop.*`) |
| `app/` | Android entry point. Flavors `googlePlay` (Firebase, billing, review) and `fdroid` (Google-free) |
| `iosApp/` | iOS entry point (Swift + Compose host) |
| `kjd/` | Standalone JVM data platform: ingests open datasets, generates the bundled language database, applies incremental patch updates |
| `mediaGenerator/` | JVM utility (javacv + coil) for media assets |
| `installer/` | Branded installer subsystem (scripts/configs; **not a Gradle module**). Version source: `installer/common/version.json` + `buildSrc/AppVersion.kt` |
| `website/` | Static site, Python build (`build.py`); consumes `../docs` |
| `buildSrc/` | Gradle logic: `AppVersion.kt`, `AppAssets.kt`, asset download/prepare tasks |

### Package layout (do not rename `ua.syt0r.kanji`)

```
core/src/commonMain/kotlin/ua/syt0r/kanji/
├── presentation/           # UI: KaiteyoApp.kt, common/ (components, theme, resources,
│   │                       #   strings, icons, nav/), screen/main/ (MainScreen, NavShell,
│   │                       #   features/, screen/<feature>/ per 4-file pattern)
├── core/                   # Data layer: app_data/, user_data/ (+migrations), srs/, sync/,
│   │                       #   backup/, account/, analytics/, theme_manager/, stroke_evaluator/, tts/
└── di/                     # Koin modules (screenModules list in AppModule.kt)
```

### Desktop suite (`desktopApp/src/jvmMain/kotlin/ua/syt0r/kanji/desktop/`)

JVM-only layered structure — does not exist on Android/iOS:

```
desktop/
├── appstate/      # AppState (central singleton state), WorkspaceView, NavLayout/NavPosition
├── engine/        # dictionary/ media/ playback/ mining/ learning/ statistics/
│                  # theming/ (ThemeManager, ThemeMapper, ThemePresets), settings/
│                  # activity/ (activity log, engagement/AFK), shortcuts/ (ShortcutRegistry),
│                  # sync/ account/ updates/ api/ (LocalApiServer), browser/ ocr/
│                  # search/ transfer/ jdata/ (second data platform — see audit §7-1)
├── designsystem/  # Ds* tokens + components (see docs/design/)
├── ui/            # One view per WorkspaceView: DashboardView, LibraryView, DictionaryManagerView,
│                  # MediaView, ReviewView, ExamView, WritingPracticeView, GrammarPracticeView,
│                  # CollectionsView, TagFlagView, StatsView, MistakesView, ActivityLogView,
│                  # TransferView, SyncView, ShortcutsView, PluginsView, ThemeStudioView,
│                  # SettingsView, AccountView, LearningBrowserView, OcrView, IntegrationsView,
│                  # MiningView + workspace/ (WorkspaceShell, WorkspaceNav, PanelHost, FloatingLauncher)
│                  # + overlays (CommandPaletteOverlay, DictionaryPopup, MiningDialog, CardEditorDialog)
└── model/ data/   # desktop card model, persistence files
```

The suite is self-contained: it talks to the shared core only through the platform
surface (AppTheme, Strings, Koin) and owns its own engines/state. `AppState` is the
singleton everything reads/writes.

## Key files (desktop UI)

| File | Purpose |
|------|---------|
| `desktopApp/.../desktopApp/Main.kt` | Window setup, work-area-corrected bounds, theme-root shell wiring |
| `desktopApp/.../desktopApp/KaiteyoWindow.kt` | Borderless window shell: title bar, window controls, resize zones, system menu, DWM rounding |
| `desktopApp/.../desktopApp/OnboardingWizard.kt` | 8-step first-run wizard (theme/accent/scaling/font/nav/motion) |
| `desktop/.../ui/KaiteyoDesktopSuite.kt` | Suite root: owns AppState, seeds demo data, maps ThemeManager → AppTheme, mounts workspace + wizard + tutorial |
| `desktop/.../ui/workspace/WorkspaceShell.kt` | Adaptive workspace: dock rail / compact tab bar, top bar, tabs, command palette, toasts, mini player, AFK rain, global keys |
| `desktop/.../ui/workspace/WorkspaceNav.kt` | Dock rail/bar, panel host, launcher bubble |
| `desktop/.../designsystem/DsTokens.kt` | Desktop design tokens (spacing/radius/type/motion/elevation/semantic) |
| `desktop/.../engine/theming/ThemeManager.kt` | Desktop theme library + persistence (`~/.kaiteyo/themes/`) |
| `desktop/.../appstate/AppState.kt` | Central state (dictionary, mining, media, review session, cards, settings, activity) |
| `desktop/.../engine/media/MediaEngine.kt` | Media playback state machine (queue, mini player, subtitles, capture) |
| `desktop/.../engine/shortcuts/ShortcutRegistry.kt` | Global shortcut registry (rebindable, persisted) |

Shared core key files: `KaiteyoApp.kt` (+`KaiteyoThemeRoot`), `theme/Color.kt`,
`theme/Theme.kt`, `theme/Typography.kt`, `theme/Dimens.kt`,
`screen/main/MainNavigation.kt` (all `MainDestination`s), `screen/main/MainScreen.kt`
(scaffold + NavShell + palette + dialogs), `common/nav/NavShell.kt`,
`common/resources/string/Strings.kt` (English/Japanese), `common/ui/kanji/*`.

## Screen pattern (the most important convention)

Every feature screen in `core/.../screen/main/screen/<feature>/` follows a 4-file
pattern:

1. `{Feature}ScreenContract.kt` — `interface {Feature}ScreenContract { interface ViewModel ... }`
2. `{Feature}ScreenViewModel.kt` — implements the contract (`StateFlow` state)
3. `{Feature}ScreenModule.kt` — Koin module:
   ```kotlin
   val featureScreenModule = module {
       multiplatformViewModel<FeatureScreenContract.ViewModel> { FeatureScreenViewModel(...) }
   }
   ```
4. `{Feature}Screen.kt` / `{Feature}ScreenUI.kt` — composables; obtain the VM via
   `getMultiplatformViewModel<Contract.ViewModel>()`.

**Register every new screen module in `di/AppModule.kt`** (`screenModules`), or it
won't load. New destinations must be added to `MainNavigation.kt` (`MainDestination`
+ `defaultMainDestinations`) with a kotlinx.serialization config. `multiplatformViewModel`
/ `getMultiplatformViewModel` are `expect` in `presentation/ViewModel.kt` with JVM/
Android/iOS actuals.

## Strings (i18n)

Strings are **interface-based**, not resource files: `Strings` interface with
`EnglishStrings` + `JapaneseStrings`. Lookup via `resolveString { someString }`,
selected by `Locale.current.language` ("ja" → Japanese). **Adding a string requires
editing the interface and both implementations** (the interface enforces it).

## Coding style

- 4-space indent, 120-char lines, explicit imports (no wildcards)
- `val` over `var`; `data class` for state; `sealed class`/`sealed interface` for
  hierarchies; `@Serializable` where persisted
- Composable: PascalCase, `modifier` param last with default `Modifier`
- Modifier order: size → padding → background/clip → clickable → align →
  graphicsLayer → semantics
- State: `StateFlow` in ViewModels; `mutableStateOf`/`derivedStateOf` for local UI;
  `CompositionLocal` for theme propagation; `rememberSaveable` for navigation state
- Animations: spring physics; all durations via `tweenDuration`/`springAnim` so user
  config applies (see `docs/design/ANIMATION_SYSTEM.md`)
- Desktop: views are `@Composable fun XView(state: AppState)` reading `AppState`
  through `LocalAppState`/`rememberAppState()`; use `Ds*` components and tokens —
  never hardcode colors/spacing/radii

## Import rules (Compose Multiplatform 1.8.2)

- `animateColorAsState` → `androidx.compose.animation`
- `animateFloatAsState`/`spring`/`tween`/`snap` → `androidx.compose.animation.core`
- `Window`/`FrameWindowScope`/`WindowState` → `androidx.compose.ui.window`
- `WindowDraggableArea` → `androidx.compose.foundation.window`

## Never change (from AGENTS.md + engineering standard)

- SRS algorithm logic and core learning logic (reviews, study sessions, card
  scheduling) — STANDARDS §341
- SQLDelight `.sq` schemas unless explicitly requested (never change schema without
  an explicit request)
- Package namespace `ua.syt0r.kanji`
- Gradle build configuration unless the build is broken
- `adjustFlavorTasks()` in `app/build.gradle.kts` (F-Droid reproducibility)

## Definition of done

1. `./gradlew :desktopApp:compileKotlinJvm` passes; no new warnings
2. New screens registered in `di/AppModule.kt` (+ `MainNavigation.kt` if a new
   destination)
3. UI follows `docs/design/DESIGN_LANGUAGE.md` / `UI_SYSTEM.md`
4. New strings added to both `EnglishStrings` and `JapaneseStrings`
5. Docs updated if behavior changed; `docs/planning/CURRENT_ISSUES.md` updated if an
   issue was fixed
6. For the desktop suite: `:desktopApp:test` passes when touched

## Build gotchas

- **JDK 17 required.** Plugin versions in `settings.gradle.kts` are literal (keep in
  sync with `gradle/libs.versions.toml`).
- `gradle.properties`: `org.gradle.daemon=false`, 2 GB heap — builds are slow; don't
  launch many Gradle invocations in parallel.
- iOS targets can't build on Windows (`kotlin.native.ignoreDisabledTargets=true`).
- Android builds need `ANDROID_HOME`/`ANDROID_SDK_ROOT` + machine-specific
  `local.properties` (`sdk.dir=...`); never commit `local.properties`.
- App data asset: `AppAssets.kt` declares `kanji-dojo-data-base-v15.sql`
  (AppDataDatabaseVersion=15) + TTS files. The prepare task **deletes any file in
  `core/src/<sourceSet>Main/composeResources/files/` not declared there** — never drop
  files in by hand; register them in `AppAssets.kt`.
- No IDE run configurations ship; run `gradlew :desktopApp:run` from the terminal.
