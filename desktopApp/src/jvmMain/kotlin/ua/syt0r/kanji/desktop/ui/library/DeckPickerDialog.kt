package ua.syt0r.kanji.desktop.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.designsystem.DsBadge
import ua.syt0r.kanji.desktop.designsystem.DsButton
import ua.syt0r.kanji.desktop.designsystem.DsButtonKind
import ua.syt0r.kanji.desktop.designsystem.DsDialog
import ua.syt0r.kanji.desktop.designsystem.DsRadius
import ua.syt0r.kanji.desktop.designsystem.DsSearchField
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.accent
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import ua.syt0r.kanji.desktop.model.DeckDef

/**
 * Pick one deck from the library. Used by Move-to-folder, Merge-into
 * and Add-to-deck workflows. [decks] lets callers control the pool
 * (e.g. excluding the current deck's subtree when moving).
 */
@Composable
fun DeckPickerDialog(
    state: AppState,
    title: String,
    subtitle: String,
    decks: List<DeckDef>,
    onPick: (DeckDef) -> Unit,
    onDismiss: () -> Unit
) {
    val sc = surfaceColors()
    val ac = accent()
    var query by remember { mutableStateOf("") }
    var picked by remember { mutableStateOf(false) }

    // Once a deck is chosen the dialog closes itself WITHOUT invoking
    // onDismiss, so callers that need to transition to a follow-up step
    // (e.g. merge confirmation) keep their state intact.
    if (picked) return

    DsDialog(title = title, onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
            Text(
                text = subtitle,
                color = sc.textSecondary,
                fontSize = DsType.Body
            )
            DsSearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Filter decks…"
            )
            val q = query.trim()
            val filtered = remember(decks, q) {
                if (q.isBlank()) decks
                else decks.filter {
                    it.name.contains(q, ignoreCase = true) ||
                        it.description.contains(q, ignoreCase = true) ||
                        it.tags.any { tag -> tag.contains(q, ignoreCase = true) }
                }
            }
            if (filtered.isEmpty()) {
                Text(
                    text = "No decks available.",
                    color = sc.textMuted,
                    fontSize = DsType.Body,
                    modifier = Modifier.padding(DsSpacing.Md)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                ) {
                    items(filtered, key = { it.id }) { deck ->
                        val interaction = remember { MutableInteractionSource() }
                        val hovered by interaction.collectIsHoveredAsState()
                        val cardCount = state.library.cardsIn(deck, state.cards.toList()).size
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(DsRadius.Md))
                                .background(
                                    if (hovered) sc.surfaceInteractive else sc.surfaceElevated
                                )
                                .clickable(interactionSource = interaction, indication = null) {
                                    picked = true
                                    onPick(deck)
                                }
                                .hoverable(interaction)
                                .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
                        ) {
                            Icon(
                                imageVector = if (deck.parentId == null) Icons.Default.FolderOpen else Icons.Default.Folder,
                                contentDescription = null,
                                tint = ac.primary,
                                modifier = Modifier.width(18.dp)
                            )
                            Spacer(Modifier.width(2.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = deck.name,
                                    color = sc.textPrimary,
                                    fontSize = DsType.Body,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = deck.description.ifBlank { deck.kind.label },
                                    color = sc.textMuted,
                                    fontSize = DsType.Caption,
                                    maxLines = 1
                                )
                            }
                            DsBadge(text = "$cardCount cards", tint = sc.textMuted)
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm, Alignment.End)
            ) {
                DsButton(text = "Cancel", kind = DsButtonKind.Ghost, onClick = onDismiss)
            }
        }
    }
}
