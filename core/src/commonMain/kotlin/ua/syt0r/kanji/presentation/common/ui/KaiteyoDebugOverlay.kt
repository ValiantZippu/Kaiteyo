package ua.syt0r.kanji.presentation.common.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ua.syt0r.kanji.BuildConfig
import ua.syt0r.kanji.presentation.common.theme.Dimens
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors

// ============================================================
// KAITEYO DEBUG OVERLAY
// ------------------------------------------------------------
// The subtle, theme-aware developer surface shown at the bottom
// of the window while debug display is enabled. Reports Page /
// Route / Panel plus optional live FPS and viewport readouts,
// offers a one-tap "copy debug info" that produces a ready-to-
// paste bug-report header, and a shortcut into Debug settings.
// Developer-facing only — off by default and never shown without
// its toggle (Navigation settings "Show page debug info" or the
// Debug settings overlay toggles).
// ============================================================

@Composable
fun KaiteyoDebugOverlay(
    page: PageIdentity,
    modifier: Modifier = Modifier,
    navigationMode: String = "",
    themeLabel: String = "",
    windowState: String = "",
    fps: String? = null,
    viewport: String? = null,
    onOpenSettings: (() -> Unit)? = null
) {
    val surfaceColors = LocalSurfaceColors.current
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    val debugInfo = remember(page, navigationMode, themeLabel, windowState, fps, viewport) {
        buildString {
            append("Kaiteyo ").append(BuildConfig.versionName).append('\n')
            append(page.summary()).append('\n')
            if (themeLabel.isNotEmpty()) append("Theme: ").append(themeLabel).append('\n')
            if (navigationMode.isNotEmpty()) append("Navigation: ").append(navigationMode).append('\n')
            if (windowState.isNotEmpty()) append("Window: ").append(windowState).append('\n')
            if (fps != null) append("FPS: ").append(fps).append('\n')
            if (viewport != null) append("Viewport: ").append(viewport).append('\n')
            append("Platform: ").append(platformTag)
        }
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(Dimens.RadiusMd))
            .background(surfaceColors.surfaceElevated.copy(alpha = 0.92f))
            .border(
                width = 1.dp,
                color = surfaceColors.border.copy(alpha = 0.4f),
                shape = RoundedCornerShape(Dimens.RadiusMd)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "DEBUG",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = surfaceColors.textMuted
            )
            Text(
                text = page.name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = surfaceColors.textPrimary
            )
            Text(
                text = page.route,
                style = MaterialTheme.typography.labelSmall,
                color = surfaceColors.textMuted
            )
            if (!page.panel.isNullOrBlank()) {
                Text(
                    text = "· ${page.panel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = surfaceColors.textMuted
                )
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable {
                        clipboard.setText(AnnotatedString(debugInfo))
                        copied = true
                    }
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = "Copy page debug information",
                    tint = surfaceColors.textMuted,
                    modifier = Modifier.size(10.dp)
                )
                Text(
                    text = if (copied) "copied" else "copy",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (copied) surfaceColors.textPrimary else surfaceColors.textMuted
                )
            }
        }
        if (fps != null) {
            Text(
                text = "FPS $fps",
                style = MaterialTheme.typography.labelSmall,
                color = surfaceColors.textMuted
            )
        }
        if (viewport != null) {
            Text(
                text = "Viewport: $viewport",
                style = MaterialTheme.typography.labelSmall,
                color = surfaceColors.textMuted
            )
        }
        if (onOpenSettings != null) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onOpenSettings)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Open debug settings",
                    tint = surfaceColors.textMuted,
                    modifier = Modifier.size(10.dp)
                )
                Text(
                    text = "debug settings",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = surfaceColors.textMuted
                )
            }
        }
    }
}

/**
 * The platform tag embedded in copied debug info. Kept constant in shared
 * code (the app builds for JVM/Android/iOS); each platform build reports
 * its own tag through the platform-specific BuildConfig fields when present.
 */
private const val platformTag: String = "MPP"
