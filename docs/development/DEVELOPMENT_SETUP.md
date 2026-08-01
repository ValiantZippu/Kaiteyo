# Kaiteyo — Development Setup

This guide walks you through setting up a development environment for Kaiteyo from scratch.

## System Requirements

| Component | Minimum | Recommended |
|-----------|---------|-------------|
| RAM | 8GB | 16GB+ |
| Disk Space | 10GB free | 20GB+ free |
| OS | Windows 10, macOS 12+, Linux (Ubuntu 22.04+) | Windows 11, macOS 14+ |
| Internet | Broadband (for first build) | Broadband |

## Step 1: Install Git

### Windows
1. Download from https://git-scm.com/download/win
2. Run the installer (default options are fine)
3. Verify: Open Command Prompt and run `git --version`

### macOS
```bash
# Using Homebrew
brew install git

# Or download from https://git-scm.com/download/mac
```

### Linux
```bash
sudo apt update
sudo apt install git
```

## Step 2: Install Java JDK 17

### Windows
1. Download from https://adoptium.net/temurin/releases/?version=17
2. Choose the MSI installer for your architecture (x64)
3. Run the installer — check "Add to PATH"
4. Verify: Open new Command Prompt and run `java --version`
5. Expected output: `openjdk 17.x.x`

### macOS
```bash
# Using Homebrew
brew install openjdk@17

# Or download from https://adoptium.net/
```

### Linux
```bash
sudo apt install openjdk-17-jdk
java --version
```

## Step 3: Install VS Code

1. Download from https://code.visualstudio.com/
2. Install with default options
3. Launch VS Code

## Step 4: Install VS Code Extensions

Open VS Code, go to Extensions (Ctrl+Shift+X), and install:

1. **Kotlin** — Language support
2. **Gradle for Java** — Build integration
3. **Extension Pack for Java** — Java support
4. **GitLens** — Git history
5. **Error Lens** — Inline errors
6. **Even Better TOML** — TOML support
7. **YAML** — YAML support
8. **Markdown All in One** — Markdown preview
9. **GitHub Pull Requests** — PR management

## Step 5: Clone the Repository

```bash
# Open terminal in VS Code (Ctrl+`)
# Navigate to your projects folder
cd C:\Projects  # Windows
# or
cd ~/Projects   # macOS/Linux

# Clone
git clone https://github.com/YOUR_USERNAME/kaiteyo.git

# Enter project
cd kaiteyo
```

## Step 6: First Build

```bash
# Compile the desktop app (this downloads dependencies)
./gradlew :desktopApp:compileKotlinJvm
```

**First build takes 5-15 minutes.** It downloads:
- Gradle wrapper (if not cached)
- Kotlin compiler
- Compose Multiplatform libraries
- All dependencies

## Step 7: Run the Desktop App

```bash
./gradlew :desktopApp:run
```

## Step 8: (Optional) Install Android Studio

For Android development:
1. Download from https://developer.android.com/studio
2. Install with default options
3. During setup, install Android SDK
4. Set `ANDROID_HOME` environment variable

## Troubleshooting

### "java is not recognized"
JDK is not in PATH. Reinstall JDK and check "Add to PATH". Restart VS Code.

### "Permission denied" on Linux/macOS
```bash
chmod +x gradlew
```

### Build fails with "Connection refused"
Your network may be blocking Gradle downloads. Try:
```bash
./gradlew --no-daemon
```

### "Kotlin/Native targets cannot be built"
This is expected on Windows. Add to `gradle.properties`:
```
kotlin.native.ignoreDisabledTargets=true
```

## Next Steps

1. Read `docs/00_START_HERE.md`
2. Read `docs/AI_CONTEXT.md`
3. Check `docs/planning/CURRENT_ISSUES.md`
4. Check `docs/planning/TODO.md`
