package ua.syt0r.kanji.desktop.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// ============================================
// KAITEYO DESIGN SYSTEM — PAGE SHELL
// Standardized page layout that every screen
// should consume for consistent spacing,
// hierarchy and responsive behavior.
//
// Usage:
//   DsPageShell(title = "Settings") {
//       DsSection(title = "General") {
//           // section content
//       }
//       DsSection(title = "Appearance") {
//           // section content
//       }
//   }
// ============================================

/**
 * Standard page shell with title bar and scrollable content.
 * Every screen should wrap its content in this for consistent
 * spacing, hierarchy and scroll behavior.
 */
@Composable
fun DsPageShell(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    scrollable: Boolean = true,
    headerActions: @Composable () -> Unit = {},
    footer: @Composable () -> Unit = {},
    content: @Composable () -> Unit
) {
    val sc = surfaceColors()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(sc.background)
    ) {
        // Page header — title + optional actions
        DsPageHeader(
            title = title,
            subtitle = subtitle,
            actions = headerActions
        )
        DsToolbarDivider()

        // Page content — scrollable or fixed
        val contentModifier = if (scrollable) {
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(DsSpacing.Lg)
        } else {
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(DsSpacing.Lg)
        }

        Column(
            modifier = contentModifier,
            verticalArrangement = Arrangement.spacedBy(DsSpacing.Xl)
        ) {
            content()
        }

        // Optional footer
        if (footer != @Composable {}) {
            DsToolbarDivider()
            footer()
        }
    }
}

/**
 * Page header with title, subtitle and optional right-side actions.
 */
@Composable
fun DsPageHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable () -> Unit = {}
) {
    val sc = surfaceColors()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DsSpacing.Lg, vertical = DsSpacing.Md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = sc.textPrimary,
                fontSize = DsType.Title,
                fontWeight = FontWeight.SemiBold
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = sc.textMuted,
                    fontSize = DsType.Caption
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs)) {
            actions()
        }
    }
}

/**
 * Content section with optional title and right-side action.
 * Provides consistent section spacing and hierarchy.
 */
@Composable
fun DsSection(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    action: @Composable () -> Unit = {},
    content: @Composable () -> Unit
) {
    val sc = surfaceColors()
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)
    ) {
        // Section header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = sc.textPrimary,
                    fontSize = DsType.BodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        color = sc.textMuted,
                        fontSize = DsType.Caption
                    )
                }
            }
            action()
        }
        // Section content
        content()
    }
}

/**
 * Section card — a DsCard wrapper for grouping related content
 * within a section. Use when content needs visual grouping.
 */
@Composable
fun DsSectionCard(
    modifier: Modifier = Modifier,
    elevated: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(DsSpacing.Lg),
    content: @Composable () -> Unit
) {
    DsCard(
        modifier = modifier.fillMaxWidth(),
        elevated = elevated
    ) {
        Box(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}

/**
 * Two-column responsive layout that stacks on narrow windows.
 */
@Composable
fun DsTwoColumn(
    modifier: Modifier = Modifier,
    gap: androidx.compose.ui.unit.Dp = DsSpacing.Lg,
    first: @Composable () -> Unit,
    second: @Composable () -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(gap)
    ) {
        Box(modifier = Modifier.weight(1f)) { first() }
        Box(modifier = Modifier.weight(1f)) { second() }
    }
}

/**
 * Empty page state — shown when a page has no content to display.
 * Wraps DsEmptyState with standard page padding.
 */
@Composable
fun DsPageEmpty(
    title: String,
    message: String = "",
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    action: @Composable () -> Unit = {}
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        DsEmptyState(
            title = title,
            message = message,
            icon = icon,
            action = action
        )
    }
}

/**
 * Page loading state — skeleton layout that matches the page structure.
 */
@Composable
fun DsPageLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(DsSpacing.Lg),
        verticalArrangement = Arrangement.spacedBy(DsSpacing.Lg)
    ) {
        // Title skeleton
        DsSkeleton(width = 180.dp, height = 24.dp)
        DsSkeleton(width = 120.dp, height = 12.dp)

        // Content skeletons
        DsSkeletonCard()
        DsSkeletonCard()
        DsSkeletonCard()
    }
}

/**
 * Page error state — shown when a page fails to load.
 */
@Composable
fun DsPageError(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        DsEmptyState(
            title = "Something went wrong",
            message = message,
            icon = androidx.compose.material.icons.Icons.Default.Warning,
            action = if (onRetry != null) {
                { DsButton(text = "Try again", onClick = onRetry) }
            } else {
                {}
            }
        )
    }
}
