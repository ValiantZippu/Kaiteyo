# Kaiteyo Reference

Consolidated lookup layer. These documents are **indexes and catalogs** — the
canonical detail lives in the linked specs; this directory is where you go to
find something fast.

| Document | Content | Canonical source |
|---|---|---|
| [glossary.md](glossary.md) | The Kaiteyo terminology: nodes, knowledge, SRS, content, game terms | across specs |
| Node catalog | Every node type, fields, status | `docs/architecture/nodes/NODE_TYPE_REGISTRY.md` |
| Relationship catalog | Typed relationship vocabulary | `docs/architecture/nodes/RELATIONSHIP_REGISTRY.md` |
| Event catalog | Every event, payload, subscribers | `docs/architecture/nodes/EVENT_CATALOG.md` |
| Schema reference | Database schema (two SQLDelight DBs) + migrations | `docs/architecture/database.md`, `data/ARCHITECTURE.md` |
| API reference | Local HTTP API + CLI | `docs/integrations/LOCAL_API.md`, `docs/cli/` |
| ADR index | All architecture decision records | `docs/architecture/decisions/README.md` |
| Docs map | Full documentation tree | `docs/README.md` |

## Related

- Architecture overview: `docs/architecture/OVERVIEW.md`
- File structure: `docs/architecture/FILE_STRUCTURE.md`
- Glossary: [glossary.md](glossary.md)
