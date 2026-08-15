package ua.syt0r.kanji.desktop.engine.learning

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import ua.syt0r.kanji.desktop.model.ReviewRating

// ============================================
// MISTAKE ENGINE
// A real mistakes queue generated from actual
// recorded mistakes — never from fabricated data.
//
// Categories:
//   - reading mistakes (Again on a reading card)
//   - meaning mistakes (Again on a meaning card)
//   - recognition mistakes (Again on recognition)
//   - writing mistakes (failed WritingAttempt)
//   - exam mistakes (wrong exam answers)
//   - lapses (cards with repeated failures)
// ============================================

enum class MistakeCategory(val label: String) {
    Writing("Writing"),
    Reading("Reading"),
    Meaning("Meaning"),
    Recognition("Recognition"),
    Exam("Exam"),
    Lapses("Lapses")
}

data class MistakeItem(
    val cardId: String,
    val noteId: String,
    val expression: String,
    val category: MistakeCategory,
    val count: Int,
    val lastMistakeAt: Instant,
    val detail: String = ""
)

class MistakeEngine(private val store: LearningStore) {

    /** The full mistake queue, grouped by card and category, worst first. */
    fun queue(limit: Int = 200, now: Instant = Clock.System.now()): List<MistakeItem> {
        val map = LinkedHashMap<String, MistakeItem>()

        fun add(cardId: String, category: MistakeCategory, at: Instant, detail: String = "") {
            val card = store.card(cardId) ?: return
            val note = store.note(card.noteId) ?: return
            val existing = map[cardId + category.name]
            if (existing != null) {
                map[cardId + category.name] = existing.copy(
                    count = existing.count + 1,
                    lastMistakeAt = maxOf(existing.lastMistakeAt, at)
                )
            } else {
                map[cardId + category.name] = MistakeItem(
                    cardId = cardId,
                    noteId = card.noteId,
                    expression = note.expression,
                    category = category,
                    count = 1,
                    lastMistakeAt = at,
                    detail = detail
                )
            }
        }

        // Reading/meaning/recognition mistakes from review events.
        store.reviewEvents.filter { it.rating == ReviewRating.Again }.forEach { e ->
            val category = when (e.cardType) {
                CardType.Reading -> MistakeCategory.Reading
                CardType.Recognition -> MistakeCategory.Recognition
                CardType.Meaning, CardType.Pattern -> MistakeCategory.Meaning
                CardType.Cloze -> MistakeCategory.Meaning
                else -> MistakeCategory.Recognition
            }
            add(e.cardId, category, e.reviewedAt)
        }

        // Writing mistakes.
        store.writingAttempts.filter { !it.correct }.forEach { w ->
            add(w.cardId, MistakeCategory.Writing, w.attemptedAt, w.mistakeCount.toString())
        }

        // Exam mistakes.
        store.examResults.forEach { exam ->
            exam.questions.filter { !it.correct && it.cardId.isNotBlank() }.forEach { q ->
                add(q.cardId, MistakeCategory.Exam, exam.finishedAt, q.questionType)
            }
        }

        // Lapses (repeated failures).
        store.cards.filter { it.lapses >= 2 }.forEach { card ->
            val lastEvent = store.reviewEvents.filter { it.cardId == card.id && !it.correct }
                .maxByOrNull { it.reviewedAt }
            add(card.id, MistakeCategory.Lapses, lastEvent?.reviewedAt ?: now)
        }

        return map.values
            .sortedWith(compareByDescending<MistakeItem> { it.count }.thenByDescending { it.lastMistakeAt })
            .take(limit)
    }

    /** Count of distinct cards with active mistakes (badge number). */
    fun mistakeCount(): Int = queue(limit = Int.MAX_VALUE).map { it.cardId }.distinct().size

    /** The queue scoped to one category (used by the Mistakes study mode). */
    fun forCategory(category: MistakeCategory, limit: Int = 100): List<MistakeItem> =
        queue(limit).filter { it.category == category }

    /**
     * Build a study queue from mistake cards — same StudyEngine flow, so
     * reviewing mistakes updates SRS exactly like normal study.
     */
    fun asStudyQueue(limit: Int = 50): List<StudyQueueItem> {
        val items = queue(limit)
        val cardIds = items.map { it.cardId }.distinct().take(limit)
        return cardIds.mapNotNull { cardId ->
            val card = store.card(cardId) ?: return@mapNotNull null
            val note = store.note(card.noteId) ?: return@mapNotNull null
            StudyQueueItem(card, note)
        }
    }

    /** Breakdown by category for the UI. */
    fun breakdown(): Map<MistakeCategory, Int> =
        queue(limit = Int.MAX_VALUE).groupingBy { it.category }.eachCount()
}
