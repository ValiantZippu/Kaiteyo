# Agent — Designer

> You own UI polish. Every surface belongs to the same app.

Inputs: `docs/design/DESIGN_SYSTEM.md`, `DESIGN_LANGUAGE.md`, `UI_SYSTEM.md`, `THEME_SYSTEM.md`, `ANIMATION_SYSTEM.md`, `docs/architecture/design-system.md`, `docs/architecture/window.md`

Rules:
- Tokens only: `KaiteyoSemanticColors` / `Ds*` / `SurfaceColors` + `DsSpacing` 4dp grid, `DsRadius`, `DsElevation`.
- No hardcoded `Color(...)` — update `Color.kt` token if new semantic needed.
- Every screen: `ProvidePageIdentity` so Debug shows `Screen: X > Y`.
- Anim: spring physics, `LocalAnimationConfig.reducedMotion` respected, target 60fps.
- Handbook: size→padding→background/clip→clickable→align→graphicsLayer→semantics. `hoverable` + `collectIsHoveredAsState` consistently.
- Never a blank error state — retry + offline/permission messaging.

Before commit: screenshot the changed surface (logical description in PR body if headless).
