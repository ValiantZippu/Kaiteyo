# Kaiteyo — OCR System

> **Status**: `PARTIALLY_IMPLEMENTED` (Tess4J desktop path works) + `ARCHITECTED` (provider abstraction, caching, GPU, manga/PDF flows).
> Companion: `reading.md` (§6 manga), `dictionary.md`, `mining.md`, `browser.md`.

## 1. What it is

A centralized `OcrService` that converts **image regions** (manga page, PDF page image, screenshot, clipboard image, browser image, photo) into **selectable Japanese text regions** with confidence and bounding boxes, feeding `DictionaryService` and `MiningService`.

## 2. Why it exists

Scanned manga, scanned PDFs, screenshots, and web images have no selectable text. Without OCR, Yomitan and mining are unavailable on the richest immersion material. OCR is the bridge: `Image → text boxes → selectable spans → dictionary → mining`.

## 3. Engine options (evaluate before inventing)

| Engine | Platform | Language | Strength | Kaiteyo fit |
|--------|----------|----------|----------|-------------|
| **Tesseract** (tess4j 4.5.5) | Desktop JVM | JP (jpn, jpn_vert) | Mature, offline, vertical text | ✅ Primary desktop — already wired (`OcrEngine.kt`) |
| **ML Kit Text Recognition** | Android | JP | On-device, fast | ✅ Android stub (`MlKitOcrProvider.kt`) |
| **Apple Vision** | iOS | JP | On-device | Planned iOS provider |
| **Mokuro** (deep-learning manga OCR) | Cross | JP manga | Manga-optimized, high accuracy | Reference — `reference/mokuro/` |
| **DaKanji / kanji-heatmap** | — | — | Not OCR | Reference only |

Do not invent OCR from scratch. Reuse mature engines behind the `OcrProvider` abstraction.

## 4. Architecture

```
Image bytes (screenshot / manga page / PDF raster / clipboard / browser img)
  ↓ pre-process (grayscale, deskew, denoise, resize — engine-specific)
  → OcrProvider.detect(image, language="jpn")
  → OcrResult { regions: List<OcrTextRegion>, confidence: Float, engine: String }
  → cache (key = imageHash + engine + lang)
  → selectable TextRun[] overlay on original image
  → DictionaryService.lookup(region.text) on selection
  → MiningService.mine(MiningContext{ ocrRegion, screenshot, page, confidence })
```

Single `OcrService` — no `MangaOcr`/`PdfOcr` split.

## 5. Data model

```kotlin
data class OcrResult(
    val id: String,
    val imageHash: String,
    val engine: String, // "tesseract", "mlkit", "vision"
    val language: String, // "jpn"
    val regions: List<OcrTextRegion>,
    val createdAt: Instant,
)

data class OcrTextRegion(
    val text: String,
    val confidence: Float, // 0..1
    val boundingBox: Rect, // in image coordinates (0..imageSize)
    val language: String = "jpn",
    val readingOrder: Int, // sorted top→bottom, right→left for vertical
)

interface OcrProvider {
    val id: String
    suspend fun detect(imageBytes: ByteArray, language: String = "jpn"): OcrResult
    val requiresDownload: Boolean // true if language data not bundled
}

interface OcrService {
    suspend fun detect(imageBytes: ByteArray, language: String = "jpn"): Result<OcrResult>
    suspend fun detectRegion(imageBytes: ByteArray, rect: Rect): Result<OcrResult>
    fun cached(hash: String): OcrResult?
    fun providers(): List<OcrProvider>
}
```

## 6. Language models / segmentation / reading order

- Tesseract: `jpn` + `jpn_vert` traineddata (bundled or downloaded on first use; guided setup handles missing).
- Preprocessing: adaptive threshold, deskew (Hough), denoise — per-engine, never hand-rolled as primary.
- Segmentation: engine returns text boxes; service sorts into reading order (vertical: right→left columns, then top→bottom; horizontal: top→bottom, left→right). Confidence per region (not global).
- Vertical text: `jpn_vert` model; horizontal remains `jpn`.

## 7. Caching

- Disk: `~/.kaiteyo/ocr/{imageHash}.json` (OcrResult) + optional debug image copy.
- Memory: LRU (50 entries) for hot pages.
- Key: `sha256(imageBytes) + engine + lang` — same image never re-OCRed.
- Eviction: LRU on disk size cap (default 500MB, setting-controlled).

## 8. Manga Yomitan integration (shared path)

```
Manga page image
  → OcrService.detect → OcrTextRegion[] with boundingBox
  → Renderer overlays invisible selectable TextRuns at region rects
  → user drag-selects "食べる" across regions → concatenated query
  → DictionaryService.lookup → DictionaryPopup at selection rect
  → actions: Create card (with cropped screenshot + page/chapter), Copy, TTS
```

The popup is the same component as Media/Reading — no duplicate dictionary UI.

## 9. PDF scanned fallback

```
PDF page → try embedded text
  → if none → rasterize page at 200dpi → OcrService.detect → selectable spans
```

Surfaced as "Scanned page — OCR text (confidence X%)" in the UI; user can retry with higher DPI.

## 10. UI states

Loading (detecting), Empty (no text found — "No Japanese text detected, try a larger selection"), Error (engine missing — guided install prompt with download link; never a bare toast), Offline (irrelevant — OCR is offline), Low confidence warning, Retry.

## 11. Permissions / background / performance

- Permissions: screen capture (desktop, per-OS), file read, optional camera (future).
- Background: detect runs off UI thread (Dispatchers.IO / worker); progress callback for large images.
- Performance: downscale >4k images before detect; GPU acceleration where engine supports it (Tesseract CPU; future Manga OCR may use GPU — documented per provider). Batch: manga chapter pre-OCR is opt-in, cancellable, shows progress.

## 12. Offline

Fully offline once language data is installed. Missing data → guided setup (download `jpn.traineddata` to `~/.kaiteyo/tessdata/`), not a crash. `OcrService.providers().requiresDownload` drives the prompt.

## 13. Failure states

Missing engine / missing language data (guided setup), corrupt image, timeout (10s default), low confidence (surface but allow mining with warning), image too small, permission denied (screen capture not granted).

## 14. Evolution

New engine → implement `OcrProvider`, register via Koin, appear in Settings → OCR (priority order, per-source override). No `OcrService` rewrite.
