package ua.syt0r.kanji.core.statistics

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlin.time.Duration

// ============================================
// KAITEYO STATISTICS DOMAIN MODELS
// Single source of truth for the analytics and
// examination system. Everything the UI renders
// is produced by StatisticsRepository / the
// StatisticsCalculator from real database data.
// ============================================

/** Study mode labels used across statistics recording & analysis. */
object StudyModes {
    const val FLASHCARD = "flashcard"
    const val WRITING = "writing"
    const val READING = "reading"
    const val EXAM = "exam"
    const val KANJI = "kanji"
    const val VOCAB = "vocab"
}

/** Content types a reviewed/studied entity belongs to. */
object ContentTypes {
    const val KANJI = "kanji"
    const val VOCAB = "vocab"
    const val RADICAL = "radical"
    const val GRAMMAR = "grammar"
    const val KANA = "kana"
}

// ============================================
// OVERVIEW
// ============================================

/**
 * Top-level dashboard numbers. All fields are derived from real
 * review history, SRS cards and recorded study sessions.
 */
data class StatisticsOverview(
    val today: DailyActivity = DailyActivity(),
    val weekReviews: Int = 0,
    val weekStudyTime: Duration = Duration.ZERO,
    val weekAccuracy: Float = 0f,
    val monthReviews: Int = 0,
    val monthStudyTime: Duration = Duration.ZERO,
    val monthAccuracy: Float = 0f,
    val totalReviews: Long = 0,
    val totalStudyTime: Duration = Duration.ZERO,
    val overallAccuracy: Float = 0f,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val averageReviewsPerDay: Float = 0f,
    val averageTimePerCard: Duration = Duration.ZERO,
    val uniqueKanjiStudied: Int = 0,
    val uniqueVocabStudied: Int = 0,
    val cards: CardStatusCounts = CardStatusCounts(),
    val retention: RetentionSummary = RetentionSummary(),
    val forecastNextDays: List<Int> = emptyList(),
    val firstStudyDate: LocalDate? = null,
    val activeDays: Int = 0
)

/** Snapshot of the SRS card state distribution. */
data class CardStatusCounts(
    val total: Int = 0,
    val due: Int = 0,
    val new: Int = 0,
    val learning: Int = 0,
    val young: Int = 0,
    val mature: Int = 0,
    val relearning: Int = 0,
    val suspended: Int = 0,
    val buried: Int = 0,
    val archived: Int = 0,
    val flagged: Int = 0,
    val averageIntervalDays: Int = 0,
    val averageEase: Float = 2.5f
)

/** Aggregate retention (correct / total) over a window. */
data class RetentionSummary(
    val totalReviews: Long = 0,
    val correct: Long = 0,
    val incorrect: Long = 0
) {
    val accuracy: Float
        get() = if (totalReviews == 0L) 0f else correct.toFloat() / totalReviews
}

// ============================================
// DAILY ACTIVITY + HEATMAP
// ============================================

/** Everything that happened on a single local calendar day. */
data class DailyActivity(
    val date: LocalDate? = null,
    val reviews: Int = 0,
    val newCards: Int = 0,
    val reviewCards: Int = 0,
    val correct: Int = 0,
    val incorrect: Int = 0,
    val lapses: Int = 0,
    val studyTime: Duration = Duration.ZERO,
    val cardsStudied: Int = 0,
    val kanjiReviews: Int = 0,
    val vocabReviews: Int = 0,
    val writingAttempts: Int = 0,
    val writingCorrect: Int = 0,
    val examsTaken: Int = 0,
    val examScoreSum: Int = 0,
    val examScoreCount: Int = 0,
    val sessions: Int = 0
) {
    val accuracy: Float
        get() = if (reviews == 0) 0f else correct.toFloat() / reviews

    val averageExamScore: Float
        get() = if (examScoreCount == 0) 0f else examScoreSum.toFloat() / examScoreCount

    val isEmpty: Boolean
        get() = reviews == 0 && writingAttempts == 0 && examsTaken == 0 && sessions == 0
}

/** One heatmap cell. */
data class HeatmapCell(
    val date: LocalDate,
    val activity: DailyActivity
) {
    val intensity: Float get() = activity.intensityForHeatmap
}

private val DailyActivity.intensityForHeatmap: Float
    get() {
        val total = reviews + writingAttempts + examsTaken * 20
        return when {
            total <= 0 -> 0f
            total < 5 -> 0.25f
            total < 15 -> 0.5f
            total < 40 -> 0.75f
            else -> 1f
        }
    }

/** Heatmap for one year (already laid out per week). */
data class HeatmapYear(
    val year: Int,
    val cells: Map<LocalDate, DailyActivity> = emptyMap(),
    val totalReviews: Long = 0,
    val totalStudyTime: Duration = Duration.ZERO,
    val activeDays: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0
)

/** A recorded study session (start/end, mode, counts, accuracy). */
@Serializable
data class StudySessionRecord(
    val id: Long = 0,
    val startTime: kotlinx.datetime.Instant,
    val endTime: kotlinx.datetime.Instant? = null,
    val duration: Duration = Duration.ZERO,
    val practiceType: Long = -1,
    val mode: String = "",
    val deckId: Long = 0,
    val deckName: String = "",
    val itemsStudied: Int = 0,
    val newItems: Int = 0,
    val reviewItems: Int = 0,
    val correct: Int = 0,
    val incorrect: Int = 0,
    val isComplete: Boolean = false
) {
    val accuracy: Float
        get() = if (itemsStudied == 0) 0f else correct.toFloat() / itemsStudied
}

/** A milestone on the learning timeline, derived from real history. */
data class LearningMilestone(
    val date: LocalDate,
    val title: String,
    val icon: String = "★",
    val value: String = ""
)

// ============================================
// KNOWLEDGE
// ============================================

/** Per-item knowledge state definitions (documented, shared app-wide). */
enum class KnowledgeState(val label: String) {
    /** Never reviewed. */
    New("New"),
    /** Reviewed at least once but never graduated out of the learning phase. */
    Learning("Learning"),
    /** Reviewed at least once with an interval >= 1 day. */
    Learned("Learned"),
    /** FSRS review status with interval >= 21 days. */
    Mature("Mature"),
    /** FSRS review status with interval >= 180 days. */
    Mastered("Mastered"),
    /** Relearning after a lapse (FSRS Relearning status). */
    Relearning("Relearning"),
    /** 3+ lapses recorded on the FSRS card. */
    Weak("Weak"),
    /** Suspended or buried. */
    Suspended("Suspended")
}

/** A single item (kanji or vocabulary) with its derived knowledge state. */
data class KnowledgeItem(
    val key: String,
    val content: String,
    val reading: String = "",
    val meaning: String = "",
    val state: KnowledgeState,
    val jlptLevel: Int? = null,
    val lapses: Int = 0,
    val reviews: Int = 0,
    val intervalDays: Int = 0,
    val accuracy: Float = 0f,
    val writingAccuracy: Float? = null
)

/** Aggregated knowledge for one content type (kanji / vocab). */
data class ContentTypeKnowledge(
    val contentType: String,
    val totalCatalog: Int = 0,
    val studied: Int = 0,
    val learned: Int = 0,
    val mature: Int = 0,
    val mastered: Int = 0,
    val learning: Int = 0,
    val relearning: Int = 0,
    val weak: Int = 0,
    val suspended: Int = 0,
    val writingVerified: Int = 0,
    val recognitionOnly: Int = 0,
    val jlptCoverage: List<JlptCoverage> = emptyList(),
    val frequencyCoverage: List<FrequencyCoverage> = emptyList(),
    val weakItems: List<KnowledgeItem> = emptyList()
)

/** Coverage of one JLPT band. */
data class JlptCoverage(
    val level: Int,
    val total: Int = 0,
    val encountered: Int = 0,
    val studied: Int = 0,
    val learned: Int = 0,
    val mastered: Int = 0
) {
    val studiedRatio: Float get() = if (total == 0) 0f else studied.toFloat() / total
    val learnedRatio: Float get() = if (total == 0) 0f else learned.toFloat() / total
}

/** Coverage of one frequency band (vocabulary). */
data class FrequencyCoverage(
    val label: String,
    val bandStart: Int,
    val bandEnd: Int?,
    val total: Int = 0,
    val studied: Int = 0,
    val learned: Int = 0
) {
    val studiedRatio: Float get() = if (total == 0) 0f else studied.toFloat() / total
}

/** Per-skill accuracy (recognition / reading / meaning / writing). */
data class SkillMatrixRow(
    val label: String,
    val recognition: Float? = null,
    val reading: Float? = null,
    val meaning: Float? = null,
    val writing: Float? = null
)

/** Writing analytics for a character (real evaluator output). */
data class WritingCharacterStats(
    val character: String,
    val attempts: Int = 0,
    val correct: Int = 0,
    val mistakes: Int = 0,
    val wrongOrder: Int = 0,
    val almost: Int = 0
) {
    val accuracy: Float get() = if (attempts == 0) 0f else correct.toFloat() / attempts
}

// ============================================
// DAY PRACTICE (cards practiced on a single day)
// ============================================

/**
 * One item's aggregated practice on a single local day, derived from raw
 * review history (never reconstructed from current deck state).
 */
data class DayItemPractice(
    val key: String,
    val contentType: String,
    val practiceType: Long,
    val practiceLabel: String,
    /** Display content — kanji character or vocabulary expression. */
    val content: String = "",
    val reading: String = "",
    val meaning: String = "",
    /** Number of review answers recorded for this item that day. */
    val count: Int = 0,
    /** Correct answers (grade > 1, i.e. Good/Easy). */
    val correct: Int = 0,
    /** Sum of stroke/typing mistakes recorded across those answers. */
    val mistakes: Int = 0,
    /** Best grade observed that day (0=Again, 1=Hard, 2=Good, 3=Easy). */
    val lastGrade: Int = -1
) {
    val accuracy: Float
        get() = if (count == 0) 0f else correct.toFloat() / count
}

/** Everything practiced on a single local day, grouped by content type. */
data class DayPracticeBreakdown(
    val date: LocalDate? = null,
    val kanji: List<DayItemPractice> = emptyList(),
    val vocab: List<DayItemPractice> = emptyList(),
    /** Practice type ids considered "writing mode" (subset overlap). */
    val writingTypes: Set<Long> = emptySet()
) {
    val totalReviews: Int
        get() = kanji.sumOf { it.count } + vocab.sumOf { it.count }

    val correct: Int
        get() = kanji.sumOf { it.correct } + vocab.sumOf { it.correct }

    val totalMistakes: Int
        get() = kanji.sumOf { it.mistakes } + vocab.sumOf { it.mistakes }

    val accuracy: Float
        get() = if (totalReviews == 0) 0f else correct.toFloat() / totalReviews

    /** Items studied in a writing mode (kanji or vocab writing). */
    val writing: List<DayItemPractice>
        get() = (kanji + vocab).filter { it.practiceType in writingTypes }

    val isEmpty: Boolean
        get() = kanji.isEmpty() && vocab.isEmpty()
}

// ============================================
// RETENTION / SRS
// ============================================

/** Distribution of review answers (grades 0..3 => Again/Hard/Good/Easy). */
data class GradeDistribution(
    val again: Long = 0,
    val hard: Long = 0,
    val good: Long = 0,
    val easy: Long = 0
) {
    val total: Long get() = again + hard + good + easy
}

/** Interval buckets observed in review history. */
data class IntervalBucket(
    val label: String,
    val minDays: Int,
    val count: Long = 0
)

/** Retention split by time since review (1d/3d/7d/14d/30d/90d+). */
data class RetentionByAge(
    val label: String,
    val total: Long = 0,
    val correct: Long = 0
) {
    val accuracy: Float get() = if (total == 0L) 0f else correct.toFloat() / total
}

// ============================================
// MISTAKES / WEAKNESS
// ============================================

/** Structured, persisted mistake record. */
data class LearningMistake(
    val id: Long = 0,
    val timestamp: kotlinx.datetime.Instant,
    val entityKey: String = "",
    val contentType: String = "",
    val mode: String = "",
    val questionType: String = "",
    val expected: String = "",
    val actual: String = "",
    val category: String = "unknown",
    val severity: Int = 1,
    val sessionId: Long? = null,
    val examId: Long? = null,
    val deckId: Long = 0
)

/** Weak entity aggregated from mistake history + FSRS lapses. */
data class WeakEntity(
    val entityKey: String,
    val contentType: String,
    val content: String = "",
    val reading: String = "",
    val meaning: String = "",
    val mistakeCount: Int = 0,
    val lapses: Int = 0,
    val accuracy: Float = 0f
)

// ============================================
// EXAMS
// ============================================

enum class ExamStatus(val dbValue: Int) {
    InProgress(0),
    Completed(1),
    Abandoned(2);

    companion object {
        fun from(value: Int): ExamStatus = entries.firstOrNull { it.dbValue == value } ?: Abandoned
    }
}

/** An exam attempt (header). */
data class ExamRecord(
    val id: Long = 0,
    val title: String = "",
    val examType: String = "",
    val scopeJson: String = "",
    val questionCount: Int = 0,
    val timeLimitMs: Long? = null,
    val seed: Long = 0,
    val startedAt: kotlinx.datetime.Instant,
    val finishedAt: kotlinx.datetime.Instant? = null,
    val status: ExamStatus = ExamStatus.InProgress,
    val score: Int = 0,
    val accuracy: Float = 0f,
    val totalTimeMs: Long = 0
)

/** Exam statistics aggregated across completed exams. */
data class ExamStatistics(
    val completed: Int = 0,
    val averageScore: Float = 0f,
    val highestScore: Int = 0,
    val lowestScore: Int = 0,
    val averageAccuracy: Float = 0f,
    val averageTimeMs: Long = 0,
    val scoreTrend: List<ExamScorePoint> = emptyList(),
    val byType: List<Pair<String, Int>> = emptyList(),
    val byJlpt: List<Pair<Int, Int>> = emptyList()
)

data class ExamScorePoint(
    val date: kotlinx.datetime.Instant,
    val score: Int,
    val accuracy: Float
)

// ============================================
// GOALS
// ============================================

/** User-defined learning goal. Persisted locally, offline. */
@Serializable
data class LearningGoal(
    val id: String = "",
    val type: GoalType = GoalType.DailyReviews,
    val target: Int = 20,
    val period: GoalPeriod = GoalPeriod.Daily
) {
    val label: String get() = type.displayName
}

@Serializable
enum class GoalType(val displayName: String) {
    DailyReviews("Daily reviews"),
    NewKanji("New kanji"),
    NewVocab("New vocabulary"),
    StudyTime("Study time (minutes)"),
    WritingAttempts("Writing attempts"),
    Exams("Exams completed"),
    Streak("Day streak")
}

@Serializable
enum class GoalPeriod {
    Daily, Weekly
}

/** Current progress toward a goal. */
data class GoalProgress(
    val goal: LearningGoal,
    val current: Int = 0,
    val target: Int = 1,
    val completed: Boolean = false
) {
    val fraction: Float get() = if (target <= 0) 0f else (current.toFloat() / target).coerceIn(0f, 1f)
    val remaining: Int get() = (target - current).coerceAtLeast(0)
}
