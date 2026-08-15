# ASBPlayer-style Subtitle Mining in Kaiteyo

> **Status**: core workflow `IMPLEMENTED` in the desktop suite (media → subtitle →
> popup → mine → card); product integration `TARGET` (ADR-0017); multi-word selection
> improvements `PLANNED` (KT-MINE-003).

## 1. What we take from ASBPlayer (and what we don't)

**Take**: the workflow — subtitle-driven sentence mining: select subtitle text, get
glossary, capture screenshot + audio clip, generate a card, jump back to the exact
moment. Multi-word selection is first-class (MASTER §18).

**Don't**: clone ASBPlayer's UI, its browser coupling, or depend on it at runtime.
Kaiteyo implements the workflow natively (STANDARDS §197 spirit: reuse workflows, not
code).

## 2. The workflow (verified in the suite)

```
Media playing (VLC/mpv/Java Sound)
→ subtitle line shown (SRT/ASS/SSA/VTT, timed + synced)
→ select word or phrase (segmentation + deinflection)
→ dictionary popup (readings, definitions, pitch/freq when available)
→ capture: screenshot (timestamped) + audio clip (mining duration setting)
→ card: sentence + target + reading + gloss + screenshot + audio + timestamp + source
→ destination: Kaiteyo deck (and/or Anki via AnkiConnect)
→ review: card links back to the exact scene (bookmark/timestamp)
```

## 3. Multi-word subtitle selection (KT-MINE-003)

**Requirement (MASTER §18)**: multi-word selection is first-class, not an afterthought.

| Capability | Requirement |
|---|---|
| Line selection | Select a whole subtitle line → single card |
| Phrase selection | Select a contiguous phrase across tokens → one card with sentence context |
| Token selection | Single word → compact card |
| Fallback | No dictionary hit → user can still mine the raw selection with a note |
| Deinflection | Conjugated forms resolve to dictionary headwords (Deinflect) |

## 4. Jidoujisho-style loop (MASTER §20)

Jidoujisho's mobile workflow (media → lookup → mine → study in one app) is the
reference for Kaiteyo's **single-app seamless loop**. Differences are intentional:
desktop-first (no reliance on Android-only mechanics), same loop everywhere, deeper
knowledge-graph integration (each mine writes `mined_from` + `appears_in_media` edges —
target, KT-MINE-002).

## 5. Subtitle mining contract (target)

- **Data**: MediaDocument → SubtitleTrack → SubtitleLine → text segments → tokens →
  dictionary gloss → user selection → card (+ screenshot, audio clip, timestamp,
  sourceDetail).
- **Duplicate protection**: `MinedRecord` (idempotent-ish) — mining the same line twice
  updates rather than duplicates.
- **Provenance**: every mined card records media, subtitle track/line id, timestamp,
  screenshot path, audio path (`EVENT_CATALOG.md` CardMined).

## 6. Failure modes (STANDARDS §219)

| Failure | Behavior |
|---|---|
| Missing subtitle file / track | Clear message; mining disabled with explanation |
| Media file missing on resume | Card keeps metadata; jump-to-scene shows "media not found" + browse |
| Bad subtitle timing | Parse/sync errors surfaced; player unaffected |
| Screenshot/audio capture fails | Card still created with text-only note (degraded, never lost) |
| Anki unavailable | Card mines to Kaiteyo; Anki forwarding queued/retried (MASTER §201) |

## Related

- Media/subtitle engine: `docs/architecture/media.md`
- Mining pipeline: `docs/architecture/mining.md`
- Glossary: [`YOMITAN.md`](YOMITAN.md)
- Anki forwarding: `docs/integrations/ANKI.md`
- Product spec: `docs/product/PRODUCT.md` (MASTER §18, §20, §58)
