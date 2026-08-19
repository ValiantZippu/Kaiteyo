package ua.syt0r.kanji.core.knowledge

import kotlinx.serialization.Serializable

// ============================================================
// MEDIA MODELS
// ------------------------------------------------------------
// Models for the media centre: documents, bookmarks, clips,
// and references. The media centre connects to the knowledge
// graph — every media item can link to kanji, words, and
// sentences.
// ============================================================

/**
 * Types of media documents.
 */
enum class MediaDocumentType(val label: String, val emoji: String) {
    Video("Video", "🎬"),
    Audio("Audio", "🎵"),
    Image("Image", "🖼️"),
    Pdf("PDF", "📄"),
    Text("Text", "📝"),
    Web("Web", "🌐"),
    Epub("EPUB", "📚"),
    Subtitle("Subtitle", "💬"),
    Unknown("Unknown", "❓")
}

/**
 * A media document in the media centre.
 */
@Serializable
data class MediaDocument(
    /** Unique identifier. */
    val id: String,
    /** Display name. */
    val name: String,
    /** File path or URL. */
    val path: String,
    /** Document type. */
    val type: MediaDocumentType,
    /** File size in bytes. */
    val sizeBytes: Long = 0,
    /** Duration in milliseconds (for audio/video). */
    val durationMs: Long? = null,
    /** When this document was added. */
    val addedAt: Long = System.currentTimeMillis(),
    /** When this document was last accessed. */
    val lastAccessedAt: Long = System.currentTimeMillis(),
    /** How many times accessed. */
    val accessCount: Int = 0,
    /** Language of the content. */
    val language: String = "ja",
    /** Tags associated with this document. */
    val tags: Set<String> = emptySet(),
    /** Whether this document is bookmarked. */
    val isBookmarked: Boolean = false,
    /** Custom notes. */
    val notes: String? = null,
    /** Thumbnail path (if available). */
    val thumbnailPath: String? = null
)

/**
 * A media bookmark — a saved position in a media document.
 */
@Serializable
data class MediaBookmark(
    /** Unique identifier. */
    val id: String,
    /** ID of the media document. */
    val documentId: String,
    /** Display name for this bookmark. */
    val name: String,
    /** Position in milliseconds (for audio/video). */
    val positionMs: Long = 0,
    /** Page offset (for PDFs/text). */
    val pageOffset: Int = 0,
    /** Text associated with this bookmark. */
    val text: String? = null,
    /** Screenshot path (if captured). */
    val screenshotPath: String? = null,
    /** Color tag. */
    val colorTag: String? = null,
    /** When created. */
    val createdAt: Long = System.currentTimeMillis(),
    /** User notes. */
    val notes: String? = null
)

/**
 * A subtitle clip — a segment of subtitle text from a media document.
 */
@Serializable
data class SubtitleClip(
    /** Unique identifier. */
    val id: String,
    /** ID of the media document. */
    val documentId: String,
    /** Start time in milliseconds. */
    val startMs: Long,
    /** End time in milliseconds. */
    val endMs: Long,
    /** The subtitle text (Japanese). */
    val text: String,
    /** Translation (if available). */
    val translation: String? = null,
    /** Index in the subtitle file. */
    val index: Int = 0,
    /** Whether this clip has been mined (card created). */
    val isMined: Boolean = false,
    /** Words mined from this clip. */
    val minedWords: Set<String> = emptySet()
)

/**
 * An audio clip — a segment of audio from a media document.
 */
@Serializable
data class AudioClip(
    /** Unique identifier. */
    val id: String,
    /** ID of the media document. */
    val documentId: String,
    /** Start time in milliseconds. */
    val startMs: Long,
    /** End time in milliseconds. */
    val endMs: Long,
    /** Display label. */
    val label: String? = null,
    /** When created. */
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * A reading bookmark — a saved position in a reading document.
 */
@Serializable
data class ReadingBookmark(
    /** Unique identifier. */
    val id: String,
    /** Document path. */
    val documentPath: String,
    /** Display name. */
    val name: String,
    /** Page/offset position. */
    val pageOffset: Int = 0,
    /** Scroll position (for web/text). */
    val scrollPosition: Float = 0f,
    /** Selected text (if any). */
    val selectedText: String? = null,
    /** Highlighted text ranges. */
    val highlights: List<ReadingHighlight> = emptyList(),
    /** When created. */
    val createdAt: Long = System.currentTimeMillis(),
    /** User notes. */
    val notes: String? = null,
    /** Color tag. */
    val colorTag: String? = null
)

/**
 * A highlighted text range in a reading document.
 */
@Serializable
data class ReadingHighlight(
    /** Start offset in the document. */
    val startOffset: Int,
    /** End offset (exclusive). */
    val endOffset: Int,
    /** Highlight color. */
    val color: String = "#FFEB3B",
    /** Note attached to this highlight. */
    val note: String? = null,
    /** When created. */
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * An OCR result — text recognized from an image.
 */
@Serializable
data class OcrResult(
    /** Unique identifier. */
    val id: String,
    /** The recognized text. */
    val text: String,
    /** Confidence score (0.0–1.0). */
    val confidence: Float,
    /** Source image path. */
    val imagePath: String? = null,
    /** Bounding boxes of recognized text regions. */
    val regions: List<OcrRegion> = emptyList(),
    /** When recognized. */
    val createdAt: Long = System.currentTimeMillis(),
    /** Whether this result has been looked up in the dictionary. */
    val isLookedUp: Boolean = false,
    /** Whether a card was created from this result. */
    val isMined: Boolean = false,
    /** Language detected. */
    val language: String = "ja"
)

/**
 * A bounding box region in an OCR result.
 */
@Serializable
data class OcrRegion(
    /** Left x-coordinate (normalized 0-1). */
    val left: Float,
    /** Top y-coordinate (normalized 0-1). */
    val top: Float,
    /** Right x-coordinate (normalized 0-1). */
    val right: Float,
    /** Bottom y-coordinate (normalized 0-1). */
    val bottom: Float,
    /** The recognized text in this region. */
    val text: String,
    /** Confidence for this specific region. */
    val confidence: Float
)

/**
 * A media reference — a link from a knowledge entity to media.
 */
@Serializable
data class MediaReference(
    /** The knowledge entity this reference points to. */
    val entityId: String,
    /** Type of the knowledge entity. */
    val entityType: GraphNodeType,
    /** The media document. */
    val documentId: String,
    /** Type of reference. */
    val referenceType: MediaReferenceType,
    /** Timestamp/position in the media (if applicable). */
    val positionMs: Long? = null,
    /** Text excerpt from the media. */
    val excerpt: String? = null,
    /** When this reference was created. */
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Types of media references.
 */
enum class MediaReferenceType(val label: String) {
    Subtitle("Subtitle"),
    Dialogue("Dialogue"),
    Text("Text"),
    Image("Image"),
    Audio("Audio"),
    Example("Example"),
    Usage("Usage")
}
