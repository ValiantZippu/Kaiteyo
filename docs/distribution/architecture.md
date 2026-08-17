# Kaiteyo — Distribution Architecture

How Kaiteyo moves from source code to an installed, updated, uninstalled
product. The full pipeline, in order:

```
KAITEYO SOURCE
      │
      v
VERSION SOURCE (version.json + AppVersion.kt)
      │
      v
BUILD SYSTEM (Gradle → jpackage app bundles + Android APK/AAB)
      │
      v
PACKAGING (installer/ — per-platform wrappers)
      ├── Windows: Inno Setup EXE · MSI · portable ZIP · WinGet/Choco/Scoop manifests
      ├── Linux:   AppImage · deb · rpm · Flatpak · Snap (optional) · PKGBUILD
      └── Android: APK (fdroid) · AAB (googlePlay)
      │
      v
SIGNING (Windows signtool · macOS codesign+notarize · Android keystore)
      │
      v
CHECKSUMS + MANIFEST (artifact-manifest.json, sha256 per artifact)
      │
      v
STAGING + VERIFICATION (stage-artifacts.sh → verify-artifacts.sh)
      │
      v
RELEASE (GitHub release + update feeds per channel)
      │
      v
USER (download → install → launch → onboarding → updates)
```

## 1. Build architecture

- One Kotlin Multiplatform project. `:desktopApp` produces JVM app bundles via
  the Compose Gradle plugin (`createDistributable` — a jpackage image with a
  bundled JRE); `:app` produces Android APKs/AABs per flavor.
- JDK 17 everywhere (`jvmToolchain(17)`), Kotlin/Compose MPP pinned in
  `settings.gradle.kts` + `gradle/libs.versions.toml`.
- **Important:** the Compose plugin's raw MSI/DMG/AppImage output is *plain*.
  Everything branded lives in `installer/`, which wraps those bundles. This
  decoupling means packaging is testable without a Gradle build
  (see `installer/docs/ARCHITECTURE.md` §1).

## 2. Packaging architecture

Every format is produced in two stages: app bundle first, then package
(see `installer/docs/BUILD.md`):

| Format | Builder | Role |
|---|---|---|
| Windows EXE | `installer/windows/kaiteyo.iss` (Inno Setup 6) | Primary consumer installer |
| Windows MSI | `:desktopApp:packageMsi` (jpackage) | Enterprise / Programs & Features |
| Windows portable | `installer/windows/portable/` | Self-contained ZIP, no install |
| macOS DMG | `installer/macos/build-dmg.sh` | Styled drag-to-Applications |
| Linux AppImage | `installer/linux/appimage/build.sh` | Portable single-file |
| Linux deb | `installer/linux/deb/build.sh` | Debian/Ubuntu |
| Linux rpm | `installer/linux/rpm/build.sh` | Fedora/RHEL |
| Linux Flatpak | `installer/linux/flatpak/build.sh` | Sandboxed store format |
| Linux Snap | `installer/linux/snap/` | Optional |
| Android APK/AAB | Gradle (`:app`) | F-Droid / Play |

Design principle: **platform-native where it wins.** Windows gets a native
wizard, Linux gets native packages, Android gets store-native artifacts — one
shared brand, no forced one-size-fits-all installer
(see `installer/docs/ARCHITECTURE.md`).

## 3. Installation architecture

- **Windows**: Inno Setup wizard — welcome → location (remembered across
  upgrades) → optional Start Menu/desktop tasks → progress (real steps, not
  fake percentages) → done with a launch checkbox. Silent install via
  `/VERYSILENT /SUPPRESSMSGBOXES`. The MSI is offered to enterprise users.
  Portable ZIP: extract and run, self-contained data folder.
- **Linux**: package-manager install (deb/rpm/Flatpak/Snap) or portable
  (AppImage). No sudo needed for ordinary use — the app runs as the user.
- **Android**: Play Store (AAB, googlePlay flavor) or F-Droid/sideload (APK,
  fdroid flavor). First launch asks only for permissions the user's chosen
  features actually need.

## 4. Update architecture

Three channels — `stable`, `beta`, `nightly` — one JSON feed each
(`update-<channel>.json`, schema in `installer/common/update-manifest.schema.json`),
published to the dedicated `update-feed` GitHub release. The desktop app
fetches the feed over HTTPS, compares version codes, downloads the artifact,
**verifies its sha256 before writing anything**, applies via the platform's
native mechanism, and keeps the previous version for rollback
(see `installer/docs/UPDATES.md` and [updates.md](updates.md)).

## 5. Signing architecture

| Platform | Requirement | Tooling |
|---|---|---|
| Windows | Code signature (EV/OV cert) | `signtool` with RFC 3161 timestamp |
| macOS | Developer ID + notarization + stapling | `codesign` + `notarytool` |
| Android | Release keystore (never in repo) | Gradle signing config |
| Linux | None (sha256 manifest) | — |

Credentials live in CI secrets / the developer environment **only**; the repo
contains no private keys. Windows/macOS releases refuse to publish unsigned
artifacts once secrets are configured (see [signing.md](signing.md)).

## 6. Release architecture

Tagged releases (`vX.Y.Z`) trigger `.github/workflows/build-release.yml` →
`build-all.yml`, which builds all platforms in parallel, then a staging job
renames artifacts canonically, computes the sha256 manifest, verifies
everything, and publishes the release + update feeds. A failed verification
**fails the release**. Full detail:
`docs/releases/RELEASE_PROCESS.md` + `docs/releases/RELEASE_CHECKLIST.md`.

## 7. Onboarding architecture

Two deliberately separate phases:

1. **Installer phase** (per-OS tool): location, components, shortcuts, launch
   checkbox.
2. **App phase** (`OnboardingWizard`, shown once): theme, accent, scaling, font
   size, navigation, motion — applied live. Gated by the persisted
   `onboarding.completed` flag so it never re-runs after an update, and can be
   re-opened from Settings.

See [onboarding.md](onboarding.md) and `installer/docs/FIRST_RUN.md`.

## 8. Directory layout (packaging source)

```
installer/
├── common/     version.json + JSON schemas (single source of truth)
├── assets/     SVG brand sources + generated icons (no binaries)
├── windows/    Inno Setup script + Includes + portable build + package-manager manifests
├── macos/      DMG styling + notarization
├── linux/      appimage/ deb/ rpm/ flatpak/ snap/ arch/ builders
├── scripts/    stage-artifacts, verify-artifacts, bump-version, generate-assets, make-update-manifest
├── templates/  release notes + update feed examples
└── docs/       ARCHITECTURE · BUILD · SIGNING · RELEASE · UPDATES · FIRST_RUN
```

## Technology decisions (why)

Every packaging choice is justified against its alternatives in
`installer/docs/ARCHITECTURE.md` (Inno Setup vs WiX/NSIS/Qt IFW; styled DMG vs
installer wizard; AppImage+Flatpak+deb+rpm vs a single "Linux installer").
Rule: **do not add a packaging framework just because it exists** — each format
must earn its maintenance cost.
