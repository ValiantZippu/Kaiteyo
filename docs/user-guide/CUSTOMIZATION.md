# Customization

Kaiteyo is designed to be customized — themes, layouts, and settings are all user-facing.

## Themes

- **Base modes** — Light, Dark, and OLED (true black).
- **Accent schemes** — switch the accent color; the Signature theme uses Kaiteyo Lime
  (#C2FC8B) + Orange (#FEAB57).
- **Theme Studio (desktop)** — a full editor with tabs:
  - **Base / Accent** — pick base mode and accent
  - **Color** — HSV color wheel + synchronized RGB/HSL/HSV/HEX editors; 11 color targets
    (Primary, Secondary, Tertiary, Background, Surface, Text, …); saved color palette
  - **Gradients** — linear/radial/angular gradients with multiple stops, angle,
    intensity, opacity
  - **Motion** — animation presets (None … Cinematic), durations, spring tuning,
    reduced motion
  - **Layout** — density (Compact/Comfortable/Spacious), corner radius, glow,
    transparency
  - Theme presets can be saved, exported/imported as JSON, and reset.
- Theme settings live in **Settings → Appearance** and persist across restarts.

## Navigation & layout (desktop)

- **Dock position** — left/right/top/bottom (icon popup picker).
- **Layout states** — expanded / compact / hidden, with animated transitions; a peek tab
  restores the dock when hidden.
- **Compact mode** — below ~720dp width the app uses a bottom tab bar (top or bottom edge).
- **Workspace panels** — Dictionary, Kanji Browser, Statistics, Deck Browser, Theme Studio,
  Search open as a right dock or floating windows; layout persists (`workspace.panels`).
- Onboarding offers a first-run choice of these; reopen it from Settings.

## Settings overview

Settings are grouped into categories:

- **General** — language/region (Japanese UI is available), behavior defaults
- **Appearance** — theme mode, accent, UI scale, font size, density
- **Navigation** — dock position, layout, auto-hide
- **Study** — practice preferences, daily limits, answer methods
- **Media (desktop)** — playback, subtitles, system media keys, notifications, API port
- **System** — reminders (Android), backup, integrations
- **Accessibility** — UI scale, reduced motion, high contrast, font size

The Google Play flavor adds an **Analytics** settings category (Firebase, opt-out).

## Keyboard shortcuts

- **Global shortcuts (desktop)** — e.g. `Ctrl+Shift+D` dictionary popup, `Ctrl+Shift+M`
  mining dialog, `Ctrl+Shift+B` browser — configurable in the **Shortcuts** view.
- **Review shortcuts** — `Space` reveal, `1–4` grade, `B` bury, `S` suspend, `R` retry,
  `Ctrl+Enter` skip, `Ctrl+Z` undo.
- A dedicated **Keyboard Shortcuts page** lets you view and remap shortcuts (VS Code-style
  manager).

## Mobile & tablet

- The shared engine adapts to phone and tablet layouts; navigation uses a bottom/edge nav
  shell appropriate to the form factor.
- Daily review reminders are available on Android (Settings → System → Reminders).

## Backup & restore

- Export a profile backup (settings, window state, and study data) and restore it —
  see `../data/ARCHITECTURE.md` for what's included.
- Desktop sync (GitHub) can mirror your data off-device — see
  `../architecture/SYNC.md`.
