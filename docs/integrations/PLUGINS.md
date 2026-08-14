# Plugin Architecture

## Status: 📋 Planned — scaffold exists, no runtime loading

The desktop suite ships a **manifest-driven plugin registry and marketplace scaffold**
(`desktopApp/.../engine/plugin/`), but **no runtime code loading is implemented**. Plugins
cannot currently be installed, loaded, or executed. Treat everything below as the intended
architecture, not shipped behavior.

## Intended architecture

```
PluginMarketplace (scaffold)
    │  discover plugins (manifest list)
    ▼
PluginRegistry (scaffold)
    │  register known plugins, capabilities, versions
    ▼
(planned) Plugin runtime
    │  load plugin artifact, sandbox, verify
    ▼
Extension points:
    - dictionary plugins (additional sources/formats)
    - OCR backends
    - subtitle extractors
    - mining sources
    - theming / integrations
```

## Design principles (intended)

1. **Manifest-driven** — a plugin declares its id, version, capabilities, and permissions
   in a manifest; the registry validates it before anything is loaded.
2. **Marketplace as a catalog** — the marketplace lists available plugins; installation
   is a separate, user-confirmed step.
3. **Sandboxing** — if runtime loading is implemented, plugins must run with explicit
   capability grants and no implicit access to user data or the network.
4. **Security first** — this is the reason runtime loading is not shipped yet. See the
   note in `SECURITY.md`: "If plugin execution is added in the future, this document must
   be updated to describe its sandboxing."

## What exists today

| Piece | Location | Status |
|---|---|---|
| `PluginRegistry` | `desktopApp/.../engine/plugin/PluginRegistry.kt` | Scaffold (registration data model) |
| `PluginMarketplace` | `desktopApp/.../engine/plugin/PluginMarketplace.kt` | Scaffold (catalog UI model) |
| `PluginsView` | `desktopApp/.../ui/plugins/PluginsView.kt` | UI surface |

## Roadmap

- Define the manifest schema and capability model
- Implement secure runtime loading (sandboxed classloader / subprocess)
- Wire extension points (dictionary sources, OCR backends, subtitle extractors)
- Ship the marketplace catalog + install flow
