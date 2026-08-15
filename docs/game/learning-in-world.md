# Learning in the World

**Status**: TARGET (spec). **Source**: expansion spec §8 (the event flow), §26
(game+media connection), §40 (recommendations), §68 (location knowledge);
NODE §112 (learning in Journey), §149–§150 (the loop).

## Principle

Learning lives **inside the world**. A shop sign is vocabulary. The player can
inspect it, see kanji highlighted, open a dictionary panel, save it, mine it, and
the word becomes known — updating vocabulary progress, statistics, future quests,
exams, media recommendations, and curriculum progression. One event flow, one
knowledge model.

## The complete event flow (§8)

```
WORLD_TEXT_SELECTED
        │
        ▼
TEXT_ANALYZER (shared pipeline: the same analyzer as the study app)
        │
        ├──▶ TOKENIZER (wanakana/MeCab-style — existing text analysis)
        ├──▶ KANJI_DETECTOR
        ├──▶ VOCABULARY_MATCHER (dictionary)
        ├──▶ GRAMMAR_ANALYZER
        │
        ▼
DICTIONARY (shared AppDataDatabase + suite dictionary engine)
        │
        ▼
USER_INTERACTION (glossary → expand → full entry — JOURNEY_RUNTIME_SPEC §5)
        │
        ├──▶ LOOKUP      → knowledge signal (exposure)
        ├──▶ SAVE        → discovered word / collection
        ├──▶ MINE        → MiningEngine → card → deck
        ├──▶ EXAM        → offered as exam material (curriculum feed)
        ├──▶ QUEST       → quests may use this vocabulary (quest generator input)
        ├──▶ DECK        → card pool / Anki (mining destination)
        │
        ▼
PROGRESS ENGINE (knowledge model — ADR-0016: events update knowledge)
        │
        ▼
STATS (shared statistics pipeline — events in → stats out)
        │
        ▼
RECOMMENDATION ENGINE (§40) → next media, next quest, next review, next lesson
```

**Everything downstream is event-driven.** No world-local bookkeeping: the word's
knowledge update, the stat, and the recommendation all derive from the same
events the rest of Kaiteyo uses (ADR-0016, `EVENT_CATALOG.md`).

## What the player experiences

1. **Hover/select text** in the world (sign, menu, NPC line, station name) →
   compact glossary (word, reading, gloss).
2. **Expand** → full dictionary entry (existing popup behavior hosted in the
   world layer) with actions: create card, edit card, add tags/flags, copy,
   pronunciation (TTS), open full dictionary, related-node chips.
3. **Mine** → MiningEngine creates a card in the shared card pool (→ Kaiteyo
   deck and/or Anki — `docs/architecture/mining.md`).
4. **Knowledge updates** → the word moves along its knowledge state; the
   knowledge overlay shows per-dimension mastery (§112); stats update; the
   heatmap reflects the session.
5. **Future content adapts** → quests may include the word; exams may test it;
   media recommendations may consider it; the knowledge-density overlay
   (`map-system.md`) changes.

## Learning interactions inside the world (§112)

| Interaction | What it is | Opt-in? |
|---|---|---|
| Gloss on hover | reading help | yes (never spoils by default) |
| Dictionary expand | full entry + actions | yes |
| Mining | create card | yes (explicit action) |
| Practice spots | authored "learn this word" spots (rare, gentle) | yes |
| NPC dialogue exposure | talking = exposure (knowledge signal) | implicit, never quizzed |
| Photo → word | photographing a sign collects its words | yes |

**Never**: pop-up quizzes mid-exploration, forced practice, "answer to
continue." The world offers; the learner chooses (game philosophy).

## Location knowledge (§68)

Locations carry language content:

```
Kamakura
├── location vocabulary (地名: station, shrine, beach names)
├── historical vocabulary (temple history terms)
├── signs (shop signs, directions)
├── NPC dialogue (knowledge links)
├── quests (learning quests)
├── stories (story content)
├── landmarks (POI nodes)
├── media (media set in this location)
├── grammar (dialogue grammar patterns)
└── cultural notes (data — shown in the journal/glossary)
```

This is the **world knowledge graph** — every location node links to the language
nodes it exposes. The knowledge-density overlay and curriculum recommendations
query exactly this.

## Game + media connection (§26)

```
User watches Japanese anime (Media Centre)
   → words discovered (subtitle selection, mining)
   → vocabulary progress updates (shared knowledge)
   → system sees known vocabulary
   → a quest may use that vocabulary
   → the world may display that vocabulary (signs, NPCs)
   → an exam may test it
   → recommendations suggest another anime
```

One event chain, bidirectional: world → media and media → world both write to
the same knowledge model.

## Contracts

- **DictionaryService** (shared): lookup(text) → LookupResult. Must NOT modify
  progress automatically (lookup alone is an exposure *signal*, not a knowledge
  update — see ADR-0016 for evidence weighting).
- **MiningService** (shared): createCard(MiningContext) → KaiteyoCard. Must NOT
  mutate Anki unless explicitly requested (mining destination setting:
  Kaiteyo / Anki / Both — `docs/architecture/mining.md`).
- **ProgressEngine** (shared): update(events) → knowledge state. Must NOT be
  called from world UI directly — the world emits events; the engine derives
  state (single source of truth).

## Acceptance criteria

1. WORLD_TEXT_SELECTED → dictionary → card → review works end-to-end inside the
   world (the §150 loop).
2. Knowledge/stats update exactly once per event (no double-counting between
   world and app paths).
3. Lookup without explicit action does not change knowledge state (only
   exposure signals).
4. World learning events are indistinguishable in the stats pipeline from study
   events (one stream, §210–§213).

## Related

- Dictionary bridge: `docs/architecture/nodes/JOURNEY_RUNTIME_SPEC.md` §5
- Mining: `docs/architecture/mining.md` · Knowledge: `docs/architecture/nodes/KNOWLEDGE_STATE_MODEL.md`
- Events: `docs/architecture/nodes/EVENT_CATALOG.md` · Stats: `docs/architecture/statistics.md`
- Recommendations: `docs/learning/adaptive-learning.md` (§40 spec)
- Spec: NODE §112, §149–§150; STANDARDS §210–§213
