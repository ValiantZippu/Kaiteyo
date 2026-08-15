# Subsystem Map

Pick a subsystem → its canonical docs, status, and entry points. This is the §75
lookup: "select a subsystem and immediately understand purpose, architecture,
dependencies, current implementation, intended implementation, data, events,
interfaces, UX, edge cases, performance, tests, TODOs, risks."

## Map

| Subsystem | Status | Canonical docs | Entry points (code) |
|---|---|---|---|
| **Dictionary** | FUNCTIONAL (lookup) / TARGET (node layer, FTS) | `architecture/dictionary.md`, `architecture/pitch-frequency.md`, `data/SOURCES.md` | `desktopApp/.../engine/dictionary/`, core `app_data` |
| **Search** | FUNCTIONAL (in-memory) / TARGET (indexed) | `architecture/dictionary.md` (§search), STANDARDS §186–§187 | suite SearchEngine; core search |
| **Learning engine** | FUNCTIONAL | `architecture/study-engine.md`, `learning/progress-model.md` | suite `engine/learning/` |
| **SRS (FSRS-5)** | STABLE (never change) | `architecture/study-engine.md`, ADR-0006 | core `srs/fsrs/` |
| **Writing/strokes** | FUNCTIONAL | `architecture/study-engine.md`, `CURRENT_ISSUES.md` (KanjiVG) | `engine/stroke_evaluator/` |
| **Exams** | FUNCTIONAL | `architecture/exams.md` | `engine/learning/ExamEngine` |
| **Statistics** | FUNCTIONAL | `architecture/statistics.md`, ADR-0016 | `engine/stats/StatisticsRepository` |
| **Knowledge model** | PARTIAL (profile) / TARGET (full) | `nodes/KNOWLEDGE_STATE_MODEL.md`, `learning/progress-model.md`, ADR-0016 | `KnowledgeProfileEngine` |
| **Node layer** | TARGET | `architecture/NODE_ARCHITECTURE.md`, `nodes/` (registries, data model), ADR-0013 | — |
| **Media Centre** | FUNCTIONAL | `architecture/media.md` | suite `engine/media/` + core `MainDestination.Media` |
| **Subtitles** | FUNCTIONAL | `architecture/media.md` §4 | suite subtitle engine |
| **Mining** | FUNCTIONAL | `architecture/mining.md` | `engine/mining/MiningEngine` |
| **Anki interop** | FUNCTIONAL (apkg) / BLOCKED (e2e) | `integrations/ANKI.md`, `architecture/integrations.md` §2 | `AnkiImporter`, transfer |
| **Yomitan-like lookup** | FUNCTIONAL (in-app) | `integrations/YOMITAN_DICTIONARIES.md`, `architecture/dictionary.md` | dictionary popup |
| **Browser workspace** | PLANNED (not core destination) | `architecture/browser.md` | suite BrowserView (dev) |
| **Import/export/backup** | FUNCTIONAL | `architecture/backup.md`, `integrations/LOCAL_API.md` | transfer engine |
| **Sync/account** | FUNCTIONAL | `architecture/SYNC.md`, ADR-0009 | core sync |
| **Curriculum** | TARGET | `learning/curriculum-engine.md` | — |
| **Adaptive/recommendations** | TARGET | `learning/adaptive-learning.md` | — |
| **Reading/stories** | PARTIAL | `learning/reading-stories.md`, `content/content-formats.md` | study engine reading mode |
| **Kanji graph** | PARTIAL (data) / TARGET (traversal) | `learning/kanji-knowledge-graph.md`, `architecture/language-model.md` | AppData relationships |
| **KJD data platform** | STABLE | `architecture/DATA_PLATFORM.md`, `data/ARCHITECTURE.md`, ADR-0007 | `kjd/` |
| **Journey world** | TARGET | `game/` (all), `nodes/JOURNEY_WORLD_SCHEMA.md`, ADR-0014 | — |
| **Rendering (world)** | TARGET | `rendering/` | — |
| **Input** | FUNCTIONAL (app) / TARGET (game action layer) | `input/`, `architecture/NAVIGATION.md` | shortcut registry |
| **Design system / theme** | STABLE | `design/`, `vision/design-philosophy.md`, ADR-0002 | core theme + suite `Ds*` |
| **Content pipeline** | TARGET | `content/`, `nodes/CONTENT_AUTHORING.md`, ADR-0015 | — |
| **Plugins** | DEFERRED (scaffold) | `integrations/PLUGINS.md`, ADR-0011 | `engine/plugin/` |
| **Installer/release** | STABLE | `releases/`, `architecture/ci-cd.md` | `installer/` |
| **Tooling/CLI** | FUNCTIONAL | `tools/`, `cli/` | `tools/cli/` |
| **Accessibility** | PARTIAL | `architecture/accessibility.md`, `input/accessibility.md` | — |
| **Localization** | FUNCTIONAL | `architecture/localization.md`, `content/content-formats.md` | `Strings` EN/JA |

## How to read a subsystem

For any subsystem above, open: its canonical doc(s) → `docs/planning/TODO.md`
(tasks) → `docs/production/risk-register.md` (risks) → `docs/production/technical-debt.md`
(debt) → the tests in `desktopApp/src/jvmTest/...` or `core/src/commonTest/...`.
Honest statuses per §64: NOT STARTED / PLANNED / PROTOTYPE / PARTIAL / FUNCTIONAL /
INCOMPLETE / NEEDS REFACTOR / BLOCKED / STABLE.

## Related

- Full tree: `docs/README.md` · Glossary: `docs/reference/glossary.md`
- Audit: `docs/production/project-audit.md`
