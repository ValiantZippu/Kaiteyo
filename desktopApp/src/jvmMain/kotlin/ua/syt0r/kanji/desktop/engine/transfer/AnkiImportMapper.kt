package ua.syt0r.kanji.desktop.engine.transfer

import kotlinx.datetime.Instant
import ua.syt0r.kanji.desktop.model.ContentKind
import ua.syt0r.kanji.desktop.model.DesktopCard
import ua.syt0r.kanji.desktop.model.SrsStatus

// ============================================
// ANKI → KAITEYO IMPORT MAPPER
// Pure, deterministic mapping between Anki
// concepts (notes, fields, decks, scheduling)
// and Kaiteyo's model. No I/O here — the
// importer feeds raw strings/numbers in and
// gets cards + side effects out. This keeps the
// mapping unit-testable and independent of the
// AnkiConnect HTTP client.
// ============================================

object AnkiImportMapper {

    // ------------------------------------------------------------
    // Deck hierarchy
    // ------------------------------------------------------------

    /** `Japanese::N5::Kanji` → [Japanese, N5, Kanji] (case preserved). */
    fun splitDeckPath(fullName: String): List<String> =
        fullName.split("::").map { it.trim() }.filter { it.isNotBlank() }

    /** The last path segment, used as the Kaiteyo deck display name. */
    fun leafDeckName(fullName: String): String =
        splitDeckPath(fullName).lastOrNull() ?: fullName

    /** Build the incremental parent paths: [Japanese, Japanese::N5]. */
    fun parentPaths(fullName: String): List<String> {
        val segments = splitDeckPath(fullName)
        return segments.dropLast(1).runningFold("") { acc, seg ->
            if (acc.isEmpty()) seg else "$acc::$seg"
        }.drop(1)
    }

    // ------------------------------------------------------------
    // Field extraction
    // ------------------------------------------------------------

    /** Case-insensitive field lookup. */
    fun pickField(fields: Map<String, String>, vararg preferred: String): String {
        val lower = fields.mapKeys { it.key.lowercase() }
        preferred.forEach { key ->
            lower[key.lowercase()]?.takeIf { it.isNotBlank() }?.let { return it }
        }
        return ""
    }

    /** Strip HTML tags, entities and Anki media/cloze markup to plain text. */
    fun stripHtml(html: String): String {
        var text = html
        // Anki cloze: {{c1::answer}} and {{c1::answer::hint}}
        text = Regex("\\{\\{c\\d+::(.*?)(?:::.*?)?}}").replace(text) { m ->
            m.groupValues[1]
        }
        // <br>, <p>, </div> become line breaks
        text = Regex("(?i)<br\\s*/?>|</p>|</div>|</li>").replace(text, "\n")
        // Drop any remaining tags
        text = Regex("<[^>]+>").replace(text, "")
        // Anki sound placeholders
        text = Regex("\\[sound:[^\\]]+]").replace(text, "")
        // Entities
        text = text.replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
        // Collapse blank lines / whitespace runs
        return text.lines().map { it.trim() }.filter { it.isNotBlank() }.joinToString("\n").trim()
    }

    /** Media filenames referenced by a note's fields ([sound:x] and <img src>). */
    fun extractMediaFilenames(fields: Map<String, String>): List<String> {
        val all = fields.values.joinToString(" ")
        val out = LinkedHashSet<String>()
        Regex("\\[sound:([^\\]]+)]").findAll(all).forEach { out.add(it.groupValues[1]) }
        Regex("(?i)<img[^>]+src=[\"']([^\"']+)[\"']").findAll(all).forEach {
            out.add(it.groupValues[1].substringBefore("?").substringBefore("#"))
        }
        return out.filter { it.isNotBlank() }.distinct()
    }

    // ------------------------------------------------------------
    // Content kind inference
    // ------------------------------------------------------------

    /** A single kanji is Kanji content; anything else with kana is Vocabulary. */
    fun inferContentKind(headword: String): ContentKind {
        val chars = headword.filter { it.isLetterOrDigit() }
        if (chars.length == 1 && chars[0].code in 0x4E00..0x9FFF) return ContentKind.Kanji
        val hasKana = headword.any { it.code in 0x3040..0x30FF }
        val hasKanji = headword.any { it.code in 0x4E00..0x9FFF }
        return when {
            hasKanji || hasKana -> ContentKind.Vocabulary
            headword.any { it.isLetter() } -> ContentKind.Vocabulary
            else -> ContentKind.Sentence
        }
    }

    // ------------------------------------------------------------
    // Scheduling mapping (honest, approximate where required)
    // ------------------------------------------------------------

    /**
     * Anki queue codes → Kaiteyo status.
     * 0 new, 1 learning, 2 review, 3 day-learning, -1/-2 suspended, -3/-4 buried.
     */
    fun queueToStatus(queue: Long): SrsStatus = when (queue) {
        -3L, -4L -> SrsStatus.Buried
        -2L, -1L -> SrsStatus.Suspended
        2L -> SrsStatus.Review
        1L, 3L -> SrsStatus.Learning
        else -> SrsStatus.New
    }

    /**
     * Anki stores learning intervals in seconds and review intervals in
     * days; normalise to days. Negative/zero → 0.
     */
    fun intervalToDays(interval: Long, queue: Long): Double = when {
        interval <= 0 -> 0.0
        queue == 1L || queue == 3L -> interval / 86400.0
        else -> interval.toDouble()
    }

    /**
     * Anki review `due` is a day number; learning `due` is a millisecond
     * timestamp. We approximate the review day number as days since the
     * Unix epoch (the interpretation modern Anki schedulers use) and map
     * learning timestamps directly. Suspended/buried/new have no due.
     */
    fun dueToInstant(due: Long, queue: Long): Instant? = when {
        due <= 0 -> null
        queue == 1L || queue == 3L -> Instant.fromEpochMilliseconds(due)
        queue == 2L -> Instant.fromEpochMilliseconds(due * 86_400_000L)
        else -> null
    }

    /** Anki ease is permille (2500 → 2.5); clamp to a sane range. */
    fun easeToDouble(ease: Long?): Double {
        val raw = (ease ?: 2500L) / 1000.0
        return raw.coerceIn(1.0, 5.0)
    }

    // ------------------------------------------------------------
    // Identity + dedupe
    // ------------------------------------------------------------

    /** External id stored on Kaiteyo cards so re-imports are idempotent. */
    fun externalIdFor(guid: String): String = "anki:$guid"

    /** Deterministic, stable card id derived from the Anki note guid. */
    fun cardIdFor(guid: String): String =
        "anki-${guid.hashCode().toUInt().toString(16).padStart(8, '0')}"

    /** Fingerprint for notes without a guid (fallback duplicate detection). */
    fun fingerprint(character: String, meaning: String): String {
        val normalizedMeaning = stripHtml(meaning).lowercase().trim().take(120)
        return "${character.trim()}|$normalizedMeaning"
    }

    /**
     * Locate an existing Kaiteyo card that an incoming Anki note collides
     * with: exact external-id match wins, then content fingerprint. Exposed
     * so the dedupe rule is testable without a transport.
     */
    fun findExisting(
        existingByExternal: Map<String, DesktopCard>,
        existingByFingerprint: Map<String, List<DesktopCard>>,
        card: DesktopCard
    ): DesktopCard? = existingByExternal[card.externalId]
        ?: existingByFingerprint[fingerprint(card.character, card.meaning)]?.firstOrNull()

    // ------------------------------------------------------------
    // Note → Kaiteyo card
    // ------------------------------------------------------------

    private val FRONT_KEYS = arrayOf("Front", "Expression", "Word", "Question", "Japanese", "Vocab", "Term", "Text", "First")
    private val READING_KEYS = arrayOf("Reading", "Kana", "Furigana", "Yomikata", "Pronunciation")
    private val SENTENCE_KEYS = arrayOf("Sentence", "Example", "Context", "ExampleSentence", "Usage")
    private val BACK_KEYS = arrayOf("Back", "Meaning", "Definition", "English", "Explanation", "Gloss", "Answer", "Hint")

    /**
     * Build a Kaiteyo card from an Anki note's fields and scheduling.
     * All fields are preserved: primary ones map onto the card's typed
     * slots, the remainder are kept in the note so nothing disappears.
     */
    fun noteToCard(
        guid: String,
        fields: Map<String, String>,
        tags: List<String>,
        scheduling: AnkiSchedulingData?,
        deckId: String,
        includeScheduling: Boolean,
        ankiDeckName: String,
        createdAt: Instant = kotlinx.datetime.Clock.System.now()
    ): DesktopCard {
        val headword = stripHtml(pickField(fields, *FRONT_KEYS))
            .ifBlank { fields.entries.firstOrNull { it.value.isNotBlank() }?.let { stripHtml(it.value) }.orEmpty() }
        val reading = stripHtml(pickField(fields, *READING_KEYS))
        val meaning = stripHtml(
            pickField(fields, *BACK_KEYS).ifBlank {
                val known = (FRONT_KEYS + READING_KEYS + SENTENCE_KEYS + BACK_KEYS)
                    .map { it.lowercase() }.toSet()
                fields.entries
                    .filterNot { (k, v) -> k.lowercase() in known || v.isBlank() }
                    .joinToString("\n") { (_, v) -> stripHtml(v) }
            }
        ).take(800)
        val sentence = stripHtml(pickField(fields, *SENTENCE_KEYS))

        val note = buildString {
            if (sentence.isNotBlank()) append("Sentence: ").append(sentence).append("\n")
            append("Anki deck: ").append(ankiDeckName).append("\n")
            val known = (FRONT_KEYS + READING_KEYS + SENTENCE_KEYS + BACK_KEYS).map { it.lowercase() }.toSet()
            fields.forEach { (name, value) ->
                if (name.lowercase() !in known && value.isNotBlank()) {
                    append(name).append(": ").append(value.trim()).append("\n")
                }
            }
            append("Source: AnkiConnect")
        }.trim()

        val status = if (includeScheduling) queueToStatus(scheduling?.queue ?: 0L)
        else SrsStatus.New

        return DesktopCard(
            id = cardIdFor(guid),
            character = headword.take(60),
            meaning = meaning,
            onReadings = reading.takeIf { it.isNotBlank() }?.let { listOf(it) } ?: emptyList(),
            tags = (listOf("anki", "anki:${leafDeckName(ankiDeckName)}") + tags).distinct(),
            note = note,
            status = status,
            intervalDays = if (includeScheduling) intervalToDays(scheduling?.interval ?: 0L, scheduling?.queue ?: 0L) else 0.0,
            dueAt = if (includeScheduling) dueToInstant(scheduling?.due ?: 0L, scheduling?.queue ?: 0L) else null,
            lapses = if (includeScheduling) (scheduling?.lapses ?: 0).toInt() else 0,
            reps = if (includeScheduling) (scheduling?.reps ?: 0).toInt() else 0,
            ease = if (includeScheduling) easeToDouble(scheduling?.ease) else 2.5,
            deckId = deckId,
            createdAt = createdAt,
            contentKind = inferContentKind(headword),
            externalId = externalIdFor(guid)
        )
    }
}

/** Anki scheduling fields extracted from a cardsInfo entry. */
data class AnkiSchedulingData(
    val queue: Long,
    val interval: Long,
    val due: Long,
    val reps: Long,
    val lapses: Long,
    val ease: Long
)
