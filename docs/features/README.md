# ⚡ features — Kaiteyo Feature Specifications

This directory contains detailed specifications for every major feature.

## Contents

| File | Feature | Status |
|------|---------|--------|
| `FEATURES.md` | Full feature status matrix (source of truth) | ✅ Maintained |
| `LIBRARY.md` | Unified Library hub | ✅ Implemented |
| `THEMES.md` | Theme system and Theme Studio | ✅ Implemented |
| `DESKTOP.md` | Desktop suite & window experience | ✅ Implemented |
| `MEDIA.md` | Media center & subtitles | ✅ Implemented |
| `STATISTICS.md` | Statistics & analytics | ✅ Implemented |

Individual specs for flashcards, search, tags, flags, and Anki import/export are folded
into `FEATURES.md`; the operational status of every feature is tracked there plus in
`../planning/TODO.md` / `../planning/CURRENT_ISSUES.md`.

## Specification Format

Each feature specification includes:

- **Purpose** — Why this feature exists
- **User Experience** — How users interact with it
- **Technical Design** — Implementation approach
- **Dependencies** — What other features/systems it depends on
- **Future Improvements** — Planned enhancements
- **Open Questions** — Decisions that need to be made

## Status Legend

| Status | Meaning |
|--------|---------|
| ✅ Implemented | Working and tested |
| 🚧 In Progress | Being actively developed |
| 📋 Planned | Scheduled for future release |
| 💡 Future Idea | Under consideration |

## Grounding

Every spec is written against the **actual code**, not a wishlist:

- `FEATURES.md` ends with a **code map** — the key files behind every status row.
- `LIBRARY.md`, `THEMES.md`, `DESKTOP.md`, `MEDIA.md`, `STATISTICS.md` reference the
  real composables and engines (e.g. `LibraryScreen.kt`, `ThemeStudioView.kt`,
  `WorkspaceShell.kt`, `MediaEngine.kt`, `StatisticsController`).
- Full engineering depth lives under `docs/architecture/` (dictionary, media, mining,
  study engine, statistics, exams, …); these specs are the user/feature view.

If a spec disagrees with the code, the code wins — report it per `CONTRIBUTING.md`.
