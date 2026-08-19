package ua.syt0r.kanji.presentation.common.ui.kaiteyo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.unit.sp
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.theme.SurfaceColors

// ============================================================
// EDITABLE WORD CARDS
//
// Inspired by Kaiteyo's Edit Word Cards screen. Shows a
// list of card sections (kanji list, readings, meanings,
// compounds, sentences, etc.) with visibility toggles and
// drag-reorder handles. Users can show/hide and reorder
// each section to customize their word entry view.
// ============================================================

/**
 * A single card section that can be shown/hidden and reordered.
 */
data class WordCardSection(
    val id: String,
    val title: String,
    val visible: Boolean = true,
    val content: @Composable () -> Unit = {}
)

/**
 * The word card configuration — an ordered list of sections.
 */
data class WordCardConfig(
    val sections: List<WordCardSection>
)

@Composable
fun EditableWordCardEditor(
    config: WordCardConfig,
    onConfigChange: (WordCardConfig) -> Unit,
    previewContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current

    Column(modifier = modifier) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Edit Word Cards",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = surfaceColors.textPrimary
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "Drag to reorder · Toggle visibility",
                fontSize = 10.sp,
                color = surfaceColors.textMuted
            )
        }

        // Preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(surfaceColors.surface)
                .padding(16.dp)
        ) {
            previewContent()
        }

        Spacer(Modifier.height(12.dp))

        // Section list with drag handles and visibility toggles
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(
                items = config.sections,
                key = { _, section -> section.id }
            ) { index, section ->
                SectionRow(
                    section = section,
                    onToggleVisibility = { visible ->
                        val updated = config.sections.toMutableList()
                        updated[index] = section.copy(visible = visible)
                        onConfigChange(config.copy(sections = updated))
                    },
                    surfaceColors = surfaceColors,
                    accent = accent.primary
                )
            }
        }
    }
}

@Composable
private fun SectionRow(
    section: WordCardSection,
    onToggleVisibility: (Boolean) -> Unit,
    surfaceColors: SurfaceColors,
    accent: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(surfaceColors.surface)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Drag handle
        Icon(
            imageVector = Icons.Default.DragHandle,
            contentDescription = "Drag to reorder",
            tint = surfaceColors.textMuted,
            modifier = Modifier.size(18.dp)
        )

        Spacer(Modifier.width(10.dp))

        // Section title
        Text(
            text = section.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = if (section.visible) surfaceColors.textPrimary else surfaceColors.textMuted,
            modifier = Modifier.weight(1f)
        )

        // Visibility toggle
        Icon(
            imageVector = if (section.visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
            contentDescription = if (section.visible) "Hide section" else "Show section",
            tint = if (section.visible) accent else surfaceColors.textMuted,
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .clickable { onToggleVisibility(!section.visible) }
                .padding(2.dp)
        )
    }
}

/**
 * Renders a word card with only the visible sections, in order.
 * Used as the preview inside the editor and as the actual
 * card renderer on the word detail page.
 */
@Composable
fun RenderedWordCard(
    config: WordCardConfig,
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(surfaceColors.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        config.sections.filter { it.visible }.forEach { section ->
            Column {
                Text(
                    text = section.title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = surfaceColors.textMuted
                )
                Spacer(Modifier.height(4.dp))
                section.content()
            }
        }
    }
}

/**
 * Default word card configuration for a kanji/word entry.
 */
fun defaultWordCardConfig(
    kanjiList: List<Pair<String, String>> = emptyList(),
    readings: List<String> = emptyList(),
    meanings: List<String> = emptyList(),
    compounds: List<String> = emptyList()
): WordCardConfig {
    return WordCardConfig(
        sections = listOf(
            WordCardSection(
                id = "kanji_list",
                title = "Kanji List",
                visible = kanjiList.isNotEmpty(),
                content = {
                    val sc = LocalSurfaceColors.current
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        kanjiList.forEach { (char, meaning) ->
                            KaiteyoBadge(
                                text = "$char $meaning",
                                containerColor = Color(0xFF7BC8FF).copy(alpha = 0.15f),
                                contentColor = Color(0xFF7BC8FF)
                            )
                        }
                    }
                }
            ),
            WordCardSection(
                id = "readings",
                title = "Readings",
                visible = readings.isNotEmpty(),
                content = {
                    val sc = LocalSurfaceColors.current
                    readings.forEach { reading ->
                        Text(
                            text = reading,
                            fontSize = 14.sp,
                            color = sc.textPrimary
                        )
                    }
                }
            ),
            WordCardSection(
                id = "meanings",
                title = "JMdict",
                visible = meanings.isNotEmpty(),
                content = {
                    val sc = LocalSurfaceColors.current
                    meanings.forEach { meaning ->
                        Text(
                            text = meaning,
                            fontSize = 13.sp,
                            color = sc.textSecondary
                        )
                    }
                }
            ),
            WordCardSection(
                id = "compounds",
                title = "Compounds",
                visible = compounds.isNotEmpty(),
                content = {
                    val sc = LocalSurfaceColors.current
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Words starting with ${compounds.firstOrNull() ?: ""}", fontSize = 12.sp, color = sc.textMuted)
                        Text("Words containing ${compounds.firstOrNull() ?: ""}", fontSize = 12.sp, color = sc.textMuted)
                    }
                }
            )
        )
    )
}
