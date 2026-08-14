# Kaiteyo (書いてよ) — Feature Status

> Status legend: ✅ **Implemented** · 🚧 **Partial / Experimental** · 📋 **Planned** ·
> 💡 **Future idea** (not scheduled)

This matrix reflects what is **actually in the codebase** today (v2.2.1). Features that
exist as prototypes or scaffolds are labeled as such.

## Core study engine (all platforms — `core/`)

| Feature | Status | Notes |
|---|---|---|
| Kanji & kana study | ✅ | JLPT (N5–N1) + school-grade decks, letter deck details/config |
| Vocabulary study | ✅ | Vocab decks, reading picker (shuffleable), writing mode |
| Flashcards | ✅ | Front/back cards, reveal, furigana on sentences |
| Writing practice | ✅ | Brush canvas (smoothing/prediction/pressure), stroke-order guides |
| Stroke evaluation | ✅ | Count/order/shape scoring; strictness Relaxed/Normal/Exam |
| SRS scheduling | ✅ | FSRS-5, custom intervals, daily limits |
| Review flow | ✅ | Grade (Again/Hard/Good/Easy), bury, suspend, retry, forget, undo |
| Deck management | ✅ | Create/edit/rename/archive/duplicate/delete, bulk actions |
| Tags & flags | ✅ | Tag/flag managers with bulk operations |
| Radical & reading search | ✅ | Radical search, word/sentence search |
| Text analysis | ✅ | Word-by-word breakdown (Ichiran-style), card creation from words |
| Statistics | ✅ | Heatmap, learning curves, retention, goals, study velocity, profile |
| Exams | ✅ | Generated exams + weekly exam, scoring |
| Achievements | ✅ | Multiple achievement categories (learning, mastery, exploration, …) |
| Anki `.apkg` import/export | ✅ | All platforms (JVM/Android/iOS actuals) |
| Backup / restore | ✅ | Profile archives incl. settings + window state |
| Grammar study | 🚧 | Desktop suite `GrammarPracticeView` — explanation-first, starter deck; no bundled grammar dataset |
| Daily review reminders | ✅ | Android (WorkManager) |
| Japanese UI locale | ✅ | `JapaneseStrings` implementation + `-Duser.language=ja` |

## Desktop suite (JVM-only — `desktopApp/.../desktop/`)

| Feature | Status | Notes |
|---|---|---|
| Native window shell | ✅ | 44dp title bar, native drag (Win/Linux), resize zones, system menu, persisted window state |
| Workspace navigation | ✅ | Edge dock (4 positions), expanded/compact/hidden layouts, compact tab bar <720dp, floating launcher |
| Workspace panels | ✅ | Dictionary/Kanji Browser/Stats/Deck Browser/Theme Studio/Search as dock or floating windows, persisted layout |
| Command palette | ✅ | Palette commands (toggle panels, navigation) |
| Yomitan-style dictionary | ✅ | Import Yomitan ZIP/JSON + JMdict/KANJIDIC/KanjiVG; manager, enabled/priority |
| Dictionary popup lookup | ✅ | Hover/click on any Japanese text; readings, definitions, example, tags, TTS |
| Deinflection & segmentation | ✅ | `Deinflect`, `JapaneseSegmenter`; EXACT/PREFIX/KANA/DEINFLECT search modes |
| Media center | ✅ | VLC (VLCJ), mpv (JSON-RPC), Java Sound backends; play/pause/seek/speed/A–B/frame-step |
| Subtitles | ✅ | SRT/ASS/SSA/VTT parse + normalize + sync |
| Subtitle mining | ✅ | Sentence cards with screenshot + audio + timestamp |
| Screenshots & bookmarks | ✅ | Media capture, bookmarks, jump-to-timestamp |
| Learning browser | ✅ | Tabs, bookmarks, downloads, reader mode (JavaFX WebView when available) |
| OCR | 🚧 | Capture pipeline (region/clipboard/image) works; detection needs local Tesseract (Tess4J) |
| Sentence mining | ✅ | Dictionary/subtitle/browser/OCR/clipboard sources; duplicate protection |
| AnkiConnect | ✅ | Push mined cards; import decks/notes/cards from AnkiConnect |
| Local HTTP API | ✅ | Bearer-token localhost server (status/mine/media/player endpoints) |
| Text hook + player WebSocket | ✅ | Subtitle line serving + player state streaming |
| System media keys (Windows) | ✅ | Global hook, only while media loaded, opt-in |
| Activity log | ✅ | Categorized activity history |
| Smart collections & saved filters | ✅ | `SmartCollectionEngine`, `SavedFilterStore` |
| Theme Studio | ✅ | Color wheel + RGB/HSL/HSV/HEX, gradients, motion presets, layout (density/radius/glow), JSON import/export |
| Onboarding wizard | ✅ | 8 steps, live previews, skip-all, reopen from Settings |
| Auto-update | 🚧 | Architecture complete (channels, sha256, rollback window); staged rollout |
| Plugin system | 🚧 | Registry + marketplace scaffold; **no runtime loading** (see `../integrations/PLUGINS.md`) |
| Grammar practice | 🚧 | Explanation-first view with built-in starter deck; `grammar`-tagged cards join |
| Sync (GitHub gist) | 🚧 | Desktop-first; device-flow OAuth; private gist transport |
| KJD database patch updates | ✅ | Incremental base+delta updates applied at runtime |
| First-run data seeding | ✅ | `DemoData` seeds a curated card pool on first launch only |

## Mobile

| Feature | Status | Notes |
|---|---|---|
| Android app | ✅ | Play flavor: Firebase analytics/crashlytics, billing (sponsor), review flow; F-Droid: Google-free reproducible builds |
| iOS app | 🚧 | Shared engine + shell, published; secondary; macOS-only builds |
| Tablet layouts | 🚧 | Form-factor-aware navigation exists; tablet-specific polish partial |
| Mobile sync UI | 🚧 | Sync provider actuals exist; desktop is the primary sync surface |

## Customization & UX

| Feature | Status | Notes |
|---|---|---|
| Themes (Light/Dark/OLED) | ✅ | Base modes + accent schemes |
| Theme persistence | ✅ | DataStore + desktop `settings.json` |
| Custom fonts/scale | ✅ | UI scale + font size settings |
| Reduced motion | ✅ | Motion presets incl. none |
| Keyboard shortcuts | ✅ | Global + review shortcuts, remappable (Shortcuts page) |
| Accessibility | 🚧 | Scale/reduced motion/high contrast exist; screen-reader and full keyboard nav partial |
| Mobile reminders | ✅ | Android notifications |

## Data & sync

| Feature | Status | Notes |
|---|---|---|
| Offline-first | ✅ | Everything works without network |
| Import/export | ✅ | JSON/CSV/TSV/TXT + Anki `.apkg`; preview, validation, conflict policies |
| Backup/restore | ✅ | Profile archives (data + settings + window state) |
| Sync | 🚧 | GitHub gist (desktop-first), no central service |
| KJD data updates | ✅ | Patch feeds (desktop) |

## Platform packaging

| Platform | Status | Notes |
|---|---|---|
| Windows | ✅ | Inno Setup EXE, MSI, portable ZIP |
| macOS | ✅ | DMG arm64/x64, signed + notarized |
| Linux | ✅ | AppImage, deb, rpm; Flatpak/Snap manifests in-tree |
| Android | ✅ | APK (F-Droid), AAB (Play) |
| iOS | 🚧 | App Store build; manual verification |

## Honest gaps (known, not hidden)

- OCR requires an external Tesseract install.
- Plugins cannot be loaded at runtime yet.
- Auto-update applies architecture but rollout is staged.
- Sync is desktop-oriented; no end-to-end encryption of gist content.
- iOS and several platform actuals are verified by build + manual testing, not CI.
- No automated UI tests yet (see `../testing/README.md`).
