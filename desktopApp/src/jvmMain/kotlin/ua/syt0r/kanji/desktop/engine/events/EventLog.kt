package ua.syt0r.kanji.desktop.engine.events

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import java.io.File

// ============================================
// EVENT LOG — append-only domain event store
// Implements the architecture EVENT_CATALOG
// (`docs/architecture/nodes/EVENT_CATALOG.md`) on
// the suite side: every event carries
// event_id / occurred_at / event_type / source /
// payload (semantic facts only) / schema_version.
//
// The log is the single source of truth for
// derived metrics: derived read-models are
// re-runnable from it. Corrections append new
// rows, never edit old ones.
// ============================================

/** Event families from the architecture event catalog. */
@Serializable
enum class EventFamily { Study, Content, Media, Exam, Journey, System }

/**
 * The event types the suite can actually emit today — each maps 1:1 to a
 * row in `EVENT_CATALOG.md`. TARGET catalog entries without a real producer
 * are deliberately NOT listed (no fabricated events).
 */
@Serializable
enum class EventType {
    CardReviewed, CardSuspended, CardBuried, CardReset,
    WritingAttempted, MistakeRecorded,
    DictionaryLookup, KanjiEncountered, VocabularyEncountered,
    MediaStarted, MediaEnded, SubtitleSelected, CardMined, BookmarkAdded,
    ExamStarted, ExamCompleted, ExamAbandoned, ExamQuestionAnswered,
    SyncCompleted, ImportFinished,
}

@Serializable
data class EventRecord(
    val eventId: String,
    val occurredAt: Instant,
    val eventType: EventType,
    /** Where the event originated: study / dictionary / media / exam / system. */
    val source: String,
    /** Semantic facts only — never UI state or credentials. */
    val payload: Map<String, String> = emptyMap(),
    val sessionId: String = "",
    val schemaVersion: Int = 1
)

/** Per-family aggregate for read-models (heatmaps, knowledge scores). */
@Serializable
data class EventSummary(
    val total: Int = 0,
    val byType: Map<EventType, Int> = emptyMap(),
    val byFamily: Map<EventFamily, Int> = emptyMap()
)

private val EventType.family: EventFamily
    get() = when (this) {
        EventType.CardReviewed, EventType.CardSuspended, EventType.CardBuried,
        EventType.CardReset, EventType.WritingAttempted, EventType.MistakeRecorded -> EventFamily.Study
        EventType.DictionaryLookup, EventType.KanjiEncountered, EventType.VocabularyEncountered -> EventFamily.Content
        EventType.MediaStarted, EventType.MediaEnded, EventType.SubtitleSelected,
        EventType.CardMined, EventType.BookmarkAdded -> EventFamily.Media
        EventType.ExamStarted, EventType.ExamCompleted, EventType.ExamAbandoned,
        EventType.ExamQuestionAnswered -> EventFamily.Exam
        EventType.SyncCompleted, EventType.ImportFinished -> EventFamily.System
    }

/**
 * Append-only event log with JSON snapshot persistence. Loaded from disk on
 * init; `record` appends and persists incrementally. Never edits existing rows.
 */
class EventLog(
    private val file: File,
    private val json: kotlinx.serialization.json.Json = kotlinx.serialization.json.Json {
        ignoreUnknownKeys = true
    }
) {
    private val _entries = mutableListOf<EventRecord>()
    val entries: List<EventRecord> get() = _entries.toList()

    private var counter = 0L

    init {
        if (file.exists()) {
            runCatching {
                val loaded = json.decodeFromString<List<EventRecord>>(file.readText())
                _entries.addAll(loaded)
                counter = loaded.size.toLong()
            }
        }
    }

    fun record(
        eventType: EventType,
        source: String,
        payload: Map<String, String> = emptyMap(),
        sessionId: String = ""
    ): EventRecord {
        counter++
        val record = EventRecord(
            eventId = "evt-${counter}-${Clock.System.now().toEpochMilliseconds()}",
            occurredAt = Clock.System.now(),
            eventType = eventType,
            source = source,
            payload = payload,
            sessionId = sessionId
        )
        _entries.add(record)
        persist()
        return record
    }

    fun count(): Int = _entries.size

    fun count(eventType: EventType): Int = _entries.count { it.eventType == eventType }

    fun since(instant: Instant): List<EventRecord> = _entries.filter { it.occurredAt >= instant }

    fun summary(): EventSummary = EventSummary(
        total = _entries.size,
        byType = _entries.groupingBy { it.eventType }.eachCount(),
        byFamily = _entries.groupingBy { it.eventType.family }.eachCount()
    )

    /** Latest N events, newest first (read-model convenience). */
    fun recent(limit: Int = 50): List<EventRecord> = _entries.asReversed().take(limit)

    /** Drop a bad snapshot and rebuild from an empty log (recovery only). */
    fun reset() {
        _entries.clear()
        counter = 0L
        persist()
    }

    private fun persist() {
        file.parentFile?.mkdirs()
        file.writeText(json.encodeToString<List<EventRecord>>(_entries))
    }
}
