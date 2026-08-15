# World Streaming

**Status**: TARGET (spec). **Source**: expansion spec §5; NODE §92 (cell streaming);
JOURNEY_RUNTIME_SPEC §17 (streaming & cache contract).

## The problem

A walkable Japan cannot be resident in memory. The player must travel
street → train → station → city → region **without the entire country ever being
loaded** — and with zero frame hitches.

## The mechanism: cells, streamed around the player (NODE §92)

- The world is divided into **map cells** (the district grid). Only cells around
  the player are loaded; neighbors stream in/out with LOD tiers; a full region is
  never resident.
- **Streaming guarantees** (JOURNEY_RUNTIME_SPEC §17):
  - adjacent-cell loads ≤ platform budget with zero frame hitches;
  - a cell is never re-streamed while resident;
  - eviction is LRU with LOD priority (high-LOD cells evict first);
  - cell content is deterministic (same package version → same cells).

## Manager architecture (expansion §5)

| Manager | Responsibility |
|---|---|
| **WORLD MANAGER** | World identity, installed packages, region activation, save/load orchestration |
| **REGION MANAGER** | Per-region metadata, bounds, city index; activates/deactivates region content |
| **CHUNK MANAGER** | Cell grid bookkeeping, load/unload queue, residency set, priority |
| **LOCATION MANAGER** | Location/interior activation (interior load on enter, unload on exit), POI resolution |
| **ASSET STREAMER** | Mesh/texture/audio asset streaming at LOD; eviction; download of package assets |
| **NPC STREAMER** | NPC spawn/despawn by tier (see `npc-system.md`), simulation handoff |
| **AUDIO STREAMER** | Audio zone activation, per-cell ambient/music/weather layers (see `game-audio.md`) |
| **WEATHER MANAGER** | Deterministic weather state (seed), weather transitions |
| **TIME MANAGER** | World clock, day/night cycle, time-of-day systems |

Relationships (one-way):

```
WORLD → REGION → CHUNK → (ASSET, NPC, AUDIO) streamers
                     ↘ LOCATION (interiors)
TIME → WEATHER → (lighting, audio, NPC schedules, transport schedules)
```

Streamers never reach into game-logic state; they serve content to it (service
contracts, `docs/architecture/nodes/SERVICE_CONTRACTS.md`).

## Loading zones & budgets

- **Loading zones**: each cell declares a loading zone (spatial bounds + trigger).
  Crossing a trigger enqueues neighbor cells ahead of the player's heading.
- **LOD tiers** per cell: full (near), medium (mid), proxy (far: collision +
  silhouette + label + ambient audio only). Proxy cells keep the world
  *perceptually complete* without geometry.
- **Memory budgets** (proposed targets, must be measured — §188; JOURNEY_RUNTIME_SPEC §10):

| Tier | Residency target | Streaming budget per frame |
|---|---|---|
| Desktop high-end | high-LOD cells around player + proxies to horizon | sized to maintain 60 FPS |
| Desktop mid | medium-LOD neighborhood | 60 FPS |
| Mobile mid | low-LOD cells + aggressive proxies | 30–60 FPS |
| Mobile low | minimum preset, dynamic resolution | 30 FPS |

All budget numbers are **proposed targets to be measured against reference devices**
(§188–§190), not shipped facts.

## Asynchronous loading & no-hitch rule

- All loading is asynchronous, batched, and budgeted per frame; priority = player
  heading > visible > surrounding.
- A cell that misses its budget is shown as a LOD-1 proxy, never a pop-in void.
- Mesh/texture streaming is size-tiered per platform; textures stream at LOD
  resolution first (mips), geometry at proxy→medium→full.
- Collision streams with geometry (never late); navmesh per cell streams with the
  cell; interiors preload their entrances during the approach.

## NPC streaming & background simulation

- NPC simulation is tiered (see `npc-system.md`): near = full detail, same area =
  medium, far = abstract. When a far NPC becomes near, their abstract state is
  reified deterministically (schedule-based; no visible pop or state reset).
- NPCs *never* teleport or despawn in view: streaming chooses transitions that
  respect the player's observation (doors, distance, LOD swaps).

## Interior streaming

- Entering a location triggers an interior cell load (door threshold = loading
  zone); exiting unloads. Interiors are separate cells — a shop interior does not
  coexist in memory with the street unless designed (transition spaces).
- During interior load, the doorway presents a short, honest transition (fade or
  doorway animation; reduced-motion respects it) — never a hard pop.

## Travel without full loads (the §5 requirement)

street → train → station → city → region:

1. Walking: cell streaming handles it.
2. Boarding a train: the train is a **moving interior** — the player's cell
   reference follows the train, so the world streams around the train. Stations
   are interior cells that load at approach.
3. Long-haul travel (region→region): the train ride is an authored experience
   (scheduled stops, scenery proxies); region switch loads the target region's
   city/cells behind the ride, then the player steps off into a streamed city.
4. Fast travel (map): load the target district's cells before the fade completes;
   the fade is the loading surface (honest: never hides a hitch, but the fade
   bounds the perceived wait).

## Cache & offline contract (JOURNEY_RUNTIME_SPEC §17, STANDARDS §267)

- World package cache on disk (re-downloadable, offline-safe); save overrides on
  disk separately.
- Eviction: LRU, LOD-priority; cache size limits per platform tier.
- Invalidating conditions: package update, region switch, save restore,
  low-memory pressure.
- All world content is offline-playable once a package is installed (§182).

## Acceptance criteria

1. Adjacent-cell loads never hitch the frame (measured per tier, §143).
2. Street → train → station → city → region is seamless and fully streamed.
3. Interior enter/exit never pops.
4. Cache eviction + re-stream is deterministic and never loses save state.
5. Low-memory pressure degrades LOD, never crashes.

## Related

- Cache/save integrity: [save-system.md](save-system.md), `docs/architecture/backup.md`
- Performance budgets: `docs/rendering/rendering-performance.md`,
  `docs/architecture/performance.md`
- Spec: NODE §92; JOURNEY_RUNTIME_SPEC §10, §17; STANDARDS §267–§268
