# Linux — Fedora / RHEL (rpm)

Native package for Fedora and RHEL-compatible distributions (x86_64).

## Build

```bash
./gradlew :desktopApp:createDistributable
bash installer/linux/rpm/build.sh 2.2.1
# → installer/linux/rpm/out/kaiteyo-2.2.1-1.x86_64.rpm
```

The wrapper (`installer/linux/rpm/build.sh`) requires `rpmbuild`
(`rpmdevtools`), tars the jpackage image, copies `.desktop` + metainfo + icons
into the rpmbuild SOURCES, and invokes `rpmbuild -bb` against
`installer/linux/rpm/build.spec` (which owns the `%files` layout, desktop
integration and post-install scriptlets).

## Install / upgrade / uninstall

```bash
sudo dnf install ./kaiteyo-2.2.1-1.x86_64.rpm     # install
sudo dnf upgrade kaiteyo                           # upgrade (when in a repo)
sudo dnf remove kaiteyo                            # uninstall (keeps user data)
```

`dnf` resolves upgrades natively. The spec declares runtime dependencies
consistent with the deb (X11, GL, fontconfig) and ships the bundled JRE, so no
system Java is required.

## Metadata

Package name `kaiteyo`, license GPL-3.0, homepage, summary/description and
icon integration matching the other formats — values are authoritative from
`version.json` and the repo `LICENSE`.

## Testing

Requires a Fedora/RHEL machine or container (rpmbuild is not installable on
Ubuntu without alien). CI runs the Linux packages on Ubuntu; the rpm build is
verified on a Fedora-based builder before release. See
[troubleshooting.md](troubleshooting.md) for common rpm failures.
