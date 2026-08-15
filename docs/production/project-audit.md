# Project Audit (§63)

**Status**: LIVE — refresh on architecture changes. **Purpose**: the §63 final
audit. It answers *what exists, what partially exists, what is broken, what is
missing, what is duplicated, what is architecturally weak, what is good, what
should not be touched, what should be refactored, what should be replaced, what
should be built next, dependency order* — plus the **TOP-100 tasks** and
**TOP-100 risks / issues**.

Sources: `docs/planning/PRODUCT_AUDIT.md` (code-level truth), `ENGINEERING_AUDIT.md`
(§376 handoff), `CURRENT_ISSUES.md` (issues), `TODO.md` (tasks), this corpus.

---

## 1. WHAT EXISTS (real, shipped, verified by reading code)

| System | Evidence | Status |
|---|---|---|
| KMP/CMP app (desktop/Android/iOS) | `core/` + `desktopApp/` + `app/` + `iosApp/` | ✅ STABLE |
| Two SQLDelight DBs + versioned migrations | ADR-0005; `sqldelight_user_data/migrations/` | ✅ STABLE |
| KJD data platform | ingest → normalize → validate → SQLite export; provenance | ✅ STABLE |
| FSRS-5 SRS with tests | ADR-0006; `srs/fsrs/`; `FsrsSchedulerTest` | ✅ STABLE |
| Study engine (notes→cards→stages) | `engine/learning/` (suite) + core study | ✅ FUNCTIONAL |
| Writing practice + stroke evaluation | built-in + KanjiVG (honest source label) | ✅ FUNCTIONAL |
| Exams + JLPT simulation | `ExamEngine`, ExamView, analytics | ✅ FUNCTIONAL |
| Event-driven statistics | `StatisticsRepository`; heatmap; ActivityTracker (AFK) | ✅ FUNCTIONAL |
| Knowledge profile (honest, labeled) | `KnowledgeProfileEngine` | ✅ FUNCTIONAL |
| Media Centre (core destination) | VLC/mpv/Java Sound; subtitles; mining; dictionary popup | ✅ FUNCTIONAL |
| Subtitle engine (SRT/ASS/SSA/VTT) | independent of backend | ✅ FUNCTIONAL |
| Mining engine | `MiningPayload`; sources; duplicate protection | ✅ FUNCTIONAL |
| Anki interop | `.apkg` import/export (JVM/Android/iOS); AnkiConnect transport; local API | ✅ FUNCTIONAL (e2e BLOCKED) |
| Dictionary (bundled + suite) | AppDataDatabase + `DictionaryService`/Importer (Yomitan formats) | ✅ FUNCTIONAL |
| Library/decks/collections/kana | unified learning store; Collections as deck containers; full kana system | ✅ FUNCTIONAL |
| Sync/backup/account | GitHub gist sync (ADR-0009); backup archive + verifier (real SHA-256) | ✅ FUNCTIONAL |
| Theme system | 17 presets; Theme Studio; token architecture (ADR-0002) | ✅ STABLE |
| Navigation | NavShell floating + sidebar; launchpad; compact tiers | ✅ FUNCTIONAL |
| Window experience | custom chrome, resize, DPI, persistence (v2.2.1) | ✅ FUNCTIONAL |
| Installers + update feeds | Inno/DMG/AppImage/deb/rpm; update manifests | ✅ STABLE |
| CLI (`kaiteyo`) | `tools/cli/` documented in `docs/cli/` | ✅ FUNCTIONAL |
| Tests | 50+ test files (core commonTest + desktop jvmTest) | ✅ GROWING |
| Documentation corpus | ~1.1M chars, fully indexed, 0 orphans, 0 broken links (last check) | ✅ |

## 2. WHAT PARTIALLY EXISTS

| System | What's real | What's missing |
|---|---|---|
| Dictionary search | lookup real | FTS/trigram indexing at scale |
| Knowledge graph | profile engine real | node/edge layer (ADR-0013) |
| Media workflows in product | Media destination real | browser/OCR/reading not core destinations |
| Integrations | apkg/AnkiConnect code real | AnkiConnect e2e; grammar/pitch/Tatoeba datasets |
| Sync | desktop-first real | cross-device conflict maturity |
| Accessibility | partial (theme contrast, reduced motion exist) | full keyboard nav, screen reader, high contrast completeness |
| Reading | practice mode real | graded reading mode, story content |
| Auto-update | architecture complete | end-user rollout |

## 3. WHAT IS BROKEN / AT RISK (open issues, not fixed)

| Issue | Where | Severity |
|---|---|---|
| Animation stutter (hover/theme/window move) | desktop polish | 🔴 P0 |
| Resize glitches (panels jump, spacing) | desktop polish | 🔴 P0 |
| Hover animation inconsistency | desktop | 🟡 |
| Spacing/radius inconsistency (4dp grid) | desktop | 🟡 |
| Archived decks visible in main lists; no restore UI | Library | 🟡 P1 |
| Settings scattered appearance options | Settings | 🟡 |
| Mobile nav lacks snap behavior | mobile | 🟡 |
| OCR missing-engine UX is a hint, not guided | OCR | 🟢 |
| iOS/Windows/Linux/Android runtime paths unverified | platform | 🔴 release gate |

## 4. WHAT IS MISSING (target systems with no implementation)

| Missing | Spec | Gate |
|---|---|---|
| Node layer (ADR-0013) | NODE_ARCHITECTURE + nodes/ | storage decision |
| User knowledge model per ADR-0016 | KNOWLEDGE_STATE_MODEL | events first |
| Journey engine (no ADR yet) | JOURNEY_RUNTIME_SPEC | §242 evaluation |
| Journey world content (packages, slice) | JOURNEY_WORLD_SCHEMA, CONTENT_AUTHORING | content pipeline |
| Curriculum engine (graph-based) | learning/curriculum-engine.md | foundations |
| Adaptive/recommendation engine | learning/adaptive-learning.md | knowledge model |
| Graded reading + stories | learning/reading-stories.md, content/ | content pipeline |
| Kanji graph traversal ("where have I seen this") | learning/kanji-knowledge-graph.md | node layer |
| Children mode | vision/child-experience.md, GAMEPLAY_SYSTEMS §22 | after slice |
| Browser workspace as core destination | architecture/browser.md | backend decision |
| Grammar/pitch-accent/Tatoeba datasets | data/SOURCES.md | licensing RESEARCH |

## 5. WHAT IS DUPLICATED

| Duplication | Detail | Action |
|---|---|---|
| SRS/settings/statistics/nav/decks in core app AND desktop suite | the two-app problem | consolidate per §7-1 decision |
| jdata: `kjd/` vs suite `engine/jdata` | two ingestion implementations | unify on kjd (ADR-0007) |
| Status tracking historically | FEATURES.md/TODO.md/COMPLETED.md overlap | FEATURES.md single source (done); retire stale |
| Dictionary code | AppDataDatabase lookup + suite DictionaryService | distinct layers today (documented), may unify post-consolidation |

## 6. WHAT IS ARCHITECTURALLY WEAK

| Weakness | Why | Direction |
|---|---|---|
| Two-app boundary unresolved | every new feature must decide where it lives | consolidation decision (P0) |
| Dictionary search brute-force at scale | full-dataset latency risk | FTS/trigram indexing |
| No UI test harness | UI regressions unguarded | harness + critical-flow tests |
| Platform actuals unverified | "works" claims unproven | BLOCKED sweeps + CI |
| Manual website build | docs/site drift | CI regeneration |

## 7. WHAT IS GOOD (preserve)

- FSRS-5 + study engine design (note→cardtype→card; events immutable)
- Event-driven statistics (raw events → aggregation → derived)
- Honest-data discipline (fake-data cleanups; source labels; confidence labels)
- Two-SQLDelight-DB separation (immutable app data vs user data)
- Media abstraction (backends behind `MediaEngine`; capability gating)
- Mining payload design (source-agnostic)
- Window system rebuild (work areas, theme-aware chrome, resize snap)
- Documentation discipline (§335/§336 checks; ADR system)
- KJD provenance pipeline
- Screen pattern + Koin DI conventions

## 8. WHAT SHOULD NOT BE TOUCHED (never-change list + stable core)

- SRS algorithm logic and core learning logic
- SQLDelight `.sq` schemas (without explicit request)
- Package namespace `ua.syt0r.kanji`
- Gradle build configuration (unless broken)
- Working, tested media/subtitle/mining engines (enhance behind abstractions)
- The theme token system (extend, don't fork)
- Shipped installers/update pipeline

## 9. WHAT SHOULD BE REFACTORED

| Refactor | When |
|---|---|
| Data-layer consolidation (post §7-1 decision) | after decision |
| jdata unification onto kjd | P1 |
| Dictionary search indexing | P2 |
| Dead/shadow code removal | after consolidation |
| Accessibility completeness pass | P3 |

## 10. WHAT SHOULD BE REPLACED

| Replace | Why | When |
|---|---|---|
| (Nothing wholesale in shipped code) | — | — |
| Suite-only UI entry points (Dictionary manager, OCR, Browser, Reading) → core destinations | two-app drift | consolidation decision |
| Legacy demo seeding / fabricated summaries (already removed) | honesty | done |

## 11. WHAT SHOULD BE BUILT NEXT (dependency order)

1. **Close v2.3** — archived-deck restore UI; release (in progress).
2. **One-product decision** — P0 architecture decision (gates everything).
3. **Node layer** (ADR-0013 storage decision + registries as code).
4. **Event stream completeness** (ADR-0016 substrate).
5. **User knowledge model** over events.
6. **Dictionary popup → mining → card vertical slice in product** (the loop).
7. **Engine evaluation ADR** (§242) → Journey runtime prototype.
8. **Content pipeline** (ADR-0015) → Kamakura/Enoshima content.
9. **Vertical slice** (the §91 gate).
10. **Children mode** + world expansion (post-slice).

## 12. DEPENDENCY ORDER (see `docs/production/phases.md`)

```
DB → Dictionary → Lookup → Mining → (Deck/Anki)
   → Learning → Curriculum → Exams → Stats
   → Media → Subtitles → Mining → Dictionary
Core (nodes) → World (Locations/NPC/Quests) & Learning (Knowledge → Progress)
Engine ADR → Runtime → Slice → Expansion
```

---

# TOP-100 MOST IMPORTANT TASKS

Priority: 🔴 P0 · 🟡 P1 · 🟢 P2 · 🔵 P3. Status per §64 taxonomy. These are the
canonical work items (they mirror and extend `docs/planning/TODO.md`); mark DONE
in both places.

## A. Release & consolidation (P0)

| # | Task | Status | Depends |
|---|---|---|---|
| 1 | Ship v2.3 (archived-deck restore UI + release) | IN PROGRESS | — |
| 2 | Decide the one-product architecture (§7-1) and record an ADR | OPEN | — |
| 3 | Runtime-verify BLOCKED platform paths (iOS, Windows, Linux, Android SAF) | BLOCKED | hardware/CI |
| 4 | Desktop polish P0: animation stutter, resize glitches, hover consistency, spacing/radius | OPEN | — |
| 5 | Consolidate duplicated data layers per the one-product decision | OPEN | 2 |
| 6 | Remove dead/shadow code (LearningPowerHub, SyncSettingsUI, dead backup path) | OPEN | 2 |

## B. Node & knowledge foundation (P0/P1)

| # | Task | Status | Depends |
|---|---|---|---|
| 7 | Node model: storage decision + registries as code (ADR-0013) | TARGET | 2 |
| 8 | Event stream completeness (all surfaces emit; ADR-0016) | TARGET | 7 |
| 9 | User knowledge model over events (dimensions, transitions) | TARGET | 8 |
| 10 | Knowledge scoring dials + honest display (NODE §85) | TARGET | 9 |
| 11 | Dictionary as node interface + traversal chips | TARGET | 7 |
| 12 | Kanji/vocab node experiences ("where have I seen this?") | TARGET | 7, 9 |
| 13 | Browse/Library node views (filters as node queries) | TARGET | 7 |
| 14 | Stats over node events (one stream; drill-down) | TARGET | 8 |
| 15 | Media as node family (Series/Episode/Scene/SubtitleLine) | TARGET | 7 |
| 16 | Mining into the graph (mined_from edges, provenance) | TARGET | 7, 8 |

## C. Dictionary / search / data (P1/P2)

| # | Task | Status | Depends |
|---|---|---|---|
| 17 | FTS/trigram search indexing at scale | OPEN | — |
| 18 | Dictionary popup → mining → card vertical slice in product | OPEN | 2 |
| 19 | jdata consolidation onto kjd | OPEN | — |
| 20 | Open grammar dataset RESEARCH + KJD adapter | RESEARCH | licensing |
| 21 | Tatoeba example-sentence adapter | RESEARCH | licensing |
| 22 | Pitch-accent dataset RESEARCH + adapter + visualization | RESEARCH | licensing |
| 23 | Open frequency/JLPT data refresh + provenance audit | MONITOR | — |
| 24 | AnkiConnect e2e verification (live Anki) | BLOCKED | hardware |

## D. Media / mining / integrations (P1/P2)

| # | Task | Status | Depends |
|---|---|---|---|
| 25 | OCR hardening (missing-engine UX, region capture polish) | OPEN | — |
| 26 | Browser workspace as core destination (backend decision first) | OPEN | backend RESEARCH |
| 27 | Media polish: subtitle edge cases, backend resilience tests | MONITOR | — |
| 28 | Mining destination UX (Kaiteyo/Anki/Both) review + duplicate/sync handling | OPEN | — |
| 29 | Auto-update end-user rollout (channels) | OPEN | — |
| 30 | Sync conflict resolution maturity + cross-device UX | OPEN | — |
| 31 | Plugin runtime sandbox design (capability model) | DEFERRED | ADR-0011 |

## E. Learning depth (P2)

| # | Task | Status | Depends |
|---|---|---|---|
| 32 | Curriculum engine (graph-based; evidence prerequisites) | TARGET | 7, 9 |
| 33 | Adaptive/recommendation engine (explainable recommendations) | TARGET | 9 |
| 34 | Graded reading mode | TARGET | content pipeline |
| 35 | Story content format + first stories | TARGET | 37 |
| 36 | Kanji similar/confusable curation | TARGET | 12 |
| 37 | Content pipeline: schemas, gates, packages (ADR-0015) | TARGET | 7 |
| 38 | Node editor (authoring tool) | TARGET | 37 |
| 39 | Grammar content expansion (starter deck + data) | OPEN | 20 |
| 40 | Speech/listening practice path | TARGET | 9 |

## F. Journey (TARGET — nothing implemented)

| # | Task | Status | Depends |
|---|---|---|---|
| 41 | Engine evaluation ADR (Godot/Unity/Unreal per §242) | RESEARCH | — |
| 42 | Journey runtime prototype (UI layers, HUD, input, save) | TARGET | 41 |
| 43 | World data schema + Kamakura/Enoshima packages | TARGET | 37, 41 |
| 44 | Kamakura vertical slice (the §91 proof gate) | TARGET | 42, 43 |
| 45 | World streaming + managers implementation | TARGET | 41 |
| 46 | NPC system (simulation tiers, schedules) | TARGET | 44 |
| 47 | Quest system (node model, non-punitive rules) | TARGET | 44 |
| 48 | Dialogue system + dialogue content | TARGET | 43, 44 |
| 49 | Photography + collections + discovery | TARGET | 44 |
| 50 | Transportation (Enoden slice) | TARGET | 44 |
| 51 | Environment simulation (time/weather/season determinism) | TARGET | 41 |
| 52 | Game audio (buses, zones, TTS reuse) | TARGET | 44 |
| 53 | Save system (versioned, checksummed, split rule) | TARGET | 42 |
| 54 | Knowledge-density map overlay | TARGET | 9, 44 |
| 55 | Children mode (config + content filter) | TARGET | 44, 37 |
| 56 | World expansion packaging (region packages) | TARGET | 44 |

## G. Rendering & input (TARGET)

| # | Task | Status | Depends |
|---|---|---|---|
| 57 | Rendering direction validation on slice (stylized, toon hybrid) | TARGET | 41, 44 |
| 58 | LOD/streaming/occlusion budgets measured per tier | TARGET | 45 |
| 59 | Environment visuals (weather/water/vegetation determinism) | TARGET | 51 |
| 60 | Action layer implementation (actions, remapping, profiles) | TARGET | 42 |
| 61 | Gamepad + touch + keyboard parity | TARGET | 60 |
| 62 | Input accessibility completeness (keyboard-first, reduced motion) | TARGET | 60 |

## H. UX / design (P1–P3)

| # | Task | Status | Depends |
|---|---|---|---|
| 63 | Animation token system + reduced-motion completeness | OPEN | — |
| 64 | Spacing/radius audit (4dp grid) | OPEN | — |
| 65 | Full keyboard navigation + screen-reader support | OPEN | — |
| 66 | High-contrast mode completeness | OPEN | — |
| 67 | Tablet layouts polish | OPEN | — |
| 68 | Mobile nav snap behavior | OPEN | — |
| 69 | Settings center cleanup (route all appearance options) | OPEN | — |
| 70 | Game UI/HUD spec implementation (per runtime spec) | TARGET | 42 |

## I. Quality / testing / tooling (P1–P3)

| # | Task | Status | Depends |
|---|---|---|---|
| 71 | UI test harness + critical flows | OPEN | — |
| 72 | Migration tests for user data DB | OPEN | — |
| 73 | Content validation gate fixtures (failing tests per gate) | TARGET | 37 |
| 74 | Perf budgets measured + reported (app + world) | MONITOR | — |
| 75 | `kaiteyo dev doctor` | PLANNED | — |
| 76 | `kaiteyo docs check` (orphan/link/freshness) | PLANNED | — |
| 77 | `kaiteyo content validate` | TARGET | 37 |
| 78 | License checker tooling | PLANNED | — |
| 79 | CI: website regeneration | OPEN | — |
| 80 | CI: platform runtime verification (best-effort) | OPEN | hardware |

## J. Content & production (P2–P3)

| # | Task | Status | Depends |
|---|---|---|---|
| 81 | Slice content authoring (Komachi shops, Hase-dera, Enoden, beach, aquarium) | TARGET | 43 |
| 82 | Slice NPC/quest/dialogue/story content | TARGET | 43 |
| 83 | Festival/seasonal event content framework | TARGET | 51 |
| 84 | Child-safe content filters + child content seed | TARGET | 55 |
| 85 | Community authoring path + marketplace (future) | DEFERRED | 37, plugin |
| 86 | Localization completeness for content (ja/en) | TARGET | 37 |

## K. Docs & process (P2–P3)

| # | Task | Status | Depends |
|---|---|---|---|
| 87 | Refresh ENGINEERING_AUDIT after consolidation decision | OPEN | 2 |
| 88 | Refresh PRODUCT_AUDIT after dead-code removal | OPEN | 6 |
| 89 | Keep docs ↔ code freshness (per DocumentationRules) | ONGOING | — |
| 90 | Website: reflect Journey as target (no launch claims) | MONITOR | — |
| 91 | Website web-trial slice (small working slice) | FUTURE | product |
| 92 | Reference: schema/API reference generation | PLANNED | — |

## L. Research (P1/P2)

| # | Task | Status | Depends |
|---|---|---|---|
| 93 | Browser backend evaluation (CEF/WebView2/Android WebView) | RESEARCH | — |
| 94 | Game engine evaluation (Godot/Unity/Unreal) | RESEARCH | — |
| 95 | Grammar/pitch/Tatoeba licensing verification | RESEARCH | — |
| 96 | AI-scheduling coexistence with FSRS-5 (future enhancement) | RESEARCH | — |
| 97 | AniList-like API terms + integration design | RESEARCH | — |
| 98 | Speech input feasibility | RESEARCH | — |

## M. Definition of done for this audit pass

| # | Task | Status |
|---|---|---|
| 99 | Documentation corpus fully indexed (0 orphans, 0 dead links) | ✅ (verify after this pass) |
| 100 | The §78 report delivered (findings, order, parallel/sequential) | ✅ this doc |

---

# TOP-100 RISKS / ISSUES

Scored P (probability) × I (impact) L/M/H; status OPEN/MITIGATED/MONITOR/BLOCKED/
ACCEPTED. Full context: `risk-register.md`, `CURRENT_ISSUES.md`.

## Strategic (1–20)

| # | Risk/issue | P | I | Status |
|---|---|---|---|---|
| 1 | World scope expands before the vertical slice proves the loop | M | H | OPEN |
| 2 | Engine evaluation never happens (or is an adoption without evidence) | M | H | OPEN |
| 3 | Art/audio production cost exceeds plan | H | H | OPEN |
| 4 | Two-app consolidation stays open forever; duplication grows | H | H | OPEN |
| 5 | Dataset license blocks redistribution/commercial use | M | H | MONITOR |
| 6 | One-developer scope: enormous vision, bounded capacity | H | H | OPEN |
| 7 | AI agents drift/fake/duplicate; quality regression | M | H | MONITOR |
| 8 | Architecture fragmentation (silos) returns after consolidation | M | H | OPEN |
| 9 | Mobile GPU can't hold world perf on low tiers | M | H | OPEN |
| 10 | Platform verification gaps ship unverified paths | H | M | OPEN |
| 11 | Children mode becomes a second stack instead of a config layer | M | M | OPEN |
| 12 | Community content bypasses validation | M | M | OPEN |
| 13 | Save compatibility breaks across world versions | M | M | OPEN |
| 14 | Sync conflicts lose user data | M | M | MONITOR |
| 15 | Data provenance drifts (unversioned datasets) | M | M | MONITOR |
| 16 | Docs claim shipped for target systems (fake completeness) | M | L | MONITOR |
| 17 | Curriculum authoring cost blocks releases | H | M | OPEN |
| 18 | Media licensing (VLC GPL interplay) surprises | L | M | MONITOR |
| 19 | AnkiConnect breaks user workflows (unverified e2e) | M | M | BLOCKED |
| 20 | AniList-like integration becomes required instead of optional | M | M | OPEN |

## Product quality (21–40)

| # | Risk/issue | P | I | Status |
|---|---|---|---|---|
| 21 | Animation stutter never fully fixed (P0) | M | M | OPEN |
| 22 | Resize glitches persist on exotic setups | M | M | MONITOR |
| 23 | Spacing/radius inconsistency erodes premium feel | H | L | OPEN |
| 24 | Archived decks confusion ships into v2.3 | L | M | OPEN |
| 25 | Search latency breaks at full dataset scale | M | M | OPEN |
| 26 | Subtitle parser hits malformed file crash (post-hardening) | L | M | MITIGATED |
| 27 | Media backend (VLC absent) degrades poorly on user machines | M | M | MONITOR |
| 28 | OCR missing-engine UX still confuses users | M | L | OPEN |
| 29 | Dashboard knowledge estimates misread as certification | M | M | MONITOR |
| 30 | Heatmap intensity misrepresents activity (clicks vs study) | L | M | MITIGATED (AFK) |
| 31 | Study time inflated by app-open time | L | M | MITIGATED (ActivityTracker) |
| 32 | UI claims a state it doesn't have (fake completeness residue) | M | M | MONITOR |
| 33 | Dead code confuses agents (LearningPowerHub etc.) | M | M | OPEN |
| 34 | Accessibility gaps block a11y users (keyboard, SR) | M | M | OPEN |
| 35 | Reduced motion incompleteness in world/cinematics | M | L | OPEN |
| 36 | Tablet/mobile layouts remain phone-columns | M | L | OPEN |
| 37 | Custom window chrome breaks on future OS updates | M | M | MONITOR |
| 38 | DPI/multi-monitor edge cases (taskbar top, mixed DPI) | M | M | MONITOR |
| 39 | Auto-update rollout breaks update-feed channels | L | M | MONITOR |
| 40 | Installer drift (artifacts missing in release) | L | H | MITIGATED (stage/verify) |

## Data & content (41–60)

| # | Risk/issue | P | I | Status |
|---|---|---|---|---|
| 41 | Grammar dataset licensing blocks curriculum depth | M | M | RESEARCH |
| 42 | Tatoeba sentence quality/licensing issues | M | M | RESEARCH |
| 43 | Pitch-accent data missing/unverified source | M | M | RESEARCH |
| 44 | KanjiVG licensing/redistribution handled incorrectly | L | H | MONITOR |
| 45 | jdata duplication produces divergent data | M | M | OPEN |
| 46 | Dictionary import (Yomitan formats) edge cases | M | M | MONITOR |
| 47 | Frequency metadata stale or inconsistent | L | M | MONITOR |
| 48 | Content package gate gaps (a bad package slips) | M | M | OPEN |
| 49 | Content localization incomplete for one locale | M | L | OPEN |
| 50 | World content references dangling (quest→NPC gone) | M | M | OPEN |
| 51 | Slice content quality below premium bar | M | H | OPEN |
| 52 | Seasonal content breaks the "nothing permanently missable" rule | M | M | OPEN |
| 53 | Deterministic weather/season saves drift | L | M | OPEN |
| 54 | NPC schedule determinism broken across tiers | M | M | OPEN |
| 55 | Train timetable fidelity creep (simulating the whole network) | M | M | OPEN |
| 56 | Photography/collections become loot-like | L | M | MONITOR |
| 57 | Knowledge-density overlay too slow at scale | M | M | OPEN |
| 58 | Quest engine non-punitive rules violated by an authored quest | M | M | OPEN |
| 59 | Child content filter misses an unsafe item | M | H | OPEN |
| 60 | Exam question generator produces nonsense distractors | M | M | MONITOR |

## Engineering (61–80)

| # | Risk/issue | P | I | Status |
|---|---|---|---|---|
| 61 | Node storage decision wrong for scale | M | H | OPEN |
| 62 | Event stream double-counts between app and world paths | M | M | OPEN |
| 63 | Knowledge state machine overfits one evidence type | M | M | OPEN |
| 64 | FSRS modified accidentally (never-change list) | L | H | MONITOR |
| 65 | Schema change without migration | L | H | MITIGATED (migrations) |
| 66 | Build environment rot (versions, assets downloads) | M | M | MONITOR |
| 67 | iOS targets break on Windows CI (expected, ignored) | H | L | ACCEPTED |
| 68 | JVM heap constraints make CI slow (2GB) | M | L | ACCEPTED |
| 69 | Native drag/window APIs break per-OS | M | M | MONITOR |
| 70 | Media tick loop regression reintroduces crashes | L | H | MITIGATED (fail-safe tick + test) |
| 71 | SHA-256/pure-Kotlin crypto correctness | L | M | MITIGATED (FIPS vectors) |
| 72 | APKG import security (zip bombs, HTML) | M | M | MITIGATED (sanitization) |
| 73 | Plugin sandbox shipped unsandboxed | L | H | DEFERRED |
| 74 | Website build dependency drift | L | L | MONITOR |
| 75 | CLI wrapper invents tasks / diverges from Gradle | L | M | OPEN |
| 76 | Documentation freshness decays between audits | H | L | MONITOR |
| 77 | Orphan/broken links re-enter docs | M | L | MONITOR |
| 78 | Test suite rot (tests that test nothing) | M | M | MONITOR |
| 79 | No UI tests → regression ships | M | M | OPEN |
| 80 | Perf budgets unmeasured → world ships slow | M | H | OPEN |

## Delivery (81–100)

| # | Risk/issue | P | I | Status |
|---|---|---|---|---|
| 81 | v2.3 release slips (blocked on archived-deck UI) | M | M | OPEN |
| 82 | Release artifacts mismatch (missing installer) | L | H | MITIGATED |
| 83 | Update-feed channel confusion (stable/beta/nightly) | L | M | MITIGATED |
| 84 | F-Droid flavor drifts from googlePlay | M | M | MONITOR |
| 85 | Google Play billing/review breakage | L | M | MONITOR |
| 86 | Firebase/crashlytics data hygiene (opt-in/privacy) | L | M | MONITOR |
| 87 | Privacy: user data export/delete incomplete | M | M | MONITOR |
| 88 | Backup restore path unverified end-to-end | M | M | OPEN |
| 89 | Import/export conflict policy confusion | M | L | OPEN |
| 90 | Anki duplicate cards on re-export | M | M | OPEN |
| 91 | Mining duplicate protection gaps | M | M | OPEN |
| 92 | Media library persistence edge cases (missing files) | M | M | MONITOR |
| 93 | Playlist/collection migration on schema change | M | M | MONITOR |
| 94 | Achievements/streaks incentivize wrong behavior | L | M | MONITOR |
| 95 | Community features (leaderboards) reintroduce gamification | M | M | MONITOR |
| 96 | Branding asset replacement loses originals | L | L | MITIGATED (pipeline rules) |
| 97 | Icon system drift (random SVGs reappear) | M | L | MONITOR |
| 98 | Localization strings drift between EN/JA | M | L | MONITOR |
| 99 | WSL/Windows dev tooling friction | M | L | MONITOR |
| 100 | The project becomes un-understandable as it grows | M | H | OPEN (mitigated by this corpus) |

---

## How to use this audit

1. **Implementation agents**: start from `docs/planning/ENGINEERING_AUDIT.md` §9,
   cross-reference this audit's TOP-100 tasks (priority + dependency order) and
   the master TODO (`docs/planning/TODO.md`).
2. **Reviewers**: use §3 (broken) + §8 (don't touch) + the TOP-100 risks to gate
   releases.
3. **Freshness**: when architecture changes, update this audit (sections 1–12)
   and the registers it summarizes. The TOP-100 lists are living: renumber as
   items close.
