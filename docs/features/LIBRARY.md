# 📚 Library — Unified Content Hub

## Purpose

The Library replaces the old split between "Kanji" (Letters) and "Vocabulary"
dashboards on the Home screen. It is the single entry point for all study
content: kanji decks, vocabulary, and word & sentence search.

## User Experience

- The Home tab bar shows a single **Library** tab instead of separate
  Kanji/Vocabulary tabs.
- Tapping the Library tab opens the **Library hub**, a card-based overview:
  - **Stats** — total kanji, words, review counts.
  - **Study** — quick actions: study now, review queue, new cards.
  - **Library** — links to Kanji Decks and Vocabulary.
  - **Review** — flagged items, recent mistakes, upcoming reviews.
- Selecting a section drills down to the existing screens:
  - **Kanji Decks** → `LettersDashboardScreen`
  - **Vocabulary** → `VocabDashboardScreen`
  - **Word & Sentence Search** → `SearchScreen`
- Each drill-down screen shows a back arrow to return to the Library hub.

## Technical Design

- `LibraryScreen.kt` (`presentation/.../screen/library/`) hosts the hub and the
  drill-down navigation. An internal `LibraryView` enum tracks the current
  sub-screen (`Hub`, `KanjiDecks`, `Vocabulary`, `WordSearch`).
- A `DrillDownScaffold` helper provides a consistent top bar with a back button
  for sub-screens.
- `HomeScreenData.kt` defines `HomeScreenTab.Library` whose content is
  `LibraryScreen`; the old `LettersDashboard`/`VocabDashboard` enum values were
  removed.
- Existing default-home-tab preference (`Letters`/`Vocab`) is remapped to
  `Library` in `NavShell.kt` and `HomeViewModel.kt`, so no user settings break.
- Reusable `SectionCard`/`StatRow` composables keep the hub visually consistent
  with the design system.

## Dependencies

- `LettersDashboardScreen`, `VocabDashboardScreen`, `SearchScreen` (existing)
- `KaiteyoDataCenter` for aggregate counts
- `HomeScreenTab` + `NavShell` navigation

## Future Improvements

- Per-card-type drill-down (radicals, readings, on/kun, sentences)
- Direct jump to a specific deck from the hub
- Library search / filter across all content types
- Badges for review queues on hub sections
