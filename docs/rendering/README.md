# Kaiteyo Rendering (Journey — target)

**Status**: TARGET — artistic direction + rendering architecture for the Journey
world. No rendering code exists yet (engine selection pending STANDARDS §242).
The Kaiteyo application itself renders via Compose Multiplatform (see
`docs/architecture/OVERVIEW.md`); this directory is about the **world's** 3D/2D
rendering layer.

## Document map

| Document | Covers | Key spec refs |
|---|---|---|
| [rendering-architecture.md](rendering-architecture.md) | Artistic direction, tech approach, toon/stylized pipeline, LOD/occlusion/instancing | §31, §367 |
| [environment-visuals.md](environment-visuals.md) | Weather, water, vegetation, particles, lighting, sky — simulation vs visual | §30, §31 |
| [rendering-performance.md](rendering-performance.md) | FPS/memory/loading budgets, draw calls, platform tiers, dynamic resolution | §44, §143 |

## Ground rules

1. **Stylized, not photoreal, not cheap.** Target: simple enough to be
   performant, stylized enough to be recognizable, detailed enough to feel
   premium — Nintendo-like accessibility, cinematic atmosphere, Japanese
   environmental authenticity (§31).
2. **No expensive technique without a measured reason** (§31, §367): evaluate
   GI/baked lighting/dynamic lighting, and prefer the cheapest option meeting the
   visual bar.
3. **Same world data, different presentation budgets** across platform tiers
   (JOURNEY_RUNTIME_SPEC §10).
4. **Engine-agnostic direction**: these docs specify *what* the world must look
   like and *which techniques are acceptable*; the concrete renderer follows the
   engine evaluation ADR (§242).
5. All proposed performance numbers are **targets to be measured** (§188), never
   shipped facts.

## Related

- Camera/LOD interplay: `docs/game/camera.md`, `docs/game/world-streaming.md`
- Asset pipeline: `docs/architecture/assets.md`, `docs/assets/ASSETS.md`
- Performance: `docs/architecture/performance.md`
- Game philosophy: `docs/vision/game-philosophy.md` (§31 of the expansion spec)
