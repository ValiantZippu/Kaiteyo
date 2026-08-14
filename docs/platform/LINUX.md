# Linux

## Supported status

✅ **Supported** — desktop app packaged for common distributions.

## Build & packaging

```bash
./gradlew :desktopApp:createDistributable      # app bundle
./gradlew :desktopApp:packageDeb               # jpackage deb (basic)
# Branded packages (installer/):
bash installer/linux/appimage/build.sh         # AppImage (+ AppStream metainfo, hicolor icons)
bash installer/linux/deb/build.sh              # deb
bash installer/linux/rpm/build.sh              # rpm (spec in installer/linux/rpm/)
bash installer/linux/flatpak/build.sh          # Flathub-ready Flatpak manifest
bash installer/linux/snap/build.sh             # Snap wrapper
```

The repo also carries `desktopApp/linux/` packaging metadata (AppImage AppDir, flatpak
metainfo, snapcraft config) used by the installer scripts.

## Platform-specific behavior

- **Native window shell** — custom undecorated window; native OS dragging via X11/EWMH
  (`_NET_WM_MOVERESIZE`, JNA) with a Compose fallback.
- **Media** — VLC/mpv/Java Sound as on other desktops.
- **IME** — Japanese input relies on the desktop environment's IM (e.g. ibus/fcitx) via
  Compose/Skiko; verify with your input method before filing input bugs.

## File system & permissions

| Item | Location |
|---|---|
| Study data / desktop suite state | `~/.kaiteyo` and `$XDG_DATA_HOME/kaiteyo` (XDG-compliant where supported) |
| AppImage | run directly (`chmod +x`, then execute); no install needed |
| Flatpak/Snap | sandboxed installs; snap uses `kaiteyo-data` plug (`~/.kaiteyo`) |

The AppImage bundles the JRE; deb/rpm depend on a JRE or bundle per packaging config.

## Input

- Full keyboard/mouse support, same shortcuts as other desktop platforms.

## Known limitations

- Not every distro is CI-tested; Ubuntu-based builds are the reference (CI installs
  `librsvg2-bin`, `imagemagick`, `rpm`).
- Wayland: behavior depends on the compositor (X11 paths are the reference).
- Snap/Flatpak metadata exists but may lag the installer scripts.
