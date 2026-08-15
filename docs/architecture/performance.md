# Kaiteyo Architecture — Performance Strategy

**Status**: Strategy defined; P0 desktop-performance work open
**Owner**: cross-cutting (per-subsystem budgets)
**Related**: `docs/planning/CURRENT_ISSUES.md` #1–#4 · `docs/design/ANIMATION_SYSTEM.md` ·
`docs/architecture/media.md` · `docs/architecture/database.md`

## 1. Rule (§188)

Measure before optimizing. Never claim "this should be fast" — measure startup, screen
transition, search latency, query latency, frame time, memory, CPU/GPU, media startup,
subtitle parsing, card creation, import, sync. Profile first; optimize against evidence
(§189). No blind optimization, no fabricated precision.

## 2. Budgets (§190)

| Subsystem | Budget (target) | Status |
|---|---|---|
| Startup | fast cold start to interactive | P3 optimization item (TODO) |
| UI interaction | 60 FPS; no dropped frames on hover/resize/theme switch | **P0 open** — stutter + resize glitches (CURRENT_ISSUES #1–#4); window-system rebuild already fixed native drag 1:1 + dock resize snapping; runtime sweep remains |
| Search | interactive latency at production dictionary scale | functional (prebuilt per-dictionary index); FTS/trigram future |
| Database | indexed per access pattern; `EXPLAIN QUERY PLAN` for slow queries | `daily_stats` rollups keep heatmap/today O(1); duration clamps keep study-time honest |
| Media | no UI-thread blocking; tick loop can never die | fail-safe tick + degradation done; thumbnails/capture on background threads |
| Memory | lazy lists, caches with eviction, no unbounded retention | P3 (TODO): image caching, list lazy-loading, Compose metrics |
| Journey | prove one location at target FPS before world expansion | not started (§366/§368) |

If a feature violates a budget: profile first, then decide.

## 3. UI performance rules (§191)

Avoid: unnecessary recomposition, large allocations, UI-thread blocking, sync network,
unnecessary image decoding, unbounded lists, full-screen re-renders. Use: lazy lists,
pagination, caching, background work, incremental updates, proper state. Motion is
centralized (no invented per-screen durations, §307). Modifier order and shadow-first
painting eliminate overdraw artifacts (audit pass) — visual noise is a perf/UX issue too.

## 4. Database performance (§192)

- Indexes follow real access patterns (`tag_parent_idx`, `card_tag_tag_idx`,
  `study_history_*`, `writing_attempt_*`, `exam_*`, `learning_mistake_*`); composite PKs
  keep uniqueness.
- `daily_stats` per-day rollups updated incrementally — the heatmap and today panel never
  scan full history.
- Timezone-aware day aggregation (`date(round((timestamp - offset)/1000), 'unixepoch')`).
- Duration clamps (120 s/review) keep totals sane.
- Slow-query investigation starts with `EXPLAIN QUERY PLAN`; don't add hundreds of
  indexes blindly.

## 5. Media performance

- 10 Hz tick with fail-safe wrapper (`tickInternal` + catch): failures → throttled toast
  + log; poisoned backends closed + dropped. Composition-read helpers degrade via
  `runCatching`.
- State pulled via accessors; hot paths avoid per-frame allocation (AFK rain ≤ 48
  particles, deterministic, no per-frame allocation).
- Thumbnails: background threads, 20 s cap, cached by item id; folder scanning/watching
  never blocks the UI; hardware acceleration capability-gated.

## 6. Profiling tools (§189)

- JVM/Kotlin: Java Flight Recorder, JDK Mission Control, JetBrains profilers, Android
  Studio Profiler.
- Native/media: platform profilers, GPU/render/memory profilers.
- Compose: compiler metrics for recomposition analysis (P3 item).

## 7. Known open work

- Animation stutter + resize glitches (P0, desktop polish track — CURRENT_ISSUES #1–#4).
- Lazy loading for large lists, image caching, startup-time reduction, Compose compiler
  metrics (P3).
- Search indexing (FTS/trigram) at production dictionary scale.
- Journey world performance validation (after engine selection).
- Performance regression harness (measure budgets in CI) — planned, not yet built.

## 8. Validation note

Performance claims require measurements (§188). The P0 polish track's runtime sweeps
(BLOCKED list) are the mechanism for verifying desktop animation/resize budgets on real
Windows/Linux hardware.
