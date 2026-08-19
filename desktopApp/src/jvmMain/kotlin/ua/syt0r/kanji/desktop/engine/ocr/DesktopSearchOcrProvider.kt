package ua.syt0r.kanji.desktop.engine.ocr

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ua.syt0r.kanji.core.knowledge.SearchOcrProvider

/**
 * Desktop OCR backend for the universal search "Scan image" input mode
 * (spec §16). Wraps the suite's [OcrEngine] (Tesseract) and recognizes
 * Japanese text from the system clipboard image. Honest contract:
 * returns null when the backend is unavailable, the clipboard holds no
 * image, or no text was recognized — the overlay then shows the failure
 * hint instead of fabricating results.
 */
class DesktopSearchOcrProvider(
    private val engine: OcrEngine = OcrEngine()
) : SearchOcrProvider {

    override suspend fun ocrClipboardImage(): String? = withContext(Dispatchers.IO) {
        if (!engine.available) return@withContext null
        runCatching {
            val result = engine.ocrClipboard(language = "jpn")
            result.lines.map { it.text.trim() }
                .filter { it.isNotEmpty() }
                .joinToString("\n")
                .ifBlank { null }
        }.getOrNull()
    }
}
