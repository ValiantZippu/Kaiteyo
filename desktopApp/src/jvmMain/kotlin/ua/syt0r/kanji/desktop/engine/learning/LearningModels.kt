package ua.syt0r.kanji.desktop.engine.learning

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import ua.syt0r.kanji.desktop.model.ReviewRating
import ua.syt0r.kanji.desktop.model.SrsStatus
import ua.syt0r.kanji.desktop.model.StudyMode
import kotlin.time.Duration

// ============================================
// KAITEYO LEARNING MODEL
// One coherent data model for every learning
// object in the product:
//
//   LearningNote   → the content (one fact)
//   CardType       → the study direction
//   NoteCard       → note × card type (the card)
//   LearningStage  → introduced / learning /
//                    established / mature
//
// Reviews, writing attempts, exams and study
// sessions are all immutable events that feed
// the StatisticsRepository — statistics are
// never derived from current card state alone.
// ============================================

/** The kinds of learning content Kaiteyo manages. */
@Serializable
enum class LearningItemKind(val label: String) {
    Kanji("Kanji"),
    Vocabulary("Vocabulary"),
    Kana("Kana"),
    Radical("Radical"),
    Grammar("Grammar"),
    Custom("Custom")
}

/** A study direction for a note. Several card types can share one note. */
@Serializable
enum class CardType(val label: String, val glyph: String) {
    Recognition("Recognition", "識"),
    Meaning("Meaning", "意"),
    Reading("Reading", "読"),
    Writing("Writing", "書"),
    Listening("Listening", "聞"),
    Production("Production", "産"),
    Cloze("Cloze", "空"),
    Pattern("Pattern", "型");

    /** The legacy StudyMode a card type maps to (keeps the two systems bridged). */
    fun toStudyMode(): StudyMode = when (this) {
        Recognition -> StudyMode.Recognition
        Meaning -> StudyMode.Flashcards
        Reading -> StudyMode.Recall
        Writing -> StudyMode.Writing
        Listening -> StudyMode.Listening
        Production -> StudyMode.Recall
        Cloze -> StudyMode.Cloze
        Pattern -> StudyMode.Pattern
    }

    companion object {
        /** Card types a note of the given kind can generate (configurable per deck). */
        fun defaultsFor(kind: LearningItemKind): List<CardType> = when (kind) {
            LearningItemKind.Kanji -> listOf(Recognition, Meaning, Reading, Writing)
            LearningItemKind.Vocabulary -> listOf(Recognition, Meaning, Reading, Listening, Production)
            LearningItemKind.Kana -> listOf(Recognition, Meaning, Reading, Writing, Listening)
            LearningItemKind.Radical -> listOf(Recognition, Meaning)
            LearningItemKind.Grammar -> listOf(Meaning, Pattern, Cloze)
            LearningItemKind.Custom -> listOf(Recognition, Meaning)
        }
    }
}

/** Where a note came from — required so source data never overwrites user data. */
@Serializable
enum class NoteSourceType(val label: String) {
    Builtin("Built-in"),
    Import("Imported"),
    Dictionary("Dictionary"),
    MediaSubtitle("Media subtitle"),
    MediaAudio("Media audio"),
    MediaImage("Media image"),
    MediaVideo("Media video"),
    Anki("Anki import"),
    Custom("Custom")
}

/** Source metadata attached to a note (media mining, dictionary, import). */
@Serializable
data class NoteSource(
    val type: NoteSourceType = NoteSourceType.Custom,
    val sourceId: String = "",
    val sourceDetail: String = "",
    val timestampMs: Long = 0,
    val sourceText: String = ""
)

/** One learning fact. This is the NOTE — cards are generated from it. */
@Serializable
data class LearningNote(
    val id: String,
    val kind: LearningItemKind,
    /** The surface form: a kanji character, word, pattern or custom text. */
    val expression: String,
    val meanings: List<String> = emptyList(),
    /** Primary reading (hiragana/katakana); on-readings for kanji. */
    val reading: String = "",
    val onReadings: List<String> = emptyList(),
    val kunReadings: List<String> = emptyList(),
    val radicals: List<String> = emptyList(),
    val components: List<String> = emptyList(),
    val strokeCount: Int = 0,
    val jlpt: Int? = null,
    val grade: Int? = null,
    val frequency: Int? = null,
    val pitchAccent: String = "",
    val tags: List<String> = emptyList(),
    val examples: List<String> = emptyList(),
    val audioPath: String = "",
    val source: NoteSource = NoteSource(),
    val createdAt: Instant = Clock.System.now(),
    val updatedAt: Instant = Clock.System.now()
) {
    val allReadings: List<String> get() = (onReadings + kunReadings + listOf(reading)).filter { it.isNotBlank() }.distinct()

    /** Stable dedupe key: kind + expression (optional reading for homographs). */
    fun dedupeKey(): String = "${kind.name}:$expression:${reading.take(32)}"
}

/** Learning stage derived from a card's SRS state. Explicit criteria, not vibes. */
@Serializable
enum class LearningStage(val label: String) {
    Introduced("Introduced"),
    Learning("Learning"),
    Established("Established"),
    Mature("Mature");

    companion object {
        /**
         * Determine the stage from SRS state.
         *  - Suspended/buried → Introduced (excluded from study)
         *  - New (never reviewed) → Introduced
         *  - Learning/Relearning → Learning
         *  - Review with interval < 21 days → Established
         *  - Review with interval >= 21 days → Mature
         */
        fun of(status: SrsStatus, intervalDays: Double): LearningStage = when (status) {
            SrsStatus.New, SrsStatus.Suspended, SrsStatus.Buried -> Introduced
            SrsStatus.Learning, SrsStatus.Relearning -> Learning
            SrsStatus.Review -> if (intervalDays >= 21.0) Mature else Established
        }
    }
}

/** A studyable card = note × card type, carrying its own SRS state. */
@Serializable
data class NoteCard(
    val id: String,
    val noteId: String,
    val cardType: CardType,
    /** Deck this card belongs to (a card can be in several decks via membership). */
    val deckId: String,
    val status: SrsStatus = SrsStatus.New,
    val intervalDays: Double = 0.0,
    val dueAt: Instant? = null,
    val lapses: Int = 0,
    val reps: Int = 0,
    val ease: Double = 2.5,
    val accuracy: Float = 0.5f,
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val lastReviewedAt: Instant? = null,
    val createdAt: Instant = Clock.System.now(),
    /** Bury is session-scoped; persisted flag keeps it out of queues. */
    val buried: Boolean = false
) {
    val isNew: Boolean get() = status == SrsStatus.New
    val isSuspended: Boolean get() = status == SrsStatus.Suspended
    val stage: LearningStage get() = LearningStage.of(status, intervalDays)

    val isDue: Boolean
        get() = (status == SrsStatus.Learning || status == SrsStatus.Review || status == SrsStatus.Relearning) &&
            dueAt != null && dueAt <= Clock.System.now()
}

/** Per-deck study settings — replaces global-only limits with real deck config. */
@Serializable
data class DeckStudyConfig(
    val deckId: String,
    val dailyNewLimit: Int = 20,
    val dailyReviewLimit: Int = 200,
    val learningStepsMinutes: List<Long> = listOf(1, 10),
    val graduatingIntervalDays: Double = 1.0,
    val easyIntervalDays: Double = 4.0,
    val maximumIntervalDays: Double = 3650.0,
    val buryRelatedNew: Boolean = true,
    val buryRelatedReviews: Boolean = false,
    val suspendOnLapse: Boolean = false,
    /** Enabled card types; empty means the per-kind defaults. */
    val enabledCardTypes: List<CardType> = emptyList(),
    val interleaveNewAndReviews: Boolean = true
) {
    fun cardTypesFor(kind: LearningItemKind): List<CardType> =
        enabledCardTypes.ifEmpty { CardType.defaultsFor(kind) }
}

// ============================================
// IMMUTABLE EVENTS — the source of truth for
// every statistic in the product.
// ============================================

/** Why a review happened — separates normal study from exams and writing. */
@Serializable
enum class StudyActivityType(val label: String) {
    Review("Review"),
    Exam("Exam"),
    Writing("Writing"),
    Preview("Preview")
}

/**
 * A full-fidelity review event. Everything statistics need is captured at
 * the moment of the answer: deck, card type, mode, response time, previous
 * and new SRS state, mistakes and writing accuracy.
 */
@Serializable
data class LearningReviewEvent(
    val id: String,
    val cardId: String,
    val noteId: String,
    val deckId: String,
    val cardType: CardType,
    val activityType: StudyActivityType,
    val rating: ReviewRating,
    val reviewedAt: Instant,
    val responseTimeMs: Long = 0,
    val statusBefore: SrsStatus,
    val statusAfter: SrsStatus,
    val intervalBefore: Double = 0.0,
    val intervalAfter: Double = 0.0,
    val wasNew: Boolean = false,
    val lapsesAfter: Int = 0,
    /** Human-readable mistakes (e.g. wrong reading, wrong kanji). */
    val mistakes: List<String> = emptyList(),
    /** Writing accuracy 0..1 when the activity was a writing attempt. */
    val writingAccuracy: Float? = null,
    /** Exam id when this review was part of an exam. */
    val examId: String = "",
    val sessionId: String = ""
) {
    val correct: Boolean get() = rating != ReviewRating.Again
}

/** One stroke attempt during writing practice. */
@Serializable
data class StrokeAttempt(
    val strokeIndex: Int,
    val correct: Boolean,
    val deviation: Float = 0f,
    val mistake: String = ""
)

/** A persisted writing attempt with per-stroke evaluation. */
@Serializable
data class WritingAttemptEvent(
    val id: String,
    val cardId: String,
    val noteId: String,
    val deckId: String,
    val attempted: String,
    val expected: String,
    val strokes: List<StrokeAttempt> = emptyList(),
    val accuracy: Float,
    val mistakeCount: Int = 0,
    val completed: Boolean,
    val durationMs: Long = 0,
    val attemptedAt: Instant = Clock.System.now()
) {
    val correct: Boolean get() = accuracy >= 0.99f
}

/** One answered exam question (kept for analytics). */
@Serializable
data class ExamQuestionResult(
    val questionId: String,
    val cardId: String,
    val noteId: String,
    val questionType: String,
    val correct: Boolean,
    val answer: String = "",
    val correctAnswer: String = "",
    val confidence: Int = 0,
    val responseTimeMs: Long = 0,
    val jlpt: Int? = null,
    val category: String = "",
    /** Exam section label (e.g. "Vocabulary", "Grammar", "Reading"). */
    val section: String = ""
)

/** A completed exam. */
@Serializable
data class ExamResult(
    val id: String,
    val title: String,
    val examType: String,
    val startedAt: Instant,
    val finishedAt: Instant,
    val questionCount: Int,
    val correctCount: Int,
    val skippedCount: Int = 0,
    val timeLimitMs: Long = 0,
    val deckId: String = "",
    val jlpt: Int? = null,
    val weekly: Boolean = false,
    val questions: List<ExamQuestionResult> = emptyList()
) {
    val score: Float get() = if (questionCount == 0) 0f else correctCount.toFloat() / questionCount
    val percentage: Int get() = (score * 100).toInt()
    val durationMs: Long get() = (finishedAt - startedAt).inWholeMilliseconds

    /** Recognition vs production split — drives the study/exam gap insight. */
    val recognitionQuestions: List<ExamQuestionResult>
        get() = questions.filter { it.questionType.contains("recognition", ignoreCase = true) }
    val productionQuestions: List<ExamQuestionResult>
        get() = questions.filter { it.questionType.contains("production", ignoreCase = true) }

    fun accuracyOf(results: List<ExamQuestionResult>): Float {
        if (results.isEmpty()) return 0f
        return results.count { it.correct }.toFloat() / results.size
    }

    val recognitionAccuracy: Float get() = accuracyOf(recognitionQuestions)
    val productionAccuracy: Float get() = accuracyOf(productionQuestions)
    val writingAccuracy: Float
        get() = accuracyOf(questions.filter { it.questionType.contains("writing", ignoreCase = true) })
}

/** A study session record (start/end, counts, resume support). */
@Serializable
data class StudySessionRecord(
    val id: String,
    val startedAt: Instant,
    val finishedAt: Instant? = null,
    val deckId: String = "",
    val mode: StudyMode = StudyMode.Flashcards,
    val cardsSeen: Int = 0,
    val cardsCompleted: Int = 0,
    val correctCount: Int = 0,
    val againCount: Int = 0,
    val newCards: Int = 0,
    val reviewCards: Int = 0,
    val interrupted: Boolean = false,
    val lastCardId: String = ""
)

// ============================================
// ID HELPERS — stable, deterministic, dedupable
// ============================================

object LearningIds {
    /** Stable card id: note id + card type, so regeneration never duplicates. */
    fun cardId(noteId: String, cardType: CardType, deckId: String): String =
        "${noteId}::${cardType.name}::${deckId.hashCode().toUInt().toString(16)}"

    fun eventId(prefix: String): String = "$prefix-${Clock.System.now().toEpochMilliseconds()}-${(Math.random() * 9999).toInt()}"

    fun noteId(kind: LearningItemKind, expression: String, reading: String = ""): String =
        "${kind.name.lowercase()}:${expression}${if (reading.isNotBlank()) ":$reading" else ""}"
}

/** Serialized in one file so stats, exams and sessions survive restarts. */
@Serializable
data class LearningSnapshot(
    val notes: List<LearningNote> = emptyList(),
    val cards: List<NoteCard> = emptyList(),
    val deckConfigs: Map<String, DeckStudyConfig> = emptyMap(),
    val reviewEvents: List<LearningReviewEvent> = emptyList(),
    val writingAttempts: List<WritingAttemptEvent> = emptyList(),
    val examResults: List<ExamResult> = emptyList(),
    val sessions: List<StudySessionRecord> = emptyList(),
    val revision: Long = 0
)

/** Convenience: review rating correctness helper shared by engines. */
internal fun ReviewRating.isPass(): Boolean = this != ReviewRating.Again

internal fun Duration?.orZero(): Duration = this ?: kotlin.time.Duration.ZERO
