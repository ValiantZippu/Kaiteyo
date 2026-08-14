package io.kaiteyo.kjd.parser

import io.kaiteyo.kjd.source.SourceMetadata
import java.io.File

/**
 * Raw JLPT classification record. The Tanos JLPT dataset is a set of CSV/text
 * lists, one per level (N5..N1). Each row associates a kanji or word with a
 * level. Level is 5 (N5) .. 1 (N1).
 */
data class RawJlptClassification(
    val item: String,
    val level: Int
)

/**
 * Parses the Tanos JLPT kanji/vocab lists. The standard distribution ships
 * one file per level where each line is a kanji or word. Some variants use a
 * `level,item` CSV; the parser accepts both.
 */
class TanosJlptParser : SourceParser<RawJlptClassification> {

    override val sourceId: String = "tanos-jlpt"

    override fun parse(file: File, metadata: SourceMetadata): ParseResult<RawJlptClassification> {
        val parsed = mutableListOf<RawJlptClassification>()
        val rejected = mutableListOf<ParseFailure>()

        // Infer the level from the filename first (e.g. "jlpt-n5-kanji.txt").
        val levelFromName = Regex("(?:n|jlpt[-_]?)([1-5])", RegexOption.IGNORE_CASE)
            .find(file.name)?.groupValues?.get(1)?.toIntOrNull()

        try {
            file.forEachLine { rawLine ->
                val line = rawLine.trim()
                if (line.isEmpty() || line.startsWith("#")) return@forEachLine

                // CSV: "5,食べる" or "食べる,5" — detect level column by value.
                val csv = line.split(',', '\t').map { it.trim() }.filter { it.isNotEmpty() }
                if (csv.size >= 2) {
                    val level = csv.firstOrNull { it.toIntOrNull() in 1..5 }?.toIntOrNull()
                        ?: csv.lastOrNull { it.toIntOrNull() in 1..5 }?.toIntOrNull()
                        ?: levelFromName
                    val item = csv.firstOrNull { it.toIntOrNull() == null }
                    if (item != null && level != null) {
                        parsed.add(RawJlptClassification(item, level))
                        return@forEachLine
                    }
                }

                val level = levelFromName
                if (level != null) {
                    parsed.add(RawJlptClassification(line, level))
                }
            }
        } catch (t: Throwable) {
            rejected.add(ParseFailure(recordId = file.name, reason = "Parse failed: ${t.summary()}", exception = t))
        }

        return ParseResult(metadata, parsed, rejected)
    }
}

/**
 * Raw frequency record. The Leeds frequency dataset provides a ranked list
 * of the most frequent Japanese words. Rank 1 = most frequent. The parser
 * is row-order aware: the line number (1-based) is the rank.
 */
data class RawFrequencyRecord(
    val item: String,
    val rank: Int
)

/**
 * Parses the Leeds Internet corpus frequency list. Each line contains a word
 * (optionally prefixed by a rank when exported from a processed copy).
 * Lines with a leading number use it as the rank; otherwise the line index is
 * the rank.
 */
class LeedsFrequencyParser : SourceParser<RawFrequencyRecord> {

    override val sourceId: String = "leeds-frequency"

    override fun parse(file: File, metadata: SourceMetadata): ParseResult<RawFrequencyRecord> {
        val parsed = mutableListOf<RawFrequencyRecord>()
        val rejected = mutableListOf<ParseFailure>()

        try {
            var lineIndex = 0
            file.forEachLine { rawLine ->
                lineIndex++
                val line = rawLine.trim()
                if (line.isEmpty() || line.startsWith("#")) return@forEachLine

                val parts = line.split(Regex("\\s+")).filter { it.isNotEmpty() }
                val leadingNumber = parts.firstOrNull()?.toIntOrNull()
                val item = if (leadingNumber != null) parts.getOrNull(1) else parts.firstOrNull()
                val rank = leadingNumber ?: lineIndex
                if (item != null && item.isNotEmpty() && item.any { it.isLetter() }) {
                    parsed.add(RawFrequencyRecord(item, rank))
                } else {
                    rejected.add(ParseFailure(recordId = lineIndex.toString(), reason = "Unparseable frequency row"))
                }
            }
        } catch (t: Throwable) {
            rejected.add(ParseFailure(recordId = file.name, reason = "Parse failed: ${t.summary()}", exception = t))
        }

        return ParseResult(metadata, parsed, rejected)
    }
}

/**
 * Raw JLPT vocabulary classification from the yomichan-jlpt-vocab dataset.
 * The dataset is a JSON array of terms with a "tags" field containing
 * "jlpt-n5".."jlpt-n1".
 */
data class RawYomichanJlptVocab(
    val expression: String,
    val reading: String?,
    val level: Int
)
