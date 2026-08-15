<div align="center">

  <img src="preview_assets/kaiteyo_logo.svg" height="120" style="border-radius: 20px;">

  # Kaiteyo (書いてよ)

  **Write it. Practice. Master it.**

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
since grown into an independently developed project with its own design language, branding,
data pipeline, and feature set.

Kaiteyo is **desktop-first**: the Windows/macOS/Linux app is a complete immersion
workspace — a Yomitan-style dictionary, an ASBPlayer-style media player, sentence mining,
OCR, and a study engine in one cohesive window. The mobile apps share the same core study
engine (kanji, kana, vocabulary, SRS, writing practice) built on Kotlin Multiplatform.

> **Project status:** actively developed. The desktop suite is the flagship; mobile shares
> the core learning engine. See [docs/features/FEATURES.md](docs/features/FEATURES.md) for a
> per-feature status matrix and [docs/roadmap/ROADMAP.md](docs/roadmap/ROADMAP.md) for what
> is planned.

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

Status legend: ✅ implemented · 🚧 partial / experimental · 📋 planned

### Core study engine (all platforms)

| Feature | Status | Notes |
|---|---|---|
| Kanji & kana study | ✅ | JLPT (N5–N1) and school-grade decks |
| Vocabulary study & flashcards | ✅ | Readings, meanings, furigana, example sentences |
| Writing practice | ✅ | Stroke-order diagrams, drawing canvas, stroke evaluation |
| Spaced repetition (SRS) | ✅ | FSRS-5 based scheduling, custom intervals, daily limits |
| Deck management | ✅ | Create, edit, archive, duplicate, bulk actions |
| Radical & reading search | ✅ | 6000+ characters, dictionary-backed |
| Text analysis | ✅ | Word-by-word breakdown (Ichiran-style output) |
| Statistics & achievements | ✅ | Heatmap, learning curves, goals, achievements, exams |
| Anki `.apkg` import/export | ✅ | Desktop, Android and iOS |
| Backup / restore | ✅ | Profile archives, settings, window state |
| User accounts & sync | 🚧 | GitHub device-flow + private-gist sync (desktop) |
| Grammar study | 🚧 | Desktop suite: explanation-first practice view with starter deck |

### Desktop suite (Windows / macOS / Linux)

| Feature | Status | Notes |
|---|---|---|
| Yomitan-style dictionary | ✅ | Import Yomitan-compatible ZIP/JSON dictionaries; JMdict, KANJIDIC, KanjiVG data |
| Dictionary popup lookup | ✅ | Hover/click on any Japanese text — reading, definitions, mining, TTS |
| Media center | ✅ | VLC / mpv / Java Sound backends; SRT/ASS/SSA/VTT subtitles |
| Subtitle mining | ✅ | Sentence cards from subtitles with screenshot + audio + timestamp |
| Learning browser | ✅ | Reader-mode + JavaFX WebView rendering, lookup & mining |
| OCR | 🚧 | Capture pipeline works; detection engine is Tesseract when available |
| Local HTTP API | ✅ | Bearer-token protected; media, mining, player-state endpoints |
| AnkiConnect integration | ✅ | Push mined cards to Anki; import decks from Anki |
| Auto-update system | 🚧 | Architecture complete (channels, sha256 verification); rollout staged |
| Plugin system | 🚧 | Manifest-driven registry + marketplace scaffold; no runtime loading yet |
| Custom theming (Theme Studio) | ✅ | Color/gradient editors, presets, live preview |
| First-run onboarding | ✅ | 8-step wizard, theme/accent/scale/font/nav/motion |
| Branded installer | ✅ | Inno Setup wizard, styled DMG, AppImage/deb/rpm/Flatpak/Snap |

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

Desktop captures live in [docs/screenshots/](docs/screenshots/README.md).

## Downloads

### Desktop

| Platform | Package |
|---|---|
| Windows | EXE (Inno Setup) + MSI + portable ZIP — [releases](https://github.com/ValiantZippu/Kaiteyo/releases) |
| macOS | DMG (arm64 + x64, signed + notarized) — [releases](https://github.com/ValiantZippu/Kaiteyo/releases) |
| Linux | AppImage, deb, rpm (+ Flatpak/Snap packaging) — [releases](https://github.com/ValiantZippu/Kaiteyo/releases) |

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

## Developer CLI (`kaiteyo`)

The repository ships its own developer command center: a single cross-platform
CLI for the repetitive workflows — Git commits & pushes, Gradle tasks, WSL
utilities, diagnostics, docs and file browsing. Python 3.9+ only, no
installation required.

```bash
# From the repository root (Linux/macOS/WSL) or `kaiteyo.cmd` on Windows
./kaiteyo --help
./kaiteyo                        # interactive command center
./kaiteyo git commit             # status → select → preview → commit → push
./kaiteyo gradle                 # Gradle Command Center (task discovery + search)
./kaiteyo doctor                 # environment diagnostics (PASS / WARN / FAIL)
./kaiteyo info                   # project snapshot
```

Non-interactive (CI-friendly):

```bash
./kaiteyo git commit --all --title "Fix library" --push
./kaiteyo gradle --task :desktopApp:compileKotlinJvm --yes
./kaiteyo doctor --json
./kaiteyo wsl --status
```

For PATH installation, configuration, the full command reference and how to
add new tools, see [`docs/cli/README.md`](docs/cli/README.md).

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
| `tools/cli/` | **Developer command center** — `kaiteyo` CLI (git, gradle, wsl, doctor, docs, …) |

## Documentation

The full documentation lives in [`docs/`](docs/README.md) and is organized like a
documentation site:

| Area | Location |
|---|---|
| 📖 Docs index | [`docs/README.md`](docs/README.md) |
| 📦 Product blueprint | [`docs/product/PRODUCT.md`](docs/product/PRODUCT.md) (MASTER §0–§88) |
| 📊 Current state | [`docs/planning/CURRENT_STATE.md`](docs/planning/CURRENT_STATE.md) |
| 🎮 Game (Journey, target) | [`docs/game/README.md`](docs/game/README.md) |
| 🤖 AI agent guide | [`docs/ai/AI_AGENT_GUIDE.md`](docs/ai/AI_AGENT_GUIDE.md) |
| ⌨️ Developer CLI | [`docs/cli/README.md`](docs/cli/README.md) |
| 🏛️ Architecture (+ ADRs) | [`docs/architecture/`](docs/architecture/OVERVIEW.md) |
| 🧱 Data & attribution | [`docs/data/README.md`](docs/data/README.md) |
| 🔌 Integrations | [`docs/integrations/README.md`](docs/integrations/README.md) |
| 👤 User guide | [`docs/user-guide/README.md`](docs/user-guide/README.md) |
| ⚙️ Development | [`docs/development/`](docs/development/DEVELOPER_GUIDE.md) |
| 🎨 Design system | [`docs/design/README.md`](docs/design/README.md) |
| 🧠 Features | [`docs/features/FEATURES.md`](docs/features/FEATURES.md) |
| 🗺️ Roadmap | [`docs/roadmap/ROADMAP.md`](docs/roadmap/ROADMAP.md) |
| 🖥️ Platforms | [`docs/platform/README.md`](docs/platform/README.md) |
| 🧪 Testing | [`docs/testing/README.md`](docs/testing/README.md) |
| 📦 Releases | [`docs/releases/RELEASE_PROCESS.md`](docs/releases/RELEASE_PROCESS.md) |
| 🔐 Security | [`SECURITY.md`](SECURITY.md) |
| ⚖️ Legal & attribution | [`docs/legal/README.md`](docs/legal/README.md) |
| 📜 Changelog | [`CHANGELOG.md`](CHANGELOG.md) |
| 🐞 Known issues | [`docs/planning/CURRENT_ISSUES.md`](docs/planning/CURRENT_ISSUES.md) |

## Technical stack

- **Language** — Kotlin Multiplatform (2.1.20), Compose Multiplatform 1.8.2
- **Architecture** — shared `core` (business logic + UI) with thin platform entry points; modular screen pattern with Koin DI
- **Data** — SQLDelight (two databases: immutable dictionary + mutable user data), DataStore preferences, JSON state on desktop; `kjd/` generates the bundled language database
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
| [Tanos JLPT lists](http://www.tanos.co.uk/jlpt/) | CC BY 3.0 (per in-app credits) |
| [Leeds frequency data](https://corpus.leeds.ac.uk/list.html) | CC BY 2.5 (per in-app credits) |
| [yomichan-jlpt-vocab](https://github.com/stephenmk/yomichan-jlpt-vocab) | CC BY-SA 4.0 |

See [docs/data/SOURCES.md](docs/data/SOURCES.md) for full provenance, redistribution
requirements, and the KJD generation pipeline.
