---
title: Integrations
description: How Kaiteyo works with Anki, Yomitan dictionaries, media players, browser tools, and its own local API — what exists today, and what is planned.
---

Kaiteyo is a complete learning environment on its own — but it plays well with the ecosystem around it. Every integration below is **real and documented** in the repository; where something is planned rather than shipped, it's labeled clearly.

## Anki

Two-way compatibility, in two forms:

- **`.apkg` import/export** — the standard Anki package format, implemented in shared code and available on desktop, Android, and iOS. Bring years of Anki history into Kaiteyo, or export Kaiteyo decks to `.apkg`. Import preserves deck hierarchy (nested decks map to Kaiteyo collections), notes, cards, tags and scheduling; media is extracted and references repaired.
- **AnkiConnect** (desktop suite) — a live bridge to a running local Anki installation. Push mined cards straight into Anki, or import whole decks from Anki, over Anki's own localhost JSON-RPC API. No credentials, trust is local.

Kaiteyo is **not** Anki — it owns its own decks, scheduler, reviews and statistics. Anki integration is optional and additive, never a dependency. See the [Anki integration documentation](/docs/integrations/anki/) for exact details and honest compatibility limits.

## Yomitan-compatible dictionaries

The desktop dictionary accepts **Yomitan-compatible formats** — ZIP archives or JSON documents with an `index.json` manifest (the format used by Yomitan, the browser dictionary). JMdict, KANJIDIC and KanjiVG files are also supported directly.

Imported dictionaries sit alongside the built-in one and are searchable together, with per-dictionary enable/priority controls. Imported dictionaries stay on your device and never leave it. See [Yomitan dictionaries](/docs/integrations/yomitan_dictionaries/).

## Media players

The Media Centre doesn't ship a fixed player — it talks to a `PlaybackBackend` and uses whichever is available:

| Backend | Video | Notes |
|---|---|---|
| **VLC** (VLCJ) | Yes | Embedded canvas; needs a VLC install with libvlc |
| **mpv** (JSON-RPC) | Yes | Launched as a child process, driven over its IPC |
| **Java Sound** | Audio only | Always available; seek, volume, no speed control |

The UI only enables controls the active backend can actually do. If no video backend is installed you get clear install guidance, never a dead end. See [Media backends](/docs/integrations/media_backends/).

## Local API & tools

The desktop app runs an opt-in **local HTTP API** (bearer-token protected, localhost only) with status, mine, media and player endpoints — so external tools can read or drive Kaiteyo. Two companion servers stream live data to other programs:

- **Player WebSocket** (`ws://127.0.0.1:8765`) — live player state + control frames (play, pause, seek, screenshot, mine, replay)
- **Text hook** (TCP 8766) — accepts raw Japanese text lines from texthookers/scripts and pushes them into the dictionary workflow

No filesystem access is exposed through any of these. See the [local API documentation](/docs/integrations/local_api/) and [local API wiki](/wiki/local-api/).

## Browser & media workflows

Because the dictionary, mining and card pool are all part of the same desktop suite, tools like Yomitan, ASBPlayer-style players, or any browser/texthooker workflow can feed Kaiteyo: look up in the browser → copy → mine into a card → review. The text hook makes this work with scripts and games too.

## Extensions & plugins

The plugin system is **planned** (registry and marketplace scaffold exist in the repository, but there is **no runtime loading yet**). Until it ships, extension happens through the integrations above. See the [plugins page](/plugins/) and [plugin documentation](/docs/integrations/plugins/).

## What's planned, honestly

- Plugin runtime loading (v3.0 roadmap)
- Public API for third-party tools (roadmap)
- Cloud sync is gist-based today (desktop-first) — no central Kaiteyo service exists

Nothing above is advertised as finished if it isn't. The full status matrix lives in [docs/features/FEATURES.md](/docs/features/features/).
