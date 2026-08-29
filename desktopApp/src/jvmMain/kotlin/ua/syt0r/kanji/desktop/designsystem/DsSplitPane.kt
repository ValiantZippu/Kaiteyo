package ua.syt0r.kanji.desktop.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ============================================
// KAITEYO DESIGN SYSTEM — SPLIT PANE
// Simple horizontal/vertical split pane with draggable divider.
// ============================================

@Composable
fun DsSplitPane(
    vertical: Boolean,
    initialFraction: Float = 0.5f,
    modifier: Modifier = Modifier,
    dividerWidth: Dp = 4.dp,
    minFraction: Float = 0.15f,
    maxFraction: Float = 0.85f,
    first: @Composable () -> Unit,
    second: @Composable () -> Unit
) {
    val sc = surfaceColors()
    var fraction by remember { mutableStateOf(initialFraction.coerceIn(minFraction, maxFraction)) }

    Box(modifier = modifier) {
        if (vertical) {
            androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(fraction).fillMaxWidth()) { first() }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dividerWidth)
                        .background(sc.border)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val delta = dragAmount.y / size.height
                                fraction = (fraction + delta).coerceIn(minFraction, maxFraction)
                            }
                        }
                )
                Box(modifier = Modifier.weight(1f - fraction).fillMaxWidth()) { second() }
            }
        } else {
            androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(fraction).fillMaxHeight()) { first() }
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(dividerWidth)
                        .background(sc.border)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val delta = dragAmount.x / size.width
                                fraction = (fraction + delta).coerceIn(minFraction, maxFraction)
                            }
                        }
                )
                Box(modifier = Modifier.weight(1f - fraction).fillMaxHeight()) { second() }
            }
        }
    }
}
