package ua.syt0r.kanji.desktop.game.ui.panels

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ua.syt0r.kanji.desktop.designsystem.DsRadius
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.game.GameSession
import ua.syt0r.kanji.desktop.game.learning.AssistanceLevel

/**
 * NPC dialogue (spec §31): Japanese first, reading + translation adapt to the
 * player's assistance level — translation is never shown by default at higher
 * levels, so the world nudges toward understanding Japanese directly (§110).
 */
@Composable
fun DialoguePanel(session: GameSession) {
    if (!session.state.dialogueOpen) return
    val runner = session.dialogue
    val rawLine = runner.currentLine ?: run {
        // Nothing left to say — close.
        androidx.compose.runtime.LaunchedEffect(Unit) {
            session.state.dialogueOpen = false
            runner.end()
        }
        return
    }
    // Kids mode (spec §68): swap in the simpler authored text (kidJp/kidReading)
    // and pin the effective level — reading + translation always show there.
    val line = if (session.settings.kidMode) rawLine.withKidText() else rawLine
    val level = session.effectiveAssistance
    val textScale = session.settings.textSizeScale
    // Knowledge-gated choices (spec §13): a choice whose word isn't
    // discovered yet simply doesn't appear — with original indices intact.
    val choices = runner.availableChoices { session.learning.isDiscovered(it) }

    // Spoken dialogue (spec §61-62): play each new line automatically when
    // the voice is on; the replay button always works.
    androidx.compose.runtime.LaunchedEffect(line.id, session.settings.dialogueTtsEnabled) {
        if (session.settings.dialogueTtsEnabled && line.reading.isNotBlank()) {
            session.tts.speak(line)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = DsSpacing.Xl),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 760.dp)
                .clip(RoundedCornerShape(DsRadius.Lg))
                .background(Color.Black.copy(alpha = 0.78f))
                .clickable {
                    if (choices.isEmpty()) runner.advance()
                }
                .padding(DsSpacing.Lg),
            verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
        ) {
            if (line.speakerName.isNotBlank()) {
                Text(
                    text = line.speakerName,
                    color = Color(0xFFFFD54F),
                    fontSize = DsType.Label * textScale,
                    fontWeight = FontWeight.SemiBold
                )
            }
            // Japanese — always first, always primary.
            Text(
                text = line.jp,
                color = Color.White,
                fontSize = DsType.BodyLarge * textScale,
                fontWeight = FontWeight.Medium
            )
            // Reading — Normal and above.
            if (line.reading.isNotBlank() && level != AssistanceLevel.Minimal) {
                Text(
                    text = line.reading,
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = DsType.Body * textScale
                )
            }
            // Translation — only where the level allows it.
            if (line.translation.isNotBlank() && showTranslation(level)) {
                Text(
                    text = line.translation,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = DsType.Body * textScale
                )
            }
            // Choices (spec §55, §13): rendered in their authored order with
            // their original indices; gated ones wait until the word is
            // discovered. An all-hidden branch shows a Continue instead of
            // dead-ending (the runner auto-skips it on advance).
            if (choices.isNotEmpty()) {
                choices.forEach { (index, choice) ->
                    ChoiceButton(
                        text = choice.text.ifBlank { choice.textJp },
                        onClick = { runner.choose(index) },
                        textScale = textScale
                    )
                }
            } else if (rawLine.options.isNotEmpty()) {
                ChoiceButton(text = "Continue…", onClick = { runner.advance() }, textScale = textScale)
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Replay the line (works even when auto-play is off).
                    val speaking = session.tts.speaking
                    Text(
                        text = if (speaking) "♪ …" else "♪",
                        color = if (speaking) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.55f),
                        fontSize = DsType.Body * textScale,
                        modifier = Modifier
                            .clip(RoundedCornerShape(DsRadius.Sm))
                            .clickable {
                                if (session.settings.dialogueTtsEnabled && line.reading.isNotBlank()) {
                                    session.tts.speak(line)
                                }
                            }
                            .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Xs)
                    )
                    Text(
                        text = "▼",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = DsType.Caption
                    )
                }
            }
        }
    }
}

@Composable
private fun ChoiceButton(text: String, onClick: () -> Unit, textScale: Float) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Text(
        text = "▸ $text",
        color = if (hovered) Color(0xFFFFD54F) else Color.White,
        fontSize = DsType.Body * textScale,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DsRadius.Sm))
            .background(if (hovered) Color.White.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .hoverable(interaction)
            .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Xs)
    )
}

private fun showTranslation(level: AssistanceLevel): Boolean = when (level) {
    AssistanceLevel.Minimal -> false
    AssistanceLevel.Normal -> false
    AssistanceLevel.Guided -> true
    AssistanceLevel.Kids -> true
}
