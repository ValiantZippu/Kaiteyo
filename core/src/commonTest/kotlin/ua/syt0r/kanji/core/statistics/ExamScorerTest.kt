package ua.syt0r.kanji.core.statistics

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for [ExamScorer]: multiple-choice exact matching and lenient
 * production (free-text) scoring with kana folding and punctuation/space
 * normalization, plus mistake-category classification.
 */
class ExamScorerTest {

    private fun question(
        type: ExamQuestionType,
        answer: String,
        options: List<String>? = null
    ) = ExamQuestionRecord(
        examId = 1,
        questionIndex = 0,
        questionType = type.name,
        prompt = "prompt",
        answer = answer,
        optionsJson = options?.let { Json.encodeToString(it) },
        userAnswer = null,
        isCorrect = null,
        timeMs = 0,
        entityKey = "食",
        skill = type.skill,
        jlptLevel = 5,
        mistakeCategory = null
    )

    // ---- Multiple choice ----

    @Test
    fun multipleChoiceExactMatchScoresCorrect() {
        val q = question(
            ExamQuestionType.KanjiToMeaning,
            answer = "to eat",
            options = listOf("to eat", "to drink", "to run", "to see")
        )
        val result = ExamScorer.score(q, "to eat")
        assertTrue(result.isCorrect)
        assertEquals("none", result.mistakeCategory)
    }

    @Test
    fun multipleChoiceWrongAnswerScoresIncorrect() {
        val q = question(
            ExamQuestionType.KanjiToMeaning,
            answer = "to eat",
            options = listOf("to eat", "to drink", "to run", "to see")
        )
        val result = ExamScorer.score(q, "to drink")
        assertFalse(result.isCorrect)
    }

    // ---- Production (free text) ----

    @Test
    fun katakanaAnswerAcceptsHiraganaInput() {
        val q = question(ExamQuestionType.KanjiToReading, answer = "スイ")
        assertTrue(ExamScorer.score(q, "すい").isCorrect)
        assertTrue(ExamScorer.score(q, "スイ").isCorrect)
    }

    @Test
    fun punctuationAndSpacesAreIgnored() {
        val q = question(ExamQuestionType.KanjiToMeaning, answer = "to eat")
        assertTrue(ExamScorer.score(q, "To Eat.").isCorrect)
        assertTrue(ExamScorer.score(q, "to eat, something").isCorrect)
    }

    @Test
    fun acceptedVariantsSplitByPipe() {
        val q = question(ExamQuestionType.KanjiToReading, answer = "すい|スイ|みず")
        assertTrue(ExamScorer.score(q, "みず").isCorrect)
        assertTrue(ExamScorer.score(q, "スイ").isCorrect)
        assertFalse(ExamScorer.score(q, "さん").isCorrect)
    }

    @Test
    fun longVowelMarkSurvivesNormalization() {
        val q = question(ExamQuestionType.VocabToReading, answer = "べんきょう")
        assertTrue(ExamScorer.score(q, "べんきょう").isCorrect)
    }

    @Test
    fun normalizeFoldsKanaAndStripsSeparators() {
        assertEquals("すい", ExamScorer.normalize("スイ"))
        assertEquals("toeat", ExamScorer.normalize("To eat."))
        assertEquals("べんきょう", ExamScorer.normalize("ベンキョウ"))
    }

    // ---- Mistake categories ----

    @Test
    fun readingQuestionsClassifyAsWrongReading() {
        val q = question(ExamQuestionType.VocabToReading, answer = "たべる")
        assertEquals("wrong_reading", ExamScorer.score(q, "はしる").mistakeCategory)
    }

    @Test
    fun meaningQuestionsClassifyAsWrongMeaning() {
        val q = question(ExamQuestionType.KanjiToMeaning, answer = "eat")
        assertEquals("wrong_meaning", ExamScorer.score(q, "drink").mistakeCategory)
    }

    @Test
    fun vocabQuestionsClassifyAsWrongMeaning() {
        val q = question(ExamQuestionType.MeaningToVocab, answer = "食べる")
        assertEquals("wrong_meaning", ExamScorer.score(q, "飲む").mistakeCategory)
    }

    @Test
    fun kanjiQuestionsClassifyAsWrongKanji() {
        val q = question(ExamQuestionType.ReadingToKanji, answer = "食")
        assertEquals("wrong_kanji", ExamScorer.score(q, "飲").mistakeCategory)
    }

    @Test
    fun radicalAndStrokeQuestionsHaveOwnCategories() {
        val radical = question(ExamQuestionType.KanjiToRadical, answer = "⻝")
        assertEquals("wrong_radical", ExamScorer.score(radical, "水").mistakeCategory)

        val stroke = question(ExamQuestionType.StrokeCount, answer = "9")
        assertEquals("wrong_stroke_count", ExamScorer.score(stroke, "4").mistakeCategory)
    }

    @Test
    fun unknownQuestionTypeFallsBackToUnknown() {
        val q = question(ExamQuestionType.ProductionKanji, answer = "食")
        assertEquals("unknown", ExamScorer.score(q, "水").mistakeCategory)
    }
}
