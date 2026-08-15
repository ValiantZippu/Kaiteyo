# 📚 Library — Unified Content Hub

## Purpose

The Library replaces the old split between "Kanji" (Letters) and "Vocabulary"
dashboards on the Home screen. It is the single entry point for all study content:
kanji decks, vocabulary, and word & sentence search.

## User Experience

- The Home tab bar shows a single **Library** tab (see `HomeScreenTab.Library` in
  `core/.../screen/main/screen/home/HomeScreenData.kt`) instead of separate
  Kanji/Vocabulary tabs. Its icon is the kanji 書.
- Tapping the tab opens the **Library hub** (`LibraryScreen`), a card-based overview:
  - **Stats** — total kanji, words, review counts (from `KaiteyoDataCenter`)
  - **Study** — quick actions: study now, review queue, new cards
  - **Library** — links to Kanji Decks and Vocabulary
  - **Review** — flagged items, recent mistakes, upcoming reviews
- Selecting a section drills down to the existing screens:
  - **Kanji Decks** → `LettersDashboardScreen`
  - **Vocabulary** → `VocabDashboardScreen`
  - **Word & Sentence Search** → `SearchScreen`
- Each drill-down screen shows a back arrow to return to the Library hub.

## Technical design

### Files

- `core/.../presentation/screen/main/screen/library/LibraryScreen.kt` — the hub +
  drill-down navigation; internal `LibraryView` enum tracks the sub-screen
  (`Hub`, `KanjiDecks`, `Vocabulary`, `WordSearch`).
- `DrillDownScaffold` — shared helper providing a consistent top bar with a back
  button for sub-screens.
- `HomeScreenData.kt` — `HomeScreenTab.Library` whose content is `LibraryScreen`;
  the old `LettersDashboard`/`VocabDashboard` enum values were removed.
- `NavShell.kt` / `HomeViewModel.kt` — the old default-home-tab preference
  (`Letters`/`Vocab`) is remapped to `Library` so no user settings break.
- Reusable `SectionCard`/`StatRow` composables keep the hub visually consistent with
  the design system.

### Behavior

- Home tab bar contents (all platforms): `GeneralDashboard`, `Library`, `Stats`,
  `Search`, `Settings` (`HomeScreenTab.entries`).
- On desktop, the Home tab is one of the shared-app destinations inside the workspace
  shell; on mobile it is the standard bottom tab bar.
- `LibraryScreen(navigationState = it)` receives the main navigation state and
  navigates to `MainDestination`s for drill-downs.

## Data

`KaiteyoDataCenter` (`core/.../screen/main/features/KaiteyoDataCenter.kt`) supplies
aggregate counts (total kanji, vocab, reviews) loaded once and shared across the
hub — `ensureLoaded()` is called by the hub and by `KanjiBrowser`/`Collections`
destinations.

## Dependencies

- `LettersDashboardScreen`, `VocabDashboardScreen`, `SearchScreen` (existing)
- `KaiteyoDataCenter` for aggregate counts
- `HomeScreenTab` + `NavShell` navigation

## Future improvements

- Per-card-type drill-down (radicals, readings, on/kun, sentences)
- Direct jump to a specific deck from the hub
- Library search/filter across all content types
- Badges for review queues on hub sections
