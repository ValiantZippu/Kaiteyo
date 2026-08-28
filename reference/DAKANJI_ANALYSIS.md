# DaKanji — Architectural Analysis

> Repo: https://github.com/dariyooo/DaKanji/tree/dev (Flutter/Dart, dev branch)  
> Inspected: README, lib/ tree (dictionary, kanji, radical, drawing, search), pubspec, dev branch structure

## 1. Source structure (dev branch)

```
lib/
├── dictionary/      # Entry search, JMdict/KanjiDic providers, result ranking
├── kanji/           # Kanji detail, readings, stroke order, radical decomposition
├── radicals/        # Radical table, lookup by stroke count / Bushu
├── drawing/         # Canvas, stroke capture, evaluation (KanjiVG geometry)
├── examples/        # Example sentences, grammar notes
├── search/          # Trigram/prefix search, kana handling, deinflection
├── plugins/         # Plugin registry/marketplace stubs
└── ui/              # Entry page, kanji page, radical grid, navigation shell
```

## 2. Information architecture

```
DICTIONARY → ENTRY → KANJI → RADICAL → COMPONENT → RELATED CONTENT
```

- Entry is hub; kanji/radical/component are spokes with bidirectional links.
- Kanji page aggregates: meaning, on/kun readings, JLPT/grade, stroke order animation, radical breakdown, similar kanji, example words.
- Radical lookup: table + search by reading/meaning; components linked to kanji containment.
- Deep links: any kanji/word on any surface can open its graph node (cf. Kaiteyo's `pendingGraphNode`).

## 3. State / persistence

- Riverpod + ObjectBox/Isar; search index built on demand per dictionary; enabled-dictionary prioritization.
- No media/player domain — purely dictionary/kanji focused.

## 4. Lessons for Kaiteyo

| DaKanji concept | Kaiteyo equivalent | Module | Decision |
|---|---|---|---|
| Entry-centric IA | `DictionaryService` + `KnowledgeGraph` + `GraphExplorerView` | `desktop/engine/dictionary`, `desktop/engine/graph` | Reinforce entry→kanji→radical fan-out; surface in Media's dictionary popup via `DictionaryPopup.kt` |
| Kanji detail (readings, stroke, radicals) | `LibraryView` stroke panel, `KanjiDetail` surfaces, `WritingEvaluator` | `desktop/ui/library`, `desktop/engine/stroke_evaluator` | Reuse existing stroke/VG stack; don't rebuild |
| Radical/component navigation | `RadicalExplorer` (core) / future desktop radical screen | `presentation/screen/main/screen/radical_explorer` | Keep navigation patterns (grid + filter) but adapt to Kaiteyo `Ds*` tokens |
| Drawing/evaluation | `WritingPracticeView` + `WritingEvaluator` | `desktop/ui/writing` | Keep canonical KanjiVG evaluation; don't fork |
| Plugin marketplace | `PluginRegistry`/`PluginMarketplace` | `desktop/engine/plugin` | Stubs only — not needed for Media Center v1 |

## 5. Anti-patterns NOT to copy

- DaKanji's custom navigation shell — Kaiteyo's `WorkspaceShell` + `WorkspaceTab` system is canonical.
- Plugin marketplace as primary surface — Kaiteyo keeps plugins as optional extension point.
- Per-feature search duplication — Kaiteyo centralizes via `SearchEngine`/`TrigramIndex`.
