# AI Agent Guide

> **Binding for all AI coding agents** working on Kaiteyo (MASTER §68, STANDARDS
> §172–§174, §370). Human contributors should follow the same rules. Read this **before
> touching any code**.

## 1. Identity

AI agents are **development assistants, not autonomous architects** (STANDARDS §172).
The architecture is the contract; the code must obey the contract. Agents never decide
architecture by fiat — decisions go through the ADR process (MASTER §7).

## 2. Mandatory read order (before any change)

1. Root `README.md` — what Kaiteyo is.
2. `docs/README.md` — the documentation map.
3. `docs/product/PRODUCT.md` — the master blueprint (§0–§88).
4. `docs/planning/CURRENT_STATE.md` — what exists / doesn't, with statuses.
5. `docs/engineering/ENGINEERING_STANDARDS.md` — the engineering contract (§163–§376).
6. `docs/development/AI_CONTEXT.md` — workflow + the **never-change list**.
7. `docs/architecture/OVERVIEW.md` — architecture map.
8. The subsystem doc for the area you're changing (`docs/architecture/README.md` index).
9. `docs/planning/MASTER_TODO.md` + `TODO.md` — what to build next.
10. `docs/planning/PRODUCT_AUDIT.md` + `ENGINEERING_AUDIT.md` — known defects and handoff state.

Then **inspect the actual code** — never assume docs are correct if code contradicts them
(if they contradict, fix the docs and file an issue; do not silently trust either side).

## 3. The 10-phase workflow (STANDARDS §173)

| Phase | Action |
|---|---|
| 1 | **Repository reconnaissance** — modules, entry points, build |
| 2 | **Relevant documentation reading** — the read order above |
| 3 | **Dependency analysis** — what your change touches (deps, data, platform) |
| 4 | **Architecture plan** — smallest correct surface; record in the PR/commit |
| 5 | **Implementation** — per CODING_STANDARDS; no fake anything |
| 6 | **Static analysis** — compile checks, no new warnings |
| 7 | **Targeted tests** — for what you changed (STANDARDS §215–§218) |
| 8 | **Documentation** — update docs in the *same* change (MASTER §69) |
| 9 | **Git diff inspection** — review your own diff: scope, secrets, dead code |
| 10 | **Handoff report** — what changed, files, architecture, DB, deps, tests, known issues, next steps (STANDARDS §174) |

## 4. Hard rules (STANDARDS §370)

- READ the documentation first. INSPECT the code second. PLAN third.
- **Change the smallest correct architectural surface.**
- **Do not destroy existing work.** No blind rewrites (STANDARDS §166).
- **Do not invent APIs or database schemas** without documenting them.
- **Do not add random dependencies** (STANDARDS §203 checklist).
- **Do not build unnecessarily.**
- **Do not claim implementation without implementing it.** No placeholder buttons as
  final features, no fake data, no fake statistics, no fake integrations
  (STANDARDS §290).
- **Do not leave broken navigation or crashes.**
- **Do not leave undocumented architectural changes.**
- TEST what you change. DOCUMENT what you change. REPORT what you could not finish.

## 5. Task selection

1. Open `docs/planning/MASTER_TODO.md` (full inventory) + `docs/planning/TODO.md`
   (operational, priority-ordered).
2. Pick the highest-priority task whose dependencies are all ✅/🚧 and whose package has
   no open 🔴 gate (see MASTER_TODO "Cross-cutting gates").
3. **Never start gated work**: Journey code before ADR-0018, suite integration before
   ADR-0017, plugin loading before the sandbox ADR, dataset adapters before license
   verification.
4. If the task is `RESEARCH`, do the investigation and write the decision (ADR or doc)
   — that *is* the deliverable.

## 6. What counts as done

- Definition of Done (AGENTS.md): compile passes, no new warnings, screens registered,
  UI follows the design system, strings added to both EN/JA, docs updated, issues
  updated.
- Acceptance criteria from the MASTER_TODO row and the subsystem's TEST_PLAN section are
  met — measured, not asserted.
- Handoff report written where the task spans subsystems.

## 7. Special zones — do not touch without explicit instruction

- SRS/FSRS scheduler logic and core learning logic (STANDARDS §6).
- SQLDelight `.sq` schemas (add migrations, never edit history).
- Package namespace `ua.syt0r.kanji`.
- Gradle build configuration unless the build is broken.

## 8. Documentation duties

- Update the docs that describe what you changed, in the same change (MASTER §69).
- Record solved issues in `docs/troubleshooting/` and fixed bugs in
  `docs/planning/CURRENT_ISSUES.md`.
- If you make an architectural decision, propose an ADR (MASTER §7).
- Never leave the repository's memory in your conversation — the repository is the
  permanent memory (STANDARDS §374).

## Related

- `docs/development/AI_CONTEXT.md` — the pre-existing agent workflow + never-change list
- `docs/development/VIBE_CODING_GUIDE.md` — AI-assisted development practice
- `docs/development/CODING_STANDARDS.md` — code style
- `docs/planning/ENGINEERING_AUDIT.md` — current handoff state and starting files
- `docs/product/PRODUCT.md` — MASTER §68
