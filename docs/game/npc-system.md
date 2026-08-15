# NPC System

**Status**: TARGET (spec). **Source**: expansion spec §28; NODE §105 (simulation
tiers); GAMEPLAY_SYSTEMS §9.

## Principle

NPCs are not static mannequins. Each NPC has an identity, a schedule, a location,
a knowledge level, quest relationships, and time/weather/season behavior — and is
simulated at a detail tier proportional to distance from the player (§105).

## NPC model

| Attribute | Meaning |
|---|---|
| **identity** | name (Japanese + gloss), role, portrait/avatar, appearance |
| **schedule** | daily/weekly routine: where they are at what time, what they do |
| **location** | home base, work, walking routes, seasonal moves |
| **dialogue** | authored dialogue trees (see `dialogue-system.md`) |
| **knowledge level** | what the NPC's Japanese *content* targets (their dialogue's language level — a data property for curriculum fit, distinct from friendship) |
| **quest relationships** | which quests they give/advance/resolve |
| **learning objectives** | which words/kanji/grammar their dialogue exposes (knowledge-node links) |
| **friendship** | a small, meaningful relationship value (0–N) — gates nothing essential, flavors dialogue and a few optional moments |
| **story state** | NPC-aware story beats (their dialogue changes as stories progress) |
| **time-of-day behavior** | shops open/close, home at night, etc. |
| **weather behavior** | take shelter in rain, beach closed in storm, etc. |
| **seasonal behavior** | festival season, summer beach lifeguards, winter closures |

## Simulation tiers (§105) — proportional detail

| Tier | Distance | Simulated | Reified when near |
|---|---|---|---|
| **near** | in the player's cell | Full: schedule tick, dialogue, interactions, full audio | — |
| **same area** | adjacent cells | Medium: position + state via schedule; ambient behavior; no dialogue | Schedule-based position; no pop |
| **far** | beyond | **Abstract**: NPC exists as a schedule entry / population density; no individual simulation | Deterministic reification from schedule + seed; never teleports in view |

Rules:

1. **Determinism**: an NPC's state at any simulated time is a pure function of
   schedule + world seed + story/quest state. Near/far reification never
   contradicts (no "NPC was at home, now magically at the shrine").
2. **No despawn in view**: tier transitions happen out of sight (through doors,
   distance LOD swaps, or at schedule waypoints).
3. **Never break immersion with the simulation**: an NPC who "should" be doing
   something is doing it (or an authored approximation), never frozen mid-pose.

## Friendship & relationships

- Small scale: a handful of meaningful relationships (shopkeeper, station staff,
  lifeguard, priest), each 0–N with a few flavor thresholds.
- Friendship **never gates required content** (no "befriend 10 NPCs to continue
  the story"). It gates optional moments: special dialogue, a photo op, a small
  story branch.
- Persisted in the save (`npcRelationships`).

## NPC language content

- Each NPC's dialogue carries knowledge-node links (words/kanji/grammar they
  expose). This feeds curriculum/adaptive systems (`docs/learning/`) and the
  knowledge-density overlay (`map-system.md`).
- NPC speech is glossable through the dictionary bridge (`interaction-system.md`
  → `learning-in-world.md`) — talking to an NPC is *exposure*, never a quiz.

## NPC kinds (slice scope)

| Kind | Example (Kamakura/Enoshima) | Behavior highlights |
|---|---|---|
| Shopkeeper | Komachi-dōri vendors | opening hours, purchase, small talk, seasonal goods |
| Station staff | Enoden stations | timetables, tickets, announcements |
| Lifeguard | Yuigahama beach | seasonal only (summer), swimming safety |
| Temple/shrine staff | Hase-dera, Hachimangū | festival quests, history dialogue |
| Aquarium staff | Enoshima Aquarium | exhibits, feeding times, child-friendly |
| Residents / passersby | walking routes | schedule loops, weather behavior, minimal dialogue |
| Children | (child mode) | age-appropriate dialogue, play interactions |

## Acceptance criteria

1. All NPCs follow deterministic schedules; near/far consistency holds in tests.
2. No NPC teleports or despawns in view; tier transitions are invisible.
3. Friendship never blocks required content (tested via quest gates).
4. NPC dialogue routes through the language pipeline (glossable, learnable).

## Related

- Dialogue: [dialogue-system.md](dialogue-system.md)
- Quests: [quest-system.md](quest-system.md)
- Simulation tiers: NODE §105; GAMEPLAY_SYSTEMS §9
- Streaming: [world-streaming.md](world-streaming.md) (NPC streamer)
