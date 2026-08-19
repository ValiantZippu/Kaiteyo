package ua.syt0r.kanji.presentation.screen.main.screen.deck_details

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ua.syt0r.kanji.core.srs.SrsItemStatus
import ua.syt0r.kanji.presentation.common.ExtraListSpacerState
import ua.syt0r.kanji.presentation.common.MultiplatformBackHandler
import ua.syt0r.kanji.presentation.common.ScreenLetterPracticeType
import ua.syt0r.kanji.presentation.common.ScreenVocabPracticeType
import ua.syt0r.kanji.presentation.common.rememberExtraListSpacerState
import ua.syt0r.kanji.presentation.common.resources.icon.ExtraIcons
import ua.syt0r.kanji.presentation.common.resources.icon.RadioButtonChecked
import ua.syt0r.kanji.presentation.common.resources.string.resolveString
import ua.syt0r.kanji.presentation.common.theme.extraColorScheme
import ua.syt0r.kanji.presentation.common.ui.FancyLoading
import ua.syt0r.kanji.presentation.common.ui.kanji.HighlightedLetter
import ua.syt0r.kanji.presentation.screen.main.screen.deck_details.DeckDetailsScreenContract.ScreenState
import ua.syt0r.kanji.presentation.screen.main.screen.deck_details.data.DeckDetailsConfiguration
import ua.syt0r.kanji.presentation.screen.main.screen.deck_details.data.DeckDetailsListItem
import ua.syt0r.kanji.presentation.screen.main.screen.deck_details.data.DeckDetailsVisibleData
import ua.syt0r.kanji.presentation.screen.main.screen.deck_details.data.FilterConfiguration
import ua.syt0r.kanji.presentation.screen.main.screen.deck_details.ui.DeckDetailsBottomSheet
import ua.syt0r.kanji.presentation.screen.main.screen.deck_details.ui.DeckDetailsFilterDialog
import ua.syt0r.kanji.presentation.screen.main.screen.deck_details.ui.DeckDetailsGroupsUI
import ua.syt0r.kanji.presentation.screen.main.screen.deck_details.ui.DeckDetailsItemsUI
import ua.syt0r.kanji.presentation.screen.main.screen.deck_details.ui.DeckDetailsLayoutDialog
import ua.syt0r.kanji.presentation.screen.main.screen.deck_details.ui.DeckDetailsSortDialog
import ua.syt0r.kanji.presentation.screen.main.screen.deck_details.ui.DeckDetailsToolbar
import ua.syt0r.kanji.presentation.screen.main.screen.deck_details.ui.DeckDetailsVocabUI
import ua.syt0r.kanji.presentation.common.ui.KaiteyoAlertDialog
import ua.syt0r.kanji.presentation.screen.main.screen.decks.defaultKaiteyoNoteTypes
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.dashboard_common.DashboardErrorState

@Composable
fun DeckDetailsScreenUI(
    state: State<ScreenState>,
    navigateUp: () -> Unit,
    navigateToDeckEdit: () -> Unit,
    navigateToCharacterDetails: (String) -> Unit,
    navigateToCardDetails: (DeckDetailsListItem.Vocab) -> Unit,
    startGroupReview: (DeckDetailsListItem.Group) -> Unit,
    startMultiselectReview: () -> Unit,
    retryLoad: () -> Unit,
) {

    var shouldShowVisibilityDialog by remember { mutableStateOf(false) }
    if (shouldShowVisibilityDialog) {
        val loadedState = state.value as? ScreenState.Loaded.Letters
        if (loadedState != null) {
            val configuration = loadedState.configuration.value
            DeckDetailsLayoutDialog(
                layout = configuration.layout,
                kanaGroups = configuration.kanaGroups,
                onDismissRequest = { shouldShowVisibilityDialog = false },
                onApplyConfiguration = { layout, kanaGroups ->
                    shouldShowVisibilityDialog = false
                    loadedState.configuration.value = configuration.copy(
                        layout = layout,
                        kanaGroups = kanaGroups
                    )
                }
            )
        } else {
            // State not loaded (anymore) — nothing to configure.
            shouldShowVisibilityDialog = false
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val practiceSharer = rememberPracticeSharer(snackbarHostState)
    val bottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)

    if (bottomSheetState.isVisible) {
        MultiplatformBackHandler { coroutineScope.launch { bottomSheetState.hide() } }
    }

    ModalBottomSheetLayout(
        sheetState = bottomSheetState,
        modifier = Modifier.clipToBounds(),
        sheetBackgroundColor = MaterialTheme.colorScheme.surface,
        sheetShape = MaterialTheme.shapes.large.copy(
            bottomStart = ZeroCornerSize,
            bottomEnd = ZeroCornerSize
        ),
        sheetContent = {
            DeckDetailsBottomSheet(
                state = state,
                onCharacterClick = navigateToCharacterDetails,
                onStudyClick = startGroupReview,
                onDismissRequest = { bottomSheetState.hide() }
            )
        }
    ) {

        Scaffold(
            topBar = {
                DeckDetailsToolbar(
                    state = state,
                    upButtonClick = navigateUp,
                    onVisibilityButtonClick = { shouldShowVisibilityDialog = true },
                    editButtonClick = navigateToDeckEdit,
                    shareButtonClick = { practiceSharer.share(it) }
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { paddingValues ->

            val noGroupsSelectedMessage = resolveString { deckDetails.multiselectNoSelected }

            ScreenContent(
                state = state,
                navigateToCharacterDetails = navigateToCharacterDetails,
                onCardClick = navigateToCardDetails,
                showGroupSheet = {
                    coroutineScope.launch { bottomSheetState.show() }
                },
                startMultiselectReview = startMultiselectReview,
                showNoSelectionMessage = {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = noGroupsSelectedMessage,
                            withDismissAction = true
                        )
                    }
                },
                retryLoad = retryLoad,
                modifier = Modifier.fillMaxSize().padding(paddingValues),
            )

        }

    }

}

@Composable
private fun ScreenContent(
    state: State<ScreenState>,
    navigateToCharacterDetails: (String) -> Unit,
    onCardClick: (DeckDetailsListItem.Vocab) -> Unit,
    showGroupSheet: () -> Unit,
    startMultiselectReview: () -> Unit,
    showNoSelectionMessage: () -> Unit,
    retryLoad: () -> Unit,
    modifier: Modifier,
) {

    val extraListSpacerState = rememberExtraListSpacerState()

    val transition = updateTransition(targetState = state.value, label = "State Transition")
    transition.AnimatedContent(
        contentKey = { it::class },
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        modifier = modifier.onGloballyPositioned { extraListSpacerState.updateList(it) }
    ) { screenState ->

        when (screenState) {
            ScreenState.Loading -> {
                FancyLoading(Modifier.fillMaxSize().wrapContentSize())
            }

            is ScreenState.Error -> {
                DashboardErrorState(
                    message = screenState.message,
                    onRetry = retryLoad
                )
            }

            is ScreenState.Loaded -> {
                ScreenLoadedState(
                    screenState = screenState,
                    extraListSpacerState = extraListSpacerState,
                    onCharacterClick = navigateToCharacterDetails,
                    showGroupSheet = showGroupSheet,
                    toggleItemSelection = { it.selected.run { value = !value } },
                    onCardClick = onCardClick
                )

                if (screenState.isSelectionModeEnabled.value) {
                    MultiplatformBackHandler(
                        onBack = { screenState.isSelectionModeEnabled.value = false }
                    )
                }
            }
        }

    }

    transition.AnimatedContent(
        transitionSpec = { scaleIn() togetherWith scaleOut() },
        modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.BottomEnd),
        contentAlignment = Alignment.BottomEnd
    ) { screenState ->
        if (screenState !is ScreenState.Loaded) {
            Box(Modifier)
            return@AnimatedContent
        }


        FAB(
            screenState = screenState,
            extraListSpacerState = extraListSpacerState,
            startPractice = {
                val anyItemSelected = screenState.visibleDataState.value
                    .items.any { it.selected.value }
                if (anyItemSelected) {
                    startMultiselectReview()
                } else {
                    showNoSelectionMessage()
                }
            },
            startSelectionMode = { screenState.isSelectionModeEnabled.apply { value = !value } }
        )
    }

}

@Composable
private fun ScreenLoadedState(
    screenState: ScreenState.Loaded,
    extraListSpacerState: ExtraListSpacerState,
    onCharacterClick: (String) -> Unit,
    showGroupSheet: () -> Unit,
    toggleItemSelection: (DeckDetailsListItem) -> Unit,
    onCardClick: (DeckDetailsListItem.Vocab) -> Unit
) {

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        // ── Deck progress header — one glance: how much of this deck is done ──
        DeckProgressHeader(visibleData = screenState.visibleDataState.value)

        AnimatedContent(
            targetState = screenState.visibleDataState.value,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier.weight(1f)
        ) { visibleData ->

            when (visibleData) {
                is DeckDetailsVisibleData.Groups -> {
                    screenState as ScreenState.Loaded.Letters
                    DeckDetailsGroupsUI(
                        configuration = screenState.configuration.value,
                        selectionModeEnabled = screenState.isSelectionModeEnabled,
                        visibleData = visibleData,
                        extraListSpacerState = extraListSpacerState,
                        onConfigurationUpdate = { screenState.configuration.value = it },
                        selectGroup = {
                            visibleData.selectedItem.value = it
                            showGroupSheet()
                        },
                        toggleGroupSelection = toggleItemSelection
                    )
                }

                is DeckDetailsVisibleData.Items -> {
                    screenState as ScreenState.Loaded.Letters
                    DeckDetailsItemsUI(
                        configuration = screenState.configuration.value,
                        selectionModeEnabled = screenState.isSelectionModeEnabled,
                        visibleData = visibleData,
                        extraListSpacerState = extraListSpacerState,
                        onConfigurationUpdate = { screenState.configuration.value = it },
                        onCharacterClick = onCharacterClick,
                        onSelectionToggled = toggleItemSelection
                    )
                }

                is DeckDetailsVisibleData.Vocab -> {
                    screenState as ScreenState.Loaded.Vocab
                    DeckDetailsVocabUI(
                        screenState = screenState,
                        visibleData = visibleData,
                        extraListSpacerState = extraListSpacerState,
                        onConfigurationUpdate = { screenState.configuration.value = it },
                        toggleItemSelection = toggleItemSelection,
                        onCardClick = onCardClick
                    )
                }
            }

        }

    }

}

@Composable
fun SrsItemStatus.toColor(
    newColor: Color = MaterialTheme.colorScheme.surfaceVariant
): Color = when (this) {
    SrsItemStatus.Done -> MaterialTheme.extraColorScheme.success
    SrsItemStatus.Review -> MaterialTheme.extraColorScheme.due
    SrsItemStatus.New -> newColor
}

@Composable
private fun DeckProgressHeader(visibleData: DeckDetailsVisibleData) {
    // Count item states across the three deck shapes (letters, letter groups,
    // vocabulary) so the header always reflects the real SRS state.
    var total = 0
    var done = 0
    var review = 0
    var newCount = 0
    visibleData.items.forEach { item ->
        val statuses = when (item) {
            is DeckDetailsListItem.Group -> listOf(item.reviewState)
            is DeckDetailsListItem.Letter -> item.data.summaryMap.values.map { it.srsItemStatus }
            is DeckDetailsListItem.Vocab -> item.data.srsStatus.values.toList()
        }
        // Deduplicate: a vocab card tracked by two practice types counts once.
        val unique = statuses.distinct()
        if (unique.isNotEmpty()) {
            total += 1
            when {
                unique.all { it == SrsItemStatus.Done } -> done += 1
                unique.any { it == SrsItemStatus.Review } -> review += 1
                else -> newCount += 1
            }
        }
    }
    if (total == 0) return

    val fraction = if (done + review == 0) 0f else (done + review).toFloat() / total
    val accent = MaterialTheme.colorScheme.primary
    val success = MaterialTheme.extraColorScheme.success
    val due = MaterialTheme.extraColorScheme.due
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val textMuted = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(surfaceVariant.copy(alpha = 0.55f))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Deck progress",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = textMuted,
                modifier = Modifier.weight(1f)
            )
            Text(
                "${(fraction * 100).toInt()}%",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.fillMaxWidth().height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = success,
            trackColor = surfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HeaderCountChip("$total", "cards", accent)
            HeaderCountChip("$done", "learned", success)
            HeaderCountChip("$review", "due now", due)
            HeaderCountChip("$newCount", "new", textMuted)
        }
    }
}

@Composable
private fun HeaderCountChip(value: String, label: String, color: Color) {
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            label,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FAB(
    screenState: ScreenState.Loaded,
    extraListSpacerState: ExtraListSpacerState,
    startPractice: () -> Unit,
    startSelectionMode: () -> Unit,
) {

    FloatingActionButton(
        onClick = {
            if (screenState.isSelectionModeEnabled.value) {
                startPractice()
            } else {
                startSelectionMode()
            }
        },
        modifier = Modifier.padding(16.dp)
            .onGloballyPositioned { extraListSpacerState.updateOverlay(it) },
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {

        AnimatedContent(
            targetState = screenState.isSelectionModeEnabled.value,
            transitionSpec = {
                fadeIn(tween(150, 150)) togetherWith fadeOut(tween(150))
            }
        ) {
            if (it) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null
                )
            } else {
                Icon(
                    ExtraIcons.RadioButtonChecked,
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
fun DeckDetailsConfigurationRow(
    configuration: DeckDetailsConfiguration.LetterDeckConfiguration,
    kanaGroupsMode: Boolean,
    onConfigurationUpdate: (DeckDetailsConfiguration.LetterDeckConfiguration) -> Unit,
) {

    var showFilterOptionDialog by remember { mutableStateOf(false) }
    if (showFilterOptionDialog) {
        DeckDetailsFilterDialog(
            filter = configuration.filterConfiguration,
            onDismissRequest = { showFilterOptionDialog = false },
            onApplyConfiguration = {
                showFilterOptionDialog = false
                onConfigurationUpdate(configuration.copy(filterConfiguration = it))
            }
        )
    }

    var showSortDialog by remember { mutableStateOf(false) }
    if (showSortDialog) {
        DeckDetailsSortDialog(
            sortOption = configuration.sortOption,
            isDesc = configuration.isDescending,
            onDismissRequest = { showSortDialog = false },
            onApplyClick = { sort, isDesc ->
                showSortDialog = false
                onConfigurationUpdate(configuration.copy(sortOption = sort, isDescending = isDesc))
            }
        )
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        FilterChip(
            selected = true,
            onClick = {
                val newPracticeType = when (configuration.practiceType) {
                    ScreenLetterPracticeType.Writing -> ScreenLetterPracticeType.Reading
                    ScreenLetterPracticeType.Reading -> ScreenLetterPracticeType.Writing
                }
                onConfigurationUpdate(configuration.copy(practiceType = newPracticeType))
            },
            modifier = Modifier.wrapContentSize(Alignment.CenterStart),
            label = { Text(resolveString(configuration.practiceType.titleResolver)) },
            trailingIcon = {
                Icon(
                    imageVector = when (configuration.practiceType) {
                        ScreenLetterPracticeType.Writing -> Icons.Default.Draw
                        ScreenLetterPracticeType.Reading -> Icons.Default.LocalLibrary
                    },
                    contentDescription = null
                )
            }
        )
        if (kanaGroupsMode) {
            FilterChip(
                selected = true,
                enabled = false,
                // Visibly disabled while kana-groups mode is active — the
                // filter controls are intentionally inert in this mode.
                onClick = { /* disabled */ },
                modifier = Modifier.wrapContentSize(Alignment.CenterStart),
                label = { Text(resolveString { deckDetails.kanaGroupsModeActivatedLabel }) },
            )
        } else {

            SrsFilterChip(
                filterConfiguration = configuration.filterConfiguration,
                onClick = { showFilterOptionDialog = true }
            )

            FilterChip(
                selected = true,
                onClick = { showSortDialog = true },
                modifier = Modifier.wrapContentSize(Alignment.CenterStart),
                label = { Text(resolveString(configuration.sortOption.titleResolver)) },
                trailingIcon = {
                    Icon(
                        imageVector = configuration.sortOption.imageVector,
                        contentDescription = null,
                        modifier = Modifier.graphicsLayer {
                            rotationZ = if (configuration.isDescending) 90f else 270f
                        }
                    )
                }
            )
        }

    }
}

@Composable
private fun SrsFilterChip(
    filterConfiguration: FilterConfiguration,
    onClick: () -> Unit
) {
    FilterChip(
        selected = true,
        onClick = onClick,
        modifier = Modifier.wrapContentSize(Alignment.CenterStart),
        label = {
            Text(
                text = resolveString {
                    filterConfiguration.run {
                        when {
                            showNew && showDue && showDone -> deckDetails.filterAllLabel
                            !(showNew || showDue || showDone) -> deckDetails.filterNoneLabel
                            else -> {
                                val appliedFilters = mutableListOf<String>()
                                if (showNew) appliedFilters.add(reviewStateNew)
                                if (showDue) appliedFilters.add(reviewStateDue)
                                if (showDone) appliedFilters.add(reviewStateDone)
                                appliedFilters.joinToString()
                            }
                        }
                    }
                }
            )
        },
        trailingIcon = { Icon(Icons.Default.FilterAlt, null) }
    )
}

@Composable
fun DeckDetailsConfigurationRow(
    configuration: DeckDetailsConfiguration.VocabDeckConfiguration,
    onConfigurationUpdate: (DeckDetailsConfiguration.VocabDeckConfiguration) -> Unit,
) {

    var showFilterOptionDialog by remember { mutableStateOf(false) }
    if (showFilterOptionDialog) {
        DeckDetailsFilterDialog(
            filter = configuration.filterConfiguration,
            onDismissRequest = { showFilterOptionDialog = false },
            onApplyConfiguration = {
                showFilterOptionDialog = false
                onConfigurationUpdate(configuration.copy(filterConfiguration = it))
            }
        )
    }

    var showNoteTypeDialog by remember { mutableStateOf(false) }
    if (showNoteTypeDialog) {
        VocabNoteTypeDialog(
            currentNoteTypeId = configuration.noteTypeId,
            onDismissRequest = { showNoteTypeDialog = false },
            onApply = { noteTypeId ->
                showNoteTypeDialog = false
                onConfigurationUpdate(configuration.copy(noteTypeId = noteTypeId))
            }
        )
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {

        FilterChip(
            selected = true,
            onClick = {
                val practiceTypes = ScreenVocabPracticeType.entries
                val newPracticeTypeOrdinal =
                    (configuration.practiceType.ordinal + 1) % practiceTypes.size
                val newPracticeType = practiceTypes[newPracticeTypeOrdinal]
                onConfigurationUpdate(configuration.copy(practiceType = newPracticeType))
            },
            modifier = Modifier.wrapContentSize(Alignment.CenterStart),
            label = { Text(resolveString(configuration.practiceType.titleResolver)) },
        )

        SrsFilterChip(
            filterConfiguration = configuration.filterConfiguration,
            onClick = { showFilterOptionDialog = true }
        )

        FilterChip(
            selected = true,
            onClick = { showNoteTypeDialog = true },
            modifier = Modifier.wrapContentSize(Alignment.CenterStart),
            label = {
                Text(
                    "Note type: " + (defaultKaiteyoNoteTypes
                        .firstOrNull { it.id == configuration.noteTypeId }?.name
                        ?: "Kaiteyo (default)")
                )
            },
            trailingIcon = { Icon(Icons.Default.Description, null) }
        )

    }

}


@Composable
fun DeckDetailsCharacterBox(
    character: String,
    srsStatus: SrsItemStatus,
    onClick: () -> Unit,
    constraintOrientation: Orientation = Orientation.Horizontal
) {
    HighlightedLetter(
        letter = character,
        onClick = { onClick() },
        aspectRatioConstraintOrientation = constraintOrientation,
        modifier = Modifier.border(
            width = 2.dp,
            color = srsStatus.toColor(newColor = Color.Transparent),
            shape = MaterialTheme.shapes.small
        )
    )
}

/**
 * Note type picker for flashcard (vocabulary) decks. Mirrors the design
 * language of the navigation settings: selectable cards with a checkmark on
 * the active one and a description under each name.
 */
@Composable
private fun VocabNoteTypeDialog(
    currentNoteTypeId: String,
    onDismissRequest: () -> Unit,
    onApply: (String) -> Unit
) {
    var selectedId by remember { mutableStateOf(currentNoteTypeId) }

    KaiteyoAlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Note Type") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "The note type defines the fields your flashcards carry. New cards in this deck use the selected type.",
                    fontSize = 12.sp
                )
                defaultKaiteyoNoteTypes.forEach { type ->
                    val selected = type.id == selectedId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.large)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.surfaceContainerHighest
                            )
                            .border(
                                width = if (selected) 1.5.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                shape = MaterialTheme.shapes.large
                            )
                            .clickable { selectedId = type.id }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (selected) Icons.Default.Check else Icons.Default.Description,
                            contentDescription = null,
                            tint = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                type.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (type.description.isNotBlank()) {
                                Text(
                                    type.description,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                "Fields: " + type.fields.joinToString(" · ") { it.label },
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(selectedId) }) { Text("Done") }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text("Cancel") }
        }
    )
}

