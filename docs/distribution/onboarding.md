# Onboarding Architecture

Onboarding is **modular, skippable, and never repeated for existing users**.
It runs after install → launch (see [first-launch.md](first-launch.md)).

## Two phases (kept separate by design)

1. **Installer phase** (per-OS tool) — location, components, shortcuts,
   launch checkbox.
2. **App phase** (`OnboardingWizard`) — first run, before the main UI.

The app phase is gated by the persisted `onboarding.completed` flag
(SettingsEngine key `onboarding.completed`, exposed via `AppState`). It shows
**once**, regardless of install method, and can be re-opened from Settings →
"Show onboarding again".

## Current app-phase steps

| Step | Purpose | Skip |
|---|---|---|
| Welcome | Brand intro | ✓ |
| Theme | light / dark / oled / sepia, live preview | ✓ |
| Accent color | accent scheme | ✓ |
| UI scaling | 80–160%, live | ✓ |
| Font size | reading size, sample | ✓ |
| Navigation | sidebar position + layout | ✓ |
| Motion | animation preset | ✓ |
| All set | summary + Get Started | — |

Every step is skippable ("Skip all" jumps to the end). Choices write straight
into the real theme/navigation state — the app behind the wizard re-themes live.
Completion is persisted before the main UI shows, so a crash mid-wizard can
never re-trigger it. All transitions are animated (reduced-motion honored).

## Designed-for future steps (not yet built)

The directive's fuller flow — language, experience level, goals, account,
dictionary setup, content import — is **not** force-fitted into the current
wizard. The current flow deliberately covers appearance + layout only; import,
dictionaries and plugins stay available from their own workspace views right
after onboarding (Transfer, Dictionary, Plugins). Adding steps is a content/UI
change, not an architecture change — the wizard is modular and skippable by
design.

## First-run detection

- Stored as a real persisted flag (`onboarding.completed`), **not** inferred
  from "a file happens to be missing".
- Corrupted state (flag readable but invalid) falls back to safe defaults and
  is handled separately from first-run detection.
- Migration: an update **never** re-triggers onboarding. Existing users get a
  migration screen only when their data actually needs migrating
  (see [first-launch.md](first-launch.md)).

## Resume

If onboarding is interrupted, it resumes at the correct step — the user is not
forced to restart the wizard.

## Accessibility

- Full keyboard navigation (Tab/Enter/Esc), visible focus, hover states.
- Text scales with UI-scale; contrast follows the theme; reduced motion
  disables step transitions.
- Large touch targets on Android.

## Rules

1. Never show onboarding twice (unless the user asks).
2. Never block usage behind an unskippable step.
3. Never fake progress — any progress shown is real
   (see [first-launch.md](first-launch.md) for DB init).
4. Updates never replay first-run onboarding.
