# Windows

Windows is supported for desktop development and can run the JVM and Android portions of the project.

## `gradlew` Is Not Recognized

**Status:** Open

**Symptoms:** PowerShell says the term `gradlew` is not recognized as a command.

**Cause:** PowerShell does not execute scripts from the current directory without an explicit path, and Windows uses `gradlew.bat` rather than the Unix `gradlew` script.

**Diagnosis:**

```powershell
Get-Location
Test-Path .\gradlew.bat
Get-Command java
```

**Fix:** From the repository root, use:

```powershell
.\gradlew.bat --version
.\gradlew.bat :desktopApp:compileKotlinJvm
```

The explicit relative path selects the checked-in wrapper and avoids relying on a globally installed Gradle.

**Verification:** The wrapper prints Gradle/JVM information and the compile task completes.

**Prevention:** Use `gradlew.bat` on Windows and keep JDK 17 on `PATH`.

**Related Issues:** [Java](Java.md), [Gradle](Gradle.md), [Fresh Setup](../setup/FreshSetup.md), [Git Guide](../guides/GIT_GUIDE.md).

Common links: [Troubleshooting index](README.md) | [Dependency problems](Gradle.md) | [Setup guide](../setup/FreshSetup.md) | [Git guide](../guides/GIT_GUIDE.md)
