package ua.syt0r.kanji.desktop.game.ui.hud

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ua.syt0r.kanji.desktop.designsystem.DsIconButton
import ua.syt0r.kanji.desktop.designsystem.DsRadius
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.accent
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import ua.syt0r.kanji.desktop.game.GameSession
import ua.syt0r.kanji.desktop.game.quest.Quest
import ua.syt0r.kanji.desktop.game.time.TimePhase

/**
 * Minimal HUD (spec §71): one objective line, the interaction prompt, a
 * small clock/weather/location chip and three quick actions. No XP bars,
 * no health, no button wall.
 */
@Composable
fun GameHud(session: GameSession) {
    val sc = surfaceColors()
    val state = session.state
    if (state.photoMode || state.menuOpen) return

    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DsSpacing.Md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Quick actions
            Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs)) {
                HudIconButton(Icons.Default.Map, "Map (M)") { state.mapOpen = true }
                HudIconButton(Icons.Default.Place, "Quests (Q)") { state.questLogOpen = true }
                HudIconButton(Icons.Default.Camera, "Photo mode (C)") { session.togglePhotoMode() }
                HudIconButton(Icons.Default.Menu, "Menu (Esc)") { state.menuOpen = true }
            }
            // Clock / weather / location chip
            Column(horizontalAlignment = Alignment.End) {
                HudChip(
                    text = "${session.clock.hourLabel()}  ${session.clock.phase.label}  ${weatherIcon(session.clock.phase, session.weather.current)}"
                )
                val region = session.world.region(state.worldState.regionId)
                HudChip(text = region?.nameJp ?: "—")
                if (session.settings.kidMode) HudChip("🧒 Kids mode")
            }
        }
        Box(Modifier.weight(1f)) {}
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = DsSpacing.Lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Interaction prompt (spec §19) — minimal, contextual.
            val target = session.currentInteractable
            if (target != null) {
                val closed = !session.isCurrentInteractableOpen()
                val showClosed = closed && session.settings.showHints
                Text(
                    text = "[E]  ${target.promptJp}   ·   ${target.promptEn}" +
                        if (showClosed) "   ·   closed 閉店" else "",
                    color = if (closed) Color(0xFF90CAF9).copy(alpha = 0.8f) else Color.White,
                    fontSize = DsType.Body,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(DsRadius.Md))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = DsSpacing.Lg, vertical = DsSpacing.Sm)
                )
            }
            // Current objective — one line, always honest.
            ObjectiveStrip(session)
        }
    }
}

@Composable
private fun ObjectiveStrip(session: GameSession) {
    val quest = session.quests.activeQuests().firstOrNull()
    if (quest == null) {
        // First-time guidance (spec §135): never leave a new player staring
        // at an empty world with no idea what to do — hidden when the player
        // turns hints off.
        if (session.settings.showHints) {
            Text(
                text = "Walk around · explore · press [E] near things — the town teaches you.",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = DsType.Label,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = DsSpacing.Sm)
                    .clip(RoundedCornerShape(DsRadius.Md))
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(horizontal = DsSpacing.Lg, vertical = DsSpacing.Sm)
            )
        }
        return
    }
    val progress = session.quests.progressFor(quest.id)
    val done = progress?.objectives?.count { it.complete } ?: 0
    val total = quest.objectives.size
    val next = nextObjective(session, quest)
    val objectiveText = next?.description ?: "Final step…"
    Text(
        text = "●  ${quest.title} — $objectiveText" +
            (if (total > 1) "   ($done/$total)" else ""),
        color = Color.White,
        fontSize = DsType.Body,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .padding(top = DsSpacing.Sm)
            .clip(RoundedCornerShape(DsRadius.Md))
            .background(Color.Black.copy(alpha = 0.45f))
            .padding(horizontal = DsSpacing.Lg, vertical = DsSpacing.Sm)
    )
}

private fun nextObjective(session: GameSession, quest: Quest): ua.syt0r.kanji.desktop.game.quest.QuestObjective? {
    val progress = session.quests.progressFor(quest.id) ?: return null
    return quest.objectives.firstOrNull { objective ->
        progress.objectives.firstOrNull { it.objectiveId == objective.id }?.complete != true
    }
}

@Composable
private fun HudIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val bg = if (hovered) Color.Black.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.25f)
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(DsRadius.Md))
            .background(bg)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .hoverable(interaction),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(17.dp))
    }
}

@Composable
private fun HudChip(text: String) {
    Text(
        text = text,
        color = Color.White,
        fontSize = DsType.Label,
        modifier = Modifier
            .padding(top = DsSpacing.Xs)
            .clip(RoundedCornerShape(DsRadius.Sm))
            .background(Color.Black.copy(alpha = 0.3f))
            .padding(horizontal = DsSpacing.Sm, vertical = 4.dp)
    )
}

private fun weatherIcon(phase: TimePhase, weather: ua.syt0r.kanji.desktop.game.time.WeatherKind): String = when (weather) {
    ua.syt0r.kanji.desktop.game.time.WeatherKind.Sun -> if (phase == TimePhase.Night) "☾" else "☀"
    ua.syt0r.kanji.desktop.game.time.WeatherKind.Cloud -> "☁"
    ua.syt0r.kanji.desktop.game.time.WeatherKind.Rain -> "☂"
    ua.syt0r.kanji.desktop.game.time.WeatherKind.Snow -> "❄"
}
