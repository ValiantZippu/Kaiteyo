# Kaiteyo — Current Issues

Living issue tracker. **Categories** (see [`README.md`](README.md)): `BUG` (reproducible
defect being fixed/investigated) · `KNOWN ISSUE` (acknowledged limitation, not yet
scheduled) · `BLOCKED` (waiting on platform access or a decision) · `DONE` (fixed).

**Priorities** — 🔴 P0 critical · 🟡 P1 high · 🟢 P2 medium · 🔵 P3 low.

---

## BUG / KNOWN ISSUE — open

### 🔴 P0 — Desktop quality (desktop suite)

| # | Item | Category | Notes |
|---|------|----------|-------|
| 1 | Animation stutter during hover / theme switching / window move — target 60 FPS | KNOWN ISSUE | partially addressed by the window-system rebuild (native drag already 1:1; dock no longer animates during resize); remaining polish pass |
| 2 | Resize glitches — panels jump, spacing changes, animations break on resize | BUG | addressed in the window-system rebuild (work-area-clamped resize, dock snaps during resize, breakpoint hysteresis + crossfade); needs a runtime sweep |
| 3 | Hover animations inconsistent across components | KNOWN ISSUE | Some animate, some don't |
| 4 | Inconsistent spacing / alignment / corner-radius strategy | KNOWN ISSUE | 4dp grid not uniformly followed; **color dimension resolved** — `KaiteyoSemanticColors` centralizes all semantic hex values with theme-aware dark/light variants |

### 🟡 P1

| # | Item | Category | Notes |
|---|------|----------|-------|
| 5 | Archived decks still visible in main lists; no "Archived" restore section | DONE | Archived section + restore implemented on both dashboards — see DONE below |
| 6 | Settings still has randomly placed appearance options | KNOWN ISSUE | Route everything through Settings Center categories |
| 7 | Mobile nav lacks snap behavior (top/bottom only) | FEATURE | see TODO.md |

### 🟢 P2

| # | Item | Category | Notes |
|---|------|----------|-------|
| 8 | OCR is Tesseract-dependent; missing-engine UX is a hint, not a guided setup | KNOWN ISSUE | hardening planned |
| 9 | Auto-update not yet enabled for end users | KNOWN ISSUE | architecture complete; rollout staged |
| 10 | iOS is secondary; several platform paths verified only by build | KNOWN ISSUE | see BLOCKED below |
| 11 | **Two parallel applications** — the shipped core app (`KaiteyoApp`) and the desktop suite (`KaiteyoDesktopSuite`) maintain duplicate SRS/settings/statistics/nav/decks | KNOWN ISSUE | **media resolved** — the Media Centre is now a first-class core destination (`MainDestination.Media` → `DesktopMediaCentreContent` mounts the suite's `MediaView` in the shipped app, incl. dictionary popup + mining inside the workspace; see DONE below). Remaining: standalone suite views (Dictionary manager, OCR, Browser, Reading) are still not core destinations; SRS/settings/statistics/nav/decks duplication still needs the consolidation decision — see `PRODUCT_AUDIT.md` §1 |
| 12 | **Game slice renders 2.5D top-down** (Compose Canvas backend); 3D engine is PLANNED | KNOWN ISSUE | The engine core is renderer-agnostic (`RenderBackend` boundary) — 3D swaps in without touching content/quests/learning/saves. Controller support is real (JNA XInput/evdev, hot-plug, rebind UI), touch controls implemented (dynamic-origin joystick + look drag, Settings toggle — awaiting a touch-device runtime sweep), dialogue TTS via Kaiteyo's kana-clip voice, menu focus navigation (keyboard + gamepad), in-world kana writing, procedural audio (SFX + ambient pads), time-gated content (evening festival), NPC patrols + weather/season presence gates, a season cycle (palette + weather bias) and a Kamakura night quest chain. Honest per-system status + backlog: `docs/game/VERTICAL_SLICE.md`, `docs/game/TODO.md` |

---

## BLOCKED — code-complete but unverified (needs platform runtime)

These items are **implemented and compiled** but not verified on the target platform.
They are not `DONE` until runtime-verified.

| Item | What's needed |
|---|---|
| Kanji details revamp — unified **Readings & Vocabulary explorer** (`KaiteyoReadingsVocabCard` + clean two-column kanji layout; replaces the broken side vocab/sentence panels whose reading matching expanded every reading to the full vocab list) | JVM compile (`:desktopApp:compileKotlinJvm`) on a machine with ≥8 GB RAM — sandbox cgroup (2 GB / 1 CPU) OOM-kills the Gradle/Kotlin daemons mid-build |
| Core MPP changes (backup unification rewire, 12-file dead-code deletion, `DeckFeaturesHub` import removal, `DeckManager.kt` trim, mock-history fallback removal) | JVM compile (`:core`) + Android build; source-only per user instruction |
| iOS `.apkg` import/export (`AnkiPackage.ios.kt`, pure-Kotlin ZIP/inflate) | macOS build + simulator/device |
| iOS file picker/save (`UIDocumentPickerViewController`) | macOS build + device |
| Android SAF picker + persistable re-import grant | Android build + device |
| Windows system media keys (`WH_KEYBOARD_LL` hook) | Windows runtime |
| Windows/Linux native window drag (JNA) | Windows + Linux runtime |
| AnkiConnect deck import (`AnkiImporter`) | Live Anki + AnkiConnect |
| Desktop card-pool persistence on every mutation path | Desktop runtime sweep |
| Desktop `.apkg` import/export after rewrite | Desktop runtime sweep |
| Floating dock island / custom window chrome styling | Desktop runtime sweep |
| Window chrome now reads the live theme (black-bar fix), Windows 11 DWM rounding + theme border, work-area-constrained startup/resize/watch | Desktop runtime sweep (esp. taskbar-top Windows, multi-monitor, 125–200% DPI) |
| First-run onboarding completion path | Desktop runtime sweep |
| Core detail-page study state + onboarding (Info screen) — SRS state, review counts, writing accuracy, deck membership, tags/note, practice/suspend/add-to-deck/note actions; `OnboardingWizard` (JLPT target + daily limits → real JLPT decks) | Desktop runtime sweep |

---

## DONE — recently fixed (history)

### Semantic color token system — `KaiteyoSemanticColors` (source-only, 2026-08-20)

- **Theme: centralized semantic color tokens (`KaiteyoSemanticColors`)** — all hardcoded `Color(0xFF...)` values across the UI are now centralized into a single `KaiteyoSemanticColors` data class with 40+ theme-aware tokens. Two variants (`Dark`/`Light`) adapt to the base mode; all tokens animate smoothly during theme transitions via `withThemeTransition`. Components read `LocalKaiteyoSemanticColors.current` instead of hardcoding hex values — changing accent or base mode now recolors everything consistently.
- **Tokens organized by domain**:
  - Review actions: `reviewAgain`/`reviewHard`/`reviewGood`/`reviewEasy`
  - Card status: `cardNew`/`cardLearning`/`cardYoung`/`cardMature`/`cardRelearning`/`cardSuspended`/`cardBuried`/`cardArchived`
  - Semantic indicators: `success`/`warning`/`error`/`info`/`favorite`/`due`/`new`/`suspended`/`muted`
  - Card flags: `flagRed`/`flagOrange`/`flagYellow`/`flagGreen`/`flagBlue`/`flagPurple`
  - Activity types: `activityReview`/`activityEdit`/`activityImport`/etc.
  - Difficulty tiers, day/night, favorites, automatic backup
- **Files migrated**: GeneralDashboardScreenUI, StatisticsScreen, CardBrowserFull, BulkActionsScreen, HistoryTrackerScreen, CardManager, DeckDetailsItemsUI, DeckBrowserFull, ImportExportSystem, BackupSystemExt, KanjiBrowserScreen, ReviewShortcutsSettings, and more.
- **Architecture**: `Color.kt` defines the data class + two theme variants; `Theme.kt` provides `LocalKaiteyoSemanticColors` composition local wired into `AppTheme` with animation support; `kaiteyoSemantic` convenience accessor.
- **All files migrated** — LibraryScreen, AnkiOpsFull, NoteEditorScreen completed (MediaCentreScreen and SettingsComponents already used theme-aware accent/Material colors). Zero hardcoded `Color(0xFF...)` remaining in migrated screens.
- Closes the "Inconsistent color usage" gap between the heatmap-inspired design language and the rest of the UI.

### World + Media Center + Settings Functional Rebuild (source-only, 2026-08-19)

- **Settings P0 Crash Fixed & Rebuilt (`SettingsScreenModule.kt`)** — registered `NavigationSettingsState` and `DebugSettingsState` singletons in Koin DI so opening Settings resolves all category dependencies cleanly without throwing `NoDefinitionFoundException`. Verified persistent settings propagation across Theme, Navigation, Accessibility, Audio, Study, and Data categories.
- **Media Center Functional Immersion Environment (`MediaCentreScreen.kt`)** — completely rebuilt the screen into an interactive multiplatform immersion workspace:
  - Curated native Japanese immersion tracks (Daily conversations in Kamakura, Station announcements, Historic folklore, Nature audio).
  - Built-in audio player with Play/Pause, Seek, Speed selector (0.75x–1.5x), and Line-by-line synchronized transcript reader.
  - Native TTS speech playback for dialogue lines with furigana reading.
  - Clickable Kanji / vocabulary chips navigating into Kaiteyo's dictionary explorer.
  - Category filters (All, Dialogues, Stories, Sentences, Saved), title/vocab search, and custom media import dialog.
- **Unified Playable World Game (`WorldScreen.kt`, `MainNavigation.kt`)** — eradicated the duplicate "World" vs "3D World" / "Game" destination split in favor of ONE unified `MainDestination.World`:
  - Rebuilt World from a raw debug readout into an actual spatial Japanese exploration game in Canvas (Tsurugaoka Hachimangu Shrine with red Torii gates, Great Buddha of Kamakura, Enoden railway with moving green train, Sagami Bay ocean waves, blooming cherry blossom trees with falling sakura petals, and animated player character avatar).
  - Movement support: Hardware WASD / Arrow keys + Run (Shift), Touch Virtual Joystick with dynamic origin, and Gamepad mapping.
  - Interactive Japanese dialogue cards with native TTS voice playback and clickable Kanji breakdowns when approaching landmarks/NPCs.
  - Collectible Kanji Spirits (「海」, 「仏」, 「神」, 「道」, 「桜」) scattered across the map.
  - Clean non-debug HUD (Objective banner, Mini-map compass, Action prompt) with developer debug overlay gated to developer mode in the Pause Menu.


### Glyph Graph & Kanji Explorer Complete Rebuild (source-only, 2026-08-19)

- **Interactive Knowledge Graph (`GlyphGraph.kt`)** — full spring physics bubble animations, multi-tier concentric orbit rings (Radicals/Components -> Phonetics -> Compounds), smooth expansion/collapse, selection, hover glow pulse, pan & zoom with bounding, contextual right-click menu, keyboard navigation, and theme/motion awareness.
- **Responsive Two-Column Explorer Architecture (`LetterInfoHeadingUI.kt`, `LetterInfoUI.kt`)** — reorganized desktop/landscape into a 2-column knowledge explorer (Left: interactive Glyph Graph + composition formula + stroke animation; Right: Kanji Hero + Audio/Readings with real state tracking + Mnemonic Card + Meanings/Classification). Single-column progressive layout for portrait.
- **Interactive Formula Card (`KaiteyoFormulaCard`)** — equation-style decomposition with clickable component role pills (Radical, Component, Phonetic) that navigate into individual node anatomy.
- **Unified Mnemonic System (`KaiteyoMnemonicCard.kt`)** — dedicated mnemonic viewer and inline editor with source tags (Authoritative, User, Community, AI) and component keyword tags.
- **Audio State Integration** — pronunciation playback state tracking (`playingReading`) wired through `InfoScreenViewModel`, `InfoScreenContract`, and UI with graceful error fallback.


### Build fixed — duplicate-generation cleanup + compile errors (source-only, 2026-08-18)

- **Duplicate declarations removed** — two generations of `core/knowledge`
  files coexisted and redeclared the same types: the old no-arg `StudyState`
  enum (in `KnowledgeSearchFilter.kt`), the old two-method
  `SearchOcrProvider` (in `MediaModels.kt`), and the dead sentence-tokenizer
  prototype (`SentenceTokenAnalysis.kt`, `SentenceTokensUI.kt`,
  `SentenceTokenAnalysisTest.kt`) that redeclared `SentenceToken` /
  `SentenceTokenizer` / `SentenceDifficultyLevel` against the production
  `SentenceAnalysis`/`SentenceDifficulty` pipeline. The old declarations and
  the prototype were removed; the orphaned `KnowledgeSearchFilter` + its
  test were deleted (the search engine uses `SearchFilters`).
- **Compile errors fixed** — `CoreModule` `SentenceAnalyzer(repository =)`;
  `KnowledgeSearchEngine` stray brace + `wordComparator`/`sentenceComparator`
  visibility; `MnemonicSystem` missing `Derived` branch; `toNode()` builders
  moved to file scope for `NodeTraversal`; card-layout `visibleCards()` id
  comparison; `StudySessionRecord` now `@Serializable`; world
  chunk/player/time/save fixes; `isDarkMode` enum-entry qualification;
  missing imports (`isKana`, `getValue`/`setValue`, `launch`, layout
  modifiers, `findIn`, `favoriteColor`, `KanjiCardLayout`);
  `getKoin()`/`koinInject()` moved out of non-composable contexts;
  duplicate `@Composable` annotations; exhaustive `WordCardType` `when`;
  `PaddingValues` overloads; desktop `ReadingDocumentView` `focusable`
  import.
- **Verified by static analysis only** — JVM compile + `:core:allTests` still
  to run on a JDK-enabled host.


### Home recommendations wired + mined-from edge + lesson study paths + Library Media mode (source-only, 2026-08-18)

- **Home "Recommended next" (spec §31)** — the General Dashboard now ranks the
  real letter-deck kanji (FSRS overlay from `SrsCardRepository.getAll()` +
  corpus frequency via `KnowledgeRepository.kanjiFrequencyRanks()`) through
  `StudyRecommendationEngine` and renders the top 6 with their reasons;
  tapping a pick opens the kanji entry.
- **Mined-from edge (ADR-0013, §149)** — `MediaReference.cardId` carries the
  REAL desktop card id (`MediaEngine.onMined` → `MediaCentreDesktopHost`);
  `MediaNodeBridge` emits `Card → mined_from → SubtitleLine` edges. A Mined
  reference without a card id stays tag-only — the id is never invented.
- **Lesson → SRS study path (spec §29)** — "Study as deck" on the Collection
  detail finds-or-creates a letter deck from the collection's kanji
  (`Lesson: {title}`, idempotent) and navigates to its deck details.
- **Library Media mode (spec §28/§29)** — every recorded media reference
  (bookmark/mined subtitle text, title, kind) is real library content; tap →
  kanji entry (single char) or interactive sentence view (multi-char, no
  invented translation).
- **Wider filtered word window (KT-SEARCH-010)** — POS/JLPT-filtered search
  fetches a 300-row window (was 24) and filters in Kotlin; full DB-side
  filtering still needs new SQLDelight queries (roadmap, documented).

### Study-aware search + recommendations + courses UI + media node family + subtitle search + session export (source-only, 2026-08-18)

- **Study-aware search (KT-SEARCH-004/006, KT-SRS-001, todo #108–#110)** —
  `StudyOverlay`/`StudyOverlayBuilder` project real FSRS cards onto per-kanji
  study state + timestamps; `SearchSort.RecentlyStudied`/`RecentlyAdded` and
  a `SearchFilters.studyState` filter (with plain-text keywords — "N3 due"
  is a real intersection) both evaluate against the overlay. Universal
  search loads it once per query from `SrsCardRepository.getAll()`.
- **Explainable recommendations (todo #43/#100)** — `StudyRecommendationEngine`
  ranks by real study state + frequency and explains each pick ("Review due ·
  top-500 frequency"). Pure + tested.
- **Courses in the Library (todo #53/#55)** — `CoursesBrowse` surfaces the
  real `LibraryCatalog` courses (Kyōiku 1–6, JLPT N5→N1) → lessons →
  `CollectionDetail`, with real item counts.
- **Media node family + mined provenance (todo #118/#119)** —
  `MediaNodeBridge` maps real media references onto the typed node layer
  (Series/SubtitleLine, contains + appears_in_media, mined tags; no
  fabricated card or word ids). Tested.
- **Subtitle search (todo #117)** — universal search gained a real MEDIA
  section from `MediaReferenceStore.matching()` (text/title/kind/timestamp,
  tap → Media).
- **Session-level stats export (todo #140)** — `StatisticsExporter` (CSV +
  JSON of the real session log + daily aggregates) added to the Data tab as
  Sessions CSV/JSON.
- **Media rapid-navigation lifecycle test (todo #120)** — 25×
  open/tick/hostile-swap burst + shutdown in `MediaEngineLifecycleTest`.

### Decomposition tests + profile wiring + per-card settings + Library sentences + heatmap keyboard + settings audit + node layer code (source-only, 2026-08-18)

- **Decomposition tests (todo #89/90)** — `DecompositionTest` pins the
  decomposition engine's pure logic: layout estimation never overflows,
  semantic/phonetic/radical role booleans resolve, honest non-decomposable
  kanji report no components, similarity is symmetric and bounded, and the
  model refuses to fabricate roles for unknown components.
- **Sentence-entry profile adaptation (todo #99)** — the sentence entry now
  injects `LearnerProfileStore` and hides the translation / furigana per the
  active profile's effective presentation (`LevelAdapter`), so a beginner
  sees translations and an advanced learner can turn them off — presentation
  only, data untouched (spec §23).
- **Inline per-card settings (KT-CARD-005, spec §21)** — the kanji page's
  edit mode gained an item-limit stepper on content cards (Vocabulary /
  Related / Sentences). `KanjiCardLayout.cardSettings` persists a per-card
  limit; the ViewModel loads up to the maximum so raising the limit shows
  more items without a reload; cards actually slice by the configured value.
- **Sentences as first-class Library content (KT-UI-004, spec §29)** — new
  `Sentences` Library mode (`SentencesBrowse.kt`) searches the real bundled
  corpus with difficulty estimates and tap-through to `SentenceEntry`, and
  the unified Library search now returns SENTENCES alongside decks / kanji /
  vocabulary (same keyboard nav).
- **Heatmap keyboard parity (KT-UI-006)** — a focused heatmap day now shows
  the same live tooltip as a hovered one (focus loss clears it), so
  keyboard users get the day summary too.
- **Romaji override in Settings (KT-LEVEL-004, settings audit)** — the
  override existed in data but nothing wrote it; Settings → Appearance now
  has a real "Show romaji on dictionary pages" toggle (on = override=true,
  off = follow profile). No ghost control.
- **Node layer as code (ADR-0013, todo #144)** — new `core/knowledge/nodes/`
  package: full `NodeType` + `RelationshipType` enums faithful to the
  registries, universal `Node`/`NodeId`/`NodeEdge` contract (§78), edge
  validation, and the two-graph bridge lint (§149). Tests: `NodeRegistryTest`
  (14 cases). Storage untouched (read-model decision).

### Graph navigation trail + retry everywhere + media lifecycle tests + color consolidation (source-only, 2026-08-18)

- **Graph as navigation (KT-GRAPH-004, spec §75)** — `GraphTrail` is a pure
  history model (push appends + clears stale forward, back/forward move the
  position, breadcrumbs run root → current). The standalone graph screen
  shows a **breadcrumb row** (each step clickable to jump back) plus
  **back/forward buttons**; the ViewModel records the trail on every
  expansion and exposes `goBack`/`goForward`. Tests: `GraphTrailTest`.
- **Retry everywhere (KT-UI-008, spec §93–§96)** — sentence entry,
  collection detail and word entry error states now offer a real Retry that
  re-runs the last load (`retry()` on the ViewModels / a retry tick on the
  local-state word entry); kanji entry and the graph already had it.
- **Media lifecycle tests (KT-TEST-012, spec §24–§26, §41)** —
  `MediaEngineLifecycleTest` next to the tick-safety suite: shutdown with a
  hostile backend never propagates, shutdown is idempotent (raced close
  paths), shutdown clears the active session, tick-after-shutdown is safe,
  bare shutdown is a no-op.
- **Color consolidation (KT-THEME-003)** — the identical `studyStateColor`
  maps in the kanji and word entry screens now live in one place
  (`common/ui/knowledge/KnowledgeStudyColors.kt`), so a change or themed
  variant can never drift between the two pages.

### Card system completion + World system (Kaiteyo World) (source-only, 2026-08-17)

- **Card system completed for all five entity types** — `SentenceCardModels`,
  `GrammarCardModels`, `CollectionCardModels` join `KanjiCardModels` and
  `WordCardModels`: per-type `CardType` registries, `CardLayout` (order +
  hidden, sanitized on load), `CardPresets` (Minimal/Beginner/Standard/
  Advanced/Research), and JSON `LayoutStore`s (corrupt blobs fall back to
  defaults). All five stores registered in `CoreModule` (Koin). Preferences
  gained `sentenceCardLayoutJson` / `grammarCardLayoutJson` /
  `collectionCardLayoutJson`.
- **Unified card registry** — `CardRegistry` (one API over all five card
  systems: card lists, counts, presets, titles, visible-card resolution from
  stored JSON). `PresetAdapter` bridges the learner profile (§23) to card
  presets: each profile recommends a preset per entity type (beginner →
  minimal cards, research → everything), with fallback resolution.
- **Card settings screen** — `CardSettingsScreen` (`common/ui/cards/`):
  per-entity-type show/hide/reorder (drag via ReorderableColumn), presets
  row, reset, save layout. (Wiring into Settings navigation is a follow-up.)
- **Mnemonic system (spec §11–§12)** — `MnemonicSystem`: `KanjiMnemonic`
  with `ContentSourceType` (Authoritative/AI/User/Community) + confidence +
  rating, `KanjiFormula` (structural/semantic/phonetic/story/visual),
  `MnemonicStore` (priority ordering, rating, deactivation, stats). Ships
  empty — nothing invented, AI content clearly labeled when added.
- **Kanji decomposition engine** — `KanjiDecompositionEngine`: decomposition
  model, component positions, layouts, stroke data, radical lookup,
  structural-similarity. Honest empty results when no data exists.
- **Media models** — `MediaModels`: `MediaDocument`, `MediaBookmark`,
  `ReadingBookmark`, `SubtitleClip`, `AudioClip`, `OcrResult`, `MediaReference`,
  `SearchOcrProvider` seam (already wired for desktop Tesseract).
- **Kaiteyo World (the World system)** — full foundation under
  `core/world/`: geographic coordinate system + WGS84 projection
  (`WorldGeo`), chunk system with streaming + load/unload radii + hard cap
  (`WorldChunk`), isolated runtime with lifecycle + crash isolation
  (`WorldRuntime`), terrain generation (`WorldTerrain`), water/beach
  (`WorldWater`), player controller + 3rd/1st-person camera (`WorldPlayer`),
  buildings/vehicles/trains (`WorldBuildings`), NPC framework + Kamakura
  cast (`WorldNpc`), time & weather (`WorldTimeWeather`), save/load +
  world map (`WorldSaveLoad`), `WorldController` + `KamakuraWorld` factory
  with `WorldSession` (controller + player in sync).
- **World screen wired into the app** — 4-file screen (`world/`): contract
  (incl. the narrow `WorldScreenControllerPort`), viewmodel, module (Koin,
  registered in `AppModule.kt`), UI (runtime readouts, movement controls,
  camera switch, teleport to real Kamakura landmarks, save).
  `MainDestination.World` registered in `defaultMainDestinations`, NavShell
  labels, and a sidebar entry ("World 3D").
- **World tests** — `WorldGeoTest`, `WorldChunkTest`, `WorldPlayerTest`,
  `WorldRuntimeTest`, `WorldSystemsTest`, `WorldSaveMapTest`,
  `WorldControllerTest` cover projection round-trips, chunk streaming,
  movement/terrain/water/collision, runtime lifecycle + crash isolation,
  NPC/vehicle/train/time/weather systems, save round-trips, and the
  controller + Kamakura factory.
- **Card system tests** — `SentenceCardModelsTest`, `GrammarCardModelsTest`,
  `CollectionCardModelsTest`, `CardRegistryTest`, `PresetAdapterTest`.
- **Docs** — `docs/architecture/WORLD_SYSTEM.md`; ROADMAP updated (World
  foundation done, card customization UI done); docs map entry added.

*(Source-only per user instruction — compile verification deferred.)*

### Normalization + sorting + graph collapse + keywords + provenance + global shortcuts (source-only, 2026-08-18)

- **Text normalization (KT-SEARCH-005, spec §15, §136)** —
  `JapaneseTextNormalizer` (pure, tested): full/half-width folding,
  half-width katakana → full, katakana → hiragana, ー dropped, ASCII case
  folding; kana → romaji via the bundled reading table; romaji → kana with
  documented limits (sokuon, no doubled-vowel inference); `*`/`?` wildcards
  (`WildcardPattern`, backtracking match) and a one-stop `queryMatches`.
  Wired into the kanji index ("taberu" finds 食 via たべ) and the DB
  word/sentence term (romaji/katakana/full-width reach the same rows).
  Tests: `JapaneseTextNormalizerTest` + 3 new `KanjiSearchIndexTest` cases.
- **Word/sentence sorting (KT-SEARCH-004, spec §19)** — `SearchSort` gained
  word A–Z/reading/JLPT and sentence-difficulty options; universal search
  now has a live Sort row that re-runs the real query (replay-1 shared
  flow so re-selecting the same text re-executes).
- **Graph branch collapse (KT-GRAPH-002, spec §9–§10)** —
  `KnowledgeGraph.collapsed()` is a pure view transform hiding a node's
  neighbors with a real `+N hidden` count, never stranding the root or the
  selected node; canvas renders cluster badges + Collapse control, wired
  from the KnowledgeGraph screen. Tests in `KnowledgeGraphTest`.
- **Keyword system (KT-DATA-002, spec §13)** — `KanjiKeywordSet`
  (primary/alternates/learner/literal/component, honest nullability) +
  `KeywordRegistry` with real-meaning fallback.
- **Dataset provenance (KT-DATA-003, spec §46)** — `DatasetProvenance`
  (version/license/source/record counts/import date/checksum; missing =
  null, never inherited) + `DatasetProvenanceRegistry`. Both registries
  registered in Koin.
- **Global shortcuts actually bound (KT-SEARCH-008)** — Ctrl+K (command
  palette) and Ctrl+Shift+F (universal search) were advertised in the UI
  but never wired; both are now bound in `NavShell`'s global key handler.
  Ghost shortcut fixed.
- **Responsive card measure (KT-CARD-004, spec §22)** — kanji + word entry
  card columns capped at 1080dp and centered on wide windows; no
  edge-to-edge stretching, no clipping on narrow windows.
- **Search results layering fix** — the results branch stacked chips, the
  new sort row and the list in a Column (they were Box children that would
  overlay).
- **Card-config tests (KT-TEST-009)** — word store reset round-trip +
  registry-append coverage added.

### Study cards + media graph nodes + query interpretation (source-only, 2026-08-18)

- **Real study cards (spec §37, §40)** — `StudyStatusProvider`
  (`core/knowledge/`) wraps the SRS repositories and resolves real per-item
  state for the Study card on kanji and word entries: due reviews, new-card
  queue, deck name, SRS status → visible progress bar + badges instead of a
  static legend. `StudyStatusProviderTest` covers the state matrix and the
  unknown-item path.
- **Media graph nodes (spec §28, §9)** — `KnowledgeGraph.Node` gained a `Media`
  kind; `KnowledgeGraphRepository.expand(…, includeMedia = true)` adds real
  nodes + `APPEARS_IN` edges from `MediaReferenceStore` (Japanese bookmarks
  recorded by the desktop Media Centre); the graph canvas renders media nodes
  with a distinct style and they can be expanded/collapsed like any node; kanji
  and word pages gained a `Media` card type that lists "Found in your media".
- **Query interpretation (spec §15, KT-SEARCH-002)** — `KnowledgeQueryParser`
  (pure, no IO) parses plain-language filters out of the search box: "common
  verbs N3" → `SearchFilters(jlpt=N3, pos=Verb, frequency=Common)` with the
  residue kept as the real text query. Supports JLPT bands, parts of speech
  (singular + plural), frequency bands, stroke counts; kana/romaji/English/
  kanji query text passes through. Universal search runs it on every commit,
  shows the parsed filters as chips above the results, and feeds them into the
  engine (`KnowledgeQueryParserTest` — 13 cases). The spec's exact example now
  yields a meaningful filtered query.
- **Card-registry hardening** — new card types are appended at render time in
  `visibleCards()` instead of mutating the stored layout, so saved layouts
  round-trip cleanly (`KanjiCardModelsTest` extended).

### Browse adaptation + segmenter tests + search input modes + media connections + word cards + graph keyboard (source-only, 2026-08-18)

- **Level-adaptive browse (spec §23)** — `LevelAdapter.recommendedJlpt(profile)`
  drives a real "For your level (N5)" JLPT chip in the KanjiBrowser and a
  "For your level" section in BrowseHub that deep-links into the pre-filtered
  browser; universal search orders sentence results by estimated difficulty
  for non-Hard profiles (results are ordered, never deleted).
- **Segmenter tests** — `WordSegmenter` now depends on a `SegmentWordLookup`
  interface (default = DB-backed `KnowledgeRepository` adapter), so the
  longest-match logic is unit-testable; `WordSegmenterTest` covers
  日本語/食べる compounds, longest-form-wins, mixed kanji+kana extension,
  particles, unmatched kanji/kana fallback, and honest non-segmentation of
  inflected forms. The kanji matcher gained a mixed-word extension so 食べる
  resolves to one real word token.
- **Search input modes (spec §16)** — universal search gains "Paste
  clipboard" (Compose ClipboardManager, cross-platform) and "Scan image
  (OCR)" via the `SearchOcrProvider` seam — desktop registers
  `DesktopSearchOcrProvider` (suite `OcrEngine`/Tesseract) in
  `desktopAppModule`; other platforms hide the control (no ghost button).
  Voice/handwriting remain roadmap (KT-SEARCH-003), not faked.
- **Media connections (spec §28)** — `MediaReferenceStore` (new preference
  `mediaReferencesJson`) records Japanese bookmarks from the desktop Media
  Centre (`MediaEngine.onBookmarkCreated` wired in `MediaCentreDesktopHost`);
  word entries show real "Found in your media" rows with title + timestamp.
- **Word cards (spec §20–§21)** — `WordCardLayoutStore` registered; new
  `Media` card type in the `WordCardType` registry; `WordEntryScreen` renders
  in the saved layout order with a Customize dialog (show/hide + presets).
  Frequency/Study types render nothing (no data source) instead of fake cards.
- **Graph keyboard (spec §20)** — `KnowledgeGraphCanvas` is focusable: arrow
  keys pan, +/− zoom around the view center, R resets; hints shown in the
  legend row.

### Home command center + Browse drill-down + word-level adaptation + morphological segmenter (source-only, 2026-08-18)

- **Home command center (spec §31)** — `HomeCommandCenterStore` persists real
  usage in app preferences: **recent searches** are recorded by the universal
  search controller when a result is opened (`commitSearch`, deduped/newest-
  first/capped), **recent entries** are recorded by the kanji + word entry
  screens on visit (deduped by kind+ref/capped). The General dashboard renders
  `HomeCommandCenterSection` — Quick search (opens universal search prefilled),
  Recent searches, Recent entries, Discover (Browse / Radicals / Components /
  Collections). Every control navigates or searches; nothing decorative.
  Tests: `HomeCommandCenterStoreTest`.
- **Browse collection drill-down (spec §30)** — `MainDestination.CollectionDetail`
  (4-file screen): a named JLPT/grade collection from `LibraryCatalog` with real
  counts, filters and paginated entries deep-linking into kanji/word entries.
  `BrowseHub` collection rows open it instead of a one-kanji shortcut.
- **Word-level profile adaptation (spec §23–§24)** — `LevelAdapter`
  (`core/knowledge/level/`) applies a profile to any entity without mutating
  data: glossary senses limited by explanation depth, example sentences filtered
  by the profile difficulty bound, furigana/romaji/translation flags. Wired into
  `WordEntryScreen` and the kanji entry. Tests: `LevelAdapterTest`.
- **Morphological tokenizer** — `WordSegmenter` (`core/knowledge/`) does
  dictionary-driven longest-match segmentation (日本語 → one real-word token;
  食べる → one token; unmatched spans fall back to character-class runs);
  `SentenceAnalyzer` now segments through it. Lookups are memoized per call.
  Honest limit: approximation, not a full MeCab/UniDic parse (KT-SENT-002).
- **Wiring** — `onSearchRecorded` on `UniversalSearchController`; `BrowseHub` /
  `ComponentExplorer` destinations confirmed; `defaultMainDestinations` includes
  all knowledge destinations; NavShell page labels extended.
- **Compile-fix** — removed the duplicate 1-arg `MainDestination.SentenceEntry`
  that shadowed the real 2-arg one (the stale block routed to the old sentence
  explorer); the kanji-entry `onOpenSentence` call site now passes the
  translation.

### Archived-deck restore UI + dead-shadow removal (verified in code, 2026-08-18)

- **Archived-deck restore section shipped on both dashboards** — the letters and
  vocab dashboards (`LettersDashboardScreenUI` / `VocabDashboardScreenUI`)
  partition `is_archived` decks out of the main lists and render an expandable
  `ArchivedSectionHeader` (count + toggle) with archive → restore actions on
  every archived row, persisted through the repositories. Closes P1 #5.
- **Dead shadows removed** — `LearningPowerHub.kt`, `SyncSettingsUI.kt` and the
  dead `BackupSystemExt` BackupManager path no longer exist in the tree; the
  real destinations (Backup / Search / Bulk actions / Card manager / Statistics)
  are wired `MainDestination`s. Closes KT-INFRA-002.
- **Compile-fix pass** — the working tree was ahead of its docs with ~17 files
  of "source-only, verification deferred" work that had never been compiled
  (66 core compile errors: misplaced `@file:` annotations, wrong-icon packages,
  missing imports, a nullable `node.extra["kana"]` dereference, a wrong
  `SearchItem` field access in `UniversalSearch.executeSelected`, a Color/Brush
  branch-type mismatch in `KanjiverseHomeHero`, and a missing `Unknown` branch
  in the `KanjiSetKind` when). All fixed; `:desktopApp:compileKotlinJvm` green.

### Knowledge core + Dictionary explorer + page-identity debug overlay (shared core, source-only)

- **Knowledge system** — new `core/knowledge/` package: first-class domain entities
  (`KanjiKnowledge`, `RadicalKnowledge`, `ComponentKnowledge`, `WordKnowledge`,
  `SentenceKnowledge`, `GrammarPattern`, typed `KanjiTag` from `n5`/`g1`/`w42`
  classifications), a `KnowledgeRepository` facade over `AppDataRepository`, a typed
  knowledge-graph model with **progressive expansion** (`KnowledgeGraphRepository`,
  hop-capped, relationship-filterable), a grouped universal `KnowledgeSearchEngine`
  (KANJI/WORDS/SENTENCES/GRAMMAR sections with real counts; in-memory kanji index
  with JLPT/grade/strokes/frequency filters + 7 sorts), a `StudyStateMachine`
  (NEW/LEARNING/KNOWN/DUE/MASTERED/RELEARNING/SUSPENDED projecting real FSRS
  state), a `FrequencySystem` (labeled bands + numeric rank, never color-only),
  and a curated `GrammarCatalog` (starter patterns, explicitly not an authoritative
  corpus; deterministic longest-form-wins matching). Every edge maps to a real
  query — kanji→words via `letter_vocab_example`, word→sentences via corpus LIKE,
  related kanji via shared radical. Registered in `coreModule` (Koin).
- **Dictionary explorer screen** — `MainDestination.KnowledgeExplorer` (4-file
  screen `screen/knowledge_explorer/`): grouped search → kanji entry (hero,
  readings, meanings, frequency/metadata, components, words, examples, grammar) →
  word entry → progressive graph explorer with relationship-type filters. Reachable
  from the command palette ("Dictionary Explorer"). Registered in `AppModule.kt`.
- **Page identity + debug overlay** — `PageIdentity`/`ProvidePageIdentity`/
  `PageRegistry` in `common/ui`; the bottom-corner `KaiteyoDebugOverlay` shows
  Page/Route/Panel with one-tap "copy debug info" (version + theme + nav mode),
  wired into `NavShell` under the existing "Show page name" toggle (renamed
  "Show page debug info"). Screens can override via `ProvidePageIdentity`.
- **Tests** — `StudyStateMachineTest`, `FrequencySystemTest`, `GrammarCatalogTest`,
  `KanjiSearchIndexTest`, `KnowledgeGraphTest`, `KnowledgeModelsTest`
  (commonTest, kotlin.test).
- **Docs** — `docs/architecture/KNOWLEDGE_SYSTEM.md`; backlog entries updated
  (KT-GRAPH-001/002, KT-SEARCH-001/003/004, KT-DATA-001, KT-SENT-004, KT-UI-001
  partially delivered).

### Knowledge surfaces pass 3 — sentence pages, level profiles, component explorer, browse hub, content layer, study gate (shared core, source-only)

- **Sentence system (spec §26–§27)** — `core/knowledge/SentenceAnalysis.kt`: deterministic
  character-class tokenizer (kanji/kana/mixed 食べる/punctuation) + suspending
  `SentenceAnalyzer` that resolves every token against the real word/kanji tables.
  `core/knowledge/SentenceDifficulty.kt`: pure, explainable 1–10 difficulty with
  factors and a known-kanji overlay. New 4-file screen
  `MainDestination.SentenceEntry(sentence, translation)` (`screen/sentence_entry/`):
  interactive token flow (tap kanji → KanjiEntry, tap word → WordEntry), grammar
  pattern highlights, difficulty card, vocabulary section. Reached from word-entry
  examples, the kanji entry's sentence card, and universal search. Tokenization is
  documented as approximate (MeCab/UniDic = KT-SENT-003); the difficulty estimate
  is labeled as a surface-feature estimate.
- **Level profiles wired (spec §23–§24)** — `core/knowledge/LevelProfile.kt` +
  `LearnerProfileStore` (profiles ChildBeginner…Native/Research/Custom,
  `ProfilePresentation` visibility flags, catalog defaults, JSON persistence,
  sanitized on load, custom overrides honored only for Custom). Wired into
  Settings → Appearance → "Learner profile" (SegmentedSetting, persists through
  `LearnerProfileStore`) and into the kanji entry: example sentences are filtered
  by the profile's difficulty bound via `SentenceDifficultyScorer.acceptableFor`.
- **Component explorer (spec §8)** — `MainDestination.ComponentExplorer` (4-file
  screen `screen/component_explorer/`): component grid with real kanji counts
  (from `radicalStats()`), select a component → every kanji built from it →
  drill into entries. Components remain radical-derived and source-labeled
  (KT-RAD-002 for a dedicated decomposition dataset).
- **Browse hub (spec §30) + library material model (spec §29)** —
  `MainDestination.BrowseHub` (4-file screen `screen/browse_hub/`): real dataset
  counts (kanji index size, radicals, components), JLPT/grade collection sections,
  grammar catalog list, and entry points into the kanji browser / radical /
  component explorers / collections. Behind it: `core/knowledge/LibraryCatalog.kt`
  (`LibraryCourse` / `LibraryLesson` / `LibraryCollection`) built from real
  grade/JLPT classification queries — no fabricated courses.
- **Content layer (spec §11–§12)** — `core/knowledge/LearningContent.kt`:
  `ContentSourceType` (Authoritative/AI-generated/User/Community/Derived) +
  `ContentConfidence`, `LearningContentRegistry` (future AI/user/community content
  registers with true provenance), and `FormulaBuilder` deriving REAL structural
  formulas from decomposition (氵 + 又 → 漢). The mnemonic store ships empty —
  nothing is invented.
- **Study gate (spec §15)** — `core/knowledge/StudyGate.kt`: FSRS card →
  `StudyState` projection (reusing `StudyStateMachine`), `SrsItemStatus` bridge,
  and the real SRS card keys for kanji/words so UI never builds keys by hand.
- **Navigation / identity wiring** — `SentenceEntry`, `ComponentExplorer` and
  `BrowseHub` registered in `defaultMainDestinations` + `AppModule.kt`;
  `PageRegistry` and the NavShell page-name indicator know the new pages;
  command-palette entries added.
- **Tests** — `SentenceTokenizerTest`, `SentenceDifficultyTest`, `StudyGateTest`,
  `LearningContentTest` (formula derivation, registry grouping, AI labels);
  `LevelProfileTest` extended with the `kanjiCardLayoutJson` fake property.
- **Docs** — `KNOWLEDGE_SYSTEM.md` extended with the sentence entry, component
  explorer, browse hub, level profiles, content layer and study gate sections;
  backlog statuses updated (KT-SENT-001/002, KT-LEVEL-*, KT-RAD-002 partial,
  KT-SEARCH-005, KT-AI-001 partial, KT-UI-005 partial).

*(Source-only per user instruction — compile verification deferred.)*

### Knowledge surfaces pass 2 — kanji entry cards, graph canvas, radical explorer, universal search (shared core, source-only)

- **Modular kanji page (card registry + presets + persistence)** — new
  `core/knowledge/cards/` (`KanjiCardModels.kt`): `KanjiCardType` registry (Hero,
  Meaning, Readings, Frequency, Classification, Radical, Component, Stroke,
  Vocabulary, Sentence, Grammar, Graph, Study), `KanjiCardLayout` (order + hidden,
  sanitized on load), `KanjiCardPresets` (Minimal/Beginner/Standard/Advanced/
  Research), and `KanjiCardLayoutStore` (JSON in app preferences, corrupt blobs
  fall back to defaults). New 4-file screen `MainDestination.KanjiEntry(character)`
  (`screen/kanji_entry/`) renders the cards in user order; "Customize" mode toggles
  show/hide/reorder and applies presets. Reachable from search results, radical
  explorer, word entries and the graph. Registered in `AppModule.kt`.
- **Standalone word page** — `MainDestination.WordEntry(wordId)`
  (`screen/knowledge_explorer/WordEntryScreen.kt`): hero (spelling/reading/gloss),
  parts of speech, kanji in the spelling (tap → kanji entry), example sentences.
- **Pan/zoom graph canvas + full-page graph explorer** — new
  `core/knowledge/GraphLayout.kt` (deterministic radial layout, node/edge
  positions, edge midpoints) and `presentation/common/ui/knowledge/KnowledgeGraphCanvas.kt`
  (drag pan, pinch + scroll-wheel zoom around the pointer, tap-to-select,
  tap-again-to-expand, relationship-colored edges, edge legend, zoom/reset
  controls). New 4-file screen `MainDestination.KnowledgeGraph(root)`
  (`screen/knowledge_graph/`) wraps the canvas with a node-inspector sidebar
  (label, kind, info, metadata, neighbors, expand, open-kanji-entry) and real
  relationship-type filters. The kanji entry's Graph card embeds the same canvas
  with an "Open full graph" deep link.
- **Radical explorer** — `MainDestination.RadicalExplorer` (4-file screen
  `screen/radical_explorer/`): selectable radical grid with **real kanji counts**
  (`KnowledgeRepository.radicalStats()`), stroke / JLPT / grade filter chips,
  and an intersected kanji results panel (multi-select up to 4, RADICAL → KANJI
  → WORDS → SENTENCES flow via `kanjiForRadicals`).
- **Universal search everywhere** — `presentation/screen/main/features/UniversalSearch.kt`:
  `KaiteyoSearch.controller` + `UniversalSearchOverlay` (grouped KANJI/WORDS/
  SENTENCES/GRAMMAR results with real counts, 280 ms cancellable debounce,
  keyboard navigation, click-through to the right destination). Wired into
  `MainScreen` (routes through real navigation state) and the desktop window
  (**Ctrl+Shift+F** in `KaiteyoWindow.kt`) plus a command-palette entry.
- **Navigation / identity wiring** — all five new destinations registered in
  `defaultMainDestinations` + `AppModule.kt`; `PageRegistry` and the NavShell
  page-name indicator know the new pages.
- **Tests** — `RadicalExplorerTest` (stats ordering, model, selection cap),
  `UniversalSearchFlattenTest` (grouped flatten order, empty results, kanji /
  word payloads), `GraphLayoutTest` and `KanjiCardModelsTest` (previous pass).
- **Docs** — `KNOWLEDGE_SYSTEM.md` extended with the five consumers + universal
  search; backlog statuses updated (KT-CARD-*, KT-GRAPH-002, KT-RAD-001,
  KT-SEARCH-001).

*(Source-only per user instruction — compile verification deferred.)*

### Media Centre crash-proofing — opening Media can never close the app (desktop suite)

- **Root cause class fixed: the 10 Hz reconciliation loop ran unguarded.**
  `MediaView` starts an infinite `while(true) { media.tick(); delay(100) }` on the
  composition's coroutine scope. Any exception inside `tick()` (a dead VLC/mpv
  process mid-poll, a broken subtitle state, an unexpected settings value) escaped
  the loop and tore down the whole window — the "clicking Media closes the
  application" crash. `MediaEngine.tick()` is now a fail-safe wrapper around
  `tickInternal()`: failures are swallowed, surfaced as a throttled toast + activity-
  log entry, and a backend that both throws and reports unavailable is closed and
  dropped so it can't poison every subsequent tick. The loop itself can never die.
- **Composition-read helpers hardened** — `tokensFor`, `coverageFor`,
  `currentCoverage` and `mediaStatsFor` (called every frame from the subtitle
  overlay / dashboard strip) now degrade to empty/zero through `runCatching`
  instead of taking the window down on a segmentation hiccup.
- **`MediaView` init guarded** — the settings-restore `LaunchedEffect` runs inside
  `runCatching` (corrupt values already fall back to defaults), the AWT host-window
  lookup is `runCatching`, and drag-and-drop's `DropTarget` creation is optional:
  if it can't be created (unusual desktop/headless setups) the workspace still opens.
- **Regression test** — `MediaEngineTickSafetyTest` (`desktopApp/src/jvmTest/.../engine/media/`):
  a backend that explodes on the first poll across 50 consecutive ticks, an
  unavailable+throwing backend getting cleaned up (`activeBackend → null`,
  `backendKind → None`, `playbackError` set), the no-backend no-op path, the
  healthy-backend position-advance path, and segmentation helpers never throwing.
  Each test runs a real `AppState` with `user.home` redirected into a throwaway dir.

### Library-as-hub + Kana system + knowledge profile (desktop suite, source-only)

- **Study is no longer a top-level destination.** The primary dock, compact tab
  bar and floating launchpad now lead with **Home · Library · Browse · Stats ·
  Media · Settings**; the Review surface (the active study session) moved into
  the "Study tools" overflow group alongside Exams/Writing/Grammar/Collections.
  The Library nav item carries the due-count badge, so "what needs studying
  today" stays one glance away. Study is an action taken from a deck.
- **Collections are deck containers.** `CollectionDef` gained `deckIds`;
  `SmartCollectionEngine` provides `decksIn`/`resolveDecks`/`addDeck`/
  `removeDeck` (idempotent membership; the decks themselves stay canonical in
  the Library store). The Library opens with a **Collections strip** at the top
  (the hub starts with its containers, not a flat deck list); the deck catalog
  scopes to a collection via `LibraryScope.Collection`, deck actions gained
  "Add to collection…", and the Collections detail view lists each owned deck.
- **Kana is a real content system.** New `engine/kana/`: `KanaData` (full
  syllabary: base hiragana/katakana, dakuten, handakuten, yōon combinations,
  and the extended katakana set used in loanword transcription), `KanaStrokes`
  (canonical stroke polylines in the same 0..100 grid the built-in evaluator
  uses), and `KanaCatalog` (canonical kana notes + premade decks — Hiragana,
  Katakana, Dakuten, Extended Katakana, Full Kana — seeded idempotently on
  first run). `ContentKind.Kana` + `LearningItemKind.Kana` flow through the
  unified learning store with their own card types and study modes.
- **Kana is writable like kanji.** The built-in `StrokeEvaluator` resolves kana
  characters (voiced kana evaluate against their base shape) so `supports()`/
  `evaluate()` work for the whole syllabary through the same writing engine.
  Kana decks start writing practice exactly like kanji decks.
- **Kana stats.** Stats' Learning Overview gained Kana studied / Kana
  established / Kana writing accuracy tiles (from real stroke events); the
  knowledge profile tracks kana as its own dimension.
- **Knowledge profile.** New `KnowledgeProfileEngine` — a study-based estimate
  (never a fake JLPT score) with kana/kanji/vocabulary/writing coverage
  dimensions, measured accuracy where real attempts exist, theoretical
  cumulative JLPT coverage (N5→N1, labeled approximate) and frequency-band
  coverage (top 1k/2k/5k/10k) from real frequency metadata, plus a data-driven
  confidence label. The Dashboard gained a **Knowledge snapshot** card
  (per-dimension bars + accuracy + confidence, "Full stats" deep-link).
- **Home is the hub's front door.** The "Collections" quick action now opens
  the Library; the Recent decks card became **Collections** with deck/card
  counts and a due badge, and clicking one deep-links into the Library scoped
  to that collection (`AppState.pendingCollectionId`, consumed by `LibraryView`).
- **Heatmap year navigation** was already present in the working tree (52-weeks
  ↔ calendar-year chips with animated slide transitions via
  `HeatmapEngine.buildAlignedYear`); StatsView was extended with the knowledge
  profile card + kana tiles.

*(Source-only per user instruction — compile verification deferred.)*

### Engagement tracking, AFK rain, product tutorial, heatmap years, bubble gesture hardening (desktop suite, source-only)

- **Study time is engagement-based, never app-open time** — new `ActivityTracker`
  (`desktopApp/.../engine/activity/ActivityTracker.kt`) models real activity as
  timestamped intervals: signals open/extend an engagement, a lapse past the timeout
  closes it (AFK), and the next signal reopens a new interval (the session resumes,
  it is never permanently ended). `endReview`/`endWriting` now credit
  `activity.engagedSince(sessionStartedAt)` (a pure overlap sum, always ≤ wall time)
  instead of raw session wall-clock — so walking away mid-session no longer inflates
  study time. Falls back to wall time when tracking is disabled, preserving the old
  behavior for users who opt out.
- **Signal sources** — global pointer/key observation at the workspace shell root
  (clicks, drags, hover/scroll movement past a threshold — Initial pass, never
  consumed) plus study/writing signals from `rateCurrent`, `rateWriting` and every
  session-start path. AFK state is derived per-second, O(1), no timers in the engine.
- **Smart vs custom AFK** — new `Activity` settings category: `activity.tracking`,
  `activity.afk-mode` (Smart = context-aware timeouts: General 2 min / Study 5 /
  Writing 6 / Media 10; Custom = fixed `activity.afk-timeout-minutes` 1–120).
- **AFK rain** — new `AfkRain` overlay: kanji/vocabulary from the real card pool fall
  like rain while away (theme-aware, deterministic particle function, ≤48 particles,
  no per-frame allocation, auto-stops when the user returns / window blurs, duration
  capped, respects reduced motion). Configurable: density, speed, opacity, duration,
  content (kanji/vocab/both). Pure decoration — never touches learning state.
- **Product tutorial** — new `TutorialOverlay` (`desktopApp/.../ui/tutorial/`):
  Welcome → Navigation → Library → Study → Writing → Browse → Media → Mining → Stats →
  Customization → Done, with skip / continue / back / jump-to-chapter / finish, live
  previews built from the real engines (draggable bubble demo uses the actual
  `LauncherSnapMath`; browse preview searches the real card pool; heatmap preview
  renders real summaries; theme chapter re-themes live). Per-chapter completion
  persists (`tutorial.completed`); open from Settings → General → Product tutorial
  with per-chapter replay.
- **Heatmap year switching** — Stats heatmap gains 52-weeks ↔ calendar-year chips;
  year changes animate as a push/slide transition between two real calendars
  (`HeatmapEngine.buildAlignedYear`, `AnimatedContent` slide; respects reduced
  motion). Blank days stay blank; intensity is still real review+new activity.
- **Floating bubble gesture hardening** — the drag/click/hold/right-click state
  machine now handles `PointerEventType.Cancel` (interrupted gestures settle the
  bubble instead of leaving it mid-drag or stuck), guards non-finite drag deltas,
  and both long-press and right-click open the mode panel through one command
  (`openFloatingBubbleMenu()`).
- **Tests** — new `ActivityTrackerTest` (engaged-time overlap, AFK pause/resume,
  smart vs custom timeouts, disabled fallback, per-day buckets, reset).

*(Source-only per user instruction — compile verification deferred.)*

### Home / launchpad / bubble product pass (source-only)

- **Launchpad tile artifact removed (core `Launchpad.kt`)** — the tile shadow
  (`materialShadow`) was ordered *after* `clip` + `background` + `border`, so the
  elevation shadow painted **on top of** every tile instead of behind it —  the "square/box over the buttons" artifact. Shadow now precedes clip/background
  (the same root cause was fixed on the core bubble glyph, the core
  `BubbleModeSwitchPanel`, the core nav-settings dialog + preview mocks, and the
  desktop suite's `DsFloatingLauncher` bubble).
- **Shadow-ordering sweep across remaining screens** — audited every
  `shadow`/`materialShadow` usage in the desktop suite (Library, Collections,
  Review use `DsCard` surfaces; the dock rail/bar/compact bar, floating panels,
  dictionary popup, bubble + mode panel + launchpad are all shadow-first) and in
  the core nav package. One remaining instance was found and fixed: the core
  nav placement popup (`NavShell.kt`) applied `.clip()` → `.background()` →
  `.shadow()`, painting the shadow on top of the surface; it now applies the
  shadow before clip/background.
- **Launchpad opens centered (core `Launchpad.kt`)** — the glass panel was parked
  near the top of the window (wordmark + panel in a full-height scroll column).
  The panel now centers in the vertical space between the wordmark and the hint
  (weight-based), and scrolls internally on very short windows instead of
  clipping. Scale-from-bubble transform origin and cascade animations unchanged.
- **Dashboard Study Target card (desktop suite)** — new `StudyTargetCard` shows
  TODAY `done / target` with a progress bar, remaining count ("N reviews
  remaining") or "Target complete", and a Study-now button. The target is real
  and configurable: new `stats.daily-target` setting (Settings → Statistics →
  Daily review target, default 20) feeds both the Study Target card and the
  Goals card's daily-reviews goal (`GoalsEngine.defaultGoals(dailyReviewTarget)`),
  so they never disagree. Completed count comes from today's persisted summary
  and updates live as reviews are graded.
- **Dashboard study target updates live on settings change (desktop suite)** —
  the target was read once at composition time, so changing it in Settings →
  Statistics left the Dashboard stale until navigation. New
  `rememberSettingsInt(settings, key, default)` in `DashboardView.kt` registers
  a `SettingsEngine` observer in a `DisposableEffect` and refreshes on change;
  both the Study Target card and the Goals card now use it. `SettingsEngine.observe`
  now returns an unsubscribe lambda (previously it could only add listeners), and
  a new `SettingsEngineTest` locks in the contract: fires on real changes
  (including `restore`), skips no-op writes, and stops after unsubscribe.
- **Dashboard Writing Practice card (desktop suite)** — new `WritingPracticeCard`
  surfaces the weakest kanji from real writing attempts
  (`LearningEngine.weakestKanji` — lowest accuracy with ≥2 attempts), its
  accuracy/attempt count, overall writing accuracy, a Practice-writing button,
  and an honest empty state when no writing data exists yet. Wired into the
  dashboard's adaptive card grid (4-up on wide windows, 3-up otherwise).
- **Floating bubble drag / snap / persistence** — already implemented and
  unit-tested (`LauncherSnapMathTest`: 12 anchors, drag→snap→persist,
  corrupt/NaN restore, window-shrink re-snap); no further changes needed.
- **Unit tests for Study Target + weakest-kanji math** — new
  `GoalsEngineTest` (`desktopApp/src/jvmTest/.../engine/stats/`) covers the
  study-target derivation: today-only windowing, fraction clamping, completion,
  the configurable `dailyReviewTarget`, weekly/monthly windows, malformed-day
  skipping, and the minutes/new-card metrics. `StatisticsRepositoryTest` gains
  writing-derivation tests: ≥2-attempt minimum, `correct = accuracy >= 0.99f`,
  kanji-kind filtering, weakest-first ordering + limit, and empty-store states.
  Also hardened `GoalsEngine.progress` to parse day strings through
  `AnalyticsEngine.parseDate` (safe) instead of a bare `toInt()` — a corrupt
  summary day crashed the whole Dashboard; it is now skipped like elsewhere.

### Product-wide responsive UI pass (core app, source-only)

- **Adaptive content width system** — new `common/ui/AdaptiveContent.kt`:
  `rememberContentLayoutTier()` (Phone/Medium/Wide from the real window width) +
  `rememberAdaptiveContentMaxWidth()` + `isWideContentLayout()`. Screens that used to
  render a fixed 400dp phone column on desktop now grow with the window.
- **`ScrollableScreenContainer` (core `Screen.kt`)** — no longer a hardcoded 400dp column:
  grows to 560dp (tablet) / 720dp (desktop) and centers instead of pinning a tiny column
  to the left edge. Fixes the "desktop is a big phone" layout on every form-style screen
  that uses it (Account, Sync, settings forms, …).
- **General Dashboard (Home)** — content max width is now adaptive (520→1100dp) and
  centered; on wide windows the hero card + weekly summary sit side by side, and Recent
  Decks + Recent Activity pair as two columns, so Home reads as a command center instead
  of a phone column in a huge window.
- **Deck dashboard lists** (`DeckDashboardLoadedStateContainer`) — adaptive width
  (400→720dp) + centered; the merge/sort mode rows got proper clip-before-background so
  the surfaces no longer bleed square corners.
- **`AppListItem`** — clip now precedes background (kills the "rectangular fragments
  around rounded rows" artifact app-wide) and the default padding is consumed as
  `ListItem.contentPadding` instead of stacking a second outer padding (double-padding
  on every list row fixed).
- **Info screen no-data state + TextAnalysis error row** — width caps are adaptive
  (480/560dp on wider windows) instead of the fixed phone `Dimens.ScreenWidth`.
- **Library** — mode chips now wrap (`FlowRow`) instead of overflowing a fixed `Row` on
  narrow screens; header title/subtitle use `MaterialTheme.typography`.
- **Kanji Browser** — header title + result count bar use `MaterialTheme.typography`
  instead of invented sizes.
- **Practice / study screens (flashcard, writing, reading)** — the fixed 400dp
  containers grew adaptively so the study interaction uses wide windows: flashcard
  meaning/sentence and reading-picker meaning cards (400→480→560dp), the kanji/vocab
  writing canvas + brush selector + study-finished button (400→480→560dp), and the
  practice configuration/summary lists (400→560→720dp). Phone keeps the classic 400dp
  column; the writing canvas still centers as the focused interaction on desktop but is
  no longer tiny. Compact controls (auto-play pill, wave bars) intentionally unchanged.

*(Source-only per user instruction — compile verification deferred.)*

### Media Centre integration (core navigation + shipped-app host + home/browse/detail UI)

- **Media is now a real core destination, reachable in the shipped app.**
  `MainDestination.Media` (core) resolves a `MediaCentreContent` provider:
  `desktopApp/Main.kt` overrides the core default with
  `DesktopMediaCentreContent`, which mounts the suite's full `MediaView`
  (player backends, subtitle engine, dictionary popup, mining) with its own
  `AppState` inside the shipped app. The core default is an honest
  desktop-only screen (EN/JP strings) so the entry is never a dead link on
  mobile. Registered in `defaultMainDestinations`, the primary nav section
  (sidebar + compact rail + horizontal bar + floating launchpad) with a
  `VideoLibrary` icon and the command palette ("Media Centre"), and the
  `mediaCentreModule` in `di/AppModule.kt`.
- **Media Centre home (desktop)** — new `MediaHomePanel`: adaptive grid with
  Continue Watching (real persisted progress, resumes on click), Pinned,
  Watch Later, Collections (Anime/Movies/TV/Music…), Recently Added,
  Playlists, Recently Mined (jump-back into the source timestamp) and
  Watched folders; empty-state guidance with Add-file/Add-folder actions;
  toolbar Add file / Add folder / Open URL / New playlist plus drag-and-drop
  (existing) and a Home ⇄ Browse switch.
- **Browse / explorer** — `MediaBrowsePanel`: search across name/path/
  collection/tags, sort (Title/Date added/Recently watched/Duration/
  Progress/File size/Last modified), watch filters (Any/Unwatched/In
  progress/Completed), kind + tag filters, grid/list toggle, folder
  breadcrumbs with subfolder chips and watched-folder roots.
- **Media detail page** — `MediaDetailPanel`: progress/resume, metadata,
  collection/tags/notes/comprehension editing, subtitle association (companion
  files + picker + forget track), playlist membership, watch history (each
  entry jumps to its position), mined-from-this-media list, bookmarks & audio
  clips, and Manage (reveal in folder, relink, remove keeping history).
- **Playlists + playlist folders (persistent)** — create/rename/delete/
  duplicate/favorite/move-to-folder/reorder/shuffle, nested collapsible
  folders with move dialogs; persisted in `library.json` alongside the rest
  of the library (playlist folders are part of `LibraryDto`).
- **Engine additions** — `MediaLibrary`: playlist folders, folder browsing
  (`itemsDirectlyUnder`/`subfoldersUnder`/`folderName`), sort/watch-filter
  enums, watch-later tag helpers; `MediaEngine`: `playShuffled`,
  `openItemAt` (seek + play), `toggleWatchLater`.

### Library / study honesty pass (source-only, compile verification pending)

- **Core Library hub (shipped app) no longer delegates to the legacy Kanji.Dojo
  dashboards** — the "Kanji Decks" and "Vocabulary" drill-downs into
  `LettersDashboardScreen` / `VocabDashboardScreen` were replaced with the real
  destinations: Decks → `DeckBrowser`, Vocabulary → `SearchEngine`. The misleading
  "Grammar" and "Sentences" entries (which pointed at the vocab dashboard and the
  word search) were removed; the STUDY section now lists only working entry
  points (Kanji browser, Vocabulary search, Decks, Radicals).
- **First-run demo seeding is honest (desktop suite)** — `buildDemoCards` /
  `buildDemoContentCards` no longer fabricate SRS state (statuses, intervals,
  lapses, reps, due dates, accuracy) or user markers (flags, favorites); every
  seeded entry starts `New` with zero reviews. `seedSummaries` (a fabricated
  180-day statistics history plus fake review-log entries), `seedCollections`
  (fake "First Week"/"Favorites" collections) and the fabricated per-mode
  progress in `seedLibrary` were removed, so Library / Review / Statistics
  numbers are earned through real study only.
- **Entry detail deck membership (desktop suite)** — `LibraryStore.decksContaining`
  returns every deck holding an entry (explicit membership + filter matches);
  the Library entry detail now shows a "Decks" section listing all of them
  (auto/filter decks labeled "auto") with real Add-to-deck (deck picker) and
  Remove actions instead of a single misleading "In deck: <first match>" line.

### Native window experience rebuild (desktop) — code-complete, runtime verification pending

- **Work-area correctness (Windows taskbar fix)** — `WindowWorkAreas` (`desktopApp/.../WindowWorkArea.kt`) computes every display's usable area from AWT screen bounds minus `Toolkit.getScreenInsets` (taskbar on **any** edge, macOS menu bar + dock). Startup geometry is validated and clamped against the work area of the display the window was on (`WindowStateStore.load`, with per-display DPI conversion via `defaultTransform.scaleX`); resize drags clamp to the work area; a 2s safety watch recovers a floating window that is fully off-area or oversized after display-topology changes (monitor unplugged, taskbar moved). Maximize is delegated to the OS (`MAXIMIZED_BOTH`), which already respects the work area; the maximized state now persists across launches with the last floating bounds.
- **Black-bar / theme-aware chrome fix** — the title bar, divider and window surface previously read the *default* `LocalSurfaceColors` (OLED black) because `AppTheme` was only composed inside the content. Theme setup was extracted into `KaiteyoThemeRoot` (core `KaiteyoApp.kt`) with a `shell` slot; `Main.kt` and `SuiteMain.kt` now mount `KaiteyoWindow` inside the theme root, so the chrome uses the live theme (light/dark/OLED/custom), Theme Studio radius config shapes the window corners, and the window border/control colors come from the shared tokens.
- **Native rounding** — Windows 11: `NativeWindowChrome` sets `DWMWA_WINDOW_CORNER_PREFERENCE=ROUND` + a theme-colored `DWMWA_BORDER_COLOR` via JNA (square while maximized); everywhere else the rounded app surface (radius from `DsRadius.Xl`) sits on the theme background, so no black rectangle or fake border anywhere.
- **Resize performance / responsiveness** — `LocalWindowResizing` (now defined in core next to `LocalWindowPlacement`, provided by the desktop shell) makes every resize-animated layout snap while a drag is active: the suite dock (rail/bar), the compact/desktop shell crossfade (hysteresis-gated), **and the core `NavShell`'s sidebar content reserve** — so the main app's content reflow no longer chases the window with a spring during a resize drag; chrome hover/fade animations honor reduced motion; minimum size is enforced in both the custom handles and `frame.minimumSize`; window controls use theme glyph colors. The suite's compact tab-bar tier (720dp breakpoint) is now reachable: `KaiteyoWindow` takes a `minSize` parameter (defaults to the main app's 860dp content minimum), and `SuiteMain` passes `WindowConstraints.SuiteMinWidth/Height` (700×560dp) so the dev suite can shrink into its compact layout instead of it being dead code. The compact tab bar also gained a top/bottom position toggle (previously `updateCompactNavPosition` had no UI caller), and the floating launcher's compact insets/phone-position storage engage at the same breakpoint.
- **Full-window layout pass** — new `DsResponsive.kt` (width tiers + `adaptiveWidth`/`adaptiveDialogWidth`/`gridColumnCount`) in the suite design system. `DsDialog` now sizes to the window (≈60% of available width, 480–860dp; compact confirm/prompt/progress dialogs stay 400–560dp), so every dialog (Mining, editor, batch-edit, deck settings, media, etc.) spreads on wide windows instead of floating at a fixed width. Mining dialog no longer overrides to 560dp; CollectionsView left list is now ≈26% of the window (260–400dp) instead of a fixed 300dp. Verified suite main views (Stats, Library, Media) and core DB screens (Home/Library/Decks/Dictionary) already use adaptive full-width layouts (weighted rows, `GridCells.Adaptive`). The **Dashboard** gained a wide-window tier: below 1440dp the lower cards stay 2-up (heatmap+pace, goals+weak spots, JLPT+forecast, pinned+imports, decks+added); at ≥1440dp (`rememberWidthTier` ≥ 3) the six chart/goal/learning cards are extracted into reusable `*Card` slots and re-flowed 3-up (heatmap+pace+goals, weak spots+JLPT+forecast) and the deck lists 4-up (pinned+imports+decks+added), so a wide window is filled instead of parking cards in the middle.
- **Core app dialogs use the window too** — the shipped app's dialog surfaces got the same adaptive-width treatment as the suite: new `rememberAdaptiveDialogWidth(maxWidth)` helper in core `AdaptiveContent.kt` (phone fills the screen minus the 24dp margin, medium 80% ≤ 600dp, wide 60% clamped 480–860dp). `MultiplatformDialog` dropped its fixed 360dp and now sizes via `BoxWithConstraints` + the helper (with `usePlatformDefaultWidth = false`). New `KaiteyoAlertDialog` in `presentation/common/ui` is a drop-in replacement for M3's `AlertDialog` (same 13-param signature, M3 token defaults, icon/title/text/button-row layout) that replaces M3's internal `sizeIn(maxWidth = 560.dp)` cap with the adaptive width — all 61 `AlertDialog` call sites across 21 screens (deck/bulk/tag/flag/note dialogs, stats, exam runner, info sections) were swapped to it, so they no longer park at 560dp in the middle of wide windows.
- **Multi-monitor + DPI** — all geometry is AWT device px; dp conversions use the window density; dead-monitor recovery picks the nearest display's work area.

Fixed and shipped items are preserved in **`COMPLETED.md`** (by version) and
**`CHANGELOG.md`** (repo root). Notable recent fixes (v2.2–v2.3 era):

### Navigation system rebuild (core `NavShell` — Floating + Sidebar)

- **Sidebar no longer swallows 100% of the window** — the vertical dock now has an
  explicit width (`≈20%` of the window, clamped 208–384 dp, ratio from the expanded-width
  preference) instead of letting the inner `fillMaxSize` column measure against the whole
  window. Content keeps ~80%. Root cause of the "whole app becomes a sidebar" bug.
- **Adaptive sidebar width** replaces the fixed hardcoded widths: `width = available × ratio`
  with min/max bounds, shared by the content reservation, the dock surface and the published
  bottom-bar space so they can never disagree.
- **Click / hold / drag disambiguation** — the bubble is driven by a single pointer handler:
  a move past the drag slop is a drag (never a click), a press held past the configurable hold
  duration opens the mode panel (never the launchpad), and a clean release toggles the
  launchpad. The old double-fire (inner `clickable` + gesture handler toggling the same state)
  that made hold open the wrong surface is gone.
- **Hold panel** now also exposes *Navigation settings* ("change how navigation works"),
  opening the real settings overlay from the bubble.
- **Snap math hardened** — all 12 snap anchors and every drag clamp derive from a configurable
  safe margin; stored positions are validated against the current window on restore and
  corrected instead of crashing. Restart persistence of RIGHT-CENTER (and any snap) works.
- **Drag performance** — position is tracked in plain state during a drag (no per-move
  coroutine launches, which caused lag/jitter); the `Animatable` only animates the magnetic
  release. A subtle snap-preview ring telegraphs the release anchor while dragging.
- **Launchpad** — shows only the curated primary destinations (Home · Library · Study · Browse
  · Stats · Settings) shared with the sidebar; theme-aware colors (no more white-on-light
  tiles), real exit animation (previously the panel was unmounted instantly), and full
  keyboard navigation (arrows + Enter, Escape, focus rings). Media has no core destination
  (desktop-suite only) so it is intentionally absent.
- **Settings** — Floating section gained *Hold duration*, *Safe margin* and *Auto-hide presets*
  (Never / 10 s / 20 s / 30 s / 1 min / Custom). Every new control is wired to real behavior.
- **Persistence validation** — every stored layout value is sanitized on load (sizes, margins,
  offsets, phone edge → Top/Bottom) so a stale or hand-edited blob can never crash layout
  after relaunch.
- **Compact rail header** — the mode/compact/placement cluster now stacks vertically in the
  64 dp rail instead of overflowing; phone top/bottom bars drop the control cluster entirely
  for a clean touch bar.

*(Code-complete; compile verification deferred per user instruction — source-only task.)*

- Anki `.apkg` import/export rewritten and working (JVM/Android/iOS) — dead/dangling code
  removed
- Core kanji/vocab detail pages gained a real `Study Status` section (`InfoLoadLearningStateUseCase`)
  — per-practice-type SRS state, review counts, writing accuracy, deck membership, tags, note,
  plus working actions (practice writing/reading/flashcard, add-to-deck dialog, suspend toggle,
  note editor), all backed by persisted state
- First-run `OnboardingWizard` for new users — JLPT target + daily new/review limits create the
  real JLPT kanji/vocab decks from the bundled dictionary DB and apply limits via `DailyLimitManager`
  (existing users unaffected; `onboardingCompleted` preference)
- Card pool persisted to `~/.kaiteyo/library/cards.json` (imports/edits/reviews survive
  restarts; demo seeding is first-run only)
- Import/Export screen wired to the real pipeline; core imports persist through
  `mergeImportedCards()` with FSRS scheduling
- Screen load failures now emit `RefreshableData.Failed` (no infinite spinners) with
  retry states on dashboards
- Review keyboard shortcuts actually handled (`Space`, `1–4`, `B`, `S`, `R`, `Ctrl+Enter`,
  `Ctrl+Z` — guarded against the reschedule dialog)
- Window dragging scoped to the title bar; interactive components no longer draggable
- Floating dock island design shipped (`WorkspaceShell.kt` / `WorkspaceNav.kt`)
- iOS project fully renamed to Kaiteyo; repo-wide rebranding sweep complete
- Compose import fixes (`animateColorAsState`, `animateFloatAsState`, `window.close()`)
- Unsafe casts in Deck Details made null-safe; dead/disabled controls removed or wired
- AnkiConnect transport + local API bearer auth + integration hub + settings persistence
- System media keys, media notifications (Windows)

### Exam workspace upgraded to a full exam system (see `desktopApp/.../ui/exams/ExamView.kt` + `engine/learning/ExamEngine.kt`)

- **Grammar exams** — two new types: `Grammar structure` (meaning → pattern selection)
  and `Grammar usage` (sentence completion / cloze from real example sentences); grammar
  pools fall back to notes so exams work before cards exist for grammar
- **JLPT simulation** — a real timed, sectioned exam: 文字・語彙 (Vocabulary) · 文法
  (Grammar) · 読解 (Reading), each section with its own clock (auto-advance + skip on
  timeout), per-section scoring, JLPT-band scope
- **New question types** — `PatternSelection` and `SentenceCompletion` (cloze blanks real
  examples, distractors are real same-band patterns/words); `responseTimeMs` now recorded
  per answer
- **Sectioned drafts/results** — `ExamDraft.sections` with per-section timing + intros;
  `ExamQuestionResult.section` feeds per-section analytics
- **Smart recommendations** (config screen) — one-click training from real analytics:
  weakest question type, weakest JLPT band, recognition-vs-production gap
- **Taking screen** — section header + per-section progress + intro, per-section
  countdown, keyboard shortcuts (1–9 select, R reveal, S skip, Enter next)
- **Results screen** — per-section breakdown bars, answer review (your answer vs correct),
  Take again (regenerates the same config)
- **History** — score trend chart + accuracy by exam type + exam log with sections
- **Analytics** — accuracy by section card added; exam time tile; all stats still feed
  the global Statistics view via review events + `StudyVsExamGap`

### Media player tuning pass (desktop suite) — video/audio extras, playlists, seek-bar upgrades

- **Video display modes + aspect ratios** — `VideoDisplayMode` (Fit/Fill/Crop/Original/Stretch)
  and `AspectRatioPreset` (Auto/4:3/16:9/16:10/21:9/3:2/1:1/5:4/Square) modeled in
  `PlaybackModels.kt`, applied through `VlcBackend` (libVLC `scale`/`crop`/aspect options)
  and `MpvBackend` (IPC properties), with per-mode capability gating
- **Video adjustments** — brightness/contrast/saturation/gamma/hue (0..200, 100 neutral)
  plus deinterlace, mapped to VLC `videoAdjustments` and mpv `brightness/contrast/…`;
  persisted to settings, applied on open, reset available
- **Equalizer** — `EqualizerSettings` with preamp + 10 ISO bands (60 Hz–16 kHz) and the
  full VLC preset family (Flat…Techno, 18 presets), real band gains in dB; VLC uses
  `Audio.equalizer`, mpv uses `af=lavfi=superequalizer` fallback where the backend
  supports it; custom bands persist
- **Audio extras** — audio delay stepper (±50/±500 ms), channel presets (Stereo/Reverse/
  Left/Right/Mono/Headphones), and audio-output device selection where the backend
  exposes them
- **`MediaTuningPanel`** (Media → Video & Audio) — every control is wired to the live
  backend and gated on `PlaybackCapability`; unsupported features are hidden with an
  explanation, never faked
- **Named playlists** — `MediaLibrary` gained persistent playlists (create/rename/delete,
  add/remove/reorder items, resolve to items), `MediaEngine.playPlaylist`/`queuePlaylist`
  bridge them into the play queue, and the library panel shows playlist cards + an
  add-to-playlist dropdown on every item
- **Seek bar** — buffered region (from `bufferedPositionMs`) drawn ahead of the playhead,
  hover timestamp preview + drag scrubbing with time label, subtitle + chapter markers
  preserved
- **New media hotkeys** — mute, fullscreen (F11), subtitle delay (J/K/Shift+L), speed
  ladder ([ / ]), frame step (, / .), chapter navigation (PageUp/PageDown), display-mode
  cycle (I), aspect cycle (O); all rebindable in Media → Settings
- **New settings** — `media.display-mode`, `media.aspect-ratio`, `media.video-*`,
  `media.audio-delay-ms`, `media.audio-channel`, `media.audio-output`, `media.eq-*`,
  `media.subtitle-scale`, `media.controls-hide-ms`, `media.screenshot-format/-folder`
- **Screenshot naming** — canonical `Kaiteyo_<media>_<HH-MM-SS>.<ext>` extracted into
  `MediaEngine.screenshotFileName` (pure, tested); format/folder configurable
- **Tests** — `MediaPlaylistTest` (CRUD/reorder/persist/missing items), `MediaTuningModelsTest`
  (EQ preset integrity, adjustment clamping, display modes, screenshot naming), expanded
  `MediaShortcutsTest` for the new chords

*(Code-complete; compile verification pending the build environment — per user
instruction, compile/test runs deferred. The `core` module was additionally unblocked:
missing `mutableStateOf` import in `HomeScreenUI.kt`, `maxWidth` receiver capture in
`AppearanceStudio.kt`, and Long-vs-Int hex constants in the SHA-256 K array in
`BackupSystemExt.kt` — all pre-existing WIP breakage, minimal mechanical fixes.)*

### Dead controls wired / fake code removed (audit pass — see `PRODUCT_AUDIT.md`)

- Compact bottom-nav "More" overflow button now opens a real tab menu (was a `// TODO` no-op)
- Bottom-nav tab buttons no longer double-fire (`VerticalTabButton` had two `.clickable`s)
- Tag Manager selection-mode Merge/Delete now act on the selected tags via `DeckFeaturesController`
- Plugin Manager Refresh re-registers built-ins; plugin config Apply actually writes config
- `BackupVerifier` no longer fakes results — real pure-Kotlin SHA-256 checksums (verified against FIPS vectors)
- Dead `CompactLauncher` composable removed

*(Code-complete; compile verification pending the build environment — see `PRODUCT_AUDIT.md` §5.1.)*

Full detail: [`COMPLETED.md`](COMPLETED.md) and the root [`CHANGELOG.md`](../../CHANGELOG.md).

---

## DONE — unified learning ecosystem (desktop suite)

Implemented as one connected layer over the existing real SRS/deck stack —
no new fake data anywhere; every number traces to persisted events.

- **Unified learning model** (`engine/learning/LearningModels.kt`): `LearningNote`
  (content) → `CardType` (study direction) → `NoteCard` (note × type, own SRS state),
  `LearningStage` with explicit criteria (introduced/learning/established/mature),
  per-deck `DeckStudyConfig`, immutable `LearningReviewEvent` / `WritingAttemptEvent` /
  `ExamResult` / `StudySessionRecord` — all `@Serializable`
- **`LearningStore`** — single persisted source of truth (`~/.kaiteyo/learning/learning.json`),
  note dedupe by kind+expression, stable card ids so regeneration never resets SRS,
  custom-note protection (source data never overwrites user notes)
- **`StudyEngine`** — queue from real DB state (new + due, per-deck limits, interleave),
  grading through the shared `SrsScheduler` with full-fidelity review events,
  suspend/bury/forget/reschedule, resumable sessions
- **`StatisticsRepository`** — the single statistics pipeline: period stats, streaks,
  JLPT stage coverage, character progress, writing stats/weakest-kanji, card history,
  exam analytics, study-vs-exam gap, due forecast, mistakes snapshot, goals — all from
  events, never from current card state alone
- **`ExamEngine`** + **Exams workspace** — real question generation from decks/JLPT/
  mistakes/weekly activity with real distractors, typed + multiple-choice + multi-select,
  scoring, persistence, per-question analytics, recognition-vs-production gap
- **`MistakeEngine`** — queue built from actual Again events, failed writing attempts,
  wrong exam answers and lapsed cards
- **`ImportExportEngine`** — JSON snapshot (full fidelity) + CSV/TSV import/export with
  dedupe and validation
- **Wiring**: `AppState.learning` bridges the legacy card pool on launch (`syncFromLegacy`),
  `rateCurrent`/`rateWriting` feed the event stream, StatsView gained a Learning Overview
  section, Exams reachable from the dock + command palette

### Deck settings UI (per-deck study config)

- **`DeckStudySettingsDialog`** (LibraryView deck menu → "Study settings…"): edits the
  persisted `DeckStudyConfig` — new/review daily limits, learning steps (comma list),
  graduating / easy / maximum intervals, bury-related toggles, suspend-on-lapse, and an
  enabled-card-types chip selector (empty = per-kind defaults)
- Saving goes through `LearningEngine.saveDeckStudyConfig` → `LearningStore.setDeckConfig`
  (persisted to `learning.json`); `StudyEngine.buildQueue` already consumes these limits

### Mistakes study mode

- **`MistakesView`** (new workspace, dock + palette + `open-mistakes` shortcut): real
  mistake stats, category breakdown (writing/reading/meaning/recognition/exam/lapses) and
  a worst-first queue — all derived from review events, failed writing attempts, wrong
  exam answers and lapsed cards
- **`AppState.startMistakesReview`** materializes the mistake cards as a normal review
  session, so studying mistakes updates SRS + statistics exactly like regular study
- **`MistakeEngine.asStudyQueue`** bridges the queue into the shared study flow

### Import / export UI (unified learning data)

- **TransferView "Learning data" tab**: JSON full-fidelity export (notes, cards, deck
  configs, review events, writing attempts, exam results, sessions) + CSV/TSV
  spreadsheet exports, with save-to-file (native picker), clipboard, live preview and
  per-format counts
- Import merges (never deletes): JSON restores full fidelity; CSV/TSV add notes and
  auto-generate default cards (`ensureCards`); the legacy card pool is re-synced so every
  view sees the imported content immediately

### Engine tests (`desktopApp/src/jvmTest/.../engine/learning/`)

- **`LearningStoreTest`** — note dedupe, custom-note protection, stable card ids across
  regeneration, suspend persistence, deck-config defaults, full persistence round-trip
  (notes/cards/events/exams/config survive reload), `toDesktopCard` bridge, delete cascade
- **`StudyEngineTest`** — queue building (limits, due inclusion, suspend/bury exclusion,
  mode filter), grading through `SrsScheduler` (new→learning, Again lapses, Easy interval
  growth, running accuracy), resumable sessions, `deckTotals` stage counts
- **`ExamEngineTest`** — generation from real notes, empty-pool → null, real distractors,
  deck scope, multiple-choice + typed (normalized) evaluation, skipped-questions,
  exam→SRS event feed, weekly assessment (studied-only content, null when nothing studied)
- **`StatisticsRepositoryTest`** — period stats from events, streaks (incl. break), JLPT
  stage coverage, character progress, weakest-kanji from failed writing, mistake snapshot,
  study-vs-exam gap, due forecast bucketing, `dueToday`, goals, exam aggregates

### Stroke evaluator (`engine/stroke_evaluator/StrokeEvaluator.kt`)

- **Real per-stroke evaluation**: canonical stroke sequences (0..100 grid polylines) for
  20+ common kanji with correct stroke order — shape deviation, direction error and order
  quality are scored per stroke with explicit tolerances (22 grid units / 45°)
- Greedy nearest-reference matching, arc-length resampling to 12 points, aspect-ratio
  normalization — works when the learner draws in a different order or merges strokes
- Characters without canonical data report `supported = false` and record a plain attempt
  instead of fabricating strokes
- **Wired into writing practice**: the canvas captures strokes, `WritingCanvasState`
  normalizes them, and `AppState.rateWriting` → `LearningEngine.recordEvaluatedWriting`
  stores per-stroke `StrokeAttempt`s with real accuracy; the view shows a per-stroke
  analysis card (correct / shape / direction / both) after reveal

### KanjiVG writing evaluation wired into the shipped app (`WritingEvaluator.kt`)

- **`WritingEvaluator` facade** routes writing evaluation through the canonical KanjiVG
  stack when a licensed dataset directory is present (`~/.kaiteyo/kanjivg/` or
  `~/kanjivg/` with `kanji/*.svg` or `kanjivg.xml`): the platform database is built from
  the installed dictionaries (`PlatformBuilder.fromRepository`) with real SVG geometry
  and attempts are evaluated through `KanjiWritingSession` + `StrokeEvaluationBridge`
  (shape/direction/order, `WritingStrictness.Normal`)
- **Honest fallback**: without a KanjiVG directory the built-in common-kanji dataset is
  used with the same result shape; every result carries a `sourceLabel` ("kanjivg" /
  "builtin") that the writing UI shows as a badge, plus an "install KanjiVG for full
  coverage" hint when the fallback is active — the app never pretends KanjiVG geometry
  exists when it does not
- `AppState.writingEvaluator` (lazy), `pointsToSvgPath` canvas→SVG conversion,
  `WritingEvaluatorTest` (7 tests: source selection, correct/incorrect strokes,
  unsupported honesty, SVG conversion)

### Per-deck review launch grid (Review view)

- The "Unified study" card lists every non-archived deck with real new/learning/due
  counts from the unified store; clicking a row starts that deck's unified review
  (`startUnifiedDeckReview`) or shows an honest "nothing due" toast

### Writing statistics history (Statistics view)

- **Writing History card**: recent `WritingAttemptEvent`s with per-stroke correct counts
  ("3/4 strokes") vs self-graded entries, accuracy bars, and a per-character accuracy
  trend strip (last 14 attempts, color-coded) — all from the real writing-event stream
- `LearningEngine.recentWritingAttempts` / `writingAccuracyTrend` facade methods

### Unified review (Review screen sources the learning store)

- **`AppState.startUnifiedReview` / `startUnifiedDeckReview`** — builds the queue from
  `StudyEngine.buildQueue` (due + new, per-deck limits), materializes cards into the
  legacy `ReviewSession` UI, tracks a `StudySessionRecord`
- **Grading routes through `StudyEngine.grade`** (full-fidelity review events) when the
  unified source is active; suspend/forget sync to the unified store; session completion
  persists the study-session record; the launch panel gained a "Unified study" card with
  real due/new/recorded-review counts

### JLPT coverage + forecast on the Dashboard

- **JLPT coverage card** — per-level N5→N1 stage-based bars (introduced/learning/unseen,
  due) from `StatisticsRepository.jlptCoverage` — real SRS stages, labeled as estimated
  study coverage, never a certification claim
- **Due forecast card** — 14-day expected workload from actual due dates
  (`StatisticsRepository.forecast`)

### Statistics: Exam analytics + card history

- **Exam Analytics card** — study-vs-exam gap (recognition/production/writing), accuracy
  by exam type, score-trend chart and recent exam history — all from persisted `ExamResult`s
- **Card History card** — per-card review timeline (date, rating, card type, interval
  before→after, correct/miss) from the immutable review-event stream with a card picker

### Library: unified search + deck stats

- **`LearningEngine.search`** — one indexed search over notes+cards with scoring
  (exact > prefix > substring > reading > meaning), kind/JLPT/tag/state filters and real
  stage/due per result
- **Unified search section** in the Library — kind chips + JLPT chips + results with
  stage/due badges; clicking opens the card
- **Unified deck stats section** — per-deck new/learning/review/due/suspended counts
  straight from the learning store's card state

### Ghost UI pass — fake "Export active theme JSON" command fixed

- **Command palette "Export active theme JSON" was a fake action** — it showed a
  success toast ("Theme JSON copied to clipboard") without ever touching the system
  clipboard. The command now serializes the active theme via
  `ThemeManager.exportJson(activeTheme.id)` and writes the pretty-printed JSON to the
  system clipboard before showing the toast. No other ghost controls found in the
  sweep: the only inert buttons in the codebase are the Theme Studio's live preview
  gallery (a design-system showcase, intentionally non-interactive) and providers
  honestly labeled "Coming soon" in Account settings.

### Mobile/core MPP ghost-UI pass — Backup Manager "Start Restore" fixed

- **`BackupManagerScreen` Restore tab was a fully fake screen (reachable from
  Library → Manage → Backup)** — "Start Restore" had an empty `onClick`, the
  Restore-Options checkboxes did nothing, and the copy claimed "your current data
  will be backed up automatically" (false). The tab now lists the real backups with
  radio selection, "Start Restore" is enabled only when one is selected and calls the
  same `onRestoreBackup` path as the per-item Restore menu, the dead checkboxes are
  gone, and the copy + empty state are honest.
- **Backup systems unified — the deck-features Backup Manager now drives the real
  engine** (replaces the earlier metadata-only gap):
  - **Create + Restore** in `BackupManagerScreen` (header action, Backups tab,
    per-item menu, Restore tab) now launch the real file-based backup flow
    (`MainDestination.Backup` → platform file picker + `BackupManager`, which zips
    the real database + preferences and version-checks restores).
  - **Real metadata**: the JVM/Android/iOS backup ViewModels record actual
    filename + size (from `javaFile`, the picked URI via `ContentResolver`, or the
    tmp file via `SystemFileSystem`) into `CardDatabaseManager` after a successful
    `backupTo`, so the manager's history list shows real backups.
  - **Fake paths removed**: `DeckFeaturesController.recordBackup` (size-0,
    checksum-"" records), `recordRestore` and `recordVerify` (log-only) are gone;
    the fake "Verify" menu item is removed (a metadata row has no file to verify);
    the Settings/Schedule tabs are honestly labeled as stored-but-not-active
    (automatic scheduling ships later).
  - Remaining: verify per-platform behavior at runtime (JVM + Android sweeps;
    iOS build).
- **Dead-code cleanup backlog — RESOLVED.** Every decks-folder screen was verified
  against the live routes (`features/DeckFeatureScreens.kt` + `MainNavigation.kt`):
  `CardBrowserFull`, `DeckBrowserFull`, `BulkActionsScreen`, `HistoryTrackerScreen`,
  `NoteEditorScreen`, `AnkiOpsFull`, `AnkiCardOperations`, `CardManager`, `TagManager`,
  `TagManagerScreen`, `FlagManagerScreen`, `SearchEngineImpl`, `ImportExportSystem`,
  `KeyboardShortcutsPage`, `ReviewShortcutsSettings`, `PluginSystem`, `TagDialogs`,
  `TagFlagDialogs`, `CardEnhancements` are all **live** (the earlier guess that
  CardBrowserFull/DeckBrowserFull/BulkActions/History/NoteEditor/Anki* were dead was
  wrong — CardBrowserRoute/DeckBrowserRoute/BulkActionsRoute/HistoryRoute/
  NoteEditorRoute/AnkiOperationsRoute compose them). The genuinely unreferenced files
  were deleted — see the Dead-code deletion entry below. Remaining known dead-but-
  harmless code: `generateMockCards/Decks/Tags/HeatmapData` in
  `LearningPowerDataModels.kt` (unreachable; the file itself is live via
  `HistoryEntryType`).

### Dead-code deletion + mock-history fix (core MPP, source-only)

- **Deleted 12 unreferenced files** from `screen/decks/`: the entire achievement
  subsystem (`AchievementSystem.kt`, `AchievementManager.kt`, `AchievementChecks.kt`,
  `AchievementDefs_*.kt` ×5 — no reachable UI, no wiring anywhere), plus
  `DeckBrowserEnhanced.kt`, `DeckManagerEnhanced.kt`, `NoteEditorEnhanced.kt` and
  `UndoSystem.kt` (`UndoManager`/`UndoableAction`/`UndoSnackbar` — nothing references
  them; `DeckFeaturesController` has its own undo stack). All public symbols were
  verified unreferenced repo-wide (including tests) before deletion.
- **Removed the dangling `DeckFeaturesHub` import** from `MainNavigation.kt` — the
  only external reference to the deleted `DeckManagerEnhanced.kt` (an unused import
  that would have broken the build).
- **Trimmed `DeckManager.kt`** to the live model surface only (`KaiteyoDeck` +
  `DeckFilters`, both consumed by the deck routes): removed the unreferenced
  mock-data `DeckManager()` composable (fake decks, one empty `.clickable {}`) and
  the dead `BulkOperation`/`BulkAction`/`SmartDeckRule`/`SmartDeckDefinition` models.
- **Fixed a live mock-data leak in `HistoryFullScreen`** (reachable via Library →
  Manage → History): when the real history was empty it fabricated **50 fake entries**
  with random timestamps/descriptions (`generateMockHistory()` fallback). The
  fallback is gone; an empty history now renders the real empty state.

### BLOCKED — unified learning ecosystem (needs runtime verification)

| Item | What's needed |
|---|---|
| Exams workspace end-to-end (generate → take → score → analytics) | Desktop runtime sweep |
| LearningStore persistence round-trip across restarts | Desktop runtime sweep |
| Legacy→unified bridge with an existing populated card pool | Desktop runtime sweep |
| Statistics Learning Overview numbers vs legacy engines | Desktop runtime sweep |
| Deck study-settings dialog save → queue limit behavior | Desktop runtime sweep |
| Mistakes workspace + mistakes review session | Desktop runtime sweep |
| Learning data export/import (JSON/CSV/TSV) round-trip | Desktop runtime sweep |
| Engine tests (4 files) pass under `:desktopApp:jvmTest` | Gradle test run (per user instruction, compile/test runs deferred) |
| Stroke evaluator scoring quality against real handwriting | Desktop runtime sweep (needs a pen/mouse) |
| KanjiVG path (real geometry when a dataset directory is present) | Desktop runtime sweep + dataset |
| Unified review end-to-end (queue → grade → session record) | Desktop runtime sweep |
| Per-deck review launch grid + deck-scoped unified sessions | Desktop runtime sweep |
| Writing History card (stroke counts, per-character trends) | Desktop runtime sweep |
| JLPT coverage / forecast numbers on Dashboard vs legacy engines | Desktop runtime sweep |
| Exam analytics + card history drill-down | Desktop runtime sweep |
| Unified library search + deck stats | Desktop runtime sweep |

### 🔵 P3 — Knowledge System Overhaul (August 2026)

| # | Item | Category | Notes |
|---|------|----------|-------|
| 1 | Word card models (WordCardModels.kt) — modular word pages | DONE | Created with 9 card types, 5 presets, persistence | 
| 2 | Word card layout store (wordCardLayoutJson preference) | DONE | Added to PreferencesContract + AppPreferences + all test fakes |
| 3 | Word card models test (WordCardModelsTest.kt) | DONE | Tests for layout ops, persistence, presets |
| 4 | Search category filter chips | DONE | Added to UniversalSearch overlay |
| 5 | Browse hub frequency/JLPT distribution | DONE | Added FrequencyDistributionSection + JlptBreakdownSection |
| 6 | Browse hub grade distribution | DONE | Added to BrowseHubContract + ViewModel |
| 7 | Media centre foundation rebuilt | DONE | Proper workspace overview with feature cards |
| 8 | Performance utilities (Debouncer, LazyPager, caches) | DONE | Created PerformanceUtils.kt |
| 9 | Accessibility utilities (focus, keyboard, touch) | DONE | Created Accessibility.kt |
| 10 | Architecture documentation | DONE | OVERVIEW.md, KNOWLEDGE_SYSTEM.md, NAVIGATION.md, THEMES.md |
| 11 | Roadmap document | DONE | docs/planning/ROADMAP.md |
| 12 | Media centre crash on non-desktop platforms | KNOWN ISSUE | Placeholder screen shows honest "desktop only" message — no crash |
| 13 | OCR/voice/handwriting search | FEATURE | Roadmap item — requires ML libraries |
| 14 | Card customization drag-reorder UI | FEATURE | Backend complete, needs dedicated settings screen |
| 15 | Course system drill-down UI | FEATURE | Catalog exists, needs course detail screen |
| 16 | Advanced search (wildcard, fuzzy, saved) | FEATURE | Core search complete, enhancements on roadmap |
| 17 | Statistics enhancement (heatmap, charts) | FEATURE | Basic stats complete, needs depth |
| 18 | Import/export expansion (CSV, Anki) | FEATURE | Basic import exists, expansion on roadmap |
| 19 | Game foundation | FEATURE | Not started — blocked on core completion |
| 20 | Search performance profiling | FEATURE | Foundation complete, needs profiling across 13k+ kanji |
| 21 | Accessibility testing across platforms | FEATURE | Foundation complete, needs systematic testing |
| 22 | Cross-platform resize/mode-switch testing | FEATURE | Needs runtime sweep |

---

## How to use this tracker

1. **New bug** → add a row under BUG with priority; verify against
   `../troubleshooting/` first.
2. **Fix verified** → move the row to DONE (brief note), add to `COMPLETED.md` +
   `CHANGELOG.md`.
3. **Code-complete but unverified** → list under BLOCKED with what's needed.
4. **Won't fix / deferred** → move to `FUTURE_IDEAS.md` with a reason.
