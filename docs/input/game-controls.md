# Game Controls (Journey — target)

**Status**: TARGET (spec). **Source**: JOURNEY_RUNTIME_SPEC §13 (default input
mapping); STANDARDS §251–§253.

## Action set (JOURNEY_RUNTIME_SPEC §7)

Move · Look · Interact · Run · Camera (switch 1st/3rd) · Map · Dictionary ·
Journal · Quest · Inventory · Pause · Screenshot · Confirm · Back

All mapped separately per device, remappable, profile-scoped, conflict-checked.

## Default mappings (JOURNEY_RUNTIME_SPEC §13)

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

## Settings tiers (§253)

- **Simple**: preset schemes + a few toggles (invert look, sensitivity).
- **Advanced**: per-action remap, camera sensitivity, stick deadzone, look
  acceleration, vibration toggle.
- All settings persisted per profile, synced with the shared settings store.

## Behavioral rules

1. Gamepad follows platform conventions (Nintendo-style A=confirm on the
   dominant bottom button; Xbox/PlayStation labels localized).
2. Touch: an on-screen control scheme replaces mouse-look when touch input is
   detected; controllers connect mid-game without restart (§13).
3. Every action is also reachable via keyboard-first navigation (STANDARDS §254).
4. Hold-to-run never conflicts with hold-to-aim; the action layer resolves
   simultaneous presses deterministically (priority order documented).

## Accessibility hooks

- Reduced motion disables camera sweeps/jiggle (in `camera.md`).
- Vibration is off by default; toggleable.
- Subtitles/subtitle controls per `game-audio.md`.

## Acceptance criteria

1. Full Journey playable with keyboard alone; full playable with gamepad; full
   with touch.
2. Every binding in the table is remappable and persists.
3. Mid-game device changes work without restart.

## Related

- Action layer: [input-system.md](input-system.md)
- Touch specifics: [mobile-controls.md](mobile-controls.md)
- Camera: `docs/game/camera.md` · Runtime: `docs/architecture/nodes/JOURNEY_RUNTIME_SPEC.md` §13
