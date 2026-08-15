# Map System

**Status**: TARGET (spec). **Source**: expansion spec §4/§5; NODE §89 (map modes),
§118 (progressive reveal); GAMEPLAY_SYSTEMS §3.

## The map is a progress surface, not a wiki

The map is the world's face: **"a stylized, beautiful surface — never a developer
map"** (§89). What the player can see on the map *is* their discovery record.
Unvisited areas show stylized geography with no fake detail; "undiscovered" looks
intentional (§118).

## Map hierarchy — one continuous surface, five scales (§89)

```
WORLD → REGION → CITY → DISTRICT → WALKING MAP
```

This is **one smooth camera path, not page switches**: zooming from "Japan" to the
street you're standing on is a continuous animated transition (spring-based §123;
fade under reduced motion). The levels correspond exactly to the world hierarchy
(`world-architecture.md`):

| Level | Shows | Loads when |
|---|---|---|
| world | region index, metadata | always (tiny) |
| region | summary, bounds, city index | region view |
| prefecture | summary, bounds, city index | region view |
| city | summary, bounds, district index, map overlays | city view |
| district | geometry, cell grid, POI index | district / walking map |
| map cell | terrain, NPCs, objects, knowledge nodes, quest nodes | streamed around the player |

## What's on the map — markers and overlays (§89, §101)

Per-mode contextual layers, never all at once:

- **Discovery markers** — places/objects encountered or findable
- **Quest markers** — where the current objective is (the quest UI *is* a map
  marker, not a quest log; §101)
- **Visited vs unvisited regions** — differentiated tint
- **Landmarks, railways, roads, water** — always visible at the right scales
- **The knowledge-density overlay** (toggle): a soft heat showing areas whose
  content vocabulary/kanji the learner knows vs hasn't met (§118, GAMEPLAY_SYSTEMS §3)
- **Transport overlay** — routes, stations, timetables (in city/district modes)

Rules:

1. Markers are data-driven (quest nodes, discovery nodes, POI nodes) — never
   hardcoded UI pins.
2. A marker never shows content that is not yet discovered (no "?" pins for
   undiscovered locations — discovery is spatial, not quest-compass).
3. The objective card (§101) is the only persistent quest surface; everything
   else fades when idle.

## Progressive reveal (§118)

- Japan → Kanto → Kanagawa → Kamakura is visible early (L0–L1).
- District/street/location detail unlocks with real discovery (visiting, quests,
  photography).
- Reveal state is part of the save (`revealedMap`), deterministic and restorable.
- **Rules**: no fake detail before reveal (an unrevealed district shows stylized
  geography, not "???" pins or blurry textures); revealed geometry is never
  *removed* by updates; map reveal is the primary progression surface (progression
  without XP — `progression-rewards.md`).

## Knowledge-density overlay

- A per-cell aggregation of knowledge state over the content's language nodes
  (words/kanji tagged to locations, objects, signs).
- Soft heat, not a grading heatmap: "words you know here" vs "words you haven't
  met." Never a difficulty rating of the player.
- Resolves within the search latency budget at full dataset scale (§188) — a real
  graph query over the knowledge model, not a precomputed static layer (kept fresh
  as knowledge changes).
- Toggleable; default off; honored under accessibility (color-blind-safe palette).

## Travel & map interaction

- Click/tap a destination → travel confirmation → transition to the transport
  flow (train/bus/walk — `transportation.md`).
- Zoom persists per user; map camera returns to the player on "back to me".
- The map is keyboard/controller reachable (M / Select; `docs/input/`).

## Acceptances

1. World→street zoom is one continuous animation at 60 FPS (desktop tier),
   hitched-free on the streaming budget (`world-streaming.md`).
2. Revealed state is deterministic: same save → same revealed map.
3. Unvisited areas are "intentionally undiscovered," never broken-looking.
4. Knowledge overlay resolves within budget at full dataset scale.

## Related

- World data: [world-architecture.md](world-architecture.md)
- Streaming: [world-streaming.md](world-streaming.md)
- Spec: NODE §89, §101, §118; GAMEPLAY_SYSTEMS §3
- UX: `docs/architecture/nodes/UX_FLOWS.md` (map flows)
