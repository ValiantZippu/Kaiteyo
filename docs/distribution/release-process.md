# Release Process

The authoritative release workflow is **`docs/releases/RELEASE_PROCESS.md`** +
**`docs/releases/RELEASE_CHECKLIST.md`**. This page is the distribution-level
summary and the pointer to the operational details.

## The one source of truth

```
installer/common/version.json  +  buildSrc/src/main/kotlin/AppVersion.kt
```

Bump both together, never apart:

```bash
bash installer/scripts/bump-version.sh 2.3.0 2310
```

## Pipeline

```
commit → build → test → package → sign → checksum → validate → release
        → package repositories → download metadata (update feeds)
```

1. **Prepare** — `release/vX.Y.Z` branch, `bump-version.sh`, metainfo
   `<releases>` block, changelog.
2. **Verify** — `:core:allTests`, `:desktopApp:compileKotlinJvm`, at least one
   platform smoke-tested per `installer/docs/BUILD.md`.
3. **Tag** — `vX.Y.Z` → `build-release.yml` triggers `build-all.yml` (all
   platforms in parallel) → staging job renames artifacts canonically →
   `stage-artifacts.sh` computes the sha256 manifest → `verify-artifacts.sh`
   **fails the release on any mismatch** → release published with manifest →
   update feeds regenerated for all three channels and published to the
   `update-feed` release.
4. **Publish** — release notes from `installer/templates/RELEASE_NOTES.md`,
   Flathub PR (when publishing there), merge `release/` → `main` → `develop`.

## Release types

| Type | Branch | Version | Audience |
|---|---|---|---|
| Development | `develop` | `{version}-dev.{n}` | internal |
| Release candidate | `release/v{version}` | `{version}-rc.{n}` | QA |
| Stable | tag `v{version}` | `{version}` | everyone |
| Hotfix | from `main` | `{version}` | production fixes |

## Artifact set per release

| Platform | Artifacts |
|---|---|
| Windows | Inno EXE · MSI · portable ZIP (+ WinGet/Choco/Scoop manifests) |
| macOS | styled DMG arm64 + x64 (signed + notarized when secrets configured) |
| Linux | AppImage · deb · rpm (+ Flatpak/Snap/PKGBUILD when published) |
| Android | APK (fdroid, what CI ships) · AAB (Play, googlePlay flavor) |

See [artifacts.md](artifacts.md) for naming.

## Failed release recovery

- **Artifact failure** — fix, rebuild, re-stage; verification must pass before
  any publish.
- **Signing failure** — release ships unsigned with an explicit release-note
  callout (never mislabeled as signed); fix secrets, retry.
- **Bad release** — GitHub releases are immutable; publish a hotfix tag. The
  `update-feed` release is regenerated on the next tagged release; rollback to
  the previous tag is always possible (previous artifacts remain downloadable).
- Do not improvise mid-emergency — the checklist in
  `docs/releases/RELEASE_CHECKLIST.md` is the runbook.

## CI/CD

See [ci-cd.md](ci-cd.md) for what GitHub Actions builds, and
`docs/architecture/ci-cd.md` for the architecture.
