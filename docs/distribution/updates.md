# Updates

The auto-update architecture. Operational detail: `installer/docs/UPDATES.md`.

## Channels

| Channel | Feed | Audience | Notes |
|---|---|---|---|
| `stable` | `update-stable.json` | everyone (default) | tested releases only |
| `beta` | `update-beta.json` | opt-in testers | feature previews |
| `nightly` | `update-nightly.json` | contributors | built from `develop`, may break |

Channels are **opt-in** for beta/nightly — a user can never accidentally land
on a development build. The app never auto-switches channels and never
downgrades below the installed version.

## How an update happens

```
1. Check   — fetch update-<channel>.json over HTTPS, compare version codes
2. Inform  — "Kaiteyo update available: 1.4.0 — what's new [Update] [Later]"
             (never interrupts an active study session; waits for session end
             or explicit user restart unless it is a critical security update)
3. Download— to the data dir (never the app dir), streamed
4. Verify  — sha256 + size verified BEFORE anything is written
5. Stage   — new version prepared next to the current one
6. Replace — platform-native swap (see below)
7. Restart — relaunch; rollback marker written on first clean launch
```

## Apply per platform

| Platform | Mechanism |
|---|---|
| Windows | Inno silent upgrade (`/VERYSILENT`) — same AppId, preserves data |
| macOS | Replace `.app` contents (signed+notarized bundles only) |
| Linux (AppImage) | Swap file + relaunch |
| Linux (deb/rpm/flatpak) | Delegate to the package manager — the app never self-updates inside a managed install |

## Security (never negotiable)

- Feeds and artifacts are served over **HTTPS only**.
- **Never** trust an arbitrary downloaded executable: every artifact is
  checksum-verified against the manifest before execution/install.
- No "download whatever URL the server returns and execute it" — the manifest
  is schema-validated and pinned to the `update-feed` release.
- Rollback: the previous version is kept until the new one launches cleanly
  once; a failed update preserves the working install.

## Update UX

- Updates are calm: a small notification with version + release notes and
  Update/Later choices.
- No update is applied mid-study; the app waits for a natural break.
- Progress is real (bytes verified vs. total), never fake percentages.

## Future-proofing (already in the design)

- **`ReleaseProvider` abstraction** — update logic talks to an interface, not
  one hardcoded URL, so GitHub / an official Kaiteyo server / store APIs can
  back it without rewriting the updater.
- Delta updates, background download and signed manifests are anticipated by
  the manifest schema (see `installer/common/update-manifest.schema.json`).

## Related

- [checksums.md](checksums.md) — verification mechanics.
- [security.md](security.md) — trust model.
- [installers.md](installers.md) — atomic apply / interrupted-update behavior.
