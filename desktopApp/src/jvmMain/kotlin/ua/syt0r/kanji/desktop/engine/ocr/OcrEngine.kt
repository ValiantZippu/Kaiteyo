package ua.syt0r.kanji.desktop.engine.ocr

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.awt.Rectangle
import java.awt.Robot
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO

// ============================================
// KAITEYO OCR SYSTEM
// Built-in OCR for images, screenshots, clipboard
// images and selected screen regions. Detection is
// delegated to a pluggable engine (Tesseract via
// Tess4J when it is on the classpath, with a
// graceful hint otherwise). The pipeline feeds
// extracted Japanese text straight into the
// dictionary -> mining workflow.
// ============================================

/** A region of the desktop to capture for OCR. */
data class CaptureRegion(val x: Int, val y: Int, val width: Int, val height: Int) {
    fun toRect(): Rectangle = Rectangle(x, y, width, height)
}

/** Tokenised OCR line result. */
data class OcrLine(val text: String, val confidence: Double = 0.0)

data class OcrResult(
    val lines: List<OcrLine>,
    val language: String = "jpn",
    val imagePath: String? = null
) {
    val text: String get() = lines.joinToString("\n") { it.text }
}

/** Interface any OCR backend must satisfy. */
interface OcrProvider {
    val name: String
    val available: Boolean
    fun recognize(image: BufferedImage, language: String): List<OcrLine>
}

/**
 * Screen-capture + OCR coordinator. Uses [ocrProvider] for text
 * recognition and AWT Robot for screen/region capture. If the
 * provider is unavailable, capture still works and the UI shows
 * a clear hint about installing an OCR backend.
 */
class OcrEngine(
    val ocrProvider: OcrProvider = TesseractProvider
) {

    var lastResult by mutableStateOf<OcrResult?>(null)
    var busy by mutableStateOf(false)
    var selectedRegion by mutableStateOf<CaptureRegion?>(null)

    val available: Boolean get() = ocrProvider.available

    // ------------------------------------------------------------
    // Sources
    // ------------------------------------------------------------

    fun ocrImage(file: File, language: String = "jpn"): OcrResult {
        val image = ImageIO.read(file) ?: return OcrResult(emptyList(), language, file.absolutePath)
        return recognize(image, language, file.absolutePath)
    }

    fun ocrClipboard(language: String = "jpn"): OcrResult {
        val image = clipboardImage() ?: return OcrResult(emptyList(), language)
        val tmp = File.createTempFile("kaiteyo-ocr", ".png")
        ImageIO.write(image, "png", tmp)
        val result = recognize(image, language, tmp.absolutePath)
        tmp.deleteOnExit()
        return result
    }

    fun ocrScreen(region: CaptureRegion, language: String = "jpn"): OcrResult {
        val image = captureRegion(region)
        val tmp = File.createTempFile("kaiteyo-ocr", ".png")
        ImageIO.write(image, "png", tmp)
        return recognize(image, language, tmp.absolutePath)
    }

    fun ocrFullScreen(language: String = "jpn"): OcrResult {
        val bounds = Toolkit.getDefaultToolkit().screenSize
        return ocrScreen(CaptureRegion(0, 0, bounds.width, bounds.height), language)
    }

    /** Capture an arbitrary region of the screen. */
    fun captureRegion(region: CaptureRegion): BufferedImage {
        selectedRegion = region
        return Robot().createScreenCapture(region.toRect())
    }

    /** Grab the current clipboard image, if any. */
    fun clipboardImage(): BufferedImage? {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        return runCatching {
            val data = clipboard.getData(DataFlavor.imageFlavor) as? BufferedImage
            data
        }.getOrNull()
    }

    // ------------------------------------------------------------
    // Recognition
    // ------------------------------------------------------------

    private fun recognize(image: BufferedImage, language: String, imagePath: String?): OcrResult {
        busy = true
        return try {
            val lines = if (!ocrProvider.available) {
                listOf(OcrLine("OCR backend unavailable — add Tesseract (Tess4J) to enable recognition.", 0.0))
            } else {
                ocrProvider.recognize(image, language)
            }
            OcrResult(lines, language, imagePath).also { lastResult = it }
        } finally {
            busy = false
        }
    }
}

/** Default provider. Uses Tesseract when Tess4J is on the classpath. */
object TesseractProvider : OcrProvider {

    override val name: String = "Tesseract (Tess4J)"

    override val available: Boolean = runCatching {
        Class.forName("net.sourceforge.tess4j.Tesseract")
        true
    }.getOrDefault(false)

    override fun recognize(image: BufferedImage, language: String): List<OcrLine> {
        if (!available) return emptyList()
        return runCatching {
            val tessClass = Class.forName("net.sourceforge.tess4j.Tesseract")
            val instance = tessClass.getDeclaredConstructor().newInstance()
            tessClass.getMethod("setLanguage", String::class.java).invoke(instance, language)
            val out = tessClass.getMethod("doOCR", BufferedImage::class.java).invoke(instance, image)
            (out?.toString() ?: "")
                .split("\n")
                .map { OcrLine(it.trim()) }
                .filter { it.text.isNotBlank() }
        }.getOrDefault(emptyList())
    }
}