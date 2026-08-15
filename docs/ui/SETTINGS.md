# Settings Catalog

> **Status**: this catalog reflects settings that exist in the codebase (verified via
> `PreferencesContract`, `ThemeSettingsState`, suite `SettingsEngine`, Theme Studio,
> review/global shortcut settings) plus **planned** entries for subsystems not yet in the
> product (Journey, children, web trial). Planned entries are marked `📋`. The rule from
> MASTER §45: every setting documents **default · valid range · effect · persistence ·
> platform differences**. Settings navigation must never crash (regression gate).

## Conventions

- **Persistence**: core settings → DataStore (`PreferencesContract`); theme → DataStore
  via `ThemeSettingsState`; desktop suite → `~/.kaiteyo/settings.json` (until ADR-0017
  consolidation); window state → per-platform persistence.
- **Defaults** here are the app defaults; all values are user-overridable unless marked
  `(system)`.

## 1. General

| Setting | Default | Range | Effect | Persistence |
|---|---|---|---|---|
| UI language | system (EN/JA available) | en, ja, (system) | Locale for UI strings (`Strings` interface) | DataStore |
| App theme mode | system | Light / Dark / OLED / system | Base mode for all surfaces | DataStore |
| UI scale | 1.0 | 0.8–1.5+ | Scales entire UI (accessibility) | DataStore |
| Font size | default | small–large | Typography scale | DataStore |
| Startup screen | Home | destination list | Where the app opens | DataStore |

## 2. Appearance (Theme Studio)

| Setting | Default | Range | Effect | Persistence |
|---|---|---|---|---|
| Accent color | Signature | color wheel / presets | Brand accent across surfaces | DataStore/Theme |
| Built-in preset | Signature | 17 presets | Full palette bundle | Theme |
| Custom gradient | off | color stops | Surface gradients | Theme |
| Motion preset | standard | none / reduced / standard / lively | Global animation intensity (NODE §123) | Theme |
| Layout density | comfortable | compact / comfortable / relaxed | Spacing scale (4dp grid multiplier) | Theme |
| Corner radius | default | system tokens | Radii scale | Theme |
| Glow effects | on | on/off | Soft glow accents (performance on low-end) | Theme |
| Theme JSON import/export | — | file | Share/restore custom themes | Theme file |

## 3. Navigation (MASTER §42–§44)

| Setting | Default | Range | Effect | Persistence |
|---|---|---|---|---|
| Navigation mode | Sidebar | Sidebar / Floating / (suite: dock options) | Content-area navigation layout | DataStore + suite JSON |
| Edge | Left | Left/Right/Top/Bottom (phone: Top/Bottom) | Dock/bubble side | DataStore |
| Expanded/compact | expanded | expanded / compact / hidden | Navigation chrome size | DataStore |
| `Ctrl+B` mode toggle | on | on/off | Quick switch between modes | DataStore |
| Bubble snap edge/position | nearest | per-edge snap points | Floating bubble location (3/edge) | DataStore |

## 4. Study & review

| Setting | Default | Range | Effect | Persistence |
|---|---|---|---|---|
| Daily new card limit | per-deck default | 0–999 | New cards per day | DataStore |
| Daily review limit | per-deck default | 0–999 | Reviews per day | DataStore |
| FSRS intervals | FSRS-5 defaults | custom | SRS scheduling (scheduler logic untouched) | DataStore |
| Review grade shortcuts | standard | remappable | 1–4 grade keys | DataStore |
| Bury/suspend behavior | default | toggle | Post-review card handling | DataStore |
| Writing strictness | Normal | Relaxed/Normal/Exam | Stroke evaluation scoring | DataStore |

## 5. Media & subtitles (suite; product target)

| Setting | Default | Range | Effect | Persistence |
|---|---|---|---|---|
| Playback backend | auto | VLC / mpv / Java Sound | Decoding + UI backend | suite JSON |
| Subtitle auto-load | matching files | on/off | External subtitle discovery | suite JSON |
| Subtitle font size | default | small–large | Subtitle rendering | suite JSON |
| Subtitle delay | 0 ms | ±10 s | Global timing offset | suite JSON |
| Playback speed presets | 1.0 | 0.25–2.0+ | Quick speed menu | suite JSON |
| A–B loop keys | standard | remappable | Loop controls | suite JSON |
| Screenshot folder | default | path picker | Capture destination | suite JSON |
| Media keys (Windows) | off | on/off | Global media keys (opt-in) | suite JSON |

## 6. Dictionary (suite; product target)

| Setting | Default | Range | Effect | Persistence |
|---|---|---|---|---|
| Popup trigger | hover+click | hover / click | DictionaryPopup activation | suite JSON |
| TTS voice | default | installed voices | Pronunciation playback | suite JSON |
| Search modes | all | EXACT/PREFIX/KANA/DEINFLECT toggles | Lookup breadth | suite JSON |
| Popup TTS on hover | off | on/off | Auto-pronounce | suite JSON |

## 7. Mining & integrations (suite; product target)

| Setting | Default | Range | Effect | Persistence |
|---|---|---|---|---|
| Audio clip duration | 2 s | 1–10 s | Mined card audio length | suite JSON |
| Mined card destination | Kaiteyo | Kaiteyo / Anki / Both | Forward mined cards (KT-ANKI-003) | suite JSON |
| AnkiConnect enabled | off | on/off | Push/import over AnkiConnect | suite JSON |
| Local API bearer token | generated | per install | Protects localhost API | generated file |
| Duplicate policy | Skip | Skip / Update / Duplicate | Import conflicts | suite JSON |

## 8. Sync & account

| Setting | Default | Range | Effect | Persistence |
|---|---|---|---|---|
| Sync provider | GitHub | GitHub (others target) | Sync transport (ADR-0009) | DataStore/account |
| Auto-sync | off | on/off | Scheduled sync | DataStore |
| Conflict policy | ask | ask / newest wins / manual | Conflict resolution | DataStore |

## 9. Accessibility (MASTER §67)

| Setting | Default | Range | Effect | Persistence |
|---|---|---|---|---|
| Reduced motion | off | on/off | Disables non-essential animation (NODE §123) | DataStore |
| High contrast | off | on/off | Contrast-boosted tokens | DataStore |
| UI scale / font size | see General | — | Text legibility | DataStore |
| Keyboard navigation | on | on/off | Focus traversal completeness | DataStore |
| Colorblind assist | off | on/off | 📋 planned | DataStore |

## 10. Data, backup & privacy

| Setting | Default | Range | Effect | Persistence |
|---|---|---|---|---|
| Backup location | default profile | path picker | Backup archives | file |
| Auto-backup | off | on/off + interval | Scheduled profile archives | DataStore |
| Analytics (Android Play) | on (flavor) | on/off | Firebase analytics (googlePlay flavor) | Play flavor |
| Crash reporting (Android Play) | on (flavor) | on/off | Crashlytics (googlePlay flavor) | Play flavor |
| World/country visualization | off | on/off | MASTER §65 — never leaves device by default | DataStore |

## 11. Game (Journey) — `📋` planned (MASTER §45)

| Setting | Default | Range | Effect |
|---|---|---|---|
| Camera mode | third-person | first/third | Default camera |
| Sensitivity / FOV / camera height | engine defaults | adjustable | `PLAYER.md` |
| Quality tier | auto | Low/Med/High/Ultra | `RENDERING.md` |
| Keymap | platform default | editable + reset | `PLAYER.md` |
| Reduced camera motion | off | on/off | Accessibility |
| Child mode | off | on (parent-gated) | `docs/vision/child-experience.md` |

## 12. Developer (MASTER §45, STANDARDS §236)

| Setting | Default | Range | Effect |
|---|---|---|---|
| Developer mode | off | on/off | Reveals dev tools (data inspect, logs) |
| Feature flags | per-flag | journey, new-search, etc. | STANDARDS §222 — removed when stable |

## Settings UX rules

1. Grouped by category; search within settings.
2. Every setting has a help line (effect) — no unexplained toggles.
3. Changes apply live with preview where possible (Theme Studio pattern).
4. Settings navigation has no dead ends; reset-per-category available.
5. `📋` entries must not render as functional until implemented (no fake controls —
   STANDARDS §290).

## Related

- Design system: `docs/design/`
- Navigation: `docs/architecture/NAVIGATION.md`
- Accessibility: `docs/architecture/accessibility.md`
- Privacy: `docs/security/PRIVACY.md`
- Game settings: `docs/game/player.md`, `docs/input/game-controls.md`
