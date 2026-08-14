# The Desktop Suite (Windows / macOS / Linux)

The desktop app is a complete **immersion workspace**: dictionary lookup, media playback,
sentence mining, OCR, and a learning browser around the shared study engine — all in one
window.

> Status note: everything below is implemented, but a few areas are partial — OCR depends
> on a local Tesseract install, and the plugin system is scaffold-only (see
> `../features/FEATURES.md` for the full status matrix).

## The workspace

- **Sidebar / navigation rail** — docked to any edge (left/right/top/bottom), expanded or
  collapsed (232dp ⇄ 64dp), or hidden with a peek tab. Below ~720dp width the app switches
  to a compact bottom tab bar.
- **Workspace panels** — Dictionary, Kanji Browser, Statistics, Deck Browser, Theme Studio,
  and Search open as a right dock or as draggable floating windows; their layout persists
  across restarts.
- **Command palette** — press the palette shortcut (e.g. to toggle panels, jump to views,
  run commands).
- **Floating launcher / bubble** — quick access to workspace areas from anywhere.
- Window chrome is custom (44dp title bar, native window controls, resize zones); window
  size/position is remembered.

## Dictionary lookup (Yomitan-style)

- **Dictionary manager** — import Yomitan-compatible dictionaries (ZIP / JSON /
  JMdict/KANJIDIC/KanjiVG), enable/disable and reorder them, browse entries.
- **Popup lookup** — hover or click any Japanese text (media subtitles, browser, OCR
  results, pasted text) to see: headword, reading(s), definitions, example sentence, tags
  (JLPT, radicals), plus actions: **Create card**, **Edit card**, add tags/flags,
  suspend/bookmark, copy, pronunciation (TTS), and open the full dictionary.
- Search across all enabled dictionaries with deinflection and reading/kana matching.

## Media center

- Play local video/audio with **VLC**, **mpv**, or the built-in Java Sound backend
  (whichever is installed).
- Subtitle support: **SRT, ASS, SSA, VTT** with synchronization.
- Player controls: playback speed, A–B repeat, frame stepping, screenshots, bookmarks,
  jump-to-timestamp.
- **Subtitle mining** — hover a subtitle line, click "Mine subtitle" → a sentence card is
  created with the screenshot, audio, and timestamp. Later, jump from the card straight
  back to that scene.
- System media keys (Windows) and tray notifications are optional (Settings → Media).

## Sentence mining

Mining turns anything you read/watch into SRS cards:

- Sources: dictionary popup, subtitles, browser text, OCR results, clipboard, and the
  local API.
- Mined cards land in your card pool with source, sentence, screenshot/audio paths, tags,
  and timestamp.
- Duplicate protection: the same content won't be mined twice (unless you choose to).
- Mined cards can be forwarded to **Anki** (via AnkiConnect) when enabled.

## OCR

- Capture a screen region, an image file, or clipboard image, and run OCR to extract
  Japanese text.
- Results feed straight into the dictionary popup → mining.
- **Requires a local Tesseract installation** (Tess4J); without it, Kaiteyo shows a hint
  instead of failing.

## Learning browser

- A study-friendly browser: tabs, address bar, back/forward, bookmarks, downloads, reader
  mode (JavaFX WebView when available, reader-mode rendering otherwise).
- Select any Japanese text → popup lookup → mining.

## Collections, tags & flags

- **Collections** (deck details) — Study, Browse (up to 200 cards), Edit, Statistics,
  Duplicate, Export (JSON to clipboard), Archive/Restore, Delete.
- Browser upgrades: selection mode, bulk toolbar (Tag, Flag, Favorite, Suspend, Reset,
  Delete), sorting (Default/Character/Meaning/Status/Interval/Due/Tags), "Review these N".
- Tag/flag management screens with bulk operations.

## Transfer & sync

- **Import/export** — Anki `.apkg`, JSON/CSV/TSV/TXT (file picker or clipboard), with
  preview, validation, and conflict policies.
- **Account & sync** — sign in with GitHub (device-flow OAuth) and sync your study data to
  a private gist; view sync state, force upload/download. (See
  `../architecture/SYNC.md`.)
- **Backup** — profile archives including settings and window state.

## Local API & integrations

- The app exposes a **localhost HTTP API** (opt-in) for media, mining, and player control —
  see `../integrations/LOCAL_API.md`.
- **Integrations hub** — status cards for Local API, GameSentenceMiner, AnkiConnect, Text
  hook, Player WebSocket, and System media keys, each with test buttons.

## Keyboard shortcuts

Review: `1–4` grade, `Space` reveal, `B` bury, `S` suspend, `R` retry, `Ctrl+Enter` skip,
`Ctrl+Z` undo. Navigation: `Ctrl+B` toggle auto-hide, `Ctrl+Shift+N` cycle dock layouts.
See the in-app **Shortcuts** page (`../user-guide/CUSTOMIZATION.md` → Shortcuts) for the
full list.
