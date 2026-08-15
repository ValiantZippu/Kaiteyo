# Kaiteyo — Vision

The vision layer is the **"why"** of Kaiteyo: the product, design, learning, game,
and user-philosophy documents that every architectural decision must serve. Code and
specs change; these documents change rarely and only deliberately.

## Documents

| Document | Content | Status |
|---|---|---|
| [product-vision.md](product-vision.md) | What Kaiteyo is, who it is for, what it is NOT; the product pillars | LIVE — read first |
| [design-philosophy.md](design-philosophy.md) | How the product must *feel*: calm, premium, connected, dense-but-clean (§53–§54 of the standards) | LIVE |
| [learning-philosophy.md](learning-philosophy.md) | The learning doctrine: domain-first, evidence-driven knowledge, respect the learner, no fabricated stats | LIVE |
| [game-philosophy.md](game-philosophy.md) | The Journey world as a **real game**: exploration-first, Nintendo-like accessibility, never an XP grinder | TARGET (ADR-0014) |
| [child-experience.md](child-experience.md) | A distinct instructional structure for children — shared core, different curriculum/UX | TARGET |
| [normal-user-experience.md](normal-user-experience.md) | The two interconnected experiences: study app and world; how a user moves between them | LIVE + TARGET |
| [long-term-vision.md](long-term-vision.md) | The arc from today's app to the unified language platform; what "done" looks like | LIVE (direction) |

## How this layer is used

- **Architecture** (`docs/architecture/`) is the "what/how" — the vision layer is its
  requirement source. When an architecture decision conflicts with a vision document,
  the conflict must be resolved deliberately and the decision recorded in an ADR.
- **Planning** (`docs/roadmap/`, `docs/planning/`) sequences work toward the long-term
  vision; nothing in the roadmap may silently contradict a vision pillar.
- **The engineering standard** (`docs/engineering/ENGINEERING_STANDARDS.md`) is the
  process contract; vision is the content contract.

## Relationship to existing documents

- `docs/roadmap/PROJECT_VISION.md` is the original one-page vision (v1-era). These
  documents extend and supersede it where they differ; the roadmap non-goals list still
  applies unless explicitly amended here.
- `docs/architecture/journey.md` and the `docs/architecture/nodes/` suite are the
  architectural translation of the game vision. This layer does not duplicate them —
  it is the intent behind them.
