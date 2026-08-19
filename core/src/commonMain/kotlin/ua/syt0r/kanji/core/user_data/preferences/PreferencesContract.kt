package ua.syt0r.kanji.core.user_data.preferences

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable
import ua.syt0r.kanji.core.suspended_property.SuspendedProperty


interface PreferencesContract {

    interface AppPreferences {

        val refreshToken: SuspendedProperty<String?>
        val idToken: SuspendedProperty<String?>
        val userInfo: SuspendedProperty<PreferencesUserInfo?>
        val subscriptionAlert: SuspendedProperty<String?>

        val localDataId: SuspendedProperty<String>
        val localDataTimestamp: SuspendedProperty<Instant?>
        val lastSyncedDataInfo: SuspendedProperty<PreferencesSyncDataInfo?>

        val analyticsEnabled: SuspendedProperty<Boolean>

        val practiceType: SuspendedProperty<PreferencesLetterPracticeType>
        val filterNew: SuspendedProperty<Boolean>
        val filterDue: SuspendedProperty<Boolean>
        val filterDone: SuspendedProperty<Boolean>
        val sortOption: SuspendedProperty<PreferencesLetterSortOption>

        val isSortDescending: SuspendedProperty<Boolean>

        val practicePreviewLayout: SuspendedProperty<PreferencesDeckDetailsLetterLayout>

        val kanaGroupsEnabled: SuspendedProperty<Boolean>

        val theme: SuspendedProperty<PreferencesTheme>

        val dailyLimitEnabled: SuspendedProperty<Boolean>
        val dailyLimitConfigurationJson: SuspendedProperty<String>

        val reminderEnabled: SuspendedProperty<Boolean>
        val reminderTime: SuspendedProperty<LocalTime>

        val defaultHomeTab: SuspendedProperty<PreferencesDefaultHomeTab>

        val lastAppVersionWhenChangesDialogShown: SuspendedProperty<String>
        val tutorialSeen: SuspendedProperty<Boolean>
        val onboardingCompleted: SuspendedProperty<Boolean>
        val generalDashboardStudyTargets: SuspendedProperty<Map<String, Boolean>>

        val letterDashboardPracticeType: SuspendedProperty<PreferencesLetterPracticeType>
        val letterDashboardSortByTime: SuspendedProperty<Boolean>

        val vocabDashboardPracticeType: SuspendedProperty<PreferencesVocabPracticeType>
        val vocabDashboardSortByTime: SuspendedProperty<Boolean>
        val vocabNoteTypeId: SuspendedProperty<String>

        val dailyResetTime: SuspendedProperty<LocalTime>

        // Navigation shell
        val navSidebarMode: SuspendedProperty<String>
        val navSidebarPosition: SuspendedProperty<String>
        val navAutoHide: SuspendedProperty<String>
        val navCollapsed: SuspendedProperty<Boolean>
        val navWidth: SuspendedProperty<Int>
        val navHeight: SuspendedProperty<Int>
        val navFloatingOffsetX: SuspendedProperty<Int>
        val navFloatingOffsetY: SuspendedProperty<Int>
        val navAccentIndex: SuspendedProperty<Int>
        val navSettingsJson: SuspendedProperty<String>
        val themeSettingsJson: SuspendedProperty<String>
        val debugSettingsJson: SuspendedProperty<String>
        val learnerProfileJson: SuspendedProperty<String>
        val romajiOverrideJson: SuspendedProperty<String>

        // Kaiteyo features (serialized JSON held in string properties)
        val reviewSettingsJson: SuspendedProperty<String>
        val backupConfigJson: SuspendedProperty<String>
        val savedSearchesJson: SuspendedProperty<String>
        val homeCommandCenterJson: SuspendedProperty<String>
        val mediaReferencesJson: SuspendedProperty<String>
        val deckFavoritesJson: SuspendedProperty<String>
        val browserColumnsJson: SuspendedProperty<String>
        val shortcutBindingsJson: SuspendedProperty<String>
        val tagSortOrder: SuspendedProperty<String>
        val collectionSortOrder: SuspendedProperty<String>
        val browserLastQuery: SuspendedProperty<String>
        val statisticsGoalsJson: SuspendedProperty<String>
        val statisticsGoalHistoryJson: SuspendedProperty<String>
        val kanjiCardLayoutJson: SuspendedProperty<String>
        val wordCardLayoutJson: SuspendedProperty<String>
        val sentenceCardLayoutJson: SuspendedProperty<String>
        val grammarCardLayoutJson: SuspendedProperty<String>
        val collectionCardLayoutJson: SuspendedProperty<String>

        // Kaiteyo World (game) progress
        val gameProgressJson: SuspendedProperty<String>
    }

    interface PracticePreferences {

        val shuffle: SuspendedProperty<Boolean>
        val newCardsOrder: SuspendedProperty<PreferencesNewCardsOrder>

        val noTranslationLayout: SuspendedProperty<Boolean>
        val leftHandMode: SuspendedProperty<Boolean>
        val altStrokeEvaluator: SuspendedProperty<Boolean>

        val highlightRadicals: SuspendedProperty<Boolean>
        val kanaAutoPlay: SuspendedProperty<Boolean>

        val writingInputMethod: SuspendedProperty<PreferencesLetterPracticeWritingInputMode>
        val letterWritingStrictness: SuspendedProperty<PreferencesWritingStrictness>
        val vocabWritingStrictness: SuspendedProperty<PreferencesWritingStrictness>
        val writingRomajiInsteadOfKanaWords: SuspendedProperty<Boolean>

        val readingRomajiFuriganaForKanaWords: SuspendedProperty<Boolean>

        val vocabFlashcardMeaningInFront: SuspendedProperty<Boolean>
        val vocabReadingPickerShowMeaning: SuspendedProperty<Boolean>
        val vocabWritingShowKanaReading: SuspendedProperty<Boolean>

    }

}

enum class PreferencesNewCardsOrder { First, Last, Mixed }
enum class PreferencesLetterPracticeType { Writing, Reading }
enum class PreferencesLetterSortOption { AddOrder, Frequency, Name, ReviewTime }
enum class PreferencesDeckDetailsLetterLayout { Character, Groups }
enum class PreferencesTheme { System, Light, Dark, Amoled, Sepia, Cream, Paper, Midnight }
enum class PreferencesLetterPracticeWritingInputMode { Stroke, Character }
enum class PreferencesWritingStrictness { Normal, Hard, Exam }
enum class PreferencesVocabPracticeType { Flashcard, ReadingPicker, Writing }
enum class PreferencesDefaultHomeTab { GeneralDashboard, Letters, Vocab }

@Serializable
data class PreferencesSyncDataInfo(
    val dataId: String,
    val dataVersion: Long,
    val dataTimestamp: Long?
)


@Serializable
data class PreferencesUserInfo(
    val email: String,
    val subscriptionEnabled: Boolean,
    val subscriptionDue: Long?
)
