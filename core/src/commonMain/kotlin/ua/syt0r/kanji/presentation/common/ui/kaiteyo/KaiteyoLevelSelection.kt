package ua.syt0r.kanji.presentation.common.ui.kaiteyo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors

// ============================================================
// LEVEL SELECTION DIALOG — Kaiteyo-style
//
// Novice / Beginner / Intermediate / Advanced / Expert
// Each level has a description, gradient color, and expandable detail.
// ============================================================

enum class JapaneseLevel(
    val label: String,
    val emoji: String,
    val description: List<String>,
    val gradientColors: List<Color>
) {
    Novice(
        label = "NOVICE",
        emoji = "📚",
        description = listOf(
            "I know fewer than 300 kanji.",
            "I'm just starting to learn Japanese.",
            "I know basic hiragana and katakana.",
            "My level is equivalent to JLPT N5 or below."
        ),
        gradientColors = listOf(Color(0xFF6B5B95), Color(0xFF8B7DB8))
    ),
    Beginner(
        label = "BEGINNER",
        emoji = "📗",
        description = listOf(
            "I know between 100 and 300 kanji.",
            "I can read simple texts with furigana.",
            "I've studied basic grammar (particles, verb conjugation).",
            "My level is equivalent to JLPT N5–N4."
        ),
        gradientColors = listOf(Color(0xFF3F51B5), Color(0xFF5C6BC0))
    ),
    Intermediate(
        label = "INTERMEDIATE",
        emoji = "📘",
        description = listOf(
            "I know between 300 and 1000 kanji.",
            "I can read texts simplified for non-natives.",
            "I've studied between 600 and 1500 hours.",
            "My level is equivalent to JLPT N3–N2, CEFR B1–B2."
        ),
        gradientColors = listOf(Color(0xFF7B1FA2), Color(0xFF9C27B0))
    ),
    Advanced(
        label = "ADVANCED",
        emoji = "📕",
        description = listOf(
            "I know between 1000 and 2000 kanji.",
            "I can read native Japanese content with a dictionary.",
            "I've studied over 2000 hours.",
            "My level is equivalent to JLPT N2–N1, CEFR B2–C1."
        ),
        gradientColors = listOf(Color(0xFFC62828), Color(0xFFE53935))
    ),
    Expert(
        label = "EXPERT",
        emoji = "🎓",
        description = listOf(
            "I know over 2000 kanji.",
            "I can read native Japanese fluently.",
            "I've lived in Japan or studied for many years.",
            "My level is equivalent to JLPT N1, CEFR C1–C2."
        ),
        gradientColors = listOf(Color(0xFF4A148C), Color(0xFF880E4F))
    )
}

@Composable
fun LevelSelectionDialog(
    currentLevel: JapaneseLevel?,
    onSelectLevel: (JapaneseLevel) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    var expandedLevel by remember { mutableStateOf(currentLevel) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(surfaceColors.surface)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Title
        Text(
            text = "Japanese level",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = surfaceColors.textPrimary
        )

        Text(
            text = "Select your level to customize the app experience.",
            fontSize = 12.sp,
            color = surfaceColors.textMuted
        )

        Spacer(Modifier.height(4.dp))

        // Level cards
        JapaneseLevel.entries.forEach { level ->
            LevelCard(
                level = level,
                isSelected = currentLevel == level,
                isExpanded = expandedLevel == level,
                onToggleExpand = {
                    expandedLevel = if (expandedLevel == level) null else level
                },
                onSelect = { onSelectLevel(level) }
            )
        }
    }
}

@Composable
private fun LevelCard(
    level: JapaneseLevel,
    isSelected: Boolean,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onSelect: () -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val scale by animateFloatAsState(
        targetValue = if (hovered) 1.01f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "cardScale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isSelected) level.gradientColors[0].copy(alpha = 0.14f)
                else surfaceColors.surfaceInteractive.copy(alpha = 0.3f)
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) level.gradientColors[0].copy(alpha = 0.5f)
                else surfaceColors.surfaceInteractive.copy(alpha = 0.5f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onToggleExpand
            )
    ) {
        // Header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(level.emoji, fontSize = 18.sp)
            Spacer(Modifier.width(10.dp))
            Text(
                text = level.label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) level.gradientColors[1] else surfaceColors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            Icon(
                if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = surfaceColors.textMuted
            )
        }

        // Expanded content
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(spring(stiffness = Spring.StiffnessLow)) + fadeIn(),
            exit = shrinkVertically(tween(150)) + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                level.description.forEach { point ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("•", fontSize = 12.sp, color = level.gradientColors[1])
                        Text(
                            text = point,
                            fontSize = 11.sp,
                            color = surfaceColors.textSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Select button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(level.gradientColors[0].copy(alpha = 0.14f))
                        .border(1.dp, level.gradientColors[0].copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .clickable(onClick = onSelect)
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "I'm ${level.label.lowercase().replaceFirstChar { it.uppercase() }}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = level.gradientColors[1]
                    )
                }
            }
        }
    }
}

// ============================================================
// ANIMATED CARD ENTRANCE — reusable spring animation
// ============================================================

@Composable
fun AnimatedEntrance(
    index: Int,
    content: @Composable () -> Unit
) {
    val scale = remember { Animatable(0.92f) }
    val alpha = remember { Animatable(0f) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 50L)
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        alpha.animateTo(1f, animationSpec = tween(200))
    }

    Box(
        modifier = Modifier.graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
            this.alpha = alpha.value
        }
    ) {
        content()
    }
}
