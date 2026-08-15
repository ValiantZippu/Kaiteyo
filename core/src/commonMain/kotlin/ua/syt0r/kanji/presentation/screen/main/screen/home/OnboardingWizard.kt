package ua.syt0r.kanji.presentation.screen.main.screen.home

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import ua.syt0r.kanji.core.app_data.AppDataRepository
import ua.syt0r.kanji.core.srs.DailyLimitConfiguration
import ua.syt0r.kanji.core.srs.DailyLimitManager
import ua.syt0r.kanji.core.srs.PracticeLimit
import ua.syt0r.kanji.core.user_data.database.LetterPracticeRepository
import ua.syt0r.kanji.core.user_data.database.VocabCardData
import ua.syt0r.kanji.core.user_data.database.VocabPracticeRepository
import ua.syt0r.kanji.core.user_data.preferences.PreferencesContract

// ============================================
// FIRST-RUN ONBOARDING
// New users pick a JLPT target and daily limits.
// "Set up" creates the real JLPT kanji + vocab
// decks from the bundled dictionary database and
// applies the daily limits. Nothing is fake:
// the decks contain actual dictionary entries and
// appear immediately in the Library.
// ============================================

@Composable
fun OnboardingWizard(
    onComplete: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val appPreferences = koinInject<PreferencesContract.AppPreferences>()
    val appDataRepository = koinInject<AppDataRepository>()
    val letterPracticeRepository = koinInject<LetterPracticeRepository>()
    val vocabPracticeRepository = koinInject<VocabPracticeRepository>()
    val dailyLimitManager = koinInject<DailyLimitManager>()

    var targetLevel by remember { mutableIntStateOf(5) }
    var newPerDay by remember { mutableStateOf(10) }
    var reviewsPerDay by remember { mutableStateOf(60) }
    var isSettingUp by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "書いてよ",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Welcome to Kaiteyo",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Set your study target and daily limits — the JLPT decks are built from the bundled dictionary.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "JLPT target (decks created for N5 → N$targetLevel)",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (5 downTo 1).forEach { level ->
                FilterChip(
                    selected = targetLevel == level,
                    onClick = { targetLevel = level },
                    label = { Text("N$level") }
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "New cards / day",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                OutlinedTextField(
                    value = newPerDay.toString(),
                    onValueChange = { newPerDay = it.toIntOrNull() ?: 0 },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Reviews / day",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                OutlinedTextField(
                    value = reviewsPerDay.toString(),
                    onValueChange = { reviewsPerDay = it.toIntOrNull() ?: 0 },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        errorMessage?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Button(
            onClick = {
                isSettingUp = true
                errorMessage = null
                scope.launch {
                    runCatching {
                        // Create JLPT decks from real dictionary data, N5 up to target.
                        for (level in 5 downTo targetLevel) {
                            val kanji = appDataRepository.getKanjiForClassification("n$level")
                            if (kanji.isNotEmpty()) {
                                letterPracticeRepository.createDeck(
                                    title = "JLPT N$level · Kanji",
                                    characters = kanji
                                )
                            }

                            val words = appDataRepository.getImportDeckWords("n$level")
                            if (words.isNotEmpty()) {
                                vocabPracticeRepository.createDeck(
                                    title = "JLPT N$level · Vocabulary",
                                    words = words.map {
                                        VocabCardData(
                                            kanjiReading = it.kanji,
                                            kanaReading = it.kana,
                                            meaning = it.meaning,
                                            dictionaryId = it.id
                                        )
                                    }
                                )
                            }
                        }

                        dailyLimitManager.update(
                            isEnabled = true,
                            configuration = DailyLimitConfiguration(
                                letterCombinedLimit = PracticeLimit(
                                    new = newPerDay,
                                    due = reviewsPerDay
                                ),
                                vocabCombinedLimit = PracticeLimit(
                                    new = newPerDay,
                                    due = reviewsPerDay
                                )
                            )
                        )

                        appPreferences.onboardingCompleted.set(true)
                    }.onFailure {
                        errorMessage = "Setup failed: ${it.message ?: "unknown error"}"
                    }.onSuccess {
                        onComplete()
                    }
                    isSettingUp = false
                }
            },
            enabled = !isSettingUp && newPerDay > 0 && reviewsPerDay > 0
        ) {
            if (isSettingUp) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(if (isSettingUp) "Building your decks…" else "Set up & start learning")
        }

        TextButton(
            onClick = {
                scope.launch {
                    appPreferences.onboardingCompleted.set(true)
                    onComplete()
                }
            },
            enabled = !isSettingUp
        ) {
            Text("Skip — I'll set everything up myself")
        }
    }
}
