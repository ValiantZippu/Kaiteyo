# Linux — Arch Linux (PKGBUILD)

Arch packaging for Kaiteyo, defined in `installer/linux/arch/`.

## The PKGBUILD

`installer/linux/arch/PKGBUILD` — real values, no invented metadata:

| Field | Value |
|---|---|
| `pkgname` | `kaiteyo` |
| `pkgver` | read from `installer/common/version.json` (bumped by `bump-version.sh`) |
| `pkgrel` | `1` (bump when rebuilding the same version) |
| `arch` | `x86_64` |
| `license` | `GPL3` |
| `depends` | matching the deb: `libx11`, `libxext`, `libxi`, `libxrender`, `libxtst`, `libgl`, `fontconfig` |
| `optdepends` | `vlc` / `mpv` for the media backends |
| `source` | the release AppImage (GitHub) |
| `sha256sums` | real hashes, regenerated each release (never `SKIP` without reason) |
| `install` | `.desktop`, icons, AppStream metainfo, `/usr/bin/kaiteyo` launcher |

Build from the release AppImage (consistent with the other Linux formats):

```bash
cd installer/linux/arch
makepkg -si        # build + install
```

or install the produced `kaiteyo-<ver>-1-x86_64.pkg.tar.zst` directly:

```bash
sudo pacman -U kaiteyo-<ver>-1-x86_64.pkg.tar.zst
```

## Install / upgrade / uninstall

- **Install**: `makepkg -si` or `pacman -U`.
- **Upgrade**: rebuild with the new release AppImage + version, then
  `pacman -U` (pacman replaces the older package; user data untouched).
- **Uninstall**: `sudo pacman -R kaiteyo` (keeps user data in `~/.kaiteyo`).

## AUR path

Publishing to the **AUR** is a release-policy decision and is **not yet done**.
When pursued:

- AUR package name: `kaiteyo` (or `kaiteyo-bin` for the binary package —
  decided at publish time).
- The AUR PKGBUILD must reference the release AppImage + real `sha256sums`
  (updated on every release) and must be tested with `makepkg` in a clean
  chroot (`extra-x86_64-build`) before submission.
- Maintainership is a commitment — the AUR package must be updated on every
  release. Until a maintainer owns that, keep the PKGBUILD in-repo only and
  document the path.

## Packaging rules

1. Real names, real versions, real checksums — nothing invented.
2. `pkgver` comes from `version.json` at build time; never hardcode.
3. `depends` are declared, not assumed — same set the deb/rpm declare.
4. The desktop entry + icons + metainfo come from the shared metadata
   (`installer/linux/appimage/`), not duplicated copies.
