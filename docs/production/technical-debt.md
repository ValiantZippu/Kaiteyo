# Technical Debt Register

**Status**: LIVE — deliberate paydowns. **Source**: `docs/planning/TODO.md`
(TECHNICAL DEBT section), `docs/planning/PRODUCT_AUDIT.md`.

## Debt items

| # | Debt | Impact | Paydown condition | Priority |
|---|---|---|---|---|
| 1 | **Two jdata implementations** — `kjd/` (standalone) and desktop `engine/jdata` evolved separately | data platform drift; duplicate logic | Unify into one pipeline (ADR-0007); kjd is the canonical | 🟡 P1 |
| 2 | **Two-app data duplication** — SRS/settings/statistics/nav/decks exist in core app AND desktop suite | divergence; double work; agent confusion | One-product consolidation decision (ENGINEERING_AUDIT §7-1) then merge data layers | 🔴 P0 |
| 3 | **No UI tests** — Compose UI test harness not established | regressions slip; UI claimed without verification | Establish a UI test harness; test critical flows (§218) | 🟡 P1 |
| 4 | **Platform actuals under-verified** — code-complete but runtime-unverified paths (iOS, Windows, Linux, Android SAF) | release risk; "works" claims unproven | BLOCKED list runtime sweeps (CURRENT_ISSUES) | 🔴 P0 (release gate) |
| 5 | **Website `dist/` committed** — manual build step per docs change | stale site; doc/index drift | CI regeneration of the website | 🟢 P2 |
| 6 | **Dead/shadow code** — `LearningPowerHub.kt`, `SyncSettingsUI.kt`, `BackupSystemExt.kt` path reachable only via dead hub | agent confusion; maintenance weight | Remove after consolidation decision (PRODUCT_AUDIT §5.2) | 🟡 P1 |
| 7 | **Scattered status tracking** — historical FEATURES.md/TODO.md/COMPLETED.md overlap | status ambiguity | FEATURES.md single source of truth (done); retire stale duplicates | 🟢 P2 |
| 8 | **Grammar/sentence data gap** — grammar content limited to starter deck; no open grammar dataset | curriculum breadth blocked | RESEARCH open grammar + Tatoeba datasets; KJD adapters (TODO.md) | 🟢 P2 |
| 9 | **No FTS/trigram search indexing** — dictionary search scans at scale | latency at full dataset | FTS/trigram indexes (STANDARDS §186) | 🟢 P2 |
| 10 | **Accessibility completeness** — keyboard nav, screen reader, high contrast, reduced motion partial | a11y gaps | A11y completeness pass (TODO.md P3) | 🔵 P3 |

## Debt rules

1. Debt is **recorded, owned, and paid** — not silently carried.
2. A paydown is scheduled by priority; P0/P1 paydowns gate releases.
3. New debt enters this register (or TODO.md) when introduced — the register is
   the memory, not the guilt.

## Related

- TODO: `docs/planning/TODO.md` (TECHNICAL DEBT section)
- Product audit: `docs/planning/PRODUCT_AUDIT.md`
- Audit: [project-audit.md](project-audit.md) · Risks: [risk-register.md](risk-register.md)
