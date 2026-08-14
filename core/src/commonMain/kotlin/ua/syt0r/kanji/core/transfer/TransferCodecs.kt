package ua.syt0r.kanji.core.transfer

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ua.syt0r.kanji.presentation.screen.main.screen.decks.CardFlagType
import ua.syt0r.kanji.presentation.screen.main.screen.decks.CardStatus
import ua.syt0r.kanji.presentation.screen.main.screen.decks.KaiteyoCard

// ============================================
// CORE TRANSFER CODECS
// Pure string codecs for JSON / CSV / TSV / TXT.
// Deterministic, lossless for KaiteyoCard model.
// ============================================

enum class TransferFormat { Json, Csv, Tsv, Txt }

@Serializable
data class TransferCard(
    val id: String = "",
    val character: String = "",
    val meaning: String = "",
    val reading: String = "",
    val deck: String = "",
    val deckId: Long = 0L,
    val tags: List<String> = emptyList(),
    val flag: String = "None",
    val notes: String = "",
    val status: String = "New",
    val difficulty: String = "Good",
    val priority: Int = 0,
    val isSuspended: Boolean = false,
    val isBuried: Boolean = false,
    val isArchived: Boolean = false,
    val isFavorite: Boolean = false,
    val customFields: Map<String, String> = emptyMap(),
    val aliases: List<String> = emptyList(),
    val relatedCards: List<String> = emptyList(),
    val createdAt: String = "",
    val modifiedAt: String = "",
    val lastReviewed: String = "",
    val reviewCount: Int = 0,
    val interval: Int = 0,
    val ease: Float = 2.5f,
    val lapses: Int = 0,
    val accuracy: Float = 0.5f,
    val totalTimeStudied: Long = 0L
) {
    companion object {
        fun fromKaiteyoCard(card: KaiteyoCard): TransferCard {
            return TransferCard(
                id = card.id,
                character = card.character,
                meaning = card.meaning,
                reading = card.reading,
                deck = card.deck,
                deckId = card.deckId,
                tags = card.tagNames.toList(),
                flag = card.flag.name,
                notes = card.notes,
                status = card.status.name,
                difficulty = card.difficulty.name,
                priority = card.priority,
                isSuspended = card.isSuspended,
                isBuried = card.isBuried,
                isArchived = card.isArchived,
                isFavorite = card.isFavorite,
                customFields = card.customFields.toMap(),
                aliases = card.aliases.toList(),
                relatedCards = card.relatedCards.toList(),
                createdAt = card.createdAt,
                modifiedAt = card.modifiedAt,
                lastReviewed = card.lastReviewed,
                reviewCount = card.reviewCount,
                interval = card.interval,
                ease = card.ease,
                lapses = card.lapses,
                accuracy = card.accuracy,
                totalTimeStudied = card.totalTimeStudied
            )
        }

        fun toKaiteyoCard(transfer: TransferCard): KaiteyoCard {
            return KaiteyoCard(
                // Sparse imports (e.g. a pasted "character, meaning" JSON row)
                // carry no id — derive a stable one so cards stay deduplicable.
                id = transfer.id.ifBlank { "imported-${transfer.character.hashCode().toString(16)}" },
                character = transfer.character,
                meaning = transfer.meaning,
                reading = transfer.reading,
                deck = transfer.deck,
                deckId = transfer.deckId,
                tagNames = transfer.tags.toMutableList(),
                flag = CardFlagType.entries.firstOrNull { it.name == transfer.flag } ?: CardFlagType.None,
                notes = transfer.notes,
                status = CardStatus.entries.firstOrNull { it.name == transfer.status } ?: CardStatus.New,
                difficulty = ua.syt0r.kanji.presentation.screen.main.screen.decks.CardDifficulty.entries.firstOrNull { it.name == transfer.difficulty } ?: ua.syt0r.kanji.presentation.screen.main.screen.decks.CardDifficulty.Good,
                priority = transfer.priority,
                isSuspended = transfer.isSuspended,
                isBuried = transfer.isBuried,
                isArchived = transfer.isArchived,
                isFavorite = transfer.isFavorite,
                customFields = transfer.customFields.toMutableMap(),
                aliases = transfer.aliases.toMutableList(),
                relatedCards = transfer.relatedCards.toMutableList(),
                createdAt = transfer.createdAt,
                modifiedAt = transfer.modifiedAt,
                lastReviewed = transfer.lastReviewed,
                reviewCount = transfer.reviewCount,
                interval = transfer.interval,
                ease = transfer.ease,
                lapses = transfer.lapses,
                accuracy = transfer.accuracy,
                totalTimeStudied = transfer.totalTimeStudied
            )
        }
    }
}

object TransferCodecs {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val HEADERS = listOf(
        "id", "character", "meaning", "reading", "deck", "deckId",
        "tags", "flag", "notes", "status", "difficulty", "priority",
        "isSuspended", "isBuried", "isArchived", "isFavorite",
        "customFields", "aliases", "relatedCards",
        "createdAt", "modifiedAt", "lastReviewed",
        "reviewCount", "interval", "ease", "lapses", "accuracy", "totalTimeStudied"
    )

    // ------------------------------------------------------------
    // JSON
    // ------------------------------------------------------------

    fun toJson(cards: List<KaiteyoCard>): String {
        val transferCards = cards.map { TransferCard.fromKaiteyoCard(it) }
        return json.encodeToString(transferCards)
    }

    fun fromJson(text: String): Result<List<KaiteyoCard>> = runCatching {
        val transferCards = json.decodeFromString<List<TransferCard>>(text)
        transferCards.map { TransferCard.toKaiteyoCard(it) }
    }

    // ------------------------------------------------------------
    // CSV / TSV
    // ------------------------------------------------------------

    fun toCsv(cards: List<KaiteyoCard>): String = toDelimited(cards, ',')
    fun toTsv(cards: List<KaiteyoCard>): String = toDelimited(cards, '\t')

    fun fromCsv(text: String): Result<List<KaiteyoCard>> = fromDelimited(text, ',')
    fun fromTsv(text: String): Result<List<KaiteyoCard>> = fromDelimited(text, '\t')

    private fun toDelimited(cards: List<KaiteyoCard>, delimiter: Char): String {
        val sb = StringBuilder()
        sb.append(HEADERS.joinToString(delimiter.toString()))
        cards.forEach { card ->
            val transfer = TransferCard.fromKaiteyoCard(card)
            sb.append('\n')
            sb.append(
                listOf(
                    transfer.id,
                    transfer.character,
                    transfer.meaning,
                    transfer.reading,
                    transfer.deck,
                    transfer.deckId.toString(),
                    joinList(transfer.tags),
                    transfer.flag,
                    escape(transfer.notes, delimiter),
                    transfer.status,
                    transfer.difficulty,
                    transfer.priority.toString(),
                    transfer.isSuspended.toString(),
                    transfer.isBuried.toString(),
                    transfer.isArchived.toString(),
                    transfer.isFavorite.toString(),
                    escape(mapToString(transfer.customFields), delimiter),
                    joinList(transfer.aliases),
                    joinList(transfer.relatedCards),
                    transfer.createdAt,
                    transfer.modifiedAt,
                    transfer.lastReviewed,
                    transfer.reviewCount.toString(),
                    transfer.interval.toString(),
                    transfer.ease.toString(),
                    transfer.lapses.toString(),
                    transfer.accuracy.toString(),
                    transfer.totalTimeStudied.toString()
                ).joinToString(delimiter.toString()) { escape(it, delimiter) }
            )
        }
        return sb.toString()
    }

    private fun fromDelimited(text: String, delimiter: Char): Result<List<KaiteyoCard>> = runCatching {
        val lines = text.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return@runCatching emptyList()
        val cards = mutableListOf<KaiteyoCard>()
        lines.drop(1).forEach { line ->
            val cells = splitLine(line, delimiter)
            val row = HEADERS.indices.associate { i -> HEADERS[i] to cells.getOrElse(i) { "" } }
            cards.add(
                KaiteyoCard(
                    id = row["id"] ?: "",
                    character = row["character"] ?: "",
                    meaning = row["meaning"] ?: "",
                    reading = row["reading"] ?: "",
                    deck = row["deck"] ?: "",
                    deckId = row["deckId"]?.toLongOrNull() ?: 0L,
                    tagNames = splitList(row["tags"]).toMutableList(),
                    flag = CardFlagType.entries.firstOrNull { it.name == row["flag"] } ?: CardFlagType.None,
                    notes = row["notes"] ?: "",
                    status = CardStatus.entries.firstOrNull { it.name == row["status"] } ?: CardStatus.New,
                    difficulty = ua.syt0r.kanji.presentation.screen.main.screen.decks.CardDifficulty.entries.firstOrNull { it.name == row["difficulty"] } ?: ua.syt0r.kanji.presentation.screen.main.screen.decks.CardDifficulty.Good,
                    priority = row["priority"]?.toIntOrNull() ?: 0,
                    isSuspended = row["isSuspended"]?.toBooleanStrictOrNull() ?: false,
                    isBuried = row["isBuried"]?.toBooleanStrictOrNull() ?: false,
                    isArchived = row["isArchived"]?.toBooleanStrictOrNull() ?: false,
                    isFavorite = row["isFavorite"]?.toBooleanStrictOrNull() ?: false,
                    customFields = stringToMap(row["customFields"] ?: "").toMutableMap(),
                    aliases = splitList(row["aliases"]).toMutableList(),
                    relatedCards = splitList(row["relatedCards"]).toMutableList(),
                    createdAt = row["createdAt"] ?: "",
                    modifiedAt = row["modifiedAt"] ?: "",
                    lastReviewed = row["lastReviewed"] ?: "",
                    reviewCount = row["reviewCount"]?.toIntOrNull() ?: 0,
                    interval = row["interval"]?.toIntOrNull() ?: 0,
                    ease = row["ease"]?.toFloatOrNull() ?: 2.5f,
                    lapses = row["lapses"]?.toIntOrNull() ?: 0,
                    accuracy = row["accuracy"]?.toFloatOrNull() ?: 0f,
                    totalTimeStudied = row["totalTimeStudied"]?.toLongOrNull() ?: 0L
                )
            )
        }
        cards
    }

    // ------------------------------------------------------------
    // TXT (simple line-based: character = meaning | reading)
    // ------------------------------------------------------------

    fun toTxt(cards: List<KaiteyoCard>): String = cards.joinToString("\n") { card ->
        "${card.character}\t${card.meaning}\t${card.reading}"
    }

    fun fromTxt(text: String): Result<List<KaiteyoCard>> = runCatching {
        text.lines()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.split('\t')
                val character = parts.getOrNull(0)?.trim() ?: return@mapNotNull null
                val meaning = parts.getOrNull(1)?.trim() ?: ""
                val reading = parts.getOrNull(2)?.trim() ?: ""
                KaiteyoCard(
                    id = "txt-${character.hashCode()}",
                    character = character,
                    meaning = meaning,
                    reading = reading,
                    status = CardStatus.New
                )
            }
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

    private fun mapToString(map: Map<String, String>): String =
        map.entries.joinToString(";") { "${escape(it.key, ';')}=${escape(it.value, ';')}" }

    private fun stringToMap(value: String): Map<String, String> =
        if (value.isBlank()) emptyMap()
        else value.split(';').mapNotNull { entry ->
            val parts = entry.split('=', limit = 2)
            val key = parts.firstOrNull()?.trim()
            if (key.isNullOrBlank()) null else key to (parts.getOrNull(1)?.trim() ?: "")
        }.toMap()

    private fun splitLine(line: String, delimiter: Char): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> {
                    current.append('"')
                    i++
                }
                c == '"' -> inQuotes = !inQuotes
                c == delimiter && !inQuotes -> {
                    result.add(current.toString())
                    current.setLength(0)
                }
                else -> current.append(c)
            }
            i++
        }
        result.add(current.toString())
        return result
    }
}