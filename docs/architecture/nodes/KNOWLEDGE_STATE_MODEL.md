# User Knowledge State Model

**Status**: TARGET (specified; FSRS-5 review state exists, the knowledge layer does not)
**Source spec**: [Node Architecture master spec](../NODE_ARCHITECTURE.md) §84–§85, §152
**Related**: `docs/data/ARCHITECTURE.md` (current DBs), STANDARDS §210–§214 (events/stats),
STANDARDS §6 never-change list (SRS algorithm logic).

## 1. Core principle

**Knowledge is per-dimension, per-node, and event-derived.**

- A `user_knowledge` node exists per (user, language node, dimension). `食|reading` and
  `食|writing` are different states — that is what expresses "I recognize 食 but cannot
  write it" (§84).
- States are **derived from evidence** (events, reviews, interactions) — never from
  user self-rating as the source of truth (STANDARDS §210, §290).
- The knowledge layer **never replaces FSRS-5 scheduling**. FSRS decides *when* a card is
  reviewed; knowledge state records *what the user can demonstrably do* with a node.

## 2. Dimensions

| Dimension | What it measures | Typical evidence |
|---|---|---|
| `reading` | can read the node (surface→sound/meaning) | recognition review, lookup, subtitle exposure |
| `writing` | can write the node from memory | writing practice, production review |
| `listening` | recognizes it by ear | audio exposure, dictation, media listening |
| `recognition` | passive recognition (see → know) | review success, exposure |
| `production` | active production (know → recall/use) | production review, writing, composition |
| `context` | understands it in context (sentence/scene) | sentence/media/Journey exposure |
| `meaning` | knows the meaning | lookup, review, glossary use |
| `pronunciation` | can pronounce correctly | TTS practice, pronunciation review |

Every dimension has a state; a node may be **relevant** for only some dimensions
(e.g. pitch for vocab, not for a radical). Irrelevant dimensions are not tracked.

## 3. State machine

States (§84) with transition rules. States are ordered by strength but movement is not
strictly linear (review failures regress; dormancy is a side-door).

```
UNSEEN ──exposure──▶ ENCOUNTERED ──study/lookup──▶ EXPOSED
EXPOSED ──first success──▶ LEARNING
LEARNING ──success──▶ FAMILIAR ──▶ RECOGNIZED ──▶ STRONG ──▶ MASTERED
LEARNING ──weak performance──▶ WEAK
RECOGNIZED ──production/writing success──▶ PRODUCED / WRITING_CAPABLE
MASTERED ──no activity──▶ DORMANT ──failed recall──▶ FORGOTTEN (→ ENCOUNTERED/LEARNING)
STRONG ──failed recall──▶ LEARNING (re-entry)
```

### State semantics

| State | Meaning | Typical trigger (evidence) |
|---|---|---|
| `UNSEEN` | never encountered | initial state |
| `ENCOUNTERED` | seen but not studied | dictionary lookup, subtitle glance, Journey exposure (passive) |
| `EXPOSED` | actively exposed / context shown | glossary opened, "want to learn more" (§112), listening |
| `LEARNING` | in active learning loop | first review scheduled/completed |
| `WEAK` | performing below expectation | repeated review failures, long overdue + miss |
| `FAMILIAR` | stable recognition, not yet strong | several spaced successes |
| `RECOGNIZED` | reliable passive recognition | recognition reviews at growing intervals |
| `LISTENING_RECOGNIZED` | recognized by ear | listening review success |
| `READING_RECOGNIZED` | recognized in reading | reading review success |
| `PRODUCED` | can produce actively | production review success |
| `WRITING_CAPABLE` | can write | writing practice success (stroke evaluator) |
| `STRONG` | high stability/retrievability | long-interval successes (FSRS stability high) |
| `MASTERED` | terminal for a dimension | sustained strong across dimension's evidence types |
| `DORMANT` | previously mastered/strong, inactive | long inactivity (configurable, activity-signal based — STANDARDS §212) |
| `FORGOTTEN` | recalled incorrectly after mastery | failed review while STRONG/MASTERED/DORMANT |

### Transition rules (specified, implementable)

1. Each transition records: `from`, `to`, `trigger event type`, `eventId`, `timestamp` —
   full history retained (evidence ledger).
2. A transition requires the *strongest applicable evidence*: a failed recall overrides a
   prior success (recency-weighted, not monotonic).
3. `DORMANT` → `FORGOTTEN` requires a demonstrated recall failure (not mere inactivity).
4. Thresholds (e.g. "N spaced successes", "interval ≥ X days") are configuration
   constants owned by the knowledge service — documented, tested, and versioned with
   `schemaVersion`.

## 4. FSRS integration

| Concern | Owner |
|---|---|
| Review scheduling (when, interval, ease) | FSRS-5 (CURRENT — never change algorithm logic, STANDARDS §6) |
| Review results | `review` events + `card.srsState` (CURRENT) |
| Knowledge state per dimension | knowledge service (TARGET — this document) |

- FSRS review success/failure feeds the **trigger events** that move knowledge states;
  the knowledge layer reads review outcomes but never writes SRS fields.
- Reverse: knowledge state may *inform* card selection heuristics (e.g. prefer cards whose
  nodes are `WEAK`/`FORGOTTEN`), but the scheduler's arithmetic stays FSRS-owned.

## 5. Scoring model (§85)

- **Derived, not stored as truth**: scores aggregate per-dimension states of member nodes,
  weighted by: node frequency (§80 `has_frequency`), state strength, and evidence recency.
  Standard aggregation pipeline: events → aggregation → derived metrics (STANDARDS §213).
- **Display surface** (simplified):
  - Kanji (recognition + writing dials)
  - Vocabulary (recognition + production dials)
  - Listening, Reading, Grammar, Pitch, Contextual understanding (per §85)
  - Labels: "estimated"/"based on studied content" whenever data is partial (§290).
- **Never** one fabricated "Japanese level" percentage unless labeled as an estimate with
  its basis.

## 6. Data model (target)

Storage decision belongs to ADR-0013. Sketch (SQL-flavored, subject to that ADR):

```sql
-- one row per (user, language_node, dimension)
user_knowledge (
  id, user_id, node_id, node_type,
  dimension,               -- reading | writing | listening | ...
  state,                   -- UNSEEN..FORGOTTEN
  state_since, updated_at,
  evidence_ledger_ref      -- link to knowledge_transitions
)

knowledge_transitions (
  id, knowledge_id, from_state, to_state,
  trigger_event_id, trigger_type, event_time, payload_json
)

knowledge_score (
  user_id, dimension, score, basis, computed_at, schema_version
)  -- derived cache; always rebuildable from events
```

Constraints: unique (user_id, node_id, dimension); FK to events; transitions immutable
(append-only). Offline-first (STANDARDS §182) and syncable (STANDARDS §271) like other
user data.

## 7. Acceptance criteria

1. From any node, the system answers: state per dimension, last transition, history,
   contributing evidence — within UI latency budgets.
2. "Recognize 食 but not write it" and "recognize 食べる but not produce 食べられない"
   are representable and observable in UI (dials differ per dimension).
3. No UI self-rating feeds the machine as truth; ratings are auxiliary signals only.
4. FSRS scheduling logic is untouched by this layer (STANDARDS §6).
5. Scores never claim precision beyond their evidence (§290).
6. The §150 loop closes: Journey/media exposure creates `encountered_by` evidence that
   actually moves states, and states drive review selection and stats.
