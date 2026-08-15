# The Desktop Suite (Windows / macOS / Linux)

The desktop app is a complete **immersion workspace**: dictionary lookup, media
playback, sentence mining, OCR, and a learning browser around the shared study
engine — all in one window.

> Status note: everything below is implemented, but a few areas are partial — OCR
> depends on a local Tesseract install, and the plugin system is scaffold-only (see
> `../features/FEATURES.md` for the full status matrix).

## The workspace

The workspace shell (`WorkspaceShell`) is the app's home on desktop. Its **dock**
can sit on any edge (left/right/top/bottom), in **expanded** or **compact** mode,
or be replaced by a **floating launcher bubble** (`NavLayout.Sidebar` vs
`NavLayout.Floating`). Below ~720dp window width the app switches to a compact
bottom/top tab bar automatically.

The **top bar** shows the current view, a **"Search or jump to…" palette button**
(`Ctrl+K`), Settings, and the workspace-panel menu. Workspace **panels**
(Dictionary, Kanji Browser, Statistics, Deck Browser, Theme Studio, Search) open as
a right dock or floating windows; their layout persists across restarts.

### The views

`WorkspaceView` defines every view in the dock (with shortcuts `1–9` for tabs):

| View | What it is |
|------|-----------|
| **Dashboard** | Today's overview: stats, review queue, quick actions |
| **Library** | Card pool browser + search + editing (the old "Browser") |
| **Review** | Review sessions (`ReviewView`: launch panel with filters → session panel with grading) |
| **Writing** | Writing practice (`WritingPracticeView`) |
| **Grammar** | Grammar practice (`GrammarPracticeView`, starter deck + grammar-tagged cards) |
| **Exams** | Exam runner (`ExamView`) |
| **Dictionary** | Dictionary manager + lookup (`DictionaryManagerView`) |
| **Mining** | Mining inbox (`MiningView`) |
| **Media** | Media center (`MediaView`: Home/Player/Library/Stats/Bookmarks/Tuning/Settings) |
| **Web Browser** | Learning browser (`LearningBrowserView`) |
| **OCR** | OCR capture + results (`OcrView`) |
| **Collections** | Smart collections & saved filters (`CollectionsView`) |
| **Tags & Flags** | Tag/flag management (`TagFlagView`) |
| **Statistics** | Stats dashboard (`StatsView`) |
| **Mistakes** | Weak cards / frequent mistakes (`MistakesView`) |
| **Activity Log** | Categorized activity history (`ActivityLogView`) |
| **Import / Export** | Transfer view — Export / Import / Backup / Learning data tabs (`TransferView`) |
| **Sync** | GitHub-gist sync (`SyncView`) |
| **Shortcuts** | Keyboard shortcut manager (`ShortcutsView`) |
| **Plugins** | Plugin registry/marketplace scaffold (`PluginsView`) |
| **Theme Studio** | Theme editor (`ThemeStudioView`) |
| **Settings** | Settings (`SettingsView`) |
| **Account** | Account + sync state (`AccountView`) |
| **About** | About/credits (`ContributionsView`) |
| **Integrations** | Integration status cards (`IntegrationsView`) |

Browser-style **tabs** (`Ctrl+T` new tab, `Ctrl+W` close, `Ctrl+Tab` cycle,
`Ctrl+1..9` jump, `Ctrl+Shift+T` reopen) let you keep several views open; each tab
remembers its own state.

## Dictionary lookup (Yomitan-style)

- **Dictionary manager** — import Yomitan-compatible dictionaries (ZIP / JSON /
  JMdict/KANJIDIC/KanjiVG), enable/disable and reorder them, browse entries.
- **Popup lookup** (`DictionaryPopup`) — hover or click any Japanese text (media
  subtitles, browser, OCR results, pasted text) to see: headword, reading(s),
  definitions, example sentence, tags (JLPT, radicals), plus actions: **Create
  card**, **Edit card**, add tags/flags, suspend/bookmark, copy, pronunciation
  (TTS), and open the full dictionary.
- Search across all enabled dictionaries with deinflection and reading/kana
  matching (`SearchMode` EXACT/PREFIX/KANA/DEINFLECT).

## Media center

- Play local video/audio with **VLC**, **mpv**, or the built-in **Java Sound**
  backend (whichever is installed; the app tells you when a backend is missing).
- Subtitle support: **SRT, ASS, SSA, VTT** with synchronization and a secondary
  (dual-language) track.
- Player controls: playback speed, A–B repeat, frame stepping, screenshots,
  bookmarks, jump-to-timestamp, chapter markers, condensed fast-forward, cinema
  mode, resume prompt.
- **Subtitle mining** — select words (click, shift-click, drag, or select-all) →
  **Mine** → a sentence card is created with the screenshot, audio, and timestamp.
  Later, jump from the card straight back to that scene.
- **Play queue** — next/previous with end-of-episode overlay and auto-advance.
- System media keys (Windows) and tray controls are optional (Media → Settings).
- Drag & drop: drop a video/audio to play it, a subtitle file to attach it, a
  folder to add to the library.

## Sentence mining

Mining turns anything you read/watch into SRS cards (`MiningEngine`):

- Sources: dictionary popup, subtitles, browser text, OCR results, clipboard, and
  the local API.
- Mined cards land in your card pool with source, sentence, screenshot/audio paths,
  tags, and timestamp.
- Duplicate protection: the same content won't be mined twice (unless you choose to).
- Mined cards can be forwarded to **Anki** (via AnkiConnect) when enabled, or kept
  for Kaiteyo's own review.

## OCR

- Capture a screen region, an image file, or clipboard image, and run OCR to extract
  Japanese text.
- Results feed straight into the dictionary popup → mining.
- **Requires a local Tesseract installation** (Tess4J); without it, Kaiteyo shows a
  hint instead of failing.

## Learning browser

- A study-friendly browser (`LearningBrowserView`): bookmarks, downloads, and reader
  modes (JavaFX WebView when available, reader-mode rendering otherwise).
- Select any Japanese text → popup lookup → mining.

## Collections, tags & flags

- **Collections** (`CollectionsView`) — smart collections built from saved filters.
- **Tags & flags** — tag/flag management screens with bulk operations
  (`TagFlagView`).
- Card browser bulk actions: select → Tag, Flag, Favorite, Suspend, Reset, Delete;
  sorting by Default/Character/Meaning/Status/Interval/Due/Tags; "Review these N".

## Transfer & sync

- **Import/export** (`TransferView`) — Anki `.apkg`, JSON/CSV/TSV/TXT, with preview,
  validation, and conflict policies; plus a **Backup** tab (profile archives incl.
  settings + window state) and a **Learning data** tab.
- **Account & sync** — sign in with GitHub (device-flow OAuth) and sync your study
  data to a private gist (`SyncView`). See `../architecture/SYNC.md`.
- **Backup** — profile archives including settings and window state.

## Local API & integrations

- The app exposes a **localhost HTTP API** (opt-in) for media, mining, and player
  control — see `../integrations/LOCAL_API.md`.
- **Integrations hub** (`IntegrationsView`) — status cards for Local API,
  GameSentenceMiner, AnkiConnect, Text hook, Player WebSocket, and System media
  keys, each with test buttons.
- **Text hook** (TCP) and **Player WebSocket** let external tools (texthookers,
  scripts) push text or control the player.

## Keyboard shortcuts

- **Review**: `1–4` grade, `Space` reveal, `B` bury, `S` suspend, `R` retry,
  `Ctrl+Enter` skip, `Ctrl+Z` undo.
- **Palette**: `Ctrl+K` open, `Esc` close.
- **Tabs**: `Ctrl+T` new, `Ctrl+W` close, `Ctrl+Tab` cycle, `Ctrl+1..9` jump,
  `Ctrl+Shift+T` reopen.
- **Nav**: `Ctrl+B` toggle sidebar/floating, `Ctrl+Shift+N` cycle dock layouts.
- **Media** (while Media is active): `Space` play/pause, arrows seek,
  `Ctrl+←/→` ±30s, `Alt+←/→` word cycling, `Shift+←/→` cue navigation, `R` replay,
  `A` mine, `D` dictionary, `B` bookmark, `S` screenshot, `C` audio clip, `E`
  condensed, `F` transcript, `T` subtitles, `L` loop, `N`/`V` next/previous item.
  All media hotkeys are rebindable from Media → Settings → Keyboard shortcuts.
- The in-app **Shortcuts** page lists and remaps everything
  (`../user-guide/CUSTOMIZATION.md` → Shortcuts).
