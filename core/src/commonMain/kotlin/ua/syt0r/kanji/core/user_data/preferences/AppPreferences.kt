package ua.syt0r.kanji.core.user_data.preferences

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalTime
import ua.syt0r.kanji.core.suspended_property.BooleanSuspendedPropertyType
import ua.syt0r.kanji.core.suspended_property.EnumSuspendedPropertyType.Companion.enumSuspendedPropertyType
import ua.syt0r.kanji.core.suspended_property.InstantSuspendedPropertyType
import ua.syt0r.kanji.core.suspended_property.IntSuspendedPropertyType
import ua.syt0r.kanji.core.suspended_property.LocalTimeSuspendedPropertyType
import ua.syt0r.kanji.core.suspended_property.StringSuspendedPropertyType
import ua.syt0r.kanji.core.suspended_property.SuspendedProperty
import ua.syt0r.kanji.core.suspended_property.SuspendedPropertyCreatorScope
import ua.syt0r.kanji.core.suspended_property.jsonPojoSuspendedPropertyType
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class AppPreferences(
    creatorScope: SuspendedPropertyCreatorScope
) : PreferencesContract.AppPreferences,
    SuspendedPropertyCreatorScope by creatorScope {

    override val refreshToken: SuspendedProperty<String?> = createNullableProperty(
        type = StringSuspendedPropertyType,
        key = "refresh_token",
        initialValue = { null },
        enableBackup = false
    )

    override val idToken: SuspendedProperty<String?> = createNullableProperty(
        type = StringSuspendedPropertyType,
        key = "id_token",
        initialValue = { null },
        enableBackup = false
    )

    override val userInfo: SuspendedProperty<PreferencesUserInfo?> = createNullableProperty(
        type = jsonPojoSuspendedPropertyType(),
        key = "user_info",
        enableBackup = false,
        initialValue = { null }
    )

    override val subscriptionAlert: SuspendedProperty<String?> = createNullableProperty(
        type = StringSuspendedPropertyType,
        key = "subscription_alert",
        enableBackup = false,
        initialValue = { null }
    )

    @OptIn(ExperimentalUuidApi::class)
    override val localDataId: SuspendedProperty<String> = createProperty(
        type = StringSuspendedPropertyType,
        key = "local_data_id",
        initialValue = { Uuid.random().toHexDashString() },
        saveInitialValue = true
    )

    override val localDataTimestamp: SuspendedProperty<Instant?> = createNullableProperty(
        type = InstantSuspendedPropertyType,
        key = "local_data_timestamp",
        initialValue = { null }
    )

    override val lastSyncedDataInfo: SuspendedProperty<PreferencesSyncDataInfo?> =
        createNullableProperty(
            type = jsonPojoSuspendedPropertyType(),
            key = "last_synced_data_info_json",
            initialValue = { null },
            enableBackup = false
        )

    override val analyticsEnabled: SuspendedProperty<Boolean> = createProperty(
        type = BooleanSuspendedPropertyType,
        key = "analytics_enabled",
        initialValue = { true },
        enableBackup = false
    )

    override val practiceType: SuspendedProperty<PreferencesLetterPracticeType> = createProperty(
        type = enumSuspendedPropertyType<PreferencesLetterPracticeType>(),
        key = "practice_type",
        initialValue = { PreferencesLetterPracticeType.Writing }
    )

    override val filterNew: SuspendedProperty<Boolean> = createProperty(
        type = BooleanSuspendedPropertyType,
        key = "filter_new",
        initialValue = { true }
    )

    override val filterDue: SuspendedProperty<Boolean> = createProperty(
        type = BooleanSuspendedPropertyType,
        key = "filter_due",
        initialValue = { true }
    )

    override val filterDone: SuspendedProperty<Boolean> = createProperty(
        type = BooleanSuspendedPropertyType,
        key = "filter_done",
        initialValue = { true }
    )

    override val sortOption: SuspendedProperty<PreferencesLetterSortOption> = createProperty(
        type = enumSuspendedPropertyType<PreferencesLetterSortOption>(),
        key = "sort_option",
        initialValue = { PreferencesLetterSortOption.AddOrder }
    )

    override val isSortDescending: SuspendedProperty<Boolean> = createProperty(
        type = BooleanSuspendedPropertyType,
        key = "is_desc",
        initialValue = { false }
    )

    override val practicePreviewLayout: SuspendedProperty<PreferencesDeckDetailsLetterLayout> =
        createProperty(
            type = enumSuspendedPropertyType<PreferencesDeckDetailsLetterLayout>(),
            key = "practice_preview_layout2",
            initialValue = { PreferencesDeckDetailsLetterLayout.Groups }
        )

    override val kanaGroupsEnabled: SuspendedProperty<Boolean> = createProperty(
        type = BooleanSuspendedPropertyType,
        key = "kana_groups_enabled",
        initialValue = { true }
    )

    override val theme: SuspendedProperty<PreferencesTheme> = createProperty(
        type = enumSuspendedPropertyType<PreferencesTheme>(),
        key = "theme",
        initialValue = { PreferencesTheme.System }
    )

    override val dailyLimitEnabled: SuspendedProperty<Boolean> = createProperty(
        type = BooleanSuspendedPropertyType,
        key = "daily_limit_enabled",
        initialValue = { true },
        affectSync = true
    )

    override val dailyLimitConfigurationJson: SuspendedProperty<String> = createProperty(
        type = StringSuspendedPropertyType,
        key = "daily_limit_configuration",
        initialValue = { "" },
        affectSync = true
    )

    override val reminderEnabled: SuspendedProperty<Boolean> = createProperty(
        type = BooleanSuspendedPropertyType,
        key = "reminder_enabled",
        initialValue = { false },
        enableBackup = false
    )

    override val reminderTime: SuspendedProperty<LocalTime> = createProperty(
        type = LocalTimeSuspendedPropertyType,
        key = "reminder_time",
        initialValue = { LocalTime(hour = 9, minute = 0) },
        affectSync = true
    )

    override val defaultHomeTab: SuspendedProperty<PreferencesDefaultHomeTab> = createProperty(
        type = enumSuspendedPropertyType(),
        key = "default_home_tab",
        affectSync = true,
        initialValue = { PreferencesDefaultHomeTab.GeneralDashboard }
    )

    override val lastAppVersionWhenChangesDialogShown: SuspendedProperty<String> = createProperty(
        type = StringSuspendedPropertyType,
        key = "last_changes_dialog_version_shown",
        initialValue = { "" }
    )

    override val tutorialSeen: SuspendedProperty<Boolean> = createProperty(
        type = BooleanSuspendedPropertyType,
        key = "tutorial_seen",
        initialValue = { false }
    )

    override val onboardingCompleted: SuspendedProperty<Boolean> = createProperty(
        type = BooleanSuspendedPropertyType,
        key = "onboarding_completed",
        initialValue = { false },
        enableBackup = false
    )

    override val generalDashboardStudyTargets: SuspendedProperty<Map<String, Boolean>> =
        createProperty(
            type = jsonPojoSuspendedPropertyType(),
            key = "general_dashboard_study_targets",
            initialValue = { emptyMap() },
            affectSync = true
        )

    override val letterDashboardPracticeType: SuspendedProperty<PreferencesLetterPracticeType> =
        createProperty(
            type = enumSuspendedPropertyType<PreferencesLetterPracticeType>(),
            key = "letter_dashboard_practice_type",
            initialValue = { PreferencesLetterPracticeType.Writing }
        )

    override val letterDashboardSortByTime: SuspendedProperty<Boolean> = createProperty(
        type = BooleanSuspendedPropertyType,
        key = "letter_dashboard_sort_by_time",
        initialValue = { false }
    )

    override val vocabDashboardPracticeType: SuspendedProperty<PreferencesVocabPracticeType> =
        createProperty(
            type = enumSuspendedPropertyType<PreferencesVocabPracticeType>(),
            key = "vocab_dashboard_practice_type",
            initialValue = { PreferencesVocabPracticeType.Flashcard }
        )

    override val vocabDashboardSortByTime: SuspendedProperty<Boolean> = createProperty(
        type = BooleanSuspendedPropertyType,
        key = "vocab_dashboard_sort_by_time",
        initialValue = { false }
    )

    override val vocabNoteTypeId: SuspendedProperty<String> = createProperty(
        type = StringSuspendedPropertyType,
        key = "vocab_note_type_id",
        initialValue = { "kaiteyo-default" }
    )

    override val dailyResetTime: SuspendedProperty<LocalTime> = createProperty(
        type = LocalTimeSuspendedPropertyType,
        key = "daily_reset_time",
        initialValue = { LocalTime(0, 0) },
        affectSync = true
    )

    override val navSidebarMode: SuspendedProperty<String> = createProperty(
        type = StringSuspendedPropertyType,
        key = "nav_sidebar_mode",
        initialValue = { "Expanded" },
        enableBackup = false
    )

    override val navSidebarPosition: SuspendedProperty<String> = createProperty(
        type = StringSuspendedPropertyType,
        key = "nav_sidebar_position",
        initialValue = { "Left" },
        enableBackup = false
    )

    override val navAutoHide: SuspendedProperty<String> = createProperty(
        type = StringSuspendedPropertyType,
        key = "nav_auto_hide",
        initialValue = { "Never" },
        enableBackup = false
    )

    override val navCollapsed: SuspendedProperty<Boolean> = createProperty(
        type = BooleanSuspendedPropertyType,
        key = "nav_collapsed",
        initialValue = { false },
        enableBackup = false
    )

    override val navWidth: SuspendedProperty<Int> = createProperty(
        type = IntSuspendedPropertyType,
        key = "nav_width",
        initialValue = { 260 },
        enableBackup = false
    )

    override val navHeight: SuspendedProperty<Int> = createProperty(
        type = IntSuspendedPropertyType,
        key = "nav_height",
        initialValue = { 56 },
        enableBackup = false
    )

    override val navFloatingOffsetX: SuspendedProperty<Int> = createProperty(
        type = IntSuspendedPropertyType,
        key = "nav_floating_offset_x",
        initialValue = { 0 },
        enableBackup = false
    )

    override val navFloatingOffsetY: SuspendedProperty<Int> = createProperty(
        type = IntSuspendedPropertyType,
        key = "nav_floating_offset_y",
        initialValue = { 0 },
        enableBackup = false
    )

    override val navAccentIndex: SuspendedProperty<Int> = createProperty(
        type = IntSuspendedPropertyType,
        key = "nav_accent_index",
        initialValue = { -1 },
        enableBackup = false
    )

    override val navSettingsJson: SuspendedProperty<String> = createProperty(
        type = StringSuspendedPropertyType,
        key = "nav_settings_json",
        initialValue = { "" },
        enableBackup = false
    )

    override val themeSettingsJson: SuspendedProperty<String> = createProperty(
        type = StringSuspendedPropertyType,
        key = "theme_settings_json",
        initialValue = { "" },
        enableBackup = false
    )

    override val debugSettingsJson: SuspendedProperty<String> = createProperty(
        type = StringSuspendedPropertyType,
        key = "kaiteyo_debug_settings_json",
        initialValue = { "" },
        enableBackup = false
    )

    override val learnerProfileJson: SuspendedProperty<String> = createProperty(
        type = StringSuspendedPropertyType,
        key = "kaiteyo_learner_profile_json",
        initialValue = { "" },
        enableBackup = false
    )

    override val romajiOverrideJson: SuspendedProperty<String> = createProperty(
        type = StringSuspendedPropertyType,
        key = "kaiteyo_romaji_override_json",
        initialValue = { "" },
        enableBackup = false
    )

    override val reviewSettingsJson: SuspendedProperty<String> = createProperty(
        type = StringSuspendedPropertyType,
        key = "kaiteyo_review_settings_json",
        initialValue = { "" }
    )

    override val backupConfigJson: SuspendedProperty<String> = createProperty(
        type = StringSuspendedPropertyType,
        key = "kaiteyo_backup_config_json",
        initialValue = { "" }
    )

    override val savedSearchesJson: SuspendedProperty<String> = createProperty(
        type = StringSuspendedPropertyType,
        key = "kaiteyo_saved_searches_json",
        initialValue = { "" }
    )

    override val homeCommandCenterJson: SuspendedProperty<String> = createProperty(
        type = StringSuspendedPropertyType,
        key = "kaiteyo_home_command_center_json",
        initialValue = { "" },
        enableBackup = false
    )

    override val mediaReferencesJson: SuspendedProperty<String> = createProperty(
        type = StringSuspendedPropertyType,
        key = "kaiteyo_media_references_json",
        initialValue = { "" },
        enableBackup = false
    )

    override val deckFavoritesJson: SuspendedProperty<String> = createProperty(
        type = StringSuspendedPropertyType,
        key = "kaiteyo_deck_favorites_json",
        initialValue = { "" }
    )

    override val browserColumnsJson: SuspendedProperty<String> = createProperty(
        type = StringSuspendedPropertyType,
        key = "kaiteyo_browser_columns_json",
        initialValue = { "" }
    )

    override val shortcutBindingsJson: SuspendedProperty<String> = createProperty(
        type = StringSuspendedPropertyType,
        key = "kaiteyo_shortcut_bindings_json",
        initialValue = { "" }
    )

    override val tagSortOrder: SuspendedProperty<String> = createProperty(
        type = StringSuspendedPropertyType,
        key = "kaiteyo_tag_sort_order",
        initialValue = { "name" }
    )

    override val collectionSortOrder: SuspendedProperty<String> = createProperty(
        type = StringSuspendedPropertyType,
        key = "kaiteyo_collection_sort_order",
        initialValue = { "name" }
    )

    override val browserLastQuery: SuspendedProperty<String> = createProperty(
        type = StringSuspendedPropertyType,
        key = "kaiteyo_browser_last_query",
        initialValue = { "" }
    )

    override val statisticsGoalsJson: SuspendedProperty<String> = createProperty(
        type = StringSuspendedPropertyType,
        key = "kaiteyo_statistics_goals_json",
        initialValue = { "" },
        enableBackup = false
    )

    override val statisticsGoalHistoryJson: SuspendedProperty<String> = createProperty(
        type = StringSuspendedPropertyType,
        key = "kaiteyo_statistics_goal_history_json",
        initialValue = { "" },
        enableBackup = false
    )

    override val kanjiCardLayoutJson: SuspendedProperty<String> = createProperty(
        type = StringSuspendedPropertyType,
        key = "kaiteyo_kanji_card_layout_json",
        initialValue = { "" },
        enableBackup = false
    )

    override val wordCardLayoutJson: SuspendedProperty<String> = createProperty(
        type = StringSuspendedPropertyType,
        key = "kaiteyo_word_card_layout_json",
        initialValue = { "" },
        enableBackup = false
    )

    override val sentenceCardLayoutJson: SuspendedProperty<String> = createProperty(
        type = StringSuspendedPropertyType,
        key = "kaiteyo_sentence_card_layout_json",
        initialValue = { "" },
        enableBackup = false
    )

    override val grammarCardLayoutJson: SuspendedProperty<String> = createProperty(
        type = StringSuspendedPropertyType,
        key = "kaiteyo_grammar_card_layout_json",
        initialValue = { "" },
        enableBackup = false
    )

    override val collectionCardLayoutJson: SuspendedProperty<String> = createProperty(
        type = StringSuspendedPropertyType,
        key = "kaiteyo_collection_card_layout_json",
        initialValue = { "" },
        enableBackup = false
    )

    override val gameProgressJson: SuspendedProperty<String> = createProperty(
        type = StringSuspendedPropertyType,
        key = "kaiteyo_game_progress_json",
        initialValue = { "" },
        enableBackup = false
    )

}
