# Input Accessibility

**Status**: LIVE (app) + TARGET (game). **Source**: expansion spec §37; STANDARDS
§254; `docs/architecture/accessibility.md` (the app-wide plan).

## Principle

Accessibility is not a checklist at the end — it is a property of the input
layer itself: **every feature is reachable through multiple input paths, and
every input preference is configurable.**

## What the input layer must support (expansion §37)

| Need | Support |
|---|---|
| Font size / text scaling | App-wide (exists); game UI scales (subtitle size, HUD text, dialog) |
| Contrast | High-contrast theme (exists — app theme system); world UI honors it |
| Color blindness | Color-blind-safe palettes for map overlays, knowledge overlay, UI states (not color-alone signals) |
| Subtitles | Subtitle size/background controls; all authored audio has subtitles (`game-audio.md`) |
| Audio descriptions | Where appropriate (cinematic beats) — optional, labeled |
| Reduced motion | Disables sweeps, jiggle, non-essential animation, particle storms, camera moves (app + world) |
| Animation speed | Configurable (settings: animation enabled/reduced/speed — `docs/vision/design-philosophy.md`) |
| Controller remapping | Per-action remap (gamepad profile) |
| Keyboard remapping | Per-action remap (keyboard profile) |
| Touch customization | Gesture presets, target size, left/right handedness |
| Difficulty | Presentation adaptation (gloss density, pacing — `docs/learning/adaptive-learning.md`); never walls |
| Reading assistance | Furigana density options, gloss on demand, TTS |
| Text-to-speech | TTS exists (app); world reuses it for language audio (`game-audio.md`) |
| Speech input possibilities | Future: optional voice answers (RESEARCH — never a required path) |

## Keyboard-first rule

Every screen (app + world) is fully operable with keyboard alone: focus order,
focus rings, no mouse-only actions, no hover-only interactions. This is enforced
in review (TEST_PLAN) and UI review (design philosophy).

## Input adaptation

- **One-handed layouts**: presets (left/right handed, compact) for app + world.
- **Hold vs tap**: no timing-sensitive actions; long-press actions have an
  equivalent tap (context menu) for motor-accessibility users.
- **Deadzones & acceleration**: configurable per device (Advanced tier).

## Never-excluded rule

No feature exists that a user *cannot* access without a specific input device or
without specific motion/vision/hearing/cognitive ability. If a feature can't meet
this, it doesn't ship as-is.

## Acceptance criteria

1. App + Journey fully playable keyboard-only; fully playable gamepad; touch.
2. Reduced-motion removes all non-essential motion (world + app).
3. No gameplay info is color-only or audio-only.
4. Accessibility settings are per-profile, synced, persisted.

## Related

- App-wide plan: `docs/architecture/accessibility.md`
- Input layer: [input-system.md](input-system.md) · Mobile: [mobile-controls.md](mobile-controls.md)
- Vision: `docs/vision/design-philosophy.md` · Standards §254
