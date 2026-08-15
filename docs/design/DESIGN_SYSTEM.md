# Kaiteyo 書いてよ — Complete Visual Identity Design System

> Version 2.0.0 — Premium Experience
>
> *"A quiet futuristic studio for mastering Japanese"*

---

## Table of Contents

1. [Brand Philosophy](#1-brand-philosophy)
2. [Color System](#2-color-system)
3. [Typography](#3-typography)
4. [Layout & Spacing](#4-layout--spacing)
5. [Logo & Iconography](#5-logo--iconography)
6. [Desktop Window Design](#6-desktop-window-design)
7. [Sidebar Design](#7-sidebar-design)
8. [Main Content Panel Design](#8-main-content-panel-design)
9. [Dashboard Concept](#9-dashboard-concept)
10. [Study Screen Concept](#10-study-screen-concept)
11. [GitHub Banner Concept](#11-github-banner-concept)
12. [UI Component Style Guide](#12-ui-component-style-guide)
13. [Animation System](#13-animation-system)
14. [Implementation Roadmap](#14-implementation-roadmap)

---

## 1. Brand Philosophy

### Core Identity

Kaiteyo is not "Kanji Dojo renamed." It is a complete reimagining — a premium Japanese language mastery tool that feels like a carefully crafted creative application.

### Brand Values

| Value | Expression |
|-------|-----------|
| **Learning** | Clean, focused interfaces that reduce cognitive load |
| **Writing** | Tactile, expressive interactions for character practice |
| **Mastery** | Progress systems that feel rewarding, not gamified |
| **Discipline** | Structured layouts that encourage daily practice |
| **Creativity** | Modern aesthetics that inspire continued engagement |
| **Calm** | Dark, quiet interfaces that don't overwhelm |

### Design Language Keywords

- OLED dark minimalism
- Controlled accent colors
- Floating card architecture
- Smooth, meaningful animations
- Professional tool aesthetic
- Japanese-inspired, not Japanese-themed

### What We Are NOT

- ❌ Generic AI app visuals
- ❌ Flashy gaming RGB interface
- ❌ Traditional Japanese clichés (torii gates, samurai, anime)
- ❌ Corporate education software
- ❌ Over-designed or busy layouts

---

## 2. Color System

### Primary Background

```
OLED Black:       #050505  — Main application background
Surface Dark:     #0D0D0D  — Panel backgrounds
Surface Medium:   #101010  — Elevated surfaces, cards
Surface Light:    #1A1A1A  — Interactive elements, inputs
```

### Accent Colors

```
Primary Accent:   #C2FC8B  — Soft neon lime/mint green
Secondary Accent: #FEAB57  — Warm pastel orange
```

### Text Colors

```
Text Primary:     #F0F0F0  — Near white for headings
Text Secondary:   #A0A0A0  — Soft gray for body text
Text Muted:       #606060  — Dimmed text, labels
Text Inverse:     #050505  — Text on accent backgrounds
```

### Semantic Colors

```
Success:   #C2FC8B  — Reviews completed, mastery achieved
Warning:   #FEAB57  — Due reviews, pending items
Error:     #FF6B6B  — Incorrect answers, errors
Info:      #7BC8FF  — Information, hints
New:       #A78BFA  — New characters to learn
```

### Extended Surface Palette

```
Surface Hover:    #1A1A1A  — Hover state for cards
Surface Active:   #222222  — Active/selected state
Surface Border:   #2A2A2A  — Subtle borders
Overlay:          rgba(0, 0, 0, 0.6) — Modal overlays
Glow Primary:     rgba(194, 252, 139, 0.15) — Primary accent glow
Glow Secondary:   rgba(254, 171, 87, 0.12)  — Secondary accent glow
```

### Color Usage Guidelines

| Element | Color | Rule |
|---------|-------|------|
| Background | `#050505` | Always pure OLED black |
| Sidebar panel | `#0D0D0D` | Rounded floating panel |
| Main content | `#101010` | Slightly lighter for depth |
| Active nav item | `#C2FC8B` | Text + subtle glow |
| Progress bars | `#C2FC8B` | Gradient to darker green |
| Important buttons | `#FEAB57` | Primary actions |
| Headings | `#F0F0F0` | High contrast readability |
| Body text | `#A0A0A0` | Comfortable reading |
| Dividers | `#2A2A2A` | Subtle separation |

### Gradient Definitions

```css
/* Primary accent gradient */
background: linear-gradient(135deg, #C2FC8B 0%, #9CE85E 100%);

/* Secondary accent gradient */
background: linear-gradient(135deg, #FEAB57 0%, #FD8A2E 100%);

/* Progress gradient */
background: linear-gradient(90deg, #C2FC8B 0%, #FEAB57 100%);

/* Surface elevation gradient (subtle) */
background: linear-gradient(180deg, #0D0D0D 0%, #0A0A0A 100%);
```

> **Critical Principle:** The app should remain ~85% black/dark surfaces with ~10% text and ~5% accent colors. Use accents sparingly for maximum impact.

---

## 3. Typography

### Font Stack

```
Primary:    Inter — Clean, modern, highly readable (UI text, headings)
Writing:    Noto Sans JP — Japanese character display
Monospace:  JetBrains Mono — Data, statistics, code-adjacent content
```

### Font Sizes

| Token | Size | Weight | Usage |
|-------|------|--------|-------|
| `text-xs` | 11px | 400 | Captions, metadata |
| `text-sm` | 13px | 400 | Body text, descriptions |
| `text-base` | 15px | 400 | Standard UI text |
| `text-lg` | 17px | 500 | Card titles |
| `text-xl` | 20px | 600 | Section headings |
| `text-2xl` | 24px | 600 | Panel titles |
| `text-3xl` | 32px | 700 | Page headings |
| `text-4xl` | 40px | 700 | Display text |
| `text-hero` | 64px | 700 | Hero/banner text |

### Line Height

- Body text: 1.5
- Headings: 1.2
- Japanese text: 1.6

### Typography Rules

- Keep line lengths between 60-75 characters for readability
- Use letter-spacing: 0.02em for Japanese text
- Headings use tighter tracking (-0.02em)
- Use uppercase sparingly — only for labels with letter-spacing: 0.1em

---

## 4. Layout & Spacing

### Corner Radius System

```
Radius-xs:   4px   — Checkboxes, small indicators
Radius-sm:   8px   — Buttons, inputs, small cards
Radius-md:   12px  — Standard cards, list items
Radius-lg:   16px  — Large cards, modals, context panels
Radius-xl:   24px  — Sidebar panel, main content panel
Radius-2xl:  32px  — Large containers, dialogs
Radius-full: 50%   — Avatars, circular elements
```

### Spacing Scale

```
Space-1:  4px
Space-2:  8px
Space-3:  12px
Space-4:  16px
Space-5:  20px
Space-6:  24px
Space-8:  32px
Space-10: 40px
Space-12: 48px
Space-16: 64px
Space-20: 80px
```

### Panel Sizing

```
Sidebar width:     260px  (fixed)
Sidebar radius:    24px   (top-left + bottom-left or all corners)
Content radius:    24px
Card min-width:    240px
Card max-width:    400px
```

### Desktop Window Default

```
Initial size:    1200 x 800px
Minimum size:    900 x 600px
Content padding: 24px
```

### Layout Architecture

```
┌──────────────────────────────────────────────────────────┐
│                     OLED Background (#050505)             │
│                                                          │
│  ┌──────────┐  ┌──────────────────────────────────────┐  │
│  │          │  │                                        │  │
│  │  SIDEBAR │  │           MAIN CONTENT                 │  │
│  │          │  │                                        │  │
│  │ #0D0D0D  │  │           #101010                      │  │
│  │ radius   │  │           radius 24px                   │  │
│  │ 24px     │  │                                        │  │
│  │          │  │                                        │  │
│  │ 260px    │  │                                        │  │
│  │          │  │                                        │  │
│  │          │  │                                        │  │
│  │          │  │                                        │  │
│  └──────────┘  └──────────────────────────────────────┘  │
│                                                          │
│               Gap between panels: 24px                   │
│             Outer padding from window: 24px              │
└──────────────────────────────────────────────────────────┘
```

---

## 5. Logo & Iconography

### Primary Logo: "The Mark"

An abstract writing stroke / calligraphic movement. Three weighted brush-like strokes that suggest:

- A character being written
- A checkmark of completion
- Progress and forward movement

**SVG Concept:**

```svg
<!-- Simplified representation of the mark -->
<svg viewBox="0 0 512 512">
  <!-- Main stroke — downward sweeping motion -->
  <path d="M120,80 Q280,60 380,180 Q420,240 400,320"
        stroke="#C2FC8B" stroke-width="28" fill="none"
        stroke-linecap="round" />

  <!-- Secondary stroke — crossing/defining motion -->
  <path d="M160,240 Q280,200 380,260"
        stroke="#C2FC8B" stroke-width="16" fill="none"
        stroke-linecap="round" opacity="0.8"/>

  <!-- Accent dot — completion mark -->
  <circle cx="380" cy="320" r="16" fill="#FEAB57"/>
</svg>
```

### Logo Usage Rules

- The mark should always be `#C2FC8B` on dark backgrounds
- On light backgrounds (rare), invert to `#050505`
- Never stretch, skew, or rotate the logo
- Minimum clear space: 16px on all sides
- Minimum size: 32px (for app icon)

### App Icon (Desktop)

- OLED `#050505` background
- The Kaiteyo mark centered at 60% of icon size
- Subtle `#C2FC8B` glow effect (15% opacity, 8px blur)
- Square with rounded corners (radius: 22% of size)
- Export sizes: 32x32, 64x64, 128x128, 256x256, 512x512

### Favicon (Web/Browser)

- 32x32 simplified version
- Just the accent dot + one stroke
- SVG format preferred

### Wordmark

```
KAITEYO
```

Typography: Inter, Bold, letter-spacing: 0.15em

The wordmark should appear below or beside the mark in:
- Sidebar header
- About screen
- GitHub banner

---

## 6. Desktop Window Design

### Window Frame

Completely custom borderless window implementation:

```
┌─────────────────────────────────────────────────────┐
│  [icon] KAITEYO                         ─  □  ×   │  ← Custom title bar (32px)
│                                                     │
│  ┌──────────┐  ┌─────────────────────────────────┐  │
│  │          │  │                                 │  │
│  │ Content  │  │          Content                │  │
│  │          │  │                                 │  │
│  └──────────┘  └─────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
```

### Title Bar (Custom)

- Height: 32px
- Background: `#0D0D0D` (match sidebar)
- Draggable: Yes (entire bar)
- Double-click to maximize: Yes

### Window Controls

Position: Top-right corner, 8px from edges

```
Normal state:
  ─  □  ×    (font-size: 14px, color: #606060, transparent bg)

Hover state:
  ─  □  ×    (color: #C2FC8B for close, #A0A0A0 for others)
              (background: rgba(194, 252, 139, 0.08) on hover)
              (transition: all 150ms ease)

Close button special:
  Hover: background: rgba(255, 107, 107, 0.15), color: #FF6B6B
```

### Window Behavior

- Startup: Centered on screen, 1200x800
- Remember: Last window position and size
- Snap: Support Windows snap layouts
- Fullscreen: Smooth transition, hide title bar
- Shadows: Custom drop shadow (0 8px 32px rgba(0,0,0,0.4))

### Import/Implementation Notes

For Compose Desktop, implement with:

```kotlin
// Custom window with undecorated frame
Window(
    onCloseRequest = { exitApplication() },
    state = windowState,
    undecorated = true, // Remove default title bar
    resizable = true
) {
    // Custom title bar composable at the top
    // Main content with proper padding below
}
```

---

## 7. Sidebar Design

### Container

```
Background:         #0D0D0D
Border radius:      24px (all corners or left corners)
Width:              260px (fixed)
Padding:            16px
Border:             0.5px solid #2A2A2A (optional subtle edge)
Elevation shadow:   0 4px 24px rgba(0,0,0,0.3)
```

### Sidebar Content Layout

```
┌──────────────────┐
│  ┌────────────┐  │
│  │  [logo]    │  │  ← Logo area (64px)
│  │  KAITEYO   │  │
│  └────────────┘  │
│                  │
│  ─────────────   │  ← Divider (#2A2A2A, 1px, 24px margin)
│                  │
│  ◆ Dashboard     │  ← Nav item (active: #C2FC8B)
│  ◇ Kanji         │  ← Nav item (default: #A0A0A0)
│  ◇ Vocabulary    │
│  ◇ Practice      │
│  ◇ Dictionary    │
│  ◇ Decks         │
│                  │
│  ─────────────   │  ← Divider
│                  │
│  ◇ Statistics    │  ← Secondary nav
│  ◇ Settings      │
│                  │
│  ─────────────   │  ← Divider
│                  │
│  [Progress Bar]  │  ← Study progress (compact)
│  45/120 today    │
│                  │
│  ┌────────────┐  │
│  │ Review: 12│  │  ← CTA button (#FEAB57 accent)
│  └────────────┘  │
└──────────────────┘
```

### Navigation Item Styles

```
Default:
  Text:     #A0A0A0, 15px, Inter Regular
  Icon:     #606060, 20px
  Padding:  10px 16px
  Radius:   10px

Hover:
  Background: rgba(255,255,255,0.04)
  Text:     #D0D0D0

Active:
  Background: rgba(194,252,139,0.08)
  Text:     #C2FC8B
  Icon:     #C2FC8B
  Right border: 2px solid #C2FC8B (animated in)
  Glow:     text-shadow: 0 0 12px rgba(194,252,139,0.3)

Transition:
  All properties: 200ms cubic-bezier(0.4, 0, 0.2, 1)
```

### Progress Widget (Bottom of sidebar)

```
Compact progress bar:
  Height:         4px
  Track color:    #2A2A2A
  Fill color:     #C2FC8B
  Border radius:  2px

  Label:
    "45 / 120 today" — #A0A0A0, 12px, Inter

Review CTA Button:
  Background:     #C2FC8B
  Text:           #050505
  Text:           "Review Due: 12"
  Radius:         12px
  Padding:        12px 16px
  Hover:          brightness(1.1)
```

---

## 8. Main Content Panel Design

### Container

```
Background:         #101010
Border radius:      24px
Margin from:        sidebar 24px
Padding:            32px
Elevation shadow:   0 4px 24px rgba(0,0,0,0.2)
Min height:         100% of available space
```

### Panel Header

```
┌─────────────────────────────────────────────┐
│  Dashboard                         ⚙️ 🎯   │
│  Your study overview                        │
└─────────────────────────────────────────────┘

Title:    #F0F0F0, 24px, Inter Bold
Subtitle: #A0A0A0, 14px, Inter Regular
Actions:  Icon buttons, transparent bg, #606060
          Hover: #C2FC8B or #A0A0A0
```

### Content Architecture

Content inside the panel should use a card-based layout:

```
┌─────────────────────────────────────────────┐
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  │
│  │  Card 1  │  │  Card 2  │  │  Card 3  │  │
│  │          │  │          │  │          │  │
│  └──────────┘  └──────────┘  └──────────┘  │
│                                             │
│  ┌─────────────────────────────────────┐    │
│  │  Wide Card                          │    │
│  │                                     │    │
│  └─────────────────────────────────────┘    │
│                                             │
│  ┌──────────┐  ┌──────────┐                 │
│  │  Card 4  │  │  Card 5  │                 │
│  └──────────┘  └──────────┘                 │
└─────────────────────────────────────────────┘
```

### Card Styles

```
Background:     #0D0D0D (match sidebar)
Radius:         16px
Padding:        20px
Border:         0.5px solid #2A2A2A (optional)
Hover:          transform: translateY(-2px)
                box-shadow: 0 8px 32px rgba(0,0,0,0.3)
Transition:     250ms cubic-bezier(0.4, 0, 0.2, 1)

Card Title:      #F0F0F0, 17px, Inter Medium
Card Value:      #C2FC8B, 32px, Inter Bold
Card Label:      #606060, 12px, Inter
Card Divider:    #2A2A2A, 1px
```

---

## 9. Dashboard Concept

### Layout Vision

```
┌─────────────────────────────────────────────────────────┐
│                                                         │
│  Dashboard                                              │
│  Your study overview for today                          │
│                                                         │
│  ┌─────────────────────────────────────────────────┐    │
│  │  Study Streak            │  Reviews Today        │    │
│  │  12 days 🔥              │  45 / 120             │    │
│  │  Best: 45 days           │  38% complete         │    │
│  └─────────────────────────────────────────────────┘    │
│                                                         │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐              │
│  │ Learning │  │ Review   │  │ Mastered │              │
│  │   23     │  │   156    │  │   89     │              │
│  │ kanji    │  │ due now  │  │ total    │              │
│  └──────────┘  └──────────┘  └──────────┘              │
│                                                         │
│  ┌────────────────────────────────────────────────┐     │
│  │  Recent Activity           ┌────────────────┐  │     │
│  │                            │ Quick Practice │  │     │
│  │  • Reviewed 水 today       │   ▶ Start      │  │     │
│  │  • Learned 5 new kanji     └────────────────┘  │     │
│  │  • Completed N5 deck                          │     │
│  └────────────────────────────────────────────────┘     │
│                                                         │
│  ┌────────────────────────────────────────────────┐     │
│  │  Continue Learning                              │     │
│  │                                                   │
│  │  N5 Kanji ━━━━━━━━╸━━━━━━━━━━━━━ 45%  ─▶         │
│  │  N4 Kanji ━━━━━╸━━━━━━━━━━━━━━━━ 25%  ─▶         │
│  │  Vocabulary ━━╸━━━━━━━━━━━━━━━━━ 15%  ─▶         │
│  └────────────────────────────────────────────────┘     │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### Dashboard Widgets

All widgets follow a consistent card pattern with:
- Icon (top-left, #606060 or accent)
- Title (#F0F0F0)
- Metric (#C2FC8B for main, #A0A0A0 for secondary)
- Optional progress bar or sparkline
- Interactive on click/hover

---

## 10. Study Screen Concept

### Layout

```
┌─────────────────────────────────────────────────────────┐
│                                                         │
│  Study — N5 Kanji                        ╳ (close)      │
│  水 (mizu) — Water                       3/12           │
│                                                         │
│  ┌─────────────────────────────────────────────────┐    │
│  │                                                   │    │
│  │                                                   │    │
│  │           [ Character Display Area ]               │    │
│  │                                                   │    │
│  │           水                                       │    │
│  │           Stroke animation or static               │    │
│  │                                                   │    │
│  │                                                   │    │
│  └─────────────────────────────────────────────────┘    │
│                                                         │
│  ┌─────────────────────────────────────────────────┐    │
│  │  Meaning: Water           Kun: みず              │    │
│  │  On: スイ               Radical: ⽔              │    │
│  └─────────────────────────────────────────────────┘    │
│                                                         │
│  [Drawing Canvas Area — for writing practice]            │
│  ┌─────────────────────────────────────────────────┐    │
│  │                                                   │    │
│  │    [Draw character here]                          │    │
│  │                                                   │    │
│  │    ────────────────────────                       │    │
│  │                                                   │    │
│  └─────────────────────────────────────────────────┘    │
│                                                         │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐              │
│  │  Show    │  │  I'know  │  │  Next    │              │
│  │  Answer  │  │  ✓ (skip)│  │  ►       │              │
│  └──────────┘  └──────────┘  └──────────┘              │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### Character Display

- Large character rendering: Centered, responsive
- Size: 120-180px based on available space
- Color: #F0F0F0 for known characters
- Stroke order: SVG overlay with animated strokes
- Grid: Subtle #2A2A2A guide lines when drawing

### Progress Bar (Study Header)

```
  水 (mizu) — Water                        3/12

  ━━━━━━━━━━━━━╸━━━━━━━━━━━━━━━━━━━━━  25%

  Track: #2A2A2A | Fill: #C2FC8B | Height: 4px
```

### Action Buttons

```
Show Answer:
  Background:     transparent
  Border:         1px solid #2A2A2A
  Text:           #A0A0A0
  Radius:         12px
  Hover:          border: 1px solid #C2FC8B, text: #C2FC8B

I Know:
  Background:     rgba(194,252,139,0.1)
  Text:           #C2FC8B
  Radius:         12px
  Hover:          background: rgba(194,252,139,0.2)

Next:
  Background:     #C2FC8B
  Text:           #050505
  Radius:         12px
  Hover:          filter: brightness(1.1)
  Active:         transform: scale(0.97)
```

---

## 11. GitHub Banner Concept

### Banner Specifications

```
Format:            SVG/GitHub social preview
Dimensions:        1280 x 640px
Background:        #050505 (OLED black)
```

### Banner Layout

```
┌──────────────────────────────────────────────────────────────────┐
│                                                                  │
│                                                   ┌──────────┐  │
│                                                   │  GitHub   │  │
│                                                   │  Star ⭐  │  │
│                                                   └──────────┘  │
│                                                                  │
│               [Kaiteyo Mark — large, centered]                    │
│                                                                  │
│                       K A I T E Y O                               │
│                                                                  │
│             書いてよ — Write. Practice. Master.                   │
│                                                                  │
│         An open-source Japanese mastery tool                      │
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │  [Subtle glow gradient line — #C2FC8B → transparent]       │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                  │
│  Built with Jetpack Compose  |  Kotlin Multiplatform             │
│  Available on Windows · macOS · Linux · Android · iOS            │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

### Banner Elements

```
Logo Mark:
  Position: Center-top, y: 120px
  Size: 120x120px
  Color: #C2FC8B with subtle glow (0 0 40px rgba(194,252,139,0.3))

Title "KAITEYO":
  Position: Below logo
  Font: Inter Bold, 64px
  Color: #F0F0F0
  Letter-spacing: 0.15em

Subtitle "書いてよ":
  Position: Below title
  Font: Noto Sans JP, 28px
  Color: #C2FC8B

Tagline "Write. Practice. Master.":
  Position: Below subtitle
  Font: Inter, 20px
  Color: #A0A0A0
  Letter-spacing: 0.05em

Description:
  Font: Inter, 16px
  Color: #606060

Decoration Line:
  Gradient: #C2FC8B → transparent
  Height: 1px
  Width: 480px
  Margin: auto

Footer Tech Stack:
  Font: Inter, 14px
  Color: #404040
```

---

## 12. UI Component Style Guide

### Buttons

```
Primary Button:
  ┌──────────────────────┐
  │    Start Review       │
  └──────────────────────┘
  Background:         #C2FC8B
  Text:               #050505, 15px, Inter Medium
  Padding:            14px 28px
  Radius:             12px
  Hover:              filter: brightness(1.08), translateY(-1px)
  Active:             transform: scale(0.97)

Secondary Button:
  ┌──────────────────────┐
  │    Show Answer        │
  └──────────────────────┘
  Background:         transparent
  Border:             1px solid #2A2A2A
  Text:               #A0A0A0, 15px, Inter Medium
  Radius:             12px
  Hover:              border-color: #C2FC8B, text: #C2FC8B

Ghost Button:
  ┌──────────────────────┐
  │    Cancel             │
  └──────────────────────┘
  Background:         transparent
  Text:               #606060, 15px, Inter Medium
  Hover:              text: #A0A0A0

Icon Button:
  ┌──┐
  │⚙️│
  └──┘
  Size:               36x36px
  Icon:               20px
  Color:              #606060
  Hover:              color: #C2FC8B
  Radius:             8px
```

### Text Inputs

```
┌────────────────────────────────────┐
│  Search kanji...                    │
└────────────────────────────────────┘
Background:         #1A1A1A
Border:             1px solid #2A2A2A (focus: #C2FC8B)
Text:               #F0F0F0, 15px
Placeholder:        #404040
Radius:             12px
Padding:            14px 16px
Focus glow:         box-shadow: 0 0 0 2px rgba(194,252,139,0.1)
```

### Progress Bars

```
Linear Progress:
  ━━━━━━━━━━━━━╸━━━━━━━━━━━━━━━━
  Height:             4px
  Track:              #2A2A2A
  Fill:               #C2FC8B
  Radius:             2px
  Animation:          600ms ease when updating

Radial Progress:
  Size:               48x48px
  Track color:        #2A2A2A
  Fill color:         #C2FC8B
  Stroke width:       3px
```

### Toggle / Switch

```
OFF:  ○ ————○   (circle: #404040, track: #2A2A2A)
ON:   ● ————●   (circle: #C2FC8B, track: rgba(194,252,139,0.2))
Size: 40x24px
Animation: 200ms ease
```

### Cards

```
┌──────────────────────────────────────┐
│  ◆ Title                     ─▶      │
│  Subtitle or description              │
│  ─────────────────────────────────    │
│  45% complete    [━━━━━━━━━╸━━━━]    │
└──────────────────────────────────────┘
Background:   #0D0D0D
Radius:       16px
Padding:      20px
Hover:        translateY(-2px), stronger shadow
Transition:   250ms cubic-bezier(0.4, 0, 0.2, 1)
Border:       Optional 0.5px #2A2A2A
```

### Dropdown / Select

```
┌──────────────────────────────────────┐
│  N5 Kanji                      ▼     │
└──────────────────────────────────────┘
┌──────────────────────────────────────┐
│  N5 Kanji                       ✓    │
│  N4 Kanji                            │
│  N3 Kanji                            │
│  Vocabulary                     ▸    │
└──────────────────────────────────────┘
Background:       #1A1A1A
Text:             #F0F0F0
Selected:         #C2FC8B text
Hover BG:         rgba(255,255,255,0.04)
Radius:           12px
Border:           1px solid #2A2A2A
```

### Tooltips

```
┌──────────────────────┐
│  Study Streak: 12    │
│  Best: 45 days       │
└──────────────────────┘
Background:           #1A1A1A
Border:               1px solid #2A2A2A
Text:                 #F0F0F0, 13px
Radius:               8px
Padding:              8px 12px
Arrow:                8px triangle pointing to element
Animation:            fade in + slight slide, 150ms
```

### Scrollbar (Custom)

```
Track:
  Width:     6px
  Color:     transparent

Thumb:
  Width:     6px
  Color:     #2A2A2A
  Radius:    3px
  Hover:     #606060

Behavior:   Auto-hide when not scrolling
```

### Modal / Dialog

```
┌──────────────────────────────────────────┐
│                                          │
│           Dialog Title                    │
│                                          │
│    Dialog content and description         │
│    that explains the action.              │
│                                          │
│       [Cancel]    [Confirm]              │
│                                          │
└──────────────────────────────────────────┘
Overlay:          rgba(0,0,0,0.6)
Dialog BG:        #101010
Radius:           24px
Width:            400px (max 90vw)
Padding:          32px
Shadow:           0 16px 48px rgba(0,0,0,0.5)
Animation:        scale(0.95→1) + fade, 200ms
```

---

## 13. Animation System

### Principles

1. **Purposeful** — Every animation serves a purpose (feedback, guidance, delight)
2. **Subtle** — Nothing flashy or attention-seeking
3. **Fast** — Under 300ms for most interactions
4. **Smooth** — Use cubic-bezier for natural motion

### Timing Chart

| Interaction | Duration | Easing |
|------------|----------|--------|
| Hover state | 150ms | ease-out |
| Button press | 100ms | ease |
| Page transition | 300ms | cubic-bezier(0.4, 0, 0.2, 1) |
| Sidebar item | 200ms | cubic-bezier(0.4, 0, 0.2, 1) |
| Card hover | 250ms | cubic-bezier(0.4, 0, 0.2, 1) |
| Modal appear | 200ms | cubic-bezier(0.4, 0, 0.2, 1) |
| Progress bar | 600ms | ease-out |
| Tooltip appear | 150ms | ease-out |
| Drawing stroke | 300ms per stroke | ease-in-out |

### Transition Styles

```
Page Transition (Navigation):
  - Fade + slight slide (8px in direction)
  - Duration: 300ms
  - Affects: content panel only (sidebar stays static)

Card Entrance (On page load):
  - Fade in + translateY(12px → 0px)
  - Stagger: 80ms between each card
  - Duration: 400ms

Sidebar Indicator (Active item):
  - Right border appears from right edge
  - Width: 0px → 2px
  - Duration: 200ms

Hover Effects:
  - Scale: 1.0 → 1.02 (for cards)
  - TranslateY: 0 → -2px
  - Shadow opacity increase
  - Duration: 250ms
```

### Glow Animations

```
Accent Glow (pulse):
  - For: active items, review buttons
  - Keyframes:
    0%:     box-shadow: 0 0 0px rgba(194,252,139,0)
    50%:    box-shadow: 0 0 12px rgba(194,252,139,0.3)
    100%:   box-shadow: 0 0 0px rgba(194,252,139,0)

Download/Update Glow:
  - Subtle rotating gradient on progress
  - Duration: 2s, infinite
  - Opacity: 0.08
```

---

## 14. Implementation Roadmap

### Phase 1: Theme System (v1.1.0)

```
Priority: HIGH
Files to modify:
  - Color.kt           → Replace with Kaiteyo palette
  - Theme.kt           → Update color scheme, extra colors
  - Typography.kt      → Update font definitions
  - Dimens.kt          → Add Kaiteyo spacing/radius system

Implementation order:
  1. Color palette
  2. Typography
  3. Theme application
  4. Surface/elevation values
```

### Phase 2: Window Design (v1.1.0)

```
Priority: HIGH
Files to modify:
  - desktopApp/Main.kt → Custom undecorated window
  - New: TitleBar.kt    → Custom window controls

Implementation order:
  1. Window undecorated flag
  2. Custom title bar component
  3. Window control buttons with hover animations
  4. Window dragging and resize handling
```

### Phase 3: Layout Architecture (v1.1.0)

```
Priority: HIGH
Files to modify:
  - MainScreen.kt      → New floating panel layout
  - MainNavigation.kt  → Sidebar navigation
  - MainScreenData.kt  → Updated nav items

Implementation order:
  1. Background (OLED black)
  2. Sidebar panel with radius
  3. Content panel with radius
  4. Navigation items restyled
```

### Phase 4: Component Polish (v1.1.0)

```
Priority: MEDIUM
Files to modify:
  - common/Button.kt       → Updated button styles
  - common/AppTextField.kt → Updated input styles
  - Common card styles     → Card radius and colors
  - Progress indicators     → Kaiteyo progress bar style

Implementation order:
  1. Buttons (primary, secondary, ghost)
  2. Cards and containers
  3. Input fields
  4. Progress indicators
  5. Toggles and switches
```

### Phase 5: Animation System (v1.1.0)

```
Priority: MEDIUM
Files to create/modify:
  - New: Animation.kt         → Animation constants
  - Sidebar animations        → Active indicator
  - Page transitions          → Fade + slide
  - Card entrance animations  → Staggered entrance

Implementation order:
  1. Animation constants and easings
  2. Sidebar navigation animations
  3. Page/content transitions
  4. Card and list entrance animations
  5. Micro-interactions (hover, press)
```

### Phase 6: Logo & Assets (v1.1.0)

```
Priority: MEDIUM
Files to create:
  - New: kaiteyo_logo.svg             → Main logo mark
  - New: kaiteyo_wordmark.svg         → Wordmark
  - New: kaiteyo_banner.svg           → GitHub banner
  - Updated: windowIcon reference     → New icon reference
  - New: App icon resources           → Multi-resolution icons

Implementation order:
  1. SVG logo mark
  2. SVG wordmark
  3. App icon (all required sizes)
  4. GitHub/project banner
  5. Splash screen concept
```

---

## 15. Theme Studio v2.0 Design

### Overview

The Theme Studio is a full-featured theme customization suite accessible from Settings. It provides 6 tabs with real-time live preview.

### Tab System

```
┌─────────────────────────────────────────────────────────────┐
│  [Base]  [Accent]  [Color]  [Grad]  [Motion]  [Layout]     │ ← Animated tab bar
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Tab Content (changes based on selected tab)                │
│                                                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────────────────────────────────────────────┐   │
│  │           LIVE PREVIEW PANEL                          │   │
│  │  Real-time preview of sidebar + content + stats       │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### Color Wheel (HSV)

```
┌──────────────────┐
│     ● ← indicator │  ← Interactive HSV color wheel
│   ●      ●       │     180x180px, circular
│  ●   ●    ●      │     Touch/drag to pick color
│   ●      ●       │     White + black ring indicator
│     ●            │
└──────────────────┘

Hue: 0-360° mapped to angle around wheel
Saturation: 0-1 mapped to radius from center
Value: Fixed at 1.0 (controlled via HSV slider tab)
```

### Color Editors

| Mode | Controls | Range |
|------|----------|-------|
| RGB | 3 sliders (R, G, B) | 0-255 each |
| HSL | 3 sliders (Hue, Saturation, Lightness) | 0-360°, 0-100%, 0-100% |
| HSV | 3 sliders (Hue, Saturation, Value) | 0-360°, 0-100%, 0-100% |
| HEX | Text input with validation | #000000-#FFFFFF |

### Gradient Editor

```
Type: [Linear] [Radial] [Angular]  ← Tab selection

Stops: ● ● ● ● ●  (2-8 stops, each clickable for color picker)

Angle: ────●──────── 0-360° (for Linear/Angular)

Intensity: ──●────── 0.0-2.0x
Opacity:    ──●────── 0-100%

Preview:
┌──────────────────────────────────────┐
│  ░░░░░░░░░░░░░░ gradient ░░░░░░░░░  │  ← 60dp preview box
└──────────────────────────────────────┘

[Apply Gradient to Theme]
```

### Motion Studio Presets

| Preset | Speed | Best For |
|--------|-------|----------|
| None | Instant | Accessibility, low-power devices |
| Minimal | Fast | Productivity, power users |
| Balanced | Normal | Default — most users |
| Smooth | Slow | Premium feel, presentations |
| Cinematic | Slow | Demo mode, first impressions |

---

## 16. Floating Island Sidebar v2.0 Design

### Concept

The sidebar is no longer attached to the window edge. It floats as an elevated island that can be repositioned, resized, and docked.

### Dock States

```
Floating:    Free-moving island with drag handle
DockedLeft:  Snapped to left edge with margin
DockedRight: Snapped to right edge with margin
DockedTop:   Snapped to top edge with margin
DockedBottom:Snapped to bottom edge with margin
+ 4 Corner variants (TL, TR, BL, BR)
```

### Visual Design

```
┌──────────────────┐
│ ═══ drag handle   │ ← 36x4dp rounded pill, 25% opacity
│ [×]            [◀]│ ← Close/dock + collapse toggle
│                    │
│ ◆ Dashboard        │ ← Active: accent color + 12% bg
│ ◇ Kanji            │ ← Default: secondary text
│ ◇ Vocabulary       │
│ ◇ Practice         │
│                    │
│ ──────────────     │ ← Divider
│                    │
│ 45/120 today  45%  │ ← Progress widget
│                    │
│ ┌──────────────┐   │
│ │ Start Review  │   │ ← Primary CTA
│ └──────────────┘   │
│                    │
│         ╭──────────╯ ← Resize handle (bottom-right)
└─────────╯
```

### Floating Mode Specs

```
Elevation:      24dp shadow (16dp when docked)
Corner Radius:  36dp (20dp when docked)
Background:     surface color with 12dp margin from edges
Drag Handle:    36x4dp rounded pill at top center
Close Button:   20dp circle with ×, top-left
Resize Handle:  16dp area, bottom-right, diagonal lines
Collapse Toggle:22dp, rotates 180° on expand/collapse
```

---

## 17. Brush System Design

### Brush Types

| Brush | Cap | Join | Width Multiplier | Use Case |
|-------|-----|------|------------------|----------|
| Pen | Round | Round | 1.0x | Standard writing |
| Calligraphy | Round | Round | 1.5x | Japanese brush style |
| Pencil | Butt | Bevel | 0.8x | Sketching, fine detail |
| Side | Square | Round | 3.0x | Thick marker strokes |

### Stroke Processing Pipeline

```
Raw Input (touch/pen/mouse)
    │
    ▼
┌─────────────┐
│ Jitter       │ ← Eliminates tremor (< threshold distance)
│ Reduction    │
└─────────────┘
    │
    ▼
┌─────────────┐
│ Low-pass     │ ← Moving average smoothing (factor 0-0.95)
│ Smoothing    │
└─────────────┘
    │
    ▼
┌─────────────┐
│ Velocity     │ ← Adaptive: slow=more smooth, fast=responsive
│ Smoothing    │
└─────────────┘
    │
    ▼
┌─────────────┐
│ Prediction   │ ← Extrapolates 1-3 points from velocity+accel
└─────────────┘
    │
    ▼
┌─────────────┐
│ Bezier       │ ← Catmull-Rom spline (2-8 segments per pair)
│ Interpolation│
└─────────────┘
    │
    ▼
Smooth Output Points
```

### Settings

| Setting | Default | Range | Effect |
|---------|---------|-------|--------|
| Smoothing | Enabled | On/Off | Master toggle |
| Smoothing Factor | 0.5 | 0.0-0.95 | Higher = smoother but more lag |
| Prediction | Enabled | On/Off | Reduces perceived latency |
| Prediction Points | 2 | 1-3 | Number of extrapolated points |
| Velocity Smoothing | Enabled | On/Off | Adaptive smoothing by speed |
| Pressure | Disabled | On/Off | Requires pressure-sensitive stylus |
| Jitter Reduction | Enabled | On/Off | Eliminates hand tremor |
| Jitter Threshold | 1.5px | 0.5-10px | Minimum movement to register |
| Bezier Smoothing | Enabled | On/Off | Curve interpolation |
| Bezier Segments | 4 | 2-8 | Quality vs performance |

---

## 18. Branded Installer Design

### Screen Flow

```
Welcome → Location → Components → Theme → Accent → Accessibility → Progress → Completion
```

### Visual Style

```
Window: Full-size dialog, 800x600px recommended
Background: Surface gradient with subtle radial accent glow
Logo: Kaiteyo mark in accent color, 96dp
Typography: Consistent with app design system
Animations: Slide + fade between steps
Step Indicator: Dots at top, color-filled for completed steps
Navigation: Back/Skip/Next/Finish buttons at bottom
```

### Completion Screen Design

```
┌────────────────────────────────────────────┐
│                                            │
│           ✓ (success circle)                │
│                                            │
│       Installation Complete!               │
│  Kaiteyo has been successfully installed.  │
│                                            │
│  ┌─────────────────────────────────────┐   │
│  │ [✓] Launch Kaiteyo now              │   │
│  │ [✓] Delete installer files          │   │
│  │ ─────────────────────────────       │   │
│  │ [📖] View Release Notes  [🌐] GitHub│   │
│  └─────────────────────────────────────┘   │
│                                            │
│       Thank you for choosing Kaiteyo!      │
└────────────────────────────────────────────┘
```

---

## 19. Onboarding Wizard Design

### Screen Flow

```
Welcome → Theme → Accent → Scaling → Font Size → Sidebar → Animations → Finish
```

### Design Principles

1. **First-launch only** — Wizard shows once, can be re-triggered from Settings
2. **Live preview** — Each step shows real-time effect of choices
3. **Skip All** — Power users can jump to finish
4. **Step progress** — Visual bar at top, labeled "Step X/8"
5. **Consistent navigation** — Back / Skip All / Continue

### Sidebar Layout Step

```
Position:                                 Mode:
┌────┐ ┌────┐ ┌────┐ ┌────┐     ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐
│ ▶  │ │ ◀  │ │ ▼  │ │ ▲  │     │Always│ │Auto  │ │Icons │ │Floating│
│Left│ │Right│ │Top │ │Bot │     │      │ │Hide  │ │Only  │ │Island │
└────┘ └────┘ └────┘ └────┘     └──────┘ └──────┘ └──────┘ └──────┘

Preview:
┌──────────────────┐
│ ┌──┐             │  ← Animated mini preview showing
│ │  │             │     sidebar position relative to content
│ │S │   Content   │
│ │  │             │
│ └──┘             │
└──────────────────┘
```

---

## 20. Animation System Expansion

### Spring Animations

All major animations now use spring physics for natural feel:

```kotlin
// Standard spring configuration
spring(
    dampingRatio = Spring.DampingRatioMediumBouncy,  // 0.5
    stiffness = Spring.StiffnessMedium               // 400
)

// Components using spring:
// - Sidebar collapse/expand
// - Floating island drag
// - Hover effects on controls
// - Theme switching transitions
// - Progress bar updates
```

### New Animation Use Cases

| Component | Animation | Duration | Type |
|-----------|-----------|----------|------|
| Sidebar dock/undock | Slide + fade + size | 300ms | Spring |
| Color wheel indicator | Circle move | 100ms | Spring |
| Gradient preview | Opacity | 200ms | Tween |
| Installer steps | Slide + fade | 300ms | Tween |
| Onboarding steps | Slide + fade | 350ms | Tween |
| Progress bars | Width | 600ms | Tween |
| Tab selection | Background color | 200ms | Tween |
| Card selection | Border + bg color | 200ms | Tween |
| Collapse toggle | Rotation 180° | 300ms | Spring |
| Drag elevation | Shadow increase | 200ms | Spring |
| Resize handle | Opacity on hover | 150ms | Tween |

### 60-120 FPS Targets

| Operation | Target FPS | Optimization |
|-----------|-----------|--------------|
| Sidebar drag | 120 FPS | Use `graphicsLayer` for transforms |
| Color wheel | 60 FPS | Canvas drawing, avoid recomposition |
| Progress bars | 60 FPS | Animate only width fraction |
| Page transitions | 60 FPS | Use AnimatedContent with spec |
| Theme switching | 60 FPS | Animate only visible colors |
| List scrolling | 120 FPS | LazyColumn with stable keys |

---

## Appendix: Color Accessibility

### Contrast Ratios

| Combination | Ratio | WCAG |
|------------|-------|------|
| #C2FC8B on #050505 | 12.1:1 | AAA ✓ |
| #F0F0F0 on #050505 | 17.4:1 | AAA ✓ |
| #A0A0A0 on #050505 | 8.5:1 | AAA ✓ |
| #606060 on #050505 | 4.6:1 | AA ✓ |
| #C2FC8B on #0D0D0D | 11.8:1 | AAA ✓ |
| #F0F0F0 on #0D0D0D | 16.8:1 | AAA ✓ |
| #050505 on #C2FC8B | 12.1:1 | AAA ✓ |

### Focus Indicators

- Default: 2px solid #C2FC8B outline
- Keyboard navigation: 3px solid #C2FC8B with 2px offset
- Never remove focus indicators entirely

---

## Appendix: File Structure for New Assets

```
Kaiteyo/
├── core/
│   └── src/
│       └── commonMain/
│           └── kotlin/
│               └── ua/
│                   └── syt0r/
│                       └── kanji/
│                           └── presentation/
│                               └── common/
│                                   └── theme/
│                                       ├── Color.kt        ← Updated
│                                       ├── Theme.kt        ← Updated
│                                       ├── Typography.kt   ← Updated
│                                       └── Dimens.kt       ← Updated
├── desktopApp/
│   └── src/
│       └── jvmMain/
│           └── kotlin/
│               └── ua/
│                   └── syt0r/
│                       └── kanji/
│                           └── desktopApp/
│                               ├── Main.kt               ← Updated
│                               └── TitleBar.kt           ← NEW
├── preview_assets/
│   ├── kaiteyo_logo.svg           ← NEW
│   ├── kaiteyo_wordmark.svg       ← NEW
│   ├── kaiteyo_banner.svg         ← NEW
│   ├── kaiteyo_icon_32.png        ← NEW
│   ├── kaiteyo_icon_64.png        ← NEW
│   ├── kaiteyo_icon_128.png       ← NEW
│   ├── kaiteyo_icon_256.png       ← NEW
│   └── kaiteyo_icon_512.png       ← NEW
└── DESIGN_SYSTEM.md               ← NEW (this document)
```

---

> *"This is not just Kanji Dojo renamed. This is a completely new premium Japanese learning application."*

---

## Appendix: Implementation Status (v2.2.1)

> The vision above is the **brand contract**. What ships today is grounded in real
> code — this appendix maps each vision area to its implementation so engineers can
> tell aspirational from shipped. When the docs and code disagree, the code wins.

### Theme & palette — SHIPPED

- Palette lives in `core/.../theme/Color.kt`: `BaseMode` (`Oled`/`Dark`/`Light`/
  `Sepia`) + `AllAccentSchemes` (7: Signature Pineapple, Cotton Candy, Ocean,
  Forest, Sunset, Lavender, Monochrome) + semantic colors. The "Signature" theme
  above = `BaseMode.Oled` + `AllAccentSchemes[0]`.
- `Theme.kt` provides `KaiteyoThemeState`, `AppTheme`, the CompositionLocals and the
  **whole-app color crossfade** on theme switch (450ms base, speed-aware).
- Desktop Theme Studio themes persist to `~/.kaiteyo/themes/` via `ThemeManager`
  and map onto the shared system through `ThemeMapper`.

### Typography — SHIPPED (Material 3 scale)

- `core/.../theme/Typography.kt` `AppTypography` — Material 3 type scale with the
  **Japanese locale list** baked in (not the hand-rolled 11–64px scale above).
- Live user scaling via `TypeScale` (fontScale/titleScale/lineHeight/letterSpacing).

### Window shell — SHIPPED (delivered in `desktopApp/.../KaiteyoWindow.kt`)

| Vision | Reality |
|--------|---------|
| Custom borderless window, controls top-right | ✅ 44dp custom title bar, undecorated window |
| Title bar 32px, #0D0D0D | ✅ 44dp, surface background |
| 1200×800 default, 900×600 min | ✅ 1200×800 default, **860×600** min |
| Rounded 24px sidebar/content | ✅ `Dimens.SidebarWidth=260`, `SidebarRadius=24dp`, `ContentRadius=24dp`, `PanelGap=24dp`, `WindowPadding=24dp` |
| Window position/size remembered | ✅ `WindowStateStore` → `~/.kaiteyo/window.json`, work-area validated |
| Native drag + snap | ✅ Windows `WM_NCLBUTTONDOWN`/HTCAPTION, Linux EWMH, Compose fallback |
| 8-zone resize handles | ✅ 5dp edges / 10dp corners, native cursors |

### Sidebar / navigation — SHIPPED (`NavShell.kt`, `WorkspaceShell.kt`)

- **Mobile/shared** `NavShell`: Sidebar (any edge, expanded/compact) or Floating
  bubble with snap points; `Ctrl+B` toggles; item height 40dp (48dp phone).
- **Desktop** `WorkspaceShell`: dock rail on any edge in `DsDockIsland` (8dp float
  ring), `<720dp` falls to a compact tab bar (hysteresis exit 760dp); top bar with
  palette button; floating launcher/launchpad in bubble mode.
- Active nav item = accent 14% fill + accent text (matches the vision's active state).

### Components — SHIPPED (`desktopApp/.../designsystem/`)

`DsButton`/`DsIconButton`/`DsTextButton` (5 kinds), `DsCard`/`DsListItem`/
`DsVirtualList`/`DsSkeleton`/`DsEmptyState`, `DsDialog`/`DsConfirmDialog`/
`DsPromptDialog`/`DsProgressDialog`, `DsTextField`/`DsSearchField`/`DsNumericField`,
`DsSelect`/`DsTabRow`/`DsChip`/`DsCategoryBadge`, `DsMenuPanel`/`DsMenuItem`,
`DsTagChip`/`DsFlagBadge`/`DsPriorityFlag`, `DsToastHost`, `DsToolbar`/`DsSplitPane`,
`DsBadge`/`DsStatTile`/`DsProgressBar`/`DsToggle`/`DsLink`/`DsSectionHeader`.
All token-driven (`DsSpacing`/`DsRadius`/`DsType`/`DsMotion`/`DsElevation`/
`DsSemantic`), adaptive (`DsResponsive` width tiers + `adaptiveDialogWidth`).

### Iconography — PARTIAL

- Brand mark shipped as `BrandMark` (brand asset, used in wizard/launcher); custom
  `ExtraIcons` set for app-specific glyphs; Material Icons for the rest. The stroke-
  trio SVG above is the brand concept the mark derives from.

### Theme Studio / Appearance Studio — SHIPPED

- `AppearanceStudio.kt` (shared) + desktop `ThemeStudioView` (`ThemeManager`):
  Base/Accent/Color/Gradients/Motion/Layout tabs, color wheel + RGB/HSL/HSV/HEX,
  gradient editor, motion presets, layout controls, JSON export/import, live preview.

### Onboarding wizard — SHIPPED (`desktopApp/.../OnboardingWizard.kt`)

- 8 steps: Welcome → Theme → Accent → UI scaling → Font size → Navigation →
  Motion → Finish. Live previews, skip-all, re-openable from Settings.

### Not yet shipped (from the vision)

- Branded installer theme steps (installer exists; the themed in-app wizard flow is
  a concept), system-wide global hotkeys, per-monitor DPI awareness, multiple
  windows, minimize-to-tray, brush calligraphy/pressure stylus pipeline, animated
  gradient backgrounds.