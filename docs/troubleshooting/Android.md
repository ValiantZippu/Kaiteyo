# Android

Android development uses the `app` module and requires Android Studio, an Android SDK, and a configured emulator or device.

## Android SDK Not Found

**Status:** Open

**Symptoms:** Gradle reports that the Android SDK location is missing or that an SDK platform/build tool cannot be found.

**Cause:** The Android Gradle Plugin cannot locate the SDK through `local.properties`, `ANDROID_HOME`, or `ANDROID_SDK_ROOT`, or the requested SDK package is not installed.

**Diagnosis:**

```powershell
$env:ANDROID_HOME
$env:ANDROID_SDK_ROOT
Test-Path local.properties
.\gradlew.bat :app:assembleDebug --stacktrace
```

**Fix:** Install the SDK and required platform/build tools through Android Studio SDK Manager, then set the SDK environment variable for the shell. Android Studio can generate `local.properties`; do not commit that machine-specific file.

**Verification:** ` .\gradlew.bat :app:assembleDebug` produces a debug APK under `app/build/outputs/apk/`.

**Prevention:** Keep Android Studio and SDK packages aligned with the Android Gradle Plugin and document SDK changes in [Required Software](../setup/RequiredSoftware.md).

**Related Issues:** [Gradle](Gradle.md), [Windows](Windows.md), [Fresh Setup](../setup/FreshSetup.md), [Git Guide](../guides/GIT_GUIDE.md).

Common links: [Troubleshooting index](README.md) | [Dependency problems](Gradle.md) | [Setup guide](../setup/FreshSetup.md) | [Git guide](../guides/GIT_GUIDE.md)

## SDK Location Not Found During Aggregate Build

**Status:** Solved after Android SDK configuration
**First seen:** 2026-08-01
**Last verified:** 2026-08-01

### Symptoms

```text
Could not determine the dependencies of task ':app:compileFdroidDebugJavaWithJavac'.
SDK location not found. Define a valid SDK location with an ANDROID_HOME environment variable
or by setting the sdk.dir path in your project's local properties file.
```

### Cause

The Android Gradle Plugin configures the `app` module even when the requested build includes shared or desktop work. It must locate the Android SDK before it can calculate task dependencies. The SDK was not discoverable through `ANDROID_HOME`, `ANDROID_SDK_ROOT`, or `local.properties`, so dependency calculation stopped before Java compilation.

### Diagnosis

```powershell
$env:ANDROID_HOME
$env:ANDROID_SDK_ROOT
$sdk = "$env:LOCALAPPDATA\Android\Sdk"
Test-Path $sdk
Test-Path local.properties
.\gradlew.bat :app:assembleFdroidDebug --stacktrace
```

### Fix

Install the Android SDK and required packages with Android Studio's SDK Manager. Then, if the standard Windows SDK directory exists, configure the current shell and write the machine-local Gradle path:

```powershell
$sdk = "$env:LOCALAPPDATA\Android\Sdk"
if (-not (Test-Path $sdk)) { throw "Android SDK not found at $sdk. Install it with Android Studio SDK Manager first." }
$env:ANDROID_HOME = $sdk
$env:ANDROID_SDK_ROOT = $sdk
"sdk.dir=$($sdk -replace '\\','/')" | Set-Content -Encoding ASCII local.properties
.\gradlew.bat :app:assembleFdroidDebug
```

`local.properties` is machine-specific and must not be committed.

### Verification

```powershell
.\gradlew.bat build
```

Expected: the build passes the SDK-location phase. A later failure is a separate issue and must be recorded separately.

### Prevention

Install Android SDK packages before running aggregate `build`, keep `local.properties` local, and run `:core:compileKotlinJvm` or `:desktopApp:compileKotlinJvm` when validating only non-Android code.

### Related Issues

- [Java](Java.md)
- [Gradle](Gradle.md)
- [Windows](Windows.md)
- [Fresh Setup](../setup/FreshSetup.md)
- [Git Guide](../guides/GIT_GUIDE.md)
