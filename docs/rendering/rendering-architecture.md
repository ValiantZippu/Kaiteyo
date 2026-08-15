# Rendering Architecture

**Status**: TARGET (direction). **Source**: expansion spec §31; STANDARDS §367
(use the engine's pipeline). Engine choice is an open decision (STANDARDS §242 —
must be evaluated and recorded as an ADR before implementation).

## Artistic direction

**Goal**: stylized, premium, Japanese-authentic — never cheap, never
photoreal-dependent, never generic.

| Quality bar | Requirement |
|---|---|
| Performance | Simple enough for mobile mid-tier at 30 FPS (budgets in `rendering-performance.md`) |
| Recognizability | Stylized enough that Kamakura is Kamakura (real layout, iconic landmarks) |
| Premium detail | Crafted materials, good lighting composition, intentional color |
| Accessibility | Clear silhouettes, color-blind-safe palettes, no strobe, reduced-motion respected |
| Atmosphere | Cinematic light (golden hour), weather mood, quiet beauty |

**Anti-direction**: low-poly-with-flat-colors (too cheap), photoreal PBR-everything
(too expensive, uncanny), Roblox-like default materials, generic asset-store
cities, dark/busy UI in the world.

## Tech approach (to evaluate per engine, §242)

| Concern | Recommendation (proposed, verify against engine) |
|---|---|
| Geometry | **Mid-poly stylized**: clean silhouettes, low-detail hero props, authored LODs (L0 proxy → L4 hero). No photogrammetry by default (representation > replication — `docs/game/world-architecture.md`) |
| Materials | **Toon/stylized hybrid**: lit toon shading (ramps) + light PBR accents (metallic/roughness only where they read well: water, metal, glass). Color-graded, authored, not raw PBR |
| Lighting | **Baked GI/lightmaps for static scenes** + limited dynamic lights (time-of-day sun, interiors, night windows). Real-time GI only where the engine makes it cheap; measure first (§31) |
| Shadows | One dynamic shadow-casting light (sun/moon) + baked shadows; contact shadows for near objects |
| Post | Minimal, authored: tone mapping, subtle bloom (night), vignette in cinematic moments; **no film grain default**, no motion blur (accessibility) |
| Sky | Procedural sky with authored weather variants; sun/moon position from world clock (`environment-simulation.md`) |
| Water | Stylized layered water (see `environment-visuals.md`) |
| Vegetation | Instanced, LOD-billboards at distance; season-swappable (see `environment-visuals.md`) |

## LOD & streaming (relationship to `docs/game/world-streaming.md`)

- LOD is **presentation**, content stays identical: proxy (collision+silhouette+
  label+ambient audio) → mid → full per cell.
- LOD selection driven by distance + screen-space error + platform tier budget.
- Texture streaming at LOD resolution (mips); mesh streaming proxy-first
  (no pop-in voids — a proxy cell is *intentional* presentation, not failure).

## Occlusion & instancing

- Occlusion culling per cell (engine-provided where available) to keep draw calls
  bounded (budget: `rendering-performance.md`).
- Instancing for vegetation, crowds (NPC far-tier), props, and stationery —
  the world has lots of repetition; instancing is how it stays cheap.
- Frustum culling is mandatory at every tier.

## Draw-call & memory strategy

- Per-cell material/texture atlasing; shared materials across cells (no
  per-object materials for common props).
- Budget-guided streaming (ASSET STREAMER in `world-streaming.md`): never load
  an asset the tier won't render.
- Dynamic resolution as the last-resort lever on low tiers (render scale
  downscales before dropping content).

## Acceptance criteria (visual bar)

1. The Kamakura slice at desktop high-end reads as *premium and Japanese* — a
   reviewer cannot mistake it for a generic tutorial world.
2. Mobile low tier holds its budget while remaining recognizable (LOD + dynamic
   resolution, never content removal).
3. No technique ships without a measured budget check (§188–§190).

## Related

- World representation: `docs/game/world-architecture.md` (L0–L4, real vs
  gameplay data)
- Streaming: `docs/game/world-streaming.md`
- Budgets: [rendering-performance.md](rendering-performance.md)
- Visuals: [environment-visuals.md](environment-visuals.md)
