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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LearningStoreTest {

    private lateinit var dir: File
    private lateinit var store: LearningStore

    @BeforeTest
    fun setUp() {
        dir = Files.createTempDirectory("kaiteyo-learning-test").toFile()
        store = LearningStore(dir)
    }

    @AfterTest
    fun tearDown() {
        dir.deleteRecursively()
    }

    private fun kanjiNote(expression: String, reading: String = "おん", meanings: List<String> = listOf("meaning $expression")) =
        LearningNote(
            id = LearningIds.noteId(LearningItemKind.Kanji, expression, reading),
            kind = LearningItemKind.Kanji,
            expression = expression,
            reading = reading,
            meanings = meanings
        )

    private fun cardFor(note: LearningNote, type: CardType = CardType.Recognition, deckId: String = "deck-a") = NoteCard(
        id = LearningIds.cardId(note.id, type, deckId),
        noteId = note.id,
        cardType = type,
        deckId = deckId
    )

    // ------------------------------------------------------------
    // Notes
    // ------------------------------------------------------------

    @Test
    fun `upsert deduplicates by kind and expression`() {
        store.upsertNote(kanjiNote("日", "にち"))
        store.upsertNote(kanjiNote("日", "にち"))

        assertEquals(1, store.notes.size, "Same kind + expression must not duplicate")
    }

    @Test
    fun `same character different kind are distinct notes`() {
        store.upsertNote(kanjiNote("日"))
        store.upsertNote(
            LearningNote(
                id = LearningIds.noteId(LearningItemKind.Vocabulary, "日", "ひ"),
                kind = LearningItemKind.Vocabulary,
                expression = "日",
                reading = "ひ"
            )
        )
        assertEquals(2, store.notes.size)
    }

    @Test
    fun `custom notes are never overwritten by imported data`() {
        val custom = kanjiNote("手").copy(source = NoteSource(NoteSourceType.Custom), meanings = listOf("user meaning"))
        store.upsertNote(custom)

        // A Builtin source trying to claim the same expression must not clobber the custom note.
        store.upsertNote(kanjiNote("手", meanings = listOf("imported meaning")))

        val kept = store.noteByExpression(LearningItemKind.Kanji, "手")
        assertNotNull(kept)
        assertEquals(listOf("user meaning"), kept.meanings, "Custom meanings must survive a source-data import")
        assertEquals(NoteSourceType.Custom, kept.source.type)
    }

    // ------------------------------------------------------------
    // Cards
    // ------------------------------------------------------------

    @Test
    fun `card ids are stable so regeneration never duplicates or resets`() {
        val note = kanjiNote("水")
        store.upsertNote(note)
        store.upsertCard(cardFor(note, CardType.Writing))

        val again = CardGenerator.generateForDeck(note, "deck-a", store.deckConfig("deck-a"), existing = store.cardsFor(note.id))
        assertEquals(1, again.size)
        assertEquals(store.cards.first().id, again.first().id, "Regeneration must reuse the existing card id")
        assertEquals(1, store.cards.count { it.noteId == note.id }, "No duplicate cards from regeneration")
    }

    @Test
    fun `suspending persists the card status`() {
        val note = kanjiNote("火")
        store.upsertNote(note)
        val card = cardFor(note)
        store.upsertCard(card)

        val study = StudyEngine(store)
        study.suspend(card.id)

        assertEquals(SrsStatus.Suspended, store.card(card.id)?.status)
    }

    // ------------------------------------------------------------
    // Deck config
    // ------------------------------------------------------------

    @Test
    fun `deck config is created with defaults and persists`() {
        val config = store.deckConfig("deck-x")
        assertEquals(20, config.dailyNewLimit)

        store.setDeckConfig(config.copy(dailyNewLimit = 7, enabledCardTypes = listOf(CardType.Reading)))
        assertEquals(7, store.deckConfig("deck-x").dailyNewLimit)
        assertEquals(listOf(CardType.Reading), store.deckConfig("deck-x").enabledCardTypes)
    }

    // ------------------------------------------------------------
    // Persistence round-trip
    // ------------------------------------------------------------

    @Test
    fun `notes cards events and exams survive a reload`() {
        val note = kanjiNote("山", meanings = listOf("mountain"))
        store.upsertNote(note)
        val card = cardFor(note)
        store.upsertCard(card)
        store.recordReview(
            LearningReviewEvent(
                id = LearningIds.eventId("review"),
                cardId = card.id,
                noteId = note.id,
                deckId = card.deckId,
                cardType = card.cardType,
                activityType = StudyActivityType.Review,
                rating = ReviewRating.Good,
                reviewedAt = Instant.parse("2026-01-01T00:00:00Z"),
                statusBefore = SrsStatus.New,
                statusAfter = SrsStatus.Learning
            )
        )
        store.recordExam(
            ExamResult(
                id = LearningIds.eventId("exam"),
                title = "Test",
                examType = "MixedJlpt",
                startedAt = Instant.parse("2026-01-01T00:00:00Z"),
                finishedAt = Instant.parse("2026-01-01T00:05:00Z"),
                questionCount = 1,
                correctCount = 1
            )
        )
        store.setDeckConfig(store.deckConfig(card.deckId).copy(dailyNewLimit = 3))

        // Reload from disk with a fresh store instance over the same directory.
        val reloaded = LearningStore(dir)
        assertEquals(1, reloaded.notes.size)
        assertEquals(1, reloaded.cards.size)
        assertEquals(1, reloaded.reviewEvents.size)
        assertEquals(1, reloaded.examResults.size)
        assertEquals(3, reloaded.deckConfig(card.deckId).dailyNewLimit)
        assertEquals(card.id, reloaded.cards.first().id)
    }

    @Test
    fun `isEmpty is true for a fresh store`() {
        assertTrue(LearningStore(dir).isEmpty)
    }

    @Test
    fun `toDesktopCard bridges the unified card to the legacy model`() {
        val note = kanjiNote("月", readings = emptyList(), meanings = listOf("moon"))
        store.upsertNote(note)
        val card = cardFor(note)
        store.upsertCard(card)

        val legacy = store.toDesktopCard(card)
        assertNotNull(legacy)
        assertEquals("月", legacy.character)
        assertEquals("moon", legacy.meaning)
        assertEquals(card.status, legacy.status)
    }

    @Test
    fun `revision bumps on structural mutations`() {
        val before = store.revision
        store.upsertNote(kanjiNote("雨"))
        assertTrue(store.revision > before)
    }

    @Test
    fun `delete note removes its cards`() {
        val note = kanjiNote("風")
        store.upsertNote(note)
        store.upsertCard(cardFor(note))

        store.deleteNote(note.id)
        assertNull(store.note(note.id))
        assertTrue(store.cards.isEmpty())
    }
}
