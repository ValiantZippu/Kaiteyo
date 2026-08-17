# Windows Package Managers

Kaiteyo provides manifests for the three mainstream Windows package managers so
users can install, upgrade and uninstall through their ecosystem. All three
wrap the same **signed EXE silent install**, so behavior is identical to a
manual install — same AppId, same data location, same uninstall path.

> **Status: defined, not yet published.** The manifests live in the repo
> (`installer/windows/packaging/`) and are ready to publish, but publishing
> requires maintainer accounts on each community repository. Until a
> maintainer owns each channel, the manifests are documentation + ready-to-go
> definitions, not live packages. Do not claim `winget install kaiteyo` works
> until it is published and tested.

## WinGet

- **Manifest**: `installer/windows/packaging/winget/` (YAML, winget-pkgs
  format).
- **Package id**: `Kaiteyo.Kaiteyo` (candidate; final id decided at publish).
- **Install**: `winget install kaiteyo`
- **Upgrade**: `winget upgrade kaiteyo`
- **Uninstall**: `winget uninstall kaiteyo`
- **Silent**: the installer runs with the same `/VERYSILENT` flags the
  manifest declares (`silent` / `silentWithProgress` install modes).
- **Checksums**: WinGet manifests carry `InstallerSha256` — regenerated from
  the actual signed EXE at publish time.

## Chocolatey

- **Package**: `installer/windows/packaging/chocolatey/` (`.nuspec` +
  `chocolateyinstall.ps1`).
- **Package id**: `kaiteyo` (candidate).
- **Install**: `choco install kaiteyo`
- **Upgrade**: `choco upgrade kaiteyo`
- **Uninstall**: `choco uninstall kaiteyo`
- **Silent**: `chocolateyinstall.ps1` invokes the EXE with
  `/VERYSILENT /SUPPRESSMSGBOXES /NORESTART`.
- **Checksums**: the install script verifies `checksum64` against the actual
  artifact before running it.

## Scoop

- **Manifest**: `installer/windows/packaging/scoop/` (`kaiteyo.json`).
- **Package id**: `kaiteyo` (candidate).
- **Install**: `scoop install kaiteyo`
- **Upgrade**: `scoop update kaiteyo`
- **Uninstall**: `scoop uninstall kaiteyo`
- **Silent**: `installer` array with `silent` / `silentWithProgress` switches.
- **Checksums**: `hash` field, regenerated at publish.

## Publication checklist (per manager, when pursued)

- [ ] Manifest id/name finalized and consistent across managers
- [ ] `InstallerSha256` / `checksum64` / `hash` regenerated from the **actual
      signed release artifact** (never stale hashes)
- [ ] Silent install verified on a clean machine
- [ ] Upgrade verified over an existing install (data preserved)
- [ ] Uninstall verified (data preserved by default)
- [ ] PR submitted to the community repository (winget-pkgs, chocolatey
      community repo, scoop bucket)
- [ ] Version + URL updated on every release (or a bot handles it)

## Rules

1. Manifests always point at the **signed** release EXE on GitHub Releases.
2. Hashes are computed from real artifacts at release time — never `SKIP`,
   never stale.
3. Package-manager installs must be silent and non-interactive
   (`/VERYSILENT`), so automation never hangs on a wizard.
4. Upgrade behavior must preserve `%LOCALAPPDATA%\Kaiteyo` user data
   (it does — the EXE upgrade path never touches the data dir).
