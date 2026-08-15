package ua.syt0r.kanji.desktop.engine.learning

import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import ua.syt0r.kanji.desktop.model.ReviewRating
import ua.syt0r.kanji.desktop.model.SrsStatus
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class StudyEngineTest {

    private lateinit var dir: File
    private lateinit var store: LearningStore
    private lateinit var engine: StudyEngine

    @BeforeTest
    fun setUp() {
        dir = Files.createTempDirectory("kaiteyo-study-test").toFile()
        store = LearningStore(dir)
        engine = StudyEngine(store)
    }

    @AfterTest
    fun tearDown() {
        dir.deleteRecursively()
    }

    private fun addCard(
        expression: String,
        deckId: String = "deck-a",
        type: CardType = CardType.Recognition,
        status: SrsStatus = SrsStatus.New,
        dueAt: Instant? = null
    ): NoteCard {
        val note = LearningNote(
            id = LearningIds.noteId(LearningItemKind.Kanji, expression, "reading"),
            kind = LearningItemKind.Kanji,
            expression = expression,
            reading = "reading",
            meanings = listOf("meaning")
        )
        store.upsertNote(note)
        val card = NoteCard(
            id = LearningIds.cardId(note.id, type, deckId),
            noteId = note.id,
            cardType = type,
            deckId = deckId,
            status = status,
            dueAt = dueAt
        )
        store.upsertCard(card)
        return card
    }

    private fun item(card: NoteCard): StudyQueueItem =
        StudyQueueItem(card, store.note(card.noteId)!!)

    // ------------------------------------------------------------
    // Queue building
    // ------------------------------------------------------------

    @Test
    fun `queue contains new cards up to the deck limit and due cards`() {
        repeat(5) { addCard("新$it") }
        val dueCard = addCard("古", status = SrsStatus.Review, dueAt = Instant.parse("2020-01-01T00:00:00Z"))

        val queue = engine.buildQueue("deck-a", newLimit = 3)
        val ids = queue.map { it.card.id }

        assertTrue(ids.contains(dueCard.id), "Due cards must always be included")
        assertEquals(4, queue.size, "3 new (limited) + 1 due")
    }

    @Test
    fun `suspended and buried cards are excluded from the queue`() {
        addCard("活", status = SrsStatus.Suspended)
        addCard("埋", status = SrsStatus.Buried)
        addCard("学", status = SrsStatus.New)

        val queue = engine.buildQueue("deck-a")
        assertEquals(1, queue.size)
        assertEquals("学", queue.first().note.expression)
    }

    @Test
    fun `queue is empty when nothing is due or new`() {
        addCard("済", status = SrsStatus.Review, dueAt = Instant.parse("2099-01-01T00:00:00Z"))
        assertTrue(engine.buildQueue("deck-a", includeNew = false).isEmpty())
    }

    @Test
    fun `queue filters by mode`() {
        val recognition = addCard("識", type = CardType.Recognition)
        addCard("意", type = CardType.Meaning)

        val queue = engine.buildQueue("deck-a", mode = ua.syt0r.kanji.desktop.model.StudyMode.Recognition)
        assertEquals(1, queue.size)
        assertEquals(recognition.id, queue.first().card.id)
    }

    // ------------------------------------------------------------
    // Grading / SRS
    // ------------------------------------------------------------

    @Test
    fun `grading a new card with Good enters learning and appends an event`() {
        val card = addCard("新")
        val now = Instant.parse("2026-01-01T00:00:00Z")

        val result = engine.grade(item(card), ReviewRating.Good, now = now)

        assertEquals(SrsStatus.Learning, result.card.status, "A new card graded Good stays in learning steps")
        assertTrue(result.card.dueAt!! > now)
        assertEquals(1, result.card.reps)
        assertEquals(1, store.reviewEvents.size)

        val event = store.reviewEvents.first()
        assertEquals(card.id, event.cardId)
        assertEquals(SrsStatus.New, event.statusBefore)
        assertEquals(SrsStatus.Learning, event.statusAfter)
        assertTrue(event.correct)
    }

    @Test
    fun `grading with Again increments lapses and records a failure`() {
        val card = addCard("落", status = SrsStatus.Review, dueAt = Instant.parse("2020-01-01T00:00:00Z"))
        val now = Instant.parse("2026-01-01T00:00:00Z")

        engine.grade(item(card), ReviewRating.Again, now = now)

        val updated = store.card(card.id)!!
        assertEquals(1, updated.lapses)
        assertEquals(0, updated.streak)
        assertFalse(store.reviewEvents.first().correct)
    }

    @Test
    fun `grading a review card with Easy grows the interval`() {
        val card = addCard(
            "熟",
            status = SrsStatus.Review,
            dueAt = Instant.parse("2020-01-01T00:00:00Z")
        )
        store.cards[store.cards.indexOfFirst { it.id == card.id }] = card.copy(intervalDays = 4.0)
        val now = Instant.parse("2026-01-01T00:00:00Z")

        engine.grade(item(card), ReviewRating.Easy, now = now)

        val updated = store.card(card.id)!!
        assertTrue(updated.intervalDays > 4.0, "Easy must increase the interval")
        assertTrue(updated.ease > 2.5, "Easy must raise ease")
        assertEquals(ReviewRating.Easy, store.reviewEvents.first().rating)
    }

    @Test
    fun `accuracy is a running average of correct answers`() {
        val card = addCard("均")
        val now = Instant.parse("2026-01-01T00:00:00Z")
        engine.grade(item(card), ReviewRating.Good, now = now)
        engine.grade(item(store.card(card.id)!!), ReviewRating.Good, now = now.plus(10.minutes))
        engine.grade(item(store.card(card.id)!!), ReviewRating.Again, now = now.plus(20.minutes))

        assertEquals(3, store.card(card.id)!!.reps)
        assertTrue(store.card(card.id)!!.accuracy in 0.6f..0.7f, "2/3 correct → accuracy ~0.66")
    }

    // ------------------------------------------------------------
    // Sessions
    // ------------------------------------------------------------

    @Test
    fun `sessions track answers and can be finished`() {
        val session = engine.openSession("deck-a", ua.syt0r.kanji.desktop.model.StudyMode.Recognition)
        assertNotNull(session.id)

        engine.recordAnswer(session, correct = true, wasNew = true)
        engine.recordAnswer(session, correct = false, wasNew = false)
        engine.finishSession(store.sessions.first { it.id == session.id })

        val finished = store.sessions.first { it.id == session.id }
        assertEquals(2, finished.cardsSeen)
        assertEquals(1, finished.correctCount)
        assertEquals(1, finished.againCount)
        assertNotNull(finished.finishedAt)
    }

    @Test
    fun `pendingSession returns the latest unfinished session`() {
        engine.openSession("deck-a", ua.syt0r.kanji.desktop.model.StudyMode.Flashcards)
        val open = engine.openSession("deck-a", ua.syt0r.kanji.desktop.model.StudyMode.Flashcards)
        val pending = engine.pendingSession("deck-a")
        assertEquals(open.id, pending?.id)
    }

    // ------------------------------------------------------------
    // Deck totals
    // ------------------------------------------------------------

    @Test
    fun `deckTotals counts real stages`() {
        addCard("新1")
        addCard("新2")
        addCard("学", status = SrsStatus.Learning, dueAt = Instant.parse("2020-01-01T00:00:00Z"))
        addCard("復", status = SrsStatus.Review, dueAt = Instant.parse("2099-01-01T00:00:00Z"))

        val totals = engine.deckTotals("deck-a")
        assertEquals(4, totals.total)
        assertEquals(2, totals.new)
        assertEquals(1, totals.learning)
        assertEquals(1, totals.review)
        assertEquals(1, totals.due, "Only the lapsed learning card is due now")
    }
}
