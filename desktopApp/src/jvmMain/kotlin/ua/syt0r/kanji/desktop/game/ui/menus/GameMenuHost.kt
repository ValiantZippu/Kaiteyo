package ua.syt0r.kanji.desktop.game.ui.menus

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import ua.syt0r.kanji.desktop.game.engine.input.InputAction
import ua.syt0r.kanji.desktop.designsystem.DsButton
import ua.syt0r.kanji.desktop.designsystem.DsButtonKind
import ua.syt0r.kanji.desktop.designsystem.DsIconButton
import ua.syt0r.kanji.desktop.designsystem.DsRadius
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.game.GameSession

/**
 * The pause menu (spec §72, §105): Map · Quests · Collection · Album ·
 * Knowledge · Character · Settings · Exit Game, with a clean Resume. Opening
 * a panel pauses the world; nothing freezes mid-animation.
 */
@Composable
fun GameMenuHost(
    session: GameSession,
    onOpenInKaiteyo: (String) -> Unit,
    onExitToKaiteyo: () -> Unit
) {
    val state = session.state
    when {
        state.menuOpen -> MenuRoot(session, onExitToKaiteyo)
        // The map is a full-window surface, not a small frame — the world
        // deserves the window (spec §69, §89: a stylized map, not a list).
        state.mapOpen -> MapView(session)
        state.questLogOpen -> MenuFrame(session, "Quests") { QuestLogView(session) }
        state.collectionOpen -> MenuFrame(session, "Collection") { CollectionView(session) }
        state.albumOpen -> MenuFrame(session, "Photo Album") { AlbumView(session) }
        state.knowledgeOpen -> MenuFrame(session, "Knowledge") { KnowledgeMapView(session, onOpenInKaiteyo) }
        state.dictionaryOpen -> MenuFrame(session, "Dictionary") { DictionaryView(session, onOpenInKaiteyo) }
        state.peopleOpen -> MenuFrame(session, "People") { PeopleView(session) }
        state.characterOpen -> MenuFrame(session, "Character") { CharacterView(session) }
        state.storyOpen -> MenuFrame(session, "Story") { StoryView(session) }
        state.savesOpen -> MenuFrame(session, "Save Slots") { SavesView(session) }
        state.settingsOpen -> MenuFrame(session, "Game Settings") { GameSettingsView(session) }
    }
}

@Composable
private fun MenuRoot(session: GameSession, onExitToKaiteyo: () -> Unit) {
    val state = session.state
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .clip(RoundedCornerShape(DsRadius.Lg))
                .background(Color(0xFF171E26).copy(alpha = 0.96f))
                .padding(DsSpacing.Xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
        ) {
            Text(
                text = "かいてよ — Game",
                color = Color.White,
                fontSize = DsType.Heading,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Hamanaka ・ 浜中 — a summer town",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = DsType.Caption
            )
            // Gamepad/keyboard navigation (spec §14-15): arrows/DPad move the
            // cursor, Interact (A/Enter) activates. Same edges as gameplay,
            // so one path serves every device.
            val items = listOf<Triple<String, String, () -> Unit>>(
                Triple("Map", "Discoveries & travel") { state.menuOpen = false; state.mapOpen = true },
                Triple("Quests", "Active quests & objectives") { state.menuOpen = false; state.questLogOpen = true },
                Triple("Collection", "Stamps, souvenirs & phrases") { state.menuOpen = false; state.collectionOpen = true },
                Triple("Photo Album", "Your captures") { state.menuOpen = false; state.albumOpen = true },
                Triple("Knowledge", "Your word graph") { state.menuOpen = false; state.knowledgeOpen = true },
                Triple("Dictionary", "Look up discovered words") { state.menuOpen = false; state.dictionaryOpen = true },
                Triple("People", "Who you've met & bonded with") { state.menuOpen = false; state.peopleOpen = true },
                Triple("Character", "Level, items & cosmetics") { state.menuOpen = false; state.characterOpen = true },
                Triple("Story", "Chapters, scenes & stories") { state.menuOpen = false; state.storyOpen = true },
                Triple("Save Slots", "Journeys & autosave") { state.menuOpen = false; state.savesOpen = true },
                Triple("Settings", "Assistance, camera, controls") { state.menuOpen = false; state.settingsOpen = true },
                Triple("Resume", "Back to the world") { state.menuOpen = false }
            )
            var focusIndex by remember { mutableIntStateOf(items.lastIndex) } // Resume first
            LaunchedEffect(session) {
                while (true) {
                    val input = session.input.state
                    if (input.wasPressedThisFrame(InputAction.MoveUp)) {
                        focusIndex = (focusIndex - 1 + items.size) % items.size
                    }
                    if (input.wasPressedThisFrame(InputAction.MoveDown)) {
                        focusIndex = (focusIndex + 1) % items.size
                    }
                    if (input.wasPressedThisFrame(InputAction.Interact)) {
                        items[focusIndex].third()
                    }
                    delay(40)
                }
            }
            items.forEachIndexed { index, (title, subtitle, action) ->
                MenuItem(
                    title = title,
                    subtitle = subtitle,
                    focused = index == focusIndex,
                    onClick = action
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                DsButton(
                    text = "Resume",
                    kind = DsButtonKind.Primary,
                    onClick = { state.menuOpen = false }
                )
                DsButton(
                    text = "Save & exit to Kaiteyo",
                    kind = DsButtonKind.Ghost,
                    onClick = onExitToKaiteyo
                )
            }
        }
    }
}

@Composable
private fun MenuItem(
    title: String,
    focused: Boolean = false,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DsRadius.Md))
            .background(
                when {
                    focused -> Color(0xFFFFD54F).copy(alpha = 0.18f)
                    hovered -> Color.White.copy(alpha = 0.08f)
                    else -> Color.Transparent
                }
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .hoverable(interaction)
            .padding(horizontal = DsSpacing.Lg, vertical = DsSpacing.Md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                title,
                color = if (focused) Color(0xFFFFD54F) else Color.White,
                fontSize = DsType.Body,
                fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Medium
            )
            if (subtitle != null) {
                Text(subtitle, color = Color.White.copy(alpha = 0.5f), fontSize = DsType.Caption)
            }
        }
        Text("›", color = if (focused) Color(0xFFFFD54F) else Color(0xFFFFD54F).copy(alpha = 0.4f), fontSize = DsType.Body)
    }
}

/** Shared frame for sub-panels: header, content, close. */
@Composable
private fun MenuFrame(
    session: GameSession,
    title: String,
    content: @Composable () -> Unit
) {
    val state = session.state
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp)
                .heightIn(max = 560.dp)
                .clip(RoundedCornerShape(DsRadius.Lg))
                .background(Color(0xFF171E26).copy(alpha = 0.96f))
                .padding(DsSpacing.Lg)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = DsType.Heading,
                    fontWeight = FontWeight.Bold
                )
                DsIconButton(
                    icon = Icons.Default.Close,
                    onClick = { state.closeAllPanels() },
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
            Box(Modifier.fillMaxWidth().padding(vertical = DsSpacing.Sm)) {
                content()
            }
        }
    }
}
