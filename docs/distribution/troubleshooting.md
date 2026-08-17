# Distribution Troubleshooting

User-facing failures and how to fix them. Developer build issues live in
`docs/troubleshooting/` — this page is about **installed packages**.

## Windows

| Symptom | Cause / fix |
|---|---|
| "Windows protected your PC" (SmartScreen) | Unsigned or low-reputation build. Click More info → Run anyway only if you trust the source; ideally install from the signed release. Signing status is stated per release. |
| Installer "insufficient rights" | The installer offers to relaunch elevated (Program Files write needs admin). Accept elevation. |
| App won't start after install | Try `kaiteyo --safe-mode` (disables themes/GPU effects/plugins). If a stale install is in the way, run the uninstaller first. |
| Uninstaller keeps asking about data | That's by design — it protects your decks/database. Choose "Keep" unless you intend to remove everything. |
| Missing runtime | The installer bundles the JRE — no Java install required. If the bundled JRE is corrupt, reinstall. |

## Linux

| Symptom | Cause / fix |
|---|---|
| AppImage "FUSE error" | Some distros disable FUSE. Run with `--appimage-extract-and-run` or install via a package. |
| AppImage not executable | `chmod +x Kaiteyo-*.AppImage`. |
| Desktop icon missing | Refresh the icon cache: `update-desktop-database` / `gtk-update-icon-cache` (the deb postinst does this automatically). |
| Wayland issues (input/flicker) | Behavior depends on the compositor; X11 is the reference. Try `GDK_BACKEND=x11`-style workarounds for the desktop environment, or report it. |
| "libX11.so.6 not found" (deb) | Missing dependency — `sudo apt install -f` resolves declared deps. |
| Flatpak won't launch / sandbox denies access | Check the manifest permissions (documented inline); file access uses portals. `flatpak run io.github.syt0r.kaiteyo`. |
| Application prompts for root | It shouldn't — the app runs as your user. If a package script asks for root, that's install-time only. |

## Android

| Symptom | Cause / fix |
|---|---|
| "App not installed" | Version downgrade or signature mismatch — uninstall the old APK (data loss risk: back up first) or install the same-signature update. |
| Storage permission request | The app uses SAF/scoped storage and asks only when a feature needs it. Granting is optional for core study. |
| First launch looks empty | Onboarding creates decks from the bundled dictionary data; if it was skipped, Settings → Show onboarding again. |
| Update fails | Ensure the new version's versionCode is higher (it always is from `version.json`); a sideloaded downgrade is blocked by the system. |

## Updates

| Symptom | Cause / fix |
|---|---|
| "Update failed" | The updater verifies sha256 before applying; a truncated download is rejected. Retry — it will re-download. |
| App won't update itself on Linux package installs | By design: deb/rpm/flatpak delegate to the package manager. Use `apt upgrade` / `dnf upgrade` / `flatpak update`. |
| Rollback | The previous version is kept until the new one launches cleanly; a failed update preserves the working install. |

## Reproducing a failed CI build

- Same OS + JDK 17 + the packaging tools in `installer/docs/BUILD.md`.
- The exact commands are in `installer/docs/BUILD.md` and
  `docs/development/COMMANDS.md`; every script reads `version.json`, so no
  version arguments are needed for a faithful rebuild.
- Installer logs: Windows `%TEMP%\Kaiteyo Setup Log*.txt`; Linux package-manager
  logs; application logs under the user data dir — separate by design
  (see [installers.md](installers.md)).
