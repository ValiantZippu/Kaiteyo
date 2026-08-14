# Release Checklist

Companion to [`RELEASE_PROCESS.md`](RELEASE_PROCESS.md). Work through this before every
tagged release.

## 1. Version & metadata

- [ ] `installer/scripts/bump-version.sh <version> <code>` ran; `version.json` equals
      `buildSrc/src/main/kotlin/AppVersion.kt` (`versionName` / `versionCode`)
- [ ] `desktopAppVersion` is 3 numbers
- [ ] `installer/linux/appimage/io.github.syt0r.kaiteyo.metainfo.xml` `<releases>` block
      updated
- [ ] `CHANGELOG.md` updated with the new version (Added/Changed/Fixed/Removed/Security)
- [ ] `fastlane` changelog for the Android store updated (if mobile release)
- [ ] `website/config/site.json` version bumped (if site is rebuilt)

## 2. Verification gates

- [ ] `./gradlew :core:allTests` green
- [ ] `./gradlew :desktopApp:compileKotlinJvm` green, no new warnings
- [ ] `./gradlew :kjd:test` green (if KJD/database changed)
- [ ] `./gradlew :desktopApp:test` green (if desktop suite changed)
- [ ] Desktop app smoke-tested on at least one native OS (study flow + desktop suite)
- [ ] Android `:app:assembleFdroidRelease` builds (what CI ships)
- [ ] iOS: macOS build verified if iOS changed (manual)

## 3. Packaging gates (per host OS)

- [ ] **Windows:** EXE built with Inno Setup, MSI, portable ZIP; EXE upgrades a previous
      install cleanly; uninstaller preserves/removes data per user choice; code-signed
- [ ] **macOS:** DMGs for arm64 + x64 built, `spctl` accepts them (signed, notarized,
      stapled)
- [ ] **Linux:** AppImage runs on a clean machine; deb and rpm install/uninstall cleanly
- [ ] Artifacts named per `RELEASE_PROCESS.md` artifact table
- [ ] `installer/scripts/generate-assets.sh` re-ran (brand assets current)

## 4. Release integrity

- [ ] `installer/scripts/stage-artifacts.sh <version>` succeeds (canonical names +
      sha256 manifest)
- [ ] `installer/scripts/verify-artifacts.sh <version>` green (missing/corrupt artifact
      fails)
- [ ] Update feeds regenerate for all three channels (stable/beta/nightly) and publish to
      the `update-feed` release
- [ ] `artifact-manifest.json` attached to the release

## 5. Data & migrations

- [ ] If the bundled app-data database version changed (`AppDataDatabaseVersion` /
      `AppAssets.kt`), the asset file name and build downloads are consistent
- [ ] User-data `.sqm` migrations reviewed (schema changes are additive/backward-safe)
- [ ] KJD patch feed (if shipped) matches the bundled database version

## 6. Rollback plan

- [ ] Previous release artifacts still downloadable (GitHub releases are immutable; the
      `update-feed` release is regenerated — keep the previous tag)
- [ ] `UpdatePolicy` min-version guard prevents downgrade loops
- [ ] Hotfix path documented (branch from `main`, bump PATCH, tag, merge back)

## 7. Post-release

- [ ] Tag pushed → `build-release.yml` ran green end-to-end
- [ ] Release notes drafted from `installer/templates/RELEASE_NOTES.md`
- [ ] Flathub PR prepared (if publishing there)
- [ ] `release/vX.Y.Z` merged → `main` → back to `develop`
- [ ] `docs/planning/COMPLETED.md` and `CURRENT_ISSUES.md` updated for what shipped
