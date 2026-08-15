# Kaiteyo Content (target + live)

**Status**: MIXED — content *authoring contracts* are TARGET (ADR-0015, schema
design); some content is live (dictionary data via KJD, deck/card content). This
directory defines the **content formats** for everything authorable: courses,
quests, dialogue, stories, exams, world content, localization.

## Document map

| Document | Covers |
|---|---|
| [content-formats.md](content-formats.md) | The format family: course, lesson, quest, dialogue, story, exam, world content — JSON schemas, validation, versioning |
| [content-pipeline.md](content-pipeline.md) | Authoring → validation gates → packages → install → consume (ADR-0015) |

## Principles

1. **Data-driven everything** (§73): quests, lessons, dialogue, NPC schedules,
   locations, exam questions, vocabulary sets, rewards, events, world
   interactions — no hardcoded content in code.
2. **Validation gates** (§148, ADR-0015): every content package passes schema +
   reference + localization + size checks before it can ship; invalid packages
   fail loudly with actionable errors.
3. **Versioned packages** (ADR-0015): content is versioned, installable,
   upgradeable; saves reference versions.
4. **Localization is part of content** (Japanese + English + future).
5. **No fake content**: shipped content is real, validated, and honest; the
   content pipeline never fabricates.
6. **World content** (locations, NPCs, quests, dialogue) uses the same pipeline
   as learning content — one authoring system (`docs/game/world-architecture.md`).

## Relationship to the KJD pipeline

The **KJD data platform** (`docs/data/ARCHITECTURE.md`) ingests *open datasets*
(JMdict, KANJIDIC, etc.) into the bundled database. This `content/` layer is the
**authoring layer for Kaiteyo-authored content** (courses, stories, quests,
dialogue, exams, world) on top of that data. Both feed the same consumers; both
carry provenance.

## Related

- Authoring contract: `docs/architecture/nodes/CONTENT_AUTHORING.md` (ADR-0015)
- World schema: `docs/architecture/nodes/JOURNEY_WORLD_SCHEMA.md`
- Data: `docs/data/SOURCES.md`, `docs/data/ARCHITECTURE.md`
- Learning content status: `docs/learning/README.md`
