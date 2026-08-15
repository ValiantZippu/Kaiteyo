# Kaiteyo — Engineering Audit & Agent Handoff

**Date**: 2026-08-15
**Status**: living reference — refresh when architecture changes; keep in sync with
`docs/README.md`, `PRODUCT_AUDIT.md`, and `CURRENT_ISSUES.md`.
**Purpose**: this document satisfies the §376 stop-condition deliverables of the
[Kaiteyo Engineering Standards](../engineering/ENGINEERING_STANDARDS.md) (adopted by
[ADR-0012](../architecture/decisions/0012-engineering-standards.md)):

1. repository audit
2. architecture map
3. technology decisions
4. dependency map
5. implementation order
6. risks
7. unresolved questions
8. first implementation milestone
9. exact files/modules future agents should begin with

**Contract**: the next implementation agent starts here, reads the referenced documents,
and follows the standard's 10-phase workflow (§173). Architecture is the contract; code
must obey it.

---

## 1. Repository audit

Detailed product audit (what is real, duplicated, dead, or fake, verified by reading code):
**`docs/planning/PRODUCT_AUDIT.md`**. Summary of the state at this pass:

- **The two-app problem is the headline.** `desktopApp/Main.kt` `main()` ships the product
  (`KaiteyoApp`, shared Compose MPP across desktop/Android/iOS). `SuiteMain.kt`
  `desktopSuiteMain()` builds the JVM-only desktop suite (`ua.syt0r.kanji.desktop.*`) with a
  second copy of SRS, settings, statistics, navigation, decks, dictionary, media, mining,
  and OCR. **Progress since the audit**: Media is now a first-class core destination
  (`MainDestination.Media` hosts the suite's `MediaView` in the shipped app). **Remaining**:
  suite Dictionary manager, OCR, Browser, and Reading views are not core destinations; the
  data-layer duplication (SRS/settings/statistics/nav/decks) still awaits the consolidation
  decision (§7).
- **Confirmed real and healthy**: `kjd/` data platform (ingest → normalize → validate →
  SQLite export; provenance kept), `AppDataDatabase` (bundled read-only), FSRS-5 SRS with
  tests, writing stroke evaluator, `.apkg` import/export (JVM/Android/iOS), import/export
  pipeline, GitHub sync/account, statistics pipeline, deck features, window shell.
- **Dead/shadow code (removal candidates, not yet removed)**: `LearningPowerHub.kt` (+
  friends), `SyncSettingsUI.kt`, the `BackupSystemExt.kt` path reachable only through the
  dead hub. See `PRODUCT_AUDIT.md` §5.2.
- **Fake-data cleaned**: demo seeding no longer fabricates SRS state or 180-day statistics;
  `BackupVerifier` computes real SHA-256. See `PRODUCT_AUDIT.md` §5.
- **Platform status**: Android real; iOS code-complete but build-verified only; Windows/
  Linux desktop runtime sweeps pending (list in `CURRENT_ISSUES.md` → BLOCKED).

## 2. Architecture map

Canonical overviews: **`docs/architecture/OVERVIEW.md`** (modules, UI architecture, data
flow) and **`docs/architecture/FILE_STRUCTURE.md`** (repository layout). Quick map:

| Module | Role |
|---|---|
| `core/` | All shared code (KMP): `presentation/` (Compose MPP UI), `core/` (data: `app_data`, `user_data`, `srs`, `sync`, …), `di/` (Koin). `commonMain` / `jvmMain` / `androidMain` / `iosMain` |
| `desktopApp/` | Thin JVM shell (`Main.kt` → the product) + the desktop suite (`ua.syt0r.kanji.desktop.*`): `engine/` (dictionary, media, mining, ocr, review, sync, search, transfer, theming, history), `ui/`, `designsystem/` (`Ds*`), `appstate/`, `model/` |
| `app/` | Android entry point; flavors `googlePlay` / `fdroid` |
| `iosApp/` | iOS entry point (Swift + Compose host) |
| `kjd/` | Standalone data platform (JVM): datasets → bundled language database + desktop incremental patches |
| `mediaGenerator/` | JVM asset generation (javacv + coil) |
| `installer/` | Branded installer subsystem (not a Gradle module) |
| `website/` | Static site (Python build, consumes `docs/`) |
| `buildSrc/` | Gradle logic: `AppVersion.kt`, `AppAssets.kt` |
| `desktopApp/…/engine/plugin/` | Plugin registry + marketplace scaffold (JVM-only; runtime loading deferred — ADR-0011) |

Key conventions: 4-file screen pattern (Contract / ViewModel / Module / UI) with Koin
`multiplatformViewModel`; screens registered in `core/.../di/AppModule.kt`; interface-based
`Strings` localization (English + Japanese implementations); two SQLDelight databases
(migrations versioned under `commonMain/sqldelight_user_data/migrations/`).

## 3. Technology decisions

Decision records (with Context/Decision/Alternatives/Consequences): **`docs/architecture/
decisions/README.md`** — index of ADR-0001…0018 (Accepted = implemented in code; Proposed = decided but not yet implemented — see §158, do not treat as shipped):

| ADR | Decision |
|---|---|
| 0001 | Kaiteyo brand identity (fork of Kanji Dojo) |
| 0002 | Token-based theme system |
| 0003 | Kotlin Multiplatform + Compose Multiplatform for all platforms |
| 0004 | Shared UI in `core` + 4-file screen pattern + Koin DI |
| 0005 | Two SQLDelight databases (immutable app data / mutable user data) |
| 0006 | FSRS-5 spaced repetition |
| 0007 | KJD standalone data platform |
| 0008 | Desktop immersion suite as a self-contained module |
| 0009 | GitHub device-flow OAuth + private-gist sync (no central service) |
| 0010 | Installer subsystem decoupled from the Gradle build |
| 0011 | Plugin runtime loading deferred (security first) |
| 0012 | Professional engineering standards adopted (§163–§376) |
| 0013 | Node-based architecture as the connective tissue (§76–§162) |
| 0014 | Journey as target architecture (separate runtime, vertical slice first) |
| 0015 | Data-driven content authoring with hard validation gates |

Open technology decisions (not yet made — see §7): one-product consolidation, Journey game
engine (§242 mandates an evaluation, not adoption), embedded-browser backend.

## 4. Dependency map

Source of truth: `gradle/libs.versions.toml` + module `build.gradle.kts` files + plugin
versions in `settings.gradle.kts` (literal there — keep in sync with the catalog). Records
of changes: `docs/maintenance/DependencyUpdates.md`.

### Pinned toolchain
JDK 17 (`jvmToolchain(17)` everywhere) · Kotlin 2.1.20 (language/api version pinned to
KOTLIN_2_1) · Compose Multiplatform 1.8.2 · AGP 8.5.2 · SQLDelight 2.0.2 · Koin 4.0.0 ·
Ktor 3.1.2 · kotlinx-serialization 1.8.0 · kotlinx-datetime 0.6.1.

### By module
| Module | Key dependencies |
|---|---|
| `core` (commonMain) | Compose MPP 1.8.2 (+ material3-window-size-class), Koin (core/compose/viewmodel), Ktor client (core/cio/auth; darwin on iOS), SQLDelight drivers (android/jvm/native), kotlinx-serialization-json, kotlinx-datetime, Wanakana 1.1.1, coil3, AboutLibraries 12.0.0, reorderable 2.4.3 |
| `core` (androidMain) | DataStore preferences 1.1.1, AndroidX lifecycle/navigation/activity/appcompat/core-ktx, WorkManager 2.9.1, Media3 ExoPlayer 1.4.1 |
| `app` (googlePlay only) | Firebase BOM (analytics, crashlytics), Play billing 7.1.1, Play review 2.0.1 |
| `desktopApp` (jvmMain) | `:core`, `:kjd`, Ktor server (core/netty — localhost API), JNA 5.14.0 (+ platform — native window drag), vlcj 4.8.2 (VLC playback backend; GPL-3 compatible). mpv backend is IPC-based (no library); audio via Java Sound; OCR via Tesseract |
| `kjd` | kotlinx-serialization-json, sqlite-jdbc 3.46.0.0, kotlin-reflect |
| `mediaGenerator` | javacv-platform 1.5.11, coil |
| Tests | kotlin-test (JUnit platform), mockk 1.13.16 |

### Policy notes
- No reinvention of infrastructure (§164): HTTP = Ktor, JSON = kotlinx-serialization,
  database = SQLDelight/SQLite, media = VLC/mpv/Java Sound behind the `MediaEngine`
  abstraction, crypto = platform/verified pure-Kotlin SHA-256 where `MessageDigest` is
  unavailable (commonMain).
- VLCJ is licensed GPL-3.0 and compatible with Kaiteyo's license (comment in
  `desktopApp/build.gradle.kts`); the app degrades gracefully when VLC is not installed.
- Dependency additions must clear §203 before landing and be recorded in
  `docs/maintenance/DependencyUpdates.md`.

## 5. Implementation order

The §365 phase dependency graph mapped onto Kaiteyo's actual state (roadmap:
`docs/roadmap/ROADMAP.md`; task list: `docs/planning/TODO.md`). Status reflects what is
real and shipped in the product (or suite engines reachable through the product):

| §365 phase | Status | Notes |
|---|---|---|
| 0 Repository stabilization | ✅ done | packaging, installer, versioning in place |
| 1 Toolchain | ✅ done | pinned versions; `docs/development/DEVELOPMENT_SETUP.md` |
| 2 Design system | ✅ done | `docs/design/` + tokens; suites' `Ds*` |
| 3 Core domain | ✅ done | learning models, SRS, kana/kanji/vocab domains |
| 4 Database | ✅ done | two SQLDelight DBs, versioned migrations (ADR-0005) |
| 5 Data ingestion | ✅ done | `kjd/` pipeline (ADR-0007) |
| 6 Knowledge graph | 🟡 partial | knowledge profile engine exists; full graph is future |
| 7 Dictionary/search | 🟡 partial | `AppDataDatabase` lookup + suite `DictionaryService`; FTS/trigram indexing is future |
| 8 Kanji/Kana/Vocabulary | ✅ done | + Kana content system (suite) |
| 9 User knowledge | ✅ done | `KnowledgeProfileEngine` (suite), study status on detail pages |
| 10 Library/decks/cards | ✅ done | unified learning store (suite) + core Library hub |
| 11 Review scheduler | ✅ done | FSRS-5 (ADR-0006) |
| 12 Statistics/events | ✅ done | event-driven statistics repository |
| 13 Exams | ✅ done | ExamEngine + JLPT simulation |
| 14 Media abstraction | ✅ done | `MediaEngine` with VLC/mpv/Java Sound backends |
| 15 Subtitle engine | ✅ done | SRT/ASS/SSA/VTT independent of backend |
| 16 Mining | ✅ done | `MiningEngine` + `MiningPayload` |
| 17 Anki/Yomitan integrations | 🟡 partial | `.apkg` + AnkiConnect done; AnkiConnect e2e unverified |
| 18 Home/Browse/Library/Stats UX | ✅ done | plus adaptive responsive pass |
| 19 Navigation/floating/launchpad | ✅ done | `NavShell` rebuilt (floating + sidebar) |
| 20 Embedded browser/media workflows | 🟡 partial | media workflows integrated; browser workspace not a core destination |
| 21–26 Journey (data model → content authoring) | ⬜ not started | spec: `docs/architecture/NODE_ARCHITECTURE.md` (§76–§162) + `nodes/`; ADR-0013/0014/0015; engine evaluation first (§242) |
| 27 Cloud/sync | 🟡 partial | GitHub sync shipped; conflict UX evolving |
| 28 Release engineering | ✅ done | installers, update feeds, release workflow |

**Ordering rule**: finish the open P0/P1 consolidation and v2.3 milestone (§8) before
starting new phases; Journey work (§21+) begins only after the game-engine evaluation.

## 6. Risks

| Risk | Severity | Mitigation / owner |
|---|---|---|
| Two-app consolidation decision stays open — duplicate data systems drift further | 🔴 high | Decide per §7-1; PRODUCT_AUDIT §1; track in CURRENT_ISSUES #11 |
| Platform verification gaps (iOS/Windows/Linux runtime paths) | 🟡 | BLOCKED list in CURRENT_ISSUES; needs hardware/CI |
| Desktop polish P0 (animation stutter, resize glitches) | 🟡 | CURRENT_ISSUES #1–4; perf budgets §190 |
| Media backend external dependency (VLC) | 🟡 | graceful degradation exists; backend abstraction |
| Dead/shadow code confusing agents (LearningPowerHub etc.) | 🟡 | remove per PRODUCT_AUDIT §5.2 after consolidation decision |
| Docs drift (code says A, docs say B) | 🟡 | DocumentationRules.md; §336 freshness rule; refresh this audit on architecture changes |
| Website `dist/` committed, manual build step | 🟢 | consider CI regeneration (TODO.md → TECHNICAL DEBT) |

## 7. Unresolved questions

1. **One-product decision** — make the suite the desktop product (porting its engines onto
   the core SQLDelight data model) vs keep the core app as the product and integrate suite
   engines into its destinations. Gates the data-layer consolidation (PRODUCT_AUDIT §1, §8).
   **Now tracked as ADR-0017 (Proposed) + `docs/planning/MASTER_TODO.md` KT-INFRA-001.**
2. **Journey game engine** — perform the mandated technical evaluation (§242): Godot vs
   Unity vs Unreal for the stylized cross-platform Journey; embed strategy and the
   JourneyService ↔ runtime boundary (§243–§244). Nothing ships until the evaluation is
   documented. **Now tracked as ADR-0018 (Proposed) + KT-GAME-001; no Journey code before
   acceptance.** Target architecture is specified (ADR-0014, `docs/architecture/nodes/`);
   it is not implementation (NODE §158).
3. **Embedded browser backend** — CEF vs WebView2 vs Android WebView; extension
   compatibility assumptions (§198, §360). RESEARCH.
4. **Open datasets for grammar, pitch accent, example sentences** — licensing verification
   before ingestion (§183–§185). RESEARCH (TODO.md). Gates several LANGUAGE-family
   node types (NODE_TYPE_REGISTRY §1).
5. **Plugin sandbox design** — capability model, subprocess vs classloader (ADR-0011
   deferred; §261–§262).
6. **AnkiConnect e2e verification** — needs a live Anki instance (BLOCKED).
7. **Mobile sync UX** beyond desktop-first; conflict handling maturity (§270–§271).

## 8. First implementation milestone

**v2.3 — Anki interoperability & persistent data** (in progress; most is implemented):

- [x] Persistent desktop card pool; real `.apkg` import/export; unified import/export pipeline
- [ ] Filter archived decks out of main lists + "Archived" restore section (TODO.md P1)
- [ ] Release v2.3

Next milestone candidates (vertical slices, §322): bring the suite's dictionary popup →
mining → card loop into the product as a complete slice (search → result → entry detail →
knowledge → card → stats), and close the dead-code removal + data-layer consolidation
opened by the §7-1 decision.

## 9. Starting files for the next agent

Read first, in order:

1. `docs/product/PRODUCT.md` — the Master Blueprint (§0–§88) + `docs/planning/CURRENT_STATE.md` — what exists, with statuses.
2. `docs/ai/AI_AGENT_GUIDE.md` — the binding agent workflow (read order, rules, task selection).
3. `docs/engineering/ENGINEERING_STANDARDS.md` — the contract (§163–§376).
4. `docs/development/AI_CONTEXT.md` — workflow + the "never change" list.
5. `docs/development/COMMANDS.md` — real commands (build/test/install).
6. `docs/planning/CURRENT_ISSUES.md`, `docs/planning/TODO.md`, `docs/planning/MASTER_TODO.md` — what needs doing.
7. `docs/planning/PRODUCT_AUDIT.md` — what is real/duplicated/dead/fake.
8. `docs/architecture/OVERVIEW.md`, `docs/architecture/FILE_STRUCTURE.md`,
   `docs/architecture/decisions/README.md` — how the system is built and why.
9. `docs/architecture/NODE_ARCHITECTURE.md` + `docs/architecture/nodes/` — the target
   node system, knowledge model, and Journey world specs (§76–§162, ADR-0013/0014/0015);
   **target architecture only — do not treat as implemented** (§158).
10. `docs/testing/README.md` — validation ladder targets.
11. `docs/maintenance/DependencyUpdates.md` + `gradle/libs.versions.toml` — dependencies.

Then inspect code: `desktopApp/Main.kt` (product entry) and `desktopApp/SuiteMain.kt`
(dev suite entry) · `core/.../presentation/KaiteyoApp.kt` · `core/.../di/AppModule.kt` ·
`core/.../srs/fsrs/` · `desktopApp/.../engine/` (dictionary, media, mining, learning) ·
`desktopApp/.../appstate/AppState.kt`.

## 10. §373 documentation inventory (handoff state)

Every deliverable named in the standard's §373 "final handoff state" exists in the
repository and is indexed from `docs/README.md` (no orphans, §335). "Content" = the
spec is written and honest (status-labeled); implementation status is per §5 and
`docs/planning/PRODUCT_AUDIT.md`.

| §373 deliverable | Document(s) | Content |
|---|---|---|
| ARCHITECTURE | `docs/architecture/OVERVIEW.md` + `README.md` (index) | ✅ |
| DESIGN SYSTEM | `docs/design/DESIGN_SYSTEM.md` (+ `design/README.md`) | ✅ |
| DATABASE SPECIFICATION | `docs/architecture/database.md` | ✅ |
| KNOWLEDGE GRAPH | `docs/architecture/language-model.md` §4 + `nodes/KNOWLEDGE_STATE_MODEL.md` + `NODE_ARCHITECTURE.md` | ✅ (profile real; entity graph target) |
| LANGUAGE MODEL | `docs/architecture/language-model.md` | ✅ |
| DICTIONARY SPECIFICATION | `docs/architecture/dictionary.md` | ✅ |
| STUDY ENGINE | `docs/architecture/study-engine.md` | ✅ |
| CARD MODEL | `docs/architecture/study-engine.md` §2 (note → card type → card) | ✅ |
| STATISTICS MODEL | `docs/architecture/statistics.md` | ✅ |
| EXAM MODEL | `docs/architecture/exams.md` | ✅ |
| MEDIA ARCHITECTURE | `docs/architecture/media.md` | ✅ |
| SUBTITLE ARCHITECTURE | `docs/architecture/media.md` §4 | ✅ |
| MINING ARCHITECTURE | `docs/architecture/mining.md` | ✅ |
| ANKI INTEGRATION | `docs/integrations/ANKI.md` + `architecture/integrations.md` §2 | ✅ |
| YOMITAN INTEGRATION | `docs/integrations/YOMITAN_DICTIONARIES.md` | ✅ |
| BROWSER ARCHITECTURE | `docs/architecture/browser.md` | ✅ (planned) |
| JOURNEY ARCHITECTURE | `docs/architecture/journey.md` + ADR-0014 | ✅ (target) |
| WORLD ARCHITECTURE | `nodes/JOURNEY_WORLD_SCHEMA.md` | ✅ (target) |
| GAME RUNTIME PLAN | `nodes/JOURNEY_RUNTIME_SPEC.md` + `journey.md` | ✅ (target) |
| NODE SYSTEM | `docs/architecture/NODE_ARCHITECTURE.md` + `nodes/` (registries, knowledge model, world schema, gameplay systems, runtime, authoring, UX flows, test plan — ADR-0013/0016) | ✅ (target) |
| CONTENT SYSTEM | `docs/architecture/content.md` + `nodes/CONTENT_AUTHORING.md` (ADR-0015) | ✅ (foundation + planned) |
| PLUGIN SYSTEM | `docs/integrations/PLUGINS.md` (ADR-0011) | ✅ (scaffold, deferred) |
| ASSET SYSTEM | `docs/architecture/assets.md` + `docs/assets/ASSETS.md` | ✅ |
| TOOLCHAIN | `docs/architecture/toolchain.md` + `development/DEVELOPMENT_SETUP.md` | ✅ |
| TEST STRATEGY | `docs/testing/README.md` | ✅ |
| PERFORMANCE STRATEGY | `docs/architecture/performance.md` | ✅ |
| SECURITY STRATEGY | `docs/security/README.md` + root `SECURITY.md` | ✅ |
| LICENSE STRATEGY | `docs/legal/README.md` + `data/SOURCES.md` | ✅ |
| CI/CD STRATEGY | `docs/architecture/ci-cd.md` | ✅ |
| RELEASE STRATEGY | `docs/releases/RELEASE_PROCESS.md` + `RELEASE_CHECKLIST.md` | ✅ |
| DEVELOPER CLI PLAN | `docs/cli/` (`README.md`, `COMMANDS.md`, `ARCHITECTURE.md`) | ✅ (CLI exists; `dev doctor` planned) |
| DATABASE MIGRATION PLAN | `docs/architecture/database.md` §5 | ✅ |
| BACKUP PLAN | `docs/architecture/backup.md` | ✅ |
| SYNC PLAN | `docs/architecture/SYNC.md` + ADR-0009 | ✅ |
| ACCESSIBILITY PLAN | `docs/architecture/accessibility.md` | ✅ (partial implementation) |
| LOCALIZATION PLAN | `docs/architecture/localization.md` | ✅ |
| TODO | `docs/planning/TODO.md` | ✅ |
| ROADMAP | `docs/roadmap/ROADMAP.md` | ✅ |
| ADR INDEX | `docs/architecture/decisions/README.md` (ADR-0001…0018) | ✅ |
| CHANGELOG | root `CHANGELOG.md` | ✅ |

Validation rule (§341–§342): documentation/planning passes do not build; code changes use
the cheapest useful validation level first, full builds only when justified.
