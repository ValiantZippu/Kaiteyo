# Journey — Gameplay Systems Specification

**Status**: TARGET — nothing implemented. This is the game-design and gameplay-systems
contract for Journey. It translates [Node Architecture master spec](../NODE_ARCHITECTURE.md)
§86–§119 into concrete system contracts (purpose, rules, data hooks, acceptance
criteria). It is not a fictional design bible: every system is marked with production
flags (§159) and nothing here is claimed as shipped.
**Source spec**: NODE §86–§119 · STANDARDS §251–§254 (input/accessibility) ·
§360–§361 (security) · §143 (performance)
**Data contracts**: [Journey World Schema](JOURNEY_WORLD_SCHEMA.md) · Node registries:
[NODE_TYPE_REGISTRY](NODE_TYPE_REGISTRY.md) (GAMEPLAY/WORLD families),
[RELATIONSHIP_REGISTRY](RELATIONSHIP_REGISTRY.md)
**Runtime**: [Journey Runtime Spec](JOURNEY_RUNTIME_SPEC.md) · **Owner doc**:
[`docs/architecture/journey.md`](../journey.md) (STANDARDS §175)
**Production flags**: CONTENT PRODUCTION · ART PRODUCTION · 3D PRODUCTION ·
AUDIO PRODUCTION · EXTERNAL DEPENDENCY (geodata) · SEPARATE RUNTIME (engine decision —
STANDARDS §242, ADR-0014)

---

## 1. Game design philosophy (§86)

Journey is **primarily exploration, observation, discovery, language, culture, story,
photography, collection, and daily life** — with light RPG-style progression only for
motivation.

| The player should feel | The player must NOT feel |
|---|---|
| "I am living inside a Japanese learning world" | "I am grinding XP in an educational RPG" |
| every encounter could teach something real | encounters are loot drops |
| the world exists independent of the player | the world exists to serve a quest list |
| knowledge comes from noticing | knowledge comes from pop-up quizzes |

Design rules derived from §86:

1. **Language is ambient.** Signs, menus, dialogue, and objects carry real Japanese;
   teaching surfaces appear only on demand (§112).
2. **Observation is a mechanic.** LOOK/EXAMINE/PHOTOGRAPH are first-class actions, not
   filler.
3. **No fake verbs.** If a system isn't implemented, the surface says so (§158–§159,
   §325).
4. **Culture is content.** Festivals, shrine customs, shop etiquette, train behavior are
   authored content — never approximations hardcoded into the engine.
5. **Every system must pass the §153 test** (does it improve exploration, language,
   culture, story, discovery, or immersion?); otherwise it does not belong.

## 2. Core loop (§87)

```
EXPLORE → NOTICE → INTERACT → UNDERSTAND → LEARN → DISCOVER → COLLECT → USE → REMEMBER → RETURN
```

The canonical example (the slice proof, §91):

> Player walks into a convenience store → sees おにぎり → interacts → glossary reveals
> おにぎり (with お・に・ぎ・り) → related items surface → takes a photograph → records a
> discovery → optionally creates a card → later meets the word in media → knowledge
> increases → statistics update → a quest progresses.

Loop invariants (must hold in the vertical slice):

- Every loop iteration creates ≥ 1 event (encounter, lookup, photo, discovery, mine) that
  feeds stats/knowledge (§210–§213).
- The loop can be exited at any point without losing state (save, §144).
- No step forces the player into a modal study UI (§112).

## 3. World map & navigation UX (§89, §118)

- The map is a **stylized, beautiful surface** — never a developer map: soft terrain,
  clean typography, water, roads, railways, landmarks, discovery/quest markers, visited
  vs. unvisited regions, and an optional **knowledge-density overlay** (which areas
  contain words/kanji the player has or hasn't met).
- Map modes: WORLD → REGION → CITY → DISTRICT → WALKING MAP, with smooth animated
  transitions between scales (spring-based, §123; reduced-motion fades).
- **Progression reveal (§118):** detail is revealed by discovery — a district shows only
  its name and silhouette until the player visits; streets/locations unlock as
  discoveries accumulate. The map is a progress surface, not a wiki.
- Data: map overlays are authored content (styled geometry + marker metadata), not raw
  GIS dumps (§249–§250). Geodata is EXTERNAL DEPENDENCY with verified licensing (§248).

## 4. Vertical slice definition (§91)

**One polished location before any expansion**: Kamakura + Enoshima.

| Slice content (must all be data-driven) | Proof items (§366) |
|---|---|
| streets, beach, railway + station, shops, temples, shrines, residential areas, aquarium attraction, ocean | movement, camera, interaction |
| NPCs, vehicles, trains, weather, day/night | dictionary, language node, NPC, dialogue |
| photography, language nodes, quests, dialogue, collections, contextual media-style learning | quest, discovery, photography, collection |
| knowledge, stats, save/load, performance | knowledge, stats, save/load, performance |

Exit criteria (the slice is "done" only when, per TEST_PLAN §13):

1. The §2 onigiri loop completes end-to-end.
2. All performance budgets hold on reference desktop + mobile (§143, TEST_PLAN §2).
3. Adding a new object/NPC/dialogue/quest/story requires **zero engine changes**
   (content-only, §148).
4. No §325–§329 violations (no fake implementations, no hardcoded data, no placeholder
   logic).

## 5. World structure & cells (§88, §92)

- Hierarchy: `world → region → prefecture → city → district → neighborhood → map_cell →
  location → interior → interaction_node` (full level contracts in
  JOURNEY_WORLD_SCHEMA §1).
- **Streaming is mandatory**: cells load/unload around the player with LOD tiers; a full
  region is never resident. Cache contract (STANDARDS §267): owner = streaming service,
  size budget per platform tier, LRU eviction, persistence = world package cache,
  invalidation = package update/region switch.
- **Determinism**: cell content and NPC schedules are derived from seeded data — the same
  (world, time, weather, season, save) always reproduces the same state. Debugging never
  depends on runtime generation (§92, §98).

## 6. Object & interaction systems (§93–§94)

Every meaningful object is optionally an **interaction node** with a language surface
(nameJa, sign/menu text), knowledge links (`represents`), and a typed interaction set.
Full object schema: JOURNEY_WORLD_SCHEMA §3.

**Interaction registry** (data-driven; the table is the contract):

| Interaction | Eligibility | Inputs | Outputs | Notes |
|---|---|---|---|---|
| LOOK | always | focus | name + gloss on demand | cheapest; default |
| EXAMINE | any object | confirm | info card + full glossary | unlockable info per object |
| TALK | NPC | confirm | dialogue start (§11) | requires speaker |
| PHOTOGRAPH | `photography.eligible` | camera mode | photo → `depicts` (§7) | collectible flag |
| READ | text-bearing object | confirm | text panel (sign/menu/book) | reading quests |
| PICK_UP / COLLECT | item | confirm | inventory/collection + vocab | collection quests |
| BUY | shop, schedule-aware | confirm + payment flow | item + price (numbers!) | shop hours matter |
| SIT | seatable | confirm | rest state, photo opportunity | — |
| EAT / DRINK | food/drink | confirm | consumption + food vocab + counters | menu/table only |
| SWIM | water | confirm | swim state + water vocab | beach/pool only |
| BOARD / ENTER / EXIT | doors/trains | confirm | transition + transport vocab | — |
| SEARCH | searchable area | hold | hidden discoveries | exploration quests |
| LISTEN | audio source | confirm | audio focus + announcement text | TTS hookup |
| PLAY / WATCH | media surface | confirm | media surface + media vocab | media links |
| COLLECT | collectible | confirm | add to collection | member nodes |

Rules:

- Interaction types are **code** (gated by the authoring pipeline §148 + ADR note);
  combinations of existing types on new objects are **content only**.
- Every interaction emits typed events (STUDY/media/journey families, EVENT_CATALOG).
- Failure behavior is defined per type (e.g. BUY outside hours → polite refusal with a
  schedule hint — never a dead button).

## 7. Photography system (§95)

Photography is a **real mechanic**, not a collectible gimmick.

- Camera modes: first-person (default for photography), third-person, free camera —
  with zoom, focus, composition guides, and filters (filters are rewards, §24).
- Capture pipeline: frame → capture → (optional) object/scene recognition → `depicts`
  edges to recognized nodes **or an explicit "not recognized" state** — fabricated links
  are forbidden (§325).
- A photo can become: a discovery, a collection item, a memory, a quest objective, or a
  mined card source (§95). Each path is a real event.
- Gallery lives in the Journal (§26); photos carry location metadata and are exported
  with backups (STANDARDS §205–§206).
- Photography connects to the language surface: a photo of a menu captures the words on
  it; framing a landmark shows its name + gloss on demand.

## 8. Camera modes (§96)

| Mode | Strong for | Weak for |
|---|---|---|
| FIRST PERSON | photography, reading signs, immersion, dictionary interaction, fine interaction | character expression |
| THIRD PERSON | movement, social scenes, walking, avatar visibility | fine interaction |

- One deliberate switch action (key/button, remappable — STANDARDS §251–§253).
- Per-interaction camera preference is authored data (e.g. PHOTOGRAPH prefers first
  person) — never a hard lock.

## 9. Character & avatar system (§97)

- Avatar: appearance, clothing, accessories, animation set (walk/run/swim/sit/ride/
  photo/emotes). Customization is cosmetic-only (no stat effects, §24 anti-grind).
- **Artistic direction (documented, coherent)**: stylized Nintendo-like, cozy, clean,
  expressive, simple enough for cross-platform performance. Explicitly avoided: generic
  Roblox-like proportions, generic mobile-game characters, AAA character production
  costs. This direction is an authoring standard for all characters (ART PRODUCTION).

## 10. NPC system (§98)

An NPC has: identity, appearance, occupation, age category, location, schedule,
relationships (with the player), dialogue refs, knowledge refs, quest refs, activities.
Schema: JOURNEY_WORLD_SCHEMA §5.

**Simulation tiers** (STANDARDS §105, §143 — scalable, not all-or-nothing):

| Tier | Scope | Cost |
|---|---|---|
| 0 — background crowd | no logic, ambient animation only | trivial |
| 1 — schedule-driven | location + activity per schedule slot | cheap |
| 2 — quest/relationship | extended state: dialogue variants, relationship, quest flags | moderate |

- Schedules are deterministic (time-of-day × weekday/weekend × season × weather) so the
  world is debuggable and testable (§98). Saves capture position + relationship state
  only.
- NPCs must *appear* to have lives; nothing is simulated that the player can't observe
  (§86 — observation is the mechanic).

## 11. Dialogue system (§99)

- Node anatomy (JOURNEY_WORLD_SCHEMA §6): speaker, text (ja + translation + furigana),
  voice ref, emotion, choices, conditions, effects, knowledge refs.
- **Dialogue teaches naturally**: language exposure is ambient; knowledge links surface
  only on demand (§112). Not every conversation is an obvious lesson.
- Choice effects are typed: quest objective updates, relationship deltas, discovery
  unlocks, story beats, knowledge exposure events.
- Conditions gate lines/choices on world state (time, quest progress, relationship,
  knowledge) — data-driven, validated (§148).
- Voice is AUDIO PRODUCTION; a synthesized fallback (app TTS) is acceptable for
  announcements, but authored dialogue carries authored audio.

## 12. Quest system (§100–§101)

**Quest kinds** (authored variants of one schema — type is metadata, not code):
DISCOVERY · EXPLORATION · LANGUAGE · PHOTOGRAPHY · COLLECTION · STORY · CULTURE ·
LISTENING · READING · WRITING · VOCABULARY · KANJI · GRAMMAR · MEDIA · DAILY.

Structure: quest → objectives (ordered) → conditions → interactions → rewards →
knowledge → story consequences. Schema + objective condition types: JOURNEY_WORLD_SCHEMA
§7.

**Quest UX rules (§101)**:

- No giant RPG quest lists. Objectives appear as: one small objective card, a map marker,
  a subtle notification, a journal entry. Quest UI disappears when not needed.
- Objective conditions evaluate from world state + events — never from quest-UI state.
- Failure is allowed and **non-punitive** (no energy, lives, loot boxes, or artificial
  timers — §117).
- LANGUAGE quests are study-aware: "review node X", "encounter 3 words containing 食" are
  real conditions over knowledge/discovery events.

## 13. Story system (§102)

- Structure: story → chapter → scene → beat → dialogue/interaction/choice → outcome.
  Ordering enforced with `precedes`/`follows`/`requires`; save/load restores the exact
  beat.
- **Dual progression**: story and language progressions advance together. Beats carry
  knowledge nodes (ambient), quest flags, relationship effects, discoveries.
- The slice's proof story: "A summer day in Kamakura" — take the train, buy a drink, walk
  the beach, meet a character, visit a shop, photograph an object, learn words, return
  home (§102). This is both story progression and language progression.
- Stories are content packages; new stories never require engine changes (§148).

## 14. Daily life activities (§103)

Activity registry (data-driven): walk, sit, eat, drink, shop, visit, photograph, ride
train, go to the beach, swim, visit the aquarium, read, listen, talk, observe, collect,
write, study. Activities are **contextual language exposure**, not minigames. Each
activity: location ref, duration, schedule hooks, exposure knowledge refs, event types.
No activity exists purely as a score minigame (§86, §153).

## 15. Transport systems (§104–§105)

- **Trains (§104):** stations, routes, lines, platforms, timetables, vehicles, boarding/
  exiting, announcements, signage, destination text. **Data-driven route simulation** —
  never a physical railway simulation. Knowledge hooks: station names, direction words,
  transport vocab, numbers, time expressions. Announcements reuse language audio
  (authoring + TTS).
- **Vehicles (§105):** cars, buses, trains, bicycles, boats with traffic/routes/spawn
  rules/LOD/animation/audio — **scalable simulation tiers** like NPCs (tier 0 ambient,
  tier 1 routes, tier 2 scripted). Never simulate every vehicle individually.
- Both systems are CONTENT/ART/AUDIO PRODUCTION; gameplay code stays engine-generic.

## 16. Ocean & beach (§106)

Beach system: walking, sitting, swimming, water, waves, weather, photography, objects,
NPCs, activities. Optional: diving, aquarium, marine discoveries. Water rendering is
performance-aware (mobile tiers, §143). Knowledge hooks: water/marine vocab, safety
signage, seasonal activities (海開き etc. as cultural content).

## 17. World time, weather, seasons (§107–§109)

| System | States | Affects | Rules |
|---|---|---|---|
| World clock | real / game / accelerated time; morning/day/evening/night | lighting, NPCs, shops, transport, quests, events, weather | player-selected, saved |
| Weather | clear/cloudy/rain/storm/snow/fog | lighting, audio, NPCs, movement, world appearance, quests, photography | deterministic seeds; **never purely cosmetic**; guaranteed clear days so weather can never soft-lock progression |
| Seasons | spring/summer/autumn/winter | vegetation, events, NPC clothing, food, weather, quests, photography, decoration | world-level state; content variants are data (§113) |

- Time/weather/seasons are world state in the save; they drive schedule slots and content
  variants deterministically.
- Slice ships summer; the swap to other seasons must be data-only (proves §90's
  "expand without rewriting the engine").

## 18. Collections & discovery (§110–§111)

- **Collection = "I have this"**; **Discovery = "I encountered this."** Distinct
  semantics, both event-derived.
- Collection kinds: Kanji, Vocabulary, Objects, Food, Locations, Photography, Media,
  NPCs, Stories, Discoveries. Memberships connect to knowledge-graph nodes
  (`belongs_to`).
- Discovery kinds: word, kanji, location, object, NPC, food, sign, media scene, cultural
  fact. Discovery history contributes to stats, Journey, knowledge, and quest conditions
  (`encountered_by` / `discovered_by` edges).
- Nothing is added to a collection except through a real encounter (§325 — no seeded
  collections; the earlier fake-seeding removal in PRODUCT_AUDIT §5 applies to Journey
  too).

## 19. Learning in Journey (§112)

The cardinal rule: **Journey never interrupts exploration with flashcards.**

1. Player sees 電車 → the UI subtly reveals 電車 (ambient exposure event).
2. Later (or on demand): "Want to learn more?" → dictionary → cards → writing →
   pronunciation → related words.
3. Knowledge exposure events feed `encountered_by`; deeper learning is opt-in.

This is the §150 loop's Journey half: exposure creates evidence; evidence moves
knowledge; knowledge drives stats and review selection.

## 20. Difficulty adaptation (§113)

- Player level: BEGINNER · ELEMENTARY · INTERMEDIATE · ADVANCED · CUSTOM.
- **One geometry, N language depths**: same world; different dialogue variants, quest
  text, glossary richness, explanation availability. Content files carry `level` fields;
  the runtime filters presentation only — never geometry.
- Example: BEGINNER sees 駅 "train station" + simple sentence; ADVANCED gets
  駅員さんに切符を見せる with natural dialogue.
- Mid-game level changes are safe: only presentation changes.
- Knowledge state (§84) refines which words surface per player (adaptive smoothing).

## 21. Normal Kaiteyo vs Journey (§114)

| | Normal Kaiteyo | Journey |
|---|---|---|
| Posture | dictionary-first, learning-first, media-first, study-first | exploration-first, context-first, discovery-first, story-first |
| Shared foundation | knowledge graph, database, user knowledge, statistics, cards, accounts, settings | same |

The two postures are **front-ends over one data model**, not two products (ADR-0014).

## 22. Child mode (§115)

- **Same world engine, same node architecture, same knowledge graph** — different UX,
  progression, language complexity, safety, and content.
- More guided (visible objectives, gentler pacing); normal mode is more open.
- No separate world build; child mode is a content + presentation + safety layer:
  restricted dialogue/content, parent-visible settings, no social surfaces.
- Safety review checklist applies (STANDARDS §356–§360 spirit applied to content).

## 23. Progression (§116)

**No XP grinding.** Progression axes: knowledge, discoveries, collections, story
completion, locations, photography, quests, language mastery. The player should feel
"I know more Japanese," never "I have 87,421 XP." Progress displays derive from the same
event streams as stats — there is no parallel "game progress" currency.

## 24. Rewards (§117)

Reward catalog (all cosmetic/experiential): new locations, new stories, camera filters,
cosmetics, collections, journal pages, photo frames, music, world events.

**Anti-predatory rules**: no energy systems, no lives, no loot boxes, no artificial
timers, no paid progression. Rewards are authored content (`unlocks`/`rewards` edges),
never monetization mechanics.

## 25. Map progression (§118)

Japan → Kanto → Kanagawa → Kamakura → district → streets → locations: discovery reveals
detail (§3). Unvisited content is hidden but discoverable; nothing is paywalled.

## 26. Journal (§119)

Journal contents: memories, photos, discoveries, people, places, words/kanji
(graph-linked), stories, quests, maps. **The journal is a personal travel notebook** —
not a trophy cabinet:

- Word/kanji entries are real knowledge-graph queries ("words I met in Kamakura").
- Photos link back to location/object nodes and can be exported/backed up.
- Offline-first; exported with user data (STANDARDS §205–§206).

## 27. Cross-system rules

1. **Everything is events**: exploration, interaction, photo, discovery, quest, story —
   all produce EVENT_CATALOG entries that feed knowledge and stats. No Journey number is
   computed from game-UI state (§213).
2. **Determinism**: same inputs → same world state (streaming, schedules, weather,
   quests, saves) — testable (TEST_PLAN §9).
3. **Content-only extensibility**: new objects/NPCs/dialogue/quests/stories/regions are
   content packages (§148); gameplay code is engine-generic.
4. **Security**: content never executes code (§361); media/assets are untrusted (§357);
   no scraping of Google data (§248).
5. **No fake**: any unimplemented system is labeled TARGET/FUTURE, never presented as
   shipped (§158–§159, §325).

## 28. Slice-level acceptance criteria

1. §2 loop end-to-end inside Kamakura + Enoshima (onigiri example).
2. §4 exit criteria met (performance, content-only additions, no fakes).
3. Photography → recognition → discovery/collection/card paths all real (no fabricated
   `depicts`).
4. Quest objectives evaluate from world state; failure is non-punitive; quest UI
   disappears when idle (§101).
5. Weather/seasons/time drive schedules and content variants deterministically; nothing
   soft-locks progression.
6. Difficulty switch (BEGINNER→ADVANCED) changes only presentation.
7. Save/load determinism; learning data never in the save (§144).
8. Child mode reuses the slice world with restricted presentation/content.

## 29. Production flags & dependencies

| Item | Flag | Notes |
|---|---|---|
| World geometry/3D | 3D PRODUCTION · ART PRODUCTION | Blender source → LOD export (STANDARDS §245–§246) |
| Characters/NPCs/avatar | ART PRODUCTION | one artistic direction (§9) |
| Audio (dialogue, ambience, announcements) | AUDIO PRODUCTION | TTS reuse for language audio |
| Geodata (Kamakura/Enoshima reference) | EXTERNAL DEPENDENCY · LEGAL REVIEW | OSM/government/public GIS with attribution (§248) |
| Game engine | SEPARATE RUNTIME | STANDARDS §242 evaluation + ADR before any code (ADR-0014) |
| Content volume | CONTENT PRODUCTION | slice-first (§4, §91) |

---

*The runtime consumes these systems through the service contracts
([SERVICE_CONTRACTS.md](SERVICE_CONTRACTS.md)) and the world data contracts
([JOURNEY_WORLD_SCHEMA.md](JOURNEY_WORLD_SCHEMA.md)); implementation is gated on the
§157 build order in `docs/planning/TODO.md` and the engine evaluation (ADR-0014).*
