# Kaiteyo — Library

> **Status**: `IMPLEMENTED` (core decks + suite LibraryStore) + `ARCHITECTED` (unified Content-backed Library).
> Companion: `data-model.md` (Content), `core.md`, `stats.md`, `mining.md`, `platforms.md`.

## 1. What it is

The **central organization system** — not a file list. Everything consumed by Kaiteyo that is worth keeping can appear in Library: media, reading, courses, games, collections, series, seasons, books, manga, imported content, bookmarks, active content, history, progress.

Library is **ownership/organization**; Browse is **discovery**; Media/Reading are **consumption**; Platforms are **external sources** (rule 12–15).

## 2. What the user does

- View collections → series → seasons → episodes / books → chapters.
- See active items (in progress), bookmarks, history, progress.
- Create/rename/delete collections; add/remove content; reorder.
- Import content (file, folder, URL, platform record) → becomes `Content` → appears in Library.
- Tap any Library item → open in its consumer (Media player, Reader, Game) at saved progress.

## 3. Data model

```kotlin
data class LibraryItem(
    val contentId: ContentId, // the unified Content
    val kind: ContentKind,
    val title: String,
    val artwork: Artwork?,
    val progress: ContentProgress?,
    val addedAt: Instant,
    val lastConsumedAt: Instant?,
    val bookmarks: List<BookmarkRef>,
    val collectionIds: Set<String>,
    val history: List<HistoryEntry>, // derived from ActivityService
)

data class Collection(
    val id: String,
    val name: String,
    val kind: CollectionKind, // Series, Season, Course, Custom, Smart
    val itemIds: List<ContentId>, // ordered
    val artwork: Artwork?,
    val createdAt: Instant,
)

enum class CollectionKind { Series, Season, Course, Custom, Smart }
```

Smart collections (suite `SmartCollectionEngine`) are rule-based (e.g., "JLPT N5 kanji") with `resolveDecks()` at display time — not stored copies.

## 4. Ownership & flow

| Concern | Owner |
|---------|-------|
| Content rows + progress | ContentService |
| Collections/containers | LibraryService |
| History/events | ActivityService (Library reads) |
| Bookmarks/highlights | MediaService/ReadingService (Library reads) |
| Stats display | StatisticsService |

```
Import file/URL/platform record → ContentService.resolveOrCreate → LibraryService.addToLibrary(contentId, collectionId?)
  → ActivityEvent(Added, contentId)
  → LibraryView shows item; Stats/Home reflect
```

## 5. Persistence / sync / offline

- `library_item` (contentId FK, addedAt, collectionIds JSON), `collection` (id, kind, itemIds JSON).
- Progress is in `ContentService` (not duplicated here).
- Offline: fully available (local Content + Library rows); platform imports queue until online; sync via `SyncService` (lastWriteWins by `updatedAt`).

## 6. UI states

Loading, Empty ("No items — import or browse to add"), Error (orphaned contentId — "Source missing, remove?"), Offline (platform item cached), Retry.

## 7. Evolution

New ContentKind → new Library rendering (icon, progress type); no schema change. New collection rule → new SmartCollection predicate.
