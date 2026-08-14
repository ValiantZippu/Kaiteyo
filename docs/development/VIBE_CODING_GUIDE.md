# Kaiteyo — Vibe Coding Guide

This guide is for developers using AI assistants (Claude, GPT, Gemini, local models) to contribute to Kaiteyo. It covers everything from zero setup to daily workflow.

## Prerequisites

### Accounts
- **GitHub** account (free)
- **VS Code** (free, recommended) or **IntelliJ IDEA** (Community Edition is free)

### Software Installation (Windows)

#### 1. Git
```bash
# Download from https://git-scm.com/download/win
# Install with default options
# Verify:
git --version
```

#### 2. VS Code
```bash
# Download from https://code.visualstudio.com/
# Install with default options
```

#### 3. Java JDK 17 (Temurin)
```bash
# Download from https://adoptium.net/temurin/releases/?version=17
# Install MSI
# Add to PATH (installer usually does this)
# Verify:
java --version
# Should show: openjdk 17.x.x
```

#### 4. Android Studio (optional, for Android builds)
```bash
# Download from https://developer.android.com/studio
# Install with default options
# During setup, install Android SDK
```

#### 5. GitHub Desktop (optional, for Git beginners)
```bash
# Download from https://desktop.github.com/
```

### VS Code Extensions

Install these extensions (Ctrl+Shift+X):

| Extension | Purpose |
|-----------|---------|
| **Kotlin** | Kotlin language support |
| **Gradle for Java** | Gradle integration |
| **Extension Pack for Java** | Java support |
| **GitLens** | Git history and blame |
| **Error Lens** | Inline error display |
| **Even Better TOML** | TOML file support |
| **YAML** | YAML file support |
| **Markdown All in One** | Markdown preview |
| **GitHub Pull Requests** | PR management |

Optional AI extensions:
- **Continue** (continue.dev) — Open-source AI coding assistant
- **Cline** — AI agent for VS Code
- **GitHub Copilot** — Microsoft's AI pair programmer

## First Clone

```bash
# Open terminal (Ctrl+`)
# Navigate to where you want the project
cd C:\Projects

# Clone the repository
git clone https://github.com/YOUR_USERNAME/kaiteyo.git

# Enter the project
cd kaiteyo

# Switch to develop branch
git checkout develop

# Run the desktop app (first build downloads dependencies)
./gradlew :desktopApp:run
```

**Note:** The first build takes 5-15 minutes as it downloads Gradle, Kotlin compiler, and all dependencies.

## What is Gradle?

Gradle is the build system. It:
- Downloads dependencies (libraries)
- Compiles Kotlin code
- Packages the application
- Runs tests

Key commands:
```bash
./gradlew :desktopApp:run           # Run the desktop app
./gradlew :desktopApp:compileKotlinJvm  # Just compile (faster)
./gradlew :core:allTests            # Run shared-engine tests
./gradlew clean                     # Clean all build files
```

## What is JDK?

JDK (Java Development Kit) is needed because Kotlin runs on the Java Virtual Machine (JVM). It includes:
- **javac** — Java compiler (Kotlin also uses it)
- **java** — Runtime to run the application
- **jlink** — To package the app with a minimal JVM

## Why PATH Matters

PATH is an environment variable that tells your terminal where to find programs. When you install Git, Java, or Android Studio, they should add themselves to PATH automatically. If a command like `java --version` doesn't work, the program isn't in PATH.

**Fix PATH issues:**
1. Open System Properties → Advanced → Environment Variables
2. Edit the `Path` variable
3. Add the installation directories (e.g., `C:\Program Files\Eclipse Adoptium\jdk-17.0.xx\bin`)

## Daily Workflow

### Starting a Session
```bash
# Get latest code
git checkout develop
git pull

# Read documentation first
# Open docs/README.md
# Open docs/development/AI_CONTEXT.md
# Open docs/planning/CURRENT_ISSUES.md

# Start the app
./gradlew :desktopApp:run
```

### Making Changes
```bash
# Create a feature branch
git checkout -b feature/my-feature

# Make changes in VS Code
# Compile to check for errors
./gradlew :desktopApp:compileKotlinJvm

# If successful, commit
git add .
git commit -m "feat: description of changes"

# Push to GitHub
git push origin feature/my-feature
```

### Creating a Pull Request
1. Go to GitHub.com → your fork → Pull Requests
2. Click "New Pull Request"
3. Select your branch
4. Write a description
5. Click "Create Pull Request"

## Common Errors and Fixes

### "Unresolved reference 'X'"
**Cause:** Missing import or wrong package.
**Fix:** Add the correct import. Check `docs/development/AI_CONTEXT.md` for import rules.

### "BUILD FAILED in Xs"
**Cause:** Compilation error. Scroll up to see the actual error (look for `e:` lines).
**Fix:** Read the error message carefully. It tells you the file, line number, and what's wrong.

### "Java not found"
**Cause:** JDK not installed or not in PATH.
**Fix:** Install JDK 17 and add to PATH. Restart VS Code.

### "Gradle sync failed"
**Cause:** Network issue or corrupted cache.
**Fix:** 
```bash
./gradlew clean
# Then try again
```

### "Kotlin/Native targets cannot be built on this machine"
**Cause:** iOS targets can't be built on Windows.
**Fix:** This is expected. Ignore the warning. Add to `gradle.properties`:
```
kotlin.native.ignoreDisabledTargets=true
```

## How GitHub Actions Builds Releases

When you push a tag (e.g., `v1.1.0`), GitHub Actions automatically:
1. Checks out the code
2. Sets up JDK 17
3. Builds the desktop app for Windows (MSI), macOS (DMG), Linux (Deb)
4. Builds the Android app (APK, AAB)
5. Creates a GitHub Release with all artifacts

## How Signing Works for Android

Release builds need a keystore for signing. **No keystore or credentials are committed to
the repository** — they are kept out of git for security. The build resolves the keystore
from (in order):

1. `KEYSTORE_PATH` environment variable
2. `~/.kaiteyo/keystore.jks`
3. Repo-root `keystore.jks` (CI decodes it from the `KEYSTORE_BASE64` secret)

If none exists, the build falls back to **debug signing** so local builds still work.
Production release secrets (keystore password, key alias, key password) come from CI
environment variables — never commit them.

## AI Workflow Tips

1. **Always read docs first** — The `/docs` directory is the project brain
2. **Check CURRENT_ISSUES.md** — Know what's broken before adding features
3. **Compile after each change** — Don't make 10 changes then compile
4. **Follow the design language** — Read `../design/DESIGN_LANGUAGE.md` before UI changes
5. **Update documentation** — If you add a feature, document it
6. **One file at a time** — Make focused changes, not massive rewrites
7. **Use the correct imports** — See `AI_CONTEXT.md` for import rules
