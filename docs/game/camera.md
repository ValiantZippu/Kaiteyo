# Camera System

**Status**: TARGET (spec). **Source**: expansion spec §6; NODE §96 (camera modes);
JOURNEY_RUNTIME_SPEC §4.

## Principle

**Both first-person and third-person exist. Neither is forced permanently.** The
camera is a mode with an explicit, remappable switch — and per-interaction camera
preference is declared *data* (an interaction node says "best in first person"),
never a hard lock (§96).

| Mode | Used for | Characteristics |
|---|---|---|
| **First person** | Photography, reading signs, immersion, fine interaction, dictionary use | Bodyless view, high detail proximity, reticle interaction |
| **Third person** | Movement, social scenes, avatar visibility, exploration | Shoulder/follow camera, avatar in frame, better spatial read |

## Camera modes (expansion §6)

| Mode | When appropriate | Behavior |
|---|---|---|
| First-person exploration | Default in FP | Mouse-look (drag on touch), head bob off by default |
| Third-person shoulder | Default in TP | Offset behind-right shoulder, obstacle avoidance |
| Vehicle camera | Trains (seated view), cars, boats | Fixed/rail camera respecting the vehicle interior; window view |
| Swimming camera | Swimming/diving | Lower waterline view, clear water surface handling |
| Underwater camera | Diving | Distinct color/tone, motion damping, bubble trail (presentation) |
| Photography camera | Photography mode | Free look + aim; composition aids; framing overlay |
| Dialogue camera | NPC conversations | Medium close-up framing; both speaker and player visible; never lock input for long |
| Cinematic camera | Story beats, train departures, festival moments | Authored shots (data-driven per beat), skippable, reduced-motion aware |

## Camera switching rules

1. One deliberate switch action (V / Y / two-finger double-tap — `docs/input/`),
   remappable.
2. Per-interaction declared preference: e.g., a sign's interaction node declares
   `camera: first-person`; entering it suggests a switch (does not force).
3. Photography mode opens its own camera (composed view) and returns on exit.
4. Dialogue/cinematic cameras are *contextual* — they take over during the beat and
   return; never permanent.

## Camera engineering requirements

- **Collision**: the camera never clips geometry; near-camera obstacle pushes the
  camera in smoothly (shoulder camera) or fades the obstruction (FP); never snaps.
- **Smoothing**: spring-damped follow (respects reduced motion — see below);
  look smoothing with configurable sensitivity; no input lag spikes.
- **Sensitivity / FOV**: configurable (per profile), FOV adjustable (60–100°,
  default ~70); gamepad look uses its own curve + deadzone (see `docs/input/`).
- **Zoom**: scroll/pinch zoom in FP (photography) with configurable step;
  shoulder-camera pull-in for detail viewing.
- **First-person interaction**: reticle-based interaction (see
  `interaction-system.md`); FP interaction target uses the crosshair, TP uses a
  soft screen-center ray with aim assist on interaction nodes (subtle, never
  magnetic snapping of camera).
- **Third-person exploration**: obstacle-aware follow, avatar offset respects
  facing; camera never enters walls; transitions between modes are animated
  (spring), never cut (except reduced-motion fade).

## Accessibility (STANDARDS §254; `docs/vision/design-philosophy.md`)

- **Reduced motion**: camera sweeps, jiggle, cinematic pans, and mode transitions
  become fades/static framing; no vestibular-triggering motion.
- **Sensitivity & inversion**: look inversion (X/Y), sensitivity sliders, stick
  deadzone, look acceleration — all configurable, profile-scoped.
- **Camera shake**: none by default; authored shake is opt-in (or off entirely).
- **Seizure safety**: no strobe/rapid flashing in any camera effect.

## Acceptance criteria

1. FP↔TP switch is instant (≤1 frame state), animated over ≤300ms, and never
   disorients (camera keeps world-space target).
2. No camera ever intersects geometry in authored content (tested in TEST_PLAN).
3. All camera params configurable and persisted per profile.
4. Reduced-motion mode removes all non-essential camera motion.

## Related

- Input: `docs/input/` (look, sensitivity, deadzone)
- Rendering: `docs/rendering/rendering-architecture.md` (camera/LOD interplay)
- Spec: NODE §96; JOURNEY_RUNTIME_SPEC §4, §7, §13; STANDARDS §254
