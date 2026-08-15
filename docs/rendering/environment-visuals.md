# Environment Visuals

**Status**: TARGET (direction). **Source**: expansion spec §30 (environment) and
§31 (graphics); NODE §117; `docs/game/environment-simulation.md` (the state model
this renders).

## Principle

Environment visuals render the **deterministic environment state** (clock,
weather, season) from `environment-simulation.md`. They are *presentation*: they
never create gameplay state and never fight the simulation's determinism.

## Sky & lighting

| Element | Behavior |
|---|---|
| Day cycle | Sun position + sky color curve from world clock; golden-hour composition at sunrise/sunset (photography moments — `docs/game/collectibles-photography.md`) |
| Night | Moon, stars, streetlights/building lights (authored, deterministic with clock) |
| Weather skies | Cloud cover variants (clear/partly/overcast/storm), fog state, snow tint |
| Interior lighting | Baked ambient + authored lamps; windows react to outside time/weather (deterministic) |

## Weather visuals (render layer of the weather simulation)

- Rain: particles + wet surface response (puddle reflection, darker asphalt —
  authored roughness variation, not a global "wet shader" hack)
- Snow: sparse, seasonal only, settles on rooftops in winter (LOD-billboard at
  distance)
- Fog: distance haze affecting LOD pop-in (fog hides streaming transitions — a
  *feature*, §31)
- Wind: vegetation sway + particle direction (visual only; deterministic with
  weather seed)
- Storm: heavier rain, dark sky, lightning **without strobe** (slow, distant,
  accessibility-safe)

Rules: weather visuals are deterministic from (clock, seed, season); transitions
are authored curves (no weather popping mid-frame).

## Water (ocean, beaches, lakes, rivers)

- Stylized layered water: base color + normal/specular animation + shoreline
  foam; wave state from ocean simulation (calm/rough/storm).
- Beach: tide line, wet sand band, seasonal swim zone visuals (lifeguard tower,
  closed-flag state).
- Underwater presentation: distinct tone/color grading, light shafts, bubble
  trail, motion damping (see `docs/game/camera.md` underwater camera).
- Determinism: wave/foam parameters derive from weather+wind state; a stormy
  beach looks stormy because the simulation says so.

## Vegetation

- Instanced trees/grass/bushes with authored LOD (billboard at distance);
  collision via cell nav/collision (never rendered geometry for gameplay).
- **Season-swappable**: sakura in spring, green in summer, autumn leaves, bare
  winter — content-driven (season state picks the variant), never a shader hack
  alone.
- Wind sway is visual only and deterministic.

## Particles & post

- Particles: rain, snow, petals (spring), confetti (festival), steam (food),
  dust — authored, budgeted, instanced where possible.
- Post-processing (minimal, authored): tone mapping, subtle bloom at night,
  vignette in cinematic beats, no default grain, no motion blur (accessibility).
- Reduced motion: disables particle storms, camera-relative effects, non-essential
  world animation (`docs/vision/design-philosophy.md`).

## Street & building lights

- Data-driven light nodes (lamp posts, shop signs, windows) — deterministic
  on/off with clock + weather (overcast days brighten windows).
- Light baking for interiors; dynamic lights limited per cell (budget in
  `rendering-performance.md`).

## Traffic & life ambience (visual)

- Sparse vehicles/pedestrians at distance are ambience only (no traffic
  gameplay); they respect the deterministic schedule/weather where authored.

## Acceptance criteria

1. Every environment visual derives from the deterministic simulation state —
  no random generation (tests: same state → same frames in golden-master shots).
2. Weather/season transitions are seamless (no popping).
3. Reduced motion + color-blind palettes are honored in all effects.
4. Vegetation/water/particles respect per-tier budgets without removing
  recognizability.

## Related

- State model: `docs/game/environment-simulation.md`
- Rendering tech: [rendering-architecture.md](rendering-architecture.md)
- Budgets: [rendering-performance.md](rendering-performance.md)
- Camera: `docs/game/camera.md` (underwater/photo)
