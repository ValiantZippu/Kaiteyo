package ua.syt0r.kanji.presentation.screen.main.features

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors

// ============================================
// COMMAND PALETTE
// VS Code-style quick command palette.
// Fuzzy search across destinations, actions,
// filters and theme commands.
// ============================================

data class PaletteAction(
    val title: String,
    val subtitle: String = "",
    val keywords: String = "",
    val shortcut: String = "",
    val icon: ImageVector? = null,
    val category: String = "Navigate",
    val execute: () -> Unit
)

class CommandPaletteController {

    var isOpen by mutableStateOf(false)
        private set
    var query by mutableStateOf("")
        private set
    var selectedIndex by mutableStateOf(0)
        private set
    private val _actions = mutableStateListOf<PaletteAction>()

    val actions: List<PaletteAction> get() = _actions

    fun setActions(newActions: List<PaletteAction>) {
        _actions.clear()
        _actions.addAll(newActions)
    }

    val filteredActions by derivedStateOf {
        val q = query.trim().lowercase()
        if (q.isEmpty()) {
            _actions.take(12)
        } else {
            _actions
                .map { action ->
                    val haystack = (action.title + " " + action.subtitle + " " + action.keywords)
                        .lowercase()
                    val titleLower = action.title.lowercase()
                    val score = when {
                        titleLower == q -> 0
                        titleLower.startsWith(q) -> 1
                        haystack.startsWith(q) -> 2
                        haystack.contains(q) -> 3
                        else -> Int.MAX_VALUE
                    }
                    score to action
                }
                .filter { it.first != Int.MAX_VALUE }
                .sortedWith(compareBy({ it.first }, { it.second.title }))
                .take(12)
                .map { it.second }
        }
    }

    fun open() {
        query = ""
        selectedIndex = 0
        isOpen = true
    }

    fun close() {
        isOpen = false
    }

    fun toggle() {
        if (isOpen) close() else open()
    }

    fun updateQuery(newQuery: String) {
        query = newQuery
        selectedIndex = 0
    }

    fun selectNext() {
        val count = filteredActions.size
        if (count > 0) selectedIndex = (selectedIndex + 1) % count
    }

    fun selectPrevious() {
        val count = filteredActions.size
        if (count > 0) selectedIndex = (selectedIndex - 1 + count) % count
    }

    fun executeSelected() {
        filteredActions.getOrNull(selectedIndex)?.execute()
    }
}

/**
 * Global palette controller so platform entry points
 * (desktop key events) can open the palette.
 */
object KaiteyoPalette {
    val controller = CommandPaletteController()
}

@Composable
fun CommandPaletteOverlay(controller: CommandPaletteController = KaiteyoPalette.controller) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current

    AnimatedVisibility(
        visible = controller.isOpen,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { controller.close() },
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 96.dp)
                    .width(600.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { }
            ) {
                AnimatedVisibility(
                    visible = true,
                    enter = scaleIn(initialScale = 0.96f, animationSpec = androidx.compose.animation.core.spring()) + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(surfaceColors.surfaceElevated)
                    ) {
                        PaletteSearchField(
                            controller = controller,
                            accent = accent,
                            surfaceColors = surfaceColors
                        )
                        PaletteResultsList(
                            controller = controller,
                            accent = accent,
                            surfaceColors = surfaceColors
                        )
                        PaletteFooter(
                            controller = controller,
                            accent = accent,
                            surfaceColors = surfaceColors
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaletteSearchField(
    controller: CommandPaletteController,
    accent: ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme,
    surfaceColors: ua.syt0r.kanji.presentation.common.theme.SurfaceColors
) {
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(controller.isOpen) {
        if (controller.isOpen) {
            text = controller.query
            focusRequester.requestFocus()
        }
    }

    LaunchedEffect(controller.query) {
        if (controller.isOpen) text = controller.query
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(surfaceColors.surfaceInteractive)
            .focusRequester(focusRequester)
            .focusable()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "⌘",
                color = accent.primary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            androidx.compose.foundation.text.BasicTextField(
                value = text,
                onValueChange = {
                    text = it
                    controller.updateQuery(it)
                },
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = surfaceColors.textPrimary,
                    fontSize = 16.sp
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(accent.primary),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            if (controller.query.isNotEmpty()) {
                Text(
                    text = "esc",
                    color = surfaceColors.textMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun PaletteResultsList(
    controller: CommandPaletteController,
    accent: ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme,
    surfaceColors: ua.syt0r.kanji.presentation.common.theme.SurfaceColors
) {
    val listState = rememberLazyListState()
    val filtered = controller.filteredActions

    LaunchedEffect(controller.selectedIndex, filtered.size) {
        val index = controller.selectedIndex
        if (index > 0) listState.animateScrollToItem(index)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
    ) {
        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No matches",
                        color = surfaceColors.textSecondary,
                        fontSize = 15.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Try a different search term",
                        color = surfaceColors.textMuted,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 8.dp, vertical = 8.dp
                )
            ) {
                itemsIndexed(filtered, key = { _, action -> action.title + action.shortcut }) { index, action ->
                    PaletteResultItem(
                        action = action,
                        isSelected = index == controller.selectedIndex,
                        accent = accent,
                        surfaceColors = surfaceColors,
                        onClick = { action.execute() }
                    )
                }
            }
        }
    }
}

@Composable
private fun PaletteResultItem(
    action: PaletteAction,
    isSelected: Boolean,
    accent: ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme,
    surfaceColors: ua.syt0r.kanji.presentation.common.theme.SurfaceColors,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isSelected) accent.primary.copy(alpha = 0.10f)
                else Color.Transparent
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (isSelected) accent.primary.copy(alpha = 0.18f)
                    else surfaceColors.surfaceInteractive
                ),
            contentAlignment = Alignment.Center
        ) {
            if (action.icon != null) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = null,
                    tint = if (isSelected) accent.primary else surfaceColors.textSecondary,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Text(
                    text = action.category.take(1),
                    color = if (isSelected) accent.primary else surfaceColors.textMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = action.title,
                color = if (isSelected) accent.primary else surfaceColors.textPrimary,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (action.subtitle.isNotBlank()) {
                Text(
                    text = action.subtitle,
                    color = surfaceColors.textMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (action.shortcut.isNotBlank()) {
            Text(
                text = action.shortcut,
                color = surfaceColors.textMuted,
                fontSize = 11.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun PaletteFooter(
    controller: CommandPaletteController,
    accent: ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme,
    surfaceColors: ua.syt0r.kanji.presentation.common.theme.SurfaceColors
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(surfaceColors.surface)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        FooterHint(icon = Icons.Default.KeyboardArrowUp, label = "Up", accent = accent, surfaceColors = surfaceColors)
        FooterHint(icon = Icons.Default.KeyboardArrowDown, label = "Down", accent = accent, surfaceColors = surfaceColors)
        FooterHint(icon = Icons.Default.ArrowForward, label = "Enter", accent = accent, surfaceColors = surfaceColors)
        Spacer(Modifier.weight(1f))
        Text(
            text = "Ctrl+K to open",
            color = surfaceColors.textMuted,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun FooterHint(
    icon: ImageVector,
    label: String,
    accent: ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme,
    surfaceColors: ua.syt0r.kanji.presentation.common.theme.SurfaceColors
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = surfaceColors.textMuted,
            modifier = Modifier.size(14.dp)
        )
        Text(text = label, color = surfaceColors.textMuted, fontSize = 11.sp)
    }
}
