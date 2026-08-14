package ua.syt0r.kanji.core.statistics

import kotlinx.datetime.Clock

// ============================================================
// WEEKLY EXAM
// A recurring examination concept: generate an exam from the
// items actually studied during the trailing 7 days. The exam
// still flows through the regular [ExamGenerator] so question
// safety rules (valid distractors, unambiguous prompts) apply.
// ============================================================

object WeeklyExam {

    /** Default config: 20 questions, mixed content, last 7 days, reproducible seed. */
    fun config(
        questionCount: Int = 20,
        includeProduction: Boolean = true,
        seed: Long = Clock.System.now().toEpochMilliseconds()
    ): ExamConfig = ExamConfig(
        title = "Weekly Exam",
        questionCount = questionCount,
        includeProduction = includeProduction,
        seed = seed,
        studiedWithinDays = 7
    )

    /** What the weekly exam would be built from — shown in the preview. */
    data class WeeklyExamSummary(
        val kanjiStudied: Int = 0,
        val vocabStudied: Int = 0,
        val byJlpt: List<Pair<Int, Int>> = emptyList()
    ) {
        val total: Int get() = kanjiStudied + vocabStudied
        val hasContent: Boolean get() = total > 0
    }

    /**
     * Summarizes the eligible source items (already filtered to the last
     * 7 days by the caller). JLPT bands with zero items are omitted.
     */
    fun summarize(items: List<ExamSourceItem>): WeeklyExamSummary {
        val kanji = items.count { it.contentType == ContentTypes.KANJI }
        val vocab = items.count { it.contentType == ContentTypes.VOCAB }
        val byJlpt = items
            .mapNotNull { it.jlptLevel }
            .groupingBy { it }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
        return WeeklyExamSummary(kanjiStudied = kanji, vocabStudied = vocab, byJlpt = byJlpt)
    }
}
