package ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings

import org.koin.core.qualifier.qualifier
import org.koin.dsl.module
import ua.syt0r.kanji.presentation.common.debug.DebugSettingsState
import ua.syt0r.kanji.presentation.common.nav.NavigationSettingsState
import ua.syt0r.kanji.presentation.common.theme.ThemeSettingsState
import ua.syt0r.kanji.presentation.multiplatformViewModel
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.categories.AboutSettingsCategory
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.categories.AccessibilitySettingsCategory
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.categories.AppearanceSettingsCategory
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.categories.DataSettingsCategory
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.categories.DebugSettingsCategory
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.categories.FlashcardSettingsCategory
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.categories.GeneralSettingsCategory
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.categories.NavigationSettingsCategory
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.categories.ShortcutsSettingsCategory
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.categories.StudySettingsCategory
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.categories.WritingSettingsCategory

val defaultSettingItemsQualifier = qualifier("default_setting_items")
val settingItemsQualifier = qualifier("setting_items")

val settingsScreenModule = module {

    single {
        ThemeSettingsState(appPreferences = get())
    }

    single {
        NavigationSettingsState(appPreferences = get())
    }

    single {
        DebugSettingsState(appPreferences = get())
    }

    multiplatformViewModel<SettingsScreenContract.ViewModel> {
        SettingsScreenViewModel(
            coroutineScope = it.component1(),
            defaultCategories = get(defaultSettingItemsQualifier),
            customCategories = get(settingItemsQualifier)
        )
    }

    factory(defaultSettingItemsQualifier) {
        listOf<SettingsScreenContract.Category>(
            GeneralSettingsCategory(appPreferences = get()),
            AppearanceSettingsCategory(
                themeManager = get(),
                themeSettingsState = get(),
                learnerProfileStore = get(),
                displayOverridesStore = get()
            ),
            NavigationSettingsCategory(),
            AccessibilitySettingsCategory(
                themeSettingsState = get()
            ),
            StudySettingsCategory(
                appPreferences = get(),
                practicePreferences = get()
            ),
            WritingSettingsCategory(practicePreferences = get()),
            FlashcardSettingsCategory(practicePreferences = get()),
            DataSettingsCategory(),
            ShortcutsSettingsCategory(),
            AboutSettingsCategory(),
            DebugSettingsCategory(
                debugSettingsState = get(),
                navSettingsState = get()
            )
        )
    }

    factory(settingItemsQualifier) { listOf<SettingsScreenContract.Category>() }

}
