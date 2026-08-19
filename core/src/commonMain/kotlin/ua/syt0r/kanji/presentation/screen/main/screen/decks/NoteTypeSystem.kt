package ua.syt0r.kanji.presentation.screen.main.screen.decks

// ============================================
// KAITEYO NOTE TYPE SYSTEM
// Every flashcard deck uses a note type — the
// schema of fields a note can carry. Kaiteyo
// ships with a default note type (cloze-capable,
// same spirit as Anki's "Basic (and reversed
// card)" + "Cloze") and lets users build custom
// note types from scratch.
// ============================================

/** A single named field of a note type. */
data class KaiteyoNoteField(
    val id: String,
    val label: String,
    val kind: NoteFieldKind = NoteFieldKind.Text
)

enum class NoteFieldKind {
    Text,
    Audio,
    Image,
    Cloze
}

/** A note type: the field schema notes in a deck follow. */
data class KaiteyoNoteType(
    val id: String,
    val name: String,
    val description: String = "",
    val fields: List<KaiteyoNoteField>,
    val supportsCloze: Boolean = false,
    val isDefault: Boolean = false
)

/** The shipped Kaiteyo note types. */
val defaultKaiteyoNoteTypes = listOf(
    KaiteyoNoteType(
        id = "kaiteyo-default",
        name = "Kaiteyo (default)",
        description = "Front: Japanese expression. Back: reading, meaning, example and notes.",
        fields = listOf(
            KaiteyoNoteField("expression", "Expression"),
            KaiteyoNoteField("reading", "Reading"),
            KaiteyoNoteField("meaning", "Meaning"),
            KaiteyoNoteField("example", "Example sentence"),
            KaiteyoNoteField("audio", "Audio", NoteFieldKind.Audio),
            KaiteyoNoteField("notes", "Notes")
        ),
        supportsCloze = true,
        isDefault = true
    ),
    KaiteyoNoteType(
        id = "kaiteyo-basic",
        name = "Basic",
        description = "Simple front / back flashcard.",
        fields = listOf(
            KaiteyoNoteField("front", "Front"),
            KaiteyoNoteField("back", "Back")
        )
    ),
    KaiteyoNoteType(
        id = "kaiteyo-cloze",
        name = "Cloze",
        description = "Hide parts of a sentence with {{c1::…}} deletions.",
        fields = listOf(
            KaiteyoNoteField("text", "Text", NoteFieldKind.Cloze),
            KaiteyoNoteField("extra", "Extra")
        ),
        supportsCloze = true
    )
)

/**
 * Reads the note type a card uses from its custom fields ("noteType" holds
 * the note type id; "noteFields" holds the JSON-ish field values).
 */
fun KaiteyoCard.noteTypeId(): String = customFields["noteType"] ?: "kaiteyo-default"

fun KaiteyoCard.noteFieldValue(fieldId: String): String =
    customFields["field:$fieldId"] ?: ""

fun KaiteyoCard.withNoteField(fieldId: String, value: String): KaiteyoCard {
    customFields["field:$fieldId"] = value
    return this
}

fun KaiteyoCard.withNoteType(noteTypeId: String): KaiteyoCard {
    customFields["noteType"] = noteTypeId
    return this
}

/**
 * Inserts a cloze deletion around the selected text (or at the caret when no
 * text is selected) using the next free cloze number in the field.
 *
 * あなたは{{c1::日本人}}です。 → cloze index 1 on the next insert.
 */
fun insertCloze(fieldText: String, selectionStart: Int, selectionEnd: Int): Pair<String, Int> {
    // Find the next free cloze number: c1, c2, ... cN already used.
    val usedNumbers = Regex("""\{\{c(\d+)::""")
        .findAll(fieldText)
        .mapNotNull { it.groupValues.getOrNull(1)?.toIntOrNull() }
        .toSet()
    var next = 1
    while (next in usedNumbers) next++

    val safeStart = selectionStart.coerceIn(0, fieldText.length)
    val safeEnd = selectionEnd.coerceIn(safeStart, fieldText.length)
    val selected = fieldText.substring(safeStart, safeEnd)

    val replacement = if (selected.isBlank()) {
        // No selection: insert a placeholder the user can type over.
        "{{c$next::}}"
    } else {
        "{{c$next::$selected}}"
    }

    val updated = fieldText.substring(0, safeStart) + replacement + fieldText.substring(safeEnd)
    // Return the new text and the caret position after the inserted deletion.
    return updated to (safeStart + replacement.length)
}

/** True when the field text contains at least one cloze deletion. */
fun String.hasClozeDeletions(): Boolean = Regex("""\{\{c\d+::""").containsMatchIn(this)
