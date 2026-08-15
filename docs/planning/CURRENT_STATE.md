# Kaiteyo — Current State Audit

> **What this is**: the living, per-subsystem status matrix of the repository — what
> exists, where, how it works, what is broken/missing, and what is planned. The
> authoritative *deep* audit (file-level evidence, the two-app defect, dead code) is
> [`PRODUCT_AUDIT.md`](PRODUCT_AUDIT.md); the feature status matrix (source of truth for
> ✅/🚧/📋) is [`../features/FEATURES.md`](../features/FEATURES.md); the bug tracker is
> [`CURRENT_ISSUES.md`](CURRENT_ISSUES.md). This file is the **map** that ties them to
> the master blueprint ([`../product/PRODUCT.md`](../product/PRODUCT.md)).
>
> **Audited**: 2026-08-15, against the working tree (HEAD `develop`). Everything marked
> CURRENT was verified by reading code paths; anything unverified is `UNKNOWN` with a
> note on what to inspect.

## Status taxonomy

`IMPLEMENTED` · `PARTIALLY_IMPLEMENTED` · `BROKEN` · `PLACEHOLDER` · `PROTOTYPE` ·
`DOCUMENTED_ONLY` · `PLANNED` · `ARCHITECTED` · `BLOCKED` · `DEPRECATED` · `UNKNOWN`

Additional dimension used here: **where** the feature lives.

- **(core)** — shared Compose MPP, ships on desktop/Android/iOS via `main()` entry points
- **(suite)** — JVM-only desktop suite under `desktopApp/src/jvmMain/kotlin/ua/syt0r/kanji/desktop`;
  **not reachable from any shipped `main()`** (PRODUCT_AUDIT §1) until ADR-0017 lands
- **(kjd)** — the standalone data platform
- **(TARGET)** — architected/documented only; no implementation

---

## 1. Subsystem status matrix

| Subsystem | Status | Where | Evidence / entry point | Notes |
|---|---|---|---|---|
| **Home / dashboards** | `IMPLEMENTED` | core | `presentation/screen/main/screen/home` → `HomeScreen` → `GeneralDashboardScreen`/`LibraryScreen`/`StatisticsScreen`/`SearchScreen`/`SettingsScreen` | Real counts from SQLDelight via `KaiteyoDataCenter` + SRS managers |
| **Library / decks** | `IMPLEMENTED` | core | `screen/library`, `screen/decks`, `DeckFeaturesController` | Real due/new counts; archive filter follow-up open (TODO P1) |
| **Deck details / browse** | `IMPLEMENTED` | core | `screen/deck_details`, `DeckBrowserRoute` | Overview/Study/Cards/Browse/Stats/Settings |
| **Card browser / editor** | `IMPLEMENTED` | core | `CardBrowserRoute`, `NoteEditorRoute` | Tag/flag managers, bulk actions |
| **Kanji study** | `IMPLEMENTED` | core | `screen/kanji_browser`, letter practice | JLPT N5–N1 + grade decks; kanji browser; collections |
| **Kana study** | `IMPLEMENTED` | core | letter decks (hiragana/katakana) | Same writing engine as kanji |
| **Writing practice** | `IMPLEMENTED` | core | `practice_letter`, `stroke_evaluator` | Brush canvas, stroke-order guides, count/order/shape scoring, strictness levels |
| **Vocabulary study** | `IMPLEMENTED` | core | `practice_vocab`, `vocab_card` | Reading/writing modes, furigana, text analysis |
| **SRS (FSRS-5)** | `IMPLEMENTED` | core | `core/srs/fsrs`, `FsrsSchedulerTest.kt` | Scheduling logic is on the never-change list |
| **Review flow** | `IMPLEMENTED` | core | review screens + managers | Grade/bury/suspend/retry/forget/undo; daily limits |
| **Statistics** | `IMPLEMENTED` | core | `StatisticsController`, `screen/statistics`, `screen/stats` | Event-driven; heatmap, curves, retention, goals, velocity |
| **Exams** | `IMPLEMENTED` | core | `screen/.../exams` (via stats/learning paths), `docs/architecture/exams.md` | Generated + weekly exam; scoring; analytics |
| **Achievements** | `IMPLEMENTED` | core | achievements subsystem | Multiple categories |
| **Import/export** | `IMPLEMENTED` | core | `core/transfer/` (APKG, JSON/CSV/TSV/TXT), `ImportExportPipeline` | Preview, validation, conflict policies; rollback-safe |
| **Backup/restore** | `IMPLEMENTED` | core | `BackupRoute`/`BackupScreen`, `core/backup/` | Profile archives incl. settings + window state |
| **Anki `.apkg`** | `IMPLEMENTED` | core (JVM/Android/iOS actuals) | `core/transfer/AnkiPackage.*` | Template rendering, scheduling mapping, media extraction, sanitization |
| **Sync / account** | `PARTIALLY_IMPLEMENTED` | core | `core/sync/`, `core/account/` | GitHub device-flow + private gist; desktop-first; conflict dialog |
| **Settings** | `IMPLEMENTED` | core | `screen/settings` + `PreferencesContract` (DataStore) | Settings Center categories; Theme Studio |
| **Navigation (NavShell)** | `IMPLEMENTED` | core | `presentation/.../navigation` (NavShell), `docs/architecture/NAVIGATION.md` | Sidebar/Floating modes, 4 edges, snap, persistence, `Ctrl+B` |
| **Floating bubble / Launchpad** | `IMPLEMENTED` | core | NavShell floating mode + launchpad surfaces | Drag, snap (~3/edge), hold/right-click menu, persistence |
| **Theming** | `IMPLEMENTED` | core + suite | `theme_manager`, `ThemeStudio`, suite `theming/` | 17 presets, Light/Dark/OLED, custom gradients, JSON import/export |
| **Localization (EN/JA)** | `IMPLEMENTED` | core | `Strings`/`EnglishStrings`/`JapaneseStrings` | Interface-based; `-Duser.language=ja` |
| **Dictionary (bundled)** | `IMPLEMENTED` | core | `core/app_data` (AppDataDatabase), `KaiteyoDataCenter` | Read-only asset, versioned (v15), kjd-generated |
| **Dictionary (import + popup)** | `IMPLEMENTED (suite, unshipped)` | suite | `desktop/engine/dictionary/*` — `DictionaryService`, `DictionaryImporter`, `DictionaryPopup` | Yomitan ZIP/JSON/JMdict; EXACT/PREFIX/KANA/DEINFLECT; TTS; mining actions |
| **Search** | `IMPLEMENTED` | core + suite | `SearchScreen` (core), `SearchEngine` (suite) | Radical/word/sentence search; node-family browse is target |
| **Text analysis** | `IMPLEMENTED` | core | text_analysis screen | Word-by-word breakdown (Ichiran-style) |
| **Media center** | `IMPLEMENTED (suite, unshipped)` | suite | `desktop/engine/media/*` — `MediaEngine`, `AudioPlayer`, `SubtitleEngine` | VLC/mpv/Java Sound backends; SRT/ASS/SSA/VTT; screenshots; bookmarks; A–B; speed |
| **Mining** | `IMPLEMENTED (suite, unshipped)` | suite | `desktop/engine/mining/*` — `MiningEngine`, `MiningPayload` | Sources: dictionary/subtitle/browser/OCR/clipboard; duplicate protection |
| **OCR** | `PARTIALLY_IMPLEMENTED (suite)` | suite | `desktop/engine/ocr/OcrEngine` (Tess4J when present) | Capture pipeline works; detection needs external Tesseract |
| **Learning browser** | `IMPLEMENTED (suite)` | suite | `desktop/ui/browser/*` | Tabs, bookmarks, reader mode (JavaFX WebView when available) |
| **AnkiConnect** | `IMPLEMENTED (suite)` | suite | `AnkiConnectTransport`, `AnkiImporter` | Push/import; e2e verification needs live Anki (BLOCKED item) |
| **Local HTTP API** | `IMPLEMENTED (suite)` | suite | `LocalApiServer` (Ktor), bearer token | status/mine/media/player endpoints |
| **Text hook / WebSocket** | `IMPLEMENTED (suite)` | suite | `TextHookServer`, `PlayerStateWebSocket` | Subtitle lines + player state for external tools |
| **Plugin system** | `PLACEHOLDER→PLANNED` | suite | `desktop/engine/plugin/` | Registry + marketplace scaffold; **no runtime loading** (ADR-0011) |
| **Auto-update** | `PARTIALLY_IMPLEMENTED` | desktop | installer update feeds, `Updater` | Architecture complete (channels, sha256, rollback); rollout staged |
| **Onboarding** | `IMPLEMENTED (suite)` | suite | `OnboardingWizard` | 8 steps; core app has no onboarding (gap) |
| **Grammar study** | `PARTIALLY_IMPLEMENTED (suite)` | suite | `GrammarPracticeView` + starter deck | No bundled grammar dataset (RESEARCH) |
| **Pitch accent** | `PLANNED` | — | RESEARCH in TODO | No open pitch dataset adopted |
| **Knowledge graph (language)** | `IMPLEMENTED` | kjd + core | kjd entity resolution → `AppDataDatabase` | kanji↔readings↔meanings↔radicals↔words |
| **Knowledge graph (user)** | `ARCHITECTED (TARGET)` | — | ADR-0013/0016, `nodes/KNOWLEDGE_STATE_MODEL.md` | Node/edge layer + event-derived knowledge states |
| **Node architecture** | `ARCHITECTED (TARGET)` | — | ADR-0013, `NODE_ARCHITECTURE.md` | No node tables exist yet |
| **Journey (game)** | `ARCHITECTED (TARGET)` | — | ADR-0014/0018, `docs/game/`, `nodes/` | **No implementation**; engine decision pending |
| **World / content packages** | `ARCHITECTED (TARGET)` | — | ADR-0015, `CONTENT_AUTHORING.md` | Schema + validation gates specified |
| **Curriculum / courses** | `PLANNED` | — | `docs/learning/curriculum-engine.md` | Not implemented; deck-generation groundwork exists |
| **Children's world** | `PLANNED (TARGET)` | — | `docs/vision/child-experience.md` | After vertical slice (NODE §115) |
| **Website** | `IMPLEMENTED` | website/ | Python build consuming `docs` | `dist/` committed; regeneration is a tracked debt |
| **Web trial** | `PLANNED` | — | MASTER_TODO KT-WEB-001 | Requires WASM/Compose-Web evaluation |
| **Embedded browser** | `PARTIALLY_IMPLEMENTED (suite)` | suite | `desktop/ui/browser` (reader + WebView) | Full browser architecture is `PLANNED` (`docs/architecture/browser.md`) |
| **Gamepad input** | `PLANNED` | — | `docs/game/player.md`, `docs/input/` | No game exists; app controller support partial |

## 2. Platform status

| Platform | Status | Evidence | Notes |
|---|---|---|---|
| Desktop (Windows/macOS/Linux) | `IMPLEMENTED` (two apps — see note) | `desktopApp/Main.kt` (core app) + `desktopApp/.../SuiteMain.kt` (suite, unshipped) | One-product decision pending (ADR-0017) |
| Android | `IMPLEMENTED` | `app/` — googlePlay + fdroid flavors | SAF picker, APKG via `SQLiteDatabase`, WorkManager reminders |
| iOS | `IMPLEMENTED` (unverified at runtime on this host) | `iosApp/`, core `iosMain` | Builds from macOS only; APKG codec dependency-free; verification BLOCKED |
| Windows runtime checks | `UNKNOWN` | — | Media keys, tray, native drag need a Windows machine (BLOCKED) |

## 3. Known problems (top, by severity)

1. **Two parallel applications** — the shipped core app and the unshipped suite duplicate
   navigation, settings, theme, stats, SRS, library, sync, and import/export
   (PRODUCT_AUDIT §1, §6). All suite-only *engines* (dictionary, media, mining, OCR,
   AnkiConnect, local API) are therefore unreachable by users. **Gate**: ADR-0017.
2. **Dead shadows** — `LearningPowerHub` + friends (fake callbacks), `SyncSettingsUI`
   (dead "Sync Now"), unused backup-manager path (PRODUCT_AUDIT §5.2). Removal is a
   deliberate step; tracked in MASTER_TODO KT-CORE-*.
3. **Demo data seeding** — suite `seedDemoData()` presents demo cards as study content on
   first run (PRODUCT_AUDIT §5.4). Must become an empty first-run state.
4. **Animation/resize stutter** — P0 polish items (CURRENT_ISSUES/TODO P0).
5. **Grammar/pitch data gaps** — no bundled grammar dataset, no pitch data (RESEARCH).
6. **No UI tests** — Compose UI testing harness not established.
7. **Platform actuals under-verified** — iOS/Windows/Android-specific paths (BLOCKED).
8. **Two jdata implementations** — `kjd/` standalone vs suite `engine/jdata`; consolidate
   (ADR-0007 debt item).
9. **Archived decks still visible** — `is_archived` filtering follow-up (TODO P1).

## 4. Risks

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| One-product decision stalls → suite engines rot | High | High | ADR-0017 is first in MASTER_TODO dependency order |
| Game engine chosen without evaluation → rewrite | Medium | High | ADR-0018 gate: no Journey code before decision (STANDARDS §242) |
| Node layer added as parallel data store → drift | Medium | High | Read-model-over-existing-DB option in ADR-0013; docs-first |
| Dataset license issues (grammar/pitch/geo) | Medium | Medium | All new datasets gated by `docs/data/SOURCES.md` verification |
| Docs drift from code after large feature work | High | Medium | MASTER §69 change rules; link check at end of every pass |
| Demo data treated as real content | Medium | Medium | Remove seeding (KT-CORE-*); empty-state first run |

## 5. Verification method (how to check a status yourself)

1. Find the subsystem's owner doc in `docs/architecture/README.md` (or `docs/game/`,
   `docs/features/`, `docs/integrations/`).
2. Confirm the status in `docs/features/FEATURES.md` (feature-level source of truth).
3. Trace the claim to code: entry point (`Main.kt`/`MainActivity`/`KaiteyoActivity`) →
   module (Koin) → screen → view model → repository/service → data store.
4. If the code contradicts the doc, fix the doc (and open a CURRENT_ISSUES entry if it's
   a product defect). Do not silently change code to match docs.

## 6. MASTER §87 audit answers (34 questions, summarized)

| # | Question | Answer |
|---|---|---|
| 1–6 | Subsystems documented (game, media, mining, Yomitan, Anki, AnkiConnect, knowledge graph, stats, exams, library) | ✅ — see matrix above + `docs/architecture/`, `docs/game/`, `docs/integrations/` |
| 7 | Current vs planned separated | ✅ — taxonomy + WHERE dimension |
| 8–12 | Dependencies, datasets, licenses documented | ✅ — `ENGINEERING_AUDIT.md`, `docs/data/SOURCES.md`, `docs/legal/` |
| 13–18 | Android/desktop/web/controls/world/streaming documented | ✅ — `docs/platform/`, `docs/game/` |
| 19–24 | Asset pipeline, curriculum, children's world documented | ✅ — `docs/game/asset-pipeline.md`, `docs/learning/`, `docs/vision/child-experience.md` |
| 25 | Master TODO actionable | ✅ — `MASTER_TODO.md` (IDs, status, priority, deps, acceptance) |
| 26–29 | Future AI can find start/implemented/remaining/architecture | ✅ — `docs/ai/AI_AGENT_GUIDE.md` + this file + `ENGINEERING_AUDIT.md` |
| 30–34 | Unknowns marked, ADRs recorded, performance/privacy/licensing defined | ✅ — UNKNOWN labels above; ADR-0001…0018; `performance.md`; `docs/security/`; `docs/data/SOURCES.md` |

## Related

- [`PRODUCT_AUDIT.md`](PRODUCT_AUDIT.md) — deep audit with file-level evidence
- [`../features/FEATURES.md`](../features/FEATURES.md) — feature status source of truth
- [`CURRENT_ISSUES.md`](CURRENT_ISSUES.md) — bug tracker
- [`MASTER_TODO.md`](MASTER_TODO.md) — what to build next
- [`../product/PRODUCT.md`](../product/PRODUCT.md) — the blueprint (MASTER §5)
