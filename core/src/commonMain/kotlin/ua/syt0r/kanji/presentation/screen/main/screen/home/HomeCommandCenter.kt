@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package ua.syt0r.kanji.presentation.screen.main.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.Clock
import org.koin.compose.koinInject
import ua.syt0r.kanji.core.knowledge.home.HomeCommandCenterData
import ua.syt0r.kanji.core.knowledge.home.HomeCommandCenterStore
import ua.syt0r.kanji.core.knowledge.home.RecentEntryKind
import ua.syt0r.kanji.presentation.common.theme.Dimens
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.screen.main.features.KaiteyoSearch
import ua.syt0r.kanji.presentation.common.ui.KaiteyoPill
import ua.syt0r.kanji.presentation.common.ui.KaiteyoSectionCard
import ua.syt0r.kanji.presentation.common.ui.KaiteyoTag

// ============================================================
// HOME COMMAND CENTER (spec §31)
// ------------------------------------------------------------
// \"What should I do now?\" — the top of Home. Real data only:
//   - Quick Search   opens the universal search (Ctrl+Shift+F)
//   - Recent searches (persisted by the universal search controller)
//   - Recent entries  (kanji / word pages you actually opened)
//   - Discover        (Browse / Radicals / Components / Collections)
// Nothing here is a dead control: every row navigates or searches.
// ============================================================

/**
 * Store-backed wrapper: loads the persisted command-center data and renders
 * [HomeCommandCenter]. Used by the General dashboard so Home's "Recent
 * searches / Recent entries" reflect the real, persisted history.
 */
@Composable
fun HomeCommandCenterSection(
    onOpenKanji: (String) -> Unit = {},
    onOpenWord: (Long) -> Unit = {},
    onOpenBrowse: () -> Unit = {},
    onOpenRadicals: () -> Unit = {},
    onOpenComponents: () -> Unit = {},
    onOpenCollections: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val store = koinInject<HomeCommandCenterStore>()
    var data by remember { mutableStateOf<HomeCommandCenterData?>(null) }
    LaunchedEffect(Unit) { data = store.load() }
    val current = data ?: return
    HomeCommandCenter(
        data = current,
        onOpenKanji = onOpenKanji,
        onOpenWord = onOpenWord,
        onOpenBrowse = onOpenBrowse,
        onOpenRadicals = onOpenRadicals,
        onOpenComponents = onOpenComponents,
        onOpenCollections = onOpenCollections,
        modifier = modifier
    )
}

@Composable
fun HomeCommandCenter(
    data: HomeCommandCenterData,
    onOpenUniversalSearch: () -> Unit = { KaiteyoSearch.controller.open() },
    onSearch: (String) -> Unit = { query ->
        KaiteyoSearch.controller.open()
        KaiteyoSearch.controller.updateQuery(query)
    },
    onOpenKanji: (String) -> Unit,
    onOpenWord: (Long) -> Unit,
    onOpenBrowse: () -> Unit,
    onOpenRadicals: () -> Unit,
    onOpenComponents: () -> Unit,
    onOpenCollections: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimens.Space3)
    ) {
        // ── Quick search ─────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Dimens.RadiusLg))
                .background(surfaceColors.surface)
                .clickable(onClick = onOpenUniversalSearch)
                .padding(horizontal = Dimens.Space4, vertical = Dimens.Space3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.Space3)
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = accent.primary, modifier = Modifier.size(20.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Quick search",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = surfaceColors.textPrimary
                )
                Text(
                    text = "Kanji, words, sentences, grammar — Ctrl+Shift+F",
                    style = MaterialTheme.typography.labelSmall,
                    color = surfaceColors.textMuted
                )
            }
            KaiteyoTag(text = "Everywhere", tint = accent.secondary)
        }

        // ── Recent searches ──────────────────────────────────────────
        if (data.recentSearches.isNotEmpty()) {
            KaiteyoSectionCard(title = "Recent searches", icon = Icons.Default.History) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.Space2),
                    verticalArrangement = Arrangement.spacedBy(Dimens.Space2)
                ) {
                    data.recentSearches.forEach { search ->
                        KaiteyoPill(
                            text = search.query,
                            selected = false,
                            onClick = { onSearch(search.query) }
                        )
                    }
                }
            }
        }

        // ── Recent entries ───────────────────────────────────────────
        if (data.recentEntries.isNotEmpty()) {
            KaiteyoSectionCard(title = "Recent entries", subtitle = "Pages you opened") {
                data.recentEntries.take(5).forEach { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Dimens.RadiusSm))
                            .clickable {
                                when (entry.kind) {
                                    RecentEntryKind.Kanji -> onOpenKanji(entry.ref)
                                    RecentEntryKind.Word -> onOpenWord(entry.ref.toLongOrNull() ?: return@clickable)
                                }
                            }
                            .padding(horizontal = Dimens.Space2, vertical = Dimens.Space2),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.Space3)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(Dimens.RadiusSm))
                                .background(
                                    if (entry.kind == RecentEntryKind.Kanji) accent.primary.copy(alpha = 0.14f)
                                    else accent.secondary.copy(alpha = 0.14f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = entry.label.take(1),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (entry.kind == RecentEntryKind.Kanji) accent.primary else accent.secondary
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(entry.label, style = MaterialTheme.typography.bodyMedium, color = surfaceColors.textPrimary, fontWeight = FontWeight.Medium)
                            entry.subtitle?.let {
                                Text(it, style = MaterialTheme.typography.labelSmall, color = surfaceColors.textMuted)
                            }
                        }
                        Text(
                            text = relativeTime(entry.recordedAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = surfaceColors.textMuted
                        )
                    }
                }
            }
        }

        // ── Discover ─────────────────────────────────────────────────
        KaiteyoSectionCard(title = "Discover", subtitle = "Explore the language") {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.Space2),
                verticalArrangement = Arrangement.spacedBy(Dimens.Space2)
            ) {
                KaiteyoPill(text = "Browse", selected = false, onClick = onOpenBrowse)
                KaiteyoPill(text = "Radicals", selected = false, onClick = onOpenRadicals)
                KaiteyoPill(text = "Components", selected = false, onClick = onOpenComponents)
                KaiteyoPill(text = "Collections", selected = false, onClick = onOpenCollections)
            }
        }
    }
}

private fun relativeTime(epochMillis: Long): String {
    val now = Clock.System.now().toEpochMilliseconds()
    val diffMinutes = (now - epochMillis) / 60_000
    return when {
        diffMinutes < 1 -> "now"
        diffMinutes < 60 -> "${diffMinutes}m"
        diffMinutes < 60 * 24 -> "${diffMinutes / 60}h"
        else -> "${diffMinutes / (60 * 24)}d"
    }
}
