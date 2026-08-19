# Kaiteyo — Master Implementation TODO (150 items)

> The **operational implementation list** for the full product overhaul. Every
> phase of the directives is broken into actionable rows here; each row is
> either already done (✅, cross-referenced), in progress (🚧), or open (📋).
> This is the checklist that keeps the 200k-character directive honest — no
> phase is "done" because one screen was touched.
>
> Status legend: ✅ DONE · 🚧 IN PROGRESS · 🔬 TARGET (architected) ·
> 📋 PLANNED · 🔍 RESEARCH · ⛔ BLOCKED.
>
> Where a row duplicates an existing backlog row, the KT id is cross-referenced
> (see [`OVERHAUL_BACKLOG.md`](OVERHAUL_BACKLOG.md) and [`MASTER_TODO.md`](MASTER_TODO.md)).

## A. Audit & foundation (1–10)

- [x] 1. Repository audit — modules, screens, navigation, themes, components, database (Phase 1; KT-UI-001)
- [x] 2. Domain model audit — Kanji/Radical/Component/Word/Sentence/Graph entities exist (`KnowledgeModels.kt`)
- [x] 3. Navigation audit — one `NavShell` drives Floating + Sidebar (KT-NAV-001)
- [x] 4. Theme audit — Light/Dark/OLED/Sepia + token system (`Theme.kt`, `Color.kt`)
- [x] 5. Page identity system — `PageIdentity`/`PageRegistry`/debug overlay (KT-DEBUG-001)
- [x] 6. Ghost-control sweep #1 — dead shadows removed (KT-UI-002)
- [x] 7. Branding sweep — zero user-facing Kanji Dojo remnants (KT-UI-013)
- [x] 8. Design tokens — spacing/radius/typography scales exist (`Dimens`, `Ds*` system)
- [x] 9. Two-database architecture — AppData (read-only) + UserData (mutable) SQLDelight
- [x] 10. Koin DI modules — `CoreModule` + per-feature modules registered in `AppModule.kt`

## B. Navigation shell (11–20)

- [x] 11. NavigationController consolidation — one settings model, two presentations (KT-NAV-001)
- [x] 12. Sidebar presentation — explicit width, hover/focus states, collapse/expand (KT-NAV-005)
- [x] 13. Floating bubble — drag/snap/hold disambiguation, right-click menu (KT-NAV-004)
- [x] 14. Launchpad — centered glass panel, internal scroll on short windows (KT-NAV-006)
- [x] 15. Nav settings — every control wired to real persisted state (KT-NAV-003)
- [x] 16. Global shortcuts — Ctrl+K palette, Ctrl+Shift+F search, Ctrl+B mode (KT-SEARCH-008)
- [ ] 17. Mode-switch stress regression — floating↔sidebar ×N across screens (KT-NAV-002) 📋
- [ ] 18. Per-OS runtime sweep — Windows/macOS/Linux nav feel (KT-NAV-004/005) 📋
- [ ] 19. Topbar/window chrome polish — themed chrome, no resize flashes (KT-NAV-007) 🚧
- [ ] 20. Navigation stress tests — §99 regression scenario automated (KT-TEST-006) 📋

## C. Themes & design system (21–30)

- [x] 21. Sepia theme — full palette + EN/JA strings + reading theme (KT-THEME-002 partial)
- [x] 22. Token-based surfaces — `surfaceColors()` + `LocalSurfaceColors`
- [x] 23. Study-state color consolidation — one shared map (KT-THEME-003 partial)
- [x] 24. Reduced-motion / scale / high-contrast accessibility toggles
- [x] 25. Japanese typography basics — large kanji breathing room on entry pages
- [ ] 26. Design-token consolidation — one owner for spacing/typography/radius (KT-THEME-001) 📋
- [ ] 27. Theme completeness sweep — Sepia across every surface (KT-THEME-002) 📋
- [ ] 28. Hardcoded-color sweep — zero reachable `Color(0x` outside theme defs (KT-THEME-003) 📋
- [ ] 29. JP typography system — font stacks, line heights, kana/romaji differentiation (KT-THEME-004) 📋
- [ ] 30. Theme matrix tests — every theme renders + persists (KT-TEST-007) 📋

## D. Debug tooling (31–35)

- [x] 31. Page identity declarations — `PageIdentity` per screen
- [x] 32. Debug overlay — bottom-corner PAGE pill, copy debug info
- [x] 33. Page name indicator — top-right screen name (Navigation settings)
- [x] 34. Debug panel — FPS/viewport readouts (KT-DEBUG-002 code-complete)
- [ ] 35. Debug settings + gating — force theme/nav mode, release gating (KT-DEBUG-003/004) 🚧

## E. Home & dashboard (36–45)

- [x] 36. Home command center — Quick search, Recent searches/entries, Discover (spec §31)
- [x] 37. Study targets + daily limits on General dashboard
- [x] 38. Writing practice card on dashboard
- [x] 39. Knowledge snapshot + activity heatmap
- [x] 40. Collections cards + quick actions
- [x] 41. Resume/Continue inside Home (not a nav destination)
- [x] 42. Due-count badge on Library
- [ ] 43. Recommended section — explainable, profile-aware (KT-LEVEL-003) 📋
- [ ] 44. Home hierarchy polish — never 40 cards; continue/due/recent ordering (KT-UI-003) 🚧
- [ ] 45. Empty/offline dashboard states — "Offline", not blank (KT-UI-008) 📋

## F. Library (46–55)

- [x] 46. Library-as-hub — decks, collections, unified search with keyboard nav
- [x] 47. Collection containers — JLPT/grade decks in Library
- [x] 48. Kana content system + study-from-deck actions
- [x] 49. Exams inside Library (not top-level nav)
- [x] 50. Archived deck restore UI on both dashboards
- [x] 51. Favorites + due filters
- [ ] 52. Sentences/grammar as first-class Library content (KT-LIB-002/003) 🚧
- [ ] 53. Courses/lessons model — material collection hierarchy (KT-LIB-004) 📋
- [ ] 54. Saved/Imported sections — user material (KT-UI-004) 📋
- [ ] 55. Library content gaps sweep — every entry links to the dictionary (KT-UI-004) 📋

## G. Browse & explorer (56–65)

- [x] 56. Explorer landing — JLPT/grade/frequency/strokes/POS browse tiles (KT-UI-005 partial)
- [x] 57. Universal search — grouped results, counts, debounce, keyboard nav (KT-SEARCH-001)
- [x] 58. Query interpretation — "common verbs N3" → real filters (KT-SEARCH-002)
- [x] 59. Text normalization — width/script/romaji/wildcards (KT-SEARCH-005)
- [x] 60. Word/sentence sorting + filter-only browse (KT-SEARCH-003/004)
- [x] 61. Search input modes — clipboard + image OCR (KT-SEARCH-007)
- [x] 62. Radical explorer — grid, stroke/JLPT/grade filters, kanji results (KT-RAD-001)
- [x] 63. Component explorer — radical-derived decomposition (KT-RAD-003)
- [ ] 64. Browse redesign — radicals/grammar/collections tiles + node-family browse (KT-UI-005) 🚧
- [ ] 65. Voice/handwriting input — gated RESEARCH until real engines exist (KT-SEARCH-007) 🔍

## H. Dictionary domain (66–80)

- [x] 66. Kanji entity — character/readings/meanings/tags/strokes/frequency (spec §6)
- [x] 67. Radical entity — first-class, not a tag (spec §7)
- [x] 68. Component model — radical-derived decomposition (spec §8 partial)
- [x] 69. Word entity — readings, glossary, POS, furigana (spec §25)
- [x] 70. Sentence entity — text/translation/provenance/difficulty (spec §26–§27)
- [x] 71. Grammar entity — `GrammarPattern` + `GrammarCatalog` (KT-SENT-004)
- [x] 72. Frequency system — bands + rank labels + source (KT-DATA-001)
- [x] 73. Keyword system — primary/alternates/learner/literal/component (KT-DATA-002)
- [x] 74. Dataset provenance model — version/license/source/counts (KT-DATA-003)
- [x] 75. Kanji card registry — 15 card types + presets (KT-CARD-001/003)
- [x] 76. Word card registry — 10 card types + presets + Customize dialog (KT-CARD-001/003)
- [x] 77. Card drag-reorder — long-press drag, stride-corrected (KT-CARD-002)
- [x] 78. Sentence/grammar card systems — presets for both
- [x] 79. Kanji entry page — modular cards, real data on every card
- [x] 80. Word entry page — layout-driven, profile-adapted, media card

## I. Decomposition & structure (81–90)

- [x] 81. `KanjiDecompositionEngine` — structural decomposition of kanji
- [x] 82. Kanji graph relationships — radical/component/word/sentence edges (KT-GRAPH-001)
- [x] 83. Graph model — typed nodes/edges, progressive expansion (KT-GRAPH-001)
- [x] 84. Graph canvas — pan/zoom/select/expand, edge colors + legend (KT-GRAPH-002)
- [x] 85. Branch collapse — cluster nodes with +N badge (KT-GRAPH-002)
- [x] 86. Graph navigation trail — breadcrumbs + back/forward (KT-GRAPH-004)
- [x] 87. Decomposition-driven formulas — `FormulaBuilder` (spec §11)
- [x] 88. Mnemonic registry — honest, non-authoritative (spec §12)
- [ ] 89. Component model beyond radical — semantic/phonetic/structural roles (KT-RAD-002) 🔬
- [ ] 90. Decomposition dataset coverage — more kanji decomposed (KT-DATA-004) 🔬

## J. Level adaptation (91–100)

- [x] 91. `LearnerProfile` model — 10 presets + Custom (KT-LEVEL-001)
- [x] 92. Profile store — persisted, sanitized, switchable (KT-LEVEL-002)
- [x] 93. LevelAdapter — glossary depth, sentence difficulty, furigana/romaji flags
- [x] 94. Word-page adaptation — senses/sentences/visibility by profile (KT-LEVEL-003)
- [x] 95. Kanji-entry adaptation — example sentences by difficulty bound
- [x] 96. Browse adaptation — "For your level (N5)" chip + BrowseHub section
- [x] 97. Romaji override — per-user toggle, never destroys data (KT-LEVEL-004)
- [x] 98. Custom profile editor — visibility + depth/difficulty/graph/card pickers
- [ ] 99. Sentence-entry adaptation — profile-bound examples on sentence pages (KT-LEVEL-003) 🚧
- [x] 100. Explainable recommendations — "why this kanji next" — `StudyRecommendationEngine.recommend` returns a human reason per candidate (spec §176–§177)

## K. Study integration (101–110)

- [x] 101. StudyState machine — New/Learning/Known/Due/Mastered/Relearning/Suspended
- [x] 102. StudyStatusProvider — real FSRS-backed state per item
- [x] 103. Kanji study card — real per-practice state (spec §15)
- [x] 104. Word study card — real flashcard state
- [x] 105. Study gate — SRS scheduling untouched (never-change rule)
- [x] 106. Review/study session integration — cards from dictionary work in review
- [x] 107. Mining engine — dictionary → card pool (desktop suite)
- [x] 108. Study-based search sorts — `SearchSort.RecentlyStudied`/`RecentlyAdded` in `KnowledgeSearchEngine` (KT-SEARCH-004)
- [x] 109. Study state filters in search — `SearchFilters.studyState` evaluated against the real SRS cards via `KnowledgeSearchQuery.studyOverlay` (KT-SEARCH-003)
- [x] 110. SRS ↔ knowledge bridge — study state drives `StudyRecommendationEngine` ranking + study-aware search sorts (KT-LEVEL-003)

## L. Media & knowledge connections (111–120)

- [x] 111. MediaReference model + store — Japanese bookmarks (spec §28)
- [x] 112. Media card on word pages — "Found in your media"
- [x] 113. Media graph nodes — `APPEARS_IN` edges from media references
- [x] 114. Media Centre crash root-cause + tick safety test (KT-MEDIA-001)
- [x] 115. Media lifecycle tests — shutdown/idempotency/tick-after-shutdown (KT-TEST-012)
- [x] 116. OCR search input — `SearchOcrProvider` seam + desktop Tesseract
- [ ] 117. Subtitle search — subtitle text searchable from search (KT-MEDIA-005) 📋
- [ ] 118. Media node family — Series/Episode/Scene/SubtitleLine nodes (spec §130) 🔬
- [ ] 119. Mining into the graph — `mined_from` edges + card provenance (spec §149) 🔬
- [ ] 120. Media player lifecycle under rapid navigation — detail/player surfaces (KT-TEST-012) 📋

## M. Search & filters (121–130)

- [x] 121. Grouped search — KANJI/WORDS/SENTENCES/GRAMMAR with counts
- [x] 122. Kanji search index — in-memory, 2k+ jōyō, filters + sorts
- [x] 123. Search normalization — width/script/romaji (KT-SEARCH-005)
- [x] 124. Wildcard search — `*`/`?` opt-in (KT-SEARCH-005)
- [x] 125. Filter chips + filter-only browse (KT-SEARCH-003)
- [x] 126. Sort chips — relevance/frequency/strokes/JLPT/grade/A–Z/reading/difficulty
- [x] 127. Search recording — recent searches feed Home command center
- [ ] 128. Search performance — cached/indexed queries, instant feel (KT-SEARCH-006) 📋
- [ ] 129. DB-side word POS/JLPT filtering — new queries (KT-SEARCH-003) 📋
- [ ] 130. MEDIA/LISTS result sections — future extension (KT-SEARCH-001) 📋

## N. Stats & study history (131–140)

- [x] 131. Activity heatmap — GitHub/Anki-style with year navigation
- [x] 132. Heatmap hover details — reduced-motion safe
- [x] 133. Study statistics — totals, per-deck, per-type
- [x] 134. Study history — session records
- [x] 135. Daily targets on dashboard
- [ ] 136. Heatmap hover popup placement — small, offset, never under cursor (KT-UI-006) 📋
- [ ] 137. Heatmap keyboard accessibility (KT-UI-006) 📋
- [ ] 138. Stats drill-down by date (spec §131) 📋
- [ ] 139. Stats over node events — one stream (spec §213) 🔬
- [x] 140. Stats export — CSV/JSON of study history — `StatisticsExporter` (build/toJson/toCsv) + `StatisticsExporterTest`

## O. Node layer & knowledge graph data (141–150)

- [x] 141. Knowledge graph data layer — typed edges over real queries (KT-GRAPH-001)
- [x] 142. Progressive expansion — never thousands of nodes at once
- [x] 143. Relationship-type filters + edge legend
- [ ] 144. Node contract + registries — NODE_TYPE_REGISTRY + RELATIONSHIP_REGISTRY as code (ADR-0013) 🔬
- [ ] 145. Node-layer storage decision — new tables vs read-model (ADR-0013) 🔬
- [ ] 146. Node experiences — "where have I seen this?" (needs media + graph events) 🔬
- [ ] 147. User Knowledge model — KNOWLEDGE_STATE_MODEL (spec §85) 🔬
- [ ] 148. Knowledge scoring — derived dials, no fabricated precision 🔬
- [x] 149. Graph auto-clustering — `KnowledgeGraph.autoClustered` (greedy dense-expansion collapse, root/pinned protected) + `KnowledgeGraphAutoClusterTest` (KT-GRAPH-002)
- [ ] 150. Graph relationship tests — edge resolution, acyclic-safe (KT-TEST-010) ✅ → completed

---

**Progress**: 108 / 150 checked as of this revision. Remaining open items are the
runtime-dependent sweeps, gated research (AI, voice/handwriting, 3D world), and the
documented TARGET subsystems (node layer, courses/lessons, import pipeline consolidation).
