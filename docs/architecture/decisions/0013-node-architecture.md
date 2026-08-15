# ADR-0013: Node-Based Architecture (Target)

**Status**: Proposed — agreed as target architecture, not yet implemented
**Date**: 2026-08

## Context

Kaiteyo has grown into many surfaces — dictionary, study, media, mining, statistics,
exams, and (planned) a Journey world — but the connections between them exist only in
user flows, not in data. The product spec (NODE §76–§83, §150–§152) defines the product
as *one language world*: the user moves from kanji → word → sentence → media → Journey →
discovery → deck → review → stats without switching applications, and every subsystem
must be able to consume another subsystem's data without duplicating it.

Today there is no shared identity/relationship layer. Data lives in two SQLDelight
databases (ADR-0005) plus desktop-suite JSON files; cross-domain questions ("where have I
seen this word?") are answered ad hoc per screen, if at all. Without a typed node layer,
Journey, media exposure, discovery, and user knowledge can never be one trajectory — the
core promise of NODE §87 and §150.

## Decision

Adopt a **node-based architecture as the connective tissue** of Kaiteyo, per the Node
Architecture master spec (`docs/architecture/NODE_ARCHITECTURE.md`, §76–§162):

- **Typed node families** (registry: `docs/architecture/nodes/NODE_TYPE_REGISTRY.md`) —
  LANGUAGE, LEARNING, MEDIA, WORLD, GAMEPLAY, USER, SYSTEM. A node has a typed schema,
  never a generic "thing" with nullable soup (§76, §77).
- **Universal node contract** (§78): `id`, `nodeType`, `schemaVersion`, timestamps,
  provenance (`source`/`sourceId`), optional `parentId`/`ownerId`/`worldId`/tags.
- **First-class typed relationships** (§79–§80, registry:
  `docs/architecture/nodes/RELATIONSHIP_REGISTRY.md`) — a controlled vocabulary with
  validation; `related_to` is linted as the escape hatch, not the default.
- **User knowledge as nodes** (§84–§85, `KNOWLEDGE_STATE_MODEL.md`) — per-dimension
  states derived from events; FSRS-5 remains the scheduler (STANDARDS §6 never-change).
- **The two-graph bridge** (§149): the World Graph and Language Knowledge Graph connect
  only through explicit edge types (`represents`, `encountered_by`/`discovered_by`,
  `mined_from`, `appears_in_media`/`appears_in_scene`, ambient `teaches`).

Scope discipline:

- **The node layer is additive and layered.** The existing `AppDataDatabase` and
  `UserDataDatabase` remain the storage of record. Node identities, edges, and knowledge
  state are implemented as typed tables/views on top (exact storage mechanics — new
  SQLDelight tables vs. read-model — are an implementation detail resolved during
  implementation with this ADR's constraints).
- **Not a rewrite.** No existing subsystem is replaced until a node-backed equivalent
  proves superior (STANDARDS §166, §370). The two-app consolidation question (audit §1)
  stays separate.
- **Status is TARGET** (NODE §158): nothing here is implemented yet; docs and registries
  are the contract.

## Alternatives

- **No shared layer (status quo)** — rejected: cross-domain queries (NODE §83 "where have
  I seen this?") stay ad hoc; Journey/media/discovery knowledge can never unify; every
  new surface re-solves identity and provenance.
- **A generic property-graph (node = JSON blob)** — rejected (§76): untyped nodes with
  nullable fields cannot be validated, cannot evolve schema, and invite exactly the
  "developer database" UX NODE §81 forbids.
- **Full schema rewrite now** — rejected (STANDARDS §166): the current databases work and
  are tested; the node layer is added above them, incrementally.
- **Two entirely separate graphs (language vs. game) with no bridge** — rejected (§149):
  the product is defined by the bridge; without it Journey cannot feed knowledge or stats.

## Consequences

- Implementers get a stable vocabulary (`NODE_ARCHITECTURE.md`, registries) before
  writing code; the §157 handoff order in `docs/planning/TODO.md` governs sequencing.
- New features must pass the NODE §152 test ("what node does this create/consume?").
- Adding node/relationship types is a registry + validation change, gated by the authoring
  pipeline (§148) — no ad-hoc types inline.
- Risk: an over-engineered graph layer. Mitigation: the registries are intentionally
  minimal; the vertical slice (NODE §91) is the proof gate before any world-scale work.

## Implementation notes

- `docs/architecture/NODE_ARCHITECTURE.md` — master spec (§76–§162)
- `docs/architecture/nodes/` — registries, knowledge model, world schema, runtime spec,
  authoring pipeline
- `docs/planning/TODO.md` → Node & Journey — the build order (model → graph → dictionary/
  kanji/vocab node views → user knowledge → …)
- First milestone (per §157): node contract + registries as storage, then the knowledge
  graph bridge for media and Journey exposure.
