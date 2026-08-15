# Node Architecture — Reference Docs

**Status**: TARGET — the node system, knowledge graph, and Journey world are blueprints,
not implementation. Reality check: `docs/planning/PRODUCT_AUDIT.md` and
`docs/planning/ENGINEERING_AUDIT.md`. Implementation order: `docs/planning/TODO.md` →
Node & Journey (§157).

This directory is the implementer's reference for the
[Node Architecture master spec](../NODE_ARCHITECTURE.md) (§76–§162). Read order:

1. `../NODE_ARCHITECTURE.md` — the master spec (product decisions, UX, principles)
2. This index (the map)
3. `NODE_TYPE_REGISTRY.md` — every node type and its fields
4. `RELATIONSHIP_REGISTRY.md` — every typed relationship
5. `KNOWLEDGE_STATE_MODEL.md` — user knowledge: dimensions, states, scoring, FSRS bridge
6. `JOURNEY_WORLD_SCHEMA.md` — world content: cells, objects, NPCs, dialogue, quests, stories
7. `JOURNEY_RUNTIME_SPEC.md` — runtime: UI layers, HUD, overlays, input, audio, save, perf
8. `CONTENT_AUTHORING.md` — authoring pipeline, validation gates, packages
9. `NODE_DATA_MODEL.md` — the storage contract: node/edge/knowledge/event/save schemas
   and the money queries
10. `EVENT_CATALOG.md` — the event catalog behind stats & knowledge (§210–§211)
11. `SERVICE_CONTRACTS.md` — the stable internal service interfaces (§209, §244)
12. `TEST_PLAN.md` — the test contract node & Journey must satisfy (§215–§218)
13. `UX_FLOWS.md` — UX flows: consolidation target for existing surfaces + Journey
    flows (§296–§299)
14. `GAMEPLAY_SYSTEMS.md` — Journey gameplay-systems spec (§86–§119): philosophy, core
    loop, quests, dialogue, NPCs, photography, time/weather/seasons, progression

Decisions: ADR-0013 (node storage/architecture), ADR-0014 (Journey target architecture),
ADR-0015 (content authoring format), ADR-0016 (event-driven user knowledge) —
`docs/architecture/decisions/`.
14. `JOURNEY_SLICE_CONTENT.md` — worked Kamakura+Enoshima slice: complete reference
    JSON for every world schema (§91 proof material)

Decisions: ADR-0013 (node storage/architecture), ADR-0014 (Journey target architecture),
ADR-0015 (content authoring format) — `docs/architecture/decisions/`.

## The master conceptual graph (§160)

```
                         KAITEYO
                            |
       +--------------------+--------------------+
       |                    |                    |
   LANGUAGE              MEDIA                WORLD
       |                    |                    |
   Kanji                  Anime             Location
   Vocab                  Episode           Object
   Grammar                Scene             NPC
   Sentence               Subtitle          Activity
       |                    |                    |
       +---------+----------+---------+----------+
                 |                    |
             KNOWLEDGE            DISCOVERY
                 |                    |
                 +---------+----------+
                           |
                     USER KNOWLEDGE
                           |
              +------------+------------+
              |            |            |
           LIBRARY       STATS        EXAMS
              |
           DECK/CARD
              |
           REVIEW
              |
           MASTERY
```

Key flow (the product, §150):

```
DISCOVER → UNDERSTAND → PRACTICE → USE → REMEMBER → EXPLORE MORE
```

Bridges between the Language Knowledge Graph and the World Graph (§149):
`represents`, `encountered_by`/`discovered_by`, `mined_from`,
`appears_in_media`/`appears_in_scene`, `teaches` (ambient) — see
[RELATIONSHIP_REGISTRY.md](RELATIONSHIP_REGISTRY.md) §6.

## Current-state mapping

| Concept | In the repo today | Target |
|---|---|---|
| Node contract/registry | — (SQLDelight tables, ADR-0005) | `NODE_TYPE_REGISTRY.md` |
| Relationships/edges | — | `RELATIONSHIP_REGISTRY.md` |
| User knowledge | FSRS-5 card scheduling only | `KNOWLEDGE_STATE_MODEL.md` |
| Storage contract | two SQLDelight DBs (ADR-0005) | `NODE_DATA_MODEL.md` |
| Service interfaces | suite services (partial) | `SERVICE_CONTRACTS.md` |
| Event stream | suite + core event stacks | `EVENT_CATALOG.md` |
| Test contract | `docs/testing/README.md` | `TEST_PLAN.md` |
| UX flows | screens implemented; flows ad hoc | `UX_FLOWS.md` |
| World content | — | `JOURNEY_WORLD_SCHEMA.md` + `JOURNEY_SLICE_CONTENT.md` |
| Journey runtime | — | `JOURNEY_RUNTIME_SPEC.md` |
| Authoring | kjd data platform (dictionary data only) | `CONTENT_AUTHORING.md` |

## Status flags legend (NODE §158–§159)

`CURRENT` = exists today (verified in `PRODUCT_AUDIT.md`) · `TARGET` = specified, not
built · `FUTURE` = postponed · `OPEN QUESTION` = uncertain · `HIGH COST` · `SEPARATE
RUNTIME` · `EXTERNAL DEPENDENCY` · `LEGAL REVIEW` · `CONTENT PRODUCTION` · `ART
PRODUCTION` · `3D PRODUCTION` · `AUDIO PRODUCTION`.
