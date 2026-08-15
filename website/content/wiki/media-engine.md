---
title: How the Media Centre works
description: The media engine — playback backends, subtitle parsing, dictionary integration, and the watch→lookup→mine loop.
---

The Media Centre turns your local video and audio into an immersion workspace. This article is how it works.

## Playback backends

The UI only talks to a `PlaybackBackend` interface; each backend reports its capabilities and the UI enables only what's actually available:

- **VLC** (VLCJ) — embedded video canvas, needs a VLC install with libvlc
- **mpv** (JSON-RPC) — launched as a child process and driven over its IPC socket
- **Java Sound** — audio only, always available (no speed control; the chips correctly hide)

A `BackendManager` probes PATH and platform install locations and honours `KAITEYO_VLC_PATH` / `KAITEYO_MPV_PATH`. No backend → a clear error with install guidance.

## Subtitles

Parsers for SRT/ASS/SSA/VTT handle malformed timestamps, overlaps, empty cues, and ASS styling (stripped for lookup). Multiple tracks are supported, including a dual-language secondary track. The active cue is found by binary search at ~10 Hz, with global offset, cue navigation, transcript search, and timeline markers. In-memory cue corrections never touch the source file.

## Text → dictionary → mine

The subtitle overlay renders tokens; click one, shift-click or drag to select a phrase, or click "select all". Selected text opens the dictionary popup and can be mined — the selection always preserves exact original text and timing. A `JapaneseSegmenter` (longest headword match + deinflection fallback) tokenizes the line, and token statuses (unknown/known/learning/mature/mined/suspended) come from your live card pool.

## Mining and media round-trips

Mining a cue creates a real card with sentence, target, reading, definition, screenshot, audio, timestamp, source, and tags. Screenshots come from the backend snapshot; audio clips are extracted with ffmpeg (or Java Sound for WAV/AIFF). Every mined card records a `MediaMiningEvent`, so reviewing a card can jump you back to the exact timestamp.

## Library, history, and persistence

The media library stores items, watched folders, history, resume positions, favorites, tags, collections, and auto-detected episode markers (S01E05, 第3話…) as JSON. A folder watcher sweeps configured folders; a play queue persists across restarts; end-of-episode offers Play next / Replay / Library.

## Analytics

Watch time, study time, lookups, and mined sentences are recorded incrementally into a per-day store (no history scans), answerable per date range — and study-mode watch time is tracked separately from leisure time.

## Local API & external tools

Read-only media endpoints (`/api/player/state`, `/api/media/library`, etc.) plus a player WebSocket (state every 500 ms + control frames) and a text hook server let external tools integrate. All local-only, no filesystem access exposed.

See the [media documentation](/docs/features/media/) for the full specification.
