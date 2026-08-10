package ua.syt0r.kanji.presentation.screen.main.screen.account

import androidx.compose.runtime.Composable
import io.ktor.http.Url
import kotlinx.serialization.Serializable
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState

interface AccountScreenContract {

    companion object {

        const val ACCOUNT_WEB_PAGE_URL = "https://valiantzippu.github.io/Kaiteyo"
        const val ACCOUNT_DELETE_URL = "$ACCOUNT_WEB_PAGE_URL?delete=true"
        const val DEEP_LINK_AUTH_URL = "$ACCOUNT_WEB_PAGE_URL?deepLinkAuth=true"
        const val DEEP_LINK_AUTH_REDIRECT_URL = "kaiteyo://signin"

        fun serverAuthUrl(port: Int): String = "$ACCOUNT_WEB_PAGE_URL?callbackPort=$port"

        fun parseDeepLink(url: String): ScreenData? {
            return Url(url).parameters.run {
                ScreenData(
                    refreshToken = get("refreshToken") ?: return@run null,
                    idToken = get("idToken") ?: return@run null
                )
            }
        }
    }

    @Serializable
    data class ScreenData(
        val refreshToken: String,
        val idToken: String
    )

    interface Content {

        @Composable
        operator fun invoke(
            state: MainNavigationState,
            data: ScreenData?
        )

    }

}