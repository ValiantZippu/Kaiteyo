# Journey World Schema

**Status**: TARGET — nothing implemented. Blueprint for world content and runtime data.
**Source spec**: [Node Architecture master spec](../NODE_ARCHITECTURE.md) §88–§113, §118, §145
**Runtime counterpart**: [Journey Runtime Spec](JOURNEY_RUNTIME_SPEC.md)
**Content pipeline**: [Content Authoring](CONTENT_AUTHORING.md)
**Owner doc**: [`docs/architecture/journey.md`](../journey.md) (STANDARDS §175)
**Flags**: CONTENT PRODUCTION · 3D PRODUCTION · ART PRODUCTION · AUDIO PRODUCTION ·
EXTERNAL DEPENDENCY (geodata) · SEPARATE RUNTIME (engine selection — STANDARDS §242, ADR-0014)

## 1. World hierarchy (§88)

```
world → region → prefecture → city → district → neighborhood → map_cell → location → interior → interaction_node
```

- Every level is a node (registry: WORLD family) with `worldId`-scoped identity and
  `parentId` edges (`contains_location`).
- **Content is immutable package data; player state is sparse overrides** (save, §144).
- Addressable path example: `world/japan/kanagawa/kamakura/komachi/cell-07/shop-14/onigiri-shelf`.

### 1.1 Level contracts

| Level | Contents | Streaming rule |
|---|---|---|
| world | metadata, region index, season/time config | always loaded (tiny) |
| region | summary, bounds, city index | loaded on region view |
| prefecture | summary, bounds, city index | loaded on region view |
| city | summary, bounds, district index, map overlays | loaded on city view |
| district | geometry, cell grid, POI index | loaded on district view / walking map |
| neighborhood | streets, buildings list | loaded when entered |
| map_cell | terrain, geometry, NPCs, objects, audio, lighting, nav, interactions, knowledge nodes, quest nodes, streaming metadata (§92) | streamed around player |
| location | building/shop/station/etc. instance | loaded on enter |
| interior | rooms, fixtures, interaction nodes | loaded on enter |
| interaction_node | object + interaction set (§94) + language surface | loaded with parent |

## 2. Cell system (§92)

Cell record (per world package):

```json
{
  "cellId": "kamakura/komachi/cell-07",
  "worldId": "japan",
  "x": 7, "y": 3, "size": [128, 128],
  "terrainRef": "assets/terrain/cell-07.glb",
  "geometryRefs": [...],
  "audioRefs": [...],
  "lightingRef": "...",
  "navMeshRef": "...",
  "npcs": ["npc:shopkeeper-14", ...],
  "objects": ["object:vending-03", ...],
  "knowledgeNodes": ["node:vocab/onigiri", ...],
  "questNodes": ["quest:errand-01", ...],
  "weatherState": {"default": "clear", "overrides": {...}}
}
```

- Neighbor streaming: cells load/unload around the player with LOD tiers; cache with
  owner/size/eviction (STANDARDS §267). Never load a full region at once.
- Cell content is deterministic; debugging state is reproducible (no runtime generation).

## 3. Object system (§93)

```json
{
  "objectId": "shop-14/onigiri-shelf",
  "name": "Rice ball shelf",
  "nameJa": "おにぎりコーナー",
  "description": "Fresh onigiri in the chilled section.",
  "knowledge": ["node:vocab/onigiri", "node:kanji/食"],
  "interactions": ["EXAMINE", "PHOTOGRAPH", "READ"],
  "photography": {"collectible": true, "collectionRef": "collection:kamakura-food"},
  "quests": ["quest:errand-01"],
  "dialogueRefs": [],
  "mediaRefs": [],
  "colliderRef": "...", "renderRef": "..."
}
```

- The **language surface** (nameJa, sign text, menu lines) is the knowledge connection:
  `represents` → vocabulary/kanji nodes, or inline text nodes for signs/menus.
- Interactions are typed and data-driven (§94) — never per-object code.

## 4. Interaction system (§94)

| Interaction | Definition fields | Knowledge hook | Notes |
|---|---|---|---|
| LOOK | focus + glance action | name/glossary on demand | cheapest; always available |
| EXAMINE | inspect animation + info card | full object info + glossary | |
| TALK | dialogue start (NPC only) | dialogue lines (§7) | requires speaker |
| PHOTOGRAPH | camera mode entry | photo → `depicts` → object/nodes (§95) | eligibility flag |
| READ | text panel | sign/menu/book text nodes | reading quests |
| PICK_UP / COLLECT | item into inventory/collection | item vocab | collection quests |
| BUY | transaction flow | item names + prices (numbers) | shop only; schedule-aware |
| SIT | seating state | — | rest/photo opportunity |
| EAT / DRINK | consumption animation | food vocab, counters, polite forms | menu/table only |
| SWIM | water activity | water vocab | beach/pool only |
| BOARD / ENTER / EXIT | transition | location vocab | doors/trains |
| SEARCH | scan area | hidden discoveries | exploration quests |
| LISTEN | audio focus | announcements/dialogue audio | TTS hookup |
| PLAY / WATCH | media surface | media vocabulary | media links |
| COLLECT | add to collection | collection member nodes | |

Each interaction type defines: eligibility, inputs, outputs (knowledge/dialogue/quest/
stat events), animation/sound hooks, failure behavior. New interaction types are a code
change gated by the authoring pipeline (§148) + ADR note — combinations of existing types
are content-only.

## 5. NPC system (§98)

```json
{
  "npcId": "npc:shopkeeper-14",
  "identity": {"name": "田中", "nameRomaji": "Tanaka", "occupation": "shopkeeper",
               "ageCategory": "adult", "appearanceRef": "assets/npc/tanaka.glb"},
  "homeCell": "kamakura/komachi/cell-07",
  "scheduleRef": "schedule:tanaka-week",
  "relationships": {"player": 0},
  "dialogueRefs": [...],
  "knowledge": ["node:vocab/いらっしゃいませ"],
  "quests": ["quest:errand-01"],
  "activities": ["WORK", "EAT", "HOME"]
}
```

Schedule slot (deterministic, data-driven):

```json
{
  "timeOfDay": "morning", "weekday": "*", "season": "*", "weather": "*",
  "locationRef": "shop-14", "activity": "WORK", "dialogVariant": "working"
}
```

- Tiers of NPC simulation (STANDARDS §105, §143): tier 0 background crowd (no logic),
  tier 1 schedule-driven (most NPCs), tier 2 quest/relationship NPCs (extended state).
- Deterministic enough for debugging; saves capture position + relationship state (§144).

## 6. Dialogue system (§99)

```json
{
  "dialogueId": "dlg:tanaka-greeting",
  "speaker": "npc:shopkeeper-14",
  "lines": [
    {"ja": "いらっしゃいませ！", "en": "Welcome!", "furigana": "いらっしゃいませ",
     "voiceRef": "...", "emotion": "happy",
     "knowledge": ["node:vocab/いらっしゃいませ"]}
  ],
  "choices": [
    {"textJa": "おにぎりを買います", "textEn": "I'll buy an onigiri",
     "conditions": [], "effects": [{"quest": "quest:errand-01", "objective": 0, "op": "complete"}],
     "knowledge": ["node:vocab/買う"]}
  ],
  "conditions": [], "effects": []
}
```

- Human-authored; validated (§148: ja/en/furigana required, knowledge refs must resolve).
- Language exposure is ambient — knowledge links surface only on demand (§112).

## 7. Quest system (§100–§101)

```json
{
  "questId": "quest:errand-01",
  "title": "A small errand",
  "kind": "STORY",
  "level": "BEGINNER",
  "giverNpc": "npc:shopkeeper-14",
  "objectives": [
    {"id": 0, "type": "INTERACT", "target": "object:shop-14/onigiri-shelf",
     "conditionRef": "interaction:EXAMINE", "order": 1},
    {"id": 1, "type": "COLLECT", "target": "collection:kamakura-food/onigiri", "order": 2},
    {"id": 2, "type": "TALK", "target": "npc:shopkeeper-14", "dialogRef": "dlg:errand-done", "order": 3}
  ],
  "rewards": [{"kind": "discovery", "ref": "location:komachi-backstreet"}],
  "knowledge": ["node:vocab/おにぎり", "node:grammar/〜を買います"],
  "storyConsequences": [{"story": "story:summer-day", "beat": "errand-complete"}]
}
```

- Quest types (DISCOVERY, EXPLORATION, LANGUAGE, PHOTOGRAPHY, COLLECTION, STORY, CULTURE,
  LISTENING, READING, WRITING, VOCABULARY, KANJI, GRAMMAR, MEDIA, DAILY) are authored
  variants of the same schema — type is metadata, not code.
- Objective condition types: INTERACT, COLLECT, DISCOVER, PHOTOGRAPH, TALK, READ, LISTEN,
  VISIT, WRITE, REVIEW (study objective: complete a review of node X), EXAM.
- Failure is allowed and non-punitive (no energy/lives/timers, §117).

## 8. Story system (§102)

```
story → chapter → scene → beat → dialogue/interaction/choice → outcome
```

- Typed hierarchy with `precedes`/`follows`/`requires` edges; strict ordering by default,
  optional beats author-declared.
- Each beat may carry: knowledge nodes (ambient), quest flags, NPC relationship effects,
  discoveries — the story and language progressions advance together.
- Example slice story: "A summer day in Kamakura" — train → drink → beach → character →
  shop → photo → words → home (§102). Save/load restores the exact beat.

## 9. World time, weather, seasons (§107–§109)

- Clock modes: real time / game time / accelerated (player-selected, saved).
- Time of day (morning/day/evening/night) drives schedules, lighting, audio, shops,
  transport, quests, events.
- Weather kinds: clear/cloudy/rain/storm/snow/fog — per-cell/region state with
  deterministic seeds; affects audio, lighting, NPC schedule overrides, quest conditions,
  photography. Never purely cosmetic; never soft-locks progression (guaranteed clear day).
- Seasons: spring/summer/autumn/winter — world-level state with per-season content
  variants (NPC clothing, food, events, decoration). Slice ships summer; swap must be
  data-driven.

## 10. Collections & discovery (§110–§111)

- Collection: `{collectionId, title, kind, members[]}` with `belongs_to` edges; membership
  earned through real encounters only.
- Discovery: `{discoveryId, kind, nodeRef, foundAt, source, questRef?}` — event-derived
  records feeding `encountered_by`/`discovered_by`, stats, and quest conditions.
- Distinct semantics: collection = "I have this"; discovery = "I encountered this".

## 11. Difficulty adaptation (§113)

- One geometry, N language depths: dialogue variants, quest text, glossary richness, and
  explanation availability keyed by player level (BEGINNER/ELEMENTARY/INTERMEDIATE/ADVANCED/CUSTOM).
- Content files carry `level` fields; the runtime filters presentation, never geometry.
- Adaptive smoothing: knowledge state (§84) refines which words surface per player.

## 12. World packages (§145)

```json
{
  "manifest": {
    "worldId": "japan", "version": "1.0.0", "minEngineVersion": "3.0",
    "dependencies": [{"packageId": "geo-kanagawa", "version": ">=1.0"}],
    "contentHash": "sha256:...", "license": "...", "attribution": "..."
  },
  "assets": [...], "nodes": [...], "relationships": [...], "localization": {...}
}
```

- Packages: validated (§148), hash-verified, version-gated against the engine, licensed
  and attributed (STANDARDS §259–§260), and never execute code (STANDARDS §361).
- Adding a region = a package update, not an engine change (§90).

## 13. Acceptance criteria (slice-level, §91)

1. Kamakura + Enoshima slice ships: streets, beach, railway+station, shops, temples,
   shrines, residential areas, aquarium attraction, ocean, NPCs, trains, weather,
   day/night, photography, language nodes, quests, dialogue, collections, contextual
   learning — all data-driven.
2. The §87 loop runs end-to-end inside the slice (onigiri example).
3. All schema examples above validate against the §148 pipeline with zero engine changes
   to add a new object, NPC, dialogue, quest, or story.
4. Save/load restores exact world state; learning data remains in shared user data.
