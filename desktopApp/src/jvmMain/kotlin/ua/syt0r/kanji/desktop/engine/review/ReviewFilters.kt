package ua.syt0r.kanji.desktop.engine.review

import kotlinx.datetime.Clock
import ua.syt0r.kanji.desktop.model.DesktopCard
import ua.syt0r.kanji.desktop.model.SrsStatus

// ============================================
// REVIEW FILTERS & SETTINGS
// Filtered review, custom intervals, preview, and
// the review settings model used by the UI.
// ============================================

data class ReviewSettings(
    val includeNew: Boolean = true,
    val newLimit: Int = 20,
    val reviewLimit: Int = 200,
    val shuffle: Boolean = true,
    val burySiblings: Boolean = true,
    val autoAdvance: Boolean = true,
    val confirmAgain: Boolean = false,
    val showPreview: Boolean = true,
    val customIntervalDays: Double? = null
)

/** Named filtered-review presets (Anki "filtered deck" equivalents). */
enum class ReviewFilterPreset(val id: String, val label: String, val query: String) {
    DueToday("due-today", "Due today", "due:today"),
    New("new", "New cards", "status:new"),
    Overdue("overdue", "Overdue", "due:past"),
    Lapsed("lapsed", "Lapsed", "lapses:>2"),
    LowAccuracy("low-accuracy", "Low accuracy", "accuracy:<0.7"),
    Flagged("flagged", "Flagged", "flag:*"),
    Favorite("favorite", "Favorites", "favorite:true"),
    All("all", "Everything", "")
}

/** Pure functions to build the filtered review queue. */
object ReviewQueueBuilder {

    fun build(
        cards: List<DesktopCard>,
        preset: ReviewFilterPreset,
        settings: ReviewSettings,
        now: kotlinx.datetime.Instant = Clock.System.now()
    ): List<DesktopCard> {
        val base = cards.filter { it.status != SrsStatus.Suspended && it.status != SrsStatus.Buried }
        val byQuery = if (preset.query.isBlank()) base
        else base.filter { ua.syt0r.kanji.desktop.engine.search.SearchEngine.matches(it, preset.query) }

        val newCards = if (settings.includeNew) byQuery.filter { it.status == SrsStatus.New }.take(settings.newLimit) else emptyList()
        val due = byQuery.filter { it.status != SrsStatus.New && it.dueAt != null && it.dueAt <= now }
            .take(settings.reviewLimit)

        val combined = (newCards + due).distinctBy { it.id }
        return if (settings.shuffle) combined.shuffled(kotlin.random.Random(7)) else combined
    }

    fun buildCustom(
        cards: List<DesktopCard>,
        query: String,
        settings: ReviewSettings,
        now: kotlinx.datetime.Instant = Clock.System.now()
    ): List<DesktopCard> {
        val matched = cards.filter { ua.syt0r.kanji.desktop.engine.search.SearchEngine.matches(it, query) }
        val newCards = if (settings.includeNew) matched.filter { it.status == SrsStatus.New } else emptyList()
        val due = matched.filter { it.status != SrsStatus.New && it.dueAt != null && it.dueAt <= now }
        val combined = (newCards + due).distinctBy { it.id }
        return if (settings.shuffle) combined.shuffled(kotlin.random.Random(7)) else combined
    }
}
