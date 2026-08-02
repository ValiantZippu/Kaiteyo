package ua.syt0r.kanji.desktop.engine.review

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.datetime.toLocalDateTime
import ua.syt0r.kanji.desktop.model.DesktopCard
import ua.syt0r.kanji.desktop.model.ReviewLogEntry
import ua.syt0r.kanji.desktop.model.ReviewRating
import ua.syt0r.kanji.desktop.model.SrsStatus
import ua.syt0r.kanji.desktop.engine.srs.SrsParameters
import ua.syt0r.kanji.desktop.engine.srs.SrsScheduler
import ua.syt0r.kanji.desktop.engine.srs.toLike
import kotlin.random.Random
import kotlin.time.Duration

// ============================================
// REVIEW QUEUE ENGINE
// Builds, edits and walks review queues with full
// Anki-class controls: bury, suspend, undo, retry,
// skip, custom intervals, filtered queues, preview.
// ============================================

/** Queue modifiers that are non-destructive to the underlying card. */
enum class QueueAction {
    Bury, Suspend, Skip, Undo, Retry, Preview, Defer, Forget, Reschedule
}

/** A single live entry in the queue. */
data class QueueEntry(
    val card: DesktopCard,
    val position: Int,
    val isNew: Boolean,
    val isPreview: Boolean = false,
    val deferredUntil: Instant? = null
)

/**
 * A review session. Holds the working copy of the queue, an undo
 * stack for the session, and audit logging of every action.
 */
class ReviewSession(
    val name: String,
    val createdAt: Instant = Clock.System.now(),
    private val random: Random = Random(42)
) {
    private val _queue = mutableListOf<QueueEntry>()
    val queue: List<QueueEntry> get() = _queue

    private val _done = mutableListOf<Pair<QueueEntry, ReviewRating>>()
    val done: List<Pair<QueueEntry, ReviewRating>> get() = _done

    private val undoStack = ArrayDeque<ReviewLogEntry>()
    val audit: MutableList<ReviewLogEntry> = mutableListOf()

    private var position = 0
    private var buried = mutableListOf<String>()
    private var suspended = mutableListOf<String>()

    var currentIndex: Int = 0
        private set

    /** Queue is exhausted when the cursor passes the last card. */
    val isFinished: Boolean get() = currentIndex >= _queue.size

    val remaining: Int get() = (_queue.size - currentIndex).coerceAtLeast(0)
    val total: Int get() = _queue.size

    // ------------------------------------------------------------
    // Queue construction & editing
    // ------------------------------------------------------------

    fun enqueue(cards: List<DesktopCard>, shuffle: Boolean = true) {
        val ordered = if (shuffle) cards.shuffled(random) else cards
        _queue.clear()
        _queue.addAll(ordered.mapIndexed { i, card ->
            QueueEntry(
                card = card,
                position = i,
                isNew = card.status == SrsStatus.New
            )
        })
        position = 0
        currentIndex = 0
    }

    fun current(): QueueEntry? = _queue.getOrNull(currentIndex)

    fun removeCard(cardId: String) {
        _queue.removeAll { it.card.id == cardId }
        currentIndex = currentIndex.coerceAtMost(_queue.size)
    }

    fun reorder(fromIndex: Int, toIndex: Int) {
        if (fromIndex !in _queue.indices || toIndex !in _queue.indices || fromIndex == toIndex) return
        val entry = _queue.removeAt(fromIndex)
        _queue.add(toIndex, entry)
    }

    fun moveCardToEnd(cardId: String) {
        val idx = _queue.indexOfFirst { it.card.id == cardId }
        if (idx == -1) return
        val entry = _queue.removeAt(idx)
        _queue.add(entry)
        if (currentIndex > idx) currentIndex--
    }

    fun clearBuried(): Int {
        val count = buried.size
        buried.clear()
        return count
    }

    val buriedCards: List<String> get() = buried.toList()
    val suspendedCards: List<String> get() = suspended.toList()

    // ------------------------------------------------------------
    // Answering
    // ------------------------------------------------------------

    /**
     * Grade the current card. Returns the updated card state (for
     * persistence) and records the review in both audit + undo stack.
     */
    fun answer(rating: ReviewRating, now: Instant = Clock.System.now()): DesktopCard {
        val entry = current() ?: return entryCardFallback()
        val card = entry.card

        val result = SrsScheduler.schedule(
            currentStatus = card.status.toLike(),
            currentInterval = card.intervalDays,
            currentEase = card.ease,
            lapses = card.lapses,
            learningSteps = 0,
            rating = rating.toLike(),
            now = now
        )

        val updated = card.copy(
            status = when (result.status) {
                ua.syt0r.kanji.desktop.engine.srs.SrsSchedulingStatus.Learning -> SrsStatus.Learning
                ua.syt0r.kanji.desktop.engine.srs.SrsSchedulingStatus.Review -> SrsStatus.Review
                ua.syt0r.kanji.desktop.engine.srs.SrsSchedulingStatus.Relearning -> SrsStatus.Relearning
            },
            intervalDays = result.intervalDays,
            ease = result.ease,
            dueAt = result.dueAt,
            reps = card.reps + 1,
            lapses = card.lapses + if (rating == ReviewRating.Again) 1 else 0,
            lastReviewedAt = now,
            accuracy = updateAccuracy(card, rating)
        )

        val log = ReviewLogEntry(
            cardId = card.id,
            reviewedAt = now,
            rating = rating,
            intervalBefore = card.intervalDays,
            intervalAfter = result.intervalDays,
            wasNew = entry.isNew
        )
        audit.add(log)
        undoStack.addLast(log)

        _done.add(entry to rating)
        currentIndex++
        return updated
    }

    private fun entryCardFallback(): DesktopCard =
        DesktopCard(id = "unknown", character = "?", meaning = "Unknown", status = SrsStatus.Learning)

    private fun updateAccuracy(card: DesktopCard, rating: ReviewRating): Float {
        val correct = rating != ReviewRating.Again
        val current = card.reps * card.accuracy
        val next = if (correct) current + 1.0 else current + 0.0
        return (next / (card.reps + 1)).toFloat().coerceIn(0f, 1f)
    }

    // ------------------------------------------------------------
    // Non-destructive controls
    // ------------------------------------------------------------

    /** Move the current card out of today's queue. */
    fun bury() {
        val entry = current() ?: return
        buried.add(entry.card.id)
        removeCard(entry.card.id)
    }

    /** Suspend the current card; removed from queue and future reviews. */
    fun suspend(): DesktopCard {
        val entry = current() ?: return entryCardFallback()
        suspended.add(entry.card.id)
        val card = entry.card.copy(status = SrsStatus.Suspended)
        removeCard(entry.card.id)
        return card
    }

    /** Un-suspend a card back into the queue. */
    fun unsuspend(cardId: String) {
        suspended.remove(cardId)
    }

    /** Requeue the current card at the end of the queue. */
    fun skip() {
        val entry = current() ?: return
        moveCardToEnd(entry.card.id)
    }

    /** Remove the last answered card from [done] and restore the cursor. */
    fun undo(now: Instant = Clock.System.now()): DesktopCard? {
        val log = undoStack.removeLastOrNull() ?: return null
        val (entry, _) = _done.removeLastOrNull() ?: return null
        currentIndex = (currentIndex - 1).coerceAtLeast(0)
        // Re-insert entry at front of the not-yet-answered segment.
        if (currentIndex <= _queue.size) {
            _queue.add(currentIndex, entry)
        }
        audit.add(
            ReviewLogEntry(
                cardId = log.cardId,
                reviewedAt = now,
                rating = ReviewRating.Good,
                source = "undo"
            )
        )
        return entry.card
    }

    /** Re-queue the current card to be answered again now. */
    fun retry() {
        val entry = current() ?: return
        // No-op for same-position retry in a linear queue; keeps API symmetric.
        moveCardToEnd(entry.card.id)
    }

    /**
     * Custom interval override: move the card [days] out without a
     * rating. Useful for "review preview" and bulk rescheduling.
     */
    fun setCustomInterval(days: Double, now: Instant = Clock.System.now()) {
        val entry = current() ?: return
        val updated = entry.card.copy(
            intervalDays = days,
            dueAt = now + ua.syt0r.kanji.desktop.engine.srs.intervalDaysToDuration(days)
        )
        replaceCurrent(updated)
        currentIndex++
    }

    private fun replaceCurrent(card: DesktopCard) {
        val idx = currentIndex
        if (idx in _queue.indices) {
            _queue[idx] = _queue[idx].copy(card = card)
        }
    }

    /** "Forget card" — reset it back to a brand-new state. */
    fun forget(now: Instant = Clock.System.now()): DesktopCard {
        val entry = current() ?: return entryCardFallback()
        val updated = entry.card.copy(
            status = SrsStatus.New,
            intervalDays = 0.0,
            dueAt = now,
            lapses = 0,
            reps = 0,
            ease = 2.5
        )
        replaceCurrent(updated)
        return updated
    }

    /** Reschedule the whole queue relative to today (bulk reschedule). */
    fun rescheduleAll(offsetDays: Double, now: Instant = Clock.System.now()) {
        _queue.replaceAll { entry ->
            val due = entry.card.dueAt
            val newDue = due?.let { it + ua.syt0r.kanji.desktop.engine.srs.intervalDaysToDuration(offsetDays) }
            entry.copy(card = entry.card.copy(dueAt = newDue))
        }
    }

    /** Queue preview mode — show every card regardless of due status. */
    fun enterPreview() {
        _queue.replaceAll { it.copy(isPreview = true) }
    }

    // ------------------------------------------------------------
    // Metrics
    // ------------------------------------------------------------

    fun sessionStats(): ReviewSessionStats {
        var again = 0; var hard = 0; var good = 0; var easy = 0
        _done.forEach { (_, rating) ->
            when (rating) {
                ReviewRating.Again -> again++
                ReviewRating.Hard -> hard++
                ReviewRating.Good -> good++
                ReviewRating.Easy -> easy++
            }
        }
        val total = again + hard + good + easy
        return ReviewSessionStats(
            total = total,
            again = again, hard = hard, good = good, easy = easy,
            accuracy = if (total == 0) 1f else (good + easy).toFloat() / total
        )
    }
}

data class ReviewSessionStats(
    val total: Int,
    val again: Int,
    val hard: Int,
    val good: Int,
    val easy: Int,
    val accuracy: Float
)

/** Returns whether a card is due on the given date (local, timezone aware). */
fun isDue(card: DesktopCard, today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()), now: Instant = Clock.System.now()): Boolean {
    val due = card.dueAt ?: return card.status == ua.syt0r.kanji.desktop.model.SrsStatus.New
    return when (card.status) {
        ua.syt0r.kanji.desktop.model.SrsStatus.New, ua.syt0r.kanji.desktop.model.SrsStatus.Suspended, ua.syt0r.kanji.desktop.model.SrsStatus.Buried -> false
        else -> due <= now
    }
}

/** Builds the "due today" queue from a pool of cards. */
fun buildDueQueue(cards: List<DesktopCard>, now: Instant = Clock.System.now()): List<DesktopCard> =
    cards.filter { isDue(it, now = now) }
        .sortedWith(compareBy({ it.status != ua.syt0r.kanji.desktop.model.SrsStatus.Review }, { it.dueAt?.epochSeconds ?: Long.MAX_VALUE }))
