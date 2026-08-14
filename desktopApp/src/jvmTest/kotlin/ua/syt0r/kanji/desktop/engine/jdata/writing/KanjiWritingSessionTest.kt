package ua.syt0r.kanji.desktop.engine.jdata.writing

import ua.syt0r.kanji.desktop.engine.jdata.api.LanguageDatabase
import ua.syt0r.kanji.desktop.engine.jdata.engine.WritingStrictness
import ua.syt0r.kanji.desktop.engine.jdata.model.KanjiEntry
import ua.syt0r.kanji.desktop.engine.jdata.model.PlatformData
import ua.syt0r.kanji.desktop.engine.jdata.model.SourceRef
import ua.syt0r.kanji.desktop.engine.jdata.model.StableIds
import ua.syt0r.kanji.desktop.engine.jdata.model.StrokeEntry
import ua.syt0r.kanji.desktop.engine.jdata.model.StrokeSet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Integration test for [KanjiWritingSession] — the real consumer path:
 * PlatformData -> LanguageDatabase -> WritingSession -> attempt evaluation.
 */
class KanjiWritingSessionTest {

    private fun platform(): PlatformData {
        val kanjiId = StableIds.kanji("食")
        val strokeId = StableIds.strokeSet("食")
        return PlatformData(
            schemaVersion = 1,
            generatedAt = "test",
            kanji = mapOf(kanjiId to KanjiEntry(id = kanjiId, character = "食", strokeCount = 2)),
            kana = emptyMap(),
            vocab = emptyMap(),
            radicals = emptyMap(),
            components = emptyMap(),
            strokeSets = mapOf(
                strokeId to StrokeSet(
                    character = "食",
                    strokeCount = 2,
                    strokes = listOf(
                        StrokeEntry(index = 0, path = "M0,0 L100,0"),
                        StrokeEntry(index = 1, path = "M0,0 L0,100")
                    ),
                    source = SourceRef("kanjivg", "98df")
                )
            ),
            relations = emptyList(),
            sources = emptyMap()
        )
    }

    @Test
    fun beginLoadsStrokeDataAndSubmitEvaluates() {
        val writer = KanjiWritingSession(LanguageDatabase.open(platform()))

        val session = assertNotNull(writer.begin("食"))
        assertEquals(2, session.strokeCount)
        assertEquals("kanjivg", session.reference)
        assertTrue(session.hasGeometry)

        val attempt = writer.submit(
            session,
            listOf("M0,0 L100,0", "M0,0 L0,100"),
            WritingStrictness.Exam
        )
        assertTrue(attempt.accepted)
        assertTrue(attempt.accuracyPercent() >= 99)
        assertTrue(attempt.summaryLines().any { it.contains("PASS") })
        assertEquals(2, attempt.perStrokeLabels().size)
    }

    @Test
    fun beginReturnsNullForUnknownCharacter() {
        val writer = KanjiWritingSession(LanguageDatabase.open(platform()))
        assertNull(writer.begin("あ"))
    }

    @Test
    fun liveStrokeGivesPerStrokeFeedback() {
        val writer = KanjiWritingSession(LanguageDatabase.open(platform()))
        val session = assertNotNull(writer.begin("食"))
        val feedback = writer.liveStroke(session, expectedIndex = 0, drawnPath = "M0,0 L100,0", WritingStrictness.Normal)
        assertNotNull(feedback)
        assertTrue(feedback.metrics.overallScore >= 0.99f)
    }

    @Test
    fun failedAttemptRendersFailVerdict() {
        val writer = KanjiWritingSession(LanguageDatabase.open(platform()))
        val session = assertNotNull(writer.begin("食"))
        val attempt = writer.submit(
            session,
            listOf("M0,0 L0,100", "M0,0 L100,0"),
            WritingStrictness.Exam
        )
        assertFalse(attempt.accepted)
        assertTrue(attempt.summaryLines().any { it.contains("FAIL") })
        assertTrue(attempt.summaryLines().any { it.contains("Wrong order") })
    }
}
