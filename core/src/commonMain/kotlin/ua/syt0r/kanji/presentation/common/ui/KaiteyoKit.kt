package ua.syt0r.kanji.presentation.common.ui

// ============================================================
// KAITEYO DESIGN KIT
// ------------------------------------------------------------
// The shared visual language for every screen in the app.
//
// One kit, one look: every page composes the same icon chips,
// stat tiles, section cards, pills, rings and hero banners, so
// Home, Library, Stats, Browse and Settings always read as the
// same product instead of five different apps glued together.
//
// All components are theme-driven: they pull the Kaiteyo accent
// and surface palette, so switching theme recolors everything.
// ============================================================

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import kotlin.math.roundToInt

// ============================================================
// ICON CHIP — the rounded accent tile used beside titles/values
// ============================================================

@Composable
fun KaiteyoIconChip(
    icon: ImageVector,
    tint: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
    chipSize: Dp = 36.dp,
    iconSize: Dp = 18.dp,
    cornerRadius: Dp = 11.dp
) {
    Box(
        modifier = modifier
            .size(chipSize)
            .clip(RoundedCornerShape(cornerRadius))
            .background(tint.copy(alpha = 0.13f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

// ============================================================
// ANIMATED COUNT — numbers count up instead of snapping
// ============================================================

@Composable
fun AnimatedCountText(
    target: Int,
    modifier: Modifier = Modifier,
    fontSize: Int = 22,
    fontWeight: FontWeight = FontWeight.Bold,
    color: Color = MaterialTheme.colorScheme.onSurface,
    format: (Int) -> String = { it.toString() }
) {
    val animated by animateIntAsState(
        targetValue = target,
        animationSpec = tween(650),
        label = "count"
    )
    Text(
        text = format(animated),
        modifier = modifier,
        fontSize = fontSize.sp,
        fontWeight = fontWeight,
        color = color
    )
}

// ============================================================
// PROGRESS RING — circular progress with rounded cap
// ============================================================

@Composable
fun KaiteyoProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    strokeWidth: Dp = 7.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color? = null,
    center: @Composable BoxScope.() -> Unit = {}
) {
    val surfaceColors = LocalSurfaceColors.current
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(700),
        label = "ring"
    )
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = androidx.compose.ui.graphics.drawscope.Stroke(
                width = strokeWidth.toPx(),
                cap = StrokeCap.Round
            )
            drawArc(
                color = trackColor ?: surfaceColors.surfaceInteractive,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke
            )
            if (animated > 0f) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * animated,
                    useCenter = false,
                    style = stroke
                )
            }
        }
        center()
    }
}

// ============================================================
// STAT TILE — icon chip + big value + label, the dashboard unit
// ============================================================

@Composable
fun KaiteyoStatTile(
    value: Int,
    label: String,
    icon: ImageVector,
    tint: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
    format: (Int) -> String = { it.toString() }
) {
    val surfaceColors = LocalSurfaceColors.current
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(surfaceColors.surface)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        KaiteyoIconChip(icon = icon, tint = tint, chipSize = 32.dp, iconSize = 16.dp, cornerRadius = 10.dp)
        if (animate) {
            AnimatedCountText(
                target = value,
                fontSize = 22,
                color = tint,
                format = format
            )
        } else {
            Text(
                text = format(value),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = tint
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = surfaceColors.textMuted,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

// ============================================================
// SECTION CARD — the standard content container
// ============================================================

@Composable
fun KaiteyoSectionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    action: (@Composable () -> Unit)? = null,
    contentPadding: Dp = 14.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = surfaceColors.surface)
    ) {
        Column(Modifier.fillMaxWidth().padding(contentPadding)) {
            if (title != null || action != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (icon != null) {
                        KaiteyoIconChip(
                            icon = icon,
                            tint = iconTint,
                            chipSize = 28.dp,
                            iconSize = 15.dp,
                            cornerRadius = 9.dp
                        )
                        Spacer(Modifier.width(9.dp))
                    }
                    if (title != null) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = surfaceColors.textPrimary
                            )
                            if (subtitle != null) {
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = surfaceColors.textMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    action?.invoke()
                }
                Spacer(Modifier.height(12.dp))
            } else if (subtitle != null) {
                Spacer(Modifier.height(0.dp))
            }
            content()
        }
    }
}

// ============================================================
// SECTION LABEL — alias kept for backward compatibility
// ============================================================

@Composable
fun KaiteyoSectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    KaiteyoSectionTitle(text = text, modifier = modifier, tint = tint)
}

// ============================================================
// SECTION TITLE — uppercase accent label with leading dot
// ============================================================

@Composable
fun KaiteyoSectionTitle(
    text: String,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(tint))
        Spacer(Modifier.width(7.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ============================================================
// PILL — the standard selectable/toggle chip
// ============================================================

@Composable
fun KaiteyoPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    val surfaceColors = LocalSurfaceColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val background = when {
        selected -> tint.copy(alpha = 0.16f)
        hovered -> surfaceColors.surfaceInteractive
        else -> surfaceColors.surface
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .border(
                width = 1.dp,
                color = if (selected) tint.copy(alpha = 0.45f) else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .hoverable(interactionSource)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) tint else surfaceColors.textSecondary
        )
    }
}

// ============================================================
// COUNT BADGE — colored count chip (new / due / flags)
// ============================================================

@Composable
fun KaiteyoCountBadge(
    count: Int,
    color: Color,
    modifier: Modifier = Modifier,
    label: String? = null
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 7.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(color))
        Text(
            text = if (label != null) "$count $label" else count.toString(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

// ============================================================
// HOVER CARD — interactive surface that reacts on hover
// ============================================================

@Composable
fun KaiteyoHoverCard(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = MaterialTheme.shapes.large,
    contentPadding: Dp = 14.dp,
    content: @Composable RowScope.() -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val background = if (hovered) surfaceColors.surfaceInteractive else surfaceColors.surface

    val clickModifier = if (onClick != null) {
        Modifier.clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    } else {
        Modifier
    }

    Row(
        modifier = modifier
            .clip(shape)
            .background(background)
            .hoverable(interactionSource)
            .then(clickModifier)
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

// ============================================================
// GRADIENT HERO — the banner used at the top of dashboards
// ============================================================

@Composable
fun KaiteyoGradientHero(
    modifier: Modifier = Modifier,
    colors: List<Color>,
    cornerRadius: Dp = 20.dp,
    contentPadding: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius))
            .background(Brush.linearGradient(colors))
            .padding(contentPadding),
        content = content
    )
}

// ============================================================
// EMPTY STATE — friendly, actionable placeholder
// ============================================================

@Composable
fun KaiteyoEmptyState(
    icon: String,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val surfaceColors = LocalSurfaceColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(surfaceColors.surface)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(icon, fontSize = 32.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = surfaceColors.textPrimary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = message,
            fontSize = 12.sp,
            color = surfaceColors.textMuted,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(12.dp))
            androidx.compose.material3.TextButton(onClick = onAction) {
                Text(
                    text = actionLabel,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ============================================================
// TAG — small label chip (JLPT level, category, etc.)
// ============================================================

@Composable
fun KaiteyoTag(
    text: String,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(tint.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = tint,
            maxLines = 1
        )
    }
}

// ============================================================
// SPARKLINE — tiny inline trend, no axes
// ============================================================

@Composable
fun KaiteyoSparkline(
    values: List<Float>,
    modifier: Modifier = Modifier,
    height: Dp = 26.dp,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val surfaceColors = LocalSurfaceColors.current
    if (values.isEmpty()) {
        Spacer(modifier.height(height))
        return
    }
    val max = values.maxOrNull()?.takeIf { it > 0f } ?: 1f
    Canvas(modifier.fillMaxWidth().height(height)) {
        val stepX = size.width / (values.size - 1).coerceAtLeast(1)
        val points = values.mapIndexed { i, v ->
            androidx.compose.ui.geometry.Offset(
                x = i * stepX,
                y = size.height * (1f - (v / max))
            )
        }
        val path = androidx.compose.ui.graphics.Path()
        points.forEachIndexed { i, p ->
            if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        }
        drawPath(path, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f, cap = StrokeCap.Round))
        points.forEach { p ->
            drawCircle(color, radius = 2f, center = p)
        }
        if (surfaceColors.textMuted != Color.Transparent) {
            drawLine(
                color = surfaceColors.textMuted.copy(alpha = 0.2f),
                start = androidx.compose.ui.geometry.Offset(0f, size.height),
                end = androidx.compose.ui.geometry.Offset(size.width, size.height),
                strokeWidth = 1f
            )
        }
    }
}

// ============================================================
// KEY-VALUE ROW — the standard two-line list row
// ============================================================

@Composable
fun KaiteyoKeyValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    labelColor: Color? = null
) {
    val surfaceColors = LocalSurfaceColors.current
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = labelColor ?: surfaceColors.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ============================================================
// HERO STAT — oversized number inside a hero banner
// ============================================================

@Composable
fun KaiteyoHeroStat(
    value: Int,
    label: String,
    modifier: Modifier = Modifier,
    onSurface: Color = Color.White,
    format: (Int) -> String = { it.toString() }
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedCountText(
            target = value,
            fontSize = 26,
            color = onSurface,
            format = format
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = onSurface.copy(alpha = 0.75f),
            maxLines = 2,
            textAlign = TextAlign.Center
        )
    }
}

// ============================================================
// PERCENT PILL — compact "42%" indicator
// ============================================================

@Composable
fun KaiteyoPercentPill(
    fraction: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val surfaceColors = LocalSurfaceColors.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Text(
            text = "${(fraction.coerceIn(0f, 1f) * 100).roundToInt()}%",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

