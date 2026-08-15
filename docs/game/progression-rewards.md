# Progression & Rewards

**Status**: TARGET (spec). **Source**: expansion spec §13; NODE §116–§117
(progression, rewards, anti-grind); GAMEPLAY_SYSTEMS §20 (progression), §21
(rewards catalog).

## Principle

Progression is **discovery, knowledge, story, and collections** — never XP, loot,
levels, or stat points. Rewards are meaningful moments, not currency. The game is
generous and anti-grind by construction (§117).

## What "progression" means here

| Axis | Progresses by | Visible as |
|---|---|---|
| **Map reveal** | visiting/discovering places | revealed districts, discovered markers (`map-system.md`) |
| **Knowledge** | encountering/learning words, kanji, grammar | knowledge overlay, shared knowledge model |
| **Collections** | photography, stamps, discovered words/kanji, seasonal items | collection books (`collectibles-photography.md`) |
| **Story** | quest chains, dialogue, world events | journal, story beats, NPC relationships |
| **Relationships** | optional friendship moments | NPC dialogue flavor, small scenes |
| **Achievements** | real milestones (study + world) | achievements list (shared with the app) |

## Rewards catalog (GAMEPLAY_SYSTEMS §21)

Rewards are **non-currency, non-power**:

| Reward | What it does |
|---|---|
| Story beat / scene | new dialogue, a moment, journal entry |
| Photo op / collectible | a collection entry (stamp, photo, discovery) |
| Map reveal | a district opens (`map-system.md`) |
| NPC moment | optional friendship dialogue |
| Language unlock (presentation) | gloss settings unlock, "learn this word" practice spot |
| Cosmetic | avatar accessory (child mode loves this; adult mode optional) |

**Explicitly not rewards**: +XP, +gold, stat points, loot, energy, lives,
timers, gacha, leaderboard points. Achievements exist but are records, not the
loop (§117; `docs/vision/game-philosophy.md`).

## Anti-grind rules (§117, GAMEPLAY_SYSTEMS §20)

1. **Nothing repeatable-in-place for points.** A photo spot gives one entry; a
   discovery is discovered once.
2. **No daily-checklist tyranny.** Daily quests *reward* existing habits; they
   never punish absence (no streak-loss penalties, no "come back or lose").
3. **No artificial scarcity for engagement.** Seasonal content returns; nothing
   is FOMO-priced into the core loop.
4. **Learning never requires grinding.** Review is a study-app concern (FSRS);
   the world exposes, it doesn't farm.
5. **Difficulty adaptation** (§113, `docs/learning/adaptive-learning.md`) tunes
   presentation and pacing — never gates rewards behind skill walls.

## Achievements (shared)

- Achievements span study + world (one list, one data store — shared user data).
- World achievements are milestone records: "Rode every Enoden station",
  "Photographed the festival", "Met all of Komachi's shopkeepers."
- Never unlock-gated progression; never hide the list; never "secret
  achievements" that punish discovery.

## Economy — there is none (§117)

The game has **no currency system**. Where purchase interactions exist (a shop,
a ticket), they are *story/learning flavor*: you buy a stamp book entry, a
ticket for the ferry — with story items, not accumulated money. This kills an
entire class of grind and keeps the world calm (design philosophy).

## Difficulty & child mode

- Progression pace adapts: child mode gets gentler cadence and celebration
  (`docs/vision/child-experience.md`); adult mode gets depth.
- The knowledge overlay is the "difficulty display": what you know here is
  visible — the world never grades you.

## Acceptance criteria

1. No numeric progression (XP/level/stat) exists anywhere in the data model.
2. No authored quest can be farmed for repeatable rewards (validation gate).
3. All rewards are non-currency and non-power (data schema enforces reward kinds).
4. Achievements are shared across app + world with no duplication.

## Related

- Quests: [quest-system.md](quest-system.md)
- Collections: [collectibles-photography.md](collectibles-photography.md)
- Spec: NODE §116–§117; GAMEPLAY_SYSTEMS §20–§21; STANDARDS §117
