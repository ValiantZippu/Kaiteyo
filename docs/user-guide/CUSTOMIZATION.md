# Customization

Kaiteyo is designed to be customized — themes, layouts, and settings are all
user-facing. This guide describes what the current app actually exposes.

## Themes

- **Base modes** — OLED Black (default), Dark Gray, Light, Sepia (reading mode).
- **Accent schemes** — 7 presets: Signature Pineapple (default), Cotton Candy,
  Ocean, Forest, Sunset, Lavender, Monochrome. On mobile: Settings → Appearance /
  Appearance Studio. On desktop: the **Theme Studio** view.
- **Appearance Studio (shared)** — base mode + accent + custom colors; live preview.
- **Theme Studio (desktop)** — a full editor with 7 tabs:
  - **Colors** — primary/secondary/tertiary + on-colors + surface/text/border;
    gradient handling (brand gradient follows primary/secondary edits)
  - **Typography** — font size
  - **Scaling** — UI scale (display zoom)
  - **Animation** — speed (Off/Minimal/Balanced/Smooth/Cinematic), reduced motion
  - **Effects** — glow intensity/radius/opacity, blur, transparency, glass opacity
  - **Accessibility** — high contrast and related options
  - **Preview** — live preview
  - Theme library rail: presets + custom/imported themes; duplicate, rename,
    favorite, delete, reset; **JSON export/import**.
- All theme changes apply instantly across the app (including the window chrome)
  and persist across restarts. Desktop themes live in `~/.kaiteyo/themes/`.

## Navigation & layout (desktop)

- **Dock position** — left/right/top/bottom.
- **Layout states** — expanded rail / compact rail / hidden; or **floating launcher
  bubble** mode with snap points. Animated transitions between states.
- **Compact mode** — below ~720dp window width the app uses a compact bottom/top
  tab bar instead of the dock.
- **Workspace panels** — Dictionary, Kanji Browser, Statistics, Deck Browser, Theme
  Studio, Search open as a right dock or floating windows; layout persists.
- **Tabs** — browser-style workspace tabs with per-tab state (`Ctrl+T`, `Ctrl+W`,
  `Ctrl+Tab`, `Ctrl+1..9`, `Ctrl+Shift+T`).
- Onboarding offers a first-run choice of these; reopen it from Settings.

## Settings overview

### Desktop (`SettingsView`)

Settings are grouped into categories (`SettingCategory` in `SettingsEngine.kt`):
**General, Navigation, Appearance, Review, Browser, Statistics, History, Activity,
ImportExport, Sync, Updates, Plugins, Accessibility, Advanced, Media, About**. The
settings screen has a search box (`settings.search(query)`) to jump straight to a
setting.

### Mobile / shared (`SettingsScreen`)

Settings tabs include **General**, **Appearance**, **Study**, **System** and more,
plus platform extras:
- **System** — daily review reminders (Android), backup, integrations
- **Accessibility** — UI scale, reduced motion, high contrast, font size
- The Google Play flavor adds an **Analytics** category (Firebase, opt-out).

## Keyboard shortcuts

- **Global shortcuts (desktop)** — e.g. `Ctrl+K` palette, `Ctrl+B` nav toggle,
  `Ctrl+Shift+N` dock layouts — all configurable in the **Shortcuts** view
  (`ShortcutsView`, backed by `ShortcutRegistry`), persisted to disk.
- **Review shortcuts** — `Space` reveal, `1–4` grade, `B` bury, `S` suspend,
  `R` retry, `Ctrl+Enter` skip, `Ctrl+Z` undo.
- **Media shortcuts (desktop)** — fully rebindable from Media → Settings → Keyboard
  shortcuts (see [DESKTOP_SUITE.md](DESKTOP_SUITE.md)).
- A dedicated **Keyboard Shortcuts page** lets you view and remap shortcuts
  (VS Code-style manager).

## Mobile & tablet

- The shared engine adapts to phone and tablet layouts; navigation uses the same
  `NavShell` (bottom bar on phone, edge sidebar on tablet/desktop).
- Daily review reminders are available on Android (Settings → System → Reminders).

## Backup & restore

- Export a profile backup (settings, window state, and study data) and restore it —
  see `../data/ARCHITECTURE.md` for what's included.
- Desktop sync (GitHub) can mirror your data off-device — see
  `../architecture/SYNC.md`.
