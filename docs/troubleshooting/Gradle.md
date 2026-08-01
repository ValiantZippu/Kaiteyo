# Gradle

Kaiteyo uses the committed Gradle wrapper and a multi-project Kotlin Multiplatform build. Use wrapper commands from the repository root.

## Duplicate `jvmRun` Task Warning

**Status:** Investigating
**Symptoms:**

```text
Target 'jvm': Unable to create run task 'jvmRun' as there is already such a task registered
Please remove the conflicting task or rename the new task
```

**Cause:** Two configuration paths register the same JVM run task in `desktopApp`. Gradle can continue, but one registration may mask the other and future plugin changes can turn this warning into a failure.

**Diagnosis:**

```powershell
.\gradlew.bat :desktopApp:tasks --all | Select-String jvmRun
.\gradlew.bat :desktopApp:run --stacktrace
```

**Fix:** Do not add a second `jvmRun` registration. Inspect `desktopApp/build.gradle.kts` and the applied Compose/Kotlin plugins, then remove or rename the custom registration. Record the exact code change here when resolved.

**Verification:** ` .\gradlew.bat :desktopApp:run` starts the desktop app and configuration emits no duplicate-task warning.

**Prevention:** Use existing plugin-created tasks before registering custom tasks, and run `:desktopApp:tasks --all` after build-script changes.

**Related Issues:** [Build Errors](BuildErrors.md), [Desktop](Desktop.md), [First Build](../setup/FirstBuild.md), [Git Guide](../guides/GIT_GUIDE.md).

## Dependency Resolution Failure

**Status:** Open

**Symptoms:** Gradle reports it cannot resolve a dependency or cannot download from `google()`, Maven Central, or JitPack.

**Cause:** The dependency is resolved from the repositories declared in `settings.gradle.kts` or the root build script. A blocked network, unavailable artifact, bad version, or stale cache prevents Gradle from constructing the dependency graph.

**Diagnosis:**

```powershell
.\gradlew.bat :core:dependencies --configuration jvmMainCompileClasspath
.\gradlew.bat :core:compileKotlinJvm --refresh-dependencies --stacktrace
```

**Fix:** Confirm network access, verify the version in `gradle/libs.versions.toml`, and rerun with `--refresh-dependencies`. Do not delete caches first; capture the exact missing module and repository.

**Verification:** The affected task completes with `BUILD SUCCESSFUL`.

**Prevention:** Use the version catalog, avoid dynamic versions, and document repository changes in the same change.

**Related Issues:** [Common Problems](CommonProblems.md), [Required Software](../setup/RequiredSoftware.md), [Build Errors](BuildErrors.md), [Git Guide](../guides/GIT_GUIDE.md).

Common links: [Troubleshooting index](README.md) | [Dependency problems](Gradle.md) | [Setup guide](../setup/FreshSetup.md) | [Git guide](../guides/GIT_GUIDE.md)
