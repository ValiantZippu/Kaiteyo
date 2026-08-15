# Quest System

**Status**: TARGET (spec). **Source**: expansion spec §13; NODE §101 (quest UI),
§104 (stories); GAMEPLAY_SYSTEMS §10 (quest kinds + non-punitive rules).

## Principle

A generalized quest engine over a **node model**. Quests are data
(`docs/content/content-formats.md`), authored with objectives, conditions,
progress, completion, rewards, prerequisites, unlocks, failure/retry, branches,
and dependencies. The quest UI is a **map marker + one objective card** — not a
quest log jammed into the HUD (§101).

## Quest types (expansion §13)

learning · exploration · vocabulary · kanji · writing · listening · reading ·
dialogue · media · photography · location · cultural · collection · exam ·
daily · weekly · seasonal · story

## Quest node model

```
QUEST
 ├─ id, title (ja + gloss), type, kind, icon
 ├─ PREREQUISITE  (quest/story/world-state conditions to become AVAILABLE)
 ├─ UNLOCK        (what this quest unlocks on completion)
 ├─ OBJECTIVE[]   (1..n)
 │    ├─ CONDITION (what counts: "photograph 3 landmarks")
 │    ├─ PROGRESS  (current/required; events increment)
 │    └─ DEPENDENCY (objective ordering: parallel or sequential)
 ├─ BRANCH        (optional: choice alters path — data)
 ├─ COMPLETION    (event + reward)
 ├─ FAILURE       (only where meaningful — see non-punitive rules)
 └─ RETRY         (how to retry, if repeatable)
```

## Quest state machine (§49)

```
LOCKED → AVAILABLE → ACTIVE → (PAUSED) → COMPLETED
                    ↓              ↓
                 (REPEATABLE)   FAILED → (RETRY → ACTIVE)
```

- `LOCKED`: prerequisites not met (not shown or shown as "?"-free — see rules).
- `AVAILABLE`: shown as a marker; player may accept.
- `ACTIVE`: objective card visible; progress persists in the save.
- `PAUSED`: story/seasonal quests pause when their season ends; resume next
  season (nothing lost).
- `COMPLETED`: event fired, reward granted, unlock applied.
- `FAILED`: **only authored, non-punitive failures** — e.g., a festival quest
  ends when the festival ends. Never "you failed because you were slow."
- `REPEATABLE`: daily/weekly quests; progress resets on their cadence.

## Non-punitive rules (GAMEPLAY_SYSTEMS §10 — hard rules)

1. **Quests never brick**: an objective that becomes impossible (season ended,
   NPC left) resolves gracefully — the quest completes with partial credit or
   moves to the next season, never a dead "failed forever."
2. **No time pressure in required content**: timers exist only in authored
   optional moments (festival countdown), never in learning-required quests.
3. **Missed content is revisitable**: seasonal content returns; story quests are
   checkpointed.
4. **No failure costs progress**: no lost items, no lost collections, no
   knowledge penalties. The world is generous.
5. **Learning quests never quiz-lock**: a "vocabulary quest" asks the learner to
   *find* words in the world (discovery), never "answer or you can't continue."

## Quest UI (NODE §101)

- **Marker**: the active objective is a map marker — the quest UI *is* the map.
- **Objective card**: one card (top corner, dismissible) with current objective +
  progress; disappears when idle.
- **Quest view** (Q / shoulder button): a calm panel listing active/completed
  quests with journal text — no urgency chrome, no "incomplete!" guilt.

## Daily / weekly / seasonal

- Daily: small, varied (photograph today's flower, talk to 3 NPCs, review 10
  words in the world) — designed to *reward* existing habit, never to force.
- Weekly: themed around the region/season.
- Seasonal: festival quests, beach season, New Year shrine visit — the world's
  rhythm (see `environment-simulation.md`).
- Cadence is data (authorable), not engine constants.

## Quest ↔ learning connection

- Quests consume knowledge state: a "words you're weak on" quest surfaces
  vocabulary the learner has met but not mastered (recommendation-driven, §40
  spec / `docs/learning/adaptive-learning.md`).
- Quest completion emits events that feed stats and the knowledge overlay.
- Media quests connect the world to the Media Centre (§26): "watch one episode,
  find this word."

## Acceptance criteria

1. Any authored quest runs with zero engine changes (data-driven).
2. No authored quest can brick (validation gate + runtime guard; TEST_PLAN).
3. Quest UI never exceeds: one marker + one objective card + one calm panel.
4. All quest events flow to the shared event stream; no quest-local bookkeeping
   duplicates stats.

## Related

- Content format: `docs/content/content-formats.md` (quest schema)
- Progression/rewards: [progression-rewards.md](progression-rewards.md)
- Spec: NODE §101, §104; GAMEPLAY_SYSTEMS §10; STANDARDS §49 (state machines)
