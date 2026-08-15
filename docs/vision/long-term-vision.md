# Long-Term Vision

**Status**: LIVE (direction) — the arc. Roadmaps and TODOs (`docs/roadmap/`,
`docs/planning/`) sequence this; nothing here is a commitment schedule.

## The arc: from study app to language platform

| Stage | State | Ends with |
|---|---|---|
| **v1.x** | ✅ shipped (historical) | Desktop excellence: branding, theming, window experience |
| **v2.x** | ✅ shipped | Learning analytics, unified statistics, adaptive layout, media centre, mining, Anki interop, persistent data |
| **v2.3** | 🟡 in progress | Archived-deck restore UI; release |
| **Node & knowledge layer** | 🔬 TARGET | Node model + registries (ADR-0013), knowledge graph, event-derived user knowledge (ADR-0016), dictionary/media as node interfaces |
| **Journey foundations** | 🔬 TARGET | Engine evaluation (STANDARDS §242) → ADR → runtime prototype; content pipeline (ADR-0015) |
| **Kamakura vertical slice** | 🔬 TARGET | One polished location proves the whole loop (§91, §366) |
| **World expansion** | 🔬 TARGET | v1 region → additional region → additional city → large-scale expansion (packaged, §72) |
| **Platform breadth** | 🟡 ongoing | Children mode; cloud/sync maturity; web/PWA trial; full mobile parity |

The **dependency order is a graph, not a calendar** (§365): foundations first, one
vertical slice, then expansion. The engineering audit (`docs/planning/ENGINEERING_AUDIT.md`)
maps these against actual repo state.

## What "the platform" means at the end state

One Kaiteyo where the learner can:

- look up any Japanese text anywhere (dictionary popup, node traversal, pitch,
  frequency, JLPT, examples) — offline;
- study with FSRS-5 across any device, synced;
- learn from media (subtitles, mining, ASBPlayer-like workflows) and from the
  world (signs, NPCs, quests);
- see their knowledge honestly: per-dimension states, real statistics, exam
  analytics, no fabricated numbers;
- explore a walkable Japan that is beautiful, culturally real, and pedagogically
  woven into the same graph;
- switch between child mode and adult mode with shared underlying data;
- import/export/backup everything, integrate with Anki, never be locked in;
- use the whole system offline, with network features clearly marked.

## Sequencing principles

1. **Foundations before scale** (§368): prove ingestion before 100k vocabulary,
   prove event aggregation before millions of events, prove the design system
   before hundreds of screens, prove one location before 1,000.
2. **Vertical slices** (§322): every milestone is a complete loop (dictionary →
   mining → card → review), not a horizontal layer of stubs.
3. **Data before UI, domain before screen** (§371): the data model and domain are
   designed before any screen for that feature.
4. **Existing technology for infrastructure** (§164): FFmpeg/VLC/mpv, SQLite,
   Ktor, established game engines (evaluated, §242) — custom technology only for
   Kaiteyo's unique value (the learning graph, the world content model).
5. **Never break what ships**: migration paths for data, honest feature flags for
   unfinished systems (§222–§223).

## The four big bets (and their gates)

| Bet | Gate before investment |
|---|---|
| **Node/knowledge layer** (ADR-0013/0016) | Storage decision (new tables vs read-model); event stream first |
| **Journey world** (ADR-0014) | Engine evaluation documented as an ADR (STANDARDS §242); then the Kamakura slice proves the loop |
| **Children mode** (§115) | Same runtime proven on the slice; content pipeline + age-gated content |
| **Cloud/sync** (§27, ADR-0009 evolution) | Conflict model maturity; cross-device UX beyond desktop-first |

## What success looks like (the "product survives" test, §372)

- Another developer can understand it; another AI can safely modify it.
- The database survives migration; the app survives errors; the user can recover
  from failures.
- The UI survives resizing; the media player survives bad files; the dictionary
  survives malformed data; the integration survives unavailable services; the
  game survives large worlds; the statistics survive years of history.
- Kaiteyo remains understandable even after becoming enormous.

## Explicit non-goals (reaffirmed, from `PROJECT_VISION.md` + this vision)

- No gamification gimmicks as the core loop — even in Journey (game philosophy).
- Not a mobile-first app; not a social network; not a textbook replacement.
- No Kaiteyo-hosted central service (sync is provider-based; ADR-0009).
- No full-Japan-at-once: the world grows by packaged slices (§72, §366).
- No custom game engine: evaluate and adopt an established one (§242, §367).

## Related

- Roadmap: `docs/roadmap/ROADMAP.md`; master TODO: `docs/planning/TODO.md`
- Engineering audit & implementation order: `docs/planning/ENGINEERING_AUDIT.md`
- Product audit (what exists vs target): `docs/planning/PRODUCT_AUDIT.md`
- Production phases/risks: `docs/production/phases.md`, `docs/production/risk-register.md`
