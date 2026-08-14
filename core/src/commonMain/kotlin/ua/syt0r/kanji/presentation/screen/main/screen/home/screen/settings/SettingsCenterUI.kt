package ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ua.syt0r.kanji.presentation.common.MultiplatformDialog
import ua.syt0r.kanji.presentation.common.resources.string.resolveString
import ua.syt0r.kanji.presentation.common.theme.Dimens
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.components.DangerActionSetting
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.components.SettingGroup
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.components.SettingHeader
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.components.SettingsSearchField
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.components.scaledRadius

// ============================================
// KAITEYO SETTINGS CENTER
// Desktop / tablet: category rail + content.
// Phone: search + category chips + content.
// Instant search across every setting.
// ============================================

private val TwoPaneMinWidth = 720.dp

@Composable
fun SettingsCenterShell(
    categories: List<SettingsScreenContract.Category>,
    mainNavigationState: MainNavigationState
) {
    var selectedId by rememberSaveable { mutableStateOf(categories.firstOrNull()?.id ?: "") }
    var query by rememberSaveable { mutableStateOf("") }
    val selected = categories.firstOrNull { it.id == selectedId } ?: categories.firstOrNull()
    if (selected == null) return

    CompositionLocalProvider(LocalSettingsNavigation provides mainNavigationState) {
        SettingsCenterShellContent(
            categories = categories,
            mainNavigationState = mainNavigationState,
            selected = selected,
            onSelect = { selectedId = it },
            query = query,
            onQuery = { query = it }
        )
    }
}

@Composable
private fun SettingsCenterShellContent(
    categories: List<SettingsScreenContract.Category>,
    mainNavigationState: MainNavigationState,
    selected: SettingsScreenContract.Category,
    onSelect: (String) -> Unit,
    query: String,
    onQuery: (String) -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= TwoPaneMinWidth

        if (wide) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = Dimens.Space4, vertical = Dimens.Space3),
                horizontalArrangement = Arrangement.spacedBy(Dimens.Space4)
            ) {
                SettingsRail(
                    categories = categories,
                    selected = selected,
                    query = query,
                    onQuery = onQuery,
                    onSelect = {
                        onSelect(it.id)
                        onQuery("")
                    },
                    modifier = Modifier
                        .width(264.dp)
                        .fillMaxHeight()
                )
                SettingsContentPane(
                    categories = categories,
                    selected = selected,
                    query = query,
                    mainNavigationState = mainNavigationState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = Dimens.Space3),
                verticalArrangement = Arrangement.spacedBy(Dimens.Space2)
            ) {
                SettingsSearchField(
                    value = query,
                    onValueChange = onQuery,
                    placeholder = resolveString { center.searchPlaceholder }
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { category ->
                        CategoryChip(
                            category = category,
                            selected = category == selected && query.isBlank(),
                            onClick = {
                                onSelect(category.id)
                                onQuery("")
                            }
                        )
                    }
                }
                SettingsContentPane(
                    categories = categories,
                    selected = selected,
                    query = query,
                    mainNavigationState = mainNavigationState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            }
        }
    }
}

// ============================================
// RAIL (desktop / tablet)
// ============================================

@Composable
private fun SettingsRail(
    categories: List<SettingsScreenContract.Category>,
    selected: SettingsScreenContract.Category,
    query: String,
    onQuery: (String) -> Unit,
    onSelect: (SettingsScreenContract.Category) -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Dimens.Space3)) {
        Text(
            text = resolveString { home.settingsTabLabel },
            style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
            color = surfaceColors.textPrimary,
            fontWeight = FontWeight.SemiBold
        )
        SettingsSearchField(
            value = query,
            onValueChange = onQuery,
            placeholder = resolveString { center.searchPlaceholder }
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            categories.forEach { category ->
                RailCategoryRow(
                    category = category,
                    selected = category == selected && query.isBlank(),
                    matchCount = if (query.isNotBlank()) countMatches(category, query) else null,
                    onClick = { onSelect(category) }
                )
            }
        }
    }
}

@Composable
private fun RailCategoryRow(
    category: SettingsScreenContract.Category,
    selected: Boolean,
    matchCount: Int?,
    onClick: () -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(scaledRadius(Dimens.RadiusMd)))
            .background(
                when {
                    selected -> accent.primary.copy(alpha = 0.14f)
                    isHovered -> surfaceColors.surfaceInteractive.copy(alpha = 0.6f)
                    else -> Color.Transparent
                }
            )
            .border(
                width = if (selected) 1.dp else 0.dp,
                color = if (selected) accent.primary.copy(alpha = 0.4f) else Color.Transparent,
                shape = RoundedCornerShape(scaledRadius(Dimens.RadiusMd))
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .hoverable(interactionSource)
            .padding(horizontal = Dimens.Space3, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.Space2)
    ) {
        category.icon?.let { icon ->
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) accent.primary else surfaceColors.textSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = category.title,
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            color = if (selected) surfaceColors.textPrimary else surfaceColors.textSecondary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        if (matchCount != null && matchCount > 0) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(accent.primary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = matchCount.toString(),
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    color = accent.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ============================================
// PHONE CHIP
// ============================================

@Composable
private fun CategoryChip(
    category: SettingsScreenContract.Category,
    selected: Boolean,
    onClick: () -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) accent.primary.copy(alpha = 0.16f) else surfaceColors.surfaceElevated)
            .border(
                1.dp,
                if (selected) accent.primary.copy(alpha = 0.5f) else surfaceColors.border.copy(alpha = 0.3f),
                RoundedCornerShape(999.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.Space3, vertical = Dimens.Space2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        category.icon?.let { icon ->
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) accent.primary else surfaceColors.textSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = category.title,
            style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
            color = if (selected) accent.primary else surfaceColors.textSecondary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

// ============================================
// CONTENT PANE
// ============================================

@Composable
private fun SettingsContentPane(
    categories: List<SettingsScreenContract.Category>,
    selected: SettingsScreenContract.Category,
    query: String,
    mainNavigationState: MainNavigationState,
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    val scope = rememberCoroutineScope()
    var showResetDialog by remember { mutableStateOf(false) }
    var showResetAllDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(Dimens.Space3)
    ) {
        if (query.isBlank()) {
            SettingHeader(
                icon = selected.icon,
                title = selected.title,
                subtitle = selected.subtitle,
                onReset = if (selected.reset != null) {
                    { showResetDialog = true }
                } else null
            )
            AnimatedContent(
                targetState = selected,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                label = "settingsCategory"
            ) { category ->
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.Space3)) {
                    category.content(mainNavigationState)
                    Spacer(Modifier.height(2.dp))
                }
            }
            DangerActionSetting(
                title = resolveString { center.resetAllLabel },
                description = resolveString { center.resetAllDescription },
                icon = Icons.Default.RestartAlt,
                onClick = { showResetAllDialog = true }
            )
        } else {
            val matches = searchCategories(categories, query)
            if (matches.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Dimens.Space10),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        tint = surfaceColors.border,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(Modifier.height(Dimens.Space3))
                    Text(
                        text = resolveString { center.searchNoResults },
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                        color = surfaceColors.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = resolveString { center.searchNoResultsHint },
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = surfaceColors.textMuted
                    )
                }
            } else {
                matches.forEach { (category, descriptors) ->
                    SettingHeader(
                        icon = category.icon,
                        title = category.title,
                        subtitle = category.subtitle
                    )
                    if (descriptors.isNotEmpty()) {
                        SettingGroup(
                            title = null,
                            children = descriptors.map { descriptor -> descriptor.render }
                        )
                    }
                }
            }
        }
    }

    if (showResetDialog && selected.reset != null) {
        val reset = selected.reset
        MultiplatformDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text(resolveString { center.resetConfirmTitle(selected.title) })
            },
            content = {
                Text(
                    text = resolveString { center.resetConfirmMessage(selected.title) },
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = surfaceColors.textSecondary
                )
            },
            buttons = {
                TextButton({ showResetDialog = false }) {
                    Text(resolveString { center.cancel })
                }
                TextButton({
                    showResetDialog = false
                    scope.launch { runCatching { reset?.invoke() } }
                }) {
                    Text(resolveString { center.confirm })
                }
            }
        )
    }

    if (showResetAllDialog) {
        MultiplatformDialog(
            onDismissRequest = { showResetAllDialog = false },
            title = {
                Text(resolveString { center.resetAllConfirmTitle })
            },
            content = {
                Text(
                    text = resolveString { center.resetAllConfirmMessage },
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = surfaceColors.textSecondary
                )
            },
            buttons = {
                TextButton({ showResetAllDialog = false }) {
                    Text(resolveString { center.cancel })
                }
                TextButton({
                    showResetAllDialog = false
                    scope.launch {
                        categories.forEach { category ->
                            category.reset?.let { reset -> runCatching { reset() } }
                        }
                    }
                }) {
                    Text(resolveString { center.confirm })
                }
            }
        )
    }
}

// ============================================
// SEARCH
// ============================================

private fun countMatches(category: SettingsScreenContract.Category, query: String): Int =
    category.descriptors.count { it.matches(query) } +
        if (categoryKeywordsMatch(category, query)) 1 else 0

private fun categoryKeywordsMatch(category: SettingsScreenContract.Category, query: String): Boolean {
    val haystack = buildString {
        append(category.title.lowercase())
        append(' ')
        append(category.subtitle.lowercase())
        append(' ')
        append(category.id.lowercase())
        category.keywords.forEach { append(' '); append(it.lowercase()) }
    }
    return query.split(' ').filter { it.isNotBlank() }.all { haystack.contains(it) }
}

private fun searchCategories(
    categories: List<SettingsScreenContract.Category>,
    query: String
): List<Pair<SettingsScreenContract.Category, List<SettingDescriptor>>> {
    return categories.mapNotNull { category ->
        val matched = category.descriptors.filter { it.matches(query) }
        if (matched.isEmpty() && !categoryKeywordsMatch(category, query)) null
        else category to matched
    }
}
