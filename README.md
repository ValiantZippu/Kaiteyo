<div align="center">

<table>
<tr>
<td align="center" width="100%">

<br>

<img src="https://img.shields.io/badge/書く—Kaku?style=for-the-badge&labelColor=1A1A1A&color=C2FC8B&label=&font=monospace" height="4">

<br><br>

# <img src="https://img.shields.io/badge/Kaiteyo- writeFile()?style=flat-square&labelColor=1A1A1A&color=C2FC8B&logo=kotlin&logoColor=white" height="32">

### <code>書いてよ</code>

<br>

<img src="https://img.shields.io/badge/🟢_Stable-v2.2.1-C2FC8B?style=for-the-badge&labelColor=1A1A1A" height="28">&nbsp;
<img src="https://img.shields.io/badge/⚡_Kotlin_2.1-7F52FF?style=for-the-badge&labelColor=1A1A1A&logo=kotlin&logoColor=white" height="28">&nbsp;
<img src="https://img.shields.io/badge/🎨_Compose_1.8-4285F4?style=for-the-badge&labelColor=1A1A1A&logo=jetpackcompose&logoColor=white" height="28">&nbsp;
<img src="https://img.shields.io/badge/📦_GPL--3.0-FEAB57?style=for-the-badge&labelColor=1A1A1A" height="28">

<br><br>

<table>
<tr>
<td align="center"><img src="https://img.shields.io/badge/🖥️_Desktop-1A1A1A?style=flat-square&logo=windows&logoColor=white" height="22"></td>
<td align="center"><img src="https://img.shields.io/badge/🍎_macOS-1A1A1A?style=flat-square&logo=apple&logoColor=white" height="22"></td>
<td align="center"><img src="https://img.shields.io/badge/🐧_Linux-1A1A1A?style=flat-square&logo=linux&logoColor=white" height="22"></td>
<td align="center"><img src="https://img.shields.io/badge/🤖_Android-1A1A1A?style=flat-square&logo=android&logoColor=white" height="22"></td>
<td align="center"><img src="https://img.shields.io/badge/📱_iOS-1A1A1A?style=flat-square&logo=apple&logoColor=white" height="22"></td>
</tr>
</table>

<br>

> <sub>**Write it. Practice it. Master it.**</sub>
>
> <sub>A premium cross-platform Japanese learning workspace — offline-first, desktop-focused, and free.</sub>
>
> <sub>Built with Kotlin Multiplatform + Compose Multiplatform.</sub>

<br>

<table>
<tr>
<td align="center">

#### 🏠 [Home](#-what-is-kaiteyo)
</td>
<td align="center">

#### ✨ [Features](#-features-at-a-glance)
</td>
<td align="center">

#### 📥 [Download](#-download)
</td>
<td align="center">

#### 🛠️ [Develop](#-quick-start)
</td>
<td align="center">

#### 📖 [Docs](#-documentation)
</td>
</tr>
</table>

</td>
</tr>
</table>

---

## What is Kaiteyo?

<table>
<tr>
<td width="56%" valign="top">

<br>

**Kaiteyo** — *write it!* — is a **premium application** for learning Japanese.

It started as a fork of [Kanji Dojo](https://github.com/syt0r/Kanji-Dojo) and grew into
an independently developed project with its own design language, branding, data pipeline,
and feature set.

### Desktop-first immersion

The desktop app is a **complete immersion workspace**:

| Layer | What it does |
|:---|:---|
| 📚 **Dictionary** | Yomitan-style popup — hover any Japanese word, get instant readings + definitions |
| 🎬 **Media** | ASBPlayer-style video player with subtitle sync, screenshot capture, audio clips |
| ⛏️ **Mining** | One-click sentence cards from dictionary, video, subtitles, OCR, or clipboard |
| 📝 **Study** | FSRS-5 spaced repetition, writing practice, kanji/kana/vocab decks |
| 🔄 **Sync** | GitHub-based device sync, Anki import/export, backup & restore |

### The workflow

```
  Read / Watch ──→ Hover ──→ Mine ──→ Review ──→ Master
  ────────────     ──────    ─────    ────────    ───────
  Japanese text    Popup     Card     SRS         Fluency
  (any source)     Lookup    Created  Schedule    Achieved
```

Everything works **offline by default**. Your data is yours.

</td>
<td width="44%" valign="top" align="center">

<br><br>

```
  ┌────────────────────────────────────┐
  │  📖  Kaiteyo                      │
  ├────────────────────────────────────┤
  │                                    │
  │    食べる  たべる                   │
  │    to eat; to have a meal          │
  │                                    │
  │  ┌──────────┐  ┌──────────┐       │
  │  │ ⛏ Mine   │  │ 🔊 Hear  │       │
  │  └──────────┘  └──────────┘       │
  │                                    │
  │  JLPT N5 · Verb · Ichidan        │
  │                                    │
  └────────────────────────────────────┘

         Dictionary popup

  ┌────────────────────────────────────┐
  │  📊  Today's Review                │
  ├────────────────────────────────────┤
  │  ████████████░░░░  73%  127 cards  │
  │                                    │
  │  New: 15  │  Review: 89  │  Rest: 23│
  └────────────────────────────────────┘

         Study dashboard
```

</td>
</tr>
</table>

---

## ✨ Features at a glance

> `✅` Implemented &nbsp; `🚧` Partial / experimental &nbsp; `📋` Planned

<table>
<tr>
<td width="50%" valign="top">

### 📚 Core Study Engine
<small>All platforms — Android, iOS, Desktop</small>

| Feature | |
|:---|:---:|
| Kanji & kana study (JLPT N5–N1, school grades) | ✅ |
| Vocabulary flashcards with furigana | ✅ |
| Writing practice with stroke evaluation | ✅ |
| FSRS-5 spaced repetition | ✅ |
| Deck management & bulk actions | ✅ |
| Text analysis (Ichiran-style) | ✅ |
| Statistics, heatmap & achievements | ✅ |
| Anki `.apkg` import / export | ✅ |
| Backup, restore & profiles | ✅ |
| GitHub sync & accounts | 🚧 |
| Grammar study | 🚧 |

</td>
<td width="50%" valign="top">

### 🖥️ Desktop Suite
<small>Windows · macOS · Linux</small>

| Feature | |
|:---|:---:|
| Yomitan-style dictionary import | ✅ |
| Hover popup lookup on any text | ✅ |
| Media center (VLC / mpv / Java Sound) | ✅ |
| Subtitle mining with timestamps | ✅ |
| Learning browser with lookup | ✅ |
| Local HTTP API | ✅ |
| AnkiConnect integration | ✅ |
| Custom theming (Theme Studio) | ✅ |
| First-run onboarding wizard | ✅ |
| OCR capture pipeline | 🚧 |
| Plugin system | 🚧 |
| Auto-update system | 🚧 |

</td>
</tr>
</table>

---

## 📥 Download

<table>
<tr>
<td align="center" width="33%">

### 🖥️ Desktop

<br>

<img src="https://img.shields.io/badge/Windows-0078D4?style=for-the-badge&logo=windows&logoColor=white" height="30"><br>
<sub>EXE · MSI · Portable</sub>

<br><br>

<img src="https://img.shields.io/badge/macOS-000000?style=for-the-badge&logo=apple&logoColor=white" height="30"><br>
<sub>DMG (arm64 + x64)</sub>

<br><br>

<img src="https://img.shields.io/badge/Linux-FCC624?style=for-the-badge&logo=linux&logoColor=black" height="30"><br>
<sub>AppImage · deb · rpm</sub>

<br>

[<img src="https://img.shields.io/badge/⬇️_Download-Releases-1A1A1A?style=for-the-badge&color=C2FC8B" height="30">](https://github.com/ValiantZippu/Kaiteyo/releases)

</td>
<td align="center" width="33%">

### 🤖 Android

<br>

[<img src="https://img.shields.io/badge/Google_Play-34A853?style=for-the-badge&logo=google-play&logoColor=white" height="38">](https://play.google.com/store/apps/details?id=ua.syt0r.kanji)
<br><br>
[<img src="https://img.shields.io/badge/F--Droid-1976D2?style=for-the-badge&logo=f-droid&logoColor=white" height="38">](https://f-droid.org/en/packages/ua.syt0r.kanji.fdroid/)
<br><br>
<sub>Play adds Firebase, billing, review</sub>
<sub>F-Droid is reproducible-build clean</sub>

</td>
<td align="center" width="33%">

### 📱 iOS

<br>

[<img src="https://img.shields.io/badge/App_Store-0D96F6?style=for-the-badge&logo=appstore&logoColor=white" height="38">](https://apps.apple.com/ua/app/kanji-dojo/id6745169386)
<br><br>
<sub>Shared engine + Compose UI host</sub>
<sub>Built from macOS only</sub>

</td>
</tr>
</table>

---

## 🛠️ Quick Start

### Prerequisites

```
JDK 17   ·   Android SDK (for mobile)   ·   Network (first build only)
```

### Run the desktop app

```bash
git clone https://github.com/ValiantZippu/Kaiteyo.git
cd Kaiteyo
./gradlew :desktopApp:run
```

<details>
<summary><b>🌏 Japanese UI locale</b></summary>

```bash
./gradlew :desktopApp:run -Duser.language=ja -Duser.country=JP
```
</details>

<details>
<summary><b>🔨 Build & test commands</b></summary>

```bash
# Compile
./gradlew :desktopApp:compileKotlinJvm

# Tests
./gradlew :core:allTests

# Installers (matching host OS)
./gradlew :desktopApp:packageMsi        # Windows
./gradlew :desktopApp:packageDmg        # macOS
./gradlew :desktopApp:packageDeb        # Linux
./gradlew :desktopApp:packageDistributionForCurrentOS  # CI default

# Android
./gradlew :app:assembleDebug
./gradlew :app:assembleFdroidRelease
```
</details>

<details>
<summary><b>⌨️ Developer CLI</b></summary>

```bash
./kaiteyo --help            # full command reference
./kaiteyo                   # interactive command center
./kaiteyo git commit        # status → select → commit → push
./kaiteyo gradle            # Gradle task discovery + search
./kaiteyo doctor            # environment diagnostics
./kaiteyo info              # project snapshot
```
</details>

> **First build** downloads dictionary database + TTS voices from GitHub releases.
> See [`DEVELOPMENT_SETUP.md`](docs/development/DEVELOPMENT_SETUP.md) for full setup.

---

## 📁 Repository Layout

<table>
<tr>
<td width="48%" valign="top">

```
Kaiteyo/
├── core/          Shared KMP code (all platforms)
│                  Study engine · UI · Data layer
│
├── desktopApp/    Desktop shell + suite
│                  Dictionary · Media · Mining · OCR · Sync
│
├── app/           Android entry point
│                  googlePlay · fdroid flavors
│
├── iosApp/        iOS entry (Swift + Compose)
│
├── kjd/           Kaiteyo Japanese Data Platform
│                  Ingests datasets → generates offline DB
│
└── buildSrc/      Versions & asset declarations
```

</td>
<td width="48%" valign="top">

```
├── mediaGenerator/   Media asset generator (JVM)
│
├── installer/        Inno Setup · DMG · AppImage
│                     Not a Gradle module — scripts only
│
├── website/          Static site (Python build)
│
├── brand/            Brand assets pipeline
│                     Source → Processed → Generated
│
├── tools/cli/        Developer command center
│                     kaiteyo CLI (Python 3.9+)
│
└── docs/             Full documentation site
                     Architecture · Features · Roadmap
```

</td>
</tr>
</table>

---

## 📖 Documentation

<table>
<tr>
<td width="33%" valign="top">

**📐 Architecture**
- [`OVERVIEW.md`](docs/architecture/OVERVIEW.md)
- [`DESIGN_SYSTEM.md`](docs/design/DESIGN_SYSTEM.md)
- [`DATA_FLOW.md`](docs/architecture/DATA_FLOW.md)
- [`NAVIGATION.md`](docs/architecture/NAVIGATION.md)

</td>
<td width="33%" valign="top">

**🧠 Features & Design**
- [`FEATURES.md`](docs/features/FEATURES.md)
- [`DESIGN_LANGUAGE.md`](docs/design/DESIGN_LANGUAGE.md)
- [`ANIMATION_SYSTEM.md`](docs/design/ANIMATION_SYSTEM.md)
- [`UI_SYSTEM.md`](docs/design/UI_SYSTEM.md)

</td>
<td width="33%" valign="top">

**🛠️ Development**
- [`AI_CONTEXT.md`](docs/development/AI_CONTEXT.md)
- [`CODING_STANDARDS.md`](docs/development/CODING_STANDARDS.md)
- [`COMMANDS.md`](docs/development/COMMANDS.md)
- [`ROADMAP.md`](docs/roadmap/ROADMAP.md)

</td>
</tr>
</table>

<sub>Full index: [`docs/README.md`](docs/README.md) — 180+ documentation pages covering architecture, ADRs, data sources, testing, releases, and more.</sub>

---

## 🧱 Tech Stack

<table>
<tr>
<td align="center" width="12%">

**Language**
<br>
<code>Kotlin 2.1</code>

</td>
<td align="center" width="12%">

**UI**
<br>
<code>Compose 1.8</code>

</td>
<td align="center" width="12%">

**Platform**
<br>
<code>KMP</code>

</td>
<td align="center" width="12%">

**Data**
<br>
<code>SQLDelight</code>

</td>
<td align="center" width="12%">

**DI**
<br>
<code>Koin</code>

</td>
<td align="center" width="12%">

**Media**
<br>
<code>VLCJ · mpv</code>

</td>
<td align="center" width="12%">

**Build**
<br>
<code>Gradle 8.7</code>

</td>
<td align="center" width="12%">

**Runtime**
<br>
<code>JDK 17</code>

</td>
</tr>
</table>

**Architecture:** Shared `core` with thin platform entry points · Modular screen pattern (Contract → ViewModel → Module → UI) · Koin DI · FSRS-5 scheduling · Two SQLDelight databases (immutable dictionary + mutable user data)

---

## 🤝 Contributing

Contributions of all kinds are welcome — code, docs, design, data, translations.

1. Read [`CONTRIBUTING.md`](CONTRIBUTING.md)
2. Check [`CURRENT_ISSUES.md`](docs/planning/CURRENT_ISSUES.md) for things to fix
3. Read [`CODING_STANDARDS.md`](docs/development/CODING_STANDARDS.md) before writing code
4. Read [`AI_CONTEXT.md`](docs/development/AI_CONTEXT.md) — written for AI-assisted contributors

**Branches:** `main` (production) → `develop` (stable) → `early-develop` (active) → `testing-chamber` (experiments)

---

## 📜 License

Kaiteyo is free software under the **GNU General Public License v3.0** (or later).

<sub>© 2022–2023 Yaroslav Shuliak (original Kanji Dojo). Kaiteyo is independently developed with its own design language, branding, and feature set.</sub>

---

## 📊 Data Attribution

Kaiteyo bundles openly licensed Japanese-language datasets:

| Dataset | Content | License |
|:---|:---|:---|
| [KanjiVG](https://kanjivg.tagaini.net/) | Stroke order data | CC BY-SA 3.0 |
| [KANJIDIC](https://www.edrdg.org/kanjidic/) | Character info | CC BY-SA 3.0 |
| [JMdict](https://www.edrdg.org/jmdict/) | Dictionary | CC BY-SA 4.0 |
| [JmdictFurigana](https://github.com/Doublevil/JmdictFurigana) | Furigana readings | CC BY-SA 4.0 |
| [Tanos JLPT](http://www.tanos.co.uk/jlpt/) | JLPT vocabulary lists | CC BY 3.0 |
| [Leeds frequency](https://corpus.leeds.ac.uk/list.html) | Word frequency data | CC BY 2.5 |
| [yomichan-jlpt-vocab](https://github.com/stephenmk/yomichan-jlpt-vocab) | JLPT vocab tags | CC BY-SA 4.0 |

Full provenance: [`docs/data/SOURCES.md`](docs/data/SOURCES.md)

---

<div align="center">

<img src="https://img.shields.io/badge/書く—書く?style=for-the-badge&labelColor=1A1A1A&color=C2FC8B&label=Built+with+by+the+Kaiteyo+team&font=monospace" height="28">

<br><br>

<sub>**Kaiteyo** · [GitHub](https://github.com/ValiantZippu/Kaiteyo) · [Releases](https://github.com/ValiantZippu/Kaiteyo/releases) · [Docs](docs/README.md) · [Roadmap](docs/roadmap/ROADMAP.md)</sub>

</div>
