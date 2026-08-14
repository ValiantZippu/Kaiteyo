package ua.syt0r.kanji.presentation

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import org.koin.android.ext.android.inject
import ua.syt0r.kanji.core.logger.Logger
import ua.syt0r.kanji.core.theme_manager.ThemeManager
import ua.syt0r.kanji.presentation.screen.main.features.DeepLinkHandler

open class KaiteyoActivity : AppCompatActivity() {

    private val deepLinkHandler by inject<DeepLinkHandler>()
    private val themeManager by inject<ThemeManager>()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        themeManager.invalidate()

        Logger.d("intentData[${intent.dataString}]")
        intent.dataString?.let { deepLinkHandler.notifyDeepLink(it) }

        setContent {
            AndroidTransferFilePickerHost {
                KaiteyoApp(
                    windowSizeClass = calculateWindowSizeClass(this),
                    deepLinkHandler = deepLinkHandler
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Logger.d("intentData[${intent.dataString}]")
        intent.dataString?.let { deepLinkHandler.notifyDeepLink(it) }
    }

}
