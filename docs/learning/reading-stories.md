# Reading & Stories

**Status**: MIXED — reading practice is LIVE (study engine reading mode); graded
reading mode and story content pipeline are TARGET. **Source**: expansion spec
§11, §26; NODE §82–§83; `docs/architecture/language-model.md`.

## Principle

Reading is a first-class skill with its own practice surface and its own content
type (**stories**), and it feeds the same knowledge model as everything else:
words read are words exposed; glossed words become SEEN; comprehension is
measured by honest means (not fake "reading speed").

## Reading modes

| Mode | Status | Notes |
|---|---|---|
| Reading practice (existing) | ✅ live | study engine reading mode (card-based comprehension) |
| Graded reading mode | 🔬 TARGET | content tagged with difficulty vectors (`adaptive-learning.md`); reader UI with gloss, furigana options, TTS |
| Story content | 🔬 TARGET | authored stories as content packages (`docs/content/content-formats.md`) |
| Reading in world | 🔬 TARGET | signs/dialogue as reading exposure (`learning-in-world.md`) |
| Reading comprehension (exams) | ✅ live | exam reading sections (JLPT simulation) |

## Stories as content

- Stories are **data** (story format: chapters, paragraphs, sentences with
  knowledge links, gloss metadata, difficulty vector, localization).
- Story → sentence → word → kanji: stories link down to the language graph and
  up to media/quests (a story can be set in the world; a quest can reference it).
- **Level-aware**: stories carry a difficulty vector; the library filters by it
  (graded reading).
- **Honest comprehension**: comprehension checks are optional, authored, and
  non-punitive (no "wrong answer locked you out"); they feed exam/curriculum
  evidence when answered.

## Reading events

- Sentence opened / glossed / completed → reading exposure events
  (SEEN/RECOGNIZED signals, low-medium weight).
- Gloss usage is a signal of *need* (adaptive: lower the gloss density as
  knowledge grows — presentation adaptation, never a wall).
- Stories completed → curriculum evidence (comprehension when answered).

## The reading loop

```
choose story (difficulty fit, recommendation) → read (gloss on demand, furigana per settings,
TTS where wanted) → gloss/select words → knowledge updates → comprehension (optional) →
curriculum/adaptive feedback → next story
```

## Acceptance criteria

1. Graded reading filters stories by difficulty vector; never by fake scores.
2. Gloss usage personalizes presentation without blocking content.
3. Story content flows through the same knowledge/event pipeline as all reading.
4. Comprehension items are authored, optional, non-punitive.

## Related

- Formats: `docs/content/content-formats.md` (story schema)
- Adaptive: [adaptive-learning.md](adaptive-learning.md)
- Language model: `docs/architecture/language-model.md` · Exams: `docs/architecture/exams.md`
