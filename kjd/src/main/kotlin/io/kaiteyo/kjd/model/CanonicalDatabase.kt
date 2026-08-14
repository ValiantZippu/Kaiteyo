package io.kaiteyo.kjd.model

import io.kaiteyo.kjd.resolve.CanonicalDatabaseBuilder

/**
 * An immutable snapshot of the resolved canonical database, ready for
 * validation, generation and export. Produced by [CanonicalDatabaseBuilder.snapshot].
 */
class CanonicalDatabase private constructor(
    val kanji: List<Kanji>,
    val kana: List<KanaCharacter>,
    val vocabulary: List<VocabularyEntry>,
    val senses: List<Sense>,
    val radicals: List<Radical>,
    val components: List<Component>,
    val exampleSentences: List<ExampleSentence>,
    val relationships: List<Relationship>,
    val tags: List<Tag>
) {
    companion object {
        fun from(builder: CanonicalDatabaseBuilder): CanonicalDatabase =
            CanonicalDatabase(
                kanji = builder.kanji,
                kana = builder.kana,
                vocabulary = builder.vocabulary,
                senses = builder.allSenses,
                radicals = builder.radicals,
                components = builder.components,
                exampleSentences = builder.exampleSentences,
                relationships = builder.relationships,
                tags = builder.tags
            )
    }
}

/** Convenience accessors used across the pipeline. */
val CanonicalDatabase.allCharacters: List<Character>
    get() = kanji.map { it.character } + kana.map { it.character }

fun CanonicalDatabase.findKanji(literal: String): Kanji? =
    kanji.firstOrNull { it.character.literal == literal }

fun CanonicalDatabase.findVocabulary(expression: String): VocabularyEntry? =
    vocabulary.firstOrNull { it.expression == expression }
        ?: vocabulary.firstOrNull { entry -> entry.readings.any { it.value == expression } }
