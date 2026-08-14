# ADR-0008: Desktop Immersion Suite as a Self-Contained Module

**Status**: Accepted

## Context

The desktop flagship needed a Yomitan-style dictionary, ASBPlayer-style media player,
sentence mining, OCR, a learning browser, sync, and theming — a large amount of JVM-only
code that does not exist on Android/iOS. It needed to grow fast without destabilizing the
shared engine, and without being coupled to the Koin-based core screen pattern.

## Decision

- Implement the suite in `desktopApp/src/jvmMain/kotlin/ua/syt0r/kanji/desktop/` as a
  **self-contained layered stack** (JVM-only):
  - `engine/` — dictionary, media, mining, ocr, browser, review/srs, sync, transfer,
    theming, updates, plugins, shortcuts, settings, stats, collections, account, api, cli
  - `designsystem/` — reusable `Ds*` components on core theme tokens
  - `ui/` — one view per domain (Dashboard, Browser, Review, Collections, Media, Mining,
    Ocr, Stats, Sync, Account, Transfer, Tags/Flags, Themes, Plugins, Settings, Shortcuts,
    Activity Log, Grammar)
  - `appstate/` — single `AppState` facade; `WorkspacePanels` for docked/floating panels
  - `model/`, `data/` — models + `DemoData`
- Launch via `desktopSuiteMain()` (`SuiteMain.kt`) inside the same `KaiteyoWindow` shell;
  the real app entry is `desktopApp/Main.kt` (`main()`).
- Persistent state: JSON under `~/.kaiteyo/` (`library/cards.json`, `settings.json`,
  `window.json`, dictionaries, history).

## Alternatives

- Implement the suite inside the shared core screen pattern — rejected: would force
  JVM-only code into the KMP module and couple it to core ViewModels.
- Separate Gradle module — considered; kept inside `desktopApp` to share the window shell
  and build config while still being a clean package boundary.

## Consequences

- Fast iteration on desktop features without touching mobile code.
- Some conceptual duplication between the core SRS/transfer code and the desktop engine
  (e.g. desktop has its own SRS scheduler and transfer pipeline); the desktop suite
  reuses core where practical (jdata strokes, AnkiPackage on JVM).
- Mobile never pays for desktop-only code.

## Implementation notes

- `desktopApp/src/jvmMain/kotlin/ua/syt0r/kanji/desktop/`
- `desktopApp/SuiteMain.kt`, `desktopApp/Main.kt`
