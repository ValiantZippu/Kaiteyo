package ua.syt0r.kanji.presentation.screen.main.screen.info.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.koin.compose.koinInject
import ua.syt0r.kanji.core.app_data.AppDataRepository
import ua.syt0r.kanji.core.app_data.Sentence
import ua.syt0r.kanji.core.app_data.data.JapaneseWord
import ua.syt0r.kanji.presentation.common.ExtraListSpacerState
import ua.syt0r.kanji.presentation.common.ExtraSpacer
import ua.syt0r.kanji.presentation.common.PaginationLoadLaunchedEffect
import ua.syt0r.kanji.presentation.common.collectAsState
import ua.syt0r.kanji.presentation.common.trackList
import ua.syt0r.kanji.presentation.common.ui.LocalOrientation
import ua.syt0r.kanji.presentation.common.ui.Orientation
import ua.syt0r.kanji.presentation.common.ui.kaiteyo.KaiteyoReadingsCard
import ua.syt0r.kanji.presentation.dialog.SaveWordDialog
import ua.syt0r.kanji.presentation.screen.main.screen.info.InfoScreenContract
import ua.syt0r.kanji.presentation.screen.main.screen.info.LetterInfoData
import ua.syt0r.kanji.presentation.screen.main.screen.info.infoScreenExpandableSentenceSection
import ua.syt0r.kanji.presentation.screen.main.screen.info.infoScreenExpandableVocabSection
import ua.syt0r.kanji.presentation.screen.main.screen.info.ui.LearningAction
import ua.syt0r.kanji.presentation.screen.main.screen.info.use_case.ItemLearningState

@Composable
fun LetterInfoUI(
    letterData: LetterInfoData,
    listState: LazyListState,
    listSpacerState: ExtraListSpacerState,
    learningState: ItemLearningState?,
    learningActions: List<LearningAction>,
    onFuriganaClick: (String) -> Unit,
    onWordClick: (JapaneseWord) -> Unit,
    onPlayReading: ((String) -> Unit)? = null,
    isPlayingReading: String? = null,
    onSaveUserNote: ((String) -> Unit)? = null
) {
    var wordToAddToDeck by remember { mutableStateOf<JapaneseWord?>(null) }
    wordToAddToDeck?.let {
        SaveWordDialog(
            word = it,
            onDismissRequest = { wordToAddToDeck = null }
        )
    }

    if (letterData is LetterInfoData.Kanji) {
        KanjiInfoLayout(
            letterData = letterData,
            listState = listState,
            listSpacerState = listSpacerState,
            learningState = learningState,
            learningActions = learningActions,
            onFuriganaClick = onFuriganaClick,
            onWordClick = onWordClick,
            onPlayReading = onPlayReading,
            isPlayingReading = isPlayingReading,
            onSaveUserNote = onSaveUserNote
        )
    } else if (letterData is LetterInfoData.Kana) {
        KanaInfoLayout(
            letterData = letterData,
            listState = listState,
            listSpacerState = listSpacerState,
            learningState = learningState,
            learningActions = learningActions,
            onFuriganaClick = onFuriganaClick,
            onWordClick = onWordClick,
            onAddWordToDeck = { wordToAddToDeck = it }
        )
    }
}

/**
 * Kanji details: a clean two-column layout. The right column carries the
 * unified readings → vocabulary explorer (with inline example sentences),
 * replacing the old separate vocab + sentence side panels.
 */
@Composable
private fun KanjiInfoLayout(
    letterData: LetterInfoData.Kanji,
    listState: LazyListState,
    listSpacerState: ExtraListSpacerState,
    learningState: ItemLearningState?,
    learningActions: List<LearningAction>,
    onFuriganaClick: (String) -> Unit,
    onWordClick: (JapaneseWord) -> Unit,
    onPlayReading: ((String) -> Unit)?,
    isPlayingReading: String?,
    onSaveUserNote: ((String) -> Unit)?
) {
    val appDataRepository = koinInject<AppDataRepository>()
    val sentenceProvider: suspend (JapaneseWord) -> List<Sentence> =
        remember(appDataRepository) {
            suspend { word: JapaneseWord ->
                val query = word.reading.kanjiReading ?: word.reading.kanaReading
                runCatching {
                    appDataRepository.getSentencesWithText(query, offset = 0, limit = 5)
                }.getOrDefault(emptyList())
            }
        }

    val vocab = letterData.vocab.collectAsState()
    val sentences = letterData.sentences.collectAsState()

    LazyColumn(
        state = listState,
        modifier = Modifier.trackList(listSpacerState)
    ) {
        item(key = "kanji-heading") {
            LetterInfoKanjiHeading(
                data = letterData,
                onRadicalClick = onFuriganaClick,
                onPlayReading = onPlayReading,
                isPlayingReading = isPlayingReading,
                userNote = learningState?.note,
                onSaveUserNote = onSaveUserNote,
                onWordClick = onWordClick,
                statusContent = if (learningState != null) {
                    { LearningStatusSection(state = learningState, actions = learningActions) }
                } else null,
                readingsVocabContent = {
                    KaiteyoReadingsCard(
                        character = letterData.character,
                        on = letterData.on,
                        kun = letterData.kun,
                        vocab = vocab.list,
                        sentences = sentences.list,
                        totalVocab = vocab.total,
                        onPlayReading = onPlayReading,
                        isPlayingReading = isPlayingReading,
                        onWordClick = onWordClick,
                        onFuriganaClick = onFuriganaClick,
                        canLoadMoreVocab = vocab.canLoadMore,
                        onLoadMoreVocab = { vocab.loadMore() },
                        sentenceProvider = sentenceProvider
                    )
                }
            )
        }

        listSpacerState.ExtraSpacer(this)
    }
}

/** Kana details keep the classic expandable vocab + sentence sections. */
@Composable
private fun KanaInfoLayout(
    letterData: LetterInfoData.Kana,
    listState: LazyListState,
    listSpacerState: ExtraListSpacerState,
    learningState: ItemLearningState?,
    learningActions: List<LearningAction>,
    onFuriganaClick: (String) -> Unit,
    onWordClick: (JapaneseWord) -> Unit,
    onAddWordToDeck: (JapaneseWord) -> Unit
) {
    val vocabExpanded = rememberSaveable { mutableStateOf(true) }
    val sentencesExpanded = rememberSaveable { mutableStateOf(true) }

    val paginateableData = listOf(
        letterData.vocab to vocabExpanded,
        letterData.sentences to sentencesExpanded
    )

    PaginationLoadLaunchedEffect(
        listState = listState,
        prefetchDistance = InfoScreenContract.ListPrefetchDistance,
        paginateableToExpandedStateList = paginateableData
    )

    val vocab = letterData.vocab.collectAsState()
    val sentences = letterData.sentences.collectAsState()

    if (LocalOrientation.current == Orientation.Portrait) {
        LazyColumn(
            state = listState,
            modifier = Modifier.trackList(listSpacerState)
        ) {
            item(key = "kana-heading") {
                LetterInfoKanaHeading(data = letterData)
            }

            if (learningState != null) {
                item(key = "learning-status") {
                    LearningStatusSection(
                        state = learningState,
                        actions = learningActions
                    )
                }
            }

            infoScreenExpandableVocabSection(
                expanded = vocabExpanded,
                paginateable = vocab,
                onWordClick = onWordClick,
                onFuriganaClick = onFuriganaClick,
                addWordToVocabDeckClick = onAddWordToDeck
            )

            infoScreenExpandableSentenceSection(
                expanded = sentencesExpanded,
                paginateable = sentences,
                onFuriganaClick = onFuriganaClick
            )

            listSpacerState.ExtraSpacer(this)
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .trackList(listSpacerState)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1.15f)
                    .fillMaxHeight()
            ) {
                item(key = "kana-heading") {
                    LetterInfoKanaHeading(data = letterData)
                }
                listSpacerState.ExtraSpacer(this)
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(0.85f)
                    .fillMaxHeight()
            ) {
                if (learningState != null) {
                    item(key = "learning-status") {
                        LearningStatusSection(
                            state = learningState,
                            actions = learningActions
                        )
                    }
                }

                infoScreenExpandableVocabSection(
                    expanded = vocabExpanded,
                    paginateable = vocab,
                    onWordClick = onWordClick,
                    onFuriganaClick = onFuriganaClick,
                    addWordToVocabDeckClick = onAddWordToDeck
                )

                infoScreenExpandableSentenceSection(
                    expanded = sentencesExpanded,
                    paginateable = sentences,
                    onFuriganaClick = onFuriganaClick
                )

                listSpacerState.ExtraSpacer(this)
            }
        }
    }
}
