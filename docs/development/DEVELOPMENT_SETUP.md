# Kaiteyo — Development Setup

This guide walks you through setting up a development environment for Kaiteyo from
scratch. It is grounded in the actual build requirements: JDK 17, Gradle wrapper,
version catalog, and the Android SDK only when building Android.

## System requirements

| Component | Minimum | Recommended |
|-----------|---------|-------------|
| RAM | 8 GB | 16 GB+ |
| Disk | 10 GB free | 20 GB+ free (Gradle caches + Android SDK) |
| OS | Windows 10, macOS 12+, Linux (Ubuntu 22.04+) | Windows 11, macOS 14+ |
| Internet | Broadband (first build downloads everything) | Broadband |
| JDK | **17** (Temurin) | 17 (toolchain is pinned to 17) |

## Step 1 — Install Git

### Windows
1. Download from https://git-scm.com/download/win
2. Run the installer (default options are fine)
3. Verify: `git --version`

### macOS
```bash
brew install git
```

### Linux
```bash
sudo apt update && sudo apt install git
```

## Step 2 — Install JDK 17 (Temurin)

The Gradle build uses `jvmToolchain(17)` everywhere; a different major version will
not build.

### Windows
1. Download from https://adoptium.net/temurin/releases/?version=17 (MSI, x64)
2. Install with "Add to PATH" checked
3. Verify in a new terminal: `java --version` → `openjdk 17.x.x`

### macOS
```bash
brew install --cask temurin@17
```

### Linux
```bash
sudo apt install openjdk-17-jdk
java --version
```

> Tip: the Gradle wrapper will find JDK 17 via toolchain auto-download if your
> default JDK differs, but installing 17 directly avoids surprises.

## Step 3 — IDE

### Recommended: IntelliJ IDEA (Community is free)

1. Download from https://www.jetbrains.com/idea/download/
2. Open the repository root — IntelliJ auto-detects the Gradle project
3. Let indexing + dependency resolution finish (first import: several minutes)

### VS Code (for docs, scripts, markdown; weaker Kotlin support)

Extensions: **Kotlin**, **Gradle for Java**, **Extension Pack for Java**, **GitLens**,
**Error Lens**, **Even Better TOML**, **YAML**, **Markdown All in One**.

## Step 4 — Clone the repository

```bash
cd ~/Projects          # or C:\Projects on Windows
git clone https://github.com/ValiantZippu/Kaiteyo.git
cd Kaiteyo
git checkout develop   # the default integration branch
```

## Step 5 — First build

```bash
# Linux/macOS
./gradlew :desktopApp:compileKotlinJvm

# Windows (PowerShell or cmd)
.\gradlew.bat :desktopApp:compileKotlinJvm
```

**First build takes 5–15 minutes** and downloads the Gradle distribution, Kotlin
compiler, Compose Multiplatform libraries, and the app-data asset
(`kanji-dojo-data-base-v15.sql`, from GitHub releases — network required).

## Step 6 — Run the desktop app

```bash
./gradlew :desktopApp:run
# Japanese UI:
./gradlew :desktopApp:run -Duser.language=ja -Duser.country=JP
```

The first desktop launch seeds a small demo deck and (on very first run) shows the
8-step onboarding wizard.

## Step 7 (optional) — Android SDK

Only needed for `:app` builds.

1. Install Android Studio (https://developer.android.com/studio) — it bundles the SDK.
2. Set environment variables:
   - **Windows (PowerShell)**:
     ```powershell
     $sdk = "$env:LOCALAPPDATA\Android\Sdk"
     $env:ANDROID_HOME = $sdk
     $env:ANDROID_SDK_ROOT = $sdk
     "sdk.dir=$($sdk -replace '\\','/')" | Set-Content -Encoding ASCII local.properties
     ```
   - **macOS/Linux**:
     ```bash
     export ANDROID_HOME=$HOME/Library/Android/sdk   # macOS
     # export ANDROID_HOME=$HOME/Android/Sdk          # Linux
     export ANDROID_SDK_ROOT=$ANDROID_HOME
     echo "sdk.dir=$ANDROID_HOME" > local.properties
     ```
3. `local.properties` is machine-specific — it must stay untracked (it's in
   `.gitignore`). Never commit it.

## Step 8 (optional) — iOS

macOS only. Open `iosApp/KaiteyoApp.xcodeproj` and build from Xcode, or build the
shared framework with `./gradlew :core:linkDebugFrameworkIosArm64`.

## Troubleshooting

### "java is not recognized" / wrong version
JDK 17 not in PATH. Reinstall Temurin 17 with "Add to PATH" (Windows), or install
`temurin@17` (macOS). Restart the terminal/IDE.

### "Permission denied" on Linux/macOS
```bash
chmod +x gradlew
```

### Build fails with "Connection refused"
Network is blocking Gradle/asset downloads. Check proxy settings; retry
`./gradlew --refresh-dependencies`. The first build needs GitHub releases access for
the app-data asset.

### "Kotlin/Native targets cannot be built"
Expected on Windows — `kotlin.native.ignoreDisabledTargets=true` is already set in
`gradle.properties`.

### Android "SDK location not found"
`ANDROID_HOME` not set or `local.properties` missing — see Step 7.

## Next steps

1. Read `docs/README.md` (docs map)
2. Read `docs/engineering/ENGINEERING_STANDARDS.md` (engineering contract)
3. Read `docs/development/AI_CONTEXT.md` (workflow + never-change list)
4. Read `docs/development/COMMANDS.md` (command library)
5. Check `docs/planning/CURRENT_ISSUES.md` and `docs/planning/TODO.md`
