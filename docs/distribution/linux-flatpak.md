# Linux — Flatpak

Sandboxed, store-ready Linux packaging (the Flathub path).

## Application identity

- **Application ID**: `io.github.syt0r.kaiteyo` (matches every other format's
  `.desktop` id and AppStream id).
- **Manifest**: `installer/linux/flatpak/io.github.syt0r.kaiteyo.yaml`.
- **Runtime**: Freedesktop platform (see the manifest for the pinned version).

## Build

```bash
./gradlew :desktopApp:createDistributable
bash installer/linux/flatpak/build.sh 2.2.1
```

Requires `flatpak-builder` on the host (and the matching Flatpak runtime).

## Sandbox & permissions

The manifest requests **only** what the app needs — no blanket
`--filesystem=home` unless a feature genuinely requires it:

- **Network**: required for updates and (optionally) online dictionaries — kept
  to the `network` permission only, no special socket grants.
- **Filesystem**: user data lives in the standard `~/.kaiteyo` / XDG locations;
  the manifest grants access to those specific paths (or relies on the portal)
  rather than requesting whole-home access.
- Every permission in the manifest is documented inline — unexplained access is
  not allowed (distribution spec §150).

The app runs entirely unprivileged inside the sandbox; no root anywhere.

## Desktop integration

- `.desktop` entry + icons (hicolor) come from the shared metadata, so the
  Flatpak launcher shows the Kaiteyo icon and name.
- AppStream metainfo (screenshots + releases) is embedded for the store UI.

## Updates

`flatpak update` (or Flathub auto-updates) upgrades the app; the sandbox
handles the swap atomically. The app's own update feed is disabled in the
Flatpak build by policy — the package manager is the update mechanism, avoiding
two competing updaters (see [updates.md](updates.md)).

## Flathub

Publishing is a **manual PR** to `flathub/io.github.syt0r.kaiteyo` — not yet
done, so the Flathub listing is pending. Until then the manifest builds and
installs locally (`flatpak-builder` + `flatpak install`).

## Known limitations

- Requires a Flatpak runtime download on first install (network).
- Some desktop features behave differently under the sandbox (media backends,
  global hotkeys) — verified per release before claiming support.
