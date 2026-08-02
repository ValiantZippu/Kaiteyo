package ua.syt0r.kanji.desktop.ui.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.designsystem.DsButton
import ua.syt0r.kanji.desktop.designsystem.DsButtonKind
import ua.syt0r.kanji.desktop.designsystem.DsCard
import ua.syt0r.kanji.desktop.designsystem.DsConfirmDialog
import ua.syt0r.kanji.desktop.designsystem.DsEmptyState
import ua.syt0r.kanji.desktop.designsystem.DsIconButton
import ua.syt0r.kanji.desktop.designsystem.DsPromptDialog
import ua.syt0r.kanji.desktop.designsystem.DsSectionHeader
import ua.syt0r.kanji.desktop.designsystem.DsSelect
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsTextField
import ua.syt0r.kanji.desktop.designsystem.DsToggle
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import ua.syt0r.kanji.desktop.engine.history.ActivityCategory
import ua.syt0r.kanji.desktop.engine.sync.ConflictResolution
import ua.syt0r.kanji.desktop.engine.sync.SyncBlob
import ua.syt0r.kanji.desktop.engine.sync.SyncCodec
import ua.syt0r.kanji.desktop.engine.sync.SyncEngine
import ua.syt0r.kanji.desktop.engine.sync.SyncManifest
import ua.syt0r.kanji.desktop.engine.sync.SyncProfile
import ua.syt0r.kanji.desktop.engine.sync.SyncProviderType
import ua.syt0r.kanji.desktop.engine.sync.SyncResult
import ua.syt0r.kanji.desktop.engine.sync.SyncTransport
import ua.syt0r.kanji.desktop.model.ToastKind

// ============================================
// SYNC
// Provider profiles + one-click reconciliation.
// A memory transport stands in for real cloud
// providers; the diff engine is fully real.
// ============================================

@Composable
fun SyncView(state: AppState) {
    val sc = surfaceColors()
    val scope = rememberCoroutineScope()
    val transport = remember { MemoryTransport() }
    val profiles = remember { mutableStateListOf<SyncProfile>() }
    val codec = remember { SyncCodec() }
    var showAdd by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<SyncProfile?>(null) }
    var syncDone by remember { mutableStateOf<SyncResult?>(null) }

    Column(Modifier.fillMaxSize().padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Lg)) {
        DsSectionHeader(
            title = "Sync",
            subtitle = state.lastSyncMessage,
            action = {
                DsButton(
                    text = "Add profile",
                    icon = Icons.Default.Add,
                    kind = DsButtonKind.Ghost,
                    onClick = { showAdd = true },
                    compact = true
                )
            }
        )

        if (profiles.isEmpty()) {
            DsCard {
                DsEmptyState(
                    title = "No sync profiles",
                    message = "Add a profile to start syncing your deck across devices.",
                    action = {
                        DsButton(text = "Add profile", icon = Icons.Default.Add, onClick = { showAdd = true })
                    }
                )
            }
        } else {
            profiles.forEachIndexed { index, profile ->
                ProfileCard(
                    profile = profile,
                    onUpdate = { updated -> profiles[index] = updated },
                    onDelete = { deleteTarget = profile }
                )
            }
        }

        DsCard {
            Column(Modifier.padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Sync now", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
                        Text("Push and pull ${state.cards.size} cards, ${state.reviewLog.size} reviews, ${state.summaries.size} daily summaries", color = sc.textMuted, fontSize = DsType.Caption)
                    }
                    DsButton(
                        text = if (state.syncBusy) "Syncing…" else "Sync now",
                        icon = Icons.Default.CloudSync,
                        onClick = {
                            if (state.syncBusy) return@DsButton
                            state.syncBusy = true
                            scope.launch {
                                val manifest = codec.manifest(
                                    cards = state.cards.toList(),
                                    reviewLog = state.reviewLog.toList(),
                                    summaries = state.summaries.toList()
                                )
                                val result = try {
                                    state.syncEngine.reconcile(transport, manifest, ConflictResolution.LocalWins)
                                } finally {
                                    state.syncBusy = false
                                }
                                state.lastSyncAt = Clock.System.now()
                                state.lastSyncMessage = "Synced ${manifest.blobs.size} blobs — pushed ${result.pushed}, pulled ${result.pulled}, skipped ${result.skipped}"
                                state.activityLog.record(
                                    ActivityCategory.Sync,
                                    "Sync completed",
                                    details = "pushed ${result.pushed}, pulled ${result.pulled}, skipped ${result.skipped}",
                                    affectedCount = result.pushed + result.pulled
                                )
                                syncDone = result
                                state.toastHost.show(state.lastSyncMessage, kind = ToastKind.Success)
                            }
                        },
                        enabled = !state.syncBusy
                    )
                }

                val manifest = remember(state.cards.size, state.reviewLog.size, state.summaries.size) {
                    codec.manifest(
                        cards = state.cards.toList(),
                        reviewLog = state.reviewLog.toList(),
                        summaries = state.summaries.toList()
                    )
                }
                Text("Payload", color = sc.textMuted, fontSize = DsType.Caption, fontWeight = FontWeight.SemiBold)
                manifest.blobs.forEach { blob ->
                    BlobRow(blob)
                }
                syncDone?.let { result ->
                    Text(
                        text = "Last run — pushed ${result.pushed}, pulled ${result.pulled}, skipped ${result.skipped}",
                        color = sc.textSecondary,
                        fontSize = DsType.Caption
                    )
                }
            }
        }
    }

    if (showAdd) {
        DsPromptDialog(
            title = "New sync profile",
            placeholder = "Profile name (e.g. Work laptop)",
            onConfirm = { name ->
                if (name.isNotBlank()) {
                    profiles.add(
                        SyncProfile(
                            id = "profile-${Clock.System.now().toEpochMilliseconds()}",
                            name = name,
                            provider = SyncProviderType.LocalFolder
                        )
                    )
                    state.activityLog.record(ActivityCategory.Sync, "Added sync profile '$name'")
                }
            },
            onDismiss = { showAdd = false }
        )
    }

    deleteTarget?.let { target ->
        DsConfirmDialog(
            title = "Delete sync profile",
            message = "Remove '${target.name}'? The local deck is never touched.",
            confirmText = "Delete",
            danger = true,
            onConfirm = {
                profiles.removeAll { it.id == target.id }
                state.activityLog.record(ActivityCategory.Sync, "Removed sync profile '${target.name}'")
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null }
        )
    }
}

@Composable
private fun ProfileCard(
    profile: SyncProfile,
    onUpdate: (SyncProfile) -> Unit,
    onDelete: () -> Unit
) {
    val sc = surfaceColors()
    DsCard {
        Column(Modifier.padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(profile.name, color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
                    Text(profile.provider.name + if (profile.endpoint.isNotBlank()) " · ${profile.endpoint}" else "", color = sc.textMuted, fontSize = DsType.Caption)
                }
                DsIconButton(icon = Icons.Default.Delete, onClick = onDelete, contentDescription = "Delete profile", size = 30.dp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md), verticalAlignment = Alignment.CenterVertically) {
                DsSelect(
                    selected = profile.provider,
                    options = SyncProviderType.entries.toList(),
                    onSelected = { onUpdate(profile.copy(provider = it)) },
                    labelOf = { it.name },
                    modifier = Modifier.width(170.dp)
                )
                val endpoint = remember(profile.id) { mutableStateOf(profile.endpoint) }
                DsTextField(
                    value = endpoint.value,
                    onValueChange = { endpoint.value = it; onUpdate(profile.copy(endpoint = it)) },
                    placeholder = "Endpoint / path",
                    singleLine = true
                )
                DsToggle(
                    checked = profile.enabled,
                    onCheckedChange = { onUpdate(profile.copy(enabled = it)) },
                    label = "Enabled"
                )
                DsToggle(
                    checked = profile.autoSync,
                    onCheckedChange = { onUpdate(profile.copy(autoSync = it)) },
                    label = "Auto"
                )
            }
        }
    }
}

@Composable
private fun BlobRow(blob: SyncBlob) {
    val sc = surfaceColors()
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(blob.name, color = sc.textPrimary, fontSize = DsType.Body, modifier = Modifier.weight(1f))
        Text("v${blob.version}", color = sc.textMuted, fontSize = DsType.Caption)
        Spacer(Modifier.width(DsSpacing.Md))
        Text("${blob.payload.length} bytes", color = sc.textMuted, fontSize = DsType.Caption)
    }
}

/** In-memory transport used as a stand-in for a real cloud provider. */
private class MemoryTransport : SyncTransport {
    private val storage = mutableMapOf<String, SyncBlob>()

    override val type: SyncProviderType = SyncProviderType.CustomServer

    override suspend fun list(): List<SyncBlob> = storage.values.toList()

    override suspend fun download(name: String): SyncBlob = storage[name] ?: SyncBlob(name)

    override suspend fun upload(blob: SyncBlob): Long {
        val version = (storage[blob.name]?.version ?: 0) + 1
        storage[blob.name] = blob.copy(version = version, modifiedAt = Clock.System.now())
        return version
    }

    override suspend fun delete(name: String) {
        storage.remove(name)
    }

    override suspend fun testConnection(): Result<String> =
        Result.success("Connected (in-memory transport)")
}
