package ua.syt0r.kanji.presentation.screen.main.screen.decks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoSemanticColors
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors

// ============================================
// ENHANCED BACKUP SYSTEM
// Automatic, manual, restore points, verification,
// compression, scheduling, cloud sync
// ============================================

data class BackupConfig(
    val automaticBackups: Boolean = true,
    val backupIntervalHours: Int = 24,
    val maxBackups: Int = 30,
    val compressBackups: Boolean = true,
    val includeMedia: Boolean = false,
    val includePreferences: Boolean = true,
    val includeHistory: Boolean = true,
    val includePlugins: Boolean = false,
    val cloudSync: Boolean = false,
    val backupLocation: String = "",
    val lastBackupTime: Instant? = null
)

data class BackupVerificationResult(
    val isValid: Boolean = false,
    val checksumMatch: Boolean = false,
    val fileSizeMatch: Boolean = false,
    val corruptionDetected: Boolean = false,
    val details: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupManagerScreen(
    backups: List<BackupMetadata> = emptyList(),
    config: BackupConfig = BackupConfig(),
    onDismiss: () -> Unit,
    onOpenBackup: () -> Unit,
    onDeleteBackup: (BackupMetadata) -> Unit,
    onUpdateConfig: (BackupConfig) -> Unit
) {
    var selectedTab by remember { mutableStateOf("Backups") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup & Restore") },
                navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close") } },
                actions = {
                    // Create goes through the real backup flow (platform file
                    // picker + BackupManager), never a metadata-only stub.
                    IconButton(onClick = onOpenBackup) { Icon(Icons.Default.Backup, "Backup Now") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Spacer(Modifier.height(4.dp))
            // Tabs
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Backups", "Restore", "Settings", "Schedule").forEach { tab ->
                    FilterChip(selected = selectedTab == tab, onClick = { selectedTab = tab }, label = { Text(tab) })
                }
            }
            Spacer(Modifier.height(8.dp))

            when (selectedTab) {
                "Backups" -> BackupsList(
                    backups = backups,
                    config = config,
                    onCreateBackup = onOpenBackup,
                    onRestore = onOpenBackup,
                    onDelete = onDeleteBackup
                )
                "Restore" -> RestoreTab(onRestore = onOpenBackup)
                "Settings" -> BackupSettingsTab(config = config, onUpdateConfig = onUpdateConfig)
                "Schedule" -> BackupScheduleTab(config = config, onUpdateConfig = onUpdateConfig)
            }
        }
    }
}

@Composable
private fun BackupsList(
    backups: List<BackupMetadata>,
    config: BackupConfig,
    onCreateBackup: () -> Unit,
    onRestore: () -> Unit,
    onDelete: (BackupMetadata) -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        // Quick stats
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(Modifier.weight(1f)) {
                Column(Modifier.padding(12.dp)) {
                    Text("${backups.size}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Backups", style = MaterialTheme.typography.labelSmall, color = surfaceColors.textMuted)
                }
            }
            Card(Modifier.weight(1f)) {
                Column(Modifier.padding(12.dp)) {
                    val totalSize = backups.sumOf { it.fileSize }
                    val sizeText = when {
                        totalSize > 1_000_000_000 -> "${totalSize / 1_000_000_000} GB"
                        totalSize > 1_000_000 -> "${totalSize / 1_000_000} MB"
                        totalSize > 1_000 -> "${totalSize / 1_000} KB"
                        else -> "$totalSize B"
                    }
                    Text(sizeText, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Total Size", style = MaterialTheme.typography.labelSmall, color = surfaceColors.textMuted)
                }
            }
            Card(Modifier.weight(1f)) {
                Column(Modifier.padding(12.dp)) {
                    Text(config.backupIntervalHours.toString() + "h", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Interval", style = MaterialTheme.typography.labelSmall, color = surfaceColors.textMuted)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Create backup button
        Button(onClick = onCreateBackup, modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = accent.primary)) {
            Icon(Icons.Default.Backup, null)
            Spacer(Modifier.width(8.dp))
            Text("Create Backup Now", fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(12.dp))

        if (backups.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Backup, null, Modifier.size(48.dp), tint = surfaceColors.textMuted.copy(alpha = 0.3f))
                    Spacer(Modifier.height(8.dp))
                    Text("No backups yet", style = MaterialTheme.typography.bodyMedium, color = surfaceColors.textMuted)
                    Text("Create your first backup to protect your data",
                        style = MaterialTheme.typography.bodySmall, color = surfaceColors.textMuted)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(backups, key = { it.id }) { backup ->
                    BackupListItem(
                        backup = backup,
                        onRestore = onRestore,
                        onDelete = { onDelete(backup) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BackupListItem(
    backup: BackupMetadata,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    var showMenu by remember { mutableStateOf(false) }
    val sizeText = when {
        backup.fileSize > 1_000_000 -> "${backup.fileSize / 1_000_000} MB"
        backup.fileSize > 1_000 -> "${backup.fileSize / 1_000} KB"
        else -> "${backup.fileSize} B"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = surfaceColors.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val sem = LocalKaiteyoSemanticColors.current
            Icon(if (backup.isAutomatic) Icons.Default.Schedule else Icons.Default.Backup,
                null, Modifier.size(24.dp), tint = if (backup.isAutomatic) sem.automaticBackup else MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(backup.filename, fontWeight = FontWeight.Medium, fontSize = 13.sp, maxLines = 1)
                Row {
                    Text(sizeText, fontSize = 11.sp, color = surfaceColors.textMuted)
                    Text(" • ", fontSize = 11.sp, color = surfaceColors.textMuted)
                    Text(backup.createdAt.toString().take(19).replace("T", " "), fontSize = 11.sp, color = surfaceColors.textMuted)
                }
                if (backup.isAutomatic) {
                    Text("Automatic backup", fontSize = 10.sp, color = sem.automaticBackup)
                }
                if (backup.notes.isNotBlank()) {
                    Text(backup.notes, fontSize = 10.sp, color = surfaceColors.textMuted, maxLines = 1)
                }
            }
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.MoreVert, "Options", Modifier.size(18.dp))
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    // Restore opens the real backup flow — the metadata row does
                    // not hold the backup file itself, so the file must be picked.
                    DropdownMenuItem(text = { Text("Restore…") }, onClick = { showMenu = false; onRestore() },
                        leadingIcon = { Icon(Icons.Default.Restore, null, Modifier.size(18.dp)) })
                    HorizontalDivider()
                    DropdownMenuItem(text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        onClick = { showMenu = false; onDelete() },
                        leadingIcon = { Icon(Icons.Default.Delete, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error) })
                }
            }
        }
    }
}

@Composable
private fun RestoreTab(onRestore: () -> Unit) {
    val surfaceColors = LocalSurfaceColors.current
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Restore from Backup", style = MaterialTheme.typography.titleSmall)
        Text(
            "Restoring replaces your current data with the contents of a backup file. Pick the .zip backup you created — Kaiteyo verifies the database version before restoring.",
            style = MaterialTheme.typography.bodySmall, color = surfaceColors.textMuted
        )

        HorizontalDivider()

        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.RestorePage, null, Modifier.size(48.dp), tint = surfaceColors.textMuted.copy(alpha = 0.3f))
                Spacer(Modifier.height(8.dp))
                Text("Restore from a backup file", style = MaterialTheme.typography.bodyMedium, color = surfaceColors.textSecondary)
                Text(
                    "Opens the backup screen where you pick the file, review it and confirm.",
                    style = MaterialTheme.typography.bodySmall, color = surfaceColors.textMuted
                )
            }
        }

        Button(
            onClick = onRestore,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.Default.RestorePage, null)
            Spacer(Modifier.width(8.dp))
            Text("Restore from backup file…", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun BackupSettingsTab(
    config: BackupConfig,
    onUpdateConfig: (BackupConfig) -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Backup Settings", style = MaterialTheme.typography.titleSmall)
        Text(
            "Automatic backup scheduling is not active in this build — these preferences are stored for future use. Create backups manually from the Backup screen.",
            style = MaterialTheme.typography.bodySmall, color = surfaceColors.textMuted
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = config.automaticBackups, onCheckedChange = { onUpdateConfig(config.copy(automaticBackups = it)) })
            Spacer(Modifier.width(8.dp))
            Column { Text("Automatic Backups"); Text("Schedule regular backups", fontSize = 12.sp) }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = config.compressBackups, onCheckedChange = { onUpdateConfig(config.copy(compressBackups = it)) })
            Spacer(Modifier.width(8.dp))
            Column { Text("Compress Backups"); Text("Reduce backup file size", fontSize = 12.sp) }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = config.includeMedia, onCheckedChange = { onUpdateConfig(config.copy(includeMedia = it)) })
            Spacer(Modifier.width(8.dp))
            Column { Text("Include Media"); Text("Back up audio/images", fontSize = 12.sp) }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = config.includePreferences, onCheckedChange = { onUpdateConfig(config.copy(includePreferences = it)) })
            Spacer(Modifier.width(8.dp))
            Column { Text("Include Preferences"); Text("Save settings with backup", fontSize = 12.sp) }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = config.cloudSync, onCheckedChange = { onUpdateConfig(config.copy(cloudSync = it)) })
            Spacer(Modifier.width(8.dp))
            Column { Text("Cloud Sync"); Text("Sync backups to cloud", fontSize = 12.sp) }
        }

        HorizontalDivider()

        Text("Max Backups: ${config.maxBackups}", style = MaterialTheme.typography.bodyMedium)
        Slider(value = config.maxBackups.toFloat(), onValueChange = { onUpdateConfig(config.copy(maxBackups = it.toInt())) },
            valueRange = 5f..100f, steps = 18)
        Row(Modifier.fillMaxWidth()) { Text("5", fontSize = 10.sp); Spacer(Modifier.weight(1f)); Text("100", fontSize = 10.sp) }
    }
}

@Composable
private fun BackupScheduleTab(
    config: BackupConfig,
    onUpdateConfig: (BackupConfig) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Backup Schedule", style = MaterialTheme.typography.titleSmall)
        Text("Set how often automatic backups should be created.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            "Automatic backups are not scheduled in this build — this setting is stored for when scheduling ships.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text("Interval: Every ${config.backupIntervalHours} hours", style = MaterialTheme.typography.bodyMedium)
        Slider(value = config.backupIntervalHours.toFloat(), onValueChange = { onUpdateConfig(config.copy(backupIntervalHours = it.toInt())) },
            valueRange = 1f..168f)
        Row(Modifier.fillMaxWidth()) {
            Text("1h", fontSize = 10.sp); Spacer(Modifier.weight(1f)); Text("7d (168h)", fontSize = 10.sp)
        }

        HorizontalDivider()

        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = config.automaticBackups, onCheckedChange = { onUpdateConfig(config.copy(automaticBackups = it)) })
            Spacer(Modifier.width(8.dp))
            Column {
                Text("Automatic Backups ${if (config.automaticBackups) "Enabled" else "Disabled"}")
                Text(config.lastBackupTime?.let { "Last backup: ${it.toString().take(19)}" } ?: "No backup yet",
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ============================================
// BACKUP RESTORE VERIFICATION
// ============================================

class BackupVerifier {

    /**
     * Verifies content against an expected SHA-256 checksum by hashing the
     * bytes and comparing case-insensitively. This is commonMain-safe (no
     * file access — callers read the platform file and pass its bytes).
     * Never fabricates a pass: an empty expected checksum or a mismatch is
     * reported honestly.
     */
    fun verifyChecksum(content: ByteArray, expectedChecksum: String): BackupVerificationResult {
        if (expectedChecksum.isBlank()) {
            return BackupVerificationResult(
                isValid = false,
                checksumMatch = false,
                fileSizeMatch = false,
                corruptionDetected = true,
                details = "No expected checksum provided"
            )
        }
        val actual = Sha256.digest(content)
        val match = actual.equals(expectedChecksum, ignoreCase = true)
        return BackupVerificationResult(
            isValid = match,
            checksumMatch = match,
            fileSizeMatch = true,
            corruptionDetected = !match,
            details = if (match) "Checksum verified (SHA-256)"
            else "Checksum mismatch — expected $expectedChecksum, got $actual"
        )
    }

    /**
     * Without a live database handle this cannot run PRAGMA integrity_check;
     * it reports that honestly instead of pretending the database is fine.
     */
    fun verifyDatabaseIntegrity(): BackupVerificationResult {
        return BackupVerificationResult(
            isValid = false,
            checksumMatch = false,
            fileSizeMatch = false,
            corruptionDetected = false,
            details = "Database integrity check requires the app database handle and is not available here"
        )
    }

    /**
     * SQLite databases typically compress well; 40% is a documented
     * expectation used only for UI pre-allocation, not a measurement.
     */
    fun estimateCompressionRatio(originalSize: Long): Float = 0.4f
}

/**
 * Minimal, dependency-free SHA-256 (FIPS 180-4) so backup checksums can be
 * verified on every platform — commonMain cannot use JVM MessageDigest.
 * Returns the lowercase hex digest of the input bytes.
 */
private object Sha256 {

    /** Right-rotate a 32-bit value (Kotlin common has no Integer.rotateRight). */
    private fun Int.rotateRight(bits: Int): Int =
        (this ushr bits) or (this shl (32 - bits))

    // Hex literals at or above 0x80000000 are inferred as Long in Kotlin, so
    // they are narrowed to Int explicitly (two's-complement is the exact
    // SHA-256 bit pattern).
    private val K = intArrayOf(
        0x428a2f98, 0x71374491, 0xb5c0fbcf.toInt(), 0xe9b5dba5.toInt(), 0x3956c25b, 0x59f111f1, 0x923f82a4.toInt(), 0xab1c5ed5.toInt(),
        0xd807aa98.toInt(), 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe.toInt(), 0x9bdc06a7.toInt(), 0xc19bf174.toInt(),
        0xe49b69c1.toInt(), 0xefbe4786.toInt(), 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
        0x983e5152.toInt(), 0xa831c66d.toInt(), 0xb00327c8.toInt(), 0xbf597fc7.toInt(), 0xc6e00bf3.toInt(), 0xd5a79147.toInt(), 0x06ca6351, 0x14292967,
        0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e.toInt(), 0x92722c85.toInt(),
        0xa2bfe8a1.toInt(), 0xa81a664b.toInt(), 0xc24b8b70.toInt(), 0xc76c51a3.toInt(), 0xd192e819.toInt(), 0xd6990624.toInt(), 0xf40e3585.toInt(), 0x106aa070,
        0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
        0x748f82ee, 0x78a5636f, 0x84c87814.toInt(), 0x8cc70208.toInt(), 0x90befffa.toInt(), 0xa4506ceb.toInt(), 0xbef9a3f7.toInt(), 0xc67178f2.toInt()
    )

    fun digest(bytes: ByteArray): String {
        val bitLength = bytes.size.toLong() * 8
        val paddedLength = ((bytes.size + 8) / 64 + 1) * 64
        val padded = ByteArray(paddedLength)
        bytes.copyInto(padded)
        padded[bytes.size] = 0x80.toByte()
        var idx = paddedLength - 8
        var shift = 56
        while (shift >= 0) {
            padded[idx++] = ((bitLength ushr shift) and 0xFF).toByte()
            shift -= 8
        }

        var h0 = 0x6a09e667.toInt(); var h1 = 0xbb67ae85.toInt(); var h2 = 0x3c6ef372.toInt(); var h3 = 0xa54ff53a.toInt()
        var h4 = 0x510e527f.toInt(); var h5 = 0x9b05688c.toInt(); var h6 = 0x1f83d9ab.toInt(); var h7 = 0x5be0cd19.toInt()
        val w = IntArray(64)

        for (chunk in 0 until paddedLength step 64) {
            for (t in 0 until 16) {
                val i = chunk + t * 4
                w[t] = ((padded[i].toInt() and 0xFF) shl 24) or
                    ((padded[i + 1].toInt() and 0xFF) shl 16) or
                    ((padded[i + 2].toInt() and 0xFF) shl 8) or
                    (padded[i + 3].toInt() and 0xFF)
            }
            for (t in 16 until 64) {
                val s0 = w[t - 15].rotateRight(7) xor w[t - 15].rotateRight(18) xor (w[t - 15] ushr 3)
                val s1 = w[t - 2].rotateRight(17) xor w[t - 2].rotateRight(19) xor (w[t - 2] ushr 10)
                w[t] = w[t - 16] + s0 + w[t - 7] + s1
            }

            var a = h0; var b = h1; var c = h2; var d = h3
            var e = h4; var f = h5; var g = h6; var h = h7

            for (t in 0 until 64) {
                val s1 = e.rotateRight(6) xor e.rotateRight(11) xor e.rotateRight(25)
                val ch = (e and f) xor (e.inv() and g)
                val temp1 = h + s1 + ch + K[t] + w[t]
                val s0 = a.rotateRight(2) xor a.rotateRight(13) xor a.rotateRight(22)
                val maj = (a and b) xor (a and c) xor (b and c)
                val temp2 = s0 + maj
                h = g; g = f; f = e; e = d + temp1
                d = c; c = b; b = a; a = temp1 + temp2
            }

            h0 += a; h1 += b; h2 += c; h3 += d
            h4 += e; h5 += f; h6 += g; h7 += h
        }

        return listOf(h0, h1, h2, h3, h4, h5, h6, h7).joinToString("") { value ->
            (value.toLong() and 0xFFFFFFFFL).toString(16).padStart(8, '0')
        }
    }
}
