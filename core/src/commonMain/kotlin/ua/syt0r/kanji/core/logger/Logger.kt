package ua.syt0r.kanji.core.logger

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.measureTime

/**
 * Cross-platform logging facade backed by Koin.
 *
 * Delegates to platform-specific implementations ([platformLogMessage],
 * [platformLogMethod], [platformLogError]) defined in each target's
 * `Logger.<platform>.kt` file.
 */
object Logger : KoinComponent {

    private val configuration by inject<LoggerConfiguration>()

    /** Debug-level log. No-op if logging is disabled via [LoggerConfiguration.isEnabled]. */
    fun d(message: String) {
        if (configuration.isEnabled) platformLogMessage(message)
    }

    /** Logs the calling method (class + method name) for tracing call sites. */
    fun logMethod() {
        if (configuration.isEnabled) platformLogMethod()
    }

    /** Warn-level log. No-op if logging is disabled. */
    fun w(message: String) {
        if (configuration.isEnabled) platformLogMessage("WARN: $message")
    }

    /** Error-level log. Always logged, regardless of [LoggerConfiguration.isEnabled]. */
    fun e(message: String) {
        platformLogError(message)
    }

}

/**
 * Per-platform expectation for sending a debug log message.
 * Implemented in each target source set (e.g. `Logger.android.kt`, `Logger.jvm.kt`).
 */
expect fun platformLogMessage(message: String)

/** Per-platform expectation for logging the current method call site. */
expect fun platformLogMethod()

/** Per-platform expectation for logging an error message. */
expect fun platformLogError(message: String)

/**
 * Configuration for [Logger].
 *
 * @param isEnabled When `false`, debug and warn logs are suppressed. Error logs still pass through.
 */
data class LoggerConfiguration(
    val isEnabled: Boolean
)

/**
 * Executes [block] and logs the elapsed time using [Logger.d].
 *
 * @param variableName A human-readable label describing what was loaded.
 * @param block The block to measure and execute.
 * @return The result of [block].
 */
inline fun <T> runWithTimeLog(variableName: String, block: () -> T): T {
    val value: T
    val time = measureTime { value = block() }
    Logger.d("Loaded $variableName, loadingTime[$time]")
    return value
}