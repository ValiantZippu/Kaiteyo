package io.kaiteyo.kjd.resolve

import io.kaiteyo.kjd.model.SourceRef
import io.kaiteyo.kjd.parser.RawJmdictEntry
import io.kaiteyo.kjd.parser.RawJmdictSense
import io.kaiteyo.kjd.parser.RawJlptClassification
import io.kaiteyo.kjd.parser.RawJmdictReadingElement
import io.kaiteyo.kjd.parser.RawJmdictKanjiElement
import io.kaiteyo.kjd.parser.RawKanjidicCharacter
import io.kaiteyo.kjd.parser.RawKanjiVgCharacter
import io.kaiteyo.kjd.parser.RawVgStroke
import io.kaiteyo.kjd.parser.RawFrequencyRecord
import io.kaiteyo.kjd.source.BuiltinSources
import io.kaiteyo.kjd.source.SourceIds
import io.kaiteyo.kjd.validate.DatabaseValidator
import io.kaiteyo.kjd.validate.Severity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EntityResolutionTest {

    private fun kanjidicCharacter(
        literal: String,
        on: List<String> = emptyList(),
        kun: List<String> = emptyList(),
        meanings: List<String> = listOf("meaning"),
        grade: Int? = null,
        strokeCount: Int? = null
    ) = RawKanjidicCharacter(
        kanji = literal,
        onReadings = on,
        kunReadings = kun,
        meanings = meanings.map { io.kaiteyo.kjd.parser.RawMeaning(it, "en") },
        grade = grade,
        strokeCount = strokeCount
    )

    private fun kanjiVgCharacter(literal: String, strokeCount: Int) = RawKanjiVgCharacter(
        kanji = literal,
        strokes = (1..strokeCount).map {
            RawVgStroke(index = it, path = "M0,0 L10,10", elementId = "kvg:$it")
        }
    )

    private fun jmdictEntry(seq: Long, expression: String, reading: String, gloss: String) =
        RawJmdictEntry(
            entSeq = seq,
            kanji = listOf(RawJmdictKanjiElement(keb = expression)),
            readings = listOf(RawJmdictReadingElement(reb = reading)),
            senses = listOf(
                RawJmdictSense(
                    pos = listOf("noun"),
                    glosses = listOf(RawJmdictGloss(value = gloss, language = "en"))
                )
            )
        )

    @Test
    fun kanjiMergeAcrossSources() {
        val resolver = EntityResolver()
        resolver.resolve(
            kanjidic = mapOf(SourceIds.KANJIDIC to BuiltinSources.byId(SourceIds.KANJIDIC)),
            kanjiVg = mapOf(SourceIds.KANJIVG to BuiltinSources.byId(SourceIds.KANJIVG)),
            kanjidicCharacters = listOf(
                kanjidicCharacter("食", on = listOf("ショク", "ジキ"), kun = listOf("く.う"), meanings = listOf("eat"), grade = 2, strokeCount = 9)
            ),
            kanjiVgCharacters = listOf(kanjiVgCharacter("食", 9))
        )
        val database = resolver.database().snapshot()

        assertEquals(1, database.kanji.size)
        val kanji = database.kanji.first()
        assertEquals("食", kanji.character.literal)
        assertEquals(listOf("ショク", "ジキ"), kanji.onReadings.map { it.value })
        assertEquals(2, kanji.grade)
        // Stroke data merged from KanjiVG.
        assertEquals(9, kanji.strokes.size)
        assertEquals(9, kanji.strokeCount)
    }

    @Test
    fun vocabularyResolutionWithFurigana() {
        val resolver = EntityResolver()
        resolver.resolve(
            jmdict = mapOf(SourceIds.JMDICT to BuiltinSources.byId(SourceIds.JMDICT)),
            jmdictEntries = listOf(
                jmdictEntry(seq = 1000990, expression = "食べる", reading = "たべる", gloss = "to eat")
            )
        )
        val database = resolver.database().snapshot()

        assertEquals(1, database.vocabulary.size)
        val entry = database.vocabulary.first()
        assertEquals("食べる", entry.expression)
        assertEquals("たべる", entry.readings.first().value)
        assertEquals("to eat", entry.senses.first().glosses.first().value)
        // Cross-link: 食 is a kanji component → vocabulary links to kanji.
        assertTrue(entry.kanjiIds.any { it.value == "kanji:食" })
    }

    @Test
    fun jlptAttachesWithSourceProvenance() {
        val resolver = EntityResolver()
        resolver.resolve(
            kanjidic = mapOf(SourceIds.KANJIDIC to BuiltinSources.byId(SourceIds.KANJIDIC)),
            tanosJlpt = mapOf(SourceIds.TANOS_JLPT to BuiltinSources.byId(SourceIds.TANOS_JLPT)),
            kanjidicCharacters = listOf(kanjidicCharacter("食", meanings = listOf("eat"))),
            tanosJlptRecords = listOf(RawJlptClassification("食", 5))
        )
        val database = resolver.database().snapshot()
        val kanji = database.kanji.first()

        assertEquals(1, kanji.jlpt.size)
        val classification = kanji.jlpt.first()
        assertEquals(5, classification.level)
        assertEquals(SourceIds.TANOS_JLPT, classification.source.sourceId)
        assertTrue(classification.source.isCanonical)
    }

    @Test
    fun frequencyAttachesToVocabulary() {
        val resolver = EntityResolver()
        resolver.resolve(
            jmdict = mapOf(SourceIds.JMDICT to BuiltinSources.byId(SourceIds.JMDICT)),
            leedsFrequency = mapOf(SourceIds.LEEDS_FREQUENCY to BuiltinSources.byId(SourceIds.LEEDS_FREQUENCY)),
            jmdictEntries = listOf(
                jmdictEntry(seq = 1, expression = "食べる", reading = "たべる", gloss = "to eat")
            ),
            leedsFrequencyRecords = listOf(RawFrequencyRecord("食べる", 42))
        )
        val database = resolver.database().snapshot()
        val entry = database.vocabulary.first()
        assertEquals(42, entry.frequency.first().value)
        assertEquals(SourceIds.LEEDS_FREQUENCY, entry.frequency.first().source.sourceId)
    }

    @Test
    fun validationDetectsOrphansAndDuplicates() {
        val builder = CanonicalDatabaseBuilder()
        builder.upsertKanji(
            io.kaiteyo.kjd.model.Kanji(
                id = io.kaiteyo.kjd.model.EntityId("kanji:食"),
                character = io.kaiteyo.kjd.model.Character(
                    id = io.kaiteyo.kjd.model.EntityId("kanji:食"),
                    literal = "食",
                    codepoint = 0x98DF,
                    normalized = "食",
                    characterType = io.kaiteyo.kjd.model.CharacterType.Kanji,
                    strokeCount = 9
                ),
                vocabularyIds = listOf(io.kaiteyo.kjd.model.EntityId("vocab:nonexistent"))
            )
        )
        builder.upsertKanji(
            io.kaiteyo.kjd.model.Kanji(
                id = io.kaiteyo.kjd.model.EntityId("kanji:食"),
                character = io.kaiteyo.kjd.model.Character(
                    id = io.kaiteyo.kjd.model.EntityId("kanji:食"),
                    literal = "水",
                    codepoint = 0x6C34,
                    normalized = "水",
                    characterType = io.kaiteyo.kjd.model.CharacterType.Kanji
                )
            )
        )
        val findings = DatabaseValidator().validate(builder.snapshot())
        assertTrue(findings.any { it.severity == Severity.Fatal && it.message.contains("Duplicate") })
        assertTrue(findings.any { it.severity == Severity.Fatal && it.message.contains("Orphan") })
    }
}
