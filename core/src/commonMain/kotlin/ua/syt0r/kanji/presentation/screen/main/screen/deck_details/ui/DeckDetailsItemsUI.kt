package ua.syt0r.kanji.presentation.screen.main.screen.deck_details.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.syt0r.kanji.presentation.common.CollapsibleContainer
import ua.syt0r.kanji.presentation.common.ExtraListSpacerState
import ua.syt0r.kanji.presentation.common.ExtraSpacer
import ua.syt0r.kanji.presentation.common.rememberCollapsibleContainerState
import ua.syt0r.kanji.presentation.common.resources.string.resolveString
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoSemanticColors
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.theme.SurfaceColors
import ua.syt0r.kanji.presentation.common.theme.studyColorFor
import ua.syt0r.kanji.core.srs.SrsItemStatus
import ua.syt0r.kanji.presentation.screen.main.screen.deck_details.DeckDetailsConfigurationRow
import ua.syt0r.kanji.presentation.screen.main.screen.deck_details.data.DeckDetailsConfiguration
import ua.syt0r.kanji.presentation.screen.main.screen.deck_details.data.DeckDetailsListItem
import ua.syt0r.kanji.presentation.screen.main.screen.deck_details.data.DeckDetailsVisibleData

// ============================================================
// DECK DETAILS ITEMS — REBORN
//
// Replaces the old 300dp-wide rows with a proper kanji grid.
// Each tile shows: the kanji, its meaning, a color-coded
// status indicator, and a frequency badge. Layout adapts
// from 60dp tiles on narrow screens to ~90dp on wide.
// ============================================================

@Composable
fun DeckDetailsItemsUI(
    configuration: DeckDetailsConfiguration.LetterDeckConfiguration,
    selectionModeEnabled: MutableState<Boolean>,
    visibleData: DeckDetailsVisibleData.Items,
    extraListSpacerState: ExtraListSpacerState,
    onConfigurationUpdate: (DeckDetailsConfiguration.LetterDeckConfiguration) -> Unit,
    onCharacterClick: (String) -> Unit,
    onSelectionToggled: (DeckDetailsListItem) -> Unit,
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current

    if (visibleData.items.isEmpty()) {
        Column {
            DeckDetailsConfigurationRow(
                configuration = configuration,
                kanaGroupsMode = false,
                onConfigurationUpdate = onConfigurationUpdate
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = resolveString { deckDetails.emptyListMessage },
                    color = surfaceColors.textMuted,
                    fontSize = 14.sp
                )
            }
        }
        return
    }

    Column {
        val collapsibleConfigurationContainerState = rememberCollapsibleContainerState()

        CollapsibleContainer(collapsibleConfigurationContainerState) {
            DeckDetailsConfigurationRow(
                configuration = configuration,
                kanaGroupsMode = false,
                onConfigurationUpdate = onConfigurationUpdate
            )
        }

        // Summary bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val totalItems = visibleData.items.size
            val newCount = visibleData.items.count { it.data.summaryMap[configuration.practiceType]?.srsItemStatus == SrsItemStatus.New }
            val reviewCount = visibleData.items.count { it.data.summaryMap[configuration.practiceType]?.srsItemStatus == SrsItemStatus.Review }
            val doneCount = visibleData.items.count { it.data.summaryMap[configuration.practiceType]?.srsItemStatus == SrsItemStatus.Done }

            Text(
                text = "$totalItems kanji",
                style = MaterialTheme.typography.bodySmall,
                color = surfaceColors.textPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.width(12.dp))
            StatusPill("New", newCount, accent.primary, surfaceColors)
            Spacer(Modifier.width(6.dp))
            val sem = LocalKaiteyoSemanticColors.current
            StatusPill("Review", reviewCount, sem.info, surfaceColors)
            Spacer(Modifier.width(6.dp))
            StatusPill("Done", doneCount, sem.success, surfaceColors)
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(80.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .nestedScroll(collapsibleConfigurationContainerState.nestedScrollConnection)
        ) {
            items(
                items = visibleData.items,
                key = { it.key.value },
            ) { item ->
                KanjiGridTile(
                    item = item,
                    isSelectionModeEnabled = selectionModeEnabled,
                    configuration = configuration,
                    onCharacterClick = onCharacterClick,
                    onSelectionToggled = onSelectionToggled,
                    surfaceColors = surfaceColors,
                    accent = accent
                )
            }

            extraListSpacerState.ExtraSpacer(this)
        }
    }
}

// ============================================================
// KANJI GRID TILE — the actual kanji card
// ============================================================

@Composable
private fun KanjiGridTile(
    item: DeckDetailsListItem.Letter,
    isSelectionModeEnabled: State<Boolean>,
    configuration: DeckDetailsConfiguration.LetterDeckConfiguration,
    onCharacterClick: (String) -> Unit,
    onSelectionToggled: (DeckDetailsListItem.Letter) -> Unit,
    surfaceColors: SurfaceColors,
    accent: ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme
) {
    val summary = item.data.summaryMap.getValue(configuration.practiceType)
    val srsStatus = summary.srsItemStatus
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val sem = LocalKaiteyoSemanticColors.current
    val statusColor = when (srsStatus) {
        SrsItemStatus.New -> accent.primary
        SrsItemStatus.Review -> sem.info
        SrsItemStatus.Done -> sem.success
    }

    val bg by animateColorAsState(
        targetValue = when {
            isSelectionModeEnabled.value && item.selected.value -> accent.primary.copy(alpha = 0.16f)
            isHovered -> surfaceColors.surfaceInteractive
            else -> surfaceColors.surface
        },
        label = "tileBg"
    )

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(
                width = if (isSelectionModeEnabled.value && item.selected.value) 1.5.dp else 0.dp,
                color = if (isSelectionModeEnabled.value && item.selected.value) accent.primary else Color.Transparent,
                shape = RoundedCornerShape(14.dp)
            )
            .hoverable(interactionSource)
            .clickable(isSelectionModeEnabled.value) {
                onSelectionToggled(item)
            }
            .clickable(!isSelectionModeEnabled.value) {
                onCharacterClick(item.data.character)
            }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status dot + selection indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionModeEnabled.value) {
                RadioButton(
                    selected = item.selected.value,
                    onClick = { onSelectionToggled(item) },
                    colors = RadioButtonDefaults.colors(selectedColor = accent.primary),
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
            }
            Spacer(Modifier.weight(1f))
            // Rep count badge
            if (summary.repeats > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "${summary.repeats}",
                        fontSize = 8.sp,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // The kanji character — large and centered
        Text(
            text = item.data.character,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = surfaceColors.textPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(4.dp))

        // Status label
        Text(
            text = srsStatus.name,
            fontSize = 8.sp,
            color = statusColor,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )

        // Lapses indicator
        if (summary.lapses > 0) {
            Text(
                text = "${summary.lapses} lapses",
                fontSize = 7.sp,
                color = LocalKaiteyoSemanticColors.current.error,
                maxLines = 1
            )
        }
    }
}

// ============================================================
// STATUS PILL — compact count badge
// ============================================================

@Composable
private fun StatusPill(label: String, count: Int, color: Color, surfaceColors: SurfaceColors) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = "$count $label",
            fontSize = 10.sp,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}
