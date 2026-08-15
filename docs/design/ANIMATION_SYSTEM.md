# Kaiteyo (書いてよ) — Animation System

> This document describes the animation system **as implemented**. Grounding:
> `core/.../presentation/common/theme/Theme.kt` (`AnimationConfig`, `AnimationSpeed`,
> `springAnim`, `tweenDuration`, `pageTransitionSpec`, theme crossfade) and
> `desktopApp/.../desktop/designsystem/DsTokens.kt` (`DsMotion`), plus the concrete
> patterns in `DsButtons.kt`, `DsDialog.kt`, `DsToast.kt`, `WorkspaceShell.kt`,
> `NavShell.kt`, `KaiteyoDesktopSuite.kt` and `OnboardingWizard.kt`.

## Animation philosophy

- **Subtle** — never distracting; motion guides attention, never competes with it.
- **Fast** — micro-interactions 120–380ms (`DsMotion.Fast/Normal/Slow`), scaled by
  the user's speed multiplier.
- **Natural** — springs for organic feel (`spring(damping=0.5..0.7)`).
- **Consistent** — the same interaction animates the same way everywhere; every
  animation goes through `springAnim()`/`tweenDuration()` so user config applies
  globally.
- **Respectful** — `reducedMotion` disables motion app-wide (all helpers return
  instant specs), and `AnimationSpeed.Instant` zeroes durations.

## Motion configuration (`AnimationConfig`)

```kotlin
data class AnimationConfig(
    val speed: AnimationSpeed = AnimationSpeed.Normal,   // Slow 1.5× / Normal 1.0× / Fast 0.6× / Instant 0.0×
    val reducedMotion: Boolean = false,
    val springDamping: Float = 0.6f,
    val springStiffness: Float = 300f,
    val defaultDuration: Int = 300,
    val pageTransition: PageTransitionType = PageTransitionType.FadeThrough,
    val themeTransitionEnabled: Boolean = true
)
```

`PageTransitionType`: Crossfade / Slide / FadeThrough (default) / Scale.

### Core helpers (`Theme.kt`)

```kotlin
fun springAnim(config, dampingRatio = config.springDamping, stiffness = config.springStiffness): FiniteAnimationSpec<Float>
fun tweenDuration(config, baseDuration = config.defaultDuration): Int
    // 0 when reducedMotion; else baseDuration × speed.multiplier
fun <S> pageTransitionSpec(config): AnimatedContentTransitionScope<S>.() -> ContentTransform
    // fade-through: fadeIn + fadeOut at 350ms base
fun snapSizeTransform() / snapToBiggerSizeTransform(delay)
fun <S> snapToBiggerContainerCrossfadeTransitionSpec(delay)
```

Every view/component that animates uses these helpers — never raw constants — so a
Motion Studio / onboarding speed change retunes the whole app instantly.

## Desktop tokens (`DsMotion`)

| Token | Value |
|-------|-------|
| `DsMotion.Fast` | 120ms |
| `DsMotion.Normal` | 240ms |
| `DsMotion.Slow` | 380ms |

`DsMotion.duration(base)` applies speed × reduced-motion the same way as the shared
`tweenDuration`.

## Interaction animations

### Buttons (`DsButtons.kt`)

- Background color: `animateColorAsState`, `tween(160)` between rest/hover/pressed
  (`colorsFor(kind)`).
- Scale: `animateFloatAsState` with `spring(damping=0.6, stiffness=500)` —
  pressed 0.97, hovered 1.02, else 1.
- Icon buttons: background `tween(160)` on hover; tint flips to accent.

### Cards (`DsCards.kt`)

- Interactive `DsCard` lifts: `animateDpAsState` elevation 0 → 2dp (`tween(200)`) and
  reveals a 2dp accent top line on hover.
- `DsSkeleton` pulses alpha 0.45↔1.0 with `infiniteRepeatable(tween(900), Reverse)`;
  static at 0.6 alpha under reduced motion.

### Dialogs (`DsDialog.kt`)

Shared `dialogEntranceLayer()`: scale 0.94→1 with
`spring(DampingRatioMediumBouncy, StiffnessMediumLow)` + fade `tween(180)` (0ms under
reduced motion), applied via `graphicsLayer` so it never re-lays out.

### Menus & selects

- `DsSelect` chevron rotates 0→180° (`spring(0.6, 500)`).
- `DsMenuItemRow` background `tween(140)` hover fill; selected row accent 14%.
- `DsTagChip` background `tween(160)` between 16%/25%/35% alpha states.

### Toasts (`DsToast.kt`)

- Entrance: slide up ¼ height + fade (`tween(240)` via `tweenDuration`).
- Icon spring pop: 0.5→1.08 (`spring(0.45, 480)`) then settle to 1
  (`spring(0.55, 900)`); skipped for reduced motion / Instant speed.

## Navigation & layout animations

### NavShell (`NavShell.kt`)

- Sidebar ↔ Floating mode crossfade: fade + scale (0.86/0.9) with transform origins
  following the edge/snap point; springs `spring(0.7, 300)`; all specs swap to `snap()`
  when animations are off.
- Dock edge change reflows with directional slide (spring) + fade (`tween(180)`).
- Content reserve (sidebar width) animates with `animateDpAsState` + `navAnimSpec`,
  but **snaps** during a live window resize (`LocalWindowResizing`) to avoid
  frame-chasing.
- Nav item hover/selected fills are immediate (state colors); focus shows 2dp accent
  border.

### Workspace shell (`WorkspaceShell.kt`)

- Compact ↔ desktop layout tier flips via `AnimatedContent` fade (`tween(160)`,
  duration resolved from config) — only when the tier actually flips (hysteresis).
- Dock show/hide: slide + fade `tween(240)` in the dock direction.
- View/tab transitions: `AnimatedContent` slide (direction follows tab order) + fade
  at `tween(280)`; exit slides at ⅓ speed; each tab's subtree is recreated so
  per-instance state restores from the snapshot.

### Floating launcher & launchpad

`BubbleLauncher`/`Launchpad`/`FloatingLauncher` use scale + fade entrances with
springs, per the `DESKTOP.md` spec (bubble scale follows `LayoutConfig.bubbleScale`).

## Theme switching (the big crossfade)

`AppTheme` (Theme.kt) morphs **every color in place**:

- `withThemeTransition()` wraps the whole Material `ColorScheme`, the `ExtraColorsScheme`,
  `SurfaceColors` and `KaiteyoAccentScheme` in `animateColorAsState`
  (`tween(450)` base × speed, or instant when disabled/reduced-motion).
- The UI tree never leaves composition — only colors animate toward their targets, so
  state is preserved and the switch feels like a dissolve rather than a reload.
- `KaiteyoDesktopSuite` adds a "settle" pop: after the crossfade finishes, the whole
  window dips to 0.985 then springs back (`spring(0.55, 340)`), gated by the same
  config and never played on first launch.

## Onboarding wizard (`OnboardingWizard.kt`)

- Entrance: scale 0.96→1 (`spring(0.7, 220)`) + fade `tween(280)`.
- Step changes: `AnimatedContent` fade + directional slide (240ms, 40px).
- Progress segments pulse (active) via `infiniteRepeatable(tween(900), Reverse)`;
  done segments fill accent.
- Selection cards: scale press 0.97 / hover 1.015 (`spring(0.7, 420)`), bg/border
  `tween(180)`, check badge pops in with `scaleIn(0.4, spring(0.5, 600))`.
- Logo glow pulses (`tween(1400)` loop) behind the brand mark.
- All motion honors `reducedMotion`.

## Performance guidelines

1. Animate `graphicsLayer` (scale/alpha) not layout properties — dialogs, launcher,
   wizard, toast icons all use `graphicsLayer`.
2. `animateXAsState` for simple values; `Animatable` for sequenced/loop animations.
3. `LaunchedEffect` for one-shot sequences; `rememberInfiniteTransition` for loops.
4. Keep animated composables small; avoid re-animating whole trees.
5. Target 60 FPS (120 for dock drags) — if frames drop, simplify or gate by
   `reducedMotion`.
6. `derivedStateOf`/`remember` to avoid re-triggering animation specs per frame.
7. Resolve durations *in the composable body* (transitionSpec lambdas can't read
   CompositionLocals — see `WorkspaceLayout`).

## Reduced motion

`reducedMotion` (from config, settings, or system) is honored in every helper and
every component: durations become 0, springs become `snap()`, loops stop, and the
theme crossfade/settle pop are skipped. `AnimationSpeed.Instant` behaves the same for
durations while keeping immediate state changes.

## Related

- `docs/design/THEME_SYSTEM.md` — the config objects behind motion
- `docs/design/UI_SYSTEM.md` — components that use these animations
- `docs/features/DESKTOP.md` — window/dock motion specifics
- `docs/architecture/performance.md` — budgets and profiling
