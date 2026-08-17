package ua.syt0r.kanji.desktop.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.rememberTextMeasurer
import ua.syt0r.kanji.desktop.game.GameSession
import ua.syt0r.kanji.desktop.game.engine.input.toGameKey
import ua.syt0r.kanji.desktop.game.engine.render.CanvasRenderer
import ua.syt0r.kanji.desktop.game.engine.geom.Vec2

/**
 * The render surface + loop driver. A Compose [Canvas] is the host surface;
 * the frame loop advances the session's fixed-step engine and bumps a
 * counter that re-triggers the draw, which renders through [CanvasRenderer].
 */
@Composable
fun GameCanvas(
    session: GameSession,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val textMeasurer = rememberTextMeasurer()

    // Give the canvas focus so keys reach the game, not the workspace shell.
    LaunchedEffect(session) {
        focusRequester.requestFocus()
    }

    // The frame loop — advances the engine at the host frame rate; the
    // engine itself steps at a fixed 60 Hz (see GameLoop).
    var frameTick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(session) {
        var last = 0L
        while (true) {
            val now = withFrameNanos { it }
            if (last > 0) {
                val dt = (now - last) / 1_000_000_000f
                session.advance(dt.coerceAtMost(0.1f))
                frameTick++
            }
            last = now
        }
    }

    Canvas(
        modifier = modifier
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                handleKey(session, event)
            }
            .pointerInput(session) {
                awaitPointerEventScope {
                    var lastPosition = Offset.Unspecified
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Press -> {
                                val change = event.changes.firstOrNull() ?: continue
                                lastPosition = change.position
                                if (session.state.photoMode) {
                                    session.input.onPhotoCapture(down = true)
                                } else {
                                    session.input.onMouseClick(
                                        session.camera.screenToWorld(Vec2(change.position.x, change.position.y)),
                                        down = true
                                    )
                                }
                            }
                            PointerEventType.Release -> {
                                val change = event.changes.firstOrNull() ?: continue
                                session.input.onMouseClick(
                                    session.camera.screenToWorld(Vec2(change.position.x, change.position.y)),
                                    down = false
                                )
                            }
                            PointerEventType.Move -> {
                                val change = event.changes.firstOrNull() ?: continue
                                if (lastPosition != Offset.Unspecified) {
                                    session.input.onMouseMove(
                                        change.position.x - lastPosition.x,
                                        change.position.y - lastPosition.y
                                    )
                                }
                                lastPosition = change.position
                            }
                            PointerEventType.Scroll -> {
                                val change = event.changes.firstOrNull() ?: continue
                                session.input.onMouseWheel(change.scrollDelta.y)
                            }
                            else -> {}
                        }
                    }
                }
            }
    ) {
        session.camera.viewportWidth = size.width
        session.camera.viewportHeight = size.height
        // Ensure the frame counter is observed so the draw re-runs each frame.
        frameTick
        val renderer = CanvasRenderer(this, textMeasurer)
        session.engine.render(renderer)
    }
}

private fun handleKey(session: GameSession, event: androidx.compose.ui.input.key.KeyEvent): Boolean {
    // Global chords (Ctrl+K palette, Ctrl+Shift+…) always pass through to the
    // workspace shell — the game only owns plain keys.
    if (event.isCtrlPressed || event.isMetaPressed) return false
    if (event.type != KeyEventType.KeyDown && event.type != KeyEventType.KeyUp) return false

    val gameKey = event.key.toGameKey() ?: return false
    session.input.onKey(gameKey, down = event.type == KeyEventType.KeyDown)

    // Consume keys that map to a game action so they never bubble to the
    // shell's global handlers; unbound keys fall through.
    return session.input.scheme.actionFor(gameKey) != null
}
