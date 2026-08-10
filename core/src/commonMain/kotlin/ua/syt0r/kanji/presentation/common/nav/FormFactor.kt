package ua.syt0r.kanji.presentation.common.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ============================================
// FORM FACTOR DETECTION
// The navigation shell adapts to five layout
// tiers instead of simply shrinking the desktop UI.
// ============================================

fun formFactorForWidth(width: Dp): FormFactor = when {
    width < 600.dp -> FormFactor.Phone
    width < 840.dp -> FormFactor.SmallTablet
    width < 1024.dp -> FormFactor.LargeTablet
    width < 1280.dp -> FormFactor.CompactWindow
    else -> FormFactor.Desktop
}

@Composable
fun rememberFormFactor(): FormFactor {
    val density = LocalDensity.current
    val containerWidth = LocalWindowInfo.current.containerSize.width
    val widthDp = with(density) { containerWidth.toDp() }
    return formFactorForWidth(widthDp)
}
