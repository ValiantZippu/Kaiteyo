package ua.syt0r.kanji.presentation.screen.main.screen.info

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import ua.syt0r.kanji.core.srs.VocabPracticeType
import ua.syt0r.kanji.core.user_data.database.CardDatabaseManager
import ua.syt0r.kanji.core.user_data.database.LetterPracticeRepository
import ua.syt0r.kanji.core.user_data.database.VocabCardData
import ua.syt0r.kanji.core.user_data.database.VocabPracticeRepository
import ua.syt0r.kanji.presentation.common.ScreenLetterPracticeType
import ua.syt0r.kanji.presentation.common.ScreenVocabPracticeType
import ua.syt0r.kanji.presentation.getMultiplatformViewModel
import ua.syt0r.kanji.presentation.screen.main.MainDestination
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState
import ua.syt0r.kanji.presentation.screen.main.features.LETTER_WRITING_PRACTICE_TYPE
import ua.syt0r.kanji.presentation.screen.main.features.SUSPENDED_FLAG_TYPE
import ua.syt0r.kanji.presentation.screen.main.screen.info.ui.DeckMembershipDialog
import ua.syt0r.kanji.presentation.screen.main.screen.info.ui.LearningAction
import ua.syt0r.kanji.presentation.screen.main.screen.info.ui.NoteDialog
import ua.syt0r.kanji.presentation.screen.main.screen.info.use_case.InfoLoadLearningStateUseCase
import ua.syt0r.kanji.presentation.screen.main.screen.info.use_case.ItemLearningState
import ua.syt0r.kanji.presentation.screen.main.screen.practice_letter.data.LetterPracticeScreenConfiguration
import ua.syt0r.kanji.presentation.screen.main.screen.practice_vocab.data.VocabPracticeScreenConfiguration


@Composable
fun InfoScreen(
    screenData: InfoScreenData,
    mainNavigationState: MainNavigationState,
    viewModel: InfoScreenContract.ViewModel = getMultiplatformViewModel(screenData)
) {

    val scope = rememberCoroutineScope()
    val loadLearningState = koinInject<InfoLoadLearningStateUseCase>()
    val letterPracticeRepository = koinInject<LetterPracticeRepository>()
    val vocabPracticeRepository = koinInject<VocabPracticeRepository>()
    val cardDatabaseManager = koinInject<CardDatabaseManager>()

    var learningState by remember { mutableStateOf<ItemLearningState?>(null) }
    var showDeckDialog by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(screenData) {
        learningState = when (screenData) {
            is InfoScreenData.Letter -> loadLearningState.loadLetter(screenData.letter)

            is InfoScreenData.Vocab -> {
                val id = screenData.id
                if (id == null) {
                    null
                } else {
                    loadLearningState.loadVocab(
                        wordId = id,
                        kanjiReading = screenData.kanjiReading,
                        kanaReading = screenData.kanaReading.orEmpty()
                    )
                }
            }
        }
    }

    val state = learningState

    val actions: List<LearningAction> = remember(state, screenData) {
        val current = state ?: return@remember emptyList()

        fun practiceDeckId(): Long = current.decks.firstOrNull { it.contains }?.deckId ?: 0L

        val practiceActions = when (screenData) {
            is InfoScreenData.Letter -> {
                val character = screenData.letter
                val deckId = practiceDeckId()
                listOf(
                    LearningAction("Practice writing") {
                        mainNavigationState.navigate(
                            MainDestination.LetterPractice(
                                LetterPracticeScreenConfiguration(
                                    practiceType = ScreenLetterPracticeType.Writing,
                                    cards = listOf(
                                        LetterPracticeScreenConfiguration.Card(
                                            letter = character,
                                            deckId = deckId
                                        )
                                    )
                                )
                            )
                        )
                    },
                    LearningAction("Practice reading") {
                        mainNavigationState.navigate(
                            MainDestination.LetterPractice(
                                LetterPracticeScreenConfiguration(
                                    practiceType = ScreenLetterPracticeType.Reading,
                                    cards = listOf(
                                        LetterPracticeScreenConfiguration.Card(
                                            letter = character,
                                            deckId = deckId
                                        )
                                    )
                                )
                            )
                        )
                    }
                )
            }

            is InfoScreenData.Vocab -> {
                val id = screenData.id ?: return@remember emptyList()
                val deckId = practiceDeckId()
                listOf(
                    LearningAction("Flashcard") {
                        mainNavigationState.navigate(
                            MainDestination.VocabPractice(
                                VocabPracticeScreenConfiguration(
                                    practiceType = ScreenVocabPracticeType.Flashcard,
                                    cards = listOf(
                                        VocabPracticeScreenConfiguration.Card(cardId = id, deckId = deckId)
                                    )
                                )
                            )
                        )
                    },
                    LearningAction("Writing") {
                        mainNavigationState.navigate(
                            MainDestination.VocabPractice(
                                VocabPracticeScreenConfiguration(
                                    practiceType = ScreenVocabPracticeType.Writing,
                                    cards = listOf(
                                        VocabPracticeScreenConfiguration.Card(cardId = id, deckId = deckId)
                                    )
                                )
                            )
                        )
                    },
                    LearningAction("Reading") {
                        mainNavigationState.navigate(
                            MainDestination.VocabPractice(
                                VocabPracticeScreenConfiguration(
                                    practiceType = ScreenVocabPracticeType.ReadingPicker,
                                    cards = listOf(
                                        VocabPracticeScreenConfiguration.Card(cardId = id, deckId = deckId)
                                    )
                                )
                            )
                        )
                    }
                )
            }
        }

        val managementActions = listOf(
            LearningAction("Add to deck") { showDeckDialog = true },
            LearningAction(if (current.isSuspended) "Unsuspend" else "Suspend") {
                scope.launch {
                    val (key, practiceType) = when (screenData) {
                        is InfoScreenData.Letter ->
                            screenData.letter to LETTER_WRITING_PRACTICE_TYPE

                        is InfoScreenData.Vocab ->
                            (screenData.id ?: return@launch).toString() to
                                VocabPracticeType.Flashcard.srsPracticeType.value
                    }
                    if (current.isSuspended) {
                        cardDatabaseManager.removeFlag(key, practiceType)
                        learningState = learningState?.copy(isSuspended = false)
                    } else {
                        cardDatabaseManager.setFlag(key, practiceType, SUSPENDED_FLAG_TYPE)
                        learningState = learningState?.copy(isSuspended = true)
                    }
                }
            },
            LearningAction("Note") { showNoteDialog = true }
        )

        practiceActions + managementActions
    }

    if (showDeckDialog && state != null) {
        DeckMembershipDialog(
            decks = state.decks,
            onDismiss = { showDeckDialog = false },
            onToggle = { deck, add ->
                scope.launch {
                    when (screenData) {
                        is InfoScreenData.Letter -> {
                            letterPracticeRepository.updateDeck(
                                id = deck.deckId,
                                title = deck.title,
                                charactersToAdd = if (add) listOf(screenData.letter) else emptyList(),
                                charactersToRemove = if (add) emptyList() else listOf(screenData.letter)
                            )
                        }

                        is InfoScreenData.Vocab -> {
                            if (add) {
                                vocabPracticeRepository.addCard(
                                    deckId = deck.deckId,
                                    data = VocabCardData(
                                        kanjiReading = screenData.kanjiReading,
                                        kanaReading = screenData.kanaReading.orEmpty(),
                                        meaning = null,
                                        dictionaryId = screenData.id
                                    )
                                )
                            } else {
                                // Remove: find the saved card id for this word in the deck.
                                val cardId = vocabPracticeRepository.getCardIdList(deck.deckId)
                                    .firstOrNull { id ->
                                        vocabPracticeRepository.getAllCards()
                                            .firstOrNull { it.cardId == id }
                                            ?.data
                                            ?.let { it.kanaReading == screenData.kanaReading }
                                            ?: false
                                    }
                                if (cardId != null) vocabPracticeRepository.deleteCard(cardId)
                            }
                        }
                    }
                    val refreshed = when (screenData) {
                        is InfoScreenData.Letter ->
                            loadLearningState.loadLetter(screenData.letter)

                        is InfoScreenData.Vocab -> {
                            val id = screenData.id ?: return@launch
                            loadLearningState.loadVocab(id, screenData.kanjiReading, screenData.kanaReading.orEmpty())
                        }
                    }
                    learningState = refreshed
                }
            }
        )
    }

    if (showNoteDialog && state != null) {
        NoteDialog(
            initialNote = state.note,
            onDismiss = { showNoteDialog = false },
            onSave = { content ->
                scope.launch {
                    val (key, practiceType) = when (screenData) {
                        is InfoScreenData.Letter ->
                            screenData.letter to LETTER_WRITING_PRACTICE_TYPE

                        is InfoScreenData.Vocab ->
                            (screenData.id ?: return@launch).toString() to
                                VocabPracticeType.Flashcard.srsPracticeType.value
                    }
                    if (content.isBlank()) {
                        cardDatabaseManager.deleteNote(key, practiceType)
                    } else {
                        cardDatabaseManager.setNote(key, practiceType, content, 0)
                    }
                    learningState = learningState?.copy(note = content)
                }
            }
        )
    }

    InfoScreenUI(
        state = viewModel.state,
        learningState = state,
        learningActions = actions,
        onUpButtonClick = { mainNavigationState.navigateBack() },
        onLetterClick = {
            val nextScreenData = InfoScreenData.Letter(it)
            if (screenData != nextScreenData)
                mainNavigationState.navigate(MainDestination.Info(nextScreenData))
        },
        onWordClick = {
            val nextScreenData = it.toInfoScreenData()
            if (screenData != nextScreenData)
                mainNavigationState.navigate(MainDestination.Info(nextScreenData))
        },
        onPlayReading = { reading -> viewModel.speakReading(reading) },
        isPlayingReading = viewModel.playingReading.value,
        onSaveUserNote = { content ->
            scope.launch {
                val (key, practiceType) = when (screenData) {
                    is InfoScreenData.Letter ->
                        screenData.letter to LETTER_WRITING_PRACTICE_TYPE

                    is InfoScreenData.Vocab ->
                        (screenData.id ?: return@launch).toString() to
                            VocabPracticeType.Flashcard.srsPracticeType.value
                }
                if (content.isBlank()) {
                    cardDatabaseManager.deleteNote(key, practiceType)
                } else {
                    cardDatabaseManager.setNote(key, practiceType, content, 0)
                }
                learningState = learningState?.copy(note = content)
            }
        }
    )

}
