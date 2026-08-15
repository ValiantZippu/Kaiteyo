# Journey Runtime Spec

**Status**: TARGET — blueprint. Runtime architecture decisions deferred to ADR-0014 and
STANDARDS §242 (engine evaluation; no custom engine build).
**Source spec**: [Node Architecture master spec](../NODE_ARCHITECTURE.md) §137–§144, §96,
plus STANDARDS §251–§253 (input), §254 (accessibility), §267 (caches).
**World data**: [Journey World Schema](JOURNEY_WORLD_SCHEMA.md)
**Owner doc**: [`docs/architecture/journey.md`](../journey.md) (STANDARDS §175)

## 1. Game UI layer architecture (§137)

| Layer | Owner | Scope | Z-order |
|---|---|---|---|
| WORLD UI | game runtime | in-world signs, interaction prompts, minimal HUD (§2) | lowest |
| JOURNEY UI | journey features | journal, quests, camera UI, collections | mid |
| GLOBAL KAITEYO UI | shared app | launchpad, settings, library chrome | above Journey |
| DICTIONARY OVERLAY | shared dictionary | lookup popup (§5) | above game |
| KNOWLEDGE OVERLAY | shared knowledge | glossary, mastery display (§5) | above game |
| MEDIA OVERLAY | media | subtitle/dictionary in media contexts | above game |
| SYSTEM UI | app shell | dialogs, confirmations, update prompts | top |

- HUD is never mixed into app navigation chrome and vice versa. The shared overlays are
  the only crossing points (§5, §6) and they are deliberate bridges, not merged UIs.

## 2. World HUD (§138)

- Thin, collapsible strip: time, weather, location, current objective, interaction prompt,
  camera switch control. Default state: interaction prompt + subtle location label only.
- Everything else lives in overlays and the Journal (§119).

## 3. Interaction prompt (§139)

- Proximity → compact prompt: object name (Japanese + gloss), e.g. `[Interact] おにぎり
  Onigiri`. Interact → contextual options per §94. Expand → knowledge overlay (§5).
- Never a big panel unless expanded; never covers the objective card (§101).

## 4. Camera modes (§96)

- First person: photography, reading signs, immersion, fine interaction, dictionary use.
- Third person: movement, social scenes, avatar visibility.
- One deliberate switch action (key/button, remappable — STANDARDS §251–§253); per-
  interaction camera preference is declared data, never a hard lock.

## 5. Knowledge overlay / dictionary bridge (§140, §82–§83)

- Press dictionary action → small glossary for the focused node; expand → full dictionary
  entry (existing dictionary popup behavior hosted in the game layer).
- Available actions from the overlay: create card (mining), edit card, tags, copy,
  pronunciation (TTS), open full dictionary, related-node chips (§81 traversal).
- This is the "game → dictionary without leaving the world" seam and the §150 loop's hub.

## 6. Game ↔ Kaiteyo transition (§141)

- Journey is a destination inside the app (Launchpad entry): shared identity, theme,
  settings, knowledge, statistics, library, updates (ADR-0014).
- Enter/exit uses the motion system (spring, §123); all state preserved (§9).

## 7. Input & controls (STANDARDS §251–§253, §254)

- Action set: Move, Look, Interact, Run, Camera, Map, Dictionary, Journal, Quest,
  Inventory, Pause, Screenshot, Confirm, Back — mapped separately for keyboard, mouse,
  touch, gamepad; remappable; Simple/Advanced settings tiers.
- Accessibility: reduced motion, text scaling, high contrast, subtitle size/background,
  audio controls, controller remapping, keyboard navigation — all honored inside the
  world as in the app (STANDARDS §254).

## 8. Audio (§142)

- Data-driven audio per cell/location/object: ambient, music, weather layers, NPC/vehicle/
  train/shop/ocean sounds, distance mixing, schedule-aware.
- Language audio reuses app TTS for pronunciation/announcements; dialogue is authored
  audio content (AUDIO PRODUCTION).
- App volume/mute settings apply inside the world; audio reacts to time/weather
  deterministically.

## 9. Save system (§144)

```json
{
  "saveVersion": 1,
  "userRef": "...", "worldId": "japan",
  "player": {"position": [...], "cameraPrefs": {...}},
  "worldProgress": {"unlockedCells": [...], "revealedMap": [...], "timeMode": "game",
                    "clock": "...", "weatherSeed": "...", "season": "summer"},
  "quests": {"quest:errand-01": {"objectiveProgress": [1, 2], "state": "active"}},
  "discoveries": [...], "collections": [...], "photos": [...],
  "npcRelationships": {"npc:shopkeeper-14": 3},
  "storyState": {"story:summer-day": {"beat": "errand-complete"}},
  "worldSettings": {...}
}
```

- Sparse overrides over immutable content; versioned; compatible across app versions;
  offline; included in backups (STANDARDS §205–§206).
- **Learning data never lives in the save** — knowledge/reviews/cards/stats stay in shared
  user data (§114). That is what makes Journey and study one trajectory.

## 10. Performance budgets (§143)

| Platform tier | Target | Strategy |
|---|---|---|
| Desktop high-end | 60+ FPS, high fidelity | max LOD, dynamic resolution cap, high texture/mesh budgets |
| Desktop mid | 60 FPS | medium LOD, adjusted budgets |
| Mobile mid (Android/iOS) | 30–60 FPS | low LOD, occlusion, aggressive streaming, reduced NPC tiers |
| Mobile low | 30 FPS | minimum preset, dynamic resolution active |

- Documented budgets (startup, frame time, memory, streaming, audio, save) measured
  against reference devices (STANDARDS §188–§190); cell streaming (§92) + simulation
  tiers (§105) are the primary levers.
- Same world data across tiers; different presentation budgets (§143).

## 11. Accessibility in the world

- All §7 controls and STANDARDS §254 items apply in-game. The knowledge overlay and
  interaction prompt are reachable by keyboard alone. Reduced-motion disables camera
  sweeps, jiggle, and non-essential world animation.

## 12. Acceptance criteria

1. Entering/exiting Journey preserves all app and world state (§6, §9).
2. Interaction → glossary → dictionary → card → review works inside the world (§5).
3. HUD stays minimal; quest UI disappears when not needed (§101).
4. All platforms hit their §10 budgets on the vertical slice (§91).
5. Save/load is deterministic (same inputs → same world state) and survives app restarts.

## 13. Default input mapping (STANDARDS §251–§253)

Remappable, profile-scoped, conflict-checked (existing `keyboard_shortcut` + shortcut
registry pattern). Defaults below; gamepad follows platform conventions.

| Action | Keyboard (default) | Mouse | Touch | Gamepad |
|---|---|---|---|---|
| Move | WASD / arrows | — | left virtual stick / swipe | left stick |
| Look | mouse look | drag in first person | drag | right stick |
| Interact | E / Space | left click | tap | A |
| Run | Shift | — | hold on stick | left stick press |
| Camera switch (1st/3rd) | V | — | two-finger double-tap | Y |
| Map | M | — | map button | Select/Back |
| Dictionary / knowledge overlay | D (or Ctrl+Shift+D) | right click on object | long-press | X |
| Journal | J | — | journal button | B |
| Quest view | Q | — | quest button | Right shoulder |
| Inventory / collections | I | — | inventory button | Left shoulder |
| Pause | Esc / P | — | pause button | Start |
| Screenshot | F12 (rebindable) | — | camera action | Capture |
| Confirm / Back | Enter / Esc | left / right click | tap / back gesture | A / B |

- Simple/Advanced settings tiers (§253): Advanced exposes per-action remap, camera
  sensitivity, stick deadzone, look acceleration, vibration toggle.
- Touch: on-screen control scheme replaces mouse-look when a touch input is detected;
  controllers connect mid-game without restart.
- Accessibility: every action is also reachable via the keyboard-first navigation path
  (STANDARDS §254).

## 14. Audio architecture (§142)

Buses (order = mixing order; each bus has a volume source):

| Bus | Content | Reactive to |
|---|---|---|
| `ambience` | per-cell/location ambient loops | time of day, location |
| `music` | authored region/location music, event themes | location, story/quest state, season |
| `weather` | rain/storm/wind/fog layers | weather state (deterministic seeds) |
| `npc` | NPC proximity dialogue/voice, footsteps | NPC tier, schedule, distance |
| `transport` | trains/vehicles/announcements | transport simulation, schedule |
| `ocean` | waves, beach | location, weather, wind |
| `ui` | world UI feedback (prompts, journal, camera) | UI events |
| `language` | pronunciation, announcement text (TTS reuse), dialogue audio | knowledge overlay, announcements |

Rules:

- App master volume / mute settings apply inside the world (single audio path).
- Distance mixing + LOD for spatial sources; per-platform bus budgets (§10).
- TTS reuse: pronunciation/announcements use the app TTS; authored dialogue uses
  authored audio (AUDIO PRODUCTION) with a TTS fallback when voice assets are absent
  (honest label, §158–§159).
- Audio reacts to time/weather/season deterministically (same state → same mix).

## 15. UI layer contents & rules (§137)

| Layer | Contents | Rules |
|---|---|---|
| WORLD UI | signs, interaction prompts (§16), minimal HUD (§16) | never hosts app chrome |
| JOURNEY UI | journal, quests (objective card only), camera UI, collections, map | contextual panels; disappears when idle (§101) |
| GLOBAL KAITEYO UI | launchpad, settings, library chrome | shared; unchanged by world |
| DICTIONARY OVERLAY | lookup popup (existing behavior hosted in game layer) | the §150 hub seam |
| KNOWLEDGE OVERLAY | glossary, per-dimension mastery display, "learn more" (§112) | opt-in, never interrupts |
| MEDIA OVERLAY | subtitle/dictionary in media contexts | shared with Media Centre |
| SYSTEM UI | dialogs, confirmations, update prompts | top-most; error paths (§296) |

Layer rules:

1. A layer may only invoke the layer below it or the shared bridges (dictionary/
   knowledge/media overlays) — no layer reaches into another's state except via the
   service contracts (SERVICE_CONTRACTS §Journey).
2. Game HUD and application navigation chrome are never merged (no game buttons inside
   the sidebar; no app buttons inside the HUD).
3. All layers honor the motion system (§123): springs in, reduced-motion fades.
4. Every layer has defined empty/loading/error/offline behavior (STANDARDS §296–§299,
   UX_FLOWS §12).

## 16. World HUD element spec (§138–§139)

| Element | Default | Expanded | Data source |
|---|---|---|---|
| Interaction prompt | compact `[Interact] おにぎり Onigiri` on proximity | contextual options (EXAMINE/…) | interaction node (§94) |
| Location label | subtle, fading | map jump | world node |
| Time / weather | icon + short text | full overlay | world clock/weather state |
| Current objective | one card, top corner, dismissible | quest view | quest service |
| Camera control | mode icon + switch | camera settings | camera service |

- Default HUD is never more than: prompt + location + time/weather. Everything else is
  an overlay (§15).
- The prompt never covers the objective card; both are keyboard-reachable.

## 17. Streaming & cache contract (§92, STANDARDS §267)

- Owner: world streaming service. Size limits per platform tier (§10). Eviction: LRU
  with LOD-priority (high-LOD cells evict first). Persistence: world package cache
  (re-downloadable), save overrides on disk.
- Invalidating conditions: package update, region switch, save restore, low-memory
  pressure (STANDARDS §278).
- Guarantee: adjacent-cell loads ≤ budget (§10) with zero frame hitches; a cell is
  never re-streamed while resident.

## 18. Save versioning & integrity (§144)

- `saveVersion` is monotonic; unknown/newer versions refuse to load with a clear message
  (STANDARDS §219) instead of migrating blindly.
- Saves are checksummed; corrupt saves recover to the last good save with explanation
  (never a crash, never silent data loss).
- Saves are included in backups (STANDARDS §205–§206) and exported as semantic,
  versioned objects (STANDARDS §207).
- Learning data never enters the save (§9) — verified by a schema-level guard test
  (TEST_PLAN §9.5).
