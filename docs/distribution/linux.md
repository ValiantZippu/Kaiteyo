# Linux Distribution

Linux is not one platform — Kaiteyo ships **multiple formats** for different
distribution models, all built from the same jpackage app bundle.

## Formats

| Format | Role | Install | Update | Uninstall |
|---|---|---|---|---|
| **AppImage** | Portable single-file download | `chmod +x`, run | swap file | delete file |
| **deb** | Debian/Ubuntu native | `apt install ./kaiteyo.deb` | `apt upgrade` | `apt remove kaiteyo` |
| **rpm** | Fedora/RHEL native | `dnf install ./kaiteyo.rpm` | `dnf upgrade` | `dnf remove kaiteyo` |
| **Flatpak** | Sandboxed store format | `flatpak install` | `flatpak update` | `flatpak uninstall` |
| **PKGBUILD** | Arch Linux / AUR | `makepkg -si` / helper | package manager | package manager |
| Snap | Optional | `snap install` | `snap refresh` | `snap remove` |

Guides: [appimage](linux-appimage.md) · [deb](linux-debian.md) ·
[ubuntu](linux-ubuntu.md) · [fedora](linux-fedora.md) ·
[flatpak](linux-flatpak.md) · [arch](linux-arch.md).

## Desktop integration

Every format ships the same desktop metadata:

- **`.desktop` entry** — `io.github.syt0r.kaiteyo.desktop` (name, icon,
  categories `Education;Languages;`).
- **Icons** — SVG + PNG 16–512 under the hicolor theme
  (`/usr/share/icons/hicolor/*/apps/`), so every desktop shows the same artwork.
- **AppStream metainfo** — `io.github.syt0r.kaiteyo.metainfo.xml` (id,
  screenshots, release notes, content rating, brand colors) for software centers.

## User data (XDG conventions)

| Kind | Location |
|---|---|
| Config / state | `$XDG_CONFIG_HOME/kaiteyo` (falls back `~/.kaiteyo`) |
| Data / databases / decks | `$XDG_DATA_HOME/kaiteyo` (falls back `~/.kaiteyo`) |
| Cache | `$XDG_CACHE_HOME/kaiteyo` |

Kaiteyo prefers XDG locations where supported and falls back to `~/.kaiteyo`
on older setups. **Never** store user data inside the install dir (except the
portable AppImage data folder, which is self-contained by design).

## Permissions

- The **application runs as the normal user** — never as root.
- Installation may use package-manager privileges (apt/dnf/flatpak), which is
  the platform convention; the app itself requires none.
- No sudo is needed for ordinary usage (AppImage/portable paths).

## Dependencies

- The AppImage bundles the JRE; deb/rpm bundle or declare it per packaging
  config. Graphics/audio deps (X11 libs, fontconfig, GL) are declared in each
  package's metadata (see `installer/linux/deb/build.sh` control `Depends`).
- Bundling is preferred where it makes the app "just work"; the distro provides
  system libs where that is safer. Rationale per format in
  `installer/docs/ARCHITECTURE.md`.

## Support statement (honest)

- **Reference distro for CI:** Ubuntu (x86_64). AppImage builds are per-host
  arch (x86_64 in CI; aarch64 on an ARM runner is configured but not yet in the
  default matrix — see [README.md](README.md) matrix).
- Wayland: behavior depends on the compositor; X11 paths are the reference.
- Snap/Flatpak metadata exists and is maintained with the installer scripts,
  but Flatpak publish (Flathub PR) and Snap store review are **pending**.
