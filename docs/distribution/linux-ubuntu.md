# Linux — Ubuntu

Ubuntu uses the **deb** package (see [linux-debian.md](linux-debian.md)) — this
page covers Ubuntu-specific behavior.

## Installing

From the release artifact:

```bash
sudo apt install ./kaiteyo_2.2.1_amd64.deb
```

Or from **Ubuntu Software** (GNOME Software) when the deb is opened/downloaded
through the GUI — the AppStream metainfo makes it show proper branding,
screenshots and release notes.

## No PPA by default

Kaiteyo does not require a PPA. Installing from the `.deb` is enough. A
repository/PPA is a possible future distribution channel, but is **not**
currently published — do not document a `ppa:` line that does not exist.

## Upgrades

- `apt upgrade kaiteyo` when the package is in a configured repo.
- Otherwise: download the new `.deb` and `sudo apt install ./kaiteyo_*.deb`
  — apt treats it as an upgrade of the same package and preserves
  `/usr/lib/kaiteyo` data separation (user data lives in `~/.kaiteyo` /
  XDG dirs, untouched by apt).

## What Ubuntu gets that other distros may not

- `update-desktop-database` + `gtk-update-icon-cache` run automatically
  (postinst) so the launcher icon appears immediately.
- GNOME Software integration through the shared AppStream metainfo.

## Testing

CI's Linux reference is Ubuntu — the deb build and AppImage run on a clean
Ubuntu runner before every release.
