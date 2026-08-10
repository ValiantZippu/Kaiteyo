package ua.syt0r.kanji.presentation.screen.main.screen.account

import ua.syt0r.kanji.core.ApiRequestIssue
import ua.syt0r.kanji.core.SubscriptionInfo

interface GooglePlayAccountScreenContract {

    interface ViewModel {
        val state: StateFlow<ScreenState>
        fun signIn(data: AccountScreenContract.ScreenData)
        fun signOut()
        fun refresh()
    }

    sealed interface ScreenState {
        data object Loading : ScreenState
        data object SignedOut : ScreenState
        data class SignedIn(
            val email: String,
            val subscriptionInfo: SubscriptionInfo,
            val issue: ApiRequestIssue?,
        ) : ScreenState

        data class Error(
            val issue: ApiRequestIssue
        ) : ScreenState
    }

}

data class DisplaySubscriptionOffer(
    val formattedPeriod: String,
    val formattedPrice: String,
    val billingFlowParams: BillingFlowParams
)
