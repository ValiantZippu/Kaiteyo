# Normal User Experience

**Status**: LIVE (study app) + TARGET (world integration). This document defines the
experience of the "normal" user — the adult self-learner — across both front-ends.

## Two interconnected experiences (§70)

Kaiteyo deliberately offers **two** experiences over the *same* underlying
knowledge, dictionary, decks, stats, curriculum, quests, media, and progress:

| | Normal Kaiteyo (LIVE) | Kaiteyo World / Journey (TARGET) |
|---|---|---|
| Nature | Study application: dictionary, decks, reviews, writing, exams, stats, media centre, mining | Explorational world: walkable Japan, NPCs, quests, photography, discovery |
| Entry | App launch → Home/Library/Browse/Stats/Media/Settings | Launchpad → Journey destination (ADR-0014) |
| Learning | Explicit: flashcards, writing practice, exams, reading | Embedded: reading signs, glossary popups, mining in context |
| Progress | Knowledge states, SRS, statistics, heatmap | Same knowledge states + world progress (map reveal, collections, quests, stories) |
| Not for | Everyone (it is the app) | Users who opt in; never forced |

**No user is ever forced into the game.** A learner can use normal Kaiteyo
forever. A learner can use only the world. A learner can combine both — and the
combination is the product's superpower: the same graph, same knowledge model,
same statistics feed both front-ends (§70).

## The normal user's day (LIVE, real)

1. **Open the app** → Home dashboard: knowledge snapshot, study target card,
   collections, recent activity, writing practice card (real data; `CURRENT_ISSUES.md`).
2. **Library** → decks, collections, unified search; **Browse** → kanji/vocabulary
   search; **Stats** → heatmap, knowledge profile, exam analytics, forecast.
3. **Study** → review session from any deck (FSRS-5), writing practice (stroke
   evaluation), flashcards, JLPT/exam simulations.
4. **Media** → watch local video with subtitles; hover text → dictionary popup →
   mine a card (the §150 hub seam, live in the Media Centre).
5. **Settings** → themes (17 presets, Theme Studio), shortcuts, activity tracking,
   sync/backup, Anki/import-export.

Everything is offline-capable; sync is provider-based (GitHub gist, ADR-0009).

## The world user's day (TARGET)

1. **Enter Journey** from the Launchpad (spring cross-fade; all state preserved).
2. Walk Kamakura: read signs (interaction prompt `[Interact] おにぎり Onigiri`),
   talk to NPCs, ride the Enoden, photograph Yuigahama beach, complete quests.
3. Hover a sign → glossary → expand → full dictionary entry → **mine a card**
   (create card → deck → review) without leaving the world (§140).
4. Discover locations → map reveals → collections fill → journal updates.
5. Exit to the study app: the mined card, the encountered words, the stats all
   reflect the world session (§9 — learning data lives in shared user data, never
   the game save).

## Movement between the two

- **The knowledge overlay is the bridge** (JOURNEY_RUNTIME_SPEC §5): glossary →
  dictionary → card → review, hosted in the world layer, sharing the existing
  dictionary popup and mining behavior.
- **Media ↔ World**: watching anime mines words into the same graph the world
  shows; a quest can reuse vocabulary the learner met in media (§26).
- **Stats over events**: world events (LOCATION_DISCOVERED, WORLD_TEXT_SELECTED,
  NPC_INTERACTED, QUEST_COMPLETED) flow into the same event stream as study
  events — one heatmap, one knowledge model (§210–§213).

## Shared foundations (what makes it one product)

- Same identity/theme/settings/account/updates (ADR-0014)
- Same dictionary data and lookup pipeline (dictionary popup reused in world)
- Same mining engine and card pool (world mining == media mining == dictionary mining)
- Same knowledge model and FSRS scheduling (never duplicated)
- Same statistics pipeline (events in → stats out)
- Same design system and motion language (`docs/design/`, `docs/vision/design-philosophy.md`)

## UX rules for the dual experience

1. **Never duplicate state**: if the app knows it, the world shows it (knowledge
   overlay) — never a second bookkeeping system in the world.
2. **Never break immersion for chrome**: HUD stays minimal; app chrome never
   appears inside the world except through deliberate bridges (§137 layers).
3. **Explicit exits**: leaving the world preserves everything and returns to
   exactly where the user was.
4. **One first-run**: onboarding (JLPT target → real decks) is shared; the world
   is discoverable later, never a required step.
5. **Both front-ends honor accessibility** and the reduced-motion/contrast
   settings of the shared profile.

## Related

- Spec: §70 (two experiences), §114 (shared account/data), `docs/vision/game-philosophy.md`
- Runtime layering: `docs/architecture/nodes/JOURNEY_RUNTIME_SPEC.md` §1 (UI layers)
- UX flows: `docs/architecture/nodes/UX_FLOWS.md`
- Product audit (current reality): `docs/planning/PRODUCT_AUDIT.md`
