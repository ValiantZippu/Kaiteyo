package ua.syt0r.kanji.desktop.engine.curriculum

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CurriculumEngineTest {

    /** Fake data source — tests drive study data directly. */
    private class FakeDataSource : CurriculumDataSource {
        val cardCounts = mutableMapOf<String, Int>()
        val reviewedCounts = mutableMapOf<String, Int>()
        var totalReviews = 0

        override fun deckName(deckId: String): String? = if (deckId in cardCounts) deckId else null
        override fun availableDeckIds(): Set<String> = cardCounts.keys.toSet()
        override fun cardCountInDeck(deckId: String): Int = cardCounts[deckId] ?: 0
        override fun reviewedCardCountInDeck(deckId: String): Int = reviewedCounts[deckId] ?: 0
        override fun totalReviewEvents(): Int = totalReviews
    }

    private fun tempStore(): CurriculumStore =
        CurriculumStore(File.createTempFile("kaiteyo-curriculum", ".json").apply { delete() })

    private fun engine(dataSource: FakeDataSource = FakeDataSource()): CurriculumEngine =
        CurriculumEngine(dataSource, tempStore())

    // ------------------------------------------------------------
    // Courses
    // ------------------------------------------------------------

    @Test
    fun exposesBuiltInCourses() {
        val e = engine()
        assertTrue(e.courses.size >= 3)
        val ids = e.courses.map { it.id }
        assertTrue("kana-foundation" in ids)
        assertTrue("jlpt-n5" in ids)
        assertTrue("jlpt-n4" in ids)
    }

    @Test
    fun startsOnFirstLesson() {
        val e = engine()
        e.startCourse("jlpt-n5")
        assertEquals("jlpt-n5", e.activeCourse()?.id)
        assertEquals("n5-kanji", e.activeLesson()?.id)
    }

    @Test
    fun startCourseIgnoresUnknownId() {
        val e = engine()
        e.startCourse("does-not-exist")
        assertNull(e.activeCourse())
    }

    @Test
    fun exitCourseReturnsToPicker() {
        val e = engine()
        e.startCourse("kana-foundation")
        e.exitCourse()
        assertNull(e.activeCourse())
    }

    // ------------------------------------------------------------
    // Objective progress
    // ------------------------------------------------------------

    @Test
    fun objectiveProgressReflectsDataSource() {
        val data = FakeDataSource()
        data.cardCounts["kanji-jlpt5"] = 30
        data.reviewedCounts["kanji-jlpt5"] = 12
        val e = engine(data)
        e.startCourse("jlpt-n5")

        val status = e.lessonStatus(e.activeLesson()!!)
        val newCards = status.objectives.first { it.objective.id == "n5-kanji-new" }
        assertEquals(30, newCards.progress)
        assertFalse(newCards.complete) // target is 80

        val reviews = status.objectives.first { it.objective.id == "n5-kanji-review" }
        assertEquals(12, reviews.progress)
        assertFalse(reviews.complete) // target is 40
    }

    @Test
    fun refreshMarksCompletedObjectives() {
        val data = FakeDataSource()
        data.cardCounts["kanji-jlpt5"] = 80
        val e = engine(data)
        e.startCourse("jlpt-n5")

        val completed = e.refresh()
        assertTrue(completed.any { it.id == "n5-kanji-new" })
        assertTrue(e.nextObjective()?.objective?.id != "n5-kanji-new")
    }

    @Test
    fun missingDeckObjectiveIsSatisfiedNotStalled() {
        val data = FakeDataSource()
        // No grammar decks installed — grammar objectives must not stall the course.
        data.cardCounts["kanji-jlpt5"] = 80
        data.reviewedCounts["kanji-jlpt5"] = 40
        data.cardCounts["vocab-jlpt5"] = 150
        data.reviewedCounts["vocab-jlpt5"] = 60
        val e = engine(data)
        e.startCourse("jlpt-n5")
        e.refresh()

        val grammar = e.lessonStatus(e.activeLesson()!!).objectives
        // The first two lessons complete; grammar lesson objectives auto-satisfy.
        assertTrue(e.nextObjective()?.objective?.id == "n5-grammar-new" || e.nextObjective() == null)
        // Unavailable-deck objectives report as complete.
        assertTrue(e.lessonStatus(e.courses.first { it.id == "jlpt-n5" }.lessons[2]).objectives.all { it.complete })
    }

    @Test
    fun totalReviewObjectiveUsesGlobalCount() {
        val data = FakeDataSource()
        data.totalReviews = 250
        val e = engine(data)
        e.startCourse("jlpt-n5")
        val milestone = e.courses.first { it.id == "jlpt-n5" }.lessons.last()
        val status = e.objectiveStatus(milestone.objectives.first())
        assertEquals(250, status.progress)
        assertTrue(status.complete) // target 200
    }

    // ------------------------------------------------------------
    // Lesson / course completion
    // ------------------------------------------------------------

    @Test
    fun advanceIfCompleteMovesToNextLesson() {
        val data = FakeDataSource()
        data.cardCounts["kanji-jlpt5"] = 80
        data.reviewedCounts["kanji-jlpt5"] = 40
        data.cardCounts["vocab-jlpt5"] = 150
        data.reviewedCounts["vocab-jlpt5"] = 60
        val e = engine(data)
        e.startCourse("jlpt-n5")
        e.refresh()

        val next = e.advanceIfComplete()
        assertEquals("n5-vocab", e.activeLesson()?.id)
        assertNotNull(next)
    }

    @Test
    fun advanceIfCompleteReturnsNullWhenLessonIncomplete() {
        val data = FakeDataSource()
        data.cardCounts["kanji-jlpt5"] = 5
        val e = engine(data)
        e.startCourse("jlpt-n5")
        e.refresh()
        assertNull(e.advanceIfComplete())
    }

    @Test
    fun courseCompletionTracksAllLessons() {
        val data = FakeDataSource()
        val e = engine(data)
        e.startCourse("kana-foundation")
        assertEquals(0f, e.courseCompletion(e.activeCourse()!!))
        // Complete hiragana + katakana objectives (46 cards each + 20 reviews each).
        data.cardCounts["kana-hiragana"] = 46
        data.reviewedCounts["kana-hiragana"] = 20
        data.cardCounts["kana-katakana"] = 46
        data.reviewedCounts["kana-katakana"] = 20
        data.cardCounts["kana-hiragana-advanced"] = 25
        data.cardCounts["kana-katakana-advanced"] = 25
        data.cardCounts["kana-full"] = 100
        data.reviewedCounts["kana-full"] = 40
        e.refresh()
        assertTrue(e.courseCompletion(e.activeCourse()!!) > 0.9f)
    }

    // ------------------------------------------------------------
    // Persistence
    // ------------------------------------------------------------

    @Test
    fun progressPersistsAcrossEngines() {
        val file = File.createTempFile("kaiteyo-curriculum-persist", ".json").apply { delete() }
        val store = CurriculumStore(file)
        val data = FakeDataSource()
        data.cardCounts["kanji-jlpt5"] = 80

        val first = CurriculumEngine(data, store)
        first.startCourse("jlpt-n5")
        first.refresh()

        // A fresh engine over the same store sees the completed objective.
        val second = CurriculumEngine(data, store)
        second.startCourse("jlpt-n5")
        assertTrue(second.lessonStatus(second.activeLesson()!!).objectives.first().complete)
        file.delete()
    }
}
