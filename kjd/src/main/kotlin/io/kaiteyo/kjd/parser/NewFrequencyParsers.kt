package io.kaiteyo.kjd.parser

import io.kaiteyo.kjd.model.FrequencyRecord
import io.kaiteyo.kjd.source.SourceIds
import io.kaiteyo.kjd.source.SourceMetadata
import io.kaiteyo.kjd.source.toSourceRef
import java.io.File

/**
 * Parser for Netflix Japanese Frequency List.
 * Format: word<TAB>frequency<TAB>rank
 */
object NetflixFrequencyParser : SourceParser<FrequencyRecord> {
    override val sourceId: String = SourceIds.NETFLIX_FREQUENCY

    override fun parse(file: File, metadata: SourceMetadata): ParseResult<FrequencyRecord> {
        val results = mutableListOf<FrequencyRecord>()
        val failures = mutableListOf<ParseFailure>()
        val sourceRef = metadata.toSourceRef(transformation = "parsed")

        file.bufferedReader().use { reader ->
            reader.lineSequence().forEachIndexed { idx, line ->
                if (line.isBlank() || line.startsWith("#")) return@forEachIndexed
                try {
                    val parts = line.split("\t")
                    if (parts.size >= 2) {
                        val freq = parts[1].trim().toIntOrNull() ?: return@forEachIndexed
                        results.add(FrequencyRecord(
                            value = freq,
                            source = sourceRef,
                            methodology = "Netflix Japanese subtitle frequency"
                        ))
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
 * Parser for Chris Kempson's Japanese Subtitles Frequency.
 * Format: word<TAB>frequency
 */
object KempsonFrequencyParser : SourceParser<FrequencyRecord> {
    override val sourceId: String = SourceIds.KEMPSON_FREQUENCY

    override fun parse(file: File, metadata: SourceMetadata): ParseResult<FrequencyRecord> {
        val results = mutableListOf<FrequencyRecord>()
        val failures = mutableListOf<ParseFailure>()
        val sourceRef = metadata.toSourceRef(transformation = "parsed")

        file.bufferedReader().use { reader ->
            reader.lineSequence().forEachIndexed { idx, line ->
                if (line.isBlank() || line.startsWith("#")) return@forEachIndexed
                try {
                    val parts = line.split("\t")
                    if (parts.size >= 2) {
                        val freq = parts[1].trim().toIntOrNull() ?: return@forEachIndexed
                        results.add(FrequencyRecord(
                            value = freq,
                            source = sourceRef,
                            methodology = "Japanese subtitle word frequency (Kempson)"
                        ))
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
 * Parser for Patrick Kandrac's 2242 Kanji Frequency.
 * Format: rank<TAB>kanji<TAB>keyword
 */
object Kandrac2242Parser : SourceParser<FrequencyRecord> {
    override val sourceId: String = SourceIds.KANDRAC_2242

    override fun parse(file: File, metadata: SourceMetadata): ParseResult<FrequencyRecord> {
        val results = mutableListOf<FrequencyRecord>()
        val failures = mutableListOf<ParseFailure>()
        val sourceRef = metadata.toSourceRef(transformation = "parsed")

        file.bufferedReader().use { reader ->
            reader.lineSequence().forEachIndexed { idx, line ->
                if (line.isBlank() || line.startsWith("#") || line.startsWith("Rank")) return@forEachIndexed
                try {
                    val parts = line.split("\t")
                    if (parts.size >= 2) {
                        val rank = parts[0].trim().toIntOrNull() ?: return@forEachIndexed
                        results.add(FrequencyRecord(
                            value = rank,
                            source = sourceRef,
                            methodology = "Kandrac 2242 kanji frequency (Google/KUF/MCD/文化庁)"
                        ))
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
 * Parser for Nukemarine RTK Frequency Groups.
 * Format: group_number<TAB>kanji_list (comma or space separated)
 */
object NukemarineRtkParser : SourceParser<FrequencyRecord> {
    override val sourceId: String = SourceIds.NUKEMARINE_RTK

    override fun parse(file: File, metadata: SourceMetadata): ParseResult<FrequencyRecord> {
        val results = mutableListOf<FrequencyRecord>()
        val failures = mutableListOf<ParseFailure>()
        val sourceRef = metadata.toSourceRef(transformation = "parsed")

        file.bufferedReader().use { reader ->
            var groupRank = 0
            reader.lineSequence().forEachIndexed { idx, line ->
                if (line.isBlank() || line.startsWith("#")) return@forEachIndexed
                try {
                    groupRank++
                    line.trim().split("\\s+".toRegex()).forEach { kanji ->
                        if (kanji.isNotEmpty() && kanji[0] in '\u4E00'..'\u9FFF') {
                            results.add(FrequencyRecord(
                                value = groupRank,
                                source = sourceRef,
                                methodology = "Nukemarine RTK frequency group $groupRank"
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

/**
 * Parser for Yatskov's Wikipedia Kanji Frequency.
 * Format: kanji<TAB>frequency<TAB>rank
 */
object YatskovWikipediaParser : SourceParser<FrequencyRecord> {
    override val sourceId: String = SourceIds.YATSKOV_WIKIPEDIA

    override fun parse(file: File, metadata: SourceMetadata): ParseResult<FrequencyRecord> {
        val results = mutableListOf<FrequencyRecord>()
        val failures = mutableListOf<ParseFailure>()
        val sourceRef = metadata.toSourceRef(transformation = "parsed")

        file.bufferedReader().use { reader ->
            reader.lineSequence().forEachIndexed { idx, line ->
                if (line.isBlank() || line.startsWith("#")) return@forEachIndexed
                try {
                    val parts = line.split("\t")
                    if (parts.size >= 2) {
                        val freq = parts[1].trim().toIntOrNull() ?: return@forEachIndexed
                        results.add(FrequencyRecord(
                            value = freq,
                            source = sourceRef,
                            methodology = "Wikipedia Japanese kanji frequency (Yatskov)"
                        ))
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
 * Parser for Alexandre Girardi's Word Frequency.
 * Format: word<TAB>frequency<TAB>rank
 */
object GirardiWordFreqParser : SourceParser<FrequencyRecord> {
    override val sourceId: String = SourceIds.GIRARDI_WORD_FREQ

    override fun parse(file: File, metadata: SourceMetadata): ParseResult<FrequencyRecord> {
        val results = mutableListOf<FrequencyRecord>()
        val failures = mutableListOf<ParseFailure>()
        val sourceRef = metadata.toSourceRef(transformation = "parsed")

        file.bufferedReader().use { reader ->
            reader.lineSequence().forEachIndexed { idx, line ->
                if (line.isBlank() || line.startsWith("#")) return@forEachIndexed
                try {
                    val parts = line.split("\t")
                    if (parts.size >= 2) {
                        val freq = parts[1].trim().toIntOrNull() ?: return@forEachIndexed
                        results.add(FrequencyRecord(
                            value = freq,
                            source = sourceRef,
                            methodology = "Girardi Japanese word frequency"
                        ))
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
 * Parser for Shpika's Kanji Keys / TopoKanji data.
 * Format varies but typically: kanji<TAB>metadata
 */
object ShpikaParser : SourceParser<FrequencyRecord> {
    override val sourceId: String = SourceIds.SHPIKA_KANJI_KEYS

    override fun parse(file: File, metadata: SourceMetadata): ParseResult<FrequencyRecord> {
        val results = mutableListOf<FrequencyRecord>()
        val failures = mutableListOf<ParseFailure>()
        val sourceRef = metadata.toSourceRef(transformation = "parsed")

        file.bufferedReader().use { reader ->
            reader.lineSequence().forEachIndexed { idx, line ->
                if (line.isBlank() || line.startsWith("#")) return@forEachIndexed
                try {
                    val parts = line.split("\t")
                    if (parts.size >= 2) {
                        val freq = parts.last().trim().toIntOrNull()
                        if (freq != null) {
                            results.add(FrequencyRecord(
                                value = freq,
                                source = sourceRef,
                                methodology = "Shpika Kanji Keys frequency"
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
