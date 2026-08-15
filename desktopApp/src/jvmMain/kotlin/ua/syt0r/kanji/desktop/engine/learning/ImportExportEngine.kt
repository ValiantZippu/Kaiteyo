package ua.syt0r.kanji.desktop.engine.learning

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

// ============================================
// IMPORT / EXPORT ENGINE
// Robust CSV / JSON / TSV import & export of the
// unified learning model with deduplication,
// validation and progress reporting. Never loses
// learning history: exports include notes, cards,
// deck configs, review events and exam results.
// ============================================

@Serializable
data class ExportPayload(
    val version: Int = 1,
    val exportedAt: String = Clock.System.now().toString(),
    val notes: List<LearningNote> = emptyList(),
    val cards: List<NoteCard> = emptyList(),
    val deckConfigs: Map<String, DeckStudyConfig> = emptyMap(),
    val reviewEvents: List<LearningReviewEvent> = emptyList(),
    val writingAttempts: List<WritingAttemptEvent> = emptyList(),
    val examResults: List<ExamResult> = emptyList(),
    val sessions: List<StudySessionRecord> = emptyList()
)

data class ImportResult(
    val notesAdded: Int = 0,
    val notesUpdated: Int = 0,
    val cardsAdded: Int = 0,
    val eventsImported: Int = 0,
    val skipped: Int = 0,
    val errors: List<String> = emptyList()
)

object ImportExportEngine {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // ------------------------------------------------------------
    // Export
    // ------------------------------------------------------------

    fun exportSnapshot(store: LearningStore): String = json.encodeToString(
        ExportPayload(
            notes = store.notes.toList(),
            cards = store.cards.toList(),
            deckConfigs = store.deckConfigs.toMap(),
            reviewEvents = store.reviewEvents.toList(),
            writingAttempts = store.writingAttempts.toList(),
            examResults = store.examResults.toList(),
            sessions = store.sessions.toList()
        )
    )

    /** Export just notes+cards as CSV (spreadsheet-friendly). */
    fun exportCsv(store: LearningStore): String {
        val header = "id,kind,expression,reading,meaning,jlpt,grade,frequency,stage,status,interval_days,due_at,reps,lapses,accuracy,tags"
        val rows = store.cards.mapNotNull { card ->
            val note = store.note(card.noteId) ?: return@mapNotNull null
            listOf(
                csv(card.id),
                csv(note.kind.name),
                csv(note.expression),
                csv(note.reading),
                csv(note.meanings.joinToString("; ")),
                note.jlpt?.toString() ?: "",
                note.grade?.toString() ?: "",
                note.frequency?.toString() ?: "",
                csv(card.stage.name),
                csv(card.status.name),
                card.intervalDays.toString(),
                card.dueAt?.toString().orEmpty(),
                card.reps.toString(),
                card.lapses.toString(),
                "%.3f".format(card.accuracy),
                csv(note.tags.joinToString(" "))
            ).joinToString(",")
        }
        return (listOf(header) + rows).joinToString("\n")
    }

    /** Export the full snapshot as TSV (tabs, spreadsheet safe). */
    fun exportTsv(store: LearningStore): String {
        val header = listOf(
            "id", "kind", "expression", "reading", "meanings", "jlpt", "frequency",
            "stage", "status", "interval_days", "due_at", "reps", "lapses", "accuracy", "tags"
        ).joinToString("\t")
        val rows = store.cards.mapNotNull { card ->
            val note = store.note(card.noteId) ?: return@mapNotNull null
            listOf(
                card.id, note.kind.name, note.expression, note.reading,
                note.meanings.joinToString("; "),
                note.jlpt?.toString() ?: "", note.frequency?.toString() ?: "",
                card.stage.name, card.status.name, card.intervalDays.toString(),
                card.dueAt?.toString().orEmpty(), card.reps.toString(), card.lapses.toString(),
                "%.3f".format(card.accuracy), note.tags.joinToString(" ")
            ).joinToString("\t") { v -> v.replace("\t", " ").replace("\n", " ") }
        }
        return (listOf(header) + rows).joinToString("\n")
    }

    private fun csv(value: String): String {
        val v = value.replace("\"", "\"\"")
        return if (v.contains(',') || v.contains('"') || v.contains('\n')) "\"$v\"" else v
    }

    // ------------------------------------------------------------
    // Import
    // ------------------------------------------------------------

    /** Import a native JSON snapshot (full fidelity). */
    fun importJson(store: LearningStore, text: String, onProgress: (Int, Int) -> Unit = { _, _ -> }): ImportResult {
        return runCatching {
            val payload = json.decodeFromString<ExportPayload>(text)
            var added = 0
            var updated = 0
            payload.notes.forEachIndexed { i, note ->
                val existing = store.note(note.id)
                val before = store.notes.size
                store.upsertNote(note)
                if (store.notes.size > before) added++ else if (existing != null) updated++
                onProgress(i + 1, payload.notes.size)
            }
            val beforeCards = store.cards.size
            payload.cards.forEach { store.upsertCard(it) }
            val cardsAdded = store.cards.size - beforeCards
            payload.deckConfigs.forEach { (id, config) -> store.deckConfigs[id] = config }
            payload.reviewEvents.forEach { store.recordReview(it) }
            payload.writingAttempts.forEach { store.recordWriting(it) }
            payload.examResults.forEach { store.recordExam(it) }
            payload.sessions.forEach { store.recordSession(it) }
            store.save()
            ImportResult(
                notesAdded = added,
                notesUpdated = updated,
                cardsAdded = cardsAdded,
                eventsImported = payload.reviewEvents.size + payload.writingAttempts.size + payload.examResults.size
            )
        }.getOrElse { e ->
            ImportResult(errors = listOf(e.message ?: "Import failed"))
        }
    }

    /** Import a CSV file (id,kind,expression,reading,meaning,...). */
    fun importCsv(store: LearningStore, text: String): ImportResult {
        val lines = text.lines().filter { it.isNotBlank() }
        if (lines.size < 2) return ImportResult(errors = listOf("CSV needs a header and at least one row"))
        val header = parseCsvLine(lines.first()).map { it.trim() }
        val idx = { name: String -> header.indexOf(name) }
        val result = ImportResult()
        var added = 0
        var skipped = 0
        val errors = mutableListOf<String>()

        lines.drop(1).forEachIndexed { i, line ->
            try {
                val cols = parseCsvLine(line)
                val kind = LearningItemKind.entries.firstOrNull { it.name.equals(cols.getOrNull(idx("kind")) ?: "", ignoreCase = true) }
                    ?: return@forEachIndexed
                val expression = cols.getOrNull(idx("expression"))?.trim() ?: return@forEachIndexed
                if (expression.isBlank()) return@forEachIndexed
                val reading = cols.getOrNull(idx("reading"))?.trim().orEmpty()
                val note = LearningNote(
                    id = LearningIds.noteId(kind, expression, reading),
                    kind = kind,
                    expression = expression,
                    reading = reading,
                    meanings = (cols.getOrNull(idx("meaning")) ?: "").split("; ").filter { it.isNotBlank() },
                    jlpt = cols.getOrNull(idx("jlpt"))?.toIntOrNull(),
                    grade = cols.getOrNull(idx("grade"))?.toIntOrNull(),
                    frequency = cols.getOrNull(idx("frequency"))?.toIntOrNull(),
                    tags = (cols.getOrNull(idx("tags")) ?: "").split(" ").filter { it.isNotBlank() },
                    source = NoteSource(NoteSourceType.Import)
                )
                val before = store.notes.size
                store.upsertNote(note)
                if (store.notes.size > before) added++
            } catch (e: Exception) {
                errors.add("Line ${i + 2}: ${e.message}")
            }
        }
        store.save()
        return result.copy(notesAdded = added, skipped = skipped, errors = errors)
    }

    private fun parseCsvLine(line: String): List<String> {
        val out = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        line.forEach { c ->
            when {
                c == '"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> {
                    out.add(sb.toString()); sb.clear()
                }
                else -> sb.append(c)
            }
        }
        out.add(sb.toString())
        return out
    }

    /** Import a TSV file. */
    fun importTsv(store: LearningStore, text: String): ImportResult {
        val lines = text.lines().filter { it.isNotBlank() }
        if (lines.size < 2) return ImportResult(errors = listOf("TSV needs a header and at least one row"))
        val header = lines.first().split("\t").map { it.trim() }
        val idx = { name: String -> header.indexOf(name) }
        var added = 0
        val errors = mutableListOf<String>()
        lines.drop(1).forEachIndexed { i, line ->
            try {
                val cols = line.split("\t")
                val kind = LearningItemKind.entries.firstOrNull { it.name.equals(cols.getOrNull(idx("kind")) ?: "", ignoreCase = true) }
                    ?: return@forEachIndexed
                val expression = cols.getOrNull(idx("expression"))?.trim() ?: return@forEachIndexed
                if (expression.isBlank()) return@forEachIndexed
                val reading = cols.getOrNull(idx("reading"))?.trim().orEmpty()
                val note = LearningNote(
                    id = LearningIds.noteId(kind, expression, reading),
                    kind = kind,
                    expression = expression,
                    reading = reading,
                    meanings = (cols.getOrNull(idx("meanings")) ?: "").split("; ").filter { it.isNotBlank() },
                    jlpt = cols.getOrNull(idx("jlpt"))?.toIntOrNull(),
                    frequency = cols.getOrNull(idx("frequency"))?.toIntOrNull(),
                    tags = (cols.getOrNull(idx("tags")) ?: "").split(" ").filter { it.isNotBlank() },
                    source = NoteSource(NoteSourceType.Import)
                )
                val before = store.notes.size
                store.upsertNote(note)
                if (store.notes.size > before) added++
            } catch (e: Exception) {
                errors.add("Line ${i + 2}: ${e.message}")
            }
        }
        store.save()
        return ImportResult(notesAdded = added, errors = errors)
    }
}
