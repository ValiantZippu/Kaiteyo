# Linux — Debian / Ubuntu (deb)

Native package for Debian and Ubuntu (amd64).

## Build

```bash
./gradlew :desktopApp:createDistributable
bash installer/linux/deb/build.sh 2.2.1
# → installer/linux/deb/out/kaiteyo_2.2.1_amd64.deb
```

The builder stages the jpackage image into a standard `usr/` layout using
`dpkg-deb`: app payload in `/usr/lib/kaiteyo`, a `/usr/bin/kaiteyo` launcher,
`.desktop` entry, AppStream metainfo, hicolor icons, and `postinst`/`prerm`
hooks that refresh the desktop database and icon caches.

## Install / upgrade / uninstall

```bash
sudo apt install ./kaiteyo_2.2.1_amd64.deb     # install
sudo apt upgrade kaiteyo                        # upgrade (once in a repo/PPA)
sudo apt remove kaiteyo                         # uninstall (keeps user data)
sudo apt purge kaiteyo                          # uninstall + config (still keeps ~/.kaiteyo data)
```

`apt` resolves upgrades natively; the package declares its runtime
dependencies (`libx11-6`, `libxext6`, `libxi6`, `libxrender1`, `libxtst6`,
`libgl1`, `libfontconfig1`) and the bundled JRE makes the app self-contained.

## Metadata

Package name `kaiteyo`, section `education`, priority `optional`,
architecture `amd64`, homepage and a GPL-3.0 description — authoritative values
from `installer/common/version.json` + the repo `LICENSE`. AppStream metainfo
(shared with every format) feeds GNOME Software / KDE Discover.

## Testing

Installs/uninstalls cleanly on current Ubuntu/Debian (CI reference: Ubuntu).
See [troubleshooting.md](troubleshooting.md) for the usual failure modes
(missing icons, dependency resolution).
