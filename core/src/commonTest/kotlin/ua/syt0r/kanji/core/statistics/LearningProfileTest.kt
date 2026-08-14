package ua.syt0r.kanji.core.statistics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LearningProfileTest {

    private fun knowledge(type: String, studied: Int, learned: Int) = ContentTypeKnowledge(
        contentType = type,
        studied = studied,
        learned = learned
    )

    private fun session(mode: String) = StudySessionRecord(
        startTime = kotlinx.datetime.Instant.fromEpochMilliseconds(0),
        mode = mode,
        itemsStudied = 1,
        correct = 1
    )

    @Test
    fun contentStrengthComesFromLearnedRatio() {
        val profile = ProfileCalculator.build(
            contentKnowledge = listOf(
                knowledge(ContentTypes.KANJI, studied = 10, learned = 8),
                knowledge(ContentTypes.VOCAB, studied = 20, learned = 4)
            ),
            skillMatrix = emptyList(),
            mistakeCategories = emptyList(),
            studySessions = emptyList()
        )
        assertEquals(ContentTypes.KANJI, profile.strongestContentType)
        assertEquals(ContentTypes.VOCAB, profile.weakestContentType)
    }

    @Test
    fun skillsOnlyCountWhenActuallyMeasured() {
        val profile = ProfileCalculator.build(
            contentKnowledge = listOf(knowledge(ContentTypes.KANJI, 5, 5)),
            skillMatrix = listOf(
                SkillMatrixRow("Kanji", recognition = 0.9f, writing = 0.4f),
                SkillMatrixRow("Vocabulary", recognition = null, reading = null, writing = null)
            ),
            mistakeCategories = emptyList(),
            studySessions = emptyList()
        )
        assertEquals("Kanji", profile.bestSkill)
        assertEquals("Kanji writing", profile.weakestSkill)
    }

    @Test
    fun dominantModeComesFromSessions() {
        val profile = ProfileCalculator.build(
            contentKnowledge = emptyList(),
            skillMatrix = emptyList(),
            mistakeCategories = emptyList(),
            studySessions = listOf(session("writing"), session("writing"), session("reading"))
        )
        assertEquals("writing", profile.dominantStudyMode)
        assertEquals("writing" to 2L, profile.studyModeBalance.first())
    }

    @Test
    fun emptyDataProducesNoConclusionClaims() {
        val profile = ProfileCalculator.build(
            contentKnowledge = emptyList(),
            skillMatrix = emptyList(),
            mistakeCategories = emptyList(),
            studySessions = emptyList()
        )
        assertNull(profile.strongestContentType)
        assertNull(profile.bestSkill)
        assertTrue(profile.conclusion.contains("Study more"))
    }

    @Test
    fun weakestJlptBandPicksLowestStudiedRatio() {
        val profile = ProfileCalculator.build(
            contentKnowledge = listOf(
                ContentTypeKnowledge(
                    contentType = ContentTypes.KANJI,
                    jlptCoverage = listOf(
                        JlptCoverage(level = 5, total = 100, studied = 90),
                        JlptCoverage(level = 4, total = 100, studied = 10)
                    )
                )
            ),
            skillMatrix = emptyList(),
            mistakeCategories = emptyList(),
            studySessions = emptyList()
        )
        assertEquals(4, profile.weakestJlptBand)
    }

    @Test
    fun percentFormatting() {
        assertEquals("75%", ProfileCalculator.percent(0.75f))
        assertEquals("100%", ProfileCalculator.percent(1f))
    }
}
