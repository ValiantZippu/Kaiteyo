# Collectibles, Discovery & Photography

**Status**: TARGET (spec). **Source**: expansion spec §4/§27; NODE §106–§110
(discovery, photography, collections, swimming, ocean); GAMEPLAY_SYSTEMS §13
(photography), §12 (collections vs discovery), §15 (swimming/diving).

## Principle

Collecting is **recording real experience**, not looting. Photos, stamps,
discovered words, and collected kanji are the world's memory of the player's
journey — and they are the *only* "inventory" the game has.

## Discovery (spatial, §12 GAMEPLAY_SYSTEMS)

- Discovery is **spatial + observational**: visiting a place, seeing an object,
  reading a sign. It is never a compass quest ("go to the ??").
- Discovered locations feed the map reveal (`map-system.md`); discovered words
  feed the knowledge model (`learning-in-world.md`).
- Discovery markers are data (POI nodes); the map shows only what's found.
- Discovery is deterministic and restorable (save + world state).

## Photography (Shashingo-like, §13 GAMEPLAY_SYSTEMS)

Photography is a **first-class activity** — the way the player "captures" Japan
and its language:

- **Photography mode** = its own camera (compose, framing aids, focal effects —
  `camera.md`), opened from the camera action (F12 / Capture / camera button).
- **Photo subjects**: locations, NPCs, objects, seasonal moments, and
  **language moments** (a sign, a menu, a word in the wild).
- **Photo → collection**: each photo is saved with location/context metadata and
  goes to the player's photo collection.
- **Photo → learning**: photographing a sign can "collect" the words on it
  (discovered vocabulary + knowledge events) — the Shashingo-like loop:
  *see → photograph → word learned*.
- Photos are the player's own media; they flow into the media library (shared
  user data) so they appear in the app too.

## Collections (stamp book & friends)

- **Stamp book**: stamps earned at landmarks (shrine stamp 御朱印-style, station
  stamps 駅スタンプ, aquarium stamps) — each one a real visit record.
- **Word/kanji collections**: "words I found in Kamakura", "kanji I photographed"
  — driven by the shared knowledge model (discovered nodes), never a second
  bookkeeping system.
- **Seasonal collections**: festival stamps, beach items, seasonal photos.
- Collection entries are non-currency, non-power rewards
  (`progression-rewards.md`).

## Exploration activities (expansion §4, §27)

| Activity | Where | Notes |
|---|---|---|
| Swimming | Yuigahama beach (summer) | swimming state, lifeguard NPC, seasonal gate (no soft-lock) |
| Diving | Enoshima dive spots | underwater state, presentation (color/tone), collections (fish stamps) |
| Aquarium | Enoshima Aquarium | exhibits (data), feeding times, child-friendly learning |
| Sitting/watching | benches, viewpoints, train platforms | idle scenes, cinematic camera (`camera.md`) |
| Street food / shops | Komachi-dōri | purchase flavor (no currency — `progression-rewards.md`), seasonal menus |

Rules for these:

1. **Swimming/diving are seasonal and weather-gated** (beach closed in storm) —
   with honest UI ("the beach is closed today") and never a lost-progress trap
   (the gate is a state, not a failure).
2. Aquarium exhibits are data-driven (exhibit nodes: name, fish, knowledge
   links, audio) — new exhibits = content, not code.
3. Seasonal activities return every season; nothing is permanently missable
   (`quest-system.md` non-punitive rules).

## Data & events

- Every discovery/photo/collection entry is a node/event: `LOCATION_DISCOVERED`,
  `PHOTO_TAKEN`, `COLLECTION_COMPLETED`, `WORD_DISCOVERED` (see
  `docs/architecture/nodes/EVENT_CATALOG.md`).
- Collections live in the save (`collections`, `photos`); word/kanji discoveries
  live in shared user data. The split rule from `player.md` applies.

## Acceptance criteria

1. Photography works from both camera modes and is fully keyboard/controller
   accessible.
2. Photo → word discovery → knowledge update is a complete, tested loop.
3. Seasonal gates never lose progress and are honest about closure.
4. No collection duplicates shared knowledge data (single source of truth).

## Related

- Camera: [camera.md](camera.md) · Quests: [quest-system.md](quest-system.md)
- Rewards: [progression-rewards.md](progression-rewards.md)
- Environment: [environment-simulation.md](environment-simulation.md)
- Spec: NODE §106–§110; GAMEPLAY_SYSTEMS §12–§15
