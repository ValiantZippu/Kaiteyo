# Kaiteyo Developer Tooling

**Status**: LIVE (existing CLI + scripts, documented in `docs/cli/`) + TARGET
(proposed tooling). **Source**: expansion spec §59–§60; STANDARDS §236–§240.

## What exists (LIVE)

- **`kaiteyo` CLI** (`tools/cli/`) — project-local Python CLI: task discovery,
  dev commands, project config cache. Full docs: `docs/cli/README.md`,
  `docs/cli/COMMANDS.md`, `docs/cli/ARCHITECTURE.md`, `docs/cli/CONFIGURATION.md`,
  `docs/cli/AUTOMATION.md`, `docs/cli/TROUBLESHOOTING.md`.
- **Scripts** (`scripts/`, `installer/scripts/`) — build/installer helpers,
  stage/verify artifacts, update manifests.
- **Gradle** — the real build/test toolchain (`docs/development/COMMANDS.md`).

## Proposed tooling (expansion §59 — build only when justified)

| Tool | Responsibility | Status |
|---|---|---|
| `kaiteyo dev doctor` | Diagnose env: Java, Gradle, Git, Android SDK, Python, dataset deps (§240) | PLANNED |
| `kaiteyo dev lint` / `test` | Wrapper over real Gradle tasks (never invents tasks — §238) | PLANNED |
| `kaiteyo docs check` | Validate docs: orphan scan, broken links, freshness (§336) | PLANNED |
| `kaiteyo content validate` | Run content-pipeline gates on a package (`docs/content/content-pipeline.md`) | TARGET (with pipeline) |
| `kaiteyo data import` | KJD dataset import wrapper with provenance checks (§184–§185) | PLANNED |
| `kaiteyo asset validate` | Asset pipeline validation (naming, dimensions, formats) | TARGET |
| `kaiteyo db migrate` | Database migration helper (versioned migrations) | PLANNED |
| `kaiteyo license check` | Dependency/license checker (§202) | PLANNED |
| TODO scanner / dead-code scanner / translation checker | Hygiene tooling (§59) | PLANNED (only if they earn their keep) |

## Rules

1. **The wrapper must not invent tasks** (§238): it discovers actual Gradle tasks.
2. **Never hide Git** (§237): commit/push helpers show files, diff summary,
   branch, remote, message, confirmation.
3. Tools are built when they reduce friction measurably — not because they
   sound useful (§59).
4. All tooling documented in `docs/cli/` + `docs/development/`; no undocumented
   scripts.

## Related

- CLI docs: `docs/cli/` · Development: `docs/development/DEVELOPER_GUIDE.md`
- Content tools: `docs/content/content-pipeline.md` (authoring/validation)
- Asset tools: `docs/architecture/assets.md`
- AI contributor workflow: `docs/development/AI_CONTEXT.md`
