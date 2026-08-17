# Kaiteyo Game — 書いてよ World

> "I am playing a beautiful little Japanese world and naturally learning Japanese
> while exploring it." — not "I am studying Japanese inside a game."

The Game is a **second space inside Kaiteyo**: an optional, deeply integrated
learning environment. Normal Kaiteyo (Dictionary, Library, Study, Media, Stats,
Sync) stays exactly as it is. The game adds a world you can walk around — a
Japanese summer town with stations, shops, beaches, signs that carry real
Japanese, NPCs, quests, photography, collections and travel.

The direction is **Shashingo + Nintendo-like presentation + Japanese
summer/daily-life game + the Kaiteyo learning system**, with light RPG elements.
Progression means *"I know more Japanese and I have explored more of the world"* —
never a grind, never a stat spreadsheet, never a textbook with 3D graphics.

## Where things live

| Path | What |
|---|---|
| `desktopApp/.../desktop/game/` | The whole game subsystem (JVM, desktop suite) |
| `desktopApp/src/jvmMain/resources/game/` | All game content (data-driven JSON) |
| `desktopApp/src/jvmTest/.../desktop/game/` | Game logic tests |
| `docs/game/` | This documentation |

## Docs

- [ARCHITECTURE.md](ARCHITECTURE.md) — the full architecture (engine, world,
  player, camera, input, interaction, NPC, dialogue, quest, story, learning,
  bridge, save, debug, validation)
- [ENGINE_DECISION.md](ENGINE_DECISION.md) — why the engine core is what it is,
  and the exact swap path to a 3D engine (Orx/libGDX)
- [WORLD.md](WORLD.md) — world structure + content pipeline (how to add a
  region, a quest, a word — no code required)
- [VERTICAL_SLICE.md](VERTICAL_SLICE.md) — what is implemented today, how to
  run it, and the honest per-system status
- [ROADMAP.md](ROADMAP.md) — the living roadmap (foundation → slice → content →
  advanced → expansion)
- [TODO.md](TODO.md) — the massive categorized TODO (spec §133)

## Design rules (condensed from the game spec)

1. It must feel like a **game first**. Learning is embedded in places, people,
   objects, stories, activities, photography and travel.
2. **A tiny beautiful town beats 100 km² of empty terrain.** Prove the core
   loop before scaling.
3. **Never create a second Kaiteyo.** The game is a layer over the existing
   dictionary, knowledge, study, stats, media and mining systems — through
   clean APIs (`GameBridge`).
4. Content is **data-driven** — new regions, quests, NPCs and vocabulary are
   JSON, never engine rewrites.
5. **No fake features.** Every subsystem either works, is honestly marked
   ARCHITECTED/PARTIALLY IMPLEMENTED/PLANNED, or has its TODO recorded.

## Status (short)

| System | Status |
|---|---|
| Engine core (fixed-timestep loop, scene, entities, camera, spatial index) | IMPLEMENTED |
| Input layer (InputAction, rebinding, gamepad/touch interfaces) | IMPLEMENTED (gamepad/touch wiring PLANNED) |
| World model + content loader + validation | IMPLEMENTED (vertical slice) |
| Player + movement + collision | IMPLEMENTED (2D slice; 3D PLANNED) |
| Interaction system | IMPLEMENTED |
| NPCs + schedules + relationships | IMPLEMENTED (schedules basic) |
| Dialogue system | IMPLEMENTED |
| Quest system (graph, events, rewards) | IMPLEMENTED |
| Story system | ARCHITECTED (data + engine; UI/trigger PLANNED) |
| Knowledge graph + Kaiteyo bridge (dictionary/mine/stats) | IMPLEMENTED |
| Photography + album | IMPLEMENTED (slice viewfinder) |
| Collections | IMPLEMENTED |
| Travel network | IMPLEMENTED (architecture; multi-region PLANNED) |
| Time + weather | IMPLEMENTED (basic) |
| Save system (versioned, migration) | IMPLEMENTED |
| Debug tools + content validation | IMPLEMENTED |
| 3D rendering (Orx/libGDX) | PLANNED (swap at `RenderBackend`) |

Full per-system status: [VERTICAL_SLICE.md](VERTICAL_SLICE.md).
