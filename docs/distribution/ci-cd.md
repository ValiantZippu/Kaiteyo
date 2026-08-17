# CI/CD — GitHub Actions

What CI builds, validates, and publishes. Architecture detail:
`docs/architecture/ci-cd.md`.

## Workflows

| Workflow | Trigger | Job |
|---|---|---|
| `build-all.yml` | `workflow_call` / `workflow_dispatch` | Builds every platform in parallel |
| `build-release.yml` | tag `v*.*` | Calls `build-all`, stages, verifies, publishes release + feeds |

## build-all matrix

| Job | Runner | Artifacts |
|---|---|---|
| Android + desktop Linux | ubuntu-latest | `app:assembleFdroidRelease` APK, AppImage, deb, rpm |
| Windows | windows-latest | Inno EXE, MSI, portable ZIP |
| macOS Intel | macos-13 | styled signed+notarized DMG (`x64`) |
| macOS ARM | macos-15 | styled signed+notarized DMG (`arm64`) |

> **Windows arm64 and Linux aarch64 are NOT in the default matrix.** The
> download page and platform docs must not claim architectures CI does not
> build (see [windows.md](windows.md)). Adding them means adding real runners +
> real artifacts + clean-machine tests.

## What every release job verifies (built into the pipeline)

- Artifact exists
- Artifact size is reasonable (staged manifest)
- Version is correct (names derived from `version.json`)
- Application launches where feasible (smoke tests on clean runners)
- Checksums generated + verified (`verify-artifacts.sh` — mismatch fails the job)
- Package metadata correct (desktop/metainfo blocks, control/spec files)

## Release job sequence

```
tag vX.Y.Z
  → build-all (parallel)
  → stage-and-verify:
       download artifacts
       stage-artifacts.sh   (canonical names + sha256 manifest)
       verify-artifacts.sh  (integrity gate — fail = no release)
       make-update-manifest.sh × stable/beta/nightly (from verified manifest)
       upload staged release + feeds
  → create-release:
       publish GitHub release (artifacts + manifest + feeds)
       publish update-feed release (channel feeds, prerelease)
```

## Secrets

Signing credentials are GitHub Actions secrets only
(`WINDOWS_CERT_*`, `APPLE_*`, `CODESIGN_IDENTITY`, `KEYSTORE_BASE64`,
`KEYSTORE_PASS`, `SIGN_KEY`, `SIGN_PASS`). The repo contains no secrets —
see [signing.md](signing.md).

## Reproducibility

- JDK 17 Temurin pinned via `actions/setup-java`; Gradle dependency cache.
- Packaging tools installed per job (`inno-setup`, `imagemagick`,
  `librsvg2-bin`, `rpm`, `create-dmg`).
- Build environment is documented in `docs/architecture/toolchain.md` and
  [troubleshooting.md](troubleshooting.md) (reproducing a failed build).

## Testing packaging in CI

- Clean-machine smoke: Ubuntu runner installs the deb and runs the AppImage;
  Windows runner builds and can install the EXE; macOS runners assess `spctl`.
- Full clean-machine install/upgrade/uninstall test suites are the next step
  (see [troubleshooting.md](troubleshooting.md) and the release checklist).

## How to trigger

- Release: push tag `vX.Y.Z`.
- Manual full build: Actions → "Build All" → Run workflow.
- Nightly: `workflow_dispatch` with the nightly channel (feeds generated for
  the nightly channel) — the app labels these clearly and never serves them to
  stable users.
