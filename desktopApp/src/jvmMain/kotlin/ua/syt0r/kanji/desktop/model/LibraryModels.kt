package ua.syt0r.kanji.desktop.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

// ============================================
// KAITEYO LIBRARY MODELS
// The content-management layer for decks, study
// modes and per-mode progress. Every card in the
// pool is typed by ContentKind and grouped into
// decks. Each deck exposes several independent
// StudyModes (Flashcards, Writing, Recognition,
// Recall, Listening) that maintain their own SRS
// progress, streaks, due dates and statistics while
// sharing the same underlying content.
// ============================================

/** Typed content the library can manage. Future kinds integrate without schema changes. */
@Serializable
enum class ContentKind(val label: String, val glyph: String) {
    Kanji("Kanji", "字"),
    Vocabulary("Vocabulary", "語"),
    Kana("Kana", "あ"),
    Grammar("Grammar", "文"),
    Radical("Radicals", "部"),
    Sentence("Sentences", "句"),
    Media("Media", "媒");

    companion object {
        fun fromName(name: String?): ContentKind =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: Kanji
    }
}

/**
 * A study track. Each mode is an independent learning
 * lane over the same content: graduating flashcards does
 * NOT graduate writing, recognition, recall or listening.
 * Grammar decks get original drill lanes (Cloze and
 * Pattern) instead of a one-size-fits-all workflow.
 */
@Serializable
enum class StudyMode(val label: String, val glyph: String, val hint: String) {
    Flashcards("Flashcards", "卡", "See the front, recall the back"),
    Writing("Writing", "書", "Handwrite the kanji from memory"),
    Recognition("Recognition", "識", "Recognise the meaning from the form"),
    Recall("Recall", "憶", "Recall the reading from the form"),
    Listening("Listening", "聞", "Understand the spoken form"),
    Cloze("Fill the Blank", "空", "Complete the pattern from context"),
    Pattern("Pattern Review", "型", "Explain the pattern, then confirm");

    companion object {
        fun fromName(name: String?): StudyMode =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: Flashcards

        /** The study lanes a deck of the given content kind exposes. */
        fun forKind(kind: ContentKind): List<StudyMode> = when (kind) {
            ContentKind.Kanji -> listOf(Flashcards, Recognition, Writing)
            ContentKind.Vocabulary -> listOf(Flashcards, Recognition, Recall, Listening)
            ContentKind.Kana -> listOf(Flashcards, Recognition, Recall, Writing, Listening)
            ContentKind.Grammar -> listOf(Flashcards, Pattern, Cloze)
            ContentKind.Radical -> listOf(Recognition, Flashcards)
            ContentKind.Sentence -> listOf(Flashcards, Recall)
            ContentKind.Media -> listOf(Flashcards, Listening)
        }
    }
}

/** Independent SRS + stats state for one card within one study mode. */
@Serializable
data class StudyModeProgress(
    val status: SrsStatus = SrsStatus.New,
    val intervalDays: Double = 0.0,
    val dueAt: Instant? = null,
    val reps: Int = 0,
    val lapses: Int = 0,
    val ease: Double = 2.5,
    val accuracy: Float = 0.5f,
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val lastReviewedAt: Instant? = null,
    val totalReviews: Int = 0,
    val totalCorrect: Int = 0,
    val addedAt: Instant = Clock.System.now()
) {
    val isNew: Boolean get() = status == SrsStatus.New
    val isSuspended: Boolean get() = status == SrsStatus.Suspended || status == SrsStatus.Buried
    val isLearned: Boolean get() = status != SrsStatus.New && totalReviews > 0
    val isDue: Boolean
        get() = (status == SrsStatus.Learning || status == SrsStatus.Review || status == SrsStatus.Relearning) &&
            dueAt != null && dueAt <= Clock.System.now()
    val isCompleted: Boolean get() = status == SrsStatus.Review && intervalDays >= 21.0
    val accuracyPercent: Float get() = accuracy * 100f

    companion object {
        fun fromCard(card: DesktopCard): StudyModeProgress = StudyModeProgress(
            status = card.status,
            intervalDays = card.intervalDays,
            dueAt = card.dueAt,
            reps = card.reps,
            lapses = card.lapses,
            ease = card.ease,
            accuracy = card.accuracy,
            lastReviewedAt = card.lastReviewedAt
        )
    }
}

/**
 * A deck (learning group). Built-in decks use a [filterQuery]
 * so membership is resolved dynamically against any imported
 * content (future expansion friendly); custom decks use explicit
 * [cardIds] (optionally combined with a filter). No hardcoded
 * assumption that only one content kind exists.
 */
@Serializable
data class DeckDef(
    val id: String,
    val name: String,
    val description: String = "",
    val kind: ContentKind = ContentKind.Kanji,
    val builtIn: Boolean = false,
    val parentId: String? = null,
    val pinned: Boolean = false,
    val favorite: Boolean = false,
    val archived: Boolean = false,
    val difficulty: Int = 2,                 // 1..5
    val tags: List<String> = emptyList(),
    val source: String = "builtin",
    val filterQuery: String = "",            // SearchEngine query => dynamic membership
    val cardIds: List<String> = emptyList(), // explicit membership
    val createdAt: Instant = Clock.System.now(),
    val importedAt: Instant? = null,
    val icon: String = ""                    // optional leading glyph
)

/** Aggregate statistics for one deck within one study mode. */
data class DeckModeStats(
    val mode: StudyMode,
    val total: Int = 0,
    val newCount: Int = 0,
    val learningCount: Int = 0,
    val reviewCount: Int = 0,
    val suspendedCount: Int = 0,
    val buriedCount: Int = 0,
    val dueCount: Int = 0,
    val completedCount: Int = 0,
    val masteredCount: Int = 0,
    val accuracy: Float = 0f,
    val bestStreak: Int = 0,
    val avgInterval: Double = 0.0,
    val totalReviews: Int = 0
) {
    val learnedCount: Int get() = total - newCount
    val progressFraction: Float
        get() = if (total == 0) 0f else (completedCount + masteredCount).toFloat() / total
}

/** Aggregate statistics for a deck across all modes. */
data class DeckStats(
    val total: Int = 0,
    val byMode: Map<StudyMode, DeckModeStats> = emptyMap(),
    val anyDue: Int = 0,
    val anyNew: Int = 0,
    val anyCompleted: Int = 0,
    val favoriteCount: Int = 0,
    val targetedKind: ContentKind = ContentKind.Kanji
)

/** A ranked suggestion shown while typing in the universal search. */
data class LibrarySuggestion(
    val kind: String,           // "entry", "deck", "jlpt", "tag", "recent", "grade", "frequency"
    val title: String,
    val subtitle: String = "",
    val payload: String = "",   // search query to apply when chosen
    val action: String = ""     // optional direct navigation ("open-deck:<id>", "open-entry:<cardId>")
)

/** Result of a universal library search, grouped by content kind. */
data class LibrarySearchResult(
    val entry: DesktopCard,
    val deckId: String? = null
)

@Serializable
data class DeckExportDto(
    val deck: DeckDef,
    val cardIds: List<String>,
    val exportedAt: String = Clock.System.now().toString()
)

@Serializable
data class LibraryProgressDto(
    val version: Int = 1,
    val progress: Map<String, Map<String, StudyModeProgress>> = emptyMap(),
    val recentlyStudied: List<String> = emptyList()
)