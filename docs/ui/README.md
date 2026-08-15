# 🎨 ui — Application UI

The UI/UX specification is distributed across purpose-built sections; this folder holds
the settings catalog and points to the rest:

| Document | Purpose | Status |
|---|---|---|
| [`SETTINGS.md`](SETTINGS.md) | **The settings catalog** — every setting with default, range, effect, persistence (MASTER §45) | Current + planned entries marked |
| `docs/design/` | Design system: tokens, typography, spacing, components, themes, animation | Current |
| `docs/architecture/NAVIGATION.md` | Navigation: NavShell, sidebar/floating modes, floating bubble, launchpad | Current |
| `docs/architecture/nodes/UX_FLOWS.md` | Step-by-step UX flows for every surface (incl. empty/loading/error/offline states) | Current + target |
| `docs/architecture/accessibility.md` | Accessibility plan | Partial |

## UI rules (MASTER §40–§44, NODE §120–§125)

1. No empty pages, no centered-tiny-card layouts, no random spacing/radii/colors.
2. One token-based design system everywhere (no per-screen design).
3. Motion communicates (origin/destination/change); reduced motion respected.
4. Responsive reflow across window sizes; platform-appropriate feel per OS.
5. Navigation chrome is never more than ~20% on desktop; content stays visible.
