package ua.syt0r.kanji.desktop.engine.reading

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.datetime.Clock
import ua.syt0r.kanji.desktop.engine.dictionary.DictionaryMatch
import ua.syt0r.kanji.desktop.engine.dictionary.DictionaryRepository
import ua.syt0r.kanji.desktop.engine.dictionary.JapaneseSegmenter
import ua.syt0r.kanji.desktop.engine.dictionary.SearchMode
import ua.syt0r.kanji.desktop.engine.mining.MiningPayload
import java.io.File
import kotlin.random.Random

// ============================================
// KAITEYO READING ENGINE
// State machine for the native reading
// workspace. Owns the open documents, the
// active document, position/progress tracking,
// bookmarks, highlights and the reading
// history. Persistence lives in ReadingLibrary;
// dictionary lookup delegates to the shared
// DictionaryRepository (via JapaneseSegmenter
// for tokenization-aware word clicks).
// ============================================

class ReadingEngine(
    private val dictionaryRepository: DictionaryRepository? = null
) {

    // ------------------------------------------------------------
    // State
    // ------------------------------------------------------------

    /** Documents opened in this session, most-recently-active first. */
    val documents = mutableStateListOf<ReadingDocument>()

    /** The document currently shown in the reader (null = library view). */
    var activeDocumentId by mutableStateOf<String?>(null)

    /** Reading history — every session start, newest first. */
    val history = mutableStateListOf<ReadingHistoryEntry>()

    /** True while a file is being parsed/loaded. */
    var isLoading by mutableStateOf(false)

    var lastError by mutableStateOf<String?>(null)

    /** The active document resolved from [activeDocumentId]. */
    val activeDocument: ReadingDocument?
        get() = documents.firstOrNull { it.id == activeDocumentId }

    // ------------------------------------------------------------
    // Opening / closing
    // ------------------------------------------------------------

    /**
     * Open a file from disk: parse it, register it in the library and make
     * it active. Returns the document on success (null + [lastError] on
     * failure).
     */
    fun openFile(file: File): ReadingDocument? {
        if (!file.exists() || !file.isFile) {
            lastError = "File not found: ${file.name}"
            return null
        }
        isLoading = true
        val result = runCatching { ReadingParsers.parse(file, file.readText()) }
            .getOrElse { ReadingOpenResult(error = it.message ?: "Failed to read file") }
        isLoading = false
        if (!result.isSuccess) {
            lastError = result.error
            return null
        }
        return registerDocument(result.document!!, openedNow = true)
    }

    /** Open a parsed document directly (library restore / clipboard import). */
    fun registerDocument(document: ReadingDocument, openedNow: Boolean): ReadingDocument {
        val existing = documents.firstOrNull { it.id == document.id }
        val merged = existing
            ?.copy(
                blockCount = document.blockCount,
                sizeBytes = document.sizeBytes,
                kind = document.kind,
                lastOpenedAt = Clock.System.now()
            )
            ?: document.copy(lastOpenedAt = Clock.System.now())

        if (existing == null) {
            documents.add(0, merged)
        } else {
            val idx = documents.indexOfFirst { it.id == document.id }
            documents[idx] = merged
        }

        // Restore the most recently read position when re-opening.
        if (existing != null && openedNow) {
            merged.position = existing.position
        }

        activeDocumentId = merged.id
        lastError = null

        if (openedNow) {
            recordHistory(merged)
        }
        return merged
    }

    /** Close the active document (back to the library view). */
    fun closeDocument() {
        activeDocumentId = null
    }

    /** Remove a document from the library entirely (and its history). */
    fun removeDocument(id: String) {
        documents.removeAll { it.id == id }
        history.removeAll { it.documentId == id }
        if (activeDocumentId == id) activeDocumentId = null
    }

    /** Re-parse a document from its source path (file changed on disk). */
    fun reloadDocument(id: String) {
        val doc = documents.firstOrNull { it.id == id } ?: return
        val file = File(doc.sourcePath)
        if (!file.exists()) {
            lastError = "Source file is missing: ${file.name}"
            return
        }
        val result = runCatching { ReadingParsers.parse(file, file.readText()) }
            .getOrElse { ReadingOpenResult(error = it.message ?: "Reload failed") }
        if (!result.isSuccess) {
            lastError = result.error
            return
        }
        val idx = documents.indexOfFirst { it.id == id }
        documents[idx] = documents[idx].copy(
            blockCount = result.document!!.blockCount,
            sizeBytes = file.length()
        )
    }

    // ------------------------------------------------------------
    // Position & progress
    // ------------------------------------------------------------

    /** Update the active document's position (block + char offset). */
    fun setPosition(blockIndex: Int, charOffset: Int = 0) {
        val doc = activeDocument ?: return
        val idx = documents.indexOfFirst { it.id == doc.id }
        if (idx < 0) return
        documents[idx] = documents[idx].copy(
            position = ReadingPosition(blockIndex.coerceAtLeast(0), charOffset.coerceAtLeast(0))
        )
    }

    fun setPositionByFraction(fraction: Float) {
        val doc = activeDocument ?: return
        val target = (fraction.coerceIn(0f, 1f) * (doc.blockCount - 1).coerceAtLeast(0)).toInt()
        setPosition(target)
    }

    // ------------------------------------------------------------
    // Bookmarks
    // ------------------------------------------------------------

    fun toggleBookmark(blockIndex: Int, charOffset: Int = 0, label: String = "") {
        val doc = activeDocument ?: return
        val idx = documents.indexOfFirst { it.id == doc.id }
        if (idx < 0) return
        val existing = documents[idx].bookmarks.firstOrNull {
            it.blockIndex == blockIndex && it.charOffset == charOffset
        }
        documents[idx] = if (existing != null) {
            documents[idx].copy(bookmarks = documents[idx].bookmarks - existing)
        } else {
            documents[idx].copy(
                bookmarks = documents[idx].bookmarks + ReadingBookmark(
                    id = "rb-${Clock.System.now().toEpochMilliseconds()}-${Random.nextInt(9999)}",
                    label = label,
                    blockIndex = blockIndex,
                    charOffset = charOffset
                )
            )
        }
    }

    fun removeBookmark(id: String) {
        val doc = activeDocument ?: return
        val idx = documents.indexOfFirst { it.id == doc.id }
        if (idx < 0) return
        documents[idx] = documents[idx].copy(bookmarks = documents[idx].bookmarks.filterNot { it.id == id })
    }

    // ------------------------------------------------------------
    // Highlights
    // ------------------------------------------------------------

    fun toggleHighlight(blockIndex: Int, start: Int, end: Int, text: String) {
        if (end <= start) return
        val doc = activeDocument ?: return
        val idx = documents.indexOfFirst { it.id == doc.id }
        if (idx < 0) return
        val existing = documents[idx].highlights.firstOrNull {
            it.blockIndex == blockIndex && it.start == start && it.end == end
        }
        documents[idx] = if (existing != null) {
            documents[idx].copy(highlights = documents[idx].highlights - existing)
        } else {
            documents[idx].copy(
                highlights = documents[idx].highlights + ReadingHighlight(
                    id = "rh-${Clock.System.now().toEpochMilliseconds()}-${Random.nextInt(9999)}",
                    blockIndex = blockIndex,
                    start = start,
                    end = end,
                    text = text
                )
            )
        }
    }

    fun removeHighlight(id: String) {
        val doc = activeDocument ?: return
        val idx = documents.indexOfFirst { it.id == doc.id }
        if (idx < 0) return
        documents[idx] = documents[idx].copy(highlights = documents[idx].highlights.filterNot { it.id == id })
    }

    fun highlightsInBlock(blockIndex: Int): List<ReadingHighlight> =
        activeDocument?.highlights?.filter { it.blockIndex == blockIndex } ?: emptyList()

    // ------------------------------------------------------------
    // History
    // ------------------------------------------------------------

    private fun recordHistory(doc: ReadingDocument) {
        val entry = ReadingHistoryEntry(
            documentId = doc.id,
            title = doc.title,
            kind = doc.kind,
            openedAt = Clock.System.now(),
            maxProgress = doc.progress
        )
        // Keep one entry per document — the newest read bumps to the top.
        history.removeAll { it.documentId == doc.id }
        history.add(0, entry)
        while (history.size > 200) history.removeAt(history.lastIndex)
    }

    /** Update the latest history entry's progress as the user reads. */
    fun updateHistoryProgress() {
        val doc = activeDocument ?: return
        val idx = history.indexOfFirst { it.documentId == doc.id }
        if (idx < 0) return
        history[idx] = history[idx].copy(maxProgress = maxOf(history[idx].maxProgress, doc.progress))
    }

    // ------------------------------------------------------------
    // Dictionary bridge
    // ------------------------------------------------------------

    /**
     * Best dictionary match for a clicked word surface. Uses the shared
     * segmenter so inflected forms resolve through deinflection.
     */
    fun lookup(surface: String): DictionaryMatch? {
        val repo = dictionaryRepository ?: return null
        if (surface.isBlank()) return null
        return JapaneseSegmenter.bestMatch(surface, repo)
    }

    /** Grouped lookup for a query (used by the reader's search box). */
    fun lookupGrouped(query: String): List<ua.syt0r.kanji.desktop.engine.dictionary.DictionaryResultGroup> {
        val repo = dictionaryRepository ?: return emptyList()
        if (query.isBlank()) return emptyList()
        return repo.lookupGrouped(query, SearchMode.All)
    }

    // ------------------------------------------------------------
    // Mining bridge
    // ------------------------------------------------------------

    /**
     * Build a MiningPayload for a dictionary match inside a reading context:
     * the clicked word + the sentence it appeared in, tagged `source=reader`
     * with a pointer back to the document.
     */
    fun buildMiningPayload(match: DictionaryMatch, sentence: String): MiningPayload {
        val entry = match.entry
        return MiningPayload(
            headword = entry.headword,
            reading = entry.readings.firstOrNull()?.reading.orEmpty(),
            definition = entry.senses.joinToString("\n") { s -> s.glosses.joinToString("; ") },
            sentence = sentence,
            source = "reader",
            sourceDetail = activeDocument?.title.orEmpty(),
            tags = buildList {
                add("source:reader")
                match.dictionary.name.takeIf { it.isNotBlank() }?.let { add("dict:$it") }
                entry.senses.firstOrNull()?.partOfSpeech?.firstOrNull()?.let { add("pos:$it") }
            },
            example = entry.senses.firstOrNull()?.primaryGloss.orEmpty()
        )
    }

    // ------------------------------------------------------------
    // Import from clipboard (raw text becomes a document)
    // ------------------------------------------------------------

    fun importClipboard(title: String, content: String): ReadingDocument? {
        val result = ReadingParsers.parseText(title, content)
        if (!result.isSuccess) {
            lastError = result.error
            return null
        }
        return registerDocument(result.document!!, openedNow = true)
    }
}
