# Version History and Build History

This is the dated record of releases and solved development issues. Append entries; do not rewrite historical results.

## 2026-08-01

**Issue:** GradleWrapperMain ClassNotFoundException

**Root Cause:** The wrapper bootstrap JAR was missing, corrupt, or unreadable, so the wrapper JVM could not load `GradleWrapperMain`.

**Fix:** Restore the tracked wrapper JAR and verify with `gradlew.bat --version`.

**Verified:** Yes

## 2026-08-01

**Issue:** Kotlin/Native iOS targets disabled on non-Apple host

**Root Cause:** iOS SDK/toolchains are unavailable on Windows/Linux during shared-project configuration.

**Fix:** Keep `kotlin.native.ignoreDisabledTargets=true` for non-Apple development and run iOS builds on macOS.

**Verified:** Yes for JVM compilation; iOS requires macOS.

## 2026-08-01

**Issue:** Aggregate build could not find Android SDK

**Root Cause:** Android Gradle Plugin could not find `ANDROID_HOME`, `ANDROID_SDK_ROOT`, or `local.properties` while configuring `:app:compileFdroidDebugJavaWithJavac`.

**Fix:** Install/configure the Android SDK and create machine-local `local.properties`; use a module-specific JVM compile when Android is not needed.

**Verified:** Reproduction captured; fix requires the machine's Android SDK installation.

## 2026-08-01

**Issue:** Experimental Material 3 API compilation errors

**Root Cause:** Helper composables used experimental dropdown APIs outside existing function-level opt-in scopes.

**Fix:** Added file-level `ExperimentalMaterial3Api` opt-ins to the four compiler-reported deck files.

**Verified:** Source fix applied; final compile verification requires JDK 17 because the shell initially launched Gradle on JDK 25.

## 2026-08-01

**Issue:** Kotlin compiler rejected Java version `25.0.4`

**Root Cause:** `JAVA_HOME` pointed to JDK 25 while `PATH` pointed to JDK 17; Gradle used `JAVA_HOME`.

**Fix:** Align `JAVA_HOME`, `PATH`, and Gradle with JDK 17.

**Verified:** Diagnosis confirmed; compile should be rerun after the environment is aligned.

## Append Template

```markdown
## YYYY-MM-DD

**Issue:** <title>

**Root Cause:** <why>

**Fix:** <exact fix>

**Verified:** Yes / No
```

Related: [Troubleshooting](../troubleshooting/README.md), [Known Limitations](KnownLimitations.md), [Git Guide](../guides/GIT_GUIDE.md).
