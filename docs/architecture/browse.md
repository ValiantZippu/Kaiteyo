# Kaiteyo — Browse (Discovery)

> **Status**: `IMPLEMENTED` (core browse_hub + knowledge browsers) + `ARCHITECTED` (platform-aware discovery without content duplication).
> Companion: `library.md` (organization), `platforms.md`, `data-model.md`.

## 1. What it is

**Discovery** — search, filters, recommendations, popular/recent, genres/tags/language/source/type/progress — over both **local** and **platform** content **without duplicating Content rows**. Browse surfaces candidates; Library owns what you keep.

## 2. Why separate from Library

| Surface | Purpose | Data |
|---------|---------|------|
| Browse | Find new content | platform search + local catalog + recommendations |
| Library | Organize kept content | `Content` rows + collections + progress |
| Media/Reading | Consume content | session + progress |
| Platforms | Manage external accounts/sync | `PlatformAccount` + `PlatformRecord` |

Merging them produces either a file manager pretending to be discovery or discovery that cannot remember what you own.

## 3. What the user does

- Search (smart search: "common verbs N3" → filtered query via `KnowledgeQueryParser`).
- Filter by genre, tag, language, source, type, progress, JLPT, frequency.
- Browse popular/recent/recommendations/genre shelves.
- Tap result → preview (artwork, metadata, source list) → `Add to Library` → becomes `Content` → opens in consumer.
- Smart search: kana/romaji/English/kanji input; romaji→kana inference where documented limits apply.

## 4. Data model

```kotlin
data class BrowseQuery(
    val text: String,
    val filters: BrowseFilters, // genres, tags, language, source, kind, jlpt, frequency
    val sorts: List<BrowseSort>, // Relevance, Frequency, Jlpt, Grade, Alphabetical
)

data class BrowseResult(
    val kind: BrowseKind, // LocalContent | PlatformRecord | Recommendation
    val title: String,
    val artwork: Artwork?,
    val metadata: Map<String,String>, // genre, year, language, etc.
    val sources: List<String>, // which MediaSourceKind / PlatformAdapter can provide it
    val contentId: ContentId?, // present if already in Library (matched via ExternalIds)
    val platformRecord: PlatformRecord?, // present if from platform search
    val canAddToLibrary: Boolean,
)

interface BrowseService {
    suspend fun search(query: BrowseQuery): List<BrowseResult>
    suspend fun recommendations(profileId: String?): List<BrowseResult>
    suspend fun popular(): List<BrowseResult>
}
```

## 5. How it pulls from platforms without duplicating

```
BrowseService.search("Violet Evergarden")
  → fan-out: local ContentService.search + PlatformService.search per enabled adapter
  → results merged, deduped by ExternalIds union (same Content candidate)
  → BrowseResult.contentId set if Content already exists — "In Library" badge
  → "Add to Library" → ContentService.resolveOrCreate(hint, externalIds) — idempotent, no second row
```

The same title from local file + AniList + MAL surfaces as **one** card with `sources: ["LocalFile","AniList","MAL"]`.

## 6. Persistence / caching / offline

- No browse persistence beyond query history (recent searches in `SettingsService`).
- Result TTL per source (local: no TTL; platform: adapter TTL).
- Offline: local results + cached platform results visible; live platform search unavailable (surfaced as Offline with retry + cached).

## 7. UI states

Loading, Empty ("No results — try broader filters"), Error (provider timeout — retry per source), Offline, Partial (some providers failed — show what succeeded with per-source error).

## 8. Evolution

New filter/sort → add to `BrowseFilters`/`BrowseSort`; UI adds chip. New platform → new adapter, Browse fans out to it automatically if enabled.
