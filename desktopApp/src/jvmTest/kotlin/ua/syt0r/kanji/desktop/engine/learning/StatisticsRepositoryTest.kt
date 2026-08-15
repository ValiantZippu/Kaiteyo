package ua.syt0r.kanji.desktop.engine.learning

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import ua.syt0r.kanji.desktop.model.ReviewRating
import ua.syt0r.kanji.desktop.model.SrsStatus
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StatisticsRepositoryTest {

    private lateinit var dir: File
    private lateinit var store: LearningStore

    @BeforeTest
    fun setUp() {
        dir = Files.createTempDirectory("kaiteyo-stats-test").toFile()
        store = LearningStore(dir)
    }

    @AfterTest
    fun tearDown() {
        dir.deleteRecursively()
    }

    private fun addNote(expression: String, kind: LearningItemKind = LearningItemKind.Kanji, jlpt: Int? = 5): LearningNote {
        val note = LearningNote(
            id = LearningIds.noteId(kind, expression, "reading"),
            kind = kind,
            expression = expression,
            reading = "reading",
            meanings = listOf("meaning $expression"),
            jlpt = jlpt
        )
        store.upsertNote(note)
        return note
    }

    private fun addCard(note: LearningNote, deckId: String = "deck-a", status: SrsStatus = SrsStatus.New, intervalDays: Double = 0.0, dueAt: Instant? = null): NoteCard {
        val card = NoteCard(
            id = LearningIds.cardId(note.id, CardType.Recognition, deckId),
            noteId = note.id,
            cardType = CardType.Recognition,
            deckId = deckId,
            status = status,
            intervalDays = intervalDays,
            dueAt = dueAt
        )
        store.upsertCard(card)
        return card
    }

    private fun review(card: NoteCard, rating: ReviewRating, at: Instant, wasNew: Boolean = false) {
        store.recordReview(
            LearningReviewEvent(
                id = LearningIds.eventId("review"),
                cardId = card.id,
                noteId = card.noteId,
                deckId = card.deckId,
                cardType = card.cardType,
                activityType = StudyActivityType.Review,
                rating = rating,
                reviewedAt = at,
                statusBefore = if (wasNew) SrsStatus.New else SrsStatus.Review,
                statusAfter = if (rating == ReviewRating.Again) SrsStatus.Relearning else SrsStatus.Review,
                wasNew = wasNew,
                responseTimeMs = 3_000
            )
        )
    }

    // ------------------------------------------------------------
    // Period stats
    // ------------------------------------------------------------

    @Test
    fun `period stats derive from events not card state`() {
        val note = addNote("統")
        val card = addCard(note)
        val today = LocalDate(2026, 1, 10)
        val tz = TimeZone.currentSystemDefault()

        review(card, ReviewRating.Good, today.atTime(0, 0).toInstant(tz), wasNew = true)
        review(card, ReviewRating.Good, today.atTime(0, 0).toInstant(tz))
        review(card, ReviewRating.Again, today.atTime(0, 0).toInstant(tz))

        val stats = StatisticsRepository.periodStats(store, StatsPeriod.Today, today)
        assertEquals(3, stats.reviews)
        assertEquals(1, stats.newCards)
        assertEquals(2, stats.correct)
        assertEquals(1, stats.again)
        assertEquals(2f / 3f, stats.accuracy, 0.001f)
        assertEquals(9_000, stats.studyTimeMs)
    }

    @Test
    fun `lifetime covers all events regardless of date`() {
        val note = addNote("全")
        val card = addCard(note)
        review(card, ReviewRating.Good, Instant.parse("2020-01-01T00:00:00Z"))

        val all = StatisticsRepository.lifetime(store)
        assertEquals(1, all.reviews)
    }

    // ------------------------------------------------------------
    // Streaks
    // ------------------------------------------------------------

    @Test
    fun `streaks count consecutive real activity days`() {
        val note = addNote("鎖")
        val card = addCard(note)
        val tz = TimeZone.currentSystemDefault()
        val today = LocalDate(2026, 1, 10)

        review(card, ReviewRating.Good, today.atTime(0, 0).toInstant(tz))
        review(card, ReviewRating.Good, today.minus(1, kotlinx.datetime.DateTimeUnit.DAY).atTime(0, 0).toInstant(tz))
        review(card, ReviewRating.Good, today.minus(2, kotlinx.datetime.DateTimeUnit.DAY).atTime(0, 0).toInstant(tz))

        val streaks = StatisticsRepository.streaks(store, today)
        assertEquals(3, streaks.current)
        assertEquals(3, streaks.longest)
    }

    @Test
    fun `a missed day breaks the current streak`() {
        val note = addNote("断")
        val card = addCard(note)
        val tz = TimeZone.currentSystemDefault()
        val today = LocalDate(2026, 1, 10)

        review(card, ReviewRating.Good, today.atTime(0, 0).toInstant(tz))
        review(card, ReviewRating.Good, today.minus(3, kotlinx.datetime.DateTimeUnit.DAY).atTime(0, 0).toInstant(tz))

        val streaks = StatisticsRepository.streaks(store, today)
        assertEquals(1, streaks.current, "Gap yesterday breaks the current run")
        assertEquals(1, streaks.longest)
    }

    // ------------------------------------------------------------
    // JLPT coverage + character progress
    // ------------------------------------------------------------

    @Test
    fun `jlpt coverage counts stage-based known learning unseen`() {
        val n5 = addNote("段", jlpt = 5)
        addCard(n5, status = SrsStatus.Review, intervalDays = 30.0)   // mature
        val n5learning = addNote("学", jlpt = 5)
        addCard(n5learning, status = SrsStatus.Learning, intervalDays = 0.02)
        val n4 = addNote("段4", jlpt = 4)
        addCard(n4)

        val coverage = StatisticsRepository.jlptCoverage(store)
        val five = coverage.first { it.level == 5 }
        assertEquals(2, five.total)
        assertEquals(1, five.known)
        assertEquals(1, five.learning)
        assertEquals(1, coverage.first { it.level == 4 }.unseen)
    }

    @Test
    fun `character progress separates reviews from distinct characters`() {
        val a = addNote("字一")
        val b = addNote("字二")
        val cardA = addCard(a)
        val cardB = addCard(b)
        val tz = TimeZone.currentSystemDefault()
        val today = LocalDate(2026, 1, 10)

        review(cardA, ReviewRating.Good, today.atTime(0, 0).toInstant(tz), wasNew = true)
        review(cardA, ReviewRating.Good, today.atTime(0, 0).toInstant(tz))
        review(cardB, ReviewRating.Good, today.atTime(0, 0).toInstant(tz), wasNew = true)

        val progress = StatisticsRepository.characterProgress(store)
        assertEquals(2, progress.uniqueKanji)
        assertEquals(2, progress.uniqueKanjiStudied)
        assertEquals(1, progress.uniqueKanjiEstablished)
    }

    // ------------------------------------------------------------
    // Writing statistics
    // ------------------------------------------------------------

    private fun recordWriting(note: LearningNote, card: NoteCard, expected: String, accuracy: Float, at: Instant) {
        store.recordWriting(
            WritingAttemptEvent(
                id = LearningIds.eventId("writing"),
                cardId = card.id,
                noteId = note.id,
                deckId = card.deckId,
                attempted = expected,
                expected = expected,
                accuracy = accuracy,
                mistakeCount = if (accuracy >= 0.99f) 0 else 1,
                completed = true,
                attemptedAt = at
            )
        )
    }

    @Test
    fun `weakest kanji comes from actual failed writing attempts`() {
        val note = addNote("難")
        val card = addCard(note, status = SrsStatus.Review, intervalDays = 1.0)
        val tz = TimeZone.currentSystemDefault()
        val today = LocalDate(2026, 1, 10)
        val at = today.atTime(0, 0).toInstant(tz)

        // Three failed attempts: the stored self-accuracy is 0.4f, but the
        // derived accuracy counts an attempt as correct only at >= 0.99f.
        repeat(3) { recordWriting(note, card, "難", accuracy = 0.4f, at = at) }

        val weakest = StatisticsRepository.weakestKanji(store)
        assertTrue(weakest.isNotEmpty())
        assertEquals("難", weakest.first().expression)
        assertEquals(3, weakest.first().attempts)
        assertEquals(0f, weakest.first().accuracy, 0.001f)
    }

    @Test
    fun `writing stats count only near-perfect attempts as correct`() {
        val note = addNote("完")
        val card = addCard(note)
        val at = LocalDate(2026, 1, 10).atTime(0, 0).toInstant(TimeZone.currentSystemDefault())

        recordWriting(note, card, "完", accuracy = 0.4f, at = at)
        recordWriting(note, card, "完", accuracy = 1f, at = at)

        val row = StatisticsRepository.writingStats(store).single()
        assertEquals(2, row.attempts)
        assertEquals(1, row.correct)
        assertEquals(0.5f, row.accuracy, 0.001f)
    }

    @Test
    fun `single attempts are excluded until a character has two`() {
        val note = addNote("一")
        val card = addCard(note)
        val at = LocalDate(2026, 1, 10).atTime(0, 0).toInstant(TimeZone.currentSystemDefault())

        recordWriting(note, card, "一", accuracy = 0.4f, at = at)
        assertTrue(StatisticsRepository.writingStats(store).isEmpty(), "One attempt is not enough to judge weakness")

        recordWriting(note, card, "一", accuracy = 0.4f, at = at)
        assertEquals(1, StatisticsRepository.writingStats(store).size)
    }

    @Test
    fun `weakest kanji excludes vocabulary and other kinds`() {
        val kanji = addNote("弱", kind = LearningItemKind.Kanji)
        val kanjiCard = addCard(kanji)
        val vocab = addNote("語彙", kind = LearningItemKind.Vocabulary)
        val vocabCard = addCard(vocab)
        val at = LocalDate(2026, 1, 10).atTime(0, 0).toInstant(TimeZone.currentSystemDefault())

        recordWriting(kanji, kanjiCard, "弱", accuracy = 0.4f, at = at)
        recordWriting(kanji, kanjiCard, "弱", accuracy = 0.4f, at = at)
        recordWriting(vocab, vocabCard, "語彙", accuracy = 0.4f, at = at)
        recordWriting(vocab, vocabCard, "語彙", accuracy = 0.4f, at = at)

        assertEquals(2, StatisticsRepository.writingStats(store).size, "writingStats keeps every kind")
        val weakestKanji = StatisticsRepository.weakestKanji(store)
        assertEquals(1, weakestKanji.size)
        assertEquals("弱", weakestKanji.single().expression)
        assertEquals(LearningItemKind.Kanji, weakestKanji.single().kind)
    }

    @Test
    fun `weakest kanji orders weakest first and applies the limit`() {
        val at = LocalDate(2026, 1, 10).atTime(0, 0).toInstant(TimeZone.currentSystemDefault())

        // 覚: 0% (two bad), 忘: 50% (one good one bad), 想: 100% (two good)
        val weak = addNote("覚")
        val weakCard = addCard(weak)
        recordWriting(weak, weakCard, "覚", accuracy = 0.4f, at = at)
        recordWriting(weak, weakCard, "覚", accuracy = 0.4f, at = at)

        val mid = addNote("忘")
        val midCard = addCard(mid)
        recordWriting(mid, midCard, "忘", accuracy = 0.4f, at = at)
        recordWriting(mid, midCard, "忘", accuracy = 1f, at = at)

        val strong = addNote("想")
        val strongCard = addCard(strong)
        recordWriting(strong, strongCard, "想", accuracy = 1f, at = at)
        recordWriting(strong, strongCard, "想", accuracy = 1f, at = at)

        assertEquals(listOf("覚", "忘"), StatisticsRepository.weakestKanji(store, limit = 2).map { it.expression })
        assertEquals(listOf("覚", "忘", "想"), StatisticsRepository.weakestKanji(store).map { it.expression })
    }

    @Test
    fun `empty store yields no weakest kanji`() {
        assertTrue(StatisticsRepository.weakestKanji(store).isEmpty())
        assertTrue(StatisticsRepository.writingStats(store).isEmpty())
    }

    // ------------------------------------------------------------
    // Mistake snapshot
    // ------------------------------------------------------------

    @Test
    fun `mistake snapshot counts again events and wrong exam answers`() {
        val note = addNote("誤")
        val card = addCard(note)
        val today = LocalDate(2026, 1, 10)
        val tz = TimeZone.currentSystemDefault()

        review(card, ReviewRating.Again, today.atTime(0, 0).toInstant(tz))
        review(card, ReviewRating.Again, today.atTime(0, 0).toInstant(tz))
        store.recordExam(
            ExamResult(
                id = LearningIds.eventId("exam"),
                title = "t",
                examType = "MixedJlpt",
                startedAt = today.atTime(0, 0).toInstant(tz),
                finishedAt = today.atTime(0, 0).toInstant(tz),
                questionCount = 2,
                correctCount = 1,
                questions = listOf(
                    ExamQuestionResult("q1", card.id, note.id, "recognition:meaning", correct = true),
                    ExamQuestionResult("q2", card.id, note.id, "production:writing", correct = false)
                )
            )
        )

        val snapshot = StatisticsRepository.mistakeSnapshot(store)
        assertEquals(2, snapshot.againEvents)
        assertEquals(1, snapshot.examMistakes)
        assertTrue(card.id in snapshot.againCardIds)
        assertTrue(card.id in snapshot.examWrongCardIds)
    }

    // ------------------------------------------------------------
    // Study vs exam gap
    // ------------------------------------------------------------

    @Test
    fun `study vs exam gap compares review accuracy to exam production accuracy`() {
        val note = addNote("差")
        val card = addCard(note)
        val tz = TimeZone.currentSystemDefault()
        val today = LocalDate(2026, 1, 10)

        // Study: 2 correct reviews.
        review(card, ReviewRating.Good, today.atTime(0, 0).toInstant(tz))
        review(card, ReviewRating.Good, today.atTime(0, 0).toInstant(tz))
        // Exam: production question answered wrong.
        store.recordExam(
            ExamResult(
                id = LearningIds.eventId("exam"),
                title = "t",
                examType = "MixedJlpt",
                startedAt = today.atTime(0, 0).toInstant(tz),
                finishedAt = today.atTime(0, 0).toInstant(tz),
                questionCount = 1,
                correctCount = 0,
                questions = listOf(
                    ExamQuestionResult("q1", card.id, note.id, "production:writing", correct = false)
                )
            )
        )

        val gap = StatisticsRepository.studyVsExamGap(store)
        assertEquals(1f, gap.studyAccuracy, 0.001f)
        assertEquals(0f, gap.examProductionAccuracy, 0.001f)
        assertEquals(1f, gap.studyAccuracy - gap.examProductionAccuracy, 0.001f, "The gap is measured, not invented")
    }

    // ------------------------------------------------------------
    // Forecast
    // ------------------------------------------------------------

    @Test
    fun `forecast buckets cards by their actual due date`() {
        val note = addNote("予")
        val today = LocalDate(2026, 1, 10)
        val tz = TimeZone.currentSystemDefault()
        addCard(note, status = SrsStatus.Review, intervalDays = 1.0, dueAt = today.atTime(0, 0).toInstant(tz))
        val later = addNote("後")
        addCard(later, status = SrsStatus.Review, intervalDays = 1.0, dueAt = today.plus(2, kotlinx.datetime.DateTimeUnit.DAY).atTime(0, 0).toInstant(tz))

        val forecast = StatisticsRepository.forecast(store, 7, today)
        assertEquals(2, forecast[0].due, "Overdue + due today land in the first bucket")
        assertEquals(1, forecast[2].due)
        assertEquals(0, forecast[1].due)
    }

    @Test
    fun `dueToday counts cards due at or before now`() {
        val note = addNote("到")
        val card = addCard(note, status = SrsStatus.Review, intervalDays = 1.0, dueAt = Instant.parse("2020-01-01T00:00:00Z"))
        addCard(addNote("将"), status = SrsStatus.Review, intervalDays = 1.0, dueAt = Instant.parse("2099-01-01T00:00:00Z"))
        assertTrue(store.card(card.id) != null)

        assertEquals(1, StatisticsRepository.dueToday(store, Instant.parse("2026-01-01T00:00:00Z")))
    }

    // ------------------------------------------------------------
    // Goals
    // ------------------------------------------------------------

    @Test
    fun `goal progress reads real today stats`() {
        val note = addNote("標")
        val card = addCard(note)
        val today = LocalDate(2026, 1, 10)
        val tz = TimeZone.currentSystemDefault()
        review(card, ReviewRating.Good, today.atTime(0, 0).toInstant(tz), wasNew = true)
        review(card, ReviewRating.Good, today.atTime(0, 0).toInstant(tz))

        val goals = GoalsRepository.allProgress(store, today)
        val reviewsGoal = goals.first { it.goal.metric == GoalMetric.Reviews }
        assertEquals(2, reviewsGoal.achieved)
        val newGoal = goals.first { it.goal.metric == GoalMetric.NewCards }
        assertEquals(1, newGoal.achieved)
    }

    @Test
    fun `exam aggregates compute real averages`() {
        val tz = TimeZone.currentSystemDefault()
        val day = LocalDate(2026, 1, 10).atTime(0, 0).toInstant(tz)
        store.recordExam(
            ExamResult("e1", "A", "MixedJlpt", day, day, 10, 10)  // 100%
        )
        store.recordExam(
            ExamResult("e2", "B", "MixedJlpt", day, day, 10, 5)   // 50%
        )

        val aggregates = StatisticsRepository.examAggregates(store)
        assertEquals(2, aggregates.count)
        assertEquals(75, aggregates.averageScore)
        assertEquals(100, aggregates.bestScore)
        assertEquals(50, aggregates.worstScore)
        assertEquals(20, aggregates.totalQuestions)
    }
}
