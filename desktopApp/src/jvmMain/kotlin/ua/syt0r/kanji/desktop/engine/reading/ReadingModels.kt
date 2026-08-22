package ua.syt0r.kanji.desktop.engine.reading

import kotlinx.datetime.Instant
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

// ============================================
// KAITEYO READING ENGINE — MODELS
// A native reading workspace: local TXT /
// Markdown / HTML documents (EPUB planned),
// rendered with selectable-text dictionary
// lookup, bookmarks, highlights and history.
// Everything is serializable so the library
// persists to ~/.kaiteyo/reading/library.json.
// ============================================

/** Formats the reading engine can open today. */
@Serializable
enum class ReadingDocumentKind(val label: String) {
    Text("Plain text"),
    Markdown("Markdown"),
    Html("HTML"),
    Epub("EPUB")
}

/** A normalized block inside a document — the atom the reader renders. */
@Serializable
data class ReadingBlock(
    val index: Int,
    val kind: ReadingBlockKind,
    val text: String
)

@Serializable
enum class ReadingBlockKind {
    Heading,
    Paragraph,
    ListItem,
    Quote,
    Code,
    Divider,
    Empty
}

/**
 * A bookmark — a saved reading position, `MediaBookmark`-like but scoped to
 * a document with a block + char offset instead of a media timestamp.
 */
@Serializable
data class ReadingBookmark(
    val id: String,
    val label: String = "",
    val blockIndex: Int = 0,
    val charOffset: Int = 0,
    val createdAt: Instant = Clock.System.now()
)

/** A highlighted text span within one block. */
@Serializable
data class ReadingHighlight(
    val id: String,
    val blockIndex: Int,
    val start: Int,
    val end: Int,
    val text: String,
    val note: String = "",
    val color: String = "accent",
    val createdAt: Instant = Clock.System.now()
)

/** Where the reader currently is inside a document. */
@Serializable
data class ReadingPosition(
    val blockIndex: Int = 0,
    val charOffset: Int = 0
) {
    /** Fractional progress (0..1) derived from the last known block count. */
    fun progress(blockCount: Int): Float =
        if (blockCount <= 1) 0f else (blockIndex.toFloat() / (blockCount - 1)).coerceIn(0f, 1f)
}

/** One entry in the reading history (a completed/visited read session). */
@Serializable
data class ReadingHistoryEntry(
    val documentId: String,
    val title: String,
    val kind: ReadingDocumentKind,
    val openedAt: Instant,
    val durationMillis: Long = 0,
    val maxProgress: Float = 0f
)

/** A document in the reading library. */
@Serializable
data class ReadingDocument(
    val id: String,
    val title: String,
    val sourcePath: String,
    val kind: ReadingDocumentKind,
    val sizeBytes: Long = 0,
    val blockCount: Int = 0,
    val createdAt: Instant = Clock.System.now(),
    val lastOpenedAt: Instant = Clock.System.now(),
    var position: ReadingPosition = ReadingPosition(),
    val bookmarks: List<ReadingBookmark> = emptyList(),
    val highlights: List<ReadingHighlight> = emptyList()
) {
    /** Display progress derived from the current position. */
    val progress: Float get() = position.progress(blockCount)

    val bookmarkCount: Int get() = bookmarks.size
    val highlightCount: Int get() = highlights.size
}

/** The persisted library payload (documents + history in one file). */
@Serializable
data class ReadingLibraryPayload(
    val documents: List<ReadingDocument> = emptyList(),
    val history: List<ReadingHistoryEntry> = emptyList()
)

/** Result of opening a file that could not be parsed as a supported format. */
@Serializable
data class ReadingOpenResult(
    val document: ReadingDocument? = null,
    val error: String? = null
) {
    val isSuccess: Boolean get() = document != null
}
