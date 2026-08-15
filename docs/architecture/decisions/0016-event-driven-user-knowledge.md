# ADR-0016: Event-Driven User Knowledge (Dimensioned, FSRS-Owned Scheduling)

**Status**: Proposed — contract specified, not implemented
**Date**: 2026-08

## Context

The product promise is a connected language ecosystem: "recognize 食 but not write it",
"recognize 食べる but not produce 食べられない" (NODE §84). Today the only per-item
learning state is FSRS-5 card scheduling (`fsrs_card` in `UserDataDatabase`) plus
event logs; there is no per-dimension knowledge state, and statistics are event-derived
but knowledge itself is not modeled. Without a knowledge layer, Journey/media exposure
(NODE §150) can never demonstrably change what the user knows, and the §83 "where have I
seen this" question cannot be answered from a single source.

The engineering standard requires: statistics derived from events, never UI state
(STANDARDS §213); SRS algorithm logic never changed (STANDARDS §6); no fabricated
precision (STANDARDS §290); and every architectural decision recorded in the repository
(STANDARDS §374).

## Decision

Model **user knowledge as event-derived, per-dimension, per-node state**, per
`docs/architecture/nodes/KNOWLEDGE_STATE_MODEL.md`:

- A `user_knowledge` record exists per (user, language node, dimension) with a state
  machine (UNSEEN → ENCOUNTERED → EXPOSED → LEARNING → … → MASTERED → DORMANT →
  FORGOTTEN, with per-dimension terminal states like WRITING_CAPABLE).
- **States change only from evidence**: review outcomes, writing attempts, lookups,
  subtitle/media exposure, Journey encounters — all captured as typed events
  (`EVENT_CATALOG.md`). Self-rating is an auxiliary signal, never the source of truth.
- **FSRS-5 remains the scheduler.** FSRS decides when a card is reviewed and its
  interval/ease; the knowledge layer reads review outcomes and never writes SRS fields.
  Knowledge state may inform review *selection* heuristics only.
- Knowledge is derived; `knowledge_score` is a rebuildable cache over events, never a
  stored truth.
- Scores are presented honestly: per-dimension dials with "estimated / based on studied
  content" labels where the basis is partial (§290).

## Alternatives

- **Single mastery percentage per item** — rejected (§85): cannot express
  recognition-vs-production or per-dimension gaps; invites fabricated precision.
- **Fold knowledge into FSRS fields** — rejected (STANDARDS §6 never-change list): the
  scheduler's model exists to schedule reviews, not to answer "what can the user
  demonstrably do"; conflating them makes the SRS algorithm unchangeable and knowledge
  un-testable.
- **User self-rating as truth** — rejected: untrustworthy, non-standardized, and it
  would let Journey/media exposure never matter.

## Consequences

- The node layer (ADR-0013) and the event log (ADR-0016 storage sketch, `NODE_DATA_MODEL.md`
  §4–§5) are prerequisites; this ADR is implemented after ADR-0013 storage exists.
- Journey/media systems gain a real integration point: exposure events move states, and
  states drive review selection, stats, and difficulty adaptation (NODE §113).
- Transition thresholds are versioned configuration owned by the knowledge service —
  documented, tested, and migrated like any schema.
- Guard tests must prove FSRS scheduling arithmetic is untouched (TEST_PLAN §4).

## Implementation notes

- `docs/architecture/nodes/KNOWLEDGE_STATE_MODEL.md` — the state machine, dimensions,
  scoring, FSRS boundary, acceptance criteria
- `docs/architecture/nodes/NODE_DATA_MODEL.md` §4–§5 — storage sketch (user_knowledge,
  knowledge_transition, knowledge_score, event_log)
- `docs/architecture/nodes/EVENT_CATALOG.md` — the evidence event catalog
- `docs/planning/TODO.md` → Node & Journey build order, step 5 (after node model)
