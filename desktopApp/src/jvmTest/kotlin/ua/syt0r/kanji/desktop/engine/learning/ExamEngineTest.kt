package ua.syt0r.kanji.desktop.engine.learning

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
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExamEngineTest {

    private lateinit var dir: File
    private lateinit var store: LearningStore
    private lateinit var engine: ExamEngine

    @BeforeTest
    fun setUp() {
        dir = Files.createTempDirectory("kaiteyo-exam-test").toFile()
        store = LearningStore(dir)
        engine = ExamEngine(store)
    }

    @AfterTest
    fun tearDown() {
        dir.deleteRecursively()
    }

    private fun addNote(
        kind: LearningItemKind,
        expression: String,
        reading: String = "おん",
        meanings: List<String> = listOf("meaning of $expression"),
        jlpt: Int? = null,
        deckId: String = "deck-a"
    ): LearningNote {
        val note = LearningNote(
            id = LearningIds.noteId(kind, expression, reading),
            kind = kind,
            expression = expression,
            reading = reading,
            meanings = meanings,
            jlpt = jlpt
        )
        store.upsertNote(note)
        val card = NoteCard(
            id = LearningIds.cardId(note.id, CardType.Recognition, deckId),
            noteId = note.id,
            cardType = CardType.Recognition,
            deckId = deckId
        )
        store.upsertCard(card)
        return note
    }

    private fun seedStandardPool() {
        addNote(LearningItemKind.Kanji, "日", "にち", listOf("sun", "day"), 5)
        addNote(LearningItemKind.Kanji, "月", "げつ", listOf("moon", "month"), 5)
        addNote(LearningItemKind.Kanji, "火", "か", listOf("fire"), 5)
        addNote(LearningItemKind.Vocabulary, "食べる", "たべる", listOf("to eat"), 5)
        addNote(LearningItemKind.Vocabulary, "飲む", "のむ", listOf("to drink"), 5)
    }

    // ------------------------------------------------------------
    // Generation
    // ------------------------------------------------------------

    @Test
    fun `generates a draft from real notes`() {
        seedStandardPool()
        val draft = engine.generate(ExamType.KanjiMeaning, questionCount = 3)
        assertNotNull(draft)
        assertEquals(3, draft.questions.size)
        assertTrue(draft.questions.all { it.noteId.isNotBlank() })
        assertTrue(draft.questions.all { it.options.isNotEmpty() }, "Multiple-choice questions need options")
    }

    @Test
    fun `returns null when there is no matching content`() {
        seedStandardPool()
        assertNull(engine.generate(ExamType.KanjiMeaning, questionCount = 5, jlpt = 1), "No JLPT N1 content → no draft")
        assertNull(engine.generate(ExamType.VocabProduction, questionCount = 5, deckId = "missing-deck"))
    }

    @Test
    fun `distractors are real meanings from other notes`() {
        seedStandardPool()
        val draft = engine.generate(ExamType.KanjiMeaning, questionCount = 5)!!
        val pool = draft.questions.first().options
        // The correct answer is one of the options, and distractors come from real notes.
        assertTrue(draft.questions.first().correctAnswer in pool)
        assertTrue(pool.size >= 2)
    }

    @Test
    fun `scope restricts the pool to a deck`() {
        addNote(LearningItemKind.Kanji, "A", "あ", listOf("a"), deckId = "deck-1")
        addNote(LearningItemKind.Kanji, "B", "い", listOf("b"), deckId = "deck-2")
        val draft = engine.generate(ExamType.KanjiMeaning, questionCount = 10, deckId = "deck-1")!!
        assertTrue(draft.questions.all { it.deckId == "deck-1" })
    }

    // ------------------------------------------------------------
    // Evaluation
    // ------------------------------------------------------------

    @Test
    fun `evaluates multiple choice exactly and persists the result`() {
        seedStandardPool()
        val draft = engine.generate(ExamType.KanjiMeaning, questionCount = 4)!!
        val startedAt = Instant.parse("2026-01-01T00:00:00Z")

        val answers = draft.questions.map { q ->
            q.id to ExamAnswer(q.id, answer = q.correctAnswer, confidence = 3)
        }.toMap()
        val result = engine.evaluate(draft, answers, startedAt)

        assertEquals(4, result.correctCount)
        assertEquals(100, result.percentage)
        assertEquals(1, store.examResults.size, "Result must persist to the store")
    }

    @Test
    fun `evaluates typed answers with normalization`() {
        val note = addNote(LearningItemKind.Vocabulary, "食べる", "たべる", listOf("to eat"))
        val card = store.cardsFor(note.id).first()

        // Typed reading question — the engine normalizes kana/space/punctuation.
        val draft = ExamDraft(
            type = ExamType.VocabReading,
            title = "test",
            sections = listOf(
                ExamSection(
                    id = "s1",
                    label = "Reading",
                    questions = listOf(
                        ExamQuestion(
                            id = "q1",
                            cardId = card.id,
                            noteId = note.id,
                            questionType = ExamQuestionType.TypedReading,
                            prompt = "Read it",
                            correctAnswer = "たべる"
                        )
                    )
                )
            )
        )
        val startedAt = Instant.parse("2026-01-01T00:00:00Z")

        val perfect = engine.evaluate(draft, mapOf("q1" to ExamAnswer("q1", answer = "たべる")), startedAt)
        assertTrue(perfect.questions.first().correct)

        val sloppy = engine.evaluate(draft, mapOf("q1" to ExamAnswer("q1", answer = "たべる ！！ ")), startedAt)
        assertTrue(sloppy.questions.first().correct, "Whitespace and punctuation must be normalized away")

        val wrong = engine.evaluate(draft, mapOf("q1" to ExamAnswer("q1", answer = "のむ")), startedAt)
        assertFalse(wrong.questions.first().correct)
    }

    @Test
    fun `skipped questions count as wrong`() {
        seedStandardPool()
        val draft = engine.generate(ExamType.KanjiMeaning, questionCount = 3)!!
        val startedAt = Instant.parse("2026-01-01T00:00:00Z")

        val answers = draft.questions.mapIndexed { i, q ->
            q.id to if (i == 0) ExamAnswer(q.id, skipped = true) else ExamAnswer(q.id, answer = q.correctAnswer)
        }.toMap()
        val result = engine.evaluate(draft, answers, startedAt)

        assertEquals(2, result.correctCount)
        assertEquals(1, result.skippedCount)
    }

    @Test
    fun `evaluating an exam feeds SRS review events for the tested cards`() {
        seedStandardPool()
        val draft = engine.generate(ExamType.KanjiMeaning, questionCount = 3)!!
        val startedAt = Instant.parse("2026-01-01T00:00:00Z")

        val answers = draft.questions.map { q -> q.id to ExamAnswer(q.id, answer = q.correctAnswer, confidence = 3) }.toMap()
        engine.evaluate(draft, answers, startedAt)

        val examEvents = store.reviewEvents.filter { it.activityType == StudyActivityType.Exam }
        assertTrue(examEvents.isNotEmpty(), "Correct exam answers must be recorded as review events")
        assertTrue(examEvents.all { it.examId.isNotBlank() })
        assertTrue(examEvents.all { it.correct })
    }

    // ------------------------------------------------------------
    // Weekly assessment
    // ------------------------------------------------------------

    @Test
    fun `weekly assessment covers recently studied notes only`() {
        seedStandardPool()
        val studied = store.notes.first()
        val card = store.cardsFor(studied.id).first()
        val now = Clock.System.now()
        store.recordReview(
            LearningReviewEvent(
                id = LearningIds.eventId("review"),
                cardId = card.id,
                noteId = studied.id,
                deckId = card.deckId,
                cardType = card.cardType,
                activityType = StudyActivityType.Review,
                rating = ReviewRating.Good,
                reviewedAt = now,
                statusBefore = SrsStatus.New,
                statusAfter = SrsStatus.Learning
            )
        )

        val draft = engine.generateWeekly(now)
        assertNotNull(draft)
        assertEquals(ExamType.Weekly, draft.type)
        assertTrue(draft.questions.all { it.noteId == studied.id }, "Weekly exam must only test what was studied this week")
    }

    @Test
    fun `weekly assessment is null when nothing was studied this week`() {
        seedStandardPool()
        assertNull(engine.generateWeekly(Clock.System.now()))
    }
}
