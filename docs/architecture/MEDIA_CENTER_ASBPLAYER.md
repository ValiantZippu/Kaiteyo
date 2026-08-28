# Media Center — ASBPlayer

> Status: IMPLEMENTED (boundary, localhost)  
> Files: `desktop/engine/media/TextHookServer.kt`, `PlayerStateWebSocket.kt`, `MediaEngine.kt` (hook wiring), `MiningEngine.kt`

## Concept

Not a UI clone — the workflow `video → subtitle → select text → dictionary → sentence → audio → screenshot → mining → Anki/Kaiteyo` is provided via a clean integration boundary so external players (ASBPlayer, texthookers, browser extensions) can feed `MiningService` without Kaiteyo owning their player.

## Boundary

```
ExternalMediaIntegration (ASBPlayer, texthooker, browser extension)
        ↓  localhost TCP / WebSocket
KaiteyoMiningEndpoint (TextHookServer `8766`, PlayerStateWebSocket `8765`)
        ↓
MiningService (MiningEngine.mine)
        ↓
DestinationResolver (Kaiteyo / Anki / Both)
```

## Implementation

| Surface | Port | Auth | File |
|---|---|---|---|
| `TextHookServer` | 8766 (`media.text-hook.port`) | localhost only, `normalizeForLookup` | `TextHookServer.kt` |
| `PlayerStateWebSocket` | 8765 (`media.ws.port`) | localhost only, `PlayerStateSnapshot` broadcast 500ms | `PlayerStateWebSocket.kt` |

- `MediaEngine.startIntegrationsIfEnabled()` starts each when `media.text-hook.enabled` / `media.ws.enabled` / `media.watch-folders` / `media.api.enabled` (settings).
- `onHookText(text)` → `normalizeForLookup().take(120)` → `lookupText()` + `ActivityLog`; `CLEAR` clears lookup.
- `onSocketCommand(json)` → `play`/`pause`/`toggle`/`stop`/`screenshot`/`mine`/`replay`/`seek`/`lookup`.
- `stateJson()` broadcasts `PlayerStateSnapshot(media, path, positionMs, durationMs, playing, buffering, speed, backend, subtitle, selectedToken, lookupQuery, minedCount, textHookRunning, wsClients)`.

## Media → Mining

External text → `lookupText()` → `DictionaryPopup` → `payloadForCue()` → `MiningEngine.mine(payload)` → same destinations. No external API is invented; feasibility is runtime-verified (toasts on `Text hook listening on port …` vs failure).

## Limitations (honest)

- External player must speak the simple JSON/text protocol (no proprietary ASBPlayer API assumed).
- Auth is localhost-only; future extension can add token headers (GSM-style) without touching media core.
- If no external tool is present, the boundary is dormant (no ghost buttons).
