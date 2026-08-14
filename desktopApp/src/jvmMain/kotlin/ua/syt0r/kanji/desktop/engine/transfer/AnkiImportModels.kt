package ua.syt0r.kanji.desktop.engine.transfer

// ============================================
// ANKI → KAITEYO IMPORT MODELS
// Preview + options + per-deck results for the
// AnkiConnect import direction. Kaiteyo's own
// deck hierarchy and card pool are the target;
// the Anki deck path is preserved as nesting.
// ============================================

/** One Anki deck as seen in the import preview. */
data class AnkiDeckPreview(
    /** Full Anki path, e.g. `Japanese::N5::Kanji`. */
    val name: String,
    val ankiDeckId: Long,
    val noteCount: Int,
    val cardCount: Int,
    /** Distinct tags across the deck's notes (sampled). */
    val tags: List<String> = emptyList(),
    /** Sample front fields for the user to inspect. */
    val sampleFronts: List<String> = emptyList()
)

/** How to treat a note that already exists in Kaiteyo. */
enum class AnkiConflictPolicy(val label: String, val description: String) {
    Skip("Skip", "Keep the existing card, drop the Anki note"),
    Update("Update", "Overwrite the existing card with the Anki note"),
    Duplicate("Duplicate", "Import a second copy anyway")
}

data class AnkiImportOptions(
    val policy: AnkiConflictPolicy = AnkiConflictPolicy.Skip,
    /** Carry over interval/due/reps/lapses/ease from Anki scheduling. */
    val includeScheduling: Boolean = true,
    /** Download audio/images referenced by note fields. */
    val includeMedia: Boolean = true,
    /** Import cards directly into the deck's name as-is. */
    val deck: String = ""
)

/** Per-deck outcome after an import run. */
data class AnkiDeckImportResult(
    val deckName: String,
    val imported: Int = 0,
    val updated: Int = 0,
    val skipped: Int = 0,
    val duplicated: Int = 0,
    val mediaSaved: Int = 0,
    val warnings: List<String> = emptyList()
) {
    val total: Int get() = imported + updated + skipped + duplicated
}
