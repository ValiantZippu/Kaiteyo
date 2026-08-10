package ua.syt0r.kanji.presentation.screen.main.screen.account

import com.android.billingclient.api.BillingClient.BillingResponseCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.DateTimePeriod
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import ua.syt0r.kanji.Res
import ua.syt0r.kanji.account_subscription_duration_days
import ua.syt0r.kanji.account_subscription_duration_months
import ua.syt0r.kanji.account_subscription_duration_unknown
import ua.syt0r.kanji.account_subscription_duration_years
import ua.syt0r.kanji.core.AccountManager
import ua.syt0r.kanji.core.AccountState
import ua.syt0r.kanji.core.NetworkApi
import ua.syt0r.kanji.core.SubscriptionInfo
import ua.syt0r.kanji.core.billing.BillingManager
import ua.syt0r.kanji.core.billing.BillingState
import ua.syt0r.kanji.core.billing.PurchasesUpdate
import ua.syt0r.kanji.presentation.screen.main.screen.account.GooglePlayAccountScreenContract.ScreenState


class GooglePlayAccountScreenViewModel(
    viewModelScope: CoroutineScope,
    private val accountManager: AccountManager,
    private val billingManager: BillingManager,
    private val networkApi: NetworkApi
) : GooglePlayAccountScreenContract.ViewModel {

    private val _state = MutableStateFlow<ScreenState>(ScreenState.Loading)
    override val state: StateFlow<ScreenState> = _state

    init {

        accountManager.state
            .flatMapLatest { it.toScreenStateFlow() }
            .onEach { _state.value = it }
            .launchIn(viewModelScope)

    }

    override fun signIn(data: AccountScreenContract.ScreenData) {
        accountManager.signIn(data.refreshToken, data.idToken)
    }

    override fun signOut() {
        accountManager.signOut()
    }

    override fun refresh() {
        accountManager.refreshUserData()
    }

    private fun AccountState.toScreenStateFlow() = when (this) {
        AccountState.Loading -> flowOf(ScreenState.Loading)
        AccountState.LoggedOut -> flowOf(ScreenState.SignedOut)
        is AccountState.LoggedIn -> channelFlow {
            coroutineScope {
                val screenState = ScreenState.SignedIn(
                    email = email,
                    subscriptionInfo = subscriptionInfo,
                    issue = issue,
                )
                send(screenState)
            }

        }

        is AccountState.Error -> flowOf(ScreenState.Error(issue))
    }
