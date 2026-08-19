package ua.syt0r.kanji.core.knowledge

// ============================================================
// SEARCH INPUT MODES — image OCR (spec §16, §18)
// ------------------------------------------------------------
// The universal search can accept text from sources other than
// the keyboard. Clipboard is handled by the UI directly through
// the Compose ClipboardManager (cross-platform); image OCR needs
// a real recognition backend, which only exists on desktop
// (Tesseract). This interface is the platform seam:
//
//   - the universal search overlay asks Koin for an optional
//     SearchOcrProvider; when none is registered (Android/iOS),
//     the "Scan image" control is simply not shown — no ghost UI
//   - desktop registers DesktopSearchOcrProvider in
//     desktopAppModule, backed by the suite's OcrEngine
//
// The provider is honest: it returns null when no image is on
// the clipboard, the OCR backend is unavailable, or no Japanese
// text was recognized.
// ============================================================

interface SearchOcrProvider {

    /**
     * Recognizes Japanese text from the image currently on the system
     * clipboard, or null when there is no image / no backend / no text.
     */
    suspend fun ocrClipboardImage(): String?
}
