# Kaiteyo Input (target layer)

**Status**: LIVE (existing app shortcuts + remapping) + TARGET (game action layer).
**Sources**: expansion spec §35–§37; STANDARDS §251–§253 (input), §254
(accessibility); JOURNEY_RUNTIME_SPEC §7, §13 (default mappings).

## Principle

**One abstract action layer.** UI and game code bind to *actions* (`ACTION_CONFIRM`,
`ACTION_MOVE`…), never to physical keys. Physical inputs map to actions per
profile/device; remapping never requires code changes. This is how keyboard,
mouse, touch, and controller coexist (§35).

## Document map

| Document | Covers |
|---|---|
| [input-system.md](input-system.md) | The abstract action layer, profiles, remapping, conflict checks |
| [game-controls.md](game-controls.md) | Journey action set + default mappings (keyboard/mouse/touch/gamepad) |
| [mobile-controls.md](mobile-controls.md) | Touch design: virtual joystick, gestures, context-sensitive interaction, controller support |
| [accessibility.md](accessibility.md) | Input accessibility: remapping, reduced motion, keyboard-first, assistive tech |

## Relationship to the existing app

The shipped app already has a shortcut registry (`desktopApp/.../engine/shortcuts/`),
keyboard handling in review/media, and remappable shortcuts (`ShortcutsView`). The
action layer is the **generalization**: existing shortcuts become the desktop
keyboard profile of the same action system, so Journey and the app share one input
model (`docs/architecture/NAVIGATION.md`, `docs/architecture/nodes/UX_FLOWS.md`).

## Ground rules

1. UI behavior is never hardcoded to a specific key (expansion §35).
2. Every action is reachable on every device class (keyboard-first accessibility;
   touch has gestures; controller has conventions).
3. Remapping is per-profile, conflict-checked, and persisted (existing
   `keyboard_shortcut` pattern extended).
4. Gamepad/touch connect mid-game without restart (JOURNEY_RUNTIME_SPEC §13).
5. All input settings respect the shared profile/settings store (`docs/architecture/SYNC.md`).

## Related

- Journey runtime: `docs/architecture/nodes/JOURNEY_RUNTIME_SPEC.md` §7, §13
- Camera controls: `docs/game/camera.md` · Interaction: `docs/game/interaction-system.md`
- Accessibility: `docs/architecture/accessibility.md`
