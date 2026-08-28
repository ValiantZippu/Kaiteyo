package ua.syt0r.kanji.desktop.ui.workspace

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.appstate.WorkspaceView
import ua.syt0r.kanji.desktop.designsystem.surfaceColors

/**
 * Spatial camera states for the navigation system.
 *
 * IDLE = normal — content fills the viewport, no overlay.
 * OVERVIEW = zoomed out — workspace cards visible, current screen shrunk.
 * TRANSITIONING = camera moving toward a selected workspace.
 */
enum class CameraState {
    IDLE,
    OVERVIEW,
    TRANSITIONING
}

/**
 * The spatial camera: manages zoom-out/zoom-in transitions between
 * workspaces. When the user opens Launchpad, the camera pulls back
 * to reveal the workspace overview. When they select a workspace,
 * the camera zooms into the selected card.
 *
 * This replaces the simple slide-fade AnimatedContent transitions
 * with a spatial model.
 */
class SpatialCamera {
    var state by mutableStateOf(CameraState.IDLE)
        private set

    /** The current zoom scale (1.0 = full viewport, 0.7 = pulled back). */
    var scale by mutableStateOf(1f)
        private set

    /** Horizontal offset for directional transitions (positive = moving right). */
    var offsetX by mutableStateOf(0f)
        private set

    /** Opacity of the current screen content during transition. */
    var contentAlpha by mutableStateOf(1f)
        private set

    /** The target workspace when transitioning. */
    var targetWorkspace by mutableStateOf<WorkspaceView?>(null)
        private set

    /** Opacity of the workspace overview overlay. */
    var overviewAlpha by mutableStateOf(0f)
        private set

    /** Scale of the workspace overview (starts small, grows as camera pulls back). */
    var overviewScale by mutableStateOf(0.85f)
        private set

    private val scaleAnim = Animatable(1f)
    private val overviewAlphaAnim = Animatable(0f)
    private val overviewScaleAnim = Animatable(0.85f)
    private val contentAlphaAnim = Animatable(1f)
    private val offsetXAnim = Animatable(0f)

    /** Pull back to workspace overview (Launchpad open). */
    suspend fun zoomToOverview() {
        state = CameraState.OVERVIEW
        // Run animations in parallel for smooth spatial feel
        kotlinx.coroutines.coroutineScope {
            launch {
                scaleAnim.animateTo(
                    targetValue = 0.72f,
                    animationSpec = spring(
                        dampingRatio = 0.7f,
                        stiffness = Spring.StiffnessLow
                    )
                ) { scale = value }
            }
            launch {
                overviewAlphaAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(200)
                ) { overviewAlpha = value }
            }
            launch {
                overviewScaleAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = 0.65f,
                        stiffness = Spring.StiffnessMedium
                    )
                ) { overviewScale = value }
            }
            launch {
                contentAlphaAnim.animateTo(
                    targetValue = 0.6f,
                    animationSpec = tween(250)
                ) { contentAlpha = value }
            }
        }
    }

    /** Zoom back from overview to current screen (Launchpad dismiss without selection). */
    suspend fun zoomToIdle() {
        state = CameraState.IDLE
        targetWorkspace = null
        kotlinx.coroutines.coroutineScope {
            launch {
                scaleAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = 0.75f,
                        stiffness = Spring.StiffnessMedium
                    )
                ) { scale = value }
            }
            launch {
                overviewAlphaAnim.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(180)
                ) { overviewAlpha = value }
            }
            launch {
                overviewScaleAnim.animateTo(
                    targetValue = 0.85f,
                    animationSpec = tween(200)
                ) { overviewScale = value }
            }
            launch {
                contentAlphaAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(200)
                ) { contentAlpha = value }
            }
        }
    }

    /** Zoom from overview into a selected workspace. */
    suspend fun zoomToWorkspace(target: WorkspaceView) {
        state = CameraState.TRANSITIONING
        targetWorkspace = target

        // First: fade out overview, then zoom into content
        kotlinx.coroutines.coroutineScope {
            launch {
                overviewAlphaAnim.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(150)
                ) { overviewAlpha = value }
            }
            launch {
                overviewScaleAnim.animateTo(
                    targetValue = 1.05f,
                    animationSpec = spring(
                        dampingRatio = 0.8f,
                        stiffness = Spring.StiffnessMedium
                    )
                ) { overviewScale = value }
            }
            launch {
                contentAlphaAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(200)
                ) { contentAlpha = value }
            }
            launch {
                scaleAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = 0.75f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) { scale = value }
            }
        }

        state = CameraState.IDLE
        targetWorkspace = null
    }

    /** Immediate reset without animation (for reduced motion). */
    fun resetImmediate() {
        state = CameraState.IDLE
        scale = 1f
        offsetX = 0f
        contentAlpha = 1f
        overviewAlpha = 0f
        overviewScale = 0.85f
        targetWorkspace = null
        // Animatable.snapTo() is suspend, but resetImmediate is called
        // from non-suspend contexts. The animatable state is internal and
        // will be overwritten on the next animateTo() call anyway.
    }

    /** Jump to overview state without animation (for reduced-motion mode). */
    fun jumpToOverview() {
        state = CameraState.OVERVIEW
        scale = 0.72f
        contentAlpha = 0.6f
        overviewAlpha = 1f
        overviewScale = 1f
    }
}

/**
 * Modifier that applies the spatial camera transform to content.
 * Content scales down when camera pulls back, and fades appropriately.
 */
fun Modifier.spatialCameraTransform(camera: SpatialCamera): Modifier = this.graphicsLayer {
    scaleX = camera.scale
    scaleY = camera.scale
    alpha = camera.contentAlpha
    translationX = camera.offsetX
    transformOrigin = TransformOrigin(0.5f, 0.5f)
}
