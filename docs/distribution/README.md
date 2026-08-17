# Kaiteyo — Distribution & Packaging

> How Kaiteyo gets from source to a downloaded, installed, updated, uninstalled
> product on **Windows**, **Linux** and **Android** — with a coherent Kaiteyo
> experience at every step.

This is the **index** for the distribution documentation set. The packaging
**source** (scripts, manifests, installer definitions) lives in `installer/`;
this set explains *what* exists, *why* it exists, and *how* it behaves.

## The package matrix

| Platform | Architecture | Format | Install | Update | Uninstall | Signing | Status |
|---|---|---|---|---|---|---|---|
| Windows | x64 | **EXE** (Inno Setup) | Installer wizard | Inno silent upgrade / update feed | Inno uninstaller (data-preserving) | signtool (CI-gated) | ✅ built |
| Windows | x64 | MSI (jpackage) | Programs & Features / enterprise | MSI upgrade | MSI uninstall | signtool (CI-gated) | ✅ built |
| Windows | x64 | portable ZIP | Extract & run | Replace files | Delete folder | — | ✅ built |
| Windows | x64 | **WinGet** manifest | `winget install` | `winget upgrade` | `winget uninstall` | — | 🚧 manifest (unpublished) |
| Windows | x64 | **Chocolatey** package | `choco install` | `choco upgrade` | `choco uninstall` | — | 🚧 manifest (unpublished) |
| Windows | x64 | **Scoop** manifest | `scoop install` | `scoop update` | `scoop uninstall` | — | 🚧 manifest (unpublished) |
| Windows | arm64 | — | — | — | — | — | ❌ not built (do not claim) |
| Linux | x86_64 | **AppImage** | Download, `chmod +x`, run | AppImage swap | Delete file | sha256 manifest | ✅ built |
| Linux | x86_64 | **deb** (Debian/Ubuntu) | `apt` / software center | `apt upgrade` | `apt remove` | sha256 manifest | ✅ built |
| Linux | x86_64 | **rpm** (Fedora/RHEL) | `dnf` / software center | `dnf upgrade` | `dnf remove` | sha256 manifest | ✅ built |
| Linux | x86_64 | **Flatpak** | `flatpak install` / Flathub | `flatpak update` | `flatpak uninstall` | Flathub key | 🚧 manifest (Flathub PR pending) |
| Linux | x86_64 | **PKGBUILD** (Arch/AUR) | `makepkg` / AUR helper | package manager | package manager | sha256 sums | 🚧 added (not yet published) |
| Linux | x86_64 | Snap | `snap install` | `snap refresh` | `snap remove` | Snapcraft | ⚪ optional (not default) |
| Android | arm64 / armv7 / x86_64 | **APK** (F-Droid flavor) | Sideload / F-Droid | reinstall / F-Droid | launcher | F-Droid reproducible | ✅ built (CI) |
| Android | all | **AAB** (googlePlay flavor) | Play Store | Play Store | Play Store | Play signing | ✅ configured |

Legend: ✅ built and CI-tested · 🚧 defined, needs publish step · ⚪ optional/designed · ❌ explicitly unsupported.
**Only formats that are actually built are marked ✅.** Everything else is honest.

## Documentation set

| Doc | What it covers |
|---|---|
| [architecture.md](architecture.md) | The full distribution architecture (build → package → install → update → sign → release) |
| [versioning.md](versioning.md) | Semantic versioning + the single source of truth |
| [windows.md](windows.md) | Windows: EXE/MSI/portable, silent install, shortcuts, uninstall, package managers |
| [linux.md](linux.md) | Linux overview and per-format guides |
| [linux-appimage.md](linux-appimage.md) | AppImage: build, run, integrate, update |
| [linux-debian.md](linux-debian.md) | deb package for Debian/Ubuntu |
| [linux-ubuntu.md](linux-ubuntu.md) | Ubuntu-specific notes (software center, PPA-free) |
| [linux-fedora.md](linux-fedora.md) | rpm package for Fedora/RHEL |
| [linux-flatpak.md](linux-flatpak.md) | Flatpak: sandbox, manifest, Flathub |
| [linux-arch.md](linux-arch.md) | Arch Linux: PKGBUILD + AUR path |
| [android.md](android.md) | Android: flavors, APK/AAB, signing, Play/F-Droid |
| [installers.md](installers.md) | Installer behavior: silent flags, errors, launch handoff, interrupted installs |
| [uninstall.md](uninstall.md) | Uninstall semantics + user-data preservation rules |
| [signing.md](signing.md) | Code signing, notarization, secrets handling |
| [updates.md](updates.md) | Auto-update architecture, channels, integrity, rollback |
| [checksums.md](checksums.md) | sha256 verification of every artifact |
| [release-process.md](release-process.md) | End-to-end release workflow (links `docs/releases/`) |
| [ci-cd.md](ci-cd.md) | CI/CD: what GitHub Actions builds, validates and publishes |
| [security.md](security.md) | Security model: signing, provenance, update verification, secrets |
| [onboarding.md](onboarding.md) | First-run onboarding architecture |
| [first-launch.md](first-launch.md) | What happens on first launch, per platform |
| [troubleshooting.md](troubleshooting.md) | Common install/update/uninstall failures |
| [artifacts.md](artifacts.md) | Artifact naming, manifest format, verification |
| [faq.md](faq.md) | User-facing distribution questions |

## Authoritative sources (do not duplicate)

| Concern | Lives in |
|---|---|
| Installer scripts & definitions | `installer/` (see `installer/README.md`) |
| Version | `installer/common/version.json` + `buildSrc/src/main/kotlin/AppVersion.kt` |
| Release workflow | `docs/releases/RELEASE_PROCESS.md` + `docs/releases/RELEASE_CHECKLIST.md` |
| Installer architecture decisions | `installer/docs/ARCHITECTURE.md` |
| Build commands | `installer/docs/BUILD.md` + `docs/development/COMMANDS.md` |
| Signing details | `installer/docs/SIGNING.md` |
| Update feeds & app updater | `installer/docs/UPDATES.md` |
| First-run / onboarding | `installer/docs/FIRST_RUN.md` + `docs/distribution/onboarding.md` |
| Per-platform behavior | `docs/platform/` (WINDOWS.md, LINUX.md, ANDROID.md, …) |

## Ground rules

1. **One source of truth** for version numbers — never hardcode a version in a
   packaging file (see [versioning.md](versioning.md)).
2. **Never delete user data** — no installer/uninstaller/updater removes study
   data, settings or databases without explicit, labelled confirmation
   (see [uninstall.md](uninstall.md)).
3. **Never claim what isn't built** — formats are marked built only after CI
   produces and verifies them (see the matrix above).
4. **Secrets never enter the repo** — certificates and passwords come from CI
   secrets or developer environment (see [signing.md](signing.md)).
5. **Documentation reflects reality** — when a doc disagrees with the source,
   the source wins; report the contradiction.
