# Game Audio

**Status**: TARGET (spec). **Source**: expansion spec §34; NODE §142 (audio);
JOURNEY_RUNTIME_SPEC §14 (audio buses); `docs/architecture/media.md`.

## Principle

The world sounds alive: ambient per cell/location, music, weather layers, NPC and
transport sounds, ocean, UI feedback, and language audio — all data-driven,
distance-mixed, deterministic with time/weather/season, and fully accessible.

## Audio zones (expansion §34)

- **Zones are data**: each cell/location/object declares audio refs
  (ambient loops, music themes, spot sounds). Entering a zone fades its layer in;
  exiting fades out (deterministic curves, no pops).
- Zone types: exterior ambience (street, beach, mountains), interior ambience
  (shop, station, aquarium), event audio (festival music), weather layers,
  transport (train rattle, announcements), ocean (waves by state), NPC/vehicle
  proximity sources.

## Buses (JOURNEY_RUNTIME_SPEC §14 — the mixing model)

| Bus | Content | Reactive to |
|---|---|---|
| `ambience` | per-cell/location ambient loops | time of day, location |
| `music` | region/location music, event themes | location, story/quest state, season |
| `weather` | rain/storm/wind/fog layers | weather state (deterministic seeds) |
| `npc` | NPC dialogue/voice, footsteps | NPC tier, schedule, distance |
| `transport` | trains/vehicles/announcements | transport simulation, schedule |
| `ocean` | waves, beach | location, weather, wind |
| `ui` | world UI feedback | UI events |
| `language` | pronunciation (TTS reuse), announcements, dialogue audio | knowledge overlay, announcements |

Rules:

1. **Single audio path**: app master volume/mute settings apply inside the world
   (the game doesn't have its own separate volume world).
2. **Distance mixing + LOD** for spatial sources; per-platform bus budgets
   (`world-streaming.md`).
3. **Deterministic**: same world state → same mix (audio is a pure function of
   state, so save/load restores the same soundscape).
4. **Dynamic mixing**: buses duck (announcement over music, dialogue over
   ambience) with authored priorities — never chaos.

## Language audio

- Pronunciation/announcements reuse the app TTS (one TTS path, `docs/architecture/tts*`).
- Authored dialogue uses authored audio (AUDIO PRODUCTION) with a **TTS fallback
  + honest label** when voice assets are absent (§158–§159) — never fake
  "voice-acting."
- Announcements (trains, aquarium feeding times) are language content:
  glossable, replayable.

## Volume groups & accessibility (expansion §34, STANDARDS §254)

| Group | Purpose |
|---|---|
| Master | everything (shared with app) |
| Music | background music |
| Effects | world/NPC/transport/UI |
| Voice/language | dialogue, TTS, announcements |
| Ambience | loops (separately reduceable) |

- Accessibility: any bus individually muteable; **audio cues never required for
  gameplay** (a train is also visual; an NPC is also on the map); subtitle size/
  background settings apply to dialogue; reduced motion doesn't affect audio
  beyond removing whooshes if authored.
- Deaf/hard-of-hearing: subtitles for all authored audio; visual fallbacks for
  audio-only information (announcement text shows on screen).

## Audio content pipeline

- Audio is content (packages): loops, music, spot SFX authored per region/season;
  validated (format, duration, size) by the content pipeline (ADR-0015).
- Procedural layering (weather/water) uses seeded parameters for determinism,
  not random generators.

## Acceptance criteria

1. Entering/exiting zones crossfades without pops; same state → same mix.
2. App volume/mute is honored inside the world (single audio path).
3. No gameplay information is audio-only.
4. All authored audio has subtitle/visual equivalents.

## Related

- Environment: [environment-simulation.md](environment-simulation.md)
- NPCs: [npc-system.md](npc-system.md) · Transport: [transportation.md](transportation.md)
- Spec: NODE §142; JOURNEY_RUNTIME_SPEC §14; STANDARDS §34
