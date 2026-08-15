# ADR-0018: Game Engine Evaluation (Decision Pending)

**Status**: Proposed — evaluation in progress; **no Journey runtime code may start until
this is Accepted**
**Date**: 2026-08

## Context

The Journey (MASTER §21–§40, NODE §86–§119) is specified in full as target architecture
but no game code exists. The engineering standard requires evaluating established
engines rather than selecting one because "AI knows it" (STANDARDS §242), and the
blueprint forbids building custom engine systems prematurely (STANDARDS §367). The
engine choice determines rendering, world streaming, asset pipeline, input, and how the
game embeds into Kaiteyo — it is the #1 gate for all Journey work
(`docs/planning/MASTER_TODO.md` KT-GAME-001).

## Decision

**Evaluate, then decide — with evidence.** The evaluation procedure:

1. **Candidate set** (must be verified, not assumed): Godot 4, Unity, Unreal 5, a
   custom engine, and the existing Kaiteyo rendering technology (Compose/Skia).
2. **Scoring axes** (each scored with evidence): Android · desktop (Win/macOS/Linux) ·
   controller · touch · 3D rendering · world streaming · animation · asset pipeline ·
   licensing (engine + runtime) · performance on low-end mobile · tooling ·
   **AI-agent friendliness** (scriptability, headless builds, diffable scene files) ·
   maintainability · **embedding into Kaiteyo** (separate runtime with IPC vs embedded
   view vs shared module; data sharing via SQLDelight/JSON).
3. **Spike before decision**: one test scene with movement + camera + interaction at the
   §91 slice's performance budget on desktop AND Android.
4. **Embedding decision recorded**: separate runtime (recommended default) vs shared
   module, with the integration surface defined (how the Journey talks to the core app —
   node/event stores, IPC or in-process boundary).
5. **Deliverables**: written comparison table, spike results, embedding design, and this
   ADR updated to Accepted with consequences + upgrade path.

**Default position** (to be confirmed or overturned by evidence): a separate game
runtime embedded/linked with Kaiteyo, using an established engine's rendering/physics/
scene/animation/audio/input/asset pipeline (STANDARDS §367) — custom code only where
Kaiteyo genuinely needs custom behavior (language-knowledge bridge, content packages,
the dictionary/knowledge overlays).

## Alternatives

- **Choose now, no evaluation** — rejected (STANDARDS §242, MASTER §36): the choice
  locks rendering, streaming, asset pipeline, and platform story for years.
- **Custom engine built on Kaiteyo/Compose** — rejected as the default (STANDARDS §164,
  §367): building a 3D engine is reinventing infrastructure; would only be chosen if
  every established engine fails the axes with evidence.
- **Defer indefinitely** — rejected: the blueprint commits to the Journey as a real
  product (MASTER §21); the evaluation is the first concrete step and unblocks all
  game packages.

## Consequences

- No Journey code until Accepted (KT-GAME-001 gate); documentation and content schemas
  (ADR-0015) proceed in parallel — they are engine-agnostic.
- The chosen engine's asset pipeline shapes `docs/game/asset-pipeline.md`; rendering tiers in
  `docs/rendering/rendering-performance.md` are calibrated against the spike results.
- Risk: evaluation effort with no code shipped — accepted; the spike is small and the
  decision is the contract for years of game work.
- If embedding turns out to be infeasible cleanly, the fallback (standalone game
  sharing the knowledge graph via node/event stores) is documented here before any
  architecture branches.

## Implementation notes

- `docs/game/game-overview.md` §Engine evaluation — the candidate matrix and axes (this ADR's companion)
- `docs/production/phases.md` — the phase graph (Journey stages depend on this decision)
- `docs/planning/MASTER_TODO.md` — KT-GAME-001…005
- STANDARDS §242 — the evaluation methodology
