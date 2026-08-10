# Kaiteyo Linux Packaging

Kaiteyo ships **four** Linux formats so users can pick what fits their distro —
we never force one format. All of them are built from the same jpackage image
(`desktopApp:createDistributable`) and share one AppStream metainfo file.

| Format | Dir | Audience | Upgrade path |
|--------|-----|----------|--------------|
| AppImage | `appimage/` | Portable / GitHub downloads | Swap file (self-update via update feed) |
| deb | `deb/` | Debian / Ubuntu / Mint | `apt upgrade` |
| rpm | `rpm/` | Fedora / RHEL / openSUSE | `dnf upgrade` |
| Flatpak | `flatpak/` | Stores / sandbox users | `flatpak update` / Flathub |
| Snap | `snap/` | Ubuntu Store (optional) | `snap refresh` |

## Shared conventions

- **Icons**: `assets/generated/linux/kaiteyo-{16,32,48,64,128,256,512}.png` installed
  under `/usr/share/icons/hicolor/<size>/apps/` plus `kaiteyo.svg` for scalable
  contexts. No single-icon shortcuts.
- **AppStream**: `io.github.syt0r.kaiteyo.metainfo.xml` ships in every package
  (`/usr/share/metainfo/`), so GNOME Software / KDE Discover show screenshots,
  descriptions and release notes.
- **Data**: the app writes to `$XDG_DATA_HOME/kaiteyo` / `~/.kaiteyo`. Packages
  never write there at install time, and uninstalls never remove it.

## Build order

```bash
bash installer/scripts/generate-assets.sh        # icons (once)
./gradlew :desktopApp:createDistributable        # app image (once)
bash installer/linux/appimage/build.sh
bash installer/linux/deb/build.sh
bash installer/linux/rpm/build.sh
bash installer/linux/flatpak/build.sh            # needs flatpak tooling
bash installer/linux/snap/build.sh               # optional, needs snapcraft
```

Each builder writes into `installer/linux/<fmt>/out/`. `stage-artifacts.sh`
collects them for a release.
