# ADR-0011: Plugin Runtime Loading Deferred (Security First)

**Status**: Accepted

## Context

Community plugins (dictionary sources, OCR backends, subtitle extractors, integrations)
are a long-term goal. Executing third-party code at runtime is the single largest
security risk the app could take on, and the project has no sandbox infrastructure yet.

## Decision

- Ship the **registry + marketplace scaffold** now (manifest-driven plugin data model and
  a catalog UI), but **do not implement runtime code loading**.
- Plugins are documented as **planned**; `SECURITY.md`, `docs/security/README.md`, and
  `docs/integrations/PLUGINS.md` explicitly state that when runtime loading is added, the
  sandboxing design must be documented and the security docs updated.
- Extension points (dictionary sources, OCR backends, subtitle extractors, mining sources)
  are designed but not wired to plugin code.

## Alternatives

- Ship runtime loading immediately — rejected: unsandboxed third-party code on the user's
  machine is unacceptable without a capability model and subprocess/classloader sandbox.
- No plugin story at all — rejected: plugin extensibility is a roadmap goal.

## Consequences

- Users cannot install plugins yet; the scaffold communicates the direction.
- When implemented, plugins must declare capabilities and run with explicit grants.

## Implementation notes

- `desktopApp/.../engine/plugin/` (`PluginRegistry`, `PluginMarketplace`),
  `desktopApp/.../ui/plugins/PluginsView.kt`
- `docs/integrations/PLUGINS.md`
