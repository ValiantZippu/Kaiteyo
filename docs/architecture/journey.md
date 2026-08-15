# Kaiteyo Architecture — Journey, World & Game Runtime Plan

**Status**: **Not started** — design phase. Nothing ships until the mandated engine
evaluation (STANDARDS §242) is performed and recorded as an ADR.
**Owner**: unassigned (see §9)
**Related**: `docs/architecture/decisions/` (ADRs 0013–0015) · `docs/planning/ENGINEERING_AUDIT.md` §5 (phases 21–26) · `docs/roadmap/PROJECT_VISION.md`
**Deep spec**: `docs/architecture/NODE_ARCHITECTURE.md` (§76–§162 — the product spec) +
`docs/architecture/nodes/JOURNEY_WORLD_SCHEMA.md` (world content) +
`docs/architecture/nodes/GAMEPLAY_SYSTEMS.md` (gameplay systems, §86–§119) +
`docs/architecture/nodes/JOURNEY_RUNTIME_SPEC.md` (runtime) +
`docs/architecture/nodes/CONTENT_AUTHORING.md` (authoring). This file is the
STANDARDS-§175 owner document; the deep spec is the contract.

## 1. Purpose

Journey is Kaiteyo's stylized, cross-platform learning world — a walkable, recognizable
Japan where language and knowledge state are the game mechanics. This document defines the
planned architecture boundaries so future work starts from a contract, not a blank page.

## 2. Decisions already made

- **DO NOT build a game engine from scratch** (§242, §367). Use an established engine's
  rendering/physics/scene/animation/audio/input/navigation/asset pipeline; custom code
  lives *around* the engine.
- **One polished location first** (§366): the Kamakura vertical slice must prove
  movement, camera, interaction, dictionary, language node, NPC, dialogue, quest,
  discovery, photography, collection, knowledge, stats, save/load, performance — before
  any world expansion.
- **Data-driven content, not scripts** (§361): content packages never execute arbitrary
  application code.
- **Artistic accuracy over photogrammetry** (§249–§250): recognizable landmarks, street
  layout, railways, major geography, cultural locations; geographic truth is separated
  from artistic representation.
- **Real-world data with verified licensing** (§248): OpenStreetMap/government/public GIS
  datasets with attribution; never scrape Google Maps/imagery without permission.

## 3. Runtime boundaries

```
Kaiteyo Application
  ↓  JourneyService (domain API: identity, knowledge state, progress, settings, events, discoveries)
  ↓  World Runtime Adapter
Journey Runtime (selected engine)
  ↓  World Content (content packages)
```

- The game runtime **never touches application database tables directly** (§244) — it
  talks through `JourneyService` and the World Runtime Adapter.
- No duplicated user database (§243): knowledge/study state lives in Kaiteyo; the world
  references it through the adapter.
- `JourneyService` is the stable interface (§209) regardless of engine choice.

## 4. Node system

The world is authored as a **node graph** (locations → points of interest → language
nodes/NPCs/quests/discoveries/photo spots). Node definitions live in world content
packages (see `docs/architecture/content.md`), not in code. A node carries: id, position,
type, linked content (dictionary entries, dialogue, quests), required knowledge state,
and discovery/photo flags. Schema must be proven before scaling (§368: prove the quest
schema before thousands of quests).

## 5. World creation pipeline (§247)

`reference → geographic data → procedural base → artist refinement → gameplay placement →
optimization → validation`. Blender for source assets (models/UVs/rigging/animation/LOD);
source assets separated from processed/exported (`art/source|processed|exported`, §245–
§246).

## 6. Input & accessibility

- Input abstraction with actions (Move, Look, Interact, Run, Camera, Map, Dictionary,
  Journal, Quest, Inventory, Pause, Screenshot, Confirm, Back) mapped per keyboard/mouse/
  touch/controller (§251).
- Remappable controls persisted in settings (§252); Simple/Advanced settings tiers
  (§253); sensitivity/deadzone/vibration where relevant.
- Accessibility: reduced motion, text scaling, high contrast, subtitle controls,
  controller remapping, keyboard navigation (§254).

## 7. Game security

Content packages cannot execute arbitrary application code (§361); capabilities/permissions
are explicit if any scripted content is ever introduced. Media/asset inputs are untrusted
(§357).

## 8. Performance & scale

- Per-subsystem performance budgets before world expansion (§190); prove one location at
  target FPS first (§366, §368).
- Memory discipline for 3D assets/world cells (streaming, lazy loading, caching,
  eviction, §278).
- Scale testing before production (§369): large Journey saves, many quests, many nodes.

## 9. Open items (research before implementation)

1. **Engine evaluation (§242)** — Godot vs Unity vs Unreal against Kaiteyo's actual
   requirements (stylized 2D/3D, mobile+desktop, embedding in the Compose app, licensing).
   Must be a documented technical evaluation with prototype + benchmark (§364), then an
   ADR.
2. Embedding strategy — how the engine hosts inside the Compose window on each platform
   (§243).
3. Node/quest/discovery schema design (data model before UI, §371).
4. Content authoring tooling and package format (see `docs/architecture/content.md`).
5. Ownership assignment for the Journey subsystem.
6. Journey ↔ knowledge connection design (how world encounters feed knowledge state and
   vice versa — the "world/knowledge connection" that makes Kaiteyo unique, §363).
