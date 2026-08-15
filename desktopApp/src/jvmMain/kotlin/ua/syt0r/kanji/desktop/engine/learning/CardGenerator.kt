package ua.syt0r.kanji.desktop.engine.learning

import kotlinx.datetime.Clock

// ============================================
// CARD GENERATOR
// Deterministic note → card generation.
//
//   note + deck config → the exact card types
//
// Given the same note and the same deck config
// the generator produces the same card ids, so
// regeneration never duplicates cards and never
// resets SRS state (existing cards are updated in
// place, fresh cards are created as New).
// ============================================

object CardGenerator {

    /**
     * Generate (or refresh) the cards a note produces in a deck, according
     * to the deck's enabled card types. Returns the full set of cards for
     * the note+deck pair so callers can replace the old set atomically.
     *
     * @param existing the cards already present for this note+deck (kept with
     *                 their SRS state; new card types start as New).
     */
    fun generateForDeck(
        note: LearningNote,
        deckId: String,
        config: DeckStudyConfig,
        existing: List<NoteCard> = emptyList(),
        now: kotlinx.datetime.Instant = Clock.System.now()
    ): List<NoteCard> {
        val existingById = existing.associateBy { it.id }
        val cardTypes = config.cardTypesFor(note.kind)
        return cardTypes.map { cardType ->
            val id = LearningIds.cardId(note.id, cardType, deckId)
            existingById[id] ?: NoteCard(
                id = id,
                noteId = note.id,
                cardType = cardType,
                deckId = deckId,
                status = ua.syt0r.kanji.desktop.model.SrsStatus.New,
                createdAt = now
            )
        }
    }

    /** Generate cards for many notes at once (bulk deck population). */
    fun generateBatch(
        notes: List<LearningNote>,
        deckId: String,
        config: DeckStudyConfig,
        existing: List<NoteCard> = emptyList(),
        now: kotlinx.datetime.Instant = Clock.System.now()
    ): List<NoteCard> {
        val existingByNote = existing.groupBy { it.noteId }
        return notes.flatMap { note ->
            generateForDeck(note, deckId, config, existingByNote[note.id].orEmpty(), now)
        }
    }

    /**
     * The set of card types a note would generate with a config — used by the
     * deck editor to preview before saving.
     */
    fun previewCardTypes(noteKind: LearningItemKind, config: DeckStudyConfig): List<CardType> =
        config.cardTypesFor(noteKind)
}
