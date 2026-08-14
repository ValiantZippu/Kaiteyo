package io.kaiteyo.kjd.patch

import io.kaiteyo.kjd.db.DatabaseWriter
import io.kaiteyo.kjd.model.Character
import io.kaiteyo.kjd.model.CharacterType
import io.kaiteyo.kjd.model.EntityId
import io.kaiteyo.kjd.model.Kanji
import io.kaiteyo.kjd.model.Meaning
import io.kaiteyo.kjd.model.Reading
import io.kaiteyo.kjd.model.Sense
import io.kaiteyo.kjd.model.VocabularyEntry
import io.kaiteyo.kjd.model.VocabularyReading
import io.kaiteyo.kjd.normalize.JapaneseNormalizer
import io.kaiteyo.kjd.resolve.CanonicalDatabaseBuilder
import io.kaiteyo.kjd.source.BuiltinSources
import java.io.File

/**
 * Shared fixture builders for the incremental update tests.
 *
 * The "old" release contains 食 + 水 and vocabulary 食べる (with one sense).
 * The "new" release: adds 山, updates 水's meanings, removes 食べる (and its
 * sense) and adds 食堂 (with one sense) — i.e. a realistic cross-release
 * delta covering inserts, updates and deletes across kanji, vocab and senses.
 */
object PatchTestFixtures {

    fun buildOldDatabase(): File {
        val builder = CanonicalDatabaseBuilder()
        builder.upsertKanji(kanji("食", on = "ショク", kun = "く.う", meanings = listOf("eat"), grade = 2))
        builder.upsertKanji(kanji("水", on = "スイ", kun = "みず", meanings = listOf("water"), grade = 1))
        vocab(
            builder,
            id = "vocab:taberu",
            expression = "食べる",
            reading = "たべる",
            senseId = "sense:taberu:0",
            gloss = "to eat",
            kanjiIds = listOf("kanji:食")
        )
        return write(builder)
    }

    fun buildNewDatabase(): File {
        val builder = CanonicalDatabaseBuilder()
        builder.upsertKanji(kanji("食", on = "ショク", kun = "く.う", meanings = listOf("eat"), grade = 2))
        builder.upsertKanji(
            kanji("水", on = "スイ", kun = "みず", meanings = listOf("water", "Wednesday"), grade = 1)
        )
        builder.upsertKanji(kanji("山", on = "サン", kun = "やま", meanings = listOf("mountain"), grade = 1))
        vocab(
            builder,
            id = "vocab:shokudo",
            expression = "食堂",
            reading = "しょくどう",
            senseId = "sense:shokudo:0",
            gloss = "dining hall",
            kanjiIds = listOf("kanji:食")
        )
        return write(builder)
    }

    fun buildUnrelatedDatabase(): File {
        val builder = CanonicalDatabaseBuilder()
        builder.upsertKanji(kanji("火", on = "カ", meanings = listOf("fire"), grade = 1))
        return write(builder)
    }

    private fun write(builder: CanonicalDatabaseBuilder): File {
        val file = File.createTempFile("kjd-patch-fixture-", ".db")
        file.deleteOnExit()
        DatabaseWriter().write(builder.snapshot(), file, BuiltinSources.all)
        return file
    }

    private fun kanji(
        literal: String,
        on: String,
        meanings: List<String>,
        kun: String = "",
        grade: Int? = null
    ): Kanji {
        val id = EntityId("kanji:$literal")
        return Kanji(
            id = id,
            character = Character(
                id = id,
                literal = literal,
                codepoint = literal.first().code,
                normalized = JapaneseNormalizer.toNfc(literal),
                characterType = CharacterType.Kanji,
                grade = grade
            ),
            onReadings = listOf(Reading(on, "on")),
            kunReadings = if (kun.isBlank()) emptyList() else listOf(Reading(kun, "kun")),
            meanings = meanings.map { Meaning(it, "en") },
            grade = grade
        )
    }

    private fun vocab(
        builder: CanonicalDatabaseBuilder,
        id: String,
        expression: String,
        reading: String,
        senseId: String,
        gloss: String,
        kanjiIds: List<String>
    ) {
        val entryId = EntityId(id)
        val sense = Sense(
            id = EntityId(senseId),
            vocabularyId = entryId,
            index = 0,
            glosses = listOf(Meaning(gloss, "en")),
            partsOfSpeech = listOf(io.kaiteyo.kjd.model.PartOfSpeech("noun"))
        )
        builder.addSense(sense)
        builder.upsertVocab(
            VocabularyEntry(
                id = entryId,
                expression = expression,
                readings = listOf(
                    VocabularyReading(value = reading, isKanaOnly = JapaneseNormalizer.isKanaOnly(reading))
                ),
                senses = listOf(sense),
                kanjiIds = kanjiIds.map { EntityId(it) }
            )
        )
    }
}
