package ua.syt0r.kanji.desktop.engine.graph

import ua.syt0r.kanji.desktop.model.DesktopCard

// ============================================
// KAITEYO MEDIA EXPOSURE INDEX
// "Where have I seen this?" — a read-model over
// mined cards. Cards mined from subtitles carry
// real provenance (tags `source:subtitle` +
// `Source:`/`Timestamp:` note lines), so every
// appearance below is real data, never guessed.
// ============================================

object MediaExposureIndex {

    /**
     * Media appearances for an expression. Exact word cards match directly;
     * a kanji matches every mined word that contains it (kanji exposure).
     */
    fun appearancesFor(expression: String, cards: List<DesktopCard>): List<MediaAppearance> =
        cards.asSequence()
            .filter { it.character.contains(expression) }
            .filter { card -> card.tags.any { it.startsWith("source:") && it != "source:reader" } }
            .mapNotNull(::appearanceFrom)
            .sortedByDescending { it.timestamp ?: Double.MIN_VALUE }
            .toList()

    /** All media titles the user has mined from, with occurrence counts. */
    fun mediaTitles(cards: List<DesktopCard>): List<Pair<String, Int>> =
        cards.asSequence()
            .mapNotNull(::appearanceFrom)
            .groupBy { it.mediaTitle }
            .map { (title, list) -> title to list.size }
            .sortedByDescending { it.second }
            .toList()

    private fun appearanceFrom(card: DesktopCard): MediaAppearance? {
        val mediaTitle = card.note
            .lineSequence()
            .firstOrNull { it.startsWith("Source:") }
            ?.removePrefix("Source:")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: return null

        val timestamp = card.note
            .lineSequence()
            .firstOrNull { it.startsWith("Timestamp:") }
            ?.removePrefix("Timestamp:")
            ?.trim()
            ?.toDoubleOrNull()

        val source = card.tags
            .firstOrNull { it.startsWith("source:") }
            ?.removePrefix("source:")
            ?: "media"

        return MediaAppearance(
            mediaTitle = mediaTitle,
            timestamp = timestamp,
            cardId = card.id,
            source = source
        )
    }
}
