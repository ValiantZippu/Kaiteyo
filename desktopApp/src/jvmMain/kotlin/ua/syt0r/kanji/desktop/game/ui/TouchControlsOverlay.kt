package ua.syt0r.kanji.desktop.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import ua.syt0r.kanji.desktop.game.GameSession
import ua.syt0r.kanji.desktop.game.engine.geom.Vec2
import ua.syt0r.kanji.desktop.game.engine.input.InputAction
import ua.syt0r.kanji.desktop.game.engine.input.InputState
import ua.syt0r.kanji.desktop.game.engine.input.VirtualTouchProvider
import kotlin.math.sqrt

/**
 * Touch controls (spec §16) — PUBG/Genshin-style, never a shrunk desktop HUD:
 *
 * - **Left side** — a dynamic-origin joystick: it appears where the thumb
 *   lands and drives movement; pushed to the rim it runs.
 * - **Right side** — drag to look; a quick tap acts as Interact.
 * - **Contextual buttons** — Jump and the photo button live bottom-right,
 *   Interact appears only when something is interactable, and photo mode
 *   swaps in Capture/Close.
 *
 * The overlay is pure presentation: every gesture feeds the shared
 * [InputState] through [VirtualTouchProvider], so the world never knows a
 * touch happened. Menus and the dialogue panel sit above it (later in the
 * Box) and win their own taps.
 */
@Composable
fun TouchControlsOverlay(session: GameSession) {
    if (!session.settings.touchControlsEnabled) return
    val state = session.state
    if (state.menuOpen || state.mapOpen || state.questLogOpen || state.collectionOpen ||
        state.albumOpen || state.knowledgeOpen || state.characterOpen ||
        state.settingsOpen || state.storyOpen || state.savesOpen
    ) return

    val touch = remember { session.input.connectTouch() }
    val inputState = session.input.state

    // Visual joystick state (pixels).
    var joystickOrigin by remember { mutableStateOf<Offset?>(null) }
    var joystickThumb by remember { mutableStateOf<Offset?>(null) }

    val density = LocalDensity.current
    val buttonSizePx = with(density) { 72.dp.toPx() }

    // The view size drives both the visuals and the hit-test (kept in sync
    // through the pointer loop's own size on one hand, and this state on the
    // other — the cluster must sit in the same pixels either way).
    var viewSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { viewSize = it }
            .pointerInput(session) {
                val stickRadius = 64f
                val tapSlop = 10f
                val joystickPointers = mutableMapOf<androidx.compose.ui.input.pointer.PointerId, Offset>() // id → origin
                val lookPointers = mutableMapOf<androidx.compose.ui.input.pointer.PointerId, LookDrag>()   // id → drag state
                val buttonPointers = mutableMapOf<androidx.compose.ui.input.pointer.PointerId, InputAction>()

                awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    val width = this.size.width.toFloat()
                    val height = this.size.height.toFloat()
                    val splitX = width * 0.45f
                    val buttons = ButtonLayout(width, height, buttonSizePx)

                    for (change in event.changes) {
                        val pos = change.position
                        val id = change.id
                        when {
                            // --- Press ---
                            change.changedToDown() -> {
                                val button = buttons.actionAt(pos, session.state.photoMode, session.currentInteractable != null)
                                when {
                                    button != null -> {
                                        buttonPointers[id] = button
                                        touch.pressAction(button, down = true, inputState)
                                    }
                                    pos.x < splitX && joystickPointers.isEmpty() -> {
                                        joystickPointers[id] = pos
                                        joystickOrigin = pos
                                        joystickThumb = pos
                                        touch.setMovePad(Vec2.Zero, run = false, inputState)
                                    }
                                    pos.x >= splitX && lookPointers.isEmpty() -> {
                                        lookPointers[id] = LookDrag(pressPos = pos, pressTime = System.nanoTime())
                                    }
                                }
                                change.consume()
                            }
                            // --- Move ---
                            change.positionChanged() && change.pressed -> {
                                val origin = joystickPointers[id]
                                if (origin != null) {
                                    val dx = pos.x - origin.x
                                    val dy = pos.y - origin.y
                                    val length = sqrt(dx * dx + dy * dy)
                                    val clamped = if (length > stickRadius) {
                                        origin + Offset(dx / length * stickRadius, dy / length * stickRadius)
                                    } else pos
                                    joystickThumb = clamped
                                    val axis = Vec2(
                                        (clamped.x - origin.x) / stickRadius,
                                        (clamped.y - origin.y) / stickRadius
                                    )
                                    touch.setMovePad(axis, axis.length() > 0.85f, inputState)
                                }
                                val look = lookPointers[id]
                                if (look != null) {
                                    if (!look.dragging && (pos - look.pressPos).length() > tapSlop) {
                                        look.dragging = true
                                        look.lastPos = pos
                                    }
                                    if (look.dragging) {
                                        touch.addLook(pos.x - look.lastPos.x, pos.y - look.lastPos.y, inputState)
                                        look.lastPos = pos
                                    }
                                }
                                change.consume()
                            }
                            // --- Release / cancel ---
                            change.changedToUp() || !change.pressed -> {
                                val origin = joystickPointers.remove(id)
                                if (origin != null) {
                                    joystickOrigin = null
                                    joystickThumb = null
                                    touch.setMovePad(Vec2.Zero, run = false, inputState)
                                }
                                val look = lookPointers.remove(id)
                                if (look != null && !look.dragging) {
                                    val quick = (System.nanoTime() - look.pressTime) < 400_000_000L
                                    if (quick) touch.tapInteract(inputState)
                                }
                                buttonPointers.remove(id)?.let { action ->
                                    touch.pressAction(action, down = false, inputState)
                                }
                                change.consume()
                            }
                        }
                    }
                }
                }
            }
    ) {
        // Joystick visuals — a soft ring where the thumb landed + the thumb.
        Canvas(Modifier.fillMaxSize()) {
            val origin = joystickOrigin ?: return@Canvas
            val thumb = joystickThumb ?: origin
            drawCircle(Color.White.copy(alpha = 0.10f), radius = 64f, center = origin)
            drawCircle(Color.White.copy(alpha = 0.22f), radius = 34f, center = origin)
            drawCircle(Color.White.copy(alpha = 0.45f), radius = 26f, center = thumb)
        }

        // Contextual buttons — positioned exactly where the hit-test expects
        // them (bottom-right cluster); pure visuals, the loop above taps them.
        val photoMode = session.state.photoMode
        val interactVisible = session.currentInteractable != null
        val layout = ButtonLayout(viewSize.width.toFloat(), viewSize.height.toFloat(), buttonSizePx)
        if (photoMode) {
            TouchButton("📷", Color(0xFFFFD54F), layout.captureCenter(), buttonSizePx, Modifier)
            TouchButton("✕", Color.White.copy(alpha = 0.6f), layout.closeCenter(), buttonSizePx, Modifier)
        } else {
            if (interactVisible) {
                TouchButton("✋", Color(0xFFA5D6A7), layout.interactCenter(), buttonSizePx, Modifier)
            }
            TouchButton("⤒", Color.White.copy(alpha = 0.55f), layout.jumpCenter(), buttonSizePx, Modifier)
            TouchButton("📷", Color.White.copy(alpha = 0.55f), layout.photoCenter(), buttonSizePx, Modifier)
        }
    }
}

/**
 * Bottom-right button cluster in pixels. Centers are derived from the view
 * size, so the visuals and the pointer-loop hit-test always agree.
 */
private class ButtonLayout(
    private val width: Float,
    private val height: Float,
    private val buttonSize: Float
) {
    private val margin = 72f
    private val gap = 16f

    fun interactCenter(w: Float = width, h: Float = height) = Offset(w - margin, h - margin)
    fun jumpCenter(w: Float = width, h: Float = height) = Offset(w - margin - buttonSize - gap, h - margin)
    fun photoCenter(w: Float = width, h: Float = height) = Offset(w - margin, h - margin - buttonSize - gap)
    fun captureCenter(w: Float = width, h: Float = height) = interactCenter(w, h)
    fun closeCenter(w: Float = width, h: Float = height) = jumpCenter(w, h)

    /** The action whose button contains [pos], or null. */
    fun actionAt(pos: Offset, photoMode: Boolean, interactVisible: Boolean): InputAction? {
        val hitRadius = buttonSize / 2f + 8f
        if (photoMode) {
            if (pos.distanceTo(interactCenter()) <= hitRadius) return InputAction.PhotoCapture
            if (pos.distanceTo(jumpCenter()) <= hitRadius) return InputAction.Back
            return null
        }
        if (interactVisible && pos.distanceTo(interactCenter()) <= hitRadius) return InputAction.Interact
        if (pos.distanceTo(jumpCenter()) <= hitRadius) return InputAction.Jump
        if (pos.distanceTo(photoCenter()) <= hitRadius) return InputAction.PhotoMode
        return null
    }
}

private data class LookDrag(
    val pressPos: Offset,
    val pressTime: Long,
    var dragging: Boolean = false,
    var lastPos: Offset = pressPos
)

private fun Offset.length(): Float = sqrt(x * x + y * y)

private fun Offset.distanceTo(other: Offset): Float = (other - this).length()

@Composable
private fun TouchButton(
    label: String,
    color: Color,
    center: Offset,
    sizePx: Float,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val sizeDp = with(density) { sizePx.toDp() }
    Box(
        modifier = modifier
            .offset { IntOffset((center.x - sizePx / 2f).toInt(), (center.y - sizePx / 2f).toInt()) }
            .size(sizeDp)
            .background(color.copy(alpha = 0.18f), CircleShape)
    ) {
        Text(
            text = label,
            color = color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}
