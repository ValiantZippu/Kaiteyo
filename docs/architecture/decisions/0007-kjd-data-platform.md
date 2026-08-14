# ADR-0007: KJD — Standalone Data Platform for the Bundled Language Database

**Status**: Accepted

## Context

The bundled language database is generated from multiple openly licensed datasets
(KanjiVG, KANJIDIC, JMdict, JmdictFurigana, Tanos, Leeds, yomichan-jlpt-vocab). Doing
this by hand or inside the app is unmaintainable, non-reproducible, and risky from a
licensing/provenance standpoint.

## Decision

- Create **KJD** (`kjd/`, package `io.kaiteyo.kjd`) — a standalone, reusable JVM data
  platform: ingests datasets → normalizes → resolves into a canonical model → validates →
  emits a SQLite database + typed API + CLI + export formats.
- KJD is **architecturally independent of Kaiteyo**: no runtime dependency on source
  projects; the app (desktop) consumes the generated database and can apply incremental
  patches (`DatabasePatcher`).
- Provenance is first-class: every entity carries `SourceRef`s; licenses are recorded per
  source (never invented); an attribution manifest is emitted with every build.
- Deterministic builds: same sources + same generator ⇒ same database.

## Alternatives

- Hand-maintained data files — rejected: unmaintainable, error-prone.
- In-app data transformation — rejected: mixes concerns, bloats the app, hard to test.
- Depend on third-party prebuilt databases (e.g. JMDict SQLite dumps) — rejected: less
  control over provenance and licensing, inconsistent shapes.

## Consequences

- The desktop suite also embeds a JVM copy of the platform (`desktop/engine/jdata/`) with
  its own generation pipeline (a second implementation evolved separately — see
  `architecture/DATA_PLATFORM.md`). Consolidating these two is an open technical-debt item.
- Data updates ship as patch feeds rather than full app releases (desktop).
- New datasets can be added behind the same pipeline with proper attribution.

## Implementation notes

- `kjd/README.md`, `kjd/src/main/kotlin/io/kaiteyo/kjd/`
- Desktop jdata: `desktopApp/.../desktop/engine/jdata/`
- Bundled DB versioning: `buildSrc/.../AppAssets.kt`
