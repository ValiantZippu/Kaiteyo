# Legal & Licensing

## Kaiteyo's license

Kaiteyo is free software under the **GNU General Public License v3.0** (or, at your
option, any later version). The full text is in the repository root
[`LICENSE`](../../LICENSE).

> Kaiteyo is a fork of [Kanji Dojo](https://github.com/syt0r/Kanji-Dojo) (© 2022–2023
> Yaroslav Shuliak) and is independently developed. Original Kanji Dojo code remains
> GPL-3.0.

## What is licensed under what

| Component | License | Notes |
|---|---|---|
| Kaiteyo source code (core, desktopApp, app, iosApp, kjd, installer, website, mediaGenerator) | GPL-3.0 (or later) | see `LICENSE` |
| Bundled third-party **data** (KanjiVG, KANJIDIC, JMdict, JmdictFurigana, Tanos, Leeds, yomichan-jlpt-vocab) | their own licenses (CC BY-SA / CC BY family) | see [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md) and `../data/SOURCES.md` |
| User-imported dictionaries & decks | user's responsibility | never redistributed by Kaiteyo |
| App icons / branding | original Kaiteyo artwork | see `../branding/` |
| Fonts (media generator assets) | Quicksand — OFL | build-time promo assets only |

## Third-party notices structure

- In-app: the **Credits/About** screen (AboutLibraries) renders
  `core/credits/libraries/*.json` — the authoritative list of bundled data + library
  licenses. Keep these JSON files in sync when datasets change.
- Repo: [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md) summarizes the same
  information for humans.
- Generated data: KJD emits a machine-readable attribution manifest
  (`THIRD_PARTY_DATA.json` / `.md`) with every generated database — provenance is
  per-entity, not just per-project.

## Redistribution requirements (for packagers)

1. Keep the license and attribution notices with any distributed build.
2. Keep the credits/attribution data bundled and reachable from the app.
3. Re-verify Tanos and Leeds redistribution terms before each release (they may change).
4. Do not remove upstream (Kanji Dojo) attribution for derived code.

## Integration licensing

- **VLCJ** (GPL-3.0) — compatible with Kaiteyo's GPL-3.0; used only when VLC is present.
- **Tesseract/Tess4J** — Apache-2.0 (Tess4J) with Tesseract's own license; OCR is
  optional and user-provided.
- **Anki / AnkiConnect** — Anki is AGPL-3.0; AnkiConnect is licensed under AGPL-3.0.
  Kaiteyo *interoperates* via its HTTP API — it does not embed or link Anki code.
- **Yomitan** — AGPL-3.0; Kaiteyo reads the same dictionary formats and is not derived
  from Yomitan code.

## Contact

License questions: open an issue on GitHub
(<https://github.com/ValiantZippu/Kaiteyo/issues>) or use GitHub Discussions.
