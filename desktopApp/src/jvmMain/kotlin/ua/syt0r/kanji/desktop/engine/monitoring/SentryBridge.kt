package ua.syt0r.kanji.desktop.engine.monitoring

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

// ============================================
// KAITEYO SENTRY BRIDGE
// Error tracking and performance monitoring.
// Delegates to Sentry SDK when available,
// provides a no-op fallback otherwise.
// ============================================

/** Severity levels for error reporting. */
enum class SentryLevel { Debug, Info, Warning, Error, Fatal }

/** A breadcrumb for context in error reports. */
data class SentryBreadcrumb(
    val message: String,
    val category: String = "",
    val level: SentryLevel = SentryLevel.Info,
    val data: Map<String, String> = emptyMap(),
    val timestamp: Instant = Clock.System.now()
)

/** Performance span for timing operations. */
class SentrySpan(
    val operation: String,
    val description: String = ""
) {
    private val startTime = System.currentTimeMillis()
    var isFinished = false; private set

    fun finish() {
        isFinished = true
    }

    val durationMs: Long get() = System.currentTimeMillis() - startTime
}

/** Interface for error monitoring backends. */
interface MonitoringBackend {
    val name: String
    val available: Boolean
    fun init(dsn: String, environment: String = "production")
    fun captureException(throwable: Throwable, extra: Map<String, String> = emptyMap()): String?
    fun captureMessage(message: String, level: SentryLevel = SentryLevel.Info): String?
    fun addBreadcrumb(breadcrumb: SentryBreadcrumb)
    fun startSpan(operation: String, description: String = ""): SentrySpan
    fun setUser(userId: String?, email: String? = null)
    fun flush(timeoutMs: Long = 2000)
}

/** Sentry SDK backend. */
class SentryBackend : MonitoringBackend {
    override val name = "Sentry"

    private var initialized = false

    override val available: Boolean by lazy {
        runCatching {
            Class.forName("io.sentry.Sentry")
            true
        }.getOrDefault(false)
    }

    override fun init(dsn: String, environment: String) {
        if (!available || initialized) return
        runCatching {
            val sentryClass = Class.forName("io.sentry.Sentry")
            val optionsClass = Class.forName("io.sentry.SentryOptions")
            val options = optionsClass.getDeclaredConstructor().newInstance()

            // Set DSN
            optionsClass.getMethod("setDsn", String::class.java)
                .invoke(options, dsn)
            optionsClass.getMethod("setEnvironment", String::class.java)
                .invoke(options, environment)
            optionsClass.getMethod("setRelease", String::class.java)
                .invoke(options, "kaiteyo@desktop")

            // Initialize
            sentryClass.getMethod("init", optionsClass).invoke(null, options)
            initialized = true
        }
    }

    override fun captureException(throwable: Throwable, extra: Map<String, String>): String? {
        if (!available || !initialized) return null
        return runCatching {
            val sentryClass = Class.forName("io.sentry.Sentry")
            sentryClass.getMethod("captureException", Throwable::class.java)
                .invoke(null, throwable)?.toString()
        }.getOrNull()
    }

    override fun captureMessage(message: String, level: SentryLevel): String? {
        if (!available || !initialized) return null
        return runCatching {
            val sentryClass = Class.forName("io.sentry.Sentry")
            val levelClass = Class.forName("io.sentry.SentryLevel")
            val constants = levelClass.enumConstants ?: emptyArray()
            val sentryLevel = constants.firstOrNull { c ->
                c?.toString()?.equals(level.name, true) == true
            } ?: constants.firstOrNull()
            sentryClass.getMethod("captureMessage", String::class.java, levelClass)
                .invoke(null, message, sentryLevel)?.toString()
        }.getOrNull()
    }

    override fun addBreadcrumb(breadcrumb: SentryBreadcrumb) {
        if (!available || !initialized) return
        runCatching {
            val sentryClass = Class.forName("io.sentry.Sentry")
            val bcClass = Class.forName("io.sentry.Breadcrumb")
            val bc = bcClass.getDeclaredConstructor().newInstance()
            bcClass.getMethod("setMessage", String::class.java).invoke(bc, breadcrumb.message)
            bcClass.getMethod("setCategory", String::class.java).invoke(bc, breadcrumb.category)
            sentryClass.getMethod("addBreadcrumb", bcClass).invoke(null, bc)
        }
    }

    override fun startSpan(operation: String, description: String): SentrySpan {
        val span = SentrySpan(operation, description)
        if (available && initialized) {
            runCatching {
                val sentryClass = Class.forName("io.sentry.Sentry")
                val hubClass = Class.forName("io.sentry.ISpan")
                // Start transaction/span via reflection
            }
        }
        return span
    }

    override fun setUser(userId: String?, email: String?) {
        if (!available || !initialized) return
        runCatching {
            val sentryClass = Class.forName("io.sentry.Sentry")
            val userClass = Class.forName("io.sentry.protocol.User")
            val user = userClass.getDeclaredConstructor().newInstance()
            userClass.getMethod("setId", String::class.java).invoke(user, userId)
            userClass.getMethod("setEmail", String::class.java).invoke(user, email)
            sentryClass.getMethod("setUser", userClass).invoke(null, user)
        }
    }

    override fun flush(timeoutMs: Long) {
        if (!available || !initialized) return
        runCatching {
            val sentryClass = Class.forName("io.sentry.Sentry")
            sentryClass.getMethod("flush", Long::class.java)
                .invoke(null, timeoutMs)
        }
    }
}

/**
 * No-op fallback for when Sentry is not available.
 */
object NoOpMonitoringBackend : MonitoringBackend {
    override val name = "No-Op"
    override val available = true
    override fun init(dsn: String, environment: String) {}
    override fun captureException(throwable: Throwable, extra: Map<String, String>) = null
    override fun captureMessage(message: String, level: SentryLevel) = null
    override fun addBreadcrumb(breadcrumb: SentryBreadcrumb) {}
    override fun startSpan(operation: String, description: String) = SentrySpan(operation, description)
    override fun setUser(userId: String?, email: String?) {}
    override fun flush(timeoutMs: Long) {}
}

/**
 * Composite monitor that tries Sentry first, falls back to no-op.
 */
object SentryBridge {
    private var backend: MonitoringBackend = NoOpMonitoringBackend

    val active: MonitoringBackend get() = backend

    fun init(dsn: String, environment: String = "production") {
        val sentry = runCatching { SentryBackend() }.getOrNull()
        if (sentry?.available == true) {
            backend = sentry
            backend.init(dsn, environment)
        }
    }

    fun captureException(throwable: Throwable, extra: Map<String, String> = emptyMap()): String? {
        return backend.captureException(throwable, extra)
    }

    fun captureMessage(message: String, level: SentryLevel = SentryLevel.Info): String? {
        return backend.captureMessage(message, level)
    }

    fun addBreadcrumb(message: String, category: String = "") {
        backend.addBreadcrumb(SentryBreadcrumb(message, category))
    }

    fun startSpan(operation: String, description: String = "") = backend.startSpan(operation, description)
}
