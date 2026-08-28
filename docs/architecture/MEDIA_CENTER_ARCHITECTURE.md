# Media Center — Architecture

> Status: IMPLEMENTED — core engine + desktop workspace  
> Owner: `desktop/engine/media` + `desktop/engine/playback` + `desktop/engine/mining`  
> Related: `media.md` (canonical), `reference/REFERENCE_ARCHITECTURE.md`, `MEDIA_CENTER_CURRENT_STATE.md`

## 1. Target (from rebuild directive §66)

```
Kaiteyo Core
 ├── Domain Services (MediaService → MediaLibrary/MediaScanner, MiningService, DictionaryService, TtsService)
 ├── Event Log + Node System
 └── Media Center
      ├── Library (MediaLibraryPanel, MediaHome, MediaDetail)
      ├── Player (MediaEngine + PlaybackBackend)
      ├── Transcript (SubtitleEngine)
      ├── Dictionary (DictionaryPopup → MiningPayload)
      ├── Mining (MiningEngine → DestinationResolver → Kaiteyo/Anki/Both)
      └── History/Stats/Home (events)
```

Every media action emits a domain event; nodes are materialized from provenance.

## 2. Module boundary (B1 — DONE)

- Core: `MediaCentreContent` (fun interface) + `DefaultMediaCentreContent` (honest desktop-only placeholder, no fake tracks/sliders). Koin: `mediaCentreModule`. Replaces prototype `MediaCentreScreen.kt` (40 fake tracks removed).
- Desktop override: `DesktopMediaCentreContent` in `desktopApp/MediaCentreDesktopHost.kt` mounts real `MediaView` with `AppState()`. Registered in `desktopApp/Main.kt` (`single<MediaCentreContent> { DesktopMediaCentreContent }`).
- No ghost UI: fallback has no sliders/buttons that pretend to play; desktop path has capability-gated controls only.

## 3. Services

| Service | Type | Notes |
|---|---|---|
| `MediaEngine` | Controller | Owns `BackendManager`, `SubtitleEngine`, `MediaLibrary`, `MediaScanner`, `MediaCapture`, `MediaStatisticsStore`, play queue (`~/.kaiteyo/media/queue.json`) |
| `PlaybackBackend` | Interface | `VlcBackend`, `MpvBackend`, `AudioBackend`; `PlaybackCapability` gates UI |
| `MiningEngine` | Card pipeline | `mine(payload, destinationOverride)` → `DestinationResolver` → `MiningIntegrationManager.forward` |
| `DictionaryService` | Shared | Yomitan ZIP import, prioritized search — reused, not duplicated |

## 4. Data flow (one pipeline)

```
MediaView → selection → DictionaryService → MiningPayload (mediaId, cueId, timestamp, screenshot/audio)
        → MiningEngine.mine() → CardDestination (Kaiteyo/Anki/Both) → AnkiConnectTransport / AppState.addCard
        → MinedRecord + MediaMiningEvent + EventLog (CardMined) + MediaNodeGraph (mined_from)
```

All mining producers (Media, Reader, OCR, Browser, external TextHook) feed the same `MiningEngine`.

## 5. Decisions

| Decision | Reference | Why not copy | Implementation | Test |
|---|---|---|---|---|
| Keep `PlaybackBackend` abstraction | jidoujisho player layer | Kaiteyo already has VLC/mpv capability gating (§194) | `engine/playback/PlaybackModels.kt` | `MediaEngineTickSafetyTest` (hostile backend) |
| Pure subtitle parsers | jidoujisho SRT/ASS | Don't couple parsers to player | `SubtitleParser.kt` | `SubtitleParserFuzzTest` (8 cases, 20k) + `MediaLargeLibraryPerfTest` |
| One mining pipeline with `CardDestination` | jidoujisho Anki export | Avoid 3 separate export pipelines | `MiningEngine.resolveDestination` | `MiningStatisticsStore` counters |
| Honest fallback, no fake tracks | — | No prototype should masquerade as finished | `MediaCentreScreen.kt` rewrite | Visual: mobile shows desktop-only empty state |
| 10Hz fail-safe tick | Kaiteyo hardening | Prevent "opening Media closes app" | `MediaEngine.tick()` wrapper | `MediaEngineTickSafetyTest` |

## 6. Statuses (2026-08-29)

| Item | Status |
|---|---|
| Engine + playback + library + subtitles + capture | IMPLEMENTED |
| Mining pipeline (Kaiteyo/Anki/Both) with fallback + pending queue | IMPLEMENTED |
| Node graph (full subtitle index, idempotent, mined preservation) | IMPLEMENTED |
| Statistics events (MediaStarted/MediaEnded/SubtitleSelected/DictionaryLookup/BookmarkAdded) | IMPLEMENTED |
| Browser/download (honest, no DRM bypass) | IMPLEMENTED |
| Immersion heatmap (52-week media calendar) | IMPLEMENTED |
| Subtitle parser fuzz / mining E2E / graph index tests | IMPLEMENTED |
| Future readers | DEFERRED (extension point) |

## 7. Batch 8 Polish — Verification (2026-08-29)

- **Theming:** all media UI via `surfaceColors()` + `Ds*` tokens (`DsCard`, `DsTabRow`, `DsStatTile`, `DsBadge`), no hard-coded colors; light/dark respected; `heatColor(level)` uses `ac.primary` alpha ramp, not raw hex.
- **Responsive:** `WorkspaceShell` adaptive (dock rail / compact tab bar / floating launcher), `MediaView` cinema/fullscreen, same `MediaEngine` powers desktop/phone (phone uses bottom sheets — not scaled desktop).
- **Accessibility:** heatmap not color-only (level labels “Less/More”, watch minutes text, tooltip + dialog), frequency bands labeled in `MiningStatisticsStore`, contrast via `surfaceColors` tokens, keyboard nav via `ShortcutRegistry` + `handleKey`.
- **Ghost UI:** no ghost buttons/sliders — fallback `DefaultMediaCentreContent` shows honest empty-state; desktop controls gated on `PlaybackCapability` + `PlaybackError.userMessage`.
