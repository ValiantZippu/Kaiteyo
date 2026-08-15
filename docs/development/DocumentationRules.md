# Kaiteyo — Documentation Rules

## Why Documentation Must Stay Current

Outdated documentation is worse than no documentation. It actively misleads developers. These rules ensure documentation evolves with the project.

## When Documentation Must Be Updated

Documentation MUST be updated whenever:

| Change | Documentation to Update |
|--------|------------------------|
| New folder created | `docs/architecture/FILE_STRUCTURE.md` |
| New module added | `docs/architecture/FILE_STRUCTURE.md`, `docs/architecture/OVERVIEW.md` |
| New feature added | `docs/features/FEATURES.md`, relevant feature spec in `docs/features/` |
| Feature removed | `docs/features/FEATURES.md`, `CHANGELOG.md` (repo root) |
| Feature renamed | All references to the feature name |
| New dependency added | `docs/architecture/OVERVIEW.md`, `docs/architecture/FILE_STRUCTURE.md` |
| Architecture changes | `docs/architecture/OVERVIEW.md`, `docs/architecture/FILE_STRUCTURE.md` |
| API changes | `docs/api/` relevant files |
| Theme system changes | `docs/design/THEME_SYSTEM.md` |
| UI component changes | `docs/design/UI_SYSTEM.md` |
| Animation changes | `docs/design/ANIMATION_SYSTEM.md` |
| Bug fixed | `docs/planning/CURRENT_ISSUES.md` (mark as fixed) |
| Release made | `CHANGELOG.md` (repo root), `docs/planning/COMPLETED.md` |
| Roadmap changed | `docs/roadmap/ROADMAP.md` |
| New document created | `docs/README.md` (update documentation map) |
| Engineering standard / process change | `docs/engineering/ENGINEERING_STANDARDS.md`, `docs/architecture/decisions/` (new ADR) |
| New feature spec written | `docs/features/` index + `docs/architecture/` spec (`database.md`, `dictionary.md`, …) |
| Build/setup/toolchain issue solved | `docs/troubleshooting/` issue entry, `docs/maintenance/VersionHistory.md` |
| Build/setup/toolchain issue remains | `docs/maintenance/KnownLimitations.md` with a status |
| Dependency changed | `docs/setup/UpdatingDependencies.md`, `docs/maintenance/DependencyUpdates.md` |

## Documentation Quality Standards

1. **Every document must have a clear purpose** — The first paragraph should explain what the document covers and who should read it.

2. **Every document must be findable** — The documentation map in `docs/README.md` must list all documents.

3. **Every folder must have a README.md** — Explaining the folder's purpose, contents, and how to use it.

4. **No dead links** — All internal links must resolve. Broken links are bugs.

5. **No placeholder content** — "TODO" or "Coming soon" in documentation is not acceptable. Either write the content or don't include the document.

6. **Code examples must compile** — Any code snippet in documentation should be valid Kotlin/Compose code.

7. **Screenshots must be current** — If a screenshot shows UI, it must match the current implementation.

## Documentation Review Checklist

Before merging any PR that changes functionality:

- [ ] Does this change affect any existing documentation?
- [ ] Have I updated all affected documentation?
- [ ] Have I checked for broken links?
- [ ] Have I updated the changelog?
- [ ] Have I updated the feature status?
- [ ] Have I updated the issue tracker if this fixes a known issue?
- [ ] Have I updated the roadmap if this changes priorities?

## AI Documentation Workflow

When using AI assistants:

1. **Before making changes**: The AI should read relevant documentation first
2. **After making changes**: The AI should update all affected documentation
3. **Verification**: The AI should check that documentation is consistent with code

Prompt template for AI:
```
Read docs/development/AI_CONTEXT.md first.
Then read docs/planning/CURRENT_ISSUES.md.
Implement the fix for [issue].
After implementation, update:
- docs/planning/CURRENT_ISSUES.md (mark issue as fixed)
- CHANGELOG.md (repo root — add entry)
- Any other affected documentation
```

## File Structure Documentation

`docs/architecture/FILE_STRUCTURE.md` is the map of the project. It must be updated whenever:

- A new directory is created at the root level
- A new module is added
- A key file is added or removed
- Module responsibilities change

The file structure document should always accurately reflect the current state of the repository.

## Permanent Troubleshooting Workflow

Documentation is part of the definition of done. When any developer fixes a bug, changes architecture, setup, dependencies, folders, Gradle, Java, Git workflow, project structure, build process, or release process:

1. Add or update the issue in `docs/troubleshooting/` using Title, Symptoms, Cause, Diagnosis, Fix, Verification, Prevention, and Related Issues.
2. Append the dated result to `docs/maintenance/VersionHistory.md`.
3. Add or update a status in `docs/maintenance/KnownLimitations.md`.
4. Update the relevant setup, dependency, architecture, or file-structure page.
5. Run the affected verification command and record it in the issue.

Every troubleshooting page links back to the issue index, setup guidance, dependency guidance, and Git workflow. Do not close a task until those links and records are current.
