# Kaiteyo — Installer & First-Run Subsystem

Everything that turns a `jpackage`-produced app bundle into a **premium, branded,
production-ready installation experience** for Windows, macOS and Linux.

This subsystem is **deliberately decoupled from the Gradle build**. The Compose
plugin produces plain app bundles (`desktopApp:createDistributable`); everything
in this directory wraps, brands, packages and distributes those bundles.

```
installer/
├── README.md                ← you are here
├── common/                  ← shared metadata & JSON schemas (single source of truth)
│   ├── version.json
│   ├── update-manifest.schema.json
│   └── artifact-manifest.schema.json
├── assets/                  ← brand artwork + generators (SVG sources, no binaries)
├── windows/                 ← Inno Setup 6 premium installer + portable build + package-manager manifests
├── macos/                   ← styled DMG builder + notarization pipeline
├── linux/                   ← AppImage, deb, rpm, Flatpak, Snap, Arch (PKGBUILD)
├── scripts/                 ← cross-platform helper scripts (staging, versions, updates)
├── templates/               ← release notes + update feed templates
└── docs/                    ← ARCHITECTURE, BUILD, SIGNING, RELEASE, UPDATES, FIRST_RUN
```

> Distribution-level docs (what each format is for, install/update/uninstall
> behavior, package managers, Arch) live in `docs/distribution/`; the source
> tree map is `docs/packaging/README.md`.

## Design principles

1. **Platform-native where it wins.** Inno Setup on Windows, DMG + notarization on
   macOS, native package formats on Linux. No forced one-size-fits-all cross-platform
   installer. Rationale in `docs/ARCHITECTURE.md`.
2. **Never touch user data.** Every installer/uninstaller/updater treats study data,
   settings and databases as sacred. Data is only ever removed after explicit,
   labelled confirmation.
3. **Single source of truth.** `common/version.json` drives every script. Release
   engineering never hard-codes versions in five places.
4. **Real progress, not fake.** Installer progress reflects actual steps from the
   packaging manifest.
5. **Future-ready but honest.** The auto-update *architecture* ships now (interfaces,
   manifest schema, checker); downloading/swapping is designed but behind a flag.

## Quick start

```bash
# 0. Produce the app bundles (Gradle side, see desktopApp/build.gradle.kts)
./gradlew :desktopApp:createDistributable

# 1. Generate branded artwork (svg → png/bmp/icns/ico)
bash installer/scripts/generate-assets.sh

# 2. Build the Windows premium installer (requires Inno Setup 6.4+)
powershell -File installer/windows/build.ps1 -Version 2.2.1

# 3. Build the styled macOS DMG (macOS host only)
bash installer/macos/build-dmg.sh

# 4. Build Linux packages
bash installer/linux/appimage/build.sh
bash installer/linux/deb/build.sh
bash installer/linux/rpm/build.sh

# 5. Stage everything into release/ with canonical names + checksums
bash installer/scripts/stage-artifacts.sh 2.2.1
```

See `docs/BUILD.md` for the full per-platform build guide and `docs/RELEASE.md`
for the end-to-end release workflow.

## Layout map

| Path | Purpose |
|------|---------|
| `common/version.json` | Version, app id, channel — consumed by every script and CI |
| `windows/kaiteyo.iss` | The Inno Setup installer (welcome → options → progress → done) |
| `windows/Includes/*.iss` | Branding, tasks, Pascal logic, uninstaller, languages |
| `windows/portable/` | Portable (zip) build + launcher |
| `macos/build-dmg.sh` | Re-styles the jpackage DMG with branded background + drag-to-apps |
| `macos/notarize.sh` | Hardened-runtime signing, notarization, stapling |
| `linux/*/build.sh` | AppImage / deb / rpm / Flatpak / Snap builders |
| `scripts/` | generate-assets, stage-artifacts, bump-version, make-update-manifest, verify |
| `docs/` | Architecture decisions, build/sign/release/update/first-run guides |

## License & branding

The installer is part of Kaiteyo (GPL-3.0). Brand artwork follows
`docs/branding/BRAND_GUIDELINES.md` — colors, mark usage and typography rules apply
to every installer surface exactly as they do in the app.
