# Distribution FAQ

User-facing questions about downloading, installing, updating and uninstalling
Kaiteyo.

## Download

**Where do I download Kaiteyo?**
The [Download page](https://kaiteyo.app/download) (website) or the
[GitHub Releases](https://github.com/ValiantZippu/Kaiteyo/releases) page. All
releases are direct downloads — no store, no account, no tracking.

**Which file should I pick?**
- Windows → the EXE installer (`Kaiteyo-*-windows-setup.exe`).
- macOS → the DMG matching your chip (arm64 for Apple silicon, x64 for Intel).
- Linux → AppImage (portable) or the native package for your distro
  (deb for Ubuntu/Debian, rpm for Fedora).
- Android → the APK from the fdroid flavor (or the Play Store listing when live).

**Is the web trial a real install?** No — the web trial is a browser slice with
sample data, nothing leaves your device. It's for trying before installing.

## Install

**Do I need Java?** No. Desktop installers bundle the JRE.

**Why does the Windows installer ask for admin?** Writing to Program Files is
an OS requirement. The app itself runs as your user without elevation.

**Does Linux need sudo?** Only at install time (package manager). The app runs
as your user, never as root.

**I want no installer at all.** Windows: use the portable ZIP. Linux: use the
AppImage (`chmod +x`, run).

## Updates

**How do updates work?** The desktop app checks the channel feed (stable by
default) over HTTPS, downloads the verified artifact, and applies it
platform-natively. Updates are calm — never mid-study, never without asking.

**Why doesn't my Flatpak/deb install self-update?** Managed installs delegate
updates to the package manager (`flatpak update`, `apt upgrade`, `dnf upgrade`)
so you don't end up with two competing updaters.

**What are beta/nightly?** Opt-in channels in Settings → Updates. Beta is a
feature preview; nightly is for contributors and may break. You can never
accidentally land on them.

## Uninstall

**Will uninstalling delete my study data?** No — the uninstaller explicitly
asks, and "keep my study data" is the default. Your decks, database and history
live separately from the app files and survive uninstall.

**How do I back up?** Kaiteyo's data is in one user directory
(`%LOCALAPPDATA%\Kaiteyo` / `~/.kaiteyo` / XDG dirs). Copy it, or use the
in-app backup/export (see `docs/architecture/backup.md`). Back up before any
clean OS reinstall.

**Can I have stable + beta installed at once?** Not on desktop (same AppId —
they upgrade each other). Switching channels is a Settings action. Data is
shared, so back up before experimenting.

## Platform support

**Is Windows ARM64 supported?** Not yet — only x64 is built and tested. The
download page does not claim arm64.

**Which Linux distros?** Debian/Ubuntu (deb), Fedora/RHEL (rpm), Arch (PKGBUILD),
plus the universal AppImage and Flatpak. Ubuntu is the CI reference.

**Android minimum version?** Android 8.0 (API 26)+.

## Localization

**Is the installer localized?** Installer strings are prepared for
localization (English + Japanese at minimum); Windows uses Inno's language
system. The app itself is localized through the standard Strings interface
(English/Japanese) — see `docs/architecture/localization.md`.

## Security

**How do I know the download is genuine?** Every artifact has a SHA-256 checksum
in the release manifest and notes; Windows/macOS builds are signed/notarized
when secrets are configured. See [checksums.md](checksums.md) and
[security.md](security.md).

**Does the installer phone home?** No. Installation is local. No telemetry, no
upload of your decks or study data.
