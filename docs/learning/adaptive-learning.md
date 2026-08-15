# Adaptive Learning & Recommendation Engine

**Status**: TARGET (spec). **Source**: expansion spec §40 (recommendation engine),
§69 (multi-dimensional difficulty); NODE §113 (difficulty adaptation), §85
(knowledge scoring); ADR-0016.

## Principle

Adaptive learning = **the system tunes presentation and pacing from real
knowledge evidence** — never fake difficulty, never "AI magic," never walls.
Recommendations are honest, explainable, and derived from the same event stream
as everything else.

## Difficulty is multi-dimensional (§69)

| Dimension | Source | Example |
|---|---|---|
| Vocabulary difficulty | frequency bands, JLPT tags, knowledge model | N5 words vs N2 words |
| Grammar difficulty | grammar dataset (future — open dataset RESEARCH), exam data | N3 grammar in an easy-vocab story |
| Cultural/contextual difficulty | content metadata (authorable) | a shrine sign is N5 vocab but culturally advanced |
| Reading speed/comprehension | reading events, exam reading sections | story pace |
| Listening difficulty | media metadata, listening evidence | subtitle density, speech speed |

A content item has a **difficulty vector**, not a single number. The engine never
collapses it to "this story is N3."

## The recommendation engine (§40)

Inputs: known words, unknown words, kanji, grammar, media difficulty, frequency,
JLPT, user goals, recent mistakes, retention, interests, anime list, quest
progress, curriculum.

Outputs (all explainable, all opt-in):

| Recommendation | Example |
|---|---|
| Media fit | "Here are 5 episodes likely to be understandable (78% known vocabulary)." |
| Review set | "Review these 12 words — you're weak on them." |
| Story fit | "This story introduces mostly known grammar." |
| Quest fit | "This quest will reinforce words you are weak on." |
| Curriculum next | "Next lesson: these 10 kanji (prerequisite: met)." |
| World areas | "Komachi-dōri has words you know — great for reading practice." (knowledge-density overlay) |

Rules:

1. Every recommendation shows **why** (the evidence: "you missed this in your
   last exam", "known vocabulary 78%").
2. Recommendations never block: they suggest; the user decides.
3. No "recommended for you" that hides content — discovery stays open.

## Evidence weighting (ADR-0016, progress model)

- Evidence types are weighted by reliability: production/writing/exam > review
  ratings > reading/media exposure > lookups.
- Recommendations consume the *derived knowledge state*, never raw event spam.
- Confidence is tracked: low-confidence estimates are labeled ("estimated" —
  standards §290).

## Difficulty adaptation in Journey (§113)

- Presentation adaptation: gloss density, furigana, audio support, subtitle
  language — tuned by the learner's per-dimension knowledge (never their age
  alone).
- Pacing: objective complexity and quest content reference knowledge state
  ("words you're weak on" quests) without grading the player.
- **Never**: difficulty walls, "too hard → blocked", adaptive punishment. The
  world is a playground; adaptation softens or deepens the *experience*.

## Contracts

- `RecommendationService.recommend(context) → Recommendation[]` — read-only over
  knowledge model + content metadata; never mutates.
- `DifficultyService.vectorOf(content) → DifficultyVector` — from metadata +
  knowledge model.
- All recommendations carry `evidence[]` for display.

## Acceptance criteria

1. Every recommendation has an explainable evidence chain.
2. Difficulty vectors drive presentation only — no content gating beyond
  authored prerequisites.
3. Media-fit recommendations are measured for accuracy on a test corpus
  (TEST_PLAN) before shipping.
4. Adaptation respects accessibility (reduced motion, contrast) and never
  overrides user settings.

## Related

- Progress: [progress-model.md](progress-model.md) · Curriculum: [curriculum-engine.md](curriculum-engine.md)
- Knowledge states: `docs/architecture/nodes/KNOWLEDGE_STATE_MODEL.md`
- Learning in world: `docs/game/learning-in-world.md`
