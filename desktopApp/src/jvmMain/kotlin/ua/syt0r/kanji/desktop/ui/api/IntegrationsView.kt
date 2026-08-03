package ua.syt0r.kanji.desktop.ui.api

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
// KAITEYO INTEGRATIONS
// Local HTTP API for external tools — most notably
// GameSentenceMiner. Start the server, point the
// external tool at the endpoint, and mined words
// arrive in your deck instantly.
// ============================================

@Composable
fun IntegrationsView(state: AppState) {
    val sc = surfaceColors()
    val api = state.localApi

    Column(Modifier.fillMaxSize().padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Lg)) {
        DsSectionHeader(
            title = "Integrations",
            subtitle = "Connect external mining tools to Kaiteyo through a local HTTP API.",
            action = {
                DsButton(
                    text = if (api.running) "Stop server" else "Start server",
                    icon = if (api.running) Icons.Default.Stop else Icons.Default.PlayArrow,
                    kind = if (api.running) DsButtonKind.Danger else DsButtonKind.Primary,
                    onClick = { if (api.running) api.stop() else api.start() }
                )
            }
        )

        DsCard {
            Column(Modifier.fillMaxWidth().padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DsBadge(text = if (api.running) "Running" else "Stopped", tint = if (api.running) sc.textSecondary else sc.textMuted)
                    Spacer(Modifier.height(0.dp))
                }
                Text("Endpoint", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                    Text(api.portInfo, color = sc.textPrimary, fontSize = DsType.Body, modifier = Modifier.weight(1f))
                    DsButton(
                        text = "Copy",
                        icon = Icons.Default.ContentCopy,
                        kind = DsButtonKind.Secondary,
                        compact = true,
                        onClick = {
                            val cb = java.awt.Toolkit.getDefaultToolkit().systemClipboard
                            cb.setContents(java.awt.datatransfer.StringSelection(api.portInfo), null)
                            state.toastHost.show("Endpoint copied")
                        }
                    )
                }
                Text(
                    buildString {
                        appendLine("POST $endpointPath")
                        appendLine()
                        appendLine("GameSentenceMiner sends words/definitions/sentences/screenshots here. A JSON body is accepted:")
                        appendLine()
                        append("{\"word\":\"食べる\",\"reading\":\"たべる\",\"definition\":\"to eat\",\"sentence\":\"朝ごはんを食べる\",\"deckId\":\"default\"}")
                    },
                    color = sc.textMuted,
                    fontSize = DsType.Caption,
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).height(120.dp)
                )
                api.lastError?.let { err ->
                    Text("Last error: $err", color = sc.textPrimary, fontSize = DsType.Caption)
                }
            }
        }

        DsCard {
            Column(Modifier.fillMaxWidth().padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                Text("Last request", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
                api.lastRequest?.let { req -> RequestSummary(req) }
                    ?: Text("No requests yet. Start the server and send a card from your external tool.", color = sc.textMuted, fontSize = DsType.Caption)
            }
        }

        DsCard {
            Column(Modifier.fillMaxWidth().padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                Text("Supported tools", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
                Text("• GameSentenceMiner — sends word + sentence + screenshot on capture.", color = sc.textSecondary, fontSize = DsType.Body)
                Text("• Any script — curl / Python / browser extension can POST to the endpoint.", color = sc.textSecondary, fontSize = DsType.Body)
                Text("• CORS-friendly for browser-based tools; binds to 127.0.0.1 only.", color = sc.textSecondary, fontSize = DsType.Body)
            }
        }
    }
}

private const val endpointPath = "/api/mine"

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
