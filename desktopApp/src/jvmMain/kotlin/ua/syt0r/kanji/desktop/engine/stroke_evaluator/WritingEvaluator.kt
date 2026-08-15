package ua.syt0r.kanji.desktop.engine.stroke_evaluator

import ua.syt0r.kanji.desktop.engine.dictionary.DictionaryRepository
import ua.syt0r.kanji.desktop.engine.jdata.api.LanguageDatabase
import ua.syt0r.kanji.desktop.engine.jdata.engine.StrokeGeometryProvider
import ua.syt0r.kanji.desktop.engine.jdata.engine.WritingStrictness
import ua.syt0r.kanji.desktop.engine.jdata.integration.PlatformBuilder
import ua.syt0r.kanji.desktop.engine.jdata.writing.KanjiWritingSession
import java.io.File

// ============================================================
// WRITING EVALUATOR — the writing-practice evaluation facade.
//
// Two real sources, never fabricated:
//
//   1. KANJIVG (preferred) — when a licensed KanjiVG dataset
//      directory exists (e.g. ~/.kaiteyo/kanjivg/ with kanji/*.svg
//      or kanjivg.xml), the canonical stack is used: the platform
//      database is built from the installed dictionaries with the
//      real SVG geometry, and attempts are evaluated through
//      KanjiWritingSession + StrokeEvaluationBridge (shape,
//      direction, order, strictness). CC BY-SA 3.0 — see
//      KanjiVgSource.Definition; never claimed as Kaiteyo's own.
//
//   2. BUILT-IN — when no KanjiVG directory is present, the
//      built-in canonical dataset (StrokeEvaluator, common kanji
//      subset) is used with the same result shape. Characters
//      outside that subset report unsupported instead of guessing.
//
// The [sourceLabel] on every result tells the UI exactly which
// path produced it — the app never pretends KanjiVG geometry
// exists when it does not.
// ============================================================

/** A unified evaluation result from either source. */
data class WritingEvaluation(
    val expression: String,
    val strokes: List<StrokeEvaluation>,
    val accuracy: Float,
    val supported: Boolean,
    /** "kanjivg" when the real KanjiVG geometry was used, "builtin" otherwise. */
    val sourceLabel: String,
    val referenceStrokeCount: Int,
    val drawnStrokeCount: Int
) {
    val correctStrokes: Int get() = strokes.count { it.correct }

    /** True when the real KanjiVG geometry dataset produced this result. */
    val kanjiVgPresent: Boolean get() = sourceLabel == "kanjivg"
}

/** Converts raw canvas points to an SVG path string in the given grid size. */
fun pointsToSvgPath(points: List<StrokePoint>, grid: Double = 100.0): String {
    if (points.isEmpty()) return ""
    val sb = StringBuilder()
    points.forEachIndexed { index, p ->
        val x = (p.x / grid * 1092.0).toInt().coerceIn(0, 1092)
        val y = (p.y / grid * 1092.0).toInt().coerceIn(0, 1092)
        if (index == 0) sb.append("M$x,$y") else sb.append(" L$x,$y")
    }
    return sb.toString()
}

/**
 * Evaluates a handwriting attempt against the best available canonical
 * stroke data. Thread-safe: the database is built once and only read after.
 */
class WritingEvaluator(
    private val repository: DictionaryRepository?,
    kanjiVgDirectory: File? = defaultKanjiVgDirectory()
) {

    private val geometrySource: GeometrySource =
        if (kanjiVgDirectory != null && kanjiVgDirectory.isDirectory) {
            GeometrySource.KanjiVg(kanjiVgDirectory)
        } else {
            GeometrySource.BuiltIn
        }

    private val database: LanguageDatabase? = runCatching {
        repository?.let { repo ->
            val geometry = when (geometrySource) {
                is GeometrySource.KanjiVg -> ua.syt0r.kanji.desktop.engine.jdata.engine.KanjiVgGeometryProvider(geometrySource.directory)
                else -> ua.syt0r.kanji.desktop.engine.jdata.engine.NoStrokeGeometryProvider
            }
            val data = PlatformBuilder.fromRepository(repo, geometry = geometry)
            if (data.kanji.isEmpty() && data.kana.isEmpty()) null else LanguageDatabase.open(data)
        }
    }.getOrNull()

    private val kanjiSession = database?.let { KanjiWritingSession(it) }

    private val builtIn = StrokeEvaluator

    /** Which geometry source is active (for the UI badge). */
    val activeSource: String get() = geometrySource.label

    /** The KanjiVG directory when present. */
    val kanjiVgPresent: Boolean get() = geometrySource is GeometrySource.KanjiVg

    /** True when we have geometry for this character from the active source. */
    fun supports(expression: String): Boolean =
        expression.length == 1 && when (geometrySource) {
            is GeometrySource.KanjiVg -> kanjiVgHas(expression)
            else -> builtIn.supports(expression)
        }

    private fun kanjiVgHas(expression: String): Boolean {
        val session = kanjiSession?.begin(expression) ?: return false
        return session.hasGeometry
    }

    /**
     * Evaluate a full attempt. [drawnStrokes] are the learner's freehand
     * strokes in drawing order, in the raw canvas coordinate space described
     * by [canvasWidth] / [canvasHeight].
     */
    fun evaluate(
        expression: String,
        drawnStrokes: List<List<StrokePoint>>,
        canvasWidth: Double,
        canvasHeight: Double
    ): WritingEvaluation {
        if (drawnStrokes.isEmpty()) {
            return WritingEvaluation(expression, emptyList(), 0f, supported = false, geometrySource.label, 0, 0)
        }
        return when (geometrySource) {
            is GeometrySource.KanjiVg -> evaluateKanjiVg(expression, drawnStrokes, canvasWidth, canvasHeight)
            else -> evaluateBuiltIn(expression, drawnStrokes, canvasWidth, canvasHeight)
        }
    }

    // ------------------------------------------------------------
    // KanjiVG path — the real canonical stack
    // ------------------------------------------------------------

    private fun evaluateKanjiVg(
        expression: String,
        drawnStrokes: List<List<StrokePoint>>,
        canvasWidth: Double,
        canvasHeight: Double
    ): WritingEvaluation {
        val session = kanjiSession?.begin(expression) ?: return notSupported(expression, drawnStrokes.size, geometrySource.label)
        if (!session.hasGeometry) return notSupported(expression, drawnStrokes.size, geometrySource.label)

        val drawnPaths = drawnStrokes.map { stroke ->
            pointsToSvgPath(stroke, minOf(canvasWidth, canvasHeight).coerceAtLeast(1.0))
        }
        val attempt = kanjiSession.submit(session, drawnPaths, WritingStrictness.Normal)
        val perStroke = attempt.result.perStroke.mapIndexed { index, evaluation ->
            StrokeEvaluation(
                strokeIndex = index,
                correct = evaluation.classification != ua.syt0r.kanji.core.stroke_evaluator.StrokeClassification.Incorrect,
                deviation = evaluation.metrics.accuracyPercent() / 100f,
                directionErrorDegrees = 0f,
                matchedReferenceIndex = index,
                mistake = when (evaluation.classification) {
                    ua.syt0r.kanji.core.stroke_evaluator.StrokeClassification.Correct -> StrokeMistake.None
                    ua.syt0r.kanji.core.stroke_evaluator.StrokeClassification.AlmostCorrect -> StrokeMistake.Shape
                    ua.syt0r.kanji.core.stroke_evaluator.StrokeClassification.Incorrect -> StrokeMistake.ShapeAndDirection
                }
            )
        }
        return WritingEvaluation(
            expression = expression,
            strokes = perStroke,
            accuracy = attempt.result.overallAccuracy.coerceIn(0f, 1f),
            supported = true,
            sourceLabel = geometrySource.label,
            referenceStrokeCount = session.strokeCount,
            drawnStrokeCount = drawnStrokes.size
        )
    }

    // ------------------------------------------------------------
    // Built-in path — same result shape, honest subset
    // ------------------------------------------------------------

    private fun evaluateBuiltIn(
        expression: String,
        drawnStrokes: List<List<StrokePoint>>,
        canvasWidth: Double,
        canvasHeight: Double
    ): WritingEvaluation {
        val result = builtIn.evaluate(expression, drawnStrokes, canvasWidth, canvasHeight)
        if (!result.supported) return notSupported(expression, drawnStrokes.size, geometrySource.label)
        return WritingEvaluation(
            expression = expression,
            strokes = result.strokeEvaluations,
            accuracy = result.accuracy,
            supported = true,
            sourceLabel = geometrySource.label,
            referenceStrokeCount = result.strokeEvaluations.size,
            drawnStrokeCount = drawnStrokes.size
        )
    }

    private fun notSupported(expression: String, drawn: Int, label: String): WritingEvaluation =
        WritingEvaluation(expression, emptyList(), 0f, supported = false, label, 0, drawn)

    // ------------------------------------------------------------
    // Geometry source selection
    // ------------------------------------------------------------

    private sealed interface GeometrySource {
        val label: String
        data class KanjiVg(val directory: File) : GeometrySource {
            override val label: String get() = "kanjivg"
        }
        data object BuiltIn : GeometrySource {
            override val label: String get() = "builtin"
        }
    }

    companion object {
        fun defaultKanjiVgDirectory(): File? {
            val home = System.getProperty("user.home") ?: return null
            return listOf(
                File(home, ".kaiteyo/kanjivg"),
                File(home, "kanjivg")
            ).firstOrNull { it.isDirectory }
        }
    }
}
