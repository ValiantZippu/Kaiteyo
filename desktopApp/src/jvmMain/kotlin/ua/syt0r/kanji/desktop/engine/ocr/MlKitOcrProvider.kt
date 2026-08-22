package ua.syt0r.kanji.desktop.engine.ocr

import java.awt.image.BufferedImage

// ============================================
// KAITEYO ML KIT OCR STUB
// Placeholder for Google ML Kit text recognition.
// On Android, this would use
// com.google.mlkit.vision.text.TextRecognition.
// On desktop/iOS, the Tesseract backend handles OCR.
// ============================================

/**
 * ML Kit OCR provider — Android only.
 * On desktop, this is a no-op stub that returns
 * a clear message indicating ML Kit is unavailable.
 */
object MlKitOcrProvider : OcrProvider {

    override val name: String = "ML Kit (Google)"

    override val available: Boolean = runCatching {
        // On Android, check for ML Kit classes
        Class.forName("com.google.mlkit.vision.text.TextRecognition")
        true
    }.getOrDefault(false)

    override fun recognize(image: BufferedImage, language: String): List<OcrLine> {
        if (!available) {
            return listOf(
                OcrLine(
                    "ML Kit is not available on this platform. " +
                    "Use Tesseract for desktop OCR or ML Kit for Android.",
                    confidence = 0.0
                )
            )
        }

        // Future Android implementation would convert BufferedImage → InputImage
        // and run TextRecognition.getClient().process(inputImage)
        return listOf(OcrLine("ML Kit integration not yet implemented.", 0.0))
    }
}

/**
 * Composite OCR provider that tries ML Kit first, falls back to Tesseract.
 * Useful for Android builds where ML Kit is preferred.
 */
class CompositeOcrProvider(
    private val providers: List<OcrProvider> = listOf(MlKitOcrProvider, TesseractProvider)
) : OcrProvider {

    override val name: String = "Composite (ML Kit → Tesseract)"

    override val available: Boolean get() = providers.any { it.available }

    private val activeProvider: OcrProvider?
        get() = providers.firstOrNull { it.available }

    override fun recognize(image: BufferedImage, language: String): List<OcrLine> {
        return activeProvider?.recognize(image, language)
            ?: listOf(OcrLine("No OCR provider available.", 0.0))
    }
}
