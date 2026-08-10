# Building Kaiteyo Installers

Every format is produced in two stages:

1. **App bundle** — `./gradlew :desktopApp:createDistributable`
   (a plain jpackage image, JRE included — no host Java required at install time)
2. **Package** — one of the builders below

## 0. Prerequisites

| Tool | Needed for |
|------|-----------|
| JDK 17 + Gradle | app bundle |
| `rsvg-convert` + ImageMagick | installer brand assets |
| Inno Setup 6.4+ (`ISCC.exe`) | Windows EXE installer |
| `create-dmg` (Homebrew) | styled macOS DMG |
| `appimagetool` (auto-downloaded) | Linux AppImage |
| `dpkg-deb` | Linux deb |
| `rpmbuild` (rpmdevtools) | Linux rpm |
| `flatpak-builder` | Linux Flatpak |
| `snapcraft` | Snap (optional) |

## 1. Generate brand assets (once per artwork change)

```bash
bash installer/scripts/generate-assets.sh
# → installer/assets/generated/{windows,macos,linux}/
```

## 2. Windows

```powershell
# EXE installer (premium Inno Setup)
powershell -File installer/windows/build.ps1 -Version 2.2.1
# → installer/windows/build/kaiteyo-2.2.1/Kaiteyo-Setup-2.2.1.exe

# Portable zip
powershell -File installer/windows/portable/build-portable.ps1 -Version 2.2.1
# → installer/windows/build/kaiteyo-2.2.1/Kaiteyo-Portable-2.2.1.zip

# MSI (enterprise/Compose-native)
./gradlew :desktopApp:packageMsi
# → desktopApp/build/compose/binaries/main/msi/*.msi
```

Signing: `build.ps1 -Sign -CertThumbprint <sha1>` (see `docs/SIGNING.md`).

## 3. macOS (must run on a Mac)

```bash
# 1. Build the .app (twice — once per arch — or use universal)
./gradlew :desktopApp:packageDmg          # produces a plain DMG we replace

# 2. Styled DMG with drag-to-Applications background
bash installer/macos/build-dmg.sh arm64 2.2.1
bash installer/macos/build-dmg.sh x64   2.2.1

# 3. Sign + notarize (credentials via env, see docs/SIGNING.md)
bash installer/macos/notarize.sh "desktopApp/build/compose/binaries/main/dmg/Kaiteyo-2.2.1-macos-arm.dmg"
```

## 4. Linux

```bash
./gradlew :desktopApp:createDistributable

bash installer/linux/appimage/build.sh 2.2.1     # portable single file
bash installer/linux/deb/build.sh 2.2.1          # Debian/Ubuntu
bash installer/linux/rpm/build.sh 2.2.1          # Fedora/RHEL
bash installer/linux/flatpak/build.sh 2.2.1      # sandboxed store format
bash installer/linux/snap/build.sh 2.2.1         # optional
```

## 5. Stage & verify a release

```bash
bash installer/scripts/stage-artifacts.sh 2.2.1
bash installer/scripts/verify-artifacts.sh 2.2.1
# → release/kaiteyo-2.2.1/ (canonical names + artifact-manifest.json)
```

## Gotchas

- **Windows**: ISCC must run with the repo root as CWD; paths in `kaiteyo.iss`
  are relative to the script directory.
- **macOS**: `create-dmg` may refuse to sign a bundle signed with an identity it
  doesn't know — sign the DMG *after* styling it (`notarize.sh` does this).
- **Linux**: AppImage builds are per-arch. CI builds `x86_64` on ubuntu-latest
  and `aarch64` on an ARM runner.
- **Sizes**: the bundled JRE dominates (~180 MB unpacked). `jlink` trimming is
  a future optimization tracked in `docs/planning/FUTURE_IDEAS.md`.
