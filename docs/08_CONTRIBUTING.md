# Kaiteyo (書いてよ) — Contributing Guide

## How to Contribute

We welcome contributions from the community. This guide explains how to contribute effectively.

## Quick Start

1. Fork the repository
2. Create a branch: `git checkout -b feature/your-feature-name`
3. Make your changes
4. Run tests: `./gradlew :core:test`
5. Commit your changes: `git commit -m "feat: add your feature"`
6. Push to your fork: `git push origin feature/your-feature-name`
7. Open a Pull Request

## Development Workflow

1. Check the `/docs/11_TODO.md` for available tasks
2. Discuss your approach in the issue tracker
3. Implement the changes
4. Ensure the build passes locally
5. Submit a pull request

## Coding Standards

### General
- Write clean, readable code
- Comment complex logic, not obvious code
- Use meaningful variable names
- Keep functions focused and small

### Kotlin
- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use `data class` for data holders
- Use `sealed class` for sealed hierarchies
- Prefer `val` over `var`
- Use explicit return types for public API

### Compose
- Annotate all composable functions with `@Composable`
- Use `Modifier` parameter for styling
- Keep composables focused on a single responsibility
- Extract reusable components
- Use previews for visual testing

### Git Commits

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
feat: add floating sidebar
fix: correct window drag behavior
docs: update architecture guide
refactor: extract theme state holder
perf: optimize animation performance
style: format code according to standards
test: add unit tests for ThemeManager
```

### Branch Naming

```
feature/floating-sidebar
fix/window-drag-region
docs/architecture-guide
refactor/theme-system
perf/animation-optimization
```

### Pull Requests

1. **Title**: Clear description of the change
2. **Description**: What, why, and how
3. **Linked Issue**: Reference the issue number
4. **Screenshots**: For UI changes
5. **Testing**: How you tested the changes
6. **Checklist**: 
   - [ ] Build passes
   - [ ] Tests pass
   - [ ] No new warnings
   - [ ] Documentation updated

## AI Contribution Workflow

Since AI contributes to this codebase, follow these guidelines:

1. Read `/docs` first to understand the project
2. Identify the specific file(s) to modify
3. Understand the existing code patterns
4. Make minimal, focused changes
5. Do NOT modify Gradle configurations
6. Do NOT execute terminal commands
7. Only edit source code files
8. Ensure imports are correct for the project's Compose version
9. Follow the design language in `/docs/02_DESIGN_LANGUAGE.md`

## Code Review Expectations

- All code is reviewed before merging
- Automated checks must pass
- Performance must not regress
- Design must align with brand guidelines
- Architecture must follow existing patterns

## Community

- Use GitHub Issues for bug reports and feature requests
- Use GitHub Discussions for questions and ideas
- Be respectful and constructive
- Follow the code of conduct

## License

By contributing, you agree that your contributions will be licensed under the project's license.
