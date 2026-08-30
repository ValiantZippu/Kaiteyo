# Kaiteyo — Jidoujisho Integration Reference

> **Status**: `ARCHITECTED` (workflow reference) — Kaiteyo's immersion loop is the Jidoujisho-style loop made desktop-native.
> Reference: `reference/jidoujisho/` (upstream clone for research).

## 1. Upstream

- Repository: https://github.com/lrorpilla/jidoujisho
- License: **GPL-3.0** (Android) — verify before reuse.
- Core: media → lookup → understand → mine → study on device (MPV + Anki + Yomitan workflow).

## 2. What Kaiteyo takes from it

The **seamless single-app loop**:

```
media → subtitle → select → dictionary popup → pitch/frequency → example → screenshot/audio → card → deck (Kaiteyo and/or Anki) → study → statistics → exam → knowledge graph
```

Documented end-to-end in `docs/architecture/mining.md` + `docs/media/ASBPLAYER_WORKFLOW.md`. Jidoujisho proves the loop works; Kaiteyo implements it without Android-only assumptions.

## 3. Differences (intentional)

- Platform: Jidoujisho is Android-first; Kaiteyo is desktop-first (Windows/macOS/Linux) with Android/iOS sharing Core.
- Persistence: Kaiteyo owns its own `ActivityEvent` ledger and `daily_stats` — no Anki-only stats.
- Mining destination: Kaiteyo / Anki / Both (Jidoujisho is Anki-centric).

## 4. What to reuse vs not

- Reuse: workflow insights, subtitle UX patterns (auto-pause, condensed playback inspiration for MediaService), sentence mining payload shape.
- Not reuse: Android-specific player code, AnkiDroid assumptions.

## 5. Maintenance risk

Moderate — mobile workflow may diverge from desktop; keep the MiningContext shape stable across platforms.
