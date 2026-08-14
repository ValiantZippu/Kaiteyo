package ua.syt0r.kanji.presentation.common.nav

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow as materialShadow
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
import ua.syt0r.kanji.presentation.common.theme.Dimens
import ua.syt0r.kanji.presentation.common.theme.LocalAnimationConfig
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalRadiusConfig
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState
import ua.syt0r.kanji.presentation.screen.main.screen.home.HomeNavigationState

// ============================================
// LAUNCHPAD
// macOS-style application launcher opened from
// the bubble. Springy scale-in, translucent
// glass panel, grouped grid, hover lift.
// Entrance choreography:
//   wordmark → frosted glass blur-in → section
//   headers → staggered icon tiles (cascade).
// ============================================

/** Base delay before the cascade starts, in ms. */
private const val StaggerBaseMs = 60L

/** Extra delay per revealed element, in ms. */
private const val StaggerStepMs = 42L

/**
 * Staggered-reveal wrapper. Fades + rises each child in sequence when the
 * launchpad opens; skipped entirely under reduced motion or when the user
 * disabled the staggered cascade.
 */
@Composable
private fun LaunchStagger(
    index: Int,
    reducedMotion: Boolean,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (reducedMotion || !enabled) {
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
    val enterSpec = spring<Float>(dampingRatio = 0.72f, stiffness = 260f)
    // User-configurable launchpad scale: the panel width adapts so scale > 1
    // never pushes content off screen.
    val launchpadScale = launchpad.scale.coerceIn(0.7f, 1.2f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { overlaySize = it }
            .background(Color.Black.copy(alpha = 0.42f))
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                    onClose()
                    true
                } else {
                    false
                }
            }
    ) {
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.fillMaxSize(),
            enter = fadeIn(tween(160)) + scaleIn(
                initialScale = 0.96f,
                transformOrigin = origin,
                animationSpec = enterSpec
            ),
            exit = fadeOut(tween(120)) + scaleOut(
                targetScale = 0.96f,
                transformOrigin = origin,
                animationSpec = tween(140)
            )
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onClose
                    ),
                contentAlignment = Alignment.Center
            ) {
                val panelWidth = ((maxWidth * 0.86f).coerceAtMost(860.dp) * launchpadScale)
                val tileWidth = 128.dp * launchpad.spacing.coerceIn(0.7f, 1.5f)
                val columns = ((panelWidth - Dimens.Space8 * 2 - Dimens.Space3) / tileWidth)
                    .toInt()
                    .coerceIn(3, 7)

                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.86f)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = Dimens.Space8),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Wordmark — reveals first.
                    LaunchStagger(index = 0, reducedMotion = reducedMotion, enabled = launchpad.staggeredReveal) {
                        Text(
                            text = "Kaiteyo",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White.copy(alpha = 0.92f),
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 2.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }

                    // Glass panel — frosted layer blurs in and sharpens while
                    // the content (headers + tiles) cascades on top, crisp.
                    val panelShape = RoundedCornerShape(scaledRadius(Dimens.Radius2xl))
                    val glassProgress = remember { Animatable(0f) }
                    LaunchedEffect(Unit) {
                        if (reducedMotion) {
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
                            .padding(top = Dimens.Space6)
                            .width(panelWidth)
                            .materialShadow(36.dp, panelShape)
                            .clip(panelShape)
                    ) {
                        // Frosted glass layer (blur-in).
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .alpha(glassAlpha)
                                .blur(glassBlur.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            surfaceColors.surfaceElevated.copy(alpha = 0.92f * launchpad.opacity.coerceIn(0.6f, 1f)),
                                            surfaceColors.surface.copy(alpha = 0.84f * launchpad.opacity.coerceIn(0.6f, 1f))
                                        )
                                    )
                                )
                                .border(1.dp, Color.White.copy(alpha = 0.08f), panelShape)
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
                                    LaunchStagger(index = cascadeIndex++, reducedMotion = reducedMotion, enabled = launchpad.staggeredReveal) {
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
                                val entries = section.entries
                                entries.chunked(columns).forEach { rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(Dimens.Space3)
                                    ) {
                                        rowItems.forEach { entry ->
                                    LaunchStagger(
                                        index = cascadeIndex++,
                                        reducedMotion = reducedMotion,
                                        enabled = launchpad.staggeredReveal,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        LaunchpadItem(
                                                    entry = entry,
                                                    columns = columns,
                                                    onClick = {
                                                        entry.onClick()
                                                        onClose()
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

                    // Hint — appears last, after the tile cascade.
                    LaunchStagger(index = 100, reducedMotion = reducedMotion, enabled = launchpad.staggeredReveal) {
                        Text(
                            text = "Press Escape or click outside to close",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.45f),
                            modifier = Modifier.padding(top = Dimens.Space4),
                            textAlign = TextAlign.Center
                        )
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
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.07f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 320f),
        label = "launchpadScale"
    )
    val tileRadius = scaledRadius(Dimens.RadiusLg)
    val tileShape = RoundedCornerShape(tileRadius)
    val tileColor = if (entry.selected) accent.primary.copy(alpha = 0.22f)
    else surfaceColors.surfaceInteractive.copy(alpha = 0.75f)
    val tileSize = (if (columns >= 6) 44.dp else 54.dp)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(scaledRadius(Dimens.RadiusLg)))
            .background(Color.Transparent)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .hoverable(interactionSource)
            .padding(vertical = Dimens.Space1),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.Space2)
    ) {
        Box(
            modifier = Modifier
                .size(tileSize)
                .graphicsLayer(scaleX = scale, scaleY = scale)
                .clip(tileShape)
                .background(tileColor)
                .border(
                    width = if (entry.selected) 1.5.dp else 0.dp,
                    color = accent.primary.copy(alpha = 0.6f),
                    shape = tileShape
                )
                .materialShadow(if (isHovered) 16.dp else 6.dp, tileShape),
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
            color = if (entry.selected) accent.primary else Color.White.copy(alpha = 0.88f),
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
