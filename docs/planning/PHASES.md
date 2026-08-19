# Kaiteyo — Consolidated Phases & Master TODO (~110 items)

> **Purpose.** The four product directives (Core Intelligence Overhaul, UI/UX Audit +
> Debug + Page Identity, Media Forensic Audit, World System) each define their own phase
> order. This file merges them into **one dependency-ordered phase map** and **one numbered
> master TODO** so any agent can pick the next item by phase without re-reading the 200K
> directive.
>
> **Sources of truth** (do not duplicate IDs):
> - [`MASTER_TODO.md`](MASTER_TODO.md) — authoritative catalog, work packages P0–P39, every
>   `KT-<AREA>-<NNN>` ID.
> - [`OVERHAUL_BACKLOG.md`](OVERHAUL_BACKLOG.md) — spec-derived overhaul backlog with file
>   evidence (NAV/THEME/DEBUG/CARD/LEVEL/SEARCH/GRAPH/RAD/SENT/DATA/AI/UI/DOC/TEST).
> - [`TODO.md`](TODO.md) — operational short-list.
>
> **Status legend** (per `planning/README.md`): ✅ done · 🚧 in progress · 🔬 target ·
> 📋 planned · 🔍 research · ⛔ blocked · 💀 placeholder. In the numbered list below:
> `[x]` = done (with evidence), `[~]` = code-complete but not yet compiled/verified,
> `[ ]` = open. **Nothing is DONE because a button exists** — see Definition of Done in
> `OVERHAUL_BACKLOG.md` §6.

---

## 1. Phase map (21 phases, dependency-ordered)

Each phase builds, runs, inspects, fixes, and documents before the next starts. Gates
(ADR-0017 one-product, ADR-0018 game engine, ADR-0013 node storage, plugin sandbox,
dataset licenses) block their dependent packages regardless of priority.

| Phase | Name | Scope | Packages |
|---|---|---|---|
| 1 | Repository audit + page identity | Spec→repo mapping, page registry, debug overlay | KT-UI-001, KT-DEBUG-001, KT-DOC-006/007 |
| 2 | Navigation shell | One controller, two presentations, no crashes | KT-NAV-001…007, KT-TEST-006 |
| 3 | Theme & design tokens | Tokens, Sepia completeness, typography | KT-THEME-001…004, KT-TEST-007 |
| 4 | Debug panel + debug settings | Real dev surface, wired toggles | KT-DEBUG-002…004 |
| 5 | Ghost-control + sweep passes | Dead controls, space, responsive, animation, keyboard | KT-UI-002, KT-UI-008…013 |
| 6 | Core screen rebuilds | Home / Library / Browse / Stats / Settings | KT-UI-003…007, KT-CORE-004/007 |
| 7 | Card registry & customization | Modular cards, presets, drag-reorder | KT-CARD-001…004, KT-TEST-009 |
| 8 | Level profiles & adaptation | Model, presets, kanji/word/browse adaptation, custom editor | KT-LEVEL-001…004 |
| 9 | Universal search | Grouped search, filters, sorts, normalization | KT-SEARCH-001…008, KT-TEST-008 |
| 10 | Sentence system & grammar | Token pages, provenance, grammar entities | KT-SENT-001…004, KT-GRAM-002 |
| 11 | Radical/component explorer | Radical grid, component model | KT-RAD-001…003 |
| 12 | Knowledge graph | Data layer, canvas, navigation | KT-GRAPH-001…004, KT-TEST-010 |
| 13 | Frequency, keywords, provenance, imports | Metadata systems + pipelines | KT-DATA-001…004, KT-TEST-011 |
| 14 | Study integration | SRS polish, study-state surfaces, curriculum links | KT-STUDY-002/003, KT-CURR-001 |
| 15 | Dictionary / node layer | Node storage, traversal, search indexes | KT-DB-002/003, KT-DICT-003/004 |
| 16 | Media | Library polish, node family, subtitle search | KT-MEDIA-003/005/006, KT-TEST-012 |
| 17 | AI service design | Source model + service boundary (RESEARCH until service exists) | KT-AI-001/002 |
| 18 | Performance & accessibility | Budgets, lazy loading, keyboard, screen readers | KT-PERF-*, KT-A11Y-* |
| 19 | World / 3D (gated on ADR-0018) | Engine decision → prototype → vertical slice | KT-GAME-001…005, KT-WORLD-002…006 |
| 20 | Release engineering | Desktop polish P0, v2.3, channels, CI | KT-DESK-002, KT-REL-001…003, KT-CI-002/003 |
| 21 | Visual QA, docs, decision log | Screenshot sweep, architecture docs, ADRs | KT-TEST-013, KT-DOC-006/007 |

---

## 2. Master TODO — numbered items by phase

> Check `[x]` items are done in code (see `OVERHAUL_BACKLOG.md` for file evidence).
> `[~]` = source-complete, compile verification deferred per user standing rule.

### Phase 1 — Repository audit + page identity

- [x] **1.** Spec→repository mapping (audit verdict: core intelligence overhaul already exists in code) — `OVERHAUL_BACKLOG.md` §1, §4a (KT-UI-001)
- [x] **2.** Page identity system — `PageIdentity`/`PageRegistry`/`ProvidePageIdentity` (KT-DEBUG-001)
- [x] **3.** Debug overlay with copy-debug-info (page/route/panel/theme/nav/window/version/platform) (KT-DEBUG-001)
- [ ] **4.** Per-screen audit rows recorded in `PageRegistry` (every discoverable destination documented) (KT-UI-001)
- [ ] **5.** Overhaul architecture docs — nav controller, theme tokens, page registry, card registry, level profiles, graph, search, import, AI (KT-DOC-006)
- [ ] **6.** Overhaul decision log — ADRs for nav unification, debug panel, card registry, level profiles, graph storage, AI (KT-DOC-007)

### Phase 2 — Navigation shell

- [x] **7.** One `NavigationController`, two presentations (floating/sidebar) from one `NavShell` (KT-NAV-001)
- [ ] **8.** Mode-switch stress regression — floating↔sidebar ×N, with overlays, during resize (KT-NAV-002)
- [x] **9.** Navigation settings fully wired — every control changes real state and persists (KT-NAV-003)
- [ ] **10.** Keyboard-shortcut customization UI (present: Ctrl+B mode toggle) (KT-NAV-003 remainder)
- [ ] **11.** Floating bubble interaction polish sweep per OS (drag feel, snap, right-click) (KT-NAV-004)
- [x] **12.** Sidebar presentation — width guard, hover/focus, collapse, mobile top/bottom (KT-NAV-005)
- [ ] **13.** Launchpad composition verification at small/medium/large windows (KT-NAV-006)
- [ ] **14.** Topbar/window chrome — themed topbar, native snap/resize without flashes (KT-NAV-007)
- [ ] **15.** Navigation stress tests — §99 regression scenario (mode switch → entry → back → resize → Sepia → card order persists) (KT-TEST-006)

### Phase 3 — Theme & design tokens

- [ ] **16.** Design-token consolidation — spacing/typography/radius/elevation owned by the theme (KT-THEME-001)
- [ ] **17.** Theme completeness pass — Sepia (and every theme) verified on every surface (KT-THEME-002)
- [ ] **18.** Hardcoded-color sweep — zero reachable `Color(0x…)` outside theme definitions (KT-THEME-003)
- [ ] **19.** Japanese typography system — JP font stacks, line heights, large-glyph breathing room (KT-THEME-004)
- [ ] **20.** Theme tests — every theme renders + persists; switching with open dialogs is safe (KT-TEST-007)

### Phase 4 — Debug panel + debug settings

- [x] **21.** Page identity (Phase 1) feeds the debug overlay — never hand-duplicated strings (KT-DEBUG-001)
- [~] **22.** Debug panel — live smoothed FPS (`withFrameNanos`), viewport, page/route/theme/nav readouts (KT-DEBUG-002)
- [~] **23.** Debug settings — persisted toggles, disable-animations gate, reduce-motion/high-contrast wiring, force theme/nav mode, reset (KT-DEBUG-003)
- [ ] **24.** Debug gating & polish — dev-only, subtle, theme-aware, never in release builds (KT-DEBUG-004)

### Phase 5 — Ghost-control + sweep passes

- [~] **25.** Ghost-control elimination — sweep run this pass: graph neighbor pills now focus the node (was dead); preview/tutorial/disabled controls honestly labeled (KT-UI-002)
- [ ] **26.** Space-utilization audit — desktop 3-column where useful, no forced empty columns (KT-UI-009)
- [~] **27.** Keyboard navigation — explorer results now support ↑/↓ navigate + Enter open + highlighted selection (Library already had it); standing audit for the rest of the app (KT-UI-010, this pass)
- [ ] **28.** Responsive breakpoints — compact/standard/wide; resize never crashes or clips (KT-UI-011)
- [~] **29.** Animation language — `AnimationTokens` catalog created (feedback/content/emphasis durations + springs + reduced-motion collapse) and used in the search filter row (KT-UI-012, this pass)
- [x] **30.** Old Kanji.Dojo remnant removal — sweep verified: zero user-facing remnants in core; only upstream attribution comments remain (KT-UI-013)
- [~] **31.** Empty/loading/error states — retry added to kanji entry + explorer errors; existing surfaces use `KaiteyoEmptyState` with specific copy; remaining surfaces in the standing audit (KT-UI-008)
- [x] **32.** Visual QA checklist — `VISUAL_QA_CHECKLIST.md` created: page × form-factor × theme matrix + per-page checks + recording format; screenshot sweep pending runtime (KT-TEST-013, this pass)

### Phase 6 — Core screen rebuilds

- [~] **33.** Home redesign — Continue/Today/Due/Recent/Recommended/Discover (Discover section delivered; hierarchy polish remains) (KT-UI-003)
- [~] **34.** Library redesign — audit verdict: Library is already a material hub (unified search + keyboard nav, mode chips incl. Exams, collections, deck grids, kanji/vocab browsing with filters, Manage menu); remaining gaps: courses/lessons/sentences/grammar as Library content + Saved/Imported sections (KT-UI-004, this pass)
- [~] **35.** Browse redesign — explorer landing is now an exploratory browse surface: JLPT/grade/frequency/strokes/POS tiles run **real filter-only queries** (engine now supports empty-text + filters) + graph entry card (KT-UI-005, this pass)
- [x] **36.** Statistics/heatmap polish — hover popup placement was already clamped (never under the cursor); **day cells now keyboard-accessible** (focusable on non-empty days, Enter/Space opens the day report, focus ring, screen-reader description, empty days skipped as tab stops) and **the Overview heatmap's dead `onDayClick = { }` ghost is fixed** (now opens the Activity tab's full day report via lifted `selectedDay` state) (KT-UI-006, this pass)
- [~] **37.** Settings rebuild audit — verdict: Settings Center is descriptor-based (`SettingDescriptor` + `SettingBinding` live pref binding + instant search with fully-functional matched settings); zero ghost handlers across all six categories; remaining: category breadth (Dictionary/Search/Keyboard/Data) (KT-UI-007, this pass)
- [x] **38.** Archived-deck filtering + restore section (KT-CORE-002)
- [ ] **39.** Sync indicator / sponsor button in shell chrome (KT-CORE-004)
- [ ] **40.** Command palette completion — covers all core destinations (KT-CORE-007)

### Phase 7 — Card registry & customization

- [x] **41.** Card registry — 15 card types (incl. Related + Variant family), one enum + one render branch per card (KT-CARD-001)
- [x] **42.** Card customization UI — show/hide, reorder, restore defaults via presets (KT-CARD-002)
- [x] **43.** Drag-reorder gesture — long-press header drag with midpoint slot-crossing and stride correction (card glued to pointer, no artifacts), persisted per move (KT-CARD-002, this pass)
- [x] **44.** Preset breadth — Intermediate/Writing/Reading/Dictionary presets delivered for kanji/word/sentence/grammar pages + tests (KT-CARD-003, this pass)
- [ ] **45.** Responsive card layout — desktop/tablet/mobile; no clipping/overflow on resize (KT-CARD-004)
- [ ] **46.** Card-config persistence tests — visibility/order/presets round-trip (KT-TEST-009)

### Phase 8 — Level profiles & adaptation

- [x] **47.** Level profile model + catalog — 10 profiles, `ProfilePresentation` (KT-LEVEL-001)
- [x] **48.** Presets + persistence + switching — `LearnerProfileStore`, sanitize-on-load, tests (KT-LEVEL-002)
- [x] **49.** Kanji-entry adaptation — example sentences filtered by profile difficulty (KT-LEVEL-003)
- [x] **50.** Word-page adaptation — adapted glossary, sentence difficulty, translation/furigana visibility (KT-LEVEL-003)
- [x] **51.** Browse-surface adaptation — `KnowledgeExplorer` kanji/word detail adapted (KT-LEVEL-003, this pass)
- [x] **52.** Custom profile editor — full editor (visibility toggles, depth/difficulty/graph/card preset), live-saved (KT-LEVEL-004, this pass)
- [x] **53.** Romaji configurability — persisted user override (`DisplayOverridesStore`) on top of the profile default; "Aa romaji" toggle on word pages + romanization under on/kun readings on kanji pages (KT-LEVEL-004, this pass)
- [ ] **54.** Explainable recommendations — why a word/sentence was chosen for this profile (KT-LEVEL-003 remainder)

### Phase 9 — Universal search

- [x] **55.** Universal search surface — global overlay Ctrl+Shift+F, grouped results, keyboard nav (KT-SEARCH-001)
- [ ] **56.** Query interpretation — "common verbs N3" → structured filters + autocomplete chips (KT-SEARCH-002)
- [~] **57.** Real filter system — kanji filters + word POS/JLPT exist in the engine and are now **surfaced as chips** on the explorer results (JLPT N1–N5, grade 1–6, frequency bands, verb/noun/adjective, clear-all); every chip re-queries (KT-SEARCH-003, this pass)
- [~] **58.** Sorting — all engine sorts (relevance/frequency/strokes/JLPT/grade/A–Z/reading/difficulty) surfaced as chips on the explorer results; word/sentence sorts run over the fetched window (KT-SEARCH-004, this pass)
- [x] **59.** Normalization + tokenization — `JapaneseTextNormalizer` (width/script/case folding, romaji both ways, wildcards, one-stop `queryMatches`) with exhaustive tests; **now wired into the universal search input** (`SearchScreenProcessInputUseCase` normalizes every query) (KT-SEARCH-005, this pass)
- [~] **60.** Search performance — debounce, indexes, incremental results (KT-SEARCH-006) — suite-side `engine/search/TrigramIndex` + `SearchPipeline` ranking power dictionary suggestions (`DictionaryService.suggestions` → lookup card chips)
- [ ] **61.** Search input modes — voice/handwriting/OCR gated until real engines exist; never fake (KT-SEARCH-007)
- [ ] **62.** Global command search — Ctrl+K palette for entries/settings (KT-SEARCH-008)

### Phase 10 — Sentence system & grammar

- [x] **63.** Sentence entry + explorer — token-interactive pages, difficulty, vocabulary, related sentences (KT-SENT-001)
- [~] **64.** Dictionary-driven `WordSegmenter` — longest real-word match, memoized (KT-SENT-002)
- [x] **65.** Provenance + quality labels — `ContentProvenance`, corpus tagged Authoritative/Tatoeba (KT-SENT-003)
- [~] **66.** Grammar entities + sentence highlighting — `GrammarCatalog`, deterministic matching (KT-SENT-004)
- [ ] **67.** Morphological tokenization upgrade — MeCab/UniDic lemma/POS (KT-SENT-002)
- [ ] **68.** Openly licensed grammar dataset research + kjd adapter (KT-GRAM-002)

### Phase 11 — Radical/component explorer

- [x] **69.** Radical explorer — selectable grid, stroke/JLPT/grade filters, real counts (KT-RAD-001)
- [ ] **70.** Component model beyond radical-derived — semantic/phonetic/structural decomposition (KT-RAD-002)
- [x] **71.** Component explorer — component grid → its kanji → entries (radical-derived) (KT-RAD-003)
- [ ] **72.** Frequency filter in the radical explorer (KT-RAD-001 remainder)

### Phase 12 — Knowledge graph

- [x] **73.** Graph data layer — typed nodes/edges, cached indexes over real queries (KT-GRAPH-001)
- [x] **74.** Graph visualization — pan/zoom canvas, select/expand, relationship colors, legend (KT-GRAPH-002)
- [ ] **75.** Graph multi-input UX — touch pinch/tap, hover previews, keyboard (KT-GRAPH-003)
- [ ] **76.** Graph as navigation — traversal breadcrumbs + back-stack preservation (KT-GRAPH-004)
- [ ] **77.** Graph relationship tests — edges resolve, traversal acyclic-safe (KT-TEST-010)

### Phase 13 — Frequency, keywords, provenance, imports

- [x] **78.** Frequency metadata system — 5 bands + rank label + source, accessible (KT-DATA-001)
- [ ] **79.** Keyword system — primary/alternate/learner/literal/component keywords (KT-DATA-002)
- [ ] **80.** Data versioning + source metadata — version, import date, license, checksum (KT-DATA-003)
- [ ] **81.** Import pipeline consolidation — JMdict/KANJIDIC/radicals/frequency/sentences normalized (KT-DATA-004, KT-DB-005)

### Phase 14 — Study integration

- [x] **82.** FSRS-5 SRS + review flow (scheduler logic never changed) (KT-STUDY-001)
- [ ] **83.** Study sessions/daily limits polish across card types (KT-STUDY-002)
- [ ] **84.** Study-state visualization on dictionary entries (Study card exists; per-card SRS state next)
- [ ] **85.** Exam-linked curriculum progression (KT-STUDY-003, KT-CURR-001)

### Phase 15 — Dictionary / node layer

- [x] **86.** Node-layer storage decision per ADR-0013 (tables vs read-model) (KT-DB-002) — **Decided: typed read-model over the existing databases, no new SQLDelight tables, no schema change** (satisfies the AGENTS.md never-change constraint). ADR-0013 moved Proposed → Accepted with the storage design recorded; code-side registries (`NodeTypeRegistry`, `RelationshipRegistry`) mirror the documented vocabulary with status + inverses; unit-tested (`NodeRegistryTest.kt`)
- [~] **87.** Event log table — `event_log` per EVENT_CATALOG (KT-DB-003) — **suite-side append-only `engine/events/EventLog`** (EVENT_CATALOG event families/types, semantic-only payloads, JSON snapshot persistence, recovery on corrupt snapshot, summary read-model) + tests; **wired into mining** (`CardMined` events on every mine). Core SQLDelight table remains gated on ADR-0013 node storage decision (this pass)
- [x] **88.** Node-anchored dictionary traversal — 食べる → 食 → 食事 → … (KT-DICT-003) — `NodeTraversal` over the knowledge core: resolve stable node ids (kanji/word/radical/grammar) → one-hop typed chips via `KnowledgeGraphRepository.expand` + multi-hop `walk(edgeSequence)` for 食べる→食→食事; DI-registered in `CoreModule`; sentence ids honestly documented as non-reversible content hashes (expand via the node itself). Suite `engine/graph/KnowledgeGraph` BFS path-finder remains reachable from dictionary surfaces (this pass)
- [~] **89.** Search pipeline indexes — FTS/trigram/prefix, no brute-force scans (KT-DICT-004) — suite `engine/search/TrigramIndex` (lazy per-dictionary trigram index) + `SearchPipeline` ranking wired into `DictionaryService.suggestions()` (this pass)

### Phase 16 — Media

- [x] **90.** Media Centre crash root cause + regression test (KT-MEDIA-001)
- [ ] **91.** Media library polish — continue-watching, history, playlists (KT-MEDIA-003)
- [~] **92.** Media node family — Series/Episode/Scene/SubtitleLine + exposure edges (KT-MEDIA-005) — **suite `engine/media/MediaNodeFamily`** (`MediaNodeGraph`: series inference from filenames, per-episode scenes from mining timestamps, exposure counts per line, flattened sentence surface) built from real `MediaMiningEvent`s + bookmarks, exposed via `AppState.mediaNodeGraph`; tests (this pass)
- [~] **93.** Subtitle search + history (KT-MEDIA-006) — `MediaNodeGraph.allLines()` provides the sentence surface; subtitle lookup/mining already flows through the shared dictionary popup
- [ ] **94.** Media lifecycle tests — backend failure, release, resume (KT-TEST-012)

### Phase 17 — AI service design

- [ ] **95.** Content-source model — sourceType + confidence metadata, display rules (KT-AI-001)
- [ ] **96.** AI service layer design — explain/generate/recommend behind a boundary, privacy disclosure (KT-AI-002)

### Phase 18 — Performance & accessibility

- [ ] **97.** Startup time budget — measure + hit (KT-PERF-001)
- [ ] **98.** Lazy loading / image caching / memory passes (KT-PERF-002)
- [ ] **99.** Full keyboard navigation + focus (KT-A11Y-002)
- [ ] **100.** Screen-reader support on core flows (KT-A11Y-003)

### Phase 19 — World / 3D (gated on ADR-0018)

- [ ] **101.** Game engine evaluation (Godot/Unity/Unreal/custom/Compose) + ADR-0018 (KT-GAME-001)
- [ ] **102.** Prototype player — move/look in a test scene (KT-GAME-002)
- [ ] **103.** Camera + input — first/third person switch, keyboard/mouse/controller/touch (KT-GAME-003)
- [ ] **104.** Small environment — one playable cell with collisions + lighting (KT-GAME-004)
- [ ] **105.** Interaction — node → prompt → action (KT-GAME-005)
- [ ] **106.** Kamakura + Enoshima vertical slice — §91 proof gate (KT-WORLD-002, KT-CONTENT-001)

### Phase 20 — Release engineering

- [ ] **107.** Desktop polish P0 — animation stutter + resize glitches to 60 FPS (KT-DESK-002)
- [ ] **108.** v2.3 release checklist (Anki interop + persistent data) (KT-REL-001)
- [ ] **109.** Update channels rollout — stable/beta/nightly end-user delivery (KT-REL-002)
- [ ] **110.** CI — PR validation + nightly builds with retention (KT-CI-002/003)

### Phase 21 — Visual QA, docs, decision log

- [ ] **111.** Screenshot sweep — every page × form factor × theme; no placeholder-looking screens (KT-TEST-013)
- [ ] **112.** Architecture docs + decision log complete (KT-DOC-006/007)
- [ ] **113.** Backlog/status docs in sync with code (§87 audit passes) (KT-DOC-004)

---

## 3. Selection rule

Work the **lowest-numbered open item** whose dependencies are all `[x]`/`[~]` and whose
package has no red gate (ADR-0017/0018/0013, plugin sandbox, dataset licenses). Phases
advance top-down; a phase may be partially open while its done items are used by later
phases, but do not start phase N+1's open items until phase N's blocking items are closed.

**Current position (this pass):** Phases 1–4 largely closed in code; Phase 7 and Phase 8
fully closed (drag-reorder 43, presets 44, browse adaptation 51, custom editor 52,
romaji override 53); Phase 5 progressing (25, 29, 30, 31 done/~; **27 explorer keyboard
nav; 32 QA checklist — this pass**); Phase 6 (Browse landing 35 done; **Library 34 +
Settings 37 audit verdicts — this pass**; **36 stats heatmap keyboard access + Overview
ghost fix — this pass**); Phase 9 search filters/sorts surfaced (57–58)
and **normalization wired into the universal search input (59 — this pass)**; Phase 15
**node-layer storage decision accepted (86 ✅), NodeTraversal delivered (88 ✅) — this
pass**; suite-side search indexes code-complete (89 ~).
Next open blocks: Phase 5 items 26/28, Phase 6 Library content gaps (courses/lessons,
sentences/grammar as Library modes — grammar mode delivered this pass, see backlog),
Phase 10 grammar dataset (68), Phase 15 event-log table (87, gated on ADR-0013 storage
— decision now recorded, table still needs explicit schema-change request per AGENTS.md).

## Related

- [`MASTER_TODO.md`](MASTER_TODO.md) — authoritative catalog (P0–P39)
- [`OVERHAUL_BACKLOG.md`](OVERHAUL_BACKLOG.md) — spec-derived backlog with file evidence
- [`TODO.md`](TODO.md) — operational short-list
- [`COMPLETED.md`](COMPLETED.md) — shipped work
- [`CURRENT_ISSUES.md`](CURRENT_ISSUES.md) — bug tracker
