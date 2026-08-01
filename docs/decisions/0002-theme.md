# ADR-0002: Theme System Architecture

**Status**: Accepted  
**Date**: 2024  
**Deciders**: Project maintainers

## Context

The application needed a flexible theming system that supports:
- Multiple built-in themes (Light, Dark, OLED, etc.)
- Custom user-created themes
- Real-time preview in Appearance Studio
- Cross-platform consistency (Desktop, Android, iOS)
- Gradient, glow, radius, animation, and density customization

## Decision

We will implement a token-based theme system with the following architecture:

1. **KaiteyoThemeState** — Mutable state holder for all theme properties
2. **KaiteyoAccentScheme** — Data class defining accent colors and gradients
3. **BaseMode** — Enum for Light, Dark, and OLED base modes
4. **CompositionLocals** — For propagating theme state through the composable tree
5. **ThemeManager** — Interface for persisting and loading theme preferences

### Key Design Decisions

- **CompositionLocals over ViewModels** for theme state — Theme is a UI concern, not business logic
- **Token-based colors** — All colors are derived from a small set of tokens, not hardcoded
- **Mutable state** — Theme state is mutable for real-time preview, but persisted via ThemeManager
- **Gradient support** — Each accent scheme can define gradient start/end colors
- **Config objects** — Glow, radius, animation, and density each have their own config data class

## Rationale

- CompositionLocals provide the simplest way to propagate theme through deeply nested composables
- Token-based approach allows infinite theme variations from a small set of parameters
- Mutable state enables live preview without complex state management
- Separate config objects keep the system extensible

## Consequences

- Theme state is available anywhere in the composable tree via `LocalKaiteyoThemeState.current`
- Adding new theme properties requires updating KaiteyoThemeState and all consumers
- Theme changes trigger recomposition of all consuming composables
- Performance must be monitored — excessive recomposition could be an issue

## Alternatives Considered

- **Material3 dynamic theming** — Rejected, not flexible enough for custom themes
- **Singleton theme object** — Rejected, not testable and causes issues with preview
- **Redux-style state management** — Rejected, overkill for UI-only state
