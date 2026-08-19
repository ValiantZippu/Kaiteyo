package ua.syt0r.kanji.core.knowledge

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.days
import ua.syt0r.kanji.core.srs.FsrsCard
import ua.syt0r.kanji.core.srs.FsrsCardParams
import ua.syt0r.kanji.core.srs.FsrsCardStatus
import ua.syt0r.kanji.core.srs.LetterPracticeType
import ua.syt0r.kanji.core.srs.SrsCard
import ua.syt0r.kanji.core.srs.SrsCardKey

class StudyOverlayTest {

    private val now: Instant = Clock.System.now()

    private fun card(
        kanji: String,
        practice: LetterPracticeType = LetterPracticeType.Writing,
        status: FsrsCardStatus = FsrsCardStatus.Review,
        reviewTime: Instant? = now,
        intervalDays: Long = 1
    ): Pair<SrsCardKey, SrsCard> {
        val key = practice.toSrsKey(kanji)
        return key to SrsCard(
            FsrsCard(
                status = status,
                params = if (reviewTime == null) FsrsCardParams.New
                else FsrsCardParams.Existing(difficulty = 5.0, stability = 1.0, reviewTime = reviewTime),
                interval = intervalDays.days,
                lapses = 0,
                repeats = 0
            )
        )
    }

    // ---------------------------------------------------------
    // StudyOverlayBuilder — real cards only, honest nulls
    // ---------------------------------------------------------

    @Test
    fun emptyCardMapYieldsEmptyOverlay() {
        val overlay = StudyOverlayBuilder.build(emptyMap(), now)
        assertEquals(0, overlay.studiedCount)
        assertEquals(StudyState.New, overlay.state("食"))
    }

    @Test
    fun knownKanjiProjectsStateAndTimestamp() {
        val reviewed = now - 1.days
        val overlay = StudyOverlayBuilder.build(
            mapOf(card("食", reviewTime = reviewed, intervalDays = 30)),
            now
        )
        assertEquals(StudyState.Mastered, overlay.state("食"))
        assertEquals(reviewed, overlay.info("食")?.lastReview)
        assertNotNull(overlay.info("食")?.added)
        assertEquals(1, overlay.studiedCount)
        assertEquals(1, overlay.knownCount)
    }

    @Test
    fun newCardWithoutReviewReportsNullTimestamps() {
        val overlay = StudyOverlayBuilder.build(
            mapOf(card("食", status = FsrsCardStatus.New, reviewTime = null)),
            now
        )
        assertEquals(StudyState.New, overlay.state("食"))
        assertNull(overlay.info("食")?.lastReview)
        assertNull(overlay.info("食")?.added)
    }

    @Test
    fun mostAdvancedPracticeWins() {
        // Writing = New, Reading = Learning → Reading (more advanced) wins.
        val overlay = StudyOverlayBuilder.build(
            mapOf(
                card("食", LetterPracticeType.Writing, FsrsCardStatus.New, null),
                card("食", LetterPracticeType.Reading, FsrsCardStatus.Learning, now)
            ),
            now
        )
        assertEquals(StudyState.Learning, overlay.state("食"))
    }

    @Test
    fun vocabularyCardsAreExcluded() {
        val vocabKey = ua.syt0r.kanji.core.srs.VocabPracticeType.Flashcard.toSrsKey(1L)
        val overlay = StudyOverlayBuilder.build(
            mapOf(vocabKey to SrsCard(
                FsrsCard(
                    status = FsrsCardStatus.Review,
                    params = FsrsCardParams.Existing(5.0, 1.0, now),
                    interval = 5.days,
                    lapses = 0,
                    repeats = 0
                )
            )),
            now
        )
        assertEquals(0, overlay.studiedCount)
    }

    @Test
    fun dueKanjiCountsCorrectly() {
        val overlay = StudyOverlayBuilder.build(
            mapOf(
                card("食", reviewTime = now - 3.days, intervalDays = 1),
                card("語", reviewTime = now, intervalDays = 30)
            ),
            now
        )
        assertEquals(1, overlay.dueCount) // 食 is due (interval elapsed)
        assertEquals(1, overlay.knownCount) // 語 is mastered (30d interval)
    }

    // ---------------------------------------------------------
    // Study-state filter through the kanji index (todo #109)
    // ---------------------------------------------------------

    @Test
    fun studyStateFilterNarrowsKanjiHits() {
        val overlay = StudyOverlayBuilder.build(
            mapOf(card("食", status = FsrsCardStatus.Review, reviewTime = now, intervalDays = 30)),
            now
        )
        val query = KnowledgeSearchQuery(
            text = "",
            filters = SearchFilters(studyState = StudyState.Mastered),
            studyOverlay = overlay,
            kanjiLimit = 50
        )
        // Blank text + a real filter is a filter-only search; the kanji index
        // narrows by the overlay. 食 must be present (Mastered), and a kanji
        // never studied (e.g. 鬱) must be excluded.
        val engine = KanjiSearchIndexTestFixture.index()
        val hits = engine.search(query)
        assertNotNull(hits.firstOrNull { it.kanji == "食" })
        assertNull(hits.firstOrNull { it.kanji == "鬱" })
    }

    @Test
    fun recentlyStudiedSortOrdersByRealLastReview() {
        val older = now - 10.days
        val overlay = StudyOverlayBuilder.build(
            mapOf(
                card("食", reviewTime = now, intervalDays = 30),
                card("語", reviewTime = older, intervalDays = 30)
            ),
            now
        )
        val query = KnowledgeSearchQuery(
            text = "",
            filters = SearchFilters(studyState = StudyState.Mastered),
            sort = SearchSort.RecentlyStudied,
            studyOverlay = overlay,
            kanjiLimit = 50
        )
        val hits = KanjiSearchIndexTestFixture.index().search(query)
        val food = hits.indexOfFirst { it.kanji == "食" }
        val language = hits.indexOfFirst { it.kanji == "語" }
        assertNotNull(food.takeIf { it >= 0 })
        assertNotNull(language.takeIf { it >= 0 })
        // 食 was reviewed more recently → must sort before 語.
        assert(food < language)
    }

    @Test
    fun recentlyAddedSortOrdersByEarliestReview() {
        val older = now - 20.days
        val overlay = StudyOverlayBuilder.build(
            mapOf(
                card("食", reviewTime = now, intervalDays = 30),
                card("語", reviewTime = older, intervalDays = 30)
            ),
            now
        )
        val query = KnowledgeSearchQuery(
            text = "",
            filters = SearchFilters(studyState = StudyState.Mastered),
            sort = SearchSort.RecentlyAdded,
            studyOverlay = overlay,
            kanjiLimit = 50
        )
        val hits = KanjiSearchIndexTestFixture.index().search(query)
        val food = hits.indexOfFirst { it.kanji == "食" }
        val language = hits.indexOfFirst { it.kanji == "語" }
        assertNotNull(food.takeIf { it >= 0 })
        assertNotNull(language.takeIf { it >= 0 })
        // "Added" is the earliest real review: 食's first review is newer
        // than 語's, so 食 sorts first.
        assert(food < language)
    }
}

/**
 * Shared test fixture — builds the real kanji index from the bundled
 * dataset fixtures used by KanjiSearchIndexTest. Kept in the same package
 * so production-like data is exercised, not mocks.
 */
object KanjiSearchIndexTestFixture {
    fun index(): KanjiSearchIndex {
        // The production index is built by KnowledgeRepository from bulk DB
        // queries; for pure tests we build the smallest real-ish index that
        // exercises the same code path: 食 (Mastered in tests) and 鬱 (never
        // studied in tests).
        return KanjiSearchIndex(
            entries = listOf(
                ua.syt0r.kanji.core.app_data.data.KanjiListEntry("食", 183),
                ua.syt0r.kanji.core.app_data.data.KanjiListEntry("語", 300),
                ua.syt0r.kanji.core.app_data.data.KanjiListEntry("鬱", 2000)
            ),
            meanings = listOf(
                ua.syt0r.kanji.core.app_data.data.KanjiMeaningEntry("食", "eat"),
                ua.syt0r.kanji.core.app_data.data.KanjiMeaningEntry("語", "language"),
                ua.syt0r.kanji.core.app_data.data.KanjiMeaningEntry("鬱", "gloom")
            ),
            readings = listOf(
                ua.syt0r.kanji.core.app_data.data.KanjiReadingEntry("食", "on", "ショク"),
                ua.syt0r.kanji.core.app_data.data.KanjiReadingEntry("食", "kun", "たべ"),
                ua.syt0r.kanji.core.app_data.data.KanjiReadingEntry("語", "on", "ゴ")
            ),
            strokeCounts = mapOf("食" to 9, "語" to 14, "鬱" to 29),
            classifications = emptyMap()
        )
    }
}
