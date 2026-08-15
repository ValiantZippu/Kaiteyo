package ua.syt0r.kanji.presentation.screen.main.screen.media

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.koin.dsl.module
import ua.syt0r.kanji.presentation.common.resources.string.resolveString

// ============================================
// MEDIA CENTRE — CORE DESTINATION
// The Media Centre is the desktop suite's
// immersion workspace (player, subtitles,
// dictionary, mining). The desktop app supplies
// the real implementation through this contract;
// every other platform gets an honest screen
// pointing at the desktop app. This keeps Media
// a first-class destination in navigation (and
// the floating launcher / command palette)
// without dead links.
// ============================================

/** Renders the platform's Media Centre content. Implemented by the desktop app. */
fun interface MediaCentreContent {
    @Composable
    fun Content(onClose: () -> Unit)
}

/** Off-desktop fallback: an honest "desktop only" screen with a back button. */
object MediaCentrePlaceholderContent : MediaCentreContent {

    @Composable
    override fun Content(onClose: () -> Unit) {
        val strings = resolveString { mediaCentre }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onClose) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.width(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(strings.backButton)
                }
                Text(
                    text = strings.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
            Spacer(Modifier.height(32.dp))
            Icon(
                Icons.Default.Movie,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(48.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = strings.desktopOnlyTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = strings.desktopOnlyMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(0.72f)
            )
            Spacer(Modifier.height(24.dp))
            Card(modifier = Modifier.fillMaxWidth(0.72f)) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = strings.featuresTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    listOf(
                        strings.featurePlayer,
                        strings.featureLibrary,
                        strings.featureDictionary,
                        strings.featureMining
                    ).forEach { feature ->
                        Text(
                            text = "•  $feature",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

val mediaCentreModule = module {
    single<MediaCentreContent> { MediaCentrePlaceholderContent }
}
