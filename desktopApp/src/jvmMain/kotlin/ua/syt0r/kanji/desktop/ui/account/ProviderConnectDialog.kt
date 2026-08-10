package ua.syt0r.kanji.desktop.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import ua.syt0r.kanji.desktop.designsystem.DsButton
import ua.syt0r.kanji.desktop.designsystem.DsButtonKind
import ua.syt0r.kanji.desktop.designsystem.DsDialog
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsTextButton
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.accent
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import ua.syt0r.kanji.desktop.engine.account.AccountEngine
import ua.syt0r.kanji.desktop.engine.account.AuthFlowState

// ============================================
// PROVIDER CONNECT DIALOG
// Renders the OAuth device flow: contacting the
// provider, the one-time user code + verification
// URL with a live countdown, and error/retry.
// ============================================

@Composable
fun ProviderConnectDialog(
    engine: AccountEngine,
    state: AuthFlowState
) {
    when (state) {
        AuthFlowState.Polling -> PollingDialog(engine)
        is AuthFlowState.AwaitingAuthorization -> AwaitingAuthorizationDialog(engine, state)
        is AuthFlowState.Failed -> FailedDialog(engine, state)
        AuthFlowState.Idle,
        is AuthFlowState.Completed -> Unit
    }
}

@Composable
private fun PollingDialog(engine: AccountEngine) {
    val sc = surfaceColors()
    DsDialog(title = "Connecting to provider", onDismiss = { engine.cancelAuthFlow() }) {
        Column(verticalArrangement = Arrangement.spacedBy(DsSpacing.Lg)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Text("Contacting provider…", color = sc.textSecondary, fontSize = DsType.Body)
            }
            DsButton(
                text = "Cancel",
                kind = DsButtonKind.Ghost,
                onClick = { engine.cancelAuthFlow() }
            )
        }
    }
}

@Composable
private fun AwaitingAuthorizationDialog(
    engine: AccountEngine,
    state: AuthFlowState.AwaitingAuthorization
) {
    val sc = surfaceColors()
    val ac = accent()
    val uriHandler = LocalUriHandler.current

    var remainingSeconds by remember(state.expiresAtEpochMs) {
        mutableIntStateOf(
            ((state.expiresAtEpochMs - Clock.System.now().toEpochMilliseconds()) / 1000L)
                .toInt().coerceAtLeast(0)
        )
    }
    LaunchedEffect(state.expiresAtEpochMs) {
        while (remainingSeconds > 0) {
            delay(1000)
            remainingSeconds = ((state.expiresAtEpochMs - Clock.System.now().toEpochMilliseconds()) / 1000L)
                .toInt().coerceAtLeast(0)
        }
    }

    DsDialog(
        title = "Connect ${state.provider.displayName}",
        onDismiss = { engine.cancelAuthFlow() }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
            Text(
                text = "Open the verification page and enter this code to authorize Kaiteyo:",
                color = sc.textSecondary,
                fontSize = DsType.Body
            )

            // One-time user code.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(sc.surfaceInteractive)
                    .padding(vertical = DsSpacing.Lg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.userCode,
                    color = ac.primary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 4.sp
                )
            }

            DsButton(
                text = "Open verification page",
                icon = Icons.Default.OpenInBrowser,
                onClick = { uriHandler.openUri(state.verificationUri) },
                modifier = Modifier.fillMaxWidth()
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(DsSpacing.Xs))
                Text(
                    text = "Waiting for authorization…",
                    color = sc.textMuted,
                    fontSize = DsType.Caption,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Expires in ${remainingSeconds / 60}:${(remainingSeconds % 60).toString().padStart(2, '0')}",
                    color = sc.textMuted,
                    fontSize = DsType.Caption
                )
            }

            DsButton(
                text = "Cancel",
                kind = DsButtonKind.Ghost,
                onClick = { engine.cancelAuthFlow() },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun FailedDialog(
    engine: AccountEngine,
    state: AuthFlowState.Failed
) {
    val sc = surfaceColors()
    DsDialog(
        title = "Couldn't connect ${state.provider.displayName}",
        onDismiss = { engine.dismissAuthFlow() }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(DsSpacing.Lg)) {
            Text(
                text = state.message,
                color = sc.textSecondary,
                fontSize = DsType.Body
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm, Alignment.End)
            ) {
                DsTextButton(
                    text = "Cancel",
                    onClick = { engine.dismissAuthFlow() }
                )
                if (state.retryable) {
                    DsButton(
                        text = "Try again",
                        icon = Icons.Default.Refresh,
                        onClick = { engine.connectGitHub() }
                    )
                }
            }
        }
    }
}
