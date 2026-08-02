package ua.syt0r.kanji.desktop.ui.activity

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import ua.syt0r.kanji.desktop.designsystem.DsChip
import ua.syt0r.kanji.desktop.designsystem.DsEmptyState
import ua.syt0r.kanji.desktop.designsystem.DsSearchField
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import kotlinx.datetime.toLocalDateTime
import ua.syt0r.kanji.desktop.engine.history.ActivityCategory
import ua.syt0r.kanji.desktop.engine.history.ActivityFormatters

// ============================================
// ACTIVITY LOG
// Every event the app records — reviews, imports,
// undos, tag edits, syncs, theme changes — with
// category filters and search.
// ============================================

@Composable
fun ActivityLogView(state: AppState) {
    val sc = surfaceColors()
    var filter by remember { mutableStateOf<ActivityCategory?>(null) }
    var query by remember { mutableStateOf("") }

    val entries = remember(state.activityLog.entries, filter, query) {
        state.activityLog.filter(filter, query.ifBlank { null })
    }
    val summary = state.activityLog.summary()

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DsSpacing.Lg, vertical = DsSpacing.Md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
        ) {
            DsSearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Search activity…",
                modifier = Modifier.weight(1f)
            )
            DsBadge(text = "${entries.size} events", tint = Color(0xFF7BC8FF))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DsSpacing.Lg),
            horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs)
        ) {
            DsChip(text = "All", selected = filter == null, onClick = { filter = null })
            ActivityCategory.entries.forEach { category ->
                if (summary.byCategory[category] != null) {
                    DsChip(
                        text = category.name,
                        selected = filter == category,
                        onClick = { filter = if (filter == category) null else category },
                        trailing = summary.byCategory[category].toString()
                    )
                }
            }
        }

        Spacer(Modifier.height(DsSpacing.Sm))

        if (entries.isEmpty()) {
            DsEmptyState(
                title = "No activity",
                message = "Everything you do in Kaiteyo is logged here.",
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(DsSpacing.Md)
            ) {
                items(entries, key = { it.id }) { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(categoryColor(entry.category))
                        )
                        Spacer(Modifier.width(DsSpacing.Md))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = entry.summary,
                                color = sc.textPrimary,
                                fontSize = DsType.Body,
                                fontWeight = FontWeight.Medium
                            )
                            if (entry.details.isNotBlank()) {
                                Text(
                                    text = entry.details,
                                    color = sc.textMuted,
                                    fontSize = DsType.Caption
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = ActivityFormatters.relative(entry.timestamp),
                                color = sc.textMuted,
                                fontSize = DsType.Caption
                            )
                            Text(
                                text = entry.timestamp.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).toString().substringBefore("T"),
                                color = sc.textMuted.copy(alpha = 0.6f),
                                fontSize = DsType.Caption
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun categoryColor(category: ActivityCategory): Color = when (category) {
    ActivityCategory.Review -> Color(0xFFC2FC8B)
    ActivityCategory.Study -> Color(0xFF7BC8FF)
    ActivityCategory.Import -> Color(0xFFFEAB57)
    ActivityCategory.Export -> Color(0xFFFFD93D)
    ActivityCategory.Undo -> Color(0xFFA78BFA)
    ActivityCategory.Tag -> Color(0xFF8CF0C8)
    ActivityCategory.Flag -> Color(0xFFFF8C8C)
    ActivityCategory.Favorite -> Color(0xFFFFD93D)
    ActivityCategory.Deck -> Color(0xFF9FE2FF)
    ActivityCategory.Sync -> Color(0xFF00D4AA)
    ActivityCategory.Plugin -> Color(0xFFF0A8D0)
    ActivityCategory.Settings -> Color(0xFFBDBDBD)
    ActivityCategory.Theme -> Color(0xFFA0D2FF)
    ActivityCategory.Note -> Color(0xFFD4C48B)
    ActivityCategory.System -> Color(0xFF606060)
}
