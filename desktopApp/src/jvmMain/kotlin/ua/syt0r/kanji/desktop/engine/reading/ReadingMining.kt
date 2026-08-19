package ua.syt0r.kanji.desktop.engine.reading

import ua.syt0r.kanji.desktop.engine.mining.MiningEngine
import ua.syt0r.kanji.desktop.engine.mining.MiningPayload

// ============================================
// READING — MINING BRIDGE
// Phrase-level mining (KT-MINE-003): a whole
// sentence mined from the reader becomes one
// card whose headword is the phrase and whose
// note carries the full sentence context, so
// the review card stands on its own.
// ============================================

/**
 * Mine a full sentence as a single phrase card. The headword is the trimmed
 * sentence (capped so card fronts stay readable); the sentence context and
 * the document title are preserved in the payload. Tagged `phrase` so the
 * card pool and statistics can distinguish phrase cards from word cards.
 */
fun MiningEngine.minePhraseSentence(
    phrase: String,
    sentence: String,
    documentTitle: String
): ua.syt0r.kanji.desktop.model.DesktopCard? {
    val trimmed = phrase.trim()
    if (trimmed.isBlank()) return null
    val headword = trimmed.take(60)
    return mine(
        MiningPayload(
            headword = headword,
            sentence = sentence,
            source = "reader",
            sourceDetail = documentTitle,
            definition = "Phrase mined from reading “$documentTitle”",
            tags = listOf("source:reader", "phrase", "sentence"),
            example = sentence
        )
    )
}
