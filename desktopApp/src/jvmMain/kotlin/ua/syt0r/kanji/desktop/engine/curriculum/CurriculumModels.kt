package ua.syt0r.kanji.desktop.engine.curriculum

import kotlinx.serialization.Serializable

// ============================================
// KAITEYO CURRICULUM — MODELS
// Courses → lessons → objectives. Objectives
// are measured against REAL study data (cards
// in a deck, distinct reviewed cards, total
// review events) through CurriculumDataSource —
// nothing is fabricated, and a course never
// claims progress the data does not back.
// ============================================

/** How an objective's target is measured. */
@Serializable
enum class CurriculumObjectiveKind {
    /** Count of cards added to a deck (learned vocabulary/kanji). */
    NewCardCount,

    /** Count of distinct cards in a deck that have at least one review. */
    ReviewCount,

    /** Total review events across all decks. */
    TotalReviewCount
}

@Serializable
data class CurriculumObjective(
    val id: String,
    val kind: CurriculumObjectiveKind,
    val target: Int,
    val label: String,
    /** The deck an objective measures; null for global objectives. */
    val deckId: String? = null
)

@Serializable
data class CurriculumLesson(
    val id: String,
    val title: String,
    val description: String = "",
    val objectives: List<CurriculumObjective> = emptyList()
)

@Serializable
data class CurriculumCourse(
    val id: String,
    val title: String,
    val description: String = "",
    val lessons: List<CurriculumLesson> = emptyList()
) {
    val objectiveCount: Int get() = lessons.sumOf { it.objectives.size }
}

/** Persisted learner state: where they are and what they have finished. */
@Serializable
data class CurriculumProgress(
    val activeCourseId: String? = null,
    val activeLessonId: String? = null,
    val completedObjectiveIds: Set<String> = emptySet()
)

/** Snapshot of one objective's live progress for the UI. */
data class CurriculumObjectiveStatus(
    val objective: CurriculumObjective,
    val progress: Int,
    val available: Boolean,
    val complete: Boolean
) {
    val fraction: Float
        get() = if (target == 0) 1f else (progress.toFloat() / target).coerceIn(0f, 1f)
    private val target get() = objective.target
}

/** Snapshot of one lesson's status for the UI. */
data class CurriculumLessonStatus(
    val lesson: CurriculumLesson,
    val objectives: List<CurriculumObjectiveStatus>
) {
    val completedObjectives: Int get() = objectives.count { it.complete }
    val totalObjectives: Int get() = objectives.size
    val fraction: Float get() = if (totalObjectives == 0) 1f else completedObjectives.toFloat() / totalObjectives
    val isComplete: Boolean get() = totalObjectives > 0 && completedObjectives == totalObjectives
}
