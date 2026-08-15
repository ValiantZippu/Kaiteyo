package ua.syt0r.kanji.desktop.engine.learning

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import ua.syt0r.kanji.desktop.engine.srs.SrsParameters
import ua.syt0r.kanji.desktop.engine.srs.SrsScheduler
import ua.syt0r.kanji.desktop.engine.srs.toLike
import ua.syt0r.kanji.desktop.engine.srs.intervalDaysToDuration
import ua.syt0r.kanji.desktop.model.ReviewRating
import ua.syt0r.kanji.desktop.model.SrsStatus
import ua.syt0r.kanji.desktop.model.StudyMode

// ============================================
// STUDY ENGINE
// Builds the study queue from the unified store
// (due + new, respecting the deck's config),
// grades cards through the shared SrsScheduler,
// records full-fidelity review events, and tracks
// resumable study sessions. Never touches mock
// cards — the queue is always the real state.
// ============================================

data class StudyQueueItem(
    val card: NoteCard,
    val note: LearningNote
)

class StudyEngine(private val store: LearningStore) {

    // ------------------------------------------------------------
    // Queue building
    // ------------------------------------------------------------

    /**
     * Build a study queue for a deck from actual SRS state.
     * New cards first (up to the deck's daily limit), then due cards.
     * Suspended/buried cards are excluded. Never random mocks.
     */
    fun buildQueue(
        deckId: String,
        mode: StudyMode = StudyMode.Flashcards,
        now: Instant = Clock.System.now(),
        includeNew: Boolean = true,
        newLimit: Int? = null,
        reviewLimit: Int? = null
    ): List<StudyQueueItem> {
        val config = store.deckConfig(deckId)
        // Flashcards mode is inclusive — every card type is studyable in a
        // general session. Specific modes (Recognition, Reading, Writing...)
        // restrict the queue to cards of that direction.
        val deckCards = store.cardsForDeck(deckId).filter {
            mode == StudyMode.Flashcards || it.cardType.toStudyMode() == mode
        }
        val active = deckCards.filter { !it.isSuspended && !it.buried }
        val newCards = if (includeNew) {
            active.filter { it.isNew }.take(newLimit ?: config.dailyNewLimit)
        } else emptyList()
        val due = active.filter { it.status != SrsStatus.New && it.isDue }
            .sortedBy { it.dueAt }
            .take(reviewLimit ?: config.dailyReviewLimit)
        val ordered = if (config.interleaveNewAndReviews) {
            val interleaved = mutableListOf<NoteCard>()
            val newIt = newCards.iterator()
            val dueIt = due.iterator()
            while (newIt.hasNext() || dueIt.hasNext()) {
                if (newIt.hasNext()) interleaved.add(newIt.next())
                if (dueIt.hasNext()) interleaved.add(dueIt.next())
            }
            interleaved
        } else {
            newCards + due
        }
        return ordered.mapNotNull { card ->
            store.note(card.noteId)?.let { StudyQueueItem(card, it) }
        }
    }

    /** Cards due in a deck regardless of mode (for badges/forecast). */
    fun dueCount(deckId: String, now: Instant = Clock.System.now()): Int =
        store.cardsForDeck(deckId).count { !it.isSuspended && !it.buried && !it.isNew && it.isDue }

    fun newCount(deckId: String): Int =
        store.cardsForDeck(deckId).count { it.isNew && !it.isSuspended && !it.buried }

    // ------------------------------------------------------------
    // Rating
    // ------------------------------------------------------------

    data class GradeResult(
        val card: NoteCard,
        val event: LearningReviewEvent
    )

    /**
     * Grade a queue item through the shared scheduler, persist the new SRS
     * state and append a full-fidelity review event. Response time is the
     * elapsed wall time between reveal and answer (supplied by the UI).
     */
    fun grade(
        item: StudyQueueItem,
        rating: ReviewRating,
        activityType: StudyActivityType = StudyActivityType.Review,
        responseTimeMs: Long = 0,
        mistakes: List<String> = emptyList(),
        examId: String = "",
        sessionId: String = "",
        mode: StudyMode = item.card.cardType.toStudyMode(),
        now: Instant = Clock.System.now(),
        params: SrsParameters = SrsParameters.Default
    ): GradeResult {
        val card = item.card
        val result = SrsScheduler.schedule(
            currentStatus = card.status.toLike(),
            currentInterval = card.intervalDays,
            currentEase = card.ease,
            lapses = card.lapses,
            learningSteps = 0,
            rating = rating.toLike(),
            now = now,
            params = params
        )
        val statusAfter = when (result.status) {
            ua.syt0r.kanji.desktop.engine.srs.SrsSchedulingStatus.Learning -> SrsStatus.Learning
            ua.syt0r.kanji.desktop.engine.srs.SrsSchedulingStatus.Review -> SrsStatus.Review
            ua.syt0r.kanji.desktop.engine.srs.SrsSchedulingStatus.Relearning -> SrsStatus.Relearning
        }
        val correct = rating != ReviewRating.Again
        val nextStreak = if (correct) card.streak + 1 else 0
        val nextReps = card.reps + 1
        val nextAccuracy = ((card.reps * card.accuracy) + if (correct) 1.0 else 0.0) / nextReps.toDouble()

        val updated = card.copy(
            status = statusAfter,
            intervalDays = result.intervalDays,
            dueAt = result.dueAt,
            lapses = card.lapses + if (rating == ReviewRating.Again) 1 else 0,
            reps = nextReps,
            ease = result.ease,
            accuracy = nextAccuracy.toFloat().coerceIn(0f, 1f),
            streak = nextStreak,
            bestStreak = maxOf(card.bestStreak, nextStreak),
            lastReviewedAt = now
        )
        store.updateCardState(
            id = card.id,
            status = updated.status,
            intervalDays = updated.intervalDays,
            dueAt = updated.dueAt,
            lapses = updated.lapses,
            reps = updated.reps,
            ease = updated.ease,
            accuracy = updated.accuracy,
            streak = updated.streak,
            now = now
        )
        val event = LearningReviewEvent(
            id = LearningIds.eventId("review"),
            cardId = card.id,
            noteId = card.noteId,
            deckId = card.deckId,
            cardType = card.cardType,
            activityType = activityType,
            rating = rating,
            reviewedAt = now,
            responseTimeMs = responseTimeMs,
            statusBefore = card.status,
            statusAfter = statusAfter,
            intervalBefore = card.intervalDays,
            intervalAfter = result.intervalDays,
            wasNew = card.isNew,
            lapsesAfter = updated.lapses,
            mistakes = mistakes,
            examId = examId,
            sessionId = sessionId
        )
        store.recordReview(event)
        return GradeResult(updated, event)
    }

    // ------------------------------------------------------------
    // Suspend / bury / reset
    // ------------------------------------------------------------
    fun suspend(cardId: String) {
        val idx = store.cards.indexOfFirst { it.id == cardId }
        if (idx == -1) return
        store.cards[idx] = store.cards[idx].copy(status = SrsStatus.Suspended, dueAt = null)
        store.save()
    }

    fun unsuspend(cardId: String) {
        val idx = store.cards.indexOfFirst { it.id == cardId }
        if (idx == -1) return
        store.cards[idx] = store.cards[idx].copy(status = SrsStatus.New, dueAt = null)
        store.save()
    }

    fun forget(cardId: String) {
        val idx = store.cards.indexOfFirst { it.id == cardId }
        if (idx == -1) return
        store.cards[idx] = store.cards[idx].copy(
            status = SrsStatus.New,
            intervalDays = 0.0,
            dueAt = null,
            reps = 0,
            lapses = 0,
            ease = 2.5,
            accuracy = 0.5f,
            streak = 0,
            lastReviewedAt = null
        )
        store.save()
    }

    fun reschedule(cardId: String, days: Int, now: Instant = Clock.System.now()) {
        val idx = store.cards.indexOfFirst { it.id == cardId }
        if (idx == -1) return
        val d = days.toDouble().coerceAtLeast(0.1)
        store.cards[idx] = store.cards[idx].copy(
            status = if (d < 1.0) SrsStatus.Learning else SrsStatus.Review,
            intervalDays = d,
            dueAt = now + intervalDaysToDuration(d)
        )
        store.save()
    }

    // ------------------------------------------------------------
    // Sessions (resumable)
    // ------------------------------------------------------------
    fun openSession(deckId: String, mode: StudyMode): StudySessionRecord {
        val session = StudySessionRecord(
            id = LearningIds.eventId("session"),
            startedAt = Clock.System.now(),
            deckId = deckId,
            mode = mode
        )
        store.recordSession(session)
        return session
    }

    fun recordAnswer(session: StudySessionRecord, correct: Boolean, wasNew: Boolean) {
        val updated = session.copy(
            cardsSeen = session.cardsSeen + 1,
            cardsCompleted = session.cardsCompleted + 1,
            correctCount = session.correctCount + if (correct) 1 else 0,
            againCount = session.againCount + if (correct) 0 else 1,
            newCards = session.newCards + if (wasNew) 1 else 0,
            reviewCards = session.reviewCards + if (wasNew) 0 else 1
        )
        store.updateSession(updated)
    }

    fun finishSession(session: StudySessionRecord, interrupted: Boolean = false, lastCardId: String = "") {
        store.updateSession(
            session.copy(
                finishedAt = Clock.System.now(),
                interrupted = interrupted,
                lastCardId = lastCardId
            )
        )
    }

    /** The most recent unfinished session for a deck (resume support). */
    fun pendingSession(deckId: String): StudySessionRecord? =
        store.sessions.filter { it.deckId == deckId && it.finishedAt == null }
            .maxByOrNull { it.startedAt }

    // ------------------------------------------------------------
    // Aggregated helpers used by study UI
    // ------------------------------------------------------------
    fun deckTotals(deckId: String): DeckLearningTotals {
        val deckCards = store.cardsForDeck(deckId)
        return DeckLearningTotals(
            total = deckCards.size,
            new = deckCards.count { it.isNew && !it.isSuspended },
            learning = deckCards.count { it.status == SrsStatus.Learning || it.status == SrsStatus.Relearning },
            review = deckCards.count { it.status == SrsStatus.Review },
            due = deckCards.count { !it.isSuspended && !it.buried && !it.isNew && it.isDue },
            suspended = deckCards.count { it.isSuspended },
            buried = deckCards.count { it.buried },
            mature = deckCards.count { it.stage == LearningStage.Mature }
        )
    }
}

data class DeckLearningTotals(
    val total: Int = 0,
    val new: Int = 0,
    val learning: Int = 0,
    val review: Int = 0,
    val due: Int = 0,
    val suspended: Int = 0,
    val buried: Int = 0,
    val mature: Int = 0
)
