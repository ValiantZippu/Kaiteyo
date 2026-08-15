# Kaiteyo (書いてよ) — Development Guide

> Grounded in the current build setup. The canonical command library is
> `COMMANDS.md`; the pinned versions and module list come from `settings.gradle.kts`,
> `gradle/libs.versions.toml`, `buildSrc/AppVersion.kt` and the module build files.

## Prerequisites

- **JDK 17** (Temurin recommended; the build uses `jvmToolchain(17)` everywhere —
  a different major version will fail)
- **Git**
- **Gradle wrapper** (no manual install; `./gradlew` on macOS/Linux,
  `.\gradlew.bat` on Windows)
- **IntelliJ IDEA** (recommended) or **Android Studio** for Android work; VS Code
  works for docs/web/scripts but Kotlin tooling is weaker
- **Android SDK** only if building `:app` (see `DEVELOPMENT_SETUP.md`)

## Modules

From `settings.gradle.kts`:

| Module | What it is |
|--------|------------|
| `:core` | All shared code (UI, domain, data) — the only module with iOS targets |
| `:desktopApp` | JVM desktop entry point + the desktop suite |
| `:app` | Android entry point (`googlePlay` / `fdroid` flavors) |
| `:iosApp` | iOS entry point (Swift project) |
| `:kjd` | Data platform (generates the bundled language DB, patch updates) |
| `:mediaGenerator` | JVM utility for media assets |

There is no `:website` Gradle module — the website has a Python build (`build.py`).

## Quick start

```bash
# Run the desktop application (the daily loop)
./gradlew :desktopApp:run

# Compile only (faster)
./gradlew :desktopApp:compileKotlinJvm

# Tests
./gradlew :core:allTests        # shared engine (commonTest)
./gradlew :desktopApp:test      # desktop suite (jvmTest)
./gradlew :kjd:test             # data platform
```

Add `-Duser.language=ja -Duser.country=JP` to `:desktopApp:run` for the Japanese UI
locale.

## Project setup

### IntelliJ IDEA

1. Open the repository root; IntelliJ detects the Gradle project.
2. Wait for indexing + dependency resolution (first import is slow).
3. Use the Gradle tool window or terminal to run `:desktopApp:run`. No run
   configurations ship in the repo (the stale `.run/` configs were removed).

### VS Code

1. Install the Kotlin + Gradle for Java extensions.
2. Open the root; use the Gradle panel to run tasks.
3. IntelliJ is preferred for Kotlin; VS Code is fine for docs, markdown, scripts.

## Building

### Desktop (JVM)

```bash
./gradlew :desktopApp:compileKotlinJvm     # compile check
./gradlew :desktopApp:run                  # run
./gradlew :desktopApp:packageDistributionForCurrentOS   # CI packaging
./gradlew :desktopApp:packageMsi           # Windows (run on Windows)
./gradlew :desktopApp:packageDmg           # macOS (run on macOS)
./gradlew :desktopApp:packageDeb           # Linux (run on Linux)
```

### Android

```bash
./gradlew :app:assembleDebug               # debug APK (ua.syt0r.kanji.dev)
./gradlew :app:assembleFdroidRelease       # what CI builds (F-Droid flavor)
./gradlew :app:assembleGooglePlayRelease   # Play flavor (needs secrets for signing)
```

Android builds require `ANDROID_HOME`/`ANDROID_SDK_ROOT` and a machine-specific
`local.properties` with `sdk.dir=...` (never commit it).

### iOS

```bash
./gradlew :core:linkDebugFrameworkIosArm64   # build the shared framework
open iosApp/KaiteyoApp.xcodeproj             # then build/run from Xcode
```

iOS targets cannot build on Windows (expected).

## Common commands

```bash
./gradlew clean
./gradlew :core:allTests
./gradlew :desktopApp:test
./gradlew :kjd:test
./gradlew :core:generateCommonMainAppDataDatabaseInterface    # after changing .sq
./gradlew :core:generateCommonMainUserDataDatabaseInterface   # after changing .sq
./gradlew :core:compileKotlinJvm
./gradlew :core:dependencies
./gradlew tasks --all
```

SQLDelight interfaces are generated from `.sq` schemas — regenerate after any schema
change, then compile.

## Debugging

### Desktop

- Attach the IntelliJ debugger to the JVM process.
- Compose debug helpers: `-Dcompose.debug.layout=true` (layout bounds), `-Dcompose.ui.graphics.forceSoftwareRendering=true` on quirky GPUs.
- Window shell diagnostics live in `desktopApp/.../KaiteyoWindow.kt`; media backend
  probing is exposed in Media → Settings.
- The `--capture-state=` dev flag pre-opens window states for screenshot capture
  (`scripts/capture-window-shell.sh`).

### Android

- Android Studio debugger + Compose layout inspector; `adb logcat` for logs.

## Branch strategy

```
main          — production-ready, tagged releases
└── develop   — integration branch (default)
    ├── feature/*   — new features (feature/floating-sidebar)
    ├── fix/*       — bug fixes (fix/window-drag-region)
    ├── docs/*      — documentation (docs/architecture-guide)
    └── release/*   — release preparation
```

See `GITHUB_WORKFLOW.md` for the full flow (squash merges, tags trigger CI release
builds).

## Code style

- Kotlin conventions: 4-space indent, 120-char max line, explicit imports
- `val` over `var`; `data class` for state; `sealed class` for hierarchies
- Screen pattern: 4-file (Contract / ViewModel / Module / Screen) — see
  `AI_CONTEXT.md`
- Compose: small composables, `remember`/`derivedStateOf`, `@Stable` state holders,
  modifier order per `CODING_STANDARDS.md`
- Desktop suite: `Ds*` components + tokens only; view composables read `AppState`

## Compose best practices

1. Keep composables small — one responsibility per function
2. No side effects in composition — use `LaunchedEffect`/`DisposableEffect`
3. `remember` expensive computations; `derivedStateOf` derived values
4. `key()`/stable keys in lazy lists
5. `graphicsLayer` for transform animations (never animate layout properties)
6. Honor `reducedMotion` and the animation config (see `docs/design/ANIMATION_SYSTEM.md`)

## Testing

- **Locations**: `core/src/commonTest/` (shared), `desktopApp/src/jvmTest/` (suite),
  `kjd/src/test/` (data platform)
- **Framework**: kotlin.test on JUnit Platform
- **Commands**: `:core:allTests`, `:desktopApp:test`, `:kjd:test`
- Pure domain logic is unit-tested (FSRS scheduler, statistics calculators, deinflect,
  media selection helpers, media-key mapping). No UI tests yet — see
  `docs/testing/README.md` for strategy and gaps.

## Performance checklist

Before submitting:

- [ ] No unnecessary recomposition (verify with Compose inspector)
- [ ] Animations at 60 FPS; use `graphicsLayer`, respect reduced motion
- [ ] No leaks (retained references in `remember`d state)
- [ ] Lazy lists for unbounded content (`DsVirtualList`/`LazyColumn`)
- [ ] No main-thread blocking (DB/network on dispatchers)
- [ ] No full-history loads into Compose (use the controllers/aggregates)

## Related

- `COMMANDS.md` — full command library
- `DEVELOPMENT_SETUP.md` — from zero to running
- `VIBE_CODING_GUIDE.md` — AI-assisted workflow
- `docs/architecture/OVERVIEW.md` — architecture map
- `docs/planning/ENGINEERING_AUDIT.md` — audit, risks, first milestone
