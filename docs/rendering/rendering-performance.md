# Rendering Performance

**Status**: TARGET — proposed budgets to be measured (§188–§190); not shipped
facts. **Source**: expansion spec §44; JOURNEY_RUNTIME_SPEC §10; STANDARDS
§188–§192, §143.

## Principle

Performance budgets are **measured against reference devices**, never assumed.
Every budget below is a proposed target for the engine evaluation and slice; the
numbers are the *contract to verify*, not the claim.

## Platform tiers (JOURNEY_RUNTIME_SPEC §10)

| Tier | Target | Strategy |
|---|---|---|
| Desktop high-end | 60+ FPS, high fidelity | max LOD, dynamic resolution cap, high texture/mesh budgets |
| Desktop mid | 60 FPS | medium LOD, adjusted budgets |
| Mobile mid (Android/iOS) | 30–60 FPS | low LOD, occlusion, aggressive streaming, reduced NPC tiers |
| Mobile low | 30 FPS | minimum preset, dynamic resolution active |

**Same world data across tiers; different presentation budgets** — LOD, streaming,
and NPC tiers are the levers, never content removal.

## Proposed budgets (verify per tier)

| Metric | Desktop high | Desktop mid | Mobile mid | Mobile low |
|---|---|---|---|---|
| Frame time | ≤16.6 ms (60 FPS) | ≤16.6 ms | ≤33 ms (30 FPS) | ≤33 ms |
| Draw calls (visible) | ≤3000 | ≤2000 | ≤800 | ≤500 |
| Triangles (visible) | ≤3M | ≤2M | ≤600K | ≤350K |
| Texture memory (resident) | ≤1.5 GB | ≤1 GB | ≤384 MB | ≤256 MB |
| Cell residency | high-LOD neighborhood | medium | low-LOD + proxies | minimum |
| Streaming per frame | budgeted, no hitch | same | same | same |
| Dynamic resolution | cap at 4K | 1440p cap | active | active (primary lever) |
| Load time (slice start) | ≤5 s | ≤5 s | ≤8 s | ≤10 s |

All numbers are **proposed targets** (expansion §44: "Do not write arbitrary
numbers as facts. Mark proposed targets as proposed targets") — to be validated
against reference devices during the slice (TEST_PLAN performance section).

## Application performance (non-world) — existing contracts

`docs/architecture/performance.md` covers the shipped app: startup, navigation,
search, dictionary lookup, media startup, subtitle parsing, statistics rendering.
The world must not regress these; the world shares process/memory with the app
(one product).

## Measurement discipline (§188–§192)

1. Measure, don't guess: frame time, draw calls, memory, streaming stalls, load
   times — on reference devices per tier.
2. Profilers: engine profilers + platform profilers (JFR/JMC for JVM layers;
   GPU profilers for rendering).
3. `EXPLAIN QUERY PLAN` for the world's data queries (knowledge overlay, POI
   queries) — no brute-force scans (STANDARDS §192).
4. Budget violations → profile first, then adjust presentation (LOD, streaming,
   dynamic resolution) — never remove content silently.

## Streaming & cache budgets (from `docs/game/world-streaming.md`)

- Adjacent-cell loads within budget, zero frame hitches.
- Cache: LRU with LOD priority; per-tier size limits.
- Low-memory pressure → LOD degrade, never crash.

## Acceptance criteria

1. Every tier hits its frame/memory budget on reference devices (slice gate).
2. Streaming produces zero frame hitches in the travel flow (street → train →
   city → region).
3. All budget numbers have a measured report behind them (§188).
4. No world feature ships that violates its budget without a documented,
   measured mitigation.

## Related

- App performance: `docs/architecture/performance.md`
- Streaming: `docs/game/world-streaming.md` · Rendering: [rendering-architecture.md](rendering-architecture.md)
- Testing: `docs/testing/README.md`, `docs/architecture/nodes/TEST_PLAN.md` (§13)
