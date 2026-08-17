# Kaiteyo Game — Roadmap (living)

Phases follow the game spec §132. This is the *living* plan — each phase has a
clear exit criterion, and content grows **only after the loop is proven**
(spec §137: a tiny beautiful town beats empty terrain).

Legend: ✅ done · 🔵 next · ⚪ later · 📌 gate (must pass before the next phase)

## Phase 0 — Foundation (spec §132) ✅ (slice-complete)

| Item | Status |
|---|---|
| Engine core: fixed-timestep loop, scenes, entities, spatial hash | ✅ |
| Input action layer + rebinding (keyboard/mouse) | ✅ |
| Save system: versioned, migration hook, autosave | ✅ |
| World model: Region → District → Cell → Object/Location/Station | ✅ |
| Player + movement + collision | ✅ |
| Camera rig (TPP/FPP, zoom, shake, settings) | ✅ |
| Camera collision avoidance | ✅ (`CameraCollision` pushes the follow point out of solid buildings — spec §30) |
| Render backend boundary | ✅ |
| Debug tools + content validation | ✅ |
| Tests for core logic (quest graph, save migration, bindings, knowledge graph, tiles, session) | ✅ |

## Phase 1 — Vertical slice (spec §91, §132) ✅ (Hamanaka)

| Item | Status |
|---|---|
| Seaside town content (data-driven JSON): station, street, beach, aquarium | ✅ |
| NPCs with dialogue (JP/reading/translation, effects) | ✅ |
| Quest chain: walk → read → talk → buy → photograph → travel | ✅ |
| Learning integration: discover → popup → mine → Kaiteyo stats | ✅ |
| Knowledge map menu (visual graph of discoveries) | ✅ |
| In-game dictionary browser (search + real senses/kanji/pitch from Kaiteyo) | ✅ (Dictionary menu; mine / open-in-Kaiteyo per entry) |
| Relationship depth (favorite topics → affinity, People menu) | ✅ (spec §53) |
| Photography: viewfinder, subject tagging, album | ✅ |
| Photo spots (seasonal camera spots that teach) | ✅ (`CameraSpot` beach + spring sakura spot; more are content) |
| Collections (stamps/discoveries) | ✅ |
| Travel: unlock → platform (next region gated honestly) | ✅ |
| Time/weather (day-night, sun/cloud/rain) | ✅ |

**Exit gate 📌:** new player boots → moves → sees something interesting →
interacts → learns Japanese → completes a quest → wants to explore again
(spec §135). Verified by hand in `:desktopApp:run`; covered by
`GameSessionTest` for the logic path.

## Phase 2 — Gameplay depth (spec §91–§120) ✅ (gate passed)

| Item | Status |
|---|---|
| Story UI + story-driven quest chains | ✅ (Story menu, arrival stories, festival chain) |
| Frame-time profiling in the debug overlay | ✅ (rolling window: avg/p95/max + sparkline, spec §94) |
| Multiple named save slots + management UI | ✅ (Saves menu) |
| Gamepad provider (Xbox/PS/generic) + dead zones | ✅ (JNA: XInput/evdev) |
| Gamepad hot-plug + rebind UI (keyboard + gamepad capture) | ✅ (Settings → Controls) |
| Touch provider + contextual touch controls | ✅ (dynamic-origin joystick, look drag, tap; Settings toggle) |
| Dialogue TTS (Kaiteyo kana-clip voice) | ✅ (auto-play + ♪ replay, settings toggle) |
| Listening practice (replay NPC lines) | ✅ (via TTS replay; station announcements still content) |
| More objectives kinds (write kana, listen, learn word) | ✅ (`WriteKana`/`Listen`/`LearnWord` objective kinds, event-driven) |
| Collection objective kind (collect N of an item) | ✅ (`Collect` + `CollectItem` events at every item gain; "Collect two drinks" quest — spec §8) |
| Adaptive learning (never re-teach known words) | ✅ (words studied in Kaiteyo are recognized, not re-taught — spec §73-74) |
| Writing/reading activities (kana + kanji tracing) | ✅ (kana + kanji desks, lenient coverage evaluator, stroke count shown; stroke-order data ⚪) |
| Branching + knowledge-gated dialogue | ✅ (`requiresKnowledge` choices — Hina's festival-lantern branch; kid-mode dialogue variants `kidJp`/`kidReading`, spec §13, §68) |
| NPC schedule expansion (time-of-day presence windows) | ✅ (day-phase windows + weather/season presence gates; evening festival NPCs appear at dusk) |
| NPC patrols (multi-leg routes, pauses) | ✅ (`patrolPoints` loops — beachcomber wanders the shore) |
| Chained NPC routes (time-windowed patrol legs) | ✅ (`NpcRoute`: active leg picked by the clock — bon dancer circles at dusk) |
| Time-gated activities (school morning, stalls evening, closed prompts) | ✅ (`availablePhases` on objects + closed toast) |
| Audio system: SFX + ambient pads + volume settings | ✅ (procedural synth, per-area pads, live sliders) |
| Season cycle + seasonal weather | ✅ (spring/summer/autumn/winter, palette tints, winter snows / spring rains) |
| Menu focus navigation (keyboard + gamepad) | ✅ (`FocusNav` model) |
| Minigames tied to language: ordering at festival stalls | ✅ (order from a Japanese menu; `OrderFood` objective kind) |
| More minigames tied to language (matching signs, train route) | ⚪ |
| Kids mode preset (guided progression, simpler language) | ✅ (pins `Kids` assistance + swaps objects to `kidTargets` vocabulary; Settings → Learning assistance) |

**Exit gate 📌:** controller + touch playable; a second full quest chain; a
writing and a listening activity in the world — **passed** (festival quest
chain is the second chain; kana tracing + TTS replay are the activities).

## Phase 3 — Region expansion (spec §6, §129–§131)

| Item | Status |
|---|---|
| Kamakura region package (station, Komachi-dōri, Hase-dera, Yuigahama beach) | ✅ (playable, data-driven) |
| Train line connecting Hamanaka ↔ Kamakura (Sea Line) | ✅ (real travel, arrival stories) |
| Multiple districts + larger cells | ⚪ |
| Advanced NPC schedules (movement, weather conditions) | ✅ (waypoint walking + chained routes; weather/season presence gates) |
| Seasonal events — all four seasons | ✅ (winter market + candy vendor, spring blossom-viewing, summer bon dance, autumn leaves path) |
| Weather gameplay effects (rain, snow) | ✅ (rain/snow presence gates + seasonal weather bias; rain streaks + snow flakes VFX) |
| Seasonal audio layer | ✅ (per-season ambient colour: birdsong, cicadas, leaves, wind) |
| World map (region/city/district zoom, progressive reveal) | ⚪ |
| Season pacing presets + debug season/weather/time/teleport | ✅ (F3 overlay; clock pacing settings) |

**Exit gate 📌:** travel between two regions with real stations, timetables,
platforms and announcements (spec §47–§48).

## Phase 4 — Advanced (spec §132)

| Item | Status |
|---|---|
| 3D rendering swap at `RenderBackend` (Orx/KorGE or libGDX) | ⚪ (see [ENGINE_DECISION.md](ENGINE_DECISION.md)) |
| World streaming of region packages on demand | ⚪ |
| Seasons (spring/summer/autumn/winter events) | ✅ (all four have events; engine data-driven) |
| Performance tiers (desktop high/mid, mobile mid/low) | ⚪ |
| Accessibility pass (reduced motion, text size, hints — colorblind + simplified controls remain) | 🟡 (reducedMotion, textSizeScale, showHints all live) |

## Phase 5 — Expansion (spec §5–§6, §131)

| Item | Status |
|---|---|
| More prefectures: Tokyo, Kyoto, Osaka, Hokkaido, Okinawa… | ⚪ |
| Photography destination packs, seasonal collectibles | ⚪ |
| Content packages installable/updateable without app releases | ⚪ |
| Community/authoring tools (see [TODO.md](TODO.md) → TOOLS) | ⚪ |

## Guiding rules (re-affirmed)

1. **Loop first, size later** — no 100 km² of empty terrain, ever (spec §137).
2. **One Kaiteyo** — every learning outcome flows to the shared knowledge/
   dictionary/study/stats/mining systems through `GameBridge` (spec §138).
3. **Content is data** — a new region or quest never touches the engine
   (spec §5, §132; see [WORLD.md](WORLD.md)).
4. **Honesty** — nothing is marked done that isn't; this file is updated with
   the code (see [VERTICAL_SLICE.md](VERTICAL_SLICE.md) for the status ledger).
