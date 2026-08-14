# Android

## Supported status

✅ **Supported** — two flavors distributed via Google Play and F-Droid.

## Flavors

| Flavor | applicationId | Extras |
|---|---|---|
| `googlePlay` | `ua.syt0r.kanji` | Firebase Analytics + Crashlytics, Google Play billing (sponsor), Play review flow, Google Play account/sponsor screens |
| `fdroid` | `ua.syt0r.kanji.fdroid` | No Google services; `adjustFlavorTasks()` disables GoogleServices/Crashlytics/ArtProfile tasks for reproducible F-Droid builds |

Android module: `app/` (entry point, flavors) + `core/` (shared engine, `androidMain`
actuals). Shared engine components (screen pattern, SRS, statistics) are identical to
desktop/iOS.

## Build

```bash
# Debug (applicationId ua.syt0r.kanji.dev)
./gradlew :app:assembleDebug

# Release flavors
./gradlew :app:assembleFdroidRelease     # what CI builds
./gradlew :app:assembleGooglePlayRelease

# On a device
./gradlew :app:installDebug
```

Requires `ANDROID_HOME`/`ANDROID_SDK_ROOT` and a machine-local `local.properties`
(`sdk.dir=...`) — never commit it. Signing keystore resolution: `KEYSTORE_PATH` env →
`~/.kaiteyo/keystore.jks` → repo-root `keystore.jks`; falls back to debug signing when
absent (see `app/build.gradle.kts`).

## Platform specifics

- **SDK levels** — compile/target SDK 35, `minSdk 26` (Android 8.0+).
- **Notifications** — daily review reminders via WorkManager (`ReminderNotification*`).
- **TTS** — kana voice uses the bundled opus voice asset (`AndroidKanaTtsManager`).
- **Media** — ExoPlayer (media3) is available in the shared core (`media3-exoplayer`).
- **File access** — SAF-based import/export (`AndroidTransferFileAccess`,
  `AndroidTransferFilePickerHost`); persistable URI grants allow "re-import last file".
- **Anki `.apkg`** — implemented with Android's built-in `SQLiteDatabase` (schema v11).
- **Backup** — Android backup archive handler (`AndroidBackupArchiveHandler`).
- **Analytics opt-out** — Google Play flavor exposes an Analytics settings category.

## Permissions

The app requires no special runtime permissions for core study. Storage access uses SAF
(user-granted per document). Notification permission is needed for reminders on
Android 13+.

## Input & layout

- Touch-first; supports tablets (form-factor aware navigation).
- Hardware keyboard shortcuts are not a focus on Android.

## Known limitations

- `local.properties` is machine-specific — CI generates its own.
- Firebase services only exist in the `googlePlay` flavor; F-Droid builds must stay
  Google-free.
- Release signing secrets come from CI env vars (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`,
  `KEY_ALIAS`, `KEY_PASSWORD`).
