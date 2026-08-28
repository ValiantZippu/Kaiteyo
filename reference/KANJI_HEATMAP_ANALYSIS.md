# kanji-heatmap — Architectural Analysis

> Repo: https://github.com/PikaPikaGems/kanji-heatmap (React 18 + TS + Vite + Cloudflare Pages Functions)  
> Local: `reference/kanji-heatmap` checked out (see README preview images in `docs/images/`)

## 1. Source structure

```
./
├── src/                 # React app (calendar heatmap, kanji detail, sort/filter dialogs)
├── functions/api/       # Cloudflare Pages Functions (Jisho/Jotoba/handwriting proxy — CORS)
├── public/json/v2/      # Generated data (served to app)
├── raw-data/            # Upstream inputs (Kanji Heatmap Data repo output)
├── scripts/generate-v2-json.mjs  # Transform raw-data → public/json/v2
├── e2e/                 # Playwright e2e (bookmarks.spec.ts etc.)
├── docs/                # component-coverage, preview images, notes
└── vite.config.ts, vitest, playwright.config.ts
```

## 2. Data pipeline

```
Upstream (kanji-heatmap-data) → raw-data/*.json → scripts/generate-v2-json.mjs → public/json/v2/*.json → app fetch
```

- Generation is input→output, not live — `generate-v2-json.mjs` is the source-of-truth transform.
- Workers/main-thread double caching noted (`docs/notes/worker-main-thread-double-caching.md`).

## 3. Study-event → heatmap flow

```
STUDY EVENT (kanji interaction, review, bookmark)
  → DATE (LocalDate bucket)
  → ACTIVITY (count/intensity per day)
  → AGGREGATION (week/month rollups, coverage metrics)
  → CALENDAR (GitHub-style grid, intensity scale)
  → HEATMAP (color scale, tooltip, kanji drill-down, sort/filter)
```

- Bookmark coverage visualization is first-class; sort/filter dialogs expose JLPT/grade/frequency views.
- Tests: Vitest unit/component + Playwright e2e (Chromium); `pnpm test`, `pnpm test:e2e`.

## 4. Lessons for Kaiteyo

| Heatmap concept | Kaiteyo equivalent | Module | Decision |
|---|---|---|---|
| Date-bucketed activity | `EventLog` (domain events) + `ReviewLog` + `Summaries` | `desktop/engine/events`, `desktop/appstate/AppState.kt` | Derive heatmap from events, not UI counters |
| Aggregation → calendar | `StatsView` / `DashboardView` / `HeatmapPanel` | `desktop/ui/stats`, `core/presentation/screen/main/screen/statistics` | Reuse Kaiteyo's `Ds*` tokens; GitHub-style calendar as design reference only |
| Filtering (JLPT/grade/frequency) | `SearchEngine`, `Collections`, `KnowledgeGraph` filters | `desktop/engine/search`, `desktop/model` | Adapt filter UX (sort dialog pattern) to media history / mining stats |
| Generated data pattern | `MiningStatisticsStore` per-day/per-source counters | `desktop/engine/mining` | Keep counters derived from real mines; don't invent separate heatmap DB |
| E2E for calendar | Future heatmap E2E (when media statistics surfaced) | `reference/kanji-heatmap/e2e/` pattern | Add Playwright-style coverage after heatmap integration |

## 5. Anti-patterns NOT to copy

- Cloudflare Functions proxy — Kaiteyo is desktop-first; server proxy not needed.
- Standalone `raw-data` → `public/json` generation — Kaiteyo derives directly from `EventLog`/`ReviewLog`.
- Copying heatmap colors/branding — adapt to Kaiteyo `surfaceColors()`.
