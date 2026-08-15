# ADR-0017: One Product — Resolve the Two Parallel Applications

**Status**: Proposed — decision required; gates all suite-engine integration work
**Date**: 2026-08

## Context

The repository contains **two complete, independent applications** (verified in
`docs/planning/PRODUCT_AUDIT.md` §1):

| | Core app | Desktop suite |
|---|---|---|
| Entry | `desktopApp/.../Main.kt` `main()` → `KaiteyoApp` (also Android/iOS) | `desktopApp/.../SuiteMain.kt` `desktopSuiteMain()` → `KaiteyoDesktopSuite` |
| Reachable? | Yes — ships | **No** — no `main()`, nothing invokes it |
| UI | Compose MPP shared | Compose Desktop JVM-only |
| User data | SQLDelight `UserDataDatabase` (FSRS, decks, stats, migrations) | JSON under `~/.kaiteyo/` |
| Reference data | SQLDelight `AppDataDatabase` (bundled, kjd) | Imported Yomitan/JMdict dictionaries |
| SRS | FSRS-5 | suite `ReviewSession` |
| Duplicated | navigation, settings, theme, statistics, library, sync, account, import/export | — |

**Consequence**: every subsystem the product brief cares about (dictionary popup, media
center, mining, OCR, AnkiConnect, local API) exists *only* in the unshipped suite, and
the two apps maintain parallel copies of user data that never share anything. Both apps
cannot ship; the duplication map (PRODUCT_AUDIT §6) must reach zero live rows.

## Decision

Adopt **one product** with this structure (to be executed by the tasks in
`docs/planning/MASTER_TODO.md` KT-INFRA-001…003):

1. **The core app is the product shell** — its shared Compose MPP UI, navigation
   (`NavShell`), two-database architecture, FSRS study engine, statistics, sync, and
   account remain the foundation. It already ships on desktop/Android/iOS.
2. **The suite's unique engines are integrated into the product**, not shipped as a
   second app: dictionary (import + popup), media center, mining, OCR, AnkiConnect,
   local API, browser reader — over the **unified data model** (the SQLDelight
   user-data layer + the node/knowledge layer, ADR-0013).
3. **All duplicated subsystems converge on one implementation each**: navigation
   (NavShell), settings (PreferencesContract/DataStore), theme (core theme tokens),
   statistics (event ledger + StatisticsController), library, SRS (FSRS), sync
   (AccountManager/SyncManager). The losing implementation is removed (or archived as a
   reference) — never maintained in parallel.
4. **Suite JSON stores migrate** into the unified data layer (additive, backup-first;
   `docs/database/MIGRATIONS.md` §5).
5. **First-run is empty** — demo-data seeding is removed (no fabricated content).

## Alternatives

- **Ship the suite as the desktop product** (invert) — considered: the suite is
  feature-rich, but it duplicates the entire learning stack and is JVM-only; the core
  app already has the shared engine + mobile/iOS story. Rejected unless evidence shows
  migration cost is lower — the integration direction above preserves the shared core
  and brings the suite's unique engines in.
- **Keep both, feature-gated** — rejected: two data models, two SRS systems, two stats
  pipelines is exactly the incoherence the blueprint bans (MASTER §1, NODE §155); users
  and future agents cannot maintain a split product.
- **Do nothing (status quo)** — rejected: suite engines remain permanently unreachable;
  PRODUCT_AUDIT §1 stands as the top defect.

## Consequences

- Every suite-engine integration task (KT-DICT-002, KT-MEDIA-004, KT-MINE-002,
  KT-YOMI-002, KT-DESK-003) is gated on this ADR.
- The suite remains in the repo as a reference until its engines land in the product;
  then its parallel copies are removed per the duplication map.
- Short-term cost: porting suite engines onto the core data model + integrating into
  `NavShell`/`MainDestination` navigation. Long-term benefit: one product, one data
  model, all platforms — the connected ecosystem (MASTER §1).
- Risk: a long porting effort. Mitigation: engine-by-engine integration with each engine
  landing behind its own feature flag (STANDARDS §222), engines keep their existing
  tests during the port.

## Implementation notes

- `docs/planning/PRODUCT_AUDIT.md` — the evidence for this decision
- `docs/planning/MASTER_TODO.md` — KT-INFRA-001…003, KT-DB-004, KT-DICT-002,
  KT-MEDIA-004, KT-MINE-002, KT-YOMI-002, KT-DESK-003
- `docs/planning/CURRENT_STATE.md` — status matrix with the (core)/(suite) dimension
