# Game Philosophy

**Status**: TARGET — this is the doctrine for the Journey world (ADR-0014). The world
is fully specified (`docs/architecture/NODE_ARCHITECTURE.md` §76–§162 +
`docs/architecture/nodes/`); **no implementation exists yet** (NODE §158).

## What Journey is

Journey is a **game system**, not "gamification." It is a second front-end over the
same data model as the study app: the knowledge graph, user knowledge, statistics,
decks, settings, account (ADR-0014, §114). The player explores a stylized,
recognizable Japan and learns Japanese by *living inside it* — reading signs,
talking to NPCs, completing quests, photographing locations, discovering words.

Desired direction (§3 of the expansion spec):

```
Japanese exploration game
+ Nintendo-like accessibility
+ Shashingo-like visual learning
+ light RPG progression
+ Japanese educational platform
+ beautiful explorable environment
```

## The design test (§153)

Every design decision in Journey must demonstrably improve at least one of:

- **exploration** (movement, discovery, map)
- **language** (exposure, learning, practice inside the world)
- **culture** (Japan's real places, customs, history, food)
- **story** (characters, quests, world events)
- **discovery** (photography, collections, knowledge reveal)
- **immersion** (audio, weather, time, believability)

If a mechanic only adds "game-y" texture with no purpose ("games have quest
markers", "RPGs have loot"), it **fails the test** and is removed.

## Non-negotiable rules (§86, §117, §366)

1. **No XP, no loot, no energy, no lives, no loot boxes, no artificial timers.**
   Progression is discovery + knowledge + story + collections — never a numbers
   treadmill. Rewards are meaningful (a story beat, a photo, a collection, a word
   mastered), never "+5 gold".
2. **No grinding.** Nothing is repeatable-in-place for points. The world exists
   independent of the player; knowledge comes from *noticing*, not pop-up quizzes.
3. **No fake educational traps.** No "answer this quiz or you can't leave the
   shrine." Learning interactions are opt-in and woven into context (a shop sign is
   readable; it is never a mandatory quiz).
4. **The world is beautiful and intentional.** "The map is a stylized, beautiful
   surface — never a developer map" (§89). No placeholder geometry, no empty flat
   rectangles, no fake detail on undiscovered areas — "undiscovered" must look
   intentional (§118).
5. **One polished location before the world.** The first slice is Kamakura +
   Enoshima — it must prove movement, camera, interaction, dictionary, language
   nodes, NPC, dialogue, quest, discovery, photography, collection, knowledge,
   stats, save/load, performance — before any expansion (§366, §91).
6. **Accessibility first, Nintendo-like.** Reduced motion, text scaling, high
   contrast, remappable input, keyboard-only play, subtitle sizes. Every game
   feature works with accessibility enabled (§254).
7. **Learning data never lives in the save** (§144). Knowledge/reviews/cards/stats
   stay in the shared user data — Journey and study are one trajectory.

## The experience target

- Player feeling: *"I am living inside a Japanese learning world"* — never *"I am
  grinding XP in an educational RPG"* (§86).
- Immersion: signs in Japanese with optional gloss, NPCs with schedules and lives,
  trains that run on time, weather that changes the world, a day/night cycle.
- Friction-free learning: hover/select text → dictionary popup → mine a card —
  inside the world, without leaving it (§140, the §150 loop's hub).

## Anti-patterns (never in Journey)

- Quest markers as the only content (a map marker is the *quest UI* — §101 — but
  discovery must be possible without it)
- Static mannequin NPCs standing in circles with one repeating line (simulation
  tiers, §105)
- Teleport-everywhere fast travel that erases the map's value (travel is part of
  the experience — trains, stations, §29)
- Pop-up quizzes interrupting exploration
- RPG filler: fetch-10-rat quests, inventory clutter, currency sinks
- Content that only exists to lengthen playtime

## Game-feel pillars

| Pillar | Promise |
|---|---|
| Movement | Responsive, floaty-but-grounded walk/run; smooth camera; continuous zoom map (World→street) |
| Interaction | One consistent interaction system (E/tap/controller-A): inspect → learn → mine → move on |
| Discovery | Progressive map reveal; knowledge-density overlay; intentional undiscovered states |
| People | NPCs with schedules, weather behavior, seasonal behavior, knowledge level (simulation tiers) |
| Transport | Trains/stations as *experiences* (platforms, timetables, announcements), data-driven, scalable |
| Environment | Time, weather, seasons that are deterministic, never soft-locking (§117) |
| Learning | Text in the world is learnable: WORLD_TEXT_SELECTED → analyzer → dictionary → knowledge (§8 flow) |

## What "game-like without RPG" means in practice

- Achievements exist (the app has them) but are never the core loop.
- Collections (photos, discovered words, stamps) are records of *real experience*,
  not loot.
- Progression unlocks *understanding and access* (the map reveals, NPCs trust you,
  stories continue) — not stat points.
- Difficulty adaptation (NODE §113) tunes *presentation* (gloss density, audio
  support, furigana) and *pacing*, never punishes failure. No fail states that
  cost progress; quests never brick (non-punitive rules, GAMEPLAY_SYSTEMS §10).

## Relationship to other docs

- Game philosophy → architecture: `docs/architecture/journey.md`, ADR-0014,
  `docs/architecture/NODE_ARCHITECTURE.md` (§76–§162)
- Game specs: `docs/game/` (world, map, camera, player, NPCs, quests, transport,
  environment, save, audio), `docs/rendering/`, `docs/input/`
- Design test & pillars: `docs/architecture/nodes/GAMEPLAY_SYSTEMS.md` (§1–§2)
- Child mode (target): `docs/vision/child-experience.md`, GAMEPLAY_SYSTEMS §22
