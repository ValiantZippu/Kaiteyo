# First Build

The first build downloads the Gradle distribution, Kotlin/Compose plugins, Android dependencies, and shared libraries. Allow 5-15 minutes on a normal connection.

```powershell
.\gradlew.bat :core:compileKotlinJvm
.\gradlew.bat :desktopApp:compileKotlinJvm
.\gradlew.bat :desktopApp:run
```

On macOS/Linux replace the wrapper command with `./gradlew`. A successful task ends with `BUILD SUCCESSFUL`. Platform warnings about unavailable iOS targets on non-macOS hosts are expected; see [Build Errors](../troubleshooting/BuildErrors.md).

Related: [Fresh Setup](FreshSetup.md), [Gradle](../troubleshooting/Gradle.md), [Command Library](../development/COMMANDS.md).
