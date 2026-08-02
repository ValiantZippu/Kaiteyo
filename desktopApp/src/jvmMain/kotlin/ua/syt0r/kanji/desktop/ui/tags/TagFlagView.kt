package ua.syt0r.kanji.desktop.ui.tags

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.designsystem.DsBadge
import ua.syt0r.kanji.desktop.designsystem.DsButton
import ua.syt0r.kanji.desktop.designsystem.DsButtonKind
import ua.syt0r.kanji.desktop.designsystem.DsCard
import ua.syt0r.kanji.desktop.designsystem.DsConfirmDialog
import ua.syt0r.kanji.desktop.designsystem.DsIconButton
import ua.syt0r.kanji.desktop.designsystem.DsPromptDialog
import ua.syt0r.kanji.desktop.designsystem.DsRadius
import ua.syt0r.kanji.desktop.designsystem.DsSectionHeader
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsTagChip
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.accent
import ua.syt0r.kanji.desktop.designsystem.parseHexColor
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import ua.syt0r.kanji.desktop.engine.history.ActivityCategory
import ua.syt0r.kanji.desktop.model.ToastKind

// ============================================
// TAGS & FLAGS MANAGER
// Global view of every tag and flag in use, with
// per-tag colors, counts, rename and delete.
// ============================================

private val tagPalette = listOf(
    "#7BC8FF", "#C2FC8B", "#FEAB57", "#A78BFA", "#FFD93D",
    "#FF8C8C", "#8CF0C8", "#F0A8D0", "#9FE2FF", "#D4C48B"
)

@Composable
fun TagFlagView(state: AppState) {
    val sc = surfaceColors()
    var renameTag by remember { mutableStateOf<String?>(null) }
    var deleteTag by remember { mutableStateOf<String?>(null) }
    var renameFlag by remember { mutableStateOf<String?>(null) }
    var deleteFlag by remember { mutableStateOf<String?>(null) }

    val tagCounts = remember(state.cards) {
        state.cards.flatMap { it.tags }.groupingBy { it }.eachCount().toSortedMap()
    }
    val flagCounts = remember(state.cards) {
        state.cards.flatMap { it.flags }.groupingBy { it }.eachCount().toSortedMap()
    }

    fun tagColor(tag: String): String =
        tagPalette[Math.floorMod(tag.hashCode(), tagPalette.size)]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(DsSpacing.Xl),
        verticalArrangement = Arrangement.spacedBy(DsSpacing.Lg)
    ) {
        DsSectionHeader(
            title = "Tags",
            subtitle = "${tagCounts.size} distinct tags across ${state.cards.size} cards"
        )

        DsCard {
            Column(
                Modifier.padding(DsSpacing.Lg),
                verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
            ) {
                if (tagCounts.isEmpty()) {
                    Text("No tags yet — add them from the browser.", color = sc.textMuted, fontSize = DsType.Body)
                }
                tagCounts.forEach { (tag, count) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = DsSpacing.Xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DsTagChip(label = tag, colorHex = tagColor(tag), onClick = {
                            state.browserQuery = "tag:$tag"
                            state.currentView = ua.syt0r.kanji.desktop.appstate.WorkspaceView.Browser
                        })
                        Spacer(Modifier.weight(1f))
                        DsBadge(text = "$count", tint = parseHexColor(tagColor(tag)))
                        Spacer(Modifier.width(DsSpacing.Sm))
                        DsIconButton(
                            icon = Icons.Default.DriveFileRenameOutline,
                            onClick = { renameTag = tag },
                            contentDescription = "Rename",
                            size = 28.dp
                        )
                        DsIconButton(
                            icon = Icons.Default.Delete,
                            onClick = { deleteTag = tag },
                            contentDescription = "Delete",
                            size = 28.dp,
                            tint = Color(0xFFFF6B6B)
                        )
                    }
                }
            }
        }

        DsSectionHeader(
            title = "Flags",
            subtitle = "${flagCounts.size} distinct flags"
        )

        DsCard {
            Column(
                Modifier.padding(DsSpacing.Lg),
                verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
            ) {
                if (flagCounts.isEmpty()) {
                    Text("No flags yet — right-click a card to add one.", color = sc.textMuted, fontSize = DsType.Body)
                }
                flagCounts.forEach { (flag, count) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = DsSpacing.Xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(flagColor(flag))
                        )
                        Spacer(Modifier.width(DsSpacing.Sm))
                        Text(
                            text = flag,
                            color = sc.textPrimary,
                            fontSize = DsType.Body,
                            modifier = Modifier.weight(1f)
                        )
                        DsBadge(text = "$count", tint = flagColor(flag))
                        Spacer(Modifier.width(DsSpacing.Sm))
                        DsIconButton(
                            icon = Icons.Default.DriveFileRenameOutline,
                            onClick = { renameFlag = flag },
                            contentDescription = "Rename",
                            size = 28.dp
                        )
                        DsIconButton(
                            icon = Icons.Default.Delete,
                            onClick = { deleteFlag = flag },
                            contentDescription = "Delete",
                            size = 28.dp,
                            tint = Color(0xFFFF6B6B)
                        )
                    }
                }
            }
        }
    }

    renameTag?.let { old ->
        DsPromptDialog(
            title = "Rename tag '$old'",
            placeholder = "New tag name",
            initialValue = old,
            onConfirm = { newName ->
                if (newName.isNotBlank() && newName != old) {
                    state.cards.replaceAll { card ->
                        if (old in card.tags) card.copy(tags = card.tags.map { if (it == old) newName else it })
                        else card
                    }
                    state.activityLog.record(ActivityCategory.Tag, "Renamed tag $old → $newName")
                }
                renameTag = null
            },
            onDismiss = { renameTag = null }
        )
    }
    deleteTag?.let { tag ->
        DsConfirmDialog(
            title = "Delete tag",
            message = "Remove tag '$tag' from all cards?",
            confirmText = "Delete",
            danger = true,
            onConfirm = {
                state.cards.replaceAll { card -> card.copy(tags = card.tags.filter { it != tag }) }
                state.activityLog.record(ActivityCategory.Tag, "Deleted tag $tag")
                state.toastHost.show("Tag deleted", kind = ToastKind.Success)
            },
            onDismiss = { deleteTag = null }
        )
    }
    renameFlag?.let { old ->
        DsPromptDialog(
            title = "Rename flag '$old'",
            placeholder = "New flag name",
            initialValue = old,
            onConfirm = { newName ->
                if (newName.isNotBlank() && newName != old) {
                    state.cards.replaceAll { card ->
                        if (old in card.flags) card.copy(flags = card.flags.map { if (it == old) newName else it })
                        else card
                    }
                    state.activityLog.record(ActivityCategory.Flag, "Renamed flag $old → $newName")
                }
                renameFlag = null
            },
            onDismiss = { renameFlag = null }
        )
    }
    deleteFlag?.let { flag ->
        DsConfirmDialog(
            title = "Delete flag",
            message = "Remove flag '$flag' from all cards?",
            confirmText = "Delete",
            danger = true,
            onConfirm = {
                state.cards.replaceAll { card -> card.copy(flags = card.flags.filter { it != flag }) }
                state.activityLog.record(ActivityCategory.Flag, "Deleted flag $flag")
                state.toastHost.show("Flag deleted", kind = ToastKind.Success)
            },
            onDismiss = { deleteFlag = null }
        )
    }
}

private fun flagColor(flag: String): Color = when (flag.lowercase()) {
    "red" -> Color(0xFFFF6B6B)
    "orange" -> Color(0xFFFEAB57)
    "yellow" -> Color(0xFFFFD93D)
    "green" -> Color(0xFFC2FC8B)
    "blue" -> Color(0xFF7BC8FF)
    "purple" -> Color(0xFFA78BFA)
    else -> Color(0xFF808080)
}
