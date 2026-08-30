# Kaiteyo — Anki / AnkiConnect

> **Status**: `IMPLEMENTED` (.apkg + AnkiConnect) + `ARCHITECTED` (offline queue, destination resolver).
> Companion: `mining.md` (MiningContext), `integrations/anki.md`, `core.md`.

## 1. What it is

A complete interop layer: **`.apkg` import/export** (pure-Kotlin ZIP, cross-platform) + **AnkiConnect** as a `MiningDestination`. Users choose per global setting or per action:

- **Kaiteyo only**
- **Anki only**
- **Kaiteyo + Anki (Both)**

The same `MiningContext` feeds any destination — never separate mining implementations.

## 2. Why it exists

Many learners maintain an Anki collection. Kaiteyo must not force a choice or duplicate mining logic. Mining → destination is a fan-out; Kaiteyo's own Library and Activity remain the source of truth for Kaiteyo features.

## 3. AnkiConnect contract

```
MiningContext → MiningRecipe → fields { Expression, Reading, Meaning, Sentence, Screenshot, Audio, Tags }
  → AnkiConnectTransport.addNote
      { deckName, modelName (note type), fields, tags, media: [{filename, data}], allowDuplicate=false }
  → { result: noteId } or { error: "cannot create note because it is a duplicate" / "collection is not available" }
```

- **Deck selection**: default deck from `SettingsService: anki.defaultDeck`; per-mine override via `MiningContext.deckId` or popup picker.
- **Note type**: default from settings; field mapping from `MiningRecipe.fieldMapping`.
- **Media**: screenshot (PNG/JPG) + audio (mp3/wav) uploaded as base64 `storeMediaFile` before `addNote`, or inline via `audio` field — transport handles either.
- **Duplicate handling**: Anki's `allowDuplicate=false` + `MinedRecord` idempotency on Kaiteyo side (rehashing `selectedTerm+sentence+source+contentId`); conflict policy user-selectable where Kaiteyo owns the `.apkg` path (Skip/Update/Duplicate — .apkg only; AnkiConnect delegates to Anki).
- **Offline queue**: when Anki not reachable, enqueue `AnkiConnectRequest { ctx, recipe, deck, tries, nextRetryAt }` in `sync_queue`; exponential backoff (30s → 2m → 10m → 30m, capped); surfaced in Debug as "Queued for Anki" with Retry/Remove.
- **Failure handling**: transport distinguishes `unreachable` (queue), `duplicate` (surface existing), `validation` (show field error), `permission` (Anki not running — prompt to launch Anki).

## 4. .apkg import/export (core)

| Capability | Impl | Notes |
|------------|------|-------|
| ZIP/inflate | `core/transfer/AnkiPackage.*` (pure Kotlin) | JVM/Android/iOS actuals — no native zlib dependency |
| Collection parsing | `AnkiImportMapper` | decks, notes, cards, media, scheduling mapping |
| Template rendering | bucket codec | HTML sanitization |
| Media extraction | stream copy | large media not loaded fully in memory |
| Conflict policy | Skip / Update / Duplicate | user choice on import |
| Rollback | transaction | broken import rolls back, never corrupts DB |

## 5. Data model

```kotlin
data class AnkiDestinationConfig(
    val ankiConnectUrl: String = "http://127.0.0.1:8765",
    val defaultDeck: String = "Default",
    val defaultModel: String = "Basic",
    val fieldMapping: Map<String,String> = mapOf(
        "Expression" to "selectedTerm",
        "Reading" to "reading",
        "Meaning" to "definition",
        "Sentence" to "sentence",
        "Screenshot" to "screenshot",
        "Audio" to "audioPath",
    ),
    val enabled: Boolean = false,
)

data class AnkiConnectRequest(
    val id: String,
    val context: MiningContext,
    val recipe: MiningRecipe,
    val tries: Int = 0,
    val nextRetryAt: Instant?,
    val error: String?,
)
```

## 6. Offline / sync

| Path | Offline |
|------|---------|
| Export .apkg to file | ✅ |
| Import .apkg from file | ✅ |
| Mining → Anki (Anki running) | ✅ (immediate) |
| Mining → Anki (Anki not running) | ⏳ queued, retry with backoff, visible in Debug/Deck UI |

Never require Anki to mine to Kaiteyo. Kaiteyo-only works with no Anki installed.

## 7. Failure states

Anki not running (queue), AnkiConnect not enabled (guided setup — "Install AnkiConnect add-on"), deck/model not found (picker prompt), media too large, duplicate (surface), permission (loopback blocked — explain firewall), timeout (retry).

## 8. Security

AnkiConnect is loopback only (`127.0.0.1:8765`); no credentials; no remote access. File imports are validated (zip slip protected — `SafeArchiveExtractor`).

## 9. UI states

Idle, Mining→Kaiteyo success, Queued for Anki, Anki success (show noteId → open in Anki if supported), Duplicate, Error with retry/change-destination, Offline queue list.

## 10. Evolution

New Anki field → update `fieldMapping` in Settings (no code). New Anki media handling → extend `AnkiConnectTransport`. No mining rewrite.
