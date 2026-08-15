# Pitch Accent & Frequency Architecture

**Status**: PARTIAL — frequency metadata is REAL (KJD pipeline); pitch-accent data
is RESEARCH (no verified open source ingested yet). **Source**: expansion spec §21;
`docs/data/SOURCES.md`; `docs/architecture/dictionary.md`; TODO.md (pitch-accent
diagrams feature).

## Principle

Pitch accent and frequency are **data-provider-driven**: the system is designed so
additional pronunciation/frequency datasets can be imported without code changes,
and the UI never hardcodes one data source. The accent type is data, not a hardcoded
lookup table in the UI (§21).

## Data model (target)

| Field | Meaning | Status |
|---|---|---|
| `word` | the surface form | ✅ (dictionary) |
| `reading` | the reading (kana) the accent applies to | ✅ (dictionary readings) |
| `pitch pattern` | per-mora H/L pattern | 🔬 RESEARCH |
| `accent type` | 平板/頭高/中高/尾高 (heiban/atamadaka/nakadaka/odaka) | 🔬 RESEARCH |
| `frequency` | frequency rank/band (top 1k/2k/5k/10k) | ✅ (KJD frequency metadata) |
| `source` | dataset provenance (license, version, checksum) | ✅ (provenance records) |

Provider interface (target):

```
PitchAccentProvider.lookup(word, reading) → PitchAccent?   // nullable — no fake data
FrequencyProvider.rank(word) → FrequencyBand?
```

- A provider is a dataset adapter (like KJD dataset adapters): ingest → normalize →
  validate → index → serve. No provider → the UI shows nothing accent-related (or a
  clearly-labeled "no data" state) — never a fabricated pattern (§64, §290).
- Multiple providers can be installed; resolution order is data (a preference),
  not code.

## Visualization (target, §21)

- Pattern strip: mora boundaries with L/H markers (平板, 頭高, …); per-mora colors
  (color-blind-safe: pattern + shape, never color alone — `docs/input/accessibility.md`).
- Optional pitch contour overlay (curve) — configurable, off by default.
- Shown on: dictionary entries, vocabulary detail, mining results, story gloss
  (when a word is glossed) — everywhere a reading is shown, if data exists.
- **Never hardcode one source into the UI**: the strip renders whatever the
  active provider returns.

## Frequency (real today)

- Frequency bands exist in the bundled data (KJD frequency metadata) and power:
  knowledge profile frequency coverage (top 1k/2k/5k/10k), difficulty vectors
  (`docs/learning/adaptive-learning.md`), search ranking (dictionary).
- Refresh/provenance: frequency datasets are versioned in the KJD pipeline with
  provenance records (`docs/data/SOURCES.md`, §185).

## Roadmap

1. RESEARCH openly licensed pitch-accent datasets (expand spec §21; TODO.md).
2. License verification (§183–§185) → KJD adapter → provider.
3. Visualization UI behind the provider interface.
4. Wire into dictionary/vocabulary/story gloss surfaces.

## Contracts

- `PitchAccentService.lookup(word, reading) → Result<PitchAccent?, ProviderInfo>`
  — never fabricates; returns provider + confidence so the UI can label it.
- The service is read-only; it never mutates knowledge or decks.

## Acceptance criteria

1. Adding a pitch dataset = adding a provider (zero UI/code changes beyond the
   adapter).
2. No pitch data → honest empty state, never a fake pattern.
3. Frequency-based features (profile coverage, difficulty) all trace to versioned
   datasets.
4. Visualization is color-blind-safe and accessible.

## Related

- Data sources/licenses: `docs/data/SOURCES.md` · Dictionary: `docs/architecture/dictionary.md`
- Difficulty: `docs/learning/adaptive-learning.md` · Knowledge: `docs/learning/progress-model.md`
- Kanji graph: `docs/learning/kanji-knowledge-graph.md` (readings edge)
