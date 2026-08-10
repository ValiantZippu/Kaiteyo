# Kaiteyo (書いてよ) — Brand Guidelines

## Brand Identity

Kaiteyo is a premium, minimalist Japanese language learning application. The brand evokes:
- Modern Japanese design aesthetics
- Clean, precise typography
- Purposeful whitespace
- Subtle, premium materials

## Brand Voice

- **Clear** — No jargon. No fluff. Instructions are direct.
- **Respectful** — Treats the learner as capable and intelligent.
- **Calm** — Neutral tone. No urgency. No gamification hype.
- **Supportive** — Encouraging without being patronizing.

## Tone

| Context | Tone |
|---------|------|
| Error messages | Informative, not alarming |
| Success states | Brief acknowledgment, no celebration |
| Onboarding | Warm, clear, minimal |
| Settings | Neutral, professional |
| Learning feedback | Constructive, precise |

## Visual Direction

### Japanese Inspiration

- Influenced by modern Japanese graphic design (Muji, Uniqlo, minimalism)
- Clean layouts with generous whitespace
- Subtle use of Japanese characters as decorative elements
- Color palette inspired by traditional Japanese art and nature

### Minimalism

- Content is the interface. Every element serves a purpose.
- No unnecessary borders, shadows, or decorations
- Information density is carefully controlled
- Focus on typography hierarchy over visual embellishment

### Craftsmanship

- Smooth 60fps animations
- Intentional micro-interactions
- Consistent spacing using 4dp grid
- Hover states on all interactive elements
- Thoughtful loading states

### Motion

- Animations use spring physics for natural feel
- Transitions are subtle and purposeful
- No gratuitous motion — movement should guide attention
- Reduced motion option for accessibility

### Premium Feel

- Applications like Figma, Linear, Raycast, Arc Browser set the quality bar
- Glass morphism used sparingly for depth
- Soft shadows with color tinting
- Consistent corner radius throughout
- High-contrast text for readability

## Logo Usage

The Kaiteyo logo should always:
- Appear in the application title bar / window chrome
- Use the signature lime (#C2FC8B) and orange (#FEAB57) colors
- Be treated as a wordmark, not an icon-only mark

## Color Usage

### Signature Palette

| Token | Color | Hex | Usage |
|-------|-------|-----|-------|
| Primary | Lime Green | `#C2FC8B` | Primary actions, selected states, active navigation |
| Secondary | Warm Orange | `#FEAB57` | Secondary actions, highlights, accents, hover states |
| Background | Dark | `#1A1A1A` | Main surface (dark mode) |
| Surface | Elevated | `#242424` | Cards, dialogs, sidebar |
| Text Primary | White | `#FFFFFF` | Primary content |
| Text Secondary | Gray | `#A0A0A0` | Supporting text, labels |

### Distribution Strategy

The Signature theme MUST use BOTH colors:
- **Lime (#C2FC8B)**: Primary buttons, navigation indicators, progress, selection, active states
- **Orange (#FEAB57)**: Secondary buttons, hover highlights, focus rings, gradient accents, glowing effects

Do NOT apply lime uniformly. Distribute colors intelligently like Material Design 3:
- Lime dominates structural highlights (selected tabs, active nav)
- Orange dominates interactive feedback (hover glow, focus indicators, press effects)
- Both colors appear in gradients for premium surfaces

## Typography

- Use system fonts (SF Pro on macOS, Segoe UI on Windows, Roboto on Linux)
- No custom font files to keep the app lightweight
- Hierarchy controlled by weight and size, not different typefaces
- Code fragments use monospace system font
