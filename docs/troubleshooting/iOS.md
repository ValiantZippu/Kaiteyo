# iOS

The shared Kotlin code includes iOS targets, while `iosApp` is built with Xcode on macOS.

## iOS Targets Disabled on Windows or Linux

**Status:** Solved for non-Apple development

**Symptoms:**

```text
iosArm64, iosSimulatorArm64, iosX64 are disabled
```

**Cause:** Kotlin/Native needs Apple's SDK and toolchain for iOS targets. Those SDKs are not available on Windows or Linux.

**Diagnosis:**

```powershell
.\gradlew.bat :core:compileKotlinJvm
Get-Content gradle.properties | Select-String kotlin.native.ignoreDisabledTargets
```

**Fix:** Use the JVM/Android targets on Windows or Linux. Use a Mac with Xcode for iOS compilation and simulator/device runs. Keep `kotlin.native.ignoreDisabledTargets=true` for non-Apple machines.

**Verification:** The JVM compile succeeds on Windows/Linux; an Xcode build succeeds on macOS.

**Prevention:** Label CI jobs by host platform and do not treat disabled-target warnings as cross-platform build failures.

**Related Issues:** [Build Errors](BuildErrors.md), [macOS](macOS.md), [Fresh Setup](../setup/FreshSetup.md), [Git Guide](../guides/GIT_GUIDE.md).

Common links: [Troubleshooting index](README.md) | [Dependency problems](Gradle.md) | [Setup guide](../setup/FreshSetup.md) | [Git guide](../guides/GIT_GUIDE.md)
