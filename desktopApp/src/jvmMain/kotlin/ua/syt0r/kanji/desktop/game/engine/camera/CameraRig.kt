package ua.syt0r.kanji.desktop.game.engine.camera

import ua.syt0r.kanji.desktop.game.engine.geom.Rect
import ua.syt0r.kanji.desktop.game.engine.geom.Vec2

/**
 * Camera rig — the layer between player input and the [Camera].
 *
 * In the vertical slice the rig is a 2D follow rig: it converts the player's
 * look/aim input into camera offsets and zoom. In the future 3D integration
 * the same rig drives a third-person orbit camera and a first-person look
 * camera without the rest of the engine caring which one is live.
 */
class CameraRig(
    private val camera: Camera,
    private val settings: CameraSettings
) {
    /** Smoothly follow a target using the configured distance/offset. */
    fun follow(target: Vec2, dt: Float) {
        follow(target, dt, emptyList())
    }

    /**
     * Follow a target, resolving the framed point against solid rects so the
     * camera never clips through a building (spec §30).
     */
    fun follow(target: Vec2, dt: Float, solids: List<Rect>) {
        val offset = if (camera.mode == CameraMode.ThirdPerson) {
            Vec2(0f, -settings.cameraDistance * 0.35f)
        } else {
            Vec2.Zero
        }
        val resolved = CameraCollision.resolve(target + offset, solids)
        camera.follow(resolved, dt, Vec2.Zero)
    }

    /** Apply a zoom step from the mouse wheel / pinch (touch) input. */
    fun zoomBy(delta: Float) {
        camera.zoom = camera.zoom + delta * settings.zoomStep
    }

    /** First-person look offset (used by the photo viewfinder in the slice). */
    fun lookOffset(delta: Vec2): Vec2 = delta * settings.lookSensitivity

    companion object {
        private const val ZOOM_OUT_FACTOR = 0.9f
    }
}

/**
 * Camera preferences — the exact surface the Settings menu exposes
 * (sensitivity, invert Y, FOV, distance, smoothing, default mode).
 * Persisted through [GameSettings].
 */
data class CameraSettings(
    var sensitivity: Float = 1f,
    var invertY: Boolean = false,
    var fov: Float = 60f,
    var cameraDistance: Float = 1f,
    var cameraSmoothing: Float = 1f,
    var defaultMode: CameraMode = CameraMode.ThirdPerson,
    var zoomStep: Float = 0.15f,
    var lookSensitivity: Float = 0.004f
)
