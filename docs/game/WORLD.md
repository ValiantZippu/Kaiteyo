# Kaiteyo Game — World & Content Pipeline

How the world is structured and how to add content. **No engine code is needed
to add a region, a quest, an NPC, a dialogue or a vocabulary word** — everything
is data. This file is the authoring guide; [ARCHITECTURE.md](ARCHITECTURE.md)
describes the code that consumes it.

## World hierarchy

```
World
└── Region (e.g. Hamanaka — the seaside starter town)
    ├── Map (bounds, spawn point, districts)
    ├── District (e.g. seaside, station, shopping street)
    │   └── Cell (one playable tile map)
    │       ├── Objects (signs, vending machines, benches, buildings…)
    │       ├── Locations (beach, station, aquarium — discoverable anchors)
    │       └── Stations (travel-network nodes)
    ├── NPCs
    ├── Quests
    ├── Stories
    ├── Collectibles
    ├── Knowledge (words/kanji/phrases + edges)
    └── TravelNetwork (station → destination edges)
```

Every level is keyed by id; the runtime indexes everything and validates every
cross-reference at load (see Validation below). The model is explicitly built
to grow: a new region is a new entry in `world.json` plus its own cells,
objects, NPCs and quests — the engine, renderer, quest system, learning system
and save format do not change.

## Content files — `desktopApp/src/jvmMain/resources/game/`

| File | Contents |
|---|---|
| `world.json` | Regions, districts, cells, tile maps, objects, locations, stations, travel network, spawn |
| `npcs.json` | NPC definitions (name, dialogue, schedule, knowledge targets) |
| `quests.json` | Quest definitions (objectives, prerequisites, rewards, learning targets) |
| `dialogue.json` | Dialogue scripts (lines with JP/reading/EN, effects) |
| `knowledge.json` | Knowledge graph (nodes + typed edges) |
| `stories.json` | Story chapters/scenes |
| `collectibles.json` | Stamp/collection definitions |

All files are JSON parsed with kotlinx.serialization into the models in
`desktop/game/world/WorldModels.kt` (etc.). Content is versioned with the
save; see [save-system.md](save-system.md).

## How to add a new location (no code)

1. In `world.json`, under the region's district, add a `Cell` (or reuse one)
   and place a `Location`:

   ```json
   {
     "id": "loc-machiya-street",
     "name": "Machiya Street",
     "nameJp": "町家通り",
     "anchor": { "x": 320, "y": 480 },
     "radius": 90,
     "learningTargets": ["word-machiya"]
   }
   ```

2. If the location should be reachable by train, add a `Station` with the
   location id and an edge in the region's `travelNetwork`.
3. Add `word-machiya` (and any related words/kanji) to `knowledge.json`.
4. Optionally give a quest a `reach-location: loc-machiya-street` objective.
5. Run the app — the `ContentValidator` will tell you about any broken
   reference before anything is shown.

The discovery is automatic: walking within `radius` of `anchor` discovers the
location, unlocks the collection stamp, fires quest events and teaches its
`learningTargets` (spec §8–§10: the world itself teaches).

## How to add a quest

In `quests.json`:

```json
{
  "id": "quest-find-station",
  "title": "Find the station",
  "titleJp": "駅を探そう",
  "description": "Walk to the station and read the sign.",
  "level": 1,
  "prerequisites": ["quest-first-walk"],
  "objectives": [
    { "id": "obj-1", "kind": "reach-location", "target": "loc-station", "label": "Reach the station" },
    { "id": "obj-2", "kind": "read-object", "target": "obj-station-sign", "label": "Read 駅" }
  ],
  "rewards": { "xp": 50, "stamps": ["stamp-station"], "unlocks": ["station:kamakura"], "knowledge": ["word-eki"] },
  "learningTargets": ["word-eki", "word-eki-ni-iku"],
  "locationId": "loc-station",
  "dialogueId": "dl-station-greeting"
}
```

Objective kinds understood by the engine: `reach-location`, `discover-location`,
`read-object`, `talk-to-npc`, `learn-word`, `buy-item`, `take-photo`,
`ride-train`. Prerequisites form the quest dependency graph — a quest only
becomes available when all prerequisites are complete (spec §23–§24).

## How to add a word (and connect it to Kaiteyo)

In `knowledge.json`:

```json
{
  "id": "word-machiya",
  "headword": "町家",
  "reading": "まちや",
  "meaning": "traditional townhouse",
  "type": "word",
  "kanji": ["kanji-cho", "kanji-ie"],
  "related": ["word-ie", "word-machi"]
}
```

Edges are expressed as fields on the node (`kanji`, `related`, `phrases`) and
built into typed `KnowledgeEdge`s at load. Any word can be discovered in the
world (object labels, dialogue lines, locations, photo tags) and then **mined
into Kaiteyo's real card pool** through the `GameBridge` — the game never keeps
a second vocabulary database (spec §28, §63–§65).

## World objects

```json
{
  "id": "obj-station-sign",
  "label": "駅",                       // Japanese shown in the world
  "position": { "x": 300, "y": 200 },
  "size": { "x": 40, "y": 24 },
  "solid": false,
  "spriteKey": "sign",
  "interactableId": "read",           // read | buy-drink | sit | photo-spot | station:<id> | inspect
  "learningTargets": ["word-eki"],
  "kidTargets": ["word-eki"]          // optional: simpler layer for kids mode
}
```

Interactions are declared, not coded: the session maps `interactableId` to an
`InteractionBehavior` and the HUD shows the prompt (e.g. `[E] 読む` for a read,
`[E] 買う` for the vending machine) — spec §10, §19.

`kidTargets` (optional, spec §7, §68): when kids mode is on and the field is
non-empty, it **replaces** `learningTargets` — the same timetable teaches 電車
to a kid and 時刻表 to everyone else. Both lists are validated against
`knowledge.json` at load; a typo is a hard content error.

## Travel network

```json
"travelNetwork": [
  { "from": "station-hamanaka", "to": "station-kamakura", "label": "Enoden line", "locked": true }
]
```

Travel unlocks through quest rewards (`unlocks: ["station:kamakura"]` sets the
`travel:kamakura` flag). The Travel panel lists unlocked stations; the slice
honestly ends the journey at the platform — Kamakura is the next region
(spec §47–§48, see [ROADMAP.md](ROADMAP.md)).

## Validation (spec §123–§126)

`ContentValidator` runs at session start and fails hard on:

- duplicate ids across any content type
- quest objectives/prerequisites/rewards pointing at missing ids
- dialogue ids referenced by NPCs/quests that don't exist
- knowledge targets that don't resolve in the knowledge graph
- collectible unlock ids that don't resolve
- spawn cell / region missing

This is the gate that keeps content additions from breaking the game
silently. `DebugTools` exposes a content-reload action for iterating on JSON
without restarting.

## Fidelity & future regions

The long-term model (see [world-architecture.md](world-architecture.md))
supports L0–L4 fidelity and installable region packages. The current code
implements one L4-style cell (Hamanaka) and the data structures for the rest.
Adding Kamakura = a new region entry in `world.json` + its cells/objects/NPCs/
quests + knowledge — no engine change. The roadmap is in
[ROADMAP.md](ROADMAP.md); open questions and TODOs are tracked in
[TODO.md](TODO.md).
