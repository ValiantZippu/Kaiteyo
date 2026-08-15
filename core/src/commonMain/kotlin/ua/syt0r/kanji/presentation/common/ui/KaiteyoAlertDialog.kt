package ua.syt0r.kanji.presentation.common.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Kaiteyo's drop-in replacement for `MaterialTheme`'s `AlertDialog` with the
 * same signature and visual structure (icon / title / text / button row, M3
 * token defaults), but an adaptive width.
 *
 * The M3 `AlertDialog` hard-caps its panel at 560dp internally
 * (`sizeIn(maxWidth = 560.dp)`), which parks every dialog in the middle of
 * wide desktop windows. This wrapper instead sizes the panel from the window
 * via [rememberAdaptiveDialogWidth] (60% of the window on desktop, clamped to
 * 480–860dp), matching the suite's `DsDialog`. On phones it behaves like the
 * classic M3 alert: fills the screen minus the 24dp dialog margin.
 *
 * Swap `AlertDialog(` call sites to `KaiteyoAlertDialog(` and import this
 * composable. Behavior is otherwise identical, including the caller-supplied
 * [properties] (only `usePlatformDefaultWidth` is forced off so the width can
 * actually grow).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KaiteyoAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    iconContentColor: Color = MaterialTheme.colorScheme.primary,
    titleContentColor: Color = MaterialTheme.colorScheme.onSurface,
    textContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    // M3 DialogTokens.ContainerElevation (ElevationTokens.Level3).
    tonalElevation: Dp = 6.dp,
    properties: DialogProperties = DialogProperties()
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        // DialogProperties is a final class in this Compose version (no copy),
        // so rebuild it preserving the caller's flags and force the platform
        // default width off — the whole point of this composable.
        properties = DialogProperties(
            dismissOnBackPress = properties.dismissOnBackPress,
            dismissOnClickOutside = properties.dismissOnClickOutside,
            usePlatformDefaultWidth = false
        )
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val dialogWidth = rememberAdaptiveDialogWidth(maxWidth)
            Surface(
                modifier = modifier.width(dialogWidth),
                shape = shape,
                color = containerColor,
                tonalElevation = tonalElevation
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    icon?.let {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            CompositionLocalProvider(
                                LocalContentColor provides iconContentColor,
                                LocalTextStyle provides MaterialTheme.typography.headlineSmall
                            ) { it() }
                        }
                    }
                    title?.let {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            CompositionLocalProvider(
                                LocalContentColor provides titleContentColor,
                                LocalTextStyle provides MaterialTheme.typography.headlineSmall
                            ) { it() }
                        }
                    }
                    text?.let {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            CompositionLocalProvider(
                                LocalContentColor provides textContentColor,
                                LocalTextStyle provides MaterialTheme.typography.bodyMedium
                            ) { it() }
                        }
                    }
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalArrangement = Arrangement.Center
                    ) {
                        dismissButton?.invoke()
                        confirmButton.invoke()
                    }
                }
            }
        }
    }
}
