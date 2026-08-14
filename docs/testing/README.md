# Testing Strategy

## Where tests live

| Location | What it covers | Runner |
|---|---|---|
| `core/src/commonTest/` | Shared logic: SRS/FSRS, stroke evaluation, statistics, transfer codecs, import pipeline, writing sessions | `./gradlew :core:allTests` |
| `core/src/jvmTest/` | JVM-specific shared code (e.g. Anki package round-trips) | `./gradlew :core:allTests` |
| `desktopApp/src/jvmTest/` | Desktop suite: dictionary/segmenter, jdata (SVG paths, KanjiVG provider, stroke bridge, writing sessions), media (library, subtitles, media keys, stats), transfer (Anki import mapper), updates (KJD patches) | `./gradlew :desktopApp:test` |
| `kjd/src/test/` | Data platform: parsers, normalization, entity resolution, FTS search, schema migration, DB diff/patch, attribution writer, safe archive extraction, end-to-end pipeline | `./gradlew :kjd:test` |
| `mediaGenerator/src/commonTest/` | Asset generator: screenshot/video recording tests | `./gradlew :mediaGenerator:test` |

Tests use `kotlin.test` with JUnit Platform (`useJUnitPlatform()` in the module build
files). Test naming uses backtick sentences, e.g. `` `load data should update state` ``.

## Test levels

### Unit tests
Cover pure logic with no platform dependencies — this is the bulk of the suite:
- **SRS / FSRS** — `FsrsSchedulerTest.kt`, `WritingSessionTest.kt`,
  `StrokeEvaluatorTest.kt`
- **Statistics** — `DayPracticeBreakdownTest`, `DeckRetentionTest`, `ExamGeneratorTest`,
  `ExamScorerTest`, `GoalHistoryTest`, `KnowledgeGrowthTest`, `LearningProfileTest`,
  `StatisticsCalculatorTest`, `StudyVelocityTest`, `WeeklyExamTest`
- **Transfer** — `TransferCodecsTest` (JSON/CSV/TSV/TXT round trips), `ImportPipelineTest`
  (preview/duplicates/policies), `AnkiPackageJvmTest` (round trip, GUID stability,
  malformed input)
- **Desktop suite** — `JapaneseSegmenterTest`, `SvgPathConverterTest`,
  `KanjiVgGeometryProviderTest`, `StrokeEvaluationBridgeTest`, `KanjiWritingSessionTest`,
  `MediaLibraryTest` family, `SubtitleEngineTest`, `SubtitleNormalizerTest`,
  `SystemMediaKeysTest`, `AnkiImportMapperTest`, `KjdDatabaseUpdaterTest`,
  `KjdPatchFeedParserTest`
- **KJD** — `ParsersTest`, `JapaneseNormalizerTest`, `EntityResolutionTest`,
  `FtsSearchTest`, `SchemaMigrationTest`, `DatabaseDiffTest`, `DatabasePatcherTest`,
  `AttributionWriterTest`, `SafeArchiveExtractorTest`, `EndToEndPipelineTest`

### Integration tests
- KJD `EndToEndPipelineTest` — full source → canonical → database pipeline.
- `ImportPipelineTest` / `AnkiPackageJvmTest` — import/export round trips across formats.
- JVM transfer tests exercise real SQLite through sqlite-jdbc.

### UI tests
- **Not yet established.** Compose UI tests (`createComposeRule` style) are not present.
  The `mediaGenerator` module has screenshot/recording-based visual tests
  (`ScreenshotTests`, `VideoTests`, `ComposableRecorderTest`) that capture composables to
  assets — a form of visual verification, not assertion-based UI testing.

### Database tests
- SQLDelight schemas are exercised indirectly through repository-level tests; migration
  correctness is covered in KJD (`SchemaMigrationTest`) and by the
  `UserDataDatabaseMigrationAfter*` Kotlin migrations (compiled, applied at runtime).

### Platform tests
- iOS targets cannot be tested on non-macOS hosts; iOS/Android platform actuals (file
  pickers, APKG, backup archives) are largely verified by build + manual testing. Several
  entries in `docs/planning/CURRENT_ISSUES.md` note "pending compile + runtime
  verification" for exactly this reason.

## Commands

```bash
# Shared core logic (JVM + common tests)
./gradlew :core:allTests

# Desktop suite tests
./gradlew :desktopApp:test

# Data platform tests
./gradlew :kjd:test

# Everything
./gradlew build
```

## Gaps & next steps (honest)

1. **UI tests** — none. Recommend adding Compose UI tests for the core study flow
   (practice screens, review) once the project adopts a UI-test harness.
2. **Android/iOS platform actuals** — file pickers, APKG import/export, backup archive
   handlers lack automated tests on-device.
3. **Sync engine** — no automated tests for the GitHub gist transport (network + OAuth).
4. **OCR engine** — untested (depends on Tesseract availability).
5. **Subtitle parsing fuzzing** — malformed SRT/ASS inputs are only partially covered.
6. **Regression suite** — consider CI wiring for `:desktopApp:test` and `:kjd:test`
   (CI currently builds artifacts; see `.github/workflows/`).

## Definition of done for changes

- Logic changes come with a unit test in the matching module's test source set.
- `./gradlew :core:allTests` (and the affected module's tests) pass.
- No new compiler warnings.
