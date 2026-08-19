package io.kaiteyo.kjd.parser

import io.kaiteyo.kjd.model.Component
import io.kaiteyo.kjd.model.EntityId
import io.kaiteyo.kjd.model.JlptClassification
import io.kaiteyo.kjd.source.SourceIds
import io.kaiteyo.kjd.source.SourceMetadata
import io.kaiteyo.kjd.source.toSourceRef
import java.io.File

/**
 * Parser for CJK Decompositions Data.
 * Format: kanji<TAB>decomposition<TAB>radical
 * Provides component/structural decomposition for kanji.
 */
object CjkDecompositionsParser : SourceParser<Component> {
    override val sourceId: String = SourceIds.CJK_DECOMPOSITIONS

    override fun parse(file: File, metadata: SourceMetadata): ParseResult<Component> {
        val results = mutableListOf<Component>()
        val failures = mutableListOf<ParseFailure>()
        val sourceRef = metadata.toSourceRef(transformation = "parsed")

        file.bufferedReader().use { reader ->
            reader.lineSequence().forEachIndexed { idx, line ->
                if (line.isBlank() || line.startsWith("#")) return@forEachIndexed
                try {
                    val parts = line.split("\t")
                    if (parts.size >= 2) {
                        val kanji = parts[0].trim()
                        val decomposition = parts[1].trim()

                        decomposition.forEach { componentChar ->
                            if (componentChar in '\u4E00'..'\u9FFF' ||
                                componentChar in '\u3400'..'\u4DBF') {
                                results.add(Component(
                                    id = EntityId("comp:${kanji}_${componentChar}"),
                                    character = componentChar.toString(),
                                    role = "graphical",
                                    sources = listOf(sourceRef)
                                ))
                            }
                        }
                    }
                } catch (e: Exception) {
                    failures.add(ParseFailure(recordId = "line:$idx", reason = e.summary(), exception = e))
                }
            }
        }
        return ParseResult(source = metadata, parsed = results, rejected = failures)
    }
}

/**
 * Parser for 文化庁 (Agency for Cultural Affairs) official kanji data.
 * Provides jōyō, jinmeiyō, and educational kanji classifications.
 * Format varies but typically: kanji<TAB>classification<TAB>grade
 */
object BunkachoParser : SourceParser<JlptClassification> {
    override val sourceId: String = SourceIds.BUNKACHO

    override fun parse(file: File, metadata: SourceMetadata): ParseResult<JlptClassification> {
        val results = mutableListOf<JlptClassification>()
        val failures = mutableListOf<ParseFailure>()
        val sourceRef = metadata.toSourceRef(transformation = "parsed")

        file.bufferedReader().use { reader ->
            reader.lineSequence().forEachIndexed { idx, line ->
                if (line.isBlank() || line.startsWith("#")) return@forEachIndexed
                try {
                    val parts = line.split("\t")
                    if (parts.size >= 2) {
                        val classification = parts[1].trim().lowercase()
                        when {
                            classification.contains("学年") -> {
                                val grade = parts.getOrNull(2)?.trim()?.toIntOrNull()
                                if (grade != null) {
                                    val jlptLevel = when (grade) {
                                        in 1..2 -> 5
                                        in 3..4 -> 4
                                        5 -> 3
                                        6 -> 2
                                        else -> 1
                                    }
                                    results.add(JlptClassification(
                                        level = jlptLevel,
                                        source = sourceRef
                                    ))
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    failures.add(ParseFailure(recordId = "line:$idx", reason = e.summary(), exception = e))
                }
            }
        }
        return ParseResult(source = metadata, parsed = results, rejected = failures)
    }
}

/**
 * Parser for Usagi Chan Kanji Phonetics Deck.
 * Provides phonetic component groups for kanji study.
 * Format: phonetic_group<TAB>kanji_list
 */
object UsagiChanPhoneticsParser : SourceParser<Component> {
    override val sourceId: String = SourceIds.USAGI_CHAN_PHONETICS

    override fun parse(file: File, metadata: SourceMetadata): ParseResult<Component> {
        val results = mutableListOf<Component>()
        val failures = mutableListOf<ParseFailure>()
        val sourceRef = metadata.toSourceRef(transformation = "parsed")

        file.bufferedReader().use { reader ->
            reader.lineSequence().forEachIndexed { idx, line ->
                if (line.isBlank() || line.startsWith("#")) return@forEachIndexed
                try {
                    val parts = line.split("\t")
                    if (parts.size >= 2) {
                        val phoneticGroup = parts[0].trim()
                        val kanjiList = parts[1].trim()

                        kanjiList.forEach { kanji ->
                            if (kanji in '\u4E00'..'\u9FFF') {
                                results.add(Component(
                                    id = EntityId("phon:${phoneticGroup}_${kanji}"),
                                    character = kanji.toString(),
                                    role = "phonetic",
                                    sources = listOf(sourceRef)
                                ))
                            }
                        }
                    }
                } catch (e: Exception) {
                    failures.add(ParseFailure(recordId = "line:$idx", reason = e.summary(), exception = e))
                }
            }
        }
        return ParseResult(source = metadata, parsed = results, rejected = failures)
    }
}

/**
 * Parser for David Gouveia's Kanji Data.
 * Provides supplementary kanji metadata.
 */
object DavidGouveiaParser : SourceParser<Component> {
    override val sourceId: String = SourceIds.DAVID_GOUVEIA

    override fun parse(file: File, metadata: SourceMetadata): ParseResult<Component> {
        val results = mutableListOf<Component>()
        val failures = mutableListOf<ParseFailure>()
        val sourceRef = metadata.toSourceRef(transformation = "parsed")

        file.bufferedReader().use { reader ->
            reader.lineSequence().forEachIndexed { idx, line ->
                if (line.isBlank() || line.startsWith("#")) return@forEachIndexed
                try {
                    val parts = line.split("\t")
                    if (parts.size >= 2) {
                        val kanji = parts[0].trim()
                        if (kanji.length == 1 && kanji[0] in '\u4E00'..'\u9FFF') {
                            results.add(Component(
                                id = EntityId("gouveia:${kanji}"),
                                character = kanji,
                                role = "graphical",
                                sources = listOf(sourceRef)
                            ))
                        }
                    }
                } catch (e: Exception) {
                    failures.add(ParseFailure(recordId = "line:$idx", reason = e.summary(), exception = e))
                }
            }
        }
        return ParseResult(source = metadata, parsed = results, rejected = failures)
    }
}
