package ua.syt0r.kanji.presentation.screen.main.screen.info.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import ua.syt0r.kanji.core.app_data.data.JapaneseWord
import ua.syt0r.kanji.presentation.common.ExtraListSpacerState
import ua.syt0r.kanji.presentation.common.ExtraSpacer
import ua.syt0r.kanji.presentation.common.PaginationLoadLaunchedEffect
import ua.syt0r.kanji.presentation.common.collectAsState
import ua.syt0r.kanji.presentation.common.trackList
import ua.syt0r.kanji.presentation.common.ui.LocalOrientation
import ua.syt0r.kanji.presentation.common.ui.Orientation
import ua.syt0r.kanji.presentation.dialog.SaveWordDialog
import ua.syt0r.kanji.presentation.screen.main.screen.info.InfoScreenContract
import ua.syt0r.kanji.presentation.screen.main.screen.info.LetterInfoData
import ua.syt0r.kanji.presentation.screen.main.screen.info.infoScreenExpandableSentenceSection
import ua.syt0r.kanji.presentation.screen.main.screen.info.infoScreenExpandableVocabSection
import ua.syt0r.kanji.presentation.screen.main.screen.info.ui.LearningAction
import ua.syt0r.kanji.presentation.screen.main.screen.info.ui.LearningStatusSection
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

    val letterHeading: LazyListScope.(LetterInfoData) -> Unit

    when (letterData) {
        is LetterInfoData.Kana -> letterHeading = {
            item {
                LetterInfoKanaHeading(
                    data = letterData,
                )
            }
        }

        is LetterInfoData.Kanji -> {
            letterHeading = {
                item {
                    LetterInfoKanjiHeading(
                        data = letterData,
                        onRadicalClick = onFuriganaClick,
                        onPlayReading = onPlayReading,
                        isPlayingReading = isPlayingReading,
                        userNote = learningState?.note,
                        onSaveUserNote = onSaveUserNote,
                        onWordClick = onWordClick
                    )
                }
            }
        }
    }

    if (LocalOrientation.current == Orientation.Portrait) {
        LazyColumn(
            state = listState,
            modifier = Modifier.trackList(listSpacerState)
        ) {
            letterHeading(letterData)

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
                addWordToVocabDeckClick = { wordToAddToDeck = it }
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
                letterHeading(letterData)
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
                    addWordToVocabDeckClick = { wordToAddToDeck = it }
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
