package ua.syt0r.kanji.presentation.screen.main.screen.credits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ua.syt0r.kanji.presentation.common.resources.string.resolveString
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditsScreen(
    state: MainNavigationState
) {

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(resolveString { credits.title }) },
                navigationIcon = {
                    IconButton(onClick = { state.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Core Development ──
            CreditsSection(
                title = resolveString { credits.coreDevelopmentTitle },
                items = listOf(
                    "syt0r" to "Core architect, lead developer"
                )
            )

            // ── Contributors ──
            CreditsSection(
                title = resolveString { credits.contributorsTitle },
                items = listOf(
                    "Community" to "Bug reports, feature requests, and feedback"
                )
            )

            // ── Design ──
            CreditsSection(
                title = resolveString { credits.designTitle },
                items = listOf(
                    "syt0r" to "UI/UX design and visual identity"
                )
            )

            // ── Translations ──
            CreditsSection(
                title = resolveString { credits.translationsTitle },
                items = listOf(
                    "English" to "English translations",
                    "日本語" to "Japanese translations"
                )
            )

            // ── Open Source Libraries ──
            CreditsSection(
                title = resolveString { credits.openSourceLibrariesTitle },
                items = listOf(
                    "Compose Multiplatform" to "UI framework",
                    "Koin" to "Dependency injection",
                    "SQLDelight" to "Database layer",
                    "Kotlin" to "Programming language",
                    "Coil" to "Image loading",
                    "kotlinx.serialization" to "JSON serialization"
                )
            )

            // ── Special Thanks ──
            CreditsSection(
                title = resolveString { credits.specialThanksTitle },
                items = listOf(
                    "Open-source community" to "For inspiration and tooling"
                )
            )

            Spacer(Modifier.height(16.dp))

        }

    }

}

@Composable
private fun CreditsSection(
    title: String,
    items: List<Pair<String, String>>
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        items.forEach { (name, description) ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(14.dp)
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}