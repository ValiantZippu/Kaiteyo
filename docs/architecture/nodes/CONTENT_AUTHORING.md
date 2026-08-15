# Content Authoring & Validation

**Status**: TARGET (authoring infra specified now; editor tool is FUTURE)
**Source spec**: [Node Architecture master spec](../NODE_ARCHITECTURE.md) §146–§148, §145
**Related**: STANDARDS §257–§262 (content formats/packages/licensing/plugins), §361
(no code execution from content), ADR-0015, `docs/architecture/nodes/README.md`,
[`docs/architecture/content.md`](../content.md) (owner doc, STANDARDS §175)

## 1. Goal

Authors (internal and external) create: nodes, relationships, locations, objects,
quests, dialogue, stories, lessons, language content, media links — **without modifying
application source code** (§146). Content is data; the runtime consumes packages.

## 2. Content format (ADR-0015)

- Structured, schema-validated content: JSON or YAML source with JSON Schema, compiled to
  SQLite packages for distribution (single-file, indexed, hashable). The final format
  decision is ADR-0015; the rules here apply regardless of syntax.
- Content schemas are versioned; every node/relationship carries `schemaVersion` (§78).

## 3. Authoring pipeline

```
author (editor / hand-written) 
→ schema validate → relationship validate → asset validate → localize → license check 
→ performance validate → package (world/lesson/content package) → sign/hash → publish
```

- Authors author in *source* form; the pipeline validates, resolves references, and
  produces the compiled package.
- In-app authoring (node editor, §147) is a FUTURE tool that emits the same source form.
  Until then, content is authored as JSON/YAML against these schemas.

## 4. Validation gates (§148)

| Gate | What it checks | Failure |
|---|---|---|
| Schema | node/relationship conforms to registry (NODE_TYPE_REGISTRY / RELATIONSHIP_REGISTRY) + `schemaVersion` | reject |
| Relationship | types from registry; cardinality (§80); no dangling refs; no forbidden cycles (e.g. story `requires` cycles) | reject + report |
| Asset | referenced assets exist, hashed, within size budgets | reject |
| Localization | every required locale string present (dialogue ja/en/furigana, signs, menus) | reject |
| License | creator/license/attribution/version metadata present (STANDARDS §260) | reject |
| Performance | sample queries within latency budgets; cell budgets (§143); package size budgets | warn / reject by severity |

- Validation runs at authoring time **and** at install time (runtime re-verification:
  manifest hash, dependency versions, min engine version — §145).
- The editor UI surfaces each failing gate with a precise message (§147 checklist).

## 5. Node editor (§147) — FUTURE tool spec

Features: create, edit, connect, filter, search, inspect, validate, preview.
Preview surfaces: how the node renders in Browse (§129), Dictionary (§81–§83), and the
Journey world (§139–§140).

Validation checklist surfaced by the editor (hard gates before publish):

- missing relationship (e.g. kanji with no readings)
- invalid node type (not in registry)
- missing localization
- missing asset
- broken reference (dangling node/relationship id)
- circular dependency where forbidden (story/quest/lesson graphs)
- invalid quest requirement (unknown condition type/target)

## 6. Content packages (§145, STANDARDS §259)

```json
{
  "manifest": {
    "packageId": "...", "kind": "world | lesson | deck | dictionary-addon | quest-pack",
    "version": "1.0.0", "minEngineVersion": "3.0",
    "dependencies": [{"packageId": "...", "version": ">=1.0"}],
    "contentHash": "sha256:...",
    "license": "...", "creator": "...", "attribution": "..."
  },
  "assets": [...], "nodes": [...], "relationships": [...], "localization": {...}
}
```

- Packages never contain executable code (STANDARDS §361) — data-driven only.
- Package kinds map to product surfaces: worlds → Journey; lessons/courses → learning;
  deck add-ons → Library; dictionary add-ons → Dictionary/Browse.

## 7. Acceptance criteria

1. A new quest, dialogue, object, or lesson ships as content only — zero engine/source
   changes (proven by the vertical slice, §91).
2. Every published package passes all §4 gates; invalid packages cannot install.
3. Package install is offline-first, hash-verified, version-gated, and safe
   (no arbitrary code).
4. The node editor (when built) can author and publish the same packages the pipeline
   accepts.

## 8. Package kinds and consumers

| Package kind | What it ships | Primary consumer surface | Validation emphasis |
|---|---|---|---|
| `world` | world/region/cell/location content (§88–§92) | Journey | relationship (world tree integrity), asset, license, performance |
| `quest-pack` | quests, objectives, rewards, dialogue, story beats | Journey | condition refs, quest cycles, story ordering, localization |
| `lesson` / `course` | courses, lessons, topics, objectives, exercises | Study / Library | knowledge dimension refs, objective→node refs, ordering |
| `deck` | premade decks, cards, notes | Library | note→card generation rules, source provenance |
| `dictionary-addon` | dictionary entries, pitch, supplements | Dictionary / Browse | entry uniqueness, `source`/`sourceId` provenance, license |
| `theme-pack` | theme tokens, presets | Appearance | token schema, license |
| `media-pack` | media metadata supplements (anime metadata add-ons) | Media | external id mapping (`mapped_to`), license |

- One package may contain multiple kinds (a world pack can carry its quest pack); the
  manifest declares kinds and dependencies.
- Package identity: `packageId` (stable) + `version` (semver) + `contentHash` (sha256);
  same id+version must hash identically (reproducible builds, STANDARDS §344).

## 9. Node editor spec (§147) — FUTURE tool

Target editor: a graph canvas + inspectors, emitting the same source form the CLI
pipeline accepts.

| Surface | Features |
|---|---|
| Canvas | create/edit/connect nodes; filter by family/type; search; pan/zoom; grid snap |
| Node inspector | registry-driven field form (type, family, provenance, payload), schemaVersion aware |
| Relationship inspector | typed edge creation limited to the registry; cardinality hints |
| Validation panel | live per-gate results (§11) with precise messages; publish blocked on failures |
| Preview | how the node renders in Browse (§129), Dictionary (§81–§83), and the world
  (§139–§140); photo/quest/dialogue preview where applicable |

Editor validation checklist surfaced live (hard gates before publish): missing
relationship (kanji with no readings), invalid node type, missing localization, missing
asset, broken reference, forbidden circular dependency (story/quest/lesson graphs),
invalid quest requirement (unknown condition type/target).

## 10. Authoring personas & workflows

| Persona | Authors | Workflow | Trust level |
|---|---|---|---|
| Kaiteyo developer (internal) | engine content, slice world | source form in-repo → pipeline → bundled/update packages | trusted, full pipeline |
| Community author | quest packs, lesson packs, dictionary add-ons | editor or hand-authored source → validate → submit | untrusted; every gate enforced, license verified (§260), capabilities explicit (§262) |
| First-party tooling (CLI, CI) | generated content, migrations | scripted pipeline runs | trusted (same gates) |

- The pipeline is identical for all personas — no privileged format that skips gates
  (§148).
- Community packages install only with explicit user consent and clear metadata
  (creator/license/version/attribution, STANDARDS §260); no silent auto-install.

## 11. Gate failure examples (what gets rejected, with why)

| Gate | Example failure | User-visible message (pattern) |
|---|---|---|
| Schema | quest references unknown nodeType `treasure` | "`quest:errand-01` references unknown node type `treasure`" |
| Relationship | quest objective targets a deleted NPC; `related_to` used where `represents` exists | "dangling reference: `npc:old-35` no longer exists"; lint: "use `represents` instead of `related_to`" |
| Asset | dialogue references `assets/sfx/bye.wav` which is missing | "missing asset: `assets/sfx/bye.wav` (referenced by `dlg:tanaka-bye`)" |
| Localization | dialogue line has `ja` but no `en`/furigana | "localization incomplete: `dlg:tanaka-greeting` line 1 (ja ✓, en ✗, furigana ✗)" |
| License | package manifest lacks creator/license/attribution | "license metadata required (§260): creator, license, attribution, version" |
| Performance | world pack exceeds cell size budget; sample queries over latency budget | "cell `cell-07` exceeds 12 MB budget (18.4 MB)" / "query budget exceeded at package scale" |
| Security | package declares an executable payload | "content packages cannot contain code (§361)" |
