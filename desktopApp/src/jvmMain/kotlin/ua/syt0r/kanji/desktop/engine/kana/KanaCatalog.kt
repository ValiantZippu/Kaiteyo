package ua.syt0r.kanji.desktop.engine.kana

import kotlinx.datetime.Clock
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.engine.history.ActivityCategory
import ua.syt0r.kanji.desktop.model.ContentKind
import ua.syt0r.kanji.desktop.model.DeckDef
import ua.syt0r.kanji.desktop.model.DesktopCard

// ============================================
// KANA CATALOG
// Turns the structured kana dataset into real
// study content. One canonical card per kana
// record; premade decks reference those cards by
// id (multi-deck membership), so a single か card
// belongs to Hiragana, Full Kana and Mixed Kana
// without ever being duplicated.
// ============================================

/** Stable card id for a kana character. */
fun kanaCardId(character: String): String = "kana-$character"

/** The deck that owns a kana card's primary membership. */
fun primaryKanaDeckId(record: KanaChar): String = when (record.category) {
    KanaCategory.Base -> if (record.script == KanaScript.Hiragana) DECK_HIRAGANA else DECK_KATAKANA
    KanaCategory.Dakuten, KanaCategory.Handakuten, KanaCategory.YoOn ->
        if (record.script == KanaScript.Hiragana) DECK_HIRAGANA_ADVANCED else DECK_KATAKANA_ADVANCED
    KanaCategory.Extended -> DECK_EXTENDED
}

const val FOLDER_KANA = "kana-folder"
const val DECK_HIRAGANA = "kana-hiragana"
const val DECK_KATAKANA = "kana-katakana"
const val DECK_HIRAGANA_ADVANCED = "kana-hiragana-advanced"
const val DECK_KATAKANA_ADVANCED = "kana-katakana-advanced"
const val DECK_EXTENDED = "kana-extended"
const val DECK_FULL = "kana-full"
const val DECK_MIXED = "kana-mixed"

/** All kana decks (children of the Kana folder) in a stable order. */
val kanaDecks: List<DeckDef> by lazy {
    val now = Clock.System.now()
    val fullIds = kanaCatalog.map { kanaCardId(it.character) }
    // Mixed Kana: an interleaved sample of the base syllabary (both scripts).
    val mixedIds = kanaFor(KanaScript.Hiragana).map { kanaCardId(it.character) } +
        kanaFor(KanaScript.Katakana).map { kanaCardId(it.character) }

    fun deck(id: String, name: String, description: String, icon: String, cardIds: List<String>): DeckDef =
        DeckDef(
            id = id,
            name = name,
            description = description,
            kind = ContentKind.Kana,
            builtIn = true,
            parentId = FOLDER_KANA,
            difficulty = 1,
            tags = listOf("kana", "builtin"),
            source = "builtin",
            icon = icon,
            cardIds = cardIds,
            createdAt = now
        )

    listOf(
        deck(DECK_HIRAGANA, "Hiragana", "The 46 basic hiragana — gojūon あ〜ん", "あ", kanaFor(KanaScript.Hiragana, KanaCategory.Base).map { kanaCardId(it.character) }),
        deck(DECK_KATAKANA, "Katakana", "The 46 basic katakana — gojūon ア〜ン", "ア", kanaFor(KanaScript.Katakana, KanaCategory.Base).map { kanaCardId(it.character) }),
        deck(DECK_HIRAGANA_ADVANCED, "Hiragana + Dakuten", "Voiced (が〜ぼ), semi-voiced (ぱ〜ぽ) and yōon (きゃ〜ぴょ)", "が", kanaFor(KanaScript.Hiragana, KanaCategory.Dakuten).map { kanaCardId(it.character) } +
            kanaFor(KanaScript.Hiragana, KanaCategory.Handakuten).map { kanaCardId(it.character) } +
            kanaFor(KanaScript.Hiragana, KanaCategory.YoOn).map { kanaCardId(it.character) }),
        deck(DECK_KATAKANA_ADVANCED, "Katakana + Dakuten", "Voiced (ガ〜ボ), semi-voiced (パ〜ポ) and yōon (キャ〜ピョ)", "ガ", kanaFor(KanaScript.Katakana, KanaCategory.Dakuten).map { kanaCardId(it.character) } +
            kanaFor(KanaScript.Katakana, KanaCategory.Handakuten).map { kanaCardId(it.character) } +
            kanaFor(KanaScript.Katakana, KanaCategory.YoOn).map { kanaCardId(it.character) }),
        deck(DECK_EXTENDED, "Extended Katakana", "Loanword transcription clusters — ティ, ファ, ウィ, ヴ series…", "ヴ", kanaFor(KanaScript.Katakana, KanaCategory.Extended).map { kanaCardId(it.character) }),
        deck(DECK_FULL, "Full Kana", "Every kana Kaiteyo knows — the complete syllabary", "ん", fullIds),
        deck(DECK_MIXED, "Mixed Kana", "Both scripts interleaved for fast recognition", "あ", mixedIds)
    )
}

/** The Kana folder deck that groups every premade kana deck. */
val kanaFolderDeck: DeckDef by lazy {
    DeckDef(
        id = FOLDER_KANA,
        name = "Kana",
        description = "Hiragana, katakana, dakuten, combinations and extended katakana",
        kind = ContentKind.Kana,
        builtIn = true,
        difficulty = 1,
        tags = listOf("kana", "builtin"),
        source = "builtin",
        icon = "あ",
        createdAt = Clock.System.now()
    )
}

/** Build one canonical card per kana record. SRS state starts New — earned, never seeded. */
fun buildKanaCards(): List<DesktopCard> = kanaCatalog.map { record ->
    DesktopCard(
        id = kanaCardId(record.character),
        character = record.character,
        meaning = record.meaning,
        onReadings = listOf(record.romanization),
        kunReadings = emptyList(),
        strokeCount = record.strokeCount,
        tags = record.deckTags,
        contentKind = ContentKind.Kana,
        deckId = primaryKanaDeckId(record),
        createdAt = Clock.System.now()
    )
}

/**
 * Idempotently seed the kana system: the folder, the seven premade decks and
 * every kana card. Runs on every launch (safe: it only creates what is
 * missing and never touches existing cards), so both new users and upgraded
 * installs get the full syllabary without duplicating anything.
 */
fun seedKanaInto(state: AppState) {
    val existingCardIds = state.cards.mapTo(HashSet()) { it.id }

    // 1. Cards — canonical, one per kana, added only when absent.
    val cardsToAdd = buildKanaCards().filter { it.id !in existingCardIds }
    if (cardsToAdd.isNotEmpty()) {
        state.cards.addAll(cardsToAdd)
    }

    // 2. Decks — the folder plus the premade decks, created only when missing.
    val existingDeckIds = state.library.decks.mapTo(HashSet()) { it.id }
    if (FOLDER_KANA !in existingDeckIds) {
        state.library.decks.add(kanaFolderDeck)
        state.library.decks.addAll(kanaDecks)
        state.library.saveDecks()
    } else if (existingDeckIds.none { it.startsWith("kana-") }) {
        // Folder exists but no kana decks (partial state) — add the decks.
        state.library.decks.addAll(kanaDecks)
        state.library.saveDecks()
    }

    if (cardsToAdd.isNotEmpty()) {
        state.library.saveCards(state.cards.toList())
        state.activityLog.record(
            ActivityCategory.Study,
            "Kana syllabary ready (${cardsToAdd.size} kana, ${kanaDecks.size} decks)"
        )
    }
}
