# Progress Model

**Status**: LIVE core (KnowledgeProfileEngine + event statistics) + TARGET (full
model per ADR-0016). **Source**: expansion spec §41; NODE §84–§85; ADR-0016;
`docs/architecture/nodes/KNOWLEDGE_STATE_MODEL.md` (the authoritative spec).

## Principle

Knowledge is **not know/don't-know**. Per node (kanji, word, grammar, sentence),
per dimension (reading, writing, listening, recognition, production, context,
meaning, pronunciation), there is a state machine — and state is derived from
**evidence events**, never self-rating (§41, ADR-0016).

## Knowledge states (expansion §41, KNOWLEDGE_STATE_MODEL)

```
UNKNOWN → SEEN → RECOGNIZED → PARTIALLY KNOWN → LEARNING → FAMILIAR → MASTERED
   ↕              ↕                ↕                ↕
UNSTABLE     FORGOTTEN        RELEARNING      (any state can decay)
```

- **UNKNOWN**: no evidence.
- **SEEN**: exposure only (a lookup, a subtitle, a world sign).
- **RECOGNIZED**: recognition demonstrated (reading recognition).
- **PARTIALLY KNOWN**: some dimensions demonstrated, others not.
- **LEARNING**: active practice evidence accumulating.
- **FAMILIAR**: stable recognition + some production.
- **MASTERED**: production + recognition across evidence types at criteria.
- **UNSTABLE / FORGOTTEN / RELEARNING**: decay paths — FSRS-driven review shows
  lapses; state regresses honestly (never "mastered forever").

The full state machine (per-dimension transitions, evidence weights) is the
canonical spec: `docs/architecture/nodes/KNOWLEDGE_STATE_MODEL.md`. This document
is the product-level view.

## How evidence updates knowledge (expansion §41)

| Evidence source | Weight | Updates |
|---|---|---|
| Flashcard review (FSRS ratings) | high | retention, intervals, stability |
| Writing attempt (stroke evaluation) | high | writing dimension, production |
| Exam answers | high | per-skill recognition/production |
| Reading (comprehension, gloss use) | medium | reading dimension |
| Media exposure (subtitles watched, selections) | medium | exposure → SEEN/RECOGNIZED |
| Dictionary lookup | low | exposure signal (SEEN) |
| World interaction (signs read, dialogue) | medium | exposure + discovery |
| Listening (TTS/audio, comprehension) | medium | listening dimension |
| Mining (card created) | low-medium | intent signal, deck membership |

- **Confidence**: each state estimate carries confidence; low-confidence states
  are labeled ("estimated") per standards §290.
- **Decay**: no evidence over time → state decays (FSRS intervals model the when;
  knowledge state reflects the what).

## The dials (NODE §85 — display)

- Per-dimension mastery dials (read/write/listen/recognize/produce/context).
- JLPT coverage (labeled approximate — a study-based estimate, never a
  certification claim).
- Frequency coverage (top 1k/2k/5k/10k from real frequency metadata).
- Measured accuracy where real attempts exist.
- Overall confidence label (data-driven, honest).

Display rule: never collapse knowledge into a single fake number; the dashboard's
knowledge snapshot shows the dials as separate, honest bars.

## FSRS vs knowledge (never conflated)

- **FSRS** (ADR-0006, unchanged): *when* to review. Scheduling logic is in the
  "never change" list.
- **Knowledge model**: *what the user can demonstrably do*.
- They talk: FSRS lapses feed decay; knowledge mastery feeds interval growth
  expectations — but neither replaces the other.

## Statistics relationship

- Stats derive from the same events (§210–§213): heatmap = real activity
  (study time via ActivityTracker, reviews, new knowledge, exams, media, mining).
- The progress model is the "knowledge" view; statistics is the "activity" view;
  both read the same event stream.

## Contracts

- `ProgressService.state(node, dimension) → KnowledgeState` (derived, read-only)
- `ProgressService.update(events)` — the ONLY writer; called from event
  consumers, never from UI directly
- Scheduler (`SrsScheduler`) remains an independent engine — the progress model
  consumes its lapses, never modifies it

## Acceptance criteria

1. Knowledge state transitions are pure functions of evidence events
   (testable, deterministic).
2. No UI can write knowledge state directly (single writer guard).
3. Self-rating, where present, is a *signal* — never the source of truth.
4. All displayed dials carry honest labels and trace to real evidence.

## Related

- Canonical model: `docs/architecture/nodes/KNOWLEDGE_STATE_MODEL.md`
- Decision: ADR-0016 · Events: `docs/architecture/nodes/EVENT_CATALOG.md`
- Stats: `docs/architecture/statistics.md` · SRS: `docs/architecture/study-engine.md`
- Vision: `docs/vision/learning-philosophy.md`
