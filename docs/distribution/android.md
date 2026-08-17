# Android Distribution

Android is a first-class platform: two flavors, store-native artifacts, secure
signing, and no debug builds shipped as production.

## Application identity

| Attribute | Value |
|---|---|
| Application ID | `ua.syt0r.kanji` (namespace) |
| F-Droid flavor ID | `ua.syt0r.kanji.fdroid` |
| Debug ID | `ua.syt0r.kanji.dev` |
| minSdk / targetSdk | 26 / 35 |
| versionCode / versionName | `AppVersion.versionCode` / `AppVersion.versionName` (single source of truth) |

## Flavors & build types

| Variant | Purpose |
|---|---|
| `googlePlayRelease` | Play Store — AAB, Firebase/billing/review enabled |
| `fdroidRelease` | F-Droid — Google-free, reproducible, no Firebase (tasks auto-disabled via `adjustFlavorTasks()`) |
| `*Debug` | Development only — suffix `-debug`, ID `.dev`, **never distributed** |

## Artifacts

```bash
./gradlew :app:assembleFdroidRelease      # APK (F-Droid / sideload) — what CI ships
./gradlew :app:bundleGooglePlayRelease    # AAB (Play Store)
./gradlew :app:assembleDebug              # local dev only
```

- **APK** for sideloading/testing; the release APK is the F-Droid flavor, never
  a debug build.
- **AAB** for Play Store distribution — Google Play signing applies on upload;
  the local keystore signs the bundle itself.

## Signing (see [signing.md](signing.md))

- The keystore is **never in the repository**. Resolved from (in order):
  `KEYSTORE_PATH` env → `~/.kaiteyo/keystore.jks` → repo-root `keystore.jks`
  (where CI decodes the `KEYSTORE_BASE64` secret).
- Release signing secrets come from CI secrets (`KEYSTORE_PASS`, `SIGN_KEY`,
  `SIGN_PASS`).
- Without a keystore, builds fall back to **debug signing** — fine for local
  dev, never for distribution.
- F-Droid reproducibility is preserved (`dependenciesInfo.includeInApk = false`).

## Installation / onboarding

- **First install**: app launches into the first-run `OnboardingWizard`
  (JLPT target + daily limits → real decks). It asks **no permissions up
  front**; runtime permissions are requested only when a feature needs them
  (e.g. file picking via SAF). Storage is handled via scoped storage/SAF — the
  app does not request blanket storage access.
- **Update**: versionCode increases monotonically; schema migrations run inside
  the app (SQLDelight user-data migrations), never by the store.
- **Reinstall / restored data**: user data lives in the app sandbox and follows
  the account/backup path; onboarding does not repeat once completed.
- **Downgrade**: prevented by the store; sideloads of older APKs are blocked by
  Android's signature check (same signing key required).

## Store notes

- **Play Store**: googlePlay flavor, AAB upload, release tracks (internal →
  closed → production), versioning from `version.json` only.
- **F-Droid**: fdroid flavor builds from source reproducibly; the app is
  expected to build with the F-Droid toolchain (no proprietary SDK bits — that
  is why Google/Firebase tasks are disabled for the flavor).
- Release checklist items for Android live in
  `docs/releases/RELEASE_CHECKLIST.md`.

## Architecture support

APKs are built for arm64-v8a, armeabi-v7a and x86_64 (see the website download
page — those are the architectures CI actually produces).
