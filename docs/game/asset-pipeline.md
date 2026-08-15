# The Journey — Asset Pipeline

> **Status**: `ARCHITECTED` (TARGET). The app-side asset system (current) is
> `docs/architecture/assets.md`; this document is the game asset pipeline (MASTER §39,
> STANDARDS §226–§227).

## 1. Pipeline

```
source asset → cleanup → modeling → UV → texture → material → rig → animation
→ LOD → optimization → import → metadata → packaging
```

Each stage has a review gate; an asset that fails a gate returns to the previous stage
(never ships half-done).

## 2. Asset inventory (target)

| Family | Content | Notes |
|---|---|---|
| Characters | Player, NPCs (per occupation/age), child cast | Stylized per RENDERING.md; rigs shared |
| Environment | Buildings, streets, terrain, coast, railway, station, shops, interiors | Modular kits for reuse (MASTER §25) |
| Props | Signs, vending machines, menus, bikes, food, benches, ticket machines | Many are interaction/knowledge nodes (NODE §93) |
| Vehicles | Trains (Enoden), buses, cars, bicycles, boats | Data-driven sims (NODE §105) |
| Textures/materials | Painted-style surface library | Tier-compressed variants |
| Audio | Ambience, music, dialogue, announcements, TTS hooks | Language audio licensing per `docs/legal/` |
| UI/game | HUD tokens, journal, photo frames, map | Reuses Kaiteyo design system |

## 3. Naming, folders, formats

- **Folders**: `assets/game/characters|environment|props|vehicles|textures|audio|ui`
  (mirrors STANDARDS §226 structure).
- **Naming**: `family_subtype_variant` snake_case; LOD suffix `_LOD0..3`; package name
  suffix (`_kamakura`).
- **Formats**: engine-native + source formats preserved (blend/gltf source, textures as
  layered sources + compressed runtime variants); audio WAV/OGG source + compressed.
- **Never scatter** game assets through source directories.

## 4. License & attribution (MASTER §8, STANDARDS §227)

Every asset record: license, attribution, source, ownership, version, checksum.
Rules:

- No unlicensed assets; no "assumed free" content.
- User-supplied branding assets: **copy, preserve source, update references, validate
  dimensions/formats** — never overwrite the only original (STANDARDS §227).
- AI-generated art: policy documented (attribution/ownership clarity); the art direction
  gate rejects low-effort AI output regardless (RENDERING.md).
- Geographic data: lawful sources only (WORLD.md §7).

## 5. Content packages (ADR-0015)

Game assets ship inside **versioned content packages** (`world:kamakura:v1.0`):
- Package manifest: schema version, asset list + checksums, license block, locale.
- Validation gates (6 hard gates, `CONTENT_AUTHORING.md`) run at build and at install.
- Packages update independently of the app (NODE §90) — a new region is a package, not
  an app release.

## 6. Optimization rules

- LODs mandatory for anything larger than a prop (LOD0-1 minimum).
- Texture budgets per tier (Low ≤ 2K, High ≤ 4K, Ultra ≤ 8K with streaming).
- Draw calls bounded per cell; materials batched; no per-object shader copies.
- Audio: streaming for music/ambience; pooled for SFX; language audio on demand.

## Related

- Game overview: [game-overview.md](game-overview.md) · World: [world-architecture.md](world-architecture.md) · Rendering: `docs/rendering/`
- App asset system: `docs/architecture/assets.md`
- Packages: `docs/architecture/nodes/CONTENT_AUTHORING.md`
- Legal: `docs/legal/`
