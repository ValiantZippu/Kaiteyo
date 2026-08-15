# Design Philosophy

**Status**: LIVE — binding on all UI work (core app and desktop suite). Mirrors
standards §53–§56 and `docs/design/`.

## The feeling Kaiteyo must produce

| Feel | Meaning in practice |
|---|---|
| **connected** | Navigation, dictionary, study, media, world feel like one system. No feature is an island; every screen can reach every related screen. |
| **fast** | Responsive at 60 FPS; no jank, no spinners where data is local, no perceived waits (§190–§191). |
| **calm** | Nothing blinks, shouts, or demands attention without reason. Motion is meaningful, springy, and quick. |
| **premium** | Craft in every detail: consistent tokens, intentional spacing, correct corner radii, no visual artifacts, no "vibecoded" layouts. |
| **responsive** | Layout is a deliberate response to window/device size — never a phone column stretched, never a desktop dashboard squeezed. |
| **playful** | Joy in details (kana, kanji, world interactions) without sacrificing seriousness. |
| **intelligent** | The app knows the learner's knowledge and acts on it — recommendations, review timing, media fit. |
| **purposeful** | Every element earns its place; nothing exists because "screens need something here." |
| **dense without clutter** | Information-dense surfaces with clear hierarchy, not empty white space, not crowded chaos. |
| **powerful without complicated** | Advanced power lives behind progressive disclosure; defaults are simple. |
| **game-like without RPG** | Journey feels like an exploration game, never a grind (§117, game philosophy). |
| **educational without school software** | Never looks or feels like a drill-and-kill edutainment product. |

## The anti-pattern list (never ship)

- Generic dashboard UI (giant cards floating in empty space)
- Empty center space with controls pinned to edges
- Giant unnecessary cards; excessive padding
- Random boxes; inconsistent radius; inconsistent typography
- Random animations; animations with no easing discipline
- Vibecoded layouts (no grid, no tokens, no hierarchy)
- Duplicate navigation (the same action reachable four ways with no reason)
- Dead buttons; placeholder screens; fake functionality
- Any UI that claims a state it does not have (fake completeness, §64)

## Spatial hierarchy (§54)

- **Use the available space.** Desktop windows are the primary surface; a 1920px
  window must not render a 400dp phone column. `AdaptiveContent.kt` (core) tiers:
  Phone / Medium / Wide, and screens grow with the window.
- **Deliberate tiers per form factor**: desktop (sidebar + content + panels),
  tablet (split panes), phone (top/bottom nav), small window (<720dp compact tab
  bar), large window (adaptive grids, 3-up/4-up cards), ultrawide (content
  max-width centered, never stretched to the edges).
- Every screen has a clear primary action and reading order; secondary actions
  never compete visually with the primary.
- Density is a feature: lists, tables, and grids present real information at a
  glance; whitespace is used for *hierarchy*, not emptiness.

## Motion (§55, `docs/design/ANIMATION_SYSTEM.md`)

- **One centralized animation system.** Springs for UI transitions; fades for
  reduced-motion users. No random per-screen constants.
- Animation tokens (duration, easing, spring stiffness) live in the design system;
  settings support: animation enabled, reduced motion, animation speed, transition
  style, UI animation intensity, game animation intensity.
- Motion communicates: what changed, where things come from/go to, state. It never
  decorates for its own sake. Hover/theme/window-move must hit 60 FPS (P0 track —
  `docs/planning/CURRENT_ISSUES.md` #1–#3).

## Tokens over ad-hoc values

- All colors, type, spacing (4dp grid), radii, elevation, icons come from the theme
  system (`docs/design/THEME_SYSTEM.md`, ADR-0002). Never hardcode random colors
  throughout screens.
- One coherent icon system: consistent stroke/fill logic, scale/alignment
  discipline, theme support, accessibility labels (§228).
- The desktop suite's `Ds*` components and the core design system must not drift
  into two visual languages — the suite is being absorbed into the core product
  (media already is; see `docs/planning/PRODUCT_AUDIT.md`).

## Accessibility is design

- Full keyboard navigation, focus rings, high contrast, reduced motion, text
  scaling, color-blind-safe palettes (standards §254; `docs/architecture/accessibility.md`).
- Not a checklist appended at the end — part of every screen's definition of done.

## Window experience (§56)

- Custom title bar, scoped drag region, 8-zone resize, system menu, state
  persistence, rounded corners, DPI scaling, fullscreen, taskbar interaction —
  shipped in v2.2.1 (see `docs/planning/COMPLETED.md`).
- Resizing must never jank: work-area clamping, breakpoint hysteresis, crossfades
  between layout tiers, chrome that snaps instead of springs during a resize drag.
- Theme integration: window chrome reads the live theme (fixed in the window-system
  rebuild).

## Relationship to other docs

- Design tokens/spec: `docs/design/DESIGN_SYSTEM.md`, `docs/design/UI_SYSTEM.md`,
  `docs/design/DESIGN_LANGUAGE.md`, `docs/design/THEME_SYSTEM.md`
- Standards source: §53 (design principles), §54 (UX philosophy), §55 (animation),
  §56 (window system), §224–§228 (design/theme/asset/icon)
- Coding rules: `docs/development/CODING_STANDARDS.md` (modifier order, 4-space,
  120-char, etc.)
