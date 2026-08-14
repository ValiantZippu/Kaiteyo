package ua.syt0r.kanji.desktop.engine.jdata.profiles

// ============================================================
// DATABASE PROFILES
// Optional datasets are composable parts; profiles pick a coherent
// set. minimal = kanji + kana + strokes + radicals; standard adds
// vocabulary, JLPT, frequency, furigana and components; full adds
// every optional extension (pitch, grammar, examples). Profiles
// are filters applied at build time — not separate code paths.
// ============================================================

enum class DataPart { Kanji, Kana, Vocabulary, Strokes, Radicals, Components, Jlpt, Frequency, Furigana, Pitch, Grammar, Examples }

enum class DatabaseProfile(val label: String) {

    Minimal("minimal"),
    Standard("standard"),
    Full("full");

    fun includes(part: DataPart): Boolean = when (this) {
        Minimal -> part in setOf(DataPart.Kanji, DataPart.Kana, DataPart.Strokes, DataPart.Radicals)
        Standard -> part in setOf(
            DataPart.Kanji, DataPart.Kana, DataPart.Vocabulary, DataPart.Strokes,
            DataPart.Radicals, DataPart.Components, DataPart.Jlpt, DataPart.Frequency, DataPart.Furigana
        )
        Full -> true
    }

    val parts: List<DataPart>
        get() = DataPart.entries.filter { includes(it) }

    companion object {
        fun parse(value: String?): DatabaseProfile? =
            entries.firstOrNull { it.label == value?.lowercase() || it.name.lowercase() == value?.lowercase() }
    }
}
