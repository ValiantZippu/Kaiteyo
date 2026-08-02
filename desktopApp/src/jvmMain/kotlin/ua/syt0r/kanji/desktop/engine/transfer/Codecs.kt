package ua.syt0r.kanji.desktop.engine.transfer

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ua.syt0r.kanji.desktop.model.DesktopCard

// ============================================
// CODECS
// Pure string codecs for JSON / CSV / TSV / TXT.
// Deterministic, lossless for the card model, and
// unit-testable without platform IO.
// ============================================

enum class TransferFormat { Json, Csv, Tsv, Txt }

object Codecs {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // ------------------------------------------------------------
    // JSON
    // ------------------------------------------------------------

    fun toJson(cards: List<DesktopCard>): String = json.encodeToString(cards)

    fun fromJson(text: String): List<DesktopCard> =
        json.decodeFromString<List<DesktopCard>>(text)

    // ------------------------------------------------------------
    // CSV / TSV
    // ------------------------------------------------------------

    val HEADERS = listOf(
        "id", "character", "meaning", "onReadings", "kunReadings", "radicals",
        "strokeCount", "jlpt", "grade", "frequency", "tags", "flags", "favorite",
        "note", "status", "intervalDays", "dueAt", "lapses", "reps", "ease",
        "accuracy", "deckId"
    )

    fun toCsv(cards: List<DesktopCard>): String = toDelimited(cards, ',')
    fun toTsv(cards: List<DesktopCard>): String = toDelimited(cards, '\t')

    fun fromCsv(text: String): List<DesktopCard> = fromDelimited(text, ',')
    fun fromTsv(text: String): List<DesktopCard> = fromDelimited(text, '\t')

    private fun toDelimited(cards: List<DesktopCard>, delimiter: Char): String {
        val sb = StringBuilder()
        sb.append(HEADERS.joinToString(delimiter.toString()))
        cards.forEach { card ->
            sb.append('\n')
            sb.append(
                listOf(
                    card.id,
                    card.character,
                    card.meaning,
                    joinList(card.onReadings),
                    joinList(card.kunReadings),
                    joinList(card.radicals),
                    card.strokeCount.toString(),
                    card.jlpt?.toString() ?: "",
                    card.grade?.toString() ?: "",
                    card.frequency?.toString() ?: "",
                    joinList(card.tags),
                    joinList(card.flags),
                    card.favorite.toString(),
                    escape(card.note, delimiter),
                    card.status.name,
                    card.intervalDays.toString(),
                    card.dueAt?.toString() ?: "",
                    card.lapses.toString(),
                    card.reps.toString(),
                    card.ease.toString(),
                    card.accuracy.toString(),
                    card.deckId
                ).joinToString(delimiter.toString()) { escape(it, delimiter) }
            )
        }
        return sb.toString()
    }

    private fun fromDelimited(text: String, delimiter: Char): List<DesktopCard> {
        val lines = text.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()
        val header = splitLine(lines.first(), delimiter)
        val cards = mutableListOf<DesktopCard>()
        lines.drop(1).forEach { line ->
            val cells = splitLine(line, delimiter)
            val row = HEADERS.indices.associate { i -> HEADERS[i] to cells.getOrElse(i) { "" } }
            val jlpt = row["jlpt"]?.toIntOrNull()
            val grade = row["grade"]?.toIntOrNull()
            val freq = row["frequency"]?.toIntOrNull()
            cards.add(
                DesktopCard(
                    id = row["id"] ?: "",
                    character = row["character"] ?: "",
                    meaning = row["meaning"] ?: "",
                    onReadings = splitList(row["onReadings"]),
                    kunReadings = splitList(row["kunReadings"]),
                    radicals = splitList(row["radicals"]),
                    strokeCount = row["strokeCount"]?.toIntOrNull() ?: 0,
                    jlpt = jlpt,
                    grade = grade,
                    frequency = freq,
                    tags = splitList(row["tags"]),
                    flags = splitList(row["flags"]),
                    favorite = row["favorite"]?.toBooleanStrictOrNull() ?: false,
                    note = row["note"] ?: "",
                    status = ua.syt0r.kanji.desktop.model.SrsStatus.fromName(row["status"]),
                    intervalDays = row["intervalDays"]?.toDoubleOrNull() ?: 0.0,
                    dueAt = row["dueAt"]?.let { kotlinx.datetime.Instant.parse(it) },
                    lapses = row["lapses"]?.toIntOrNull() ?: 0,
                    reps = row["reps"]?.toIntOrNull() ?: 0,
                    ease = row["ease"]?.toDoubleOrNull() ?: 2.5,
                    accuracy = row["accuracy"]?.toFloatOrNull() ?: 0.5f,
                    deckId = row["deckId"] ?: ua.syt0r.kanji.desktop.model.DesktopCard.DEFAULT_DECK_ID
                )
            )
        }
        return cards
    }

    // ------------------------------------------------------------
    // TXT (simple line-based: character = meaning | readings)
    // ------------------------------------------------------------

    fun toTxt(cards: List<DesktopCard>): String = cards.joinToString("\n") { card ->
        val readings = card.readings.joinToString(", ")
        "${card.character}\t${card.meaning}${if (readings.isNotEmpty()) "\t$readings" else ""}"
    }

    fun fromTxt(text: String): List<DesktopCard> = text.lines()
        .filter { it.isNotBlank() }
        .mapNotNull { line ->
            val parts = line.split('\t')
            val character = parts.getOrNull(0)?.trim() ?: return@mapNotNull null
            val meaning = parts.getOrNull(1)?.trim() ?: ""
            val readings = parts.getOrNull(2)?.split(',')?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
            DesktopCard(
                id = "txt-${character.hashCode()}",
                character = character,
                meaning = meaning,
                onReadings = emptyList(),
                kunReadings = readings,
                status = ua.syt0r.kanji.desktop.model.SrsStatus.New
            )
        }

    // ------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------

    private fun joinList(list: List<String>): String = list.joinToString("|")
    private fun splitList(value: String?): List<String> =
        value?.split('|')?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()

    private fun escape(value: String, delimiter: Char): String {
        val needsQuoting = value.contains(delimiter) || value.contains('"') || value.contains('\n')
        if (!needsQuoting) return value
        return "\"" + value.replace("\"", "\"\"") + "\""
    }

    private fun splitLine(line: String, delimiter: Char): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> {
                    current.append('"'); i++
                }
                c == '"' -> inQuotes = !inQuotes
                c == delimiter && !inQuotes -> {
                    result.add(current.toString()); current.setLength(0)
                }
                else -> current.append(c)
            }
            i++
        }
        result.add(current.toString())
        return result
    }
}
