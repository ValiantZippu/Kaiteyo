# Packaging Source Tree

This index maps the **packaging source** (`installer/`) to the documentation
that explains it. The docs themselves live in `docs/distribution/`.

## Source layout

```
installer/
├── common/          ← SINGLE SOURCE OF TRUTH
│   ├── version.json                version, version_code, channel, app_id
│   ├── artifact-manifest.schema.json
│   └── update-manifest.schema.json
├── assets/          ← brand artwork sources (SVG) + generated icons (no binaries)
├── windows/
│   ├── kaiteyo.iss                 Inno Setup 6 installer
│   ├── Includes/                   branding, tasks, Pascal logic, uninstaller, languages
│   ├── build.ps1 / build.bat       EXE build wrapper
│   ├── portable/                   portable ZIP build + launcher
│   └── packaging/                  WinGet / Chocolatey / Scoop manifests
├── macos/
│   ├── build-dmg.sh                styled DMG builder
│   ├── notarize.sh                 signing + notarization + stapling
│   └── entitlements.plist
├── linux/
│   ├── appimage/                   AppDir + appimagetool wrapper + metainfo/desktop
│   ├── deb/                        dpkg-deb builder
│   ├── rpm/                        rpmbuild wrapper + build.spec
│   ├── flatpak/                    Flatpak manifest + builder
│   ├── snap/                       Snapcraft (optional)
│   └── arch/                       PKGBUILD (Arch/AUR)
├── scripts/
│   ├── bump-version.sh             bump version.json + AppVersion.kt together
│   ├── generate-assets.sh          SVG → png/bmp/icns/ico
│   ├── stage-artifacts.sh          canonical names + sha256 manifest
│   ├── verify-artifacts.sh         integrity gate (fail = no release)
│   └── make-update-manifest.sh     update feeds per channel
├── templates/       release notes + update feed examples
└── docs/            ARCHITECTURE · BUILD · SIGNING · RELEASE · UPDATES · FIRST_RUN
```

## Mapping: source → doc

| You want to know… | Read |
|---|---|
| Why each technology was chosen | `docs/distribution/architecture.md` + `installer/docs/ARCHITECTURE.md` |
| How to build each format | `installer/docs/BUILD.md` + per-platform docs in `docs/distribution/` |
| How the version propagates | `docs/distribution/versioning.md` |
| What each format does / doesn't support | `docs/distribution/README.md` (matrix) |
| Signing / secrets | `docs/distribution/signing.md` + `installer/docs/SIGNING.md` |
| Updates / channels / rollback | `docs/distribution/updates.md` + `installer/docs/UPDATES.md` |
| Uninstall / data preservation | `docs/distribution/uninstall.md` |
| Release workflow | `docs/releases/RELEASE_PROCESS.md` |
| CI/CD | `docs/distribution/ci-cd.md` + `docs/architecture/ci-cd.md` |
| Troubleshooting | `docs/distribution/troubleshooting.md` |

## Windows

- `installer/windows/` — see `docs/distribution/windows.md`
- Package-manager manifests — see `docs/distribution/windows-package-managers.md`

## Linux

- `installer/linux/` — see `docs/distribution/linux.md`
- AppImage — `docs/distribution/linux-appimage.md`
- deb — `docs/distribution/linux-debian.md` · Ubuntu — `linux-ubuntu.md`
- rpm — `docs/distribution/linux-fedora.md`
- Flatpak — `docs/distribution/linux-flatpak.md`
- Arch — `docs/distribution/linux-arch.md`

## Android

- `app/` (Gradle) — see `docs/distribution/android.md`

## Rules

1. Version numbers come from `installer/common/version.json` only — no
   hardcoding anywhere in this tree.
2. User data is never touched by anything in this tree except explicit
   uninstall choices.
3. Every format earns its place: formats that aren't built aren't documented as
   supported (see the honest matrix in `docs/distribution/README.md`).
