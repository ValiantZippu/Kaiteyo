@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package ua.syt0r.kanji.presentation.screen.main.screen.kanji_browser

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.Clock
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme
import ua.syt0r.kanji.presentation.common.theme.SurfaceColors
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState
import ua.syt0r.kanji.presentation.screen.main.features.KaiteyoDataCenter
import ua.syt0r.kanji.presentation.screen.main.features.KaiteyoCollection
import ua.syt0r.kanji.presentation.screen.main.screen.decks.CardFlagType
import ua.syt0r.kanji.presentation.screen.main.screen.decks.CardTag
import kotlin.time.Duration.Companion.hours

// ============================================
// COLLECTIONS
// Smart collections · tag collections ·
// flag collections · favorites
// ============================================

@Composable
fun CollectionsScreen(
    navigationState: MainNavigationState,
    dataCenter: KaiteyoDataCenter
) {
    var selectedCollection by remember { mutableStateOf<KaiteyoCollection?>(null) }

    if (selectedCollection == null) {
        CollectionsOverview(
            navigationState = navigationState,
            dataCenter = dataCenter,
            onOpenCollection = { selectedCollection = it }
        )
    } else {
        CollectionDetail(
            collection = selectedCollection!!,
            dataCenter = dataCenter,
            onBack = { selectedCollection = null },
            onOpenBrowser = { criteria ->
                navigationState.navigate(
                    ua.syt0r.kanji.presentation.screen.main.MainDestination.KanjiBrowser(criteria)
                )
            }
        )
    }
}

@Composable
private fun CollectionsOverview(
    navigationState: MainNavigationState,
    dataCenter: KaiteyoDataCenter,
    onOpenCollection: (KaiteyoCollection) -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconButton(onClick = { navigationState.navigateBack() }) {
                Icon(Icons.Default.ArrowBack, "Back", tint = surfaceColors.textSecondary)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Collections",
                    color = surfaceColors.textPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${dataCenter.cards.size} kanji available",
                    color = surfaceColors.textMuted,
                    fontSize = 12.sp
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "smart") {
                SectionTitle("Smart collections", accent, surfaceColors)
            }
            items(dataCenter.collections.filter { it.isSmart }, key = { it.id }) { collection ->
                CollectionCard(
                    collection = collection,
                    count = collection.cardIds.size,
                    onClick = { onOpenCollection(collection) },
                    accent = accent,
                    surfaceColors = surfaceColors
                )
            }

            item(key = "tags") {
                SectionTitle("By tag", accent, surfaceColors)
            }
            if (dataCenter.tags.isEmpty()) {
                item(key = "no-tags") {
                    Text(
                        text = "No tags yet — tag kanji from the browser to build collections.",
                        color = surfaceColors.textMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
            }
            dataCenter.tags.forEach { tag ->
                item(key = "tag-${tag.id}") {
                    val ids = dataCenter.cardTags.entries
                        .filter { (_, tagIds) -> tag.id in tagIds }
                        .map { (cardId, _) -> cardId }
                        .toSet()
                    TagCollectionCard(
                        tag = tag,
                        count = ids.size,
                        onClick = {
                            onOpenCollection(
                                KaiteyoCollection(
                                    id = "tag-${tag.id}",
                                    name = tag.name,
                                    icon = "🏷",
                                    isSmart = false,
                                    criteria = "",
                                    cardIds = ids
                                )
                            )
                        },
                        accent = accent,
                        surfaceColors = surfaceColors
                    )
                }
            }

            item(key = "flags") {
                SectionTitle("By flag", accent, surfaceColors)
            }
            CardFlagType.entries.filter { it != CardFlagType.None }.forEach { flag ->
                item(key = "flag-${flag.id}") {
                    val ids = dataCenter.cards
                        .filter { dataCenter.cardFlagsFor(it.id) == flag }
                        .map { it.id }
                        .toSet()
                    FlagCollectionCard(
                        flag = flag,
                        count = ids.size,
                        onClick = {
                            onOpenCollection(
                                KaiteyoCollection(
                                    id = "flag-${flag.id}",
                                    name = flag.displayName,
                                    icon = "🚩",
                                    isSmart = false,
                                    criteria = "",
                                    cardIds = ids
                                )
                            )
                        },
                        accent = accent,
                        surfaceColors = surfaceColors
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, accent: KaiteyoAccentScheme, surfaceColors: SurfaceColors) {
    Text(
        text = title,
        color = accent.primary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun CollectionCard(
    collection: KaiteyoCollection,
    count: Int,
    onClick: () -> Unit,
    accent: KaiteyoAccentScheme,
    surfaceColors: SurfaceColors
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (hovered) surfaceColors.surfaceInteractive else surfaceColors.surface)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .hoverable(interactionSource)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(collection.icon, fontSize = 20.sp)
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = collection.name,
                color = surfaceColors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = if (collection.isSmart) "Auto-generated" else "Custom",
                color = surfaceColors.textMuted,
                fontSize = 12.sp
            )
        }
        Text(
            text = "$count",
            color = accent.primary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun TagCollectionCard(
    tag: CardTag,
    count: Int,
    onClick: () -> Unit,
    accent: KaiteyoAccentScheme,
    surfaceColors: SurfaceColors
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val color = tag.getDisplayColor()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (hovered) surfaceColors.surfaceInteractive else surfaceColors.surface)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .hoverable(interactionSource)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.size(14.dp).clip(CircleShape).background(color))
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = tag.name,
                color = surfaceColors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Tag",
                color = surfaceColors.textMuted,
                fontSize = 12.sp
            )
        }
        Text(
            text = "$count",
            color = accent.primary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun FlagCollectionCard(
    flag: CardFlagType,
    count: Int,
    onClick: () -> Unit,
    accent: KaiteyoAccentScheme,
    surfaceColors: SurfaceColors
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val color = flag.colorFromHex()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (hovered) surfaceColors.surfaceInteractive else surfaceColors.surface)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .hoverable(interactionSource)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.size(14.dp).clip(CircleShape).background(color))
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = flag.displayName,
                color = surfaceColors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Flag",
                color = surfaceColors.textMuted,
                fontSize = 12.sp
            )
        }
        Text(
            text = "$count",
            color = accent.primary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun CollectionDetail(
    collection: KaiteyoCollection,
    dataCenter: KaiteyoDataCenter,
    onBack: () -> Unit,
    onOpenBrowser: (KanjiBrowserCriteria) -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val now = Clock.System.now()

    val cards = remember(collection, dataCenter.cards) {
        when {
            collection.isSmart && collection.name == "Recently learned" ->
                dataCenter.cards.filter { card ->
                    card.lastReviewed.isNotBlank() &&
                        runCatching { kotlinx.datetime.Instant.parse(card.lastReviewed) > now - 24.hours }
                            .getOrDefault(false)
                }
            collection.isSmart && collection.name == "Needs review" ->
                dataCenter.cards.filter { card ->
                    val srs = dataCenter.srsCards[card.id] ?: return@filter false
                    val last = srs.lastReview ?: return@filter false
                    last + srs.interval <= now
                }
            collection.isSmart && collection.name == "Frequently failed" ->
                dataCenter.cards.filter { (dataCenter.srsCards[it.id]?.lapses ?: 0) >= 3 }
            collection.isSmart && collection.name == "Not studied in 30 days" ->
                dataCenter.cards.filter { dataCenter.notReviewedFor(it.id, 30) }
            collection.isSmart && collection.name == "Flagged" ->
                dataCenter.cards.filter { dataCenter.cardFlagsFor(it.id) != CardFlagType.None }
            collection.isSmart && collection.name == "Favorites" ->
                dataCenter.cards.filter { dataCenter.isFavorite(it.id) }
            else -> dataCenter.cards.filter { it.id in collection.cardIds }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back", tint = surfaceColors.textSecondary)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = "${collection.icon} ${collection.name}",
                    color = surfaceColors.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${cards.size} kanji",
                    color = surfaceColors.textMuted,
                    fontSize = 12.sp
                )
            }
            TextButton(
                onClick = {
                    onOpenBrowser(
                        KanjiBrowserCriteria(
                            favoritesOnly = collection.name == "Favorites",
                            showFlagged = collection.name == "Flagged",
                            minLapses = if (collection.name == "Frequently failed") 3 else null,
                            notReviewedDaysAgo = if (collection.name == "Not studied in 30 days") 30 else null,
                            sortBy = if (collection.name == "Recently learned" || collection.name == "Needs review")
                                KanjiBrowserSort.LastReviewed else KanjiBrowserSort.Frequency
                        )
                    )
                }
            ) {
                Text("Open in browser", color = accent.primary, fontSize = 13.sp)
            }
        }

        if (cards.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("This collection is empty", color = surfaceColors.textMuted)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(88.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(cards, key = { it.id }) { card ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(surfaceColors.surface)
                            .border(1.dp, Color.Transparent, RoundedCornerShape(12.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = card.character,
                                fontSize = 30.sp,
                                color = surfaceColors.textPrimary
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                card.flag.takeIf { it != CardFlagType.None }?.let { flag ->
                                    Box(Modifier.size(6.dp).clip(CircleShape).background(flag.colorFromHex()))
                                }
                                if (card.isFavorite) {
                                    Text("★", color = accent.secondary, fontSize = 10.sp)
                                }
                                dataCenter.classifications[card.id].orEmpty()
                                    .firstOrNull { it.startsWith("n") }
                                    ?.let { Text(it.uppercase(), color = surfaceColors.textMuted, fontSize = 9.sp) }
                            }
                        }
                    }
                }
            }
        }
    }
}
