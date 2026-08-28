package ua.syt0r.kanji.presentation.screen.main.screen.media

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.dsl.module
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.ui.PageIdentity
import ua.syt0r.kanji.presentation.common.ui.ProvidePageIdentity
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState

// ============================================================
// MEDIA CENTRE — CORE HOST (KOIN CONTRACT)
// Multiplatform contract: the shipped app resolves a
// MediaCentreContent via Koin. On desktop the binding
// is overridden by DesktopMediaCentreContent which mounts
// the real MediaView (MediaEngine, subtitles, mining).
// On other platforms — and as a safe fallback when the
// desktop workspace fails to construct — this default
// shows an honest empty-state: no fake tracks, no
// fake sliders, no ghost buttons.
// ============================================================

/** Multiplatform Media Centre content contract. */
fun interface MediaCentreContent {
    @Composable
    fun Content(navigationState: MainNavigationState?, onClose: () -> Unit)
}

/** Honest core fallback — no fake media, no prototype player. */
object DefaultMediaCentreContent : MediaCentreContent {

    @Composable
    override fun Content(navigationState: MainNavigationState?, onClose: () -> Unit) {
        val sc = LocalSurfaceColors.current
        ProvidePageIdentity(
            PageIdentity(id = "media", name = "Media Centre", route = "/media", panel = "default")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(sc.background)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = sc.textPrimary)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Media Centre",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = sc.textPrimary
                        )
                        Text(
                            text = "Desktop immersion workspace",
                            style = MaterialTheme.typography.bodySmall,
                            color = sc.textMuted
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = sc.surface,
                            modifier = Modifier.size(88.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    Icons.Default.VideoLibrary,
                                    contentDescription = null,
                                    tint = sc.textMuted,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                        }
                        Text(
                            text = "Media Centre is a desktop feature",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                            color = sc.textPrimary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Open Kaiteyo on Windows, macOS, or Linux to use the full media workspace — " +
                                "local video/audio, subtitles (SRT/ASS/VTT), transcript, dictionary lookup, " +
                                "sentence mining to Kaiteyo or Anki, and playback history. " +
                                "This screen has no fake tracks or demo playback.",
                            fontSize = 13.sp,
                            color = sc.textMuted,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Icon(Icons.Default.DesktopWindows, null, tint = sc.textMuted, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Run:  ./gradlew :desktopApp:run",
                                fontSize = 12.sp,
                                color = sc.textMuted
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "On desktop this destination is overridden by DesktopMediaCentreContent, " +
                                "which mounts MediaView (VLC/mpv, subtitles, mining, Anki).",
                            fontSize = 11.sp,
                            color = sc.textMuted.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// ── Koin module ──
val mediaCentreModule = module {
    single<MediaCentreContent> { DefaultMediaCentreContent }
}
