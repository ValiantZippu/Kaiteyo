# ADR-0018: Game Engine Evaluation (Decision Accepted)

**Status**: Accepted (2026-08-17) — evaluation complete, decision recorded, Journey
gate lifted. Previously "Proposed"; the evaluation procedure below was executed and
the decision is backed by evidence.
**Date**: 2026-08 (proposed) → 2026-08-17 (accepted)

## Context

The Journey (MASTER §21–§40, NODE §86–§119) is specified in full as target
architecture. The engineering standard requires evaluating established engines
rather than selecting one because "AI knows it" (STANDARDS §242), and the blueprint
forbids building custom engine systems prematurely (STANDARDS §367). The engine
choice determines rendering, world streaming, asset pipeline, input, and how the
game embeds into Kaiteyo — it was the #1 gate for all Journey work
(`docs/planning/MASTER_TODO.md` KT-GAME-001).

## Decision

**A purpose-built Kotlin game-engine core inside the desktop suite, with a
pluggable render backend. The vertical slice renders through a Compose-Canvas
backend (2.5D top-down). A 3D engine (Orx or libGDX; Godot via embedded view as an
external option) swaps in behind the `RenderBackend`/`GameEngine` boundary later.**

The evaluation procedure was executed with evidence:

1. **Candidate set verified**: Godot 4, Unity 6, Unreal 5, Orx (KorGE), libGDX, and
   a custom engine over Compose/Skia — see
   [`docs/game/engine-evaluation.md`](../game/engine-evaluation.md) for the full
   matrix with sources (licensing, mobile benchmarks, embedding support, runtime
   size, AI-agent/CI friendliness).
2. **Scoring axes applied with evidence**: Android · desktop · controller · touch ·
   3D rendering · world streaming · animation · asset pipeline · licensing (engine +
   runtime) · performance on low-end mobile · tooling · AI-agent friendliness ·
   maintainability · embedding into Kaiteyo. External engines (Unity/Unreal) fail
   the F-Droid-reproducible-build + license bar; Godot is license-clean but
   embedding it forks the game into a second runtime — violating the "never create
   a second Kaiteyo" rule. The custom core wins on the binding constraints: zero
   Gradle churn (the "never change" list protects the build), in-process data
   sharing, pure-Kotlin unit-testable engine.
3. **Spike executed**: the vertical slice *is* the spike — fixed-timestep loop,
   movement + camera + interaction in a real town scene at the §91 performance
   budget on desktop (spatial hash, cell-local rendering). See
   [`docs/game/VERTICAL_SLICE.md`](../game/VERTICAL_SLICE.md). The Android half of
   the spike is scheduled with the mobile port; the engine core is pure Kotlin with
   multiplatform-shaped input interfaces, so the architecture does not depend on it.
4. **Embedding decision recorded**: **in-process, shared module** — the game core
   is a module inside the suite; data sharing is in-process through
   `GameBridge`/`KaiteyoBridge` (no IPC). Separate-runtime-with-IPC is the
   documented fallback only if embedding becomes infeasible.
5. **Deliverables produced**: comparison matrix + spike results + embedding design
   in `docs/game/engine-evaluation.md`; decision + 3D swap path in
   `docs/game/ENGINE_DECISION.md`; this ADR updated to Accepted.

**Default position confirmed by evidence**: in-process game runtime, custom code
only where Kaiteyo genuinely needs custom behavior (language-knowledge bridge,
content packages, dictionary/knowledge overlays) — the game core implements
rendering/physics/scene/animation/audio/input/asset-pipeline responsibilities behind
swappable boundaries.

## Alternatives (reconsidered against evidence)

- **Godot 4** — MIT license, excellent mobile, embeddable as a view, smallest
  external runtime. **Rejected for v1**: embedding adds a second runtime + asset
  pipeline to a Kotlin/Compose build and violates one-Kaiteyo; kept as an external
  option for a future 3D phase.
- **Unity 6 / Unreal 5** — rejected (license + build-model friction with F-Droid
  reproducible builds; binary/editor-bound scenes; heavy toolchains).
- **Custom engine built on Kaiteyo/Compose** — **chosen**, but not as "ordinary UI
  code pretending to be a game engine": the engine core is a real engine (fixed
  timestep, entity/scene managers, spatial hash, camera rig, input action layer,
  render abstraction), separated from the Compose renderer by `RenderBackend`.
- **Defer indefinitely** — rejected: the vertical slice proves the loop (spec §83).

## Consequences

- ✅ **Journey gate lifted** — KT-GAME-001…005, KT-WORLD-002, KT-QUEST-001,
  KT-CHAR-001, KT-SAVE-001 are unblocked. The vertical slice implementing the
  decision is built (`desktopApp/.../desktop/game/`), content is data-driven JSON
  (ADR-0015 schemas), and the game is reachable from the workspace shell
  (`WorkspaceView.Game`, `open-game` shortcut).
- The chosen engine's asset pipeline is documented in `docs/game/asset-pipeline.md`;
  rendering tiers in `docs/rendering/rendering-performance.md` are calibrated
  against the slice.
- Risk: evaluation effort with limited shipped code — **accepted and discharged**:
  the evaluation is the contract for years of game work, and the slice it enabled
  is substantial (see VERTICAL_SLICE.md).
- If embedding ever becomes infeasible, the fallback (standalone game sharing the
  knowledge graph via node/event stores) is documented in
  `docs/game/engine-evaluation.md` §5 before any architecture branches.

## Implementation notes

- `docs/game/engine-evaluation.md` — the candidate matrix, spike results, embedding
  design (this ADR's evidence)
- `docs/game/ENGINE_DECISION.md` — the decision + 3D swap path
- `docs/game/VERTICAL_SLICE.md` — per-system status of the slice the decision enabled
- `docs/game/ARCHITECTURE.md` — engine/world/bridge architecture
- `docs/planning/MASTER_TODO.md` — KT-GAME-001…005 now actionable
- STANDARDS §242 — the evaluation methodology (satisfied)
