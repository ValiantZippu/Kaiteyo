# Mobile Controls (target)

**Status**: TARGET (spec). **Source**: expansion spec §36; JOURNEY_RUNTIME_SPEC
§13 (touch notes).

## Principle

Touch is **designed, not shrunk**. Mobile controls are a first-class input model:
virtual joystick, camera gestures, tap/hold/swipe/drag/pinch, context-sensitive
interaction, gesture customization, and controller support (mobile games on
phone+controller are real).

## Touch vocabulary (expansion §36)

| Gesture | Maps to |
|---|---|
| Left virtual stick | Move (dynamic: appears where the thumb lands) |
| Drag (right side) | Look (FP) / camera orbit (TP) |
| Tap | Interact / confirm |
| Hold | Context menu / secondary (long-press = dictionary on text — `interaction-system.md`) |
| Swipe | Pan (map), quick-look, back gesture |
| Pinch | Camera zoom, map zoom |
| Two-finger double-tap | Camera mode switch (V equivalent) |
| Drag object → destination | Move/place interactions (e.g., photography composition) |

## Virtual joystick design

- **Dynamic origin**: the stick appears where the thumb touches the left zone —
  no fixed dead zone on the screen; disappears when released.
- Sensitivity adjustable (Advanced tier); stick deadzone configurable.
- Run: thumb-press deeper or a dedicated run button (child mode: auto-run
  option, big buttons).

## Context-sensitive interaction

- Proximity prompts become **one floating action button** (or contextual ring):
  tap = interact; long-press = options. No tiny screen-space hunting.
- Interaction targets are generous (touch hit areas, `interaction-system.md`).
- No gesture required by the learning loop is discoverable only by accident —
  the tutorial covers gestures; help is one tap away.

## Gesture customization

- Presets: Default / Left-handed / Large targets / Reduced motion.
- Advanced: rebind gestures (within a safe catalog — gestures that conflict with
  system navigation are excluded).
- Persisted per profile; synced.

## Controller on mobile

- Bluetooth/USB gamepads supported; same gamepad mappings as desktop
  (`game-controls.md`).
- When a controller connects, touch controls hide (but remain available on
  touch taps — mixed input works).

## Accessibility

- Bigger touch targets option; haptic feedback toggle; no timing-sensitive
  gestures; keyboard-free play on tablet with Bluetooth keyboard optional.
- Touch controls never overlap the HUD or hide content (layout-aware).

## Acceptance criteria

1. Full Journey playable on phone touch alone; full playable with controller.
2. Dynamic joystick never blocks view or HUD.
3. Gestures customizable; defaults never conflict with system gestures.
4. Child mode gets larger targets + optional auto-run (`docs/vision/child-experience.md`).

## Related

- Action layer: [input-system.md](input-system.md) · Gamepad: [game-controls.md](game-controls.md)
- UX: `docs/vision/design-philosophy.md` (responsive) · Platform: `docs/platform/ANDROID.md`
