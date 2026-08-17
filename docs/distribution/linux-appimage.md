# Linux — AppImage

Portable, single-file Linux application. The primary "download from GitHub"
artifact for rolling/distro-agnostic users.

## Build

```bash
./gradlew :desktopApp:createDistributable
bash installer/linux/appimage/build.sh 2.2.1
# → installer/linux/appimage/out/Kaiteyo-2.2.1-x86_64.AppImage
```

The builder assembles an AppDir from the jpackage image: `AppRun` launcher,
`.desktop` entry, AppStream metainfo, hicolor icon theme (16–512 + SVG), then
`appimagetool` bundles it (auto-downloaded per arch). Build is per-host arch;
CI produces `x86_64` on the Ubuntu runner.

## Install / run

```bash
chmod +x Kaiteyo-2.2.1-x86_64.AppImage
./Kaiteyo-2.2.1-x86_64.AppImage
```

No package manager, no sudo, no root. User data goes to the normal XDG
locations (see [linux.md](linux.md)) — the AppImage itself stays read-only.

## Desktop integration

- The `.desktop` file + icons are embedded, so AppImageLauncher (or a manual
  integration step) can add a proper launcher entry with the Kaiteyo icon.
- Without a launcher, the AppImage still runs directly from a terminal or file
  manager — full functionality, no integration required.

## Updates

- The auto-update feed (see [updates.md](updates.md)) can point at the next
  AppImage; applying = download → verify sha256 → swap file → relaunch.
- Manual updates are just "download the new file and run it".

## Signing / integrity

- AppImages ship unsigned (no central Linux authority). Every release artifact
  carries a **sha256** in `artifact-manifest.json` and the update feed verifies
  the hash **before** any swap (see [checksums.md](checksums.md)).

## Known limitations

- Needs `FUSE` (or `--appimage-extract-and-run`) on some distros.
- Desktop integration is user-installed (AppImageLauncher) unless a distro
  package provides it.
