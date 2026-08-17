# Artifacts — Naming, Manifest, Layout

## Canonical naming

`Kaiteyo-{version}-{platform}.{ext}` — examples (version 2.3.0):

```
Kaiteyo-2.3.0-windows-setup.exe      # Inno installer
Kaiteyo-2.3.0-windows.msi
Kaiteyo-2.3.0-windows-portable.zip
Kaiteyo-2.3.0-macos-arm64.dmg
Kaiteyo-2.3.0-macos-x64.dmg
Kaiteyo-2.3.0-linux-x86_64.AppImage
Kaiteyo-2.3.0-linux-amd64.deb
Kaiteyo-2.3.0-linux-x86_64.rpm
Kaiteyo-2.3.0-linux.flatpak
Kaiteyo-2.3.0-android.apk
```

Names are produced by `stage-artifacts.sh` from `version.json` — never
hand-written per release.

## Staged release layout

```
release/kaiteyo-2.3.0/
├── Kaiteyo-2.3.0-<platform>.<ext>...   # every artifact
├── artifact-manifest.json              # name · size · sha256 · arch · version
└── (upstream CI also publishes update-<channel>.json feeds)
```

## Manifest format

`artifact-manifest.json` (schema: `installer/common/artifact-manifest.schema.json`):

```json
{
  "version": "2.3.0",
  "channel": "stable",
  "artifacts": [
    {
      "name": "Kaiteyo-2.3.0-windows-setup.exe",
      "platform": "windows",
      "arch": "x64",
      "size_bytes": 48212345,
      "sha256": "9f2c…"
    }
  ]
}
```

`update-<channel>.json` (schema: `installer/common/update-manifest.schema.json`)
carries the same integrity fields plus `url` and `release_date` so the desktop
updater can discover, download and verify without any other input.

## Verification

- `verify-artifacts.sh` re-checks every artifact against the manifest —
  missing/corrupt/renamed artifacts fail the release.
- The update feed is built **from** the verified manifest, so hashes can never
  drift between the release and the feed.

## What is never released

- Debug APKs (`.dev` suffix builds) — development only.
- Unsigned EXE/DMG published **as signed** — unsigned is called out honestly.
- Artifacts without a manifest entry — the integrity gate forbids them.
