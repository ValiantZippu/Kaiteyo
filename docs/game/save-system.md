# Save System

**Status**: TARGET (spec). **Source**: expansion spec §38; NODE §144 (save);
JOURNEY_RUNTIME_SPEC §9, §18; STANDARDS §205–§207.

## Principle

**One unified save state** with versioned, checksummed, recoverable files. The
save covers player + world + game state; learning data lives in shared user data
(never the save) — the split rule (`player.md`, §144).

## What the save contains

| Data | In save | In shared user data |
|---|---|---|
| Player position, camera prefs, appearance, inventory | ✅ | — |
| World progress: unlocked cells, revealed map, clock, weather seed, season | ✅ | — |
| Quests (state, objective progress), story state | ✅ | — |
| Discoveries, collections, photos | ✅ | — |
| NPC relationships | ✅ | — |
| World/game settings | ✅ | — |
| Knowledge, reviews, cards, decks, stats, media history, achievements | — | ✅ |

Save shape (JSON, sparse overrides over immutable content — JOURNEY_RUNTIME_SPEC §9):

```json
{
  "saveVersion": 1,
  "userRef": "...", "worldId": "japan",
  "player": {...}, "worldProgress": {...}, "quests": {...},
  "discoveries": [...], "collections": [...], "photos": [...],
  "npcRelationships": {...}, "storyState": {...}, "worldSettings": {...}
}
```

## Save lifecycle (JOURNEY_RUNTIME_SPEC §9, §18)

1. **Write**: atomic (write-temp-then-rename), checksummed, on exit + on
   significant milestones (quest complete, discovery, photo).
2. **Restore**: last good save; deterministic world (same save → same state).
3. **Corruption**: checksum mismatch → recover to last good save with an
   explanation; never crash, never silent data loss (STANDARDS §219).
4. **Migration**: `saveVersion` is monotonic; **unknown/newer versions refuse to
   load with a clear message** instead of migrating blindly. Version upgrades are
   explicit, tested, and backward-compatible where promised.
5. **Backup/export**: saves are included in backups (STANDARDS §205–§206) and
   exported as semantic, versioned objects (STANDARDS §207) — never tied to
   internal storage layout.

## Integrity rules

1. **Schema-level guard**: learning data never enters the save (a guard test
   exists in TEST_PLAN §9.5) — verified, not assumed.
2. Deterministic serialization: same state → same bytes (stable key order,
   stable float formatting) so checksums are stable.
3. Sparse overrides: content (packages) is immutable; the save only stores
   deltas — saves stay small and migration-safe across package versions.
4. Save files never contain secrets or the user's study data; they are safe to
   share/export (photos/collections are the player's own content).

## Multi-surface consistency

- **Cross-surface**: world progress shows in app stats (and vice versa) because
  both read shared data + the save. No world-only statistics.
- **Profile scoping**: saves are per user/profile; a child-mode profile and adult
  profile can coexist without data bleed (shared core, separate saves where
  gameplay differs).

## Acceptance criteria

1. Save/load is deterministic and survives app restarts.
2. Corrupt saves recover with explanation, never crash.
3. Newer-version saves refuse to load with a clear message (no blind migration).
4. The schema guard test proves learning data cannot enter the save.

## Related

- Player state: [player.md](player.md)
- Backup: `docs/architecture/backup.md` · Migration: `docs/architecture/database.md` §5
- Spec: NODE §144; JOURNEY_RUNTIME_SPEC §9, §18; STANDARDS §205–§207, §219
