package ua.syt0r.kanji.presentation.screen.main.features

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import ua.syt0r.kanji.core.srs.LetterPracticeType
import ua.syt0r.kanji.core.srs.LetterSrsDeck
import ua.syt0r.kanji.core.srs.LetterSrsDecksData
import ua.syt0r.kanji.core.srs.LetterSrsManager
import ua.syt0r.kanji.core.srs.VocabPracticeType
import ua.syt0r.kanji.core.srs.VocabSrsDeck
import ua.syt0r.kanji.core.srs.VocabSrsDecksData
import ua.syt0r.kanji.core.srs.VocabSrsManager
import ua.syt0r.kanji.presentation.common.ScreenLetterPracticeType
import ua.syt0r.kanji.presentation.common.ScreenVocabPracticeType
import ua.syt0r.kanji.presentation.screen.main.MainDestination
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState
import ua.syt0r.kanji.presentation.screen.main.screen.practice_letter.data.LetterPracticeScreenConfiguration
import ua.syt0r.kanji.presentation.screen.main.screen.practice_vocab.data.VocabPracticeScreenConfiguration

// Per-deck readiness counts shared by every resume surface.
fun LetterSrsDeck.totalNew(): Int = progressMap.values.sumOf { it.dailyNew.size }
fun LetterSrsDeck.totalDue(): Int = progressMap.values.sumOf { it.dailyDue.size }
fun VocabSrsDeck.totalNew(): Int = progressMap.values.sumOf { it.dailyNew.size }
fun VocabSrsDeck.totalDue(): Int = progressMap.values.sumOf { it.dailyDue.size }

// ============================================
// SHARED STUDY-RESUME STATE
// One source of truth for the "continue
// studying" flow, used by both the Library hub
// and the global navigation shell (sidebar +
// floating launchpad). Building the destination
// here — instead of duplicating it in every
// surface — guarantees the resume action
// behaves identically no matter where it is
// triggered from.
// ============================================

/**
 * A live snapshot of every SRS deck (kanji + vocab). Exposed with the same
 * shape in the Library hub and the nav shell so the counts and the resume
 * action can never drift apart.
 */
data class StudyDecksSnapshot(
    val letters: LetterSrsDecksData,
    val vocab: VocabSrsDecksData
) {
    val totalNew: Int
        get() = letters.decks.sumOf { it.totalNew() } + vocab.decks.sumOf { it.totalNew() }

    val totalDue: Int
        get() = letters.decks.sumOf { it.totalDue() } + vocab.decks.sumOf { it.totalDue() }

    val totalReady: Int
        get() = totalNew + totalDue
}

/**
 * Collects the current SRS deck data and refreshes whenever either manager
 * reports a change, so counts shown in the shell stay live while studying.
 */
@Composable
fun rememberStudyDecksSnapshot(): State<StudyDecksSnapshot?> {
    val letterSrsManager = koinInject<LetterSrsManager>()
    val vocabSrsManager = koinInject<VocabSrsManager>()

    return produceState<StudyDecksSnapshot?>(
        initialValue = null,
        letterSrsManager,
        vocabSrsManager
    ) {
        suspend fun reload() {
            value = StudyDecksSnapshot(
                letters = letterSrsManager.getDecks(),
                vocab = vocabSrsManager.getDecks()
            )
        }
        reload()
        launch {
            merge(letterSrsManager.dataChangeFlow, vocabSrsManager.dataChangeFlow)
                .collect { reload() }
        }
    }
}

/**
 * Builds the practice destination for the "continue studying" flow.
 *
 * Priority: kanji writing → kanji reading → vocab flashcards. Returns null
 * when nothing is ready to study (or there are no decks at all), so callers
 * can show a sensible empty state instead of navigating nowhere.
 */
fun buildStudyDestination(snapshot: StudyDecksSnapshot): MainDestination? {
    val letters = snapshot.letters
    val vocab = snapshot.vocab

    if (letters.decks.isNotEmpty()) {
        val cards = buildResumeLetterCards(letters, LetterPracticeType.Writing)
        if (cards.isNotEmpty()) {
            return MainDestination.LetterPractice(
                LetterPracticeScreenConfiguration(
                    cards = cards,
                    practiceType = ScreenLetterPracticeType.Writing
                )
            )
        }
        val readingCards = buildResumeLetterCards(letters, LetterPracticeType.Reading)
        if (readingCards.isNotEmpty()) {
            return MainDestination.LetterPractice(
                LetterPracticeScreenConfiguration(
                    cards = readingCards,
                    practiceType = ScreenLetterPracticeType.Reading
                )
            )
        }
    }

    if (vocab.decks.isNotEmpty()) {
        val cards = buildResumeVocabCards(vocab, VocabPracticeType.Flashcard)
        if (cards.isNotEmpty()) {
            return MainDestination.VocabPractice(
                VocabPracticeScreenConfiguration(
                    cards = cards,
                    practiceType = ScreenVocabPracticeType.Flashcard
                )
            )
        }
    }

    return null
}

/** Resumes studying from any surface (Library card, sidebar, launchpad). */
fun resumeStudy(
    navigationState: MainNavigationState,
    snapshot: StudyDecksSnapshot?
) {
    if (snapshot == null) return
    buildStudyDestination(snapshot)?.let { navigationState.navigate(it) }
}

private fun buildResumeLetterCards(
    decksData: LetterSrsDecksData,
    practiceType: LetterPracticeType
): List<LetterPracticeScreenConfiguration.Card> {
    if (decksData.decks.isEmpty()) return emptyList()
    val dailyNew = mutableMapOf<String, Long>()
    val dailyDue = mutableMapOf<String, Long>()
    decksData.decks.forEach { deck ->
        val progress = deck.progressMap.getValue(practiceType)
        progress.dailyNew.forEach { dailyNew[it] = deck.id }
        progress.dailyDue.forEach { dailyDue[it] = deck.id }
    }
    val leftover = decksData.dailyProgress.leftoversByPracticeTypeMap.getValue(practiceType)
    val newCards = dailyNew.toList().take(leftover.new).map { (letter, deckId) ->
        LetterPracticeScreenConfiguration.Card(letter, deckId)
    }
    val dueCards = dailyDue.toList().take(leftover.due).map { (letter, deckId) ->
        LetterPracticeScreenConfiguration.Card(letter, deckId)
    }
    return newCards + dueCards
}

private fun buildResumeVocabCards(
    decksData: VocabSrsDecksData,
    practiceType: VocabPracticeType
): List<VocabPracticeScreenConfiguration.Card> {
    if (decksData.decks.isEmpty()) return emptyList()
    val dailyNew = mutableMapOf<Long, Long>()
    val dailyDue = mutableMapOf<Long, Long>()
    decksData.decks.forEach { deck ->
        val progress = deck.progressMap.getValue(practiceType)
        progress.dailyNew.forEach { dailyNew[it] = deck.id }
        progress.dailyDue.forEach { dailyDue[it] = deck.id }
    }
    val leftover = decksData.dailyProgress.leftoversByPracticeTypeMap.getValue(practiceType)
    val newCards = dailyNew.toList().take(leftover.new).map { (cardId, deckId) ->
        VocabPracticeScreenConfiguration.Card(cardId, deckId)
    }
    val dueCards = dailyDue.toList().take(leftover.due).map { (cardId, deckId) ->
        VocabPracticeScreenConfiguration.Card(cardId, deckId)
    }
    return newCards + dueCards
}
