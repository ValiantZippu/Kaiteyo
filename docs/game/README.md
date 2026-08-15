# 🎮 game — The Journey

> **Status: TARGET ARCHITECTURE.** The Journey is fully specified here and in
> `docs/architecture/nodes/` (NODE §76–§162) — but **no game code exists**. Per NODE
> §158: CURRENT / TARGET / FUTURE are separated everywhere. Every doc in this section is
> a *specification*, not an implementation report.
>
> **Hard gate**: no Journey runtime code may start before the game-engine decision
> (ADR-0018) is `ACCEPTED` (STANDARDS §242, `docs/planning/MASTER_TODO.md` KT-GAME-001).

## What the Journey is

The Journey is **an actual game** — not "gamification", not a "gamified learning map"
(MASTER §21). It is a Japanese-learning world where exploration, observation, discovery,
language, culture, story, photography, collection, and daily life are the mechanics, and
light RPG-style progression exists only as motivation (NODE §86). The player should feel
*"I am living inside a Japanese learning world"* — never *"I am grinding XP in an
educational RPG"* (NODE §86).

## Document index

| Document | Covers |
|---|---|
| [`game-overview.md`](game-overview.md) | Genre, pillars, core loop, scope & production gates, **engine evaluation** (ADR-0018) |
| [`world-architecture.md`](world-architecture.md) | World hierarchy, fidelity levels L0–L4, real-world vs gameplay data, content packages |
| [`map-system.md`](map-system.md) | Map modes, progressive reveal, knowledge-density overlay |
| [`world-streaming.md`](world-streaming.md) | Cell/manager streaming architecture, loading zones, budgets, cache contract |
| [`camera.md`](camera.md) | First/third-person camera modes, smoothing, accessibility |
| [`player.md`](player.md) | Player/avatar, camera summary, input abstraction (actions → physical controls) |
| [`interaction-system.md`](interaction-system.md) | InteractionComponent and reusable interaction types |
| [`npc-system.md`](npc-system.md) | NPC model, schedules, simulation tiers |
| [`dialogue-system.md`](dialogue-system.md) | Data-driven dialogue trees |
| [`quest-system.md`](quest-system.md) | Quest node model, state machine, non-punitive rules, UI |
| [`progression-rewards.md`](progression-rewards.md) | Anti-grind progression and rewards |
| [`collectibles-photography.md`](collectibles-photography.md) | Discovery, photos, collections, swimming |
| [`transportation.md`](transportation.md) | Trains/stations — scalable data-driven network |
| [`environment-simulation.md`](environment-simulation.md) | Time, weather, seasons (deterministic) |
| [`learning-in-world.md`](learning-in-world.md) | WORLD_TEXT_SELECTED → knowledge flow |
| [`save-system.md`](save-system.md) | Versioned, checksummed, split-rule saves |
| [`game-audio.md`](game-audio.md) | Audio buses, zones, mixing |
| [`asset-pipeline.md`](asset-pipeline.md) | Game asset pipeline, naming/formats/licenses, packages |

## Companion sections

- **Rendering & art direction**: `docs/rendering/` (architecture, environment visuals, per-tier budgets)
- **Input layer**: `docs/input/` (abstract action layer, game/mobile controls, accessibility)
- **Game/learning doctrine**: `docs/vision/` (game-philosophy, learning-philosophy, child-experience, normal-user-experience)
- **Curriculum engine**: `docs/learning/` (graph-based curriculum, adaptive learning, progress model)

## Cross-references

- **Master spec**: `docs/architecture/NODE_ARCHITECTURE.md` (§76–§162) — canonical Journey + node product spec
- **World schema + worked slice**: `docs/architecture/nodes/JOURNEY_WORLD_SCHEMA.md`, `JOURNEY_SLICE_CONTENT.md`
- **Runtime spec**: `docs/architecture/nodes/JOURNEY_RUNTIME_SPEC.md`
- **Gameplay systems**: `docs/architecture/nodes/GAMEPLAY_SYSTEMS.md`
- **Content authoring**: `docs/architecture/nodes/CONTENT_AUTHORING.md` (ADR-0015) + `docs/content/`
- **Phases/gates**: `docs/production/phases.md` · **Product**: `docs/product/PRODUCT.md` (MASTER §21–§40)
