# Feature Roadmap & Task Tracking

> What's done, what's in progress, what's planned — the single source of truth.

---

## Status Legend

| Symbol | Meaning |
|--------|---------|
| ✅ | Implemented and working |
| 🚧 | Partial / experimental |
| 📋 | Planned (not started) |
| 🔧 | In progress (active development) |
| 🧪 | Testing / validation |
| 🐛 | Known issues (see `CURRENT_ISSUES.md`) |

---

## 🎯 Current Release: v2.2.x

**Focus:** Desktop suite polish, dictionary improvements, media playback stability.

---

## Core Study Engine (All Platforms)

| Feature | Status | Priority | Notes |
|---------|--------|----------|-------|
| Kanji study (JLPT N5–N1) | ✅ | — | School-grade decks also available |
| Kana study (Hiragana + Katakana) | ✅ | — | Stroke order + writing practice |
| Vocabulary flashcards | ✅ | — | Readings, meanings, furigana |
| Writing practice | ✅ | — | Stroke evaluation, drawing canvas |
| Spaced repetition (FSRS-5) | ✅ | — | Custom intervals, daily limits |
| Deck management | ✅ | — | Create, edit, archive, duplicate |
| Radical search | ✅ | — | 6000+ characters |
| Reading search | ✅ | — | Dictionary-backed |
| Text analysis | ✅ | — | Ichiran-style word breakdown |
| Statistics | ✅ | — | Heatmap, learning curves, goals |
| Achievements | ✅ | — | Gamification elements |
| Exams | ✅ | — | JLPT practice exams |
| Anki import/export | ✅ | — | `.apkg` format |
| Backup/restore | ✅ | — | Profile archives, settings |
| User accounts | 🚧 | 🔧 | GitHub device-flow sync |
| Grammar study | 🚧 | 🔧 | Explanation-first practice view |
| Adaptive learning | 📋 | — | AI-driven difficulty adjustment |
| Reading stories | 📋 | — | Graded reader integration |

---

## Desktop Suite (Windows / macOS / Linux)

| Feature | Status | Priority | Notes |
|---------|--------|----------|-------|
| Yomitan dictionary import | ✅ | — | ZIP/JSON format support |
| Dictionary popup | ✅ | — | Hover/click lookup |
| Media center | ✅ | — | VLC/mpv/Java Sound |
| Subtitle mining | ✅ | — | Sentence cards from subtitles |
| Learning browser | ✅ | — | Reader-mode + WebView |
| Local HTTP API | ✅ | — | Bearer-token protected |
| AnkiConnect integration | ✅ | — | Push cards to Anki |
| Theme Studio | ✅ | — | Color/gradient editors |
| Onboarding wizard | ✅ | — | 8-step setup |
| Custom window chrome | ✅ | 🔧 | Title bar, resize, snap |
| OCR | 🚧 | 🔧 | Tesseract integration |
| Plugin system | 🚧 | 📋 | Manifest-driven registry |
| Auto-update | 🚧 | 📋 | Architecture complete |
| Browser workspace | 🚧 | 📋 | Lightweight study browser |
| Reading workspace | 🚧 | 📋 | EPUB/local file reader |

---

## Mobile

| Feature | Status | Priority | Notes |
|---------|--------|----------|-------|
| Android (Play Store) | ✅ | — | Firebase, billing, review |
| Android (F-Droid) | ✅ | — | Google-free build |
| iOS | 🚧 | 🔧 | Shared engine + Compose host |

---

## 🗺️ Development Roadmap

### Phase 1: Foundation (Current)
- [x] Core study engine (kanji, kana, vocab, SRS)
- [x] Writing practice with stroke evaluation
- [x] Desktop suite shell (dictionary, media, mining)
- [x] Custom window chrome (title bar, resize, snap)
- [ ] Account system & sync
- [ ] Grammar study module

### Phase 2: Desktop Polish
- [ ] OCR integration (Tesseract)
- [ ] Plugin system (manifest-driven)
- [ ] Auto-update system
- [ ] Browser workspace
- [ ] Reading workspace (EPUB/TXT)
- [ ] Performance optimization

### Phase 3: Mobile Parity
- [ ] iOS App Store release
- [ ] Feature parity with desktop study engine
- [ ] Mobile-specific optimizations

### Phase 4: Advanced Features
- [ ] Adaptive learning (AI-driven)
- [ ] Reading stories (graded readers)
- [ ] Game mode (Kaiteyo World)
- [ ] Community features
- [ ] Multiplayer study sessions

---

## 🔥 Active Tasks

<!-- Keep this section updated during development -->

| Task | Assignee | Status | Branch |
|------|----------|--------|--------|
| Custom window chrome polish | AI | ✅ Done | `early-develop` |
| ContentAreaBounds composition local | AI | ✅ Done | `early-develop` |
| README redesign | AI | ✅ Done | `early-develop` |
| Skills documentation | AI | 🔧 In Progress | `early-develop` |

---

## 📋 Backlog

<!-- Features waiting to be picked up -->

- [ ] Command palette (Ctrl+K) for desktop
- [ ] Keyboard shortcut customization UI
- [ ] Drag-and-drop card import
- [ ] Spaced repetition statistics dashboard
- [ ] Deck sharing system
- [ ] Audio pronunciation for all vocab
- [ ] JLPT progress tracking
- [ ] Study streaks & reminders
- [ ] Dark/light theme toggle
- [ ] Multi-monitor support
- [ ] Touch-friendly mobile UI improvements

---

## 🐛 Known Issues

See [`docs/planning/CURRENT_ISSUES.md`](docs/planning/CURRENT_ISSUES.md) for the full bug tracker.

---

## 📊 Metrics

| Metric | Current |
|--------|---------|
| Total features | 42 |
| Implemented | 31 (74%) |
| In progress | 6 (14%) |
| Planned | 5 (12%) |
| Test coverage | Core engine only |
| Documentation pages | 180+ |
| Supported platforms | 5 (Windows, macOS, Linux, Android, iOS) |
