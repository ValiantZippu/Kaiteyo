# Kaiteyo (書いてよ) — Release Process

## Versioning

Kaiteyo follows [Semantic Versioning](https://semver.org/):

- **MAJOR** (1.0.0 → 2.0.0): Incompatible API changes, major redesigns
- **MINOR** (1.1.0 → 1.2.0): New features, significant improvements
- **PATCH** (1.1.0 → 1.1.1): Bug fixes, performance improvements, minor changes

Version is managed in `buildSrc/src/main/kotlin/AppVersion.kt`:
```kotlin
object AppVersion {
    const val versionCode = 110  // Increment for each release
    const val versionName = "1.1.0"  // Semantic version
    const val desktopAppVersion = "1.1.0"  // Desktop-specific version
}
```

## Release Types

### Development Build
- Built from `develop` branch
- Version: `{version}-dev.{build-number}`
- For internal testing
- Not distributed publicly

### Release Candidate
- Built from `release/v{version}` branch
- Version: `{version}-rc.{rc-number}`
- For QA testing
- Limited distribution

### Stable Release
- Built from `main` branch
- Version: `{version}`
- Tagged with `v{version}`
- Public distribution

## Release Workflow

### 1. Prepare Release Branch
```bash
git checkout develop
git pull
git checkout -b release/v1.1.0
```

### 2. Update Version
- Update `AppVersion.kt` with new version
- Update `gradle.properties` if needed
- Update `README.md` with new version

### 3. Run Full Test Suite
```bash
./gradlew :core:test
./gradlew :desktopApp:compileKotlinJvm
./gradlew :app:assembleDebug
```

### 4. Build Release Artifacts

#### Desktop
```bash
# Windows
./gradlew :desktopApp:packageMsi

# macOS
./gradlew :desktopApp:packageDmg

# Linux
./gradlew :desktopApp:packageDeb
```

#### Android
```bash
# Google Play
./gradlew :app:assembleGooglePlayRelease

# F-Droid
./gradlew :app:assembleFdroidRelease
```

### 5. Create GitHub Release
1. Tag the release: `git tag v1.1.0`
2. Push tag: `git push origin v1.1.0`
3. Create release on GitHub with:
   - Release title: `Kaiteyo v1.1.0`
   - Release notes (see template below)
   - Attach build artifacts

### 6. Merge to Main
```bash
git checkout main
git merge release/v1.1.0
git push origin main
```

### 7. Deploy

#### Desktop
- **Windows**: Upload MSI to GitHub Releases
- **macOS**: Upload DMG to GitHub Releases
- **Linux**: Upload Deb to GitHub Releases, submit to Flathub

#### Android
- **Google Play**: Upload AAB to Google Play Console
- **F-Droid**: Push tag, F-Droid bot will build automatically

### 8. Post-Release
- Merge release branch back to develop
- Update version for next development cycle
- Announce release on social media / mailing list

## Release Notes Template

```markdown
# Kaiteyo v{version}

## What's New
- {Feature 1}
- {Feature 2}
- {Feature 3}

## Improvements
- {Improvement 1}
- {Improvement 2}

## Bug Fixes
- {Fix 1}
- {Fix 2}

## Breaking Changes
- {Breaking change 1} (if any)

## Downloads
- [Windows](link-to-msi)
- [macOS](link-to-dmg)
- [Linux](link-to-deb)
- [Android](link-to-apk)

## Full Changelog
{link-to-changelog}
```

## Artifact Naming Convention

```
Kaiteyo-{version}-{platform}.{ext}
```

Examples:
- `Kaiteyo-1.1.0-windows.msi`
- `Kaiteyo-1.1.0-macos.dmg`
- `Kaiteyo-1.1.0-linux.deb`
- `Kaiteyo-1.1.0-android.apk`

## Distribution Channels

### Desktop
| Platform | Format | Distribution |
|----------|--------|--------------|
| Windows | MSI | GitHub Releases, future Microsoft Store |
| macOS | DMG | GitHub Releases, future Mac App Store |
| Linux | Deb | GitHub Releases, future Flathub/Snap |

### Mobile
| Platform | Format | Distribution |
|----------|--------|--------------|
| Android | AAB | Google Play Store |
| Android | APK | GitHub Releases, F-Droid |
| iOS | IPA | Future App Store |

## Hotfix Process

For critical bugs in production:

1. Branch from `main`: `git checkout -b hotfix/v1.1.1`
2. Fix the bug
3. Update patch version
4. Run tests
5. Build and release
6. Merge to both `main` and `develop`

## Pre-Release Checklist

- [ ] All tests pass
- [ ] Desktop app compiles successfully
- [ ] Android app compiles successfully
- [ ] No new warnings introduced
- [ ] Version numbers updated
- [ ] Changelog updated
- [ ] Release notes drafted
- [ ] Artifacts built and tested
- [ ] Documentation updated
- [ ] Breaking changes documented
- [ ] Migration guide written (if needed)
