# Kaiteyo Expressive — Design System

> A quiet futuristic studio for mastering Japanese.
> Inspired by Material 3 Expressive's emotional color systems and dynamic theming,
> but purpose-built for a language learning context where **consistency beats novelty**
> and **content dominates chrome**.

---

## 1. Design Philosophy

### 1.1 Core Principles

| Principle | Meaning | Implementation |
|-----------|---------|----------------|
| **Content-first** | UI chrome recedes; kanji, vocabulary, and sentences dominate | Slim top bars, generous whitespace, 85% dark surfaces |
| **Floating elevation** | Panels feel elevated, not attached to edges | DsDockIsland (8dp ring), DsCard hover lift, glow borders |
| **Emotional color** | Colors evoke mood — lime = progress, orange = urgency, blue = calm | KaiteyoSemanticColors with 40+ theme-aware tokens |
| **Consistent rhythm** | 4dp base grid, 8dp vertical rhythm, 16dp edge padding | Dimens.kt tokens, DsSpacing system |
| **Accessible motion** | Spring physics at 60 FPS; honors reduced-motion and speed settings | AnimationConfig, DsMotion tokens |

### 1.2 The Kaiteyo Palette Philosophy

Unlike Material 3's static palette generation, Kaiteyo uses a **two-layer system**:

1. **Base modes** define the surface atmosphere (OLED black → warm sepia → clean paper)
2. **Accent schemes** define the emotional identity (lime → orange → blue)

The **semantic layer** bridges both: `KaiteyoSemanticColors` provides 40+ tokens that
adapt to dark/light base modes while maintaining the same *meaning* across every screen.

---

## 2. Color Architecture

### 2.1 Three-Layer Color Model

```
┌─────────────────────────────────────────────────┐
│  LAYER 3: Semantic Tokens (KaiteyoSemanticColors)│
│  "What does this color MEAN?"                    │
│  reviewAgain, cardNew, success, error, favorite  │
├─────────────────────────────────────────────────┤
│  LAYER 2: Accent Schemes (KaiteyoAccentScheme)   │
│  "What is the emotional identity?"               │
│  primary, secondary, tertiary, gradients         │
├─────────────────────────────────────────────────┤
│  LAYER 1: Base Modes (SurfaceColors)             │
│  "What is the atmosphere?"                       │
│  background, surface, text, border               │
└─────────────────────────────────────────────────┘
```

### 2.2 Base Modes — The Atmosphere

Each base mode defines a complete `SurfaceColors` set:

| Mode | Background | Surface | Elevated | Interactive | Border | Text Primary | Use Case |
|------|-----------|---------|----------|-------------|--------|-------------|----------|
| **OLED Black** | #050505 | #0D0D0D | #101010 | #1A1A1A | #2A2A2A | #F0F0F0 | Default dark |
| **Dark Gray** | #121212 | #1A1A1A | #242424 | #2E2E2E | #2A2A2A | #F0F0F0 | Softer dark |
| **Light** | #F5F5F5 | #EEEEEE | #E8E8E8 | #FCFCFC | #D0D0D0 | #1A1A1A | Clean light |
| **Sepia** | #F5F0E8 | #EDE5D8 | #E5DCC8 | #F8F4EE | #D4C8B8 | #3D3028 | Reading mode |
| **Cream** | #F7F3E8 | #EDE6D4 | #E5DCC0 | #FAF7F0 | #DED1BC | #3A2F22 | Warm paper |
| **Paper** | #FCFAF5 | #F5F2E8 | #EDE9DE | #FFFFFF | #E6E0D4 | #2A2A2A | Clean off-white |
| **Midnight** | #0A0D1A | #121622 | #1A1F30 | #232940 | #2A324A | #EAEAFF | Deep blue dark |

### 2.3 Accent Schemes — The Identity

Each accent scheme has 10 color slots:

| Slot | Role | Signature Pineapple | Cotton Candy | Ocean | Forest | Sunset | Lavender | Monochrome |
|------|------|-------------------|-------------|-------|--------|--------|----------|------------|
| **primary** | Main accent | #C2FC8B (lime) | #D4A5F0 | #00D4AA | #81C784 | #FF6B6B | #B39DDB | #E0E0E0 |
| **primaryDark** | Light-mode primary | #9CE85E | #C084E8 | #00B894 | #66BB6A | #E05555 | #9575CD | #BDBDBD |
| **secondary** | Supporting accent | #FEAB57 (orange) | #FFB5C5 | #00A8FF | #A5D6A7 | #FFB347 | #CE93D8 | #9E9E9E |
| **secondaryDark** | Light-mode secondary | #FD8A2E | #FF8FA5 | #0088CC | #81C784 | #E09D3A | #BA68C8 | #757575 |
| **onPrimary** | Text on primary | #050505 | #1A1A2E | #050505 | #1A2E1A | #1A0A0A | #1A1A2E | #121212 |
| **onSecondary** | Text on secondary | #050505 | #1A1A2E | #050505 | #1A2E1A | #1A0A0A | #1A1A2E | #121212 |
| **tertiary** | Third accent | #7BC8FF (blue) | #A0D2FF | #0D47A1 | #5D4037 | #FF8C69 | #80CBC4 | #616161 |
| **gradientStart** | Gradient start | #C2FC8B | #D4A5F0 | #0D47A1 | #5D4037 | #FF6B6B | #B39DDB | #E0E0E0 |
| **gradientEnd** | Gradient end | #FEAB57 | #FFB5C5 | #00D4AA | #81C784 | #FFB347 | #CE93D8 | #9E9E9E |

### 2.4 Semantic Tokens — The Meaning Layer

`KaiteyoSemanticColors` — 40+ tokens that map *meaning* to color, adapting to dark/light:

#### Review Actions
| Token | Dark Value | Light Value | Meaning |
|-------|-----------|-------------|---------|
| `reviewAgain` | #FF6B6B | #E53935 | Wrong answer, needs re-learning |
| `reviewHard` | #FEAB57 | #EF6C00 | Difficult answer |
| `reviewGood` | #C2FC8B | #2E7D32 | Correct answer |
| `reviewEasy` | #7BC8FF | #1565C0 | Effortless answer |

#### Card Status
| Token | Dark Value | Light Value | Meaning |
|-------|-----------|-------------|---------|
| `cardNew` | #7BC8FF | #1565C0 | Never reviewed |
| `cardLearning` | #FEAB57 | #EF6C00 | In initial learning |
| `cardYoung` | #C2FC8B | #2E7D32 | Recently learned, low stability |
| `cardMature` | #4CAF50 | #1B5E20 | Well-established knowledge |
| `cardRelearning` | #FF6B6B | #E53935 | Forgotten, re-learning |
| `cardSuspended` | #B0B0B0 | #757575 | Temporarily disabled |
| `cardBuried` | #9B59B6 | #7B1FA2 | Hidden until next day |
| `cardArchived` | #7F8C8D | #616161 | Permanently stored |

#### Semantic Indicators
| Token | Dark Value | Light Value | Meaning |
|-------|-----------|-------------|---------|
| `success` | #C2FC8B | #2E7D32 | Positive outcome, completion |
| `warning` | #FEAB57 | #EF6C00 | Caution, attention needed |
| `error` | #FF6B6B | #E53935 | Error, destructive action |
| `info` | #7BC8FF | #1565C0 | Informational, neutral |
| `favorite` | #FFD93D | #F9A825 | Bookmarked, starred |
| `due` | #FF9F43 | #EF6C00 | Needs review soon |
| `new` | #A78BFA | #7B1FA2 | Newly added, never seen |
| `suspended` | #B0B0B0 | #757575 | Temporarily hidden |
| `muted` | #606060 | #A0A0A0 | De-emphasized |

#### Card Flags
| Token | Dark | Light | Use |
|-------|------|-------|-----|
| `flagRed` | #FF6B6B | #E53935 | Priority/difficult |
| `flagOrange` | #FEAB57 | #EF6C00 | Important |
| `flagYellow` | #FFD93D | #F9A825 | Review soon |
| `flagGreen` | #C2FC8B | #2E7D32 | Well done |
| `flagBlue` | #7BC8FF | #1565C0 | Reference |
| `flagPurple` | #A78BFA | #7B1FA2 | Special |

#### Activity Types
| Token | Color | Use |
|-------|-------|-----|
| `activityReview` | #4CAF50 | Successful review |
| `activityReviewFailed` | #F44336 | Failed review |
| `activityEdit` | #2196F3 | Card edited |
| `activityImport` | #9C27B0 | Content imported |
| `activityExport` | #009688 | Content exported |
| `activityTag` | #FF9800 | Tag changed |
| `activityFlag` | #FF5722 | Flag changed |
| `activityNote` | #3F51B5 | Note added/edited |
| `activityStudy` | #00BCD4 | Study session |
| `activitySystem` | #9E9E9E | System event |

#### Difficulty Tiers
| Token | Color | Meaning |
|-------|-------|---------|
| `difficultyEasy` | #C2FC8B | Easy item |
| `difficultyMedium` | #FEAB57 | Medium difficulty |
| `difficultyHard` | #FF6B6B | Hard item |

---

## 3. The Heatmap Design Language

The heatmap is Kaiteyo's **signature visual element** — it established the design
language that now permeates the entire app.

### 3.1 Heatmap Card Pattern

```
┌─────────────────────────────────────┐
│ ▎ Activity                    Less ■ │  ← Header: 12sp SemiBold
│ ▎                          ■■■■ More│  ← Legend: 8sp Muted
│ ▎ ┌─┬─┬─┬─┬─┬─┬─┐                  │
│ ▎ │ │ │█│█│ │█│ │ M  ← Grid: 10-12dp cells
│ ▎ │ │█│█│█│ │ │ │ T     with 2dp gaps
│ ▎ │█│█│█│█│█│█│ │ W     and 2dp radius
│ ▎ │ │ │ │ │ │ │ │ T                 │
│ ▎ └─┴─┴─┴─┴─┴─┴─┘                  │
│ ▎ 42 reviews across 12 days in 2026 │  ← Summary: 9sp Muted
│ ▎                    View full stats →│  ← CTA: 9sp Medium, accent
└─────────────────────────────────────┘
```

### 3.2 Card Container

Every "heatmap-style" card follows this pattern:

```kotlin
Column(
    modifier = Modifier
        .clip(RoundedCornerShape(16.dp))           // Consistent radius
        .background(surfaceColors.surface)           // Theme-aware surface
        .border(1.dp,                                // Subtle border
            surfaceColors.surfaceInteractive.copy(alpha = 0.5f),
            RoundedCornerShape(16.dp))
        .padding(horizontal = 14.dp, vertical = 12.dp)
)
```

### 3.3 Color Intensity Ramps

The heatmap uses alpha-based intensity, not hue shifts:

| Level | Alpha | Meaning |
|-------|-------|---------|
| Empty | 0.06 | No activity |
| Low | 0.12 | 1–25% of max |
| Medium | 0.30 | 26–50% of max |
| High | 0.55 | 51–75% of max |
| Peak | 1.00 | 76–100% of max |

The ramp uses `accent.primary` as the base color, so it adapts to any accent scheme.

### 3.4 Time Progress Bars

Multi-scale temporal visualization using gradient fills:

```
YEAR  ████████████████░░░░░░░░░░  67% · 123 days left
MONTH ██████████████░░░░░░░░░░░░  58% · 13 days left
WEEK  ████████████░░░░░░░░░░░░░░  43% · 4 days left
DAY   ████████████████████░░░░░░  75% · 6 hours left
HOUR  ████████████████████████░░  88% · 7 min left
```

Each bar uses:
- **Track**: `surfaceInteractive.copy(alpha = 0.3f)`
- **Fill**: `accent.primary.copy(alpha = 0.4f → 0.7f)` with horizontal gradient
- **Marker**: 2dp accent line + radial glow at current position
- **Dots**: `textMuted.copy(alpha = 0.15f)` in remaining area

---

## 4. Component Theming Guide

### 4.1 The Standard Card

```kotlin
@Composable
fun StandardCard(
    modifier: Modifier = Modifier,
    header: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val sem = LocalKaiteyoSemanticColors.current

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(surfaceColors.surface)
            .border(1.dp, surfaceColors.surfaceInteractive.copy(alpha = 0.5f),
                RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        if (header != null) {
            Text(header, style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold, color = surfaceColors.textPrimary)
            Spacer(Modifier.height(8.dp))
        }
        content()
    }
}
```

### 4.2 The Status Badge

```kotlin
@Composable
fun StatusBadge(status: CardStatus) {
    val sem = LocalKaiteyoSemanticColors.current
    val bgColor = when (status) {
        CardStatus.New -> sem.cardNew
        CardStatus.Learning -> sem.cardLearning
        CardStatus.Young -> sem.cardYoung
        CardStatus.Mature -> sem.cardMature
        CardStatus.Relearning -> sem.cardRelearning
        CardStatus.Suspended -> sem.cardSuspended
        CardStatus.Buried -> sem.cardBuried
        CardStatus.Archived -> sem.cardArchived
    }
    Box(
        modifier = Modifier.clip(RoundedCornerShape(4.dp))
            .background(bgColor.copy(alpha = 0.2f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(status.name, color = bgColor, fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
    }
}
```

### 4.3 The Progress Indicator

```kotlin
@Composable
fun ThemedProgressIndicator(fraction: Float, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = { fraction },
        modifier = modifier.height(6.dp).clip(RoundedCornerShape(3.dp)),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        strokeCap = StrokeCap.Round
    )
}
```

### 4.4 The Section Header

```kotlin
@Composable
fun SectionHeader(text: String, icon: ImageVector? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Box(
                modifier = Modifier.size(26.dp).clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    }
}
```

---

## 5. UX Patterns

### 5.1 Information Hierarchy

Every screen follows this text hierarchy:

| Level | Style | Color | Usage |
|-------|-------|-------|-------|
| **Hero** | displaySmall / headlineLarge | textPrimary | Page title, big numbers |
| **Section** | titleSmall SemiBold | textPrimary | Section headers |
| **Body** | bodyMedium | textPrimary | Primary content |
| **Secondary** | bodySmall | textSecondary | Supporting info |
| **Muted** | labelSmall | textMuted | Timestamps, metadata, legends |
| **Accent** | labelSmall Medium | accent.primary | CTAs, links, active states |

### 5.2 Interaction States

| State | Visual Change | Duration |
|-------|--------------|----------|
| **Hover** | bg → surfaceInteractive, scale 1.02 (buttons) | 160ms tween |
| **Press** | scale 0.97 | spring(0.6, 500) |
| **Focus** | 2dp accent border | instant |
| **Disabled** | muted text, no interactions | — |
| **Loading** | skeleton pulse 0.45↔1.0 | 900ms loop |

### 5.3 Empty States

Every empty state follows the same pattern:
1. Accent icon at 60% opacity, 48dp
2. Title: bodyLarge, Medium weight
3. Message: bodySmall, textMuted
4. Optional action: accent text button

### 5.4 Color Usage Ratios

Following the heatmap philosophy:

| Surface | Ratio | Description |
|---------|-------|-------------|
| Dark surfaces | ~85% | Background, cards, panels |
| Text | ~10% | Primary, secondary, muted |
| Accent | ~5% | Buttons, links, active states, badges |

This ensures the UI feels calm and focused, with accent colors reserved for
interactive elements and important indicators.

---

## 6. Accessibility

### 6.1 Contrast Ratios

All text meets WCAG AA (4.5:1 for body, 3:1 for large text):
- Text primary (#F0F0F0) on OLED surface (#0D0D0D): **15.8:1** ✓
- Text secondary (#A0A0A0) on OLED surface: **7.2:1** ✓
- Text muted (#606060) on OLED surface: **3.1:1** (large text only) ✓
- Accent primary (#C2FC8B) on OLED surface: **12.4:1** ✓

### 6.2 Color-Blind Safety

Semantic tokens use both hue AND context (icons, labels, position) so meaning
is never conveyed by color alone:
- Review grades: text labels ("Again", "Hard", "Good", "Easy") + color
- Card status: text labels + color badges
- Activity types: icons + text + color dots

### 6.3 Reduced Motion

`AnimationConfig.reducedMotion` zeros all animation durations. The heatmap grid,
time progress bars, and all transitions render instantly.

---

## 7. Animation System

### 7.1 Spring Physics

Default spring: `spring(dampingRatio = 0.6f, stiffness = 300f)`

| Element | Damping | Stiffness | Use |
|---------|---------|-----------|-----|
| Button press | 0.6 | 500 | Quick snap |
| Card hover | 0.6 | 300 | Gentle lift |
| Dialog entrance | 0.6 | 400 | Smooth appear |
| Theme transition | — | — | 450ms × speed tween |
| Page transition | — | — | FadeThrough 350ms |

### 7.2 Theme Transitions

When `themeTransitionEnabled`, every color in the scheme wraps in
`animateColorAsState` (450ms base × speed multiplier). The UI tree never
leaves composition — only colors animate toward new targets.

Animated properties:
- All 28 Material ColorScheme fields
- All 5 ExtraColorsScheme fields
- All 16 SurfaceColors fields
- All 40+ KaiteyoSemanticColors fields
- All 10 KaiteyoAccentScheme fields

---

## 8. Responsive Layout

### 8.1 Width Tiers

| Tier | Width | Layout |
|------|-------|--------|
| Phone | < 520dp | Single column, 400dp max |
| Medium | 520–760dp | Two-column where appropriate |
| Desktop | 760–1440dp | Sidebar + content, adaptive grids |
| Wide | > 1440dp | Three-column possible, full-width cards |

### 8.2 Adaptive Content Width

```kotlin
rememberAdaptiveContentMaxWidth(
    phoneMax = 520.dp,
    mediumMax = 640.dp,
    wideMax = 1100.dp
)
```

### 8.3 Dialog Sizing

| Context | Width | Max |
|---------|-------|-----|
| Compact (confirm/prompt) | 50% window | 560dp |
| Rich (settings/batch) | 60% window | 860dp |
| Phone | 90% screen | — |

---

## 9. Extending the Palette

### 9.1 Adding a New Semantic Token

1. Add the field to `KaiteyoSemanticColors` in `Color.kt`
2. Set the dark value in `KaiteyoSemanticColorsDark`
3. Set the light value in `KaiteyoSemanticColorsLight`
4. Add the animation in `Theme.kt` `withThemeTransition`
5. Use `LocalKaiteyoSemanticColors.current.yourToken` in components

### 9.2 Adding a New Accent Scheme

1. Define color constants in `Color.kt`
2. Create a `KaiteyoAccentScheme` entry in `AllAccentSchemes`
3. Set `gradientStart`/`gradientEnd` for the gradient system
4. The heatmap, time progress, and all accent-derived elements adapt automatically

### 9.3 Adding a New Base Mode

1. Define surface color constants in `Color.kt`
2. Add the enum entry to `BaseMode`
3. Implement `surfaceForBaseMode()` case
4. Set `isDarkMode` for the mode
5. The semantic tokens, Material scheme, and all UI adapt automatically

---

## 10. Related Documents

- `docs/design/DESIGN_LANGUAGE.md` — tokens and values (the "what")
- `docs/design/THEME_SYSTEM.md` — theme engine architecture (the "how")
- `docs/design/UI_SYSTEM.md` — component catalog (the "where")
- `docs/design/ANIMATION_SYSTEM.md` — motion tokens and patterns
- `docs/design/DESIGN_SYSTEM.md` — desktop Ds* component system
- `docs/architecture/OVERVIEW.md` — module map
