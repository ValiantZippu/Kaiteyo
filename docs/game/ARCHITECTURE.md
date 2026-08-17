# Kaiteyo Game — Architecture

**Status:** reflects the implemented vertical slice (v1 of the game subsystem).
Target-design documents (`game-overview.md`, `world-architecture.md`, …) describe
the long-term vision; this document describes the code that actually exists.

## Principles (from the game spec)

1. The game is a **real game**, not gamified UI — a fixed-timestep engine core,
   a world you move through, interaction, NPCs, quests, photography, collections,
   travel, save games.
2. **No second Kaiteyo.** The game is a layer over the existing dictionary,
   study, stats and mining systems, reached through one clean API (`GameBridge`).
3. **Data-driven content.** Regions, locations, objects, NPCs, dialogues, quests,
   stories, collectibles and knowledge are JSON in
   `desktopApp/src/jvmMain/resources/game/`. New content never requires engine
   rewrites.
4. **Engine-agnostic core.** The engine core is pure Kotlin (kotlinx
   serialization only) behind a `RenderBackend` boundary, so a 3D engine can
   swap in later without touching content, quests, learning, saves or UI.
   See [ENGINE_DECISION.md](ENGINE_DECISION.md).

## Package map — `ua.syt0r.kanji.desktop.game`

```
desktop/game/
├── engine/                Engine core (pure Kotlin, no Compose)
│   ├── geom/              Vec2, Rect — 2D math
│   ├── GameEngine.kt      Engine container: loop, scene, entities
│   ├── EngineTime.kt      Fixed-delta time model
│   ├── Entity.kt          Entity ids + registry
│   ├── Scene.kt           Scene interface (update/render)
│   ├── SpatialHash.kt     Spatial index for proximity queries
│   ├── camera/            Camera + CameraRig (TPP/FPP, zoom, shake)
│   ├── input/             InputAction, InputManager, InputState,
│   │                      ControlScheme (rebinding), InputProvider
│   │                      (keyboard/mouse now; gamepad/touch interfaces)
│   └── render/            RenderBackend interface + Compose Canvas backend
│
├── world/                 World model + runtime
│   ├── WorldModels.kt     World, Region, District, Cell, Location,
│   │                      WorldObject, Station, TravelNetwork, WorldNode
│   ├── GameWorld.kt       Indexes, lookup, bounds, spawn
│   ├── TileGrid.kt        Tile map + collision queries
│   └── (WorldStreamer)    Cell streaming (loaded-set, update-on-move)
│
├── player/                PlayerEntity, PlayerState, PlayerController
├── interaction/           Interactable, InteractionSystem, InteractionBehavior
├── npc/                   NpcDefinition, NpcEntity, NpcDirector (schedules,
│                          relationships, spawn into the entity registry)
├── dialogue/              DialogueRunner, DialogueLine, DialogueEffect
├── quest/                 Quest graph (prereqs), QuestManager, QuestEvent,
│                          QuestRewardHandler
├── story/                 StoryEngine, chapters/scenes (data + engine)
├── learning/              KnowledgeNode/Edge, KnowledgeGraph, LearningManager
├── bridge/                GameBridge (toasts, activity log) + KaiteyoBridge
│                          (real dictionary/mine/stats/analytics wiring)
├── photography/           PhotoCamera, PhotoSubject, PhotoAlbum
├── collection/            CollectionManager (stamps/discoveries)
├── time/                  WorldClock (day/minutes) + WeatherSystem
├── settings/              GameSettings (assistance level, camera, audio, autosave)
├── save/                  SaveData, SaveManager (versioned, migration), slots
├── state/                 GameState (compose state for panels/menus/discoveries)
├── content/               WorldContentLoader (JSON → LoadedContent)
├── validation/            ContentValidator (cross-reference checks)
├── debug/                 DebugTools (overlay, teleport, content reload)
├── render/                WorldRenderer (the slice's 2.5D town renderer)
├── ui/                    GameView (workspace mount), GameCanvas (loop + input)
│   ├── hud/               GameHud (interaction prompt, objective, minimap-ish)
│   ├── panels/            Dialogue, KnowledgeDiscovery, QuestComplete,
│   │                      PhotoMode, Travel, Debug overlays
│   └── menus/             GameMenuHost + Map, QuestLog, Collection, Album,
│                          KnowledgeMap, Character, Settings views
└── GameSession.kt         Root: owns every system, drives tick(), handles
                           interactions/dialogue effects/quest rewards,
                           saves/loads, travel, photo capture
```

## Engine core

- **`GameEngine`** — owns `GameLoop` (fixed timestep, accumulator), the active
  `Scene`, and the `EntityRegistry` (spatial hash). `advance(frameDelta)` is
  called once per host frame; the loop runs `Scene.update` at a fixed rate so
  movement is deterministic regardless of frame rate.
- **`Entity`** — id, position (Vec2), size, velocity, `solid` flag, update
  callback. The registry maintains a `SpatialHash` for cheap neighbour queries.
- **`Camera` / `CameraRig`** — camera holds position/zoom/bounds/shake; the rig
  implements third-person follow (smoothing, zoom) and first-person control
  (direct mouse-look). Settings: sensitivity, invert Y, smoothing, FOV/zoom
  bounds, default mode, camera distance (see `CameraSettings`).
- **`RenderBackend`** — the boundary to any renderer: `drawRect`, `drawCircle`,
  `drawText`, `drawLine`, camera transform helpers. The shipped backend draws
  into a Compose `Canvas`. A 3D engine implements the same interface later.
- **`InputManager` / `InputAction` / `ControlScheme`** — game logic never checks
  keys; it polls `InputState.wasPressedThisFrame(InputAction.X)`. Bindings live
  in `ControlScheme` (persisted, rebindable). `InputProvider` is the per-device
  interface; keyboard/mouse providers exist, gamepad/touch providers are
  interfaces ready to implement (see [TODO.md](TODO.md)).
- **`EngineTime`** — fixed delta + accumulated time + paused flag.

## World model & content

World hierarchy (see [WORLD.md](WORLD.md) for the authoring view):

```
World → Region → District → Cell → Location / WorldObject / Station
```

- `WorldObject` carries position/size/solid/sprite key, a Japanese `label`
  (rendered in the world), `interactableId` (read / buy-drink / sit /
  photo-spot / station:… / inspect), and `learningTargets` (knowledge ids).
- `Location` has an anchor + discovery radius + `learningTargets` — walking
  near it discovers it (spec §70, §8: the world itself teaches).
- `Station` + `TravelNetwork` model the train system: stations connect to
  destinations; travel unlocks through quest rewards (`travel:<id>` flags).
- `TileGrid` — tile layers (ground/decoration) + solid-tile collision and
  bounds; the player collides with tiles and solid objects.
- `WorldStreamer` — maintains the loaded cell set from the player's current
  cell, the streaming hook for region packages later (see
  [world-streaming.md](world-streaming.md)).
- `WorldContentLoader` — reads `world.json`, `npcs.json`, `quests.json`,
  `dialogue.json`, `knowledge.json`, `stories.json`, `collectibles.json` from
  resources, deserializes via kotlinx.serialization into `LoadedContent`.
- `ContentValidator` — run at session start: duplicate ids, dangling
  references (quest targets, dialogue ids, knowledge targets, npc ids,
  locations, stations, collectible unlocks), spawn sanity. A validation failure
  is a hard error — content bugs never silently reach the player.

## Learning & Kaiteyo integration

- **Knowledge graph** (`learning/KnowledgeModels.kt`): `KnowledgeNode`
  (id, headword, reading, meaning, kanji, type word/kanji/phrase) and typed
  `KnowledgeEdge` (kanji→word, word→word related, word→phrase, …).
  `KnowledgeGraph` indexes nodes and edges; the game navigates it in the
  Knowledge Map menu (spec §73–§75).
- **`LearningManager`** — tracks per-node state (unseen/known/learned),
  `discover(nodeId, assistanceLevel, source, position)` records a discovery
  with a source (Environment / Object / Npc / Quest / Photo / Inspect) and
  queues one discovery popup at a time; `mine(nodeId)` sends the word to
  Kaiteyo's card pool via the bridge (spec §65 — same mining architecture,
  no separate card system).
- **`GameBridge`** — the game's only outward API: toasts, activity-log
  recording, discovery stats, word mining. `KaiteyoBridge` is the desktop
  implementation wired to `AppState` (dictionary lookup for enrichments,
  `MiningEngine` for card creation, analytics for stats). Because the game
  only talks to the bridge, a non-desktop host gets a no-op bridge for free.
- Active-time accounting (spec §66–§67): only ticks where the player moved
  count toward study-adjacent stats; menus, dialogue, AFK never accrue.

## Quest system

- `Quest` = id, title/description, level, prerequisites (ids), objectives,
  rewards, learningTargets, location, dialogueId, `completionCondition`.
- `QuestManager` tracks per-quest progress; `reportEvent(QuestEvent)`
  advances objectives that match (talk, read sign, discover location, reach
  location, learn word, buy item, take photo, ride train). Objectives complete
  in any order; a quest completes when all objectives are done.
- Availability is a dependency graph: `availableQuests()` only returns quests
  whose prerequisites are satisfied; `refreshAvailability()` runs after every
  completion. `maybeAutoStartQuest()` starts the first available quest when
  nothing is active, so the slice never strands the player (spec §36 guidance).
- Rewards route through `QuestRewardHandler` (implemented by `GameSession`):
  xp, items, cosmetics, stamps, unlocks (stations/locations/quests), and
  knowledge discoveries — every reward lands in a real system.

## NPC / dialogue / story

- `NpcDefinition` (id, name, nameJp, appearance key, location, dialogueId,
  idle line, schedule, knowledge targets) + `NpcEntity` with a
  `Relationship` (met, talkedCount, questsHelped). `NpcDirector` spawns NPCs
  into the entity registry and ticks schedules (day-phase-based wandering).
- `DialogueRunner` — scripted lines with Japanese text, reading, translation,
  optional audio key, `learningTargets` (discovered when the line is shown,
  spec §61 listening practice), and `DialogueEffect`s (grant quest, start
  story, discover knowledge, set flag, give item, advance story, open shop).
  The engine only emits effects; `GameSession` implements them.
- `StoryEngine` — chapters/scenes with prerequisites and rewards; data exists
  (`stories.json`) and the engine advances/restores; the UI hook (story panel)
  is PLANNED (see [TODO.md](TODO.md)).

## Photography & collection

- `PhotoCamera` — viewfinder frame, focus, zoom; `capture()` scans objects and
  NPCs inside the frame, tags `PhotoSubject`s whose label resolves in the
  knowledge graph (spec §43–§45), and records the shot with tags.
- `PhotoAlbum` — persisted photos with subject tags; the Album menu groups
  them by category (Animals/Food/Places/Transport/Nature/Objects/People/Signs).
- `CollectionManager` — stamps/discoveries (`loc:<id>`, `stamp:<id>`, …)
  unlocked by discovery and quest rewards, persisted and shown in the
  Collection menu.

## Save system

- `SaveData` — versioned payload (`SAVE_VERSION`, `game` key): player state,
  quest progress, knowledge state, collection, world state (discovered
  locations/objects, flags, day/time, weather), story progress, album,
  settings, stats.
- `SaveManager` — slots under `~/.kaiteyo/game/saves/` (`slot.json`),
  autosave on a timer (configurable minutes), load with migration hook
  (`migrate`) so future versions upgrade old saves instead of breaking them.
- Saves are JSON via kotlinx.serialization — inspectable and testable.

## Settings

`GameSettings` (persisted in the save): assistance level (Japanese only / +
reading / + translation, spec §31–§32), camera (sensitivity, invert Y,
smoothing, zoom bounds, default mode — spec §13), photo (zoom step), audio
(master toggle), autosave minutes, active quest hint. The Settings menu writes
through the session so every value is live.

## UI

- `GameView` mounts the game as a workspace destination (`WorkspaceView.Game`,
  nav item, `open-game` command); `GameCanvas` runs the engine loop on a
  `Canvas` and feeds keyboard/mouse into `InputManager`, scaled to world
  coordinates.
- `GameHud` — minimal: interaction prompt (`[E] 話す`), current objective,
  time/weather chip, photo/menu hints. No XP bars or button walls (spec §71).
- Panels — Dialogue (Japanese, toggleable reading/translation), Knowledge
  discovery popup (word card + Mine/Copy/Close), Quest complete, Photo mode
  overlay, Travel panel (unlocked stations), Debug overlay.
- `GameMenuHost` — pause menu with contextual views: Map (discovered
  locations + travel), Quest log, Collection, Album, Knowledge map (visual
  graph of discovered nodes), Character (xp/level, items, cosmetics),
  Settings. Closing the menu resumes the world.

## Debug tools (`debug/DebugTools.kt`)

Toggleable overlay (F3-style): FPS, frame time, loaded cells, entity count,
cell/region ids, active quest count, mouse world position. Actions: teleport
to spawn/region, reload content, reset save, jump to next quest, noclip
toggle. Debug input is bound in `ControlScheme` under the debug category.

## Tests

`desktopApp/src/jvmTest/.../game/`:

- `QuestGraphTest` — quest availability/prereq graph, objective events,
  completion, rewards.
- `GameCoreLogicTest` — save migration, input bindings/rebinding,
  knowledge-graph navigation, tile collision queries, world-content load
  round-trip.
- `GameSessionTest` — a full session (loaded content + fake bridge): movement
  tick, object read → discovery, NPC talk → dialogue → quest grant, quest
  completion → rewards, photo capture → album + tags, save → load round-trip.

## How to run

From the repo root: `./gradlew :desktopApp:run`, then open the **Game**
destination from the nav rail (or `open-game` command). The game boots the
Hamanaka seaside-town slice, validates content, and either continues the
existing save or starts a fresh journey.

See [VERTICAL_SLICE.md](VERTICAL_SLICE.md) for the honest per-system status and
[ROADMAP.md](ROADMAP.md) / [TODO.md](TODO.md) for what is next.
