package ua.syt0r.kanji.desktop.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ua.syt0r.kanji.presentation.common.theme.LocalAnimationConfig

// ============================================
// KAITEYO DESIGN SYSTEM — CARDS, LISTS, SKELETON
// ============================================

/** Base card surface used across every panel. */
@Composable
fun DsCard(
    modifier: Modifier = Modifier,
    elevated: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val sc = surfaceColors()
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val bg = if (elevated) sc.surfaceElevated else sc.surface
    val shape = RoundedCornerShape(DsRadius.Lg)

    // Interactive cards lift gently on hover; non-interactive stay flat.
    val elevation by animateDpAsState(
        targetValue = if (onClick != null && hovered) DsElevation.Raised else DsElevation.Flat,
        animationSpec = tween(200),
        label = "cardElevation"
    )

    Box(
        modifier = modifier
            .shadow(elevation, shape)
            .clip(shape)
            .background(bg)
            .then(
                if (onClick != null) Modifier
                    .clickable(interactionSource = interaction, indication = null, onClick = onClick)
                    .hoverable(interaction)
                else Modifier
            )
    ) {
        // Top accent line on hoverable cards.
        if (onClick != null && hovered) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(accent().primary)
            )
        }
        content()
    }
}

/** Generic list item row with leading/trailing slots. */
@Composable
fun DsListItem(
    modifier: Modifier = Modifier,
    leading: @Composable () -> Unit = {},
    title: String,
    subtitle: String? = null,
    trailing: @Composable () -> Unit = {},
    onClick: (() -> Unit)? = null
) {
    val sc = surfaceColors()
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DsRadius.Md))
            .background(if (hovered) sc.surfaceInteractive else Color.Transparent)
            .then(
                if (onClick != null) Modifier
                    .clickable(interactionSource = interaction, indication = null, onClick = onClick)
                    .hoverable(interaction)
                else Modifier
            )
            .padding(horizontal = DsSpacing.Lg, vertical = DsSpacing.Md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leading()
        Spacer(Modifier.width(DsSpacing.Sm))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = sc.textPrimary,
                fontSize = DsType.Body,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = sc.textMuted,
                    fontSize = DsType.Caption,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        trailing()
    }
}

/** Simple virtualized column using LazyColumn — 100k+ cards safe. */
@Composable
fun <T> DsVirtualList(
    items: List<T>,
    key: (T) -> Any,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(DsSpacing.Md),
    content: @Composable (T, Int) -> Unit
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(DsSpacing.Xs)
    ) {
        items(items.size, key = { items.getOrNull(it)?.let(key) ?: it }) { index ->
            content(items[index], index)
        }
    }
}

/** Favorite star toggle. */
@Composable
fun DsFavoriteToggle(
    favorite: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ac = accent()
    val sc = surfaceColors()
    Icon(
        imageVector = if (favorite) Icons.Default.Star else Icons.Default.StarBorder,
        contentDescription = "Favorite",
        tint = if (favorite) Color(0xFFFFD93D) else sc.textMuted,
        modifier = modifier
            .size(18.dp)
            .clickable(onClick = onToggle)
    )
}

/** Chevron affordance for navigable rows. */
@Composable
fun DsChevron() {
    Icon(
        Icons.Default.ChevronRight,
        contentDescription = null,
        tint = surfaceColors().textMuted,
        modifier = Modifier.size(18.dp)
    )
}

// ============================================
// SKELETON LOADING
// ============================================

/**
 * Animated loading placeholder. Pulses gently between two surface tones so
 * the UI feels alive while content loads. Honors reduced motion.
 */
@Composable
fun DsSkeleton(
    modifier: Modifier = Modifier,
    width: androidx.compose.ui.unit.Dp = 120.dp,
    height: androidx.compose.ui.unit.Dp = 12.dp,
    rounded: Boolean = true
) {
    val sc = surfaceColors()
    val reducedMotion = LocalAnimationConfig.current.reducedMotion
    val transition = rememberInfiniteTransition(label = "dsSkeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (reducedMotion) 0 else 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dsSkeletonAlpha"
    )
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(if (rounded) DsRadius.Sm else DsRadius.Xs))
            .background(sc.surfaceInteractive.copy(alpha = if (reducedMotion) 0.6f else 0.35f + 0.4f * alpha))
    )
}

@Composable
fun DsSkeletonCard(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DsRadius.Lg))
            .background(surfaceColors().surface)
            .padding(DsSpacing.Lg),
        verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
    ) {
        DsSkeleton(height = 48.dp, width = 48.dp)
        DsSkeleton(width = 160.dp)
        DsSkeleton(width = 200.dp, height = 10.dp)
        DsSkeleton(width = 120.dp, height = 10.dp)
    }
}

// ============================================
// EMPTY STATE
// ============================================

@Composable
fun DsEmptyState(
    title: String,
    message: String = "",
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    action: @Composable () -> Unit = {}
) {
    val sc = surfaceColors()
    val ac = accent()
    Column(
        modifier = modifier.padding(DsSpacing.Xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = ac.primary.copy(alpha = 0.6f),
                modifier = Modifier.size(36.dp)
            )
        }
        Text(
            text = title,
            color = sc.textSecondary,
            fontSize = DsType.BodyLarge,
            fontWeight = FontWeight.Medium
        )
        if (message.isNotBlank()) {
            Text(
                text = message,
                color = sc.textMuted,
                fontSize = DsType.Body
            )
        }
        action()
    }
}
