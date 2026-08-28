# Media Center — Mining

> Status: IMPLEMENTED  
> Principle: one mining pipeline, multiple producers, configurable destinations

## Pipeline

```
User selects subtitle text
        ↓
SelectionService (MediaEngine.selectToken / selectTokenRange, anchor-based)
        ↓
DictionaryService (shared, Yomitan provider) → DictionaryEntry
        ↓
MiningContext (MiningPayload: headword, reading, definition, sentence, screenshot/audio/video paths, tags, flags, notes, timestamp, source, sourceDetail, deckId, example, pitchAccent)
        ↓
CardComposer (MiningEngine.createNativeCard: id, meaning, tags `mined` + `source:<x>`, note with Sentence/Example/Source/Notes/Screenshot/Audio/Video/Timestamp)
        ↓
CardDraft → DestinationResolver
         ├── Kaiteyo (AppState.addCard, always available)
         ├── Anki (AnkiConnectTransport, only if media.anki.enabled)
         └── Both (Kaiteyo + Anki, one action creates both)
```

## Implementation

| Concern | File | Notes |
|---|---|---|
| `MiningPayload` | `desktop/engine/mining/MiningEngine.kt:31` | source-agnostic, all producers use same shape |
| `mine(payload, destinationOverride)` | same:148 | `resolveDestination()` reads `media.mine-destination` (`kaiteyo`/`anki`/`both`) + legacy `media.anki.send-mined`; `Anki` path sends-only to Anki and falls back to native card so word never lost |
| `MiningIntegrationManager` | `MiningIntegration.kt` | `mode` (`Kaiteyo`/`Forward`/`Both`) for GSM, `transports` list, `forward(payload, destination)` adds GSM per mode + `anki` per destination |
| `GsmTransport` | same | `http://host:port/path` + `Authorization: Bearer token`, `send()` builds JSON, `testConnection()` GET |
| `AnkiConnectTransport` | `AnkiConnectTransport.kt` | `AnkiConfig(host, port, apiKey, deckOverride)`, JSON-RPC to `http://host:port`, media attach base64, `send(payload)` + `testConnection()` |
| `MinedRecord` / `PendingAnkiExport` | `MiningEngine.kt:71` | capped 200/20, `enqueuePendingExport` (no dup by mineId+headword), `retryPendingAnki()` (attempt counter, marks success) |
| Media hook | `MediaEngine.recordMiningEvent` | creates `MediaMiningEvent(cardId, mediaPath, mediaName, timestampMs, cueText)`, updates `mediaNodeGraph.addMiningEvent`, fires `onMined` → `MediaReferenceStore` |

## Producers (shared)

`Media (subtitle) → MiningService`, `Reader → same`, `Ocr → same`, `Browser → same`, `Clipboard → same`, `Yomitan → same`, `ASBPlayer (TextHook) → same`, `Game → same` (GSM transport). No per-feature mining duplication.

## Provenance

Every mine retains `mediaId, cueId, timestamp, screenshot/audio` in `MiningPayload.videoPath/timestamp/screenshotPath`; `DesktopCard.note` contains `Screenshot/Audio/Video/Timestamp` lines for “Where did I learn this?” (`Attack on Titan S01E01 @ 14:23`).

## Tests

`MediaMiningDestinationTest` (Kaiteyo-only, Both with unreachable Anki → pending, Anki-only fallback when disabled, provenance), existing tick/lifecycle tests.
