# Kaiteyo — Visual QA Checklist (KT-TEST-013)

> Purpose: a page-by-page, form-factor-by-theme sweep so no screen ships looking
> like a placeholder, with evidence recorded per cell. This is the visual gate for
> the overhaul (spec §100–§101). Statuses follow the planning taxonomy.

## How to run a sweep

1. Build the app for the host platform (`:desktopApp:run` for desktop; Android/iOS
   builds for those form factors).
2. For each **page** below, for each **form factor**, for each **theme**, capture a
   screenshot and record: layout sanity, spacing (4dp grid), typography (no clipped
   glyphs, JP line-height), theme coherence (no hardcoded-color stragglers), empty /
   loading / error states, keyboard + mouse + touch reachability, and "no
   placeholder-looking content".
3. Record the result in the table (`✅` / `⚠️` with note / `❌` with issue link).
   Screenshots live under `docs/planning/qa/` (one folder per sweep date).

## Form factors × themes matrix

| Form factor | Light | Dark | OLED | Sepia | Cream | Paper | Midnight |
|---|---|---|---|---|---|---|---|
| Desktop ≥1440px | | | | | | | |
| Desktop ~1024px | | | | | | | |
| Tablet portrait | | | | | | | |
| Tablet landscape | | | | | | | |
| Phone portrait | | | | | | | |
| Phone landscape | | | | | | | |

## Page checklist

### Shell & navigation
- [ ] Window shell: topbar, integrated controls, no resize flashes, no negative padding at any size (KT-NAV-007, KT-UI-011)
- [ ] Floating navigation: drag, snap, right-click menu, no artifacts (KT-NAV-004)
- [ ] Sidebar: width sane, hover/focus states, collapse, phone top/bottom (KT-NAV-005)
- [ ] Launchpad: centered, no giant empty areas (KT-NAV-006)
- [ ] Debug overlay / page-name pill: subtle, theme-aware, dev-only (KT-DEBUG-004)

### Home (GeneralDashboard)
- [ ] Continue / study target hierarchy reads as "what should I do now?" (KT-UI-003)
- [ ] Recent decks/activity, quick actions, collections, Discover — all wired
- [ ] Heatmap alignment + hover popup placement (KT-UI-006)

### Library
- [ ] Unified search results: keyboard nav hint, selection highlight, empty state (KT-UI-004)
- [ ] Mode chips: All / Decks / Kanji / Vocabulary / Due / Favorites / Exams / Kanjiverse
- [ ] Deck grid + progress rings, collections section, Manage menu

### Browse / dictionary explorer
- [ ] Landing: browse tiles run real filter-only queries (KT-UI-005)
- [ ] Results: filter/sort chips, keyboard nav (↑/↓/Enter), grouping (KT-SEARCH-003/004, KT-UI-010)
- [ ] Kanji detail, word detail: cards flow in the saved layout, no overflow (KT-CARD-004)

### Dictionary entries
- [ ] Kanji entry: every card renders real data; customize mode (show/hide/drag/up-down); drag is artifact-free (KT-CARD-002)
- [ ] Word entry: profile-adapted glossary/sentences, romaji toggle, word-card layout (KT-LEVEL-003/004)
- [ ] Sentence entry: token chips, grammar highlights, provenance row (KT-SENT-001/003)

### Graph
- [ ] Pan/zoom canvas, node select/expand, relationship colors + legend (KT-GRAPH-002)
- [ ] Node inspector: info, neighbors (clickable → focus), expand leaf state

### Study / practice / review
- [ ] Review screen: SRS answer buttons, timer/card-count, no ghost preview (KT-UI-002)
- [ ] Writing/reading practice: stroke evaluation, furigana/romaji per profile

### Stats / exams / media
- [ ] Stats heatmap year navigation; drill-down rows
- [ ] Exams workspace; media center home/browse/detail/playlists

### Settings Center
- [ ] All six categories render; every control reflects live state and persists (KT-UI-007)
- [ ] Settings instant search: matched settings render fully functional

### Cross-cutting
- [ ] Empty/loading/error states on every page: specific copy, retry where sensible, offline ≠ blank (KT-UI-008)
- [ ] Keyboard-only pass: every action reachable, focus visible (KT-UI-010)
- [ ] Reduced motion: animations collapse, no flashing (KT-UI-012)
- [ ] No user-facing Kanji Dojo branding remnants (KT-UI-013)

## Recording format

For each sweep date, add a section below and fill the tables.

### Sweep 2026-08-17 (baseline)
- Pages covered: shell/nav, home, library, explorer (browse + results + details), kanji/word/sentence entries, graph, settings center — **code-level audit only; screenshot sweep pending** (runtime verification blocked by the no-compile rule).
- Known ⚠️: kanji/word/sentence/grammar card systems each have their own layout store; verify the edit toolbars expose all nine presets consistently on every page.
