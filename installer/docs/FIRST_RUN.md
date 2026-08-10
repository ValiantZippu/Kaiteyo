# First-Run Experience

Two phases, deliberately separate:

1. **Installer phase** (per-OS tool): location, components, shortcuts.
2. **App phase** (`OnboardingWizard` in the desktop app): shown once, right after
   first launch, before the main UI.

## Phase 2 — the onboarding wizard

| Step | What it does | Skip |
|------|--------------|------|
| 1. Welcome | Brand intro with a tour of the study space | ✓ |
| 2. Choose Theme | Base mode (light/dark/oled/sepia) with live preview | ✓ |
| 3. Accent Color | Accent scheme picker (disabled in Sepia, as in-app) | ✓ |
| 4. UI Scaling | Display scale slider 80–160% with live preview | ✓ |
| 5. Font Size | Reading size (Small → Extra large) with sample text | ✓ |
| 6. Navigation | Sidebar position + layout mode with window preview | ✓ |
| 7. Motion | Animation presets (Off → Cinematic) | ✓ |
| 8. You're all set | Summary + "Get Started" | — |

Rules:

- Every step skippable; "Skip all" jumps straight to the end.
- All transitions animate (fade + slide, spring-based); reduced-motion is honoured.
- **Choices are applied live**: theme/accent/scaling/typography/navigation/motion
  write straight into `AppState.themeManager` / navigation state, so the app
  behind the wizard re-themes in real time.
- The wizard appears **once**. Completion is recorded synchronously via
  `AppState.completeOnboarding()` before the main UI is shown, so a crash
  mid-wizard can never re-trigger it.

## Where the flag lives

`AppState.onboardingCompleted` — backed by the persistent settings key
`onboarding.completed` (SettingsEngine, see `desktop/engine/settings/SettingsEngine.kt`):

| API | Effect |
|-----|--------|
| `AppState.onboardingCompleted` | whether the wizard has finished |
| `AppState.completeOnboarding()` | persist `onboarding.completed = true` |
| `AppState.requestOnboarding()` | reset flag + set `onboardingRequested` → wizard re-shows immediately |
| `KaiteyoDesktopSuite` | mounts `OnboardingWizard` when `!onboardingCompleted` or requested |

Settings → General → **"Show onboarding again"** is the only way the wizard
returns after completion.

## Import / dictionaries / plugins

The current wizard covers appearance and layout only, by design (those are the
settings users most often want before first use). Import workflows, dictionary
installation and the plugin marketplace remain available immediately after
onboarding from their own workspace views (Transfer, Dictionary, Plugins) — the
first-run flow deliberately stays focused and skippable.

## Accessibility

- Full keyboard navigation (Tab/Enter/Esc), visible focus, hover states.
- Text scales with the chosen UI-scale; contrast follows the theme.
- "Reduce motion" (app setting) disables step transitions.
