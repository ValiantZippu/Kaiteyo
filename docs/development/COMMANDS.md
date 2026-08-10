# Kaiteyo Command Library

Run commands from the repository root. Windows examples use `gradlew.bat`; macOS/Linux examples use `./gradlew`.

## Git

```bash
git clone <repository-url>
git status
git pull --rebase
git switch -c feature/name
git diff
git add <path>
git commit -m "type: short description"
git push -u origin feature/name
git log --oneline --decorate -10
git fetch --all --prune
git tag --list
```

`clone` downloads the repository, `status` shows local changes, `pull --rebase` updates a branch, `switch -c` creates a branch, `diff` reviews edits, `add` stages files, `commit` records a change, `push` publishes it, `log` inspects history, `fetch` refreshes remote refs, and `tag` lists releases.

## Gradle and Build

```powershell
.\gradlew.bat --version
.\gradlew.bat projects
.\gradlew.bat tasks --all
.\gradlew.bat :core:compileKotlinJvm
.\gradlew.bat :desktopApp:compileKotlinJvm
.\gradlew.bat :desktopApp:run
.\gradlew.bat :app:assembleDebug
.\gradlew.bat build
.\gradlew.bat clean
.\gradlew.bat --stop
.\gradlew.bat <task> --stacktrace
.\gradlew.bat <task> --info
.\gradlew.bat <task> --refresh-dependencies
```

`--version` verifies Gradle/JVM, `projects` lists modules, `tasks` lists available work, compile tasks validate a target, `run` launches desktop, `assembleDebug` creates an Android debug APK, `build` runs the aggregate build, `clean` removes outputs, `--stop` stops daemons, `--stacktrace` adds failure detail, `--info` adds diagnostic logging, and `--refresh-dependencies` bypasses dependency cache metadata.

## Android and ADB

```powershell
.\gradlew.bat :app:assembleDebug
adb devices
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb logcat
adb shell am force-stop <application-id>
```

These commands build, list connected devices, install/update an APK, stream logs, and stop the application. Use the application ID from the Android module configuration.

Configure the standard Windows SDK location before Android or aggregate builds:

```powershell
$sdk = "$env:LOCALAPPDATA\Android\Sdk"
$env:ANDROID_HOME = $sdk
$env:ANDROID_SDK_ROOT = $sdk
"sdk.dir=$($sdk -replace '\\','/')" | Set-Content -Encoding ASCII local.properties
```

Run this only after the directory exists; `local.properties` is machine-specific and must remain untracked.

## Desktop and Installers

```powershell
.\gradlew.bat :desktopApp:run
.\gradlew.bat :desktopApp:packageMsi
.\gradlew.bat :desktopApp:packageDmg
.\gradlew.bat :desktopApp:packageDeb
```

Run starts the JVM app. Packaging tasks create Windows MSI, macOS DMG, or Linux Debian artifacts and should run on the matching host platform.

## Compose and Kotlin

```powershell
.\gradlew.bat :core:compileKotlinJvm
.\gradlew.bat :desktopApp:compileKotlinJvm --rerun-tasks
.\gradlew.bat :core:allTests
```

These validate shared JVM compilation, force a desktop compile when incremental state is suspect, and run the core test aggregation when available.

## SQLDelight and Generated Code

```powershell
.\gradlew.bat :core:generateCommonMainAppDataDatabaseInterface
.\gradlew.bat :core:generateCommonMainUserDataDatabaseInterface
.\gradlew.bat :core:compileKotlinJvm
```

The first two tasks regenerate database interfaces from SQLDelight schemas; compilation then checks generated and handwritten code together.

## Release, Signing, and Sync

```powershell
.\gradlew.bat :app:assembleRelease
.\gradlew.bat :desktopApp:packageMsi
.\gradlew.bat :desktopApp:packageDmg
.\gradlew.bat :desktopApp:packageDeb
git tag v1.1.0
git push origin v1.1.0
```

Release builds and installers create distributable artifacts. Tags identify releases and trigger the repository release workflow when configured. Never put keystores, passwords, tokens, or signing properties in Git.

## Debugging

```powershell
.\gradlew.bat <task> --stacktrace --info
.\gradlew.bat --status
java --version
javac --version
$env:JAVA_HOME
$env:ANDROID_HOME
```

Use task logs first, then inspect daemon status and toolchain environment variables. See [Troubleshooting](../troubleshooting/README.md) for diagnosis by symptom.

## Platform Equivalents

On macOS/Linux, replace ` .\gradlew.bat` with `./gradlew` and use `chmod +x gradlew` once if needed. On PowerShell, use `Get-Content`, `Select-String`, `Test-Path`, and `where.exe`; on Bash, use `cat`, `grep`, `test -f`, and `command -v`.

## Related Documentation

- [Fresh Setup](../setup/FreshSetup.md)
- [First Build](../setup/FirstBuild.md)
- [Required Software](../setup/RequiredSoftware.md)
- [Troubleshooting](../troubleshooting/README.md)
- [Git Guide](../guides/GIT_GUIDE.md)
