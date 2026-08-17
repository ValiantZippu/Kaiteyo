package ua.syt0r.kanji.desktop.game.engine.camera

import ua.syt0r.kanji.desktop.game.engine.geom.Rect
import ua.syt0r.kanji.desktop.game.engine.geom.Vec2
import ua.syt0r.kanji.desktop.game.engine.geom.clamp
import kotlin.math.max

/**
 * The camera converts between world space and screen space and decides what
 * the player sees. The vertical slice uses a 2D follow camera; the settings it
 * exposes (zoom/distance, smoothing, sensitivity, mode) are the same ones a 3D
 * third-person/first-person rig consumes — see [CameraRig] and
 * `docs/game/ARCHITECTURE.md` (Camera).
 */
class Camera(
    var viewportWidth: Float = 1280f,
    var viewportHeight: Float = 720f
) {
    /** World position the camera is centred on (follow target). */
    var position: Vec2 = Vec2.Zero

    /** Zoom: 1 = 1 world unit per screen pixel. */
    var zoom: Float = 1f
        set(value) {
            field = clamp(value, MIN_ZOOM, MAX_ZOOM)
        }

    /** How quickly the camera catches up to its target (0..1 per second-ish). */
    var smoothing: Float = 6f

    /** Optional world bounds the camera never shows outside. */
    var bounds: Rect? = null

    /** Current screen-space shake offset (world units), decays each tick. */
    var shake: Vec2 = Vec2.Zero

    /** Effective camera mode (TPP default — see [CameraMode]). */
    var mode: CameraMode = CameraMode.ThirdPerson

    fun worldToScreen(world: Vec2): Vec2 =
        Vec2(
            (world.x - position.x) * zoom + viewportWidth / 2f + shake.x,
            (world.y - position.y) * zoom + viewportHeight / 2f + shake.y
        )

    fun screenToWorld(screen: Vec2): Vec2 =
        Vec2(
            (screen.x - viewportWidth / 2f - shake.x) / zoom + position.x,
            (screen.y - viewportHeight / 2f - shake.y) / zoom + position.y
        )

    fun scaleForZoom(value: Float): Float = value * zoom

    /**
     * Smoothly follow a target. The offset parameter models third-person
     * framing (target sits slightly below the screen centre); first-person
     * uses a zero offset.
     */
    fun follow(target: Vec2, dt: Float, offset: Vec2 = Vec2.Zero) {
        val targetPosition = target + offset
        val t = 1f - kotlin.math.exp(-smoothing * dt)
        position = position.lerp(targetPosition, t)
        clampToBounds()
    }

    fun snapTo(target: Vec2) {
        position = target
        clampToBounds()
    }

    fun addShake(amount: Vec2) {
        shake += amount
    }

    fun decayShake(dt: Float) {
        shake = shake * max(0f, 1f - dt * 8f)
        if (shake.lengthSquared() < 0.01f) shake = Vec2.Zero
    }

    fun visibleWorldRect(): Rect {
        val halfW = viewportWidth / (2f * zoom)
        val halfH = viewportHeight / (2f * zoom)
        return Rect(position.x - halfW, position.y - halfH, halfW * 2f, halfH * 2f)
    }

    private fun clampToBounds() {
        val b = bounds ?: return
        val halfW = viewportWidth / (2f * zoom)
        val halfH = viewportHeight / (2f * zoom)
        position = Vec2(
            clamp(position.x, b.x + halfW, b.right - halfW),
            clamp(position.y, b.y + halfH, b.bottom - halfH)
        )
    }

    companion object {
        const val MIN_ZOOM = 0.6f
        const val MAX_ZOOM = 2.2f
    }
}

/**
 * Camera presentation modes. Third-person is the default (character presence,
 * outfits, social interaction); first-person serves photography and close
 * observation. Both are first-class: the mode enum, the [CameraRig] and the
 * settings (FOV, distance, sensitivity, invert Y) exist now; the 3D rig
 * consumes them when the 3D renderer lands.
 */
enum class CameraMode {
    ThirdPerson,
    FirstPerson
}
