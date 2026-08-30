# Kaiteyo — Design System (Master)

> **Status**: `IMPLEMENTED` (two token systems) + `ARCHITECTED` (unified target).
> Companion: `THEME_SYSTEM.md`, `DESIGN_LANGUAGE.md`, `UI_SYSTEM.md`, `ANIMATION_SYSTEM.md`, `KAITEYO_EXPRESSIVE.md`, `core.md`.

## 1. What it is

One token-based design system that makes every surface belong to the same application. No screen invents its own colors, radii, spacing, or animation.

## 2. Tokens (target — unified)

### Color

| Token | Light | Dark | Use |
|-------|-------|------|-----|
| background | #F8F9FA | #121212 | page |
| surface / surfaceVariant / surfaceElevated / surfaceInteractive | stepped | stepped | cards, bars |
| textPrimary / textSecondary / muted / disabled / inverse | — | — | text |
| border / subtle / strong / focused | — | — | dividers, focus ring 2dp accent |
| accent (#4CAF50) + hoverOverlay 4% + selectedOverlay 8% | — | — | selection, focus |
| success / warning / error / info / favorite / due / new / suspended | semantic | semantic | badges, states |
| reviewAgain/Hard/Good/Easy; cardNew/Learning/Young/Mature/Suspended/Buried/Archived; flagRed…Purple; activityReview/Edit/Import | `KaiteyoSemanticColors` (40+ tokens, Light/Dark variants, withThemeTransition animation) | — | SRS, flags, activity |

### Themes

Dark (OLED default) · Dark Gray · Light · Sepia. Accent schemes (7): Pineapple, Cotton Candy, Ocean, Forest, Sunset, Lavender, Monochrome. Theme Studio live-edits and propagates to window chrome.

### Typography

| Style | Token |
|-------|-------|
| display / heading / body / label / caption | `TypeScale` (fontScale, titleScale, lineHeight) |
| Japanese text | Noto Sans JP / compatible, furigana size linked to base |
| Dictionary text | mono-friendly + line-height for glossing |
| Developer/monospace | monospace scale |

LetterSpacing scaled via `runCatching` (Compose MPP TextUnit is Unspecified-safe).

### Spacing

4dp grid (`DsSpacing`), consistent across all layouts (audit gap P0 #4 is the migration to uniform grid).

### Radii / Elevation

`DsRadius` (Sm/Md/Lg/Xl), `DsElevation` (0/1/2/4dp), `RadiusConfig` presets (Square/Rounded/VeryRounded/Soft).

## 3. Components

Buttons: primary / secondary / tertiary / icon / destructive / toggle / segmented / floating / navigation.
Inputs: search / text / dropdown / slider / checkbox / switch / chip / filter / date / media selection.
Cards: `DsCard` + per-entity card registries (KanjiCardType etc.) with presets.
Panels: side panel / floating panel / modal / bottom sheet / popover / inspector — all with enter/exit animation presets, reduced-motion aware.

All components live under `Ds*` (suite) or `KaiteyoSemanticColors` (core) — target is one import path after consolidation.

## 4. Rules

- Modifier order: size → padding → background/clip → clickable → align → graphicsLayer → semantics.
- Hover: `hoverable` + `collectIsHoveredAsState` consistently; reducedMotion disables spring where appropriate.
- No random padding/boxes/radii/typography — tokens only.
- Every screen identifies itself (`ProvidePageIdentity`) for the debug overlay.

## 5. Evolution

New component → add `Ds*` with tokens + story/preview + a11y labels. New theme → add `KaiteyoSemanticColors` variant + accent scheme. No screen-level hex.
