# Kanji Knowledge Graph

**Status**: MIXED — the data relationships are REAL (AppDataDatabase: radicals,
components, readings, meanings, vocabulary links, JLPT, frequency, stroke order
via KanjiVG when installed); the **graph traversal layer + user edges are
TARGET** (ADR-0013, node layer). **Source**: expansion spec §9; NODE §82–§83;
`docs/architecture/language-model.md`.

## Principle

Kanji are **not isolated flashcards**. Each kanji is a node in a graph with typed
relationships (radicals, components, readings, meanings, vocabulary, example
sentences, grammar, JLPT, frequency, stroke order, visually similar kanji,
confusable kanji, compounds, semantic relationships) — and the user can navigate
forward *and backward* through it:

```
radical → component → kanji → vocabulary → sentence → paragraph → story → media → quest
```

## The graph (expansion §9)

| Edge | Meaning | Data status |
|---|---|---|
| `radical` | the kanji's radical(s) | ✅ real (AppData) |
| `component` | structural components (kanji parts) | ✅ real (AppData decomposition) |
| `readings` | on'yomi/kun'yomi | ✅ real |
| `meanings` | English (and localized) meanings | ✅ real |
| `vocabulary` | words containing the kanji | ✅ real (vocab links) |
| `example sentences` | sentences using the kanji | 🔬 partial (Tatoeba RESEARCH for breadth) |
| `grammar` | grammar patterns the kanji appears in | 🔬 RESEARCH (open grammar dataset) |
| `JLPT` | JLPT band | ✅ real |
| `frequency` | frequency rank/band | ✅ real (frequency metadata in KJD) |
| `stroke order` | stroke sequence | ✅ real (built-in + KanjiVG when installed) |
| `visually similar` | look-alike kanji | 🔬 TARGET (data curation) |
| `confusable` | easily confused pairs | 🔬 TARGET (data curation + user evidence) |
| `compounds` | compound words | ✅ real (vocab links) |
| `semantic` | meaning clusters | 🔬 TARGET (derived) |

## User-side edges (TARGET, ADR-0013/0016)

- `encountered_by` — where the user met this kanji (media, world, reading)
- `mined_from` — cards created from it
- `appears_in_media` — anime/episode/subtitle nodes containing it
- `known_dimension` — per-dimension knowledge edges (progress model)
- `confused_with` — from real mistakes (writing/exam evidence)

These are the **"where have I seen this?"** answers (NODE §82–§83) — the killer
feature the static data can't provide.

## Navigation UX (target)

- Kanji detail page: graph chips (radical → components → similar → vocabulary →
  sentences → media) — each chip is a live query.
- Backward: from a word, its kanji; from a sentence, its words+kanji; from
  media, the words in it; from the world, the words in that place
  (`learning-in-world.md`).
- Graph traversal is the browse experience (§81 traversal chips; NODE §128–§129).

## Contracts

- `KanjiGraphService.neighbors(node, edgeTypes) → nodes` — typed query, indexed
  (no brute-force scans; STANDARDS §186–§187).
- `KanjiGraphService.whereHaveISeen(kanji) → sources[]` — from knowledge/media/
  world events.
- Storage: ADR-0013 decision (new `node`/`edge` tables vs read-model over
  existing data) — recorded, not yet implemented.

## Acceptance criteria

1. Forward and backward traversal work for every edge type with data.
2. "Where have I seen this?" answers from real events.
3. Similar/confusable sets are curated, not auto-guessed blindly.
4. Traversal resolves within search latency budgets at full dataset scale.

## Related

- Language model: `docs/architecture/language-model.md`
- Node registry: `docs/architecture/nodes/NODE_TYPE_REGISTRY.md`
- Progress: [progress-model.md](progress-model.md)
- Spec: NODE §82–§83, §81 (traversal)
