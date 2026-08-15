# Kaiteyo Architecture — CI/CD & Release Pipeline

**Status**: Implemented and exercised (GitHub Actions)
**Owner**: `.github/workflows/` + `installer/scripts/`
**Related**: `docs/releases/RELEASE_PROCESS.md` · `docs/releases/RELEASE_CHECKLIST.md` ·
`docs/architecture/toolchain.md` · `docs/architecture/assets.md`

## 1. Principles (§231–§232)

Targeted pipelines, not one enormous CI job. Three tiers: **PR validation** (lightweight),
**release build** (full artifacts), future **nightly** (channels exist). Release builds are
reproducible: pinned JDK 17 Temurin, Gradle cache, pinned plugin/tool versions.

## 2. `build-all.yml` (reusable, `workflow_call` + `workflow_dispatch`)

Four build jobs + a verification job; `KIT_VERSION` (2.2.1) used for artifact naming only —
the real version source is `installer/common/version.json` (read by the staging step).

### build-android-and-desktop-linux (ubuntu-latest)
1. `actions/setup-java` (17, Temurin, gradle cache).
2. Install packaging tools: `librsvg2-bin`, `imagemagick`, `rpm`.
3. Signing secrets → decode `KEYSTORE_BASE64` → `keystore.jks`.
4. `./gradlew app:assembleFdroidRelease :desktopApp:createDistributable`.
5. Linux packages: `installer/scripts/generate-assets.sh` → AppImage, deb, rpm builders.
6. Upload artifacts: `android` (fdroid release APK), `linux` (AppImage/deb/rpm),
   retention 1 day.

### build-desktop-windows (windows-latest)
- `choco install inno-setup imagemagick`.
- `:desktopApp:createDistributable` → `generate-assets.sh` → `:desktopApp:packageMsi` →
  `installer/windows/build.ps1` (branded exe) → `installer/windows/portable/build-portable.ps1`
  (zip).
- Upload `windows`: exe, zip, msi.

### build-desktop-mac-intel (macos-13) / build-desktop-mac-arm (macos-15)
- `brew install create-dmg` → `generate-assets.sh` (best-effort) →
  `:desktopApp:createDistributable` → `installer/macos/build-dmg.sh x64|arm64` →
  `installer/macos/notarize.sh`.
- Upload `mac-intel` / `mac-arm` DMGs.

### verify-uploaded-builds
Downloads all artifacts and lists them — the smoke gate before staging.

## 3. `build-release.yml` (tags `v*.*`, `contents: write`)

1. **build-all** (reusable, secrets inherited).
2. **stage-and-verify** (ubuntu):
   - Download artifacts under `artifacts/`.
   - `installer/scripts/stage-artifacts.sh "$VERSION" "release/kaiteyo-$VERSION" --from …`
     — canonical names + sha256 manifest; **missing required artifacts FAIL here** (the
     integrity gate).
   - `installer/scripts/verify-artifacts.sh "$VERSION"` — verifies the staged release.
   - `make-update-manifest.sh "$VERSION" <channel> "$BASE"` for **stable, beta, nightly**
     against the channel-stable base URL (`releases/download/update-feed` —
     `releases/latest` can never serve beta/nightly feeds).
   - Upload `release/` + `installer/common/update-*.json` as artifact `release`.
3. **create-release** (ubuntu):
   - `softprops/action-gh-release` publishes jars/apks/dmgs/msis/exes/zips/AppImages/
     debs/rpms/flatpaks + `artifact-manifest.json` + `update-*.json`.
   - Second `softprops` step refreshes the **`update-feed` prerelease** (marked
     `prerelease: true`, `fail_on_unmatched_files: false`) so each channel always sees the
     newest feed while `releases/latest` keeps pointing at real releases.

## 4. Release artifacts (§233)

| Platform | Artifacts |
|---|---|
| Windows | branded `.exe` (Inno), portable `.zip`, `.msi` |
| macOS | Intel + ARM `.dmg` (notarized) |
| Linux | AppImage, `.deb`, `.rpm` (Flatpak planned) |
| Android | fdroid release `.apk` (what F-Droid builds); googlePlay paths separate |
| Data | `artifact-manifest.json` (sha256), `update-*.json` feeds |

Versioning: `buildSrc/AppVersion.kt` (single source: `versionCode` 2210, `versionName`
2.2.1, `desktopAppVersion` must be 3 numbers) + `installer/common/version.json` mirrored
for the installer subsystem (ADR-0010).

## 5. Update system (§235)

Channel feeds (`update-*.json`) served from the `update-feed` prerelease; the desktop app
reads feeds and checks versions. Architecture complete (stable/beta/nightly channels);
**end-user rollout is staged** — verify download/verification/install/rollback before
enabling (`planning/TODO.md` P2).

## 6. Gaps / planned

- Nightly build pipeline (channels exist; nightly artifact flow is future).
- PR-validation workflow for fork PRs (currently release-triggered; §231 targets
  lightweight checks on important pushes).
- Website `dist/` regeneration in CI (currently manual; TODO → TECHNICAL DEBT).
- Release checklist: `docs/releases/RELEASE_CHECKLIST.md` (migration, backup, tests,
  lint, packaging, licenses, assets, localization, performance, crash checks, installer,
  release notes — §340).

## 7. Content validation in CI (TARGET — ADR-0015, §148)

When the content pipeline lands, CI gains package-validation gates (targeted, §231):

- **Schema + relationship validation** against the registries (NODE_TYPE_REGISTRY /
  RELATIONSHIP_REGISTRY) on every authored-content PR — cheap, L1–L2.
- **Asset validation** (hashes, budgets) and **localization validation** (ja/en
  completeness) for world/lesson packages.
- **License metadata presence** checks (§260) — reject anonymous content.
- **Performance validation**: sample queries against the §TEST_PLAN budgets on a
  reference runner before package publication.
- Gate placement: content pipeline (authoring-time) + CI (PR-time) + runtime install
  verification (§148) — three independent layers.
