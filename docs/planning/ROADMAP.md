# Kaiteyo Implementation Roadmap

## Current Status (Phase 1–21 Complete)

### ✅ Core Architecture
- Navigation system (Sidebar/Floating, two-mode, state persistence)
- Theme system (4 base modes, 7 accents, full token system)
- Domain models (Kanji, Radical, Component, Word, Sentence, Grammar)
- Knowledge repository (facade over AppDataRepository)
- Search engine (grouped results, category filters, debounced, intent detection)
- Search input analysis (kanji / kana / romaji / English / structured filters)
- Knowledge graph (pan/zoom, progressive expansion, inspector, pathfinding)
- Card system (Kanji + Word + Sentence + Grammar + Collection modular layouts)
- Card registry (unified API over all five card types)
- Preset adapter (learner profile → recommended card presets)
- Card settings screen (show/hide/reorder cards, presets)
- Study state machine (FSRS integration, 7 states)
- Level profiles (9 profiles + Custom)
- Sentence analysis (tokenization, annotation, difficulty scoring, interactive tokens)
- Mnemonic system (authoritative / AI / user / community, formulas, ratings)
- Kanji decomposition engine (components, radicals, layouts, strokes)
- Pitch accent data (mora patterns, accent types)
- Library catalog (courses, collections, grade/JLPT sets)
- Browse hub (JLPT/grade distribution, frequency, radicals, grammar)
- Media centre foundation (desktop workspace overview)
- Media models (documents, bookmarks, OCR, media references)
- Performance utilities (Debouncer, LazyPager, caches, graph limits)
- Accessibility utilities (focus, keyboard, touch, contrast)
- Architecture documentation

### ✅ World System (Kaiteyo World — Kamakura vertical slice)
- World runtime (isolated lifecycle, crash isolation, game loop)
- Geographic coordinate system (world space + WGS84 projection)
- Chunk system (256 m chunks, streaming, load radius, hard cap)
- Terrain generation (deterministic fBm + Kamakura basin + coastline)
- Water system (animated waves, beach detection, swimming)
- Player controller (walk/run/swim, terrain follow, collision)
- Camera (third-person / first-person, smooth switching)
- Buildings (procedural town grid + landmark sites)
- Vehicles (waypoint routes, limit)
- Trains (Enoden 300形, station dwell, direction reversal)
- NPCs (schedule-based movement, dialogue, interactions)
- Time & weather (day cycle, weighted weather rolls)
- Save/load (versioned JSON, corrupt-save safe)
- World map (regions, locations, discovery)
- WorldController (app-facing handle) + KamakuraWorld factory
- World screen (contract + viewmodel + module + UI + navigation)
- World tests (geo, chunks, player, runtime, systems, controller)
- WORLD_SYSTEM.md documentation

### ✅ UI Screens
- Kanji Entry (modular cards, customization, presets)
- Word Entry (profile-aware glossary, kanji connections, sentences)
- Sentence Entry (interactive tokens, grammar highlights, difficulty)
- Knowledge Graph (canvas, inspector, type filtering)
- Radical Explorer (grid, stroke/JLPT/grade filtering)
- Component Explorer (component → kanji drill-down)
- Browse Hub (JLPT, grade, frequency, collections, grammar)
- Library (deck management, kanji/vocab browsing, unified search)
- Home Dashboard (study targets, decks, streak, heatmap)
- Settings (appearance, navigation, study, data)
- Statistics (heatmap, streaks, review counts)
- Universal Search (grouped results, category chips, keyboard nav)

## Remaining Work

### Phase 18: OCR / Voice / Handwriting Search
**Status**: Roadmap item (requires ML libraries)

- [ ] Integrate Tesseract or ML Kit for OCR
- [ ] Implement camera/image → Japanese text → dictionary
- [ ] Add handwriting recognition canvas
- [ ] Add voice input → text → search
- [ ] Connect OCR results to dictionary popup
- [ ] Save OCR history for review

**Dependencies**: Platform-specific ML libraries, camera permissions

### Phase 19: Performance Optimization
**Status**: Foundation complete, needs profiling

- [ ] Profile search latency across 13k+ kanji
- [ ] Profile graph expansion with 100+ nodes
- [ ] Profile library scroll with 1000+ items
- [ ] Add virtualized list for kanji browser
- [ ] Optimize sentence tokenizer for long texts
- [ ] Add database indexes for common queries
- [ ] Cache derived graph relationships
- [ ] Measure and optimize startup time
- [ ] Add lazy loading for image thumbnails
- [ ] Profile and optimize memory usage

### Phase 20: Accessibility
**Status**: Foundation complete, needs testing

- [ ] Test keyboard navigation across all screens
- [ ] Verify focus indicators on all interactive elements
- [ ] Test with screen readers (VoiceOver, TalkBack)
- [ ] Verify touch targets (48dp minimum)
- [ ] Test high contrast mode on all screens
- [ ] Test reduced motion mode
- [ ] Add semantic labels to all icons
- [ ] Verify color contrast ratios (WCAG AA)
- [ ] Test with large font sizes
- [ ] Add keyboard shortcuts documentation

### Phase 21: Cross-Platform Polish
**Status**: Needs testing across platforms

- [ ] Test desktop window resize behavior
- [ ] Test sidebar ↔ floating mode switching
- [ ] Test theme switching performance
- [ ] Test on Windows, macOS, Linux
- [ ] Test Android (various screen sizes)
- [ ] Test iOS (if applicable)
- [ ] Verify consistent animations across platforms
- [ ] Test deep link handling
- [ ] Verify offline dictionary functionality
- [ ] Test data import/export on each platform

### Phase 22: Media Centre
**Status**: Foundation rebuilt, needs player implementation

- [ ] Implement video player abstraction
- [ ] Add subtitle loading (SRT, ASS, VTT)
- [ ] Implement dictionary popup on subtitle hover
- [ ] Add sentence mining from subtitles
- [ ] Implement media library management
- [ ] Add watch history tracking
- [ ] Implement bookmarks/timestamps
- [ ] Add playback controls (play/pause/seek/speed)
- [ ] Implement audio player for podcasts/music
- [ ] Add media search and filtering

### Phase 23: Course System
**Status**: Catalog exists, needs UI integration

- [ ] Implement course detail screen
- [ ] Add lesson-by-lesson progression
- [ ] Connect course items to dictionary
- [ ] Add progress tracking per course
- [ ] Implement course recommendations
- [ ] Add custom course creation
- [ ] Import/export courses
- [ ] Add course sharing (future)

### Phase 24: Card Customization UI
**Status**: ✅ CardSettingsScreen created (show/hide/reorder/presets)

- [x] Create card editor screen
- [x] Add drag-to-reorder
- [x] Add visibility toggles
- [x] Add preset management
- [ ] Add custom preset creation
- [ ] Add export/import presets
- [ ] Preview changes before applying
- [x] Add per-profile card defaults (PresetAdapter)
- [ ] Wire CardSettingsScreen into navigation + Settings

### Phase 25: Advanced Search
**Status**: Core search complete, needs enhancements

- [ ] Add wildcard search support
- [ ] Add fuzzy matching
- [ ] Add search history persistence
- [ ] Add saved searches
- [ ] Add search suggestions/autocomplete
- [ ] Add search by radical composition
- [ ] Add search by stroke count range
- [ ] Add search by part of speech
- [ ] Add search by frequency range

### Phase 26: Statistics Enhancement
**Status**: Basic stats complete, needs depth

- [ ] Add kanji progress heatmap
- [ ] Add vocabulary growth chart
- [ ] Add reading progress visualization
- [ ] Add accuracy trends
- [ ] Add review forecast
- [ ] Add study time tracking
- [ ] Add goal setting and tracking
- [ ] Add comparison (this week vs last week)
- [ ] Add export statistics

### Phase 27: Import/Export
**Status**: Basic import exists, needs expansion

- [ ] Add CSV export for kanji lists
- [ ] Add Anki-compatible export
- [ ] Add JSON export/import
- [ ] Add bulk tag/flag operations
- [ ] Add collection sharing
- [ ] Add backup verification
- [ ] Add incremental backup
- [ ] Add cloud sync (future)

### Phase 28: Testing
**Status**: Unit tests exist, needs expansion

- [ ] Add integration tests for search
- [ ] Add UI tests for navigation
- [ ] Add tests for card customization
- [ ] Add tests for theme persistence
- [ ] Add tests for study state transitions
- [ ] Add tests for sentence analysis
- [ ] Add tests for knowledge graph
- [ ] Add performance regression tests
- [ ] Add accessibility tests

### Phase 29: Documentation
**Status**: Core docs complete, needs expansion

- [ ] Add API documentation for knowledge repository
- [ ] Add contributor guide
- [ ] Add architecture decision records (ADRs)
- [ ] Add data source documentation
- [ ] Add testing guide
- [ ] Add release process documentation
- [ ] Add troubleshooting guide
- [ ] Add design system documentation

### Phase 30: Kaiteyo World
**Status**: ✅ Foundation complete — Kamakura vertical slice runtime, screen, tests

- [x] World runtime isolation + lifecycle
- [x] Geographic coordinate system + projection
- [x] Chunk system + streaming
- [x] Terrain generation
- [x] Water + beach
- [x] Player controller + camera (3rd/1st)
- [x] Buildings
- [x] Vehicles + train framework (Enoden)
- [x] NPC framework + Kamakura cast
- [x] Weather + time of day
- [x] Save/load + world map
- [x] WorldController + KamakuraWorld factory
- [x] World screen + navigation + DI
- [x] Tests (geo, chunk, player, runtime, systems, controller)
- [x] WORLD_SYSTEM.md documentation
- [ ] 3D renderer adapter (over the data model)
- [ ] Lighting / shadows / LOD / culling
- [ ] Interior system
- [ ] Quest / activity system
- [ ] Audio
- [ ] Controller input
- [ ] More regions (Enoshima, Yokohama, …)

## Priority Order

1. **Phase 22**: Media Centre (major feature)
2. **Phase 23**: Course System (learning path)
3. **Phase 24**: Card Customization UI wiring (user-requested)
4. **Phase 25**: Advanced Search (user-requested)
5. **Phase 26**: Statistics (engagement)
6. **Phase 27**: Import/Export (data portability)
7. **Phase 28**: Testing (quality assurance)
8. **Phase 19**: Performance (critical for usability)
9. **Phase 20**: Accessibility (required for inclusive design)
10. **Phase 21**: Cross-platform (required for release)
11. **Phase 30**: World 3D renderer + interiors (foundation complete)
12. **Phase 18**: OCR/Voice (future)
