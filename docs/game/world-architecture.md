# World Architecture

**Status**: TARGET (spec). **Source**: expansion spec §4; NODE §88 (world hierarchy);
`docs/architecture/nodes/JOURNEY_WORLD_SCHEMA.md` (data contract).

## Principle

The world represents Japan in a **stylized, recognizable, scalable** way. It does
NOT attempt centimeter-perfect reconstruction of Japan. The world grows
incrementally through **independently loadable content packages** (§72).

## World hierarchy (§88)

```
world → region → prefecture → city → district → map cell → location → interior → interaction node
```

Every level is a node with `worldId`-scoped identity and `contains_location`/`parent`
edges. Every place is addressable by path:

```
world/japan/kanagawa/kamakura/komachi/cell-07/shop-14
```

| Level | Node | Contents |
|---|---|---|
| world | `world` | metadata, region index | 
| region | `region` | summary, bounds, city index |
| prefecture | `prefecture` | summary, bounds, city index |
| city | `city` | summary, bounds, district index, map overlays |
| district | `district` | geometry, cell grid, POI index |
| map cell | `map_cell` / `neighborhood` | terrain, geometry, NPCs, objects, audio, lighting, nav, interactions, knowledge nodes, quest nodes |
| location | `location` | buildings, shops, shrines, stations, interiors |
| interior | `interior` | rooms, spaces inside a location |
| interaction node | `interaction` | signs, objects, seats, doors, counters — the things you can act on |

## Real-world data vs gameplay representation

Two separate layers, never conflated (expansion §4; NODE §249–§250):

| | REAL-WORLD DATA | GAMEPLAY REPRESENTATION |
|---|---|---|
| What | Geographic truth: streets, rail lines, landmark positions, names | Authored art: stylized terrain, buildings, signs, props |
| Source | Open datasets (OpenStreetMap-derived geometry where licensed), official/community research | Content packages authored by Kaiteyo designers (ADR-0015) |
| Purpose | Recognition + grounding | Playable beauty |
| Rendering | Never rendered directly | Rendered as art |
| Rule | "Recognizable, never a developer map" (§89) | Never satellite/GIS dump; never placeholder geometry |

Consequence: the game world is **not a mirror of reality** — it is a curated,
stylized, playable interpretation. Layouts are recognizable (real Kamakura streets,
the Enoden line, Yuigahama beach), but every building is authored content.

## Geographic fidelity levels (L0–L4)

The world can exist at different fidelity levels at once — a slice at L4 sits inside
a country at L0–L1. This is the incremental-growth mechanism (§4):

| Level | Name | What exists | Effort | When used |
|---|---|---|---|---|
| **L0** | Abstract educational map | Stylized region blobs, major cities, railway spines, labels | minimal | World map / overview; undiscovered regions |
| **L1** | Recognizable regional map | Region/prefecture shapes, major landmarks, route lines | low | Region view; travel planning |
| **L2** | Street/network accuracy | Real street layout, stations, district boundaries, POI placement | medium | City/district maps; walking areas |
| **L3** | Detailed landmark recreation | Key landmarks modeled (shrines, stations, aquarium), exteriors | high | Playable location shells |
| **L4** | High-detail playable location | Full interiors, interactive objects, NPCs, knowledge nodes, quests, audio | highest | The vertical slice (§91) |

**Rules**

1. A region's presentation level is a property of *content available*, not a
   runtime switch. Unavailable detail shows as intentional "undiscovered" — never
   broken, never fabricated (§118).
2. Fidelity upgrades are content package updates (v2 region package, etc.), not
   code changes.
3. Performance tiers (JOURNEY_RUNTIME_SPEC §10) render the *same* content at
   different budgets — LOD is presentation, not content.

## Lawful geographic data (MASTER §24)

Real-world data is used **only from lawful sources**: OpenStreetMap (ODbL —
attribution + share-alike), public GIS / government datasets, licensed geographic
data, open elevation data. **Never scrape proprietary mapping data** (MASTER §83).
Every source passes the dataset verification checklist in `docs/data/SOURCES.md`
(license, attribution, redistribution, modification, commercial use). Accuracy is
balanced against performance and art: the L0–L4 model above is the mechanism.

## Locations & interiors

Location catalog (expansion §4) — a taxonomy of authorable locations, each with
data hooks (see `JOURNEY_WORLD_SCHEMA.md`):

- **Commercial**: shopping districts, restaurants, convenience stores (konbini),
  shops, markets, aquarium, museums
- **Civic**: schools, libraries, post offices, city hall, stations
- **Faith & culture**: temples, shrines, festivals grounds
- **Natural**: beaches, mountains, rivers, lakes, parks, forests, ocean
- **Transport**: stations, platforms, bus stops, crossings
- **Residential**: neighborhoods, houses, apartments
- **Fictional educational spaces**: study rooms, language labs, "word gardens"
  — spaces that exist to teach, clearly fictional so they don't misrepresent Japan

Every location: `id`, `worldId`-scoped path, type, bounds, entrances, interiors,
POI list, NPC list, knowledge-node list, quest-node list, audio refs, lighting refs,
nav refs, fidelity level, status (available in which package).

Interiors are themselves loadable spaces with their own cells/nav/audio; entering
one triggers an interior load, exiting unloads (see `world-streaming.md`).

## Content packages (§72, ADR-0015)

The world ships as versioned, installable packages:

```
japan.base            — world map L0–L1, regions index, shared systems
kanagawa.kamakura     — L2–L4 slice: Kamakura + Enoshima (the first slice)
japan.kanto           — L1 region extension (travel between cities)
tokyo.city            — L3–L4 city package (future)
kyoto.city            — future
osaka.city            — future
hokkaido.region       — future
```

Package rules:

1. Each package declares dependencies (`kanagawa.kamakura` → `japan.base`).
2. Packages are validated by the content pipeline (ADR-0015 gates: schema,
   naming, references, localization, size budgets).
3. Installed packages stream cells on demand (`world-streaming.md`); uninstalled
   regions show L0/L1 style with a "available in Settings → Journey" note.
4. Versioned: updates are new package versions with migration-safe paths; saves
   reference package versions.
5. Localization and assets ship inside the package (no global asset bloat).

## The first slice: Kamakura + Enoshima (§91)

| Piece | Real anchor | Playable content |
|---|---|---|
| Komachi-dōri | shopping street | shops, signs, street food, NPC shopkeepers |
| Tsurugaoka Hachimangū | shrine | grounds, torii, festival quest |
| Hase-dera | temple | gardens, observation, statues |
| Yuigahama beach | beach | swimming, lifeguard NPC, seasonal |
| Enoshima island | island | aquarium (Enoshima Aquarium), summit, caves |
| Enoden line | railway | stations (Kamakura–Fujisawa), timetables, boarding |

The slice's exit criteria (§91, TEST_PLAN §13): movement, camera, interaction,
dictionary, language node, NPC, dialogue, quest, discovery, photography,
collection, knowledge, stats, save/load, performance — all proven.

## Related

- Data contract: `docs/architecture/nodes/JOURNEY_WORLD_SCHEMA.md`
- Fidelity & representation: NODE §88, §249–§250
- Packaging: `docs/architecture/nodes/CONTENT_AUTHORING.md` (ADR-0015)
- Streaming: [world-streaming.md](world-streaming.md)
- Map surface: [map-system.md](map-system.md)
