package ua.syt0r.kanji.presentation.screen.main.screen.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import ua.syt0r.kanji.presentation.common.asActivity
import ua.syt0r.kanji.presentation.common.clickable
import ua.syt0r.kanji.presentation.common.copyCentered
import ua.syt0r.kanji.presentation.common.theme.extraColorScheme
import ua.syt0r.kanji.presentation.getMultiplatformViewModel
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState
import ua.syt0r.kanji.presentation.screen.main.screen.account.GooglePlayAccountScreenContract.ScreenState

object GooglePlayAccountScreenContent : AccountScreenContract.Content {

    @Composable
    override fun invoke(
        state: MainNavigationState,
        data: AccountScreenContract.ScreenData?
    ) {

        val viewModel = getMultiplatformViewModel<GooglePlayAccountScreenContract.ViewModel>()
        val uriHandler = LocalUriHandler.current

        LaunchedEffect(Unit) {
            if (data != null) viewModel.signIn(data)
        }

        GooglePlayAccountScreenUI(
            state = viewModel.state.collectAsState(),
            onUpClick = { state.navigateBack() },
            onSignInClick = { uriHandler.openUri(AccountScreenContract.DEEP_LINK_AUTH_URL) },
            onSignOutClick = { viewModel.signOut() },
            refresh = { viewModel.refresh() }
        )

    }

}

@Composable
fun GooglePlayAccountScreenUI(
    state: State<ScreenState>,
    onUpClick: () -> Unit,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
    refresh: () -> Unit
) {

    AccountScreenContainer(
        state = state,
        onUpClick = onUpClick
    ) { screenState ->

        when (screenState) {
            ScreenState.SignedOut -> {
                AccountScreenSignedOut(
                    startSignIn = onSignInClick
                )
            }

            ScreenState.Loading -> {
                AccountScreenLoading()
            }

            is ScreenState.SignedIn -> {

                AccountScreenSignedIn(
                    email = screenState.email,
                    issue = screenState.issue,
                    refresh = refresh,
                    signOut = onSignOutClick,
                    signIn = onSignInClick
                ) {

                    extraContent?.invoke(this)

                }

            }

            is ScreenState.Error -> {
                AccountScreenError(
                    issue = screenState.issue,
                    startSignIn = onSignInClick
                )
            }
        }

    }

}
