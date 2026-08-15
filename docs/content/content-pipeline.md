# Content Pipeline

**Status**: TARGET (ADR-0015). **Source**: expansion spec §33; NODE §145–§148;
`docs/architecture/nodes/CONTENT_AUTHORING.md`.

## Principle

Future designers should not write code to create quests/lessons/dialogue/etc.
Authoring is **data + validation gates**: write content in the formats
(`content-formats.md`), validate against hard gates, package, install, consume.
Invalid content fails loudly with actionable errors — never ships broken.

## Pipeline stages

```
AUTHOR (content in format JSON, editor or hand-authored)
   ↓
VALIDATE (schema + references + enums + localization + budgets — §148 gates)
   ↓
PACKAGE (versioned package with manifest, checksums, locale bundles)
   ↓
SIGN/VERIFY (integrity)
   ↓
DISTRIBUTE (bundled, update feed, marketplace — future plugin system, ADR-0011)
   ↓
INSTALL (package manager: install/update/uninstall; save compatibility)
   ↓
CONSUME (world, curriculum, exams, stories read from installed packages)
```

## Validation gates (ADR-0015, NODE §148)

| Gate | Fails when |
|---|---|
| Schema | unknown/missing fields, bad types |
| Reference | an id referenced but not resolvable in package or dependencies |
| Enum | value outside the closed catalogs (interaction types, reward types, question types…) |
| Localization | a user-visible string missing a required locale |
| Provenance | license/source/checksum missing or disallowed |
| Budget | package size/asset count exceeds limits |
| Difficulty | learning/story content missing difficulty metadata |
| Child-safety | child-flagged content fails the age filter checks |

A package is **atomic**: it passes all gates or is rejected whole (no partial
install). Rejected packages report each failed gate with a file/line reference
(the node editor surfaces these, NODE §147).

## Packages (NODE §145)

```
kanagawa.kamakura@1.2.0
├── manifest.json          (id, version, deps, checksums, content index)
├── world/                 (locations, cells, NPCs, quests, dialogue, events)
├── learning/              (lessons, stories, exam defs, vocabulary sets)
├── audio/                 (authored dialogue, ambience, announcements)
├── assets/                (meshes, textures — LOD tiers)
└── locale/                (ja, en bundles)
```

- Dependencies declared (`kamakura` → `japan.base`); installers resolve them.
- Updates are new versions (upgrade path; saves reference versions).
- Content is **immutable**: fixes = new version (keeps saves deterministic).

## Install & consume

- Package manager (content service): install/update/uninstall, disk budget,
  cache (`docs/game/world-streaming.md` cache contract).
- Consumers (world engine, curriculum engine, exam engine, reader) read through
  a **content API** (`ContentService.get(kind, id)`) — never raw file paths.
- Uninstall with active saves → graceful handling (honest message; save keeps
  references; content re-installable).

## Authoring tools (NODE §147 — target)

- Node editor (world + learning content) with live validation, gate errors
  inline, localization pane, package preview.
- Authoring personas (developer / community / tooling) with progressively more
  guardrails — community submissions always pass the full gates.
- CLI helpers (`docs/tools/`): validate, package, check-license.

## Acceptance criteria

1. A content package authored without code installs and runs with zero engine
   changes.
2. Every gate has a failing test fixture (TEST_PLAN) proving it catches bad
   content.
3. No partial installs; no broken upgrades; saves survive content updates.
4. Community content cannot bypass validation (capability model, ADR-0011).

## Related

- Formats: [content-formats.md](content-formats.md)
- Authoring contract: `docs/architecture/nodes/CONTENT_AUTHORING.md`
- World packages: `docs/game/world-architecture.md` (packages section)
- Plugin system: `docs/integrations/PLUGINS.md`
