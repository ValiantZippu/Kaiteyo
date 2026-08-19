package ua.syt0r.kanji.presentation.screen.main.screen.home.screen.general_dashboard

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import ua.syt0r.kanji.Res
import ua.syt0r.kanji.core.srs.LetterPracticeType
import ua.syt0r.kanji.core.srs.PracticeType
import ua.syt0r.kanji.core.srs.VocabPracticeType
import ua.syt0r.kanji.discord
import ua.syt0r.kanji.practice_type_flashcard
import ua.syt0r.kanji.practice_type_reading_picker
import ua.syt0r.kanji.practice_type_writing
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.PracticeConfigurationCard
import ua.syt0r.kanji.social_discord
import ua.syt0r.kanji.social_youtube
import ua.syt0r.kanji.study_category_letter
import ua.syt0r.kanji.study_category_vocab
import ua.syt0r.kanji.youtube


sealed interface StudyTargetProgress {

    object NoDecks : StudyTargetProgress

    data class WithDecks(
        val options: StudyTargetPracticeOptions,
        val totalProgress: Float
    ) : StudyTargetProgress

}

data class StudyTargetPracticeOptions(
    val newCards: List<PracticeConfigurationCard>,
    val dueCards: List<PracticeConfigurationCard>,
    val combinedCards: List<PracticeConfigurationCard>
)

data class StudyTargetState(
    val studyTarget: StudyTarget,
    val enabled: Boolean,
    val progress: StudyTargetProgress
)

enum class StudyTarget(
    val categoryTitle: StringResource,
    val typeTitleRes: StringResource,
    val practiceType: PracticeType
) {

    LetterWriting(
        Res.string.study_category_letter,
        Res.string.practice_type_writing,
        LetterPracticeType.Writing
    ),
    LetterFlashcards(
        Res.string.study_category_letter,
        Res.string.practice_type_flashcard,
        LetterPracticeType.Reading
    ),
    VocabFlashcard(
        Res.string.study_category_vocab,
        Res.string.practice_type_flashcard,
        VocabPracticeType.Flashcard
    ),
    VocabWriting(
        Res.string.study_category_vocab,
        Res.string.practice_type_writing,
        VocabPracticeType.Writing
    ),
    VocabReadingPicker(
        Res.string.study_category_vocab,
        Res.string.practice_type_reading_picker,
        VocabPracticeType.ReadingPicker
    )

}

data class StreakCalendarItem(
    val date: LocalDate,
    val anyReviews: Boolean
)

data class DashboardDaySummary(
    val date: LocalDate,
    val count: Int
)

enum class DashboardDeckCategory { Letters, Vocabulary }

/**
 * Lightweight deck summary shown in the "Recent decks" section.
 * Counts are the *today* queue (daily new + daily due) for the
 * deck, aggregated across all practice types.
 */
data class DashboardDeckSummary(
    val deckId: Long,
    val title: String,
    val category: DashboardDeckCategory,
    val lastReview: Instant?,
    val newCount: Int,
    val dueCount: Int,
    /** Total unique cards in the deck (for the progress bar). */
    val totalCount: Int = 0,
    /** Unique cards that are past "new" (done or due) — the studied fraction. */
    val studiedCount: Int = 0
) {
    val progressFraction: Float
        get() = if (totalCount == 0) 0f else studiedCount.toFloat() / totalCount
}

data class GeneralDashboardStats(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val reviewsToday: Int = 0,
    val totalReviews: Long = 0,
    val weeklySummary: List<DashboardDaySummary> = emptyList(),
    /**
     * Daily review counts covering the last 12 weeks (oldest first), used to
     * render the header activity heatmap at a glance.
     */
    val heatmapSummary: List<DashboardDaySummary> = emptyList(),
    val newReviewedToday: Int = 0,
    val dueReviewedToday: Int = 0,
    val newLeftoverToday: Int = 0,
    val dueLeftoverToday: Int = 0
) {

    val reviewedToday: Int get() = newReviewedToday + dueReviewedToday
    val leftoverToday: Int get() = newLeftoverToday + dueLeftoverToday

    val todayProgressFraction: Float
        get() {
            val total = reviewedToday + leftoverToday
            if (total == 0) return 0f
            return reviewedToday.toFloat() / total
        }

}

/**
 * One explained "study this next" pick, computed by the recommendation
 * engine from REAL data: the user's actual FSRS study state per kanji plus
 * the kanji's corpus frequency rank. The reason is a human-readable
 * sentence built from those facts — never a fabricated score.
 */
data class DashboardRecommendation(
    val character: String,
    val keyword: String?,
    val reason: String,
    /** Sortable urgency: higher = recommended sooner. */
    val urgency: Int
)

enum class SocialButton(
    val url: String,
    val title: StringResource,
    val icon: DrawableResource
) {
    Discord(
        url = "https://discord.gg/2Ny6h6pXTY",
        title = Res.string.social_discord,
        icon = Res.drawable.discord
    ),
    YouTube(
        url = "https://github.com/ValiantZippu/Kaiteyo",
        title = Res.string.social_youtube,
        icon = Res.drawable.youtube
    )
}
