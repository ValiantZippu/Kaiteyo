# Kaiteyo — Reading System

> **Status**: `IMPLEMENTED` (suite, TXT/Markdown/HTML) + `ARCHITECTED` (EPUB/PDF/manga/web adapters).
> Companion: `core.md`, `data-model.md`, `dictionary.md`, `ocr.md`, `mining.md`, `browser.md`.

## 1. What it is

The major reading subsystem for **manga, web manga, novels, light novels, books, ebooks (EPUB/HTML/TXT), PDFs, web pages, news** — where every piece of Japanese text is selectable → `DictionaryPopup` → `MiningEngine` → Library/Stats/Deck.

Reading is not a placeholder markdown viewer.

## 2. Architecture

```
Source file / URL / CBZ
  → Parser (per format)
  → Normalized document { chapters[], pages[], images[], textRuns[] }
  → ReadingSession { documentId, position, pagination, theme, progress }
  → Renderer (text layout / image pager / PDF canvas)
  → Selection (text + rect)
  → DictionaryService → MiningService
  ↓
  Library / Activity / Stats (via events)
```

Parsers are isolated adapters; the Renderer and Session never know the source format.

## 3. Supported formats & parsers

| Format | Parser (current/target) | Library |
|--------|------------------------|---------|
| TXT | `ReadingParsers` (suite, implemented) | stdlib |
| Markdown | `ReadingParsers` (suite) | commonmark |
| HTML | `ReadingEngine` (suite) | jsoup |
| EPUB | `EpubReader` (suite, planned seam) | epublib 4.0 (catalog) |
| PDF | `PDF subsystem` (target) — see §7 | PDF.js / PDFBox (eval) |
| CBZ/CBR (manga) | `Manga adapter` (target) — see §6 | zip4j + rar handling |
| Web page | `Web reading adapter` (target) via `BrowserBridge` | — |

Every parser outputs:

```kotlin
data class ReadingDocument(
    val id: String,
    val contentId: ContentId,
    val title: String,
    val chapters: List<ReadingChapter>,
    val coverBytes: ByteArray?,
    val metadata: DocumentMetadata, // author, publisher, language, toc
)

data class ReadingChapter(
    val id: String,
    val title: String,
    val pages: List<ReadingPage>,
)

sealed interface ReadingPage {
    data class Text(val runs: List<TextRun>, val pageIndex: Int) : ReadingPage
    data class Image(val bytes: ByteArray, val rect: Rect, val ocrResultId: String?) : ReadingPage
    data class Pdf(val pageIndex: Int, val selectable: Boolean) : ReadingPage
}

data class TextRun(val text: String, val reading: String?, val rect: Rect, val selectable: Boolean)
```

## 4. Reading session

```kotlin
data class ReadingSession(
    val documentId: String,
    val contentId: ContentId,
    val position: ReadingPosition, // chapterId + pageIndex + offset
    val pagination: Pagination,    // flowing vs paginated, fontScale, theme
    val theme: ReadingTheme,       // light/dark/sepia, font, lineHeight
    val bookmarks: List<ReadingBookmark>,
    val highlights: List<Highlight>,
    val progress: ContentProgress, // via ContentService (percent, last page)
)

data class ReadingPosition(val chapterId: String, val pageIndex: Int, val charOffset: Int)
```

Session is ephemeral; bookmarks/highlights/progress persist via `ReadingService` → `ActivityService` + `ContentService`.

## 5. Text interaction

- All rendered Japanese text is **selectable** with a platform-native selection handle.
- On selection, `DictionaryService.lookup(selectedText)` is called; `DictionaryPopup` appears at the selection rect (same popup as Media/RG).
- Actions: Copy, Lookup, Mine sentence/context (with screenshot of the page region), Add bookmark/highlight/note.
- Furigana: rendered inline if present in EPUB/HTML metadata; toggleable per `ReadingTheme`.

## 6. Manga

### What it is

Local manga (`CBZ/CBR` or folder of images), web manga (via `BrowserBridge`), image pages with:

- Chapter/volume structure, page progression, bookmarks, reading progress.
- OCR-driven text selection (see `ocr.md`).
- Yomitan lookup on OCR-derived selectable text.
- Mining with page/chapter/URL context.

### OCR flow (manga & scanned PDF)

```
Image page
  → OcrService.detect(imageBytes) → OcrResult { regions[] }
  → regions → selectable TextRun[] with rects
  → renderer overlays invisible selectable spans at region rects
  → user selects word → DictionaryService → MiningService
  → mined card carries Screenshot (cropped region) + page/chapter + ocrConfidence
```

- `OcrTextRegion { text, confidence, boundingBox }` retains coordinates so the UI can highlight the source region.
- Cached per page image hash; GPU/CPU requirements documented in `ocr.md`.

### Manga Yomitan flow

Open manga → OCR Japanese text → select word → dictionary popup → inspect definition → mine sentence/context (with image) → save to Kaiteyo/Anki → card appears in Review/Library.

## 7. Light novels / novels / ebooks (EPUB/HTML/TXT)

- **Parser**: EPUB via `epublib` → XHTML per chapter → `TextRun[]` with metadata (author/title/toc).
- **Features**: chapters, pagination (flowing or paginated), fonts/themes, text selection, furigana, Yomitan lookup, sentence extraction, mining, progress/bookmarks/highlights/notes, search within document, reading history.
- **ReadingSession** is shared across all ebook kinds — no `EpubSession` vs `TxtSession` split.

## 8. PDF reader

- **Rendering**: page raster or vector via PDF library; page navigation, zoom, text selection.
- **Text selection**: if PDF contains embedded text → direct `TextRun[]`; if scanned → OCR fallback (page image → `OcrService` → selectable spans, same as manga).
- **Features**: annotations (future), bookmarks, progress, search, highlights, mining — all via `ReadingService`.
- **Fallback**:

```
PDF page
  → try embedded text extraction
  → if empty/untagged → render page to image → OcrService → selectable spans
  → DictionaryService → MiningService (identical path)
```

Never assume every PDF contains selectable text.

## 9. Web reading

Adapters separate **Web Source** from **Reading Session**:

- News/article/web-novel/manga-website adapters fetch + clean (readability) → `ReadingDocument`.
- Selection/mining identical to local reading.
- Source metadata (URL, site, publish date) preserved in `SourceBinding`.

## 10. Shared dictionary (no duplication)

There is exactly one `DictionaryService`. All of:

- selected subtitle text
- selected manga OCR text
- ebook text
- PDF text (embedded or OCR-derived)
- web text
- game text

call the same `DictionaryService.lookup`. Never `MediaDictionary`/`ReadingDictionary`/`OcrDictionary`.

## 11. Persistence / caching / sync / offline

- Documents: file on disk + `content` row + `reading_progress/bookmark/highlight` tables.
- Highlights/bookmarks: persisted, searchable, sync-able (via `SyncService`).
- OCR results: cached per image hash (`~/.kaiteyo/ocr/`), LRU evictable.
- Pagination state, theme, font: `SettingsService`.
- Offline: all local documents fully offline; web documents require cached copy ("Save for offline").

## 12. UI states

Loading (parse), Empty (no documents), Error (malformed EPUB/PDF, unsupported format), Offline (web source unavailable), Permission denied (file), Retry/cancellation, Partial failure (e.g., EPUB toc missing — still render).

## 13. Performance

Large manga (1000+ pages): paginated, lazy image decode, memory-capped cache. Large PDF: page-raster on demand, not all at once. Long ebook: virtualized chapter list, incremental pagination.

## 14. Evolution

New format → new parser adapter returning `ReadingDocument`. New theme → `ReadingTheme` variant. No `ReadingService` rewrite.
