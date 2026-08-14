<div align="center">

  <img src="preview_assets/kaiteyo_logo.svg" height="120" style="border-radius: 20px;">

  # Kaiteyo (書いてよ)

  **Write it. Practice it. Master it.**

  A premium, cross-platform Japanese language learning application — offline-first,
  desktop-focused, and free.

  ![Version](https://img.shields.io/badge/version-v2.2.1-blue?style=for-the-badge&labelColor=1A1A1A&color=C2FC8B)
  ![License](https://img.shields.io/badge/license-GPL--3.0-green?style=for-the-badge&labelColor=1A1A1A&color=FEAB57)
  ![Platforms](https://img.shields.io/badge/Windows%20%7C%20macOS%20%7C%20Linux%20%7C%20Android%20%7C%20iOS-1A1A1A?style=for-the-badge)

</div>

---

## What is Kaiteyo?

Kaiteyo (書いてよ) — *"write it!"* in Japanese — is a premium application for learning
Japanese. It began as a fork of [Kanji Dojo](https://github.com/syt0r/Kanji-Dojo) and has
since grown into an independently developed project with its own design language, roadmap,
and feature set.

Kaiteyo is **desktop-first**: the Windows/macOS/Linux app is a complete immersion
workspace — a Yomitan-style dictionary, an ASBPlayer-style media player, sentence mining,
OCR, and a study engine in one cohesive window. The mobile apps share the same core study
engine (kanji, kana, vocabulary, SRS, writing practice) built on Kotlin Multiplatform.

> **Project status:** actively developed. The desktop suite is the flagship; mobile
> shares the core learning engine. See [docs/features/FEATURES.md](docs/features/FEATURES.md)
> for a per-feature status matrix and [docs/roadmap/ROADMAP.md](docs/roadmap/ROADMAP.md)
> for what is planned.

## Why Kaiteyo?

Most Japanese learning tools split study into disconnected silos — a flashcard app here,
a dictionary there, a video player somewhere else. Kaiteyo puts them together:

1. **Read or watch something in Japanese.**
2. **Hover a word** — the dictionary popup appears instantly (Yomitan-style).
3. **Mine a sentence** — a card lands in your SRS queue with a screenshot, audio, and timestamp.
4. **Review with spaced repetition** — and jump straight back to the exact scene in the media.

Everything works **offline by default**. Your study data is yours: import/export, Anki
compatibility, backup, and GitHub-based sync are all built in.

## Features at a glance

Status legend: ✅ implemented · 🚧 partial · 📋 planned

### Core study engine (all platforms)

| Feature | Status | Notes |
|---|---|---|
| Kanji & kana study | ✅ | JLPT (N5–N1) and school-grade decks |
| Vocabulary study & flashcards | ✅ | Readings, meanings, furigana |
| Writing practice | ✅ | Stroke-order diagrams, drawing canvas, stroke evaluation |
| Spaced repetition (SRS) | ✅ | FSRS-based scheduling, custom intervals |
| Deck management | ✅ | Create, edit, archive, duplicate, bulk actions |
| Radical & reading search | ✅ | 6000+ characters, dictionary-backed |
| Text analysis | ✅ | Word-by-word breakdown (Ichiran-style output) |
| Statistics & achievements | ✅ | Heatmap, learning curves, goals, achievements |
| Anki `.apkg` import/export | ✅ | On desktop, Android and iOS |
| Backup / restore | ✅ | Profile archives, settings, window state |
| User accounts & sync | 🚧 | GitHub device-flow + private-gist sync (desktop) |

### Desktop suite (Windows / macOS / Linux)

| Feature | Status | Notes |
|---|---|---|
| Yomitan-style dictionary | ✅ | Import Yomitan/EPWING-style ZIP & JSON dictionaries; JMdict, KANJIDIC, KanjiVG data |
| Dictionary popup lookup | ✅ | Hover/click on any Japanese text — reading, definitions, mining, TTS |
| Media center | ✅ | VLC / mpv / Java Sound backends; SRT/ASS/SSA/VTT subtitles |
| Subtitle mining | ✅ | Sentence cards from subtitles with screenshot + audio + timestamp |
| Learning browser | ✅ | Study-friendly web browsing with lookup & mining |
| OCR | 🚧 | Tesseract-backed capture/lookup (screenshot, clipboard, region) |
| Local HTTP API | ✅ | Bearer-token protected; media, mining, player-state endpoints |
| AnkiConnect integration | ✅ | Push mined cards to Anki; import decks from Anki |
| Auto-update system | 🚧 | Architecture complete (channels, sha256 verification); staged rollout |
| Plugin system | 🚧 | Manifest-driven registry + marketplace scaffold; no runtime loading yet |
| Custom theming (Theme Studio) | ✅ | Color/gradient editors, presets, live preview |
| First-run onboarding | ✅ | 8-step wizard, theme/accent/scale/font/nav/motion |

### Mobile

| Feature | Status | Notes |
|---|---|---|
| Android (Play / F-Droid) | ✅ | Play flavor adds Firebase analytics, billing, review |
| iOS | 🚧 | Shared engine + app shell exist; built from macOS only |

## Screenshots

<p float="left">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" height="380"/>
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/2.png" height="380"/>
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/3.png" height="380"/>
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/4.png" height="380"/>
</p>

Desktop captures live in [docs/screenshots/](docs/screenshots/).

## Downloads

### Desktop

| Platform | Package |
|---|---|
| Windows | MSI, EXE (Inno Setup) + portable ZIP — [releases](https://github.com/ValiantZippu/Kaiteyo/releases) |
| macOS | DMG (arm64 + x64, notarized) — [releases](https://github.com/ValiantZippu/Kaiteyo/releases) |
| Linux | AppImage, deb, rpm (+ Flatpak/Snap packaging in progress) — [releases](https://github.com/ValiantZippu/Kaiteyo/releases) |

### Android

[![Google Play](https://img.shields.io/badge/Google_Play-414141?style=for-the-badge&logo=google-play&logoColor=white)](https://play.google.com/store/apps/details?id=ua.syt0r.kanji)
[![F-Droid](https://img.shields.io/badge/F--Droid-1976D2?style=for-the-badge&logo=f-droid&logoColor=white)](https://f-droid.org/en/packages/ua.syt0r.kanji.fdroid/)

### iOS

[![App Store](https://img.shields.io/badge/App_Store-blue?style=for-the-badge&logo=appstore&logoColor=white)](https://apps.apple.com/ua/app/kanji-dojo/id6745169386)

## Quick start (development)

```bash
# Clone
git clone https://github.com/ValiantZippu/Kaiteyo.git
cd Kaiteyo

# Run the desktop app (JDK 17 required)
./gradlew :desktopApp:run

# Japanese UI locale
./gradlew :desktopApp:run -Duser.language=ja -Duser.country=JP

# Compile checks
./gradlew :desktopApp:compileKotlinJvm

# Tests
./gradlew :core:allTests

# Installers (run on the matching host OS)
./gradlew :desktopApp:packageMsi    # Windows
./gradlew :desktopApp:packageDmg    # macOS
./gradlew :desktopApp:packageDeb    # Linux
```

> First build downloads app data assets (dictionary database + TTS voices) from GitHub
> releases — network required. See [docs/development/DEVELOPMENT_SETUP.md](docs/development/DEVELOPMENT_SETUP.md).

## Repository layout

| Path | What it is |
|---|---|
| `core/` | Shared Kotlin Multiplatform code — study engine, UI, data layer (all platforms) |
| `desktopApp/` | Desktop app: native window shell + the standalone desktop suite (dictionary, media, mining, OCR, sync, …) |
| `app/` | Android entry point (flavors: `googlePlay`, `fdroid`) |
| `iosApp/` | iOS entry point (Swift host + Compose UI) |
| `kjd/` | **KJD** — the Kaiteyo Japanese Data Platform: ingests open datasets and generates the offline language database |
| `mediaGenerator/` | JVM utility for generating media assets |
| `installer/` | Branded installer subsystem (Inno Setup, DMG, AppImage/deb/rpm, update feeds) |
| `website/` | Static project website (Python build) |
| `buildSrc/` | Gradle build logic — versions (`AppVersion.kt`) and app assets (`AppAssets.kt`) |

## Documentation

The full documentation lives in [`docs/`](docs/README.md) and is organized like a
documentation site:

| Area | Location |
|---|---|
| 📖 Docs index | [`docs/README.md`](docs/README.md) |
| 🏛️ Architecture | [`docs/architecture/`](docs/architecture/) |
| 🧱 Data & attribution | [`docs/data/`](docs/data/README.md) |
| 🔌 Integrations | [`docs/integrations/`](docs/integrations/README.md) |
| 👤 User guide | [`docs/user-guide/`](docs/user-guide/README.md) |
| ⚙️ Development | [`docs/development/`](docs/development/) |
| 🎨 Design system | [`docs/design/`](docs/design/README.md) |
| 🧠 Features | [`docs/features/FEATURES.md`](docs/features/FEATURES.md) |
| 🗺️ Roadmap | [`docs/roadmap/ROADMAP.md`](docs/roadmap/ROADMAP.md) |
| 🐞 Known issues | [`docs/planning/CURRENT_ISSUES.md`](docs/planning/CURRENT_ISSUES.md) |
| 🧪 Testing | [`docs/testing/README.md`](docs/testing/README.md) |
| 📦 Releases | [`docs/releases/`](docs/releases/) |
| 🔐 Security | [`SECURITY.md`](SECURITY.md) |
| ⚖️ Legal & attribution | [`docs/legal/README.md`](docs/legal/README.md) |

## Technical stack

- **Language** — Kotlin Multiplatform (2.1), Compose Multiplatform 1.8
- **Architecture** — shared `core` (business logic + UI) with thin platform entry points; modular screen pattern with Koin DI
- **Data** — SQLDelight (two databases: immutable dictionary + mutable user data), DataStore preferences, JSON state on desktop
- **Networking** — Ktor client, `java.net.http` for OAuth/sync
- **Desktop media** — VLCJ (VLC), mpv (JSON-RPC), Java Sound
- **Build** — Gradle with version catalog (`gradle/libs.versions.toml`), JDK 17

## Contributing

Contributions of all kinds are welcome — code, documentation, design, data, translations.

1. Read [`CONTRIBUTING.md`](CONTRIBUTING.md) first.
2. Check [`docs/planning/CURRENT_ISSUES.md`](docs/planning/CURRENT_ISSUES.md) for things to fix.
3. Read [`docs/development/CODING_STANDARDS.md`](docs/development/CODING_STANDARDS.md) before writing code.
4. Read [`docs/development/AI_CONTEXT.md`](docs/development/AI_CONTEXT.md) — written for AI-assisted contributors.

Development workflow is branch-based (`develop` is the default branch; PRs target it).
See [`docs/development/GITHUB_WORKFLOW.md`](docs/development/GITHUB_WORKFLOW.md).

## License

Kaiteyo is free software licensed under the **GNU General Public License v3.0**
(or, at your option, any later version). See [`LICENSE`](LICENSE).

> © 2022–2023 Yaroslav Shuliak (original Kanji Dojo). Kaiteyo is a fork of Kanji Dojo,
> independently developed with its own design language, branding, and feature set.
>
> This program is distributed in the hope that it will be useful, but **WITHOUT ANY
> WARRANTY**; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
> PARTICULAR PURPOSE. See the GNU General Public License for more details.

## Data attribution

Kaiteyo bundles openly licensed Japanese-language datasets. Original Kaiteyo code and
third-party datasets remain distinct. Sources include:

| Dataset | License |
|---|---|
| [KanjiVG](https://kanjivg.tagaini.net/) — stroke order data | CC BY-SA 3.0 |
| [KANJIDIC](https://www.edrdg.org/kanjidic/kanjdicindex.html) — character info | CC BY-SA 3.0 |
| [JMdict](https://www.edrdg.org/jmdict/j_jmdict.html) — dictionary | CC BY-SA 4.0 |
| [JmdictFurigana](https://github.com/Doublevil/JmdictFurigana) | CC BY-SA 4.0 |
| [Tanos JLPT lists](http://www.tanos.co.uk/jlpt/) | Free with attribution |
| [Leeds frequency data](https://corpus.leeds.ac.uk/list.html) | Free for research/education |
| [yomichan-jlpt-vocab](https://github.com/stephenmk/yomichan-jlpt-vocab) | CC BY-SA 4.0 |

See [docs/data/SOURCES.md](docs/data/SOURCES.md) for full provenance, redistribution
requirements, and the KJD generation pipeline.
