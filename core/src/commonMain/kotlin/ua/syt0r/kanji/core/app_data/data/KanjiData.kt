package ua.syt0r.kanji.core.app_data.data

class KanjiData(
    val kanji: String,
    val frequency: Int?,
    val variantFamily: String?
)

data class KanjiListEntry(
    val kanji: String,
    val frequency: Int?
)

data class KanjiMeaningEntry(
    val kanji: String,
    val meaning: String
)

data class KanjiReadingEntry(
    val kanji: String,
    val readingType: String,
    val reading: String
)

data class KanjiClassificationEntry(
    val kanji: String,
    val classification: String
)
