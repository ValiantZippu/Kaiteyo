# Kaiteyo (書いてよ) — Development Guide

## Prerequisites

- **Java 17+** (JDK 17 recommended)
- **Git**
- **Gradle** (wrapper included, no manual install needed)
- **IntelliJ IDEA** (recommended) or VS Code with Kotlin plugin
- **Android Studio** (for Android builds)

## Quick Start

```bash
# Clone the repository
git clone https://github.com/your-org/kaiteyo.git
cd kaiteyo

# Run the desktop application
./gradlew :desktopApp:run

# Run Android application
./gradlew :app:installDebug

# Run tests
./gradlew :core:test
```

## Project Setup

### IntelliJ IDEA
1. Open the project root directory
2. IntelliJ will automatically detect the Gradle project
3. Wait for indexing and dependency resolution to complete
4. Create a run configuration for `:desktopApp:run`

### VS Code
1. Install the Kotlin extension
2. Install the Gradle extension
3. Open the project root directory
4. Use the Gradle panel to run tasks

## Building

### Desktop (JVM)
```bash
# Compile only
./gradlew :desktopApp:compileKotlinJvm

# Run
./gradlew :desktopApp:run

# Build distribution
./gradlew :desktopApp:packageDistributionForCurrentOS

# Build specific format
./gradlew :desktopApp:packageMsi
./gradlew :desktopApp:packageDmg
./gradlew :desktopApp:packageDeb
```

### Android
```bash
# Debug build
./gradlew :app:assembleDebug

# Release build
./gradlew :app:assembleRelease

# Install on connected device
./gradlew :app:installDebug
```

### iOS
```bash
# Build framework
./gradlew :core:linkDebugFrameworkIosArm64

# Open Xcode project
open iosApp/KaiteyoApp.xcodeproj
```

## Common Commands

```bash
# Clean all builds
./gradlew clean

# Run all tests
./gradlew test

# Run specific module tests
./gradlew :core:test

# Check for dependency updates
./gradlew dependencyUpdates

# Generate dependency report
./gradlew :core:dependencies
```

## Debugging

### Desktop
- Use `println` or `logger` statements
- Attach IntelliJ debugger to the JVM process
- Enable Compose debug layout bounds: `-Dcompose.debug.layout=true`

### Android
- Use Android Studio's built-in debugger
- Enable Compose layout inspector
- Use `Log.d` for logging

## Branch Strategy

```
main           — Production-ready code
├── develop    — Integration branch
├── feature/*  — New features (feature/sidebar-floating)
├── fix/*      — Bug fixes (fix/window-drag-region)
└── docs/*     — Documentation (docs/architecture-guide)
```

## Code Style

- Follow Kotlin coding conventions
- Use 4-space indentation
- Maximum line length: 120 characters
- Use explicit imports (no wildcard imports)
- Use `val` over `var` where possible
- Use `data class` for state holders
- Use `sealed class` for sealed hierarchies

## Compose Best Practices

1. **Keep composables small** — One component per file where possible
2. **Use `@Composable` annotations** — Every composable function must be annotated
3. **Avoid side effects in composition** — Use `LaunchedEffect`, `DisposableEffect`
4. **Use `remember`** — Cache expensive computations
5. **Use `derivedStateOf`** — Derive state from other state
6. **Use `Modifier`** — Chain modifiers for layout, styling, interaction
7. **Use `@Stable`** — Mark stable state holders for performance
8. **Avoid recomposition** — Use `key()` and `remember` strategically

## Testing

### Unit Tests
- Location: `core/src/commonTest/`
- Framework: Kotlin Test
- Run: `./gradlew :core:test`

### UI Tests (Future)
- Framework: Compose UI Test
- Location: `core/src/commonTest/`
- Run: `./gradlew :core:check`

## Performance Checklist

Before submitting code:
- [ ] No unnecessary recompositions
- [ ] Animations run at 60 FPS
- [ ] No memory leaks (check for retained references)
- [ ] Lazy loading for lists
- [ ] Image caching where applicable
- [ ] No main thread blocking operations
