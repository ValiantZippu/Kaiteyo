package io.kaiteyo.kjd.search

import io.kaiteyo.kjd.api.JapaneseDatabase
import io.kaiteyo.kjd.db.DatabaseWriter
import io.kaiteyo.kjd.model.Character
import io.kaiteyo.kjd.model.CharacterType
import io.kaiteyo.kjd.model.Component
import io.kaiteyo.kjd.model.EntityId
import io.kaiteyo.kjd.model.EntityType
import io.kaiteyo.kjd.model.JlptClassification
import io.kaiteyo.kjd.model.Kanji
import io.kaiteyo.kjd.model.Meaning
import io.kaiteyo.kjd.model.Reading
import io.kaiteyo.kjd.model.Sense
import io.kaiteyo.kjd.model.SourceRef
import io.kaiteyo.kjd.model.Stroke
import io.kaiteyo.kjd.model.VocabularyEntry
import io.kaiteyo.kjd.model.VocabularyReading
import io.kaiteyo.kjd.normalize.JapaneseNormalizer
import io.kaiteyo.kjd.resolve.CanonicalDatabaseBuilder
import io.kaiteyo.kjd.source.BuiltinSources
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests the database-backed search path (relational index + SQLite FTS5).
 * Verifies exact, prefix, reading, kana-equivalent and meaning (FTS)
 * lookups — all without materializing the dictionary in memory.
 */
class FtsSearchTest {

    private fun fixtureDatabase(): File {
        val builder = CanonicalDatabaseBuilder()

        val kanjiEat = Kanji(
            id = EntityId("kanji:食"),
            character = Character(
                id = EntityId("kanji:食"),
                literal = "食",
                codepoint = 0x98DF,
                normalized = JapaneseNormalizer.toNfc("食"),
                characterType = CharacterType.Kanji,
                strokeCount = 9
            ),
            onReadings = listOf(Reading("ショク", "on")),
            kunReadings = listOf(Reading("く.う", "kun")),
            meanings = listOf(Meaning("eat", "en")),
            grade = 2,
            jlpt = listOf(JlptClassification(5, SourceRef("tanos-jlpt", isCanonical = true))),
            strokeCount = 9,
            strokes = listOf(
                Stroke(id = EntityId("stroke:kanji:食:1"), index = 1, characterId = EntityId("kanji:食"), path = "M1,1 L2,2")
            ),
            components = listOf(Component(id = EntityId("component:kanji:食:食"), character = "食", role = "radical"))
        )

        val kanjiWater = Kanji(
            id = EntityId("kanji:水"),
            character = Character(
                id = EntityId("kanji:水"),
                literal = "水",
                codepoint = 0x6C34,
                normalized = JapaneseNormalizer.toNfc("水"),
                characterType = CharacterType.Kanji,
                strokeCount = 4
            ),
            onReadings = listOf(Reading("スイ", "on")),
            kunReadings = listOf(Reading("みず", "kun")),
            meanings = listOf(Meaning("water", "en")),
            grade = 1,
            jlpt = listOf(JlptClassification(5, SourceRef("tanos-jlpt", isCanonical = true))),
            strokeCount = 4
        )

        fun vocab(id: String, expression: String, reading: String, gloss: String, senses: List<Sense> = emptyList()) =
            VocabularyEntry(
                id = EntityId(id),
                expression = expression,
                readings = listOf(
                    VocabularyReading(value = reading, isKanaOnly = JapaneseNormalizer.isKanaOnly(reading))
                ),
                senses = senses,
                kanjiIds = expression.filter { it in "食水" }.map { EntityId("kanji:$it") }
            )

        val eatSense = Sense(
            id = EntityId("sense:taberu:0"),
            vocabularyId = EntityId("vocab:jmdict_1000990"),
            index = 0,
            glosses = listOf(Meaning("to eat", "en"))
        )
        val diningHallSense = Sense(
            id = EntityId("sense:shokudo:0"),
            vocabularyId = EntityId("vocab:jmdict_1001000"),
            index = 0,
            glosses = listOf(Meaning("dining hall", "en"))
        )

        builder.upsertKanji(kanjiEat)
        builder.upsertKanji(kanjiWater)
        builder.upsertVocab(vocab("vocab:jmdict_1000990", "食べる", "たべる", "to eat", listOf(eatSense)))
        builder.upsertVocab(vocab("vocab:jmdict_1001000", "食堂", "しょくどう", "dining hall", listOf(diningHallSense)))
        builder.addSense(eatSense)
        builder.addSense(diningHallSense)

        val dbFile = File(System.getProperty("java.io.tmpdir"), "kjd-fts-${System.nanoTime()}.db")
        dbFile.deleteOnExit()
        DatabaseWriter().write(builder.snapshot(), dbFile, BuiltinSources.all)
        return dbFile
    }

    @Test
    fun exactExpressionLookup() {
        fixtureDatabase().let { file ->
            JapaneseDatabase.open(file).use { db ->
                val results = db.search("食べる")
                assertTrue(results.any { it.entityType == EntityType.Vocabulary && it.displayText == "食べる" })
            }
        }
    }

    @Test
    fun kanjiLookup() {
        fixtureDatabase().let { file ->
            JapaneseDatabase.open(file).use { db ->
                val results = db.search("食")
                assertTrue(results.any { it.entityType == EntityType.Kanji && it.displayText == "食" })
            }
        }
    }

    @Test
    fun readingSearch() {
        fixtureDatabase().let { file ->
            JapaneseDatabase.open(file).use { db ->
                val results = db.search("たべる")
                assertTrue(results.any { it.displayText == "食べる" })
            }
        }
    }

    @Test
    fun katakanaQueryFindsHiraganaReading() {
        // スイ (katakana) must fold to すい and match the 水 on-reading.
        fixtureDatabase().let { file ->
            JapaneseDatabase.open(file).use { db ->
                val results = db.search("スイ")
                assertTrue(results.any { it.displayText == "水" })
            }
        }
    }

    @Test
    fun meaningFullTextSearch() {
        fixtureDatabase().let { file ->
            JapaneseDatabase.open(file).use { db ->
                val results = db.search("eat")
                assertTrue(results.any { it.displayText == "食" || it.displayText == "食べる" })
            }
        }
    }

    @Test
    fun prefixSearch() {
        fixtureDatabase().let { file ->
            JapaneseDatabase.open(file).use { db ->
                val results = db.search("たべ")
                assertTrue(results.any { it.displayText == "食べる" })
            }
        }
    }

    @Test
    fun autocomplete() {
        fixtureDatabase().let { file ->
            JapaneseDatabase.open(file).use { db ->
                val suggestions = db.autocomplete("しょく")
                assertTrue(suggestions.any { it.displayText == "食堂" })
            }
        }
    }

    @Test
    fun typeFilteredSearch() {
        fixtureDatabase().let { file ->
            JapaneseDatabase.open(file).use { db ->
                val kanjiOnly = db.searchByType(EntityType.Kanji, "たべる")
                assertTrue(kanjiOnly.none { it.entityType == EntityType.Vocabulary })
                val vocabOnly = db.searchByType(EntityType.Vocabulary, "たべる")
                assertTrue(vocabOnly.all { it.entityType == EntityType.Vocabulary })
            }
        }
    }

    @Test
    fun searchIsDeterministic() {
        fixtureDatabase().let { file ->
            JapaneseDatabase.open(file).use { db ->
                val first = db.search("たべ")
                val second = db.search("たべ")
                assertEquals(first.map { it.entityId.value }, second.map { it.entityId.value })
            }
        }
    }
}
