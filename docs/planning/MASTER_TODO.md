# Kaiteyo — Master TODO

> The **full project inventory**, organized as hierarchical work packages **P0–P39**
> (MASTER §6, §81). The operational short-list (what to do next, priority-ordered) is
> [`TODO.md`](TODO.md); this file is the authoritative catalog. Statuses follow the
> taxonomy in [`README.md`](README.md) and `docs/product/PRODUCT.md` (MASTER §0).
>
> **Status legend**: ✅ `DONE` · 🚧 `IN PROGRESS` · 🔬 `TARGET` (architected/documented,
> not built) · 📋 `PLANNED` · 🔍 `RESEARCH` (needs investigation) · ⛔ `BLOCKED` ·
> 💀 `PLACEHOLDER` (scaffold, not functional).
>
> **Priority legend**: 🔴 P0 critical · 🟡 P1 high · 🟢 P2 medium · 🔵 P3 low.
>
> **Task format** (all rows have these columns): ID · Title · Status · Pri · Deps ·
> Acceptance (short form; full acceptance lives in the subsystem spec and
> `docs/architecture/nodes/TEST_PLAN.md`).
>
> **ID scheme**: `KT-<AREA>-<NNN>` where AREA = one of INFRA, CORE, DB, DICT, KANJI,
> VOCAB, GRAM, LIB, STUDY, STATS, EXAMS, MEDIA, MINE, YOMI, ANKI, ANKIC, WEB, ANDR,
> DESK, GAME, WORLD, CURR, QUEST, CHAR, AUDIO, REND, INPUT, SAVE, ASSET, L10N, A11Y,
> PERF, TEST, PACK, INST, CI, DOC, SEC, PRIV, REL.
>
> **Selection rule**: work the highest-priority task whose dependencies are all ✅/🚧
> and whose package has no 🔴 open gate. Agents: read `docs/ai/AI_AGENT_GUIDE.md` first.

---

## P0 — Critical infrastructure (repo stability)

Gate for everything else: the one-product decision (ADR-0017).

| ID | Title | Status | Pri | Deps | Acceptance |
|---|---|---|---|---|---|
| KT-INFRA-001 | Decide one product (core app vs suite) and record ADR-0017 | 🔬 | 🔴 | — | ADR-0017 `ACCEPTED`; losing copy marked DEPRECATED; duplication map (PRODUCT_AUDIT §6) down to zero live rows |
| KT-INFRA-002 | Remove dead shadows: `LearningPowerHub`, `SyncSettingsUI`, dead backup-manager path | ✅ | — | KT-INFRA-001 | **Removed** — `LearningPowerHub.kt`, `SyncSettingsUI.kt` and the dead `BackupSystemExt` BackupManager path no longer exist in the tree (verified 2026-08-18 by grep; only docs referenced them). Zero `onClick = { }` in reachable UI (repeat sweep tracked as KT-UI-002) |
| KT-INFRA-003 | Remove suite demo-data seeding; empty first-run state | 📋 | 🟡 | KT-INFRA-001 | First launch shows empty state, never fabricated cards; no-fake-data rule satisfied |
| KT-INFRA-004 | Fix ADR index duplicate rows (0013–0015) | ✅ | 🟢 | — | `decisions/README.md` lists each ADR once |
| KT-INFRA-005 | Keep the status taxonomy enforced in docs (CURRENT_STATE.md) | 🚧 | 🟢 | — | No undocumented status claims survive the §87 audit |

## P1 — Core application

| ID | Title | Status | Pri | Deps | Acceptance |
|---|---|---|---|---|---|
| KT-CORE-001 | Home screen polish per MASTER §41 (no useless buttons; glanceable progress) | 🚧 | 🟡 | — | UX flow in `nodes/UX_FLOWS.md` passes; no dead controls |
| KT-CORE-002 | Archived-deck filtering + restore section | ✅ | — | — | **Implemented** — both dashboards partition `is_archived` decks out of the main lists and render an expandable `ArchivedSectionHeader` with restore actions (`LettersDashboardScreenUI`/`VocabDashboardScreenUI`); archive/restore persist via the repositories (verified 2026-08-18) |
| KT-CORE-003 | Mobile navigation snap (top/bottom) consistent with desktop | 📋 | 🟡 | — | NavShell snap works in portrait/landscape on phone |
| KT-CORE-004 | Sync indicator / sponsor button in shell chrome | 📋 | 🟡 | — | Visible on all form factors |
| KT-CORE-005 | Settings Center cleanup (route all appearance options through categories) | 📋 | 🟡 | — | No randomly placed appearance options remain |
| KT-CORE-006 | Tablet layouts polish | 📋 | 🟢 | — | Form-factor nav + layouts verified on tablets |
| KT-CORE-007 | Command palette completion (feature parity with suite) | 📋 | 🟢 | — | Palette commands cover all core destinations |

## P2 — Database

| ID | Title | Status | Pri | Deps | Acceptance |
|---|---|---|---|---|---|
| KT-DB-001 | Two SQLDelight DBs + DataStore (existing) — maintain | ✅ | — | — | — |
| KT-DB-002 | Node-layer storage decision per ADR-0013 (tables vs read-model) | ✅ | 🔴 | KT-INFRA-001 | ADR-0013 `ACCEPTED` with storage design (**read-model over existing DBs — no new tables, no schema change**); registries as code (`NodeTypeRegistry`/`RelationshipRegistry`), unit-tested |
| KT-DB-003 | Event log table (target) — `event_log` per `EVENT_CATALOG.md` | 🔬 | 🟡 | KT-DB-002 | Schema + migration; ingestion from study/media events |
| KT-DB-004 | Consolidate suite JSON stores onto the unified data layer | 📋 | 🟡 | KT-INFRA-001 | One SRS, one deck model, one stats source of truth (PRODUCT_AUDIT §6) |
| KT-DB-005 | Consolidate the two jdata implementations (kjd vs suite engine/jdata) | 📋 | 🟡 | — | One pipeline; ADR-0007 updated |
| KT-DB-006 | Migration test suite incl. corruption recovery | 📋 | 🟢 | — | Every migration tested forward + rollback (STANDARDS §217) |

## P3 — Dictionary

| ID | Title | Status | Pri | Deps | Acceptance |
|---|---|---|---|---|---|
| KT-DICT-001 | Bundled AppDataDatabase + radical/word/sentence search (existing) | ✅ | — | — | — |
| KT-DICT-002 | Suite dictionary engines reachable from the product | 🔬 | 🔴 | KT-INFRA-001 | Dictionary popup + import work inside the shipped app |
| KT-DICT-003 | Node-anchored dictionary + traversal chips (NODE §81) | ✅ | 🟡 | KT-DB-002 | `NodeTraversal` walks 食べる → 食 → 食事 → … via typed one-hop chips + multi-hop `walk()` over real queries (kanji/word/radical/grammar seeds) |
| KT-DICT-004 | Search pipeline per STANDARDS §187 (normalize→tokenize→rank→filter) | 📋 | 🟢 | — | No brute-force scans; FTS/trigram/prefix indexes |
| KT-DICT-005 | Grammar/pitch/example surfaces over kjd data | 🔍 | 🟢 | KT-DATA-001 | Screens exist for grammar entries once dataset adopted |

## P4 — Kanji

| ID | Title | Status | Pri | Deps | Acceptance |
|---|---|---|---|---|---|
| KT-KANJI-001 | Kanji study, writing, stroke evaluation (existing) | ✅ | — | — | — |
| KT-KANJI-002 | Kanji exploration hub (NODE §82): overview/writing/readings/words/components/radicals/grammar/sentences/media/frequency/JLPT/user knowledge/practice/discoveries | 🔬 | 🟡 | KT-DB-002, KT-DICT-003 | Each tab reachable; traversal from any kanji page |
| KT-KANJI-003 | Kanji ↔ media exposure links ("seen in anime X") | 🔬 | 🟢 | KT-MEDIA-005 | `appears_in_media` edges queryable per kanji |

## P5 — Vocabulary

| ID | Title | Status | Pri | Deps | Acceptance |
|---|---|---|---|---|---|
| KT-VOCAB-001 | Vocab study, reading/writing modes, text analysis (existing) | ✅ | — | — | — |
| KT-VOCAB-002 | Vocabulary hub (NODE §83) incl. "Where have I seen this?" | 🔬 | 🟡 | KT-DB-002, KT-MEDIA-005 | Results: anime/media/Journey/previous reviews/mined cards |
| KT-VOCAB-003 | Pitch/frequency annotations on vocab cards | 🔍 | 🟢 | KT-DATA-003 | Dataset adopted; annotation rendered in card + popup |

## P6 — Grammar

| ID | Title | Status | Pri | Deps | Acceptance |
|---|---|---|---|---|---|
| KT-GRAM-001 | Grammar practice view + starter deck (existing, suite) | ✅ | — | — | — |
| KT-GRAM-002 | Openly licensed grammar dataset research + kjd adapter | 🔍 | 🟡 | — | Dataset passes MASTER §8 verification; adapter + tests |
| KT-GRAM-003 | Grammar node family + conjugation edges | 🔬 | 🟢 | KT-DB-002, KT-GRAM-002 | `conjugates_to` edges usable in lookups |

## P7 — Library

| ID | Title | Status | Pri | Deps | Acceptance |
|---|---|---|---|---|---|
| KT-LIB-001 | Library hub, decks, tags/flags, bulk actions (existing) | ✅ | — | — | — |
| KT-LIB-002 | Collections as first-class Library content | 📋 | 🟡 | — | Smart collections surfaced in Library top-level |
| KT-LIB-003 | Deck generation from reference data (JLPT/grade) | 📋 | 🟡 | — | Generate decks from kjd data instead of hardcoded lists |
| KT-LIB-004 | Shared decks (community) — after safety review | 📋 | 🔵 | — | Import/export of community decks with provenance |

## P8 — Study

| ID | Title | Status | Pri | Deps | Acceptance |
|---|---|---|---|---|---|
| KT-STUDY-001 | FSRS-5 SRS + review flow (existing) | ✅ | — | — | **Never change** scheduler logic (STANDARDS §6) |
| KT-STUDY-002 | Study sessions / daily limits polish | 🚧 | 🟢 | — | Limits respected across card types |
| KT-STUDY-003 | Exam-linked curriculum progression | 🔬 | 🟢 | KT-CURR-001 | Curriculum objectives drive exam content |

## P9 — Statistics

| ID | Title | Status | Pri | Deps | Acceptance |
|---|---|---|---|---|---|
| KT-STATS-001 | Event-driven stats, heatmap, AFK model (existing) | ✅ | — | — | — |
| KT-STATS-002 | Node-event drill-down (NODE §131): per-word/media/Journey stats | 🔬 | 🟡 | KT-DB-003 | Drill-down from overview to a single word's history |
| KT-STATS-003 | AFK ambient visualization (optional, reduced-motion safe) | 📋 | 🔵 | — | Optional; lightweight; respects reduced motion |
| KT-STATS-004 | Heatmap year transitions + day summary completeness | 📋 | 🟢 | — | MASTER §47 checklist passes |

## P10 — Exams

| ID | Title | Status | Pri | Deps | Acceptance |
|---|---|---|---|---|---|
| KT-EXAMS-001 | Generated + weekly exams (existing) | ✅ | — | — | — |
| KT-EXAMS-002 | Question types: listening/writing/dictation/cloze/matching/ordering/free-response/timed | 📋 | 🟡 | KT-MEDIA-002 | Each type implemented + scored |
| KT-EXAMS-003 | Adaptive/diagnostic exams from the knowledge graph | 🔬 | 🟢 | KT-DB-002 | Question generation uses user knowledge state |

## P11 — Media

| ID | Title | Status | Pri | Deps | Acceptance |
|---|---|---|---|---|---|
| KT-MEDIA-001 | Playback backends (VLC/mpv/Java Sound) + abstraction (existing, suite) | ✅ | — | — | Backend switchable without UI change |
| KT-MEDIA-002 | Subtitle engine (SRT/ASS/SSA/VTT) (existing, suite) | ✅ | — | — | — |
| KT-MEDIA-003 | Media library/playlists/folders/watched history polish | 🚧 | 🟢 | — | Continue-watching + history persisted |
| KT-MEDIA-004 | Media engines reachable from the product | 🔬 | 🔴 | KT-INFRA-001 | Media center + subtitle mining inside shipped app |
| KT-MEDIA-005 | Media node family (Series/Episode/Scene/SubtitleLine) + exposure edges | 🔬 | 🟡 | KT-DB-002 | `appears_in_media` edges; subtitle-search |
| KT-MEDIA-006 | Subtitle search + history | 📋 | 🟢 | — | Full-text search over subtitle corpus |

## P12 — Mining

| ID | Title | Status | Pri | Deps | Acceptance |
|---|---|---|---|---|---|
| KT-MINE-001 | Mining pipeline + duplicate protection (existing, suite) | ✅ | — | — | — |
| KT-MINE-002 | Mining reachable from the product + `mined_from` graph edges | 🔬 | 🔴 | KT-INFRA-001, KT-DB-002 | Mined cards carry provenance edges (media/timestamp/source) |
| KT-MINE-003 | Multi-word subtitle selection (first-class) | 📋 | 🟡 | — | Phrase-level selection → single card with sentence context |

## P13 — Yomitan

| ID | Title | Status | Pri | Deps | Acceptance |
|---|---|---|---|---|---|
| KT-YOMI-001 | Yomitan dictionary import + popup (existing, suite) | ✅ | — | — | ZIP/folder/JSON/JMdict verified |
| KT-YOMI-002 | Popup glossary in the shipped product | 🔬 | 🔴 | KT-INFRA-001 | Hover/click lookup on any Japanese text in the app |
| KT-YOMI-003 | WebView-injection evaluation for the learning browser | 🔍 | 🟢 | — | Decision recorded in `docs/media/YOMITAN.md` |

## P14 — Anki

| ID | Title | Status | Pri | Deps | Acceptance |
|---|---|---|---|---|---|
| KT-ANKI-001 | `.apkg` import/export (existing, all platforms) | ✅ | — | — | — |
| KT-ANKI-002 | On-device verification of iOS/Android APKG paths | ⛔ | 🟡 | platform access | Tested on device/simulator (BLOCKED on hardware) |
| KT-ANKI-003 | Destination selection UX (Kaiteyo / Anki / Both) | 📋 | 🟡 | — | Mine-time choice persisted per user preference |

## P15 — AnkiConnect

| ID | Title | Status | Pri | Deps | Acceptance |
|---|---|---|---|---|---|
| KT-ANKIC-001 | AnkiConnect client (existing, suite) | ✅ | — | — | — |
| KT-ANKIC-002 | End-to-end verification with live Anki | ⛔ | 🟡 | live Anki | Import + push verified; results recorded in TEST_PLAN |
| KT-ANKIC-003 | AnkiConnect service abstraction (no UI scatter) | 🚧 | 🟢 | — | STANDARDS §200 layers present |

## P16 — Website

| ID | Title | Status | Pri | Deps | Acceptance |
|---|---|---|---|---|---|
| KT-WEB-001 | Web trial evaluation (WASM/Compose Web) + decision | 🔍 | 🟢 | — | ADR per STANDARDS §242 methodology |
| KT-WEB-002 | Website section coverage (game/courses/downloads/wiki) | 📋 | 🟢 | — | MASTER §52 sections present |
| KT-WEB-003 | Automate `website/dist` regeneration in CI | 📋 | 🟢 | — | No stale committed dist (TODO debt item) |
| KT-WEB-004 | Command center backend: auth + roles (API.md §14 order) | 📋 | 🔴 | ADR-0019 | `/api/v1/auth` implemented per `docs/website/API.md`; roles enforced server-side; static site remains fallback |
| KT-WEB-005 | Suggestions backend: create/state machine/audit | 📋 | 🟡 | KT-WEB-004 | Suggestion CRUD + workflow states (spec §18) + audit log; accept→plan conversion with provenance (§19) |
| KT-WEB-006 | Kanban PATCH with corpus write-back | 📋 | 🟡 | KT-WEB-004 | Task status/priority edits update `MASTER_TODO.md`; 409 conflict handling; board re-renders |
| KT-WEB-007 | Whiteboard CRUD | 📋 | 🟢 | KT-WEB-004 | Node/edge/groups mutations per API.md §5; optimistic concurrency; validation of edge vocabulary |
| KT-WEB-008 | Realtime layer + notifications | 📋 | 🟢 | KT-WEB-004 KT-WEB-005 | WebSocket event channels; notification kinds with opt-out settings; reconnect/resume; no polling |
| KT-WEB-009 | Unified search + GitHub sync | 📋 | 🟢 | KT-WEB-005 | `/api/v1/search` over docs/tasks/roadmap/suggestions/decisions; issue→suggestion provenance links |

## P17 — Android

| ID | Title | Status | Pri | Deps | Acceptance |
|---|---|---|---|---|---|
| KT-ANDR-001 | Flavors, SAF picker, APKG, reminders (existing) | ✅ | — | — | — |
| KT-ANDR-002 | On-device verification pass (import/export, backup, sync) | ⛔ | 🟡 | hardware | Tested on Android device; results in CURRENT_ISSUES |
| KT-ANDR-003 | Gamepad/controller support for the shared app | 📋 | 🔵 | — | Input abstraction (KT-INPUT-001) covers Android |

## P18 — Desktop

| ID | Title | Status | Pri | Deps | Acceptance |
|---|---|---|---|---|---|
| KT-DESK-001 | Native window shell, resize, persistence (existing) | ✅ | — | — | — |
| KT-DESK-002 | Animation/resize stutter fixes (60 FPS) | 🚧 | 🔴 | — | P0 polish items from TODO closed |
| KT-DESK-003 | Suite engines integrated into the product window | 🔬 | 🔴 | KT-INFRA-001 | Workspace panels over unified data model |
| KT-DESK-004 | Windows runtime verification (media keys, tray, native drag) | ⛔ | 🟡 | Windows machine | Verified + recorded |

## P19 — Game engine

| ID | Title | Status | Pri | Deps | Acceptance |
|---|---|---|---|---|---|
| KT-GAME-001 | Engine evaluation (Godot/Unity/Unreal/custom/Compose) + ADR-0018 | 🔍 | 🔴 | — | ADR-0018 `ACCEPTED` with decision matrix (STANDARDS §242); **gate: no Journey code before this** |
| KT-GAME-002 | Prototype player (Stage 1) in chosen engine | 🔬 | 🔴 | KT-GAME-001 | Move/look in a test scene at budget FPS |
| KT-GAME-003 | Camera + input (Stage 2) | 🔬 | 🔴 | KT-GAME-002 | First/third person switch; keyboard/mouse/controller/touch |
| KT-GAME-004 | Small environment (Stage 3) | 🔬 | 🟡 | KT-GAME-003 | One playable cell with collisions + lighting |
| KT-GAME-005 | Interaction (Stage 4) | 🔬 | 🟡 | KT-GAME-004 | Interaction node → prompt → action |

## P20 — World

| ID | Title | Status | Pri | Deps | Acceptance |
|---|---|---|---|---|---|
| KT-WORLD-001 | World content schema + validation gates (existing as spec) | ✅ | — | — | `CONTENT_AUTHORING.md` gates defined |
| KT-WORLD-002 | Kamakura + Enoshima vertical slice content | 🔬 | 🔴 | KT-GAME-005, KT-CONTENT-001 | §91 proof gate passes (`TEST_PLAN.md` §13) |
| KT-WORLD-003 | Geographic data source review (OSM/GIS) per MASTER §24 | 🔍 | 🟢 | — | Lawful sources verified; license recorded |
| KT-WORLD-004 | World expansion via content packages (Phase 2+) | 📋 | 🔵 | KT-WORLD-002 | New region = new package, no engine change |

## P21 — Curriculum

| ID | Title | Status | Pri | Deps | Acceptance |
|---|---|---|---|---|---|
| KT-CURR-001 | Curriculum architecture (courses/lessons/objectives) | 🔬 | 🟡 | — | `docs/learning/curriculum-engine.md` implemented: objectives track user knowledge |
| KT-CURR-002 | Beginner → JLPT progression paths | 🔬 | 🟢 | KT-CURR-001, KT-DB-002 | Paths exist as course content packages |
| KT-CURR-003 | Children's curriculum (visual/audio/story-first) | 📋 | 🔵 | KT-CURR-001 | Age-appropriate path; same engine, different content (NODE §115) |

## P22 — Quests

| ID | Title | Status | Pri | Deps | Acceptance |
|---|---|---|---|---|---|
| KT-QUEST-001 | Quest schema + engine (objectives/conditions/rewards) | 🔬 | 🟡 | KT-WORLD-002 | Worked errand quest (slice) runs end-to-end |
| KT-QUEST-002 | Quest journal + contextual HUD | 🔬 | 🟢 | KT-QUEST-001 | NODE §101 UX: small cards, map markers, subtle notifications |
| KT-QUEST-003 | Daily/weekly quest rotation | 📋 | 🔵 | KT-QUEST-001 | Data-driven rotation, no predatory timers |

## P23 — Characters

| ID | Title | Status | Pri | Deps | Acceptance |
|---|---|---|---|---|---|
| KT-CHAR-001 | Player avatar (appearance/clothing/accessories) | 🔬 | 🟢 | KT-GAME-002 | Stylized per art direction; no generic proportions |
| KT-CHAR-002 | NPC system (identity/schedule/dialogue/quests) | 🔬 | 🟡 | KT-GAME-005 | Deterministic schedules; `JOURNEY_WORLD_SCHEMA.md` NPC fields |

## P24 — Audio

| ID | Title | Status | Pri | Deps | Acceptance |
|---|---|---|---|---|---|
| KT-AUDIO-001 | World audio architecture (ambient/music/weather/NPC) | 🔬 | 🟢 | KT-GAME-003 | `JOURNEY_RUNTIME_SPEC.md` audio nodes; reacts to world state |
| KT-AUDIO-002 | Language audio (dialogue/announcements/pronunciation) | 🔬 | 🟢 | KT-AUDIO-001 | TTS/pronunciation hooks to knowledge nodes |
| KT-AUDIO-003 | Audio production plan (asset sources, licensing) | 📋 | 🔵 | — | `docs/game/asset-pipeline.md` audio section |

## P25 — Rendering

| ID | Title | Status | Pri | Deps | Acceptance |
|---|---|---|---|---|---|
| KT-REND-001 | Quality tiers + frame budgets (Low/Med/High/Ultra) | 🔬 | 🟡 | KT-GAME-001 | Budgets in `TEST_PLAN.md` met per tier |
| KT-REND-002 | Water/weather/post-processing/particles (slice scope) | 🔬 | 🟢 | KT-GAME-004 | Beach/rain/sky pass on the slice |
| KT-REND-003 | LOD + occlusion + texture streaming | 🔬 | 🟢 | KT-WORLD-002 | Streaming budget verified at scale (STANDARDS §369) |

## P26 — Input

| ID | Title | Status | Pri | Deps | Acceptance |
|---|---|---|---|---|---|
| KT-INPUT-001 | Action-based input abstraction (MOVE/LOOK/INTERACT/…) | 🔬 | 🟡 | KT-GAME-001 | Physical controls map to actions; no hardcoded UI→input |
| KT-INPUT-002 | Keymap editing UI | 📋 | 🟢 | KT-INPUT-001 | Persisted per platform; reset option |

## P27 — Save system

| ID | Title | Status | Pri | Deps | Acceptance |
|---|---|---|---|---|---|
| KT-SAVE-001 | Game save schema + atomic writes + versioning | 🔬 | 🟡 | KT-GAME-003 | `docs/game/save-system.md` implemented; corruption recovery tested |
| KT-SAVE-002 | Save migration policy | 🔬 | 🟢 | KT-SAVE-001 | Migrations never destroy progress (MASTER §74) |

## P28 — Asset pipeline

| ID | Title | Status | Pri | Deps | Acceptance |
|---|---|---|---|---|---|
| KT-ASSET-001 | Game asset pipeline (model→UV→texture→LOD→package) | 🔬 | 🟡 | KT-GAME-001 | `docs/game/asset-pipeline.md` pipeline tooled |
| KT-ASSET-002 | App asset registry maintenance (`AppAssets.kt`) | ✅ | — | — | Prepare task manages declared assets only |
| KT-ASSET-003 | Branding asset replacement safety (copy, preserve source) | ✅ | — | — | STANDARDS §227 followed |

## P29 — Localization

| ID | Title | Status | Pri | Deps | Acceptance |
|---|---|---|---|---|---|
| KT-L10N-001 | EN/JA string interface (existing) — maintain | ✅ | — | — | New strings added to both implementations |
| KT-L10N-002 | Additional app languages evaluation | 📋 | 🔵 | — | Strings interface extended; game content localization plan |
| KT-L10N-003 | Japanese content accuracy guarantee | 🚧 | 🟢 | — | Linguistic review step in content pipeline (ADR-0015) |

## P30 — Accessibility

| ID | Title | Status | Pri | Deps | Acceptance |
|---|---|---|---|---|---|
| KT-A11Y-001 | Reduced motion, scale, high contrast (existing) | ✅ | — | — | — |
| KT-A11Y-002 | Full keyboard navigation | 📋 | 🟡 | — | Every action reachable by keyboard; focus visible |
| KT-A11Y-003 | Screen-reader support (desktop + mobile) | 📋 | 🟢 | — | Core flows announced correctly |
| KT-A11Y-004 | Game accessibility (camera, motion, colorblind, controls) | 📋 | 🟢 | KT-INPUT-001 | Accessibility is a Journey release gate |

## P31 — Performance

| ID | Title | Status | Pri | Deps | Acceptance |
|---|---|---|---|---|---|
| KT-PERF-001 | Startup time budget (measure + hit) | 📋 | 🟢 | — | Budget table (`performance.md`) verified |
| KT-PERF-002 | Lazy loading/image caching/memory passes | 📋 | 🟢 | — | Long lists + media lists under budget |
| KT-PERF-003 | Compose compiler metrics + profiling routine | 📋 | 🔵 | — | Profiling workflow documented in `performance.md` |

## P32 — Testing

| ID | Title | Status | Pri | Deps | Acceptance |
|---|---|---|---|---|---|
| KT-TEST-001 | Unit tests for core + kjd (existing) — maintain | ✅ | — | — | `:core:allTests`, `:kjd:test` green |
| KT-TEST-002 | Compose UI test harness | 📋 | 🟡 | — | Launch/navigation/search/review flows covered |
| KT-TEST-003 | Media/subtitle automated tests | 📋 | 🟢 | — | Parse/sync/mining covered without real media files |
| KT-TEST-004 | Migration + import/export + corruption tests | 📋 | 🟢 | — | STANDARDS §217 matrix covered |
| KT-TEST-005 | Performance regression gate (CI budget checks) | 📋 | 🔵 | KT-PERF-003 | Budget violations fail CI |

## P33 — Packaging

| ID | Title | Status | Pri | Deps | Acceptance |
|---|---|---|---|---|---|
| KT-PACK-001 | Desktop installers + portable packages (existing) | ✅ | — | — | — |
| KT-PACK-002 | Android APK/AAB (existing) | ✅ | — | — | — |
| KT-PACK-003 | Game/content packages versioning (ADR-0015) | 🔬 | 🟡 | — | Package format + verifier shipped with content pipeline |

## P34 — Installer

| ID | Title | Status | Pri | Deps | Acceptance |
|---|---|---|---|---|---|
| KT-INST-001 | Branded installers (Inno/DMG/AppImage/deb/rpm) (existing) | ✅ | — | — | — |
| KT-INST-002 | Installer options (location/shortcuts/associations/components) | 📋 | 🔵 | — | Only after packaging is stable (STANDARDS §234) |

## P35 — CI/CD

| ID | Title | Status | Pri | Deps | Acceptance |
|---|---|---|---|---|---|
| KT-CI-001 | Tag-based release pipeline + integrity gate (existing) | ✅ | — | — | — |
| KT-CI-002 | PR validation (lint/static analysis/unit tests) | 📋 | 🟢 | — | STANDARDS §231 targeted pipelines |
| KT-CI-003 | Nightly builds + artifact retention | 📋 | 🔵 | — | STANDARDS §232 separated pipelines |

## P36 — Documentation

| ID | Title | Status | Pri | Deps | Acceptance |
|---|---|---|---|---|---|
| KT-DOC-001 | Master blueprint (§0–§88) + product section | ✅ | — | — | This pass (docs/product/) |
| KT-DOC-002 | CURRENT_STATE + MASTER_TODO | ✅ | — | — | This pass |
| KT-DOC-003 | Game/media/learning/ui/database/ai sections | ✅ | — | — | This pass |
| KT-DOC-004 | Keep docs in sync with code (change rules MASTER §69) | 🚧 | 🟡 | — | §87 audit passes each pass; no drift |
| KT-DOC-005 | Website dist regeneration automation | 📋 | 🟢 | — | KT-WEB-003 |

## P37 — Security

| ID | Title | Status | Pri | Deps | Acceptance |
|---|---|---|---|---|---|
| KT-SEC-001 | Threat model + sanitization + token vault (existing) | ✅ | — | — | `docs/security/README.md` |
| KT-SEC-002 | Plugin sandbox design (capability model) before any runtime loading | 🔍 | 🟡 | — | ADR-0011 updated; no loading before sandbox |
| KT-SEC-003 | Secret-scanning in CI | 📋 | 🟢 | — | No secrets in history; pre-commit hook |

## P38 — Privacy

| ID | Title | Status | Pri | Deps | Acceptance |
|---|---|---|---|---|---|
| KT-PRIV-001 | Privacy policy (existing) — maintain | ✅ | — | — | `docs/security/PRIVACY.md` |
| KT-PRIV-002 | Opt-in analytics + world/country visualization rules | 📋 | 🟡 | — | MASTER §64/§65: nothing leaves device by default; manual override |

## P39 — Release engineering

| ID | Title | Status | Pri | Deps | Acceptance |
|---|---|---|---|---|---|
| KT-REL-001 | v2.3 release (Anki interop + persistent data) | 🚧 | 🔴 | KT-LIB-002 | Roadmap v2.3 checklist closed |
| KT-REL-002 | Update channels rollout (stable/beta/nightly) | 📋 | 🟡 | — | End users receive updates; rollback window verified |
| KT-REL-003 | Release channels doc (dev/alpha/beta/stable) | 📋 | 🟢 | — | MASTER §79 channels defined in `docs/releases/` |

---

## Cross-cutting gates (must be GREEN before dependent packages start)

| Gate | Where | Blocks |
|---|---|---|
| ADR-0017 one-product decision | `decisions/0017` | Suite-engine integration (KT-DICT-002, KT-MEDIA-004, KT-MINE-002, KT-YOMI-002, KT-DESK-003) |
| ADR-0018 game engine decision | `decisions/0018` | All of P19–P28 (game) |
| §91 vertical-slice proof gate | `TEST_PLAN.md` §13 | World expansion (KT-WORLD-004) |
| Plugin sandbox (ADR-0011) | `integrations/PLUGINS.md` | Any plugin runtime loading |
| Dataset license verification | `docs/data/SOURCES.md` | Any new dataset adapter |

## Related

- [`TODO.md`](TODO.md) — operational short-list (priority-ordered)
- [`OVERHAUL_BACKLOG.md`](OVERHAUL_BACKLOG.md) — spec-derived overhaul backlog (nav shell, themes, debug tooling, cards, level profiles, universal search, knowledge graph, sentences, media/world follow-ups)
- [`COMPLETED.md`](COMPLETED.md) — shipped work
- [`CURRENT_ISSUES.md`](CURRENT_ISSUES.md) — bug tracker
- [`../product/PRODUCT.md`](../product/PRODUCT.md) — MASTER §6, §81
- [`../engineering/ENGINEERING_STANDARDS.md`](../engineering/ENGINEERING_STANDARDS.md) — STANDARDS §365 phase order
