package ua.syt0r.kanji.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import ua.syt0r.kanji.presentation.common.theme.Dimens

object AppListItemDefaults {
    // Single source of row padding — consumed as ListItem contentPadding, so
    // rows never stack an outer padding on top of Material's own.
    val ExtraPaddings = PaddingValues(
        horizontal = Dimens.ContentPaddingSmall,
        vertical = Dimens.SpacingMid
    )
    val ListItemDefaultPaddings = PaddingValues(
        horizontal = Dimens.ContentPaddingSmall,
        vertical = Dimens.SpacingMid
    )
    val ClickableTrailingOffset = Dimens.SpacingBig
}

/**
 * Rounded, theme-colored row container shared by both [AppListItem]
 * overloads. Clip comes BEFORE the background so the surface never bleeds
 * out as square corners behind the rounded shape.
 */
@Composable
private fun Modifier.roundedRowBackground(): Modifier =
    clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surface)

@Composable
fun AppListItem(
    headlineContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    overlineContent: @Composable (() -> Unit)? = null,
    supportingContent: @Composable (() -> Unit)? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    paddingValues: PaddingValues = AppListItemDefaults.ExtraPaddings,
    colors: ListItemColors = ListItemDefaults.colors()
) {

    ListItem(
        headlineContent = headlineContent,
        // This Material3 version's ListItem has no `contentPadding` param, so
        // the row padding is applied on the modifier instead (matching the
        // Row-based overload below).
        modifier = modifier
            .roundedRowBackground()
            .clickable(onClick)
            .padding(paddingValues),
        overlineContent = overlineContent,
        supportingContent = supportingContent,
        leadingContent = leadingContent,
        trailingContent = trailingContent,
        colors = colors
    )

}

@Composable
fun AppListItem(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(8.dp),
    paddingValues: PaddingValues = AppListItemDefaults.ExtraPaddings,
    rowContent: @Composable RowScope.() -> Unit
) {

    Row(
        modifier = modifier
            .roundedRowBackground()
            .clickable(onClick)
            .padding(paddingValues),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = horizontalArrangement
    ) {
        rowContent()
    }

}
