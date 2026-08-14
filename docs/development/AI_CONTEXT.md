# Kaiteyo — AI Context

This document is written specifically for AI assistants. Read this first before making any changes.

## Project Overview

Kaiteyo is a Compose Multiplatform Japanese language learning app. It runs on Desktop (Windows/macOS/Linux), Android, and iOS. The desktop experience is the primary focus.

**Tech Stack:**
- Kotlin 2.1.20, Compose Multiplatform 1.8.2
- Gradle with version catalog (`gradle/libs.versions.toml`), JDK 17
- Koin for dependency injection
- SQLDelight for local database (two databases: AppData + UserData)
- Ktor for HTTP
- DataStore for preferences

## Architecture

```
desktopApp/  → Thin JVM wrapper, window setup, Main.kt + the desktop suite
core/        → All shared code (UI, data, business logic)
  commonMain/  → Cross-platform code
  jvmMain/     → Desktop-specific implementations
  androidMain/ → Android-specific implementations
  iosMain/     → iOS-specific implementations
app/         → Android entry point
iosApp/      → iOS entry point (Swift)
kjd/         → Standalone data platform: generates the bundled language database
installer/   → Branded installer subsystem (scripts/configs, not a Gradle module)
```

The **desktop suite** lives in `desktopApp/src/jvmMain/kotlin/ua/syt0r/kanji/desktop/`
(JVM-only): `engine/`, `designsystem/` (Ds*), `ui/`, `appstate/`, `model/`, `data/`.
It is self-contained and does not follow the core screen pattern.

## Key Files for Desktop UI

| File | Purpose |
|------|---------|
| `desktopApp/src/jvmMain/.../Main.kt` | Window setup, floating controls, drag region |
| `core/src/commonMain/.../theme/Color.kt` | Color schemes, gradients, accent definitions |
| `core/src/commonMain/.../theme/Theme.kt` | Theme state, composition locals, app theme |
| `core/src/commonMain/.../theme/Dimens.kt` | Spacing, sizing constants |
| `core/src/commonMain/.../KaiteyoApp.kt` | Root composable, theme initialization |
| `core/src/commonMain/.../settings/AppearanceStudio.kt` | Appearance Studio screen |
| `core/src/commonMain/.../settings/items/AppearanceSettingItem.kt` | Old settings item (to be replaced) |

## Coding Style

- 4-space indentation, 120 char line limit
- Explicit imports (no wildcard)
- `val` over `var`, `data class` for state, `sealed class` for hierarchies
- Composable functions: PascalCase, `modifier` param last with default `Modifier`
- Modifier order: size → padding → background/clip → clickable → align → graphicsLayer

## Current Roadmap

**v2.2.1 (Current):** premium installer, onboarding, auto-update architecture, native
window shell, unified stats, rebranding completion
**v2.3 (Next):** Anki interoperability & persistent data (largely implemented — see
`../planning/COMPLETED.md`)
Then: desktop polish (animation/performance), OCR hardening, update rollout, mobile sync.

See `../roadmap/ROADMAP.md` for the living roadmap.

## Current Issues (Must Fix Before New Features)

The operational tracker is `../planning/CURRENT_ISSUES.md` (living document). Current
focus areas: animation stutter (60 FPS target), resize glitches, hover animation
inconsistency, spacing/radius consistency, and "code-complete but unverified on
platform" items (iOS, Windows runtime checks).

## Things That Must NEVER Be Changed

- SRS (spaced repetition) algorithm logic
- Database schema (SQLDelight `.sq` files) unless explicitly requested
- Package namespaces (`ua.syt0r.kanji`)
- Core learning logic (reviews, study sessions, card scheduling)
- Gradle build configuration unless build is broken

## Things That ARE Safe to Change

- Desktop UI components and layout
- Theme system (colors, gradients, animations)
- Window chrome and behavior
- Sidebar implementation
- Settings/Appearance screens
- Documentation

## Import Rules

In Compose Multiplatform 1.8.2:
- `animateColorAsState` → `androidx.compose.animation`
- `animateFloatAsState` → `androidx.compose.animation.core`
- `spring`, `tween` → `androidx.compose.animation.core`
- `Window`, `FrameWindowScope`, `WindowState` → `androidx.compose.ui.window`
- `WindowDraggableArea` → `androidx.compose.foundation.window`

## Definition of Done

A task is complete when:
1. Code compiles without errors (`./gradlew :desktopApp:compileKotlinJvm`)
2. No new warnings introduced
3. UI changes follow the design language in `../design/DESIGN_LANGUAGE.md`
4. Animations use spring physics where appropriate
5. Documentation is updated if behavior changed
6. `../planning/CURRENT_ISSUES.md` is updated

## Workflow for AI

1. Read this file and `../README.md`
2. Check `../planning/CURRENT_ISSUES.md` for known issues
3. Check `../planning/TODO.md` for prioritized tasks
4. Read relevant source files before making changes
5. Make changes one file at a time
6. Verify compilation after each change
7. Update documentation if needed
8. Update `../planning/CURRENT_ISSUES.md` if issues were fixed
