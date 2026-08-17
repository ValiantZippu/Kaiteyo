package ua.syt0r.kanji.desktop.game.ui.menus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ua.syt0r.kanji.desktop.designsystem.DsButton
import ua.syt0r.kanji.desktop.designsystem.DsButtonKind
import ua.syt0r.kanji.desktop.designsystem.DsRadius
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.game.GameSession
import ua.syt0r.kanji.desktop.game.story.Story

/**
 * Story menu (spec §54-55): small stories (a summer trip, a missing cat, a
 * festival) told as chapters → scenes → dialogue → quests. The engine drives
 * scene effects; this panel lists the stories and continues the active one.
 */
@Composable
fun StoryView(session: GameSession) {
    val storyEngine = session.story
    val activeStory = storyEngine.activeStory
    val progress = storyEngine.progress.value

    Column(verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
        Text(
            text = "Stories",
            color = Color(0xFFFFD54F),
            fontSize = DsType.BodyLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Small stories unfold as you explore — chapters, scenes and quests.",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = DsType.Caption
        )

        activeStory?.let { story ->
            ActiveStoryCard(session, story)
        }

        session.content.stories.forEach { story ->
            StoryCard(session, story, progress.any { it.storyId == story.id && it.completed })
        }
    }
}

@Composable
private fun ActiveStoryCard(session: GameSession, story: Story) {
    val engine = session.story
    val chapter = engine.activeChapter
    val scene = engine.activeScene
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DsRadius.Md))
            .background(Color(0xFFFFD54F).copy(alpha = 0.12f))
            .padding(DsSpacing.Md),
        verticalArrangement = Arrangement.spacedBy(DsSpacing.Xs)
    ) {
        Text(
            text = "Now playing — ${story.title}",
            color = Color.White,
            fontSize = DsType.Body,
            fontWeight = FontWeight.SemiBold
        )
        if (chapter != null && scene != null) {
            val index = story.chapters.indexOf(chapter)
            Text(
                text = "Chapter ${index + 1}: ${chapter.title}",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = DsType.Caption
            )
        }
        // A scene with options is a branch (spec §55): the story waits for
        // the player instead of advancing — each choice leads somewhere real.
        if (scene?.options?.isNotEmpty() == true) {
            Column(verticalArrangement = Arrangement.spacedBy(DsSpacing.Xs)) {
                Text(
                    text = "How does the night end?",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = DsType.Caption
                )
                scene.options.forEachIndexed { index, choice ->
                    DsButton(
                        text = "${choice.textJp.ifBlank { choice.text }}  ·  ${choice.text}",
                        kind = DsButtonKind.AccentTint,
                        onClick = { session.chooseStory(index) }
                    )
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                DsButton(
                    text = "Continue",
                    kind = DsButtonKind.Primary,
                    onClick = { session.advanceStory() }
                )
                DsButton(
                    text = "Close",
                    kind = DsButtonKind.Ghost,
                    onClick = { session.state.closeAllPanels() }
                )
            }
        }
    }
}

@Composable
private fun StoryCard(session: GameSession, story: Story, completed: Boolean) {
    val progress = session.story.progress.value
    val entry = progress.firstOrNull { it.storyId == story.id }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DsRadius.Md))
            .background(Color.White.copy(alpha = if (completed) 0.06f else 0.03f))
            .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(DsSpacing.Xs)) {
            Text(
                text = "${story.title}  ${story.titleJp}",
                color = if (completed) Color(0xFFA5D6A7) else Color.White,
                fontSize = DsType.Body,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = story.description,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = DsType.Caption
            )
            Text(
                text = when {
                    completed -> "Completed ✓"
                    entry != null -> "${story.chapters.size} chapters — in progress"
                    else -> "${story.chapters.size} chapters"
                },
                color = Color.White.copy(alpha = 0.45f),
                fontSize = DsType.Caption
            )
        }
        if (!completed) {
            DsButton(
                text = if (entry != null) "Continue" else "Start",
                kind = if (entry != null) DsButtonKind.Primary else DsButtonKind.Ghost,
                onClick = {
                    if (entry != null) session.advanceStory() else session.startStory(story.id)
                }
            )
        }
    }
}
