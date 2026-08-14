package ua.syt0r.kanji.core.statistics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for [ExamGenerator]: deterministic output for a fixed seed, valid
 * multiple-choice options, JLPT/content-type filtering, production questions
 * and graceful pool exhaustion.
 */
class ExamGeneratorTest {

    private val generator = ExamGenerator()

    private fun kanjiItem(
        key: String,
        jlpt: Int? = 5,
        radical: String? = null,
        strokeCount: Int? = null,
        reading: String = "${key}reading",
        meaning: String = "${key} meaning"
    ) = ExamSourceItem(
        key = key,
        content = key,
        reading = reading,
        meaning = meaning,
        jlptLevel = jlpt,
        contentType = ContentTypes.KANJI,
        radical = radical,
        strokeCount = strokeCount,
        studied = true
    )

    private fun vocabItem(key: String, jlpt: Int? = 5) = ExamSourceItem(
        key = key,
        content = "${key}言葉",
        reading = "${key}よみ",
        meaning = "meaning of $key",
        jlptLevel = jlpt,
        contentType = ContentTypes.VOCAB,
        studied = true
    )

    private fun kanjiPool(size: Int): List<ExamSourceItem> =
        (1..size).map { kanjiItem("漢$it") }

    @Test
    fun sameSeedProducesIdenticalExam() {
        val items = kanjiPool(30) + (1..20).map { vocabItem("語$it") }
        val first = generator.generate(ExamConfig(questionCount = 20, seed = 42), items)
        val second = generator.generate(ExamConfig(questionCount = 20, seed = 42), items)

        assertEquals(first.questions.size, second.questions.size)
        first.questions.zip(second.questions).forEach { (a, b) ->
            assertEquals(a.type, b.type)
            assertEquals(a.prompt, b.prompt)
            assertEquals(a.answer, b.answer)
            assertEquals(a.options, b.options)
        }
    }

    @Test
    fun multipleChoiceOptionsAreValid() {
        val exam = generator.generate(ExamConfig(questionCount = 20, seed = 7), kanjiPool(40))
        val mc = exam.questions.filter { it.options != null }
        assertTrue(mc.isNotEmpty(), "expected multiple-choice questions")
        mc.forEach { q ->
            val options = q.options!!
            assertEquals(4, options.size)
            assertEquals(4, options.distinct().size, "options must be distinct")
            assertTrue(options.contains(q.answer), "correct answer must be among options")
            assertTrue(options.all { it.isNotBlank() })
            assertTrue(q.prompt != q.answer)
        }
    }

    @Test
    fun productionQuestionsHaveNoOptions() {
        val exam = generator.generate(
            ExamConfig(questionCount = 20, seed = 7, includeProduction = true),
            kanjiPool(40)
        )
        val production = exam.questions.filter { !it.type.isMultipleChoice }
        assertTrue(production.isNotEmpty())
        production.forEach { assertTrue(it.options == null) }
    }

    @Test
    fun jlptFilterRestrictsPool() {
        val items = kanjiPool(10) + (11..20).map { kanjiItem("漢$it", jlpt = 4) }
        val exam = generator.generate(
            ExamConfig(questionCount = 10, jlptLevel = 4, seed = 3),
            items
        )
        assertTrue(exam.questions.isNotEmpty())
        exam.questions.forEach { assertEquals(4, it.jlptLevel) }
    }

    @Test
    fun contentTypeFilterRestrictsPool() {
        val items = kanjiPool(10) + (1..20).map { vocabItem("語$it") }
        val exam = generator.generate(
            ExamConfig(questionCount = 10, contentType = ContentTypes.VOCAB, seed = 3),
            items
        )
        assertTrue(exam.questions.isNotEmpty())
        exam.questions.forEach {
            assertTrue(it.type.contentType == ContentTypes.VOCAB)
        }
    }

    @Test
    fun itemsWithoutMeaningOrReadingAreExcluded() {
        val broken = ExamSourceItem(
            key = "x", content = "x", reading = "", meaning = "",
            jlptLevel = 5, contentType = ContentTypes.KANJI, studied = true
        )
        val exam = generator.generate(
            ExamConfig(questionCount = 5, seed = 1),
            listOf(broken) + kanjiPool(20)
        )
        assertTrue(exam.questions.isNotEmpty())
        exam.questions.forEach { assertTrue(it.entityKey != "x") }
    }

    @Test
    fun emptyOrUnstudiedPoolYieldsNoQuestions() {
        val unstudied = kanjiItem("日").copy(studied = false)
        val empty = generator.generate(ExamConfig(questionCount = 10, seed = 1), emptyList())
        val unstudiedOnly = generator.generate(ExamConfig(questionCount = 10, seed = 1), listOf(unstudied))
        assertTrue(empty.questions.isEmpty())
        assertTrue(unstudiedOnly.questions.isEmpty())
        assertEquals(0, empty.exam.questionCount)
    }

    @Test
    fun tinyPoolProducesFewerQuestionsGracefully() {
        val exam = generator.generate(ExamConfig(questionCount = 20, seed = 1), kanjiPool(3))
        assertTrue(exam.questions.size <= 20)
        // Distractors are scarce with only 3 items — questions may be sparse
        // but every generated question must still be valid.
        exam.questions.filter { it.options != null }.forEach { q ->
            assertEquals(4, q.options!!.size)
        }
    }

    @Test
    fun scopeJsonRecordsQuestionTypes() {
        val exam = generator.generate(ExamConfig(questionCount = 15, seed = 9), kanjiPool(40))
        val scopeJson = exam.exam.scopeJson
        assertNotNull(scopeJson)
        exam.questions.map { it.type.name }.distinct().forEach { type ->
            assertTrue(scopeJson.contains(type), "scope must record question type $type")
        }
        assertTrue(scopeJson.contains("\"jlptLevel\""))
    }

    @Test
    fun examHeaderCarriesTypeAndSeed() {
        val exam = generator.generate(
            ExamConfig(title = "Kanji Drill", questionCount = 10, contentType = ContentTypes.KANJI, seed = 5),
            kanjiPool(30)
        )
        assertEquals("Kanji Drill", exam.exam.title)
        assertEquals("kanji", exam.exam.examType)
        assertEquals(5, exam.exam.seed)
        assertEquals(exam.questions.size, exam.exam.questionCount)
    }
}
