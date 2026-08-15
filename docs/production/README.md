# Kaiteyo Production (planning layer)

**Status**: LIVE planning documents. This directory holds the *delivery* view:
phases, dependencies, risks, debt, and the project audit. It is the operational
complement to the vision layer (`docs/vision/`) and the architecture layer
(`docs/architecture/`).

## Document map

| Document | Covers | Source |
|---|---|---|
| [phases.md](phases.md) | Dependency-aware implementation phases + the implementation graph (§51–§52) | STANDARDS §365 |
| [risk-register.md](risk-register.md) | Major risks with probability/impact/mitigation/trigger/owner/status (§62) | STANDARDS §62 |
| [technical-debt.md](technical-debt.md) | Deliberate paydowns and known structural debt | TODO.md, PRODUCT_AUDIT.md |
| [project-audit.md](project-audit.md) | The §63 final audit: what exists / partial / broken / missing / duplicated / weak / good / preserve / refactor / replace / build-next + TOP-100 tasks + TOP-100 risks | §63 |

## Relationship to other planning docs

- `docs/planning/TODO.md` — the master TODO (task-level; categorized P0–P4, type,
  dependencies, acceptance criteria). `project-audit.md` TOP-100 tasks reference
  it rather than duplicating.
- `docs/planning/CURRENT_ISSUES.md` — the living bug/issue tracker.
- `docs/planning/ENGINEERING_AUDIT.md` — the §376 agent handoff (start here for
  implementation).
- `docs/planning/PRODUCT_AUDIT.md` — code-level truth (what is real/duplicated/
  dead/fake).
- `docs/roadmap/ROADMAP.md` — release view.

## Ground rules

1. **Dependency-aware, not fantasy**: phases are a graph, never "Phase 1: build
   everything" (§51).
2. **Honest statuses**: NOT STARTED / PLANNED / PROTOTYPE / PARTIAL / FUNCTIONAL /
   INCOMPLETE / NEEDS REFACTOR / BLOCKED / STABLE (§64) — never "complete" for a
   button.
3. **No fake completeness**: the audit says what is real and what is not; nothing
   here claims shipped for unshipped systems.
4. Every risk has an owner and a trigger; every debt has a paydown condition.
