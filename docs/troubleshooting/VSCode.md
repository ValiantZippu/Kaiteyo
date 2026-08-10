# VS Code

VS Code is an editor and terminal host; Gradle remains the source of truth for build configuration.

## Java or Kotlin Diagnostics Do Not Match Gradle

**Status:** Open

**Symptoms:** VS Code shows unresolved imports or Java errors while the corresponding Gradle task succeeds, or the editor uses a different JDK.

**Cause:** The language extension may use a different interpreter, stale indexes, or an incomplete project import than the Gradle daemon.

**Diagnosis:**

```powershell
.\gradlew.bat :core:compileKotlinJvm
java --version
.\gradlew.bat --version
```

Check the selected Java runtime in VS Code's Java configuration and reload the window after changing it.

**Fix:** Select the JDK 17 runtime, ensure the workspace root is the directory containing `settings.gradle.kts`, run `Java: Clean Java Language Server Workspace`, and reload VS Code. Reimport the Gradle project if the Gradle extension offers that action.

**Verification:** The editor diagnostics settle and the narrow Gradle compile succeeds.

**Prevention:** Open the repository root, keep JDK 17 selected, and validate build changes with the wrapper rather than editor diagnostics alone.

**Related Issues:** [Java](Java.md), [First Build](../setup/FirstBuild.md), [Command Library](../development/COMMANDS.md), [Git Guide](../guides/GIT_GUIDE.md).

Common links: [Troubleshooting index](README.md) | [Dependency problems](Gradle.md) | [Setup guide](../setup/FreshSetup.md) | [Git guide](../guides/GIT_GUIDE.md)
