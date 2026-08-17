# Kaiteyo Game — TODO (spec §133)

The massive, categorized backlog. Status: ✅ done in the slice · 🔵 next ·
⚪ later. Items are honest: architecture-only items say so. Work through
[ROADMAP.md](ROADMAP.md) phases; this file is the full category index.

## ENGINE

- ✅ Fixed-timestep loop (`GameLoop`) + accumulator
- ✅ Scene interface + scene manager (world scene active)
- ✅ Entity registry + spatial hash
- ✅ Engine time (fixed delta, accumulated, paused)
- ✅ `RenderBackend` abstraction + Compose Canvas backend
- 🔵 Physics: platformer/AABB vs tiles exists; add entity-entity response,
      one-way platforms, slopes
- ⚪ 3D render backend (Orx/KorGE or libGDX) behind the same boundary —
      see [ENGINE_DECISION.md](ENGINE_DECISION.md)
- ⚪ Animation system (sprite flipbooks, tweened transforms) for entities
- ✅ Procedural audio system (see AUDIO): SFX synth + ambient pads, volume settings
- ⚪ Asset manager / texture atlas loader
- ⚪ Coroutine-driven async content loads (region streaming)

## WORLD

- ✅ World → Region → District → Cell → Object/Location/Station model
- ✅ Tile grid (ground/decoration, collision)
- ✅ Object placement + Japanese labels + interactable ids
- ✅ Kid-mode vocabulary layer (`WorldObject.kidTargets` — the same object teaches simpler words in kid mode; spec §7, §68)
- ✅ Location discovery (anchor + radius + learning targets)
- ✅ Travel network (station edges, unlock flags)
- ✅ World content loader (JSON) + indexes
- ✅ Two regions: Hamanaka + Kamakura (content-driven)
- 🔵 More regions (Kyoto, Tokyo district, Osaka… — content work, engine ready)
- 🔵 Region/cell streaming (`WorldStreamer` exists; stream on demand)
- ⚪ Interiors (rooms inside locations, load/unload)
- ⚪ Day/night lighting pass + time-of-day tints beyond the current overlay
- ✅ Seasons (spring/summer/autumn/winter cycle + palette tints + gated NPCs/objects + `Season`/`Weather` quest objectives)
- ✅ Seasonal events: winter market + spring blossom-viewing + summer bon dance + autumn leaves — all four seasons have events (data-driven, season-gated)
- ✅ Debug: one-click season/weather/time forcing + teleport to discovered locations (F3 overlay, spec §121-122)
- ✅ Clock pacing presets (fast / standard / real-time, spec §40)
- ✅ Snow: soft falling-flake particles (weather VFX, spec §41) — rain lines + snow flakes
- ✅ Seasonal audio: birdsong/cicadas/leaves/wind ambient layer (`GameAudio.setSeason`, see AUDIO)
- ⚪ Weather beyond sun/cloud/rain/snow (fog, storms)
- ⚪ Full seasonal-event breadth (a festival row or hanami market with multiple stalls is more data, not more engine)
- ⚪ L0–L4 fidelity presentation tiers (see world-architecture.md)
- ⚪ Installed content packages (versioned, downloadable regions)

## PLAYER

- ✅ Player entity + state (position, items, xp, cosmetics)
- ✅ Movement: acceleration, deceleration, run, no sliding
- ✅ Tile + solid-object collision
- ✅ Active-time accounting (only movement/interaction accrues)
- 🔵 Jump (already a mapped action; needs ground-check + animation)
- ⚪ Animations: walk/run/idle/sit/interact/photo poses
- ⚪ Swimming (only where geographically sensible — beach)
- ⚪ Riding (train seats, bicycle)
- ⚪ Emotes/gestures
- ⚪ Character customization applied to the avatar (cosmetics exist as data)

## CAMERA

- ✅ Follow rig (TPP) with smoothing + zoom + bounds
- ✅ First-person mode (mouse-look) — used by photo mode
- ✅ Camera shake (decay) — wired to photo/discovery/purchase, suppressed by reduced motion
- ✅ Settings: sensitivity, invert Y, smoothing, zoom bounds, default mode
- ✅ Camera collision avoidance (spec §30) — `CameraCollision.resolve` pushes the follow point out of solid buildings (pure + tested), rig overload wired in the session
- 🔵 FOV/exposure controls exposed in Settings UI
- ⚪ Cinematic cameras (dialogue close-ups, quest cutscenes)

## INPUT

- ✅ `InputAction` abstraction (game code never checks raw keys)
- ✅ Keyboard/mouse providers
- ✅ Rebinding model (`ControlScheme`, persisted)
- ✅ Edge-trigger (wasPressedThisFrame) + held-axis state
- ✅ Action set: Move, Look, Interact, Run, Jump, Photo, Camera, Zoom, Map,
      Quests, Collection, Menu, Back, Debug
- ✅ JNA gamepad provider: XInput (Windows) + evdev joystick (Linux), Xbox/PS/generic layouts, dead zones
- ✅ Gamepad hot-plug (auto re-probe) + rebind UI (keyboard + gamepad capture in Settings → Controls)
- ✅ Touch provider: virtual stick, look gesture, contextual buttons (dynamic-origin joystick, Settings toggle)
- ⚪ Per-device sensitivity + dead-zone settings UI

## CONTROLLER

- ✅ Gamepad action mapping in `ControlScheme` (defaults + rebind UI)
- 🔵 Haptic feedback hooks
- ✅ Controller UI navigation (focus model for menus — `FocusNav`, gamepad-driven)
- ⚪ DualSense/Switch pro layout variants

## TOUCH

- ✅ Virtual movement control (dynamic-origin joystick — appears where you touch)
- ✅ Look drag + tap-to-interact gestures
- ✅ Contextual action buttons (never a 20-button wall)
- ⚪ Touch rebinding + sensitivity (sensitivity field exists in calibration)

## NPC

- ✅ Definition model (name, appearance, dialogue, schedule, knowledge)
- ✅ Entities + spawn into registry
- ✅ Relationships (met, talked, helped)
- ✅ Schedule tick (day-phase behavior)
- 🔵 Schedules with time/weather conditions (currently day-phase + weather/season presence gates)
- ✅ NPC movement: waypoint walking (velocity-based, faces the walk direction, arrives → idles)
- ✅ NPC patrols: multi-leg patrol loops with pauses (`patrolPoints` + `patrolPauseSeconds`)
- ✅ Chained routes: time-windowed patrol legs (`NpcRoute` — the active leg is picked by the world clock; e.g. the bon dancer's evening circle)
- ⚪ Appearance rendering variants
- ✅ Relationship depth — `favoriteTopics` on NPC definitions; talking about a topic deepens `affinity`; People menu lists who you've met and what they love (quest history UI remains ⚪)

## DIALOGUE

- ✅ Script model: JP, reading, translation, audio key, learning targets
- ✅ Runner (start, line advance, end)
- ✅ Effects: grant quest, start story, discover knowledge, set flag,
      give item, advance story, open shop
- ✅ Listening exposure: line learning targets discovered once per line
- ✅ Choice/branching dialogue — welcome (yes/no) + **Hina's festival-lantern** branch (three outcomes, one knowledge-gated)
- ✅ Knowledge-gated choices (`DialogueChoice.requiresKnowledge` — a choice whose word isn't discovered yet simply doesn't appear; spec §13)
- ✅ Kid-mode dialogue variants (`kidJp`/`kidReading`/`kidTranslation` — welcome + beach-sign carry simpler all-kana text; spec §68)
- ✅ Dialogue replay (♪ button) + auto-play when voice is on
- ✅ TTS playback of lines (kana-clip voice through Kaiteyo TTS; settings toggle)
- ⚪ Portrait/emoji reaction system

## QUEST

- ✅ Quest model: objectives, prereqs, rewards, learning targets, dialogue
- ✅ Prerequisite graph + availability refresh
- ✅ Objective kinds: reach/discover location, read object, talk, learn word,
      buy item, take photo, ride train
- ✅ Event-driven progress (`reportEvent`)
- ✅ Rewards: xp, items, cosmetics, stamps, unlocks, knowledge
- ✅ Auto-guidance (start first available quest when idle)
- ✅ Objective kinds added: write kana (`WriteKana`), listen to a line (`Listen`), learn a specific word (`LearnWord`) — all event-driven
- ✅ Order minigame objective kind (`OrderFood` — order from a stall's Japanese menu)
- ✅ `Season`/`Weather` objective kinds (complete when the world enters the season/weather)
- ✅ Collect-N objective kind (`Collect` — blank target = any item; `QuestEvent.CollectItem` fired at every item gain; collection quest shipped: "Collect two drinks"; spec §8)
- 🔵 Sequence / timed objective kinds
- ✅ Quest log UI polish (filter chips: All/Active/Completed + per-quest progress bars)
- ⚪ Side-quest chains per NPC
- ⚪ Failed/abandoned quests (time-gated objectives)

## STORY

- ✅ Chapter/scene data + `StoryEngine` (start/advance/restore)
- ✅ Story menu (chapter cards, scene dialogue, effects)
- ✅ Story choices — `StoryChoice` (jump to any scene / trigger a different quest / grant knowledge / set a world flag); the festival fireworks scene branches: watch to the end (moon scene + 月 + `festival-finale`) or head home early (`festival-early`, story ends); validator checks every branch (spec §55)
- ✅ Festival story chain (Hamanaka evening — data-driven)
- ✅ Kamakura night quest chain (lantern keeper, stone lantern, moon photo)
- ⚪ Choices that alter quest order / relationships

## LEARNING

- ✅ Knowledge nodes (word/kanji/phrase) + reading/meaning
- ✅ Typed edges (kanji→word, related, phrases)
- ✅ Discovery with source tagging + one-at-a-time popup queue
- ✅ Assistance levels (Japanese only / + reading / + translation)
- ✅ Discovery → knowledge map + mine → Kaiteyo
- ✅ Sentence/grammar nodes in the graph + content (festival phrases, どこ/です/か grammar node)
- ✅ Adaptive learning: a word already studied in Kaiteyo is *recognized*, never re-taught (`hasStudyMaterialFor` wired into `LearningManager.discover` — no popup, no double count, quests still progress; spec §73-74)
- ✅ Kid-mode discovery: `kidMode` pins the effective assistance to `Kids` and swaps object targets to their simpler `kidTargets` layer — 水/お茶/ジュース instead of 飲み物 at the vending machine (spec §68)
- ⚪ Radical/component connections from Kaiteyo's kanji system
- ✅ In-world writing activity: kana tracing panel + lenient stroke-order evaluator + writing desk object
- ✅ Kanji tracing — Kamakura writing desk teaches 駅/海/町 through the same coverage evaluator; stroke *count* shown from Kaiteyo's dictionary; stroke-order verification waits for real stroke data
- ⚪ Stroke-order data from Kaiteyo (the boundary is ready; the data isn't)

## DICTIONARY

- ✅ `GameBridge` headword lookup hook
- ✅ Discovery card shows a real mini entry: senses with part of speech, and for kanji nodes the on/kun readings, stroke counts and radicals — all from Kaiteyo's dictionary through the bridge (spec §17, §63)
- ✅ In-game dictionary browser — every world word, searchable, with the entry's real senses, kanji on/kun/strokes/radicals and **pitch accents** (平板 / accent n) from Kaiteyo's dictionary through the bridge; mine or open-in-Kaiteyo per entry
- ⚪ Full dictionary-manager deep link from the discovery card

## KANJI / VOCABULARY / GRAMMAR / SENTENCES

- ✅ Vocabulary discovered from environment/objects/NPCs/quests/photos
- 🔵 Kanji nodes with stroke/radical data from Kaiteyo
- 🔵 Grammar notes surfaced contextually (e.g. どこ/です/か in a quest line)
- 🔵 Sentence library shared with study/media mining

## PHOTOGRAPHY

- ✅ Photo mode (enter/exit, focus, zoom)
- ✅ Subject tagging (objects + NPCs in frame → knowledge ids)
- ✅ Album (persisted, category grouping)
- ✅ Photo quest events + stats
- ✅ Photo review screen with tags + save-to-disk (JSON sidecar) + delete
- ⚪ Photo filters/effects
- ✅ Seasonal photo spots — `CameraSpot` objects that open photo mode and teach
      their subject (beach spot: 海の写真 → 海/写真; spring-only sakura spot in
      Kamakura); more spots are pure content (spec §19)

## COLLECTION

- ✅ Collection manager (stamps/discoveries, unlock, persist)
- ✅ Collection menu
- 🔵 Category browsing + progress bars
- ⚪ Postcards, local items, NPC memories
- ⚪ Trading/display (room decoration long-term)

## TRAVEL / TRAIN / VEHICLES

- ✅ Station model + travel network + unlock flags
- ✅ Travel panel (unlocked destinations)
- 🔵 Boarding animation / platform scene
- ⚪ Schedules, timetables, announcements (listening content)
- ⚪ Route maps + line transfers
- ⚪ Bus stop / bicycle / walking between regions
- ⚪ Real-world-accuracy grounding for real stations (lawful data only)

## ENVIRONMENT

- ✅ Time-of-day (WorldClock: day/minute, morning/day/evening/night)
- ✅ Weather (sun/cloud/rain/snow) + render tint + rain/snow VFX
- ✅ Time-gated activities: `availablePhases` on objects + closed prompt/toast; Hamanaka evening festival (stalls, NPCs, quests, dialogue, story, collectibles)
- ✅ Weather affecting NPC presence (weather-gated NPCs: rain-only, snow) + seasonal weather bias (winter snows, spring rains)
- ✅ Season system: spring/summer/autumn/winter cycle, palette tints, season-gated content
- ✅ Ambient audio per area (procedural pads: town, beach, station, festival)
- ✅ Day/night cycle pacing (Fast / Standard / Real time, live) — fixed time preset 🔵

## AUDIO / MUSIC / VFX / SHADERS

- ✅ Master/music/sfx volume settings (sliders + steppers in Settings → Audio, live)
- ✅ BGM per area (procedural ambient pads: town, beach, station, festival)
- ✅ SFX (discovery, quest complete, photo shutter, interact, purchase, closed)
- ✅ TTS for dialogue (reuse Kaiteyo TTS — kana clips, per-line)
- ✅ Seasonal ambient audio: the pad is coloured per season (spring birdsong, summer cicadas, autumn leaf crackles, winter wind — `GameAudio.setSeason`, spec §42/§91-92)
- ✅ Weather VFX: rain streaks + snow flakes (spec §41)
- ⚪ Dust/sparkle particle effects on discovery
- ⚪ Post-processing: night tint exists; add bloom/CRT for stylized look

## UI

- ✅ Minimal HUD: interaction prompt, objective, time/weather, hints
- ✅ Panels: dialogue, discovery, quest complete, photo, travel, debug
- ✅ Menus: map, quest log, collection, album, knowledge map, character,
      settings
- ✅ Menu focus/keyboard navigation pass (`FocusNav` model, Enter/Back, gamepad-driven)
- ✅ Touch-adaptive HUD (contextual controls overlay)
- ✅ Accessibility wiring: `showHints` gates tutorial guidance, `reducedMotion` suppresses camera shake, `textSizeScale` scales dialogue/discovery/quest text (all live settings)

## SAVE

- ✅ Versioned `SaveData` + migration hook
- ✅ Slots (`~/.kaiteyo/game/saves/`), autosave timer, new game
- ✅ Multiple named slots + management UI (Saves menu)
- 🔵 Save-version bump discipline documented in save-system.md
- ⚪ Cloud sync through Kaiteyo sync

## PERFORMANCE

- ✅ Spatial hash, cell-local rendering, fixed timestep
- ✅ Frame-time profiling in the debug overlay: rolling 2 s window with avg/p95/max + sparkline and a 60 Hz reference line (spec §94)
- ⚪ Texture atlas / draw-call batching in the Canvas backend
- ⚪ LOD for objects/entities
- ⚪ Mobile performance budgets (tier presets)

## ACCESSIBILITY

- ✅ Text size scaling — dialogue, discovery and quest-complete panels
      (`textSizeScale`, live; spreads to remaining panels on request)
- ✅ Reduced motion — suppresses camera shake + discovery kick
      (`reducedMotion`, live)
- ✅ Hints toggle — hides tutorial guidance + closed-suffix (`showHints`)
- ✅ Kids-mode preset — pins `Kids` assistance + swaps objects to their
      simpler `kidTargets` vocabulary layer (Settings → Learning assistance;
      spec §68-69)
- ⚪ Colorblind-friendly palettes
- ⚪ Simplified control schemes (one-button mode)

## LOCALIZATION

- ✅ UI strings mostly EN with JP game content (content is JP-first by design)
- 🔵 Japanese UI locale (settings/menus in Japanese)
- ⚪ Full i18n of game UI (not the world's Japanese)

## CONTENT / TOOLS / EDITOR

- ✅ Content pipeline: JSON → validate → runtime
- ✅ `ContentValidator` hard-fail on broken references
- 🔵 Content schema docs + examples per file (WORLD.md covers the basics)
- ⚪ In-app content authoring (place objects/NPCs/quests live)
- ⚪ Region package builder + versioning CLI
- ⚪ Community content format (custom regions without app releases)

## TESTING

- ✅ Quest graph tests (availability, events, completion, rewards)
- ✅ Core logic tests (save migration, bindings, knowledge graph, tiles, load)
- ✅ Session test (movement, interaction, dialogue→quest, rewards, photo, save)
- 🔵 Content validation tests over the shipped JSON (node-based validator used in dev sweeps)
- ✅ Input manager tests (edge triggers, rebinding, touch provider, gamepad press queue)
- ✅ Dialogue TTS tests (kana extraction, listening signal, silent-voice honesty)
- ⚪ Save-migration golden tests (v1 → v2 fixtures)
- ⚪ Performance/regression harness (frame-time budget assertions)
- ⚪ Manual test script per release (spec §134 checklist)

## RELEASE

- 🔵 Game bundled and visible in the desktop suite (done in `:desktopApp:run`)
- ⚪ Installer/update inclusion of game content
- ⚪ Mobile build of the game (bridge no-op path)
- ⚪ First-run discoverability (Launchpad entry, tutorial)

---

*Keep this file in sync with code changes. Status ledger lives in
[VERTICAL_SLICE.md](VERTICAL_SLICE.md); sequencing lives in
[ROADMAP.md](ROADMAP.md).*
