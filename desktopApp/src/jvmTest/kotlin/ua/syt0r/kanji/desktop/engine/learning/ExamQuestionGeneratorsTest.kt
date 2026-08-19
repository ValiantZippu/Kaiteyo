package ua.syt0r.kanji.desktop.engine.learning

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExamQuestionGeneratorsTest {

    private val items = listOf(
        ExamVocabItem("学校", "がっこう", "school"),
        ExamVocabItem("大学", "だいがく", "university"),
        ExamVocabItem("先生", "せんせい", "teacher"),
        ExamVocabItem("食べる", "たべる", "to eat"),
        ExamVocabItem("見る", "みる", "to see"),
        ExamVocabItem("日本語", "にほんご", "Japanese language")
    )

    @Test
    fun meaningMatchingProducesOneQuestionPerItemWithDistractors() {
        val questions = ExamQuestionGenerators.meaningMatching(items, seed = 1)
        assertEquals(items.size, questions.size)
        questions.forEach { q ->
            assertEquals(GeneratorQuestionType.Matching, q.type)
            assertEquals(4, q.options.size)
            assertTrue(q.correct.first() in q.options)
            assertEquals(q.options.distinct().size, q.options.size)
        }
    }

    @Test
    fun readingMatchingSkipsItemsWithoutReadings() {
        val withBlank = items + ExamVocabItem("謎", "", "mystery")
        val questions = ExamQuestionGenerators.readingMatching(withBlank, seed = 2)
        assertEquals(items.size, questions.size) // the blank-reading item is skipped
        assertTrue(questions.none { it.prompt == "謎" })
    }

    @Test
    fun kanjiClozeBlanksOneKanjiAndOffersAnswer() {
        val questions = ExamQuestionGenerators.kanjiCloze(items, seed = 3)
        assertTrue(questions.isNotEmpty())
        val q = questions.first { it.prompt.contains("＿") }
        assertEquals(GeneratorQuestionType.Cloze, q.type)
        assertTrue(q.correct.first().single() in q.options)
        // The blanked character equals the correct answer.
        assertEquals(1, q.prompt.count { it == '＿' })
    }

    @Test
    fun compoundOrderingAnswersMatchContextWord() {
        val questions = ExamQuestionGenerators.compoundOrdering(items, seed = 5)
        assertTrue(questions.isNotEmpty())
        val q = questions.first()
        assertEquals(GeneratorQuestionType.Ordering, q.type)
        // Re-arranging the shuffled kanji into the correct order spells the word.
        assertEquals(q.correct, q.options.sortedBy { q.correct.indexOf(it) })
        // The correct answer's kanji sequence equals the context word's kanji.
        assertEquals(q.correct.joinToString(""), q.context.filter { it.code in 0x4E00..0x9FFF })
    }

    @Test
    fun freeResponseAcceptsExpressionOrReading() {
        val questions = ExamQuestionGenerators.freeResponse(items)
        val school = questions.first { it.prompt == "school" }
        assertTrue("学校" in school.correct)
        assertTrue("がっこう" in school.correct)
    }

    @Test
    fun timedWrapperStampsTimeLimit() {
        val timed = ExamQuestionGenerators.timed(
            ExamQuestionGenerators.meaningMatching(items.take(3), seed = 7),
            secondsPerQuestion = 30
        )
        timed.forEach { q ->
            assertEquals(GeneratorQuestionType.Timed, q.type)
            assertEquals(30, q.timeLimitSeconds)
        }
    }

    @Test
    fun mixedExamIsDeterministic() {
        val a = ExamQuestionGenerators.mixedExam(items, seed = 23)
        val b = ExamQuestionGenerators.mixedExam(items, seed = 23)
        assertEquals(a, b)
        assertTrue(a.any { it.type == GeneratorQuestionType.Matching })
        assertTrue(a.any { it.type == GeneratorQuestionType.Cloze })
        assertTrue(a.any { it.type == GeneratorQuestionType.Ordering })
        assertTrue(a.any { it.type == GeneratorQuestionType.FreeResponse })
    }
}
