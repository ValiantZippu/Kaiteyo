# Kaiteyo — Event Architecture

> **Status**: `ARCHITECTED` (target catalog) + `PARTIALLY_IMPLEMENTED` (ActivityEvent ledger exists).
> Companion: `core.md`, `stats.md`, `data-model.md`, `../architecture/nodes/EVENT_CATALOG.md`.

## 1. What it is

An internal **event bus + persisted ledger** (`ActivityEvent`) that decouples publishers from consumers. Every meaningful user/system action emits an event; Stats, Library, Home, and Sync react without knowing who emitted it.

## 2. Catalog

| Event | Publisher | Consumers | Payload (typed) |
|-------|-----------|-----------|-----------------|
| `MediaStarted` | MediaService | ActivityService, Stats, Home | contentId, itemId, positionMs |
| `MediaPaused` | MediaService | ActivityService, Stats | contentId, positionMs, watchDurationMs |
| `MediaCompleted` | MediaService | ActivityService, Stats, Library | contentId, totalWatchMs |
| `SubtitleSelected` | MediaService (SubtitleEngine) | DictionaryService, ActivityService | contentId, cueIndex, text |
| `DictionaryLookupRequested` | Any surface (Media/Reading/OCR/Game/Browser) | ActivityService, DictionaryService | query, source, contentId? |
| `DictionaryLookupCompleted` | DictionaryService | ActivityService | query, resultCount, topScore |
| `OcrCompleted` | OcrService | ActivityService, ReadingService | imageHash, regionCount, engine, confidence |
| `ReadingStarted` | ReadingService | ActivityService, Stats | contentId, documentId, chapterId |
| `ReadingProgressChanged` | ReadingService | ActivityService, ContentService, Stats | contentId, page, percent |
| `MiningStarted` | MiningService | ActivityService | miningContextId, source, contentId? |
| `CardCreated` | MiningService / DeckService | ActivityService, Stats, Library, Sync | cardId, deckId, source, contentId? |
| `StudySessionStarted` | DeckService | ActivityService, Stats | deckId, mode, cardCount |
| `StudySessionCompleted` | DeckService | ActivityService, Stats | deckId, correct/incorrect, durationMs |
| `GameNodeCompleted` | Game | ActivityService, Stats | nodeId, regionId, reward? |
| `PlatformSyncCompleted` | PlatformService | ActivityService, ContentService, Library | accountId, syncedCount, durationMs |

Every event carries: `id`, `type`, `timestamp`, `actor` (userId/deviceId), `contentId?`, `sessionId?`, `details: JsonObject` (typed per event, validated).

## 3. Storage

- `activity_event` (append-only, indexed by type/timestamp/contentId/sessionId), plus `daily_stats` materialized rollups.
- Never delete; archive after years (exportable). Corrupt snapshot recovery: null → retry, logged, surfaced in Debug.

## 4. Delivery

- In-process `MutableSharedFlow<ActivityEvent>` (replay 0, extraBufferCapacity 64, onBufferOverflow DROP_OLDEST + log).
- Consumers `collect` on their scope; no consumer blocks publisher.
- Offline: events appended locally regardless of network; SyncService replicates when online (idempotent by `eventId`).

## 5. Adding an event

1. Define payload type + validation.
2. Add row to this catalog (publisher/consumers/payload).
3. Update `EVENT_CATALOG.md` (semantic payload, JSON snapshot).
4. Add publisher call + consumer handler.
5. Add test covering persistence + derived stats.

## 6. Evolution

New feature → new event type; existing consumers ignore unknown types (forward compatible). No bus rewrite.
