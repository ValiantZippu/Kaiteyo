# Media Center — Anki

> Status: IMPLEMENTED  
> Files: `desktop/engine/mining/AnkiConnectTransport.kt`, `MiningIntegration.kt`, `MiningEngine.kt`

## Configuration

| Key | Default | Meaning |
|---|---|---|
| `media.anki.enabled` | false | gate `AnkiConnectTransport` in `transports` |
| `media.anki.host` | `127.0.0.1` | AnkiConnect host |
| `media.anki.port` | 8765 | AnkiConnect port |
| `media.anki.key` | "" | API key (optional) |
| `media.anki.deck` | "" | deck override (blank = use `MiningPayload.deckId`) |
| `media.mine-destination` | `kaiteyo` | `kaiteyo`/`anki`/`both` (legacy `media.anki.send-mined` → `Both`) |
| `media.mine-deck` | `DesktopCard.DEFAULT_DECK_ID` | default deck for subtitle mines |

## Flow

`Media → MiningRequest → MiningService → DestinationResolver → AnkiDestination → AnkiIntegration → AnkiConnect`

- UI never hard-codes Anki; buttons call `MiningEngine.mine(payload)` with `destinationOverride` or settings-derived `resolveDestination()`.
- `MiningIntegrationManager.forward(payload, destination)` only adds `anki` when `media.anki.enabled && destination != Kaiteyo`.

## Transport

`AnkiConnectTransport(config: () -> AnkiConfig)` — reads host/port/key/deck at call time (changes apply without rebuild). `send(payload)` POSTs JSON-RPC `addNote` (deck, model, fields `Expression/Reading/Meaning/Sentence/Screenshot/Audio`, media attach base64), 5–10s timeout, `User-Agent: Kaiteyo/1.0`, returns `Result<String>`. `testConnection()` verifies `http://host:port` reachable.

## Semantics

- `Kaiteyo` → native `DesktopCard` only.
- `Anki` → sends to Anki; on success no Kaiteyo card (record `destination=anki`); on failure (or disabled) falls back to native card + `enqueuePendingExport` if `media.anki.enabled`, shows warning toast — word never lost.
- `Both` → native card + Anki; Anki failure queues `PendingAnkiExport` (no Kaiteyo dup on retry), `MinedRecord.ankiStatus` = `success`/`failed` + `ankiError`.
- `retryPendingAnki()` re-attempts all queued exports (no Kaiteyo re-create), marks successes, bumps `attempts` on failures.

## UI

Settings → Media → Anki (host, port, key, deck, enable toggle, test connection, `media.anki.enabled` badge). Mining dialog shows destination selector (`Kaiteyo`/`Anki`/`Both`) per `resolveDestination()`.

## Verification

Pending live AnkiConnect (BLOCKED list) — code-complete, `testConnection` guards, `PendingAnkiExport` pattern verified by `MediaMiningDestinationTest` (Both with unreachable → pending).
