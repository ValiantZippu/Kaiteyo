# iOS

## Supported status

🚧 **Partial** — the shared engine and app shell exist and are published to the App
Store, but iOS is secondary to desktop. Built from **macOS only** (Kotlin/Native targets
cannot build on Windows; `kotlin.native.ignoreDisabledTargets=true`).

## Build

```bash
# Compile the shared framework
./gradlew :core:linkDebugFrameworkIosArm64      # or IosSimulatorArm64 / IosX64

# Open the Xcode project
open iosApp/KaiteyoApp.xcodeproj
```

`iosApp/` hosts: Swift entry point (`KaiteyoApp.swift`, `ContentView.swift`), Swift
platform helpers (`SwiftBackupArchiveHandler`, `SwiftTtsKanaManager`,
`SwiftWanakanaJapaneseUtils`), and Kotlin `iosMain` actuals (`IosKotlinApplication`,
`IosKotlinViewController`, file handlers, database providers, transfer codecs).

## Platform specifics

- **Anki `.apkg`** — implemented via SQLDelight `NativeSqliteDriver` (sqlite3 linked via
  `linkSqlite = true`) + a dependency-free pure-Kotlin ZIP/inflate codec (`IosZip.kt`,
  `IosInflate.kt`).
- **File pickers** — `UIDocumentPickerViewController` for import ("open picker") and
  export ("Save to Files").
- **TTS** — AVSpeechSynthesizer-based kana voice (`SwiftTtsKanaManager`).
- **Furigana/Japanese utils** — Wanakana-based Swift helpers.
- **Backup** — Swift-based backup archive handler.
- **Sync** — iOS sync backup file provider (`IosSyncBackupFileProvider`).

## File system

Data lives in the app sandbox (standard iOS container paths); there is no
`~/.kaiteyo`-style desktop suite on iOS (dictionary import, media center, mining, OCR,
browser, and local API are desktop-only).

## Input & layout

- Touch-first; form-factor-aware navigation; back gestures via `MultiplatformBackHandler`.

## Known limitations

- Requires a macOS host with Xcode; iOS build verification is manual (several
  `CURRENT_ISSUES` entries are marked "requires a macOS build to verify").
- App Store distribution uses the Kanji Dojo-era listing URL
  (`apps.apple.com/ua/app/kanji-dojo/id6745169386`).
- The desktop suite is not available on iOS.
