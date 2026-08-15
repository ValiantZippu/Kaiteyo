# Kaiteyo Architecture — Accessibility Plan

**Status**: Partial — foundational support shipped; completeness is open work (P3)
**Owner**: cross-cutting (UI) — `docs/design/` tokens drive contrast/motion
**Related**: `docs/design/DESIGN_LANGUAGE.md` · `docs/design/ANIMATION_SYSTEM.md` ·
`docs/planning/TODO.md` (P3 accessibility) · `docs/architecture/performance.md`

## 1. Principles (§254, §300)

Accessibility is structural, not per-screen patches: motion comes from one token system,
interaction from one navigation system, and every new interactive element gets a focus
path and a semantics label. Empty/loading/error states are accessibility states too
(§297–§299).

## 2. What exists today

### Reduced motion ✅
Honored app-wide because motion is centralized (`docs/design/ANIMATION_SYSTEM.md`):
chrome hover/fade animations, dock animations, heatmap push/slide year transitions,
launchpad transforms, AFK rain, window-chrome animations all check the preference.
Structure, not per-screen luck.

### Keyboard navigation (partial → target complete)
- Command palette (global).
- Navigation chrome: bubble/launchpad arrow-key + Enter + Escape, focus rings.
- Review: `Space`, `1–4`, `B`, `S`, `R`, `Ctrl+Enter`, `Ctrl+Z` (guarded against the
  reschedule dialog).
- Media: mute, F11, J/K/Shift+L subtitle delay, `[`/`]` speed, frame step `,`/`.`,
  chapters PageUp/PageDown, display-mode `I`, aspect `O` (all rebindable).
- Exams: 1–9 select, R reveal, S skip, Enter next.
- `keyboard_shortcut` table (core) + `ShortcutRegistry`/`ShortcutsView` (suite):
  persisted, profile-scoped, conflict-checked.
- Target: full application coverage is open (P3).

### Themes & contrast
Token-based themes (Light/Dark/OLED/Custom + 17 presets, Theme Studio) — semantic tokens
drive every component (§224–§225). High-contrast *mode* is not yet a first-class
setting; contrast is reviewed in design QA (§309) with automated checks planned.

### Window & DPI
DPI-aware geometry (AWT device px + density conversion), work-area-safe chrome
(taskbar any edge, macOS menu bar/dock), multi-monitor, min sizes, OS-delegated maximize,
per-display DPI recovery — the window shell never loses controls behind the taskbar
(§304).

### Focus & semantics
Focus rings on navigation surfaces; `LocalIndication` used consistently (a duplicate
`.clickable` bug that double-fired + disabled ripples was fixed in the audit pass);
modifier order is standardized so semantics/hover/focus apply predictably.

### Text scaling & layout
Layout adapts to window tiers (COMPACT/STANDARD/WIDE/ULTRAWIDE, §303) via the adaptive
width system (`AdaptiveContent.kt`, `ScrollableScreenContainer`, adaptive dialogs) —
content reflows instead of clipping. Explicit font-scale setting is future.

## 3. Target state gap list

| Area | Status |
|---|---|
| Reduced motion | ✅ shipped (structural) |
| Keyboard-only full app coverage | 🟡 partial — nav/review/exam/media done; full surface pass open |
| Screen-reader compatibility (Compose semantics, contentDescription coverage) | 🟡 open |
| High-contrast theme mode | 🟡 open (token-driven; cheap once defined) |
| Text scaling setting | 🟡 open |
| Color accessibility (automated contrast checks in CI) | 🟡 planned |
| Subtitles (size/background controls) | ✅ media subsystem |
| Journey: controller remap, Simple/Advanced tiers | ⬜ with Journey (spec'd) |

## 4. QA matrix (§300, §302)

Test: keyboard-only, touch, mouse, controller, large text, reduced motion, dark/light,
high contrast. Device classes: small phone → large phone → tablet → small laptop →
desktop → ultrawide (responsive tiers are implemented app-wide). Journey-specific input
QA per §251–§254 when it ships.

## 5. Rules for new work

- Design tokens are authoritative — never hand-invent colors/durations per screen
  (§224, §327–§328).
- Every interactive element gets a focus path + semantics label.
- Every screen has intentional empty/loading/error states (§297–§299).
- New animations must honor reduced motion through the central motion system.

## 6. Open items

- Full keyboard navigation + screen-reader pass (P3, `planning/TODO.md`).
- High-contrast theme mode (design-token work).
- Text scaling setting (font-scale factor via tokens).
- Automated contrast checks in CI (targeted, §231).

## 6. World accessibility (TARGET — Journey, STANDARDS §254)

The game world follows the same accessibility contract as the app:

- **Keyboard-only play**: interaction prompt (§139), knowledge overlay (§140), HUD, and
  quest UI are all reachable by keyboard; no action requires a pointer.
- **Reduced motion** (§123): camera sweeps, bubble jiggle, world ambient animation, and
  transitions degrade to fades/static; `transition intensity` setting honored.
- **Text scaling + high contrast** inside the world: signs, dialogue, glossary, and
  subtitles scale and re-theme with app tokens.
- **Subtitle/dialogue controls**: size, background, and captions-on-by-default for all
  spoken lines (dialogue is subtitled, §99).
- **Controller remapping** for world controls (STANDARDS §252); Simple/Advanced tiers
  (§253).
- **Color accessibility**: weather/lighting never encodes information by color alone
  (quest markers also use shape/icon).
- QA: the world adds these checks to the §4 matrix (TEST_PLAN §12).
