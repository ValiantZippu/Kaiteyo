# 🎬 media — Media Analysis & Workflows

This section documents the **workflow layer** of Kaiteyo's media systems: how Kaiteyo
provides Yomitan-style glossing and ASBPlayer-style subtitle mining as native features,
and how the whole media → lookup → understand → mine → study loop works. The underlying
engines are specified in `docs/architecture/media.md` (playback/subtitles) and
`docs/architecture/mining.md` (mining).

| Document | Purpose | Status |
|---|---|---|
| [`YOMITAN.md`](YOMITAN.md) | How Yomitan works, what Kaiteyo reuses, and how a browser-extension-free glossary is delivered | Current (suite) + target (product) |
| [`ASBPLAYER_WORKFLOW.md`](ASBPLAYER_WORKFLOW.md) | The native subtitle-selection → screenshot/audio → card workflow (Jidoujisho-style loop) | Current (suite) + target (product) |

## The core loop (MASTER §58)

```
MEDIA → subtitle → select word/phrase → dictionary → Yomitan-style glossary →
pitch/frequency → example → screenshot → audio → card → deck → Kaiteyo →
optional Anki → study → statistics → exam → knowledge graph
```

Verified current end-to-end in the desktop suite; product integration gated on ADR-0017.

## Related

- Playback/subtitle engine: `docs/architecture/media.md`
- Mining pipeline: `docs/architecture/mining.md`
- Dictionary engine + popup: `docs/architecture/dictionary.md`
- Backends & licensing: `docs/integrations/MEDIA_BACKENDS.md`
- Anki integration: `docs/integrations/ANKI.md`
- Feature status: `docs/features/MEDIA.md`
