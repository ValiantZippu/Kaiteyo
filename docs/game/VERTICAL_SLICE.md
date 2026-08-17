# Kaiteyo Game — Vertical Slice Status

**Honest status of the game subsystem today.** Per the game spec §141, nothing
is claimed complete unless it works. Status legend:

- **IMPLEMENTED** — works in the slice (run `./gradlew :desktopApp:run`, open **Game**)
- **PARTIALLY IMPLEMENTED** — real code, a defined subset works
- **ARCHITECTED** — interfaces/data/structures exist, behavior not wired
- **PLANNED** — designed (docs) but not built

## What the slice is

Two connected Japanese summer towns — **Hamanaka** (浜中), the starter seaside
town, and **Kamakura** (鎌倉), the historic second region — with stations, a
shopping street, temples, beaches, signs and objects that carry real Japanese,
NPCs, a starter quest chain (walk → read → talk → buy → photograph → travel →
arrive), stories, photography (with a review panel that can save photos to
disk), collections, a knowledge map, real train travel between regions, save
slots, time/weather, a procedural audio system (SFX + ambient pads), an
evening festival that only appears at dusk (with a Japanese-menu ordering
minigame), NPCs that walk their schedules **and patrol chained routes**, a
season cycle that changes the palette, the weather (winter snows, spring
rains) **and the ambient audio** (birdsong, cicadas, leaves, wind), a
**Kamakura night** quest chain under the lanterns, in-world kana
writing practice, **seasonal events for all four seasons** (winter market,
spring blossoms, summer bon dance, autumn leaves), and full Kaiteyo
integration (discover → mine → stats).

It is deliberately **tiny beautiful towns, not empty terrain** (spec §137).

## Per-system status (spec §140 checklist)

| System | Status | Notes |
|---|---|---|
| Real game architecture (not gamification) | IMPLEMENTED | Fixed-timestep engine core, scenes, entities, spatial hash |
| Player exists in a world | IMPLEMENTED | Spawns in the town, collides with tiles/objects |
| Player can move | IMPLEMENTED | WASD / arrows, acceleration, run, no sliding |
| Camera works | IMPLEMENTED | Follow + first-person modes, zoom, shake, bounds |
| Keyboard + mouse | IMPLEMENTED | `ControlScheme` bindings, mouse-look in FPP/photo |
| Controller architecture | IMPLEMENTED | JNA provider: XInput (Windows) + evdev joystick (Linux), Xbox/PS/generic layouts, dead zones, rebinding (spec §14-16) |
| Gamepad hot-plug | IMPLEMENTED | Linux joystick auto re-probes `/dev/input/js*` every second; Windows XInput polls continuously — plug in anytime |
| Menu navigation | IMPLEMENTED | `FocusNav` keyboard/gamepad focus model — menus are fully playable without a mouse |
| Control rebind UI | IMPLEMENTED | Settings → Controls: capture a keyboard key or gamepad button per action, persisted with the journey |
| Touch architecture | IMPLEMENTED | `VirtualTouchProvider` + dynamic-origin joystick, look drag, tap-to-interact, contextual buttons (Settings toggle) |
| Spoken dialogue (TTS) | IMPLEMENTED | Kana-clip voice through Kaiteyo TTS: auto-play per line, replay button, listening counts as activity |
| Character exists | IMPLEMENTED | Stylized 2.5D player (drawn avatar), cosmetics data model |
| Interaction system | IMPLEMENTED | `IInteractable`-equivalent, prompts, behaviors, world/NPC objects |
| NPC architecture | IMPLEMENTED | Definitions, entities, day-phase schedules, **waypoint walking + patrol loops + chained routes** (`NpcRoute` time-windowed legs), weather/season presence gates, relationships |
| Dialogue system | IMPLEMENTED | JP/reading/translation, learning targets, effects, quest grants |
| Quest architecture | IMPLEMENTED | Prereq graph, typed objectives (incl. write-kana, listen, learn-word, order-food, **season, weather**), events, rewards, auto-guidance |
| Story architecture | IMPLEMENTED | `StoryEngine` + Story menu (chapters/scenes/effects); arrival stories guide new regions; festival story chain |
| Learning nodes | IMPLEMENTED | Knowledge nodes (word/kanji/phrase/**sentence/grammar**) discovered from world/dialogue/objects/photos/quests |
| In-world writing activity | IMPLEMENTED | Kana tracing panel + lenient stroke-order evaluator (kana desk object, `WriteKana` quest objective) |
| World nodes | IMPLEMENTED | Region→District→Cell→Object/Location/Station model |
| Knowledge graph connects to game | IMPLEMENTED | `KnowledgeGraph` + typed edges, Knowledge Map menu |
| Dictionary connects to game | IMPLEMENTED | `GameBridge` → `KaiteyoBridge` real entry data (senses + POS, kanji on/kun/strokes/radicals) rendered in the discovery card; full in-game dictionary browser is PLANNED |
| Adaptive learning (skip already-known words) | IMPLEMENTED | Words studied in Kaiteyo are recognized in the world, never re-taught — no popup, no double count, quests still progress (spec §73-74) |
| Kids mode | IMPLEMENTED | Settings toggle pins `Kids` assistance (full reading + translation + simplified support), swaps objects to their simpler `kidTargets` vocabulary layer (vending machine: 水/お茶/ジュース instead of 飲み物) and swaps dialogue to authored kid text (`kidJp`/`kidReading`) — welcome + beach-sign carry all-kana variants (spec §7, §68-69) |
| Branching dialogue | IMPLEMENTED | Welcome yes/no + Hina's festival-lantern dialogue: three real outcomes, one **knowledge-gated** choice (花火 appears only once はなび is discovered — spec §13); a line whose choices are all gated auto-continues instead of soft-locking |
| Story choices | IMPLEMENTED | The festival fireworks scene branches: watch to the end (moon-walk scene, 月 granted, `festival-finale` flag) or head home early (`festival-early`, story ends) — branches are data (`StoryChoice`: jump/quest/grants/flag) with a validator (spec §55) |
| Camera collision | IMPLEMENTED | `CameraCollision` resolves the follow point against solid buildings — pure, unit-tested, wired into the session (spec §30) |
| In-game dictionary | IMPLEMENTED | Dictionary menu: every world word searchable, with real senses, kanji on/kun/strokes/radicals and pitch accents (平板 / accent n) from Kaiteyo's dictionary; mine / open-in-Kaiteyo per entry (spec §17, §63) |
| Relationship depth | IMPLEMENTED | NPC favorite topics feed an `affinity` score when dialogue touches them; People menu shows met/talked/♥ per NPC (spec §53) |
| Kanji tracing | IMPLEMENTED | Kamakura writing desk teaches 駅/海/町 through the lenient coverage evaluator; real stroke count shown from Kaiteyo's dictionary; stroke-order verification waits for stroke data (honest ⚪) |
| Accessibility settings do things | IMPLEMENTED | `showHints` gates tutorial guidance, `reducedMotion` suppresses camera shake, `textSizeScale` scales dialogue/discovery/quest text — every toggle now has real behavior |
| Collection quests (collect N) | IMPLEMENTED | `Collect` objective kind + `CollectItem` events at every item gain; "Collect two drinks" quest ships in content |
| Mining connects to game | IMPLEMENTED | Discovery popup "Mine" → real card pool via `MiningEngine` |
| Stats connect to game | IMPLEMENTED | Activity log, discovery/location/photo/quest stats via `GameBridge` |
| Save system | IMPLEMENTED | Versioned, migration hook, autosave, multiple named slots + management UI |
| World is data-driven | IMPLEMENTED | All content JSON, validated at load — **two regions now** (Hamanaka + Kamakura) |
| Content is data-driven | IMPLEMENTED | Regions/quests/NPCs/dialogue/knowledge/collectibles/stories/audio |
| Time-gated world | IMPLEMENTED | `availablePhases` on objects (school morning, stalls evening); evening festival appears at dusk, closes at night |
| Audio system | IMPLEMENTED | Procedural SFX synth (discovery, quest, photo, interact, closed) + per-area ambient pads; master/music/sfx volumes live |
| Season cycle | IMPLEMENTED | Spring/summer/autumn/winter (3-day seasons), palette tints, weather bias (winter snows, spring rains), season/weather-gated NPCs + objects |
| Seasonal events — all four | IMPLEMENTED | Winter market + candy vendor (Hamanaka), spring blossom-viewing (Kamakura 桜), summer bon dance (evening beach circle), autumn leaves path (Kamakura 紅葉) — data-driven |
| Seasonal audio | IMPLEMENTED | Ambient pad coloured per season: birdsong, cicadas, dry leaves, winter wind (`GameAudio.setSeason`) |
| Weather VFX | IMPLEMENTED | Rain streaks + **snow flakes** (soft drifting particles, spec §41) |
| Kamakura night | IMPLEMENTED | Night quest chain: Lantern Keeper, stone lantern inspection, moon photo (data-driven) |
| Debug tools | IMPLEMENTED | F3 overlay: season/weather/time forcing + **teleport to discovered locations** (spec §121-122) |
| Quest log categories | IMPLEMENTED | Filter chips by quest category (Exploration, Vocabulary, Social…) alongside All/Active/Completed |
| Clock pacing | IMPLEMENTED | Time setting: fast / standard / real-time (spec §40), applied live |
| Ordering minigame | IMPLEMENTED | Festival stalls have Japanese menus (たこ焼き/ラムネ/たい焼き/焼きそば); order by Japanese, `OrderFood` quest objective |
| Quest log + HUD | IMPLEMENTED | Filter chips (All/Active/Completed), per-quest progress bars, HUD objective tracker with progress counts + first-run hint |
| New locations without engine rewrites | IMPLEMENTED | `world.json` + `knowledge.json` only (see [WORLD.md](WORLD.md)) |
| New quests without engine rewrites | IMPLEMENTED | `quests.json` objective kinds cover the slice |
| New learning content without engine rewrites | IMPLEMENTED | `knowledge.json` nodes/edges |
| First/third-person architecture | IMPLEMENTED | `CameraMode` toggle; FPP used for photo mode |
| Photography exists architecturally | IMPLEMENTED | Viewfinder, focus/zoom, subject tagging, album, **photo review panel (tags + save-to-disk + delete)**, photo quests |
| Travel architecture | IMPLEMENTED | Real cross-region travel: board the train, arrive in Kamakura, region-scoped NPCs, arrival stories |
| World streaming architecture | ARCHITECTED | `WorldStreamer` cell-set; region streaming PLANNED |
| Performance architecture | PARTIALLY IMPLEMENTED | Spatial hash, cell-local rendering, fixed timestep; profiler PLANNED |
| Debug tools | IMPLEMENTED | Overlay (FPS/cells/entities), teleport, content reload, noclip |
| Content validation | IMPLEMENTED | Cross-reference validator, hard fail on broken content |
| Game can start with a vertical slice | IMPLEMENTED | Hamanaka boots, autosaves, continues; Kamakura reached by train |
| Scales toward a representation of Japan | ARCHITECTED | Region/cell/package model + L0–L4 fidelity docs |

## How to run it

```bash
./gradlew :desktopApp:run
```

Then open the **Game** destination (nav rail, or the `open-game` command).
Controls (defaults, rebindable in the game Settings menu):

| Action | Key |
|---|---|
| Move | WASD / arrows (gamepad: left stick / DPad) |
| Run | Shift (gamepad: right trigger / stick rim on touch) |
| Interact | E (gamepad: A — touch: tap / button) |
| Photo mode | C (gamepad: X) |
| Camera switch (TPP/FPP) | V (gamepad: Back) |
| Zoom | Mouse wheel |
| Map / Quests / Collection / Menu | M / J / L / Esc |
| Debug overlay | F3 |

Every binding is rebindable in **Game Settings → Controls** (keyboard keys
and gamepad buttons both capture); touch has its own toggle under **Touch**.

## Known slice limitations (honest)

- The world renders as **stylized 2.5D top-down** through the Compose Canvas
  backend; 3D is a documented swap at `RenderBackend` (see
  [ENGINE_DECISION.md](ENGINE_DECISION.md)).
- Touch controls are implemented and toggleable in Settings, but the desktop
  target has no touch screen to exercise them on — the wiring is real and
  unit-tested; a touch device run is the remaining verification.
- TTS speaks **kana** (the line's reading) through Kaiteyo's kana-clip voice;
  kanji-heavy lines fall back to the reading field. Voice asset absence is
  handled gracefully (dialogue stays silent, UI still works).
- Dictionary enrichment resolves headwords through the bridge but the in-game
  dictionary entry popup is minimal (discovery card + mine).
- Kamakura is content: its cells/NPCs/quests are JSON — the engine already
  supports more regions the same way.
- Audio is **procedural** (synthesized SFX + ambient pads) — no recorded
  music/voice assets yet; TTS voice still depends on Kaiteyo's voice data.
- Kana tracing works with mouse/touch-drag input; pen pressure/tablet
  fidelity and kanji tracing (stroke-order data) are follow-ups.
- NPC schedules gate *presence* by day-phase windows; movement between
  waypoints is still PLANNED.

Full backlog: [TODO.md](TODO.md). Order of attack: [ROADMAP.md](ROADMAP.md).
