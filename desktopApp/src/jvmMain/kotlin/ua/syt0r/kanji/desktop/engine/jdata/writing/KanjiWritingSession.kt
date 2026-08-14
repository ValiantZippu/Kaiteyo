package ua.syt0r.kanji.desktop.engine.jdata.writing

import ua.syt0r.kanji.core.stroke_evaluator.StrokeEvaluation
import ua.syt0r.kanji.core.stroke_evaluator.StrokeSequenceIssueType
import ua.syt0r.kanji.desktop.engine.jdata.api.LanguageDatabase
import ua.syt0r.kanji.desktop.engine.jdata.engine.StrokeEvaluationBridge
import ua.syt0r.kanji.desktop.engine.jdata.engine.WritingStrictness
import ua.syt0r.kanji.desktop.engine.jdata.model.StrokeSet

// ============================================================
// KANJI WRITING SESSION — a real consumer of the platform
//
// Demonstrates the intended integration path for any writing
// practice UI (Kaiteyo desktop, a standalone trainer, a
// third-party app): LanguageDatabase (canonical StrokeSet via
// stable ID) -> StrokeEvaluationBridge (SVG geometry + the core
// stroke evaluators) -> structured result.
//
// The session is deliberately UI-free: callers receive typed
// results plus human-readable [WritingAttempt.summaryLines]
// that a CLI, log or dialog can print directly.
// ============================================================

/** One loaded character ready to be written. */
data class WritingSession(
    val character: String,
    val strokeSet: StrokeSet,
    /** Reference SVG paths in stroke order; empty in counts-only mode. */
    val expectedPaths: List<String>
) {
    val strokeCount: Int get() = strokeSet.strokeCount
    /** True when reference geometry exists for every stroke. */
    val hasGeometry: Boolean get() = expectedPaths.size == strokeCount
    /** Source the stroke set is attributed to (e.g. "kanjivg"), when known. */
    val reference: String? get() = strokeSet.source?.sourceId
}

/** The outcome of submitting one drawn attempt for a session. */
data class WritingAttempt(
    val session: WritingSession,
    val strictness: WritingStrictness,
    val result: StrokeEvaluationBridge.BridgeResult
) {
    val accepted: Boolean get() = result.accepted
    val accuracyPercent: Int get() = result.accuracyPercent()

    /** "Stroke N: Classification (nn%)" for every expected stroke. */
    fun perStrokeLabels(): List<String> =
        result.perStroke.mapIndexed { index, evaluation ->
            "Stroke ${index + 1}: ${evaluation.classification.name} (${evaluation.metrics.accuracyPercent()}%)"
        }

    /**
     * Everything a caller needs to render or log: geometry status, structural
     * issues, per-stroke scores, sequence issues and the verdict.
     */
    fun summaryLines(): List<String> {
        val lines = mutableListOf<String>()
        lines += "Writing check: ${session.character} " +
            "(${result.expectedCount} stroke(s), ${strictness.name.lowercase()} strictness)"
        lines += if (result.geometryAvailable) {
            "  Geometry   : ${session.reference ?: "reference"} (${result.expectedCount} path(s))"
        } else {
            "  Geometry   : unavailable — structural (count) check only"
        }
        lines += "  Drawn      : ${result.drawnCount} stroke(s)"
        result.structuralIssues.forEach { lines += "  Structural : $it" }
        if (result.sequence != null) {
            lines += "  Accuracy   : ${result.accuracyPercent()}%"
            perStrokeLabels().forEach { lines += "    $it" }
            result.issues.forEach { issue ->
                val text = when (issue.type) {
                    StrokeSequenceIssueType.WrongOrder ->
                        "Wrong order: stroke ${(issue.expectedIndex ?: 0) + 1} drawn before earlier strokes"
                    StrokeSequenceIssueType.MissingStroke ->
                        "Missing stroke: expected stroke ${(issue.expectedIndex ?: 0) + 1} never drawn"
                    StrokeSequenceIssueType.ExtraStroke ->
                        "Extra stroke: drawn #${(issue.drawnIndex ?: 0) + 1} matches no expected stroke"
                }
                lines += "  Issue      : $text"
            }
        }
        // In counts-only mode there is no geometric score — never print a
        // misleading "PASS (0%)".
        val score = if (result.geometryAvailable) "${result.accuracyPercent()}%" else "structural"
        lines += if (accepted) "  Verdict    : PASS ($score)"
        else "  Verdict    : FAIL ($score)"
        return lines
    }
}

/**
 * Session runner: loads canonical stroke data through [LanguageDatabase] and
 * evaluates attempts through [StrokeEvaluationBridge]. Thread-safe and stateless
 * beyond its two dependencies.
 */
class KanjiWritingSession(
    private val database: LanguageDatabase,
    private val bridge: StrokeEvaluationBridge = StrokeEvaluationBridge()
) {

    /** Loads a writing session for [character]; null when the platform has no stroke data. */
    fun begin(character: String): WritingSession? {
        val set = database.getStrokeData(character) ?: return null
        return WritingSession(
            character = character,
            strokeSet = set,
            expectedPaths = set.strokes.mapNotNull { it.path }
        )
    }

    /** Evaluates a full drawn attempt. */
    fun submit(
        session: WritingSession,
        drawnPaths: List<String>,
        strictness: WritingStrictness
    ): WritingAttempt =
        WritingAttempt(session, strictness, bridge.evaluate(session.strokeSet, drawnPaths, strictness))

    /**
     * Live per-stroke feedback while the user draws: compares the latest drawn
     * stroke against the expected stroke at [expectedIndex]. Returns null when
     * either side has no usable geometry.
     */
    fun liveStroke(
        session: WritingSession,
        expectedIndex: Int,
        drawnPath: String,
        strictness: WritingStrictness
    ): StrokeEvaluation? {
        val expected = session.expectedPaths.getOrNull(expectedIndex) ?: return null
        return bridge.evaluateStroke(expected, drawnPath, strictness)
    }

}
