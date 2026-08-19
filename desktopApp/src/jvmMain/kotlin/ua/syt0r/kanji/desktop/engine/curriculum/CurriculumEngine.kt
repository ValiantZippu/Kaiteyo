package ua.syt0r.kanji.desktop.engine.curriculum

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// ============================================
// KAITEYO CURRICULUM — ENGINE
// Resolves the active course/lesson and
// measures every objective against live study
// data. Completion is detected on refresh()
// (called whenever the app observes a study
// event) and lessons auto-advance. A course
// never reports progress its data does not
// back; objectives for missing decks count as
// satisfied so a course still works on a
// partial install.
// ============================================

class CurriculumEngine(
    private val dataSource: CurriculumDataSource,
    private val store: CurriculumStore = CurriculumStore()
) {

    /** All courses available (built-in registry + any future packages). */
    val courses: List<CurriculumCourse> = BuiltInCurriculum.all

    var progress by mutableStateOf(store.load())
        private set

    init {
        // Normalize: an unknown active course/lesson resets to nothing.
        val normalized = progress
        if (normalized.activeCourseId != null && activeCourse() == null) {
            progress = CurriculumProgress()
            persist()
        }
    }

    // ------------------------------------------------------------
    // Resolution
    // ------------------------------------------------------------

    fun activeCourse(): CurriculumCourse? =
        courses.firstOrNull { it.id == progress.activeCourseId }

    fun activeLesson(): CurriculumLesson? {
        val course = activeCourse() ?: return null
        return course.lessons.firstOrNull { it.id == progress.activeLessonId }
            ?: course.lessons.firstOrNull()
    }

    fun courseStatus(course: CurriculumCourse): CurriculumLessonStatus? {
        val lesson = course.lessons.firstOrNull { it.id == progress.activeLessonId } ?: return null
        return lessonStatus(lesson)
    }

    fun lessonStatus(lesson: CurriculumLesson): CurriculumLessonStatus =
        CurriculumLessonStatus(
            lesson = lesson,
            objectives = lesson.objectives.map(::objectiveStatus)
        )

    fun objectiveStatus(objective: CurriculumObjective): CurriculumObjectiveStatus {
        val available = objective.deckId == null || objective.deckId in dataSource.availableDeckIds()
        val live = if (available) liveProgress(objective) else 0
        val alreadyDone = objective.id in progress.completedObjectiveIds
        // Objectives for unavailable decks are treated as satisfied so a
        // course never stalls on an install that lacks a deck.
        val complete = alreadyDone || !available || live >= objective.target
        return CurriculumObjectiveStatus(
            objective = objective,
            progress = live,
            available = available,
            complete = complete
        )
    }

    // ------------------------------------------------------------
    // Progress measurement
    // ------------------------------------------------------------

    private fun liveProgress(objective: CurriculumObjective): Int = when (objective.kind) {
        CurriculumObjectiveKind.NewCardCount -> dataSource.cardCountInDeck(objective.deckId.orEmpty())
        CurriculumObjectiveKind.ReviewCount -> dataSource.reviewedCardCountInDeck(objective.deckId.orEmpty())
        CurriculumObjectiveKind.TotalReviewCount -> dataSource.totalReviewEvents()
    }

    // ------------------------------------------------------------
    // Navigation
    // ------------------------------------------------------------

    fun startCourse(courseId: String) {
        val course = courses.firstOrNull { it.id == courseId } ?: return
        progress = CurriculumProgress(
            activeCourseId = course.id,
            activeLessonId = course.lessons.firstOrNull()?.id,
            completedObjectiveIds = progress.completedObjectiveIds
        )
        persist()
    }

    fun startLesson(lessonId: String) {
        val course = activeCourse() ?: return
        if (course.lessons.none { it.id == lessonId }) return
        progress = progress.copy(activeLessonId = lessonId)
        persist()
    }

    fun switchCourse(courseId: String) = startCourse(courseId)

    /** Leave the active course and return to the course picker. */
    fun exitCourse() {
        progress = CurriculumProgress(completedObjectiveIds = progress.completedObjectiveIds)
        persist()
    }

    // ------------------------------------------------------------
    // Refresh: detect completion, auto-advance
    // ------------------------------------------------------------

    /**
     * Re-measure the active lesson's objectives. Called after study events
     * (reviews, mining, imports). Returns the newly completed objectives.
     */
    fun refresh(): List<CurriculumObjective> {
        val course = activeCourse() ?: return emptyList()
        val lesson = activeLesson() ?: return emptyList()

        val newlyComplete = lesson.objectives
            .filter { it.id !in progress.completedObjectiveIds }
            .filter { objectiveStatus(it).complete }

        if (newlyComplete.isNotEmpty()) {
            progress = progress.copy(
                completedObjectiveIds = progress.completedObjectiveIds + newlyComplete.map { it.id }
            )
            persist()
        }
        return newlyComplete
    }

    /** Advance to the next lesson when the current one is complete. */
    fun advanceIfComplete(): CurriculumLesson? {
        val course = activeCourse() ?: return null
        val current = activeLesson() ?: return null
        val status = lessonStatus(current)
        if (!status.isComplete) return null

        val idx = course.lessons.indexOfFirst { it.id == current.id }
        val next = course.lessons.getOrNull(idx + 1)
        if (next != null) {
            progress = progress.copy(activeLessonId = next.id)
            persist()
        }
        return next
    }

    /** The next objective the learner should work on, or null when done. */
    fun nextObjective(): CurriculumObjectiveStatus? {
        val lesson = activeLesson() ?: return null
        return lessonStatus(lesson).objectives.firstOrNull { !it.complete }
    }

    /** Completion of a whole course (0..1). */
    fun courseCompletion(course: CurriculumCourse): Float {
        if (course.objectiveCount == 0) return 1f
        val done = course.lessons.sumOf { lesson ->
            lesson.objectives.count { it.id in progress.completedObjectiveIds }
        }
        return (done.toFloat() / course.objectiveCount).coerceIn(0f, 1f)
    }

    fun isCourseStarted(course: CurriculumCourse): Boolean =
        course.id == progress.activeCourseId

    private fun persist() = store.save(progress)
}
