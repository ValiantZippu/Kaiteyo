# Kaiteyo Installer — Architecture & Technology Decisions

This document records **why** each platform uses the installer technology it does.
It is the "Research First" deliverable: a written justification for every choice,
plus the constraints we deliberately respect (rather than fight).

---

## 1. Guiding principles

| Principle | Consequence |
|-----------|-------------|
| Platform-native wins | We pick the best tool *per OS*, not one universal tool. |
| Don't fight the OS | Gatekeeper, UAC, package managers and sandboxes are respected and designed around. |
| User data is sacred | No installer/uninstaller/updater deletes study data, settings or databases without explicit confirmation. |
| Honest progress | Progress UI reflects real packaging steps, never fabricated percentages. |
| Decoupled from Gradle | Everything here consumes `jpackage`/Compose app bundles. The subsystem is testable without a build. |

---

## 2. Windows — Inno Setup 6

**Decision: Inno Setup 6.4+ (script: `windows/kaiteyo.iss`), replacing the Compose
MSI as the *primary* consumer installer. MSI stays as a secondary/enterprise artifact.**

### Why Inno Setup over the alternatives

| Tool | Assessment |
|------|-----------|
| **Inno Setup 6** | **Chosen.** Native `WizardStyle=modern dynamic includetitlebar` gives a clean light/dark-adaptive wizard with no third-party skinning. Pascal scripting covers every feature we need (remember last dir, detect existing install, upgrade/repair/modify, custom uninstaller pages). Mature, actively maintained, open source (free, but not OSI-GPL — acceptable). |
| WiX v4/v5 | Only needed when an enterprise must deploy via Group Policy / MSI. We already *emit* an MSI from the Compose plugin for that audience; writing a second MSI with a Burn bootstrapper theme in WPF is high cost for zero consumer benefit. |
| NSIS | Lightweight and scriptable, but the default UI is dated; modern dark styling requires brittle community plugins (Modern UI 2, NsCDE). Inno 6's built-in `WizardStyle` is strictly better for a branded look. |
| Qt Installer Framework | Built for Qt apps; heavy (needs Qt runtime for the installer itself), overkill for a JVM app. |
| Custom bootstrapper | We keep a **thin bootstrapper** concept: Inno runs the silent MSI when "manage via Programs & Features / enterprise" is selected, so one download covers both consumer and enterprise users. |

### What the Inno installer implements

- Modern welcome page, Kaiteyo branding, dark-mode aware (system-driven)
- DPI-aware (Inno `PrivilegesRequiredOverridesAllowed`, `ArchitecturesAllowed=x64compatible`,
  and the modern wizard scales correctly with Windows display scaling)
- Smooth page transitions (modern wizard), custom progress page driven by a real
  task list
- Install / Upgrade (same `AppId`, remembers previous directory) / Repair / Modify
  (Inno's built-in modes) / Uninstall (custom keep-or-remove data page)
- Silent install: `/VERYSILENT /SUPPRESSMSGBOXES`; all tasks individually selectable
- Tasks: desktop shortcut, Start Menu folder, `Kaiteyo:` file association (future-ready),
  auto-update opt-in, dictionary starter pack
- Start Menu + Desktop shortcuts, launch-after-install checkbox
- Error pages: insufficient rights (re-launch elevated), disk space check, corrupt
  payload (checksum verify), existing installation conflict (upgrade vs. separate)

### Upgrade & data preservation on Windows

- App code lives in `{app}`; user data lives in `{userdata}\Kaiteyo` (Local AppData).
  The two are **never** co-located, so upgrades never touch user content.
- `AppId` + `AppVerName` drive upgrade detection; `CheckForMMX`-style Pascal code
  compares installed version vs. incoming and offers upgrade.
- Uninstaller explicitly asks: *Keep my study data* (default) vs *Remove everything*,
  with a plain-language list of what each choice deletes.

---

## 3. macOS — styled DMG + notarization

**Decision: keep the Compose/jpackage `.app` bundle, wrap it in a re-styled DMG with
drag-to-Applications, and make code-signing + notarization a first-class pipeline.**

### Rationale

- macOS users expect `.app` + drag-to-Applications. Fighting this convention (e.g.
  an installer-wizard package) would be wrong and would trip Gatekeeper.
- `create-dmg` handles window geometry, custom background artwork, icon placement
  and the Applications symlink via AppleScript in one shell call. `dmgbuild` is the
  declarative alternative; we use `create-dmg` because it requires no Python env
  on CI runners beyond Homebrew.
- The jpackage `packageDmg` output already contains a working `.app`; `build-dmg.sh`
  re-mounts it, drops in branded background + volume icon, and re-burns the DMG.

### Signing & notarization (respecting Gatekeeper)

- Every binary in the `.app` (including bundled JVM `*.dylib`s) is signed with
  **Hardened Runtime** (`--options runtime`) and a timestamp.
- JVM entitlements: `com.apple.security.cs.allow-jit` and
  `allow-unsigned-executable-memory` are required for JIT; `disable-library-validation`
  covers JNA/JNI loading (see `entitlements.plist`).
- Notarization via `xcrun notarytool submit --wait`, then `stapler staple`.
- Universal architecture is prepared: we publish `macos-arm` and `macos-intel`
  DMGs; a universal build is possible (`xarch` fat binaries) but doubles signing
  time — kept as an optional CI job.

### First-run guidance

- The app shows its own onboarding wizard on first launch (see `docs/FIRST_RUN.md`).
- The DMG background includes a short "Drag Kaiteyo to Applications" hint as part
  of the artwork, so Gatekeeper's unsigned-prompt copy is the only OS text the
  user sees.

---

## 4. Linux — AppImage + Flatpak + deb + rpm

**Decision: ship all four, do not force one format.** Users of a rolling distro get
the AppImage; users of a store get Flatpak; users of apt/dnf get native packages.

| Format | Role | Notes |
|--------|------|-------|
| **AppImage** | Portable single-file | Primary "download from GitHub" artifact. Needs desktop integration via AppImageLauncher; we ship correct `.desktop` + AppStream metadata + `icon-theme-package` sized icons so integration is seamless. |
| **Flatpak** | Store/sandboxed | Flathub-quality metadata (`metainfo.xml` with screenshots + releases), strict sandbox, Freedesktop 22.08 runtime. Best long-term store story. |
| **deb** | Debian/Ubuntu | Built from the jpackage image with proper `usr/share/...` layout, `postinst`/`prerm` hooks (desktop database, icon cache). |
| **rpm** | Fedora/RHEL | rpmbuild spec with equivalent hooks. |
| **Snap** | Optional | Snapcraft manifest already exists; kept as optional, marked *not* in the default release set until store review passes. |

### Linux metadata (AppStream)

All formats embed `io.github.syt0r.kaiteyo.metainfo.xml`: `<id>`, screenshots,
release notes with dates, `content_rating`, branding colors. Icons ship as SVG +
multi-resolution PNGs under `/usr/share/icons/hicolor/…` so every desktop shows the
same artwork.

### Upgrade paths

- AppImage: self-update friendly — the update manifest points at the next AppImage.
- deb/rpm: apt/dnf resolve upgrades natively.
- Flatpak: `flatpak update` or Flathub auto-updates.

---

## 5. First-run experience

Two distinct phases, kept separate by design:

1. **Installer phase** (per-OS tool): location, components, shortcuts, launch checkbox.
2. **App phase** (Compose `OnboardingWizard`, shown once): theme, accent, scaling,
   font size, navigation layout and motion — applied live to the real theme state.

The app phase is gated by `AppState.onboardingCompleted`, backed by the settings
key `onboarding.completed` (SettingsEngine). It shows exactly once regardless of
how the app was installed, and can be re-opened from Settings → "Show onboarding
again" (`AppState.requestOnboarding()`). All steps are skippable.
See `docs/FIRST_RUN.md`.

---

## 6. Auto-update architecture (future, designed now)

Interfaces live in the desktop app (`desktop/engine/updates/`); the feed format is
`common/update-manifest.schema.json`.

- **Channels**: `stable` (default), `beta`, `nightly` — one manifest file per channel.
- **Checking**: fetch manifest over HTTPS; compare `version_code`.
- **Integrity**: every artifact carries `sha256`; verified before any swap.
- **Rollback**: the previous app version is kept alongside until the new one has
  launched successfully once (`first-run` marker per install dir).
- **Applying**: Windows — silent EXE upgrade via the existing Inno installer;
  macOS — replace `.app` (signed+notarized bundles only); Linux — format-native
  (AppImage swap, Flatpak update, package manager).

The app only ever *writes* to its own data dir; updates never touch
`~/.kaiteyo` (study data) or settings files.

---

## 7. Directory layout

```
installer/
├── common/        version.json + JSON schemas
├── assets/        SVG sources + generated png/bmp/icns/ico
├── windows/       Inno Setup script + Includes + portable build
├── macos/         DMG styling + notarization
├── linux/         appimage/ deb/ rpm/ flatpak/ snap/ builders
├── scripts/       stage-artifacts, generate-assets, bump-version, make-update-manifest
├── templates/     release notes + update feed examples
└── docs/          this file + BUILD/SIGNING/RELEASE/UPDATES/FIRST_RUN
```

All scripts are POSIX-bash (Windows CI uses PowerShell only for the Inno wrapper);
paths are relative to the repo root and never assume a specific checkout location.
