# Kaiteyo — Home

> **Status**: `IMPLEMENTED` (General/Library/Statistics/Search/Settings dashboards).
> Companion: `core.md`, `stats.md`, `library.md`, `events.md`.

## 1. What it is

A **dashboard** derived from Core services — not a second Stats page. Every tile is a live query over Library/Activity/Statistics/Deck state.

## 2. Components (derived, not duplicated)

| Tile | Source |
|------|--------|
| Continue (resume last) | ContentService lastConsumedAt |
| Recent (consumed/mined) | ActivityService recent events |
| Study progress (due/new counts) | DeckService |
| Media progress | ContentService + MediaService |
| Reading progress | ContentService + ReadingService |
| Daily activity (today) | StatisticsService.daily(today) |
| Streak / goals | StatisticsService.streak() |
| Quick actions (Search, Library, Browse, Review) | navigation intents |

Stats-specific visualizations (heatmap year nav, retention curves, velocity) live only in Stats — Home shows glanceable summaries with "→ Full stats" deep links.

## 3. Ownership

Home owns no state. Data owners: ActivityService, StatisticsService, ContentService, DeckService, LibraryService. Home is a consumer.

## 4. UI states

Loading (first paint), Empty (new user — onboarding CTA), Offline (show cached today), Error (per-tile retry, never blank page).

## 5. Evolution

New Core metric → new Home tile (consumer only). No Home-owned tables.
