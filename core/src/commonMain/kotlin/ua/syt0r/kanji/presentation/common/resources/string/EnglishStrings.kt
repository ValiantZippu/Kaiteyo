package ua.syt0r.kanji.presentation.common.resources.string

import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import ua.syt0r.kanji.presentation.common.CommonDateTimeFormat
import ua.syt0r.kanji.presentation.common.theme.extraColorScheme
import ua.syt0r.kanji.presentation.common.withClickableUrl
import ua.syt0r.kanji.presentation.screen.main.screen.feedback.FeedbackScreen
import kotlin.time.Duration

object EnglishStrings : Strings {

    override val appName: String = "Kaiteyo"

    override val hiragana: String = "Hiragana"
    override val katakana: String = "Katakana"

    override val kunyomi: String = "Kun"
    override val onyomi: String = "On"

    override val loading: String = "Loading"

    override val letterPracticeTypeWriting: String = "Writing"
    override val letterPracticeTypeReading: String = "Flashcards"
    override val vocabPracticeTypeFlashcard: String = "Flashcards"
    override val vocabPracticeTypeReadingPicker: String = "Reading Picker"
    override val vocabPracticeTypeWriting: String = "Writing"

    override val reviewStateDone: String = "Done"
    override val reviewStateDue: String = "Due"
    override val reviewStateNew: String = "New"

    override val home: HomeStrings = EnglishHomeStrings
    override val commonDashboard = EnglishCommonDashboardStrings
    override val dailyLimit: DailyLimitStrings = EnglishDailyLimitStrings
    override val tutorialDialog: TutorialDialogStrings = EnglishTutorialDialogStrings

    override val stats: StatsStrings = EnglishStatsStrings
    override val search: SearchStrings = EnglishSearchStrings
    override val alternativeDialog: AlternativeDialogStrings = EnglishAlternativeDialogStrings

    override val settings: SettingsStrings = EnglishSettingsStrings
    override val reminderDialog: ReminderDialogStrings = EnglishReminderDialogStrings
    override val about: AboutStrings = EnglishAboutStrings
    override val backup: BackupStrings = EnglishBackupStrings
    override val feedback: FeedbackStrings = EnglishFeedbackStrings

    override val account: AccountScreenStrings = EnglishAccountScreenStrings
    override val credits: CreditsStrings = EnglishCreditsStrings
    override val sync: SyncScreenStrings = EnglishSyncScreenStrings
    override val syncDialog: SyncDialogStrings = EnglishSyncDialogStrings
    override val syncSnackbar: SyncSnackbarStrings = EnglishSyncSnackbarStrings

    override val deckPicker: DeckPickerStrings = EnglishDeckPickerStrings
    override val deckEdit: DeckEditStrings = EnglishDeckEditStrings
    override val deckDetails: DeckDetailsStrings = EnglishDeckDetailsStrings
    override val commonPractice: CommonPracticeStrings = EnglishCommonPracticeStrings
    override val letterPractice: LetterPracticeStrings = EnglishLetterPracticeStrings
    override val vocabPractice: VocabPracticeStrings = EnglishVocabPracticeStrings
    override val info: InfoScreenStrings = EnglishInfoScreenStrings

    override val urlPickerMessage: String = "Open With"
    override val urlPickerErrorMessage: String = "Web browser not found"

    override val reminderNotification: ReminderNotificationStrings =
        EnglishReminderNotificationStrings

    override val nav: NavStrings = EnglishNavStrings

    override val center: SettingsCenterStrings = EnglishSettingsCenterStrings

    override val mediaCentre: MediaCentreStrings = EnglishMediaCentreStrings

}

object EnglishMediaCentreStrings : MediaCentreStrings {
    override val title: String = "Media Centre"
    override val desktopOnlyTitle: String = "Media Centre is a desktop feature"
    override val desktopOnlyMessage: String =
        "The immersion workspace — player, subtitles, dictionary and mining — runs in the desktop app. Open Kaiteyo on your computer to use it."
    override val backButton: String = "Back"
    override val featuresTitle: String = "What you'll find in the desktop Media Centre"
    override val featurePlayer: String =
        "Video & audio player (VLC / mpv) with Japanese subtitle support"
    override val featureLibrary: String =
        "Media library with folders, playlists and watch progress"
    override val featureDictionary: String =
        "Click any word in a subtitle for dictionary, pitch and frequency"
    override val featureMining: String =
        "Mine cards with sentence, screenshot and audio — to Kaiteyo decks or Anki"
}

object EnglishSettingsCenterStrings : SettingsCenterStrings {

    override val searchPlaceholder: String = "Search settings"
    override val searchNoResults: String = "No settings found"
    override val searchNoResultsHint: String =
        "Try a different keyword — for example \"sidebar\", \"font\" or \"bubble\"."
    override val livePreviewLabel: String = "Live preview"
    override val changesApplyInstantly: String =
        "Changes apply immediately and are saved automatically"
    override val resetToDefaults: String = "Reset to defaults"
    override val resetConfirmTitle: (category: String) -> String =
        { "Reset $it settings?" }
    override val resetConfirmMessage: (category: String) -> String = {
        "All $it settings will be restored to their default values."
    }
    override val cancel: String = "Cancel"
    override val confirm: String = "Reset"

    override val categoryGeneral: String = "General"
    override val categoryGeneralSubtitle: String = "Startup behavior, schedule and app defaults"
    override val categoryAppearance: String = "Appearance"
    override val categoryAppearanceSubtitle: String = "Theme, colors, typography and density"
    override val categoryNavigation: String = "Navigation"
    override val categoryNavigationSubtitle: String =
        "Sidebar, floating launcher and phone layout"
    override val categoryStudy: String = "Study"
    override val categoryStudySubtitle: String = "Session behavior and practice defaults"
    override val categoryWriting: String = "Writing"
    override val categoryWritingSubtitle: String = "Stroke evaluation and input mode"
    override val categoryFlashcards: String = "Flashcards"
    override val categoryFlashcardsSubtitle: String = "Card layout and answer behavior"
    override val categoryNotifications: String = "Notifications"
    override val categoryNotificationsSubtitle: String = "Daily study reminders"
    override val categoryData: String = "Data & Sync"
    override val categoryDataSubtitle: String = "Backup, restore, sync and account"
    override val categoryShortcuts: String = "Keyboard & Shortcuts"
    override val categoryShortcutsSubtitle: String = "Global and review shortcuts"
    override val categoryAbout: String = "About"
    override val categoryAboutSubtitle: String = "App info, credits and feedback"
    override val categoryAccessibility: String = "Accessibility"
    override val categoryAccessibilitySubtitle: String =
        "Text scaling, contrast, motion and touch targets"

    override val groupStartup: String = "Startup"
    override val groupSchedule: String = "Schedule"
    override val groupStudy: String = "Study"
    override val groupApplication: String = "Application"
    override val groupAppearance: String = "Appearance"
    override val groupTheme: String = "Theme"
    override val groupTypography: String = "Typography"
    override val groupMotion: String = "Motion"
    override val groupInput: String = "Input method"
    override val groupStroke: String = "Stroke evaluation"
    override val groupFlashcard: String = "Flashcards"
    override val groupRelated: String = "Related"
    override val groupLayout: String = "Layout & scaling"
    override val groupAdvanced: String = "Advanced"
    override val groupDisplay: String = "Display"
    override val groupInteraction: String = "Interaction"

    override val defaultTab: String = "Default tab"
    override val defaultTabDescription: String = "Tab shown when the app opens"
    override val dailyResetTime: String = "Daily reset time"
    override val dailyResetTimeDescription: String =
        "When new cards and daily limits reset"
    override val dailyLimit: String = "Daily limit"
    override val dailyLimitDescription: String =
        "Limit new cards and reviews per day"
    override val language: String = "Language"
    override val languageDescription: String =
        "Interface language — follows the system"

    override val themeMode: String = "Theme mode"
    override val themeModeDescription: String = "Light, dark, system or AMOLED"
    override val accentColor: String = "Accent color"
    override val accentColorDescription: String =
        "Primary color used across the interface"
    override val cornerRadius: String = "Corner radius"
    override val cornerRadiusDescription: String =
        "Rounding of cards, buttons and panels"
    override val density: String = "Interface density"
    override val densityDescription: String =
        "Spacing and sizing of interface elements"
    override val animationSpeed: String = "Animation speed"
    override val animationSpeedDescription: String =
        "How fast interface animations play"
    override val reducedMotion: String = "Reduce motion"
    override val reducedMotionDescription: String =
        "Disable most interface animations"
    override val fontScale: String = "Font size"
    override val fontScaleDescription: String =
        "Scales text across the application"
    override val titleScale: String = "Heading size"
    override val titleScaleDescription: String =
        "Scales titles, headings and navigation labels"
    override val lineHeight: String = "Line height"
    override val lineHeightDescription: String =
        "Vertical rhythm of body text"
    override val letterSpacing: String = "Letter spacing"
    override val letterSpacingDescription: String =
        "Extra tracking between characters"
    override val pageTransition: String = "Page transition"
    override val pageTransitionDescription: String =
        "How screens transition when you navigate"
    override val themeTransition: String = "Theme transition"
    override val themeTransitionDescription: String =
        "Crossfade colors smoothly when the theme changes"
    override val displayScale: String = "Display scale"
    override val displayScaleDescription: String =
        "Scales the whole interface (zoom)"
    override val buttonScale: String = "Button scale"
    override val buttonScaleDescription: String =
        "Scales buttons and touch controls"
    override val iconScale: String = "Icon scale"
    override val iconScaleDescription: String =
        "Scales icons across the interface"
    override val openThemeStudio: String = "Open Theme Studio"
    override val openThemeStudioDescription: String =
        "Deep theme editor — colors, gradients, motion and layout"

    override val a11yTextScale: String = "Text scale"
    override val a11yTextScaleDescription: String =
        "Scales text across the whole application"
    override val a11yLargeIcons: String = "Larger navigation icons"
    override val a11yLargeIconsDescription: String =
        "Enlarge icons in the sidebar and floating launcher"
    override val a11yLargeHitboxes: String = "Larger touch targets"
    override val a11yLargeHitboxesDescription: String =
        "Expand the clickable area of navigation controls"
    override val a11yHighContrast: String = "High contrast"
    override val a11yHighContrastDescription: String =
        "Stronger contrast for the navigation surfaces"
    override val a11yReduceMotion: String = "Reduce navigation motion"
    override val a11yReduceMotionDescription: String =
        "Disable navigation and launcher animations"
    override val a11yKeyboardNav: String = "Keyboard navigation"
    override val a11yKeyboardNavDescription: String =
        "Move between destinations with arrow keys and shortcuts"

    override val resetAllLabel: String = "Reset all settings"
    override val resetAllDescription: String =
        "Restore every category to its default values"
    override val resetAllConfirmTitle: String = "Reset all settings?"
    override val resetAllConfirmMessage: String =
        "All settings in every category will be restored to their defaults. This cannot be undone."

    override val speedOff: String = "Off"
    override val speedFast: String = "Fast"
    override val speedNormal: String = "Normal"
    override val speedSlow: String = "Slow"
    override val densityCompact: String = "Compact"
    override val densityComfortable: String = "Comfortable"
    override val densitySpacious: String = "Spacious"
    override val radiusSquare: String = "Sharp"
    override val radiusRounded: String = "Rounded"
    override val radiusVeryRounded: String = "Very rounded"
    override val radiusSoft: String = "Soft"
    override val newCardsFirst: String = "First"
    override val newCardsLast: String = "Last"
    override val newCardsMixed: String = "Mixed"

    override val learnerProfile: String = "Learner profile"
    override val learnerProfileDescription: String =
        "Adapt kanji, word and sentence pages to your level — furigana, romaji, depth and more"
    override val shuffle: String = "Shuffle order"
    override val shuffleDescription: String =
        "Randomize the order of items in practice sessions"
    override val newCardsOrder: String = "New cards order"
    override val newCardsOrderDescription: String =
        "Where new cards appear in a session"
    override val highlightRadicals: String = "Highlight radicals"
    override val highlightRadicalsDescription: String =
        "Show the radical breakdown of letters during writing"
    override val kanaAutoPlay: String = "Kana audio autoplay"
    override val kanaAutoPlayDescription: String =
        "Play the kana reading automatically in practice"
    override val letterPracticeType: String = "Letter practice type"
    override val letterPracticeTypeDescription: String =
        "Practice mode used from the Letters dashboard"
    override val vocabPracticeType: String = "Vocab practice type"
    override val vocabPracticeTypeDescription: String =
        "Practice mode used from the Vocab dashboard"

    override val vocabStrictness: String = "Vocab stroke strictness"
    override val vocabStrictnessDescription: String =
        "Tighter tolerances for stroke recognition of words"

    override val romajiFurigana: String = "Romaji furigana"
    override val romajiFuriganaDescription: String =
        "Show romaji above kana words in reading practice"

    override val notificationsHint: String =
        "Daily study reminders are available on Android"

    override val importExport: String = "Import / Export"
    override val importExportDescription: String =
        "Anki packages and other transfers"
    override val shortcutsLink: String = "Keyboard shortcuts"
    override val shortcutsLinkDescription: String =
        "View and customize review and navigation shortcuts"
    override val creditsLink: String = "Credits"
    override val creditsLinkDescription: String =
        "Contributors, translators and open-source libraries"
    override val feedbackLink: String = "Send feedback"
    override val feedbackLinkDescription: String =
        "Report issues or suggest improvements"
    override val aboutLink: String = "About Kaiteyo"
    override val aboutLinkDescription: String =
        "Version, project info and documentation"
    override val accountLink: String = "Account"
    override val accountLinkDescription: String =
        "Profile, subscription and sign-in"
    override val syncLink: String = "Sync"
    override val syncLinkDescription: String =
        "Synchronize your progress across devices"
    override val backupLink: String = "Backup & Restore"
    override val backupLinkDescription: String =
        "Create and restore database backups"
    override val themeStudioTarget: String = "Theme Studio"
}

object EnglishNavStrings : NavStrings {
    override val homeSection: String = "Home"
    override val featuresSection: String = "Features"
    override val systemSection: String = "System"
    override val collapseTooltip: String = "Collapse"
    override val expandTooltip: String = "Expand"
    override val decksLabel: String = "Decks"
    override val textAnalysisLabel: String = "Text Analysis"
    override val appearanceLabel: String = "Appearance"
    override val aboutLabel: String = EnglishAboutStrings.title
    override val accountLabel: String = "Account"
    override val backupLabel: String = EnglishBackupStrings.title
    override val syncLabel: String = EnglishSyncScreenStrings.title
    override val creditsLabel: String = EnglishCreditsStrings.title
    override val modeFloatingLabel: String = "Floating"
    override val modeSidebarLabel: String = "Sidebar"
    override val modeSwitchTitle: String = "Navigation mode"
    override val sidebarExpandedLabel: String = "Expanded"
    override val sidebarCompactLabel: String = "Compact"
    override val placementLabel: String = "Placement"
    override val settingsLabel: String = "Navigation settings"
    override val kanjiBrowserLabel: String = "Kanji Browser"

    override val quickAccessLabel: String = "Quick Access"
    override val homeLabel: String = "Home"
    override val libraryLabel: String = EnglishHomeStrings.libraryTabLabel
    override val studyLabel: String = "Study"
    override val browseLabel: String = "Browse"
    override val dictionaryLabel: String = "Dictionary"
    override val statisticsLabel: String = EnglishHomeStrings.statsTabLabel
    override val collectionsLabel: String = "Collections"
    override val mediaLabel: String = "Media"

    override val generalTabLabel: String = "General"
    override val sidebarTabLabel: String = "Sidebar"
    override val floatingTabLabel: String = "Floating"
    override val phoneTabLabel: String = "Phone"
    override val accessibilityTabLabel: String = "Accessibility"

    override val expandedWidthLabel: String = "Expanded width"
    override val sidebarIconSizeLabel: String = "Icon size"
    override val compactSpacingLabel: String = "Compact item spacing"
    override val labelsVisibilityLabel: String = "Labels"

    override val bubbleIconSizeLabel: String = "Icon size"
    override val snapPositionLabel: String = "Snap position"
    override val sidebarLayoutLabel: String = "Sidebar layout"
    override val holdDurationLabel: String = "Hold duration"
    override val safeMarginLabel: String = "Safe margin"
    override val autoHideLabel: String = "Auto-hide"
    override val autoHideNever: String = "Never"
    override val autoHideTenSeconds: String = "10 seconds"
    override val autoHideTwentySeconds: String = "20 seconds"
    override val autoHideThirtySeconds: String = "30 seconds"
    override val autoHideOneMinute: String = "1 minute"
    override val autoHideCustom: String = "Custom"

    override val phoneNavPositionLabel: String = "Navigation position"
    override val phoneLauncherPositionLabel: String = "Launcher position"
    override val phoneStoredSeparatelyHint: String =
        "Phone settings are stored separately from desktop and tablet"

    override val defaultModeLabel: String = "Default mode"
    override val rememberPreviousModeLabel: String = "Remember previous mode"
    override val enableAnimationsLabel: String = "Enable animations"

    override val openNavigationSettingsLabel: String = "Open Navigation Settings"
}

object EnglishHomeStrings : HomeStrings {
    override val screenTitle: String = "Kaiteyo"
    override val generalDashboardTabLabel: String = "Home"
    override val lettersDashboardTabLabel: String = "Letters"
    override val vocabDashboardTabLabel: String = "Vocab"
    override val libraryTabLabel: String = "Library"
    override val statsTabLabel: String = "Stats"
    override val searchTabLabel: String = "Search"
    override val settingsTabLabel: String = "Settings"
}

object EnglishCommonDashboardStrings : CommonDashboardStrings {

    override val loadFailedTitle: String = "Couldn't load your decks"
    override val retryButton: String = "Retry"

    override val emptyScreenMessage: (inlineIconId: String) -> AnnotatedString = { inlineIconId ->
        buildAnnotatedString {
            append("Create deck by clicking on ")
            appendInlineContent(inlineIconId)
            append(" button. Decks are used to track your progress")
        }
    }

    override val mergeButton: String = "Merge"
    override val mergeCancelButton: String = "Cancel"
    override val mergeAcceptButton: String = "Merge"
    override val mergeTitle: String = "Merge multiple decks into one"
    override val mergeTitleHint: String = "Enter title here"
    override val mergeSelectedCount: (Int) -> String = { "$it selected" }
    override val mergeClearSelectionButton: String = "Clear"

    override val mergeDialogTitle: String = "Merge Confirmation"
    override val mergeDialogMessage: (String, List<String>) -> String = { newTitle, mergedTitles ->
        "Following ${mergedTitles.size} decks will be merged into the new \"$newTitle\" deck: ${mergedTitles.joinToString()}"
    }
    override val mergeDialogCancelButton: String = "Cancel"
    override val mergeDialogAcceptButton: String = "Merge"

    override val sortButton: String = "Sort"
    override val sortCancelButton: String = "Cancel"
    override val sortAcceptButton: String = "Apply"
    override val sortTitle: String = "Change decks order"
    override val sortByTimeTitle: String = "Sort by last review time"

    override val archiveButton: String = "Archive"
    override val restoreButton: String = "Restore"
    override val archivedSectionTitle: (Int) -> String = { "Archived ($it)" }

    override val itemTimeMessage: (Duration?) -> String = {
        "Last review: " + when {
            it == null -> "Never"
            it.inWholeDays == 1L -> "1 day ago"
            it.inWholeDays > 0 -> "${it.inWholeDays} days ago"
            else -> "< 1 day ago"
        }
    }
    override val itemTotal: String = "Total"
    override val itemDone: String = "Done"
    override val itemReview: String = "Due"
    override val itemNew: String = "New"
    override val dailyPracticeTitle: String = "Daily practice"
    override val dailyPracticeNew: (Int) -> String = { "New ($it)" }
    override val dailyPracticeDue: (Int) -> String = { "Due ($it)" }
    override val itemGraphProgressTitle: String = "Completion"

    override val selectedPracticeTypeTemplate: (practiceType: String) -> String = {
        "Practice Type: $it"
    }
}

object EnglishDailyLimitStrings : DailyLimitStrings {
    override val enableSwitchTitle: String = "Daily Limit"
    override val enableSwitchDescription: String =
        "Limit the number of daily reviews prompted by the app"
    override val lettersSectionTitle: String = "Letters"
    override val vocabSectionTitle: String = "Vocab"
    override val combinedLimitSwitchTitle: String = "Combined Limit"
    override val combinedLimitSwitchDescription: String = "Share limit across all practice types"
    override val newLabel: String = "New"
    override val dueLabel: String = "Due"
    override val noteMessage: String =
        "Note: Writing and reading reviews are counted separately towards the limit"
    override val button: String = "Save"
    override val changesSavedMessage: String = "Done"
}

object EnglishTutorialDialogStrings : TutorialDialogStrings {
    override val title: String = "Tutorial"

    override val page1: String = """
        • The app uses the Spaced Repetition System (SRS) - a highly effective learning method that optimizes the timing of reviews based on how well you remember information
        • When you do a review the SRS schedules a follow-up review according to your recall ability
        • If you recall an item easily, the interval between reviews increases. If you struggle, the interval is shortened
    """.trimIndent()

    override val page2Top: String = """
        • To estimate your recall ability the app will offer you various rating options after review
    """.trimIndent()

    override val page2Bottom: String = """
        • Choose the option that best matches your ability to recall the item you're reviewing
        • Grading your own answers allows the app to adjust the learning pace according to individual memory capabilities
    """.trimIndent()

    override val page3Top: String = """
        • Every day, the status of each reviewed item is updated
        • The app will let you review several new items and due items that are past their scheduled review time
    """.trimIndent()

    override val page3Bottom: String = """
        • You can set a daily limit to control your workload at a comfortable level
    """.trimIndent()

    override val page4Top: String = """
        • To start using the app create a deck
        • Decks are used to organize the items you want to master. There are letter and vocab decks in the app
    """.trimIndent()

    override val page4Bottom: String = """
        • You can create your own decks from scratch or select from several pre-made ones
    """.trimIndent()

    override val page5: String = """
        • Once any deck is created you can start doing reviews
        • There are several practice modes available, check them all
        • Stay consistent - regular practice is key to making steady progress. Don't hesitate to lower your daily limit to avoid burnout
        • Good luck on your way to mastering Japanese! \(^_^)/
    """.trimIndent()
}

private val months = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug",
    "Sep", "Oct", "Nov", "Dec"
)

private fun formatDuration(duration: Duration): String = when {
    duration.inWholeHours > 0 -> "${duration.inWholeHours}h ${duration.inWholeMinutes % 60}m"
    duration.inWholeMinutes > 0 -> "${duration.inWholeMinutes}m ${duration.inWholeSeconds % 60}s"
    else -> "${duration.inWholeSeconds}s"
}

object EnglishStatsStrings : StatsStrings {
    override val todayTitle: String = "Today"
    override val monthTitle: String = "This month"
    override val monthLabel: (day: LocalDate) -> String = {
        it.run { "${months[monthNumber - 1]}, $year" }
    }
    override val yearTitle: String = "This year"
    override val yearDaysPracticedLabel = { practicedDays: Int, daysInYear: Int ->
        "Days practiced: $practicedDays/$daysInYear"
    }
    override val totalTitle: String = "Total"
    override val timeSpentTitle: String = "Time spent"
    override val reviewsCountTitle: String = "Reviews"
    override val formattedDuration: (Duration) -> String = { formatDuration(it) }
    override val uniqueLettersReviewed: String = "Unique letters reviewed"
    override val uniqueWordsReviewed: String = "Unique words reviewed"
}

object EnglishSearchStrings : SearchStrings {
    override val inputHint: String = "Search for letters or words"
    override val charactersTitle: (count: Int) -> String = { "Letters ($it)" }
    override val wordsTitle: (count: Int) -> String = { "Words ($it)" }
    override val radicalsSheetTitle: String = "Search by radicals"
    override val radicalsFoundCharacters: String = "Found letters"
    override val radicalsEmptyFoundCharacters: String = "Nothing found"
    override val radicalSheetRadicalsSectionTitle: String = "Radicals"
}

object EnglishAlternativeDialogStrings : AlternativeDialogStrings {
    override val title: String = "Alternative expressions"
    override val readingsTitle: String = "Readings"
    override val meaningsTitle: String = "Meanings"
    override val reportButton: String = "Report"
    override val closeButton: String = "Close"
}

object EnglishSettingsStrings : SettingsStrings {
    override val analyticsTitle: String = "Analytics"
    override val analyticsMessage: String = "Allow sending anonymous app usage data"
    override val themeTitle: String = "Theme"
    override val themeSystem: String = "System"
    override val themeLight: String = "Light"
    override val themeDark: String = "Dark"
    override val themeAmoled: String = "AMOLED"
    override val themeSepia: String = "Sepia"
    override val themeCream: String = "Cream"
    override val themePaper: String = "Paper"
    override val themeMidnight: String = "Midnight"
    override val reminderTitle: String = "Reminder Notification"
    override val reminderEnabled: String = "Enabled"
    override val reminderDisabled: String = "Disabled"
    override val defaultTab: String = "Default Tab"
    override val feedbackTitle: String = "Feedback"
    override val account: String = "Account"
    override val sync: String = "Sync (Preview)"
    override val backupTitle: String = "Backup & Restore"
    override val aboutTitle: String = "About"
    override val pickerDialogCancel: String = "Cancel"
    override val pickerDialogApply: String = "Apply"
}

object EnglishReminderDialogStrings : ReminderDialogStrings {
    override val title: String = "Reminder Notification"
    override val noPermissionLabel: String = "Missing notification permission"
    override val noPermissionButton: String = "Grant"
    override val enabledLabel: String = "Enabled"
    override val timeLabel: String = "Time"
    override val cancelButton: String = "Close"
    override val applyButton: String = "Apply"
}

object EnglishAboutStrings : AboutStrings {
    override val title: String = "About"
    override val appTitle: String = "Application"
    override val projectTitle: String = "Project"
    override val developmentTitle: String = "Development"
    override val legalTitle: String = "Legal"
    override val version: (versionName: String) -> String = { "Version: $it" }
    override val buildNumber: (buildNumber: String) -> String = { "Build: $it" }
    override val projectDescription: String =
        "Kaiteyo is a premium, cross-platform Japanese language learning application built with Compose Multiplatform."

    override val philosophyTitle: String = "Philosophy"
    override val philosophyText: String =
        "Desktop first, craft over features, offline by default, open source, no gamification."

    override val missionTitle: String = "Mission"
    override val missionText: String =
        "To build the most polished and effective Japanese learning experience on desktop, with a focus on clarity, responsiveness, and smooth motion."

    override val githubTitle: String = "GitHub"
    override val githubDescription: String = "Source code, bug reports, discussions"
    override val documentationTitle: String = "Documentation"
    override val documentationDescription: String = "Guides, architecture, and development docs"
    override val websiteTitle: String = "Website"
    override val websiteDescription: String = "Project homepage and showcase"

    override val changelogTitle: String = "Changelog"
    override val changelogDescription: String = "Release history and notable changes"
    override val roadmapTitle: String = "Roadmap"
    override val roadmapDescription: String = "Upcoming features and milestones"

    override val creditsTitle: String = "Credits"
    override val creditsDescription: String = "Contributors, translators, designers, and libraries"

    override val licenseTitle: String = "License"
    override val licenseDescription: String = "MIT license — see full text"
    override val openSourceTitle: String = "Open Source"
    override val openSourceDescription: String = "Third-party libraries and data sources"

    override val versionChangesTitle: String = "Version Changes"
    override val versionChangesDescription: String = "App changes history"
    override val versionChangesButton: String = "Close"
}

object EnglishAccountScreenStrings : AccountScreenStrings {
    override val title = "Account"
    override val profileSection = "Profile"
    override val usernameLabel = "Username"
    override val avatarLabel = "Avatar"
    override val localProfileLabel = "Local Profile"
    override val applicationSection = "Application"
    override val syncStatusLabel = "Sync Status"
    override val localStorageUsageLabel = "Local Storage"
    override val databaseInfoLabel = "Database"
    override val cacheLabel = "Cache"
    override val backupsLabel = "Backups"
    override val studySection = "Study"
    override val currentStreakLabel = "Current Streak"
    override val lifetimeReviewsLabel = "Lifetime Reviews"
    override val cardsLearnedLabel = "Cards Learned"
    override val totalStudyTimeLabel = "Total Study Time"
    override val connectedServicesSection = "Connected Services"
    override val githubServiceLabel = "GitHub"
    override val syncServiceLabel = "Sync"
    override val dictionariesServiceLabel = "Dictionaries"
    override val pluginsServiceLabel = "Plugins"
    override val loggedOutMessage = "Signed out"
    override val signInButton = "Sign In"
    override val signOutButton = "Sign Out"
    override val emailTitle = "E-mail"
    override val issueNoConnectionTitle = "No Connection"
    override val issueNoConnectionMessage = "Showing cached data"
    override val issueNoSubscriptionTitle = "Subscription Not Active"
    override val issueNoSubscriptionMessage = "Subscription is required for sync"
    override val issueSessionExpiredTitle = "Session Expired"
    override val issueSessionExpiredMessage = "Click to sign in again"
    override val issueOtherTitle = "Error"
    override val issueOtherMessageFallback = "Unknown error"
}

object EnglishCreditsStrings : CreditsStrings {
    override val title: String = "Credits"
    override val coreDevelopmentTitle: String = "Core Development"
    override val contributorsTitle: String = "Contributors"
    override val designTitle: String = "Design"
    override val translationsTitle: String = "Translations"
    override val openSourceLibrariesTitle: String = "Open Source Libraries"
    override val specialThanksTitle: String = "Special Thanks"
    override val licenseTitle: String = "License"
    override val licenseDescription: String = "Kaiteyo is open-source software released under the MIT license."
    override val closeButton: String = "Close"
}

object EnglishBackupStrings : BackupStrings {
    override val title: String = "Backup & Restore"
    override val backupButton: String = "Create backup"
    override val restoreButton: String = "Restore from backup"
    override val unknownError: String = "Unknown error"
    override val restoreVersionMessage: (Long, Long) -> String = { backupVersion, currentVersion ->
        "Database version: $backupVersion (Current: $currentVersion)"
    }
    override val restoreTimeMessage: (LocalDateTime) -> String = {
        "Create time: ${it.format(CommonDateTimeFormat)}"
    }
    override val restoreNote: String =
        "Note! All current progress will be replaced with the progress from the selected backup"
    override val restoreApplyButton: String = "Restore"
    override val completeMessage: String = "Done"
}

object EnglishFeedbackStrings : FeedbackStrings {
    override val title: String = "Feedback"
    override val topicTitle: String = "Topic"
    override val topicGeneral: String = "General"
    override val topicExpression: (id: Long, screen: FeedbackScreen) -> String = { id, screen ->
        val screenName: String = when (screen) {
            FeedbackScreen.WritingPractice -> "Writing practice"
            FeedbackScreen.ReadingPractice -> "Reading practice"
            FeedbackScreen.Search -> "Search"
            FeedbackScreen.CharacterInfo -> "Letter info"
            FeedbackScreen.VocabPractice -> "Vocab practice"
        }
        "$screenName, expression $id"
    }
    override val messageLabel: String = "Enter feedback here"
    override val button: String = "Send"
    override val successMessage: String = "Feedback sent"
    override val errorMessage: (String?) -> String = { "Error: $it" }
}

object EnglishDeckPickerStrings : DeckPickerStrings {

    override val title: String = "Select Deck"

    override val customDeckButton: String = "Create Empty"
    override val kanaTitle: String = "Kana"

    override val kanaDescription = { urlColor: Color ->
        buildAnnotatedString {
            append(
                "Japanese kana characters are a set of syllabic characters used in the Japanese writing system. There are two main types of kana: \n" +
                        " • Hiragana - used for native Japanese words and grammatical elements\n" +
                        " • Katakana - often used for loanwords, names, and technical terms\n" +
                        "Kana characters represent sound units, making them an essential part of reading and writing in the Japanese language. "
            )
            withClickableUrl(
                url = "https://en.wikipedia.org/wiki/Kana",
                color = urlColor
            ) {
                append("More info.")
            }
        }
    }
    override val hiragana: String = "Hiragana"
    override val katakana: String = "Katakana"

    override val jltpTitle: String = "JLPT"
    override val jlptDescription: StringResolveScope<AnnotatedString> = {
        buildAnnotatedString {
            append("The Japanese-Language Proficiency Test, or JLPT, is a standardized criterion-referenced test to evaluate and certify Japanese language proficiency for non-native speakers, covering language knowledge, reading ability, and listening ability. ")
            withClickableUrl(
                url = "https://en.wikipedia.org/wiki/Japanese-Language_Proficiency_Test",
                color = MaterialTheme.extraColorScheme.link
            ) {
                append("More info.")
            }
        }
    }
    override val jlptItem: (level: Int) -> String = { "JLPT・N$it" }

    override val gradeTitle: String = "Grade"
    override val gradeDescription = { urlColor: Color ->
        buildAnnotatedString {
            withClickableUrl("https://en.wikipedia.org/wiki/J%C5%8Dy%C5%8D_kanji", urlColor) {
                append("The Jōyō kanji")
            }
            append(" is a list of 2,136 frequently used characters maintained officially by the Japanese Ministry of Education. ")
            append("All these characters are taught in Japanese schools:\n")
            append(" • 1,026 kanji taught in primary school (Grade 1-6) (the ")
            withClickableUrl("https://en.wikipedia.org/wiki/Ky%C5%8Diku_kanji", urlColor) {
                append("kyōiku kanji")
            }
            append(")\n")
            append(" • 1,110 additional kanji taught in secondary school (Grade 7-12)")
        }
    }
    override val gradeItemNumbered: (Int) -> String = { "Grade $it" }
    override val gradeItemSecondary: String = "Secondary school"
    override val gradeItemNames: String = "Kanji for use in names (Jinmeiyō)"
    override val gradeItemNamesVariants: String = "Jinmeiyō kanji variants of Jōyō"

    override val wanikaniTitle: String = "WaniKani"
    override val wanikaniDescription = { urlColor: Color ->
        buildAnnotatedString {
            append("Kanji lists according to levels on website WaniKani by Tofugu. ")
            withClickableUrl("https://www.wanikani.com/kanji?difficulty=pleasant", urlColor) {
                append("More info. ")
            }
        }
    }
    override val wanikaniItem: (Int) -> String = { "WaniKani Level $it" }

    override val vocabOtherTitle: String = "Other"
    override val vocabOtherDescription: AnnotatedString = buildAnnotatedString {
        append("A collection of small vocabulary decks covering common topics to help you get started")
    }

    override val vocabDeckItemWordsCountLabel: (words: Int) -> String = { "$it words" }

    override val vocabDeckTitleTime: String = "Time"
    override val vocabDeckTitleWeek: String = "Week Days"
    override val vocabDeckTitleCommonVerbs: String = "Common Verbs"
    override val vocabDeckTitleColors: String = "Colors"
    override val vocabDeckTitleRegularFood: String = "Regular Food"
    override val vocabDeckTitleJapaneseFood: String = "Japanese Food"
    override val vocabDeckTitleGrammarTerms: String = "Grammar Terms"
    override val vocabDeckTitleAnimals: String = "Animals"
    override val vocabDeckTitleBody: String = "Body"
    override val vocabDeckTitleCommonPlaces: String = "Common Places"
    override val vocabDeckTitleCities: String = "Cities"
    override val vocabDeckTitleTransport: String = "Transport"

}

object EnglishDeckEditStrings : DeckEditStrings {
    override val createTitle: String = "Create Deck"
    override val ediTitle: String = "Edit Deck"
    override val searchHint: String = "Enter kana or kanji"
    override val editingModeSearchTitle: String = "Search"
    override val editingModeRemovalTitle: String = "Removal"
    override val editingModeDetailsTitle: String = "Details"
    override val vocabDetailsEmptyMessage: (inlineIconId: String) -> AnnotatedString = {
        buildAnnotatedString {
            append("No cards. To add new cards save this deck and use ")
            appendInlineContent(it)
            append(" icon on search screen, during writing reviews and other places in the app")
        }
    }
    override val completeMessage: String = "Done"
    override val saveTitle: String = "Save changes"
    override val saveInputHint: String = "Deck Title"
    override val saveButtonDefault: String = "Save"
    override val saveButtonCompleted: String = "Done"
    override val archiveTitle: String = "Archive deck"
    override val archiveHint: String = "Hidden from the main deck list until unarchived"
    override val deleteTitle: String = "Delete confirmation"
    override val deleteMessage: (deckTitle: String) -> String = {
        "Are you sure you want to delete \"$it\" deck?"
    }
    override val deleteButtonDefault: String = "Delete"
    override val deleteButtonCompleted: String = "Done"

    override val unknownTitle: String = "Unknown letters"
    override val unknownMessage: (characters: List<String>) -> String = {
        "Some letters were not found: ${it.joinToString()}"
    }
    override val unknownButton: String = "Close"

    override val leaveConfirmationTitle: String = "Leave confirmation"
    override val leaveConfirmationMessage: String = "All changes will be lost"
    override val leaveConfirmationCancel: String = "Cancel"
    override val leaveConfirmationAccept: String = "Leave"
}

object EnglishDeckDetailsStrings : DeckDetailsStrings {
    override val emptyListMessage: String = "Nothing here"
    override val detailsGroupTitle: (index: Int) -> String = { "Group $it" }
    override val firstTimeReviewMessage: (LocalDateTime?) -> String = {
        "First review time: " + when (it) {
            null -> "Never"
            else -> groupDetailsDateTimeFormatter(it)
        }
    }
    override val lastTimeReviewMessage: (LocalDateTime?) -> String = {
        "Last review time: " + when (it) {
            null -> "Never"
            else -> groupDetailsDateTimeFormatter(it)
        }
    }
    override val groupDetailsButton: String = "Start"

    override val expectedReviewDate: (LocalDate?) -> String =
        { "Expected Review: ${it ?: "-"}" }
    override val lastReviewDate: (LocalDateTime?) -> String = {
        "Last Review: ${it?.date ?: "-"}"
    }
    override val repetitions: (Int) -> String = { "Repetitions: $it" }
    override val lapses: (Int) -> String = { "Lapses: $it" }

    override val dialogCommon: LetterDeckDetailDialogCommonStrings =
        EnglishLetterDeckDetailDialogCommonStrings
    override val filterDialog: FilterDialogStrings = EnglishFilterDialogStrings
    override val sortDialog: SortDialogStrings = EnglishSortDialogStrings
    override val layoutDialog: PracticePreviewLayoutDialogStrings =
        EnglishPracticePreviewLayoutDialogStrings

    override val multiselectTitle: (selectedCount: Int) -> String = { "$it Selected" }
    override val multiselectDataNotLoaded: String = "Loading, wait a moment…"
    override val multiselectNoSelected: String = "Select at least one group"

    override val filterAllLabel: String = "All"
    override val filterNoneLabel: String = "None"
    override val kanaGroupsModeActivatedLabel: String = "Kana Groups Mode"
    override val shareLetterDeckClipboardMessage: String =
        "Letters from deck were copied to the clipboard"

}

object EnglishLetterDeckDetailDialogCommonStrings : LetterDeckDetailDialogCommonStrings {
    override val buttonCancel: String = "Cancel"
    override val buttonApply: String = "Apply"
}

object EnglishFilterDialogStrings : FilterDialogStrings {
    override val title: String = "Filter"
}

object EnglishSortDialogStrings : SortDialogStrings {
    override val title: String = "Sort"
    override val sortOptionAddOrder: String = "Add order"
    override val sortOptionAddOrderHint: String = "↑ New items last\n↓ New items first"
    override val sortOptionFrequency: String = "Frequency"
    override val sortOptionFrequencyHint: String =
        "Occurrence frequency of a character in newspapers\n↑ Frequent first\n↓ Frequent last"
    override val sortOptionName: String = "Name"
    override val sortOptionNameHint: String = "↑ Smaller first\n↓ Smaller last"
    override val sortOptionReviewTime: String = "Expected Review"
    override val sortOptionReviewTimeHint: String =
        "↑ Never reviewed first\n↓ Furthest scheduled first"
}

object EnglishPracticePreviewLayoutDialogStrings : PracticePreviewLayoutDialogStrings {
    override val title: String = "Layout"
    override val singleCharacterOptionLabel: String = "Single Letter"
    override val groupsOptionLabel: String = "Groups"
    override val kanaGroupsTitle: String = "Kana Groups"
    override val kanaGroupsSubtitle: String =
        "Make group sizes according to kana table if practice contains all kana characters"
}

object EnglishCommonPracticeStrings : CommonPracticeStrings {
    override val configurationTitle: String = "Configuration"
    override val configurationSelectedItemsLabel: String = "Selected:"
    override val configurationCharactersPreview: String = "Letters preview"
    override val shuffleConfigurationTitle: String = "Shuffle"
    override val shuffleConfigurationMessage: String = "Randomizes review order"
    override val configurationCompleteButton: String = "Start"

    override val additionalKanaReadingsNote: (List<String>) -> String = {
        "Note: can also be written as ${it.joinToString()}"
    }

    override val formattedSrsInterval: (Duration) -> String = { formattedSrsDuration(it) }
    override val flashcardRevealButton: String = "Show Answer"
    override val againButton: String = "Again"
    override val hardButton: String = "Hard"
    override val goodButton: String = "Good"
    override val easyButton: String = "Easy"

    override val summaryTimeSpentValue: (Duration) -> String = { formatDuration(it) }

    override val earlyFinishDialogTitle: String = "Finish practice?"
    override val earlyFinishDialogMessage: String =
        "Navigate to the summary, your current progress is already saved"
    override val earlyFinishDialogCancelButton: String = "Cancel"
    override val earlyFinishDialogAcceptButton: String = "Finish"

    override val writingStrokeCorrect: String = "Correct"
    override val writingStrokeAlmost: String = "Almost"
    override val writingStrokeIncorrect: String = "Try again"

    override val sequenceIssueWrongOrder: String = "Wrong stroke order"
    override val sequenceIssueMissingStroke: String = "Missing stroke"
    override val sequenceIssueExtraStroke: String = "Extra stroke"

    override val writingStrokeAccuracyTitle: String = "Stroke Accuracy"
    override val writingStrokeAccuracy: (Int) -> String = { "Stroke accuracy: $it%" }
    override val writingWrongOrder: (Int) -> String = { "Wrong order: $it" }

    override val reviewSessionTitle: String = "Session"
    override val reviewDeckLabel: String = "Deck"
    override val reviewStreakLabel: String = "Streak"
    override val reviewProgressPosition: (Int, Int) -> String = { current, total -> "Card $current of $total" }
    override val reviewStreakDays: (Int) -> String = { "$it day${if (it == 1) "" else "s"}" }
}

object EnglishLetterPracticeStrings : LetterPracticeStrings {
    override val configurationTitle: (practiceType: String) -> String = { "Letter Practice・$it" }
    override val hintStrokesTitle: String = "Hint Strokes"
    override val hintStrokesMessage: String = "Controls when to show hint strokes for letters"
    override val hintStrokeNewOnlyMode: String = "New only"
    override val hintStrokeAllMode: String = "For all"
    override val hintStrokeNoneMode: String = "Never"
    override val inputModeTitle: String = "Input Mode"
    override val inputModeMessage: String =
        "Choose whether to validate each stroke or the entire letter"
    override val inputModeStroke: String = "Stroke"
    override val inputModeCharacter: String = "Letter"
    override val evaluationStrictnessTitle: String = "Stroke Strictness"
    override val evaluationStrictnessMessage: String =
        "Tighter tolerances for stroke recognition — Exam is the strictest"
    override val evaluationStrictnessNormal: String = "Normal"
    override val evaluationStrictnessHard: String = "Hard"
    override val evaluationStrictnessExam: String = "Exam"
    override val kanaRomajiTitle: String = "Show romaji in kana practice"
    override val kanaRomajiMessage: String =
        "When reviewing kana show romaji expressions instead of kana"
    override val noTranslationLayoutTitle: String = "No translation layout"
    override val noTranslationLayoutMessage: String =
        "Hides letter translations during writing practice"
    override val leftHandedModeTitle: String = "Left-handed mode"
    override val leftHandedModeMessage: String =
        "Adjusts position of input in landscape mode of writing practice screen"

    override val headerWordsMessage: (count: Int) -> String = {
        "Examples ($it)"
    }
    override val studyFinishedButton: String = "Review"
    override val noKanjiTranslationsLabel: String = "[No translations]"

    override val altStrokeEvaluatorTitle: String = "Strict Stroke Evaluator"
    override val altStrokeEvaluatorMessage: String = "Alternative algorithm for stroke evaluation"

    override val variantsTitle: String = "Variants: "
    override val variantsHint: String = "Click to reveal"
    override val unicodeTitle: (String) -> String = { "Unicode: $it" }
    override val strokeCountTitle: (count: Int) -> String = { "Stroke count: $it" }
}

object EnglishVocabPracticeStrings : VocabPracticeStrings {
    override val configurationTitle: (practiceType: String) -> String = {
        "Vocab Practice・$it"
    }
    override val readingMeaningConfigurationTitle: String = "Always Show Meanings"
    override val readingMeaningConfigurationMessage: String =
        "Choose meaning visibility when answer is not selected"
    override val translationInFrontConfigurationTitle: String = "Translation In Front"
    override val translationInFrontConfigurationMessage: String =
        "Show translation instead of word when flashcard is hidden"
    override val writingKanaReadingConfigurationTitle: String = "Show Kana Readings"
    override val writingKanaReadingConfigurationMessage: String =
        "Show kana readings even before answer is entered"
    override val detailsButton: String = "Details"
}

fun formattedSrsDuration(
    duration: Duration,
    dayLabel: String = "d",
    hourLabel: String = "h",
    minuteLabel: String = "m",
    secondLabel: String = "s",
    separator: String = " "
): String = when {
    duration.inWholeDays > 0 -> buildString {
        append("${duration.inWholeDays}$dayLabel")
        appendIfNot0(duration.inWholeHours % 24) { "$separator${it}$hourLabel" }
    }

    duration.inWholeHours > 0 -> buildString {
        append("${duration.inWholeHours}$hourLabel")
        appendIfNot0(duration.inWholeMinutes % 60) { "$separator${it}$minuteLabel" }
    }

    duration.inWholeMinutes > 0 -> buildString {
        append("${duration.inWholeMinutes}$minuteLabel")
        appendIfNot0(duration.inWholeSeconds % 60) { "$separator${it}$secondLabel" }
    }

    else -> "${duration.inWholeSeconds}$secondLabel"
}

private fun StringBuilder.appendIfNot0(number: Long, text: (Long) -> String) {
    if (number != 0L) append(text(number))
}

object EnglishInfoScreenStrings : InfoScreenStrings {
    override val strokesMessage: (count: Int) -> AnnotatedString = {
        buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(it.toString()) }
            if (it == 1) append(" stroke")
            else append(" strokes")
        }
    }
    override val clipboardCopyMessage: String = "Copied"
    override val radicalsSectionTitle: (count: Int) -> String = { "Radicals ($it)" }
    override val noRadicalsMessage: String = "No radicals"
    override val wordsSectionTitle: (count: Int) -> String = { "Expressions ($it)" }
    override val romajiMessage: (romaji: List<String>) -> String = {
        "Romaji readings: ${it.joinToString()}"
    }
    override val gradeMessage: (grade: Int) -> String = {
        when {
            it <= 6 -> "Jōyō kanji, taught in $it grade"
            it == 8 -> "Jōyō kanji, taught in junior high"
            it >= 9 -> "Jinmeiyō kanji, used in names"
            else -> throw IllegalStateException("Unknown grade $it")
        }
    }
    override val jlptMessage: (level: Int) -> String = { "JLPT level $it" }
    override val frequencyMessage: (frequency: Int) -> String = {
        "$it of 2500 most used kanji in newspapers"
    }

}

object EnglishReminderNotificationStrings : ReminderNotificationStrings {
    override val channelName: String = "Reminder Notifications"
    override val title: String = "It's review time!"
    override val noDetailsMessage: String = "Continue to learn Japanese now"
    override val newOnlyMessage: (Int) -> String = {
        "$it new cards to study today"
    }
    override val dueOnlyMessage: (Int) -> String = {
        "$it due cards to review today"
    }
    override val message: (Int, Int) -> String = { new, due ->
        "$new new cards and $due due cards to review today"
    }
}


object EnglishSyncScreenStrings : SyncScreenStrings {
    override val title = "Sync (Preview)"
    override val guideTitle: String = "Sync Your Progress Across Devices"
    override val guideMessage =
        "Automatically upload your data to the cloud, keep it as a backup and stay in sync across all your devices"
    override val guideStepAccountTitle = "Create account and sign in"
    override val guideStepAccountMessage: String = "Go to account"
    override val guideStepSubscriptionTitle =
        "Might require a paid subscription in future"
    override val guideStepSubscriptionMessage: String =
        "Free during preview until further notice, follow announcements on our Discord server for updates"
    override val accountErrorMessage: String = "There's an error with your account"
    override val syncButton = "Sync now"
    override val statusTitle = "Status"
    override val statusMessageLoading = "Checking server for updates..."
    override val statusMessageDataDiffer = "Local and remote data differs"
    override val statusMessageLocalNewer = "Can upload updated data"
    override val statusMessageUpToDate = "Up to date with the server"
    override val statusMessageError = "Error"
    override val statusMessageUploading = "Uploading"
    override val statusMessageDownloading = "Downloading"
    override val statusMessageCanceled = "Canceled, click on sync button to restart"
    override val localDataTitle = "Local Data"
    override val localDataIdTemplate = "ID: %s"
    override val localDataTimestampTemplate = "Timestamp: %s"

    override val errorNoConnectionTitle = "No Connection"
    override val errorNoConnectionMessage = "Couldn't access the server"
    override val errorSessionExpiredTitle = "Session Expired"
    override val errorSessionExpiredMessage = "Click to sign in again"
    override val errorNoSubscriptionTitle = "Subscription status outdated"
    override val errorNoSubscriptionMessage = "Update your subscription status on account screen"
    override val errorOtherTitle = "Error"
    override val errorOtherMessageFallback = "Unknown error"
}

object EnglishSyncDialogStrings : SyncDialogStrings {
    override val title = "Sync"
    override val buttonCancel = "Cancel"
    override val buttonUpload = "Upload"
    override val buttonDownload = "Download"
    override val buttonAccount = "Account"
    override val uploadingMessage = "Uploading..."
    override val downloadingMessage = "Downloading..."
    override val conflictRemoteNewerTitle = "New Data Found"
    override val conflictRemoteNewerMessage = "Data on the server is newer than your local copy"
    override val conflictIncompatibleTitle = "Data Conflict"
    override val conflictIncompatibleMessage =
        "Both remote and local data were changed since the last sync, result can't be merged"
    override val errorNoNetworkTitle = "No Network"
    override val errorNoNetworkMessage = "Couldn't establish network connection"
    override val errorNoSubscriptionTitle = "Subscription Expired"
    override val errorNoSubscriptionMessage =
        "Your subscription has expired, sync will be disabled, refresh your subscription status on the Account screen"
    override val errorNotAuthenticatedTitle = "Session Expired"
    override val errorNotAuthenticatedMessage = "Sign in to your account again to continue"
    override val errorUnexpectedErrorTitle = "Unexpected Error"
    override val errorUnexpectedErrorMessage = "Unknown issue"
    override val errorUnsupportedDataTitle = "Data on the server is unsupported"
    override val errorUnsupportedDataMessage =
        "The data on the server was created using the newer version of the application and is not compatible with the currently installed version. Update the app to retrieve your data or upload your local data to the server"
}

object EnglishSyncSnackbarStrings : SyncSnackbarStrings {
    override val errorNoConnection = "No Connection"
    override val errorNoSubscription = "Subscription expired"
    override val errorNotAuthenticated = "Sign in data expired"
    override val errorDataNotSupported = "Remote data unsupported"
    override val errorMessageTemplate = "Sync Error: %s"
    override val errorMessageNoReason = "Sync Error"
    override val actionButton = "Details"
}