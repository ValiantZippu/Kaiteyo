package ua.syt0r.kanji.core.transfer

import ua.syt0r.kanji.presentation.screen.main.screen.decks.CardStatus
import ua.syt0r.kanji.presentation.screen.main.screen.decks.KaiteyoCard
import kotlin.Result

// ============================================
// ANKI PACKAGE (.apkg) - COMMON INTERFACE
// Platform-specific implementations in jvmMain/iosMain/androidMain
// ============================================

expect class AnkiPackage {

    companion object {
        val EXTENSION: String
    }

    /** Export cards to an Anki package format. Returns Result<ByteArray>. */
    fun write(cards: List<KaiteyoCard>, deckName: String = "Kaiteyo"): Result<ByteArray>

    /** Import cards from an Anki package. Returns Result<List<KaiteyoCard>>. */
    fun read(bytes: ByteArray): Result<List<KaiteyoCard>>

    /** Map KaiteyoCard status to Anki card type (0=new, 1=learning, 2=review). */
    fun ankiType(status: CardStatus): Int

    /** Map KaiteyoCard status to Anki queue. */
    fun ankiQueue(status: CardStatus): Int

    /** Map Anki type/queue back to CardStatus. */
    fun statusFromAnki(type: Int, queue: Int): CardStatus

    /** Generate a deterministic Anki-style GUID from card id. */
    fun cardGuid(card: KaiteyoCard): String

    /** Anki's duplicate-detection checksum: sum of front bytes, 32-bit masked. */
    fun checksum(text: String): Long

    /** Convert interval days to Anki due (days relative to collection clock). */
    fun ankiDue(card: KaiteyoCard, nowMs: Long): Long
}