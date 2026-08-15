# Interaction System

**Status**: TARGET (spec). **Source**: expansion spec §27; NODE §94 (interactions);
GAMEPLAY_SYSTEMS §6 (interaction registry).

## Principle

One **reusable `InteractionComponent`** drives all world interactions. Authors
declare interaction nodes; the engine renders prompts and routes actions. No
interaction is hardcoded per object.

## Interaction types (GAMEPLAY_SYSTEMS §6 registry — 16 types)

| Type | Example | Eligibility | Inputs | Outputs |
|---|---|---|---|---|
| `inspect` | look at an object closely | any object with inspect data | Interact | detail panel, knowledge overlay |
| `read` | read a sign/menu | text-bearing object | Interact | text + glossary (dictionary bridge) |
| `listen` | listen to an announcement/ambience | audio-bearing object | Interact | audio + optional transcript |
| `talk` | start NPC dialogue | NPC present + available | Interact | dialogue system |
| `collect` | pick up a stamp/ticket | collectible + eligible | Interact | inventory/collection + event |
| `photograph` | take a photo | photogenic target (composition) | Photography mode | photo + discovery/collection |
| `sit` | sit on a bench | seat + accessible | Interact | idle/scene, rest (cosmetic) |
| `swim` | enter water | water + season/conditions | Interact | swimming state |
| `dive` | dive underwater | dive spot | Interact | underwater state |
| `travel` | board train/bus/ferry | transport + timetable | Interact | travel flow (`transportation.md`) |
| `enter` / `exit` | enter/exit building | doorway | Interact | interior load (`world-streaming.md`) |
| `purchase` | buy something | shop + open | Interact | shop UI, item, purchase event |
| `observe` | watch a scene (festival, train passing) | scene trigger | Interact | cinematic/ambient beat |
| `learn` | practice a word/kanji in context | language node | Interact | practice prompt (opt-in) |
| `answer` | respond to a question (NPC/quest) | dialogue/quest node | selection | quest/dialogue state |
| `write` | write practice (child mode / writing spots) | writing spot | Interact | writing UI |

## InteractionComponent contract

```
InteractionComponent
  ├─ detect(targets near player / reticle)  → eligible interactions
  ├─ prompt(compact)                        → "[Interact] おにぎり Onigiri"
  ├─ activate(interaction)                  → contextual options (EXAMINE / …)
  └─ resolve(result)                        → state change + events
```

Rules:

1. **Proximity → compact prompt → context menu** (§139): the prompt shows the
   object name (Japanese + gloss). Expanding shows contextual options. Never a big
   panel unless expanded; never covers the objective card.
2. **Eligibility is data**: an interaction node declares eligibility (time of day,
   season, weather, NPC state, quest state, accessibility). The engine never shows
   an ineligible interaction as broken — it shows nothing or a gentle hint.
3. **Every interaction has a language or discovery outcome or is honestly
   cosmetic** (sitting = cosmetic; rest has no fake benefit).
4. All interactions emit events (NPC_INTERACTED, WORLD_TEXT_SELECTED,
   LOCATION_DISCOVERED, …) — see `EVENT_CATALOG.md`.
5. Reusable: the same `read` component drives signs, menus, posters, books.

## Input mapping (§139, `docs/input/`)

- Interact = E / Space (keyboard), left click (mouse), tap (touch), A (gamepad).
- Context menu navigation = standard confirm/back; keyboard-first accessible.
- Long-press (touch) / right click / D = dictionary/knowledge overlay on the
  focused node.

## Knowledge & dictionary bridge (§140)

Reading/inspecting text in the world routes through the **text analyzer pipeline**
(see `learning-in-world.md`): WORLD_TEXT_SELECTED → tokenizer → dictionary →
glossary → expand to full entry → actions (create card / edit / tags / copy /
pronunciation / open dictionary / related-node chips). This is the game↔app seam.

## Accessibility

- All interactions reachable by keyboard alone; prompts have focus rings; text
  scales; the interaction prompt respects high contrast and reduced motion.
- Interaction hit zones are generous (touch); no timing-based interactions
  (nothing requires a "quick tap"); no hold-to-confirm racing.

## Acceptance criteria

1. Any authored interaction node works with zero engine changes (data-driven).
2. Ineligible interactions never appear broken (missing = not shown).
3. The same interaction type behaves identically across input devices.
4. Every interaction that touches learning routes through the shared pipeline and
   updates shared data only.

## Related

- Input: `docs/input/` · Camera: [camera.md](camera.md)
- NPCs: [npc-system.md](npc-system.md) · Quests: [quest-system.md](quest-system.md)
- Learning flow: [learning-in-world.md](learning-in-world.md)
- Spec: NODE §94, §139–§140; GAMEPLAY_SYSTEMS §6
