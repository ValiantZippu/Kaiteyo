# Kaiteyo Architecture — Asset System Specification

**Status**: Implemented (managed runtime assets + branding); game/media asset pipelines
planned
**Owner**: `buildSrc/AppAssets.kt` + `mediaGenerator/` + `docs/branding/`
**Related**: `docs/branding/` · `docs/design/DESIGN_SYSTEM.md` · `docs/data/SOURCES.md` ·
`docs/architecture/toolchain.md`

## 1. Purpose

One managed place for every asset category (§226) with provenance, validation, and clear
separation of source vs generated vs exported files (§246, §346). Assets are declared in
code (a single registry), not dropped into folders by hand.

## 2. Runtime asset registry (`buildSrc/src/main/kotlin/AppAssets.kt`)

The declarative registry of every managed runtime asset — the **source of truth** the
Gradle prepare tasks read:

| Asset | Platform | Source URL (GitHub releases) |
|---|---|---|
| `kanji-dojo-data-base-v15.sql` | all (bundled app-data asset; `AppDataDatabaseVersion = 15`) | `Kanji-Dojo-Data/releases/download/v15.0/...` |
| `ja-JP-Neural2-B.opus` | Android | `Kanji-Dojo-Data/releases/download/voice-v1/...` |
| `ja-JP-Neural2-B.wav` | Desktop, iOS | `Kanji-Dojo-Data/releases/download/voice-v1/...` |
| `text_analysis_preview.json` | all (url = null — generated locally, not downloaded) | — |

**Management rule**: `core/src/<sourceSet>Main/composeResources/files/` is managed — the
prepare task **deletes any file not declared in `AppAssets.kt`**. Never drop files there
by hand; register them in `AppAssets.kt` (or they will be removed on the next build).
Missing assets are downloaded on first build — **needs network**.

## 3. Versioning

- Single app version source: `buildSrc/AppVersion.kt` (`versionCode`, `versionName`,
  `desktopAppVersion` — must be 3 numbers). Bump here only; `installer/common/version.json`
  mirrors it for the installer subsystem.
- **Distinct version domains** (§338): application version (AppVersion) · database
  schema version (`user_version` 16, AppDataDatabaseVersion 15) · content/data asset
  version (asset file names carry it) · API version (local API) · future Journey world
  version. Never conflate them.

## 4. mediaGenerator (JVM utility module)

`mediaGenerator/` uses javacv-platform 1.5.11 + coil to generate derived media assets
programmatically (image/media processing). Source assets stay in source locations;
generated outputs land in managed build output — generated files are never authoritative
source (§346).

## 5. Branding assets

`docs/branding/` is the dedicated home for logo, icon, wordmark, banner, splash,
background, default avatar, store images (§227). Replacement policy (§227):
1. Make a copy of the existing asset.
2. Preserve the original source.
3. Update references.
4. Validate dimensions + formats.
5. Never overwrite the only original.

Platform window/app icons are wired in `desktopApp/build.gradle.kts` (`windows_icon.ico`,
`mac_icon.icns`, `windowIcon.png`).

## 6. Icon system

One coherent icon system (§228): consistent stroke/fill logic, alignment, scaling, theme
support, accessibility labels. Do not generate ad-hoc SVGs per feature; icons come from
the shared design system (`docs/design/DESIGN_SYSTEM.md`).

## 7. Fonts

Japanese glyph coverage is a hard requirement (§283). Before bundling any font evaluate:
Japanese coverage, weights, rendering, fallback, licensing, file size. Bundled TTS voices
are audio assets (opus for Android, wav for desktop/iOS) — sound pipeline beyond TTS is
future.

## 8. Planned pipelines

- **Journey/3D assets** — `art/source|processed|exported` (§246); Blender source assets
  (models/UVs/rigging/animation/LOD) with exported LODs; exported assets are never the
  only source (`docs/architecture/journey.md` §5).
- **Content packages** — assets travel inside versioned content packages with manifest +
  license (§259–§260, `docs/architecture/content.md`).

## 9. Tests / verification

- CI verifies asset presence implicitly via the build (prepare tasks fail on undeclared
  files); `stage-artifacts.sh` checksums release artifacts.
- Gap: an asset inventory/registry doc beyond `AppAssets.kt` for non-runtime assets
  (branding, fonts, sounds).

## 10. Open items

- Asset inventory/registry for non-runtime assets.
- Sound/music pipeline (currently TTS voices only).
- Journey/3D asset pipeline (with the Journey phase, audit §5 phases 21+).

## 11. Journey & world assets (TARGET — NODE §92–§93, §145, STANDARDS §245–§246)

World content assets follow the package discipline (ADR-0015):

- **Source vs exported**: `art/source/` (blender, textures, concept, reference) is the
  permanent source of truth; `art/processed/` and `art/exported/` are generated —
  exported assets are never the only copy (§246).
- **Within packages**: assets travel inside versioned content packages with manifest,
  license, and hash (§145, §259–§260); the runtime validates hashes at install (§148).
- **Budgets per platform tier** (§143): texture/mesh/audio budgets documented per tier;
  LOD sets and compressed variants generated by the pipeline (STANDARDS §245 LOD
  generation).
- **Audio**: ambient/weather/NPC/vehicle/train/ocean beds are package assets; TTS
  pronunciation reuses the existing voice pipeline; dialogue voice is authored audio
  (AUDIO PRODUCTION).
- **Asset inventory**: the §9 gap (inventory beyond `AppAssets.kt`) extends to world
  packages — a manifest-driven inventory with checksums per package.
