# Kaiteyo — ASBPlayer Integration

> **Status**: `PARTIALLY_IMPLEMENTED` (intentional boundary) — see `docs/architecture/media.md`, `docs/media/ASBPLAYER_WORKFLOW.md`.
> Reference: `reference/asbplayer/` (upstream clone for research).

## 1. Upstream

- Repository: https://github.com/killergerbah/asbplayer
- License: **MIT/GPL mix** — verify per file before reuse (check `LICENSE` at clone time).
- Core: browser extension + subtitle handling + playback control + sentence extraction + auto-pause/condensed playback + card creation via AnkiConnect.

## 2. Architecture (ASBPlayer)

Subtitle handling (SRT/ASS/WebVTT), playback control (player abstraction over `<video>`), subtitle navigation, sentence extraction/timing, word tracking, Yomitan integration, card creation, browser extension messaging. Assumes: DOM `<video>`, extension APIs, WebExtensions messaging, selectable subtitle overlay.

## 3. What Kaiteyo reuses vs adapts

| Concern | Upstream | Kaiteyo |
|---------|----------|---------|
| Subtitle handling (cue timing, sync, track selection) | reference | native `SubtitleEngine` reuses logic where license permits |
| Playback control (seek, A-B, speed, auto-pause) | reference | `MediaEngine` (VLC/mpv/JavaSound abstraction) — not `<video>` |
| Sentence extraction (multi-word selection + segmentation + deinflection) | reference | `SubtitleNormalizer` + `DictionaryService` (multi-token lookup) |
| Card creation flow | reference | `MiningService` (unified — not ASBPlayer's Anki-only path) |
| Browser/extension APIs, DOM `<video>`, messaging | ❌ not reused | replaced by `LocalApiServer` + `TextHookServer` + `PlayerStateWebSocket` |

## 4. Adapter boundary

```
External player (ASBPlayer/mpv/...) → JSON over TextHookServer / PlayerStateWebSocket
  → KaiteyoMiningEndpoint (validated, no internal API exposure)
  → MiningService (same MiningContext as native media)
  → DestinationResolver (Kaiteyo / Anki / Both)
```

Kaiteyo owns: UI, application state, Library, Stats, Decks, Settings, Platforms, account/sync, navigation. Upstream provides mature subtitle/media-learning patterns, not a runtime dependency.

## 5. What to never copy

Extension UI, manifest, content-script injection, direct DOM `<video>` assumptions.

## 6. Evaluation gate

Before any direct code reuse, verify license per file + document attribution. The boundary above is the default (interoperate via JSON, not embed).
