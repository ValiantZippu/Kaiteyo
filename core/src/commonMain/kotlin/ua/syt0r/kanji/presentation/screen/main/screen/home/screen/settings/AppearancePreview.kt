package ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.syt0r.kanji.presentation.common.resources.brand.BrandMark
import ua.syt0r.kanji.presentation.common.resources.string.resolveString
import ua.syt0r.kanji.presentation.common.theme.BaseMode
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoThemeState
import ua.syt0r.kanji.presentation.common.theme.LocalRadiusConfig
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.theme.surfaceForBaseMode

// ============================================
// APPEARANCE LIVE PREVIEW
// Miniature application mockup that reflects
// base mode, accent, corner radius and density
// in real time. Reused by the Appearance page
// and search results.
// ============================================

@Composable
fun AppearancePreview(modifier: Modifier = Modifier) {
    val themeState = LocalKaiteyoThemeState.current
    val accent = LocalKaiteyoAccent.current
    val surfaceColors = LocalSurfaceColors.current
    val radiusMultiplier = LocalRadiusConfig.current.style.globalMultiplier

    val preview = surfaceForBaseMode(themeState.baseMode)
    val density = themeState.layoutConfig.density.spacingMultiplier
    val radius = (10 * radiusMultiplier).dp
    val windowRadius = (16 * radiusMultiplier).dp
    val padding = (8 * density).dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = resolveString { center.livePreviewLabel },
            style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
            color = surfaceColors.textMuted,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(windowRadius))
                .background(preview.background)
                .border(
                    1.dp,
                    preview.border.copy(alpha = 0.4f),
                    RoundedCornerShape(windowRadius)
                )
                .padding(padding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(padding)
            ) {
                // Mini sidebar
                Column(
                    modifier = Modifier
                        .width(46.dp)
                        .clip(RoundedCornerShape(radius))
                        .background(preview.surfaceElevated)
                        .padding(vertical = padding),
                    verticalArrangement = Arrangement.spacedBy((6 * density).dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // The real Kaiteyo mark — centralized brand asset, not a "K".
                    BrandMark(
                        modifier = Modifier.size((22 * density).dp),
                        contentDescription = null
                    )
                    repeat(4) { index ->
                        Box(
                            Modifier
                                .size((16 * density).dp)
                                .clip(CircleShape)
                                .background(
                                    if (index == 0) accent.primary.copy(alpha = 0.85f)
                                    else preview.border.copy(alpha = 0.6f)
                                )
                        )
                    }
                }

                // Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(radius))
                        .background(preview.surface)
                        .padding((8 * density).dp),
                    verticalArrangement = Arrangement.spacedBy((6 * density).dp)
                ) {
                    // Header line
                    Box(
                        Modifier
                            .fillMaxWidth(0.5f)
                            .height((6 * density).dp)
                            .clip(RoundedCornerShape(radius / 2))
                            .background(preview.border.copy(alpha = 0.5f))
                    )
                    // Stat cards
                    Row(horizontalArrangement = Arrangement.spacedBy((5 * density).dp)) {
                        repeat(2) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(radius))
                                    .background(preview.surfaceElevated)
                                    .padding((6 * density).dp)
                            ) {
                                Box(
                                    Modifier
                                        .size((14 * density).dp)
                                        .clip(CircleShape)
                                        .background(accent.primary.copy(alpha = 0.85f))
                                )
                                Spacer(Modifier.height((3 * density).dp))
                                Box(
                                    Modifier
                                        .fillMaxWidth(0.7f)
                                        .height((4 * density).dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(preview.border.copy(alpha = 0.5f))
                                )
                            }
                        }
                    }
                    // Button preview
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(radius))
                            .background(accent.primary)
                            .padding(vertical = (6 * density).dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Kaiteyo",
                            color = accent.onPrimary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = resolveString { center.changesApplyInstantly },
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            color = surfaceColors.textMuted
        )
    }
}
