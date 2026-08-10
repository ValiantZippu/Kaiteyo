package ua.syt0r.kanji.core.transfer

import ua.syt0r.kanji.presentation.screen.main.screen.decks.CardStatus
import ua.syt0r.kanji.presentation.screen.main.screen.decks.KaiteyoCard
import kotlin.Result

actual class AnkiPackage {

    actual companion object {
        actual val EXTENSION = "apkg"
    }

    actual fun write(cards: List<KaiteyoCard>, deckName: String): Result<ByteArray> {
        return Result.failure(UnsupportedOperationException("APKG export not implemented on iOS yet"))
    }

    actual fun read(bytes: ByteArray): Result<List<KaiteyoCard>> {
        return Result.failure(UnsupportedOperationException("APKG import not implemented on iOS yet"))
    }

    actual fun ankiType(status: CardStatus): Int = when (status) {
        CardStatus.New -> 0
        CardStatus.Learning, CardStatus.Relearning -> 1
        else -> 2
    }

    actual fun ankiQueue(status: CardStatus): Int = when (status) {
        CardStatus.New -> 0
        CardStatus.Learning -> 1
        CardStatus.Young, CardStatus.Mature -> 2
        CardStatus.Relearning -> 3
        CardStatus.Suspended -> -1
        CardStatus.Buried -> -3
        else -> 2
    }

    actual fun statusFromAnki(type: Int, queue: Int): CardStatus = when {
        queue == -1 -> CardStatus.Suspended
        queue <= -2 -> CardStatus.Buried
        type == 0 || queue == 0 -> CardStatus.New
        type == 1 || queue == 1 -> CardStatus.Learning
        queue == 3 -> CardStatus.Relearning
        else -> CardStatus.Young
    }

    actual fun cardGuid(card: KaiteyoCard): String {
        val alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        var value = card.id.hashCode().toLong() and 0x7fffffffL
        val sb = StringBuilder()
        repeat(10) {
            sb.append(alphabet[(value % alphabet.length).toInt()])
            value /= alphabet.length
        }
        return sb.toString()
    }

    actual fun checksum(text: String): Long {
        var sum = 0L
        text.toByteArray(Charsets.UTF_8).forEach { sum += it.toLong() and 0xff }
        return sum and 0xffffffffL
    }

    actual fun ankiDue(card: KaiteyoCard, nowMs: Long): Long {
        if (card.status == CardStatus.New) return 0L
        return card.interval.toLong()
    }
}