# ADR-0015: Data-Driven Content Authoring

**Status**: Proposed — pipeline specified; editor tool is FUTURE
**Date**: 2026-08

## Context

Kaiteyo will need content beyond the bundled dictionary: lessons and courses (NODE
LEARNING family), media metadata, and — critically — Journey world content (quests,
dialogue, objects, locations, stories). STANDARDS §257–§259 require structured content
formats and packages with schemas, validation, and licensing, so authors never modify
source code to ship content. STANDARDS §361 requires that content never executes code.

The current precedent is the KJD data platform (ADR-0007): dictionary data flows
download → verify → parse → normalize → validate → import → index → test (STANDARDS
§184), with provenance kept. Journey/lesson content is a different *kind* (interactive,
reference-rich) but needs the same discipline.

## Decision

Adopt a **data-driven content authoring pipeline** for all authored content, per
`docs/architecture/nodes/CONTENT_AUTHORING.md`:

- **Content is data.** Worlds, quests, dialogue, lessons, objects, and language content
  are authored as structured, schema-validated files (JSON/YAML source with JSON Schema,
  compiled to SQLite packages for distribution — final syntax per this ADR's
  implementation phase).
- **Validation gates are hard gates** (§148): schema, relationship, asset, localization,
  license, performance. Invalid content cannot publish; packages re-verify at install
  (manifest hash, dependency versions, min engine version).
- **Packages are versioned, attributed, and safe**: `packageId`, `version`,
  `minEngineVersion`, `dependencies`, `contentHash`, license/creator/attribution
  (STANDARDS §259–§260); no executable code in packages (STANDARDS §361).
- **The node editor is FUTURE** (§147): a tool that authors and previews the same content
  the pipeline accepts. Until then, content is authored against the schemas in
  `CONTENT_AUTHORING.md` and the registries.
- **Dictionary/language data** continues to flow through the KJD pipeline (ADR-0007);
  this ADR governs *authored* content (learning + world), sharing its validation
  philosophy.

## Alternatives

- **Content as source-code changes (author = developer)** — rejected (STANDARDS §257,
  NODE §146): every lesson/quest becomes a PR, no validation, no community authoring.
- **Unstructured content (freeform JSON/scripts)** — rejected: no schema validation, no
  license enforcement, and "content that executes code" violates STANDARDS §361.
- **Runtime-generated worlds/quests** — rejected (NODE §92, §98): determinism and
  debuggability require authored, validated content, not generation.

## Consequences

- Authorship is decoupled from the release cycle; content ships as packages.
- Validation is a first-class part of authoring (hard gates), reducing broken-content
  bugs before they reach users.
- Runtime keeps a small, safe content loader (no code execution) — consistent with
  ADR-0011 (security first) and STANDARDS §361.
- Cost: schema design and authoring tooling are real work; scheduled as part of the
  content pipeline (TODO §157 items 17–18 area), after the node layer (ADR-0013).

## Implementation notes

- `docs/architecture/nodes/CONTENT_AUTHORING.md` — pipeline, gates, package format,
  editor spec
- `docs/architecture/nodes/JOURNEY_WORLD_SCHEMA.md` — world content schemas (the first
  big consumer)
- `docs/architecture/decisions/0013-node-architecture.md` — node/relationship vocabulary
  the content validates against
