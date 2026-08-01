# Kaiteyo (書いてよ) — Architecture

## Project Structure

```
Kaiteyo/
├── app/                    # Android application module
│   ├── src/
│   │   ├── fdroid/         # F-Droid build variant
│   │   ├── googlePlay/     # Google Play build variant
│   │   └── main/           # Shared Android resources
│   └── build.gradle.kts
│
├── core/                   # Shared Kotlin Multiplatform module
│   ├── src/
│   │   ├── commonMain/     # Cross-platform code
│   │   ├── androidMain/    # Android-specific implementations
│   │   ├── iosMain/        # iOS-specific implementations
│   │   └── jvmMain/        # Desktop (JVM) specific implementations
│   └── build.gradle.kts
│
├── desktopApp/             # Desktop application module
│   ├── src/
│   │   └── jvmMain/        # Desktop-only code
│   └── build.gradle.kts
│
├── iosApp/                 # iOS application wrapper
│   ├── KanjiDojoApp/       # Swift/SwiftUI entry point
│   └── build.gradle.kts
│
├── mediaGenerator/         # Asset generation utility
│   └── build.gradle.kts
│
├── buildSrc/               # Gradle build logic
│   └── src/main/kotlin/
│
├── gradle/
│   ├── libs.versions.toml  # Version catalog
│   └── wrapper/
│
├── docs/                   # Project documentation
│
└── settings.gradle.kts
```

## Module Responsibilities

### `core` (Shared Library)
The heart of the application. Contains all business logic, data models, UI components, and theme system shared across platforms.

**Key packages:**
- `ua.syt0r.kanji.core` — Data layer (database, preferences, network)
- `ua.syt0r.kanji.di` — Dependency injection modules
- `ua.syt0r.kanji.presentation` — UI layer (Compose Multiplatform)
  - `common.theme` — Theme system, colors, typography, dimens
  - `common.resources` — String resources, drawable resources
  - `screen.*` — Feature screens organized by domain

### `desktopApp` (Desktop Entry Point)
Thin wrapper that sets up the desktop window, configures the JVM environment, and launches the shared UI from `core`.

**Key files:**
- `Main.kt` — Application entry point, window setup, Koin initialization
- `Main.kt` contains: `KaiteyoWindow`, floating controls, drag region

### `app` (Android Entry Point)
Android-specific entry point with Activity, manifest, and platform configurations.

### `iosApp` (iOS Entry Point)
Swift/SwiftUI project that hosts the Compose Multiplatform UI via UIKit integration.

## UI Architecture

### State Management
- **Koin** for dependency injection
- **StateFlow** in ViewModels for reactive state
- **Compose State** (`mutableStateOf`, `derivedStateOf`) for local UI state
- **CompositionLocal** for theme and configuration propagation

### Theme Architecture
```
ThemeManager (interface)
  └── JvmGetCreditLibrariesUseCase (desktop)
  └── Android/Koin implementations

KaiteyoThemeState (mutable state holder)
  ├── baseMode (Light/Dark/Oled)
  ├── accentScheme (KaiteyoAccentScheme)
  ├── glowConfig (GlowConfig)
  ├── radiusConfig (RadiusConfig)
  ├── animationConfig (AnimationConfig)
  └── densityConfig (DensityConfig)

CompositionLocals
  ├── LocalKaiteyoThemeState
  ├── LocalKaiteyoAccent
  ├── LocalSurfaceColors
  └── LocalKaiteyoAccentList
```

### Screen Structure
Each screen follows a consistent pattern:
```
screen/{feature}/
  ├── {Feature}Screen.kt        # Screen composable
  ├── {Feature}Contract.kt      # State + Events contracts
  └── {Feature}ViewModel.kt     # ViewModel (if applicable)
```

## Navigation

Navigation uses a simple stack-based approach:
- `MainNavigationState` manages the navigation stack
- Each screen registers with the navigator
- Deep linking handled via `DeepLinkHandler`
- Desktop uses a single-window approach with screen switching

## Dependency Injection

Dependencies are provided via Koin modules:
- `appModules` in `core` — All shared dependencies
- `desktopAppModule` in `desktopApp` — Desktop-specific overrides
- Platform modules in `androidMain` and `iosMain`

Module loading in `main()`:
```kotlin
val koinModuleList = appModules.plus(desktopAppModule)
startKoin { loadKoinModules(koinModuleList) }
```

## Data Flow

```
UI Component
  └── ViewModel / StateHolder
       └── Repository
            └── Data Source (Database, Preferences, Network)
```

- UI observes state from ViewModels
- ViewModels call repositories for data operations
- Repositories coordinate between local (SQLDelight) and remote (Ktor) sources
- Preferences stored via DataStore

## Key Design Decisions

1. **Compose Multiplatform** — Single UI codebase for Android, Desktop, iOS
2. **Koin** — Lightweight DI without code generation (unlike Dagger/Hilt)
3. **SQLDelight** — Type-safe SQL for local database
4. **DataStore** — Preferences storage (replacement for SharedPreferences)
5. **Ktor** — HTTP client for network requests
6. **AboutLibraries** — Open source license display
7. **Compose Resources** — Cross-platform resource management
