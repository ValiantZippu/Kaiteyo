# Auto-Update Architecture

> Status: **wired into the desktop app**. Settings → Updates exposes channel
> selection, a check-for-updates action, a download/install flow and an opt-in
> startup check. The Windows apply path (Inno `/VERYSILENT` upgrade) is live;
> the macOS/Linux apply paths are designed but disabled.

## Channels

Three channels, one manifest file per channel:

| Channel | Feed file | Who gets it | Notes |
|---------|-----------|-------------|-------|
| `stable` | `update-stable.json` | everyone (default) | production-safe builds only |
| `beta` | `update-beta.json` | opt-in in Settings | feature previews |
| `nightly` | `update-nightly.json` | opt-in, dev builds | built from `develop`, may break |

The user's channel preference is stored in app preferences; switching channels
never downgrades below the current version automatically.

## Feed format

`installer/common/update-manifest.schema.json`. Generated from a staged release:

```bash
bash installer/scripts/make-update-manifest.sh 2.2.1 stable \
  "https://github.com/ValiantZippu/Kaiteyo/releases/download/update-feed"
```

Every artifact entry carries `url`, `sha256`, `size_bytes`, `arch` — integrity
is verified **before** anything is written to disk.

## Feed hosting

All three channel feeds are published to a single lightweight **`update-feed`**
release (a prerelease, so GitHub's `releases/latest` keeps pointing at real
versions). CI refreshes it on every tagged release, which means:

- the app always fetches `releases/download/update-feed/update-<channel>.json`;
- each channel sees the newest feed regardless of whether the latest release is
  a stable, beta or nightly;
- `releases/latest` is **never** used for feeds — it only resolves to the newest
  *stable* release, which would make beta/nightly checks meaningless.

The generated feeds are also attached to each tagged release (`**/update-*.json`)
so every release stays self-contained.

## Application flow (per platform)

```
UpdateService.check()                     # fetch channel manifest, compare version_code
  └─ newer available? ── no ──> idle
  └─ yes
      UpdateDownloader.download(artifact) # to data dir, stream + sha256 verify
      UpdateInstaller.apply(package)
        ├─ Windows  → launch Kaiteyo-Setup-<v>.exe /VERYSILENT (Inno upgrade path)
        ├─ macOS    → swap Contents inside the running .app (signed bundles only)
        └─ Linux    → AppImage: swap file + relaunch; deb/rpm/flatpak: delegate to package manager
      Rollback guard
        └─ previous version kept until new version's first clean launch
```

## Kotlin interfaces (`desktop/engine/updates/`)

| Type | Responsibility |
|------|----------------|
| `UpdateChannel` | stable / beta / nightly + feed URL |
| `UpdateManifest` | parsed feed (kotlinx.serialization, schema v1) |
| `UpdateChecker` | fetch + version comparison |
| `UpdateDownloader` | streaming download with sha256 |
| `UpdateInstaller` | platform apply + relaunch |
| `UpdateService` | coordinator exposing `StateFlow<UpdateState>` |
| `UpdatePolicy` | channel selection, min-version guard, rollback window |

The concrete `UpdateInstaller` for each OS is a thin wrapper over the format's
native mechanism — we deliberately reuse the installer architecture (Inno upgrade
path, package manager) rather than inventing a parallel file-swap mechanism.

## Rollback

- Before applying, the current version's `Contents`/install dir is copied to a
  `previous/` sibling (Windows: the MSI/inno installer retains the prior version
  marker in the registry).
- A marker file `update-applied-<version>.json` is written on first successful
  launch of the new version. If the marker is absent after N days, the app offers
  to roll back.
- Rollback never touches user data (the data dir is shared and version-agnostic).

## Never

- Never overwrite `~/.kaiteyo` or settings during an update.
- Never auto-switch channels.
- Never install from a manifest served over plain HTTP.
- Never show fake progress in the update UI.
