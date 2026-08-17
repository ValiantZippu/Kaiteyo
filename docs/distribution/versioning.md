# Versioning

Kaiteyo uses **Semantic Versioning** (`MAJOR.MINOR.PATCH`, with optional
`-prerelease` / `+build` metadata for beta/nightly builds).

## The single source of truth

| File | Field | Role |
|---|---|---|
| `installer/common/version.json` | `version`, `version_code`, `release_channel`, `app_id` | Everything: installer scripts, update feeds, artifact names, website |
| `buildSrc/src/main/kotlin/AppVersion.kt` | `versionCode`, `versionName`, `desktopAppVersion` | Gradle: Android versionCode/versionName, desktop versioning |

**Never edit them apart.** Bump both together:

```bash
bash installer/scripts/bump-version.sh 2.3.0 2310
```

`version_code` is a monotonically increasing build number (convention: previous
+ 10) so `stable → beta → nightly` and downgrade guards compare correctly.

## How the version propagates

```
version.json + AppVersion.kt
   ├── Android versionCode/versionName        (app/build.gradle.kts)
   ├── Windows EXE/MSI product version        (kaiteyo.iss, MSI metadata)
   ├── Linux deb/rpm/Flatpak/Snap versions    (builders read version.json)
   ├── PKGBUILD pkgver                        (installer/linux/arch/)
   ├── Artifact file names                    (Kaiteyo-{version}-{platform}.{ext})
   ├── Update feed manifests                  (update-{channel}.json)
   ├── Website                                (website/config/site.json)
   └── Release notes / changelog              (CHANGELOG.md)
```

## Channels (see [updates.md](updates.md))

| Channel | Version shape | Who gets it |
|---|---|---|
| stable | `2.2.1` | everyone (default) |
| beta | `2.3.0-beta.1` | opt-in testers |
| nightly / dev | `2.3.0-dev.{n}` | contributors |

The app never auto-switches channels, and never downgrades below the installed
version (`UpdatePolicy` min-version guard).

## Rules

1. A release's `version` in `version.json` must equal `versionName` in
   `AppVersion.kt` — CI's `verify-artifacts.sh` and the release checklist
   enforce this.
2. `desktopAppVersion` must be exactly three numbers.
3. Never hardcode a version inside an installer script, a build file, or a
   package manifest — read `version.json` instead.
4. Pre-release versions never go to the stable channel feed.
