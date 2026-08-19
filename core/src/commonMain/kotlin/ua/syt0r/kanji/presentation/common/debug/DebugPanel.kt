package ua.syt0r.kanji.presentation.common.debug

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import kotlin.math.roundToInt
import ua.syt0r.kanji.presentation.common.ui.KaiteyoDebugOverlay
import ua.syt0r.kanji.presentation.common.ui.PageIdentity

// ============================================================
// DEBUG PANEL
// ------------------------------------------------------------
// The bottom-corner developer surface. Measures live frame
// times (smoothed FPS) while visible, reads the window size,
// and renders the page-identity overlay with the optional
// readouts plus a shortcut into Debug settings. Stops measuring
// automatically when it leaves composition — no leaks, no
// per-frame allocation beyond two state writes.
// ============================================================

@Composable
fun DebugPanel(
    page: PageIdentity,
    navigationMode: String,
    themeLabel: String,
    windowState: String,
    modifier: Modifier = Modifier,
    showFps: Boolean = false,
    showViewport: Boolean = false,
    onOpenSettings: () -> Unit
) {
    var lastFrameNanos by remember { mutableLongStateOf(0L) }
    var emaFrameMs by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { now ->
                if (lastFrameNanos != 0L) {
                    val deltaMs = (now - lastFrameNanos) / 1_000_000f
                    if (deltaMs > 0f) {
                        emaFrameMs = if (emaFrameMs == 0f) deltaMs
                        else emaFrameMs * 0.9f + deltaMs * 0.1f
                    }
                }
                lastFrameNanos = now
            }
        }
    }
    val fps = if (emaFrameMs > 0f) (1000f / emaFrameMs).roundToInt() else null

    val containerSize = LocalWindowInfo.current.containerSize
    val viewportText = "${containerSize.width}×${containerSize.height} px"

    KaiteyoDebugOverlay(
        page = page,
        modifier = modifier,
        navigationMode = navigationMode,
        themeLabel = themeLabel,
        windowState = windowState,
        fps = if (showFps) fps?.toString() else null,
        viewport = if (showViewport) viewportText else null,
        onOpenSettings = onOpenSettings
    )
}
