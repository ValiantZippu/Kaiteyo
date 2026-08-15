# The Journey — Player, Camera & Input

> **Status**: `ARCHITECTED` (TARGET). Input abstractions for the existing app are
> partially current (shortcut registries in core + suite); game input is target.

## 1. Player / avatar (NODE §97)

The avatar system: appearance, clothing, accessories, animation, emotes, walking,
running, swimming, sitting, riding, photography.

Art direction (see [`docs/rendering/rendering-architecture.md`](../rendering/rendering-architecture.md)):
stylized, cozy, clean, expressive, simple enough for cross-platform performance.
**Explicitly avoided**: generic Roblox-like proportions, generic mobile-game
characters, overcomplicated AAA character production. A coherent artistic direction
is mandatory.

## 2. Camera (MASTER §26, NODE §96)

**Both first-person and third-person are supported**, switched by a deliberate camera
action (never automatic mid-interaction). Full camera spec: [`camera.md`](camera.md).

| Mode | Best for | Requirements |
|---|---|---|
| FIRST PERSON | photography, reading signs, immersion, dictionary interaction, exploration, fine interaction | standard FOV range (60–90 adjustable), look sensitivity, collision (no wall clips), head-bob off option |
| THIRD PERSON | character movement, social scenes, walking, animations, avatar/cosmetic visibility | camera collision + smoothing, shoulder offset, camera height setting |

- Camera smoothing; sensitivity settings; FOV setting; camera height (accessibility).
- Mobile camera: virtual stick + look-drag; controller camera: right-stick; mouse
  camera: pointer-lock or drag per platform (`docs/input/`).
- Accessibility: motion-reduction camera, disable head-bob/shake, steady-cam and
  reduced-FOV-change options required.

## 3. Input abstraction (MASTER §27, NODE §27)

**Never hard-code UI/game actions to one physical input.** Define actions, then map
physical controls per platform. Full action layer: `docs/input/input-system.md`.

### Action set

`MOVE` · `LOOK` · `INTERACT` · `BACK` · `CONFIRM` · `CANCEL` · `SPRINT` · `JUMP` ·
`CAMERA` (mode switch) · `MENU` · `MAP` · `INVENTORY` · `QUESTS` · `DICTIONARY` ·
`SUBTITLES` · `PAUSE`

(Existing app actions — palette, navigation, review grading — already follow this
model via `ShortcutRegistry` (suite) and review shortcuts (core); the game keymap
extends the same concept.)

### Physical mappings (defaults — all editable; see `docs/input/game-controls.md`)

| Action | Keyboard/mouse | Controller | Touch |
|---|---|---|---|
| MOVE | WASD | left stick | virtual stick |
| LOOK | mouse | right stick | drag |
| INTERACT | E / click | A (or Y) | tap prompt |
| BACK | Esc | B | back gesture |
| CONFIRM | Enter / click | A | tap |
| CANCEL | Esc | B | back |
| SPRINT | Shift | left stick click | sprint button |
| JUMP | Space | X | jump button |
| CAMERA | V | right stick click | camera toggle |
| DICTIONARY | Ctrl+D (app standard) | LB+start | long-press text |
| PAUSE | Esc/P | start | pause button |

### Requirements

- Per-platform keymaps persisted (`docs/ui/SETTINGS.md` game section).
- Keymap editor UI with reset.
- Gamepad hot-plug detection; touch + gamepad coexist on mobile.
- Accessibility: remap everything; toggle-hold options for sprint.

## 4. Interaction prompts (NODE §139)

Near an object: small contextual prompt — `[Interact]` then the Japanese name + gloss
(`おにぎり / Onigiri`). No giant panel unless expanded. Full spec:
`docs/architecture/nodes/JOURNEY_RUNTIME_SPEC.md` §HUD/Interaction and
[`interaction-system.md`](interaction-system.md).

## Related

- [`game-overview.md`](game-overview.md) · [`camera.md`](camera.md) ·
  [`interaction-system.md`](interaction-system.md)
- Input layer: `docs/input/` (input-system, game-controls, mobile-controls, accessibility)
- Runtime: `docs/architecture/nodes/JOURNEY_RUNTIME_SPEC.md`
- Gameplay: `docs/architecture/nodes/GAMEPLAY_SYSTEMS.md`
- Settings: `docs/ui/SETTINGS.md`
