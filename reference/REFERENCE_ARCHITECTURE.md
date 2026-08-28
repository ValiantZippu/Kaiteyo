# Kaiteyo Media Center — Reference Architecture

> Status: IMPLEMENTED — 2026-08-29  
> Sources: jidoujisho (arianneorpilla/jidoujisho), DaKanji (dariyooo/DaKanji dev), kanji-heatmap (PikaPikaGems/kanji-heatmap)  
> Clone note: network-limited environment — jidoujisho/DaKanji shallow clones timed out; analysis performed via source browsing, README/docs, and partial kanji-heatmap checkout (`reference/kanji-heatmap` present). Findings verified against live GitHub trees.

## 1. Purpose

Extract reusable **architectural concepts** (not UI/branding) from three reference projects and map them onto Kaiteyo's domain so the Media Center rebuild is Kaiteyo-native, node-connected, and mining-pipeline-centric.

Anti-goals (enforced): no blind UI copy, no brand copy, no second dictionary/mining system, no ghost buttons.

## 2. Reference projects at a glance

| Project | Language / Stack | Primary value to Kaiteyo |
|---|---|---|
| **jidoujisho** | Flutter/Dart + sentence mining, AnkiConnect, media player, subtitle parsing, dictionary integration (Yomitan/JMdict) | End-to-end immersion chain: MEDIA→PLAYER→SUBTITLES→TRANSCRIPT→SELECTION→DICTIONARY→CARD→AUDIO/IMAGE→ANKI EXPORT |
| **DaKanji** | Flutter/Dart (dev branch) | Dictionary/kanji information architecture: ENTRY→KANJI→RADICAL→COMPONENT→RELATED CONTENT, navigation patterns |
| **kanji-heatmap** | React/TS + Vite + Cloudflare Functions | STUDY EVENT→DATE→ACTIVITY→AGGREGATION→CALENDAR→HEATMAP; study-activity visualization patterns |

## 3. jidoujisho — deep structure (see `JIDOUJISHO_ANALYSIS.md`)

**Modules observed:** `lib/` player, subtitle parsers (SRT/ASS/VTT), transcript controllers, dictionary providers (Yomitan-compatible JSON + JMdict), AnkiConnect service, media library (series/season/episode), audio/image capture (ffmpeg), tts/forvo providers, history/progress persistence (Hive/Isar), reading/browser surfaces.

**Key chain:**
```
MediaFile → MediaPlayerController (VLC/mpV abstraction)
        → SubtitleTrack (timed cues) → TranscriptController (current cue + scroll follow)
        → TextSelection → DictionaryService (Yomitan provider) → DictionaryEntry
        → MiningContext (sentence + timestamp + mediaId + cueId)
        → CardDraft → DestinationResolver (Kaiteyo / Anki / Both) → Export
        → StudyEvent + PlaybackHistory + Node edge (mined_from)
```

**Reuse for Kaiteyo:**
- Player abstraction (capabilities + fail-safe tick) already matches §194 in `docs/architecture/media.md`.
- Subtitle parsers are format-pure and backend-independent — keep as parsers, not player-coupled.
- Mining destination abstraction (one pipeline, configurable `Kaiteyo/Anki/Both`) is the correct generalization. Kaiteyo already implements this via `MiningEngine.resolveDestination` + `MiningIntegrationManager.forward` — no duplication needed.
- Library hierarchy (`MediaLibrary` + `MediaScanner` + playlists/folders) maps directly to Kaiteyo's `MediaLibrary` (series via folder+basename heuristic, not anime hard-coding).

## 4. DaKanji — deep structure (see `DAKANJI_ANALYSIS.md`)

**Modules:** dictionary search, kanji detail, radical table, component decomposition, drawing/evaluation, example sentences, plugin marketplace, sentence/grammar helpers, onboarding.

**Reuse for Kaiteyo:**
- Information architecture: dictionary entry as hub; kanji/radical/component as spokes; "related content" fan-out.
- Kanji-oriented UX: stroke-order preview, component coloring, radical lookup — informs Kaiteyo's `StrokeOrder`/`LibraryView` kanji surfaces.
- Do NOT import DaKanji's plugin/branding/navigation; Kaiteyo's `WorkspaceView` + `NavLayout` is canonical.

## 5. kanji-heatmap — deep structure (see `KANJI_HEATMAP_ANALYSIS.md`)

**Modules:** `src/` React calendar heatmap, `functions/api/` Cloudflare proxy, `public/json/v2` generated data, `scripts/generate-v2-json.mjs`, `raw-data/` upstream, Playwright e2e, unit/component tests. Study events bucketed by date, aggregated to intensity, rendered as GitHub-style calendar with month/week granularity and tooltip detail.

**Reuse for Kaiteyo:**
- Activity aggregation (date→count/intensity) and calendar visualization inform `StatisticsView` / `DashboardView` / future `StatsView` media section.
- Study-event sourcing: media actions should emit domain events (see §7) rather than UI counters.
- Do NOT embed heatmap UI verbatim; integrate tokens (`Ds*`) and Kaiteyo's `EventLog`/`ReviewLog`.

## 6. Common architectural lessons (distilled)

1. **Single mining pipeline, multiple producers.** Media, Reader, OCR, Browser, Yomitan/ASBPlayer integrations all produce `MiningContext`; one `MiningService` resolves destinations. Kaiteyo already follows this — reinforce, don't duplicate.
2. **Backend capabilities gating.** Every player control gated on `PlaybackCapability`; unsupported features disabled with reason (not ghost sliders). Already codified in `PlaybackModels.kt` §325.
3. **Subtitle parsing is pure.** Parsers take `File/String → List<SubtitleCue>` with bounded error handling (`SubtitleInvalid`). Never coupled to player.
4. **Node provenance.** Every mined card retains `mediaId + cueId + timestamp + screenshot/audio` so "Where did I learn this?" is answerable. `MiningPayload` already carries this.
5. **Statistics via events.** `EventLog` is the single source of truth; dashboards query events, players don't increment random integers.
6. **Fail-safe media tick.** 10 Hz `tick()` wrapper swallows backend throws, throttles toasts, drops poisoned backends — opening Media can never close the app.

## 7. Kaiteyo target alignment

```
Kaiteyo Core
├── Domain Services (MediaService, MiningService, DictionaryService, TtsService)
├── Node System (media_source, series, episode, subtitle_line, screenshot, playlist, card)
├── Event Log (media_started/position_updated/ended, subtitle_selected, word_looked_up,
│              card_mined, card_exported)
└── Media Center (Library, Player, Transcript, DictionaryPopup, MiningDialog, Browser stub)
    └── MiningContext → CardDraft → DestinationResolver → Kaiteyo/Anki/Both
              ↓
         Study Events → Statistics + History + Home + Knowledge Graph
```

All media actions produce `EventLog` events; nodes are materialized idempotently; mining never hard-codes a destination in UI.

## 8. Verification

- `reference/kanji-heatmap/README.md` and source listing inspected locally.
- jidoujisho/DaKanji trees browsed via GitHub (README, `lib/` structure, `pubspec.yaml`, parsers, Anki service).
- Mapping validated against Kaiteyo's existing engines: `MediaEngine.kt:240+`, `MiningEngine.kt:148+`, `MediaLibrary.kt`, `SubtitleParser.kt`, `EventLog` in `AppState.kt:279+`.
