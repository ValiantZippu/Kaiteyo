# Kaiteyo World System

> Status: Foundation complete — vertical slice (Kamakura) in progress.

## Overview

The World is a long-term subsystem: a scalable, streamable, explorable 3D Japan.
It is **not** a static demo and **not** one giant map file. The architecture is
built to survive growth from one city to the entire country.

```
Japan
  └─ Region (Kanto)
       └─ City (Kamakura)
            └─ District (Yuigahama)
                 └─ Location (Yuigahama Beach)
                      └─ Building (Beach house)
                           └─ Interior
                                └─ Activity
                                     └─ NPC → Interaction → Learning
```

## Runtime Isolation

The World runs in its own runtime (`WorldRuntime`), isolated from the app shell.
A World crash can never take down the app.

- `WorldRuntime` — owns the game loop, chunk manager, and all systems.
- `WorldController` — the app-facing handle (start/stop/pause/input/save).
- `WorldState` — observable snapshot (StateFlows for position, time, weather, errors).
- `WorldEvent` — events emitted to listeners (chunk loaded, player moved, error).
- Every system failure is caught and recorded (`lastError`) instead of propagating.

Lifecycle is strict: `start()` → `running` → `stop()`. `stop()` releases chunks
and cancels the loop. `dispose()` cancels the controller scope.

## Geographic Coordinate System

Two coordinate spaces:

| Space | Type | Units | Purpose |
|---|---|---|---|
| World space | `WorldPosition` | meters (x east, z south, y up) | rendering, movement, chunks |
| Geo space | `GeoCoordinate` | WGS84 degrees | real-world data, region pages |

`WorldProjection` maps lat/lon → planar world space with an equirectangular
approximation around a region origin. It round-trips within ~1e-5 degrees.

Real Kamakura landmarks are registered in `KamakuraLocations` with approximate
public-domain coordinates: Tsurugaoka Hachimangu, Kotoku-in (Great Buddha),
Kamakura Station, Yuigahama Beach, Hase-dera, Enoden station.

## Chunk System & Streaming

The world is divided into 256 m square chunks (`ChunkCoord`). Only chunks near
the player are loaded:

- `DEFAULT_LOAD_RADIUS = 3` → 7×7 = 49 chunks ≈ 3.2 km² resident.
- `DEFAULT_UNLOAD_RADIUS = 4` — chunks are released once the player moves away.
- Hard cap of 64 loaded chunks protects memory.
- `WorldChunkManager` queues, loads, unloads, and tracks per-chunk state
  (Unloaded → Queued → Loading → Loaded → Unloading → Failed).
- Chunk loads are capped per frame (`drainQueue`) so streaming never blocks the loop.

Chunk data is produced by a `ChunkLoader` — currently procedural generation,
later pre-baked chunk files on disk.

## Terrain

`TerrainGenerator` produces a deterministic heightfield (value noise + fBm,
seeded) blended with region facts:

- Coastal falloff — sea level at the shoreline, rising inland.
- Rolling hills inland (fBm octaves).
- Kamakura basin — hills ring the center.

All heights are meters above sea level, bounded by `maxHeight`. The same seed
always produces the same world.

## Water

`WaterSystem` animates waves (two sine offsets summed) over a configurable
baseline. `KamakuraOcean` treats everything south of the coastline as ocean.
`isBeach()` detects the shoreline band where land and ocean meet.

The player floats on the water surface when swimming (`inWater` state).

## Player & Camera

`PlayerController` converts input frames (`MovementInput`) into position
updates, frame-rate independent:

- Walk 3.5 m/s, run 7.0 m/s, swim 2.5 m/s.
- Turn rate 180°/s.
- Terrain following (ground height), water floating, collision slide.
- States: Idle, Walking, Running, Sitting, Swimming, OnVehicle, OnTrain.
- Camera modes: ThirdPerson (default, 4 m behind + 1.5 m up) and FirstPerson
  (eye height 1.6 m), toggled by input.
- Board/alight vehicles and trains; enter/exit locations.
- Tracks distance walked and play time.

## Buildings

`BuildingGenerator` produces a plausible Japanese town grid: streets along
chunk axes, houses in the quadrants (Japanese family names), and landmark
buildings (temple/shrine/station) placed from world-space sites.

## Vehicles & Trains

- `VehicleSystem` moves vehicles along waypoint routes at their own speed,
  capped by a vehicle limit.
- `TrainSystem` moves trains along station lists with realistic dwell time at
  each stop (25 s default), direction reversal at the end of the line, and a
  passenger count. The Enoden 300形 runs between Kamakura Station and Hase.

## NPCs

`NpcSystem` updates NPCs toward their hourly schedule waypoints (linear
interpolation, 2 m/s). `NpcBuilder` is a small DSL for defining characters:

```kotlin
NpcBuilder("staff-1", "Tanaka")
    .japaneseName("田中")
    .role(NpcRole.StationStaff)
    .home(station)
    .at(8, station)
    .says("鎌倉駅へようこそ！", "Welcome to Kamakura Station!")
    .can(NpcInteractionType.Info, "Ask about trains")
    .build()
```

Dialogue lines carry conditions (daytime, rainy, first meeting, etc.).
`KamakuraNpcs` provides a starter cast around the station, beach, and shrine.

## Time & Weather

`TimeWeatherSystem` runs a continuous day cycle (0..1 clock) and rolls weather
with weighted transitions:

- `clockLabelAt(0.5)` → "12:00".
- `sunIntensityAt(0.5)` → 1.0 (noon), 0 at midnight.
- Weather: Clear, PartlyCloudy, Cloudy, Rain, HeavyRain, Snow, Fog, Wind.

Both are exposed through runtime flows for renderer/UI reaction.

## Save/Load & World Map

- `WorldSaveService` persists `WorldSaveData` as JSON (same format all platforms).
  Saves capture player position, discovered regions/locations, quests, settings,
  and aggregate stats. Corrupt/missing saves return null — never crash.
- `WorldMap` tracks regions/locations, discovery state, and nearest locations.

## Controller

`WorldController` is the only thing the app shell touches:

```kotlin
val world = KamakuraWorld.create()
world.start()
world.move(MovementInput(forward = 1f), deltaSeconds)
world.teleportTo("kotoku-in")
world.save("slot1")
world.stop()
world.dispose()
```

`KamakuraWorld.create()` wires the whole slice: terrain, water, chunks, NPCs,
trains, vehicles, player, and map.

## Acceptance Status

| Area | Status |
|---|---|
| Runtime isolation & lifecycle | ✅ implemented |
| Coordinate system + projection | ✅ implemented |
| Chunk system + streaming | ✅ implemented |
| Terrain generation | ✅ implemented |
| Water + beach | ✅ implemented |
| Player + camera (3rd/1st) | ✅ implemented |
| Buildings | ✅ implemented |
| Vehicles | ✅ implemented |
| Train framework (Enoden) | ✅ implemented |
| NPC framework | ✅ implemented |
| Weather + time of day | ✅ implemented |
| Save/load | ✅ implemented |
| World map | ✅ implemented |
| 3D renderer | ⏳ next — renderer adapter over the data model |
| Lighting/shadows/LOD/culling | ⏳ next |
| Interior system | ⏳ next |
| Quest/activity system | ⏳ next |
| Audio | ⏳ roadmap |
| Controller input | ⏳ roadmap |
| More regions (Enoshima, Yokohama…) | ⏳ roadmap |

## Data & Licensing

All real-world geography uses approximate public-domain coordinates. No
proprietary Google Maps or 3D data is used. Building/NPC/vehicle content is
procedural and original. See `docs/data/SOURCES.md` for dataset licensing.
