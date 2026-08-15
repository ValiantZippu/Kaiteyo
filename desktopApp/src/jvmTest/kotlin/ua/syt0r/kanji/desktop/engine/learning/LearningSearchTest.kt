package ua.syt0r.kanji.desktop.engine.learning

import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LearningSearchTest {

    private lateinit var dir: File
    private lateinit var store: LearningStore
    private lateinit var engine: LearningEngine

    @BeforeTest
    fun setUp() {
        dir = Files.createTempDirectory("kaiteyo-search-test").toFile()
        store = LearningStore(dir)
        engine = LearningEngine(store)
    }

    @AfterTest
    fun tearDown() {
        dir.deleteRecursively()
    }

    private fun note(
        kind: LearningItemKind,
        expression: String,
        reading: String = "",
        meanings: List<String> = emptyList(),
        jlpt: Int? = null,
        tags: List<String> = emptyList()
    ) {
        store.upsertNote(
            LearningNote(
                id = LearningIds.noteId(kind, expression, reading),
                kind = kind,
                expression = expression,
                reading = reading,
                meanings = meanings,
                jlpt = jlpt,
                tags = tags
            )
        )
    }

    @Test
    fun `exact expression match ranks above substring match`() {
        note(LearningItemKind.Kanji, "食", "しょく", listOf("to eat", "food"), 5)
        note(LearningItemKind.Vocabulary, "食事", "しょくじ", listOf("meal"), 5)
        note(LearningItemKind.Vocabulary, "食べる", "たべる", listOf("to eat"), 5)

        val results = engine.search("食", kinds = setOf(LearningItemKind.Kanji))
        assertTrue(results.isNotEmpty())
        assertEquals("食", results.first().expression, "Exact kanji match must rank first")
    }

    @Test
    fun `matches by reading and meaning`() {
        note(LearningItemKind.Vocabulary, "水", "みず", listOf("water"), 5)
        note(LearningItemKind.Vocabulary, "飲む", "のむ", listOf("to drink"), 5)

        val byReading = engine.search("みず", kinds = setOf(LearningItemKind.Vocabulary))
        assertTrue(byReading.any { it.expression == "水" }, "Kana reading must match")

        val byMeaning = engine.search("water", kinds = setOf(LearningItemKind.Vocabulary))
        assertTrue(byMeaning.any { it.expression == "水" }, "English meaning must match")
    }

    @Test
    fun `kind filter restricts results`() {
        note(LearningItemKind.Kanji, "日", "にち", listOf("sun"), 5)
        note(LearningItemKind.Vocabulary, "日曜日", "にちようび", listOf("Sunday"), 5)

        val kanji = engine.search("日", kinds = setOf(LearningItemKind.Kanji))
        val vocab = engine.search("日", kinds = setOf(LearningItemKind.Vocabulary))
        assertTrue(kanji.all { it.kind == LearningItemKind.Kanji })
        assertTrue(vocab.all { it.kind == LearningItemKind.Vocabulary })
        assertTrue(kanji.isNotEmpty() && vocab.isNotEmpty())
    }

    @Test
    fun `jlpt filter restricts results`() {
        note(LearningItemKind.Kanji, "一", "いち", listOf("one"), 5)
        note(LearningItemKind.Kanji, "鬱", "うつ", listOf("depression"), 1)

        val n1 = engine.search("", jlpt = 1, kinds = setOf(LearningItemKind.Kanji))
        val n5 = engine.search("", jlpt = 5, kinds = setOf(LearningItemKind.Kanji))
        assertTrue(n1.all { it.jlpt == 1 })
        assertTrue(n5.all { it.jlpt == 5 })
        assertEquals(1, n1.size)
        assertTrue(n5.any { it.expression == "一" })
    }

    @Test
    fun `structured filters work inline in the query`() {
        note(LearningItemKind.Kanji, "山", "やま", listOf("mountain"), 5)
        note(LearningItemKind.Vocabulary, "火山", "かざん", listOf("volcano"), 4, tags = listOf("geo"))

        val byTag = engine.search("tag:geo")
        assertTrue(byTag.any { it.expression == "火山" })
        assertTrue(byTag.none { it.expression == "山" })

        val byKindInline = engine.search("kind:kanji 山")
        assertTrue(byKindInline.all { it.kind == LearningItemKind.Kanji })

        val byJlptInline = engine.search("jlpt:4")
        assertTrue(byJlptInline.all { it.jlpt == 4 })
    }

    @Test
    fun `blank query returns notes of the requested kind`() {
        note(LearningItemKind.Kanji, "木", "き", listOf("tree"), 5)
        note(LearningItemKind.Vocabulary, "林", "はやし", listOf("woods"), 4)

        val all = engine.search("", kinds = setOf(LearningItemKind.Kanji, LearningItemKind.Vocabulary))
        assertEquals(2, all.size)

        val kanjiOnly = engine.search("", kinds = setOf(LearningItemKind.Kanji))
        assertEquals(1, kanjiOnly.size)
        assertEquals("木", kanjiOnly.first().expression)
    }

    @Test
    fun `results carry real stage and due state`() {
        val n = LearningNote(
            id = LearningIds.noteId(LearningItemKind.Kanji, "水", "みず"),
            kind = LearningItemKind.Kanji,
            expression = "水",
            reading = "みず",
            meanings = listOf("water"),
            jlpt = 5
        )
        store.upsertNote(n)
        val card = NoteCard(
            id = LearningIds.cardId(n.id, CardType.Recognition, "deck-a"),
            noteId = n.id,
            cardType = CardType.Recognition,
            deckId = "deck-a",
            status = ua.syt0r.kanji.desktop.model.SrsStatus.Review,
            intervalDays = 30.0,
            dueAt = kotlinx.datetime.Instant.parse("2020-01-01T00:00:00Z")
        )
        store.upsertCard(card)

        val result = engine.search("水", kinds = setOf(LearningItemKind.Kanji)).first()
        assertEquals(LearningStage.Mature, result.stage, "30-day review interval must be Mature")
        assertEquals(1, result.due, "Overdue card must show due=1")
        assertEquals(listOf("deck-a"), result.deckIds)
    }
}
