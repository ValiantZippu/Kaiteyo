package ua.syt0r.kanji.desktop.engine.learning

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ua.syt0r.kanji.desktop.model.DesktopCard
import ua.syt0r.kanji.desktop.model.ReviewRating
import ua.syt0r.kanji.desktop.model.SrsStatus
import ua.syt0r.kanji.desktop.model.StudyMode
import java.io.File

// ============================================
// LEARNING STORE
// The single persisted source of truth for the
// unified learning model: notes, generated cards,
// per-deck study config, review events, writing
// attempts, exam results and study sessions.
//
// Everything statistics, decks, exams and study
// read from lives here. The legacy DesktopCard
// pool in AppState is bridged through [toDesktopCard]
// so existing views keep working while new systems
// use the richer model.
// ============================================

class LearningStore(
    private val directory: File = File(System.getProperty("user.home"), ".kaiteyo/learning")
) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val snapshotFile: File get() = File(directory, "learning.json")

    // ------------------------------------------------------------
    // Reactive state
    // ------------------------------------------------------------
    val notes = mutableStateListOf<LearningNote>()
    val cards = mutableStateListOf<NoteCard>()
    val deckConfigs = mutableStateMapOf<String, DeckStudyConfig>()
    val reviewEvents = mutableStateListOf<LearningReviewEvent>()
    val writingAttempts = mutableStateListOf<WritingAttemptEvent>()
    val examResults = mutableStateListOf<ExamResult>()
    val sessions = mutableStateListOf<StudySessionRecord>()

    /** Bumped on any structural mutation (cache invalidation for views). */
    var revision by mutableStateOf(0L)
        private set

    init {
        directory.mkdirs()
        load()
    }

    // ------------------------------------------------------------
    // Notes
    // ------------------------------------------------------------
    fun note(id: String): LearningNote? = notes.firstOrNull { it.id == id }

    fun noteByExpression(kind: LearningItemKind, expression: String): LearningNote? =
        notes.firstOrNull { it.kind == kind && it.expression == expression }

    /**
     * Upsert a note. Deduplicates by kind+expression+reading so importing
     * the same dictionary entry twice merges instead of duplicating, while
     * user-created custom notes are never silently destroyed.
     */
    fun upsertNote(note: LearningNote): LearningNote {
        val existing = notes.firstOrNull { it.id == note.id } ?: noteByExpression(note.kind, note.expression)
        if (existing != null) {
            if (existing.source.type == NoteSourceType.Custom && note.source.type != NoteSourceType.Custom) {
                // A custom note must not be overwritten by imported data.
                return existing
            }
            val merged = note.copy(
                id = existing.id,
                createdAt = existing.createdAt,
                tags = (existing.tags + note.tags).distinct(),
                meanings = if (note.meanings.isEmpty()) existing.meanings else note.meanings
            )
            val idx = notes.indexOfFirst { it.id == merged.id }
            if (idx >= 0) notes[idx] = merged else notes.add(merged)
            bump()
            return merged
        }
        notes.add(note)
        bump()
        return note
    }

    fun upsertNotes(batch: List<LearningNote>) {
        batch.forEach { upsertNote(it) }
        save()
    }

    fun deleteNote(id: String) {
        notes.removeAll { it.id == id }
        cards.removeAll { it.noteId == id }
        bump(); save()
    }

    // ------------------------------------------------------------
    // Cards
    // ------------------------------------------------------------
    fun card(id: String): NoteCard? = cards.firstOrNull { it.id == id }

    fun cardsFor(noteId: String): List<NoteCard> = cards.filter { it.noteId == noteId }

    fun cardsForDeck(deckId: String): List<NoteCard> = cards.filter { it.deckId == deckId }

    fun upsertCard(card: NoteCard) {
        val idx = cards.indexOfFirst { it.id == card.id }
        if (idx >= 0) cards[idx] = card else cards.add(card)
        bump(); save()
    }

    fun upsertCards(batch: List<NoteCard>) {
        batch.forEach { upsertCard(it) }
    }

    fun removeCard(id: String) {
        cards.removeAll { it.id == id }
        bump(); save()
    }

    /** Apply SRS updates in one write (used by the study engine). */
    fun updateCardState(id: String, status: SrsStatus, intervalDays: Double, dueAt: Instant?, lapses: Int, reps: Int, ease: Double, accuracy: Float, streak: Int, now: Instant) {
        val idx = cards.indexOfFirst { it.id == id }
        if (idx == -1) return
        val c = cards[idx]
        cards[idx] = c.copy(
            status = status,
            intervalDays = intervalDays,
            dueAt = dueAt,
            lapses = lapses,
            reps = reps,
            ease = ease,
            accuracy = accuracy,
            streak = streak,
            bestStreak = maxOf(c.bestStreak, streak),
            lastReviewedAt = now
        )
        bump(); save()
    }

    fun setBuried(id: String, buried: Boolean) {
        val idx = cards.indexOfFirst { it.id == id }
        if (idx == -1) return
        cards[idx] = cards[idx].copy(buried = buried)
        bump(); save()
    }

    // ------------------------------------------------------------
    // Deck config
    // ------------------------------------------------------------
    fun deckConfig(deckId: String): DeckStudyConfig =
        deckConfigs[deckId] ?: DeckStudyConfig(deckId = deckId).also { deckConfigs[deckId] = it; save() }

    fun setDeckConfig(config: DeckStudyConfig) {
        deckConfigs[config.deckId] = config
        bump(); save()
    }

    // ------------------------------------------------------------
    // Events (immutable — append only)
    // ------------------------------------------------------------
    fun recordReview(event: LearningReviewEvent) {
        reviewEvents.add(event)
        bump(); save()
    }

    fun recordWriting(attempt: WritingAttemptEvent) {
        writingAttempts.add(0, attempt)
        bump(); save()
    }

    fun recordExam(result: ExamResult) {
        examResults.add(0, result)
        bump(); save()
    }

    fun recordSession(session: StudySessionRecord) {
        sessions.add(session)
        bump(); save()
    }

    fun updateSession(session: StudySessionRecord) {
        val idx = sessions.indexOfFirst { it.id == session.id }
        if (idx >= 0) sessions[idx] = session else sessions.add(session)
        bump(); save()
    }

    // ------------------------------------------------------------
    // Mistake convenience — mistakes derive from events, never stored
    // as a separate fake list.
    // ------------------------------------------------------------
    fun reviewEventsFor(cardId: String): List<LearningReviewEvent> =
        reviewEvents.filter { it.cardId == cardId }.sortedBy { it.reviewedAt }

    fun recentFailures(limit: Int = 200): List<LearningReviewEvent> =
        reviewEvents.filter { !it.correct }.sortedByDescending { it.reviewedAt }.take(limit)

    fun lapsedCardIds(minLapses: Int = 2): Set<String> =
        cards.filter { it.lapses >= minLapses }.map { it.id }.toSet()

    // ------------------------------------------------------------
    // Bridge to the legacy DesktopCard pool (keeps old views alive)
    // ------------------------------------------------------------
    fun toDesktopCard(card: NoteCard): DesktopCard? {
        val note = note(card.noteId) ?: return null
        val kind = when (note.kind) {
            LearningItemKind.Kanji -> ua.syt0r.kanji.desktop.model.ContentKind.Kanji
            LearningItemKind.Vocabulary -> ua.syt0r.kanji.desktop.model.ContentKind.Vocabulary
            LearningItemKind.Kana -> ua.syt0r.kanji.desktop.model.ContentKind.Kana
            LearningItemKind.Radical -> ua.syt0r.kanji.desktop.model.ContentKind.Radical
            LearningItemKind.Grammar -> ua.syt0r.kanji.desktop.model.ContentKind.Grammar
            LearningItemKind.Custom -> ua.syt0r.kanji.desktop.model.ContentKind.Sentence
        }
        return DesktopCard(
            id = card.id,
            character = note.expression,
            meaning = note.meanings.joinToString("; "),
            onReadings = note.onReadings,
            kunReadings = note.kunReadings,
            radicals = note.radicals,
            components = note.components,
            strokeCount = note.strokeCount,
            jlpt = note.jlpt,
            grade = note.grade,
            frequency = note.frequency,
            tags = note.tags,
            favorite = false,
            note = note.examples.firstOrNull().orEmpty(),
            status = card.status,
            intervalDays = card.intervalDays,
            dueAt = card.dueAt,
            lapses = card.lapses,
            reps = card.reps,
            ease = card.ease,
            accuracy = card.accuracy,
            deckId = card.deckId,
            createdAt = note.createdAt,
            lastReviewedAt = card.lastReviewedAt,
            contentKind = kind
        )
    }

    fun toDesktopCards(): List<DesktopCard> = cards.mapNotNull { toDesktopCard(it) }

    // ------------------------------------------------------------
    // Persistence
    // ------------------------------------------------------------
    private fun bump() {
        revision++
    }

    private fun load() {
        if (!snapshotFile.exists()) return
        runCatching {
            val snap = json.decodeFromString<LearningSnapshot>(snapshotFile.readText())
            notes.clear(); notes.addAll(snap.notes)
            cards.clear(); cards.addAll(snap.cards)
            deckConfigs.clear(); snap.deckConfigs.forEach { (k, v) -> deckConfigs[k] = v }
            reviewEvents.clear(); reviewEvents.addAll(snap.reviewEvents)
            writingAttempts.clear(); writingAttempts.addAll(snap.writingAttempts)
            examResults.clear(); examResults.addAll(snap.examResults)
            sessions.clear(); sessions.addAll(snap.sessions)
            revision = snap.revision
        }
    }

    fun save() {
        runCatching {
            snapshotFile.writeText(
                json.encodeToString(
                    LearningSnapshot(
                        notes = notes.toList(),
                        cards = cards.toList(),
                        deckConfigs = deckConfigs.toMap(),
                        reviewEvents = reviewEvents.toList(),
                        writingAttempts = writingAttempts.toList(),
                        examResults = examResults.toList(),
                        sessions = sessions.toList(),
                        revision = revision
                    )
                )
            )
        }
    }

    /** Drop everything (used by import/replace flows and tests). */
    fun clear() {
        notes.clear()
        cards.clear()
        deckConfigs.clear()
        reviewEvents.clear()
        writingAttempts.clear()
        examResults.clear()
        sessions.clear()
        bump(); save()
    }

    /** Whether any learning data exists yet (first-run detection). */
    val isEmpty: Boolean get() = notes.isEmpty() && cards.isEmpty() && reviewEvents.isEmpty()

    companion object {
        /** Total watch of review history in ms. */
        fun totalStudyMs(events: List<LearningReviewEvent>): Long =
            events.sumOf { it.responseTimeMs }
    }
}
