package ua.syt0r.kanji.desktop.ui.media

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.designsystem.DsButton
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import ua.syt0r.kanji.desktop.engine.media.DownloadState

@Composable
fun MediaDownloadsPanel(state: AppState) {
    val sc = surfaceColors()
    val svc = state.mediaDownloads
    Column(Modifier.fillMaxSize().padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
            Text("Downloads", color = sc.textPrimary, fontSize = DsType.Title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            DsButton(text = "Clear completed", onClick = { svc.clearCompleted() }, compact = true)
        }
        Text(
            "Only http(s) media is supported. Filenames are sanitized, DRM is respected, and every download shows honest state: queued → downloading → paused → completed / failed / cancelled with retry.",
            color = sc.textMuted, fontSize = 12.sp
        )
        if (svc.jobs.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                    Icon(Icons.Default.Download, null, tint = sc.textMuted, modifier = Modifier.size(48.dp))
                    Text("No downloads yet", color = sc.textPrimary, fontWeight = FontWeight.Medium)
                    Text("Paste a direct media URL from the Browser or Library. Use “Open URL…” in the toolbar for streams.", color = sc.textMuted, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                items(svc.jobs, key = { it.id }) { job ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(sc.surface).padding(DsSpacing.Md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)
                    ) {
                        when (job.state) {
                            DownloadState.Completed -> Icon(Icons.Default.CheckCircle, null, tint = sc.textPrimary, modifier = Modifier.size(20.dp))
                            DownloadState.Failed -> Icon(Icons.Default.Error, null, tint = sc.textPrimary, modifier = Modifier.size(20.dp))
                            DownloadState.Downloading -> CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            else -> Icon(Icons.Default.Download, null, tint = sc.textMuted, modifier = Modifier.size(20.dp))
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(job.fileName, color = sc.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            Text(job.url, color = sc.textMuted, fontSize = 11.sp, maxLines = 1)
                            if (job.state == DownloadState.Downloading && job.sizeBytes > 0) {
                                LinearProgressIndicator(progress = { job.progress }, modifier = Modifier.fillMaxWidth().height(4.dp))
                                Text("${job.downloadedBytes / 1024} / ${job.sizeBytes / 1024} KB · ${(job.progress * 100).toInt()}%", color = sc.textMuted, fontSize = 11.sp)
                            } else {
                                Text("${job.state.label}${if (job.error.isNotBlank()) " — ${job.error.take(80)}" else ""}", color = sc.textMuted, fontSize = 11.sp)
                            }
                        }
                        when (job.state) {
                            DownloadState.Downloading -> IconButton(onClick = { svc.pause(job.id) }) { Icon(Icons.Default.Pause, "Pause") }
                            DownloadState.Failed, DownloadState.Paused, DownloadState.Cancelled -> IconButton(onClick = { svc.retry(job.id) }) { Icon(Icons.Default.Refresh, "Retry") }
                            DownloadState.Queued -> IconButton(onClick = { svc.cancel(job.id) }) { Icon(Icons.Default.Cancel, "Cancel") }
                            else -> {}
                        }
                    }
                }
            }
        }
    }
}
