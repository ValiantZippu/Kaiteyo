# Release Workflow

How a Kaiteyo release goes from commit to downloadable, verified installers.

## The single source of truth

`installer/common/version.json` (plus `buildSrc/.../AppVersion.kt`). Keep both in
sync — `installer/scripts/bump-version.sh` updates them together:

```bash
bash installer/scripts/bump-version.sh 2.3.0 2310
```

## 1. Prepare

```bash
git checkout develop && git pull
git checkout -b release/v2.3.0
bash installer/scripts/bump-version.sh 2.3.0 2310
# update installer/linux/appimage/io.github.syt0r.kaiteyo.metainfo.xml releases block
./gradlew :core:allTests :desktopApp:compileKotlinJvm
```

## 2. Build locally (smoke test at least one platform)

Follow `docs/BUILD.md`. Minimum sanity check per platform:

- Windows: EXE runs, upgrades an existing install, silent flag works.
- macOS: `spctl --assess` passes; first launch shows onboarding once.
- Linux: AppImage runs on a clean distro; deb installs with apt.

## 3. Tag & CI

```bash
git tag v2.3.0 && git push origin v2.3.0
```

`.github/workflows/build-release.yml` runs `build-all.yml`, which produces:

| Job | Outputs |
|-----|---------|
| Linux | AppImage (x86_64), deb, rpm, android APK |
| Windows | Inno EXE, MSI, portable zip |
| macOS x64 | styled DMG (`macos-x64.dmg`) |
| macOS arm64 | styled DMG (`macos-arm64.dmg`) |

Signing/notarization runs only when the signing secrets are configured
(`CODESIGN_IDENTITY` + Apple credentials) — see `docs/SIGNING.md`.

CI then:
1. stages everything into one release dir,
2. computes `artifact-manifest.json`,
3. runs `verify-artifacts.sh` (fails the job on any mismatch),
4. attaches artifacts + manifest to the GitHub release.

## 4. Publish

- Release notes from `installer/templates/RELEASE_NOTES.md`.
- CI regenerates the update feeds automatically (`stable`, `beta`, `nightly`)
  and publishes them to the `update-feed` release — nothing to do by hand.
  Locally, for a manual refresh:

```bash
bash installer/scripts/make-update-manifest.sh 2.3.0 stable \
  "https://github.com/ValiantZippu/Kaiteyo/releases/download/update-feed"
```

- Flathub: open a PR to `flathub/io.github.syt0r.kaiteyo` with the new manifest.
- Merge to `main`, then back to `develop`.

## 5. Hotfix

Branch from `main`, bump PATCH via `bump-version.sh`, release, merge back.

## CI integration points

| Concern | Where |
|---------|-------|
| Secrets (signing) | GitHub Actions secrets, consumed by `build-all.yml` |
| Artifact staging | `installer/scripts/stage-artifacts.sh` |
| Integrity gate | `installer/scripts/verify-artifacts.sh` |
| Update feed | `installer/scripts/make-update-manifest.sh` |
| Tag trigger | `build-release.yml` (`on.push.tags: v*.*`) |

## Checklist

- [ ] `bump-version.sh` ran; `version.json` == `AppVersion.kt`
- [ ] metainfo.xml `releases` block updated
- [ ] Installers built on their native OS
- [ ] Windows EXE signed; macOS DMG signed + notarized + stapled
- [ ] `stage-artifacts.sh` + `verify-artifacts.sh` green
- [ ] Update feed regenerated for the right channel
- [ ] Release notes drafted; checksums attached
