# Engine Decision

**Status:** accepted (v1 of the game subsystem) · **Last reviewed:** Game v1

## The requirement

The game must be a *real game*, not gamified UI. The spec (§101) demands a
proper game engine with 3D rendering, mobile support, controller/touch input,
world streaming, animation, physics, scene management and an asset pipeline,
with a clean boundary from Kaiteyo core.

## What we chose

**A purpose-built Kotlin game-engine core inside the desktop suite, with a
pluggable render backend. The vertical slice renders through a Compose-Canvas
backend (2.5D top-down). A 3D engine (Orx or libGDX) swaps in behind the
`RenderBackend`/`GameEngine` boundary later.**

Concretely, the engine core (`ua.syt0r.kanji.desktop.game.engine`) is a real
engine: fixed-timestep loop (`GameLoop`), engine time, entity manager, scene
manager, spatial hash, camera + camera rig (TPP/FPP modes), input action layer
with rebinding and per-device providers, and a render abstraction. It does not
touch Compose, AppState or any Kaiteyo core — it is pure Kotlin + kotlinx
serialization, unit-testable in isolation.

## Rationale

1. **The project pins Kotlin 2.1 / Compose 1.8.2 and forbids Gradle churn.**
   Orx (KorGE 4) and libGDX are real engines, but integrating either into this
   multiplatform build (plugin versions, konan toolchains, asset pipelines)
   is a large, build-system-level change. The AGENTS.md "never change" list
   protects the build. We do not need that risk to prove the game loop.
2. **The boundary is what matters.** The spec's real requirement is that the
   game never becomes "ordinary UI code pretending to be a game engine".
   We satisfy it by separating *game core* (engine-agnostic) from *render
   backend* (swappable). The Compose-Canvas renderer is one backend, honestly
   labeled 2.5D. A 3D engine later implements the same boundaries — the game
   core, content, save system, learning and UI all stay.
3. **Zero new dependencies, same toolchain.** Canvas rendering reuses the
   Compose stack the suite already ships. Japanese glyphs render natively.
4. **Desktop first.** Desktop is Kaiteyo's primary platform; the game starts
   there exactly like the rest of the desktop suite. The engine core is pure
   Kotlin and the input/gamepad/touch interfaces are already multiplatform
   shapes, so a future mobile port has real groundwork.

## The 3D swap path (when we take it)

| Today (slice) | 3D engine (future) |
|---|---|
| `RenderBackend` draw calls (rect/circle/text…) into Canvas | Orx scene graph / libGDX `SpriteBatch` + meshes |
| 2D `Camera` (zoom, follow) | 3D camera fed by the same `CameraRig` + `CameraSettings` (FOV, distance, sensitivity, TPP/FPP) |
| Tile-grid collision | 3D physics (or a navmesh) behind the same `PlayerController` contract |
| Vector characters | Model/skeleton rigs behind the same `NpcDefinition`/appearance data |
| World cells (1 today) | The same `Region → District → Cell` streaming model |

The engine boundary guarantees: **content, quests, learning, save files and
the Kaiteyo bridge are 100% engine-independent** — a 3D migration changes the
render layer and physics adapter only.

## Why not X?

- **Compose-as-engine (no core):** rejected — that is exactly the "turn UI
  code into a game engine" anti-pattern the spec warns about.
- **Godot/Unity/Unreal:** not viable inside a Kotlin multiplatform app; would
  be a separate app, breaking the "one Kaiteyo" rule.
- **libGDX today:** viable later; heavier Android/iOS story than Orx, no
  native Compose host. Documented as an option.
- **Orx (KorGE) today:** the most Kotlin-native 3D path and the primary
  recommendation for the 3D phase, but a significant build change — deferred
  until the slice proves the loop (spec §83: prove movement → interaction →
  learning before expanding).
