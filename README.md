<div align="center">

  <a href="https://github.com/ValiantZippu/Kaiteyo">
    <img src="assets/readme/kaiteyo_landing.svg" width="100%" alt="Kaiteyo - Write it. Practice. Master it." style="border-radius: 16px;">
  </a>

</div>

<br>

<div align="center">

**[How it works](https://github.com/ValiantZippu/Kaiteyo/tree/develop/docs/architecture)** · **[Study Features](https://github.com/ValiantZippu/Kaiteyo/tree/develop/docs/features)** · **[Desktop Suite](https://github.com/ValiantZippu/Kaiteyo/tree/develop/docs/features)** · **[Downloads](https://github.com/ValiantZippu/Kaiteyo/releases)** · **[Documentation](https://github.com/ValiantZippu/Kaiteyo/tree/develop/docs)** · **[Contributing](https://github.com/ValiantZippu/Kaiteyo/blob/develop/CONTRIBUTING.md)**

</div>

<br>

---

<br>

### ▸ How it works

| Step | What happens |
|:---:|:---|
| **1. Read** | Japanese content in the built-in browser, media player, or reading environment |
| **2. Hover** | Instant dictionary popup with readings, definitions, and TTS pronunciation |
| **3. Mine** | Card with screenshot, audio clip, and timestamp lands in your SRS queue |
| **4. Review** | Spaced repetition with FSRS-5 — jump back to the exact scene in the media |

> Everything works **offline**. Your data is yours — import, export, Anki, backup, sync.

<br>

---

<br>

### ▸ Core Study Engine

*All platforms — Desktop · Android · iOS*

| | Feature | Status |
|:---:|:---|:---|
| ✅ | **Kanji & Kana** — JLPT N5–N1 + school-grade decks | Implemented |
| ✅ | **Vocabulary** — readings, meanings, furigana, examples | Implemented |
| ✅ | **Writing Practice** — stroke-order, brush canvas, evaluation | Implemented |
| ✅ | **Spaced Repetition** — FSRS-5, custom intervals, daily limits | Implemented |
| ✅ | **Deck Management** — create, edit, archive, bulk actions | Implemented |
| ✅ | **Radical Search** — 6000+ characters, dictionary-backed | Implemented |
| ✅ | **Text Analysis** — word-by-word breakdown (Ichiran-style) | Implemented |
| ✅ | **Statistics** — heatmap, curves, goals, exams, achievements | Implemented |
| ✅ | **Anki Import/Export** — `.apkg` on all platforms | Implemented |
| ✅ | **Backup & Restore** — profile archives, settings, window state | Implemented |
| 🚧 | **Sync** — GitHub device-flow + private-gist | Partial |
| 🚧 | **Grammar** — explanation-first practice with starter deck | Partial |

<br>

---

<br>

### ▸ Desktop Suite

*Windows · macOS · Linux — the flagship experience*

| | Feature | Status |
|:---:|:---|:---|
| ✅ | **Dictionary** — Yomitan-style, ZIP/JSON, JMdict, KANJIDIC | Implemented |
| ✅ | **Popup Lookup** — hover any text → readings, mining, TTS | Implemented |
| ✅ | **Media Center** — VLC / mpv, SRT/ASS/SSA/VTT subtitles | Implemented |
| ✅ | **Subtitle Mining** — screenshot + audio + timestamp cards | Implemented |
| ✅ | **Learning Browser** — reader mode, bookmarks, lookup & mining | Implemented |
| ✅ | **Local API** — bearer-token HTTP server | Implemented |
| ✅ | **AnkiConnect** — push/import from Anki | Implemented |
| ✅ | **Theme Studio** — HSV wheel, gradients, presets, live preview | Implemented |
| ✅ | **Onboarding** — 8-step wizard | Implemented |
| ✅ | **Installers** — Inno Setup, DMG, AppImage/deb/rpm/Flatpak | Implemented |
| ✅ | **Native Window** — drag, resize, snap, rounded corners | Implemented |
| ✅ | **Float Launcher** — draggable bubble, snap-to-edge | Implemented |
| ✅ | **Overlay Sidebar** — 4 positions, elevated surface | Implemented |
| 🚧 | **OCR** — capture pipeline, Tesseract detection | Partial |
| 🚧 | **Auto-Update** — architecture complete, staged rollout | Partial |
| 🚧 | **Plugins** — registry + marketplace scaffold | Partial |

<br>

---

<br>

### ▸ Mobile

| | Platform | Status |
|:---:|:---|:---|
| ✅ | **Android** — [Google Play](https://play.google.com/store/apps/details?id=ua.syt0r.kanji) + [F-Droid](https://f-droid.org/en/packages/ua.syt0r.kanji.fdroid/) | Released |
| 🚧 | **iOS** — [App Store](https://apps.apple.com/ua/app/kanji-dojo/id6745169386) — shared engine, macOS builds | Partial |

<br>

---

<br>

### ▸ Downloads

| Platform | Link |
|:---:|:---|
| **Windows** | [Download EXE / MSI / Portable](https://github.com/ValiantZippu/Kaiteyo/releases) |
| **macOS** | [Download DMG](https://github.com/ValiantZippu/Kaiteyo/releases) — arm64 + x64, signed + notarized |
| **Linux** | [AppImage](https://github.com/ValiantZippu/Kaiteyo/releases) · deb · rpm · Flatpak · Snap |
| **Android** | [Google Play](https://play.google.com/store/apps/details?id=ua.syt0r.kanji) · [F-Droid](https://f-droid.org/en/packages/ua.syt0r.kanji.fdroid/) |
| **iOS** | [App Store](https://apps.apple.com/ua/app/kanji-dojo/id6745169386) |

<br>

---

<br>

### ▸ Development

```
  # Requirements: JDK 17
  git clone https://github.com/ValiantZippu/Kaiteyo.git
  cd Kaiteyo

  ./gradlew :desktopApp:run
  ./gradlew :desktopApp:compileKotlinJvm
  ./gradlew :core:allTests
```

<br>

---

<br>

### ▸ Documentation

| | Area | Link |
|:---:|:---|:---|
| 📖 | **Index** | [`docs/README.md`](docs/README.md) |
| 📦 | **Product** | [`docs/product/PRODUCT.md`](docs/product/PRODUCT.md) |
| 🏛️ | **Architecture** | [`docs/architecture/OVERVIEW.md`](docs/architecture/OVERVIEW.md) |
| 🎨 | **Design System** | [`docs/design/README.md`](docs/design/README.md) |
| 🧠 | **Features** | [`docs/features/FEATURES.md`](docs/features/FEATURES.md) |
| 🗺️ | **Roadmap** | [`docs/roadmap/ROADMAP.md`](docs/roadmap/ROADMAP.md) |
| 🎮 | **Game** | [`docs/game/README.md`](docs/game/README.md) |
| 🔌 | **Integrations** | [`docs/integrations/README.md`](docs/integrations/README.md) |
| 👤 | **User Guide** | [`docs/user-guide/README.md`](docs/user-guide/README.md) |
| ⚙️ | **Development** | [`docs/development/DEVELOPER_GUIDE.md`](docs/development/DEVELOPER_GUIDE.md) |
| 🧪 | **Testing** | [`docs/testing/README.md`](docs/testing/README.md) |
| 🤖 | **AI Guide** | [`docs/ai/AI_AGENT_GUIDE.md`](docs/ai/AI_AGENT_GUIDE.md) |
| ⌨️ | **CLI** | [`docs/cli/README.md`](docs/cli/README.md) |
| 📜 | **Changelog** | [`CHANGELOG.md`](CHANGELOG.md) |
| 🐞 | **Issues** | [`docs/planning/CURRENT_ISSUES.md`](docs/planning/CURRENT_ISSUES.md) |

<br>

---

<br>

### ▸ Contributing

```bash
# Fork → clone → branch from develop
git checkout -b feature/my-feature

# Verify
./gradlew :desktopApp:compileKotlinJvm
./gradlew :core:allTests

# Commit & push
git commit -m "feat: add my feature"
git push origin feature/my-feature
```

1. Read [`CONTRIBUTING.md`](CONTRIBUTING.md)
2. Read [`docs/development/CODING_STANDARDS.md`](docs/development/CODING_STANDARDS.md)
3. Read [`docs/development/AI_CONTEXT.md`](docs/development/AI_CONTEXT.md)

<br>

---

<br>

<sub style="color:#606060;">Kaiteyo is free and open source under the [GNU General Public License v3.0](LICENSE) · © 2022–2023 Yaroslav Shuliak (original Kanji Dojo) · independently developed</sub>
