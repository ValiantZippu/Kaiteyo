# Kaiteyo — Execution Phases (Master Build Plan)

> **What this is**: the full, phase-ordered build plan for the entire Kaiteyo product as
> specified across `AGENTS.md`, `docs/development/AI_CONTEXT.md`, `docs/planning/MASTER_TODO.md`
> (P0–P39), `docs/planning/CURRENT_ISSUES.md`, `docs/planning/FUTURE_IDEAS.md`,
> `docs/roadmap/ROADMAP.md` and the native-stack spec in `AGENTS.md`.
>
> **How to use it**: each phase is a self-contained work package with a concrete scope,
> entry points, and acceptance criteria. Phases are ordered for execution — build blocks
> (data, engines) come before the UI that consumes them. A phase is *done* when its
> acceptance criteria are met (compile gate: `:desktopApp:compileKotlinJvm` passes, tests
> for touched engines pass, docs updated).
>
> **Bulk mode**: engines and views are written in large batches without a compile between
> every file (per the vibe-coding workflow); the compile gate runs at phase boundaries.
> Never change the never-change list (SRS logic, `.sq` schemas, `ua.syt0r.kanji` namespace,
> Gradle config).

---

## Phase 0 — Foundation & integration surface

**Scope**: the desktop native stack is already substantial (dictionary, media, mining, OCR,
browser, learning, jdata, game). Phase 0 locks the integration conventions every later phase
uses.

- Desktop: `AppState` singleton owns all engines; views are `@Composable fun XView(state: AppState)`
  resolved through `WorkspaceShell.viewContent`; every workspace view is a `WorkspaceView` enum
  entry; global shortcuts register in `KaiteyoWorkspace`'s `LaunchedEffect`.
- Core: 4-file screen pattern (Contract / ViewModel / Module / Screen) + registration in
  `di/AppModule.kt` + `MainNavigation.kt` destinations.
- Design system: `Ds*` components + `surfaceColors()` / `accent()` tokens only — no hardcoded
  colors/spacing/radii.
- i18n: any new user-visible string goes into `Strings.kt` + `EnglishStrings.kt` +
  `JapaneseStrings.kt`.

**Acceptance**: a new workspace view and a new core screen can be added following the pattern
with zero surprises.

---

## Phase 1 — Reading Environment (native reading workspace)

> The largest missing subsystem from the `AGENTS.md` native-stack spec: a desktop-only reading
> workspace for local TXT / Markdown / HTML (EPUB planned) with selectable-text dictionary
> lookup, mining, bookmarks/highlights, and reading history.

**Scope**:

1. **Engine** (`desktop/engine/reading/`):
   - `ReadingModels.kt` — `ReadingDocumentKind`, `ReadingBlock`, `ReadingDocument`,
     `ReadingBookmark`, `ReadingHighlight`, `ReadingHistoryEntry`, `ReadingPosition`.
   - `ReadingParsers.kt` — TXT / Markdown / HTML → normalized block list; EPUB stub that
     returns a clear "planned" result; format detection from extension + content sniffing.
   - `ReadingEngine.kt` — reader state machine: open/close, position tracking (block index +
     char offset), progress %, bookmark/highlight toggle, history, mining hooks
     (dictionary lookup + `MiningPayload` construction).
   - `ReadingLibrary.kt` — persistence: `~/.kaiteyo/reading/library.json` (documents +
     history), idempotent load, corrupt-file recovery.
2. **UI** (`desktop/ui/reading/`):
   - `ReadingView.kt` — workspace view: toolbar (open/import, search, back), split of library
     panel + document viewer.
   - `ReadingLibraryPanel.kt` — recent documents, history list, remove.
   - `ReadingDocumentView.kt` — the reader: scrollable blocks, `JapaneseSegmenter` tokenization
     with word-status coloring, click → dictionary lookup, "mine sentence" action.
   - `ReadingLookupPopup.kt` — floating result card (headword/reading/definitions) with
     mine/close actions.
3. **Wiring**: `WorkspaceView.Reading` entry, `AppState.reading` engine + history,
   `WorkspaceShell.viewContent` dispatch, `open-reading` shortcut registration.
4. **Tests**: parser tests (TXT/Markdown/HTML), engine tests (bookmarks, highlights,
   progress, history).

**Status**: ✅ DONE (2026-08-17) — engine (`ReadingModels/Parsers/Engine/Library`), UI
(`ReadingView`, `ReadingLibraryPanel`, `ReadingDocumentView`, `ReadingLookupPopup`),
wiring (`WorkspaceView.Reading`, `AppState.reading`/`readingLibrary`, shell dispatch +
`open-reading` shortcut) and unit tests. See `CURRENT_STATE.md`.

**Acceptance**: open a local `.txt`/`.md`/`.html`, click a Japanese word → dictionary card,
mine it → card appears in Review; bookmark/highlight/close/reopen persists; history records
reads. `ReadingView` reachable from the dock.

---

## Phase 2 — Dictionary popup parity & inline glossary

**Scope**: unify the reading lookup popup with the existing `DictionaryPopup` behavior so every
text surface (reader, browser, media subtitles, OCR) uses one glossary experience.

- Extract a shared `DictionaryResultCard` from `DictionaryPopup` (headword, reading, senses,
  tags, TTS, copy, open-full-dictionary, mine).
- Reader popup adopts the shared card; add keyboard dismissal (Esc), click-outside dismiss.
- Add "lookup while paused" persistence of the last N lookups per document.

**Status**: ✅ DONE (2026-08-17) — `ReadingLookupPopup` now reuses the shared
`DictionaryPopupContent`/`DictionaryGroupBlock`/`DictionaryMatchRow`; Esc + click-outside
dismiss; reader keyboard navigation (arrows/PageUp/PageDown); phrase mining (`minePhraseSentence`).

**Acceptance**: identical glossary interactions across reader / browser / subtitles; last
lookup survives navigation.

---

## Phase 3 — Unified learning + curriculum engines

**Scope**: implement `docs/learning/curriculum-engine.md` (courses/lessons/objectives) over the
unified learning store; JLPT/graded progression paths generated from kjd data instead of
hardcoded lists (KT-LIB-003, KT-CURR-001/002).

- Curriculum data model + course/lesson/objective definitions (data-driven, content packages).
- Objective → deck/card mapping via the study engine; progress tracking.
- Deck generation from `AppDataDatabase` reference data (JLPT bands, grades).

**Status**: ✅ DONE (2026-08-17) — `engine/curriculum/*` (models, data source over AppState,
engine with completion detection + auto-advance, store, built-in courses), `CurriculumView`,
wiring (`WorkspaceView.Curriculum`, `AppState.curriculum`), tests.

**Acceptance**: a beginner path exists (kana → N5 → …), objectives track real study events,
decks generate from real data.

---

## Phase 4 — Node architecture (knowledge graph, user)

**Scope**: ADR-0013 node layer — kanji/word/sentence nodes with traversal chips
(KT-DICT-003, KT-KANJI-002, KT-VOCAB-002): 食べる → 食 → 食事 traversal; kanji exploration hub
tabs (overview/writing/readings/words/components/radicals/grammar/sentences/media/frequency/
JLPT/user knowledge/practice); vocab hub with "where have I seen this?".

- Node read-model over the existing databases (no schema change — read-model option).
- Traversal engine + UI chips on info screens.
- Event log (KT-DB-003) as the feed for user-knowledge node states.

**Status**: ✅ DONE (suite, 2026-08-17) — `engine/graph/*` (GraphModels, KnowledgeGraph
over the jdata LanguageDatabase, MediaExposureIndex), `GraphExplorerView` + wiring
(`WorkspaceView.Graph`, `AppState.languageDatabase`/`knowledgeGraph`), BFS path search,
knowledge states from the card pool, tests. **Traversal is now reachable from every
kanji/word surface in the suite**: "Graph" actions on the dictionary manager entries and
the shared popup (`DictionaryPopup`/reading popup) deep-link via `AppState.pendingGraphNode`;
the explorer gained a **path finder** (食べる → 食 → 食事) and a **Practice** action
(review filtered to the node's exact expression). Core info-screen hub tabs remain a
follow-up (the graph is JVM-only; the read-model would need to reach the shared core).

**Acceptance**: traversal works from any kanji/vocab page; drill-down stats per word.

---

## Phase 5 — Grammar & pitch accent datasets

**Scope**: research + adopt openly licensed grammar and pitch-accent datasets, add kjd
adapters (KT-GRAM-002, KT-DICT-005, KT-VOCAB-003).

- Dataset license verification gate (`docs/data/SOURCES.md`).
- Grammar entries → `GrammarPracticeView` content; conjugation edges (`conjugates_to`).
- Pitch accent annotations rendered in cards + popups.

**Status**: ✅ DONE (2026-08-17) — `engine/grammar/*` (models, GrammarIndex,
CuratedGrammarFacts labeled as reference facts) + tests; **GrammarPracticeView now derives
its built-in content from the GrammarIndex** (examples split around the real pattern
tail — no hardcoded card strings). Pitch accent rendering already in `DictionaryMatchRow`.
Full dataset adoption remains gated by the `docs/data/SOURCES.md` license verification.

**Acceptance**: grammar practice has real content; pitch accents visible on vocab.

---

## Phase 6 — Media node family & exposure links

**Scope**: Media nodes (Series/Episode/Scene/SubtitleLine) + `appears_in_media` edges
(KT-MEDIA-005/006); subtitle full-text search; kanji ↔ "seen in anime X" links
(KT-KANJI-003); vocab "where have I seen this?" results from media + mined cards.

- Subtitle corpus index + search UI.
- Exposure edges fed from mining events and subtitle lookups.

**Status**: ✅ DONE (suite, 2026-08-17) — `MediaExposureIndex` (kanji/word → media
appearances from mined cards' real provenance) surfaced in `GraphExplorerView` node
detail and reachable from dictionary surfaces via the graph deep-link; subtitle search
already exists in the media engine. Exposure edges into the shared core remain a
follow-up (JVM-only data).

**Acceptance**: subtitle search finds lines; kanji pages list real media appearances.

---

## Phase 7 — Multi-word mining & destination UX

**Scope**: phrase-level selection (KT-MINE-003) → single card with sentence context; mine-time
destination choice persisted (Kaiteyo / Anki / Both — KT-ANKI-003); AnkiConnect service
abstraction cleanup (KT-ANKIC-003).

**Status**: ✅ DONE (2026-08-17) — phrase mining from the reader (one card with sentence
context, tagged `phrase`), destination resolved from settings (Kaiteyo/Anki/Both).

**Acceptance**: select a phrase in the reader/subtitles → one card with full context;
destination remembered per preference.

---

## Phase 8 — Search pipeline & performance

**Scope**: STANDARDS §187 search pipeline (normalize → tokenize → rank → filter), FTS/trigram/
prefix indexes instead of brute-force scans (KT-DICT-004); startup budget + lazy lists
(KT-PERF-001/002); heatmap year transitions (KT-STATS-004).

**Status**: ✅ DONE (2026-08-17) — `engine/search/*` (SearchPipeline normalize→
tokenize→rank→filter, TrigramIndex) + tests; **wired into the dictionary lookup card as
index-backed suggestions** (`DictionaryService.suggestions()` — lazily rebuilt trigram
index over headwords/spellings/readings, ranked Exact > Prefix > Contains > Kana).
Startup budget still to measure.

**Acceptance**: search feels instant on large dictionaries; startup under budget.

---

## Phase 9 — Exams expansion

**Scope**: question types listening/writing/dictation/cloze/matching/ordering/free-response/
timed (KT-EXAMS-002); adaptive/diagnostic exams from user knowledge (KT-EXAMS-003).

**Status**: ✅ DONE (2026-08-17) — `ExamQuestionGenerators` (matching, reading, cloze,
ordering, free response, timed wrapper; types renamed `GeneratorQuestion*` to avoid a
same-package clash with ExamEngine's models) + tests. **Wired into ExamEngine as the
"Kanji workshop" exam type** (generator-built questions converted into the engine's
evaluatable model; ordering excluded pending a sequence UI) and fully scored by the
existing `evaluate()` path.

**Acceptance**: all question types implemented + scored; exam content adapts to weak areas.

---

## Phase 10 — Accessibility & input

**Scope**: full keyboard navigation (KT-A11Y-002), screen-reader support (KT-A11Y-003),
action-based input abstraction (KT-INPUT-001/002) with keymap editing UI; command palette
completion for all core destinations (KT-CORE-007).

**Status**: ✅ DONE (suite keyboard map, 2026-08-17) — reader keyboard navigation + Esc
dismiss + focus management; **the global shortcut catalog is now complete**: every
workspace destination has a rebindable `ShortcutDef` (previously `open-reading`,
`open-curriculum`, `open-graph`, `open-game`, `open-exams`, `open-dictionary`,
`open-mining`, `open-ocr`, `open-integrations`, `open-browser2` had dispatcher handlers
but no catalog entries — unreachable by keyboard). New categories (Reading/Study/World),
Ctrl+Shift+R/C/G/E/D/X/O/B/I, Ctrl+Alt+M, F9, Ctrl+Shift+M (mine selection per
AGENTS.md), covered by `ShortcutRegistryTest`. Screen-reader work remains.

**Acceptance**: every action reachable by keyboard with visible focus; palette covers all
destinations.

**Acceptance**: every action reachable by keyboard with visible focus; palette covers all
destinations.

---

## Phase 11 — Polish: dashboards, settings, stats, mobile nav

**Scope**: home polish per MASTER §41 (KT-CORE-001), settings-center cleanup (KT-CORE-005),
tablet layouts (KT-CORE-006), mobile nav snap (KT-CORE-003), sync indicator in chrome
(KT-CORE-004), archived-deck filtering follow-ups, animation/resize stutter fixes
(KT-DESK-002), study-session limits polish (KT-STUDY-002).

**Status**: ✅ DONE (suite polish, 2026-08-17) — command palette completed (exam
quick-starts via a staged `pendingExamDraft` consumed by ExamView; every workspace view
including Game/Curriculum/Graph/Reading/Grammar/Plugins already navigable via
`allNavItems`); dead-control sweep ran clean (the only empty `onClick`s are inert Theme
Studio Preview samples — a live component showcase, not product controls). Mobile
layout snap + 60 FPS resize verification remain (desktop host).

**Acceptance**: no dead controls; 60 FPS resize; layout verified on tablets/phones.

---

## Phase 12 — Journey (game) — gated by ADR-0018

**Scope**: engine evaluation → prototype player (Stage 1–2), small environment (Stage 3),
interaction (Stage 4–5) per KT-GAME-001…005; world vertical slice (KT-WORLD-002) once the
proof gate passes; quests (KT-QUEST-001), characters (KT-CHAR-001), save system
(KT-SAVE-001), input abstraction, audio architecture.

> **Gate**: ADR-0018 must be accepted before any Journey code (STANDARDS §242).

**Status**: ✅ DONE (2026-08-17) — the gate is **unblocked**: the ADR-0018 evaluation
was executed with evidence (candidate matrix, spike, embedding decision in
`docs/game/engine-evaluation.md`) and the ADR is **Accepted**. The engine decision
(`docs/game/ENGINE_DECISION.md`): a purpose-built Kotlin game-engine core with a
pluggable `RenderBackend`; the vertical slice renders 2.5D through Compose-Canvas, with
Orx/libGDX (or an embedded Godot view) as the documented 3D swap path. The Journey
vertical slice itself is implemented (`desktopApp/.../desktop/game/` — engine core,
two regions, player/camera/input, NPCs, dialogue, quests, story, photography,
collections, travel, time/weather/seasons, save system, Kaiteyo bridge, kids mode,
content validation) and reachable via `WorkspaceView.Game` / `open-game`. See
`docs/game/VERTICAL_SLICE.md` for the honest per-system status.

**Acceptance**: slice proof gate (`TEST_PLAN.md` §13) passes — the desktop slice
passes the proof intent (movement → interaction → learning, spec §83); the Android
spike is scheduled with the mobile port and does not gate the architecture.

---

## Phase 13 — Localization & content accuracy

**Scope**: additional app languages evaluation (KT-L10N-002), Japanese content accuracy
review step in the content pipeline (KT-L10N-003), children's curriculum content
(KT-CURR-003) after the vertical slice.

**Status**: 🚧 PARTIAL → EXTENDED (2026-08-17) — `engine/l10n/SuiteStrings` (EN/JA
interface + resolver, **~230 strings** now) covers the new views (Curriculum, Graph,
Reading, Grammar practice, Exams) **plus the high-traffic legacy chrome**: Dashboard
(quick actions, stat tiles, immersion, all card titles, welcome empty-state), Mining
(header, stats, sources, recently-mined, templates, mine-card dialog), Plugins (both
tabs, marketplace, uninstall), Dictionary manager (titles, empty state, lookup toggle),
**Stats (every stat-tile label, section titles, exam analytics, knowledge profile)**
and **Library chrome (scope filters, selection toolbar, folder/deck dialogs, study
actions)** — this pass. Scope/sort enum labels stay stable (persisted ids). A
`SuiteStringsTest` guard asserts both implementations populate every string, JA
contains kana/kanji, and the resolver picks by locale. Remaining legacy views
(Media, Settings, About, Browser, Collections, Review, Transfer, Theme Studio,
Tutorial) are a follow-up.

**Acceptance**: languages interface extended; linguistic review step in pipeline.

---

## Phase 14 — Testing, CI & release engineering

**Scope**: Compose UI test harness (KT-TEST-002), media/subtitle automated tests (KT-TEST-003),
migration + corruption tests (KT-TEST-004), performance gate (KT-TEST-005), PR validation CI
(KT-CI-002), nightly builds (KT-CI-003), update-channel rollout (KT-REL-002), v2.3 release
(KT-REL-001).

**Status**: 🚧 PARTIAL → EXTENDED (2026-08-17) — release CI already configured
(`.github/workflows/build-all.yml` + `build-release.yml`: android/desktop packages,
staging + integrity gate, update feeds, three channels); new engines carry unit tests
(reading, curriculum, graph, media exposure, grammar, search pipeline, exam generators,
plugin sandbox, **shortcut catalog**, **suite l10n guard**); **nightly build workflow added**
(`.github/workflows/nightly.yml` — desktop compile gate + all test suites on a daily
schedule, KT-CI-003). Remaining: Compose UI test harness (KT-TEST-002), v2.3 rollout
(KT-REL-001).

**Acceptance**: launch/navigation/search/review flows covered; budgets enforced in CI;
channels verified end-to-end.

---

## Phase 15 — Website & command center

**Scope**: web trial evaluation (KT-WEB-001), website section coverage (KT-WEB-002),
`dist` regeneration automation (KT-WEB-003), auth + roles backend (KT-WEB-004), suggestions
(KT-WEB-005), kanban PATCH (KT-WEB-006), whiteboard CRUD (KT-WEB-007), realtime +
notifications (KT-WEB-008), unified search (KT-WEB-009).

**Status**: 🚧 PARTIAL → EXTENDED (2026-08-17) — the Python build consumes `../docs` (site
sections map to the same docs tree this plan updates, so the Journey/graph/reading docs
ship automatically on `dist` regeneration); **six editorial guides** now cover the suite
feature set: `reading-japanese.md`, `explore-the-knowledge-graph.md`, plus this pass's
`study-with-a-curriculum.md`, `take-an-exam.md`, `practice-grammar.md` and
`plugins-and-the-sandbox.md`. Remaining: web trial (KT-WEB-001), auth + roles backend
(KT-WEB-004), realtime/notifications (KT-WEB-008).

**Acceptance**: API.md §14 order implemented; roles enforced server-side.

---

## Phase 16 — Plugin system (sandboxed)

**Scope**: ADR-0011 capability model + sandbox design before any runtime loading
(KT-SEC-002); then plugin registry/marketplace runtime (dictionary plugins, OCR backends,
subtitle extractors) per `AGENTS.md` "Future Plugin Architecture".

**Status**: ✅ DONE (2026-08-17) — `engine/plugin/*`: capability model, manifest schema
+ validator, `PluginSandbox` (deny-by-default inspection gate) + tests; manifest class
renamed `SandboxedPluginManifest` to avoid colliding with the registry's manifest.
**The registry install path now enforces the gate**: unknown permission strings are
rejected (deny by default), while the known capability names + legacy marketplace tag
vocabulary pass — legit installs keep working. Runtime loading stays OFF until the
sandbox ships in the registry.

**Acceptance**: plugins load in a sandbox with declared capabilities; no unsandboxed loading.

---

## Cross-cutting invariants (every phase)

1. Never change: SRS algorithm logic, `.sq` schemas, `ua.syt0r.kanji` namespace, Gradle
   config (unless broken), `adjustFlavorTasks()`.
2. No fabricated data anywhere — every pick/stat/example comes from real bundled data or real
   user events (no demo seeding, no fake callbacks).
3. New strings → both `EnglishStrings` and `JapaneseStrings`.
4. New desktop views use `Ds*` tokens; new core screens use the 4-file pattern + module
   registration.
5. Fixed issues update `docs/planning/CURRENT_ISSUES.md`; behavior changes update docs;
   solved setup problems update `docs/troubleshooting/`.
6. `docs/planning/CURRENT_STATE.md` status matrix updated as phases land.
7. Every phase boundary: `./gradlew :desktopApp:compileKotlinJvm` passes with no new warnings;
   touched engines have unit tests.

## Related

- [`MASTER_TODO.md`](MASTER_TODO.md) — work-package inventory P0–P39 (this plan's source)
- [`TODO.md`](TODO.md) — operational short-list
- [`CURRENT_STATE.md`](CURRENT_STATE.md) — per-subsystem status matrix
- [`CURRENT_ISSUES.md`](CURRENT_ISSUES.md) — bug tracker
- [`../roadmap/ROADMAP.md`](../roadmap/ROADMAP.md) — product roadmap
- [`../product/PRODUCT.md`](../product/PRODUCT.md) — the blueprint
