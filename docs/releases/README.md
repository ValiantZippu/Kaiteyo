# Releases

This section documents how Kaiteyo is versioned, built, packaged, and published.

| Document | Purpose |
|----------|---------|
| [`RELEASE_PROCESS.md`](RELEASE_PROCESS.md) | The authoritative end-to-end release workflow (versioning, branches, CI, artifact naming, channels, signing) |
| [`RELEASE_CHECKLIST.md`](RELEASE_CHECKLIST.md) | Pre-release verification checklist |

## Quick orientation

- **Single source of truth for versions:** `installer/common/version.json` and
  `buildSrc/src/main/kotlin/AppVersion.kt` — bump them together with
  `installer/scripts/bump-version.sh`.
- **CI:** pushing a `v*` tag triggers `.github/workflows/build-release.yml` →
  `build-all.yml`, which builds Android + all desktop packages on native runners, then
  stages, verifies (sha256), and publishes.
- **Installer subsystem:** everything that turns the jpackage bundle into branded
  packages lives in `installer/` (see `installer/README.md` and `installer/docs/`).
- **Changelog:** root [`CHANGELOG.md`](../../CHANGELOG.md).
