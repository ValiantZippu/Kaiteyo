package ua.syt0r.kanji.desktop.engine.history

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import ua.syt0r.kanji.desktop.model.ReviewLogEntry

// ============================================
// ACTIVITY LOG
// Everything is logged: reviews, imports, exports,
// undos, tag/flag edits, syncs, plugin changes.
// In-memory ring with serialized snapshot support.
// ============================================

@Serializable
enum class ActivityCategory { Review, Study, Import, Export, Undo, Tag, Flag, Favorite, Note, Deck, Sync, Plugin, Settings, Theme, System }

@Serializable
data class ActivityEntry(
    val id: String,
    val timestamp: Instant,
    val category: ActivityCategory,
    val summary: String,
    val details: String = "",
    val affectedCount: Int = 1,
    val cardIds: List<String> = emptyList(),
    val reversible: Boolean = false
)

/** Aggregated counts per category for the dashboard "history" panel. */
@Serializable
data class ActivitySummary(
    val total: Int = 0,
    val byCategory: Map<ActivityCategory, Int> = emptyMap()
)

class ActivityLog(private val maxEntries: Int = 2000) {

    private val _entries = mutableListOf<ActivityEntry>()
    val entries: List<ActivityEntry> get() = _entries.toList().asReversed()

    private var counter = 0L

    fun record(
        category: ActivityCategory,
        summary: String,
        details: String = "",
        affectedCount: Int = 1,
        cardIds: List<String> = emptyList(),
        reversible: Boolean = false
    ): ActivityEntry {
        counter++
        val entry = ActivityEntry(
            id = "act-${counter}-${Clock.System.now().toEpochMilliseconds()}",
            timestamp = Clock.System.now(),
            category = category,
            summary = summary,
            details = details,
            affectedCount = affectedCount,
            cardIds = cardIds,
            reversible = reversible
        )
        _entries.add(entry)
        if (_entries.size > maxEntries) _entries.removeAt(0)
        return entry
    }

    fun recordReview(log: ReviewLogEntry) {
        record(
            category = ActivityCategory.Review,
            summary = "Reviewed ${log.cardId} as ${log.rating.displayName}",
            details = "${log.intervalBefore}d -> ${log.intervalAfter}d",
            affectedCount = 1,
            cardIds = listOf(log.cardId),
            reversible = true
        )
    }

    fun load(entries: List<ActivityEntry>) {
        _entries.clear()
        _entries.addAll(entries.takeLast(maxEntries))
    }

    fun clear() {
        _entries.clear()
    }

    fun summary(): ActivitySummary {
        val byCat = _entries.groupingBy { it.category }.eachCount()
        return ActivitySummary(total = _entries.size, byCategory = byCat)
    }

    fun filter(category: ActivityCategory? = null, query: String? = null): List<ActivityEntry> {
        var result: List<ActivityEntry> = _entries
        if (category != null) result = result.filter { it.category == category }
        if (!query.isNullOrBlank()) {
            val q = query.lowercase()
            result = result.filter { it.summary.lowercase().contains(q) || it.details.lowercase().contains(q) }
        }
        return result.asReversed()
    }

    fun byDate(day: String): List<ActivityEntry> {
        val tz = kotlinx.datetime.TimeZone.currentSystemDefault()
        return _entries.filter { it.timestamp.toLocalDateTime(tz).date.toString() == day }
    }
}

/** Formatter helpers for rendering timestamps as short human strings. */
object ActivityFormatters {
    fun relative(timestamp: Instant, now: Instant = Clock.System.now()): String {
        val diff = now - timestamp
        val minutes = diff.inWholeMinutes
        return when {
            minutes < 1 -> "just now"
            minutes < 60 -> "${minutes}m ago"
            minutes < 24 * 60 -> "${minutes / 60}h ago"
            else -> "${minutes / (24 * 60)}d ago"
        }
    }
}
