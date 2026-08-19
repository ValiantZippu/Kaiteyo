package ua.syt0r.kanji.desktop.engine.reading

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

// ============================================
// KAITEYO READING ENGINE — LIBRARY
// Persists the reading library (documents +
// history) to ~/.kaiteyo/reading/library.json.
// Load is idempotent and corruption-tolerant:
// a broken payload resets to an empty library
// instead of crashing the app.
// ============================================

class ReadingLibrary(
    private val engine: ReadingEngine
) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val libraryFile: File
        get() = File(
            System.getProperty("user.home"),
            ".kaiteyo/reading/library.json"
        )

    /** Restore documents + history from disk (safe on first run / corrupt file). */
    fun load() {
        if (!libraryFile.exists()) return
        val payload = runCatching {
            json.decodeFromString<ReadingLibraryPayload>(libraryFile.readText())
        }.getOrElse {
            // Corrupt payload: keep the file for diagnosis but start empty.
            return
        }
        engine.documents.clear()
        engine.documents.addAll(payload.documents)
        engine.history.clear()
        engine.history.addAll(payload.history)
    }

    /** Write the current engine state to disk. */
    fun save() {
        runCatching {
            libraryFile.parentFile?.mkdirs()
            libraryFile.writeText(
                json.encodeToString(
                    ReadingLibraryPayload(
                        documents = engine.documents.toList(),
                        history = engine.history.toList()
                    )
                )
            )
        }
    }

    /** Recent documents, most recently active first. */
    fun recentDocuments(limit: Int = 12): List<ReadingDocument> =
        engine.documents.sortedByDescending { it.lastOpenedAt }.take(limit)

    /** Recently read documents from the history, newest first. */
    fun recentHistory(limit: Int = 20): List<ReadingHistoryEntry> =
        engine.history.take(limit)
}
