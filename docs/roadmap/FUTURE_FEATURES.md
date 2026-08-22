# Kaiteyo — Future Features Roadmap

> Every feature Kaiteyo could build, organized by domain, priority, and effort.
> This is the master reference for product planning.

---

## 1. Language Learning Core

### 1.1 SRS & Study Engine

| Feature | Priority | Effort | Status |
|---------|----------|--------|--------|
| FSRS-5 scheduler | P0 | ✅ Done | Core SRS engine |
| Daily review limits | P0 | ✅ Done | Configurable caps |
| Custom study sessions | P1 | 3 days | Study filtered subsets |
| Cram mode (all cards) | P1 | 1 day | Review without SRS scheduling |
| Preview mode | P1 | 1 day | See cards without grading |
| Anki-style decks | P0 | ✅ Done | Deck management |
| Deck sharing | P2 | 5 days | Export/import deck bundles |
| Multi-deck study | P1 | 3 days | Study across multiple decks |
| Tag-based filtering | P1 | ✅ Done | Study by tags |
| Difficulty-based sessions | P2 | 2 days | Focus on hard cards |
| New card limit per day | P0 | ✅ Done | Daily new card cap |
| Interval modifier | P2 | 2 days | Global interval adjustment |
| Lapse settings | P1 | 2 days | Configure relearning steps |
| Graduating interval | P1 | 1 day | Interval after learning phase |

### 1.2 Writing Practice

| Feature | Priority | Effort | Status |
|---------|----------|--------|--------|
| Stroke evaluation | P0 | ✅ Done | Real-time stroke matching |
| Stroke order hints | P0 | ✅ Done | Show correct order |
| Radical decomposition | P1 | ✅ Done | Show component parts |
| Writing accuracy tracking | P0 | ✅ Done | Per-character accuracy |
| Kana writing | P0 | ✅ Done | Hiragana + katakana |
| Writing speed metrics | P2 | 1 day | Time per character |
| Handwriting recognition | P2 | 5 days | ML-based stroke recognition |
| Stylus pressure sensitivity | P3 | 3 days | Pressure-aware evaluation |
| Writing journal | P2 | 3 days | History of writing practice |

### 1.3 Reading Practice

| Feature | Priority | Effort | Status |
|---------|----------|--------|--------|
| Text analysis | P0 | ✅ Done | Word-by-word breakdown |
| Furigana toggle | P0 | ✅ Done | Show/hide readings |
| Reading mode (EPUB) | P1 | 5 days | Read EPUB books with popup |
| Reading mode (HTML) | P1 | 3 days | Read web pages with popup |
| Reading mode (TXT) | P1 | 2 days | Read plain text files |
| Reading mode (PDF) | P2 | 5 days | Read PDF documents |
| Reading progress | P2 | 2 days | Track reading position |
| Reading bookmarks | P1 | ✅ Done | Save reading positions |
| Reading history | P1 | ✅ Done | Track what was read |
| Reading statistics | P2 | 3 days | Words read, time spent |

### 1.4 Listening Practice

| Feature | Priority | Effort | Status |
|---------|----------|--------|--------|
| Audio playback | P0 | ✅ Done | Media centre audio |
| Subtitle sync | P0 | ✅ Done | SRT/ASS/SSA/VTT |
| Listening comprehension | P2 | 5 days | Quiz from audio |
| Dictation mode | P2 | 5 days | Write what you hear |
| Speed control | P1 | ✅ Done | 0.75x – 2.0x |
| A-B repeat | P1 | ✅ Done | Loop section |
| Podcast integration | P3 | 10 days | Import podcast feeds |

---

## 2. Dictionary & Reference

| Feature | Priority | Effort | Status |
|---------|----------|--------|--------|
| JMdict bundle | P0 | ✅ Done | 200k+ entries |
| Yomitan import (ZIP) | P0 | ✅ Done | Import Yomitan dictionaries |
| Yomitan import (folder) | P0 | ✅ Done | Import extracted folders |
| Dictionary popup | P0 | ✅ Done | Hover/click lookup |
| Pitch accent display | P1 | 2 days | Show pitch patterns |
| Frequency display | P1 | 1 day | Show frequency rank |
| JLPT/grade badges | P1 | 1 day | Classification tags |
| Radical decomposition | P0 | ✅ Done | Component breakdown |
| Sentence examples | P1 | 2 days | Example sentences |
| Conjugation table | P2 | 2 days | All verb/adj forms |
| EPWING import | P2 | 3 days | EPWING dictionary format |
| Handlebars templates | P2 | 5 days | Custom entry layouts |
| Dictionary update | P2 | 3 days | Incremental updates |
| Custom dictionaries | P2 | 3 days | User-created dictionaries |
| Multi-dictionary priority | P1 | 1 day | Configurable ranking |
| AnkiConnect bridge | P1 | 3 days | Live Anki sync |

---

## 3. Statistics & Analytics

| Feature | Priority | Effort | Status |
|---------|----------|--------|--------|
| Activity heatmap | P0 | ✅ Done | 52-week + yearly view |
| Review statistics | P0 | ✅ Done | Retention, accuracy, grades |
| Study time tracking | P0 | ✅ Done | AFK-aware engagement |
| Knowledge profile | P1 | ✅ Done | JLPT/coverage estimates |
| Writing statistics | P1 | ✅ Done | Accuracy, attempts |
| Exam statistics | P1 | ✅ Done | Scores, trends |
| Velocity tracking | P1 | ✅ Done | Reviews/week, new/week |
| Forecast | P2 | 3 days | Predict completion date |
| Goal tracking | P1 | ✅ Done | Daily/weekly goals |
| Streak tracking | P0 | ✅ Done | Current + longest streak |
| Comparison (week/month) | P2 | 2 days | Period-over-period |
| Export (CSV/JSON) | P1 | ✅ Done | Statistics export |
| Detailed day report | P1 | ✅ Done | Per-day drill-down |
| Card maturity distribution | P1 | ✅ Done | Age/interval analysis |

---

## 4. Desktop Suite Features

### 4.1 Media Centre

| Feature | Priority | Effort | Status |
|---------|----------|--------|--------|
| Video playback | P0 | ✅ Done | VLC/mpv backends |
| Audio playback | P0 | ✅ Done | Java Sound API |
| Subtitle engine | P0 | ✅ Done | SRT/ASS/SSA/VTT |
| Dictionary popup on subtitles | P0 | ✅ Done | Hover to lookup |
| Mining from subtitles | P0 | ✅ Done | Create cards from subs |
| Screenshot capture | P1 | ✅ Done | Frame capture |
| Bookmark system | P1 | ✅ Done | Save timestamps |
| Media library | P1 | ✅ Done | Browse imported media |
| Playlists | P1 | ✅ Done | Organize media |
| Watch history | P1 | ✅ Done | Track progress |
| A-B repeat | P1 | ✅ Done | Loop section |
| Playback speed | P1 | ✅ Done | 0.75x – 2.0x |
| Frame stepping | P2 | 1 day | Step forward/back |
| Media search | P1 | ✅ Done | Search library |
| Drag-and-drop import | P1 | ✅ Done | Drop files to import |

### 4.2 Learning Browser

| Feature | Priority | Effort | Status |
|---------|----------|--------|--------|
| Local HTML viewer | P1 | 3 days | Read local HTML files |
| Dictionary popup | P1 | ✅ Done | Lookup on any text |
| Mining from browser | P1 | ✅ Done | Create cards from text |
| OCR integration | P1 | ✅ Done | Screenshot OCR |
| Reading mode | P2 | 5 days | Clean reading view |
| Tab management | P2 | 3 days | Multiple tabs |
| Bookmark system | P2 | 2 days | Save pages |
| History | P1 | ✅ Done | Browse history |

### 4.3 OCR Engine

| Feature | Priority | Effort | Status |
|---------|----------|--------|--------|
| Screenshot OCR | P1 | ✅ Done | Capture + recognize |
| Image OCR | P1 | ✅ Done | OCR from image files |
| Clipboard OCR | P1 | ✅ Done | OCR clipboard content |
| Tesseract backend | P1 | ✅ Done | Desktop OCR engine |
| ML Kit backend | P2 | 5 days | Mobile OCR |
| OCR history | P1 | ✅ Done | Past OCR results |
| OCR → Dictionary | P1 | ✅ Done | Auto-lookup results |
| OCR → Mining | P1 | ✅ Done | Create cards from OCR |

### 4.4 Mining Engine

| Feature | Priority | Effort | Status |
|---------|----------|--------|--------|
| Dictionary mining | P0 | ✅ Done | Mine from dictionary |
| Subtitle mining | P0 | ✅ Done | Mine from subtitles |
| OCR mining | P1 | ✅ Done | Mine from OCR |
| Clipboard mining | P1 | ✅ Done | Mine from clipboard |
| Browser mining | P1 | ✅ Done | Mine from browser |
| Duplicate protection | P1 | ✅ Done | MinedRecord tracking |
| Mining sources tracking | P1 | ✅ Done | Provenance metadata |
| Batch mining | P2 | 3 days | Mine multiple items |
| Mining templates | P2 | 2 days | Custom card templates |

---

## 5. Navigation & UX

| Feature | Priority | Effort | Status |
|---------|----------|--------|--------|
| Sidebar navigation | P0 | ✅ Done | Desktop sidebar |
| Floating bubble | P0 | ✅ Done | Draggable launcher |
| Bottom bar (phone) | P0 | ✅ Done | Mobile navigation |
| Command palette (Ctrl+K) | P0 | ✅ Done | Quick actions |
| Universal search | P0 | ✅ Done | Cross-content search |
| Keyboard shortcuts | P0 | ✅ Done | Configurable shortcuts |
| Drag-and-drop reorder | P1 | ✅ Done | Reorderable lists |
| Context menus | P1 | ✅ Done | Right-click actions |
| Responsive layout | P0 | ✅ Done | Phone/tablet/desktop |
| Adaptive content width | P0 | ✅ Done | Window-aware sizing |
| Animated transitions | P0 | ✅ Done | Page transitions |
| Theme transition | P0 | ✅ Done | Smooth theme switching |
| Auto-hide navigation | P1 | ✅ Done | Smart hide/show |
| Multi-monitor support | P1 | ✅ Done | Work-area aware |

---

## 6. Game & Gamification

| Feature | Priority | Effort | Status |
|---------|----------|--------|--------|
| 2D world exploration | P1 | ✅ Done | Canvas-based game |
| NPC dialogue | P1 | ✅ Done | Japanese conversations |
| Quest system | P1 | ✅ Done | Story-driven learning |
| Collectibles | P1 | ✅ Done | Kanji spirits |
| Photography | P1 | ✅ Done | In-game camera |
| Season cycle | P2 | ✅ Done | Weather/time system |
| Controller support | P1 | ✅ Done | Gamepad input |
| Touch controls | P1 | ✅ Done | Virtual joystick |
| Save system | P1 | ✅ Done | Multiple save slots |
| Story UI | P1 | ✅ Done | Chapter/scene browser |
| Writing minigame | P2 | ✅ Done | In-world kana practice |
| NPC schedules | P2 | ✅ Done | Waypoint movement |
| 3D rendering | P3 | 30+ days | Upgrade from 2D canvas |
| Multiplayer | P3 | 60+ days | Shared learning world |

---

## 7. Data & Sync

| Feature | Priority | Effort | Status |
|---------|----------|--------|--------|
| Local backup | P0 | ✅ Done | Export/import data |
| Automatic backup | P1 | ✅ Done | Scheduled backups |
| GitHub device-flow auth | P1 | ✅ Done | Account linking |
| Gist sync | P1 | ✅ Done | Sync via GitHub gists |
| Cloud sync | P2 | 10 days | Real-time cloud sync |
| Multi-device sync | P2 | 15 days | Sync across devices |
| Conflict resolution | P2 | 5 days | Handle sync conflicts |
| Sync status indicator | P1 | ✅ Done | Show sync state |
| Selective sync | P3 | 5 days | Choose what to sync |

---

## 8. Platform-Specific

### 8.1 Desktop

| Feature | Priority | Effort | Status |
|---------|----------|--------|--------|
| Native window chrome | P0 | ✅ Done | Custom title bar |
| Window state persistence | P0 | ✅ Done | Remember position/size |
| System tray | P2 | 2 days | Minimize to tray |
| Global hotkeys | P1 | ✅ Done | System-wide shortcuts |
| Auto-update | P1 | ✅ Done | Check for updates |
| Installer (MSI/DMG/AppImage) | P0 | ✅ Done | Platform installers |
| Portable mode | P1 | ✅ Done | No-install mode |
| Multi-window | P3 | 10 days | Multiple app windows |

### 8.2 Android

| Feature | Priority | Effort | Status |
|---------|----------|--------|--------|
| Material Design 3 | P0 | ✅ Done | Native Android UI |
| Widget | P2 | 5 days | Home screen widget |
| Notification | P1 | ✅ Done | Review reminders |
| Share intent | P1 | ✅ Done | Share text to Kaiteyo |
| SAF file picker | P0 | ✅ Done | System file picker |
| F-Droid build | P0 | ✅ Done | Reproducible builds |
| Play Store build | P0 | ✅ Done | Google Play variant |
| Background sync | P2 | 3 days | Sync when app is backgrounded |

### 8.3 iOS

| Feature | Priority | Effort | Status |
|---------|----------|--------|--------|
| SwiftUI shell | P1 | ✅ Done | iOS entry point |
| .apkg import | P1 | ✅ Done | Pure Kotlin ZIP/inflate |
| File picker | P1 | ✅ Done | UIDocumentPickerViewController |
| Widget | P3 | 5 days | Home screen widget |
| Shortcuts (Siri) | P3 | 3 days | Siri integration |
| iCloud sync | P3 | 10 days | CloudKit sync |

---

## 9. Content & Education

| Feature | Priority | Effort | Status |
|---------|----------|--------|--------|
| JLPT courses | P0 | ✅ Done | N5–N1 structured courses |
| Grade courses | P0 | ✅ Done | Japanese school grades |
| Grammar patterns | P1 | ✅ Done | Starter grammar catalog |
| Sentence corpus | P1 | ✅ Done | Example sentences |
| Text analysis | P0 | ✅ Done | Word-by-word breakdown |
| Reading comprehension quizzes | P2 | 5 days | Quiz from reading passages |
| Listening comprehension quizzes | P2 | 5 days | Quiz from audio |
| Flashcard creation | P0 | ✅ Done | Manual card creation |
| Bulk card import | P0 | ✅ Done | Import from files |
| Card export | P0 | ✅ Done | Export to .apkg/CSV |
| Mnemonic system | P1 | ✅ Done | User/AI mnemonics |
| Spaced repetition for mnemonics | P2 | 2 days | Review mnemonics |
| Kanji decomposition database | P1 | ✅ Done | Component breakdown |
| Frequency analysis | P1 | ✅ Done | Word frequency bands |
| Pitch accent database | P2 | 3 days | Accent patterns |

---

## 10. Community & Social

| Feature | Priority | Effort | Status |
|---------|----------|--------|--------|
| Deck marketplace | P3 | 20 days | Share/browse community decks |
| Shared statistics | P3 | 10 days | Compare with other learners |
| Achievement system | P2 | 5 days | Badges and milestones |
| Daily challenges | P2 | 5 days | Daily review goals |
| Streak leaderboard | P3 | 5 days | Community streaks |
| Study groups | P3 | 15 days | Group learning sessions |
| Content contributions | P3 | 10 days | Community mnemonics/sentences |

---

## 11. Developer Experience

| Feature | Priority | Effort | Status |
|---------|----------|--------|--------|
| Plugin system | P3 | 30 days | Extensible architecture |
| Local HTTP API | P2 | 5 days | REST API for integrations |
| Webhook support | P3 | 3 days | Event notifications |
| CLI tool | P3 | 10 days | Command-line interface |
| Theme marketplace | P3 | 10 days | Share/import themes |
| Dictionary API | P2 | 5 days | Programmatic dictionary access |
| Statistics API | P2 | 3 days | Export statistics data |

---

## 12. Recommended Implementation Order

### Quarter 1: Polish & Completeness
1. Pitch accent display (dictionary)
2. Frequency display (dictionary)
3. JLPT/grade badges (dictionary)
4. Sentence examples (dictionary)
5. Conjugation table (dictionary)
6. Multi-dictionary priority
7. EPUB reader mode
8. AnkiConnect bridge

### Quarter 2: Mobile Enhancement
1. ML Kit OCR (Android)
2. Widget (Android)
3. Background sync (Android)
4. iCloud sync (iOS)
5. Siri Shortcuts (iOS)
6. Notification enhancements

### Quarter 3: Advanced Features
1. Handlebars templates
2. EPWING import
3. Reading comprehension quizzes
4. Listening comprehension quizzes
5. Achievement system
6. Daily challenges
7. Local HTTP API

### Quarter 4: Platform Expansion
1. 3D rendering upgrade
2. Plugin system
3. Theme marketplace
4. Deck marketplace
5. Multiplayer (experimental)
6. CLI tool
