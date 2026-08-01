# ADR-0001: Kaiteyo Brand Identity

**Status**: Accepted  
**Date**: 2024  
**Deciders**: Project maintainers

## Context

The project was originally forked from Kanji Dojo. To establish an independent identity and differentiate from the source project, a new brand was needed.

## Decision

We will:

1. **Rename** the application from "Kanji Dojo" to "Kaiteyo (書いてよ)"
2. **Use** lime (#C2FC8B) as the primary brand color
3. **Use** orange (#FEAB57) as the secondary brand color
4. **Keep** internal package namespaces (`ua.syt0r.kanji`) unchanged to avoid breaking compatibility
5. **Update** all user-facing strings to use "Kaiteyo"

## Rationale

- "Kaiteyo" means "write it!" in Japanese — an active, engaging name
- The lime + orange color scheme is distinctive and memorable
- Keeping internal package names avoids unnecessary refactoring risk
- Clear separation from Kanji Dojo establishes Kaiteyo as its own project

## Consequences

- Users will see "Kaiteyo" throughout the application
- Internal code still references "kanji" in package names
- Documentation and branding consistently use the new name
- Future rename of internal packages is possible but not currently planned

## Alternatives Considered

- Keeping "Kanji Dojo" — rejected, as this is a fork with different vision
- Abstract Japanese name — rejected, "Kaiteyo" has clear meaning
- English-only name — rejected, Japanese name fits the app's purpose
