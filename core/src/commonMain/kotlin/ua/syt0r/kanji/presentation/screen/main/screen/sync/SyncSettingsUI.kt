package ua.syt0r.kanji.presentation.screen.main.screen.sync

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.core.account.SyncFrequency
import ua.syt0r.kanji.core.account.BackupFrequency
import ua.syt0r.kanji.core.account.ConflictResolutionStrategy

// ============================================
// KAITEYO SYNC SETTINGS UI v1.2
// GitHub Login, Connected Account, Devices,
// Sync Now, Auto Sync, Frequency, Network,
// Conflict Settings, Backup, Export, Sign Out
// ============================================

@Composable
fun SyncSettingsScreen() {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    var selectedTab by remember { mutableStateOf("Sync") }
    var autoSync by remember { mutableStateOf(true) }
    var wifiOnly by remember { mutableStateOf(true) }
    var meteredNetwork by remember { mutableStateOf(false) }
    var encryptLocal by remember { mutableStateOf(true) }
    var autoBackup by remember { mutableStateOf(true) }
    var syncFrequency by remember { mutableStateOf(SyncFrequency.Every15Minutes) }
    var backupFrequency by remember { mutableStateOf(BackupFrequency.Daily) }
    var conflictStrategy by remember { mutableStateOf(ConflictResolutionStrategy.AskEachTime) }
    var isSignedIn by remember { mutableStateOf(false) }
    var showDeviceCode by remember { mutableStateOf(false) }
    var deviceCode by remember { mutableStateOf("ABCD-1234") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text("Synchronization", style = MaterialTheme.typography.titleLarge,
            color = surfaceColors.textPrimary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Keep your data in sync across devices",
            color = surfaceColors.textMuted, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(16.dp))

        // Tab bar
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Sync", "Account", "Devices", "Backup", "Advanced").forEach { tab ->
                val isSelected = selectedTab == tab
                val tabBg by animateColorAsState(
                    targetValue = if (isSelected) accent.primary.copy(alpha = 0.15f) else Color.Transparent,
                    animationSpec = tween(200), label = "tabBg")
                val tabText by animateColorAsState(
                    targetValue = if (isSelected) accent.primary else surfaceColors.textSecondary,
                    animationSpec = tween(200), label = "tabText")
                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(tabBg)
                    .clickable { selectedTab = tab }.padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center) {
                    Text(tab, color = tabText, fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))

        when (selectedTab) {
            "Sync" -> SyncTab(accent, surfaceColors, autoSync, wifiOnly, meteredNetwork,
                syncFrequency, conflictStrategy, { autoSync = it }, { wifiOnly = it },
                { meteredNetwork = it }, { syncFrequency = it }, { conflictStrategy = it })
            "Account" -> AccountTab(accent, surfaceColors, isSignedIn, showDeviceCode, deviceCode,
                { isSignedIn = it }, { showDeviceCode = it })
            "Devices" -> DevicesTab(accent, surfaceColors)
            "Backup" -> BackupTab(accent, surfaceColors, autoBackup, backupFrequency, encryptLocal,
                { autoBackup = it }, { backupFrequency = it }, { encryptLocal = it })
            "Advanced" -> AdvancedTab(accent, surfaceColors)
        }
    }
}

@Composable
private fun SyncTab(
    accent: ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme,
    surfaceColors: ua.syt0r.kanji.presentation.common.theme.SurfaceColors,
    autoSync: Boolean, wifiOnly: Boolean, meteredNetwork: Boolean,
    syncFrequency: SyncFrequency, conflictStrategy: ConflictResolutionStrategy,
    onAutoSyncChange: (Boolean) -> Unit, onWifiOnlyChange: (Boolean) -> Unit,
    onMeteredNetworkChange: (Boolean) -> Unit,
    onSyncFrequencyChange: (SyncFrequency) -> Unit,
    onConflictStrategyChange: (ConflictResolutionStrategy) -> Unit
) {
    Button(onClick = { },
        colors = ButtonDefaults.buttonColors(containerColor = accent.primary, contentColor = accent.onPrimary),
        modifier = Modifier.fillMaxWidth().height(52.dp)) {
        Text("Sync Now", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
    Spacer(modifier = Modifier.height(20.dp))

    ToggleRow("Auto Sync", "Automatically synchronize data", autoSync, accent, surfaceColors, onAutoSyncChange)
    Spacer(modifier = Modifier.height(4.dp))

    if (autoSync) {
        Text("Sync Frequency", color = surfaceColors.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            SyncFrequency.entries.forEach { freq ->
                val isSelected = syncFrequency == freq
                val freqBg by animateColorAsState(
                    targetValue = if (isSelected) accent.primary.copy(alpha = 0.12f) else surfaceColors.surface,
                    animationSpec = tween(200), label = "freqBg")
                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(6.dp)).background(freqBg)
                    .clickable { onSyncFrequencyChange(freq) }.padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center) {
                    Text(freq.displayName, color = if (isSelected) accent.primary else surfaceColors.textSecondary,
                        fontSize = 9.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        textAlign = TextAlign.Center)
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }

    Text("Network", color = surfaceColors.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    Spacer(modifier = Modifier.height(6.dp))
    ToggleRow("Wi-Fi Only", "Only sync on Wi-Fi connections", wifiOnly, accent, surfaceColors, onWifiOnlyChange)
    ToggleRow("Metered Network", "Allow sync on metered connections", meteredNetwork, accent, surfaceColors, onMeteredNetworkChange)
    Spacer(modifier = Modifier.height(16.dp))

    Text("Conflict Resolution", color = surfaceColors.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    Spacer(modifier = Modifier.height(6.dp))
    ConflictResolutionStrategy.entries.forEach { strategy ->
        val isSelected = conflictStrategy == strategy
        val rowBg by animateColorAsState(
            targetValue = if (isSelected) accent.primary.copy(alpha = 0.08f) else Color.Transparent,
            animationSpec = tween(200), label = "strategyBg")
        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(rowBg)
            .clickable { onConflictStrategyChange(strategy) }.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(16.dp).clip(CircleShape)
                .background(if (isSelected) accent.primary else surfaceColors.border), contentAlignment = Alignment.Center) {
                if (isSelected) Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.White))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(strategy.displayName, color = surfaceColors.textPrimary, fontSize = 13.sp)
        }
    }
}

@Composable
private fun AccountTab(
    accent: ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme,
    surfaceColors: ua.syt0r.kanji.presentation.common.theme.SurfaceColors,
    isSignedIn: Boolean, showDeviceCode: Boolean, deviceCode: String,
    onSignInChange: (Boolean) -> Unit, onShowDeviceCode: (Boolean) -> Unit
) {
    if (!isSignedIn) {
        Text("Connect your account", style = MaterialTheme.typography.titleMedium,
            color = surfaceColors.textPrimary, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Sign in with GitHub to enable synchronization",
            color = surfaceColors.textMuted, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF24292E)).clickable { onShowDeviceCode(true) }.padding(16.dp),
            contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("⬛", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Sign in with GitHub", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        if (showDeviceCode) {
            Spacer(modifier = Modifier.height(20.dp))
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(surfaceColors.surfaceElevated).padding(20.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Device Activation", color = surfaceColors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Enter this code on GitHub:", color = surfaceColors.textMuted, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(accent.primary.copy(alpha = 0.1f))
                        .padding(horizontal = 24.dp, vertical = 12.dp)) {
                        Text(deviceCode, color = accent.primary, fontSize = 24.sp, fontWeight = FontWeight.Bold,
                            letterSpacing = 4.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("github.com/login/device", color = surfaceColors.textSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Waiting for authentication...", color = surfaceColors.textMuted, fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Other providers coming soon:", color = surfaceColors.textMuted, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(8.dp))
        listOf("WebDAV", "Self-Hosted", "Dropbox", "Google Drive", "OneDrive").forEach { provider ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(surfaceColors.border))
                Spacer(modifier = Modifier.width(8.dp))
                Text(provider, color = surfaceColors.textMuted, fontSize = 12.sp)
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(surfaceColors.surfaceElevated).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(accent.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center) {
                    Text("U", color = accent.primary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("User Name", color = surfaceColors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text("username", color = surfaceColors.textMuted, fontSize = 12.sp)
                    Text("Connected via GitHub", color = accent.primary.copy(alpha = 0.7f), fontSize = 11.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { onSignInChange(false) },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B6B).copy(alpha = 0.15f),
                contentColor = Color(0xFFFF6B6B)), modifier = Modifier.fillMaxWidth()) {
            Text("Sign Out", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun DevicesTab(
    accent: ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme,
    surfaceColors: ua.syt0r.kanji.presentation.common.theme.SurfaceColors
) {
    Text("Connected Devices", style = MaterialTheme.typography.titleMedium,
        color = surfaceColors.textPrimary, fontWeight = FontWeight.SemiBold)
    Spacer(modifier = Modifier.height(4.dp))
    Text("Manage devices connected to your account",
        color = surfaceColors.textMuted, fontSize = 13.sp)
    Spacer(modifier = Modifier.height(16.dp))

    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
        .background(surfaceColors.surface).border(1.dp, accent.primary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
        .padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                .background(accent.primary.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                Text("💻", fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("This Device", color = surfaceColors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(accent.primary.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text("Current", color = accent.primary, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Text("Desktop · v1.2.0 · Online now", color = surfaceColors.textMuted, fontSize = 11.sp)
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))

    listOf(
        "Laptop" to "💻" to "Last sync: 2 hours ago",
        "Phone" to "📱" to "Last sync: Yesterday"
    ).forEach { (device, lastSync) ->
        val (name, icon) = device
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(surfaceColors.surface).padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                    .background(surfaceColors.surfaceElevated), contentAlignment = Alignment.Center) {
                    Text(icon, fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(name, color = surfaceColors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text(lastSync, color = surfaceColors.textMuted, fontSize = 11.sp)
                }
                Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(surfaceColors.surfaceElevated)
                    .clickable { }.padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text("···", color = surfaceColors.textMuted, fontSize = 12.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
    }
}

@Composable
private fun BackupTab(
    accent: ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme,
    surfaceColors: ua.syt0r.kanji.presentation.common.theme.SurfaceColors,
    autoBackup: Boolean, backupFrequency: BackupFrequency, encryptLocal: Boolean,
    onAutoBackupChange: (Boolean) -> Unit, onBackupFrequencyChange: (BackupFrequency) -> Unit,
    onEncryptLocalChange: (Boolean) -> Unit
) {
    Text("Backup", style = MaterialTheme.typography.titleMedium,
        color = surfaceColors.textPrimary, fontWeight = FontWeight.SemiBold)
    Spacer(modifier = Modifier.height(4.dp))
    Text("Protect your data with automatic and manual backups",
        color = surfaceColors.textMuted, fontSize = 13.sp)
    Spacer(modifier = Modifier.height(16.dp))

    Button(onClick = { },
        colors = ButtonDefaults.buttonColors(containerColor = accent.primary, contentColor = accent.onPrimary),
        modifier = Modifier.fillMaxWidth().height(48.dp)) {
        Text("Create Backup Now", fontWeight = FontWeight.SemiBold)
    }
    Spacer(modifier = Modifier.height(16.dp))

    ToggleRow("Auto Backup", "Automatically create backups", autoBackup, accent, surfaceColors, onAutoBackupChange)
    Spacer(modifier = Modifier.height(4.dp))

    if (autoBackup) {
        Text("Backup Frequency", color = surfaceColors.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            BackupFrequency.entries.forEach { freq ->
                val isSelected = backupFrequency == freq
                val freqBg by animateColorAsState(
                    targetValue = if (isSelected) accent.primary.copy(alpha = 0.12f) else surfaceColors.surface,
                    animationSpec = tween(200), label = "backupFreqBg")
                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(6.dp)).background(freqBg)
                    .clickable { onBackupFrequencyChange(freq) }.padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center) {
                    Text(freq.displayName, color = if (isSelected) accent.primary else surfaceColors.textSecondary,
                        fontSize = 9.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        textAlign = TextAlign.Center)
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }

    ToggleRow("Encrypt Local Data", "Encrypt sensitive data on this device", encryptLocal, accent, surfaceColors, onEncryptLocalChange)
    Spacer(modifier = Modifier.height(16.dp))

    Text("Backup History", color = surfaceColors.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    Spacer(modifier = Modifier.height(8.dp))
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(surfaceColors.surface).padding(16.dp),
        contentAlignment = Alignment.Center) {
        Text("No backups yet", color = surfaceColors.textMuted, fontSize = 12.sp)
    }
}

@Composable
private fun AdvancedTab(
    accent: ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme,
    surfaceColors: ua.syt0r.kanji.presentation.common.theme.SurfaceColors
) {
    Text("Advanced", style = MaterialTheme.typography.titleMedium,
        color = surfaceColors.textPrimary, fontWeight = FontWeight.SemiBold)
    Spacer(modifier = Modifier.height(4.dp))
    Text("Advanced synchronization options",
        color = surfaceColors.textMuted, fontSize = 13.sp)
    Spacer(modifier = Modifier.height(16.dp))

    listOf(
        "Export All Data" to "Export your complete profile and settings",
        "Import Data" to "Import data from a previous export",
        "Delete Local Cache" to "Clear temporary synchronization data",
        "Reset Synchronization" to "Reset all sync data and start fresh"
    ).forEach { (title, desc) ->
        val isDestructive = title.contains("Delete") || title.contains("Reset")
        val textColor = if (isDestructive) Color(0xFFFF6B6B) else surfaceColors.textPrimary
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(surfaceColors.surface).clickable { }.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text(desc, color = surfaceColors.textMuted, fontSize = 11.sp)
                }
                Text("→", color = surfaceColors.textMuted, fontSize = 16.sp)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
    }
}

@Composable
private fun ToggleRow(
    title: String, description: String, isEnabled: Boolean,
    accent: ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme,
    surfaceColors: ua.syt0r.kanji.presentation.common.theme.SurfaceColors,
    onToggle: (Boolean) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
        .clickable { onToggle(!isEnabled) }.padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = surfaceColors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(description, color = surfaceColors.textMuted, fontSize = 11.sp)
        }
        Box(modifier = Modifier.size(40.dp, 24.dp).clip(RoundedCornerShape(12.dp))
            .background(if (isEnabled) accent.primary else surfaceColors.border)) {
            val alignment = if (isEnabled) Alignment.CenterEnd else Alignment.CenterStart
            Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(Color.White)
                .align(alignment).padding(2.dp))
        }
    }
}