# Kaiteyo — Unified Content Model

> **Status**: `ARCHITECTED` (target). Companion: `core.md` (service ownership), `database.md` (SQL), `mining.md` (MiningContext).

## 1. What it is

A single **Content** abstraction that represents every learnable/consumable entity in Kaiteyo — anime, TV, movies, episodes, music, podcasts, videos, manga, web manga, novels, light novels, books, ebooks, PDFs, web pages, news, games, visual novels, sentences, vocabulary, kanji, study sessions, external platform records — without producing five unrelated rows for one anime.

## 2. Why it exists

Without it: AniList ID, MAL ID, local file, streaming source, subtitle file, and Library record become five disconnected rows. Progress, artwork, and sync diverge. Search cannot deduplicate. Stats double-count.

## 3. Model

```kotlin
// Identity — one row per logical work, many sources
data class Content(
    val id: ContentId,              // Kaiteyo-internal ULID, primary key
    val kind: ContentKind,
    val title: String,              // canonical display title
    val titles: Titles,             // { canonical, romaji, english, japanese, synonyms[] }
    val artwork: Artwork?,          // see §5
    val metadata: ContentMetadata,  // kind-specific (see below)
    val sourceBindings: List<SourceBinding>, // one Content may have many Sources
    val progress: ContentProgress?,
    val timestamps: ContentTimestamps,
    val library: LibraryMembership?,
)

enum class ContentKind {
    Anime, TvSeries, Movie, Episode, Music, Podcast, Video,
    Manga, WebManga, Novel, LightNovel, Book, Ebook, Pdf,
    WebPage, NewsArticle, Game, VisualNovel,
    Sentence, Vocabulary, Kanji, StudySession,
    PlatformRecord // raw external record before resolution
}

data class SourceBinding(
    val source: ContentSource,      // LocalFile | StreamingAdapter | WebPage | PlatformRecord
    val externalIds: ExternalIds,   // { anilistId?, malId?, imdbId?, anidbId?, ... } — nullable per source
    val url: String?,               // direct URL or file path
    val subtitleBindings: List<SubtitleBinding>, // associated subtitle providers/files
    val syncState: SyncState,       // lastSync, needsSync, readOnly
)

data class ExternalIds(
    val anilist: Int? = null,
    val mal: Int? = null,
    val imdb: String? = null,
    val anidb: Int? = null,
    val kana: String? = null,       // for dictionary dedup
)

data class ContentMetadata(
    // Common
    val language: String = "ja",
    val status: ContentStatus = ContentStatus.Unknown, // Ongoing/Complete/Hiatus/Unknown
    val genres: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val year: Int? = null,
    // Kind-specific sealed payload
    val detail: Detail
)

sealed interface Detail {
    data class Episode(val season: Int?, val episode: Int?, val durationMs: Long?, val airDate: String?) : Detail
    data class Manga(val chapters: Int?, val volumes: Int?, val author: String?) : Detail
    data class Ebook(val chapters: Int?, val pageCount: Int?, val publisher: String?) : Detail
    data class Sentence(val text: String, val reading: String?, val translation: String?) : Detail
    data class Vocab(val kanji: String?, val kana: String, val glossary: String) : Detail
    data class Kanji(val character: String, val jlpt: String?, val grade: Int?) : Detail
    data object Generic : Detail
}

data class ContentProgress(
    val positionMs: Long? = null,   // media
    val page: Int? = null,          // reading
    val percent: Float = 0f,
    val status: ProgressStatus,     // NotStarted / InProgress / Completed / Dropped
    val updatedAt: Instant,
    val source: ProgressSource,     // Local | PlatformSync | Manual
)

data class ContentTimestamps(
    val createdAt: Instant,
    val updatedAt: Instant,
    val lastConsumedAt: Instant?,
    val lastMinedAt: Instant?,
)

data class LibraryMembership(
    val collectionIds: Set<String>,
    val addedAt: Instant,
    val history: List<HistoryEntry>, // read as derived from ActivityService
)
```

### Title / artwork / source example

```kotlin
Content(
  id = "cnt_01H...",
  kind = Anime,
  title = "Violet Evergarden",
  titles = Titles(canonical="Violet Evergarden", japanese="ヴァイオレット・エヴァーガーデン", synonyms=["..."]),
  artwork = Artwork(coverUrl="...", bannerUrl="...", color="#5B7C99"),
  metadata = ContentMetadata(genres=["Drama","Slice of Life"], year=2018, detail=Detail.Generic),
  sourceBindings = listOf(
    SourceBinding(LocalFile, externalIds=ExternalIds(anilist=21827, mal=33352), url="file:///media/violet/..."),
    SourceBinding(StreamingAdapter("supported-web"), externalIds=ExternalIds(anilist=21827), url="https://..."),
  ),
  progress = ContentProgress(positionMs=742_000, percent=0.34, status=InProgress, ...),
)
```

There is exactly **one** `Content` for Violet Evergarden, regardless of how many sources exist.

## 4. Relationships

```
Content 1—* SourceBinding 1—* SubtitleBinding
Content 1—* Episode (kind=Episode, parentContentId)
Content *—* Collection (LibraryService)
Content 1—* ActivityEvent (ActivityService)
Content 1—* Card (DeckService, via mining)
Content 1—* Highlight/Bookmark (ReadingService/MediaService)
Content 1—1 DictionaryMatch[] (on demand, not stored)
```

Cardinality is enforced in SQL via foreign keys + `UNIQUE(contentId, externalSource, externalId)` on bindings.

## 5. Artwork

```kotlin
data class Artwork(
    val coverUrl: String?,
    val bannerUrl: String?,
    val palette: Palette?, // extracted dominant color for theming
    val localPath: String?, // cached file
)
```

Artwork is cached on disk (`~/.kaiteyo/artwork/{contentId}/`), TTL per Platform adapter. Offline: last cached image shown; no network required.

## 6. Ownership & communication

| Concern | Owner | API |
|---------|-------|-----|
| Create/resolve/dedup Content rows | ContentService | `resolveOrCreate(metadata, externalIds)` — idempotent |
| Attach/detach sources | ContentService | `bindSource(contentId, source)` |
| Progress updates | ContentService (writes) ← MediaService/ReadingService/Game (emit) | `updateProgress(contentId, progress)` |
| History | ActivityService (append) → LibraryService/ContentService (read) | `ActivityService.events(contentId)` |
| External sync | PlatformService → ContentService | `PlatformService.sync(account)` pushes bindings |

Progress is **owned** by ContentService but **written** by the active consumer (media player, reader, game). Conflicts: last-write-wins with `updatedAt` tie-break; platform progress is `readOnly` unless user opts in to write-back.

## 7. Persistence

```sql
CREATE TABLE content (
  id TEXT PRIMARY KEY,
  kind TEXT NOT NULL,
  title TEXT NOT NULL,
  titles_json TEXT NOT NULL,
  artwork_json TEXT,
  metadata_json TEXT NOT NULL,
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL,
  last_consumed_at INTEGER
);
CREATE TABLE content_source_binding (
  content_id TEXT NOT NULL REFERENCES content(id) ON DELETE CASCADE,
  source_kind TEXT NOT NULL,
  url TEXT,
  external_ids_json TEXT,
  sync_state TEXT NOT NULL,
  UNIQUE(content_id, source_kind, url)
);
CREATE TABLE content_progress (
  content_id TEXT PRIMARY KEY REFERENCES content(id) ON DELETE CASCADE,
  position_ms INTEGER, page INTEGER, percent REAL NOT NULL,
  status TEXT NOT NULL, updated_at INTEGER NOT NULL, source TEXT NOT NULL
);
```

Migrated via `UserDataDatabase` (new tables, additive). Backups include Content tables.

## 8. APIs / interfaces

```kotlin
interface ContentService {
    suspend fun resolveOrCreate(hint: ContentHint, externalIds: ExternalIds): Content
    suspend fun bindSource(contentId: ContentId, source: ContentSource): Result<SourceBinding>
    suspend fun unbindSource(contentId: ContentId, bindingId: String)
    suspend fun updateProgress(contentId: ContentId, progress: ContentProgress)
    fun observe(contentId: ContentId): Flow<Content?>
    fun search(query: String, kinds: Set<ContentKind>): Flow<List<Content>>
}
```

## 9. Caching / sync / offline

- **Caching**: artwork TTL (7 days default); platform metadata TTL per adapter (1h–24h); search index in-memory.
- **Sync**: Platform adapters push `ExternalIds` + metadata; `resolveOrCreate` dedupes by `(kind, normalizedTitle)` + externalId match. Duplicate candidates surface as merge suggestions, never auto-merge if titles conflict.
- **Offline**: all local Content fully available; platform-bound Content shows cached metadata + progress; sync queue drains when online.

## 10. Failure / edge cases

| Case | Behavior |
|------|----------|
| Two sources claim same externalId but different titles | Create two Content rows + surface merge suggestion |
| ExternalId removed upstream | Binding marked `stale`, Content retained |
| Artwork 404 | Retry once, then keep placeholder |
| Progress conflict (local vs platform) | Local wins if newer; setting controls write-back |
| Malformed metadata JSON | Row skipped, logged, surfaced in Debug panel |

## 11. Evolution

New `ContentKind` adds a `Detail` subtype and a Library/Stats rendering — no schema break. New platform adds an adapter implementing `PlatformAdapter { fetchMetadata(externalId): ContentMetadata }`. No duplicated record types.
