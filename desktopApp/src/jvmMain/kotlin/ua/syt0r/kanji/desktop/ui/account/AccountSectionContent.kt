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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VpnKey
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.appstate.WorkspaceView
import ua.syt0r.kanji.desktop.designsystem.DsBadge
import ua.syt0r.kanji.desktop.designsystem.DsButton
import ua.syt0r.kanji.desktop.designsystem.DsButtonKind
import ua.syt0r.kanji.desktop.designsystem.DsCard
import ua.syt0r.kanji.desktop.designsystem.DsConfirmDialog
import ua.syt0r.kanji.desktop.designsystem.DsEmptyState
import ua.syt0r.kanji.desktop.designsystem.DsIconButton
import ua.syt0r.kanji.desktop.designsystem.DsPromptDialog
import ua.syt0r.kanji.desktop.designsystem.DsRadius
import ua.syt0r.kanji.desktop.designsystem.DsSectionHeader
import ua.syt0r.kanji.desktop.designsystem.DsSelect
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsTextField
import ua.syt0r.kanji.desktop.designsystem.DsToggle
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.accent
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import ua.syt0r.kanji.desktop.engine.account.AccountDevice
import ua.syt0r.kanji.desktop.engine.account.AccountEngine
import ua.syt0r.kanji.desktop.engine.account.ConnectionStatus
import ua.syt0r.kanji.desktop.engine.account.ProviderConnection
import ua.syt0r.kanji.desktop.engine.account.ProviderKind
import ua.syt0r.kanji.desktop.engine.history.ActivityCategory
import ua.syt0r.kanji.desktop.engine.sync.ConflictResolution
import ua.syt0r.kanji.desktop.engine.sync.SyncResult
import ua.syt0r.kanji.desktop.model.DeckDef
import ua.syt0r.kanji.desktop.model.DesktopCard
import ua.syt0r.kanji.desktop.model.ReviewLogEntry
import ua.syt0r.kanji.desktop.model.StudyDaySummary
import ua.syt0r.kanji.desktop.model.ToastKind
import java.io.File

// ============================================
// ACCOUNT — SECTIONS
// Profile, Connected accounts, Devices,
// Sessions, Backups, Sync, Security, Privacy,
// Notifications and Developer options.
// ============================================

private val json = Json { prettyPrint = true; encodeDefaults = true; ignoreUnknownKeys = true }

@Serializable
private data class BackupSnapshot(
    val createdAt: String,
    val cards: List<DesktopCard>,
    val decks: List<DeckDef>,
    val reviewLog: List<ReviewLogEntry>,
    val summaries: List<StudyDaySummary>
)

private fun dataRoot(): File = File(System.getProperty("user.home"), ".kaiteyo")
private fun backupsDir(): File = File(dataRoot(), "backups").apply { mkdirs() }

// ============================================
// PROFILE
// ============================================

@Composable
fun AccountProfileSection(state: AppState, engine: AccountEngine) {
    val sc = surfaceColors()
    val identity by engine.identity.collectAsState()

    var displayName by remember(identity.displayName) { mutableStateOf(identity.displayName) }
    var username by remember(identity.username) { mutableStateOf(identity.username) }
    var email by remember(identity.email) { mutableStateOf(identity.email) }
    var learnerLevel by remember(identity.learnerLevel) { mutableStateOf(identity.learnerLevel) }

    val dirty = displayName != identity.displayName ||
        username != identity.username ||
        email != identity.email ||
        learnerLevel != identity.learnerLevel

    DsCard {
        Column(
            modifier = Modifier.padding(DsSpacing.Xl),
            verticalArrangement = Arrangement.spacedBy(DsSpacing.Lg)
        ) {
            DsSectionHeader(title = "Profile", subtitle = "How you appear in Kaiteyo")

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DsSpacing.Lg)
            ) {
                AccountAvatar(name = displayName, seed = identity.avatarSeed, size = 72.dp)
                Column(verticalArrangement = Arrangement.spacedBy(DsSpacing.Xs)) {
                    Text("Account type", color = sc.textMuted, fontSize = DsType.Caption)
                    Text(
                        text = if (identity.isLocalOnly) "Local profile — fully offline" else "Cloud-linked profile",
                        color = sc.textPrimary,
                        fontSize = DsType.Body,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            DsTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = "Display name",
                placeholder = "Your name"
            )
            DsTextField(
                value = username,
                onValueChange = { username = it },
                label = "Username",
                placeholder = "username"
            )
            DsTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email",
                placeholder = "you@example.com"
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)
            ) {
                Text("Learner level", color = sc.textSecondary, fontSize = DsType.Body)
                DsSelect(
                    selected = learnerLevel,
                    options = listOf("beginner", "intermediate", "advanced"),
                    onSelected = { learnerLevel = it },
                    labelOf = { it.replaceFirstChar { c -> c.uppercase() } },
                    modifier = Modifier.width(220.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm, Alignment.End)
            ) {
                DsButton(
                    text = "Save changes",
                    icon = Icons.Default.Check,
                    onClick = {
                        engine.updateProfile(displayName, username, email, learnerLevel)
                        state.toastHost.show("Profile updated", kind = ToastKind.Success)
                    },
                    enabled = dirty
                )
            }

            Text(
                text = "Member since ${formatDate(identity.joinedAtEpochMs)}",
                color = sc.textMuted,
                fontSize = DsType.Caption
            )
        }
    }
}

// ============================================
// CONNECTED ACCOUNTS (PROVIDERS)
// ============================================

@Composable
fun AccountProvidersSection(state: AppState, engine: AccountEngine) {
    val sc = surfaceColors()
    val connections by engine.connections.collectAsState()

    DsCard {
        Column(
            modifier = Modifier.padding(DsSpacing.Xl),
            verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)
        ) {
            DsSectionHeader(title = "Connected accounts", subtitle = "Link providers to enable cloud features")
            connections.forEach { connection ->
                ProviderRow(state, engine, connection)
            }
        }
    }

    DsCard {
        Column(
            modifier = Modifier.padding(DsSpacing.Xl),
            verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
        ) {
            Text("Why connect an account?", color = sc.textPrimary, fontSize = DsType.Body, fontWeight = FontWeight.SemiBold)
            Text(
                text = "Cloud accounts are optional. They will enable synchronization of your decks, cards, " +
                    "settings and themes across devices. Kaiteyo works fully offline without one.",
                color = sc.textMuted,
                fontSize = DsType.Body
            )
        }
    }
}

@Composable
private fun ProviderRow(state: AppState, engine: AccountEngine, connection: ProviderConnection) {
    val sc = surfaceColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DsRadius.Md))
            .background(sc.surfaceInteractive.copy(alpha = 0.35f))
            .padding(DsSpacing.Md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)
    ) {
        ProviderLogo(kind = connection.kind)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
            ) {
                Text(
                    text = connection.kind.displayName,
                    color = sc.textPrimary,
                    fontSize = DsType.BodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                if (connection.isConnected) {
                    DsBadge(text = "Connected", tint = Color(0xFFC2FC8B))
                }
            }
            Text(
                text = providerSubtitle(connection),
                color = sc.textMuted,
                fontSize = DsType.Caption
            )
        }
        ProviderActions(engine, connection)
    }
}

@Composable
private fun ProviderActions(engine: AccountEngine, connection: ProviderConnection) {
    val sc = surfaceColors()
    when {
        connection.kind == ProviderKind.Local -> {
            DsBadge(text = "Always on", tint = Color(0xFFC2FC8B))
        }

        connection.isConnected -> {
            Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs)) {
                DsButton(
                    text = "Reconnect",
                    kind = DsButtonKind.Secondary,
                    compact = true,
                    onClick = { engine.reconnectProvider(connection.kind) }
                )
                DsButton(
                    text = "Disconnect",
                    kind = DsButtonKind.Ghost,
                    compact = true,
                    onClick = { engine.disconnectProvider(connection.kind) }
                )
            }
        }

        connection.status == ConnectionStatus.NotConfigured ||
            connection.kind == ProviderKind.GitHub -> {
            DsButton(
                text = "Connect",
                kind = DsButtonKind.Primary,
                compact = true,
                onClick = { engine.connectGitHub() }
            )
        }

        else -> {
            DsBadge(text = "Coming soon", tint = sc.textMuted)
        }
    }
}

private fun providerSubtitle(connection: ProviderConnection): String = when {
    connection.isConnected -> listOfNotNull(
        connection.displayName.takeIf { it.isNotBlank() },
        connection.lastUsedAtEpochMs.takeIf { it > 0 }?.let { "last used ${formatDateTime(it)}" }
    ).joinToString(" · ")

    connection.status == ConnectionStatus.NotConfigured ->
        connection.kind.tagline + " — add a client ID in Developer options to enable"

    connection.kind == ProviderKind.Local -> connection.kind.tagline
    connection.kind == ProviderKind.GitHub -> connection.kind.tagline
    else -> connection.kind.tagline + " — integration in progress"
}

@Composable
private fun ProviderLogo(kind: ProviderKind) {
    val color = when (kind) {
        ProviderKind.GitHub -> Color(0xFFA78BFA)
        ProviderKind.Google -> Color(0xFF7BC8FF)
        ProviderKind.Apple -> Color(0xFFB0B0B0)
        ProviderKind.Microsoft -> Color(0xFFFEAB57)
        ProviderKind.Local -> Color(0xFFC2FC8B)
    }
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(DsRadius.Md))
            .background(color.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = when (kind) {
                ProviderKind.GitHub -> "G"
                ProviderKind.Google -> "G"
                ProviderKind.Apple -> "A"
                ProviderKind.Microsoft -> "M"
                ProviderKind.Local -> "K"
            },
            color = color,
            fontSize = DsType.BodyLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

// ============================================
// DEVICES
// ============================================

@Composable
fun AccountDevicesSection(state: AppState, engine: AccountEngine) {
    val sc = surfaceColors()
    val devices by engine.devices.collectAsState()
    var renameTarget by remember { mutableStateOf<AccountDevice?>(null) }
    var removeTarget by remember { mutableStateOf<AccountDevice?>(null) }

    DsCard {
        Column(
            modifier = Modifier.padding(DsSpacing.Xl),
            verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)
        ) {
            DsSectionHeader(
                title = "Devices",
                subtitle = "${devices.size} registered device${if (devices.size == 1) "" else "s"}"
            )
            if (devices.isEmpty()) {
                DsEmptyState(title = "No devices", message = "This device registers itself on the next launch.")
            } else {
                devices.forEach { device ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(DsRadius.Md))
                            .background(sc.surfaceInteractive.copy(alpha = 0.35f))
                            .padding(DsSpacing.Md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)
                    ) {
                        Icon(
                            imageVector = deviceIcon(device.platform),
                            contentDescription = null,
                            tint = sc.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
                            ) {
                                Text(
                                    text = device.name,
                                    color = sc.textPrimary,
                                    fontSize = DsType.Body,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (device.isCurrent) DsBadge(text = "This device", tint = accent().primary)
                                if (!device.isTrusted) DsBadge(text = "Logged out", tint = sc.textMuted)
                            }
                            Text(
                                text = listOfNotNull(
                                    device.platform,
                                    device.appVersion.takeIf { it.isNotBlank() }?.let { "v$it" },
                                    "db v${device.databaseVersion}",
                                    device.lastSyncEpochMs.takeIf { it > 0 }
                                        ?.let { "synced ${formatDateTime(it)}" } ?: "never synced"
                                ).joinToString(" · "),
                                color = sc.textMuted,
                                fontSize = DsType.Caption
                            )
                        }
                        DsIconButton(
                            icon = Icons.Default.Edit,
                            onClick = { renameTarget = device },
                            contentDescription = "Rename device",
                            size = 30.dp
                        )
                        if (!device.isCurrent) {
                            DsIconButton(
                                icon = Icons.Default.Delete,
                                onClick = { removeTarget = device },
                                contentDescription = "Remove device",
                                size = 30.dp
                            )
                        }
                    }
                }
            }
        }
    }

    renameTarget?.let { device ->
        DsPromptDialog(
            title = "Rename device",
            placeholder = "Device name",
            initialValue = device.name,
            onConfirm = { name ->
                engine.renameDevice(device.id, name)
                renameTarget = null
            },
            onDismiss = { renameTarget = null }
        )
    }

    removeTarget?.let { device ->
        DsConfirmDialog(
            title = "Remove device",
            message = "Remove '${device.name}'? Sessions on that device will be signed out.",
            confirmText = "Remove",
            danger = true,
            onConfirm = {
                engine.removeDevice(device.id)
                removeTarget = null
            },
            onDismiss = { removeTarget = null }
        )
    }
}

private fun deviceIcon(platform: String): ImageVector = when {
    platform.equals("Windows", ignoreCase = true) ||
        platform.equals("Linux", ignoreCase = true) ||
        platform.equals("macOS", ignoreCase = true) -> Icons.Default.Computer

    else -> Icons.Default.Devices
}

// ============================================
// SESSIONS
// ============================================

@Composable
fun AccountSessionsSection(state: AppState, engine: AccountEngine) {
    val sc = surfaceColors()
    val sessions by engine.sessions.collectAsState()
    val devices by engine.devices.collectAsState()
    var confirmAll by remember { mutableStateOf(false) }

    DsCard {
        Column(
            modifier = Modifier.padding(DsSpacing.Xl),
            verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)
        ) {
            DsSectionHeader(title = "Sessions", subtitle = "Where you are currently signed in")
            if (sessions.isEmpty()) {
                DsEmptyState(title = "No sessions", message = "A local session is created automatically.")
            } else {
                sessions.forEach { session ->
                    val deviceName = devices.firstOrNull { it.id == session.deviceId }?.name ?: "Unknown device"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(DsRadius.Md))
                            .background(sc.surfaceInteractive.copy(alpha = 0.35f))
                            .padding(DsSpacing.Md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VpnKey,
                            contentDescription = null,
                            tint = accent().primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
                            ) {
                                Text(
                                    text = "${session.providerKind.displayName} · $deviceName",
                                    color = sc.textPrimary,
                                    fontSize = DsType.Body,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (session.isCurrent) DsBadge(text = "Current", tint = accent().primary)
                            }
                            Text(
                                text = "Signed in ${formatDateTime(session.createdAtEpochMs)} · " +
                                    "last active ${formatDateTime(session.lastActiveAtEpochMs)}",
                                color = sc.textMuted,
                                fontSize = DsType.Caption
                            )
                        }
                        DsButton(
                            text = "Sign out",
                            kind = DsButtonKind.Ghost,
                            compact = true,
                            onClick = { engine.signOutOfSession(session.id) }
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm, Alignment.End)
            ) {
                DsButton(
                    text = "Sign out other sessions",
                    kind = DsButtonKind.Secondary,
                    onClick = { engine.signOutOfOtherSessions() }
                )
                DsButton(
                    text = "Sign out all devices",
                    kind = DsButtonKind.Danger,
                    onClick = { confirmAll = true }
                )
            }
        }
    }

    if (confirmAll) {
        DsConfirmDialog(
            title = "Sign out everywhere",
            message = "This signs out every session on every device and removes stored credentials. " +
                "Your local study data is untouched.",
            confirmText = "Sign out all",
            danger = true,
            onConfirm = {
                engine.signOutOfAllSessions()
                state.toastHost.show("Signed out of all sessions", kind = ToastKind.Info)
            },
            onDismiss = { confirmAll = false }
        )
    }
}

// ============================================
// BACKUPS
// ============================================

@Composable
fun AccountBackupsSection(state: AppState) {
    val sc = surfaceColors()
    var revision by remember { mutableStateOf(0) }
    val backups = remember(revision) {
        backupsDir().listFiles { f -> f.isFile && f.extension == "json" }
            ?.sortedByDescending { it.lastModified() } ?: emptyList()
    }
    val lastBackup = state.settings.getString("account.last-backup-at")

    DsCard {
        Column(
            modifier = Modifier.padding(DsSpacing.Xl),
            verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)
        ) {
            DsSectionHeader(title = "Backups", subtitle = "Snapshots of your cards, decks and progress")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
            ) {
                Text(
                    text = if (lastBackup.isNotBlank())
                        "Last backup: ${formatDateTime(lastBackup.toLongOrNull() ?: 0L)}"
                    else "No backups yet",
                    color = if (lastBackup.isNotBlank()) Color(0xFFC2FC8B) else sc.textMuted,
                    fontSize = DsType.Caption,
                    modifier = Modifier.weight(1f)
                )
                DsButton(
                    text = "Open folder",
                    icon = Icons.Default.FolderOpen,
                    kind = DsButtonKind.Ghost,
                    compact = true,
                    onClick = {
                        runCatching { java.awt.Desktop.getDesktop().open(backupsDir()) }
                            .onFailure { state.toastHost.show("Could not open the backups folder", kind = ToastKind.Warning) }
                    }
                )
                DsButton(
                    text = "Back up now",
                    icon = Icons.Default.Backup,
                    onClick = { createBackup(state) { revision++ } }
                )
            }

            if (backups.isEmpty()) {
                DsEmptyState(
                    title = "No backups",
                    message = "Create your first backup to protect your study data.",
                    icon = Icons.Default.Backup
                )
            } else {
                backups.take(12).forEach { file ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(DsRadius.Md))
                            .background(sc.surfaceInteractive.copy(alpha = 0.4f))
                            .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Backup,
                            contentDescription = null,
                            tint = accent().primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(DsSpacing.Sm))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = file.name,
                                color = sc.textPrimary,
                                fontSize = DsType.Body,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${formatDateTime(file.lastModified())} · ${formatBytes(file.length())}",
                                color = sc.textMuted,
                                fontSize = DsType.Caption
                            )
                        }
                        DsIconButton(
                            icon = Icons.Default.Delete,
                            onClick = {
                                runCatching { file.delete() }
                                revision++
                                state.toastHost.show("Backup deleted", kind = ToastKind.Info)
                            },
                            contentDescription = "Delete backup",
                            size = 28.dp
                        )
                    }
                }
            }
        }
    }
}

private fun createBackup(state: AppState, onDone: () -> Unit) {
    val file = File(backupsDir(), "backup-${Clock.System.now().toEpochMilliseconds()}.json")
    val snapshot = BackupSnapshot(
        createdAt = Clock.System.now().toString(),
        cards = state.cards.toList(),
        decks = state.library.decks.toList(),
        reviewLog = state.reviewLog.toList(),
        summaries = state.summaries.toList()
    )
    runCatching { file.writeText(json.encodeToString(snapshot)) }
        .onSuccess {
            state.settings.set("account.last-backup-at", Clock.System.now().toEpochMilliseconds().toString())
            state.activityLog.record(ActivityCategory.System, "Created backup ${file.name}")
            state.toastHost.show("Backup saved", kind = ToastKind.Success)
            onDone()
        }
        .onFailure { state.toastHost.show("Backup failed: ${it.message}", kind = ToastKind.Error) }
}

// ============================================
// SYNC
// ============================================

@Composable
fun AccountSyncSection(state: AppState, engine: AccountEngine) {
    val sc = surfaceColors()
    val scope = rememberCoroutineScope()
    val settingsData by engine.settingsData.collectAsState()
    val connections by engine.connections.collectAsState()
    val github = connections.firstOrNull { it.kind == ProviderKind.GitHub }
    val connected = github?.isConnected == true
    var lastResult by remember { mutableStateOf<SyncResult?>(null) }

    DsCard {
        Column(
            modifier = Modifier.padding(DsSpacing.Xl),
            verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)
        ) {
            DsSectionHeader(
                title = "Sync",
                subtitle = "Synchronize your study data across devices",
                action = {
                    DsButton(
                        text = "Open Sync view",
                        icon = Icons.Default.Sync,
                        kind = DsButtonKind.Secondary,
                        compact = true,
                        onClick = { state.currentView = WorkspaceView.Sync }
                    )
                }
            )

            // Connection status + one-click sync.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(DsRadius.Md))
                    .background(sc.surfaceInteractive.copy(alpha = 0.35f))
                    .padding(DsSpacing.Md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = null,
                    tint = if (connected) accent().primary else sc.textMuted,
                    modifier = Modifier.size(18.dp)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = if (connected) "Connected as ${github?.displayName}" else "No cloud account",
                        color = sc.textPrimary,
                        fontSize = DsType.Body,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = state.lastSyncMessage,
                        color = sc.textMuted,
                        fontSize = DsType.Caption
                    )
                }
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

            lastResult?.let { result ->
                Text(
                    text = "Last run — pushed ${result.pushed}, pulled ${result.pulled}, skipped ${result.skipped}",
                    color = if (result.pushed + result.pulled > 0) Color(0xFFC2FC8B) else sc.textMuted,
                    fontSize = DsType.Caption
                )
            }

            DsToggle(
                checked = settingsData.autoSync,
                onCheckedChange = { engine.updateSettings { s -> s.copy(autoSync = it) } },
                label = "Automatic sync"
            )

            if (settingsData.autoSync) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)
                ) {
                    Text("Interval", color = sc.textSecondary, fontSize = DsType.Body)
                    DsSelect(
                        selected = settingsData.syncIntervalMinutes,
                        options = listOf(5, 15, 30, 60, 360),
                        onSelected = { engine.updateSettings { s -> s.copy(syncIntervalMinutes = it) } },
                        labelOf = { minutes ->
                            when (minutes) {
                                60 -> "Every hour"
                                360 -> "Every 6 hours"
                                else -> "Every $minutes min"
                            }
                        },
                        modifier = Modifier.width(200.dp)
                    )
                }
            }

            DsToggle(
                checked = settingsData.syncOnStart,
                onCheckedChange = { engine.updateSettings { s -> s.copy(syncOnStart = it) } },
                label = "Sync on app start"
            )

            // Conflict resolution policy for blobs changed on both sides.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Conflict resolution",
                        color = sc.textPrimary,
                        fontSize = DsType.Body,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "When the same data changed on two devices",
                        color = sc.textMuted,
                        fontSize = DsType.Caption
                    )
                }
                DsSelect(
                    selected = settingsData.conflictResolution,
                    options = listOf(
                        ConflictResolution.Skip,
                        ConflictResolution.LocalWins,
                        ConflictResolution.RemoteWins
                    ),
                    onSelected = { engine.updateSettings { s -> s.copy(conflictResolution = it) } },
                    labelOf = { it.label },
                    modifier = Modifier.width(200.dp)
                )
            }

            Text(
                text = "Last sync: ${state.lastSyncAt?.let { formatDateTime(it.toEpochMilliseconds()) } ?: "never"} · " +
                    state.lastSyncMessage,
                color = sc.textMuted,
                fontSize = DsType.Caption
            )
            Text(
                text = "Cards, review history, daily summaries, decks, collections, saved filters and " +
                    "settings sync as versioned blobs. Last-write-wins breaks ties by newest content timestamp.",
                color = sc.textMuted,
                fontSize = DsType.Caption
            )
        }
    }
}

// ============================================
// SECURITY
// ============================================

@Composable
fun AccountSecuritySection(state: AppState, engine: AccountEngine) {
    val sc = surfaceColors()
    val settingsData by engine.settingsData.collectAsState()
    val connections by engine.connections.collectAsState()
    var confirmAll by remember { mutableStateOf(false) }
    val stored = connections.filter { it.isConnected && it.kind != ProviderKind.Local }

    DsCard {
        Column(
            modifier = Modifier.padding(DsSpacing.Xl),
            verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)
        ) {
            DsSectionHeader(title = "Security", subtitle = "Protect your account and data")

            DsToggle(
                checked = settingsData.encryptLocalData,
                onCheckedChange = { engine.updateSettings { s -> s.copy(encryptLocalData = it) } },
                label = "Encrypt local data at rest"
            )
            Text(
                text = "Credentials are stored in a local vault restricted to your user account — never in plain " +
                    "settings files.",
                color = sc.textMuted,
                fontSize = DsType.Caption
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = accent().primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (stored.isEmpty())
                        "No stored credentials"
                    else
                        "Stored credentials: ${stored.joinToString { it.kind.displayName }}",
                    color = sc.textPrimary,
                    fontSize = DsType.Body
                )
            }

            DsButton(
                text = "Sign out of all devices",
                icon = Icons.AutoMirrored.Filled.Logout,
                kind = DsButtonKind.Danger,
                onClick = { confirmAll = true }
            )
        }
    }

    if (confirmAll) {
        DsConfirmDialog(
            title = "Sign out everywhere",
            message = "This signs out every session on every device and removes stored credentials.",
            confirmText = "Sign out all",
            danger = true,
            onConfirm = { engine.signOutOfAllSessions() },
            onDismiss = { confirmAll = false }
        )
    }
}

// ============================================
// PRIVACY
// ============================================

@Composable
fun AccountPrivacySection(state: AppState, engine: AccountEngine) {
    val sc = surfaceColors()
    val settingsData by engine.settingsData.collectAsState()

    DsCard {
        Column(
            modifier = Modifier.padding(DsSpacing.Xl),
            verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)
        ) {
            DsSectionHeader(title = "Privacy", subtitle = "What Kaiteyo collects")
            DsToggle(
                checked = settingsData.allowAnonymousTelemetry,
                onCheckedChange = { engine.updateSettings { s -> s.copy(allowAnonymousTelemetry = it) } },
                label = "Anonymous usage statistics"
            )
            Text(
                text = "Kaiteyo stores all study data locally on this device. Nothing is uploaded unless you " +
                    "connect a cloud provider and enable synchronization.",
                color = sc.textMuted,
                fontSize = DsType.Body
            )
        }
    }
}

// ============================================
// NOTIFICATIONS
// ============================================

@Composable
fun AccountNotificationsSection(state: AppState, engine: AccountEngine) {
    val settingsData by engine.settingsData.collectAsState()

    DsCard {
        Column(
            modifier = Modifier.padding(DsSpacing.Xl),
            verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)
        ) {
            DsSectionHeader(title = "Notifications", subtitle = "What Kaiteyo tells you")
            DsToggle(
                checked = settingsData.notifySyncCompleted,
                onCheckedChange = { engine.updateSettings { s -> s.copy(notifySyncCompleted = it) } },
                label = "Sync completed"
            )
            DsToggle(
                checked = settingsData.notifySyncFailed,
                onCheckedChange = { engine.updateSettings { s -> s.copy(notifySyncFailed = it) } },
                label = "Sync failed"
            )
            DsToggle(
                checked = settingsData.notifyReviewReminders,
                onCheckedChange = { engine.updateSettings { s -> s.copy(notifyReviewReminders = it) } },
                label = "Daily review reminder"
            )
        }
    }
}

// ============================================
// DEVELOPER
// ============================================

@Composable
fun AccountDeveloperSection(state: AppState, engine: AccountEngine) {
    val sc = surfaceColors()
    val settingsData by engine.settingsData.collectAsState()
    val storage by engine.storage.collectAsState()
    var confirmReset by remember { mutableStateOf(false) }

    DsCard {
        Column(
            modifier = Modifier.padding(DsSpacing.Xl),
            verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)
        ) {
            DsSectionHeader(title = "Developer options", subtitle = "Diagnostics and configuration")

            DsTextField(
                value = settingsData.githubClientId,
                onValueChange = { engine.setGitHubClientId(it) },
                label = "GitHub OAuth client ID",
                placeholder = "e.g. Iv1.xxxxxxxxxxxxxxxx"
            )

            DsToggle(
                checked = settingsData.debugLogging,
                onCheckedChange = { engine.updateSettings { s -> s.copy(debugLogging = it) } },
                label = "Debug logging"
            )

            InfoRow("Version", "Kaiteyo ${engine.appVersion}")
            InfoRow("Platform", "${runCatching { System.getProperty("os.name") }.getOrNull() ?: "Unknown"} · " +
                "JVM ${runCatching { System.getProperty("java.version") }.getOrNull() ?: "?"}")
            InfoRow("Database version", "v${engine.databaseVersion}")
            InfoRow("Data directory", dataRoot().absolutePath)
            InfoRow("Cards", state.cards.size.toString())
            InfoRow("Decks", state.library.decks.size.toString())
            InfoRow("Dictionary entries", state.dictionary.installed.sumOf { it.entryCount }.toString())
            InfoRow("Activity entries", state.activityLog.entries.size.toString())
            InfoRow("Storage used", formatBytes(storage.totalBytes))

            DsButton(
                text = "Reset account data",
                icon = Icons.Default.Delete,
                kind = DsButtonKind.Danger,
                onClick = { confirmReset = true }
            )
            Text(
                text = "Resets identity, providers, devices, sessions and stored credentials. Study data is untouched.",
                color = sc.textMuted,
                fontSize = DsType.Caption
            )
        }
    }

    if (confirmReset) {
        DsConfirmDialog(
            title = "Reset account data",
            message = "This clears your profile, connected accounts, devices, sessions and stored credentials. " +
                "Your cards, decks and study history are NOT affected.",
            confirmText = "Reset",
            danger = true,
            onConfirm = {
                engine.resetLocalAccount()
                state.toastHost.show("Account data reset", kind = ToastKind.Info)
            },
            onDismiss = { confirmReset = false }
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    val sc = surfaceColors()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            color = sc.textMuted,
            fontSize = DsType.Body,
            modifier = Modifier.width(160.dp)
        )
        Text(
            text = value,
            color = sc.textPrimary,
            fontSize = DsType.Body,
            fontWeight = FontWeight.Medium
        )
    }
}
