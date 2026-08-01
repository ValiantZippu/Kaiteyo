package ua.syt0r.kanji.presentation.screen.main.screen.decks

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.time.Duration

// ============================================
// ENHANCED CARD DATA MODELS
// Tags, Flags, Notes, Card Status, History
// ============================================

/** 7 Anki-compatible flag colors */
enum class CardFlagType(val id: Int, val displayName: String, val hexColor: String) {
    None(0, "None", "#00000000"),
    Red(1, "Red", "#FFFF6B6B"),
    Orange(2, "Orange", "#FFFEAB57"),
    Yellow(3, "Yellow", "#FFFFD93D"),
    Green(4, "Green", "#FFC2FC8B"),
    Blue(5, "Blue", "#FF7BC8FF"),
    Purple(6, "Purple", "#FFA78BFA"),
    Gray(7, "Gray", "#FFB0B0B0");

    fun colorFromHex(): androidx.compose.ui.graphics.Color {
        return try {
            val hex = hexColor.removePrefix("#")
            val a = hex.substring(0..1).toInt(16)
            val r = hex.substring(2..3).toInt(16)
            val g = hex.substring(4..5).toInt(16)
            val b = hex.substring(6..7).toInt(16)
            androidx.compose.ui.graphics.Color(r, g, b, a)
        } catch (_: Exception) {
            androidx.compose.ui.graphics.Color.Gray
        }
    }

    companion object {
        fun fromId(id: Int): CardFlagType = entries.firstOrNull { it.id == id } ?: None
    }
}

/** Card status matching Anki's states */
enum class CardStatus(val id: Int, val displayName: String) {
    New(0, "New"),
    Learning(1, "Learning"),
    Young(2, "Young"),
    Mature(3, "Mature"),
    Relearning(4, "Relearning"),
    Suspended(5, "Suspended"),
    Buried(6, "Buried"),
    Archived(7, "Archived");

    companion object {
        fun fromId(id: Int): CardStatus = entries.firstOrNull { it.id == id } ?: New
    }
}

/** Tag with color and hierarchy support */
@Serializable
data class CardTag(
    val id: Long = 0,
    val name: String,
    val color: String = "#808080",
    val parentId: Long? = null,
    val createdAt: Instant = Clock.System.now(),
    val modifiedAt: Instant = Clock.System.now()
) {
    /** Returns nested tag path like "JLPT::N5::Kanji" */
    val path: String get() = name // Full path would be resolved from hierarchy

    /** Returns display color as a compose Color */
    fun getDisplayColor(): androidx.compose.ui.graphics.Color {
        return try {
            val hex = color.removePrefix("#")
            val a = hex.substring(0..1).toInt(16)
            val r = hex.substring(2..3).toInt(16)
            val g = hex.substring(4..5).toInt(16)
            val b = hex.substring(6..7).toInt(16)
            androidx.compose.ui.graphics.Color(r, g, b, a)
        } catch (_: Exception) {
            androidx.compose.ui.graphics.Color.Gray
        }
    }
}

/** Note content with format indicator */
@Serializable
data class CardNote(
    val cardKey: String = "",
    val practiceType: Long = 0,
    val content: String = "",
    val contentFormat: NoteFormat = NoteFormat.PlainText,
    val createdAt: Instant = Clock.System.now(),
    val modifiedAt: Instant = Clock.System.now()
)

enum class NoteFormat(val id: Int) {
    PlainText(0),
    Markdown(1),
    Html(2);

    companion object {
        fun fromId(id: Int): NoteFormat = entries.firstOrNull { it.id == id } ?: PlainText
    }
}

/** Enhanced card with all metadata for browser */
@Serializable
data class EnhancedCardData(
    val key: String,
    val practiceType: Long,
    val character: String = "",
    val reading: String = "",
    val meaning: String = "",
    val deckName: String = "",
    val deckId: Long = 0,
    val tags: List<CardTag> = emptyList(),
    val flag: CardFlagType = CardFlagType.None,
    val status: CardStatus = CardStatus.New,
    val note: CardNote? = null,
    val interval: Duration = Duration.ZERO,
    val ease: Float = 2.5f,
    val lapses: Int = 0,
    val reviews: Int = 0,
    val accuracy: Float = 0f,
    val totalTimeStudied: Duration = Duration.ZERO,
    val createdAt: Instant = Clock.System.now(),
    val modifiedAt: Instant = Clock.System.now(),
    val lastReview: Instant? = null,
    val nextReview: Instant? = null
)

/** History / audit log entry */
@Serializable
data class StudyHistoryEntry(
    val id: Long = 0,
    val actionType: StudyActionType,
    val cardKey: String? = null,
    val practiceType: Long? = null,
    val details: String = "",
    val timestamp: Instant = Clock.System.now()
)

enum class StudyActionType(val id: Int, val displayName: String) {
    Review(0, "Review"),
    Suspend(1, "Suspend"),
    Bury(2, "Bury"),
    Unbury(3, "Unbury"),
    Flag(4, "Flag"),
    Tag(5, "Tag"),
    Edit(6, "Edit"),
    Delete(7, "Delete"),
    Import(8, "Import"),
    Export(9, "Export"),
    Backup(10, "Backup"),
    Restore(11, "Restore"),
    Reschedule(12, "Reschedule"),
    Reposition(13, "Reposition"),
    Merge(14, "Merge"),
    BulkOperation(15, "Bulk Operation");

    companion object {
        fun fromId(id: Int): StudyActionType = entries.firstOrNull { it.id == id } ?: Review
    }
}

/** Keyboard shortcut definition */
@Serializable
data class KeyboardShortcut(
    val id: Long = 0,
    val actionId: String,
    val primaryKey: String,
    val modifierFlags: Int = 0,
    val isEnabled: Boolean = true,
    val profileName: String = "default"
) {
    fun getDisplayText(): String {
        val parts = mutableListOf<String>()
        if (modifierFlags and 1 != 0) parts.add("Ctrl")
        if (modifierFlags and 2 != 0) parts.add("Alt")
        if (modifierFlags and 4 != 0) parts.add("Shift")
        if (modifierFlags and 8 != 0) parts.add("Meta")
        parts.add(primaryKey.uppercase())
        return parts.joinToString(" + ")
    }
}

/** Backup metadata */
@Serializable
data class BackupMetadata(
    val id: Long = 0,
    val filename: String,
    val fileSize: Long = 0,
    val checksum: String = "",
    val isAutomatic: Boolean = false,
    val createdAt: Instant = Clock.System.now(),
    val notes: String = ""
)

/** Filtered deck / custom study session */
@Serializable
data class FilteredDeck(
    val id: Long = 0,
    val name: String,
    val searchQuery: String = "",
    val maxCards: Int = 9999,
    val isRescheduled: Boolean = true,
    val createdAt: Instant = Clock.System.now()
)

/** Plugin registry entry */
@Serializable
data class PluginEntry(
    val id: Long = 0,
    val pluginId: String,
    val name: String,
    val version: String = "1.0.0",
    val enabled: Boolean = true,
    val pluginType: String = "unknown",
    val configJson: String? = null,
    val installedAt: Instant = Clock.System.now(),
    val updatedAt: Instant = Clock.System.now()
)

/** Review settings configuration */
@Serializable
data class ReviewSettings(
    val showAgain: Boolean = true,
    val showHard: Boolean = true,
    val showGood: Boolean = true,
    val showEasy: Boolean = true,
    val buttonLayout: ButtonLayout = ButtonLayout.Classic,
    val buttonSize: ButtonSize = ButtonSize.Normal,
    val autoNext: Boolean = false,
    val showTimer: Boolean = true,
    val showProgress: Boolean = true
)

enum class ButtonLayout(val displayName: String) {
    Classic("Classic 4 Buttons"),
    Minimal("Minimal"),
    Vertical("Vertical"),
    Horizontal("Horizontal"),
    Compact("Compact"),
    Auto("Auto Layout")
}

enum class ButtonSize(val displayName: String) {
    Small("Small"),
    Normal("Normal"),
    Large("Large")
}

/** Bulk action type */
enum class BulkActionType(val displayName: String) {
    Tag("Tag"),
    Flag("Flag"),
    Delete("Delete"),
    Move("Move Deck"),
    Suspend("Suspend"),
    Bury("Bury"),
    Archive("Archive"),
    Export("Export"),
    Reschedule("Reschedule"),
    ChangeDeck("Change Deck")
}

/** Statistics data models */
@Serializable
data class DailyStats(
    val date: String = "",
    val reviews: Int = 0,
    val newCards: Int = 0,
    val reviewCards: Int = 0,
    val timeStudied: Duration = Duration.ZERO,
    val accuracy: Float = 0f,
    val mistakes: Int = 0
)

@Serializable
data class CardStats(
    val cardKey: String = "",
    val practiceType: Long = 0,
    val totalReviews: Int = 0,
    val totalTime: Duration = Duration.ZERO,
    val accuracy: Float = 0f,
    val lapses: Int = 0,
    val avgResponseTime: Duration = Duration.ZERO,
    val intervalHistory: List<Duration> = emptyList(),
    val easeHistory: List<Float> = emptyList(),
    val reviewHistory: List<Instant> = emptyList()
)

@Serializable
data class DeckStats(
    val deckId: Long = 0,
    val deckName: String = "",
    val totalCards: Int = 0,
    val newCards: Int = 0,
    val learningCards: Int = 0,
    val youngCards: Int = 0,
    val matureCards: Int = 0,
    val suspendedCards: Int = 0,
    val buriedCards: Int = 0,
    val accuracy: Float = 0f,
    val retention: Float = 0f,
    val avgTime: Duration = Duration.ZERO,
    val reviewsToday: Int = 0,
    val forecastCards: List<Int> = emptyList()
)

/** Heatmap data point */
@Serializable
data class HeatmapDay(
    val date: String,
    val count: Int,
    val decksStudied: Set<String> = emptySet(),
    val newCards: Int = 0,
    val reviewCards: Int = 0,
    val timeStudied: Duration = Duration.ZERO,
    val accuracy: Float = 0f,
    val mistakes: Int = 0
)
