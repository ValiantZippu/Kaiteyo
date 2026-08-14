package ua.syt0r.kanji.desktop.engine.account

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ua.syt0r.kanji.core.logger.Logger
import ua.syt0r.kanji.desktop.engine.history.ActivityCategory
import ua.syt0r.kanji.desktop.engine.history.ActivityLog
import ua.syt0r.kanji.desktop.engine.settings.SettingsEngine
import java.io.File
import kotlin.random.Random

// ============================================
// KAITEYO DESKTOP ACCOUNT ENGINE
// The control center for identity, authentication
// and future cloud synchronization. Owns:
//   • local identity/profile
//   • provider connections (Local + OAuth)
//   • devices and sessions
//   • account settings (sync/security/privacy)
//   • the OAuth device-flow state machine
//   • on-disk storage statistics
// Persists versioned JSON state under
// ~/.kaiteyo/account/ — tokens go to TokenVault.
// Providers are pluggable: adding a new provider
// only requires a connector + ProviderKind entry.
// ============================================

class AccountEngine(
    private val dataDir: File,
    private val settings: SettingsEngine,
    private val tokenVault: TokenVault = FileTokenVault(dataDir),
    private val github: GitHubDeviceFlowClient = GitHubDeviceFlowClient(),
    private val activityLog: ActivityLog? = null,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }
    private var currentDeviceId: String? = null
    private var pollJob: Job? = null

    private val _identity = MutableStateFlow(AccountIdentity())
    val identity: StateFlow<AccountIdentity> = _identity.asStateFlow()

    private val _connections = MutableStateFlow<List<ProviderConnection>>(emptyList())
    val connections: StateFlow<List<ProviderConnection>> = _connections.asStateFlow()

    private val _devices = MutableStateFlow<List<AccountDevice>>(emptyList())
    val devices: StateFlow<List<AccountDevice>> = _devices.asStateFlow()

    private val _sessions = MutableStateFlow<List<AccountSession>>(emptyList())
    val sessions: StateFlow<List<AccountSession>> = _sessions.asStateFlow()

    private val _settingsData = MutableStateFlow(AccountSettingsData())
    val settingsData: StateFlow<AccountSettingsData> = _settingsData.asStateFlow()

    private val _authFlow = MutableStateFlow<AuthFlowState>(AuthFlowState.Idle)
    val authFlow: StateFlow<AuthFlowState> = _authFlow.asStateFlow()

    private val _storage = MutableStateFlow(StorageBreakdown())
    val storage: StateFlow<StorageBreakdown> = _storage.asStateFlow()

    init {
        load()
        refreshStorage()
    }

    // ------------------------------------------------------------
    // Persistence
    // ------------------------------------------------------------

    private fun file(name: String): File = File(dataDir, name)

    private inline fun <reified T> writeState(name: String, value: T) {
        runCatching {
            file(name).writeText(json.encodeToString(value))
        }.onFailure { Logger.w("AccountEngine: failed to persist $name: ${it.message}") }
    }

    private inline fun <reified T> readState(name: String, default: T): T = try {
        val f = file(name)
        if (f.exists()) json.decodeFromString<T>(f.readText()) else default
    } catch (e: Exception) {
        Logger.w("AccountEngine: failed to read $name: ${e.message}")
        default
    }

    private fun load() {
        dataDir.mkdirs()

        _identity.value = readState("identity.json", AccountIdentity())
        _connections.value = readState("connections.json", emptyList())
        _devices.value = readState("devices.json", emptyList())
        _sessions.value = readState("sessions.json", emptyList())
        currentDeviceId = readState("device-id.json", "")

        val hadSettingsFile = file("settings.json").exists()
        _settingsData.value = readState("settings.json", AccountSettingsData()).let {
            if (hadSettingsFile) it
            else it.copy(
                autoSync = settings.getBool("sync.auto"),
                syncIntervalMinutes = settings.getInt("sync.interval-minutes", 30)
            )
        }
        persistSettings()

        // Seed the identity from legacy settings keys on first run after upgrade.
        if (_identity.value.id.isBlank()) {
            val legacyName = settings.getString("account.profile-name", "Learner")
            val joined = settings.getString("account.joined-at", "").toLongOrNull()
                ?: Clock.System.now().toEpochMilliseconds()
            _identity.value = AccountIdentity(
                id = newId("local"),
                displayName = legacyName,
                learnerLevel = settings.getString("account.learner-level", "beginner"),
                joinedAtEpochMs = joined,
                avatarSeed = legacyName,
                isLocalOnly = true
            )
            persistIdentity()
        }

        // The provider list always contains every known kind so the UI can
        // offer connect actions; status is reconciled against the token vault.
        val existingById = _connections.value.associateBy { it.kind }
        _connections.value = ProviderKind.entries.map { kind ->
            val existing = existingById[kind]
            val status = when {
                kind == ProviderKind.Local -> ConnectionStatus.Connected
                tokenVault.configuredKinds().contains(kind) -> ConnectionStatus.Connected
                kind == ProviderKind.GitHub && !githubConfigured() -> ConnectionStatus.NotConfigured
                else -> ConnectionStatus.Available
            }
            existing?.copy(status = status, errorMessage = "") ?: ProviderConnection(
                kind = kind,
                status = status,
                connectedAtEpochMs = if (kind == ProviderKind.Local) Clock.System.now().toEpochMilliseconds() else 0L
            )
        }
        persistConnections()

        // Register this device (stable id persisted across restarts).
        val devices = _devices.value
        val existingDevice = currentDeviceId?.let { id -> devices.firstOrNull { it.id == id } }
        if (existingDevice == null) {
            val device = createCurrentDevice()
            currentDeviceId = device.id
            _devices.value = devices.filterNot { it.isCurrent } + device
            writeState("device-id.json", device.id)
        } else {
            _devices.value = devices.map {
                if (it.id == existingDevice.id) it.copy(
                    isCurrent = true,
                    lastOnlineEpochMs = Clock.System.now().toEpochMilliseconds()
                ) else it.copy(isCurrent = false)
            }
        }
        persistDevices()

        // Always keep a current local session.
        if (_sessions.value.none { it.isCurrent }) {
            _sessions.value = _sessions.value.filterNot { it.isCurrent } + AccountSession(
                id = newId("session"),
                deviceId = currentDeviceId ?: "",
                providerKind = ProviderKind.Local,
                createdAtEpochMs = Clock.System.now().toEpochMilliseconds(),
                lastActiveAtEpochMs = Clock.System.now().toEpochMilliseconds(),
                isCurrent = true
            )
            persistSessions()
        }
    }

    private fun persistIdentity() = writeState("identity.json", _identity.value)
    private fun persistConnections() = writeState("connections.json", _connections.value)
    private fun persistDevices() = writeState("devices.json", _devices.value)
    private fun persistSessions() = writeState("sessions.json", _sessions.value)
    private fun persistSettings() = writeState("settings.json", _settingsData.value)

    // ------------------------------------------------------------
    // Profile
    // ------------------------------------------------------------

    fun updateProfile(
        displayName: String,
        username: String,
        email: String,
        learnerLevel: String
    ) {
        val cleanName = displayName.trim().ifBlank { "Learner" }
        _identity.value = _identity.value.copy(
            displayName = cleanName,
            username = username.trim(),
            email = email.trim(),
            learnerLevel = learnerLevel,
            avatarSeed = cleanName
        )
        persistIdentity()
        // Mirror into legacy settings so other surfaces stay consistent.
        settings.set("account.profile-name", cleanName)
        settings.set("account.learner-level", learnerLevel)
        activityLog?.record(ActivityCategory.Settings, "Updated profile")
    }

    // ------------------------------------------------------------
    // Providers (OAuth)
    // ------------------------------------------------------------

    fun githubConfigured(): Boolean = _settingsData.value.githubClientId.isNotBlank()

    fun setGitHubClientId(value: String) {
        updateSettings { it.copy(githubClientId = value.trim()) }
    }

    /**
     * Resolves a usable GitHub access token for the connected account.
     * Refreshes the token first when it is expired and a refresh token
     * is available; otherwise returns the stored token as-is.
     */
    suspend fun githubAccessToken(): Result<String> = runCatching {
        val stored = tokenVault.read(ProviderKind.GitHub)
            ?: error("GitHub is not connected")
        if (stored.accessToken.isBlank()) error("GitHub credentials are missing")

        if (stored.isExpired) {
            // GitHub device-flow tokens are long-lived, so an expired marker
            // means the token genuinely needs a refresh.
            if (stored.refreshToken.isBlank()) {
                error("GitHub session expired — reconnect the account")
            }
            val clientId = _settingsData.value.githubClientId.ifBlank {
                error("GitHub OAuth is not configured")
            }
            val refreshed = github.refreshToken(clientId, stored.refreshToken).getOrThrow()
            val refreshedToken = AuthToken(
                accessToken = refreshed.accessToken,
                refreshToken = refreshed.refreshToken.ifBlank { stored.refreshToken },
                expiresAtEpochMs = if (refreshed.expiresInSeconds > 0) {
                    Clock.System.now().toEpochMilliseconds() + refreshed.expiresInSeconds * 1000L
                } else Long.MAX_VALUE,
                scope = refreshed.scope.ifBlank { stored.scope }
            )
            tokenVault.save(ProviderKind.GitHub, refreshedToken)
            refreshedToken.accessToken
        } else {
            stored.accessToken
        }
    }

    fun connectGitHub() {
        if (pollJob?.isActive == true) return
        if (!githubConfigured()) {
            _authFlow.value = AuthFlowState.Failed(
                provider = ProviderKind.GitHub,
                message = "GitHub OAuth is not configured yet. Add your GitHub OAuth App client ID " +
                    "in Account → Developer Options, then try again.",
                retryable = false
            )
            return
        }
        val clientId = _settingsData.value.githubClientId
        _authFlow.value = AuthFlowState.Polling
        pollJob = scope.launch {
            val deviceCode = github.requestDeviceCode(clientId).getOrElse {
                _authFlow.value = AuthFlowState.Failed(ProviderKind.GitHub, friendlyError(it), retryable = true)
                return@launch
            }
            _authFlow.value = AuthFlowState.AwaitingAuthorization(
                provider = ProviderKind.GitHub,
                verificationUri = deviceCode.verificationUri,
                userCode = deviceCode.userCode,
                expiresAtEpochMs = Clock.System.now().toEpochMilliseconds() + deviceCode.expiresInSeconds * 1000L
            )

            val tokenResult = github.pollForToken(
                clientId = clientId,
                deviceCode = deviceCode.deviceCode,
                intervalSeconds = deviceCode.intervalSeconds,
                expiresAtEpochMs = Clock.System.now().toEpochMilliseconds() + deviceCode.expiresInSeconds * 1000L
            ).getOrElse {
                _authFlow.value = AuthFlowState.Failed(ProviderKind.GitHub, friendlyError(it), retryable = false)
                return@launch
            }

            val user = github.fetchUserInfo(tokenResult.accessToken).getOrElse {
                _authFlow.value = AuthFlowState.Failed(ProviderKind.GitHub, friendlyError(it), retryable = true)
                return@launch
            }

            tokenVault.save(
                ProviderKind.GitHub,
                AuthToken(
                    accessToken = tokenResult.accessToken,
                    refreshToken = tokenResult.refreshToken,
                    expiresAtEpochMs = if (tokenResult.expiresInSeconds > 0) {
                        Clock.System.now().toEpochMilliseconds() + tokenResult.expiresInSeconds * 1000L
                    } else Long.MAX_VALUE, // GitHub device-flow tokens are long-lived
                    scope = tokenResult.scope
                )
            )

            val now = Clock.System.now().toEpochMilliseconds()
            _connections.value = _connections.value.map {
                if (it.kind == ProviderKind.GitHub) it.copy(
                    userId = user.id.toString(),
                    displayName = user.login,
                    connectedAtEpochMs = if (it.connectedAtEpochMs == 0L) now else it.connectedAtEpochMs,
                    lastUsedAtEpochMs = now,
                    status = ConnectionStatus.Connected,
                    errorMessage = ""
                ) else it
            }
            persistConnections()

            _sessions.value = _sessions.value + AccountSession(
                id = newId("session"),
                deviceId = currentDeviceId ?: "",
                providerKind = ProviderKind.GitHub,
                createdAtEpochMs = now,
                lastActiveAtEpochMs = now,
                isCurrent = false
            )
            persistSessions()

            // The cloud identity enriches the local profile — never replaces it.
            _identity.value = _identity.value.copy(
                username = user.login.ifBlank { _identity.value.username },
                email = user.email.ifBlank { _identity.value.email },
                isLocalOnly = false
            )
            persistIdentity()

            activityLog?.record(ActivityCategory.Sync, "Connected GitHub account ${user.login}")
            _authFlow.value = AuthFlowState.Completed(ProviderKind.GitHub, user.login)
        }
    }

    fun cancelAuthFlow() {
        pollJob?.cancel()
        pollJob = null
        _authFlow.value = AuthFlowState.Idle
    }

    fun dismissAuthFlow() {
        pollJob?.cancel()
        pollJob = null
        _authFlow.value = AuthFlowState.Idle
    }

    fun disconnectProvider(kind: ProviderKind) {
        if (kind == ProviderKind.Local) return
        scope.launch {
            runCatching { github.revokeToken(_settingsData.value.githubClientId, tokenVault.read(kind)?.accessToken.orEmpty()) }
            tokenVault.delete(kind)
            _connections.value = _connections.value.map {
                if (it.kind == kind) it.copy(
                    status = if (kind == ProviderKind.GitHub && !githubConfigured()) ConnectionStatus.NotConfigured else ConnectionStatus.Available,
                    userId = "",
                    displayName = "",
                    errorMessage = ""
                ) else it
            }
            _sessions.value = _sessions.value.filterNot { it.providerKind == kind }
            persistConnections()
            persistSessions()
            activityLog?.record(ActivityCategory.System, "Disconnected ${kind.displayName}")
        }
    }

    fun reconnectProvider(kind: ProviderKind) {
        when (kind) {
            ProviderKind.GitHub -> connectGitHub()
            else -> Unit
        }
    }

    // ------------------------------------------------------------
    // Sessions
    // ------------------------------------------------------------

    fun signOutOfSession(sessionId: String) {
        val target = _sessions.value.firstOrNull { it.id == sessionId } ?: return
        val wasCurrent = target.isCurrent
        _sessions.value = _sessions.value.filterNot { it.id == sessionId }
        if (wasCurrent) {
            _sessions.value = _sessions.value + AccountSession(
                id = newId("session"),
                deviceId = currentDeviceId ?: "",
                providerKind = ProviderKind.Local,
                createdAtEpochMs = Clock.System.now().toEpochMilliseconds(),
                lastActiveAtEpochMs = Clock.System.now().toEpochMilliseconds(),
                isCurrent = true
            )
        }
        // Drop tokens for providers that no longer have any active session.
        val providersStillInUse = _sessions.value.map { it.providerKind }.toSet()
        ProviderKind.entries.filterNot { it == ProviderKind.Local || it in providersStillInUse }
            .forEach { scope.launch { tokenVault.delete(it) } }
        _connections.value = _connections.value.map {
            if (it.kind == target.providerKind && !_sessions.value.any { s -> s.providerKind == it.kind }) {
                it.copy(status = ConnectionStatus.Available, userId = "", displayName = "")
            } else it
        }
        persistSessions()
        persistConnections()
        activityLog?.record(ActivityCategory.System, "Signed out of session ${target.providerKind.displayName}")
    }

    fun signOutOfOtherSessions() {
        val current = _sessions.value.firstOrNull { it.isCurrent }
        val removedKinds = _sessions.value.filterNot { it.id == current?.id }.map { it.providerKind }.toSet()
        _sessions.value = _sessions.value.filter { it.id == current?.id }
        removedKinds.forEach { kind -> scope.launch { tokenVault.delete(kind) } }
        _connections.value = _connections.value.map {
            if (it.kind != ProviderKind.Local && it.kind in removedKinds) it.copy(status = ConnectionStatus.Available, userId = "", displayName = "")
            else it
        }
        persistSessions()
        persistConnections()
        activityLog?.record(ActivityCategory.System, "Signed out of other sessions")
    }

    fun signOutOfAllSessions() {
        scope.launch { tokenVault.clear() }
        _sessions.value = listOf(
            AccountSession(
                id = newId("session"),
                deviceId = currentDeviceId ?: "",
                providerKind = ProviderKind.Local,
                createdAtEpochMs = Clock.System.now().toEpochMilliseconds(),
                lastActiveAtEpochMs = Clock.System.now().toEpochMilliseconds(),
                isCurrent = true
            )
        )
        _connections.value = _connections.value.map {
            when {
                it.kind == ProviderKind.Local -> it.copy(status = ConnectionStatus.Connected)
                it.kind == ProviderKind.GitHub && !githubConfigured() -> it.copy(status = ConnectionStatus.NotConfigured, userId = "", displayName = "")
                else -> it.copy(status = ConnectionStatus.Available, userId = "", displayName = "")
            }
        }
        _devices.value = _devices.value.map {
            if (it.isCurrent) it.copy(isTrusted = true) else it.copy(isTrusted = false)
        }
        persistSessions()
        persistConnections()
        persistDevices()
        activityLog?.record(ActivityCategory.System, "Signed out of all sessions")
    }

    // ------------------------------------------------------------
    // Devices
    // ------------------------------------------------------------

    fun renameDevice(deviceId: String, name: String) {
        val clean = name.trim().ifBlank { return }
        _devices.value = _devices.value.map { if (it.id == deviceId) it.copy(name = clean) else it }
        persistDevices()
        activityLog?.record(ActivityCategory.System, "Renamed device to '$clean'")
    }

    fun removeDevice(deviceId: String) {
        if (deviceId == currentDeviceId) return
        _devices.value = _devices.value.filterNot { it.id == deviceId }
        _sessions.value = _sessions.value.filterNot { it.deviceId == deviceId }
        persistDevices()
        persistSessions()
        activityLog?.record(ActivityCategory.System, "Removed device")
    }

    // ------------------------------------------------------------
    // Settings
    // ------------------------------------------------------------

    fun updateSettings(transform: (AccountSettingsData) -> AccountSettingsData) {
        _settingsData.value = transform(_settingsData.value)
        persistSettings()
    }

    // ------------------------------------------------------------
    // Sync bookkeeping
    // ------------------------------------------------------------

    /** Record that a provider sync just completed (updates device + connection). */
    fun recordSync(kind: ProviderKind, atEpochMs: Long = Clock.System.now().toEpochMilliseconds()) {
        _devices.value = _devices.value.map {
            if (it.isCurrent) it.copy(lastSyncEpochMs = atEpochMs) else it
        }
        _connections.value = _connections.value.map {
            if (it.kind == kind) it.copy(lastUsedAtEpochMs = atEpochMs) else it
        }
        persistDevices()
        persistConnections()
    }

    // ------------------------------------------------------------
    // Storage
    // ------------------------------------------------------------

    fun refreshStorage() {
        val root = dataDir.parentFile ?: dataDir
        _storage.value = StorageInspector.inspect(root)
    }

    // ------------------------------------------------------------
    // Danger zone
    // ------------------------------------------------------------

    /** Wipe account state (identity, providers, devices, sessions, tokens). Study data is untouched. */
    fun resetLocalAccount() {
        scope.launch { tokenVault.clear() }
        pollJob?.cancel()
        pollJob = null
        dataDir.listFiles()?.forEach { file ->
            runCatching {
                if (file.isFile) file.delete()
                else file.deleteRecursively()
            }
        }
        currentDeviceId = null
        _identity.value = AccountIdentity()
        _connections.value = emptyList()
        _devices.value = emptyList()
        _sessions.value = emptyList()
        _authFlow.value = AuthFlowState.Idle
        // Keep the user's study preferences; wipe account-scoped preferences only.
        _settingsData.value = _settingsData.value.copy(
            autoSync = false,
            syncOnStart = false
        )
        // Persist the cleared preferences first so load() does not re-seed
        // them from the legacy global settings keys.
        persistSettings()
        load()
        refreshStorage()
        activityLog?.record(ActivityCategory.System, "Reset local account data")
    }

    // ------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------

    private fun createCurrentDevice(): AccountDevice {
        val userName = runCatching { System.getProperty("user.name") }.getOrNull()
        val osName = runCatching { System.getProperty("os.name") }.getOrNull() ?: "Desktop"
        val platform = when {
            osName.contains("win", ignoreCase = true) -> "Windows"
            osName.contains("mac", ignoreCase = true) -> "macOS"
            osName.contains("linux", ignoreCase = true) -> "Linux"
            else -> "Desktop"
        }
        return AccountDevice(
            id = newId("device"),
            name = userName?.let { "$it's $platform" } ?: "This device",
            platform = platform,
            appVersion = resolveAppVersion(),
            databaseVersion = resolveDatabaseVersion(),
            lastOnlineEpochMs = Clock.System.now().toEpochMilliseconds(),
            isCurrent = true,
            isTrusted = true
        )
    }

    /** Resolved build version (falls back to "dev" when the generated BuildConfig is unavailable). */
    val appVersion: String get() = resolveAppVersion()

    /** Bundled app-data database schema version. */
    val databaseVersion: Int get() = resolveDatabaseVersion()

    private fun resolveAppVersion(): String =
        runCatching { ua.syt0r.kanji.BuildConfig.versionName }.getOrNull() ?: "dev"

    private fun resolveDatabaseVersion(): Int =
        runCatching { ua.syt0r.kanji.BuildConfig.appDataDatabaseVersion }.getOrNull() ?: 0

    private fun newId(prefix: String): String =
        "$prefix-${Clock.System.now().toEpochMilliseconds()}-${Random.nextInt(0xFFFF).toString(16)}"

    private fun friendlyError(e: Throwable): String {
        val message = e.message ?: return "Could not reach GitHub — check your connection and try again."
        return when {
            message.contains("HTTP 401", ignoreCase = true) ||
                message.contains("unauthorized", ignoreCase = true) ->
                "GitHub rejected the credentials — sign in again."

            message.contains("connect", ignoreCase = true) ||
                message.contains("timeout", ignoreCase = true) ->
                "Could not reach GitHub — check your internet connection."

            else -> message
        }
    }

    companion object {
        fun accountDataDir(): File = File(System.getProperty("user.home"), ".kaiteyo/account")
    }
}
