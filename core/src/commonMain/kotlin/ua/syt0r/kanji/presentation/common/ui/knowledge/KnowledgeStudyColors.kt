package ua.syt0r.kanji.presentation.common.ui.knowledge

import androidx.compose.ui.graphics.Color
import ua.syt0r.kanji.core.knowledge.StudyState

// ============================================================
// STUDY-STATE COLORS (KT-THEME-003)
// ------------------------------------------------------------
// One shared mapping from study state → color, used by the kanji
// and word entry pages. The palette is stable across themes by
// design (state colors are semantic, like edge colors in the
// graph) — but it lives in exactly one place, so a change or a
// themed variant never drifts between screens.
// ============================================================

/** Semantic color for a study state. */
fun studyStateColor(state: StudyState): Color = when (state) {
    StudyState.New -> Color(0xFF90A4AE)
    StudyState.Learning -> Color(0xFF42A5F5)
    StudyState.Known -> Color(0xFF66BB6A)
    StudyState.Due -> Color(0xFFFFB74D)
    StudyState.Mastered -> Color(0xFF26A69A)
    StudyState.Relearning -> Color(0xFFEF5350)
    StudyState.Suspended -> Color(0xFFBDBDBD)
}
