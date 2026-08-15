# ADR-0019: Website as Project Command Center

**Status**: Proposed — the static render is live; the interactive backend is a
documented contract awaiting acceptance.
**Date**: 2026-08

## Context

The MASTER blueprint (§0–§88 of the website spec) asks the website to become the
public source of truth for Kaiteyo's development: Wiki, Whiteboard, Kanban,
Roadmap, Suggestions with a protected approval workflow, Decisions, Changelog,
and an activity feed — all sharing one project data model (spec §44, §45, §66).

The current website is a static Python build consuming the docs corpus. It has
no server, database, or authentication. Building a fake frontend-only Kanban or
a suggestion form that goes nowhere would violate the spec's own rules
(§70, §74): every feature must work or have a documented backend contract.

The corpus already *is* the single source of truth for planning data:
`docs/planning/MASTER_TODO.md` (tasks), `docs/architecture/decisions/` (ADRs),
`docs/roadmap/ROADMAP.md` + `docs/planning/CURRENT_STATE.md` (statuses).
Duplicating that state into a website database would create the exact split the
blueprint forbids.

## Decision

Adopt a **two-layer architecture**:

1. **Static render from the corpus (live today).** The site renders real data
   at build time: the Kanban parses `MASTER_TODO.md`; the Decisions page parses
   the ADR directory; system/roadmap/whiteboard/suggestion/activity data live
   in `website/config/project/*.json` as hand-maintained mirrors of the corpus,
   with binding honesty rules (ARCHITECTURE.md §4). The whiteboard is an
   interactive read-only canvas (pan/zoom/semantic LOD/click-to-docs). The
   Kanban is filterable. No fabricated tasks, suggestions, or events.
2. **Documented interactive contracts (planned).** `docs/website/API.md`
   defines the versioned `/api/v1` endpoints (auth/roles, kanban PATCH with
   corpus write-back, whiteboard CRUD, suggestion state machine with
   provenance, activity, notifications, realtime, search, GitHub sync) and
   `docs/website/DATA_MODEL.md` defines the target schema. The static pages
   remain the fallback rendering; the frontend progressively enhances.

**Why two layers instead of building the backend now**: the spec §75 explicitly
bars turning this into a build operation, no backend infrastructure exists, and
the corpus-first render already delivers the public value (a real, honest
command center) with zero server risk. The backend is a well-scoped next step
with a complete contract to build against.

## Alternatives

- **Build the full interactive backend now** — rejected for this pass: no
  existing server/infra, spec §75, and the contract-first approach is strictly
  additive (the static site is not thrown away).
- **Frontend-only Kanban with local persistence** — rejected: violates
  §70/§74 ("do not use static JSON pretending to be persistence"), and silent
  local-only writes would mislead contributors about the official plan.
- **Mirror project state into a website database now, keep corpus** —
  rejected: creates two writers for the same truth; corpus remains canonical
  until a backend proves conflict handling (§44).
- **Do nothing** — rejected: the blueprint (§1, §66) makes the command center
  the public brain of the project; the two-roadmap and status confusion on the
  old site is real.

## Consequences

- The command center is live at `/project/**` with honest statuses; the
  Activity feed is a dated real snapshot; Suggestions point to the working
  GitHub issue channel until the backend ships.
- Future work: implement API.md in the documented order (auth → suggestions →
  accept→plan conversion → kanban PATCH → activity → whiteboard CRUD →
  notifications → realtime → search). The static pages and `config/project/*`
  remain the render path and fallback.
- Maintenance: any status/task change must keep corpus and
  `config/project/*.json` consistent (procedures in ARCHITECTURE.md §6).
- Risk: hand-maintained JSON drift. Mitigation: verification checklist
  (ARCHITECTURE.md §8) + the corpus-first rule.

## Implementation notes

- `docs/website/ARCHITECTURE.md` — build flow, honesty rules, maintenance
- `docs/website/DATA_MODEL.md` — unified project data model + target schema
- `docs/website/API.md` — versioned API contracts for the interactive layer
- `website/build.py` — `build_project()`; `website/config/project/*.json`;
  `website/templates/layouts/project*.html`; `website/assets/scripts/whiteboard.js`, `kanban.js`
