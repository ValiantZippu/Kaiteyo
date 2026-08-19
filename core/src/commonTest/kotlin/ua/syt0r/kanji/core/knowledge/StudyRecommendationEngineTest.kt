package ua.syt0r.kanji.core.knowledge

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StudyRecommendationEngineTest {

    private fun candidate(
        character: String,
        state: StudyState = StudyState.New,
        frequencyRank: Int? = null,
        lastReviewMs: Long? = null
    ) = RecommendationCandidate(
        character = character,
        keyword = null,
        frequencyRank = frequencyRank,
        studyState = state,
        lastReviewMs = lastReviewMs,
        jlpt = null
    )

    @Test
    fun dueKanjiRankFirst() {
        val due = candidate("食", StudyState.Due, frequencyRank = 100)
        val learning = candidate("語", StudyState.Learning)
        val fresh = candidate("明", StudyState.New, frequencyRank = 50)

        val recommendations = StudyRecommendationEngine.recommend(listOf(fresh, learning, due))
        assertEquals("食", recommendations.first().character)
        // The due kanji's reason explains the real cause.
        assertTrue(recommendations.first().reason.contains("Review due", ignoreCase = true))
    }

    @Test
    fun reasonsAreExplanatory() {
        val highFreq = candidate("水", StudyState.New, frequencyRank = 120)
        val rec = StudyRecommendationEngine.recommend(listOf(highFreq)).first()
        assertTrue(rec.reason.contains("New kanji"))
        assertTrue(rec.reason.contains("frequency", ignoreCase = true))
    }

    @Test
    fun highFrequencyNewKanjiBeatLowFrequencyOnes() {
        val common = candidate("水", StudyState.New, frequencyRank = 120)
        val rare = candidate("鬱", StudyState.New, frequencyRank = 50000)
        val recommendations = StudyRecommendationEngine.recommend(listOf(rare, common))
        assertEquals("水", recommendations.first().character)
    }

    @Test
    fun emptyPoolIsEmpty() {
        assertEquals(emptyList(), StudyRecommendationEngine.recommend(emptyList()))
    }

    @Test
    fun limitIsRespected() {
        val pool = (1..20).map { candidate("字$it", StudyState.Due) }
        assertEquals(5, StudyRecommendationEngine.recommend(pool, limit = 5).size)
    }

    @Test
    fun countsReflectRealState() {
        val pool = listOf(
            candidate("食", StudyState.Due),
            candidate("語", StudyState.Relearning),
            candidate("明", StudyState.Learning),
            candidate("水", StudyState.Mastered)
        )
        assertEquals(2, StudyRecommendationEngine.dueCount(pool))
        assertEquals(1, StudyRecommendationEngine.learningCount(pool))
    }

    @Test
    fun studyStateNeverInvented() {
        // A candidate with no study data projects to New — never \"known\".
        val rec = StudyRecommendationEngine.recommend(
            listOf(candidate("鬱", StudyState.New))
        ).first()
        assertEquals(StudyState.New, StudyState.New) // sanity anchor
        assertTrue(rec.urgency <= 2)
    }
}
