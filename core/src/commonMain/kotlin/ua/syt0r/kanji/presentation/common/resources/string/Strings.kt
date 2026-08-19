package ua.syt0r.kanji.presentation.common.resources.string

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.intl.Locale
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import ua.syt0r.kanji.presentation.screen.main.screen.feedback.FeedbackScreen
import kotlin.math.roundToInt
import kotlin.time.Duration


typealias StringResolveScope <T> = @Composable Strings.() -> T

@Composable
fun <T> resolveString(resolveScope: StringResolveScope<T>): T {
    return LocalStrings.current.resolveScope()
}

fun getStrings(): Strings {
    return when (Locale.current.language) {
        "ja" -> JapaneseStrings
        else -> EnglishStrings
    }
}

val LocalStrings = compositionLocalOf<Strings> { EnglishStrings }

interface Strings {

    val appName: String

    val hiragana: String
    val katakana: String

    val kunyomi: String
    val onyomi: String

    val loading: String

    val letterPracticeTypeWriting: String
    val letterPracticeTypeReading: String
    val vocabPracticeTypeFlashcard: String
    val vocabPracticeTypeReadingPicker: String
    val vocabPracticeTypeWriting: String

    val reviewStateDone: String
    val reviewStateDue: String
    val reviewStateNew: String

    val home: HomeStrings
    val commonDashboard: CommonDashboardStrings
    val stats: StatsStrings
    val search: SearchStrings
    val settings: SettingsStrings

    val dailyLimit: DailyLimitStrings

    val tutorialDialog: TutorialDialogStrings
    val alternativeDialog: AlternativeDialogStrings

    val reminderDialog: ReminderDialogStrings
    val about: AboutStrings
    val backup: BackupStrings
    val feedback: FeedbackStrings
    val account: AccountScreenStrings
    val sync: SyncScreenStrings
    val credits: CreditsStrings

    val syncDialog: SyncDialogStrings
    val syncSnackbar: SyncSnackbarStrings

    val deckPicker: DeckPickerStrings
    val deckDetails: DeckDetailsStrings
    val deckEdit: DeckEditStrings

    val commonPractice: CommonPracticeStrings
    val letterPractice: LetterPracticeStrings
    val vocabPractice: VocabPracticeStrings

    val info: InfoScreenStrings

    val urlPickerMessage: String
    val urlPickerErrorMessage: String

    val reminderNotification: ReminderNotificationStrings

    val nav: NavStrings
    val center: SettingsCenterStrings
    val mediaCentre: MediaCentreStrings

}

/** Media Centre destination strings (placeholder screen shown off desktop). */
interface MediaCentreStrings {
    val title: String
    val desktopOnlyTitle: String
    val desktopOnlyMessage: String
    val backButton: String
    val featuresTitle: String
    val featurePlayer: String
    val featureLibrary: String
    val featureDictionary: String
    val featureMining: String
}

interface NavStrings {
    val homeSection: String
    val featuresSection: String
    val systemSection: String
    val collapseTooltip: String
    val expandTooltip: String
    val decksLabel: String
    val textAnalysisLabel: String
    val appearanceLabel: String
    val aboutLabel: String
    val backupLabel: String
    val syncLabel: String
    val accountLabel: String
    val creditsLabel: String

    // Modes
    val modeFloatingLabel: String
    val modeSidebarLabel: String
    val modeSwitchTitle: String
    val sidebarExpandedLabel: String
    val sidebarCompactLabel: String

    val placementLabel: String
    val settingsLabel: String
    val kanjiBrowserLabel: String

    // Launchpad quick access
    val quickAccessLabel: String
    val homeLabel: String
    val libraryLabel: String
    val studyLabel: String
    val browseLabel: String
    val dictionaryLabel: String
    val statisticsLabel: String
    val collectionsLabel: String
    val mediaLabel: String

    // Settings tabs
    val generalTabLabel: String
    val sidebarTabLabel: String
    val floatingTabLabel: String
    val phoneTabLabel: String
    val accessibilityTabLabel: String

    // Sidebar settings
    val expandedWidthLabel: String
    val sidebarIconSizeLabel: String
    val compactSpacingLabel: String
    val labelsVisibilityLabel: String

    // Floating (bubble) settings
    val bubbleIconSizeLabel: String
    val snapPositionLabel: String
    val sidebarLayoutLabel: String
    val holdDurationLabel: String
    val safeMarginLabel: String
    val autoHideLabel: String
    val autoHideNever: String
    val autoHideTenSeconds: String
    val autoHideTwentySeconds: String
    val autoHideThirtySeconds: String
    val autoHideOneMinute: String
    val autoHideCustom: String

    // Phone settings
    val phoneNavPositionLabel: String
    val phoneLauncherPositionLabel: String
    val phoneStoredSeparatelyHint: String

    // General settings
    val defaultModeLabel: String
    val rememberPreviousModeLabel: String
    val enableAnimationsLabel: String

    // Appear Studio entry
    val openNavigationSettingsLabel: String

}

interface AccountScreenStrings {
    val title: String

    val profileSection: String
    val usernameLabel: String
    val avatarLabel: String
    val localProfileLabel: String

    val applicationSection: String
    val syncStatusLabel: String
    val localStorageUsageLabel: String
    val databaseInfoLabel: String
    val cacheLabel: String
    val backupsLabel: String

    val studySection: String
    val currentStreakLabel: String
    val lifetimeReviewsLabel: String
    val cardsLearnedLabel: String
    val totalStudyTimeLabel: String

    val connectedServicesSection: String
    val githubServiceLabel: String
    val syncServiceLabel: String
    val dictionariesServiceLabel: String
    val pluginsServiceLabel: String

    val loggedOutMessage: String
    val signInButton: String

    val emailTitle: String
    val signOutButton: String

    val issueNoConnectionTitle: String
    val issueNoConnectionMessage: String
    val issueNoSubscriptionTitle: String
    val issueNoSubscriptionMessage: String
    val issueSessionExpiredTitle: String
    val issueSessionExpiredMessage: String
    val issueOtherTitle: String
    val issueOtherMessageFallback: String
}

interface SyncScreenStrings {
    val title: String

    val guideTitle: String
    val guideMessage: String
    val guideStepAccountTitle: String
    val guideStepAccountMessage: String
    val guideStepSubscriptionTitle: String
    val guideStepSubscriptionMessage: String

    val accountErrorMessage: String

    val statusTitle: String
    val statusMessageLoading: String
    val statusMessageDataDiffer: String
    val statusMessageLocalNewer: String
    val statusMessageUpToDate: String
    val statusMessageError: String
    val statusMessageUploading: String
    val statusMessageDownloading: String
    val statusMessageCanceled: String

    val localDataTitle: String
    val localDataIdTemplate: String
    val localDataTimestampTemplate: String

    val syncButton: String

    val errorNoConnectionTitle: String
    val errorNoConnectionMessage: String
    val errorSessionExpiredTitle: String
    val errorSessionExpiredMessage: String
    val errorNoSubscriptionTitle: String
    val errorNoSubscriptionMessage: String
    val errorOtherTitle: String
    val errorOtherMessageFallback: String

}

interface SyncDialogStrings {
    val title: String
    val buttonCancel: String
    val buttonUpload: String
    val buttonDownload: String
    val buttonAccount: String
    val uploadingMessage: String
    val downloadingMessage: String
    val conflictRemoteNewerTitle: String
    val conflictRemoteNewerMessage: String
    val conflictIncompatibleTitle: String
    val conflictIncompatibleMessage: String
    val errorNoNetworkTitle: String
    val errorNoNetworkMessage: String
    val errorNoSubscriptionTitle: String
    val errorNoSubscriptionMessage: String
    val errorNotAuthenticatedTitle: String
    val errorNotAuthenticatedMessage: String
    val errorUnexpectedErrorTitle: String
    val errorUnexpectedErrorMessage: String
    val errorUnsupportedDataTitle: String
    val errorUnsupportedDataMessage: String
}

interface SyncSnackbarStrings {
    val errorNoConnection: String
    val errorNoSubscription: String
    val errorNotAuthenticated: String
    val errorDataNotSupported: String
    val errorMessageTemplate: String
    val errorMessageNoReason: String
    val actionButton: String
}

interface TutorialDialogStrings {
    val title: String
    val page1: String
    val page2Top: String
    val page2Bottom: String
    val page3Top: String
    val page3Bottom: String
    val page4Top: String
    val page4Bottom: String
    val page5: String
}

interface FeedbackStrings {
    val title: String
    val topicTitle: String
    val topicGeneral: String
    val topicExpression: (id: Long, screen: FeedbackScreen) -> String
    val messageLabel: String
    val messageSupportingText: (messageLength: Int, maxLength: Int) -> String
        get() = { messageLength, maxLength -> "$messageLength/$maxLength" }
    val button: String
    val successMessage: String
    val errorMessage: (String?) -> String
}

interface HomeStrings {
    val screenTitle: String

    val generalDashboardTabLabel: String
    val lettersDashboardTabLabel: String
    val vocabDashboardTabLabel: String
    val libraryTabLabel: String
    val statsTabLabel: String
    val searchTabLabel: String
    val settingsTabLabel: String
}

interface CommonDashboardStrings {

    val loadFailedTitle: String
    val retryButton: String

    val emptyScreenMessage: (inlineIconId: String) -> AnnotatedString

    val mergeButton: String
    val mergeCancelButton: String
    val mergeAcceptButton: String
    val mergeTitle: String
    val mergeTitleHint: String
    val mergeSelectedCount: (Int) -> String
    val mergeClearSelectionButton: String

    val mergeDialogTitle: String
    val mergeDialogMessage: (newTitle: String, mergedTitles: List<String>) -> String
    val mergeDialogCancelButton: String
    val mergeDialogAcceptButton: String

    val sortButton: String
    val sortCancelButton: String
    val sortAcceptButton: String
    val sortTitle: String
    val sortByTimeTitle: String

    val archiveButton: String
    val restoreButton: String
    val archivedSectionTitle: (count: Int) -> String

    val itemTimeMessage: (reviewToNowDuration: Duration?) -> String
    val itemTotal: String
    val itemDone: String
    val itemReview: String
    val itemNew: String
    val dailyPracticeTitle: String
    val dailyPracticeNew: (Int) -> String
    val dailyPracticeDue: (Int) -> String
    val itemGraphProgressTitle: String
    val itemGraphProgressValue: (Float) -> String
        get() = { " ${it.roundToInt()}%" }

    val selectedPracticeTypeTemplate: (practiceType: String) -> String

}

interface DailyLimitStrings {
    val enableSwitchTitle: String
    val enableSwitchDescription: String
    val lettersSectionTitle: String
    val vocabSectionTitle: String
    val combinedLimitSwitchTitle: String
    val combinedLimitSwitchDescription: String
    val newLabel: String
    val dueLabel: String
    val noteMessage: String
    val button: String
    val changesSavedMessage: String
}

interface StatsStrings {
    val todayTitle: String
    val monthTitle: String
    val monthLabel: (day: LocalDate) -> String
    val yearTitle: String
    val yearDaysPracticedLabel: (practicedDays: Int, daysInYear: Int) -> String
    val totalTitle: String
    val timeSpentTitle: String
    val reviewsCountTitle: String
    val formattedDuration: (Duration) -> String
    val uniqueLettersReviewed: String
    val uniqueWordsReviewed: String
}

interface SearchStrings {
    val inputHint: String
    val charactersTitle: (count: Int) -> String
    val wordsTitle: (count: Int) -> String
    val radicalsSheetTitle: String
    val radicalsFoundCharacters: String
    val radicalsEmptyFoundCharacters: String
    val radicalSheetRadicalsSectionTitle: String
}

interface AlternativeDialogStrings {
    val title: String
    val readingsTitle: String
    val meaningsTitle: String
    val reportButton: String
    val closeButton: String
}

interface SettingsCenterStrings {

    // Shell
    val searchPlaceholder: String
    val searchNoResults: String
    val searchNoResultsHint: String
    val livePreviewLabel: String
    val changesApplyInstantly: String
    val resetToDefaults: String
    val resetConfirmTitle: (category: String) -> String
    val resetConfirmMessage: (category: String) -> String
    val cancel: String
    val confirm: String

    // Categories
    val categoryGeneral: String
    val categoryGeneralSubtitle: String
    val categoryAppearance: String
    val categoryAppearanceSubtitle: String
    val categoryNavigation: String
    val categoryNavigationSubtitle: String
    val categoryStudy: String
    val categoryStudySubtitle: String
    val categoryWriting: String
    val categoryWritingSubtitle: String
    val categoryFlashcards: String
    val categoryFlashcardsSubtitle: String
    val categoryNotifications: String
    val categoryNotificationsSubtitle: String
    val categoryData: String
    val categoryDataSubtitle: String
    val categoryShortcuts: String
    val categoryShortcutsSubtitle: String
    val categoryAbout: String
    val categoryAboutSubtitle: String
    val categoryAccessibility: String
    val categoryAccessibilitySubtitle: String

    // Groups
    val groupStartup: String
    val groupSchedule: String
    val groupStudy: String
    val groupApplication: String
    val groupAppearance: String
    val groupTheme: String
    val groupTypography: String
    val groupMotion: String
    val groupInput: String
    val groupStroke: String
    val groupFlashcard: String
    val groupRelated: String
    val groupLayout: String
    val groupAdvanced: String
    val groupDisplay: String
    val groupInteraction: String

    // General
    val defaultTab: String
    val defaultTabDescription: String
    val dailyResetTime: String
    val dailyResetTimeDescription: String
    val dailyLimit: String
    val dailyLimitDescription: String
    val language: String
    val languageDescription: String

    // Appearance
    val themeMode: String
    val themeModeDescription: String
    val accentColor: String
    val accentColorDescription: String
    val cornerRadius: String
    val cornerRadiusDescription: String
    val density: String
    val densityDescription: String
    val animationSpeed: String
    val animationSpeedDescription: String
    val reducedMotion: String
    val reducedMotionDescription: String
    val fontScale: String
    val fontScaleDescription: String
    val titleScale: String
    val titleScaleDescription: String
    val lineHeight: String
    val lineHeightDescription: String
    val letterSpacing: String
    val letterSpacingDescription: String
    val pageTransition: String
    val pageTransitionDescription: String
    val themeTransition: String
    val themeTransitionDescription: String
    val displayScale: String
    val displayScaleDescription: String
    val buttonScale: String
    val buttonScaleDescription: String
    val iconScale: String
    val iconScaleDescription: String
    val openThemeStudio: String
    val openThemeStudioDescription: String

    // Accessibility
    val a11yTextScale: String
    val a11yTextScaleDescription: String
    val a11yLargeIcons: String
    val a11yLargeIconsDescription: String
    val a11yLargeHitboxes: String
    val a11yLargeHitboxesDescription: String
    val a11yHighContrast: String
    val a11yHighContrastDescription: String
    val a11yReduceMotion: String
    val a11yReduceMotionDescription: String
    val a11yKeyboardNav: String
    val a11yKeyboardNavDescription: String

    // Reset all
    val resetAllLabel: String
    val resetAllDescription: String
    val resetAllConfirmTitle: String
    val resetAllConfirmMessage: String

    // Option labels
    val speedOff: String
    val speedFast: String
    val speedNormal: String
    val speedSlow: String
    val densityCompact: String
    val densityComfortable: String
    val densitySpacious: String
    val radiusSquare: String
    val radiusRounded: String
    val radiusVeryRounded: String
    val radiusSoft: String
    val newCardsFirst: String
    val newCardsLast: String
    val newCardsMixed: String

    // Study
    val learnerProfile: String
    val learnerProfileDescription: String
    val shuffle: String
    val shuffleDescription: String
    val newCardsOrder: String
    val newCardsOrderDescription: String
    val highlightRadicals: String
    val highlightRadicalsDescription: String
    val kanaAutoPlay: String
    val kanaAutoPlayDescription: String
    val letterPracticeType: String
    val letterPracticeTypeDescription: String
    val vocabPracticeType: String
    val vocabPracticeTypeDescription: String

    // Writing
    val vocabStrictness: String
    val vocabStrictnessDescription: String

    // Flashcards
    val romajiFurigana: String
    val romajiFuriganaDescription: String

    // Notifications
    val notificationsHint: String

    // Links
    val importExport: String
    val importExportDescription: String
    val shortcutsLink: String
    val shortcutsLinkDescription: String
    val creditsLink: String
    val creditsLinkDescription: String
    val feedbackLink: String
    val feedbackLinkDescription: String
    val aboutLink: String
    val aboutLinkDescription: String
    val accountLink: String
    val accountLinkDescription: String
    val syncLink: String
    val syncLinkDescription: String
    val backupLink: String
    val backupLinkDescription: String
    val themeStudioTarget: String

}

interface SettingsStrings {
    val analyticsTitle: String
    val analyticsMessage: String

    val themeTitle: String
    val themeSystem: String
    val themeLight: String
    val themeDark: String
    val themeAmoled: String
    val themeSepia: String
    val themeCream: String
    val themePaper: String
    val themeMidnight: String

    val reminderTitle: String
    val reminderEnabled: String
    val reminderDisabled: String
    val defaultTab: String
    val feedbackTitle: String
    val account: String
    val sync: String
    val backupTitle: String
    val aboutTitle: String

    val pickerDialogCancel: String
    val pickerDialogApply: String
}

interface ReminderDialogStrings {
    val title: String
    val noPermissionLabel: String
    val noPermissionButton: String
    val enabledLabel: String
    val timeLabel: String
    val cancelButton: String
    val applyButton: String
}

interface AboutStrings {
    val title: String
    val appTitle: String
    val projectTitle: String
    val developmentTitle: String
    val legalTitle: String
    val version: (versionName: String) -> String
    val buildNumber: (buildNumber: String) -> String
    val projectDescription: String

    val philosophyTitle: String
    val philosophyText: String

    val missionTitle: String
    val missionText: String

    val githubTitle: String
    val githubDescription: String
    val documentationTitle: String
    val documentationDescription: String
    val websiteTitle: String
    val websiteDescription: String

    val changelogTitle: String
    val changelogDescription: String
    val roadmapTitle: String
    val roadmapDescription: String

    val creditsTitle: String
    val creditsDescription: String

    val licenseTitle: String
    val licenseDescription: String
    val openSourceTitle: String
    val openSourceDescription: String

    val versionChangesTitle: String
    val versionChangesDescription: String
    val versionChangesButton: String
}

interface CreditsStrings {
    val title: String
    val coreDevelopmentTitle: String
    val contributorsTitle: String
    val designTitle: String
    val translationsTitle: String
    val openSourceLibrariesTitle: String
    val specialThanksTitle: String
    val licenseTitle: String
    val licenseDescription: String
    val closeButton: String
}


interface BackupStrings {
    val title: String
    val backupButton: String
    val restoreButton: String
    val unknownError: String
    val restoreVersionMessage: (backupVersion: Long, currentVersion: Long) -> String
    val restoreTimeMessage: (LocalDateTime) -> String
    val restoreNote: String
    val restoreApplyButton: String
    val completeMessage: String
}

interface DeckPickerStrings {

    val title: String

    val customDeckButton: String
    val kanaTitle: String
    val kanaDescription: (urlColor: Color) -> AnnotatedString
    val hiragana: String
    val katakana: String

    val jltpTitle: String
    val jlptDescription: StringResolveScope<AnnotatedString>
    val jlptItem: (level: Int) -> String

    val gradeTitle: String
    val gradeDescription: (urlColor: Color) -> AnnotatedString

    fun getGradeItem(grade: Int): String {
        return when {
            grade <= 6 -> gradeItemNumbered(grade)
            grade == 8 -> gradeItemSecondary
            grade == 9 -> gradeItemNames
            grade == 10 -> gradeItemNamesVariants
            else -> throw IllegalStateException("Unexpected grade $grade")
        }
    }

    val gradeItemNumbered: (Int) -> String
    val gradeItemSecondary: String
    val gradeItemNames: String
    val gradeItemNamesVariants: String

    val wanikaniTitle: String
    val wanikaniDescription: (urlColor: Color) -> AnnotatedString
    val wanikaniItem: (Int) -> String

    val vocabDeckItemWordsCountLabel: (words: Int) -> String

    val vocabOtherTitle: String
    val vocabOtherDescription: AnnotatedString
    val vocabDeckTitleTime: String
    val vocabDeckTitleWeek: String
    val vocabDeckTitleCommonVerbs: String
    val vocabDeckTitleColors: String
    val vocabDeckTitleRegularFood: String
    val vocabDeckTitleJapaneseFood: String
    val vocabDeckTitleGrammarTerms: String
    val vocabDeckTitleAnimals: String
    val vocabDeckTitleBody: String
    val vocabDeckTitleCommonPlaces: String
    val vocabDeckTitleCities: String
    val vocabDeckTitleTransport: String

}

interface DeckEditStrings {
    val createTitle: String
    val ediTitle: String

    val searchHint: String

    val editingModeSearchTitle: String
    val editingModeRemovalTitle: String

    val editingModeDetailsTitle: String
    val vocabDetailsEmptyMessage: (inlineIconId: String) -> AnnotatedString

    val completeMessage: String

    val saveTitle: String
    val saveInputHint: String
    val saveButtonDefault: String
    val saveButtonCompleted: String
    val archiveTitle: String
    val archiveHint: String

    val deleteTitle: String
    val deleteMessage: (deckTitle: String) -> String
    val deleteButtonDefault: String
    val deleteButtonCompleted: String

    val unknownTitle: String
    val unknownMessage: (characters: List<String>) -> String
    val unknownButton: String

    val leaveConfirmationTitle: String
    val leaveConfirmationMessage: String
    val leaveConfirmationCancel: String
    val leaveConfirmationAccept: String
}

interface DeckDetailsStrings {

    val emptyListMessage: String
    fun listGroupTitle(index: Int, characters: String): String = "$index. $characters"

    val detailsGroupTitle: (index: Int) -> String

    val firstTimeReviewMessage: (LocalDateTime?) -> String
    val lastTimeReviewMessage: (LocalDateTime?) -> String

    val groupDetailsDateTimeFormatter: (LocalDateTime) -> String
        get() = {
            it.run { "${dayOfMonth.withLeading0}/${monthNumber.withLeading0}/$year ${hour.withLeading0}:${minute.withLeading0}" }
        }
    val groupDetailsButton: String

    val expectedReviewDate: (LocalDate?) -> String
    val lastReviewDate: (LocalDateTime?) -> String
    val repetitions: (Int) -> String
    val lapses: (Int) -> String

    val multiselectTitle: (selectedCount: Int) -> String
    val multiselectDataNotLoaded: String
    val multiselectNoSelected: String

    val filterAllLabel: String
    val filterNoneLabel: String
    val kanaGroupsModeActivatedLabel: String

    val dialogCommon: LetterDeckDetailDialogCommonStrings
    val filterDialog: FilterDialogStrings
    val sortDialog: SortDialogStrings
    val layoutDialog: PracticePreviewLayoutDialogStrings

    val shareLetterDeckClipboardMessage: String

}

interface FilterDialogStrings {
    val title: String
}

interface SortDialogStrings {
    val title: String

    val sortOptionAddOrder: String
    val sortOptionAddOrderHint: String
    val sortOptionFrequency: String
    val sortOptionFrequencyHint: String
    val sortOptionName: String
    val sortOptionNameHint: String
    val sortOptionReviewTime: String
    val sortOptionReviewTimeHint: String
}

interface PracticePreviewLayoutDialogStrings {
    val title: String
    val singleCharacterOptionLabel: String
    val groupsOptionLabel: String
    val kanaGroupsTitle: String
    val kanaGroupsSubtitle: String
}

interface LetterDeckDetailDialogCommonStrings {
    val buttonCancel: String
    val buttonApply: String
}

interface CommonPracticeStrings {
    val configurationTitle: String
    val configurationSelectedItemsLabel: String
    val configurationCharactersPreview: String
    val shuffleConfigurationTitle: String
    val shuffleConfigurationMessage: String
    val configurationCompleteButton: String

    val additionalKanaReadingsNote: (List<String>) -> String

    val formattedSrsInterval: (Duration) -> String
    val flashcardRevealButton: String
    val againButton: String
    val hardButton: String
    val goodButton: String
    val easyButton: String

    val summaryTimeSpentValue: (Duration) -> String

    val earlyFinishDialogTitle: String
    val earlyFinishDialogMessage: String
    val earlyFinishDialogCancelButton: String
    val earlyFinishDialogAcceptButton: String

    // Real-time stroke feedback (language-neutral short labels)
    val writingStrokeCorrect: String
    val writingStrokeAlmost: String
    val writingStrokeIncorrect: String

    // Whole-character sequence feedback
    val sequenceIssueWrongOrder: String
    val sequenceIssueMissingStroke: String
    val sequenceIssueExtraStroke: String

    // Summary writing stats
    val writingStrokeAccuracyTitle: String
    val writingStrokeAccuracy: (Int) -> String
    val writingWrongOrder: (Int) -> String

    // Desktop flashcard context panel
    val reviewSessionTitle: String
    val reviewDeckLabel: String
    val reviewStreakLabel: String
    val reviewProgressPosition: (current: Int, total: Int) -> String
    val reviewStreakDays: (Int) -> String
}

interface LetterPracticeStrings {
    val configurationTitle: (practiceType: String) -> String
    val hintStrokesTitle: String
    val hintStrokesMessage: String
    val hintStrokeNewOnlyMode: String
    val hintStrokeAllMode: String
    val hintStrokeNoneMode: String
    val inputModeTitle: String
    val inputModeMessage: String
    val inputModeStroke: String
    val inputModeCharacter: String
    val evaluationStrictnessTitle: String
    val evaluationStrictnessMessage: String
    val evaluationStrictnessNormal: String
    val evaluationStrictnessHard: String
    val evaluationStrictnessExam: String
    val kanaRomajiTitle: String
    val kanaRomajiMessage: String
    val noTranslationLayoutTitle: String
    val noTranslationLayoutMessage: String
    val leftHandedModeTitle: String
    val leftHandedModeMessage: String

    val headerWordsMessage: (count: Int) -> String
    val studyFinishedButton: String
    val noKanjiTranslationsLabel: String

    val altStrokeEvaluatorTitle: String
    val altStrokeEvaluatorMessage: String

    val variantsTitle: String
    val variantsHint: String
    val unicodeTitle: (hexRepresentation: String) -> String
    val strokeCountTitle: (Int) -> String
}

interface VocabPracticeStrings {
    val configurationTitle: (practiceType: String) -> String

    val readingMeaningConfigurationTitle: String
    val readingMeaningConfigurationMessage: String

    val translationInFrontConfigurationTitle: String
    val translationInFrontConfigurationMessage: String

    val writingKanaReadingConfigurationTitle: String
    val writingKanaReadingConfigurationMessage: String

    val detailsButton: String

}

interface InfoScreenStrings {
    val strokesMessage: (count: Int) -> AnnotatedString
    val clipboardCopyMessage: String
    val radicalsSectionTitle: (count: Int) -> String
    val noRadicalsMessage: String
    val wordsSectionTitle: (count: Int) -> String
    val romajiMessage: (romaji: List<String>) -> String
    val gradeMessage: (grade: Int) -> String
    val jlptMessage: (level: Int) -> String
    val frequencyMessage: (frequency: Int) -> String
}

interface ReminderNotificationStrings {
    val channelName: String
    val title: String
    val noDetailsMessage: String
    val newOnlyMessage: (Int) -> String
    val dueOnlyMessage: (Int) -> String
    val message: (Int, Int) -> String
}

