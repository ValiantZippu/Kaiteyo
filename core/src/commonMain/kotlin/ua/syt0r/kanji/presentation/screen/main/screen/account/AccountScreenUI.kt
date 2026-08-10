package ua.syt0r.kanji.presentation.screen.main.screen.account

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.datetime.format
import org.jetbrains.compose.resources.stringResource
import ua.syt0r.kanji.Res
import ua.syt0r.kanji.account_delete
import ua.syt0r.kanji.account_visit_website
import ua.syt0r.kanji.core.ApiRequestIssue
import ua.syt0r.kanji.core.format
import ua.syt0r.kanji.presentation.common.AppDropdownMenu
import ua.syt0r.kanji.presentation.common.AppDropdownMenuItem
import ua.syt0r.kanji.presentation.common.AppListItem
import ua.syt0r.kanji.presentation.common.InvertedButton
import ua.syt0r.kanji.presentation.common.ScrollableScreenContainer
import ua.syt0r.kanji.presentation.common.clickable
import ua.syt0r.kanji.presentation.common.resources.string.resolveString
import ua.syt0r.kanji.presentation.common.theme.errorColors
import ua.syt0r.kanji.presentation.common.theme.snapToBiggerContainerCrossfadeTransitionSpec
import ua.syt0r.kanji.presentation.screen.main.screen.account.AccountScreenContract.Companion.ACCOUNT_DELETE_URL
import ua.syt0r.kanji.presentation.screen.main.screen.account.AccountScreenContract.Companion.ACCOUNT_WEB_PAGE_URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> AccountScreenContainer(
    state: State<T>,
    onUpClick: () -> Unit,
    content: @Composable (T) -> Unit
) {

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(resolveString { account.title }) },
                navigationIcon = {
                    IconButton(onUpClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { paddingValues ->

        AnimatedContent(
            targetState = state.value,
            transitionSpec = snapToBiggerContainerCrossfadeTransitionSpec(),
            modifier = Modifier.padding(paddingValues)
        ) { screenState ->
            content(screenState)
        }

    }

}

@Composable
fun AccountScreenSignedOut(
    startSignIn: () -> Unit
) {

    ScrollableScreenContainer(
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {

        Text(
            text = resolveString { account.loggedOutMessage },
            modifier = Modifier.weight(1f).fillMaxWidth().wrapContentSize()
        )

        InvertedButton(
            onClick = startSignIn,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        ) {
            Text(text = resolveString { account.signInButton })
        }

    }

}

@Composable
fun AccountScreenLoading() {
    CircularProgressIndicator(Modifier.fillMaxSize().wrapContentSize())
}

@Composable
fun AccountScreenSignedIn(
    email: String,
    issue: ApiRequestIssue?,
    refresh: () -> Unit,
    signOut: () -> Unit,
    signIn: () -> Unit,
    openAccountWeb: (uriHandler: UriHandler) -> Unit = { it.openUri(ACCOUNT_WEB_PAGE_URL) },
    deleteAccount: (uriHandler: UriHandler) -> Unit = { it.openUri(ACCOUNT_DELETE_URL) },
    extraContent: @Composable (ColumnScope.() -> Unit)? = null
) {

    ScrollableScreenContainer(
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {

        if (issue != null) {
            IssueListItem(
                issue = issue,
                signIn = signIn,
                refresh = refresh
            )
        }

        val uriHandler = LocalUriHandler.current

        // ── Profile Section ──
        SectionHeader(title = resolveString { account.profileSection })

        AppListItem(
            leadingContent = { Icon(Icons.Outlined.Email, null) },
            headlineContent = { Text(resolveString { account.emailTitle }) },
            trailingContent = {
                var showDropDown by remember { mutableStateOf(false) }
                AppDropdownMenu(showDropDown, { showDropDown = false }) {
                    AppDropdownMenuItem(
                        onClick = { openAccountWeb(uriHandler) },
                        {
                            Icon(Icons.Outlined.OpenInBrowser, null)
                            Text(stringResource(Res.string.account_visit_website))
                        }
                    )
                    AppDropdownMenuItem(
                        onClick = { deleteAccount(uriHandler) },
                        {
                            CompositionLocalProvider(
                                LocalContentColor provides MaterialTheme.colorScheme.error
                            ) {
                                Icon(Icons.Outlined.Delete, null)
                                Text(stringResource(Res.string.account_delete))
                            }
                        }
                    )
                    AppDropdownMenuItem(
                        onClick = signOut,
                        {
                            Icon(Icons.AutoMirrored.Outlined.Logout, null)
                            Text(resolveString { account.signOutButton })
                        }
                    )
                }

                IconButton(
                    onClick = { showDropDown = true }
                ) {
                    Icon(Icons.Outlined.MoreVert, null)
                }
            },
            supportingContent = { Text(email) }
        )

        // ── Application Section ──
        SectionHeader(title = resolveString { account.applicationSection })

        AppListItem(
            headlineContent = { Text(resolveString { account.syncStatusLabel }) },
            supportingContent = { Text(resolveString { account.syncStatusLabel }) }
        )

        AppListItem(
            headlineContent = { Text(resolveString { account.localStorageUsageLabel }) }
        )

        AppListItem(
            headlineContent = { Text(resolveString { account.databaseInfoLabel }) }
        )

        AppListItem(
            headlineContent = { Text(resolveString { account.cacheLabel }) }
        )

        AppListItem(
            headlineContent = { Text(resolveString { account.backupsLabel }) }
        )

        // ── Study Section ──
        SectionHeader(title = resolveString { account.studySection })

        AppListItem(
            headlineContent = { Text(resolveString { account.currentStreakLabel }) }
        )

        AppListItem(
            headlineContent = { Text(resolveString { account.lifetimeReviewsLabel }) }
        )

        AppListItem(
            headlineContent = { Text(resolveString { account.cardsLearnedLabel }) }
        )

        AppListItem(
            headlineContent = { Text(resolveString { account.totalStudyTimeLabel }) }
        )

        // ── Connected Services Section ──
        SectionHeader(title = resolveString { account.connectedServicesSection })

        AppListItem(
            headlineContent = { Text(resolveString { account.githubServiceLabel }) }
        )

        AppListItem(
            headlineContent = { Text(resolveString { account.syncServiceLabel }) }
        )

        AppListItem(
            headlineContent = { Text(resolveString { account.dictionariesServiceLabel }) }
        )

        AppListItem(
            headlineContent = { Text(resolveString { account.pluginsServiceLabel }) }
        )

        extraContent?.invoke(this)

        Spacer(Modifier.weight(1f))

        InvertedButton(
            onClick = refresh,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
        ) {
            Icon(Icons.Outlined.Refresh, null)
            Text(text = "Refresh")
        }

    }

}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
    )
}

@Composable
fun AccountScreenError(
    issue: ApiRequestIssue,
    startSignIn: () -> Unit
) {

    ScrollableScreenContainer {

        IssueListItem(
            issue = issue,
            signIn = startSignIn,
            refresh = startSignIn
        )

        Spacer(modifier = Modifier.weight(1f))

        InvertedButton(
            onClick = startSignIn,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
        ) {
            Text(resolveString { account.signInButton })
        }

    }

}

@Composable
private fun IssueListItem(
    issue: ApiRequestIssue,
    signIn: () -> Unit,
    refresh: () -> Unit
) {

    val title: String
    val message: String
    val trailingIcon: ImageVector?
    val action: (() -> Unit)?

    when (issue) {
        ApiRequestIssue.NoConnection -> {
            title = resolveString { account.issueNoConnectionTitle }
            message = resolveString { account.issueNoConnectionMessage }
            trailingIcon = null
            action = null
        }

        ApiRequestIssue.NoSubscription -> {
            title = resolveString { account.issueNoSubscriptionTitle }
            message = resolveString { account.issueNoSubscriptionMessage }
            trailingIcon = null
            action = null
        }

        ApiRequestIssue.NotAuthenticated -> {
            title = resolveString { account.issueSessionExpiredTitle }
            message = resolveString { account.issueSessionExpiredMessage }
            trailingIcon = Icons.AutoMirrored.Filled.Login
            action = signIn
        }

        is ApiRequestIssue.Other -> {
            title = resolveString { account.issueOtherTitle }
            message = issue.throwable.message ?: resolveString { account.issueOtherMessageFallback }
            trailingIcon = null
            action = null
        }
    }

    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(message) },
        leadingContent = { Icon(Icons.Default.Error, null) },
        trailingContent = trailingIcon?.let { { Icon(it, null) } },
        colors = ListItemDefaults.errorColors(),
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(action)
    )

}
