# Kaiteyo (書いてよ) — Release Process

> The authoritative, current guide. The full pipeline — build, sign, package,
> stage, verify, publish — lives in `installer/`; start with `installer/README.md`.

## Versioning

Semantic Versioning (`MAJOR.MINOR.PATCH`). The **single source of truth** is:

- `installer/common/version.json`
- `buildSrc/src/main/kotlin/AppVersion.kt`

Never edit them apart. Bump both together:

```bash
bash installer/scripts/bump-version.sh 2.3.0 2310
```

(version_code = build number, usually previous + 10.)

## Release types

| Type | Branch | Version | Audience |
|------|--------|---------|----------|
| Development | `develop` | `{version}-dev.{n}` | internal |
| Release candidate | `release/v{version}` | `{version}-rc.{n}` | QA |
| Stable | tag `v{version}` | `{version}` | everyone |
| Hotfix | from `main` | `{version}` | production fixes |

## Workflow

### 1. Prepare

```bash
git checkout develop && git pull
git checkout -b release/v2.3.0
bash installer/scripts/bump-version.sh 2.3.0 2310
```

Update the `<releases>` block in
`installer/linux/appimage/io.github.syt0r.kaiteyo.metainfo.xml`.

### 2. Build & smoke test

```bash
./gradlew :core:allTests :desktopApp:compileKotlinJvm
```

Then build at least one platform installer per `installer/docs/BUILD.md` and
verify it: Windows EXE upgrades cleanly, macOS `spctl` accepts the DMG, Linux
AppImage runs on a clean machine.

### 3. Tag & release

```bash
git tag v2.3.0 && git push origin v2.3.0
```

`build-release.yml` triggers `build-all.yml`:

| Job | Artifacts |
|-----|-----------|
| Linux | AppImage, deb, rpm, Android APK |
| Windows | Inno EXE, MSI, portable zip |
| macOS Intel | styled, signed, notarized DMG |
| macOS ARM | styled, signed, notarized DMG |

A staging job then runs `stage-artifacts.sh` + `verify-artifacts.sh` (sha256
integrity gate — a mismatch fails the release), generates the update feeds for
all three channels (`update-{stable,beta,nightly}.json`) and publishes them to
the dedicated `update-feed` release that the desktop app reads from.

### 4. Publish

1. Draft release notes from `installer/templates/RELEASE_NOTES.md`.
2. Attach `artifact-manifest.json` + the `update-*.json` feeds (CI does this;
   feeds also go to the `update-feed` release).
3. Flathub: PR the manifest to `flathub/io.github.syt0r.kaiteyo`.
4. Merge `release/v2.3.0` → `main`, then back to `develop`.

### 5. Hotfix

Branch from `main`, bump PATCH, test, tag, release, merge back to `main` + `develop`.

## Artifact naming

`Kaiteyo-{version}-{platform}.{ext}` — examples:

```
Kaiteyo-2.3.0-windows-setup.exe
Kaiteyo-2.3.0-windows.msi
Kaiteyo-2.3.0-windows-portable.zip
Kaiteyo-2.3.0-macos-arm64.dmg
Kaiteyo-2.3.0-macos-x64.dmg
Kaiteyo-2.3.0-linux.AppImage
Kaiteyo-2.3.0-linux.deb
Kaiteyo-2.3.0-linux.rpm
Kaiteyo-2.3.0-linux.flatpak
Kaiteyo-2.3.0-android.apk
```

## Distribution channels

| Platform | Formats | Distribution |
|----------|---------|--------------|
| Windows | EXE (premium), MSI (enterprise), zip (portable) | GitHub Releases |
| macOS | DMG (arm64 + x64, signed + notarized) | GitHub Releases |
| Linux | AppImage, deb, rpm, Flatpak | GitHub Releases + Flathub |
| Android | APK (F-Droid), AAB (Google Play) | F-Droid + Play Console |

## Upgrades

- **Windows**: the EXE auto-detects the previous install (same AppId), remembers
  the directory, and preserves `%LOCALAPPDATA%\Kaiteyo` untouched.
- **macOS**: drag the new `.app` over the old one; study data in
  `~/Library/Application Support/Kaiteyo` (and `~/.kaiteyo`) is preserved.
- **Linux**: package manager / AppImage swap; data under `~/.kaiteyo` /
  `$XDG_DATA_HOME/kaiteyo` is preserved.
- Never uninstall before upgrading.

## Signing summary

| Platform | Requirement | Tool | See |
|----------|-------------|------|-----|
| Windows | code signature | signtool | `installer/docs/SIGNING.md` |
| macOS | signature + notarization | codesign + notarytool | `installer/docs/SIGNING.md` |
| Linux | sha256 manifest | — | `installer/docs/SIGNING.md` |

## Pre-release checklist

- [ ] `bump-version.sh` ran; `version.json` == `AppVersion.kt`
- [ ] metainfo.xml `<releases>` updated
- [ ] `:core:allTests` + `:desktopApp:compileKotlinJvm` green
- [ ] Installers built on native OS; EXE/DMG signed (macOS also notarized + stapled)
- [ ] `verify-artifacts.sh` green; manifest attached
- [ ] Update feed regenerated (`make-update-manifest.sh`)
- [ ] Release notes drafted
- [ ] Docs updated; `planning/CURRENT_ISSUES.md` reflects fixed issues
