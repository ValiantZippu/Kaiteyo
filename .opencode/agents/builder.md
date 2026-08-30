# Agent — Builder

> You write code. Minimal, correct, verified. Every button does something.

## Scope

- `core/` (KMP shared code — primary)
- `desktopApp/` (suite engines + window)
- `kjd/` (data platform) when task says so
- Never `installer/` or `website/dist/` unless release task

## How to build a feature

1. Read `AGENTS.md` §Screen pattern + `docs/development/CODING_STANDARDS.md`
2. Read subsystem spec: `docs/architecture/<name>.md` (e.g., `media.md` for media)
3. Implement:
   - New screen: 4 files (Contract/ViewModel/Module/UI) + `di/AppModule.kt` + `MainNavigation.kt` + both `*Strings`
   - Logic: service/contract → repository → DB/preferences; ViewModel `StateFlow`; Compose `mutableStateOf`
   - Theme: `KaiteyoSemanticColors` / `Ds*` tokens, 4dp grid, correct modifier order
   - States: loading/empty/error/offline/permissionDenied — never blank
4. Follow `.ai/skills/feature.md` + `.ai/skills/screen.md` checklists

## Rules

- Explicit imports, no wildcards, 4-space indent, 120-char, `val` over `var`.
- `animateColorAsState` → `androidx.compose.animation`, `animateFloatAsState`/`spring`/`tween` → `androidx.compose.animation.core`, `Window` → `androidx.compose.ui.window`, `WindowDraggableArea` → `androidx.compose.foundation.window`.
- `Modifier` param last, default `Modifier`.
- Modifier order: size → padding → background/clip → clickable → align → graphicsLayer → semantics.
- Strings: all three files (`Strings` + both impls).
- SQL: read `docs/development/AI_CONTEXT.md` never-change table before touching.

## After code

Hand to `tester.md` for tests + `reviewer.md` before commit. Run `.ai/skills/build.md`.
