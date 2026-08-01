# Kaiteyo — Setup Guide

## Prerequisites

| Requirement | Version | How to Check |
|-------------|---------|-------------|
| Java JDK | 17+ | `java --version` |
| Git | Latest | `git --version` |
| Gradle | (wrapper included) | `./gradlew --version` |

## Step-by-Step Setup

### 1. Install Git

**Windows:** Download from https://git-scm.com/download/win — use default options.

**macOS:** `brew install git`

**Linux:** `sudo apt install git`

### 2. Install Java JDK 17

**Windows/macOS/Linux:** Download from https://adoptium.net/temurin/releases/?version=17

After installation, verify:
```bash
java --version
# Expected: openjdk 17.x.x
```

### 3. Install VS Code

Download from https://code.visualstudio.com/

### 4. Install VS Code Extensions

Open VS Code, go to Extensions (Ctrl+Shift+X), search and install:

1. **Kotlin** (by Mathias Roth)
2. **Gradle for Java** (by Microsoft)
3. **Extension Pack for Java** (by Microsoft)
4. **GitLens** (by GitKraken)
5. **Error Lens** (by Alexander)
6. **Even Better TOML** (by tamasfe)
7. **YAML** (by Red Hat)

### 5. Clone the Repository

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

### 6. First Build

```
bash
./gradlew :desktopApp:compileKotlinJvm
```

First build takes 5-15 minutes to download dependencies.

### 7. Run the Desktop App

```bash
./gradlew :desktopApp:run
```

## Setting JAVA_HOME

If `java --version` works but Gradle can't find Java:

**Windows:**
1. Search "Environment Variables" in Start
2. Click "Environment Variables"
3. Under System Variables, click "New"
4. Variable name: `JAVA_HOME`
5. Variable value: `C:\Program Files\Eclipse Adoptium\jdk-17.x.x`

**macOS/Linux:**
```bash
# Add to ~/.bashrc or ~/.zshrc
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
```

## Building for Different Platforms

### Desktop
```bash
./gradlew :desktopApp:run                    # Run
./gradlew :desktopApp:compileKotlinJvm       # Compile only
./gradlew :desktopApp:packageMsi             # Windows installer
./gradlew :desktopApp:packageDmg             # macOS installer
./gradlew :desktopApp:packageDeb             # Linux installer
```

### Android
```
bash
./gradlew :app:assembleDebug                 # Debug APK
./gradlew :app:assembleRelease               # Release APK
```

## Common Issues

### "Java not found"
Install JDK 17 and restart VS Code. Verify with `java --version`.

### "Permission denied" on Linux/macOS
```
bash
chmod +x gradlew
```

### "Kotlin/Native targets cannot be built"
Expected on Windows. Add to `gradle.properties`:
```properties
kotlin.native.ignoreDisabledTargets=true
```

### Build fails with memory error
```bash
# Increase Gradle memory
export GRADLE_OPTS="-Xmx2048m"
