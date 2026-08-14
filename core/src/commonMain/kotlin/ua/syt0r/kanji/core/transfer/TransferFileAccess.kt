package ua.syt0r.kanji.core.transfer

// ============================================
// PLATFORM FILE ACCESS
// Minimal bridge between the common import/export
// UI and platform file dialogs. Implementations
// are suspend so platforms with async pickers
// (e.g. the iOS document picker) can await the
// user's choice without blocking the UI thread.
// Platforms without a usable dialog return
// null/false so the UI can explain the limitation
// instead of doing nothing.
// ============================================

/** Let the user pick a file and return its raw bytes (null = canceled/unsupported). */
expect suspend fun pickImportFile(description: String, vararg extensions: String): ByteArray?

/** Let the user choose where to save [bytes]; returns true when written. */
expect suspend fun saveExportFile(bytes: ByteArray, suggestedName: String, description: String, vararg extensions: String): Boolean

/**
 * Name of the last successfully picked import file, or null when the
 * platform does not remember one. Used to offer a picker-free re-import.
 */
expect suspend fun getLastImportFileName(): String?

/**
 * Re-reads the last successfully picked import file without opening a
 * picker; null when unavailable (or the platform does not support it).
 */
expect suspend fun readLastImportFile(): ByteArray?
