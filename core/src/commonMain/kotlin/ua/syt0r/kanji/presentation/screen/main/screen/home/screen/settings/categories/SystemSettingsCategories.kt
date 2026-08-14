package ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.categories

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Storage
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import ua.syt0r.kanji.presentation.common.resources.string.getStrings
import ua.syt0r.kanji.presentation.screen.main.MainDestination
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState
import ua.syt0r.kanji.presentation.screen.main.screen.feedback.FeedbackTopic
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.LocalSettingsNavigation
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.SettingDescriptor
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.SettingsScreenContract
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.components.LinkSetting
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.components.SettingGroup

// ============================================
// DATA & SYNC
// ============================================

class DataSettingsCategory : SettingsScreenContract.Category {

    private val strings = getStrings()
    private val s = strings.center

    override val id: String = "data"
    override val title: String = s.categoryData
    override val subtitle: String = s.categoryDataSubtitle
    override val keywords: List<String> =
        listOf("backup", "restore", "sync", "account", "import", "export", "data", "cloud")
    override val icon: ImageVector? = Icons.Default.Storage
    override val reset: (suspend () -> Unit)? = null

    override val descriptors: List<SettingDescriptor> = listOf(
        SettingDescriptor(
            id = "data_backup",
            title = s.backupLink,
            description = s.backupLinkDescription,
            keywords = listOf("backup", "restore", "database", "export"),
            render = { BackupLink() }
        ),
        SettingDescriptor(
            id = "data_sync",
            title = s.syncLink,
            description = s.syncLinkDescription,
            keywords = listOf("sync", "cloud", "devices", "upload", "download"),
            render = { SyncLink() }
        ),
        SettingDescriptor(
            id = "data_account",
            title = s.accountLink,
            description = s.accountLinkDescription,
            keywords = listOf("account", "profile", "sign in", "subscription"),
            render = { AccountLink() }
        ),
        SettingDescriptor(
            id = "data_import_export",
            title = s.importExport,
            description = s.importExportDescription,
            keywords = listOf("import", "export", "anki", "transfer", "package"),
            render = { ImportExportLink() }
        )
    )

    @Composable
    override fun content(mainNavigationState: MainNavigationState) {
        SettingGroup(
            title = null,
            children = listOf(
                { BackupLink() },
                { SyncLink() },
                { AccountLink() },
                { ImportExportLink() }
            )
        )
    }

    @Composable
    private fun BackupLink() {
        val navigationState = LocalSettingsNavigation.current ?: return
        LinkSetting(
            title = s.backupLink,
            description = s.backupLinkDescription,
            onClick = { navigationState.navigate(MainDestination.Backup) }
        )
    }

    @Composable
    private fun SyncLink() {
        val navigationState = LocalSettingsNavigation.current ?: return
        LinkSetting(
            title = s.syncLink,
            description = s.syncLinkDescription,
            onClick = { navigationState.navigate(MainDestination.Sync) }
        )
    }

    @Composable
    private fun AccountLink() {
        val navigationState = LocalSettingsNavigation.current ?: return
        LinkSetting(
            title = s.accountLink,
            description = s.accountLinkDescription,
            onClick = { navigationState.navigate(MainDestination.Account()) }
        )
    }

    @Composable
    private fun ImportExportLink() {
        val navigationState = LocalSettingsNavigation.current ?: return
        LinkSetting(
            title = s.importExport,
            description = s.importExportDescription,
            onClick = { navigationState.navigate(MainDestination.ImportExport) }
        )
    }

}

// ============================================
// KEYBOARD & SHORTCUTS
// ============================================

class ShortcutsSettingsCategory : SettingsScreenContract.Category {

    private val strings = getStrings()
    private val s = strings.center

    override val id: String = "shortcuts"
    override val title: String = s.categoryShortcuts
    override val subtitle: String = s.categoryShortcutsSubtitle
    override val keywords: List<String> =
        listOf("shortcut", "keyboard", "key", "hotkey", "binding", "ctrl", "review")
    override val icon: ImageVector? = Icons.Default.Keyboard
    override val reset: (suspend () -> Unit)? = null

    override val descriptors: List<SettingDescriptor> = listOf(
        SettingDescriptor(
            id = "shortcuts_manager",
            title = s.shortcutsLink,
            description = s.shortcutsLinkDescription,
            keywords = listOf("shortcut", "keyboard", "manager", "bindings", "review"),
            render = { ShortcutsLink() }
        )
    )

    @Composable
    override fun content(mainNavigationState: MainNavigationState) {
        SettingGroup(
            title = null,
            children = listOf(
                { ShortcutsLink() }
            )
        )
    }

    @Composable
    private fun ShortcutsLink() {
        val navigationState = LocalSettingsNavigation.current ?: return
        LinkSetting(
            title = s.shortcutsLink,
            description = s.shortcutsLinkDescription,
            onClick = { navigationState.navigate(MainDestination.KeyboardShortcuts) }
        )
    }

}

// ============================================
// ABOUT
// ============================================

class AboutSettingsCategory : SettingsScreenContract.Category {

    private val strings = getStrings()
    private val s = strings.center

    override val id: String = "about"
    override val title: String = s.categoryAbout
    override val subtitle: String = s.categoryAboutSubtitle
    override val keywords: List<String> =
        listOf("about", "version", "credits", "feedback", "info", "license", "open source")
    override val icon: ImageVector? = Icons.Default.Info
    override val reset: (suspend () -> Unit)? = null

    override val descriptors: List<SettingDescriptor> = listOf(
        SettingDescriptor(
            id = "about_app",
            title = s.aboutLink,
            description = s.aboutLinkDescription,
            keywords = listOf("about", "version", "project", "documentation"),
            render = { AboutLink() }
        ),
        SettingDescriptor(
            id = "about_credits",
            title = s.creditsLink,
            description = s.creditsLinkDescription,
            keywords = listOf("credits", "contributors", "libraries", "license"),
            render = { CreditsLink() }
        ),
        SettingDescriptor(
            id = "about_feedback",
            title = s.feedbackLink,
            description = s.feedbackLinkDescription,
            keywords = listOf("feedback", "report", "suggest", "issue"),
            render = { FeedbackLink() }
        )
    )

    @Composable
    override fun content(mainNavigationState: MainNavigationState) {
        SettingGroup(
            title = null,
            children = listOf(
                { AboutLink() },
                { CreditsLink() },
                { FeedbackLink() }
            )
        )
    }

    @Composable
    private fun AboutLink() {
        val navigationState = LocalSettingsNavigation.current ?: return
        LinkSetting(
            title = s.aboutLink,
            description = s.aboutLinkDescription,
            onClick = { navigationState.navigate(MainDestination.About) }
        )
    }

    @Composable
    private fun CreditsLink() {
        val navigationState = LocalSettingsNavigation.current ?: return
        LinkSetting(
            title = s.creditsLink,
            description = s.creditsLinkDescription,
            onClick = { navigationState.navigate(MainDestination.Credits) }
        )
    }

    @Composable
    private fun FeedbackLink() {
        val navigationState = LocalSettingsNavigation.current ?: return
        LinkSetting(
            title = s.feedbackLink,
            description = s.feedbackLinkDescription,
            onClick = { navigationState.navigate(MainDestination.Feedback(FeedbackTopic.General)) }
        )
    }

}
