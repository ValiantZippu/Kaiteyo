# 📋 planning — Kaiteyo Project Planning

This directory is the operational brain of the project: task lists, the issue tracker,
completed work, and the idea backlog. **Planning stays separate from user documentation.**

## Status taxonomy

Every item tracked in planning files is labeled with **one** category:

| Category | Meaning |
|---|---|
| `TODO` | Task with a clear definition of done, waiting to be scheduled |
| `KNOWN ISSUE` | A defect or limitation acknowledged but not yet scheduled for a fix |
| `BUG` | A reproducible defect actively being fixed or investigated |
| `FEATURE` | A new capability, specified and scheduled |
| `RESEARCH` | Open question — needs investigation/design before it can be specified |
| `TECHNICAL DEBT` | A known structural cost (e.g. duplicated subsystems) to be paid down deliberately |
| `BLOCKED` | Waiting on something outside the code (platform access, data licensing, decision) |
| `DONE` | Completed — moved to `COMPLETED.md` (and noted in the changelog) |
| `DEFERRED` | Considered and intentionally postponed (recorded in `FUTURE_IDEAS.md` or here with a reason) |

## Tracking files

| File | Purpose | Categories it holds |
|------|---------|---------------------|
| [`PRODUCT_AUDIT.md`](PRODUCT_AUDIT.md) | Whole-repo product audit — what is real, duplicated, dead or fake | RESEARCH, TECHNICAL DEBT, decision records |
| [`ENGINEERING_AUDIT.md`](ENGINEERING_AUDIT.md) | Engineering audit & agent handoff — §376 deliverables (architecture map, dependency map, implementation order, risks, starting files) | decision records, handoff |
| [`CURRENT_STATE.md`](CURRENT_STATE.md) | Per-subsystem status matrix — what exists, where, known problems, risks (MASTER §5) | status matrix, RESEARCH, TECHNICAL DEBT |
| [`OVERHAUL_BACKLOG.md`](OVERHAUL_BACKLOG.md) | Spec-derived overhaul backlog — maps the four product directives (core intelligence, UI/UX audit, media, world) onto the repo; new KT-NAV/THEME/DEBUG/CARD/LEVEL/SEARCH/GRAPH/RAD/SENT/DATA/AI/UI/DOC/TEST work packages | TODO, FEATURE, RESEARCH, TECHNICAL DEBT, BLOCKED |
| [`MASTER_TODO.md`](MASTER_TODO.md) | Hierarchical project inventory — work packages P0–P39, KT-* task IDs (MASTER §6, §81) | TODO, FEATURE, RESEARCH, TECHNICAL DEBT, BLOCKED |
| [`TODO.md`](TODO.md) | Operational task list, priority-ordered (short-list over `MASTER_TODO.md`) | TODO, FEATURE, RESEARCH, TECHNICAL DEBT, BLOCKED |
| [`CURRENT_ISSUES.md`](CURRENT_ISSUES.md) | Living issue tracker | BUG, KNOWN ISSUE, BLOCKED, DONE (recently fixed) |
| [`COMPLETED.md`](COMPLETED.md) | Shipped work by version | DONE |
| [`FUTURE_IDEAS.md`](FUTURE_IDEAS.md) | Idea backlog (not commitments) | DEFERRED, RESEARCH, FEATURE (unprioritized) |
| [`CHANGELOG.md`](../../CHANGELOG.md) | **Moved to the repository root** — release history | DONE (per release) |
| [`../roadmap/ROADMAP.md`](../roadmap/ROADMAP.md) | Milestones and version plan | FEATURE (planned), DONE (historical) |
| [`../roadmap/PROJECT_VISION.md`](../roadmap/PROJECT_VISION.md) | Mission, philosophy, non-goals | — |

## Priority levels

| Priority | Meaning |
|---|---|
| 🔴 P0 | Critical — blocks usability, fix immediately |
| 🟡 P1 | High — important for the next release |
| 🟢 P2 | Medium — planned for the current milestone |
| 🔵 P3 | Low — nice to have |

## Workflow

1. **Report** a problem → add/update it in `CURRENT_ISSUES.md` with the category `BUG` or
   `KNOWN ISSUE` (or open a GitHub issue for external reports).
2. **Plan** work → items live in `TODO.md` with a category + priority.
3. **Implement** → P0 first; verify with `./gradlew :core:allTests` and
   `:desktopApp:compileKotlinJvm`; update docs.
4. **Close** → mark `DONE`, move to `COMPLETED.md`, add a `CHANGELOG.md` entry, and update
   `CURRENT_ISSUES.md` (fixed issues are recorded there with a link/note).
5. **Defer** → move the item to `FUTURE_IDEAS.md` (or record the reason here) instead of
   letting it rot in the TODO list.

## Related

- Vision & roadmap: `../roadmap/`
- Solved-issue knowledge base: `../troubleshooting/`
- Release process: `../releases/RELEASE_PROCESS.md`
