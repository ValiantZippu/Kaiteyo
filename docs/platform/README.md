# Platform Documentation

Kaiteyo targets five platforms from one Kotlin Multiplatform codebase. This section
documents each platform's support status, build/packaging, behavior, and limitations.

| Platform | Status | Entry point | Packages |
|---|---|---|---|
| [Windows](WINDOWS.md) | ✅ supported | `desktopApp/` (JVM) | EXE (Inno Setup), MSI, portable ZIP |
| [macOS](MACOS.md) | ✅ supported | `desktopApp/` (JVM) | DMG (arm64 + x64, signed + notarized) |
| [Linux](LINUX.md) | ✅ supported | `desktopApp/` (JVM) | AppImage, deb, rpm, Flatpak, Snap |
| [Android](ANDROID.md) | ✅ supported | `app/` (flavors `googlePlay`, `fdroid`) | APK (F-Droid), AAB (Play) |
| [iOS](IOS.md) | 🚧 partial | `iosApp/` (Swift host + Compose) | App Store |

## Shared notes

- **Desktop is the flagship.** The desktop suite (dictionary, media, mining, OCR, browser,
  local API) exists only in `desktopApp/` — see `../user-guide/DESKTOP_SUITE.md`.
- **Mobile shares the core study engine** (kanji, kana, vocabulary, writing, SRS, decks,
  statistics) from `core/` — see `../architecture/OVERVIEW.md`.
- **All targets build from the same Gradle project** with JDK 17
  (`jvmToolchain(17)`, Kotlin/API 2.1). iOS targets cannot be built on Windows — that is
  expected (`kotlin.native.ignoreDisabledTargets=true` in `gradle.properties`).
- Build/installer details beyond per-platform notes: `../releases/RELEASE_PROCESS.md` and
  `installer/README.md`.
