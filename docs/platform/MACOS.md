# macOS

## Supported status

✅ **Supported** — desktop app built and packaged for both architectures.

## Build & packaging

```bash
./gradlew :desktopApp:createDistributable      # app bundle
bash installer/macos/build-dmg.sh arm64        # styled DMG (or x64)
bash installer/macos/notarize.sh <path-to.dmg> # hardened-runtime signing + notarization + stapling
```

Packages: **DMG** per architecture (`arm64` on Apple Silicon, `x64` on Intel), styled with
branded background and drag-to-Applications, signed with hardened runtime and
notarized/stapled (see `installer/docs/SIGNING.md` and `entitlements.plist`). CI builds
both arches (macos-15 arm, macos-13 intel).

## Platform-specific behavior

- **Native window shell** — custom title bar with native-style controls; drag uses the
  Compose fallback (macOS restricts WM_NCLBUTTONDOWN-style hooks).
- **TTS** — kana voice playback via AVSpeechSynthesizer (`SwiftTtsKanaManager`) and
  Swift-backed backup archive handling (`SwiftBackupArchiveHandler`).
- **Media** — VLC/mpv/Java Sound as on other desktops.

## File system & permissions

| Item | Location |
|---|---|
| Study data / desktop suite state | `~/Library/Application Support/Kaiteyo` and `~/.kaiteyo` |
| App bundle | `/Applications` (drag to install) |

Sandbox: the app is not sandboxed (desktop). Upgrading = dragging the new `.app` over the
old one; data is preserved.

## Input

- Trackpad/mouse + keyboard, same shortcuts as other desktop platforms.
- Touch bar / pen support as provided by Compose.

## Known limitations

- macOS window drag relies on the Compose fallback (no native WM hooks).
- iOS and macOS builds require a macOS host; iOS is built from `iosApp/` (see
  [IOS.md](IOS.md)).
- Notarization requires Apple developer credentials (env: `APPLE_ID`/team secrets in CI).
