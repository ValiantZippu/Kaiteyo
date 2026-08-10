# Build Errors

Use this page for failures that stop compilation, packaging, or tests. Start with the [Command Library](../development/COMMANDS.md), then record the issue here.

## GradleWrapperMain ClassNotFoundException

**Status:** Solved
**First seen:** 2026-08-01
**Last verified:** 2026-08-01

### Symptoms

```text
Error: Could not find or load main class
org.gradle.wrapper.GradleWrapperMain
```

### Cause

The Gradle wrapper script starts a JVM and loads `gradle/wrapper/gradle-wrapper.jar`. If that JAR is missing, corrupt, or not readable, the script cannot load the wrapper bootstrap class. The project build files are never reached, so changing a Kotlin or Gradle build script cannot fix this error.

### Diagnosis

```powershell
java -version
javac -version
$env:JAVA_HOME
where.exe java
jar tf gradle/wrapper/gradle-wrapper.jar | Select-String GradleWrapperMain
Test-Path gradle/wrapper/gradle-wrapper.jar
```

The final command must return `True`, and the JAR listing must contain `org/gradle/wrapper/GradleWrapperMain.class`.

### Fix

```powershell
git status --short
git restore gradle/wrapper/gradle-wrapper.jar
.\gradlew.bat --version
```

If the file is not present in Git, clone the repository again or restore the wrapper from the repository's default branch. Do not download an arbitrary wrapper JAR. The wrapper version is controlled by `gradle/wrapper/gradle-wrapper.properties`.

### Verification

```powershell
.\gradlew.bat --version
```

Expected: Gradle prints its version and JVM details without a `ClassNotFoundException`.

### Prevention

Keep the wrapper JAR committed, do not partially copy the repository, and verify `gradle/wrapper/` after resolving merge conflicts.

### Related Issues

- [Gradle](Gradle.md)
- [Fresh Setup](../setup/FreshSetup.md)
- [Git Guide](../guides/GIT_GUIDE.md)

Common links: [Troubleshooting index](README.md) | [Dependency problems](Gradle.md) | [Setup guide](../setup/FreshSetup.md) | [Git guide](../guides/GIT_GUIDE.md)

## Kotlin/Native Targets Cannot Be Built on This Machine

**Status:** Solved for Windows development; iOS builds still require macOS
**First seen:** 2026-08-01
**Last verified:** 2026-08-01

### Symptoms

```text
w: The following Kotlin/Native targets cannot be built on this machine and are disabled:
iosArm64, iosSimulatorArm64, iosX64
```

### Cause

Kotlin/Native configures iOS targets in the shared `core` and `iosApp` projects. Apple platform compilers and SDKs are unavailable on Windows and Linux, so Gradle disables those targets during configuration. This warning does not mean the JVM or Android build failed.

### Diagnosis

```powershell
.\gradlew.bat :core:compileKotlinJvm --info
Get-Content gradle.properties | Select-String kotlin.native.ignoreDisabledTargets
```

### Fix

For Windows/Linux development, keep this project setting:

```properties
kotlin.native.ignoreDisabledTargets=true
```

For an actual iOS build, use a Mac with Xcode installed and build the `iosApp` Xcode project there.

### Verification

```powershell
.\gradlew.bat :core:compileKotlinJvm
```

Expected: the JVM target completes successfully; the warning may remain absent or may remain informational during configuration.

### Prevention

Treat platform availability warnings separately from task failures. Run iOS builds on macOS and do not remove the property just to hide a diagnostic.

### Related Issues

- [iOS](iOS.md)
- [Windows](Windows.md)
- [Gradle](Gradle.md)
- [First Build](../setup/FirstBuild.md)

## Experimental Material 3 API Errors in Deck Screens

**Status:** Solved
**First seen:** 2026-08-01
**Last verified:** 2026-08-01

### Symptoms

```text
This material API is experimental and is likely to change or to be removed in the future.
```

The compiler reported this in `AnkiOpsFull.kt`, `DeckBrowserFull.kt`, `ReviewShortcutsSettings.kt`, and `TagFlagNoteSystems.kt` while running `:core:compileKotlinJvm`.

### Cause

Material 3 dropdown APIs such as `ExposedDropdownMenuBox`, `ExposedDropdownMenu`, and `menuAnchor` are marked experimental. The files opted in only on selected screen functions, while helper composables in the same files used the APIs outside those annotated scopes. Kotlin therefore treated the calls as compilation errors.

### Diagnosis

```powershell
.\gradlew.bat :core:compileKotlinJvm --stacktrace
Select-String -Path core\src\commonMain\kotlin\ua\syt0r\kanji\presentation\screen\main\screen\decks\*.kt -Pattern 'ExposedDropdownMenu|menuAnchor|OptIn'
```

### Fix

Add a file-level opt-in before the package declaration in each compiler-reported file:

```kotlin
@file:OptIn(ExperimentalMaterial3Api::class)

package ua.syt0r.kanji.presentation.screen.main.screen.decks
```

The affected files are `AnkiOpsFull.kt`, `DeckBrowserFull.kt`, `ReviewShortcutsSettings.kt`, and `TagFlagNoteSystems.kt`.

### Verification

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.20.8-hotspot'
.\gradlew.bat :core:compileKotlinJvm
```

Expected: the experimental Material 3 diagnostics no longer appear. If the command reports a Java 25 version parsing error, fix [Java](Java.md) first.

### Prevention

When a file contains multiple composables using one experimental API, prefer a file-level opt-in. Keep the opt-in narrow to the affected file and verify with the module compile task.

### Related Issues

- [Java](Java.md)
- [Gradle](Gradle.md)
- [Desktop](Desktop.md)
- [First Build](../setup/FirstBuild.md)
- [Git Guide](../guides/GIT_GUIDE.md)

## Android SDK Location Not Found During Aggregate Build

See [Android SDK Location Not Found During Aggregate Build](Android.md) for the full issue record.

## Non-Blocking Build Warnings

**Status:** Open

The 2026-08-01 `tasks` run also reported unused `debug`/`release` variables, deprecated AboutLibraries `configPath` and `excludeFields` properties in `app`, `desktopApp`, and `iosApp`, the experimental Kotlin build-tools API in `core`, and deprecated `uiTestJUnit4` in `mediaGenerator`. These warnings do not currently block `tasks`, but should be migrated in a dedicated dependency/plugin maintenance change.

Related: [Dependency Updates](../maintenance/DependencyUpdates.md), [Known Limitations](../maintenance/KnownLimitations.md), [Gradle](Gradle.md).
