# Kaiteyo Master Blueprint (MASTER §0–§88)

> **Status: LIVE SOURCE OF TRUTH for product definition.** This document is the wrapper
> around the rest of Kaiteyo's documentation. It defines **what Kaiteyo is**, **why it
> exists**, **how it is organized**, and **how everything connects**. It is maintained
> together with `docs/engineering/ENGINEERING_STANDARDS.md` (§163–§376, ADR-0012) and
> `docs/architecture/NODE_ARCHITECTURE.md` (§76–§162, ADR-0013).
>
> **Section numbering** uses stable identifiers `MASTER §NN` so other documents can cite
> them without breaking (same convention as `STANDARDS §NN` and `NODE §NN`).
>
> **Status taxonomy** used throughout: `IMPLEMENTED` · `PARTIALLY_IMPLEMENTED` · `BROKEN`
> · `PLACEHOLDER` · `PROTOTYPE` · `DOCUMENTED_ONLY` · `PLANNED` · `ARCHITECTED` ·
> `BLOCKED` · `DEPRECATED` · `UNKNOWN`. Never mix implemented and planned features: every
> claim below that describes a *current* capability was verified against the repository
> (see `docs/planning/PRODUCT_AUDIT.md` and `docs/planning/CURRENT_STATE.md`).
>
> **Related documents**: parent — [`docs/README.md`](../README.md); vision —
> [`VISION.md`](VISION.md); principles — [`PRINCIPLES.md`](PRINCIPLES.md); current state —
> [`../planning/CURRENT_STATE.md`](../planning/CURRENT_STATE.md); master task list —
> [`../planning/MASTER_TODO.md`](../planning/MASTER_TODO.md); node/Journey spec —
> [`../architecture/NODE_ARCHITECTURE.md`](../architecture/NODE_ARCHITECTURE.md);
> engineering contract — [`../engineering/ENGINEERING_STANDARDS.md`](../engineering/ENGINEERING_STANDARDS.md).

---

## MASTER §0 — Absolute rules for this documentation

These rules govern **every** document in this repository. They were written into the
master blueprint and are reproduced here because they bind the writers, the human
developers, and the future AI agents equally.

1. **Inspect the actual repository first.** No documentation of a subsystem is written
   from memory. Every status claim must trace to code (entry point → call chain → data
   store) or be labeled `UNKNOWN` with a note about what must be inspected. The audit
   that satisfies this rule for the current tree is `docs/planning/PRODUCT_AUDIT.md`
   (verified at HEAD) and the living per-subsystem matrix in
   `docs/planning/CURRENT_STATE.md`.
2. **Never mix implemented and planned features.** Every documented feature carries a
   status from the taxonomy above. The three-way split is: **CURRENT** (what exists now,
   verified), **TARGET** (what we want; `ARCHITECTED`/`PLANNED`), **FUTURE** (intentionally
   postponed; recorded, not scheduled). See `NODE §158` for the same rule on the Journey
   side and `STANDARDS §341`–§342 for the engineering side.
3. **Do not delete existing documentation without a reason.** Consolidate duplicates
   intelligently; preserve useful history. Any removal is itself recorded (see
   `docs/development/DocumentationRules.md`).
4. **Document the reasoning.** Major decisions live in Architecture Decision Records
   (`docs/architecture/decisions/`, ADR-0001…0018). Every ADR records decision,
   alternatives, reason, tradeoffs, consequences, future implications.
5. **Document dependencies.** For every major subsystem: internal dependencies, external
   dependencies, data dependencies, runtime dependencies, platform dependencies, optional
   dependencies, licensing implications, upgrade risks. The dependency map is
   `docs/planning/ENGINEERING_AUDIT.md`.
6. **Document failure modes.** For every major system: missing data, database failure,
   unavailable API, undecodable media, unavailable sync target, malformed imports,
   offline user, full storage, changed external integration, future schema change.
   Failure behavior is captured per-subsystem in `docs/architecture/` and in the test
   contract `docs/architecture/nodes/TEST_PLAN.md`.
7. **Document performance.** Define goals, budgets, indexing requirements, caching, lazy
   loading, async work, streaming, UI rendering, animation, media decoding, search,
   graph traversal, mobile/desktop/low-end constraints. See `docs/architecture/performance.md`.
8. **Document cross-platform behavior.** Desktop, Android, iOS, touch, keyboard, mouse,
   gamepad. What is shared and what is platform-specific is documented in
   `docs/platform/` and `docs/architecture/OVERVIEW.md`.
9. **Do not force everything into one architecture.** Application UI, learning platform,
   media system, dictionary system, knowledge graph, game runtime, data layer,
   integrations, platform layer are separate concerns — but their communication is
   documented (event catalog, service contracts, node edges).

### In depth — how the rules are enforced

- **Verification chain for CURRENT claims**: `FEATURES.md` (status matrix) → `CURRENT_STATE.md`
  (subsystem matrix) → `PRODUCT_AUDIT.md` (deep audit with file-level evidence) → code.
  A claim may be promoted from `PLANNED` to `IMPLEMENTED` only after it passes the
  acceptance criteria in `docs/architecture/nodes/TEST_PLAN.md` and the Definition of
  Done in `AGENTS.md`.
- **The two-app defect is the standing example** of rule 2: the desktop suite
  (`desktopApp/.../desktop/*`) is real but **unreachable** from any shipped `main()`;
  therefore its features are labeled `IMPLEMENTED (unshipped — suite)` everywhere, never
  `IMPLEMENTED (product)`. See `PRODUCT_AUDIT.md` §1.
- **No-status claims are bugs.** If a document makes a factual claim without a status,
  the claim must be either verified and labeled or removed. The final audit (MASTER §87)
  re-checks this.

---

## MASTER §1 — Kaiteyo product definition

Kaiteyo (書いてよ, *"write it!"*) is a **connected Japanese language ecosystem** — a
premium, cross-platform application that unifies: a Japanese dictionary, a kanji/kana
knowledge system, a vocabulary system, grammar and sentence systems, a learning platform
with a library/deck system and SRS study, a mining system, a media center with
Yomitan-style glossing and ASBPlayer-style subtitle workflows, Jidoujisho-style
workflows, Anki/AnkiConnect integration, extensive statistics, exams, JLPT-related
functionality, a knowledge graph, a course/curriculum system, onboarding, cross-platform
clients (desktop/Android/iOS), a website, and — as target architecture — an **actual 3D
Japanese-learning game world** (the Journey).

It is a fork of [Kanji Dojo](https://github.com/syt0r/Kanji-Dojo), since grown into an
independently developed project with its own branding, data platform (`kjd`), and design
system. GPL-3.0 licensed; offline-first; desktop-first (Windows/macOS/Linux flagship with
Android and iOS sharing the core engine).

### The ecosystem, not the features

Kaiteyo is documented as ONE ecosystem. Knowledge flows across surfaces:

```
KANJI → vocabulary → grammar → sentences → media occurrences → mined cards → decks
      → study history → exams → course progress → game interactions → statistics
```

A user's knowledge is represented **consistently** everywhere: one user-knowledge model
(`docs/architecture/nodes/KNOWLEDGE_STATE_MODEL.md`), one event ledger
(`docs/architecture/nodes/EVENT_CATALOG.md`), one node layer
(`docs/architecture/nodes/NODE_DATA_MODEL.md` — target), one statistics pipeline
(`docs/architecture/statistics.md`).

### Current vs target product surface

| Subsystem | CURRENT (verified) | TARGET (documented) |
|---|---|---|
| Dictionary | ✅ Bundled SQLDelight `AppDataDatabase` (core app) + Yomitan/JMdict import + popup (desktop suite) | Node-anchored dictionary with traversal (§81, NODE); grammar/pitch/example surfaces |
| Kanji/kana | ✅ Study, writing practice, stroke evaluation, radical search | Kanji exploration hub (§82 NODE) |
| Vocabulary | ✅ Vocab decks, reading/writing modes, text analysis | Vocabulary hub with "where have I seen this?" (§83 NODE) |
| Grammar | 🚧 Grammar practice view + starter deck (desktop suite); no bundled dataset | Grammar node family + curriculum |
| Learning platform | ✅ Library, decks, SRS (FSRS-5), reviews, exams, stats | Course/curriculum system (MASTER §29, `docs/learning/`) |
| Mining | ✅ Subtitle/browser/OCR/clipboard/dictionary mining (desktop suite) | Mining into the knowledge graph (`mined_from` edges) |
| Media center | ✅ VLC/mpv/Java Sound backends, subtitles, screenshots, bookmarks (desktop suite) | Media node family (Series/Episode/Scene/SubtitleLine) |
| Statistics | ✅ Event-driven stats, heatmap, AFK model | Node-event drill-down; Journey/media stats |
| Exams | ✅ Generated + weekly exams, JLPT-style scoring | Exam node family; adaptive testing |
| Knowledge graph | 🟡 Language data graph exists (kjd entities); user-knowledge graph is target | Full node/edge layer (ADR-0013) |
| Game world | 🟡 `UNKNOWN` — no implementation (see MASTER §21) | Journey: Kamakura + Enoshima vertical slice first |
| Website | ✅ Static site (`website/`, Python build) | Web trial (§53) — `PLANNED` |
| Sync/account | 🚧 GitHub device-flow + private gist (desktop-first) | Conflict-hardened multi-device sync |

---

## MASTER §2 — Master product principles

The full, per-domain expansion is [`PRINCIPLES.md`](PRINCIPLES.md). The summary:

The product is: **professional · dense but understandable · powerful · fast · coherent ·
highly connected · extensible · data-driven · user-controlled · offline-friendly ·
open-data-friendly · cross-platform · keyboard/touch/controller-friendly · accessible ·
visually polished · animation-rich without being wasteful · configurable · maintainable ·
scalable.**

| Domain | Core principle (short form) | Deep reference |
|---|---|---|
| UX | Complexity underneath the interface; power-tool density; no empty pages | `PRINCIPLES.md` §UX, `docs/architecture/nodes/UX_FLOWS.md` |
| UI | One token-based design system; no random anything | `docs/design/`, `docs/ui/README.md` |
| Architecture | Connected, modular, domain-first; smallest correct surface | `docs/architecture/OVERVIEW.md`, STANDARDS §177–§178 |
| Database | Relational, offline-first, migrated, integrity-checked | `docs/architecture/database.md`, `docs/database/MIGRATIONS.md` |
| Performance | Measured budgets, not vibes | `docs/architecture/performance.md` |
| Extensibility | Data-driven, plugin-safe, documented contracts | `docs/architecture/content.md`, `docs/integrations/PLUGINS.md` |
| Integrations | Local-first, graceful degradation, never a hard dependency | `docs/integrations/README.md` |
| Game design | Real game; exploration/discovery-first; never XP grinding | `docs/game/game-overview.md`, NODE §86–§87 |
| Learning design | Contextual exposure → optional depth; user knowledge respected | `docs/learning/`, `docs/vision/learning-philosophy.md` |
| Data provenance | Every node/row has source, license, checksum | `docs/data/SOURCES.md` |
| Licensing | GPL-3.0; open datasets with verified redistribution | `docs/legal/`, `docs/data/` |
| Code quality | Explainable decisions; no fake anything | STANDARDS §163–§164 |

---

## MASTER §3 — Master repository documentation

The repository is documented as a complete map in `docs/architecture/FILE_STRUCTURE.md`
and `docs/architecture/OVERVIEW.md`. The canonical, short map (verified this session):

| Path | Purpose | Ownership | Generated? |
|---|---|---|---|
| `core/` | Shared Kotlin Multiplatform code: UI (Compose MPP), business logic, data layer, `commonMain`/`jvmMain`/`androidMain`/`iosMain` | `ua.syt0r.kanji.presentation.*`, `core.*`, `di.*` | no |
| `desktopApp/` | Thin JVM wrapper (`Main.kt` → `KaiteyoApp`) + the JVM-only feature library `desktopApp/.../desktop.*` (media, game, dictionary, engines) folded into shipped destinations via the Media/Game Koin hosts | `ua.syt0r.kanji.desktopApp`, `ua.syt0r.kanji.desktop` | no |
| `app/` | Android entry point; flavors `googlePlay` (Firebase/billing/review) and `fdroid` (Google-free) | `ua.syt0r.kanji` (app source sets) | no |
| `iosApp/` | iOS entry point (Swift host + Compose UI) | — | no |
| `kjd/` | **KJD** data platform: ingest → normalize → validate → SQLite export + Kotlin SDK + CLI | `ua.syt0r.kanji.kjd` | yes (output DB) |
| `mediaGenerator/` | JVM utility (javacv + coil) for generating media assets | — | no |
| `installer/` | Branded installer subsystem (Inno Setup, DMG, AppImage/deb/rpm, update feeds) — **not a Gradle module** | scripts/configs | yes (artifacts) |
| `website/` | Static site, Python build (`build.py`) consuming `docs` | — | yes (`dist/`, committed) |
| `buildSrc/` | Gradle logic: `AppVersion.kt` (single version source), `AppAssets.kt` (declared assets) | — | no |
| `tools/cli/` | **`kaiteyo` developer command center** (git, gradle, wsl, doctor, docs, …) | Python | no |
| `gradle/`, `gradlew*`, `settings.gradle.kts`, `build.gradle.kts` | Build system (JDK 17, Kotlin 2.1, version catalog) | — | no |
| `brand/`, `preview_assets/`, `fastlane/` | Branding and store assets | — | no |
| `scripts/`, `scratch/` | Helper scripts; AI-session scratch (gitignored) | — | mixed |
| `docs/` | All documentation (this tree) | per-directory READMEs | no |

**Rules**: the root must stay clean (MASTER §85); no random docs at the root; assets in
`core` compose resources are **managed** — anything not declared in `AppAssets.kt` is
deleted by the prepare task (never drop files there by hand). `local.properties` is never
committed. `desktopApp/src/jvmMain/kotlin` is the suite's home; the suite is **not**
reachable from `main()` (PRODUCT_AUDIT §1) until the one-product decision lands
(ADR-0017).

### In depth — the §84 target document tree and where each piece actually lives

The blueprint's output list (MASTER §84) asked for a specific folder layout. The
repository uses a **topic-first** layout (subsystem docs under `docs/architecture/`,
status under `docs/features/`, design under `docs/design/`). Rather than duplicate, every
requested document is mapped below (also in `docs/README.md`):

| Blueprint target | Lives at |
|---|---|
| `docs/product/PRODUCT.md`, `VISION.md`, `PRINCIPLES.md` | ✅ created by this pass — `docs/product/` |
| `docs/architecture/ARCHITECTURE.md`, `SYSTEM_OVERVIEW.md` | `docs/architecture/OVERVIEW.md` + `FILE_STRUCTURE.md` |
| `docs/architecture/DATA_FLOW.md` | `docs/architecture/OVERVIEW.md` (data flow section) |
| `docs/architecture/KNOWLEDGE_GRAPH.md` | `docs/architecture/language-model.md` + `docs/architecture/nodes/NODE_DATA_MODEL.md` |
| `docs/architecture/PLATFORM.md` | `docs/platform/README.md` |
| `docs/architecture/DECISIONS.md` | `docs/architecture/decisions/README.md` |
| `docs/database/DATABASE.md`, `DATA_MODEL.md` | `docs/architecture/database.md` + `docs/architecture/nodes/NODE_DATA_MODEL.md` |
| `docs/database/MIGRATIONS.md` | ✅ `docs/database/MIGRATIONS.md` (this pass) |
| `docs/dictionary/DICTIONARY.md` | `docs/architecture/dictionary.md` + `docs/integrations/YOMITAN_DICTIONARIES.md` |
| `docs/kanji/KANJI.md`, `docs/vocabulary/VOCABULARY.md` | `docs/architecture/language-model.md` |
| `docs/learning/LEARNING.md`, `CURRICULUM.md` | `docs/learning/` (curriculum-engine, adaptive-learning, progress-model) + `docs/vision/learning-philosophy.md` |
| `docs/library/LIBRARY.md` | `docs/features/LIBRARY.md` |
| `docs/statistics/STATISTICS.md` | `docs/architecture/statistics.md` + `docs/features/STATISTICS.md` |
| `docs/exams/EXAMS.md` | `docs/architecture/exams.md` |
| `docs/media/MEDIA_CENTER.md`, `MINING.md` | `docs/architecture/media.md`, `docs/architecture/mining.md` |
| `docs/media/YOMITAN.md`, `ASBPLAYER_WORKFLOW.md` | ✅ `docs/media/` (this pass) |
| `docs/integrations/ANKI.md`, `ANKICONNECT.md` | ✅ `docs/integrations/ANKI.md` (covers AnkiConnect) |
| `docs/game/*` | `docs/game/` (existing spec set — 17 docs + `asset-pipeline.md`, `README.md`; engine evaluation folded into `game-overview.md`; ADR-0018) |
| `docs/ui/UI.md`, `NAVIGATION.md`, `SETTINGS.md` | `docs/design/` + `docs/architecture/NAVIGATION.md` + ✅ `docs/ui/SETTINGS.md` |
| `docs/ux/UX.md` | `docs/architecture/nodes/UX_FLOWS.md` + `docs/design/DESIGN_LANGUAGE.md` |
| `docs/platform/ANDROID.md`, `DESKTOP.md`, `WEB.md` | ✅ `docs/platform/` (existing per-OS docs) |
| `docs/engineering/CODING_STANDARDS.md`, `TESTING.md`, `PERFORMANCE.md`, `SECURITY.md`, `PRIVACY.md`, `RELEASES.md` | `docs/development/CODING_STANDARDS.md`, `docs/testing/`, `docs/architecture/performance.md`, `docs/security/`, `docs/releases/` — indexed by ✅ `docs/engineering/README.md` |
| `docs/data/DATA_SOURCES.md`, `INGESTION.md`, `LICENSES.md` | `docs/data/SOURCES.md`, `docs/data/ARCHITECTURE.md`, `docs/legal/` |
| `docs/ai/AI_AGENT_GUIDE.md` | ✅ `docs/ai/AI_AGENT_GUIDE.md` (this pass) |
| `docs/roadmap/ROADMAP.md`, `MASTER_TODO.md` | `docs/roadmap/ROADMAP.md` + ✅ `docs/planning/MASTER_TODO.md` |

No empty placeholder folders were created: a folder exists only when it holds real
documents (MASTER §3: *do not blindly create useless folders*).

---

## MASTER §4 — Master documentation index

`docs/README.md` is the documentation portal. It must let a human or an AI agent navigate
without guessing: where to start, architecture, product, development, game, media,
dictionary, learning, integrations, troubleshooting, contribution, roadmap. The map is
maintained in `docs/README.md` and re-verified at the end of every documentation pass
(MASTER §87).

Entry order for a new contributor (also encoded in `docs/ai/AI_AGENT_GUIDE.md`):

1. Root `README.md` — what Kaiteyo is
2. `docs/README.md` — the map
3. `docs/product/PRODUCT.md` — this blueprint
4. `docs/planning/CURRENT_STATE.md` — what exists, what doesn't
5. `docs/engineering/ENGINEERING_STANDARDS.md` — the engineering contract
6. `docs/architecture/OVERVIEW.md` — the architecture
7. Subsystem docs as needed
8. `docs/planning/MASTER_TODO.md` — what to build next

---

## MASTER §5 — Current state audit

The complete audit lives in **`docs/planning/CURRENT_STATE.md`** (per-subsystem status
matrix, evidence locations, known problems, debt, and risks). It is derived from:

- `docs/planning/PRODUCT_AUDIT.md` — deep, file-level audit (the two-app defect, dead
  shadows, duplication map, platform status);
- `docs/features/FEATURES.md` — the feature status matrix (source of truth for statuses);
- `docs/planning/CURRENT_ISSUES.md` — living bug tracker;
- `docs/planning/ENGINEERING_AUDIT.md` — dependency map, risks, starting files.

Headline findings (verified):

1. **Two parallel applications.** The shipped core app (Compose MPP, SQLDelight, FSRS)
   and the desktop suite (JVM-only dictionary/media/mining/OCR/AnkiConnect) do not share
   data or navigation. The suite is unreachable from any `main()`. **Decision required —
   ADR-0017.** (PRODUCT_AUDIT §1)
2. **The core learning stack is real and healthy** — Home/Library/Statistics/Exams/SRS
   driven by two SQLDelight databases and kjd-generated reference data.
3. **Dead shadows exist** (LearningPowerHub, SyncSettingsUI, backup manager path) — not
   yet removed, tracked as debt (PRODUCT_AUDIT §5.2).
4. **Demo data seeding** in the suite violates the no-fake-data rule (PRODUCT_AUDIT §5.4).
5. **Platform verification gaps**: iOS/Windows/Android actuals are code-complete but not
   runtime-verified on device (BLOCKED items in TODO.md).
6. **The game does not exist.** Journey is `ARCHITECTED` only (ADR-0014); no engine
   decision has been made yet (MASTER §36, ADR-0018 `PROPOSED`).

---

## MASTER §6 — Master TODO system

The hierarchical project TODO is **`docs/planning/MASTER_TODO.md`**. It replaces the
notion of one giant checklist with **work packages P0–P39** (infrastructure, core app,
database, dictionary, kanji, vocabulary, grammar, library, study, statistics, exams,
media, mining, Yomitan, Anki, AnkiConnect, website, Android, desktop, game engine,
world, curriculum, quests, characters, audio, rendering, input, save system, asset
pipeline, localization, accessibility, performance, testing, packaging, installer,
CI/CD, documentation, security, privacy, release engineering).

Every task carries: `ID` (`KT-<AREA>-<NNN>`), title, description, `status`, `priority`,
`dependencies`, affected modules, acceptance criteria, testing requirements, and
documentation requirements. The operational short-list remains `docs/planning/TODO.md`
(priority-ordered); MASTER_TODO is the full inventory and MASTER_TODO is authoritative
for "what exists as a planned task".

---

## MASTER §7 — Architecture decision records

All decisions are recorded in `docs/architecture/decisions/` (ADR-0001…0018). The
blueprint requires decisions at minimum for: application architecture (ADR-0003/0004),
database (ADR-0005), knowledge graph (ADR-0013), local-first strategy (ADR-0005, ADR-0009),
synchronization (ADR-0009), media architecture (ADR-0018-related; `docs/integrations/MEDIA_BACKENDS.md`),
dictionary architecture (ADR-0007 for kjd; suite `DictionaryRepository`), Anki integration
(`docs/integrations/ANKI.md`), Yomitan integration (`docs/integrations/YOMITAN_DICTIONARIES.md`),
game engine (**ADR-0018 — decision pending**), world streaming (deferred to engine choice;NODE §92), rendering (deferred; `docs/rendering/`), input abstraction
   (`docs/input/`, `docs/game/player.md`), asset management (`docs/architecture/assets.md`), mobile/desktop
architecture (ADR-0003), web architecture (deferred; MASTER §53), plugin/extension
architecture (ADR-0011), import/export (`docs/architecture/backup.md`), licensing
(ADR-0001, `docs/legal/`), data updates (ADR-0007, kjd patch feeds), migrations
(`docs/database/MIGRATIONS.md`), caching (`docs/architecture/performance.md`), offline
operation (`docs/architecture/OVERVIEW.md`).

---

## MASTER §8 — Open-source data foundation

Kaiteyo builds on openly licensed datasets via the **KJD** data platform
(`kjd/`, ADR-0007) instead of recreating language data by hand. Verified current
dataset adapters (from `docs/data/SOURCES.md` and `kjd/` source):

| Dataset | Provides | License | Status in kjd |
|---|---|---|---|
| KanjiVG | Stroke order, stroke paths, components | CC BY-SA 3.0 | ✅ adapter |
| KANJIDIC | Meanings, readings, classifications, grade, JLPT | CC BY-SA 3.0 | ✅ adapter |
| Tanos JLPT lists | JLPT classification | CC BY | ✅ adapter |
| JMdict | Expressions, readings, meanings, POS, senses, dialect | CC BY-SA 4.0 | ✅ adapter |
| JmdictFurigana | Furigana data | CC BY-SA 4.0 | ✅ adapter |
| Leeds frequency | Frequency data | CC BY 2.5 | ✅ adapter |
| yomichan-jlpt-vocab | JLPT vocabulary classification | CC BY-SA 4.0 | ✅ adapter |

Additional sources **investigated but not yet adopted** (each requires license/attribution/
update/format/transformation review before inclusion — MASTER §8 rule: *do not add
datasets just because they exist*): grammar corpora, pitch-accent datasets, Tatoeba
example sentences, handwriting corpora, name dictionaries. Tracked in
`docs/planning/TODO.md` (RESEARCH) and `kjd/README.md`.

For every source the project records: source, license, attribution, data provided,
update frequency, format, integration method, legal considerations, transformation
process, share-alike obligation, attribution requirement, commercial-use permission.
That record is `docs/data/SOURCES.md`.

---

## MASTER §9 — Data ingestion architecture

KJD implements the required reproducible pipeline:

```
SOURCE → DOWNLOAD → VERIFY → PARSE → NORMALIZE → VALIDATE → TRANSFORM → INDEX → IMPORT
      → VERSION → STORE → QUERY
```

Verified behaviors (from `kjd/` source and `docs/data/ARCHITECTURE.md`):

- Raw datasets are never silently overwritten; each source has a pinned version.
- Checksums and provenance are recorded per dataset version (MASTER §8 / `docs/data/`).
- Validation gates exist at ingest time (entity resolution, constraint checks).
- Output is a SQLite database + Kotlin SDK + CLI; the core app consumes it as the
  read-only `AppDataDatabase` asset (bundled, versioned — `AppDataDatabaseVersion = 15`).
- Desktop additionally receives **incremental patch feeds** (base + delta) applied at
  runtime (KJD patch updates, `FEATURES.md`).
- Failed imports roll back; a broken import never corrupts the shipped database.

Missing / target: dataset **rollback tooling** and **re-ingest from source in CI**
(partially covered by kjd's own tests; tracked in MASTER_TODO KT-DATA-*).

---

## MASTER §10 — Knowledge graph

Kaiteyo's central knowledge graph is specified in depth in
`docs/architecture/nodes/` (NODE §76–§162):

- **Node families**: LANGUAGE (kana, kanji, radicals, components, vocabulary,
  expressions, grammar, conjugations, sentences, stories, pitch, frequency,
  pronunciation), LEARNING (courses, lessons, exams, decks, cards, reviews, user
  knowledge, mastery), MEDIA (series, episodes, scenes, subtitles, clips, mining
  events), WORLD (regions, cities, districts, locations, objects, NPCs), GAMEPLAY
  (quests, stories, dialogue, discoveries, collections, achievements), USER, SYSTEM.
- **Edges**: a controlled vocabulary (`contains`, `has_reading`, `appears_in`,
  `mined_from`, `encountered_by`, `requires`, `unlocks`, …) in
  `docs/architecture/nodes/RELATIONSHIP_REGISTRY.md`.
- **Physical representation**: relational-first (SQLDelight `node`/`edge` tables or a
  read-model over the existing databases) — **a graph database is not assumed**
  (NODE §149; `docs/architecture/nodes/NODE_DATA_MODEL.md`). The decision is recorded
  in ADR-0013.
- **Current state**: kjd already produces a rich entity graph (kanji ↔ readings ↔
  meanings ↔ radicals ↔ words ↔ sentences); the **user-knowledge graph**
  (`encountered_by`, `mastered_by`, `mined_from`) is `TARGET` (ADR-0013, ADR-0016).

---

## MASTER §11 — Dictionary

Kaiteyo **is** a dictionary — a professional one. Current verified state:

- **Core app**: bundled `AppDataDatabase` (read-only) with kanji/readings/meanings/
  radicals/classifications; radical & reading search; word/sentence search; text
  analysis (word-by-word breakdown). (`core/.../app_data`, `KaiteyoDataCenter`)
- **Desktop suite**: Yomitan-compatible import (ZIP/folder/JSON/JMdict), enabled/
  priority management, indexed search with `SearchMode` (EXACT/PREFIX/KANA/DEINFLECT)
  + scoring, deinflection, segmentation, dictionary popup with TTS, tags, mining
  actions. (`desktopApp/.../desktop/engine/dictionary`)
- **Spec**: `docs/architecture/dictionary.md`; status matrix in `FEATURES.md`.

Required search dimensions (target): kanji, vocabulary, readings, meanings, partial/
exact matches, radicals, components, JLPT, frequency, pitch, conjugated forms,
grammatical forms, sentences, names, media occurrences, user content. Indexing strategy
per STANDARDS §186–§187 (SQLite FTS/trigram/prefix; never brute-force scans).
Traversal UX per NODE §81 (`食べる → 食 → 食事 → …`).

---

## MASTER §12 — Kanji system

Current: kanji study (JLPT N5–N1 + school grade decks), writing practice with stroke
order diagrams + drawing canvas + stroke evaluation (count/order/shape scoring,
strictness levels), radical search, kanji browser, collections. (`core/.../screen/kanji_browser`,
`practice_letter`, `stroke_evaluator`).

Target (NODE §82): each kanji is an **exploration hub** — overview, writing, readings,
words, components, radicals, grammar, sentences, media, frequency, JLPT, user knowledge,
practice, Journey discoveries — with natural traversal (`食 → 食べる → 食事 → …`).

Required data: KanjiVG strokes (✅ in kjd), decomposition (✅ components), variants and
related kanji (partially in data; traversal is target).

---

## MASTER §13 — Kana system

Current: kana decks (hiragana + katakana) with the same study/writing engine as kanji;
small-kana and dakuten/handakuten coverage via the letter decks; JLPT-beginner paths.

Target (documented in `docs/learning/` + `docs/vision/learning-philosophy.md` + NODE LANGUAGE family): explicit
kana node types (basic, dakuten, handakuten, combinations, small kana, historical/
extended katakana incl. foreign-sound combinations), stroke data rendering identical to
kanji practice, kana recognition/production knowledge dimensions.

---

## MASTER §14 — Library

The Library is the **central learning-content management system** — there is deliberately
**no separate "Study" destination**; study is integrated into Library and related views.

Current (verified): unified Library hub with decks (JLPT/grade premade, custom, imported
`.apkg`, generated), tags/flags managers, bulk actions, card browser, note editor, deck
browser, import/export (JSON/CSV/TSV/TXT/APKG with preview + conflict policies), backup,
statistics, deck details (overview/study/cards/browse/stats/settings), archive
(`is_archived` — filtering follow-up tracked in TODO.md). Spec:
`docs/features/LIBRARY.md`, `docs/architecture/study-engine.md`.

Target additions: collections as first-class content (smart collections exist in the
suite: `SmartCollectionEngine`), shared decks, deck generation from reference data
(JLPT/grade), node-based browsing (NODE §128).

---

## MASTER §15 — Anki

Anki interoperability is documented in full in `docs/integrations/ANKI.md`. Verified:

- `.apkg` import/export on **all platforms** (JVM/Android/iOS) — shared codec in
  `core/.../transfer/`, platform actuals, template rendering, scheduling mapping,
  media extraction, HTML sanitization, rollback-safe.
- **Conceptual model**: Kaiteyo and Anki are separate systems. Mined cards can go to
  Kaiteyo, Anki, or both (destination choice is a product requirement — currently the
  suite pushes to Anki via AnkiConnect, and both sides keep their own scheduling).
- Duplicate detection: note GUID (`externalId`) + content fingerprint; conflict policy
  user-selectable (Skip / Update / Duplicate).

---

## MASTER §16 — Yomitan

Research and Kaiteyo-native design: `docs/media/YOMITAN.md` (this pass) +
`docs/integrations/YOMITAN_DICTIONARIES.md`. Current verified: the suite imports
Yomitan-compatible archives (ZIP/folder/JSON, `index.json` meta) and provides popup
glossing over its own engine — **without a browser extension**. The evaluation of
approaches (native text selection / WebView injection / browser extension / clipboard /
accessibility APIs) is recorded in `docs/media/YOMITAN.md`; conclusion so far: native
in-app selection + popup over imported dictionaries is the primary path (✅ in suite),
browser support via the learning browser's WebView is `PARTIAL`/`PLANNED`, and a real
browser extension is `FUTURE` (not decided).

---

## MASTER §17 — Media center

The Media Center is a major subsystem, not a video player. Current verified (suite):
VLC (VLCJ), mpv (JSON-RPC), Java Sound backends behind a **player abstraction**;
playback controls incl. speed, A–B loop, frame step; SRT/ASS/SSA/VTT subtitles with
sync; screenshots; bookmarks; local library, playlists, folders; subtitle mining;
dictionary integration; text-hook + player WebSocket for external tooling. Spec:
`docs/architecture/media.md`; backend licensing: `docs/integrations/MEDIA_BACKENDS.md`.

Target (NODE §130): media node family (Series/Episode/Scene/SubtitleLine), subtitle
search/history, audio extraction, watched-history-driven stats.

---

## MASTER §18 — ASBPlayer-style functionality

Kaiteyo's own native implementation of the workflow (not a clone of ASBPlayer's UI):
`docs/media/ASBPLAYER_WORKFLOW.md` (this pass). Verified current: subtitle selection,
multi-word selection (segmentation + deinflection), sentence extraction, screenshot +
audio capture, subtitle timing, card generation, dictionary popup, repeated playback,
mining — the core of the suite's mining loop. **Multi-word selection is first-class**
(target: NODE media mining; current: subtitle-line selection + multi-token lookup).

---

## MASTER §19 — Pitch / frequency

Current: frequency data (Leeds) is ingested by kjd and drives word ordering/statistics;
pitch accent is **not yet surfaced** (no open pitch dataset adopted — RESEARCH in
TODO.md). Target: pitch-accent data (dataset review required), pitch visualization,
frequency/pitch annotations in dictionary results, subtitles, and cards; accent-pattern
audio playback.

---

## MASTER §20 — Jidoujisho-style workflow

Jidoujisho's workflow (media → lookup → understand → mine → study on device) is the
reference for Kaiteyo's **seamless single-app loop**. Kaiteyo's equivalent is the
verified **immersion loop** (suite): media → subtitle → select → dictionary popup →
pitch/frequency → example → screenshot/audio → card → deck (Kaiteyo and/or Anki) →
study → statistics → exam → knowledge graph. Documented end-to-end in
`docs/architecture/mining.md` and `docs/media/ASBPLAYER_WORKFLOW.md`. Differences are
intentional: Kaiteyo is not a mobile-first tool and does not depend on Android-only
workflows; the same loop runs on desktop.

---

## MASTER §21 — Game: this is an actual game

**Status: ARCHITECTED (TARGET).** No game code exists. `docs/planning/CURRENT_STATE.md`
marks every game subsystem `TARGET`. This is deliberate — the blueprint forbids
pretending otherwise (NODE §158, MASTER §83).

The Journey **is an actual game**, not "gamification" and not "a gamified learning map".
It obeys real game design principles: player, world, movement, camera, interaction,
exploration, locations, NPCs, quests, activities, dialogue, stories, rewards,
progression, collectibles, achievements, events, environments, audio, animation,
physics where appropriate, world simulation, time, weather, transportation, discovery,
secrets, optional content, replayability.

Design pillars (from NODE §86): **exploration · observation · discovery · language ·
culture · story · photography · collection · daily life**, with light RPG-style
progression for motivation — never XP grinding (NODE §116). Inspirations are
Nintendo-style polish, Shashingo-style environment-language interaction, Shin-chan-style
everyday atmosphere — **not clones** (MASTER §21, NODE §97).

Full specs: `docs/game/game-overview.md` (+ `docs/game/README.md` index), NODE §86–§119, `docs/architecture/nodes/JOURNEY_WORLD_SCHEMA.md`,
`GAMEPLAY_SYSTEMS.md`, `JOURNEY_RUNTIME_SPEC.md`.

---

## MASTER §22 — Game design philosophy

See `docs/game/game-overview.md` and NODE §86. Summary: not a traditional RPG; **avoid** combat,
giant skill trees, arbitrary XP, artificial statistics; **emphasize** exploration,
curiosity, interaction, discovery, language, culture, everyday situations, characters,
activities, quests, stories, collection, mastery. Small RPG elements are allowed as
*motivation* (progression, unlocks, collectibles, cosmetics, achievements, relationship
progression, location discovery, quest chains).

Every Journey feature must pass the test (NODE §153): *does it improve exploration,
language, culture, story, discovery, or immersion?* If not, it may not belong.

---

## MASTER §23 — Game world

Target architecture, fully specified in `docs/game/world-architecture.md` and NODE §88–§93:

```
WORLD → REGION → PREFECTURE → CITY → DISTRICT → MAP CELL → LOCATION → INTERIOR → INTERACTION NODE
```

- Long-term vision: a representation of Japan — **never all at once**; modular expansion
  (MASTER §25, NODE §90).
- World cells with dynamic load/unload (NODE §92, `docs/game/world-streaming.md`).
- Objects, NPC schedules, dialogue, quests, stories as typed nodes (NODE §93–§102,
  `docs/architecture/nodes/JOURNEY_WORLD_SCHEMA.md`).
- Terrain, buildings, roads, rail, vehicles, water, beaches, parks, stations, shops,
  restaurants, schools, aquariums — documented asset/geometry requirements in
  `docs/game/asset-pipeline.md`.

---

## MASTER §24 — Real-world accuracy

The world may use accurate geography **where licensing permits**. **Lawful sources only**:
OpenStreetMap (ODbL — attribution required, share-alike), public GIS data, government
datasets, licensed geographic data, open elevation data. **Never scrape proprietary
mapping data** (MASTER §83). Balance accuracy vs game performance (LOD, stylization,
cell size) — documented in `docs/game/world-architecture.md` and `docs/rendering/rendering-performance.md`.

---

## MASTER §25 — Map development strategy

Phased expansion (NODE §90, MASTER §70, `docs/production/phases.md` + `docs/game/world-architecture.md`):

- **Phase 1**: one small, extremely polished region — **Kamakura + Enoshima** vertical
  slice (NODE §91).
- **Phase 2**: adjacent locations; **Phase 3**: larger city; **Phase 4**: regional
  expansion; **Phase 5**: multiple regions; **Phase 6**: long-term Japan world.
- Each expansion is a **modular content package** (ADR-0015, `docs/architecture/nodes/CONTENT_AUTHORING.md`)
  — asset reuse, streaming, LOD, and procedural systems where appropriate. Engine must
  support adding regions without rewriting (NODE §90).

---

## MASTER §26 — Camera

Specified in `docs/game/player.md` + `docs/game/camera.md`. Requirements: **first-person and third-person**, with
a deliberate camera action to switch (NODE §96); camera smoothing, collision, sensitivity,
FOV, camera height, accessibility, mobile/controller/mouse camera behavior. First-person
preferred for photography/reading/dictionary interaction; third-person for movement/
social/avatar visibility.

---

## MASTER §27 — Player controls

Input abstraction layer is specified in `docs/game/player.md` + `docs/input/input-system.md` (MASTER §60, STANDARDS §317).
Actions (MOVE, LOOK, INTERACT, BACK, CONFIRM, CANCEL, SPRINT, JUMP, CAMERA, MENU, MAP,
INVENTORY, QUESTS, DICTIONARY, SUBTITLES, PAUSE) are mapped onto **physical controls**
(keyboard/mouse/touch/gamepad) in per-platform keymaps; **never hard-code UI actions to
one physical input**. The existing app already centralizes shortcuts (`ShortcutRegistry`
in the suite; review shortcuts in core) — the game keymap extends the same model.

---

## MASTER §28 — World activities

Activity catalog and design rules in `docs/game/game-overview.md` + NODE §103. Activities (walk,
sit, swim, dive, visit aquarium/beach, ride trains, explore shops/restaurants/parks/
schools/museums, interact with NPCs, observe objects, read signs, photograph, discover
vocabulary, complete quests, listen to dialogue, understand signs, collect words,
identify kanji, solve language tasks) **create contextual language exposure** — never
turn every activity into a quiz (NODE §112).

---

## MASTER §29 — Curriculum systemThe dense curriculum architecture is documented in `docs/learning/curriculum-engine.md` +
   `docs/vision/learning-philosophy.md`. It supports: complete beginner → kana → basic vocabulary → beginner grammar →
sentences → paragraphs → stories → intermediate → advanced → JLPT-aligned → custom
progression, plus a **dedicated children's educational progression**
(`docs/vision/child-experience.md`, MASTER §30).

---

## MASTER §30 — Children's Japanese world

Specified in `docs/vision/child-experience.md` and NODE §115. Rules:

- **Not** a simplified adult flashcard system. Dedicated path: visual learning, audio,
  repetition, stories, characters, songs where appropriate, simple interaction,
  vocabulary/grammar/kanji/kana/sentences/paragraphs/stories, cultural context,
  progressive difficulty.
- The map/curriculum is a **dense 3D place**: courses are represented as places, routes,
  or areas; the child progresses **through the world**.
- **Child mode shares the world engine, node architecture, and knowledge graph** with
  normal mode — different UX, progression, language complexity, safety, content (NODE
  §115). Age ranges are handled carefully (no assumption of reading ability).

---

## MASTER §31 — Normal Kaiteyo game mode

Normal users are **not forced** into the children's curriculum. Options: preset
progression, chosen level, custom progression, free exploration, knowledge-based
adaptive progression. The same world technology is shared; the content hierarchy differs
(NODE §115). See `docs/vision/child-experience.md` + `docs/learning/curriculum-engine.md`.

---

## MASTER §32 — Quest system

Specified in `docs/game/quest-system.md` and NODE §100–§101, with concrete schema + worked
example in `docs/architecture/nodes/JOURNEY_WORLD_SCHEMA.md` (errand quest) and
`JOURNEY_SLICE_CONTENT.md`. Covers: definition, objectives, prerequisites, dialogue,
locations, rewards, learning requirements, optional objectives, branching, completion,
failure, repeatability; daily/weekly/story/exploration/language quests. UX: small
objective cards, map markers, subtle notifications, journal, progress, contextual hints —
**quest UI disappears when not needed** (NODE §101).

---

## MASTER §33 — Adaptive learning

The game must know what the player already knows (NODE §113, MASTER §33). If the user
knows 日・本・語・学生, the game never re-teaches them as beginners. Difficulty adapts via:
known kanji, known vocabulary, known grammar, frequency, JLPT, previous mistakes, exam
results, media exposure, study history — all read from the **user knowledge model**
(`docs/architecture/nodes/KNOWLEDGE_STATE_MODEL.md`), which is the single source of
truth shared with the app. Reuse world geometry; change dialogue/quests/knowledge
density/language complexity (NODE §113).

---

## MASTER §34 — Game ↔ knowledge graph

One of the most important connections. Fully specified in
`docs/architecture/nodes/KNOWLEDGE_STATE_MODEL.md` + `JOURNEY_WORLD_SCHEMA.md` (§149
bridge). Mappings:

- GAME OBJECT → language metadata (`represents`)
- NPC → dialogue → vocabulary/grammar (`dialogue` node fields)
- LOCATION → signs → vocabulary/kanji
- QUEST → language requirements (`requires`)
- STORY → grammar progression
- ACTIVITY → assessment (optional, never a quiz wall)
- PLAYER KNOWLEDGE → adaptive content (`encountered_by`/`mastered_by` edges)

---

## MASTER §35 — Game save system

Specified in `docs/game/save-system.md`. Covers: player save, world state,
quest state, inventory, discovered locations, collectibles, relationships, settings,
progression, language knowledge (referenced, not duplicated — the knowledge graph owns
it), game-specific data. Defines schema versioning + migration policy, atomic writes,
corruption recovery, and the offline-first rule. Reference schema is in
`docs/architecture/nodes/NODE_DATA_MODEL.md` (save stores) and `JOURNEY_RUNTIME_SPEC.md`.

---

## MASTER §36 — Game engine / technology evaluation

**Decision pending — ADR-0018.** Do **not** blindly choose a technology (STANDARDS §242).
Candidates to evaluate: Godot, Unity, Unreal, custom engine, existing Kaiteyo rendering
technology (Compose/Skia). Comparison axes: Android, desktop, controller, touch, 3D
rendering, world streaming, animation, asset pipeline, licensing, performance, tooling,
**AI-agent friendliness**, maintainability, embedding into Kaiteyo. The evaluation
methodology, decision matrix, and the "separate runtime vs shared module" question are
documented in `docs/game/game-overview.md` §Engine evaluation (MASTER §36) and ADR-0018 (`PROPOSED`). Until the ADR
is `ACCEPTED`, **no Journey runtime code may start** (gated in MASTER_TODO KT-GAME-001).

---

## MASTER §37 — Rendering

Target spec: `docs/rendering/`. Covers renderer, lighting, shadows,
materials, shaders, water, sky, weather, particles, VFX, post-processing, LOD, occlusion,
asset streaming, texture compression, mesh optimization, mobile vs desktop rendering,
and **visual quality tiers** (Low/Medium/High/Ultra with explicit frame budgets).
Art direction (Nintendo-quality philosophy, warm, Japanese, atmospheric, never generic
Roblox / never low-effort AI 3D / never photoreal-for-its-own-sake) is documented in
`docs/rendering/rendering-architecture.md` (art direction) and NODE §97/§38.

---

## MASTER §38 — Art direction

See `docs/rendering/rendering-architecture.md` (art direction) + NODE §97. Documented: character
style (stylized, cozy, clean, expressive, simple enough for cross-platform performance),
environment style, UI style, lighting, color, typography, animation, VFX, material
language. A coherent artistic direction is a **hard requirement**; generic or
inconsistent art is explicitly banned.

---

## MASTER §39 — Asset pipeline

Target spec: `docs/game/asset-pipeline.md` + `docs/architecture/assets.md`
(current app asset system). The pipeline is documented as: source asset → cleanup →
modeling → UV → texture → material → rig → animation → LOD → optimization → import →
metadata → packaging, with naming, folder, format, license, attribution, source,
ownership, version, and optimization rules. Game content ships as **versioned packages**
(ADR-0015) so worlds expand without app updates.

---

## MASTER §40 — Kaiteyo UI / UX

Design language is documented in `docs/design/` (DESIGN_LANGUAGE, UI_SYSTEM, THEME_SYSTEM,
ANIMATION_SYSTEM) and enforced by the `Ds*` token system in the suite + the core theme
tokens. Blueprint rules (NODE §120–§125): use available space intelligently; no centered
tiny cards on empty pages; no giant margins; no random padding/boxes/radii/typography;
documented spacing scale (4dp grid), typography scale, colors, surfaces, shadows, radii,
animations, hover/focus/press/drag/selection/transitions. The settings catalog is
`docs/ui/SETTINGS.md`; navigation is `docs/architecture/NAVIGATION.md`; flows are
`docs/architecture/nodes/UX_FLOWS.md`.

---

## MASTER §41 — Home

Spec: `docs/architecture/nodes/UX_FLOWS.md` (Home). Rules: **no useless buttons**
(social/downloads/tutorial/random tools) unless they have a clear product purpose.
Home provides: glanceable progress, heatmap, study target, collections, recent activity,
current goals, continue learning, relevant recommendations, useful shortcuts — **without
duplicating full Statistics**. Current verified: `HomeScreen` → dashboards (General/
Library/Statistics/Search/Settings) with real counts from SQLDelight.

---

## MASTER §42 — Sidebar

Spec: `docs/architecture/NAVIGATION.md` + NODE §134. Blueprint rule: sidebar mode means
**the entire window content area becomes the sidebar** (an immersive navigation state),
not a tiny strip; desktop target ≈ 20% navigation / 80% content, responsive. Documented
states: collapsed, expanded, animation, keyboard, touch, controller, accessibility,
persistence. Current verified: `NavShell` (shared) supports Sidebar/Floating modes,
edges, expanded/compact, tooltips, `Ctrl+B` toggle, snap, persistence. Suite's
`WorkspaceNav` is the second implementation awaiting the one-product decision
(ADR-0017).

---

## MASTER §43 — Floating bubble

Exact behavior spec: `docs/architecture/NAVIGATION.md` + `docs/architecture/nodes/UX_FLOWS.md`
+ NODE §133. Requirements (verified implemented in core `NavShell` floating mode):

- Draggable; must never crash when moved (persisted position, safe-area aware).
- Physical magnetic behavior: drag → proximity detection → magnetic attraction →
  smooth interpolation → snap → subtle jiggle/settle. **Not teleport** (NODE §133).
- ~3 magnetic snap positions per side; persistence; collision/safe areas; multi-monitor
  and display-scaling handling.
- Click → Launchpad; hold → launchpad/menu; right-click → launchpad/menu; touch hold
  supported.
- Documented thresholds: drag threshold, click threshold, hold duration.

---

## MASTER §44 — Launchpad

Spec: `docs/architecture/nodes/UX_FLOWS.md` (Launchpad) + NODE §126/§135. Requirements:
open centered, smooth open/close, no FPS drop, consistent button spacing, no square
artifacts, consistent icons, keyboard/mouse/touch/gamepad navigation, adapts to screen
size. Core destinations: Home, Browse, Library, Media, Stats, Journey, Dictionary/search,
Settings. Animation states documented (opening, settled, closing, reduced-motion).

---

## MASTER §45 — Settings

The **settings catalog** — every setting with default, valid range, effect, persistence,
platform differences — is `docs/ui/SETTINGS.md` (this pass). Categories per blueprint:
general, appearance, animation (speed/reduced motion), media, dictionary, study, SRS,
statistics, exams, integrations (Anki, Yomitan), game, controls (keyboard/mouse/
controller/touch), accessibility, language, data/database, privacy, storage, backup,
import/export, developer. Current verified: Settings Center in core with categories;
Theme Studio; shortcut remapping; motion presets; suite settings JSON. **Settings
navigation must not crash** (regression gate in TEST_PLAN).

---

## MASTER §46 — Statistics

Full system spec: `docs/architecture/statistics.md` + `EVENT_CATALOG.md`. Current
verified: event-driven stats (`StatisticsController`), heatmap, learning curves,
retention, goals, study velocity, profile, exams/achievements. Blueprint dimensions
(target): daily/weekly/monthly/yearly/lifetime, kanji/vocab/characters/grammar/sentences
learned, media exposure, reading/listening/speaking/writing time, deck progress, JLPT
estimates, frequency coverage, knowledge-graph coverage, exam results, retention,
accuracy, mature/new/reviewed cards, lapses, study/active/idle/media/mining/game time.
**Never fake data** (STANDARDS §290) — all aggregates derive from the event ledger.

---

## MASTER §47 — Heatmap

Spec: `docs/architecture/statistics.md` + `UX_FLOWS.md`. Verified current: Anki-style
heatmap with year navigation, intensity levels, day drill-down. Requirements: smooth
year transitions, blank days stay blank, intensity = meaningful activity (not clicks),
hover/click tooltips with day summary (date, study duration, active duration, decks,
cards, kanji/vocab/grammar, media, mining, exams, game activity), no fabricated data.

---

## MASTER §48 — Smart active-time detection

Spec: `docs/architecture/statistics.md` (§AFK model). **Do not** say "30 minutes = AFK".
Activity signals: keyboard, mouse, touch, controller, window focus, media playback,
study interaction, scrolling, drawing/typing. Configurable inactivity model; modes
SMART and CUSTOM; stats distinguish active study / passive media / idle / background;
inactive time is never counted as study time (verified in the statistics event model).

---

## MASTER §49 — AFK experience

Optional ambient visualization (e.g., kanji/vocabulary falling like rain) is `PLANNED`
and specified in `docs/architecture/statistics.md` + `UX_FLOWS.md`. Hard requirements:
optional, lightweight, respects reduced motion, no excessive resource use. No current
implementation.

---

## MASTER §50 — Exams

Full spec: `docs/architecture/exams.md`. Current verified: generated exams + weekly
exam, JLPT-style scoring, analytics. Blueprint target adds: listening/reading/writing/
dictation/audio-comprehension sections, cloze, matching, ordering, free response,
timed sections, custom/diagnostic/review/course/deck exams, adaptive testing, question
generation from the knowledge graph. See `docs/learning/curriculum-engine.md` for exam-linked
curriculum progression.

---

## MASTER §51 — Browse / search

Browse is the discovery/search interface. Current verified: `SearchScreen` + radical
search + text analysis; suite `DictionaryLookupCard` + smart collections/saved filters.
Target (NODE §129): global search across kanji/vocabulary/grammar/sentences/media/decks/
Journey/collections with filters (JLPT, frequency, reading, POS, pitch, source, media,
knowledge, deck, difficulty) and a search pipeline (input → normalization → script
detection → tokenization → query interpretation → candidate retrieval → ranking →
filters → presentation — STANDARDS §187).

---

## MASTER §52 — Website

Current: static site in `website/` (Python build, consumes `docs`). Blueprint target
sections: homepage, product, dictionary, learning, media, game, courses, downloads,
documentation, Q&A, wiki, community, account, web trial. Design must share the app's
design philosophy (modern, polished, confident, minimal-but-not-empty, strong visual
hierarchy). `dist/` is committed — regenerating on docs changes is a tracked debt
(TODO.md).

---

## MASTER §53 — Web trial

**PLANNED** (no implementation). A limited but functional web trial lets users experience
Kaiteyo without installing. Requirements to document before building: available feature
set, limitations, storage, account, privacy, performance, browser compatibility. This
will require a WASM/Compose-for-Web evaluation (STANDARDS §242 methodology) — tracked as
KT-WEB-001 in MASTER_TODO.

---

## MASTER §54 — Android

Current verified: `app/` with `googlePlay` (Firebase analytics/crashlytics, billing,
review) and `fdroid` flavors; SAF file picker with persisted grants; APKG via
`SQLiteDatabase`; WorkManager reminders; controller/keyboard support partial.
Blueprint target: document touch gestures, navigation, media downloads, storage
permissions, background behavior, battery, notifications, controller, keyboard,
WebView/browser, extension possibilities. Platform doc: `docs/platform/ANDROID.md`.

---

## MASTER §55 — Desktop

Current verified: native window shell (44dp drag region, 8-zone resize, system menu,
window-state persistence, rounded corners), Windows media keys, per-OS installers
(Inno/MSI/DMG/AppImage/deb/rpm/Flatpak/Snap). Blueprint requirements: correct title bar
behavior, minimize/maximize/close, hover-visible controls, transparent/custom top bar
where appropriate, safe areas, taskbar avoidance, multi-monitor, DPI scaling, smooth
resizing, responsive layout. Platform docs: `docs/platform/WINDOWS.md`, `MACOS.md`,
`LINUX.md`; installer: `installer/` + `docs/releases/`.

---

## MASTER §56 — Installer / onboarding

Current verified: branded installers (Inno Setup wizard, styled DMG, AppImage/deb/rpm,
Flatpak/Snap manifests), 8-step first-run onboarding wizard (theme/accent/scale/font/
nav/motion), auto-update architecture (channels, sha256, rollback). Blueprint target
adds: database initialization, data import, dataset download, keyboard/controller setup,
media configuration, Anki connection, dictionary setup, optional account, backup — each
as an onboarding step or a documented post-launch flow. See `docs/user-guide/GETTING_STARTED.md`.

---

## MASTER §57 — Branding / assets

Current verified: Kaiteyo brand identity (ADR-0001), brand guidelines
(`docs/branding/`), asset inventory (`docs/architecture/assets.md`), rebranding sweep
from Kanji Dojo. Blueprint rule: when a new branding asset replaces an existing one,
**make a copy, preserve the source, update references, validate dimensions/formats** —
never overwrite the only original (STANDARDS §227). Users/developers can supply logo,
icon, banner, background, app icon, game logo, splash, theme assets; the pipeline copies
and transforms rather than destructively editing.

---

## MASTER §58 — Media + dictionary + mining

The complete workflow — the core architectural pathway:

```
MEDIA → subtitle → select word/phrase → dictionary → Yomitan-style glossary →
pitch/frequency → example → screenshot → audio → card → deck → Kaiteyo →
optional Anki → study → statistics → exam → knowledge graph
```

Verified current end-to-end in the suite (media → subtitle → popup → mine → card) and
documented in `docs/architecture/mining.md`, `docs/media/ASBPLAYER_WORKFLOW.md`,
`docs/integrations/README.md`. Target: the same loop writes **knowledge-graph edges**
(`mined_from`, `appears_in_media`, `encountered_by`).

---

## MASTER §59 — Data synchronization

Spec: `docs/architecture/SYNC.md`. Current verified: GitHub device-flow OAuth + private
gist transport (desktop-first), conflict dialog, offline/retry states. Rules: local data
owned by the user; never destructive sync; import/export/backup always available;
conflict resolution explicit; versioned formats. Anki sync = separate system (MASTER §15).

---

## MASTER §60 — CLI tools

Current verified: **`kaiteyo` developer CLI** (Python, repo root; `tools/cli/`) —
git workflow (status → select → preview → commit → push with safety confirmations),
Gradle command center (discovers real tasks, never invents them — STANDARDS §238),
WSL utilities, doctor (environment diagnostics), docs, info, JSON output for CI. Full
reference: `docs/cli/`. Blueprint additions (target): `database`, `assets`, `media`,
`test`, `format`, `clean`, `status` commands — tracked in MASTER_TODO KT-CLI-*.

---

## MASTER §61 — Testing

Full strategy: `docs/testing/README.md` + `docs/architecture/nodes/TEST_PLAN.md`.
Verified current: unit tests (FSRS, import pipeline, codecs, kjd adapters, SafeArchive
extractor), desktop suite tests (Anki import mapper). Gaps (known): no automated UI
tests, no media/subtitle automated suite, no game tests (game doesn't exist yet).
Blueprint levels: unit, integration, database, migration, UI, snapshot, performance,
media, import/export, end-to-end; acceptance criteria defined per level in TEST_PLAN.

---

## MASTER §62 — Performance engineering

Spec: `docs/architecture/performance.md` + `TEST_PLAN.md` (budget table). Blueprint
rule: define measurable targets — FPS, frame time, startup, navigation, DB queries,
search, memory, background, media playback, animation, world streaming, mobile battery —
and a profiling strategy (JFR/JMC, Android Studio Profiler, GPU profilers; STANDARDS
§189). No "this should be fast" claims.

---

## MASTER §63 — Security

Threat model: `docs/security/README.md` + root `SECURITY.md`. Verified posture:
local-first, no central service; HTML sanitization on imported decks; safe archive
extraction; bearer-token local API; no plugin runtime loading yet (ADR-0011); signed
installers; sha256 release verification; OAuth device flow (no password in app); local
data **not encrypted at rest** (documented limitation). Blueprint rules (STANDARDS §204):
never hardcode secrets, never trust arbitrary imported data, never execute downloaded
code, never disable TLS verification, secure storage for credentials.

---

## MASTER §64 — Privacy

Spec: `docs/security/PRIVACY.md`. Blueprint emphasis: study history, media history,
watched content, mined sentences, statistics, and any location/country data and
Discord/community integrations are **user-owned and opt-in**. The world/country
visualization (MASTER §65) must never transmit location merely because the app can
detect it.

---

## MASTER §65 — World map / social country visualization

**PLANNED/OPT-IN only.** Documented requirements (MASTER §65, STANDARDS privacy rules):
local country detection, manual override, privacy (nothing leaves the device by
default), optional friend comparison, Discord/community representation only with
explicit consent, anonymization. No current implementation.

---

## MASTER §66 — Localization

Spec: `docs/architecture/localization.md`. Current verified: interface-based Strings
(`EnglishStrings`/`JapaneseStrings`), selected by locale; adding a string requires
editing both implementations. Blueprint target: additional app languages; game
localization; **Japanese content must remain linguistically accurate** regardless of
UI language.

---

## MASTER §67 — Accessibility

Spec: `docs/architecture/accessibility.md`. Current verified: reduced motion (motion
presets), UI scale, font size, high-contrast (partial). Gaps: full keyboard navigation,
screen-reader support, game accessibility. Blueprint requirements: reduced motion, font
scaling, high contrast, colorblind considerations, keyboard/controller/touch navigation,
subtitles, audio controls, screen readers where practical, visual alternatives.
Accessibility is a **release gate** for Journey (MASTER §62/TEST_PLAN).

---

## MASTER §68 — Documentation for future AI agents

The agent guide is **`docs/ai/AI_AGENT_GUIDE.md`** (this pass). Combined with
`docs/development/AI_CONTEXT.md` (workflow + never-change list) and
`docs/development/VIBE_CODING_GUIDE.md`. Required instructions: read README → docs/README
→ architecture → current-state audit → relevant subsystem docs → TODO → inspect actual
code; never assume docs are correct if code contradicts them; update docs after
architectural changes; never mark unfinished work complete without testing; preserve
existing functionality; avoid unnecessary rewrites; keep architecture consistent; add
tests; record decisions. Task selection procedure: see the guide + MASTER_TODO.

---

## MASTER §69 — Documentation change rules

Whenever architecture changes, update in the same change: architecture docs, relevant
subsystem docs, TODO, ADR, README if relevant, dependency docs, migration docs. **No
architecture drift.** Enforced by `docs/development/DocumentationRules.md` and the
Definition of Done in `AGENTS.md`.

---

## MASTER §70 — Game development roadmap

Full staging in `docs/production/phases.md` + the Journey stages in `docs/game/README.md`: Stage 0 architecture → 1 prototype
player → 2 camera/input → 3 small environment → 4 interaction → 5 NPC → 6 dialogue →
7 language-knowledge integration → 8 quests → 9 curriculum → 10 save system → 11 world
streaming → 12 first polished region (Kamakura + Enoshima) → 13 media/learning
integration → 14 Android/controller → 15 larger world → 16 additional regions → 17 live
content pipeline. **Do not pretend all of Japan can be built instantly** (NODE §90).

---

## MASTER §71 — Product roadmap

`docs/roadmap/ROADMAP.md` is the product roadmap (v2.2.1 shipped; v2.3 in progress).
MASTER_TODO carries per-area roadmaps (core app, dictionary, learning, library,
statistics, exams, media, mining, integrations, website, Android, desktop, game,
content, data, infrastructure) as work packages P0–P39.

---

## MASTER §72 — Content pipeline

Spec: `docs/architecture/content.md` + `docs/architecture/nodes/CONTENT_AUTHORING.md`
(ADR-0015). Content types: dictionary data, courses, lessons, vocabulary, grammar,
sentences, stories, dialogue, quests, NPCs, locations, game activities, exams, media
metadata — each with a documented schema and 6 hard validation gates (ADR-0015, NODE
§148). The worked vertical slice (`JOURNEY_SLICE_CONTENT.md`) is the reference
implementation of every schema.

---

## MASTER §73 — Versioning

Documented versioning for: application (`buildSrc/AppVersion.kt` — single source),
database (SQLDelight schema versions + migrations), datasets (kjd per-dataset versions +
checksums), game world (content packages, ADR-0015), courses (content packages), assets
(declared in `AppAssets.kt`), APIs (local API versioning). See `docs/database/MIGRATIONS.md`
and `docs/releases/`.

---

## MASTER §74 — Migrations

Full policy: **`docs/database/MIGRATIONS.md`** (this pass). Rules (STANDARDS §180,
MASTER §74): every schema change versioned; never silently destroy user data; preserve
compatibility; backup before destructive migration; validate migrations; rollback where
possible. Current verified: `UserDataDatabase` has versioned migrations; desktop applies
kjd patch feeds incrementally.

---

## MASTER §75 — API architecture

Internal service boundaries are defined in `docs/architecture/nodes/SERVICE_CONTRACTS.md`
(22 service interfaces) and `docs/api/README.md`. No microservices — the app is
local-first; services are in-process Koin modules behind stable interfaces (STANDARDS
§209). The local HTTP API (bearer-token, versioned endpoints) is documented in
`docs/integrations/LOCAL_API.md`.

---

## MASTER §76 — Plugin / extension architecture

Spec: `docs/integrations/PLUGINS.md` + ADR-0011. Current: registry + marketplace
scaffold; **no runtime loading** (security-first). Blueprint target: dictionary plugins,
media plugins, importers, exporters, themes, content packs, game content packs — with a
documented sandbox/capability model before any loading is enabled.

---

## MASTER §77 — Data packs

Downloadable data packages (target): Japanese dictionary pack, kanji pack, furigana
pack, frequency pack, pitch pack, JLPT pack, grammar pack, course pack, game world pack.
Users must **not** be forced to download everything — the app ships the core reference
data (AppDataDatabase asset) and optional packs load on demand (ADR-0015 package format,
`CONTENT_AUTHORING.md`).

---

## MASTER §78 — Offline-first behavior

Current verified: dictionary, kanji, vocabulary, library, study, statistics, exams,
local media, local mining all work offline. Target: game content already downloaded
works offline (content packages are local data). Network-dependent features are clearly
identified (sync, auto-update, community). See `docs/architecture/OVERVIEW.md` and
STANDARDS §182.

---

## MASTER §79 — Release engineering

Spec: `docs/releases/RELEASE_PROCESS.md` + `docs/architecture/ci-cd.md`. Current:
tag-based CI (build-all → stage-and-verify with sha256 integrity gate → create-release),
channels (stable/beta/nightly update feeds), signed installers, Android flavors,
reproducible F-Droid builds. Blueprint: development → alpha → beta → stable channels;
versioning; CI; artifacts; installers; rollback.

---

## MASTER §80 — Final master system diagram

```
                          KAITEYO
                             │
   ┌──────────┬──────────────┼──────────────┬──────────────┐
   │          │              │              │              │
   UI      NAVIGATION    DICTIONARY    KNOWLEDGE       LEARNING
 (design/, (NAVIGATION. (dictionary.md,  GRAPH       (study-engine.md,
  design/   md, NODE      kjd data)    (NODE nodes/,   library, curriculum)
  system)   §133–135)                   language-model)
   │          │              │              │              │
   ├──────────┴──────────────┼──────────────┴──────────────┤
   │                  LIBRARY │  STATS   │ EXAMS            │
   │              (features/  │(statistics│(exams.md)       │
   │               LIBRARY.md)│  .md)     │                  │
   ├──────────┬──────────────┴──────────────┬──────────────┤
   MEDIA    MINING     ANKI/YOMITAN        GAME (TARGET)   WORLD (TARGET)
 (media.md, (mining.md)  (integrations/)   (docs/game/README.md, (docs/game/world-architecture.md,
  suite)    suite)      suite + core      NODE §86+)       NODE §88+)
   │          │              │              │              │
   └──────────┴──────────────┴──────────────┴──────────────┘
                          │
                      DATABASE
              (SQLDelight AppData/UserData, DataStore,
               suite JSON; node/edge target — ADR-0013)
                          │
                  DATA INGESTION (kjd)
                          │
             PLATFORM (desktop/Android/iOS) + INTEGRATIONS
```

Key flows (NODE §160): `MEDIA → SUBTITLE → WORD → DICTIONARY → KNOWLEDGE → CARD →
LIBRARY → REVIEW → STATS` and `WORLD → OBJECT → LANGUAGE → DISCOVERY → KNOWLEDGE →
JOURNEY → STATS`. This graph is the conceptual center of the project.

---

## MASTER §81 — Final master TODO

**`docs/planning/MASTER_TODO.md`** — hierarchical, actionable, with `[ ]` checkable items,
IDs, status, priority, description, dependencies, acceptance criteria, testing and
documentation requirements. Built from the actual architecture (no invented task counts).

---

## MASTER §82 — Documentation quality standard

Final docs must be: internally consistent, cross-linked, searchable, structured,
versionable, maintainable, implementation-oriented, technically specific, honest about
unknowns, useful to humans and coding agents. **Banned phrases** (they are bugs):
"etc.", "and so on", "implement similarly", "follow best practices", "future expansion",
"add more features later". Whenever something matters, document it.

---

## MASTER §83 — What must not be done

Never: compile/build via Gradle as part of a docs pass; generate thousands of meaningless
lines; create fake implementations or placeholder files just to inflate counts; claim
code works without verification; overwrite working code unnecessarily; redesign
architecture without documenting why; remove features without recording the decision;
replace real datasets with fake data; create fake statistics; fabricate external API
behavior; assume licenses; copy proprietary code; scrape proprietary services; claim
compatibility without verification. **This phase is documentation and architecture.**
Inspection is allowed and encouraged; building is not.

---

## MASTER §84 — Output requirements

The blueprint's requested document set and its mapping to the repository is in
MASTER §3 (table). The deliverables produced or updated by this pass are listed in
`docs/README.md` and tracked as KT-DOC-* in MASTER_TODO. Existing documents are improved
rather than duplicated.

---

## MASTER §85 — README

Root `README.md` is the entrance — what Kaiteyo is, why it exists, core features,
architecture, supported platforms, data sources, media, mining, learning, game,
development, documentation, status, roadmap, licenses/attributions, contributing. It is
**not** a 500-page document: the README is the entrance; the docs are the mansion.

---

## MASTER §86 — Documentation cross-linking

Every major document must link to: parent, related documents, architecture,
implementation, TODO, relevant ADR. No isolated documentation islands. Enforced by the
link-check in the final audit (MASTER §87) and `docs/development/DocumentationRules.md`.

---

## MASTER §87 — Final audit

Before finishing any documentation pass, run the audit. The checklist and its current
answers are in `docs/planning/CURRENT_STATE.md` §Audit and re-verified at the end of
this pass (§Final audit results in this file's companion docs). The 34 questions cover:
every subsystem documented, every dependency documented, every dataset documented,
licenses documented, current-vs-planned separated, game as a real game, media center as
a major subsystem, mining/Yomitan/Anki/AnkiConnect/knowledge-graph/statistics/exams/
library/Android/desktop/web/controls/world/streaming/assets/curriculum/children's world
documented, master TODO actionable, and the "can a future AI understand what is
implemented / what remains / where to start" questions.

---

## MASTER §88 — Final instruction

Treat this repository as the beginning of a serious, long-term software product.
Documentation must **be** useful for building the product — not merely look professional.
A large team of humans and coding agents must be able to build Kaiteyo coherently for
years without losing the original vision: every document answers WHAT to build, WHY,
WHERE, HOW IT CONNECTS, WHAT DEPENDS ON IT, WHAT IT MUST NOT BREAK, HOW TO TEST IT, HOW
TO MEASURE IT, HOW TO DOCUMENT IT, and WHAT TO BUILD NEXT.

---

## Master blueprint — implementation roadmap summary

The blueprint is the wrapper; the build order is the dependency graph from
`STANDARDS §365` (PHASE 0–28), re-ordered against actual repo state in
`docs/planning/TODO.md` and inventoried in `docs/planning/MASTER_TODO.md`. The two
decisions that gate the future: **one-product consolidation** (ADR-0017) and **game
engine selection** (ADR-0018).

**Next steps for any contributor**: read `docs/ai/AI_AGENT_GUIDE.md` → open
`docs/planning/MASTER_TODO.md` → pick the highest-priority task whose dependencies are
`DONE` → implement per `docs/engineering/ENGINEERING_STANDARDS.md`.
