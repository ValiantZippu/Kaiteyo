# ADR-0012: Professional Engineering Standards

**Status**: Accepted
**Date**: 2026-08

## Context

Kaiteyo grew quickly through long AI-assisted development sessions. The product audit
(`docs/planning/PRODUCT_AUDIT.md`) found duplicate subsystems, unreachable "shadow"
implementations, and fake or placeholder code that had been presented as features. Many
important architectural decisions lived only inside AI conversations and were lost between
sessions, causing agents to re-derive (or contradict) prior decisions.

The project needed a constitution: a written standard that governs how every change — human
or AI — is made, validated, and handed off, so the repository itself becomes the project's
permanent memory.

## Decision

Adopt the Kaiteyo Engineering Standards (`docs/engineering/ENGINEERING_STANDARDS.md`,
sections §163–§376) as the binding engineering contract for the project. In particular:

- **No more vibe-coding.** No fake implementations, placeholder buttons as features,
  hardcoded product data, fabricated statistics, or invented APIs/dependencies (§325–§329).
- **Understand before changing.** Agents must inspect the repository, docs, dependencies,
  and Git history before writing code, and follow the 10-phase workflow (§165, §173).
- **Never rewrite blindly.** Architecture replacement requires a documented
  current/problem/proposed/migration/benefit/risk analysis (§166).
- **Use established infrastructure** unless Kaiteyo genuinely needs custom behavior
  (§164, §193, §242, §363).
- **Validation ladder.** Use the cheapest useful validation; documentation and planning
  passes must NOT trigger Gradle builds (§341–§342).
- **Agent handoffs.** Every agent leaves a WHAT/ARCHITECTURE/DATABASE/DEPENDENCIES/TESTS/
  KNOWN ISSUES/NEXT STEPS report (§174), and the stop-condition deliverables (§376) end
  each planning phase.
- **Repository memory.** Decisions made in conversation must be moved into the repository
  (§374). `docs/README.md` is the canonical index — no orphan documents (§335).

## Alternatives

- **Informal guidelines only** — rejected: the product audit showed this produced
  duplicate subsystems and unreachable placeholder code.
- **Heavy process tooling** (mandatory code-review bots, gate-keeping CI, JIRA-style
  tracking) — rejected: overkill for the current team size; the standard is process-light
  but explicit about behavior.
- **Blanket rewrite of the repository** to match a clean-room architecture — rejected
  explicitly (§166, §370): the existing architecture is largely sound and working; the
  standard governs how it is consolidated, not replaced.

## Consequences

- Future agents (and humans) are expected to read the standard before working; the audit
  handoff (`docs/planning/ENGINEERING_AUDIT.md`) is the current map of the repository.
- Existing debt items (duplicate SRS/settings/statistics/nav, dead code, two-app question)
  are now tracked against this standard; consolidation is deliberate work, not silent
  cleanup.
- Documentation and planning passes no longer trigger compiles; code changes use the
  validation ladder (§342).
- The standard's numbering (§NNN) is a stable reference; new decisions may extend it.

## Implementation notes

- `docs/engineering/ENGINEERING_STANDARDS.md` — the standard itself.
- `docs/planning/ENGINEERING_AUDIT.md` — the §376 handoff (audit, architecture map,
  technology decisions, dependency map, implementation order, risks, open questions,
  first milestone, starting files).
- `docs/planning/PRODUCT_AUDIT.md` — detailed product audit (real/duplicate/dead/fake).
- `docs/README.md` — documentation index; kept in sync (no orphan documents).
