package ua.syt0r.kanji.core.statistics

import kotlinx.serialization.Serializable

// ============================================
// EXAMINATION SYSTEM MODELS
// ============================================

/** The kind of question an exam item asks. */
enum class ExamQuestionType(
    val displayName: String,
    val skill: String,
    val isMultipleChoice: Boolean,
    val contentType: String
) {
    KanjiToMeaning("Kanji → Meaning", "meaning", true, ContentTypes.KANJI),
    MeaningToKanji("Meaning → Kanji", "recognition", true, ContentTypes.KANJI),
    KanjiToReading("Kanji → Reading", "reading", true, ContentTypes.KANJI),
    ReadingToKanji("Reading → Kanji", "recognition", true, ContentTypes.KANJI),
    VocabToMeaning("Vocab → Meaning", "meaning", true, ContentTypes.VOCAB),
    MeaningToVocab("Meaning → Vocab", "recognition", true, ContentTypes.VOCAB),
    VocabToReading("Vocab → Reading", "reading", true, ContentTypes.VOCAB),
    ReadingToVocab("Reading → Vocab", "recognition", true, ContentTypes.VOCAB),
    RadicalToKanji("Radical → Kanji", "recognition", true, ContentTypes.KANJI),
    KanjiToRadical("Kanji → Radical", "recognition", true, ContentTypes.KANJI),
    StrokeCount("Stroke count", "recognition", true, ContentTypes.KANJI),
    ProductionReading("Write the reading", "production", false, ContentTypes.KANJI),
    ProductionKanji("Write the kanji", "production", false, ContentTypes.KANJI),
    ProductionVocabReading("Write the reading", "production", false, ContentTypes.VOCAB),
    ProductionVocabKanji("Write the word", "production", false, ContentTypes.VOCAB)
}

/** Exam builder configuration. */
@Serializable
data class ExamConfig(
    val title: String = "Weekly Exam",
    val questionCount: Int = 20,
    val jlptLevel: Int? = null,
    val contentType: String? = null,
    val includeProduction: Boolean = true,
    val timeLimitMs: Long? = null,
    val seed: Long = 0,
    /** Restrict to items studied within this window (null = all time). */
    val studiedWithinDays: Int? = null
)

/** A single language item the exam can be built from. */
data class ExamSourceItem(
    val key: String,
    val content: String,
    val reading: String,
    val meaning: String,
    val jlptLevel: Int?,
    val contentType: String,
    val radical: String? = null,
    val strokeCount: Int? = null,
    val studied: Boolean = true
)

/** A generated question, ready to be persisted. */
data class GeneratedExamQuestion(
    val type: ExamQuestionType,
    val prompt: String,
    val answer: String,
    val options: List<String>?,
    val entityKey: String,
    val jlptLevel: Int?
)

/** A generated exam = header + ordered questions. */
data class GeneratedExam(
    val exam: ExamRecord,
    val questions: List<GeneratedExamQuestion>
)

/** Result of scoring one answer. */
data class ExamAnswerResult(
    val isCorrect: Boolean,
    val normalizedUserAnswer: String,
    val mistakeCategory: String
)

/** Fully graded exam (used by the results screen). */
data class GradedExam(
    val exam: ExamRecord,
    val questions: List<ExamQuestionRecord>,
    val score: Int,
    val accuracy: Float
)
