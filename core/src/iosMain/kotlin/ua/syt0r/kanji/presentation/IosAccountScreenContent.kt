package ua.syt0r.kanji.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.uikit.LocalUIViewController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.core.module.Module
import platform.AuthenticationServices.ASPresentationAnchor
import platform.AuthenticationServices.ASWebAuthenticationPresentationContextProvidingProtocol
import platform.AuthenticationServices.ASWebAuthenticationSession
import platform.Foundation.NSURL
import platform.UIKit.UIViewController
import platform.darwin.NSObject
import ua.syt0r.kanji.core.AccountManager
import ua.syt0r.kanji.core.AccountState
import ua.syt0r.kanji.core.ApiRequestIssue
import ua.syt0r.kanji.core.SubscriptionInfo
import ua.syt0r.kanji.core.logger.Logger
import ua.syt0r.kanji.presentation.IosAccountScreenContract.ScreenState
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState
import ua.syt0r.kanji.presentation.screen.main.screen.account.AccountScreenContainer
import ua.syt0r.kanji.presentation.screen.main.screen.account.AccountScreenContract
import ua.syt0r.kanji.presentation.screen.main.screen.account.AccountScreenContract.Companion.ACCOUNT_DELETE_URL
import ua.syt0r.kanji.presentation.screen.main.screen.account.AccountScreenContract.Companion.ACCOUNT_WEB_PAGE_URL
import ua.syt0r.kanji.presentation.screen.main.screen.account.AccountScreenError
import ua.syt0r.kanji.presentation.screen.main.screen.account.AccountScreenLoading
import ua.syt0r.kanji.presentation.screen.main.screen.account.AccountScreenSignedIn
import ua.syt0r.kanji.presentation.screen.main.screen.account.AccountScreenSignedOut

fun Module.addAccountScreenComponents() {

    single<AccountScreenContract.Content> { IosAccountScreenContent }

    multiplatformViewModel {
        IosAccountScreenViewModel(
            coroutineScope = it.component1(),
            accountManager = get()
        )
    }

}

object IosAccountScreenContent : AccountScreenContract.Content {

    @Composable
    override fun invoke(
        state: MainNavigationState,
        data: AccountScreenContract.ScreenData?
    ) {

        val viewModel = getMultiplatformViewModel<IosAccountScreenViewModel>()
        val uriHandler = LocalUriHandler.current

        LaunchedEffect(Unit) {
            if (data != null) viewModel.signIn(data)
        }

        val viewController = LocalUIViewController.current

        AccountScreenUI(
            state = viewModel.state.collectAsState(),
            onUpClick = { state.navigateBack() },
            signIn = {
                val authURL = NSURL(string = AccountScreenContract.DEEP_LINK_AUTH_URL)
                val session = ASWebAuthenticationSession(
                    uRL = authURL,
                    callbackURLScheme = "kaiteyo",
                ) { url, error ->
                    Logger.d("authCallback url[$url] error[$error]")
                    url?.absoluteString
                        ?.let { AccountScreenContract.parseDeepLink(it) }
                        ?.let { viewModel.signIn(it) }
                }
                val authViewController = ViewControllerPresentationContextProvider(viewController)
                session.presentationContextProvider = authViewController
                session.start()
            },
            signOut = { viewModel.signOut() },
            refresh = { viewModel.refresh() },
            openAccountWeb = {
                val authURL = NSURL(string = ACCOUNT_WEB_PAGE_URL)
                val session = ASWebAuthenticationSession(
                    uRL = authURL,
                    callbackURLScheme = "kaiteyo",
                ) { url, error ->
                    Logger.d("deleteAccount url[$url] error[$error]")
                }
                val authViewController = ViewControllerPresentationContextProvider(viewController)
                session.presentationContextProvider = authViewController
                session.start()
            },
            deleteAccount = {
                val authURL = NSURL(string = ACCOUNT_DELETE_URL)
                val session = ASWebAuthenticationSession(
                    uRL = authURL,
                    callbackURLScheme = "kaiteyo",
                ) { url, error ->
                    Logger.d("deleteAccount url[$url] error[$error]")
                }
                val authViewController = ViewControllerPresentationContextProvider(viewController)
                session.presentationContextProvider = authViewController
                session.start()
            }
        )
    }

}

interface IosAccountScreenContract {

    sealed interface ScreenState {
        object SignedOut : ScreenState
        object Loading : ScreenState

        data class Loaded(
            val email: String,
            val subscriptionInfo: SubscriptionInfo,
            val issue: ApiRequestIssue?
        ) : ScreenState

        data class Error(
            val issue: ApiRequestIssue
        ) : ScreenState

    }

}

@Composable
fun AccountScreenUI(
    state: State<ScreenState>,
    onUpClick: () -> Unit,
    signIn: () -> Unit,
    signOut: () -> Unit,
    refresh: () -> Unit,
    openAccountWeb: (uriHandler: UriHandler) -> Unit,
    deleteAccount: (UriHandler) -> Unit,
) {

    AccountScreenContainer(
        state = state,
        onUpClick = onUpClick
    ) { screenState ->

        when (screenState) {
            ScreenState.SignedOut -> {
                AccountScreenSignedOut(
                    startSignIn = signIn
                )
            }

            ScreenState.Loading -> {
                AccountScreenLoading()
            }

            is ScreenState.Loaded -> {
                AccountScreenSignedIn(
                    email = screenState.email,
                    issue = screenState.issue,
                    refresh = refresh,
                    signOut = signOut,
                    signIn = signIn,
                    openAccountWeb = openAccountWeb,
                    deleteAccount = deleteAccount
                )
            }

            is ScreenState.Error -> {
                AccountScreenError(
                    issue = screenState.issue,
                    startSignIn = signIn
                )
            }
        }

    }

}

class IosAccountScreenViewModel(
    coroutineScope: CoroutineScope,
    private val accountManager: AccountManager
) {

    private val _state = MutableStateFlow<ScreenState>(ScreenState.Loading)
    val state: StateFlow<ScreenState> = _state

    init {

        accountManager.state
            .onEach {
                _state.value = when (it) {
                    AccountState.Loading -> ScreenState.Loading
                    AccountState.LoggedOut -> ScreenState.SignedOut
                    is AccountState.LoggedIn -> ScreenState.Loaded(
                        email = it.email,
                        subscriptionInfo = it.subscriptionInfo,
                        issue = it.issue
                    )

                    is AccountState.Error -> ScreenState.Error(it.issue)
                }
            }
            .launchIn(coroutineScope)

    }

    fun signIn(data: AccountScreenContract.ScreenData) {
        accountManager.signIn(data.refreshToken, data.idToken)
    }

    fun signOut() {
        accountManager.signOut()
    }

    fun refresh() {
        accountManager.refreshUserData()
    }

}

class ViewControllerPresentationContextProvider(
    val viewController: UIViewController
) : NSObject(),
    ASWebAuthenticationPresentationContextProvidingProtocol {

    override fun presentationAnchorForWebAuthenticationSession(
        session: ASWebAuthenticationSession
    ): ASPresentationAnchor? {
        return viewController.view.window
    }

}
