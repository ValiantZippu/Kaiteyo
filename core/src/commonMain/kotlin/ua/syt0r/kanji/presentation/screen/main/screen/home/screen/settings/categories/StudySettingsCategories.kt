package ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.categories

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.intl.Locale
import ua.syt0r.kanji.core.user_data.preferences.PreferencesContract
import ua.syt0r.kanji.core.user_data.preferences.PreferencesLetterPracticeType
import ua.syt0r.kanji.core.user_data.preferences.PreferencesLetterPracticeWritingInputMode
import ua.syt0r.kanji.core.user_data.preferences.PreferencesNewCardsOrder
import ua.syt0r.kanji.core.user_data.preferences.PreferencesVocabPracticeType
import ua.syt0r.kanji.core.user_data.preferences.PreferencesWritingStrictness
import ua.syt0r.kanji.presentation.common.resources.string.getStrings
import ua.syt0r.kanji.presentation.screen.main.MainDestination
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.LocalSettingsNavigation
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.SettingDescriptor
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.SettingsScreenContract
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.components.LinkSetting
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.components.SegmentedSetting
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.components.SettingGroup
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.components.ToggleSetting
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.rememberSetting

// ============================================
// STUDY
// ============================================

class StudySettingsCategory(
    private val appPreferences: PreferencesContract.AppPreferences,
    private val practicePreferences: PreferencesContract.PracticePreferences
) : SettingsScreenContract.Category {

    private val strings = getStrings()
    private val s = strings.center
    private val letterPracticeStrings = strings.letterPractice
    private val topStrings = strings

    override val id: String = "study"
    override val title: String = s.categoryStudy
    override val subtitle: String = s.categoryStudySubtitle
    override val keywords: List<String> = listOf(
        "session", "shuffle", "order", "practice", "dashboard", "limit", "review",
        "new", "radicals", "audio", "autoplay", "profile", "level", "beginner",
        "advanced", "adaptation", "furigana", "romaji"
    )
    override val icon: ImageVector? = Icons.Default.School

    override val reset: (suspend () -> Unit)? = {
        practicePreferences.shuffle.set(true)
        practicePreferences.newCardsOrder.set(PreferencesNewCardsOrder.First)
        practicePreferences.noTranslationLayout.set(Locale.current.language == "ja")
        practicePreferences.leftHandMode.set(false)
        practicePreferences.highlightRadicals.set(true)
        practicePreferences.kanaAutoPlay.set(true)
        appPreferences.letterDashboardPracticeType.set(PreferencesLetterPracticeType.Writing)
        appPreferences.vocabDashboardPracticeType.set(PreferencesVocabPracticeType.Flashcard)
    }

    private fun newCardsLabel(order: PreferencesNewCardsOrder): String = when (order) {
        PreferencesNewCardsOrder.First -> s.newCardsFirst
        PreferencesNewCardsOrder.Last -> s.newCardsLast
        PreferencesNewCardsOrder.Mixed -> s.newCardsMixed
    }

    private fun letterPracticeLabel(type: PreferencesLetterPracticeType): String = when (type) {
        PreferencesLetterPracticeType.Writing -> topStrings.letterPracticeTypeWriting
        PreferencesLetterPracticeType.Reading -> topStrings.letterPracticeTypeReading
    }

    private fun vocabPracticeLabel(type: PreferencesVocabPracticeType): String = when (type) {
        PreferencesVocabPracticeType.Flashcard -> topStrings.vocabPracticeTypeFlashcard
        PreferencesVocabPracticeType.ReadingPicker -> topStrings.vocabPracticeTypeReadingPicker
        PreferencesVocabPracticeType.Writing -> topStrings.vocabPracticeTypeWriting
    }

    override val descriptors: List<SettingDescriptor> = listOf(
        SettingDescriptor(
            id = "study_shuffle",
            title = s.shuffle,
            description = s.shuffleDescription,
            keywords = listOf("shuffle", "random", "order", "session"),
            render = { ShuffleSetting() }
        ),
        SettingDescriptor(
            id = "study_new_cards_order",
            title = s.newCardsOrder,
            description = s.newCardsOrderDescription,
            keywords = listOf("new", "cards", "order", "first", "last"),
            render = { NewCardsOrderSetting() }
        ),
        SettingDescriptor(
            id = "study_no_translation",
            title = letterPracticeStrings.noTranslationLayoutTitle,
            description = letterPracticeStrings.noTranslationLayoutMessage,
            keywords = listOf("translation", "layout", "hide", "writing"),
            render = { NoTranslationSetting() }
        ),
        SettingDescriptor(
            id = "study_left_hand",
            title = letterPracticeStrings.leftHandedModeTitle,
            description = letterPracticeStrings.leftHandedModeMessage,
            keywords = listOf("left", "hand", "landscape", "input"),
            render = { LeftHandSetting() }
        ),
        SettingDescriptor(
            id = "study_radicals",
            title = s.highlightRadicals,
            description = s.highlightRadicalsDescription,
            keywords = listOf("radical", "highlight", "kanji", "breakdown"),
            render = { HighlightRadicalsSetting() }
        ),
        SettingDescriptor(
            id = "study_audio",
            title = s.kanaAutoPlay,
            description = s.kanaAutoPlayDescription,
            keywords = listOf("audio", "sound", "autoplay", "kana", "tts"),
            render = { KanaAutoPlaySetting() }
        ),
        SettingDescriptor(
            id = "study_letter_practice_type",
            title = s.letterPracticeType,
            description = s.letterPracticeTypeDescription,
            keywords = listOf("letters", "practice", "mode", "dashboard", "writing", "reading"),
            render = { LetterPracticeTypeSetting() }
        ),
        SettingDescriptor(
            id = "study_vocab_practice_type",
            title = s.vocabPracticeType,
            description = s.vocabPracticeTypeDescription,
            keywords = listOf("vocab", "practice", "mode", "dashboard", "flashcard"),
            render = { VocabPracticeTypeSetting() }
        ),
        SettingDescriptor(
            id = "study_learner_profile",
            title = s.learnerProfile,
            description = s.learnerProfileDescription,
            keywords = listOf("profile", "level", "beginner", "advanced", "adaptation", "furigana", "romaji"),
            render = { LearnerProfileLink() }
        ),
        SettingDescriptor(
            id = "study_daily_limit",
            title = s.dailyLimit,
            description = s.dailyLimitDescription,
            keywords = listOf("limit", "new", "review", "daily", "cap"),
            render = { DailyLimitLink() }
        )
    )

    @Composable
    override fun content(mainNavigationState: MainNavigationState) {
        SettingGroup(
            title = s.groupStudy,
            children = listOf(
                { ShuffleSetting() },
                { NewCardsOrderSetting() },
                { NoTranslationSetting() },
                { LeftHandSetting() },
                { HighlightRadicalsSetting() },
                { KanaAutoPlaySetting() }
            )
        )
        SettingGroup(
            title = s.groupStartup,
            children = listOf(
                { LetterPracticeTypeSetting() },
                { VocabPracticeTypeSetting() }
            )
        )
        SettingGroup(
            title = s.groupRelated,
            children = listOf(
                { LearnerProfileLink() },
                { DailyLimitLink() }
            )
        )
    }

    @Composable
    private fun LearnerProfileLink() {
        val navigationState = LocalSettingsNavigation.current ?: return
        LinkSetting(
            title = s.learnerProfile,
            description = s.learnerProfileDescription,
            onClick = { navigationState.navigate(MainDestination.LearnerProfile) }
        )
    }

    @Composable
    private fun ShuffleSetting() {
        val binding = rememberSetting(practicePreferences.shuffle)
        ToggleSetting(
            title = s.shuffle,
            description = s.shuffleDescription,
            checked = binding.value,
            onChanged = { binding.set(it) }
        )
    }

    @Composable
    private fun NewCardsOrderSetting() {
        val binding = rememberSetting(practicePreferences.newCardsOrder)
        SegmentedSetting(
            title = s.newCardsOrder,
            description = s.newCardsOrderDescription,
            options = PreferencesNewCardsOrder.entries,
            labelOf = ::newCardsLabel,
            selected = binding.value,
            onSelected = { binding.set(it) }
        )
    }

    @Composable
    private fun NoTranslationSetting() {
        val binding = rememberSetting(practicePreferences.noTranslationLayout)
        ToggleSetting(
            title = letterPracticeStrings.noTranslationLayoutTitle,
            description = letterPracticeStrings.noTranslationLayoutMessage,
            checked = binding.value,
            onChanged = { binding.set(it) }
        )
    }

    @Composable
    private fun LeftHandSetting() {
        val binding = rememberSetting(practicePreferences.leftHandMode)
        ToggleSetting(
            title = letterPracticeStrings.leftHandedModeTitle,
            description = letterPracticeStrings.leftHandedModeMessage,
            checked = binding.value,
            onChanged = { binding.set(it) }
        )
    }

    @Composable
    private fun HighlightRadicalsSetting() {
        val binding = rememberSetting(practicePreferences.highlightRadicals)
        ToggleSetting(
            title = s.highlightRadicals,
            description = s.highlightRadicalsDescription,
            checked = binding.value,
            onChanged = { binding.set(it) }
        )
    }

    @Composable
    private fun KanaAutoPlaySetting() {
        val binding = rememberSetting(practicePreferences.kanaAutoPlay)
        ToggleSetting(
            title = s.kanaAutoPlay,
            description = s.kanaAutoPlayDescription,
            checked = binding.value,
            onChanged = { binding.set(it) }
        )
    }

    @Composable
    private fun LetterPracticeTypeSetting() {
        val binding = rememberSetting(appPreferences.letterDashboardPracticeType)
        SegmentedSetting(
            title = s.letterPracticeType,
            description = s.letterPracticeTypeDescription,
            options = PreferencesLetterPracticeType.entries,
            labelOf = ::letterPracticeLabel,
            selected = binding.value,
            onSelected = { binding.set(it) }
        )
    }

    @Composable
    private fun VocabPracticeTypeSetting() {
        val binding = rememberSetting(appPreferences.vocabDashboardPracticeType)
        SegmentedSetting(
            title = s.vocabPracticeType,
            description = s.vocabPracticeTypeDescription,
            options = PreferencesVocabPracticeType.entries,
            labelOf = ::vocabPracticeLabel,
            selected = binding.value,
            onSelected = { binding.set(it) }
        )
    }

    @Composable
    private fun DailyLimitLink() {
        val navigationState = LocalSettingsNavigation.current ?: return
        LinkSetting(
            title = s.dailyLimit,
            description = s.dailyLimitDescription,
            onClick = { navigationState.navigate(MainDestination.DailyLimit) }
        )
    }

}

// ============================================
// WRITING
// ============================================

class WritingSettingsCategory(
    private val practicePreferences: PreferencesContract.PracticePreferences
) : SettingsScreenContract.Category {

    private val strings = getStrings()
    private val s = strings.center
    private val letterPracticeStrings = strings.letterPractice

    override val id: String = "writing"
    override val title: String = s.categoryWriting
    override val subtitle: String = s.categoryWritingSubtitle
    override val keywords: List<String> = listOf(
        "stroke", "evaluation", "strictness", "input", "mode", "romaji", "kanji", "writing"
    )
    override val icon: ImageVector? = Icons.Default.Edit

    override val reset: (suspend () -> Unit)? = {
        practicePreferences.writingInputMethod.set(PreferencesLetterPracticeWritingInputMode.Stroke)
        practicePreferences.letterWritingStrictness.set(PreferencesWritingStrictness.Normal)
        practicePreferences.vocabWritingStrictness.set(PreferencesWritingStrictness.Normal)
        practicePreferences.altStrokeEvaluator.set(false)
        practicePreferences.writingRomajiInsteadOfKanaWords.set(true)
    }

    private fun strictnessLabel(strictness: PreferencesWritingStrictness): String = when (strictness) {
        PreferencesWritingStrictness.Normal -> letterPracticeStrings.evaluationStrictnessNormal
        PreferencesWritingStrictness.Hard -> letterPracticeStrings.evaluationStrictnessHard
        PreferencesWritingStrictness.Exam -> letterPracticeStrings.evaluationStrictnessExam
    }

    private fun inputModeLabel(mode: PreferencesLetterPracticeWritingInputMode): String = when (mode) {
        PreferencesLetterPracticeWritingInputMode.Stroke -> letterPracticeStrings.inputModeStroke
        PreferencesLetterPracticeWritingInputMode.Character -> letterPracticeStrings.inputModeCharacter
    }

    override val descriptors: List<SettingDescriptor> = listOf(
        SettingDescriptor(
            id = "writing_input_method",
            title = letterPracticeStrings.inputModeTitle,
            description = letterPracticeStrings.inputModeMessage,
            keywords = listOf("input", "mode", "stroke", "character", "letter"),
            render = { InputMethodSetting() }
        ),
        SettingDescriptor(
            id = "writing_letter_strictness",
            title = letterPracticeStrings.evaluationStrictnessTitle,
            description = letterPracticeStrings.evaluationStrictnessMessage,
            keywords = listOf("strictness", "stroke", "tolerance", "exam", "hard"),
            render = { LetterStrictnessSetting() }
        ),
        SettingDescriptor(
            id = "writing_vocab_strictness",
            title = s.vocabStrictness,
            description = s.vocabStrictnessDescription,
            keywords = listOf("strictness", "stroke", "vocab", "tolerance"),
            render = { VocabStrictnessSetting() }
        ),
        SettingDescriptor(
            id = "writing_strict_evaluator",
            title = letterPracticeStrings.altStrokeEvaluatorTitle,
            description = letterPracticeStrings.altStrokeEvaluatorMessage,
            keywords = listOf("evaluator", "algorithm", "stroke", "strict"),
            render = { StrictEvaluatorSetting() }
        ),
        SettingDescriptor(
            id = "writing_romaji",
            title = letterPracticeStrings.kanaRomajiTitle,
            description = letterPracticeStrings.kanaRomajiMessage,
            keywords = listOf("romaji", "kana", "writing", "letters"),
            render = { RomajiSetting() }
        )
    )

    @Composable
    override fun content(mainNavigationState: MainNavigationState) {
        SettingGroup(
            title = s.groupInput,
            children = listOf(
                { InputMethodSetting() },
                { RomajiSetting() }
            )
        )
        SettingGroup(
            title = s.groupStroke,
            children = listOf(
                { LetterStrictnessSetting() },
                { VocabStrictnessSetting() },
                { StrictEvaluatorSetting() }
            )
        )
    }

    @Composable
    private fun InputMethodSetting() {
        val binding = rememberSetting(practicePreferences.writingInputMethod)
        SegmentedSetting(
            title = letterPracticeStrings.inputModeTitle,
            description = letterPracticeStrings.inputModeMessage,
            options = PreferencesLetterPracticeWritingInputMode.entries,
            labelOf = ::inputModeLabel,
            selected = binding.value,
            onSelected = { binding.set(it) }
        )
    }

    @Composable
    private fun LetterStrictnessSetting() {
        val binding = rememberSetting(practicePreferences.letterWritingStrictness)
        SegmentedSetting(
            title = letterPracticeStrings.evaluationStrictnessTitle,
            description = letterPracticeStrings.evaluationStrictnessMessage,
            options = PreferencesWritingStrictness.entries,
            labelOf = ::strictnessLabel,
            selected = binding.value,
            onSelected = { binding.set(it) }
        )
    }

    @Composable
    private fun VocabStrictnessSetting() {
        val binding = rememberSetting(practicePreferences.vocabWritingStrictness)
        SegmentedSetting(
            title = s.vocabStrictness,
            description = s.vocabStrictnessDescription,
            options = PreferencesWritingStrictness.entries,
            labelOf = ::strictnessLabel,
            selected = binding.value,
            onSelected = { binding.set(it) }
        )
    }

    @Composable
    private fun StrictEvaluatorSetting() {
        val binding = rememberSetting(practicePreferences.altStrokeEvaluator)
        ToggleSetting(
            title = letterPracticeStrings.altStrokeEvaluatorTitle,
            description = letterPracticeStrings.altStrokeEvaluatorMessage,
            checked = binding.value,
            onChanged = { binding.set(it) }
        )
    }

    @Composable
    private fun RomajiSetting() {
        val binding = rememberSetting(practicePreferences.writingRomajiInsteadOfKanaWords)
        ToggleSetting(
            title = letterPracticeStrings.kanaRomajiTitle,
            description = letterPracticeStrings.kanaRomajiMessage,
            checked = binding.value,
            onChanged = { binding.set(it) }
        )
    }

}

// ============================================
// FLASHCARDS
// ============================================

class FlashcardSettingsCategory(
    private val practicePreferences: PreferencesContract.PracticePreferences
) : SettingsScreenContract.Category {

    private val strings = getStrings()
    private val s = strings.center
    private val vocabPracticeStrings = strings.vocabPractice

    override val id: String = "flashcards"
    override val title: String = s.categoryFlashcards
    override val subtitle: String = s.categoryFlashcardsSubtitle
    override val keywords: List<String> = listOf(
        "flashcard", "card", "front", "answer", "reading", "kana", "meaning"
    )
    override val icon: ImageVector? = Icons.Default.Star

    override val reset: (suspend () -> Unit)? = {
        practicePreferences.vocabFlashcardMeaningInFront.set(false)
        practicePreferences.vocabReadingPickerShowMeaning.set(true)
        practicePreferences.vocabWritingShowKanaReading.set(false)
        practicePreferences.readingRomajiFuriganaForKanaWords.set(true)
    }

    override val descriptors: List<SettingDescriptor> = listOf(
        SettingDescriptor(
            id = "flashcard_translation_front",
            title = vocabPracticeStrings.translationInFrontConfigurationTitle,
            description = vocabPracticeStrings.translationInFrontConfigurationMessage,
            keywords = listOf("translation", "front", "flashcard", "meaning"),
            render = { TranslationInFrontSetting() }
        ),
        SettingDescriptor(
            id = "flashcard_show_meanings",
            title = vocabPracticeStrings.readingMeaningConfigurationTitle,
            description = vocabPracticeStrings.readingMeaningConfigurationMessage,
            keywords = listOf("meaning", "reading", "show", "picker"),
            render = { ShowMeaningsSetting() }
        ),
        SettingDescriptor(
            id = "flashcard_show_kana",
            title = vocabPracticeStrings.writingKanaReadingConfigurationTitle,
            description = vocabPracticeStrings.writingKanaReadingConfigurationMessage,
            keywords = listOf("kana", "reading", "show", "writing"),
            render = { ShowKanaSetting() }
        ),
        SettingDescriptor(
            id = "flashcard_romaji_furigana",
            title = s.romajiFurigana,
            description = s.romajiFuriganaDescription,
            keywords = listOf("romaji", "furigana", "reading", "kana"),
            render = { RomajiFuriganaSetting() }
        )
    )

    @Composable
    override fun content(mainNavigationState: MainNavigationState) {
        SettingGroup(
            title = s.groupFlashcard,
            children = listOf(
                { TranslationInFrontSetting() },
                { ShowMeaningsSetting() },
                { ShowKanaSetting() },
                { RomajiFuriganaSetting() }
            )
        )
    }

    @Composable
    private fun TranslationInFrontSetting() {
        val binding = rememberSetting(practicePreferences.vocabFlashcardMeaningInFront)
        ToggleSetting(
            title = vocabPracticeStrings.translationInFrontConfigurationTitle,
            description = vocabPracticeStrings.translationInFrontConfigurationMessage,
            checked = binding.value,
            onChanged = { binding.set(it) }
        )
    }

    @Composable
    private fun ShowMeaningsSetting() {
        val binding = rememberSetting(practicePreferences.vocabReadingPickerShowMeaning)
        ToggleSetting(
            title = vocabPracticeStrings.readingMeaningConfigurationTitle,
            description = vocabPracticeStrings.readingMeaningConfigurationMessage,
            checked = binding.value,
            onChanged = { binding.set(it) }
        )
    }

    @Composable
    private fun ShowKanaSetting() {
        val binding = rememberSetting(practicePreferences.vocabWritingShowKanaReading)
        ToggleSetting(
            title = vocabPracticeStrings.writingKanaReadingConfigurationTitle,
            description = vocabPracticeStrings.writingKanaReadingConfigurationMessage,
            checked = binding.value,
            onChanged = { binding.set(it) }
        )
    }

    @Composable
    private fun RomajiFuriganaSetting() {
        val binding = rememberSetting(practicePreferences.readingRomajiFuriganaForKanaWords)
        ToggleSetting(
            title = s.romajiFurigana,
            description = s.romajiFuriganaDescription,
            checked = binding.value,
            onChanged = { binding.set(it) }
        )
    }

}
