package ua.syt0r.kanji.presentation.common.nav

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow as materialShadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ua.syt0r.kanji.presentation.common.theme.Dimens
import ua.syt0r.kanji.presentation.common.theme.LocalAnimationConfig
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalRadiusConfig
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState
import ua.syt0r.kanji.presentation.screen.main.screen.home.HomeNavigationState

// ============================================
// LAUNCHPAD
// The primary-destination launcher opened from
// the floating bubble. Fast, smooth and premium:
// the panel emerges from the bubble (scale
// transform origin tracks the bubble), the scrim
// fades in, and the tiles cascade in sequence.
// Fully keyboard-navigable (arrows + Enter,
// Escape closes) and theme-aware — no hardcoded
// light-on-light text, no black chrome.
// ============================================

/** Base delay before the cascade starts, in ms. */
private const val StaggerBaseMs = 60L

/** Extra delay per revealed element, in ms. */
private const val StaggerStepMs = 42L

/**
 * Staggered-reveal wrapper. Fades + rises each child in sequence when the
 * launchpad opens; skipped entirely under reduced motion or when the user
 * disabled the staggered cascade. Keyed on [visible] so the choreography
 * replays on every open.
 */
@Composable
private fun LaunchStagger(
    index: Int,
    visible: Boolean,
    reducedMotion: Boolean,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(visible, reducedMotion, enabled) {
        if (reducedMotion || !enabled || !visible) {
            progress.snapTo(1f)
        } else {
            delay(StaggerBaseMs + index * StaggerStepMs)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.62f, stiffness = 340f)
            )
        }
    }
    val value = progress.value
    Box(
        modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = value
                translationY = (1f - value) * 14f
            }
    ) {
        content()
    }
}

@Composable
fun Launchpad(
    sections: List<NavSection>,
    navigationState: MainNavigationState,
    homeNavState: HomeNavigationState,
    visible: Boolean,
    bubbleCenter: Offset? = null,
    onClose: () -> Unit,
    launchpadSettings: LaunchpadSettings = LaunchpadSettings()
) {
    val density = LocalDensity.current
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val reducedMotion = LocalAnimationConfig.current.reducedMotion
    val launchpad = launchpadSettings

    // The launchpad visibly expands from the floating bubble: the scale
    // transform origin sits at the bubble's position (clamped so the panel
    // always stays comfortably on screen). Centered placement + scroll keep
    // the panel inside the screen regardless of the bubble's location.
    var overlaySize by remember { mutableStateOf(IntSize.Zero) }
    val origin = remember(bubbleCenter, overlaySize, launchpad.direction) {
        val bubbleX = bubbleCenter?.x ?: overlaySize.width / 2f
        val bubbleY = bubbleCenter?.y ?: overlaySize.height / 2f
        if (overlaySize == IntSize.Zero) {
            TransformOrigin.Center
        } else {
            val x = (bubbleX / overlaySize.width).coerceIn(0.08f, 0.92f)
            val y = when (launchpad.direction) {
                LaunchpadDirection.Up -> 1f
                LaunchpadDirection.Down -> 0f
                LaunchpadDirection.Auto -> (bubbleY / overlaySize.height).coerceIn(0.08f, 0.92f)
            }
            TransformOrigin(x, y)
        }
    }

    // Scrim + panel animations. The panel stays composed (BubbleLauncher
    // always renders this) so closing plays a real exit: the scrim fades out
    // and the panel settles back toward the bubble instead of vanishing.
    val panelScale = remember { Animatable(0.94f) }
    val scrimAlpha = remember { Animatable(0f) }
    LaunchedEffect(visible, reducedMotion) {
        if (visible && !reducedMotion) {
            panelScale.snapTo(0.94f)
            scrimAlpha.snapTo(0f)
            launch {
                scrimAlpha.animateTo(1f, tween(180))
                panelScale.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = 260f))
            }
        } else if (visible) {
            panelScale.snapTo(1f)
            scrimAlpha.snapTo(1f)
        } else {
            scrimAlpha.animateTo(0f, tween(140))
            panelScale.snapTo(0.94f)
        }
    }

    val launchpadScale = launchpad.scale.coerceIn(0.7f, 1.2f)
    // Columns derived from the actual overlay width (the panel is centered),
    // shared by the grid layout and the keyboard navigation math.
    val overlayWidthDp = with(density) { overlaySize.width.toDp() }
    val panelWidthDp = (overlayWidthDp * 0.86f).coerceAtMost(860.dp) * launchpadScale
    val tileWidth = 128.dp * launchpad.spacing.coerceIn(0.7f, 1.5f)
    val columns = (((panelWidthDp - Dimens.Space8 * 2 - Dimens.Space3) / tileWidth).toInt())
        .coerceIn(3, 7)

    // Keyboard navigation: the grid is a flat list of entries laid out in
    // `columns` per row. Arrows move focus, Enter/Space activates, Escape
    // closes. Clicking outside also closes.
    val entries = sections.flatMap { it.entries }
    val focusRequesters = remember(entries.size) {
        List(entries.size) { FocusRequester() }
    }
    var focusedIndex by remember { mutableStateOf(-1) }
    LaunchedEffect(visible) {
        if (visible) {
            delay(120)
            if (focusedIndex !in entries.indices) {
                focusedIndex = 0
                focusRequesters.firstOrNull()?.requestFocus()
            }
        }
    }

    fun moveFocus(delta: Int, vertical: Boolean) {
        if (entries.isEmpty()) return
        val cols = columns.coerceAtLeast(1)
        val current = if (focusedIndex in entries.indices) focusedIndex else 0
        val next = if (vertical) {
            (current + delta * cols).coerceIn(0, entries.size - 1)
        } else {
            val rowStart = (current / cols) * cols
            val rowEnd = minOf(rowStart + cols, entries.size) - 1
            (current + delta).coerceIn(rowStart, rowEnd)
        }
        focusedIndex = next
        focusRequesters[next].requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { overlaySize = it }
    ) {
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.fillMaxSize(),
            enter = fadeIn(tween(160)),
            exit = fadeOut(tween(130))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f * scrimAlpha.value))
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        when (event.key) {
                            Key.Escape -> {
                                onClose()
                                true
                            }
                            Key.DirectionRight -> {
                                moveFocus(1, vertical = false)
                                true
                            }
                            Key.DirectionLeft -> {
                                moveFocus(-1, vertical = false)
                                true
                            }
                            Key.DirectionDown -> {
                                moveFocus(1, vertical = true)
                                true
                            }
                            Key.DirectionUp -> {
                                moveFocus(-1, vertical = true)
                                true
                            }
                            Key.Enter, Key.Spacebar -> {
                                val index = focusedIndex
                                if (index in entries.indices) {
                                    entries[index].onClick()
                                    onClose()
                                }
                                true
                            }
                            else -> false
                        }
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = onClose
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.86f)
                            .fillMaxHeight()
                            .padding(vertical = Dimens.Space8),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Wordmark — reveals first.
                        LaunchStagger(index = 0, visible = visible, reducedMotion = reducedMotion, enabled = launchpad.staggeredReveal) {
                            Text(
                                text = "Kaiteyo",
                                style = MaterialTheme.typography.headlineMedium,
                                color = surfaceColors.textPrimary,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 2.sp,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }

                        // The launchpad panel is centered in the vertical space
                        // between the wordmark and the hint — never parked at the
                        // top of the window. On very short windows the panel
                        // scrolls internally instead of clipping.
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            BoxWithConstraints(Modifier.fillMaxSize()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = maxHeight)
                                        .verticalScroll(rememberScrollState()),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Glass panel — frosted layer blurs in and sharpens while
                                    // the content (headers + tiles) cascades on top, crisp.
                                    val panelShape = RoundedCornerShape(scaledRadius(Dimens.Radius2xl))
                                    val glassProgress = remember { Animatable(0f) }
                                    LaunchedEffect(visible, reducedMotion) {
                                        if (reducedMotion || !visible) {
                                            glassProgress.snapTo(1f)
                                        } else {
                                            delay(40)
                                            glassProgress.animateTo(
                                                targetValue = 1f,
                                                animationSpec = tween(420, easing = FastOutSlowInEasing)
                                            )
                                        }
                                    }
                                    val glassAlpha = glassProgress.value
                                    val glassBlur = (1f - glassProgress.value) * 18f

                                    Box(
                                        modifier = Modifier
                                            .width(panelWidthDp)
                                            .graphicsLayer {
                                                scaleX = panelScale.value
                                                scaleY = panelScale.value
                                                transformOrigin = origin
                                            }
                                            .materialShadow(36.dp, panelShape)
                                            .clip(panelShape)
                                    ) {
                                        // Frosted glass layer (blur-in).
                                        Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .graphicsLayer { alpha = glassAlpha }
                                                .blur(glassBlur.dp)
                                                .background(
                                                    Brush.verticalGradient(
                                                        colors = listOf(
                                                            surfaceColors.surfaceElevated.copy(alpha = 0.94f * launchpad.opacity.coerceIn(0.6f, 1f)),
                                                            surfaceColors.surface.copy(alpha = 0.86f * launchpad.opacity.coerceIn(0.6f, 1f))
                                                        )
                                                    )
                                                )
                                                .border(1.dp, surfaceColors.border.copy(alpha = 0.3f), panelShape)
                                        )
                                        // Crisp content with staggered tiles.
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(Dimens.Space8),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            // Track a running cascade index: wordmark(0), then
                                            // every section header and tile in order.
                                            var cascadeIndex = 1
                                            sections.forEach { section ->
                                                if (section.title != null) {
                                                    LaunchStagger(index = cascadeIndex++, visible = visible, reducedMotion = reducedMotion, enabled = launchpad.staggeredReveal) {
                                                        Text(
                                                            text = section.title(),
                                                            style = MaterialTheme.typography.labelMedium,
                                                            color = surfaceColors.textMuted,
                                                            fontWeight = FontWeight.SemiBold,
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(
                                                                    start = Dimens.Space2,
                                                                    top = Dimens.Space3,
                                                                    bottom = Dimens.Space3
                                                                )
                                                        )
                                                    }
                                                }
                                                val sectionEntries = section.entries
                                                sectionEntries.chunked(columns).forEach { rowItems ->
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(Dimens.Space3)
                                                    ) {
                                                        rowItems.forEach { entry ->
                                                            val index = entries.indexOf(entry)
                                                            LaunchStagger(
                                                                index = cascadeIndex++,
                                                                visible = visible,
                                                                reducedMotion = reducedMotion,
                                                                enabled = launchpad.staggeredReveal,
                                                                modifier = Modifier.weight(1f)
                                                            ) {
                                                                LaunchpadItem(
                                                                    entry = entry,
                                                                    columns = columns,
                                                                    modifier = if (index >= 0) Modifier.focusRequester(focusRequesters[index]) else Modifier,
                                                                    onClick = {
                                                                        entry.onClick()
                                                                        onClose()
                                                                    },
                                                                    onFocusGained = {
                                                                        if (index >= 0) focusedIndex = index
                                                                    }
                                                                )
                                                            }
                                                        }
                                                        if (rowItems.size < columns) {
                                                            repeat(columns - rowItems.size) {
                                                                Box(Modifier.weight(1f))
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Hint — appears last, after the tile cascade.
                        LaunchStagger(index = 100, visible = visible, reducedMotion = reducedMotion, enabled = launchpad.staggeredReveal) {
                            Text(
                                text = "Press Escape or click outside to close",
                                style = MaterialTheme.typography.labelSmall,
                                color = surfaceColors.textMuted,
                                modifier = Modifier.padding(top = Dimens.Space4),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LaunchpadItem(
    entry: NavEntry,
    columns: Int,
    onClick: () -> Unit,
    onFocusGained: () -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isHovered || isFocused) 1.07f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 320f),
        label = "launchpadScale"
    )
    val tileRadius = scaledRadius(Dimens.RadiusLg)
    val tileShape = RoundedCornerShape(tileRadius)
    val tileColor = if (entry.selected) accent.primary.copy(alpha = 0.22f)
    else surfaceColors.surfaceInteractive.copy(alpha = 0.75f)
    val tileSize = (if (columns >= 6) 44.dp else 54.dp)

    // Report focus gains so the overlay's keyboard state stays in sync.
    LaunchedEffect(isFocused) {
        if (isFocused) onFocusGained()
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(scaledRadius(Dimens.RadiusLg)))
            .background(Color.Transparent)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .hoverable(interactionSource)
            .focusable(interactionSource = interactionSource)
            .padding(vertical = Dimens.Space1),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.Space2)
    ) {
        Box(
            modifier = Modifier
                .size(tileSize)
                .graphicsLayer(scaleX = scale, scaleY = scale)
                // The shadow must precede clip/background so it renders behind
                // the tile. Previously ordered last, it painted on top of the
                // tile — the "square box over the button" artifact.
                .materialShadow(if (isHovered || isFocused) 16.dp else 6.dp, tileShape)
                .clip(tileShape)
                .background(tileColor)
                .border(
                    width = when {
                        entry.selected -> 1.5.dp
                        isFocused -> 2.dp
                        else -> 0.dp
                    },
                    color = if (entry.selected) accent.primary.copy(alpha = 0.6f)
                    else if (isFocused) accent.primary.copy(alpha = 0.9f)
                    else Color.Transparent,
                    shape = tileShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (entry.icon != null) {
                Icon(
                    entry.icon,
                    contentDescription = null,
                    tint = if (entry.selected) accent.primary else surfaceColors.textPrimary,
                    modifier = Modifier.size(if (columns >= 6) 22.dp else 26.dp)
                )
            } else {
                Box(Modifier.size(if (columns >= 6) 22.dp else 26.dp), contentAlignment = Alignment.Center) {
                    entry.iconContent?.invoke()
                }
            }
        }
        Text(
            text = entry.label(),
            style = MaterialTheme.typography.labelMedium,
            color = if (entry.selected) accent.primary else surfaceColors.textPrimary,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun scaledRadius(base: Dp): Dp {
    val multiplier = LocalRadiusConfig.current.style.globalMultiplier
    return base * multiplier
}
