package ua.syt0r.kanji.desktop.ui.sync

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.appstate.WorkspaceView
import ua.syt0r.kanji.desktop.designsystem.DsBadge
import ua.syt0r.kanji.desktop.designsystem.DsButton
import ua.syt0r.kanji.desktop.designsystem.DsCard
import ua.syt0r.kanji.desktop.designsystem.DsEmptyState
import ua.syt0r.kanji.desktop.designsystem.DsRadius
import ua.syt0r.kanji.desktop.designsystem.DsSectionHeader
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.accent
import ua.syt0r.kanji.desktop.designsystem.errorColor
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import ua.syt0r.kanji.desktop.engine.account.ProviderKind
import ua.syt0r.kanji.desktop.engine.sync.SyncBlob
import ua.syt0r.kanji.desktop.engine.sync.SyncCodec
import ua.syt0r.kanji.desktop.engine.sync.SyncResult
import ua.syt0r.kanji.desktop.ui.account.formatDateTime

// ============================================
// SYNC
// Cloud synchronization driven by the connected
// GitHub account. Push/pull the same versioned
// blobs (cards, review log, daily summaries)
// used by the Account → Sync section.
// ============================================

@Composable
fun SyncView(state: AppState) {
    val sc = surfaceColors()
    val scope = rememberCoroutineScope()
    val connections by state.account.connections.collectAsState()
    val settingsData by state.account.settingsData.collectAsState()
    val github = connections.firstOrNull { it.kind == ProviderKind.GitHub }
    val connected = github?.isConnected == true
    val codec = remember { SyncCodec() }
    var lastResult by remember { mutableStateOf<SyncResult?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(DsSpacing.Lg)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(DsSpacing.Lg)
    ) {
        DsSectionHeader(
            title = "Sync",
            subtitle = if (connected) "Pushing and pulling with ${github?.displayName}" else state.lastSyncMessage,
            action = {
                DsButton(
                    text = if (state.syncBusy) "Syncing…" else "Sync now",
                    icon = Icons.Default.CloudSync,
                    onClick = {
                        if (state.syncBusy) return@DsButton
                        scope.launch {
                            lastResult = state.cloudSync.syncNow(manual = true)
                        }
                    },
                    enabled = connected && !state.syncBusy
                )
            }
        )

        // ── Destination / connection ──
        DsCard {
            if (!connected) {
                DsEmptyState(
                    title = "No cloud account connected",
                    message = "Connect a GitHub account in Account → Connected accounts to sync your " +
                        "cards, review history, decks, collections, saved filters and settings across devices.",
                    action = {
                        DsButton(
                            text = "Open Account",
                            icon = Icons.Default.Link,
                            onClick = { state.currentView = WorkspaceView.Account }
                        )
                    }
                )
            } else {
                Column(
                    modifier = Modifier.padding(DsSpacing.Xl),
                    verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CloudSync,
                            contentDescription = null,
                            tint = accent().primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(DsSpacing.Sm))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = github?.displayName ?: "GitHub",
                                color = sc.textPrimary,
                                fontSize = DsType.BodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Private gist · GitHub account · ${github?.userId?.take(8) ?: ""}",
                                color = sc.textMuted,
                                fontSize = DsType.Caption
                            )
                        }
                        if (state.syncBusy) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            DsBadge(text = "Connected", tint = Color(0xFFC2FC8B))
                        }
                    }
                    if (github?.errorMessage?.isNotBlank() == true) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = errorColor(), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(DsSpacing.Sm))
                            Text(github.errorMessage, color = errorColor(), fontSize = DsType.Caption)
                        }
                    }
                    Text(
                        text = state.lastSyncMessage,
                        color = sc.textMuted,
                        fontSize = DsType.Caption
                    )
                }
            }
        }

        // ── Payload ──
        if (connected) {
            val manifest = remember(
                state.cards.size,
                state.reviewLog.size,
                state.summaries.size,
                state.library.decks.size,
                state.collections.collections.size,
                state.filterStore.saved.size
            ) {
                codec.manifest(
                    cards = state.cards.toList(),
                    reviewLog = state.reviewLog.toList(),
                    summaries = state.summaries.toList(),
                    lastSeen = null,
                    decks = state.library.decks.toList(),
                    collections = state.collections.collections,
                    savedFilters = state.filterStore.saved,
                    settings = state.cloudSync.portableSettings()
                )
            }
            DsCard {
                Column(
                    modifier = Modifier.padding(DsSpacing.Xl),
                    verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)
                ) {
                    DsSectionHeader(
                        title = "Payload",
                        subtitle = "${state.cards.size} cards · ${state.reviewLog.size} reviews · " +
                            "${state.summaries.size} summaries · ${state.library.decks.size} decks · " +
                            "${state.collections.collections.size} collections · " +
                            "${state.filterStore.saved.size} saved filters · " +
                            "${state.cloudSync.portableSettings().size} settings"
                    )
                    manifest.blobs.forEach { blob ->
                        BlobRow(blob)
                    }
                    lastResult?.let { result ->
                        Text(
                            text = "Last run — pushed ${result.pushed}, pulled ${result.pulled}, skipped ${result.skipped}",
                            color = sc.textSecondary,
                            fontSize = DsType.Caption
                        )
                    }
                    Text(
                        text = "Last synced ${state.lastSyncAt?.let { formatDateTime(it.toEpochMilliseconds()) } ?: "never"}",
                        color = sc.textMuted,
                        fontSize = DsType.Caption
                    )
                }
            }
        }

        // ── Automation ──
        DsCard {
            Column(
                modifier = Modifier.padding(DsSpacing.Xl),
                verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
            ) {
                Text(
                    text = "Automatic sync",
                    color = sc.textPrimary,
                    fontSize = DsType.Body,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (settingsData.autoSync)
                        "Every ${settingsData.syncIntervalMinutes} minutes while connected"
                    else if (settingsData.syncOnStart)
                        "On app start"
                    else
                        "Manual only — use Sync now above or in Account → Sync",
                    color = sc.textMuted,
                    fontSize = DsType.Caption
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (connected) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = if (connected) Color(0xFFC2FC8B) else errorColor(),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(DsSpacing.Sm))
                    Text(
                        text = if (connected)
                            "Ready to sync — blobs are versioned · conflicts: ${settingsData.conflictResolution.label}"
                        else
                            "Connect a GitHub account to enable syncing",
                        color = sc.textMuted,
                        fontSize = DsType.Caption
                    )
                }
            }
        }
    }
}

@Composable
private fun BlobRow(blob: SyncBlob) {
    val sc = surfaceColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DsRadius.Md))
            .background(sc.surfaceInteractive.copy(alpha = 0.35f))
            .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(blob.name, color = sc.textPrimary, fontSize = DsType.Body, modifier = Modifier.weight(1f))
        Text("v${blob.version}", color = sc.textMuted, fontSize = DsType.Caption)
        Spacer(Modifier.width(DsSpacing.Md))
        Text("${blob.payload.length} bytes", color = sc.textMuted, fontSize = DsType.Caption)
    }
}
