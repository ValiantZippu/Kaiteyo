# Dialogue System

**Status**: TARGET (spec). **Source**: expansion spec §28; NODE §101, §104;
`docs/content/content-formats.md` (dialogue format); GAMEPLAY_SYSTEMS §9–§10.

## Principle

Dialogue is **authored, data-driven content** (never code), organized as dialogue
trees over dialogue nodes, with branches driven by quest state, friendship, story
state, time/weather/season, and the player's language level. Dialogue is also a
first-class **language-learning surface**: every line is glossable and every
line's language content feeds the knowledge model.

## Dialogue nodes (data)

A dialogue node:

```
id                     "npc:shopkeeper-14/welcome"
npcRef                 "npc:shopkeeper-14"
condition              { questState, friendship >= 1, season == summer, hour in 9..18 }
lines                  [ { speaker, text (ja), gloss, audioRef?, knowledgeLinks [] } ]
choices                [ { label, next }, { label, next, requiresKnowledge: "hanabi" } ]
                       # requiresKnowledge = a knowledge-node id the player
                       # must have discovered for the choice to appear
                       # (knowledge-dependent responses, spec §13). At least
                       # one ungated choice per branch — the runner skips an
                       # all-gated line instead of soft-locking.
# Kid-mode variants (spec §68): per-line simpler text for kids mode.
kidJp / kidReading / kidTranslation   (blank = fall back to main fields)
effects                [ { event: "quest:errand-01/objective/advance", payload } ]
```

- **Branches are data** (conditions over world state), so new dialogue never
  requires engine changes (ADR-0015).
- **Knowledge links** on lines expose words/kanji/grammar to the learning layer.
- **Audio**: authored voice where available; TTS fallback with an honest label
  (`game-audio.md`, JOURNEY_RUNTIME_SPEC §14).
- **Localization**: dialogue ships in Japanese + English (+ future languages) via
  the content localization system (`docs/content/content-formats.md`).

## Dialogue flow (UX)

1. NPC talk interaction → camera to dialogue camera (medium close-up, both
   parties visible; `camera.md`).
2. Line by line: speaker name, text (Japanese, furigana optional per settings),
   optional gloss on demand (don't spoil by default; learner reveals).
3. Player choices appear when authored (a few, meaningful); otherwise Continue.
4. Typing/subtitle styling follows accessibility: subtitle size/background,
   high contrast, reduced motion.
5. Text is glossable at any time: hover/select → dictionary bridge
   (`interaction-system.md` → `learning-in-world.md`).
6. Exit anytime (Back); NPC remembers where the conversation stopped (no
   punishing restart, quest dialogue is checkpointed).

## Dialogue & NPC knowledge level

- Each NPC has a knowledge level = the language level their dialogue targets.
  Low-level NPCs (child mode, beginner zones) speak simple, gloss-friendly
  Japanese; advanced NPCs speak natural Japanese.
- This is a *content* property, used by curriculum/adaptive systems and the
  child-content filter — not a difficulty wall (the learner chooses whom to talk
  to; the world doesn't block).

## Dialogue-driven state

- Dialogue can advance quests, set story beats, grow friendship, unlock optional
  scenes — all via **effects** emitted as events into the shared event bus
  (`docs/architecture/nodes/EVENT_CATALOG.md`).
- No dialogue is required for progression except authored story beats (which are
  themselves forgiving: revisitable, no fail states — GAMEPLAY_SYSTEMS §10).

## Non-punitive rules (GAMEPLAY_SYSTEMS §10)

1. No wrong-answer punishment in dialogue (there are no quizzes in dialogue).
2. Missed dialogue (NPC busy, wrong time) → the NPC is available another time;
   nothing is permanently missed except by authored story choice.
3. Skip/rewind: dialogue is replayable; story-relevant lines are in the journal.

## Acceptance criteria

1. A new NPC + dialogue tree works with zero engine changes (data-driven).
2. Branches resolve deterministically from world state.
3. Every dialogue line is glossable and feeds knowledge events when revealed.
4. Accessibility: keyboard-only playthrough, subtitle scaling, reduced motion.

## Related

- NPCs: [npc-system.md](npc-system.md)
- Format: `docs/content/content-formats.md` (dialogue schema + validation)
- Authoring: `docs/architecture/nodes/CONTENT_AUTHORING.md`
- Spec: NODE §101, §104; GAMEPLAY_SYSTEMS §9–§10
