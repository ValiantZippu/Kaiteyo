package ua.syt0r.kanji.desktop.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.time.Duration

// ============================================
// KAITEYO DESKTOP SUITE — DOMAIN MODELS
// Self-contained, serializable, platform-neutral
// card & study models used by every engine and view.
// ============================================

/** Mirrors Anki's card lifecycle states. */
@Serializable
enum class SrsStatus {
    New,
    Learning,
    Review,
    Relearning,
    Suspended,
    Buried;

    companion object {
        fun fromName(name: String?): SrsStatus =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: New
    }
}

/** The four Anki-style answer ratings. */
@Serializable
enum class ReviewRating(val code: Int, val displayName: String) {
    Again(0, "Again"),
    Hard(1, "Hard"),
    Good(2, "Good"),
    Easy(3, "Easy");

    companion object {
        fun fromCode(code: Int): ReviewRating = entries.firstOrNull { it.code == code } ?: Good
    }
}

/** Core card entity. Kept intentionally rich so filters/statistics are meaningful. */
@Serializable
data class DesktopCard(
    val id: String,
    val character: String,
    val meaning: String,
    val onReadings: List<String> = emptyList(),
    val kunReadings: List<String> = emptyList(),
    val radicals: List<String> = emptyList(),
    val components: List<String> = emptyList(),
    val strokeCount: Int = 0,
    val jlpt: Int? = null,
    val grade: Int? = null,
    val frequency: Int? = null,
    val tags: List<String> = emptyList(),
    val flags: List<String> = emptyList(),
    val favorite: Boolean = false,
    val note: String = "",
    val status: SrsStatus = SrsStatus.New,
    val intervalDays: Double = 0.0,
    val dueAt: Instant? = null,
    val lapses: Int = 0,
    val reps: Int = 0,
    val ease: Double = 2.5,
    val accuracy: Float = 0.5f,
    val deckId: String = DEFAULT_DECK_ID,
    val createdAt: Instant = Instant.fromEpochMilliseconds(0),
    val lastReviewedAt: Instant? = null,
    val contentKind: ContentKind = ContentKind.Kanji,
    /** Stable foreign key for interop (e.g. `anki:<guid>`); blank when native. */
    val externalId: String = ""
) {
    val readings: List<String> get() = onReadings + kunReadings

    /** Searchable text blob used for free-text matching. */
    val searchableText: String
        get() = buildString {
            append(character)
            append(' ').append(meaning)
            append(' ').append(readings.joinToString(" "))
            append(' ').append(radicals.joinToString(" "))
            append(' ').append(tags.joinToString(" "))
            append(' ').append(contentKind.label)
        }.lowercase()

    companion object {
        const val DEFAULT_DECK_ID: String = "default"
    }
}

/** One recorded review event. */
@Serializable
data class ReviewLogEntry(
    val cardId: String,
    val reviewedAt: Instant,
    val rating: ReviewRating,
    val timeSpent: Duration = Duration.ZERO,
    val intervalBefore: Double = 0.0,
    val intervalAfter: Double = 0.0,
    val wasNew: Boolean = false,
    val source: String = "review"
)

/** Aggregated per-day study totals used by heatmaps, dashboards and goals. */
@Serializable
data class StudyDaySummary(
    val day: String,
    val newCount: Int = 0,
    val reviewCount: Int = 0,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val timeSpent: Duration = Duration.ZERO
) {
    val accuracy: Float
        get() = if (correctCount + wrongCount == 0) 0f else correctCount.toFloat() / (correctCount + wrongCount)
}

/**
 * User-defined named collection (manual, automatic or smart). A collection
 * is a container: it owns [cardIds] directly and, since the Library is the
 * learning hub, it can also own whole decks via [deckIds]. Decks listed here
 * belong to the collection; cards inside those decks are included in
 * [CollectionStore.resolveCards] automatically.
 */
@Serializable
data class CollectionDef(
    val id: String,
    val name: String,
    val description: String = "",
    val kind: CollectionKind = CollectionKind.Manual,
    val parentId: String? = null,
    val pinned: Boolean = false,
    val favorite: Boolean = false,
    val archived: Boolean = false,
    val smartRule: SmartCollectionRule? = null,
    val cardIds: List<String> = emptyList(),
    /** Decks owned by this collection (membership, not copies). */
    val deckIds: List<String> = emptyList(),
    val createdAt: Instant = Clock.System.now()
)

@Serializable
enum class CollectionKind { Manual, Automatic, Smart }

/** Rule engine for smart collections. */
@Serializable
data class SmartCollectionRule(
    val match: MatchAll = MatchAll.MatchAll,
    val conditions: List<SmartCondition> = emptyList()
)

@Serializable
enum class MatchAll { MatchAll, MatchAny }

@Serializable
data class SmartCondition(
    val field: SmartField,
    val operator: SmartOperator,
    val value: String = ""
)

@Serializable
enum class SmartField { Status, Tag, Flag, Favorite, Accuracy, Lapses, Interval, Reps, Due, Jlpt, Grade, Strokes, Frequency, LastReviewed }
@Serializable
enum class SmartOperator { Equals, NotEquals, GreaterThan, LessThan, Contains, Is }

/** Toast/notification payloads. */
@Serializable
data class ToastMessage(
    val text: String,
    val kind: ToastKind = ToastKind.Info,
    val durationMs: Long = 3500
)

@Serializable
enum class ToastKind { Info, Success, Warning, Error }

// ============================================
// COLLECTION PRESETS — the "smart" presets the
// product spec requires (Recently learned, Failed
// today, Failed this week, Low accuracy, Not
// reviewed, Flagged, Favorite).
// ============================================

object SmartCollectionPresets {
    fun recentlyLearned(days: Int = 1): SmartCollectionRule = SmartCollectionRule(
        conditions = listOf(
            SmartCondition(SmartField.LastReviewed, SmartOperator.GreaterThan, days.toString())
        )
    )

    fun failedToday(): SmartCollectionRule = SmartCollectionRule(
        conditions = listOf(
            SmartCondition(SmartField.LastReviewed, SmartOperator.Equals, "today"),
            SmartCondition(SmartField.Accuracy, SmartOperator.LessThan, "0.6")
        )
    )

    fun failedThisWeek(): SmartCollectionRule = SmartCollectionRule(
        conditions = listOf(
            SmartCondition(SmartField.LastReviewed, SmartOperator.GreaterThan, "7"),
            SmartCondition(SmartField.Lapses, SmartOperator.GreaterThan, "0")
        )
    )

    fun lowAccuracy(threshold: Float = 0.6f): SmartCollectionRule = SmartCollectionRule(
        conditions = listOf(
            SmartCondition(SmartField.Accuracy, SmartOperator.LessThan, threshold.toString()),
            SmartCondition(SmartField.Reps, SmartOperator.GreaterThan, "5")
        )
    )

    fun notReviewed(): SmartCollectionRule = SmartCollectionRule(
        conditions = listOf(
            SmartCondition(SmartField.Reps, SmartOperator.Equals, "0")
        )
    )

    fun flagged(): SmartCollectionRule = SmartCollectionRule(
        conditions = listOf(
            SmartCondition(SmartField.Flag, SmartOperator.Contains, "*")
        )
    )

    fun favorite(): SmartCollectionRule = SmartCollectionRule(
        conditions = listOf(
            SmartCondition(SmartField.Favorite, SmartOperator.Equals, "true")
        )
    )
}
