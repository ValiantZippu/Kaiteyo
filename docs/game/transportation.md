# Transportation

**Status**: TARGET (spec). **Source**: expansion spec §29; NODE §107 (trains);
GAMEPLAY_SYSTEMS §14 (trains/vehicles).

## Principle

Transport is part of the *experience* — trains in Japan are iconic — and it must
be **scalable**: the entire real railway network is not simulated. A data-driven
network abstraction lets the slice ship with the Enoden line and grow with
regions.

## Transport modes

| Mode | Slice scope | Notes |
|---|---|---|
| Walking | everywhere | default; running (Shift/stick-press) |
| Trains | Enoden line (Kamakura–Fujisawa) | the flagship: stations, timetables, boarding, travel, arrival |
| Buses | minimal (future) | data-driven routes |
| Ferry/boat | Enoshima ferry | short crossing, scenery |
| Bicycle | future | optional, cosmetic pacing |
| Car | not in scope | no driving gameplay |

## Train system (NODE §107)

Data model (all data, no engine constants):

- **Lines** (Enoden), **routes** (line segments), **stations** (per line),
  **timetables** (direction × station × time, deterministic).
- Each station is a **location node** with platform/interior, signs, NPC station
  staff, knowledge-node links (station names 江ノ島, 鎌倉 are learning content).
- **Timetables drive the simulation**: trains run on schedule (world clock,
  `environment-simulation.md`); missing a train means the next one comes — a
  gentle lesson in patience, never a failure.

## Travel flow (expansion §29, §5)

```
approach station → platform (interior cell) → board (moving interior)
  → ride (window scenery, announcements) → arrival → step off into streamed city
```

- Boarding = the train becomes the player's cell reference; the world streams
  around the moving train (`world-streaming.md`).
- Announcements are TTS- or authored-audio language content (glossable:
  次は、長谷 — "Next: Hase").
- Visual transition: platform → train interior → window — cinematic, skippable,
  reduced-motion aware.

## Scalable network abstraction (§29)

- **Lines as data**: adding the Kanto region = adding lines/routes/stations as
  content, not engine work.
- **Fidelity**: L1 route spines for the world map; L2 real routes in-region; L3+
  playable stations only where authored (matches `world-architecture.md` L0–L4).
- **Long-haul**: region-to-region travel is an authored ride experience (scenery
  proxies, scheduled stops), then the target region streams in — the player never
  waits on a full country load.

## Rules

1. Timetables are deterministic from world clock; schedule fidelity is a content
   property (slice = real-enough, not every train).
2. Missing a train is never a failure state (no quest requires an exact train).
3. Transport is language-rich: station names, announcements, tickets, signs —
   all learnable.
4. Fast travel (map) exists but is balanced: it never hides a region, and
   walking/riding remains the discovery path (map reveal is spatial).

## Acceptance criteria

1. The Enoden slice runs end-to-end: board, ride, arrive, step off, all streamed.
2. Adding a new line requires zero engine changes (data only).
3. Travel never hits a loading stall beyond the streaming budget
   (`world-streaming.md`).
4. All transport text/audio routes through the language pipeline.

## Related

- Streaming: [world-streaming.md](world-streaming.md)
- World layout: [world-architecture.md](world-architecture.md)
- Spec: NODE §107; GAMEPLAY_SYSTEMS §14; STANDARDS §29
