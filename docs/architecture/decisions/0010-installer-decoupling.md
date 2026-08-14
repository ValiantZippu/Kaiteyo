# ADR-0010: Installer Subsystem Decoupled from the Gradle Build

**Status**: Accepted

## Context

Kaiteyo wants a premium, branded installation experience (wizard, signing, update feeds)
rather than raw `jpackage` output — but keeping installer logic inside the Gradle build
ties packaging to the build system and makes iteration slow.

## Decision

- Build plain app bundles with Gradle (`:desktopApp:createDistributable` via the Compose
  plugin) and let everything that **brands, packages, and distributes** those bundles live
  in a standalone `installer/` subsystem (scripts + configs, no Gradle).
- Single source of truth: `installer/common/version.json` drives all scripts (kept in sync
  with `buildSrc/.../AppVersion.kt`).
- Platform-native where it wins: Inno Setup on Windows, DMG + notarization on macOS,
  native formats on Linux — no forced one-size-fits-all installer.
- Integrity: canonical artifact naming, sha256 manifests, staging + verification gates,
  and generated update feeds (stable/beta/nightly) with JSON schemas.
- Never touch user data: installers/uninstallers only remove data after explicit,
  labelled confirmation.

## Alternatives

- jpackage-only packaging — rejected: no branding, no wizard, no update feeds.
- Installer logic inside Gradle tasks — rejected: couples packaging to the build, hard to
  iterate and test.

## Consequences

- Packaging can be developed, tested, and scripted independently of the Kotlin build.
- CI (`build-all.yml` / `build-release.yml`) orchestrates the scripts per OS.
- More moving parts to learn, but each is small and testable.

## Implementation notes

- `installer/README.md` + `installer/docs/{ARCHITECTURE,BUILD,SIGNING,RELEASE,UPDATES,FIRST_RUN}.md`
- `installer/scripts/` (stage-artifacts, verify-artifacts, bump-version,
  make-update-manifest, generate-assets)
- See `docs/releases/RELEASE_PROCESS.md`.
