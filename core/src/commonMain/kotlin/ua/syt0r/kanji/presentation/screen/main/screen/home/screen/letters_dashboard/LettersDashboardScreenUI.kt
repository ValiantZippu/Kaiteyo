package ua.syt0r.kanji.presentation.screen.main.screen.home.screen.letters_dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import ua.syt0r.kanji.presentation.common.ScreenLetterPracticeType
import ua.syt0r.kanji.presentation.common.rememberExtraListSpacerState
import ua.syt0r.kanji.presentation.common.theme.snapSizeTransform
import ua.syt0r.kanji.presentation.common.ui.FancyLoading
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.dashboard_common.ArchivedSectionHeader
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.dashboard_common.DashboardErrorState
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.dashboard_common.DeckDashboardEmptyState
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.dashboard_common.DeckDashboardListItem
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.dashboard_common.DeckDashboardListMode
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.dashboard_common.DeckDashboardListState
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.dashboard_common.DeckDashboardLoadedStateContainer
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.dashboard_common.DecksMergeRequestData
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.dashboard_common.DecksSortRequestData
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.dashboard_common.LetterDeckDashboardItem
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.dashboard_common.addMergeItems
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.dashboard_common.addSortItems
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.dashboard_common.deckDashboardListModeButtons
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.letters_dashboard.LettersDashboardScreenContract.ScreenState
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.letters_dashboard.ui.LetterDashboardBottomBarUI

@Composable
fun LettersDashboardScreenUI(
    state: State<ScreenState>,
    mergeDecks: (DecksMergeRequestData) -> Unit,
    sortDecks: (DecksSortRequestData) -> Unit,
    setDeckArchived: (deckId: Long, isArchived: Boolean) -> Unit,
    navigateToDeckDetails: (LetterDeckDashboardItem) -> Unit,
    navigateToDeckEdit: (LetterDeckDashboardItem) -> Unit,
    startQuickPractice: (LetterDeckDashboardItem, ScreenLetterPracticeType, List<String>) -> Unit,
    navigateToDeckPicker: () -> Unit,
    retryLoad: () -> Unit
) {

    Box {

        val extraListSpacerState = rememberExtraListSpacerState()

        AnimatedContent(
            targetState = state.value,
            transitionSpec = { ContentTransform(targetContentEnter = fadeIn(), initialContentExit = fadeOut(), sizeTransform = snapSizeTransform()) },
        ) { screenState ->
            when (screenState) {
                ScreenState.Loading -> {
                    LoadingState()
                }

                is ScreenState.Error -> {
                    DashboardErrorState(
                        message = screenState.message,
                        onRetry = retryLoad
                    )
                }

                is ScreenState.Loaded -> {
                    if (screenState.listState.items.isEmpty() && screenState.archivedItems.isEmpty()) {
                        DeckDashboardEmptyState()
                    } else {
                        val practiceType = remember {
                            derivedStateOf { screenState.selectedPracticeTypeItem.value.practiceType }
                        }
                        var archivedExpanded by rememberSaveable { mutableStateOf(false) }
                        DeckDashboardLoadedStateContainer(extraListSpacerState) {

                            if (screenState.listState.items.size > 1) {
                                deckDashboardListModeButtons(
                                    listState = screenState.listState,
                                    mergeDecks = mergeDecks,
                                    sortDecks = sortDecks
                                )
                            }

                            when (val currentMode = screenState.listState.mode.value) {
                                is DeckDashboardListMode.Browsing -> {
                                    screenState.listState.addBrowseItems(
                                        scope = this,
                                        practiceType = practiceType,
                                        showPendingNewIndicator = screenState.listState.showDailyNewIndicator,
                                        navigateToDetails = navigateToDeckDetails,
                                        navigateToEdit = navigateToDeckEdit,
                                        navigateToPractice = startQuickPractice,
                                        onArchiveClick = { setDeckArchived(it.deckId, true) }
                                    )

                                    if (screenState.archivedItems.isNotEmpty()) {
                                        item(
                                            key = "archived_section_header"
                                        ) {
                                            ArchivedSectionHeader(
                                                count = screenState.archivedItems.size,
                                                expanded = archivedExpanded,
                                                onToggle = { archivedExpanded = !archivedExpanded }
                                            )
                                        }

                                        if (archivedExpanded) {
                                            screenState.listState.addArchivedItems(
                                                scope = this,
                                                items = screenState.archivedItems,
                                                practiceType = practiceType,
                                                navigateToDetails = navigateToDeckDetails,
                                                navigateToEdit = navigateToDeckEdit,
                                                onRestoreClick = { setDeckArchived(it.deckId, false) }
                                            )
                                        }
                                    }
                                }

                                is DeckDashboardListMode.MergeMode -> {
                                    screenState.listState.addMergeItems(
                                        scope = this,
                                        listMode = currentMode
                                    )
                                }

                                is DeckDashboardListMode.SortMode -> {
                                    screenState.listState.addSortItems(
                                        scope = this,
                                        listMode = currentMode
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        LetterDashboardBottomBarUI(
            state = state,
            navigateToDeckPicker = navigateToDeckPicker,
            modifier = Modifier.align(Alignment.BottomCenter)
                .onGloballyPositioned { extraListSpacerState.updateOverlay(it) },
        )

    }

}

@Composable
private fun LoadingState() {
    FancyLoading(Modifier.fillMaxSize().wrapContentSize())
}

private fun DeckDashboardListState.addBrowseItems(
    scope: LazyListScope,
    practiceType: State<ScreenLetterPracticeType>,
    showPendingNewIndicator: Boolean,
    navigateToDetails: (LetterDeckDashboardItem) -> Unit,
    navigateToEdit: (LetterDeckDashboardItem) -> Unit,
    navigateToPractice: (LetterDeckDashboardItem, ScreenLetterPracticeType, List<String>) -> Unit,
    onArchiveClick: (LetterDeckDashboardItem) -> Unit,
) = scope.apply {

    items(
        items = items,
        key = { DeckDashboardListMode.Browsing::class.simpleName to it.deckId }
    ) { item ->

        item as LetterDeckDashboardItem

        val studyProgress = remember {
            derivedStateOf { item.studyProgress.getValue(practiceType.value) }
        }

        DeckDashboardListItem(
            itemKey = item.deckId,
            title = item.title,
            elapsedSinceLastReview = item.elapsedSinceLastReview,
            showNewIndicator = showPendingNewIndicator,
            studyProgress = studyProgress.value,
            onDetailsClick = { navigateToDetails(item) },
            onEditClick = { navigateToEdit(item) },
            navigateToPractice = { navigateToPractice(item, practiceType.value, it) },
            onArchiveClick = { onArchiveClick(item) }
        )
    }

}

private fun DeckDashboardListState.addArchivedItems(
    scope: LazyListScope,
    items: List<LetterDeckDashboardItem>,
    practiceType: State<ScreenLetterPracticeType>,
    navigateToDetails: (LetterDeckDashboardItem) -> Unit,
    navigateToEdit: (LetterDeckDashboardItem) -> Unit,
    onRestoreClick: (LetterDeckDashboardItem) -> Unit,
) = scope.apply {

    items(
        items = items,
        key = { DeckDashboardListMode.Browsing::class.simpleName to "archived_${it.deckId}" }
    ) { item ->

        val studyProgress = remember {
            derivedStateOf { item.studyProgress.getValue(practiceType.value) }
        }

        DeckDashboardListItem(
            itemKey = item.deckId,
            title = item.title,
            elapsedSinceLastReview = item.elapsedSinceLastReview,
            showNewIndicator = false,
            studyProgress = studyProgress.value,
            isArchived = true,
            onDetailsClick = { navigateToDetails(item) },
            onEditClick = { navigateToEdit(item) },
            navigateToPractice = {},
            onRestoreClick = { onRestoreClick(item) }
        )
    }

}
