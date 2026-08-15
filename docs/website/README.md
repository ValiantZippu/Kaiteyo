# Kaiteyo Website

**Status**: LIVE (static site; Python build, consumes `docs/`) + PLANNED (the
full product site vision + the interactive command-center backend).
**Source**: expansion spec §58 + MASTER website spec §0–§88; ADR-0019;
`website/` module; `docs/roadmap/ROADMAP.md` (web/PWA future).

## The command center (live, read-only)

Since 2026-08 the site includes a **public project command center** at `/project/`:

| Surface | Route | Renders from |
|---|---|---|
| Project Status | `/project/` | `systems.json` + live counters |
| Kanban | `/project/kanban/` | `docs/planning/MASTER_TODO.md` (parsed) |
| Roadmap | `/project/roadmap/` | `roadmap.json` |
| Whiteboard | `/project/whiteboard/` | `whiteboard.json` (interactive pan/zoom/LOD) |
| Suggestions | `/project/suggestions/` | `suggestions.json` + GitHub issues link |
| Decisions | `/project/decisions/` | `docs/architecture/decisions/*.md` (parsed) |
| Activity | `/project/activity/` | `activity.json` (real snapshot) |
| Contributing | `/project/contributing/` | live tasks + package knowledge |

**Honesty rules** (binding, ARCHITECTURE.md §4): every status matches
`docs/planning/CURRENT_STATE.md`; no fabricated data; no fake interactivity —
card moves, whiteboard edits, and suggestion submission are documented API
contracts, never frontend simulations.

## The four surfaces (don't conflate them)

| Surface | Purpose | Status |
|---|---|---|
| **Full Kaiteyo** | the product (desktop/Android/iOS) | LIVE |
| **Website** | communicate the product visually; downloads, docs, community | LIVE (static), site vision PLANNED |
| **Documentation site** | developer/AI documentation (this corpus) | LIVE (consumed by the build) |
| **Web trial** | a small working slice of Kaiteyo in the browser | FUTURE (a slice, never the full app) |

## Website content plan (§58)

- **Homepage** — what Kaiteyo is, the pillars (craft, offline, open source,
  respect the learner), visual proof (not mockups — real screenshots).
- **Features** — dictionary, learning, media, statistics, world (Journey **as
  target**, never a launch claim — honesty rule §64).
- **Downloads** — installers per platform (from the release pipeline).
- **Documentation** — the docs corpus (rendered).
- **Wiki / Q&A** — community (future; optional, not core).
- **Children** — child mode page (target; honest).
- **Privacy / licenses** — privacy policy, open-source licenses, dataset
  attributions (`docs/legal/`, `docs/data/SOURCES.md`).
- **Community** — optional (FUTURE_IDEAS: shared decks, marketplace, study groups).

## Web trial (future)

A **small working slice** visitors can try in-browser: kana practice, a few kanji,
a dictionary lookup, a flashcard demo — *proving the product, not pretending to be
the product*. The trial is a bounded vertical slice (web/PWA — see ROADMAP), never
the full application, and never a demo with fake data (honesty rule).

## Build & maintenance (live)

- `website/` is a Python build (`build.py`) that consumes `../docs` — a docs
  change regenerates the site (`docs/planning/TODO.md` → TECHNICAL DEBT: `dist/`
  is committed; CI regeneration is the paydown).
- The site must not contradict the docs (one truth). Journey/world pages must
  carry the TARGET label.

## Acceptance criteria

1. Every product claim on the site matches the docs' honest statuses.
2. Downloads link to real release artifacts (stage/verify pipeline).
3. The site regenerates from docs without manual editing of generated output.
4. The web trial (when built) is a real slice with real data.

## Related

- **Architecture**: `docs/website/ARCHITECTURE.md` (build flow, honesty rules, maintenance)
- **Data model**: `docs/website/DATA_MODEL.md` (unified project model + target schema)
- **API contracts**: `docs/website/API.md` (interactive layer: auth, kanban, whiteboard,
  suggestions, activity, realtime, security)
- **Decision**: `docs/architecture/decisions/0019-website-command-center.md`
- Roadmap: `docs/roadmap/ROADMAP.md` · Build: `docs/development/COMMANDS.md`
- Legal/licensing: `docs/legal/README.md` · Data attribution: `docs/data/SOURCES.md`
- Product vision: `docs/vision/product-vision.md`
