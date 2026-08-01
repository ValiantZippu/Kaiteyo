# Desktop

The desktop target is the `desktopApp` JVM module and packages installers for Windows, macOS, and Linux.

## Desktop Run Task Does Not Start

**Status:** Open

**Symptoms:** `:desktopApp:run` fails during configuration, reports a duplicate `jvmRun` task, or exits before opening a window.

**Cause:** The Compose/Kotlin desktop plugins create JVM run tasks during configuration. A conflicting custom task or an incompatible runtime can prevent the expected task from being created or launched.

**Diagnosis:**

```powershell
.\gradlew.bat :desktopApp:tasks --all | Select-String "run|jvmRun"
.\gradlew.bat :desktopApp:run --stacktrace
```

**Fix:** Resolve duplicate task registration in `desktopApp/build.gradle.kts`, use JDK 17, and run from the repository root. On Linux, make `gradlew` executable before using it.

**Verification:** ` .\gradlew.bat :desktopApp:run` opens Kaiteyo.

**Prevention:** Run the desktop compile after build-script changes and package only on the target operating system.

**Related Issues:** [Gradle](Gradle.md), [Windows](Windows.md), [Linux](Linux.md), [macOS](macOS.md), [First Build](../setup/FirstBuild.md).

Common links: [Troubleshooting index](README.md) | [Dependency problems](Gradle.md) | [Setup guide](../setup/FreshSetup.md) | [Git guide](../guides/GIT_GUIDE.md)
