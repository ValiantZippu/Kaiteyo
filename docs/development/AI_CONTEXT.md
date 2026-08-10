# Kaiteyo — AI Context

This document is written specifically for AI assistants. Read this first before making any changes.

## Project Overview

Kaiteyo is a Compose Multiplatform Japanese language learning app. It runs on Desktop (Windows/macOS/Linux), Android, and iOS. The desktop experience is the primary focus.

**Tech Stack:**
- Kotlin 2.0.21, Compose Multiplatform 1.8.2
- Gradle with version catalog (`gradle/libs.versions.toml`)
- Koin for dependency injection
- SQLDelight for local database
- Ktor for HTTP
- DataStore for preferences

## Architecture

```
desktopApp/  → Thin JVM wrapper, window setup, Main.kt
core/        → All shared code (UI, data, business logic)
  commonMain/  → Cross-platform code
  jvmMain/     → Desktop-specific implementations
  androidMain/ → Android-specific implementations
  iosMain/     → iOS-specific implementations
app/         → Android entry point
iosApp/      → iOS entry point (Swift)
```

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

**v1.1 (Current):** Window experience, theme system, branding
**v1.2 (Next):** Floating sidebar, Appearance Studio, animation system
**v1.3:** Theme Studio, Motion Studio, Layout Studio
**v2.0:** Dashboard redesign, learning analytics

## Current Issues (Must Fix Before New Features)

1. **Window drag region** — Entire app is draggable, making UI unusable. Only top 44dp should drag.
2. **Animation performance** — Stuttering, inconsistent hover animations
3. **Design quality** — Inconsistent spacing, poor alignment, no visual hierarchy
4. **Sidebar** — Feels attached, should be floating island
5. **Settings** — Appearance options are disorganized

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
