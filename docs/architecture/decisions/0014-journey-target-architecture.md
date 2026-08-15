# ADR-0014: Journey as Target Architecture

**Status**: Proposed — Journey is target architecture, not implementation (NODE §158)
**Date**: 2026-08

## Context

The product spec defines Journey (NODE §86–§119) as the exploration-first, context-first
side of Kaiteyo: a stylized Japanese world (recommended vertical slice: Kamakura +
Enoshima, §91) where exploring, photographing, discovering, and talking feed the shared
knowledge graph. Two questions must be answered before any engine work: (1) is Journey a
separate runtime or part of the shared app, and (2) how is it built without repeating the
"fake feature" failures documented in `docs/planning/PRODUCT_AUDIT.md`.

STANDARDS §242 mandates evaluating established engines (Godot, Unity, Unreal) rather than
building a game engine from scratch, and STANDARDS §365 phases 21–23 order: Journey data
model → runtime prototype → vertical slice. Nothing about Journey exists in the codebase
today.

## Decision

- **Journey is TARGET architecture** (NODE §158–§159). It is documented, specified, and
  scheduled — not implemented and not claimed.
- **Journey is a destination inside Kaiteyo, not a separate application** (NODE §114,
  §141): shared identity, theme, settings, knowledge, statistics, library, update
  mechanism. The world is a *front-end context over the shared knowledge graph*, connected
  through the §149 bridge.
- **A separate game runtime is explicitly allowed** (SEPARATE RUNTIME, NODE §159): if the
  engine evaluation (§242) selects a game engine (e.g. Godot), it embeds as a runtime
  behind a Journey boundary (STANDARDS §243–§244: `JourneyService` → World Runtime
  Adapter; the runtime never touches application tables directly; learning data stays in
  shared user data).
- **No custom engine build.** The engine decision follows the STANDARDS §242 evaluation
  process and STANDARDS §364 technology selection process; it is a documented decision,
  not an adoption by default.
- **Vertical slice first** (NODE §91, STANDARDS §366): the Kamakura + Enoshima slice is
  the proof gate — movement, camera, interaction, dictionary, language nodes, NPC,
  dialogue, quest, discovery, photography, collection, knowledge, stats, save/load,
  performance — before any world expansion (§90, §92).
- **Content-driven world** (NODE §88, §90, §146–§148, ADR-0015): world, quests, dialogue,
  and objects ship as validated, versioned content packages; the engine only evaluates.

## Alternatives

- **Journey as a fully separate app** — rejected (NODE §114, §141): would duplicate the
  knowledge graph, user data, and identity, breaking the §150 loop and "where have I seen
  this?" (§83).
- **Journey as in-app Compose-only experience (no game runtime)** — considered; kept as a
  fallback if the engine evaluation finds embedding infeasible for the target platforms.
  It changes rendering tech, not architecture (world content stays data-driven).
- **Build now, document later** — rejected: STANDARDS §370 and the product audit make
  undocumented, unvalidated "features" the failure mode to avoid; the architecture (this
  ADR + `NODE_ARCHITECTURE.md` + world/runtime specs) precedes code.
- **A custom game engine** — rejected (STANDARDS §242, §367): use established engine
  infrastructure; custom code belongs around the engine, not in competition with it.

## Consequences

- No Journey code is written until: (a) the engine evaluation (STANDARDS §242) completes,
  (b) the node/knowledge foundations (ADR-0013, TODO §157 items 4–5) exist, and (c) the
  content pipeline (ADR-0015) can validate world content.
- The roadmap gains a Journey entry (see `docs/roadmap/ROADMAP.md`) labeled TARGET; no
  marketing claim of a playable world.
- Heavy production dependencies are flagged honestly (CONTENT/3D/ART/AUDIO PRODUCTION,
  EXTERNAL DEPENDENCY for geodata, LEGAL REVIEW for dataset licensing) per NODE §159.

## Implementation notes

- `docs/architecture/nodes/JOURNEY_WORLD_SCHEMA.md` — world content contract
- `docs/architecture/nodes/JOURNEY_RUNTIME_SPEC.md` — runtime contract (UI layers, HUD,
  overlays, input, audio, save, performance budgets)
- `docs/architecture/decisions/0013-node-architecture.md` — the knowledge foundation it
  depends on
- First milestone (per §157 order): Journey runtime prototype (item 17) after knowledge
  graph (item 5) and user knowledge (item 8).
