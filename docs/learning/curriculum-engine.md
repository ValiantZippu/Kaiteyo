# Curriculum Engine

**Status**: TARGET (spec). **Source**: expansion spec §11; STANDARDS §365 (phases);
`docs/vision/learning-philosophy.md`.

## Principle

Curriculum is a **graph, not a linear list** (expansion §11). Content (kana,
kanji, vocabulary, grammar, sentences, reading, listening, speaking, writing,
comprehension, culture, media, real-world tasks) connects through prerequisites,
dependencies, recommended order, optional paths, mastery, review, remediation,
difficulty, learner profile, age profile, JLPT alignment, and non-JLPT learning.

## Curriculum graph model

```
NODE: lesson/unit/chapter (kana, kanji, vocab set, grammar point, reading, exam, task)
EDGES:
  requires        (prerequisite: must be *demonstrated* before unlock)
  recommends      (suggested next — soft, never blocking)
  teaches         (knowledge/competency targets)
  reinforces      (review/remediation content)
  optional        (parallel tracks: culture, media, writing, listening)
  aligns-to       (JLPT band, frequency band — metadata, not gates)
```

- **Prerequisites are evidence-based**: "unlock this lesson" = the learner
  demonstrated the required knowledge (via events — ADR-0016), not "clicked
  through."
- **Recommended order is a suggestion**: users may jump anywhere (the graph
  allows it); prerequisites *within a path* gate, the map never gates globally.
- **Optional paths**: media-driven (watch → learn), world-driven (find → learn),
  writing-first, listening-first — the same knowledge targets, different routes.

## Learner profile

| Dimension | Values | Purpose |
|---|---|---|
| Age profile | child band (2–12) / adult | content + presentation filter (`docs/vision/child-experience.md`) |
| Level profile | derived from knowledge model (per-dimension), JLPT alignment, frequency coverage | start/recommend content |
| Goal profile | JLPT candidate / traveler / media consumer / native-like / casual | recommendation weighting |
| Ability profile | reading/writing/listening/speaking strengths (from evidence) | remediation focus |

## Mastery, review, remediation

- **Mastery** = demonstrated production+recognition across evidence types at the
  knowledge model's criteria (`progress-model.md`) — never "seen N times."
- **Review** = FSRS-driven (when); curriculum supplies the *content* (what).
- **Remediation** = knowledge-model weakness detection (writing accuracy,
  exam gaps, lapse rates) generates targeted content: re-teach lessons, mistake
  decks, weak-spot quests (`docs/learning/adaptive-learning.md`).

## JLPT alignment — one axis, not the system

- Content carries JLPT alignment metadata (N5→N1) where the dataset supports it
  (KJD pipeline, `docs/data/SOURCES.md`).
- The app offers JLPT-aligned paths (the shipped JLPT decks, JLPT simulation
  exams) **and** non-JLPT learning: native-like, media-driven, world-driven,
  frequency-based. JLPT is a filter/label, never the whole curriculum
  (expansion §11, §15).

## Age-based curriculum (child)

- Age bands map to curriculum tiers (kana-first at 3–5, kanji families at 6–8,
  JLPT-adjacent bridge at 10–12) — see `docs/vision/child-experience.md`.
- Band is a default; **learner ability overrides age** (the profile's ability
  dimensions win).

## Curriculum ↔ world ↔ media

- The curriculum engine is a consumer of the same graph as the world: a lesson's
  "requires" can be satisfied by world encounters; a world quest can *teach*
  curriculum content. Content and curriculum are the same nodes (§68, §112).

## Contracts

- `CurriculumService.recommend(profile) → next-lessons` — never modifies
  knowledge directly; knowledge updates only via events.
- Prerequisite checks query the knowledge model (read-only); unlock state is
  derived, not stored twice.

## Acceptance criteria

1. Curriculum runs entirely from data (new curriculum = content, not code).
2. Prerequisites unlock on evidence, never on click-through.
3. Any user path is expressible; no global linear gate.
4. Remediation content generates from real weakness data.

## Related

- Progress: [progress-model.md](progress-model.md) · Adaptive: [adaptive-learning.md](adaptive-learning.md)
- Study engine (live): `docs/architecture/study-engine.md`
- Content formats: `docs/content/content-formats.md`
