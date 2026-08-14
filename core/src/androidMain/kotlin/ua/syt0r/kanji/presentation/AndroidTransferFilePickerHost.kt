package ua.syt0r.kanji.presentation

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import ua.syt0r.kanji.core.logger.Logger
import ua.syt0r.kanji.core.transfer.AndroidTransferFileAccess
import kotlin.coroutines.resume

/**
 * Wraps the app content and wires the Android import/export file pickers.
 *
 * The common `pickImportFile`/`saveExportFile` actuals have no Activity to
 * start the Storage Access Framework intents from, so this host registers
 * two Activity Result launchers and installs itself as the provider for
 * [AndroidTransferFileAccess] for the lifetime of the composition.
 *
 * Import uses `ACTION_OPEN_DOCUMENT` (all file types; the chosen extension
 * is validated against the requested list) and returns the file bytes. On a
 * successful pick the read grant is persisted via
 * `takePersistableUriPermission` and the URI + display name are remembered
 * in SharedPreferences, which lets the Import tab offer a picker-free
 * "Re-import {name}" shortcut ([AndroidTransferFileAccess.getLastImportFileName]
 * / [AndroidTransferFileAccess.readLastImportFile]).
 *
 * Export uses `ACTION_CREATE_DOCUMENT` and streams the bytes to the URI the
 * user picked. All operations suspend until the launcher callback fires, so
 * the UI thread is never blocked.
 */
@Composable
fun AndroidTransferFilePickerHost(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val host = remember { AndroidTransferFilePicker(context) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = host::onImportResult
    )
    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(AndroidTransferFilePicker.SAVE_MIME_TYPE),
        onResult = host::onSaveResult
    )

    DisposableEffect(host, importLauncher, saveLauncher) {
        host.attach(importLauncher, saveLauncher)
        AndroidTransferFileAccess.install(host)
        onDispose {
            AndroidTransferFileAccess.clear()
            host.detach()
        }
    }

    content()
}

/**
 * Holds the pending continuations, handles launcher results, and remembers
 * the last picked import file. All callbacks and suspend calls arrive on
 * the main thread, so plain fields are sufficient.
 */
private class AndroidTransferFilePicker(context: Context) : AndroidTransferFileAccess.Provider {

    private class ImportRequest(
        val continuation: CancellableContinuation<ByteArray?>,
        val extensions: Array<out String>
    )

    private class SaveRequest(
        val continuation: CancellableContinuation<Boolean>,
        val bytes: ByteArray
    )

    private class PickedRead(
        val bytes: ByteArray?,
        val displayName: String?,
        val failure: String?
    )

    companion object {
        private const val SAVE_MIME_TYPE = "application/octet-stream"
        private const val ALL_MIME_TYPES = "*/*"
        private const val PREFS_NAME = "transfer_file_access"
        private const val KEY_LAST_IMPORT_URI = "last_import_uri"
        private const val KEY_LAST_IMPORT_NAME = "last_import_name"
    }

    private val appContext = context.applicationContext
    private val contentResolver: ContentResolver = appContext.contentResolver
    private val preferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val pickerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var importLauncher: ManagedActivityResultLauncher<Array<String>, Uri?>? = null
    private var saveLauncher: ManagedActivityResultLauncher<String, Uri?>? = null

    private var pendingImport: ImportRequest? = null
    private var pendingSave: SaveRequest? = null

    fun attach(
        importLauncher: ManagedActivityResultLauncher<Array<String>, Uri?>,
        saveLauncher: ManagedActivityResultLauncher<String, Uri?>
    ) {
        this.importLauncher = importLauncher
        this.saveLauncher = saveLauncher
    }

    fun detach() {
        importLauncher = null
        saveLauncher = null
        pendingImport = null
        pendingSave = null
    }

    override suspend fun pickImportFile(
        description: String,
        extensions: Array<out String>
    ): ByteArray? = suspendCancellableCoroutine { continuation ->
        val launcher = importLauncher
        if (launcher == null) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }
        pendingImport = ImportRequest(continuation, extensions)
        continuation.invokeOnCancellation {
            if (pendingImport?.continuation === continuation) pendingImport = null
        }
        launcher.launch(arrayOf(ALL_MIME_TYPES))
    }

    override suspend fun saveExportFile(
        bytes: ByteArray,
        suggestedName: String,
        description: String,
        extensions: Array<out String>
    ): Boolean = suspendCancellableCoroutine { continuation ->
        val launcher = saveLauncher
        if (launcher == null) {
            continuation.resume(false)
            return@suspendCancellableCoroutine
        }
        pendingSave = SaveRequest(continuation, bytes)
        continuation.invokeOnCancellation {
            if (pendingSave?.continuation === continuation) pendingSave = null
        }
        launcher.launch(suggestedName)
    }

    override suspend fun getLastImportFileName(): String? {
        val storedUri = preferences.getString(KEY_LAST_IMPORT_URI, null) ?: return null
        val name = preferences.getString(KEY_LAST_IMPORT_NAME, null) ?: return null
        val stillGranted = contentResolver.persistedUriPermissions
            .any { it.uri.toString() == storedUri && it.isReadPermission }
        if (!stillGranted) {
            clearLastImport()
            return null
        }
        return name
    }

    override suspend fun readLastImportFile(): ByteArray? {
        val storedUri = preferences.getString(KEY_LAST_IMPORT_URI, null) ?: return null
        val bytes = withContext(Dispatchers.IO) {
            runCatching {
                contentResolver.openInputStream(Uri.parse(storedUri))?.use { it.readBytes() }
            }.getOrNull()
        }
        if (bytes == null || bytes.isEmpty()) {
            // The file moved, was deleted, or the grant was revoked — the
            // remembered entry is stale, so stop offering the shortcut.
            clearLastImport()
            return null
        }
        return bytes
    }

    fun onImportResult(uri: Uri?) {
        val request = pendingImport ?: return
        pendingImport = null
        val continuation = request.continuation
        if (!continuation.isActive) return

        if (uri == null) {
            continuation.resume(null)
            return
        }

        // Read + validate off the main thread — APKG collections can be large.
        pickerScope.launch {
            val read = readPickedBytes(uri, request.extensions)
            if (read.failure != null) {
                Logger.w("AndroidTransferFileAccess: ${read.failure}")
            } else {
                persistLastImport(uri, read.displayName)
            }
            if (continuation.isActive) continuation.resume(read.bytes)
        }
    }

    fun onSaveResult(uri: Uri?) {
        val request = pendingSave ?: return
        pendingSave = null
        val continuation = request.continuation
        if (!continuation.isActive) return

        if (uri == null) {
            continuation.resume(false)
            return
        }

        // Write off the main thread — exports can be large.
        pickerScope.launch {
            val written = runCatching {
                contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(request.bytes)
                    out.flush()
                } != null
            }.getOrDefault(false)

            if (!written) {
                Logger.w("AndroidTransferFileAccess: failed to write the export file")
            }
            if (continuation.isActive) continuation.resume(written)
        }
    }

    private fun readPickedBytes(uri: Uri, extensions: Array<out String>): PickedRead {
        val displayName = queryDisplayName(uri)
        val requested = extensions.map { it.removePrefix(".").lowercase() }
        if (displayName != null && requested.isNotEmpty() &&
            displayName.substringAfterLast('.', "").lowercase() !in requested
        ) {
            return PickedRead(
                bytes = null,
                displayName = displayName,
                failure = "picked file $displayName does not match ${requested.toList()}"
            )
        }
        val bytes = runCatching {
            contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull()
        if (bytes == null) {
            return PickedRead(
                bytes = null,
                displayName = displayName,
                failure = "could not open the picked content stream"
            )
        }
        return PickedRead(bytes = bytes, displayName = displayName, failure = null)
    }

    private fun persistLastImport(uri: Uri, displayName: String?) {
        // Best effort: some providers do not support persisted grants.
        runCatching {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }.onFailure {
            Logger.w("AndroidTransferFileAccess: could not persist the read grant: ${it.message}")
        }
        val name = displayName?.takeIf { it.isNotBlank() } ?: return
        preferences.edit()
            .putString(KEY_LAST_IMPORT_URI, uri.toString())
            .putString(KEY_LAST_IMPORT_NAME, name)
            .apply()
    }

    private fun clearLastImport() {
        preferences.edit()
            .remove(KEY_LAST_IMPORT_URI)
            .remove(KEY_LAST_IMPORT_NAME)
            .apply()
    }

    private fun queryDisplayName(uri: Uri): String? =
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
}
