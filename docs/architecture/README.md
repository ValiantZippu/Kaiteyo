# 🏗️ architecture — Kaiteyo Architecture Documentation

Every major subsystem has an owner document here (STANDARDS §175). Start with
[`OVERVIEW.md`](OVERVIEW.md) for the module/UI/data-flow map, then the subsystem docs
below. Decisions and their rationale live in [`decisions/`](decisions/README.md) (ADRs).

## Subsystem docs

| Document | Covers | Status |
|---|---|---|
| [`OVERVIEW.md`](OVERVIEW.md) | Modules, dependency direction, UI architecture, navigation, data flow | current |
| [`FILE_STRUCTURE.md`](FILE_STRUCTURE.md) | Repository layout reference | current |
| [`database.md`](database.md) | Database specification & migration plan (two SQLDelight DBs + DataStore + suite JSON) | current |
| [`language-model.md`](language-model.md) | Language data model & knowledge graph status | current |
| [`dictionary.md`](dictionary.md) | Dictionary engine spec (Yomitan-compatible glossary) | current |
| [`study-engine.md`](study-engine.md) | Study engine & card model (notes → card types → cards, FSRS-5) | current |
| [`statistics.md`](statistics.md) | Statistics & event model (event-driven, AFK model, heatmap) | current |
| [`exams.md`](exams.md) | Exam system spec (generation, JLPT simulation, analytics) | current |
| [`media.md`](media.md) | Media, playback & subtitle engine (backend abstraction) | current |
| [`mining.md`](mining.md) | Mining workflow spec (payload, sources, pipeline) | current |
| [`integrations.md`](integrations.md) | Integrations spec: Anki, Yomitan, local API, browser, plugins | current |
| [`browser.md`](browser.md) | Embedded browser architecture (planned; §198/§360) | planned |
| [`journey.md`](journey.md) | Journey, world & game runtime plan | target (not started) |
| [`localization.md`](localization.md) | Localization plan (Strings interface, EN/JA) | current |
| [`performance.md`](performance.md) | Performance strategy & budgets | current |
| [`toolchain.md`](toolchain.md) | Toolchain & development environment (pinned) | current |
| [`backup.md`](backup.md) | Backup, import & export | current |
| [`ci-cd.md`](ci-cd.md) | CI/CD & release pipeline | current |
| [`accessibility.md`](accessibility.md) | Accessibility plan | partial |
| [`content.md`](content.md) | Content system & authoring | foundation + planned |
| [`assets.md`](assets.md) | Asset system specification | current |
| [`DATA_PLATFORM.md`](DATA_PLATFORM.md) | KJD language data platform (jdata) | current |
| [`SYNC.md`](SYNC.md) | Sync architecture (GitHub provider) | current |
| [`ACCOUNT.md`](ACCOUNT.md) | Account structure (GitHub device flow) | current |
| [`NAVIGATION.md`](NAVIGATION.md) | Navigation system (NavShell, floating + sidebar) | current |
| [`NODE_ARCHITECTURE.md`](NODE_ARCHITECTURE.md) | Node system & Journey product master spec (§76–§162) | target |
| [`nodes/`](nodes/README.md) | Node registries, knowledge model, world schema, gameplay systems, runtime spec, authoring, UX flows, test plan, service contracts | target |
| [`decisions/`](decisions/README.md) | Architecture Decision Records (ADR-0001…0018) | current |

## How these docs relate

- **Data flow**: `database.md` → `language-model.md` → `dictionary.md` / `study-engine.md`
  → `statistics.md` / `exams.md`.
- **Immersion loop**: `dictionary.md` + `media.md` + `mining.md` + `integrations.md` +
  `browser.md` (the select → lookup → mine → card loop).
- **Future systems**: `journey.md` + `nodes/` + `content.md` + `browser.md` are the
  target architecture (ADR-0013/0014/0015); nothing there is claimed as shipped.

**Freshness rule** (§336): when a subsystem's code changes, update its doc here — code
says A, docs say B is a bug (see `docs/development/DocumentationRules.md`).
