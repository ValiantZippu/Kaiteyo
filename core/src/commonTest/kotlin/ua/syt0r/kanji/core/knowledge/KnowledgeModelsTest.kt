package ua.syt0r.kanji.core.knowledge

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class KnowledgeModelsTest {

    @Test
    fun parsesDbClassificationValues() {
        assertEquals(KanjiTag.Jlpt(5), KanjiTag.fromDbValue("n5"))
        assertEquals(KanjiTag.Jlpt(1), KanjiTag.fromDbValue("n1"))
        assertEquals(KanjiTag.Grade(2), KanjiTag.fromDbValue("g2"))
        assertEquals(KanjiTag.Wanikani(42), KanjiTag.fromDbValue("w42"))
    }

    @Test
    fun rejectsInvalidClassificationValues() {
        assertNull(KanjiTag.fromDbValue("n9"))
        assertNull(KanjiTag.fromDbValue("g0"))
        assertNull(KanjiTag.fromDbValue("g11"))
        assertNull(KanjiTag.fromDbValue("w0"))
        assertNull(KanjiTag.fromDbValue("w61"))
        assertNull(KanjiTag.fromDbValue("x5"))
        assertNull(KanjiTag.fromDbValue(""))
        assertNull(KanjiTag.fromDbValue("abc"))
    }

    @Test
    fun gradesMapToJoyoSets() {
        assertEquals(KanjiSetKind.Kyōiku, KanjiTag.Grade(3).setKind())
        assertEquals(KanjiSetKind.Joyo, KanjiTag.Grade(8).setKind())
        assertEquals(KanjiSetKind.Jinmeiyo, KanjiTag.Grade(9).setKind())
        assertEquals(KanjiSetKind.Supplementary, KanjiTag.Grade(10).setKind())
    }

    @Test
    fun kanjiKnowledgeIsJoyoDerivesFromGradeTags() {
        val joyo = KanjiKnowledge(
            character = "食",
            classifications = listOf(KanjiTag.Jlpt(4), KanjiTag.Grade(2))
        )
        assertEquals(true, joyo.isJoyo)

        val nonJoyo = KanjiKnowledge(
            character = "麺",
            classifications = listOf(KanjiTag.Grade(9))
        )
        assertEquals(false, nonJoyo.isJoyo)
    }

    @Test
    fun keywordIsFirstMeaning() {
        val kanji = KanjiKnowledge(
            character = "食",
            meanings = listOf("eat", "food")
        )
        assertEquals("eat", kanji.keyword)
        assertEquals(null, KanjiKnowledge(character = "無").keyword)
    }

    @Test
    fun wordDisplaySpellingPrefersKanji() {
        val withKanji = WordKnowledge(id = 1, kanjiReading = "食べる", kanaReading = "たべる")
        assertEquals("食べる", withKanji.displaySpelling)
        val kanaOnly = WordKnowledge(id = 2, kanaReading = "すし")
        assertEquals("すし", kanaOnly.displaySpelling)
    }
}
