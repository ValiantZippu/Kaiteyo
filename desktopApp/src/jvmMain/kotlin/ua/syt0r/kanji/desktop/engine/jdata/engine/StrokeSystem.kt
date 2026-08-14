package ua.syt0r.kanji.desktop.engine.jdata.engine

import ua.syt0r.kanji.desktop.engine.dictionary.KanjiSpelling
import ua.syt0r.kanji.desktop.engine.jdata.model.Bounds
import ua.syt0r.kanji.desktop.engine.jdata.model.KanaEntry
import ua.syt0r.kanji.desktop.engine.jdata.model.KanjiEntry
import ua.syt0r.kanji.desktop.engine.jdata.model.SourceRef
import ua.syt0r.kanji.desktop.engine.jdata.model.StrokeEntry
import ua.syt0r.kanji.desktop.engine.jdata.model.StrokeSet
import ua.syt0r.kanji.desktop.engine.jdata.model.StableIds

// ============================================================
// STROKE DATA ENGINE
// Stroke sets with per-stroke SVG geometry, computed bounds and a
// writing-evaluation foundation. The geometry API is UI-independent:
// renderers, animators and evaluators all consume [StrokeSet] /
// [StrokeEntry] / [Bounds] — never Compose types.
//
// Raw stroke path data (KanjiVG) is a *source* like any other: the
// platform ships the parsing/validation machinery and the canonical
// StrokeSet model, and consumes path data when a licensed dataset
// is installed. Without one, stroke *counts* are still available.
// ============================================================

/** Pluggable provider of per-character stroke path geometry. */
interface StrokeGeometryProvider {
    /** SVG path data per stroke index, in correct order. Empty when unavailable. */
    fun strokesFor(character: String): List<String>
}

/** Provider that has no geometry data — counts-only mode. */
object NoStrokeGeometryProvider : StrokeGeometryProvider {
    override fun strokesFor(character: String): List<String> = emptyList()
}

object StrokeSystem {

    /** Stroke set from a kanji spelling (counts; geometry filled by a provider). */
    fun fromKanji(spelling: KanjiSpelling, sourceId: String, geometry: StrokeGeometryProvider): StrokeSet? {
        val count = spelling.strokeCounts.firstOrNull() ?: return null
        if (count <= 0) return null
        val character = spelling.character
        val paths = geometry.strokesFor(character)
        val strokes = if (paths.size == count) {
            paths.mapIndexed { index, path ->
                StrokeEntry(index = index, path = path, bounds = SvgPathBounds.of(path))
            }
        } else {
            (0 until count).map { StrokeEntry(index = it) }
        }
        return StrokeSet(
            character = character,
            strokeCount = count,
            strokes = strokes,
            source = SourceRef(sourceId, character)
        )
    }

    /** Stroke set for a kana character with a reference stroke count. */
    fun fromKana(kana: KanaEntry): StrokeSet? {
        val count = kana.strokeCount ?: return null
        return StrokeSet(
            character = kana.character,
            strokeCount = count,
            strokes = (0 until count).map { StrokeEntry(index = it) },
            source = kana.sources.firstOrNull()
        )
    }

    /**
     * All stroke sets for the platform (kanji + kana). When [geometry] supplies
     * real paths and [geometrySourceId] is set, the stroke set is attributed to
     * that source (e.g. "kanjivg") instead of the dictionary that only provided
     * the stroke count.
     */
    fun fromPlatform(
        kanji: Map<String, KanjiEntry>,
        kana: Map<String, KanaEntry>,
        geometry: StrokeGeometryProvider,
        geometrySourceId: String? = null
    ): Map<String, StrokeSet> {
        val result = linkedMapOf<String, StrokeSet>()
        kanji.values.forEach { entry ->
            entry.strokeCount?.let { count ->
                if (count > 0) {
                    val paths = geometry.strokesFor(entry.character)
                    val hasGeometry = paths.size == count
                    result[StableIds.strokeSet(entry.character)] = StrokeSet(
                        character = entry.character,
                        strokeCount = count,
                        strokes = if (hasGeometry) {
                            paths.mapIndexed { index, path ->
                                StrokeEntry(index, path, SvgPathBounds.of(path))
                            }
                        } else (0 until count).map { StrokeEntry(index = it) },
                        source = when {
                            hasGeometry && geometrySourceId != null -> SourceRef(geometrySourceId, entry.character)
                            else -> entry.sources.firstOrNull()
                        }
                    )
                }
            }
        }
        kana.values.forEach { entry ->
            entry.strokeCount?.let { count ->
                if (count > 0) {
                    result[StableIds.strokeSet(entry.character)] = StrokeSet(
                        character = entry.character,
                        strokeCount = count,
                        strokes = (0 until count).map { StrokeEntry(index = it) },
                        source = entry.sources.firstOrNull()
                    )
                }
            }
        }
        return result
    }
}

/**
 * Numeric SVG path parser: extracts the bounding box from M/L/H/V/C/S/Q/A/Z
 * commands. Purely geometric — no rendering, no Compose. Returns null when
 * the path contains no usable coordinates.
 */
object SvgPathBounds {

    private val tokenRegex = Regex("-?\\d+\\.?\\d*(?:[eE][-+]?\\d+)?|[A-Za-z]")

    fun of(path: String): Bounds? {
        val tokens = tokenRegex.findAll(path).map { it.value }.toList()
        if (tokens.isEmpty()) return null

        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        var cursorX = 0f
        var cursorY = 0f
        var command = 'm'
        var i = 0

        fun isNumber(token: String): Boolean =
            token.first().isDigit() || token.first() == '-'

        fun next(): Float = tokens[i++].toFloat()

        fun record(x: Float, y: Float) {
            cursorX = x
            cursorY = y
            if (x < minX) minX = x
            if (x > maxX) maxX = x
            if (y < minY) minY = y
            if (y > maxY) maxY = y
        }

        while (i < tokens.size) {
            val token = tokens[i]
            if (!isNumber(token)) {
                command = token[0].lowercaseChar()
                i++
                continue
            }
            when (command) {
                'm', 'l' -> {
                    record(next(), next())
                }
                'h' -> record(next(), cursorY)
                'v' -> record(cursorX, next())
                'c' -> {
                    next(); next(); next(); next()
                    record(next(), next())
                }
                's', 'q' -> {
                    next(); next()
                    record(next(), next())
                }
                't' -> record(next(), next())
                'a' -> {
                    next(); next(); next(); next(); next()
                    record(next(), next())
                }
                'z' -> Unit // close path: no new coordinate
                else -> {
                    if (i + 1 < tokens.size) record(next(), next())
                }
            }
        }
        if (minX > maxX) return null
        return Bounds(minX, minY, maxX, maxY)
    }
}

// ============================================================
// WRITING EVALUATION FOUNDATION
// Explicit configuration for stroke-sequence checking. The actual
// stroke evaluator lives in the app layer; this provides the
// canonical validation rules against the platform's stroke data.
// ============================================================

enum class WritingStrictness { Relaxed, Normal, Exam }

class WritingEvaluationConfig(
    val strictness: WritingStrictness = WritingStrictness.Normal,
    val orderSensitive: Boolean = true,
    val positionTolerance: Float = 0.12f,
    val directionTolerance: Float = 0.15f
)

object StrokeSequenceValidator {

    data class StrokeValidationResult(val valid: Boolean, val issues: List<String>)

    /** The written stroke count must match the canonical set. */
    fun validateCount(declared: Int, actual: Int, strictness: WritingStrictness = WritingStrictness.Normal): StrokeValidationResult {
        val issues = mutableListOf<String>()
        if (actual != declared) {
            val tolerance = when (strictness) {
                WritingStrictness.Relaxed -> 2
                WritingStrictness.Normal -> 0
                WritingStrictness.Exam -> 0
            }
            if (kotlin.math.abs(actual - declared) > tolerance) {
                issues.add("Stroke count mismatch: wrote $actual, expected $declared")
            }
        }
        return StrokeValidationResult(issues.isEmpty(), issues)
    }

    /**
     * The written stroke indices must be a permutation of the canonical
     * order. In exam strictness, any deviation fails; otherwise a single
     * adjacent swap (e.g. 0,2,1,3) is tolerated as an order slip.
     */
    fun validateOrder(
        expected: List<Int>,
        actual: List<Int>,
        strictness: WritingStrictness
    ): StrokeValidationResult {
        val issues = mutableListOf<String>()
        if (actual.size != expected.size) {
            issues.add("Stroke sequence length mismatch")
            return StrokeValidationResult(false, issues)
        }
        if (actual == expected) return StrokeValidationResult(true, emptyList())
        val inversions = actual.zip(expected).count { (a, e) -> a != e }
        val isPermutation = actual.sorted() == expected.sorted()
        if (!isPermutation) {
            issues.add("Stroke sequence is not a permutation of the expected order")
        } else if (strictness == WritingStrictness.Exam && inversions > 0) {
            issues.add("Exam strictness: stroke order deviation detected")
        } else if (inversions > 2) {
            issues.add("Stroke order deviates in $inversions positions")
        }
        return StrokeValidationResult(issues.isEmpty(), issues)
    }

    /** Full check: count + order + structural sanity of a StrokeSet. */
    fun validateSequence(set: StrokeSet, actualIndices: List<Int>, config: WritingEvaluationConfig): StrokeValidationResult {
        val issues = mutableListOf<String>()
        val countResult = validateCount(set.strokeCount, actualIndices.size, config.strictness)
        issues += countResult.issues
        if (issues.isEmpty()) {
            val orderResult = validateOrder(set.strokeOrder, actualIndices, config.strictness)
            issues += orderResult.issues
        }
        set.strokes.forEach { stroke ->
            if (stroke.index < 0 || stroke.index >= set.strokeCount) {
                issues.add("Stroke index ${stroke.index} out of range for ${set.character}")
            }
        }
        return StrokeValidationResult(issues.isEmpty(), issues)
    }
}
