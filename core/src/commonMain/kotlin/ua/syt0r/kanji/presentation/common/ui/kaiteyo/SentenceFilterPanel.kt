package ua.syt0r.kanji.presentation.common.ui.kaiteyo

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.theme.SurfaceColors

// ============================================================
// DETAILED SENTENCE FILTER PANEL
//
// A richer filter panel than the one in KaiteyoSentenceCards.
// Uses the same SentenceDifficulty / SentenceFilterState types
// defined in KaiteyoSentenceCards.kt to avoid redeclaration.
// ============================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailedSentenceFilterPanel(
    state: SentenceFilterState,
    onStateChange: (SentenceFilterState) -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(surfaceColors.surfaceElevated.copy(alpha = 0.92f))
            .border(1.dp, surfaceColors.border.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Sentence Filters",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = surfaceColors.textPrimary
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "Tap to apply",
                fontSize = 10.sp,
                color = surfaceColors.textMuted
            )
        }

        // Difficulty chips — use ordinal for ordering since KaiteyoSentenceCards
        // SentenceDifficulty doesn't have an explicit level field.
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Difficulty",
                    fontSize = 13.sp,
                    color = surfaceColors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                val selectedLabel = state.selectedDifficulty?.label ?: "All"
                Text(
                    text = selectedLabel,
                    fontSize = 13.sp,
                    color = accent.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SentenceDifficulty.entries.forEach { difficulty ->
                    val selected = state.selectedDifficulty == difficulty
                    DifficultyChip(
                        label = difficulty.label,
                        color = Color(difficulty.color),
                        selected = selected,
                        onClick = {
                            onStateChange(state.copy(
                                selectedDifficulty = if (selected) null else difficulty
                            ))
                        },
                        accent = accent.primary,
                        surfaceColors = surfaceColors
                    )
                }
            }
        }

        // AI-generated toggle
        FilterToggleRow(
            label = "Show AI-generated sentences",
            description = "Include sentences generated by AI",
            checked = state.showAiGenerated,
            onCheckedChange = { onStateChange(state.copy(showAiGenerated = it)) },
            icon = Icons.Default.AutoAwesome,
            surfaceColors = surfaceColors,
            accent = accent.primary
        )

        // NSFW toggle
        FilterToggleRow(
            label = "Show NSFW sentences",
            description = "Might contain violent or sexual content",
            checked = state.showNsfw,
            onCheckedChange = { onStateChange(state.copy(showNsfw = it)) },
            surfaceColors = surfaceColors,
            accent = accent.primary
        )

        // Translation language
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Translation language",
                    fontSize = 13.sp,
                    color = surfaceColors.textPrimary
                )
                Text(
                    text = state.translationLanguage,
                    fontSize = 11.sp,
                    color = surfaceColors.textMuted
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("English", "日本語", "한국어").forEach { lang ->
                    val selected = state.translationLanguage == lang
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (selected) accent.primary.copy(alpha = 0.15f)
                                else surfaceColors.surfaceInteractive
                            )
                            .border(
                                1.dp,
                                if (selected) accent.primary.copy(alpha = 0.4f) else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { onStateChange(state.copy(translationLanguage = lang)) }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = lang,
                            fontSize = 11.sp,
                            color = if (selected) accent.primary else surfaceColors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DifficultyChip(
    label: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    accent: Color,
    surfaceColors: SurfaceColors
) {
    val bg by animateColorAsState(
        targetValue = if (selected) color.copy(alpha = 0.15f) else surfaceColors.surfaceInteractive,
        label = "diffBg"
    )
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(
                1.dp,
                if (selected) color.copy(alpha = 0.4f) else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = if (selected) color else surfaceColors.textSecondary,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
private fun FilterToggleRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    surfaceColors: SurfaceColors,
    accent: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 13.sp,
                color = surfaceColors.textPrimary
            )
            Text(
                text = description,
                fontSize = 11.sp,
                color = surfaceColors.textMuted
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = accent,
                checkedTrackColor = accent.copy(alpha = 0.3f),
                uncheckedThumbColor = surfaceColors.textMuted,
                uncheckedTrackColor = surfaceColors.surfaceInteractive
            )
        )
    }
}
