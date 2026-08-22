# Kaiteyo (書いてよ) Changelog

All notable changes to Kaiteyo are documented here. Format follows
[Keep a Changelog](https://keepachangelog.com/en/1.0.0/) conventions, grouped by:

- **Added** — new features
- **Changed** — changes to existing behavior
- **Fixed** — bug fixes
- **Removed** — removed functionality
- **Security** — security-relevant changes

## Unreleased

### Added
- **Home — interactive activity heatmap**: the Dashboard now embeds the full
  heatmap panel (rolling 52 weeks + per-year calendars) with smooth
  push/slide year transitions, hover tooltips (date, cards, reviews, study
  time, decks) and click-through day details — previously only available on
  the Statistics screen.
- **Home — welcome hero for new users**: a brand-new library (no cards,
  no decks) shows a real welcome card with working actions (Create a deck,
  Import content, Explore dictionary, Try Browse) instead of a dead dashboard.
- **Home — smarter continue hero**: the hero now ranks real continue targets
  instead of only the busiest deck — deck with due/new work, recently studied
  deck, saved search, collection, review or dictionary exploration — and
  shows up to two compact alternatives under the primary action.
- **Library — list/grid toggle**: the deck catalog now switches between the
  dense grid and a compact list view (persisted per user). List rows carry
  the same live stats and full deck-actions menu.
- **Library — one-click study everywhere**: smart scopes (Due today / New /
  Favorites / Recently studied) get a "Study these N" button that reviews
  exactly the shown cards, and collection cards in the strip get a Study
  button (collection-scoped review).
- **Browse — full keyboard navigation**: ↑/↓ move through results (with row
  highlight and scroll-into-view in both list and grid), Enter opens the
  focused card, Esc clears the query / selection / filters in one step.
  Arrows are ignored while the search field is focused so caret editing
  still works. The results line shows the key hints.
- **Browse — live search suggestions**: typing in the search field shows an
  in-memory suggestion dropdown (recent searches, real tags from your card
  pool, flag colors, and query operators like `meaning:` / `tag:` /
  `interval:`). Arrow keys move, Enter applies, Esc dismisses, and applying
  a suggestion just sets the query — no ghost UI.
- **Browse — dictionary link from the card detail panel**: "Look up in
  dictionary" opens the Dictionary scoped to the card's headword.

### Added
- **Library integrations — bulk adoption**:
  - **Kuromoji** (IPAdic) — Japanese morphological analysis engine with full POS tagging,
    reading assignment, and deinflection hints; integrated via `JapaneseNlpEngine` with a
    regex-based fallback tokenizer when Kuromoji isn't on the classpath.
  - **Turbine** — Flow testing library added to commonTest and jvmTest for StateFlow
    emission assertions; test examples added for NLP engine, Handlebars templates, and
    EPUB reader.
  - **Tesseract OCR (Tess4J)** — desktop OCR backend via JNI reflection with graceful
    fallback when Tess4J isn't on the classpath; integrated into `OcrEngine` with
    image, clipboard, and screen-capture sources.
  - **ML Kit OCR stub** — Android OCR provider interface with composite fallback to
    Tesseract on desktop; ready for Google ML Kit `TextRecognition` integration.
  - **EPUB parser** — pure-JVM EPUB 2/3 reader (`EpubReader`) that extracts OPF manifest,
    spine order, and converts XHTML content documents to `ReadingBlock`s; supports
    headings, paragraphs, lists, quotes, and code blocks.
  - **Handlebars template engine** — lightweight Mustache/Handlebars-inspired renderer
    for dictionary card layouts; supports variables, conditionals, loops, partials,
    and HTML escaping; includes Yomitan-style front/back/full card templates.
  - **Yomitan importer v2** — full-featured Yomitan v2/v3 dictionary importer with
    structured content parsing, frequency bands (VeryCommon→VeryRare), pitch accent
    data, kanji metadata (readings, meanings, JLPT, grade, radicals), tag banks,
    and multi-format support (ZIP, folder, index.json, single JSON).
  - **FSRS-rs native bridge** — stub for future Rust-based FSRS implementation via
    JNI; currently delegates to the pure-Kotlin FSRS-5 scheduler; ready for hot-swap
    when card pools exceed 100k.
  - **Sentry integration** — error tracking and performance monitoring bridge with
    reflection-based SDK loading; captures exceptions, messages, breadcrumbs, and
    spans; no-op fallback when Sentry SDK isn't on the classpath.
  - **Coil 3 image loading** — unified image loading across platforms with LRU cache,
    resize, and format conversion; desktop uses Java ImageIO with optional Coil 3
    backend; Android uses Coil 3 natively.
  - **Lottie animations** — animation engine with Lottie Compose and Rive stubs;
    celebration and skeleton animation presets; graceful fallback to static placeholders.
  - **Detekt static analysis** — configuration at `config/detekt/detekt.yml` with
    Kaiteyo-tuned thresholds (120-char lines, 25-complexity methods, 40-functions per
    file); plugin registered in settings.gradle.kts.
  - **EPUB reading** — EPUB files now open in the Reading Environment via `EpubReader`,
    which extracts OPF metadata, spine order, and converts XHTML content to
    `ReadingBlock`s; chapter headings are preserved as navigation anchors.
  - **NLP-enhanced dictionary search** — when a Japanese query returns no results,
    `DictionaryService` now uses `JapaneseNlpEngine` to extract content words and
    search their base forms, catching conjugated verbs and adjectives.
  - **Sentry error tracking** — `SentryBridge` initializes at startup in `Main.kt`
    (reads `SENTRY_DSN` env var); breadcrumbs are added at key flow points
    (KJD update check); no-op when DSN is empty or SDK is absent.

### Changed
- **Theme — semantic color token system (`KaiteyoSemanticColors`)**: all
  hardcoded `Color(0xFF...)` values across the UI are now centralized into a
  single `KaiteyoSemanticColors` data class with 40+ tokens (review actions,
  card status, semantic indicators, card flags, activity types, difficulty
  tiers, day/night, favorites). Dark and light variants adapt to the base
  mode, and all tokens animate smoothly during theme transitions via
  `withThemeTransition`. Components read `LocalKaiteyoSemanticColors.current`
  instead of hardcoding hex values — changing accent or base mode now
  recolors everything consistently. Migrated: home dashboard, statistics,
  card browser, bulk actions, history tracker, card manager, deck details,
  deck browser, import/export, backups, kanji browser, review shortcuts,
  LibraryScreen, AnkiOpsFull, NoteEditorScreen, and more — zero hardcoded
  `Color(0xFF...)` remaining in all migrated screens.

### Added
- **Kaiteyo Game — second region, stories, save slots & controller support**:
  - **Kamakura (鎌倉) is now playable** — the Sea Line train actually travels
    between Hamanaka and Kamakura (region switch rebuilds tiles, camera
    bounds, region-scoped NPCs and interactables — a new region is pure
    content). Arrival auto-starts the "A Kamakura Summer" story. New NPCs
    (priest, Komachi shopkeeper, lifeguard, monk), 5 new quests, dialogue,
    18 knowledge nodes, collectibles — all in `resources/game/*.json`.
  - **Story UI** — the Story menu lists stories/chapters/scenes with
    Start/Continue; scene effects (dialogue, quest triggers, knowledge
    grants) are handled by the session. `docs/game/VERTICAL_SLICE.md`
    updated.
  - **Save slots** — multiple named journeys under `~/.kaiteyo/game/saves/`
    with a Saves menu (load / save here / delete / new journey); autosave
    writes into the active slot.
  - **Gamepad support (real hardware)** — `JnaGamepadProvider` polls XInput
    on Windows and the evdev joystick API on Linux through JNA (already a
    desktop dependency), with Xbox / PlayStation / generic layouts, stick
    dead zones from `InputCalibration`, and shared `ControlScheme`
    rebinding. Touch provider interface remains for mobile wiring.
  - Map menu now shows time-of-day + weather (the world is alive on the
    map, not just in the scene).
  - New tests: `StoryAndSlotsTest` (story scenes, slot isolation,
    cross-region travel), `GamepadMappingTest` (layout maps, dead zones,
    provider→input translation).
  - **Spoken dialogue (TTS)** — NPC lines are spoken through Kaiteyo's
    kana-clip voice engine (`GameBridge.speakJp` → `KanaTtsManager`):
    auto-play per line, a ♪ replay button, a Voice settings toggle, and
    listening counts as real activity (`DialogueListened`). No voice
    asset? The game stays silent and keeps working.
  - **Touch controls (PUBG/Genshin-style)** — `VirtualTouchProvider` +
    `TouchControlsOverlay`: a dynamic-origin joystick that appears where
    the thumb lands (rim = run), right-side drag-to-look with tap-to-
    interact, and contextual buttons (Interact appears near things, photo
    mode swaps in Capture/Close). Toggle in Game Settings → Touch.
  - **Gamepad hot-plug** — the Linux joystick provider re-probes
    `/dev/input/js*` every second and Windows XInput polls continuously,
    so a controller plugged in mid-session is picked up automatically.
  - **Control rebind UI** — Game Settings → Controls lists every action
    with its bound keys; click a row and press a keyboard key or gamepad
    button to rebind (Esc cancels). Bindings persist with the journey.
  - New tests: `DialogueTtsTest` (kana extraction, listening signal,
    silent-voice honesty), `TouchControlsTest` (joystick/look/buttons →
    InputState), `RebindAndHotplugTest` (scheme changes, reset, press
    queue).
  - **Game audio system** — procedural SFX synth (discovery, quest
    complete, photo shutter, interact, purchase, closed) + per-area
    ambient pads (town, beach, station, festival); master/music/sfx
    volume sliders in Settings → Audio, applied live.
  - **Menu focus navigation** — `FocusNav` keyboard/gamepad focus model:
    menus are fully playable with arrows + Enter (or gamepad), no mouse
    needed.
  - **In-world writing activity** — kana tracing panel at the writing desk
    object with a lenient stroke-order evaluator; new `WriteKana` quest
    objective kind; discoveries from writing feed Kaiteyo stats.
  - **Time-gated world + evening festival** — `availablePhases` on objects
    (school in the morning, stalls in the evening) with closed prompts;
    Hamanaka's festival appears at dusk: festival NPCs (stall owner,
    taiko drummer, lantern keeper), stalls, new quests, dialogue,
    knowledge (祭り/屋台/金魚すくい…), collectibles and a festival story.
    NPC presence is now schedule-window driven.
  - **Richer knowledge content** — sentence + grammar nodes in the graph
    (festival phrases, どこ/です/か grammar), discovery cards show JLPT
    and kind badges.
  - New tests: `GameAudioTest` (synth math, volume gating, ambient pad),
    `WritingAndGatingTest` (evaluator, phase gating, writing discoveries),
    `QuestObjectiveKindsTest` (write/listen/learn-word objectives).
  - **NPCs walk their schedules** — waypoint movement replaces teleporting:
    NPCs stroll between scheduled positions at a walk speed, face their
    direction, arrive and idle; talk hitboxes follow live positions.
  - **Ordering minigame (spec §56)** — festival stalls now have Japanese
    menus: interact with a stall, pick たこ焼き / ラムネ / たい焼き / 焼きそば
    by their Japanese names (reading + meaning as support), order, and the
    words are discovered. New `OrderFood` quest objective kind; the
    takoyaki quest now uses it. A second taiyaki stall joined the square.
  - **Photo review panel** — click any album photo to open it: vocabulary
    tags, category, region/time, **Save to disk** (JSON sidecar under
    `~/.kaiteyo/game/photos/` via the bridge) and Delete.
  - **Quest log polish** — filter chips (All / Active / Completed) and
    per-quest progress bars; the HUD objective strip now shows objective
    progress (2/3) and a first-run hint when no quest is active.
  - New tests: `NpcOrderPhotoTest` (waypoint walking, ordering flow,
    photo save/delete, `OrderFood` matching).
  - **NPC patrols** — NPCs can loop a route of `patrolPoints` with pauses
    (the beachcomber wanders the shore); schedules already handled waypoint
    walking. Weather/season **presence gates**: an NPC can be rain-only or
    winter-only and the director spawns/despawns them accordingly.
  - **Season cycle (spec §42)** — the world now has spring/summer/autumn/
    winter: a `SeasonSystem` derives the season from the day counter (3-day
    seasons), the renderer applies a per-season palette tint, and the weather
    leans on the season (winter snows, spring rains) so seasonal quests are
    reachable, not a lottery. Season/weather **quest objective kinds**
    (`Season`, `Weather`) complete when the world enters that condition;
    season/weather-gated world objects show "closed" prompts.
  - **Kamakura night** — a new night quest chain: the Lantern Keeper
    (提灯番) appears at dusk on the Hachimangū approach, you inspect the
    stone lantern (石灯籠) and photograph the moon (月). Winter candy vendor
    (飴細工屋さん) appears only in winter snow/rain. New knowledge (月/貝/
    飴/冬/雪), stamps, dialogue.
  - New tests: `PatrolWeatherSeasonTest` (patrol loops, weather/season
    gating, season rollover, seasonal weather, `Season`/`Weather` objectives,
    Kamakura night quest grant).
  - **Seasonal events (spec §42)** — the winter market: a cocoa vendor
    (ココア屋さん) and winter stall appear in Hamanaka when the world turns
    winter (quest: 冬市で暖まる), and spring blossom-viewing (お花見) under
    the Kamakura 桜 with a picnic NPC and quest. All gated by the season
    system — the same world, a different season.
  - **Season/weather debug tools (spec §121-122)** — the F3 debug overlay
    now has one-click season (Spring/Summer/Autumn/Winter), weather
    (Sun/Cloud/Rain/Snow) and time (Morning/Day/Evening/Night) chips, so
    testers reach any seasonal content instantly.
  - **Clock pacing (spec §40)** — a new Time setting with presets: Fast (a
    day in 12 min), Standard (a day in 2 h), Real time (60 s / min); applied
    live, persisted with the journey.
  - New tests: `DebugToolsSeasonalTest` (season forcing spawns the winter
    market, weather forcing, time jumps, pacing applied live, seasonal
    content gates).
  - **All four seasons now have events** — summer bon dance (盆踊り) on the
    Hamanaka beach at dusk (dancer walks a chained route), and autumn leaves
    (紅葉) path in Kamakura with a persimmon (柿) lesson. New knowledge
    (夏/秋/紅葉/柿/涼しい/きれい/好き + grammar 〜が/〜は/〜を), dialogue,
    quests, stamps.
  - **Seasonal audio (spec §42, §91-92)** — the ambient pad is coloured per
    season: spring birdsong, summer cicadas, autumn dry-leaf crackles,
    winter wind (`GameAudio.setSeason`, wired live from the season cycle).
  - **Chained NPC routes (spec §39, §52)** — `NpcRoute`: time-windowed
    patrol legs; the director walks the active leg by the world clock (the
    bon dancer circles the beach circle at dusk, the leaf watcher loops the
    temple path in autumn). Content validator now checks route points stay
    in-cell and windows don't overlap.
  - **Snow particles (spec §41)** — soft drifting flakes replace the rain
    streaks when the weather is snow.
  - **Quest-log category filters (spec §21)** — chips for All types plus
    every category that has quests (Exploration, Social, …).
  - **Debug teleport (spec §121)** — the F3 overlay can teleport the player
    to any discovered location instantly.
  - New tests: `RoutesSeasonalAudioTest` (chained-route walking and
    despawn, autumn gating, season→audio mapping, summer/autumn quest data,
    teleport, category coverage).

### Fixed
- **`id:` search filter was silently broken** — the Browse "Review this card"
  action built a query with `id:<cardId>` but the search engine had no `id`
  field, so the query fell back to a plain-text token that never matched.
  Added `SearchField.Id` (+ `id` field pattern and evaluation); Browse review,
  the new Library "Study these N" action and the `id:` search suggestion now
  all work.
- **Kanji details — readings/vocab actually filtered** — the old Readings
  card expanded every reading to the *entire* vocabulary list (the matching
  regex stripped every reading to an empty string), and vocabulary + example
  sentences lived in separate side panels. The kanji details page now uses a
  single **Readings & Vocabulary explorer**: On/Kun tabs with live counts,
  reading pills that list only the words genuinely using that reading (proper
  kun okurigana and katakana→hiragana matching), and tapping a word expands
  its example sentences inline from the corpus. Landscape layout is a clean
  two-column arrangement (structure left; status/hero/explorer/mnemonic
  right) instead of three cramped columns.
- **Kaiteyo Game — vertical slice** (`desktop/game/`, reachable as the **Game**
  workspace destination): a real, data-driven Japanese-learning world — a dense
  seaside town (Hamanaka) with a station, shopping street, beach and aquarium,
  all carrying Japanese in the environment. Engine core with fixed-timestep
  loop, scenes, entities and spatial hash; `InputAction` abstraction with
  rebinding (gamepad/touch interfaces architected); TPP/FPP camera rig with
  settings; tile-grid world with regions→districts→cells→objects/locations/
  stations; interaction prompts; NPCs with schedules and relationships;
  dialogue with JP/reading/translation + learning targets and effects;
  quest dependency graph with typed objectives and rewards; knowledge graph
  connected to Kaiteyo via `GameBridge`/`KaiteyoBridge` (discover → mine to
  the real card pool → stats); photography with subject tagging + album;
  collections; time/weather; versioned save system with autosave;
  content validator + debug tools; tests for quest graph, core logic and a
  full session. Content is 100% JSON (`resources/game/`) — new regions,
  quests and vocabulary need no engine changes. Docs under `docs/game/`
  (ARCHITECTURE, ENGINE_DECISION, WORLD, VERTICAL_SLICE, ROADMAP, TODO).
  Honest status: engine/world/learning/quest/save IMPLEMENTED for the slice;
  3D rendering, controller/touch wiring and story UI PLANNED — see
  `docs/game/VERTICAL_SLICE.md`.

### Fixed
- **History screen no longer fabricates fake activity.** When the real history was
  empty, `HistoryFullScreen` fell back to 50 generated entries with random
  timestamps/descriptions; it now shows the real empty state (Library → Manage →
  History).
- **Removed 12 dead screens/models from the core deck-features folder** (the
  achievement subsystem — `AchievementSystem`/`AchievementManager`/
  `AchievementChecks`/`AchievementDefs_*` — plus `DeckBrowserEnhanced`,
  `DeckManagerEnhanced`, `NoteEditorEnhanced`, `UndoSystem`), trimmed
  `DeckManager.kt` to its live `KaiteyoDeck`/`DeckFilters` models (dropping the
  unreferenced mock-data `DeckManager()` screen and dead Bulk/Smart models), and
  removed the dangling `DeckFeaturesHub` import from `MainNavigation.kt`. Every
  deleted symbol was verified unreferenced repo-wide; all screens the routes
  actually compose (Card browser, Deck browser, Bulk actions, History, Note
  editor, Anki operations, Tag/Flag managers, Search, Import/Export, Keyboard
  shortcuts) are confirmed live and untouched.
- **Backup systems unified — the Backup & Restore manager now uses the real
  backup engine.** Creating and restoring launch the real file-based flow
  (platform file picker + `BackupManager` with DB-version-checked restore); the
  JVM/Android/iOS backup screens record real filename + size into the backup
  history; the fake metadata-only `recordBackup`/`recordRestore`/`recordVerify`
  paths and the fake Verify menu item were removed; the Restore, Settings and
  Schedule tabs are now honest (no dead buttons, no "auto-backup before restore"
  claims, automatic scheduling clearly marked as not yet active).
- **Command palette "Export active theme JSON" actually exports** — the command
  previously showed a success toast without copying anything; it now serializes the
  active theme via `ThemeManager.exportJson` and writes the JSON to the system
  clipboard before confirming (ghost-control fix from the UI/UX pass).

### Changed
- **Documentation restructured** — the repository documentation was reorganized into a
  professional, navigable structure (`docs/` with topic areas: architecture, data,
  integrations, user-guide, platform, security, legal, testing, releases, planning).
  Architecture Decision Records moved to `docs/architecture/decisions/`; the changelog
  moved to the repository root; the root README was redesigned with accurate per-feature
  status. Repository root was cleaned of dev-session scratch files (crash dumps, build
  logs, paste files, one-off scripts).

## v2.2.1 (Current) — Platform Polish & Rebranding Completion

### Added
- **Premium installer subsystem** (`installer/`) — new, fully decoupled from Gradle:
  - Windows: branded Inno Setup 6 installer (modern dynamic dark-mode wizard,
    install/upgrade/repair/modify, silent install, keep-or-remove uninstaller, file
    associations, launch-after-install, install-dir memory) + portable zip build
  - macOS: styled DMG with branded background artwork and drag-to-Applications;
    hardened-runtime signing + notarization + stapling pipeline (`entitlements.plist`)
  - Linux: AppImage with AppStream metadata + multi-size icon theme, deb builder, rpm
    spec, Flathub-ready Flatpak manifest, Snap wrapper
  - Shared: `common/version.json` single source of truth, update-feed + artifact-manifest
    JSON schemas, integrity verification gate, staging/bump/feed generation scripts,
    SVG→bmp/ico/icns/png brand asset generator
  - Docs: `installer/docs/{ARCHITECTURE,BUILD,SIGNING,RELEASE,UPDATES,FIRST_RUN}.md`
- **First-run onboarding** — `OnboardingWizard` (8 steps, live-applied: theme, accent,
  scaling, font, navigation, motion) wired into `KaiteyoDesktopSuite`, gated once by the
  settings key `onboarding.completed`, re-openable from Settings, every step skippable,
  crash-safe completion via `AppState.completeOnboarding()`
- **Auto-update architecture** (`desktop/engine/updates/`) — `UpdateChannel`
  (stable/beta/nightly), `UpdateManifest` (feed schema v1), `HttpUpdateChecker`,
  sha256-verified `HttpUpdateDownloader`, `UpdateInstaller` interface, `UpdateService`
  coordinator with `StateFlow<UpdateState>`, `UpdatePolicy` rollback window
- **CI extended** — `build-all.yml` now produces the Inno EXE, MSI, portable zip, styled +
  notarized DMGs (arm/intel), deb, rpm and AppImage; `build-release.yml` stages + verifies
  artifacts and generates the stable update feed
- **Native window shell** — `KaiteyoWindow.kt`, `NativeWindowDrag.kt`, `WindowActions.kt`,
  `WindowStateStore.kt`:
  - 44dp custom title bar: K-logo system menu, draggable wordmark, native-style window
    controls with hover states
  - Native OS dragging on Windows (`WM_NCLBUTTONDOWN`/`HTCAPTION`) and Linux (EWMH
    `_NET_WM_MOVERESIZE`) with a Compose fallback
  - 8-zone invisible resize handles for the undecorated window; rounded corners flatten
    when maximized
  - Custom system menu (title-bar right-click / Alt+Space / logo / dock button) with full
    keyboard navigation
  - Window size & position persisted to `~/.kaiteyo/window.json` (screen-validated on
    load, throttled saves) and included in profile backups
- **Screenshot capture pipeline** — dev-only `--capture-state` flag,
  `scripts/capture-window-shell.sh`, and the website's desktop screenshot gallery wired to
  `docs/screenshots/`
- **Unified statistics dashboard** — the card manager's stats/heatmap tabs now render the
  single analytics dashboard via `embedded` mode; legacy `StatisticsOverview` removed and
  its unique values folded into the dashboard's Library Distribution section

### Changed
- **iOS project fully renamed** — `iosApp/KanjiDojoApp` → `iosApp/KaiteyoApp` (folder,
  Swift entry point, `xcodeproj`, `pbxproj`, shared scheme, all build references)
- **Docs restructured by topic** — flat numbered docs moved into topic folders;
  `docs/README.md` is the new index; internal links, `AGENTS.md`, `README.md`, and the
  website `documentation.json` all updated
- **Desktop packaging rebranded** — snapcraft plug/paths, flatpak metainfo changelog +
  URLs, AppImage metadata, and the snap launcher all Kaiteyo
- **Play Store changelog fixed** — stale `kanji-dojo` macOS note rewritten in `fastlane`

### Removed
- **Legacy attribution comments** — `Kanji.Dojo` references removed from the stats
  dashboard, Home stats KDoc, and built-in deck catalog comments

### Verified
- **Rebranding audit across all platforms** — Android, iOS, desktop, and website are fully
  Kaiteyo; only legal attribution (fork history, original-author copyright, upstream repo)
  and functional references (`kanji-dojo-data-base-v15.sql` asset, App Store URL) remain —
  see `docs/branding/BRANDING.md`

## v2.0.0 — Premium Experience

### Added
- **Unified Library hub** — single Library tab replacing the Kanji/Vocabulary split: hub
  with Sections + stat summary rows, drill-down screens (Kanji Decks, Vocabulary, Word &
  Sentence Search); default-tab preference remapped
- **Persisted deck archive** — `is_archived` columns on `letter_deck`/`vocab_deck`
  (previously dead, added by migration 13) are now real: declared in the SQLDelight
  schema, backed by `updateDeckArchived` repository methods, and toggleable from the Deck
  Edit → Save dialog
- **Theme Studio v2.0** — complete rewrite with a functional color editor: interactive HSV
  color wheel, synchronized RGB/HSL/HSV/HEX editors, 11 color targets, palette, gradient
  editor (Linear/Radial/Angular, multiple stops, angle, intensity, opacity), live preview
- **Floating Island Sidebar v2.0** — drag to reposition, snap-to-edge detection with
  spring animation, 9 dock states, resizable in floating mode, borderless elevated
  appearance with soft glow
- **Brush Quality Engine** — stroke smoothing (moving-average low-pass), input prediction
  (velocity + acceleration), Bezier smoothing (Catmull-Rom spline), velocity-based adaptive
  smoothing, jitter reduction, pressure sensitivity; full `processStroke()` pipeline
- **Branded Installer** — 8-screen premium installation wizard (welcome, location,
  components, theme, accent, accessibility, progress, completion)
- **Onboarding Wizard** — 8-step first-launch setup with live previews and skip-all
- **Anki-like features** — flag manager, enhanced note editor, keyboard shortcuts page,
  suspend/bury/forget/reschedule operations, universal search, tree-based deck browser

### Changed
- Theme Studio custom-color tab now writes to theme state via `accentScheme`
- Corner radius selector and sidebar mode/position selectors extracted into reusable
  components
- `SliderWithLabel` component for consistent slider UX
- All `String.format()` calls replaced with KMP-safe `formatFloat()`
- All `Divider` → `HorizontalDivider` (Material3)

### Fixed
- Color picker now actually updates theme state (was using local `remember` only)
- No `String.format()` / `System.currentTimeMillis()` / `Key.Digit0`-style usages (KMP
  compilation errors)
- Missing `@OptIn(ExperimentalMaterial3Api::class)` annotations added
- No duplicate function definitions

## v1.1.0

### Added
- Undecorated window with floating window controls
- Spring-based hover animations on window controls
- Theme system with `BaseMode` (Light/Dark/Oled), accent schemes with gradient support,
  glow/radius/animation/density configuration
- Appearance Studio with live preview
- Signature theme (Lime + Orange)

### Changed
- Rebranded from Kanji Dojo to Kaiteyo (書いてよ)
- Complete documentation system in `/docs/`

### Fixed
- `animateColorAsState` import (now from `androidx.compose.animation`)
- `animateFloatAsState` import (now from `androidx.compose.animation.core`)
- `windowState.window!!.close()` → `window.close()` in FrameWindowScope
- Missing `@Composable` import
- Desktop app compilation (BUILD SUCCESSFUL)

## v1.0.0 (Initial Fork)

### Added
- Forked from Kanji Dojo
- Basic flashcard study system with spaced repetition algorithm
- JLPT decks (N5–N1), stroke-order diagrams, vocabulary lookup
- Cross-platform support (Desktop, Android, iOS)
- Koin dependency injection, SQLDelight database, Ktor HTTP client, DataStore preferences
