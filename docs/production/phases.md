# Implementation Phases & Dependency Graph

**Status**: LIVE planning. **Source**: STANDARDS §365 (phase graph), expansion
spec §51–§52; mapped against actual repo state in `docs/planning/ENGINEERING_AUDIT.md`
§5.

## Principle

Phases are a **dependency graph, not a calendar** (§365, §51). Reorder against
actual repository conditions; never "Phase 1: build everything" (§51).

## The phase graph (adapted to this repository)

```
PHASE 0  Repository stabilization          ✅ done
PHASE 1  Toolchain (pinned versions)        ✅ done
PHASE 2  Design system (tokens, Ds*)        ✅ done
PHASE 3  Core domain (learning models)      ✅ done
PHASE 4  Database (two SQLDelight DBs)      ✅ done
PHASE 5  Data ingestion (KJD)               ✅ done
PHASE 6  Knowledge graph                    🟡 partial (profile real; graph target)
PHASE 7  Dictionary/search                  🟡 partial (lookup real; FTS target)
PHASE 8  Kanji/Kana/Vocabulary              ✅ done
PHASE 9  User knowledge                     ✅ done (KnowledgeProfileEngine)
PHASE 10 Library/decks/cards                ✅ done
PHASE 11 Review scheduler (FSRS-5)          ✅ done
PHASE 12 Statistics/events                  ✅ done
PHASE 13 Exams                              ✅ done
PHASE 14 Media abstraction                  ✅ done (VLC/mpv/Java Sound)
PHASE 15 Subtitle engine                    ✅ done
PHASE 16 Mining                             ✅ done
PHASE 17 Anki/Yomitan integrations          🟡 partial (apkg done; AnkiConnect e2e BLOCKED)
PHASE 18 Home/Browse/Library/Stats UX       ✅ done
PHASE 19 Navigation/floating/launchpad      ✅ done
PHASE 20 Embedded browser/media workflows   🟡 partial (media done; browser not core)
PHASE 21+ Journey (data model → authoring)  ⬜ TARGET (spec done; engine eval first — §242)
PHASE 27 Cloud/sync                         🟡 partial (GitHub sync shipped)
PHASE 28 Release engineering                ✅ done
```

## The implementation graph (§52 — dependencies that must hold)

```
DATABASE
   ├──▶ DICTIONARY ──▶ LOOKUP ──▶ MINING ──▶ (DECK / ANKI)
   ├──▶ LEARNING ENGINE ──▶ CURRICULUM ──▶ EXAMS ──▶ STATS
   └──▶ MEDIA ──▶ SUBTITLES ──▶ MINING ──▶ DICTIONARY

CORE (node layer — ADR-0013)
   ├──▶ WORLD (LOCATION / NPC / QUEST / STREAMING)
   └──▶ LEARNING (KNOWLEDGE ──▶ PROGRESS ──▶ RECOMMENDATION)
```

Ordering rules:

1. **Node layer first** (ADR-0013 storage decision) — the world, knowledge
   graph, and unified stats all build on it.
2. **Events before knowledge** (ADR-0016): the event stream is the substrate.
3. **Engine evaluation before Journey code** (§242): a documented ADR (Godot vs
   Unity vs Unreal) gates everything world-related.
4. **Vertical slice before expansion** (§366): Kamakura+Enoshima proves the
   loop; no region is built until the slice passes its exit criteria.
5. **Consolidation before new data systems**: the two-app decision
   (ENGINEERING_AUDIT §7-1) gates SRS/settings/statistics/nav/decks dedup.

## Phase gates (definition of "done" per phase)

| Phase | Gate |
|---|---|
| 4 Database | migrations versioned + tested; schema review recorded (ADR-0005) |
| 5 Data ingestion | a dataset end-to-end with provenance (KJD) |
| 6 Knowledge graph | traversal answers "where have I seen this?" from real events |
| 17 Integrations | AnkiConnect e2e verified against a live Anki |
| 21+ Journey | engine ADR → runtime prototype → slice exit criteria (TEST_PLAN §13) |
| 27 Sync | cross-device conflict resolution verified on real devices |

## Which work can be parallel (see also §78 report)

- **Safe to parallelize**: design system extensions, media polish, data
  datasets (independent KJD adapters), content format authoring, tooling
  (docs check, content validate), test expansion.
- **Sequential (dependency-bound)**: node layer → knowledge graph → world;
  engine evaluation → runtime → slice; consolidation decision → refactor.

## Related

- Full status mapping: `docs/planning/ENGINEERING_AUDIT.md` §5
- Master TODO: `docs/planning/TODO.md` · Roadmap: `docs/roadmap/ROADMAP.md`
- Risks: [risk-register.md](risk-register.md)
