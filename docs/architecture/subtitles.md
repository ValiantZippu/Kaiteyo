# Kaiteyo — Subtitle System

> **Status**: `PARTIALLY_IMPLEMENTED` (parsers + engine exist) + `ARCHITECTED` (provider/browser interface).
> Companion: `media.md`, `core.md`, `mining.md`.

## 1. What it is

A first-class subsystem for Japanese subtitle **parsing, normalization, timing, sync, track selection, search, caching, and association** — separated from the Media UI and from any single provider.

## 2. Supported formats

SRT, ASS/SSA, WebVTT, embedded (mkv/mp4), external files — plus a future-format seam (`SubtitleParser` interface). Current parsers: `SubtitleParser.kt` + `SubtitleNormalizer.kt` + `SubtitleSearchIndex.kt` (suite).

## 3. Parsing → normalization → timing

```
File/embedded bytes
  → Parser (format-detected by extension + magic)
  → Raw cues {startMs, endMs, text, style}
  → Normalizer (strip {\an1}/{\\pos} ASS tags, HTML, trim, NFC, line-break handling)
  → SubtitleTrack { id, language, source, cues[] }
  → SearchIndex (trigram + prefix for in-subtitle search)
```

- Timing: `Long` ms, `ClosedRange` for cues, overlap resolved by `startMs` ordering.
- Offset: per-track `syncOffsetMs` (user-adjustable ±10s, step 50ms, persisted per `contentId+trackId`).
- Track selection: language-first (ja), then priority; user override persisted.
- Episode/release/language matching: `SubtitleQuery { title, episode, language }` normalized (NFKC + lower + trim) before provider search.

## 4. Data model

```kotlin
data class SubtitleTrack(val id: String, val language: String, val source: SubtitleSource, val cues: List<SubtitleCue>, val syncOffsetMs: Long = 0)
data class SubtitleCue(val index: Int, val startMs: Long, val endMs: Long, val text: String, val reading: String? = null)
enum class SubtitleSource { Embedded, ExternalFile, ProviderDownload }

data class SubtitleQuery(val title: String, val episode: Int?, val language: String = "ja")
data class SubtitleMatch(val providerId: String, val title: String, val episode: Int?, val language: String, val format: String, val url: String, val metadata: Map<String,String>)

interface SubtitleProvider {
    val id: String
    val displayName: String
    suspend fun search(query: SubtitleQuery): List<SubtitleMatch>
    suspend fun download(match: SubtitleMatch): File
}
```

## 5. Provider architecture

No hard-coded provider in Media UI. Providers are registered in `SettingsService` (enabled/disabled, priority order). Current candidates (where legally appropriate): Jimaku, Kitsunekko, future sources — each behind `SubtitleProvider`.

```
Subtitle Browser (UI)
  → SubtitleService.search(query) fans out to enabled providers in parallel
  → results merged, scored (title match, episode match, language, format)
  → user compares metadata → preview (first 10 cues) → download/import
  → SubtitleService.attach(itemId, binding) — remembers association per episode
  → cached on disk, replayable offline
```

Adding a provider: implement `SubtitleProvider`, register in Koin, appear in Settings → Subtitle Providers and the Browser.

## 6. Subtitle Browser

State: `Idle / Searching / Results / Preview / Downloading / Attached / Error`.

User flow:

1. Play episode → Subtitles tab → "Find Japanese subtitle"
2. Query auto-filled from `Content.title + episode`
3. Provider results (sortable by score/episode/format)
4. Compare metadata (uploader, language, timing notes)
5. Preview first cues (rendered as in player, with styles stripped)
6. Download → import → attach to episode → association persisted

## 7. Association & caching

- Per-episode association: `subtitle_binding { contentId, trackId, providerId, filePath, language, associatedAt }`.
- File stored: `~/.kaiteyo/subtitles/{contentId}/{trackId}.{srt|ass|vtt}` (or platform cache dir).
- Re-associated on next open; user can detach/re-attach.
- Offline: cached track fully usable; provider search unavailable (surfaced as Offline state with cached tracks still listed).

## 8. APIs

```kotlin
interface SubtitleService {
    suspend fun parse(file: File): Result<SubtitleTrack>
    suspend fun search(query: SubtitleQuery): List<SubtitleMatch>
    suspend fun download(match: SubtitleMatch): Result<File>
    suspend fun attach(itemId: String, file: File, language: String)
    suspend fun setOffset(trackId: String, offsetMs: Long)
    suspend fun selectTrack(sessionId: String, trackId: String)
    fun observeTrack(sessionId: String): Flow<SubtitleTrack?>
    fun searchCues(trackId: String, query: String): List<SubtitleCue> // in-track search
}
```

## 9. Failure states

Missing file, malformed cues, unsupported format (surfaced as "Unsupported format — expected SRT/ASS/SSA/VTT"), provider timeout/rate-limit (retry with backoff), language mismatch (warn but allow), timing drift (offset UI).

## 10. Offline / sync / performance

- Offline: attached subtitles fully work; provider search queues or shows cached results.
- Sync: associations are local-only (not platform-synced); file hash stored for re-download.
- Performance: parsing is off-UI-thread; search index incremental; large subtitle files (10k+ cues) virtualized in Browser list.

## 11. Evolution

New format → new `SubtitleParser` impl. New provider → new `SubtitleProvider` impl. No `SubtitleService` rewrite. Embedded extraction delegates to PlayerBackend (mkv/mp4).
