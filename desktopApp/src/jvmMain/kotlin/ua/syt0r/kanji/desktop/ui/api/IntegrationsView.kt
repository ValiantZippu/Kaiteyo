package ua.syt0r.kanji.desktop.ui.api

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.designsystem.DsBadge
import ua.syt0r.kanji.desktop.designsystem.DsButton
import ua.syt0r.kanji.desktop.designsystem.DsButtonKind
import ua.syt0r.kanji.desktop.designsystem.DsCard
import ua.syt0r.kanji.desktop.designsystem.DsSectionHeader
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import ua.syt0r.kanji.desktop.engine.api.IntegrationCardRequest

// ============================================
// KAITEYO INTEGRATION HUB
// Every optional bridge into/out of Kaiteyo in
// one place, with honest status: the Local HTTP
// API, GameSentenceMiner, AnkiConnect, the text
// hook, the player WebSocket and the global media
// keys. Kaiteyo's native workflow (media → subtitles
// → dictionary → mining → cards → SRS) never
// depends on any of them — a badge only ever
// reflects a real connection or server state.
// ============================================

@Composable
fun IntegrationsView(state: AppState) {
    val scope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxSize().padding(DsSpacing.Lg),
        verticalArrangement = Arrangement.spacedBy(DsSpacing.Lg)
    ) {
        DsSectionHeader(
            title = "Integrations",
            subtitle = "Kaiteyo works fully on its own — these are optional bridges into and out of other tools. Status reflects a real connection."
        )

        LocalApiCard(state, scope)
        GsmCard(state, scope)
        AnkiCard(state, scope)
        TextHookCard(state)
        PlayerSocketCard(state)
        MediaKeysCard(state)
    }
}

// ------------------------------------------------------------
// Local HTTP API
// ------------------------------------------------------------

@Composable
private fun LocalApiCard(state: AppState, scope: CoroutineScope) {
    val sc = surfaceColors()
    val api = state.localApi
    // Resolve once — the getter generates + persists the token on first use,
    // so it must not run repeatedly during recomposition.
    val apiToken = remember { api.token }
    var tokenVisible by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var testing by remember { mutableStateOf(false) }

    DsCard {
        Column(Modifier.fillMaxWidth().padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                Text("Local API", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                DsBadge(text = if (api.running) "Running" else "Stopped", tint = if (api.running) sc.textSecondary else sc.textMuted)
                DsButton(
                    text = if (api.running) "Stop server" else "Start server",
                    icon = if (api.running) Icons.Default.Stop else Icons.Default.PlayArrow,
                    kind = if (api.running) DsButtonKind.Danger else DsButtonKind.Primary,
                    compact = true,
                    onClick = { if (api.running) api.stop() else api.start() }
                )
            }

            Text("Endpoint", color = sc.textPrimary, fontSize = DsType.Body, fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                Text(api.portInfo, color = sc.textPrimary, fontSize = DsType.Body, modifier = Modifier.weight(1f))
                DsButton(
                    text = "Copy",
                    icon = Icons.Default.ContentCopy,
                    kind = DsButtonKind.Secondary,
                    compact = true,
                    onClick = { copyToClipboard(state, api.portInfo) }
                )
            }

            Text("Auth token", color = sc.textPrimary, fontSize = DsType.Body, fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                Text(
                    if (tokenVisible) apiToken else "••••••••••••${apiToken.takeLast(4)}",
                    color = sc.textSecondary,
                    fontSize = DsType.Body,
                    modifier = Modifier.weight(1f)
                )
                DsButton(
                    text = if (tokenVisible) "Hide" else "Show",
                    kind = DsButtonKind.Secondary,
                    compact = true,
                    onClick = { tokenVisible = !tokenVisible }
                )
                DsButton(
                    text = "Copy",
                    icon = Icons.Default.ContentCopy,
                    kind = DsButtonKind.Secondary,
                    compact = true,
                    onClick = { copyToClipboard(state, apiToken) }
                )
            }

            Text(
                buildString {
                    appendLine("Every endpoint except /api/health requires:")
                    appendLine("  Authorization: Bearer <token>")
                    appendLine()
                    appendLine("Example:")
                    appendLine("curl -X POST ${api.portInfo} \\")
                    appendLine("  -H \"Authorization: Bearer $apiToken\" \\")
                    appendLine("  -H \"Content-Type: application/json\" \\")
                    append("  -d '{\"word\":\"食べる\",\"reading\":\"たべる\",\"definition\":\"to eat\",\"sentence\":\"朝ごはんを食べる\"}'")
                },
                color = sc.textMuted,
                fontSize = DsType.Caption,
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).height(150.dp)
            )

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                DsButton(
                    text = if (testing) "Testing…" else "Test connection",
                    kind = DsButtonKind.Secondary,
                    compact = true,
                    onClick = {
                        testing = true
                        testResult = null
                        scope.launch(Dispatchers.IO) {
                            val r = api.selfTest()
                            testing = false
                            testResult = r.fold({ it }, { "Failed: ${it.message}" })
                        }
                    }
                )
                testResult?.let { Text(it, color = sc.textSecondary, fontSize = DsType.Caption) }
            }

            api.lastError?.let { err ->
                Text("Last error: $err", color = sc.textPrimary, fontSize = DsType.Caption)
            }
            api.lastRequest?.let { req -> RequestSummary(req) }
        }
    }
}

// ------------------------------------------------------------
// GameSentenceMiner
// ------------------------------------------------------------

@Composable
private fun GsmCard(state: AppState, scope: CoroutineScope) {
    val sc = surfaceColors()
    val gsm = state.miningIntegration.gsm
    var testResult by remember { mutableStateOf<String?>(null) }
    var testing by remember { mutableStateOf(false) }

    DsCard {
        Column(Modifier.fillMaxWidth().padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                Text("GameSentenceMiner", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                val badge = when {
                    !gsm.configured -> "Not configured"
                    gsm.connected -> "Connected"
                    else -> "Offline"
                }
                DsBadge(text = badge, tint = if (gsm.connected) sc.textSecondary else sc.textMuted)
            }
            Text("Receives mined cards (Kaiteyo → GSM) when the mining mode is Forward or Both.", color = sc.textSecondary, fontSize = DsType.Body)
            Text(
                "Capabilities: ✓ sentence mining ✓ audio/screenshot references ✓ tags. Configure host/port/token in Settings → Media → Integrations.",
                color = sc.textMuted,
                fontSize = DsType.Caption
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                DsButton(
                    text = if (testing) "Testing…" else "Test connection",
                    kind = DsButtonKind.Secondary,
                    compact = true,
                    onClick = {
                        testing = true
                        testResult = null
                        scope.launch(Dispatchers.IO) {
                            val r = gsm.testConnection()
                            testing = false
                            testResult = r.fold({ it }, { "Failed: ${it.message}" })
                        }
                    }
                )
                testResult?.let { Text(it, color = sc.textSecondary, fontSize = DsType.Caption) }
            }
            gsm.lastError?.let { err ->
                Text("Last error: $err", color = sc.textMuted, fontSize = DsType.Caption)
            }
        }
    }
}

// ------------------------------------------------------------
// AnkiConnect
// ------------------------------------------------------------

@Composable
private fun AnkiCard(state: AppState, scope: CoroutineScope) {
    val sc = surfaceColors()
    val anki = state.miningIntegration.anki
    val enabled = state.settings.getBool("media.anki.enabled")
    var testResult by remember { mutableStateOf<String?>(null) }
    var testing by remember { mutableStateOf(false) }
    var importOpen by remember { mutableStateOf(false) }

    if (importOpen) {
        AnkiImportDialog(state, onDismiss = { importOpen = false })
    }

    DsCard {
        Column(Modifier.fillMaxWidth().padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                Text("AnkiConnect", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                val badge = when {
                    !enabled -> "Disabled"
                    !anki.configured -> "Not configured"
                    anki.connected -> "Connected"
                    else -> "Offline"
                }
                DsBadge(text = badge, tint = if (anki.connected) sc.textSecondary else sc.textMuted)
            }
            Text(
                if (enabled)
                    "Each mine can target Kaiteyo, Anki, or both (chosen in the mining dialog; default from Settings → Media). Kaiteyo stays the primary destination and never depends on Anki."
                else "Enable AnkiConnect in Settings → Media → Integrations to send mined cards to Anki desktop.",
                color = sc.textSecondary,
                fontSize = DsType.Body
            )
            Text(
                "Capabilities: ✓ list/create decks ✓ create Basic notes ✓ tags ✓ screenshot/audio media ✓ duplicate detection ✓ retryable export queue ✓ import decks/notes/cards/tags into Kaiteyo.",
                color = sc.textMuted,
                fontSize = DsType.Caption
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                DsButton(
                    text = "Import from Anki…",
                    icon = Icons.Default.Download,
                    kind = DsButtonKind.Primary,
                    compact = true,
                    enabled = anki.configured,
                    onClick = { importOpen = true }
                )
                DsButton(
                    text = if (testing) "Testing…" else "Test connection",
                    kind = DsButtonKind.Secondary,
                    compact = true,
                    onClick = {
                        testing = true
                        testResult = null
                        scope.launch(Dispatchers.IO) {
                            val r = anki.testConnection()
                            testing = false
                            testResult = r.fold({ it }, { "Failed: ${it.message}" })
                        }
                    }
                )
                testResult?.let { Text(it, color = sc.textSecondary, fontSize = DsType.Caption) }
            }
            val pending = state.mining.pendingExports
            if (pending.isNotEmpty()) {
                var retrying by remember { mutableStateOf(false) }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                    DsBadge(text = "${pending.size} pending export(s)", tint = sc.textSecondary)
                    Text("Kaiteyo already saved these — retry sends only to Anki, no duplicates.", color = sc.textMuted, fontSize = DsType.Caption, modifier = Modifier.weight(1f))
                    DsButton(
                        text = if (retrying) "Retrying…" else "Retry exports",
                        kind = DsButtonKind.Secondary,
                        compact = true,
                        enabled = !retrying && anki.configured,
                        onClick = {
                            retrying = true
                            scope.launch(Dispatchers.IO) {
                                state.mining.retryPendingAnki()
                                retrying = false
                            }
                        }
                    )
                }
                pending.take(3).forEach { p ->
                    Text(
                        "• ${p.payload.headword} — attempt ${p.attempts}${p.lastError.takeIf { it.isNotBlank() }?.let { ": $it" } ?: ""}",
                        color = sc.textMuted,
                        fontSize = DsType.Caption
                    )
                }
            }
            anki.lastError?.let { err ->
                Text("Last error: $err", color = sc.textMuted, fontSize = DsType.Caption)
            }
        }
    }
}

// ------------------------------------------------------------
// Text hook + player WebSocket (browser-connector transport)
// ------------------------------------------------------------

@Composable
private fun TextHookCard(state: AppState) {
    val sc = surfaceColors()
    val media = state.media
    DsCard {
        Column(Modifier.fillMaxWidth().padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                Text("Text hook server", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                DsBadge(text = if (media.textHookRunning) "Running" else "Stopped", tint = if (media.textHookRunning) sc.textSecondary else sc.textMuted)
                DsButton(
                    text = if (media.textHookRunning) "Stop" else "Start",
                    kind = if (media.textHookRunning) DsButtonKind.Danger else DsButtonKind.Secondary,
                    compact = true,
                    onClick = { if (media.textHookRunning) media.stopTextHook() else media.startTextHook() }
                )
            }
            Text(
                "TCP listener for texthookers and scripts: send a Japanese line and it becomes a dictionary lookup. Port ${state.settings.getInt("media.text-hook.port", 8766)} · ${media.textHookClients} client(s).",
                color = sc.textSecondary,
                fontSize = DsType.Body
            )
            Text("External → Kaiteyo", color = sc.textMuted, fontSize = DsType.Caption)
        }
    }
}

@Composable
private fun PlayerSocketCard(state: AppState) {
    val sc = surfaceColors()
    val media = state.media
    DsCard {
        Column(Modifier.fillMaxWidth().padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                Text("Player WebSocket", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                DsBadge(text = if (media.wsRunning) "Running" else "Stopped", tint = if (media.wsRunning) sc.textSecondary else sc.textMuted)
                DsButton(
                    text = if (media.wsRunning) "Stop" else "Start",
                    kind = if (media.wsRunning) DsButtonKind.Danger else DsButtonKind.Secondary,
                    compact = true,
                    onClick = { if (media.wsRunning) media.stopPlayerSocket() else media.startPlayerSocket() }
                )
            }
            Text(
                "Broadcasts live player state and accepts commands (play/pause/seek/mine/screenshot) — the transport a browser connector speaks. ws://127.0.0.1:${state.settings.getInt("media.ws.port", 8765)} · ${media.wsClients} client(s).",
                color = sc.textSecondary,
                fontSize = DsType.Body
            )
            Text("Bidirectional · External ↔ Kaiteyo", color = sc.textMuted, fontSize = DsType.Caption)
        }
    }
}

// ------------------------------------------------------------
// System media keys
// ------------------------------------------------------------

@Composable
private fun MediaKeysCard(state: AppState) {
    val sc = surfaceColors()
    val media = state.media
    DsCard {
        Column(Modifier.fillMaxWidth().padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                Text("System media keys", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                val badge = when {
                    !media.systemMediaKeysSupported -> "Unsupported OS"
                    media.systemMediaKeysActive -> "Capturing"
                    else -> "Standby"
                }
                DsBadge(text = badge, tint = if (media.systemMediaKeysActive) sc.textSecondary else sc.textMuted)
            }
            Text(
                "The keyboard's Play/Pause, Next, Previous and Stop keys drive Kaiteyo even without focus (Windows). Toggle in Settings → Media → Playback.",
                color = sc.textSecondary,
                fontSize = DsType.Body
            )
            Text(
                if (media.systemMediaKeysSupported) "Data direction: OS → Kaiteyo" else "macOS/Linux: use the tray controller or in-app media hotkeys.",
                color = sc.textMuted,
                fontSize = DsType.Caption
            )
        }
    }
}

// ------------------------------------------------------------
// Helpers
// ------------------------------------------------------------

private fun copyToClipboard(state: AppState, text: String) {
    val cb = java.awt.Toolkit.getDefaultToolkit().systemClipboard
    cb.setContents(java.awt.datatransfer.StringSelection(text), null)
    state.toastHost.show("Copied")
}

@Composable
private fun RequestSummary(req: IntegrationCardRequest) {
    val sc = surfaceColors()
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
        Text(req.word.ifBlank { "(empty)" }, color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
        if (req.reading.isNotBlank()) Text(req.reading, color = sc.textSecondary, fontSize = DsType.Body)
        DsBadge(text = req.source, tint = sc.textSecondary)
    }
    if (req.definition.isNotBlank()) {
        Text(req.definition, color = sc.textSecondary, fontSize = DsType.Body)
    }
    if (req.sentence.isNotBlank()) {
        Text(req.sentence, color = sc.textMuted, fontSize = DsType.Body)
    }
    Text(
        buildString {
            if (req.tags.isNotEmpty()) append("tags: ").append(req.tags.joinToString(", ")).append("  ·  ")
            if (req.timestamp != null) append("ts: ").append(req.timestamp).append("  ·  ")
            append("deck: ").append(req.deckId.ifBlank { "default" })
        },
        color = sc.textMuted,
        fontSize = DsType.Caption
    )
}
