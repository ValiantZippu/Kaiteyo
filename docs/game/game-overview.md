# Game Overview — Journey

**Status**: TARGET (spec). **Source**: NODE §86–§87, §114–§117; ADR-0014; expansion
spec §3.

## What kind of game is this?

Journey is a **stylized open-world Japanese exploration game** with light RPG
progression, embedded language learning, and Nintendo-like accessibility. It is
deliberately:

- **NOT** a combat RPG (no enemies, no weapons, no damage)
- **NOT** an edutainment quiz game (no pop-up quizzes interrupting the world)
- **NOT** a walking simulator with nothing to do (quests, NPCs, discovery,
  photography, collections, trains, seasons)
- **NOT** a grind (no XP, loot, energy, lives, loot boxes, artificial timers — §117)

Genre line:

```
Japanese exploration game
+ Nintendo-like accessibility
+ Shashingo-like visual learning (photograph → word)
+ light RPG progression (discovery, story, collections — not stats)
+ Japanese educational platform (same graph as the study app)
+ beautiful explorable environment
```

## The core loop (§87)

```
EXPLORE the world (move, map, discover)
   ↓
NOTICE Japanese in context (signs, NPC dialogue, menus, object labels)
   ↓
LEARN (glossary → dictionary popup → knowledge overlay)
   ↓
ACT (mine a card, answer a question, complete a quest, photograph, collect)
   ↓
PROGRESS (map reveals, collections fill, stories advance, knowledge grows)
   ↓
RETURN (review in the study app; the world and the app share one knowledge model)
```

**Loop invariants** (from GAMEPLAY_SYSTEMS §1):

1. Every interaction has a *language outcome* or a *discovery outcome* — never
   busywork.
2. Learning is opt-in and contextual: the world offers, never interrogates.
3. All knowledge outcomes flow to the shared knowledge model — the world never
   keeps its own score.
4. The player is never blocked from content by a quiz or by grind.
5. No mechanic survives unless it passes the design test (§153): exploration,
   language, culture, story, discovery, or immersion.

## Design pillars

| Pillar | Promise |
|---|---|
| Exploration | A map that is a progress surface; continuous zoom World→street; progressive reveal; the world exists independent of the player |
| Language in context | Every text in the world is learnable text; dictionary + mining inside the world |
| Culture | Recognizable Japan — real places, trains, food, customs — stylized, never a developer map |
| People | NPCs with schedules, weather/season behavior, knowledge level, meaningful dialogue |
| Story | Quest chains, daily-life activities, seasonal and world events; story beats persist |
| Discovery | Photography, collections, stamps; "undiscovered" is intentional and beautiful |
| Immersion | Deterministic time/weather/seasons; audio-reactive world; trains that run on time |

## What the player actually does (slice scope, §91)

The first world is **Kamakura + Enoshima** (Kanagawa) — a vertical slice proving the
full loop before any expansion (§366). Concrete activities (from
`JOURNEY_SLICE_CONTENT.md` and GAMEPLAY_SYSTEMS):

- Walk Komachi-dōri (shopping street), read shop signs, inspect objects
- Ride the Enoden (Enoshima Electric Railway): stations, platforms, timetables,
  boarding, travel, arrival
- Visit Hase-dera temple, Tsurugaoka Hachimangū, Yuigahama beach, Enoshima island
- Talk to NPCs (shopkeeper, station staff, beach lifeguard, temple priest…)
- Complete errand/exploration/photography quests
- Photograph locations and collect stamps/discoveries
- Swim at the beach, visit the aquarium (Enoshima Aquarium)

## Scope & scale rules

1. **One location first.** The slice must prove movement, camera, interaction,
   dictionary, language node, NPC, dialogue, quest, discovery, photography,
   collection, knowledge, stats, save/load, performance (§366). Then expand.
2. **Packaged regions.** Each expansion is a content package (installable,
   versioned, validated — ADR-0015, §72). Japan → region → prefecture → city →
   district → cell (§88).
3. **Fidelity levels L0–L4** (see `world-architecture.md`): the world can grow
   incrementally from abstract map to detailed playable location; the game never
   requires centimeter-perfect Japan.
4. **Same world data, different presentation tiers** (JOURNEY_RUNTIME_SPEC §10):
   desktop high-end / desktop mid / mobile mid / mobile low — one world, scaled
   budgets.

## Production pillars (target gates)

| Gate | Requirement |
|---|---|
| Engine evaluation (§242) | Documented ADR comparing Godot/Unity/Unreal before any world code |
| Node foundations | Node model + knowledge graph + content pipeline (ADR-0013/0015) before content |
| Vertical slice | Kamakura+Enoshima playable end-to-end with acceptance criteria (TEST_PLAN §13) |
| Children mode | After the slice, as a configuration + content filter (§115) |

## Engine evaluation (ADR-0018) — the #1 gate

**Decision pending.** No Journey code starts before this ADR is Accepted (STANDARDS §242,
`docs/planning/MASTER_TODO.md` KT-GAME-001). Candidate set to be scored with evidence
(full matrix + embedding design in ADR-0018):

| Candidate | Anticipated strengths (verify) | Anticipated risks (verify) |
|---|---|---|
| Godot 4 | MIT, scriptable, 2D+3D, no royalties, AI-friendly | team experience; stylized-3D pipeline maturity |
| Unity | mature 3D, asset ecosystem | licensing per-seat; closed source; runtime size |
| Unreal 5 | AAA rendering, world tooling | heavy; C++/Blueprints; revenue terms; overkill for cozy scope |
| Custom engine | full control, KMP integration | highest cost; violates "use established tech" (STANDARDS §164) |
| Existing Compose/Skia | no new runtime | not a 3D engine — rejected as the default |

Axes: Android · desktop · controller · touch · 3D · world streaming · animation · asset
pipeline · licensing · low-end mobile perf · tooling · **AI-agent friendliness** ·
maintainability · **embedding into Kaiteyo** (separate runtime vs shared module).
Deliverables before acceptance: evidence table, spike (one test scene on desktop + Android
at the slice budget), embedding decision, ADR-0018 → Accepted.

## Relationship to the study app

Journey is a destination in the Launchpad, sharing identity, theme, settings,
knowledge, statistics, library, updates (ADR-0014). **Learning data never lives in
the game save** (§144) — the world writes to the same user data the study app
reads. One trajectory.

## Related

- Philosophy: `docs/vision/game-philosophy.md`
- World: [world-architecture.md](world-architecture.md)
- Spec: `docs/architecture/nodes/GAMEPLAY_SYSTEMS.md` (§1–§4), `docs/architecture/NODE_ARCHITECTURE.md`
