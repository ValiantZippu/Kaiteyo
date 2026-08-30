# Kaiteyo Core — Central Architecture

> **Status**: `ARCHITECTED` (target) + `PARTIALLY_IMPLEMENTED` (current engines exist, not yet unified).
> Companion: `docs/architecture/OVERVIEW.md` (current architecture), `docs/planning/MASTER_AUDIT_2026.md` (audit), `docs/planning/PRODUCT_AUDIT.md` (two-app defect).

## 1. What it is

**Kaiteyo Core** is the shared service layer that owns all persistent state and cross-feature logic. Every screen is a thin view over Core; no screen owns its own database, sync, or scheduling.

Today the code is split: `core/` (shared KMP) + `desktopApp/.../desktop/engine/*` (JVM-only). Core as defined here is the **unified** layer after ADR-0017 — suite engines migrate onto the Core data model rather than maintaining parallel JSON stores.

## 2. Why it exists

Without Core, every screen re-implements dictionary lookup, progress tracking, card creation, and statistics — producing duplicated SRS tables, two calendars, and fake mining data. Core enforces:

1. UI does not own persistent data.
2. Features communicate through services/events/interfaces.
3. One SRS, one Library, one Activity ledger, one Dictionary, one Mining pipeline.

## 3. Structure

```
Kaiteyo
├── App Shell (Window, Theme, Koin, Sentry)
├── Navigation (NavShell — single implementation)
├── Design System (Ds* + KaiteyoSemanticColors → unified tokens)
│
├── Kaiteyo Core  ← this document
│   ├── IdentityService        user, device, auth tokens, account linking
│   ├── ContentService         unified Content model (see data-model.md)
│   ├── MediaService           MediaSource/Session/Player/Subtitles (owns MediaProgress)
│   ├── ReadingService         ReadingSession, document parsers, progress, highlights
│   ├── DictionaryService      SINGLE entry point for all lookups (subtitle/OCR/ebook/web/game)
│   ├── SubtitleService        parsing/normalization/timing/sync/search/caching
│   ├── OcrService             image → text regions + confidence, caching
│   ├── MiningService          MiningContext → MiningRecipe → Card → DestinationResolver
│   ├── DeckService            decks/cards/tags/flags/notes (SQLDelight UserData)
│   ├── LibraryService         organization (collections/series/seasons/books/manga/imports/history)
│   ├── ActivityService        ActivityEvent ledger (append-only, offline-first)
│   ├── StatisticsService      derived from ActivityService (no second calendar)
│   ├── PlatformService        external metadata/accounts (AniList/MAL/IMDb adapters)
│   ├── SyncService            replication + conflict resolution + offline queue
│   └── SettingsService        typed, service-owned, versioned (no screen-owned prefs)
│
├── Consumers (UI)
│   ├── Home (dashboard over Core)
│   ├── Library (organization view)
│   ├── Browse (discovery view)
│   ├── Media (consumption view)
│   ├── Reading (consumption view)
│   ├── Platforms (external view)
│   ├── Game (world — reads/writes via Core, never a second DB universe)
│   └── Settings/Debug
│
└── Integrations (adapters at the boundary)
    ├── Yomitan dictionaries (DictionaryService)
    ├── ASBPlayer workflow (MediaService + MiningService via LocalApiServer/TextHookServer)
    ├── Anki/AnkiConnect (MiningService destinations + DeckService export)
    ├── Chromium/Browser (BrowserBridge → MediaService/ReadingService)
    └── OCR engines (OcrService — Tesseract/MLKit/Vision)
```

### UI vs Domain

| Layer | Owns | Does not own |
|-------|------|--------------|
| UI (Compose screens) | rendering, input, transient state, navigation | persistence, scheduling, sync, mining logic |
| Core services | persistence, scheduling, sync, business rules, events | Composables |
| Adapters | translation between upstream formats and Core models | Core business rules |

UI communicates with Core only through service interfaces and the event bus. Direct `SQLDelight` access from a screen is banned (`STANDARDS §177`).

## 4. Data flow

```
User action (select text / play media / OCR result / game event)
  → UI dispatches intent to owning service
  → Service validates, persists, emits ActivityEvent
  → ActivityService appends (offline-first, sync queue if needed)
  → StatisticsService derives aggregates (no polling)
  → Other services react via event subscriptions (e.g., MiningService → DeckService)
  → UI observes StateFlow (no manual refresh)
```

Example — subtitle mining:

```
SubtitleCue selected
  → DictionaryService.lookup(text) → DictionaryMatch[]
  → UI shows DictionaryPopup
  → User taps "Create card"
  → MiningService.mine(MiningContext{source=Media, sentence, reading, definition, screenshot, audio, timestamp, contentId})
  → MiningRecipe → Card (DeckService)
  → DestinationResolver (Kaiteyo / Anki / Both → AnkiConnect queue if offline)
  → ActivityEvent(CardCreated, source=Media, contentId, deckId)
  → StatisticsService + LibraryService update
  → Home/Stats reflect immediately
```

## 5. Ownership

| Data | Owner | Consumers read via |
|------|-------|--------------------|
| User identity, tokens | IdentityService | IdentityService.currentUser |
| Content records, progress, artwork, source bindings | ContentService | ContentService.content(id) |
| Media sessions, playback, subtitles, bookmarks | MediaService | MediaService.session |
| Reading sessions, highlights, progress | ReadingService | ReadingService.session |
| Dictionary entries, imports, priority | DictionaryService | DictionaryService.lookup() |
| OCR results, text regions | OcrService | OcrService.result(id) |
| Mining contexts, recipes, dest. config | MiningService | MiningService.history |
| Decks/cards/tags/flags/notes, SRS | DeckService | DeckService.deck(id) |
| Library collections/series/history | LibraryService | LibraryService.library |
| Activity events (append-only) | ActivityService | ActivityService.events(query) |
| Derived stats (daily/weekly/…) | StatisticsService | StatisticsService.aggregates |
| Platform accounts, external IDs | PlatformService | PlatformService.account(id) |
| Sync state, offline queue | SyncService | SyncService.status |
| Settings (typed, versioned) | SettingsService | SettingsService.get<T>(key) |

No duplicated databases for the same concept (rule 19).

## 6. Communication

### Synchronous: service interfaces

Defined in `docs/architecture/nodes/SERVICE_CONTRACTS.md` (NODE §209). Example:

```kotlin
interface DictionaryService {
    suspend fun lookup(query: String, modes: Set<SearchMode>): List<DictionaryMatch>
    suspend fun importBundle(bundle: DictImportBundle): Result<InstalledDictionary>
    fun suggestions(prefix: String): List<String>
}

interface MiningService {
    suspend fun mine(ctx: MiningContext, recipe: MiningRecipe, dest: MiningDestination): Result<Card>
    suspend fun mineFromDictionary(match: DictionaryMatch, sentence: String?, media: MediaContext?): Result<Card>
}
```

### Asynchronous: event bus

Defined in `docs/architecture/nodes/EVENT_CATALOG.md` (NODE §210–211). Every ActivityEvent is persisted; services subscribe without knowing publishers.

```
MediaStarted, MediaPaused, MediaCompleted,
SubtitleSelected, DictionaryLookupRequested/Completed,
OcrCompleted, ReadingStarted/ProgressChanged,
MiningStarted/CardCreated, StudySessionStarted,
PlatformSyncCompleted, GameNodeCompleted
```

Publishers/consumers are documented per-event in `docs/architecture/events.md`.

## 7. Persistence

| Service | Tables / files | Lifecycle |
|---------|---------------|-----------|
| ContentService | `content`, `content_source_binding`, `content_progress`, `content_artwork` (new) | Create on import; update progress/bookmarks; delete cascades bindings |
| MediaService | `media_session`, `media_progress`, `media_bookmark`, `subtitle_track`, `subtitle_cue` | Session ephemeral; progress/bookmarks persist |
| ReadingService | `reading_session`, `reading_bookmark`, `reading_highlight`, `reading_progress` | Session ephemeral; annotations persist |
| DictionaryService | `AppDataDatabase` (read-only) + `installed_dictionary`, `dictionary_index` (user data, per-dict *.json on disk + `data/index/`) | Import replaces by id; index rebuilt on demand |
| OcrService | `ocr_document`, `ocr_text_region` + cached image in `~/.kaiteyo/ocr/` | Per-document; cached; evictable |
| MiningService | `mining_context`, `mined_record` (dedup), `mining_recipe` | Append; idempotent via MinedRecord |
| DeckService | Existing `UserData` (letter_deck, vocab_deck, fsrs_card, review_history, tag, card_*, etc.) | Existing migrations; no duplication |
| ActivityService | `activity_event` (append-only, indexed by type/timestamp/contentId) | Never delete; archive after years |
| StatisticsService | Derived + `daily_stats` rollups (materialized) | Recompute from events; cache |
| PlatformService | `platform_account`, `platform_record`, `platform_sync_state` | OAuth tokens encrypted at rest |
| SyncService | `sync_queue`, `sync_conflict` | Queue drains when online; retry with backoff |
| SettingsService | `settings` (typed KV, versioned, per-profile) | Migration on app update |

## 8. Caching & Sync

- Dictionary index: per-dict *.json in `data/index/` (built on install, invalidated on priority change).
- Subtitle/Manga OCR results: LRU on disk + in-memory `caffeine` for hot entries.
- Platform metadata: TTL cache (configurable per adapter), offline-capable from last sync.
- Sync: `SyncService` owns the offline queue; every write that requires network enqueues and retries (exponential backoff, user-visible queue in Debug).
- No service invents its own sync queue.

## 9. Offline behavior

| Capability | Offline | Notes |
|------------|---------|-------|
| Dictionary lookup | ✅ | Bundled + imported dictionaries on disk |
| Yomitan deinflection | ✅ | Pure Kotlin |
| OCR | ✅ (desktop Tesseract) | Requires installed engine; guided setup handles missing |
| Local media | ✅ | File-based |
| Local reading/manga | ✅ | File-based |
| Mining → Kaiteyo deck | ✅ | Immediate |
| Mining → AnkiConnect | ⏳ queues | SyncService retries when Anki reachable |
| Library/Stats/Game | ✅ | Derived from local Activity ledger |
| Platform metadata sync | ❌ | Queue; cached last sync visible |
| Subtitle provider search | ❌ | Online; cached associations replay offline |

## 10. Failure states (per service)

Every service exposes `loading / empty / error / offline / permissionDenied / unsupportedFormat / timeout / retry` — never a blank screen.

## 11. Evolution

- Add a new content kind → extend `ContentKind` + add source adapter; no existing tables change.
- Add a subtitle provider → implement `SubtitleProvider` interface; UI lists it automatically.
- Add an OCR engine → implement `OcrProvider`; priority order is settings-driven.
- Add a game region → new content package (ADR-0015); no Core schema change.

## 12. Implementation plan (Core)

1. Define service interfaces in `core/src/commonMain/kotlin/ua/syt0r/kanji/core/<domain>/` (contracts first).
2. Implement ActivityService + StatisticsService derivation (single ledger).
3. Migrate suite `DictionaryService`/`MiningService`/`OcrService` onto Core interfaces (keep engine code, swap persistence).
4. Decompose `AppState` into per-service state holders.
5. Wire all screens to Core services (remove direct SQLDelight access).
6. Update `docs/architecture/nodes/SERVICE_CONTRACTS.md` and `EVENT_CATALOG.md` on every change.
