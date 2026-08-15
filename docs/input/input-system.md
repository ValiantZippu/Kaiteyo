# Input System — Abstract Action Layer

**Status**: TARGET (game) / evolving (app shortcuts → actions). **Source**:
expansion spec §35; STANDARDS §251–§253.

## The action layer

```
PHYSICAL INPUT (keyboard / mouse / touch / gamepad)
      ↓ (input devices capture)
ACTION LAYER (ACTION_MOVE, ACTION_INTERACT, ACTION_CONFIRM …)
      ↓ (mapping: action → device bindings, per profile)
GAME/APP CODE (binds to actions only)
```

- Code asks "what is the primary fire/interact?", never "is the spacebar down?"
- New devices (e.g., a future gyro) only need a new *input device* adapter —
  game/app code is untouched.

## Action catalog (expansion §35 + Journey)

| Action | App context | Journey context |
|---|---|---|
| `ACTION_CONFIRM` | enter, accept | dialogue advance, menu select |
| `ACTION_CANCEL` | back, close | close panel, exit dialogue |
| `ACTION_MOVE` | — | movement (vector) |
| `ACTION_LOOK` | — | camera look (vector) |
| `ACTION_INTERACT` | — | interaction prompt |
| `ACTION_MENU` | open nav/launchpad | open world menu |
| `ACTION_BACK` | back navigation | back / exit context |
| `ACTION_JUMP` | — | (target: parkour-lite only if authored; default off) |
| `ACTION_SPRINT` | — | run |
| `ACTION_CAMERA` | — | camera mode switch |
| `ACTION_PRIMARY` | — | photography shutter / primary action |
| `ACTION_SECONDARY` | — | contextual secondary (zoom, alt-inspect) |
| App actions | search, open dictionary, mining, screenshot… | dictionary/knowledge overlay, journal, quest, inventory, map, pause, screenshot |

## Profiles & remapping

- **Per-profile bindings**: default keyboard, mouse, touch, gamepad profiles; user
  overrides stored per profile (shared settings).
- **Simple/Advanced tiers** (§253): Simple exposes a few presets; Advanced exposes
  per-action remap, camera sensitivity, stick deadzone, look acceleration,
  vibration toggle.
- **Conflict checking**: rebinding is rejected or auto-resolved with a clear
  message (existing shortcut registry behavior extended).
- **Persistence**: bindings survive restart; corrupt bindings fall back to
  defaults with a notice (never a crash — STANDARDS §219).

## Device mapping defaults (see `game-controls.md` for the full tables)

| Action | Keyboard | Mouse | Touch | Gamepad |
|---|---|---|---|---|
| Move | WASD / arrows | — | virtual stick / swipe | left stick |
| Look | mouse look | drag (FP) | drag | right stick |
| Interact | E / Space | left click | tap | A |
| Confirm/Back | Enter / Esc | left / right click | tap / back gesture | A / B |
| Camera switch | V | — | two-finger double-tap | Y |
| Dictionary | D | right click | long-press | X |
| Map/Journal/Quest | M / J / Q | — | buttons | Select/B / R-shoulder |
| Pause | Esc / P | — | pause button | Start |

## Rules

1. Every action reachable by keyboard alone (accessibility, STANDARDS §254).
2. Action names are stable API — renaming is a breaking change (documented).
3. The action layer is platform-neutral; input *capture* is platform-specific
   (existing `expect/actual` patterns).
4. No timing-sensitive actions in the app; Journey avoids them too (nothing
   requires a quick tap — `interaction-system.md`).

## Acceptance criteria

1. Remapping a key updates every surface using that action (app + world).
2. Same action behaves identically across devices (tested per device class).
3. Bindings survive restart; conflicts resolve gracefully.
4. New device support = new input-device adapter only.

## Related

- Journey defaults: [game-controls.md](game-controls.md)
- Existing app shortcuts: `docs/architecture/NAVIGATION.md`, `desktopApp/.../engine/shortcuts/`
- Spec: STANDARDS §251–§253
