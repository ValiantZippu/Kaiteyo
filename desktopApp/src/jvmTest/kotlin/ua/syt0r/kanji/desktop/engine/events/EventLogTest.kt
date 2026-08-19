package ua.syt0r.kanji.desktop.engine.events

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EventLogTest {

    private fun tempLog(): Pair<EventLog, File> {
        val file = File.createTempFile("eventlog", ".json")
        file.delete()
        return EventLog(file) to file
    }

    @Test
    fun recordsAppendAndNeverReorder() {
        val (log, file) = tempLog()
        log.record(EventType.CardReviewed, "study", mapOf("rating" to "4"))
        log.record(EventType.ExamCompleted, "exam", mapOf("score" to "90"))
        assertEquals(2, log.count())
        assertEquals(EventType.CardReviewed, log.entries[0].eventType)
        assertEquals(EventType.ExamCompleted, log.entries[1].eventType)
        file.delete()
    }

    @Test
    fun persistsAcrossInstances() {
        val file = File.createTempFile("eventlog", ".json")
        file.delete()
        EventLog(file).apply {
            record(EventType.CardMined, "media", mapOf("headword" to "食べる"))
            record(EventType.MediaStarted, "media", mapOf("file" to "ep3.mp4"))
        }
        val reloaded = EventLog(file)
        assertEquals(2, reloaded.count())
        assertEquals(EventType.CardMined, reloaded.entries.first().eventType)
        file.delete()
    }

    @Test
    fun summaryGroupsByTypeAndFamily() {
        val (log, file) = tempLog()
        log.record(EventType.CardReviewed, "study")
        log.record(EventType.CardReviewed, "study")
        log.record(EventType.WritingAttempted, "study")
        log.record(EventType.ExamCompleted, "exam")
        val summary = log.summary()
        assertEquals(4, summary.total)
        assertEquals(2, summary.byType[EventType.CardReviewed])
        assertEquals(3, summary.byFamily[EventFamily.Study])
        assertEquals(1, summary.byFamily[EventFamily.Exam])
        file.delete()
    }

    @Test
    fun recentReturnsNewestFirst() {
        val (log, file) = tempLog()
        log.record(EventType.SyncCompleted, "system")
        log.record(EventType.DictionaryLookup, "dictionary", mapOf("headword" to "水"))
        val recent = log.recent()
        assertEquals(EventType.DictionaryLookup, recent.first().eventType)
        assertTrue(recent.first().occurredAt >= recent.last().occurredAt)
        file.delete()
    }

    @Test
    fun corruptSnapshotRecovers() {
        val file = File.createTempFile("eventlog", ".json")
        file.delete()
        EventLog(file).record(EventType.ImportFinished, "system")
        // Corrupt the snapshot on disk.
        file.writeText("{not json")
        val log = EventLog(file)
        assertEquals(0, log.count()) // survives, empty rather than crashing
        log.record(EventType.SyncCompleted, "system")
        assertEquals(1, log.count())
        file.delete()
    }
}
