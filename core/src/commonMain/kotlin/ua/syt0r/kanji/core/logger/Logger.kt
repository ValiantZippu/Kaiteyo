package ua.syt0r.kanji.core.logger

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.measureTime

object Logger : KoinComponent {

    private val configuration by inject<LoggerConfiguration>()

    fun d(message: String) {
        if (configuration.isEnabled) platformLogMessage(message)
    }

    fun logMethod() {
        if (configuration.isEnabled) platformLogMethod()
    }

    fun w(message: String) {
        if (configuration.isEnabled) platformLogMessage("WARN: $message")
    }

    fun e(message: String) {
        platformLogError(message)
    }

}

expect fun platformLogMessage(message: String)
expect fun platformLogMethod()
expect fun platformLogError(message: String)

data class LoggerConfiguration(
    val isEnabled: Boolean
)

inline fun <T> runWithTimeLog(variableName: String, block: () -> T): T {
    val value: T
    val time = measureTime { value = block() }
    Logger.d("Loaded $variableName, loadingTime[$time]")
    return value
}