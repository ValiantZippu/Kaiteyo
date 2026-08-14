package io.kaiteyo.kjd.parser

import io.kaiteyo.kjd.source.SourceMetadata
import java.io.File

/**
 * Result of parsing one source artifact.
 *
 * [parsed] contains the raw structured records extracted from the file —
 * before normalization. Raw extraction and canonicalization are deliberately
 * separate stages.
 */
class ParseResult<out T>(
    val source: SourceMetadata,
    val parsed: List<T>,
    val rejected: List<ParseFailure>
)

/** A single record that could not be parsed, with a reason. */
data class ParseFailure(
    val recordId: String?,
    val reason: String,
    val exception: Throwable? = null
)

/**
 * A parser turns a raw source artifact into structured raw records.
 *
 * Implementations must never mutate the input file. They should be
 * deterministic and report failures instead of swallowing malformed records.
 */
interface SourceParser<out T> {
    val sourceId: String

    /** Parse the file and return raw records + failure list. */
    fun parse(file: File, metadata: SourceMetadata): ParseResult<T>
}

/** Utility: classifies an exception for error reporting. */
internal fun Throwable.summary(): String =
    message ?: javaClass.simpleName
