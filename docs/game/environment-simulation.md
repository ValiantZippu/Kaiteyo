# Environment Simulation

**Status**: TARGET (spec). **Source**: expansion spec §30; NODE §117 (time/weather/
seasons); GAMEPLAY_SYSTEMS §16 (time/weather/seasons), §17 (world events).

## Principle

The world has **time of day, weather, and seasons** that are deterministic,
reactive, and never soft-locking (§117). Every environment system is classified
as **simulation** (state that affects gameplay), **visual only** (presentation),
or **gameplay relevant** (affects content availability) — and the three are
engineered separately.

## System classification

| System | Kind | Affects |
|---|---|---|
| Time of day (clock) | simulation | NPC schedules, shop hours, train timetables, lighting, audio, day/night |
| Sun/moon | visual only | lighting, sky, shadows |
| Weather (rain/snow/fog/clouds/wind) | simulation + visual | beach closure, outdoor NPC behavior, audio, lighting, particle effects |
| Season | simulation + gameplay | content gates (beach summer, festival season), vegetation, light, activities |
| Temperature abstraction | gameplay relevant (soft) | seasonal activities hint (swimming = warm), never a survival stat |
| Water (waves, beaches, sea state) | simulation + visual | swimming gate (storm = closed), audio, visuals |
| Streetlights / building lights | visual only | night ambience, deterministic with clock |
| Traffic | visual only (sparse) | ambience; no traffic gameplay |
| Vegetation | visual only + season | seasonal color (sakura, autumn), audio (wind) |

## Determinism rule

- Environment state is a pure function of **(world clock, weather seed, season)**
  — same state → same world, same save restore (§144). No random weather that
  breaks save/load determinism.
- Weather transitions are authored curves (clear → cloudy → rain), seeded per
  region/season; they feel alive but never chaotic.

## Time system

- **World clock** with adjustable time mode: real-time sync (optional) or
  game-time pacing; speed selectable (day = 1–15 min real time, configurable;
  stops while paused).
- Time drives: lighting (day/night cycle), NPC schedules, shop hours, train
  timetables, announcements, audio layers, photo conditions (golden hour is a
  *photography* moment).
- **Never soft-locks**: a shop closed at night is honest UI ("opens at 9:00");
  the world keeps offering things to do (night ambience, stations, NPCs with
  evening schedules).

## Weather system

- Weather states: clear, partly cloudy, cloudy, rain, storm, snow (winter),
  fog; wind intensity layer.
- Weather affects: beach/swimming gate (storm), outdoor NPC behavior (shelter),
  audio (rain/storm/wind layers — `game-audio.md`), visuals (particles, wet
  surfaces, fog), lighting (overcast).
- Weather **never blocks required content**: a storm delays the ferry with honest
  UI and an alternative; a festival moves indoors (authored weather fallbacks).
- Weather is deterministic from seed (same seed + clock → same weather timeline).

## Seasons

- Four seasons with authored transitions (sakura spring, green summer, autumn
  leaves, winter light).
- Season gates content: summer beach + lifeguard + swimming; festival season
  (fireworks matsuri); New Year shrine visit; seasonal menus/stamps.
- **Rules**: seasonal content returns every year (nothing permanently missable);
  seasonal quests pause/resume with the season (`quest-system.md`); the world is
  fully playable in every season (no dead seasons).

## World events (§30 expansion, GAMEPLAY_SYSTEMS §17)

- **World events** are authored, scheduled beats: festivals, fireworks, New Year,
  local markets — data-driven event nodes with windows, locations, NPCs, quests,
  audio, and effects.
- Events are deterministic per schedule; a past event is gone until next year
  (with journal/memory references, not FOMO).
- Events are a content package concern (authorable per region) — no engine
  special-casing.

## Gameplay-relevant interactions

- Swimming/diving gated by season + weather + temperature abstraction (soft).
- Photography conditions (sakura, golden hour, fireworks) make photos
  *collectible moments* — not requirements (a photo of a rainy street is valid).
- NPC availability follows weather/time (indoor NPCs on rainy days).

## Acceptance criteria

1. Same save → same environment state on restore (determinism test).
2. No environment state can prevent completing a required quest (guard test).
3. Seasonal content returns; nothing is permanently missable.
4. Environment systems are data-driven (new seasons/events = content, not code).

## Related

- Audio: [game-audio.md](game-audio.md) · Visuals: `docs/rendering/environment-visuals.md`
- Quests: [quest-system.md](quest-system.md) · Transport: [transportation.md](transportation.md)
- Spec: NODE §117; GAMEPLAY_SYSTEMS §16–§17
