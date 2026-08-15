# Learning Philosophy

**Status**: LIVE — the learning doctrine. This is the contract for every learning
feature: study engine, SRS, knowledge model, statistics, exams, curriculum (target).

## Core doctrine

1. **Knowledge is evidence, not self-report.** What a user "knows" is derived from
   observable events — reviews, writing attempts, exam answers, lookups, media
   exposure, world interactions — never from a self-rated "I know this" toggle as
   the source of truth (ADR-0016). Self-rating is a signal, never truth.
2. **Domain-first.** Learning logic lives in domain/application layers, never inside
   screens. UI consumes results (§177). A kanji knowledge calculation must not live
   in `KanjiScreen.kt`.
3. **The scheduler and the knowledge model are separate.** FSRS decides *when* to
   review; UserKnowledge records *what the user can demonstrably do* (§84–§85,
   KNOWLEDGE_STATE_MODEL). Never conflate them; never change FSRS logic (standards
   "never change" list).
4. **Honest numbers or no numbers.** No fabricated SRS state, no fake statistics,
   no invented JLPT scores, no fake coverage. Every number traces to persisted
   events. Estimates are labeled estimates (confidence labels, "approximate"
   annotations).
5. **Offline-first learning.** Study, review, exams, stats, media, mining all work
   offline (§182).
6. **Respect the learner.** The app is a tool for capable adults. It explains *why*
   (intervals, stages, difficulty) instead of hiding magic behind gamification.

## What learning means here

Learning is **multi-dimensional**. A kanji is not known/unknown — it has per-dimension
state (reading, writing, listening, recognition, production, context, meaning,
pronunciation; §84). "I can recognize 食 but not write it" must be expressible and
acted upon. This is the knowledge state model (`docs/architecture/nodes/KNOWLEDGE_STATE_MODEL.md`).

## Evidence sources (all feed the knowledge model)

| Source | Evidence produced |
|---|---|
| Flashcard review | SRS ratings, intervals, lapse data |
| Writing practice | per-stroke accuracy (shape/direction/order) via StrokeEvaluator/KanjiVG |
| Exams | per-question results, per-section analytics, study-vs-exam gap |
| Reading | exposure events, lookups (dictionary popup usage) |
| Media | subtitle selections, mining, watch progress |
| Dictionary lookup | looked-up terms (a *signal*, weighted lower than production evidence) |
| Quest/world interaction (target) | WORLD_TEXT_SELECTED → analyzer → knowledge update |
| Listening | pronunciation/TTS interactions, audio comprehension (target) |

## The learning loop (what the app optimizes)

```
EXPOSE → (encounter in context: media, world, reading)
  ↓
UNDERSTAND → (dictionary popup, glossary, exam/lesson)
  ↓
PRACTICE → (review, writing, drills, exams)
  ↓
PRODUCE → (writing, speaking, exam production items)
  ↓
REMEMBER → (FSRS schedules; intervals grow with demonstrated retention)
  ↓
EXPLORE MORE → (recommendation: next media, next quest, next lesson)
```

Every step emits events; events update knowledge and statistics; statistics inform
recommendations; recommendations shape the next exposure. One loop, one graph
(§67 content graph, `docs/architecture/NODE_ARCHITECTURE.md` §149–§150).

## Progress model principles

- **No single "level."** JLPT band, frequency coverage, per-dimension mastery, and
  accuracy are separate dials (§85). Display each honestly; never collapse into one
  fake number.
- **Streaks are a side effect of good habits, not the goal** (and never the core
  loop). Heatmap intensity derives from real activity (study time, reviews, new
  knowledge, media, exams) — not clicks.
- **Study time is engagement-based**, never app-open time (§17 spec, `docs/architecture/statistics.md`
  — ActivityTracker with AFK detection is implemented).
- **Retention over pace.** FSRS-5 optimizes long-term retention; the UI frames
  "interval grew" as success, not "more cards done."

## Difficulty is multi-dimensional (§69)

JLPT band is one axis. A location can be N5 vocabulary but culturally advanced; a
story can be N3 grammar with easy vocabulary. Curriculum and recommendations must
weigh: vocabulary difficulty, grammar difficulty, cultural/contextual difficulty,
reading speed, listening difficulty — independently.

## Child learning (target — §115, `docs/vision/child-experience.md`)

Children share the same core (SRS, knowledge model, events) but get a different
instructional structure: age-appropriate curriculum, visual language, audio
dependence, handwriting-first, character-led quests, no abstract statistics.
Instructional structure differs; *technology does not* (§71).

## What the learner never sees

- Fabricated data or statistics (enforced: `docs/planning/PRODUCT_AUDIT.md` fake-data
  cleanups).
- A knowledge estimate presented as a certification (JLPT coverage = "estimated study
  coverage", labeled approximate).
- SRS magic presented as fate — stages and intervals are explainable and shown.

## Related

- Study engine architecture: `docs/architecture/study-engine.md`
- Knowledge state model: `docs/architecture/nodes/KNOWLEDGE_STATE_MODEL.md`
- Statistics: `docs/architecture/statistics.md`; exams: `docs/architecture/exams.md`
- ADR-0006 (FSRS-5), ADR-0016 (event-driven user knowledge)
- Standards §210–§214 (events, stats, AFK, heatmap), §216 (unit tests), §290 (no
  fabricated precision)
