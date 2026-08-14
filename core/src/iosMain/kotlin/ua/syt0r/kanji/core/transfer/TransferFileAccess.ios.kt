package ua.syt0r.kanji.core.transfer

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.toByteArray
import platform.UniformTypeIdentifiers.UTTypeItem
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIModalPresentationFullScreen
import platform.UIKit.UIViewController
import platform.darwin.NSObject
import ua.syt0r.kanji.core.getPrivateAppDataDirPath
import ua.syt0r.kanji.core.logger.Logger
import kotlin.coroutines.resume

/**
 * iOS file picking for the import/export screen, wired to the system
 * document picker (Files.app).
 *
 * Import presents an open picker (any document type — the chosen extension
 * is validated by the delegate) and returns the file bytes. Export writes a
 * temporary copy of [bytes] into the app's private data directory and hands
 * it to the "Save to Files" picker via `forExportingURLs`, cleaning the
 * temporary file up when the user finishes or cancels.
 *
 * Both functions suspend: the picker is presented from the top-most view
 * controller and the coroutine resumes when the delegate reports a result,
 * so the Compose UI thread is never blocked.
 */

@OptIn(ExperimentalForeignApi::class)
actual suspend fun pickImportFile(description: String, vararg extensions: String): ByteArray? =
    suspendCancellableCoroutine { continuation ->
        val picker = IosTransferFilePicker(
            onResult = { result ->
                if (continuation.isActive) {
                    continuation.resume((result as? TransferPickResult.Picked)?.bytes)
                }
            },
            extensions = extensions
        )
        picker.presentOpenPicker()
        continuation.invokeOnCancellation { picker.dismiss() }
    }

@OptIn(ExperimentalForeignApi::class)
actual suspend fun saveExportFile(
    bytes: ByteArray,
    suggestedName: String,
    description: String,
    vararg extensions: String
): Boolean = suspendCancellableCoroutine { continuation ->
    val tempPath = writeTemporaryExportFile(bytes, suggestedName)
    if (tempPath != null) {
        val picker = IosTransferFilePicker(
            onResult = { result ->
                if (continuation.isActive) {
                    continuation.resume((result as? TransferPickResult.Saved)?.success ?: false)
                }
            },
            extensions = extensions,
            exportPath = tempPath
        )
        picker.presentExportPicker()
        continuation.invokeOnCancellation { picker.dismiss() }
    } else {
        continuation.resume(false)
    }
}

/**
 * The two picker flows produce different result types (bytes for import,
 * success flag for export). The picker never knows which flow it serves —
 * it reports a tagged result and the caller maps it to its own type.
 */
private sealed class TransferPickResult {
    data class Picked(val bytes: ByteArray?) : TransferPickResult()
    data class Saved(val success: Boolean) : TransferPickResult()
}

@OptIn(ExperimentalForeignApi::class)
private fun writeTemporaryExportFile(bytes: ByteArray, suggestedName: String): String? =
    runCatching {
        // Keep the temp path as a plain string: it is handed to the system
        // picker (NSURL(fileURLWithPath = ...)), which needs the raw path.
        val dirPath = getPrivateAppDataDirPath() + "/anki_tmp"
        val dir = Path(dirPath)
        if (!SystemFileSystem.exists(dir)) {
            SystemFileSystem.createDirectories(dir)
        }
        val safeName = suggestedName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val filePath = "$dirPath/$safeName"
        SystemFileSystem.sink(Path(filePath)).buffered().use { sink ->
            sink.write(bytes, 0, bytes.size)
        }
        filePath
    }.getOrElse {
        Logger.w("TransferFileAccess: failed to write temporary export file: ${it.message}")
        null
    }

@OptIn(ExperimentalForeignApi::class)
private class IosTransferFilePicker(
    private val onResult: (TransferPickResult) -> Unit,
    private val extensions: Array<out String>,
    private val exportPath: String? = null
) {

    private lateinit var delegate: UIDocumentPickerDelegateProtocol

    fun presentOpenPicker() {
        val controller = UIDocumentPickerViewController(
            forOpeningContentTypes = listOf(UTTypeItem),
            asCopy = true
        )
        present(controller)
    }

    fun presentExportPicker() {
        val path = exportPath
        if (path == null) {
            fail("Export failed: could not write a temporary file")
            return
        }
        val controller = UIDocumentPickerViewController(
            forExportingURLs = listOf(NSURL(fileURLWithPath = path)),
            asCopy = true
        )
        present(controller)
    }

    /** Dismisses the picker when the awaiting coroutine is cancelled. */
    fun dismiss() {
        runCatching {
            topViewController()?.dismissViewControllerAnimated(true, completion = null)
        }
    }

    private fun present(controller: UIDocumentPickerViewController) {
        val presenter = topViewController()
        if (presenter == null) {
            fail("Could not present the file picker (no active view controller)")
            return
        }
        controller.allowsMultipleSelection = false
        controller.modalPresentationStyle = UIModalPresentationFullScreen
        delegate = createDelegate()
        controller.delegate = delegate
        presenter.presentViewController(
            viewControllerToPresent = controller,
            animated = true,
            completion = null
        )
    }

    private fun createDelegate(): UIDocumentPickerDelegateProtocol =
        object : NSObject(), UIDocumentPickerDelegateProtocol {

            override fun documentPicker(
                controller: UIDocumentPickerViewController,
                didPickDocumentAtURL: NSURL
            ) {
                handlePick(didPickDocumentAtURL)
            }

            override fun documentPicker(
                controller: UIDocumentPickerViewController,
                didPickDocumentsAtURLs: List<*>
            ) {
                handlePick(didPickDocumentsAtURLs.firstOrNull() as? NSURL)
            }

            override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                Logger.d("TransferFileAccess: document picker canceled")
                handleCanceled()
            }
        }

    private fun handlePick(url: NSURL?) {
        if (exportPath != null) {
            // Export flow: the system copied the temporary file to the
            // destination the user chose, so the operation succeeded.
            // Always remove the private temp copy, on success or failure.
            Logger.d("TransferFileAccess: export completed via document picker")
            deleteTemporaryExportFile()
            onResult(TransferPickResult.Saved(true))
            return
        }

        val path = url?.path
        if (path == null) {
            Logger.w("TransferFileAccess: picked file has no readable path")
            onResult(TransferPickResult.Picked(null))
            return
        }

        val pickedExtension = path.substringAfterLast('.', "").lowercase()
        if (extensions.isNotEmpty() &&
            extensions.none { it.removePrefix(".").lowercase() == pickedExtension }
        ) {
            Logger.w("TransferFileAccess: picked file $path does not match ${extensions.toList()}")
            onResult(TransferPickResult.Picked(null))
            return
        }

        val bytes = runCatching { NSData.dataWithContentsOfFile(path)?.toByteArray() }.getOrNull()
        if (bytes == null) {
            Logger.w("TransferFileAccess: failed to read picked file at $path")
        }
        onResult(TransferPickResult.Picked(bytes))
    }

    private fun handleCanceled() {
        if (exportPath != null) {
            deleteTemporaryExportFile()
            onResult(TransferPickResult.Saved(false))
        } else {
            onResult(TransferPickResult.Picked(null))
        }
    }

    private fun fail(message: String) {
        Logger.w("TransferFileAccess: $message")
        if (exportPath != null) {
            deleteTemporaryExportFile()
            onResult(TransferPickResult.Saved(false))
        } else {
            onResult(TransferPickResult.Picked(null))
        }
    }

    private fun deleteTemporaryExportFile() {
        val path = exportPath ?: return
        runCatching {
            val file = Path(path)
            if (SystemFileSystem.exists(file)) {
                SystemFileSystem.delete(file)
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun topViewController(): UIViewController? {
    var top = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (top?.presentedViewController != null) {
        top = top.presentedViewController
    }
    return top
}

// iOS does not remember the last picked file yet — that would need
// security-scoped bookmarks (startAccessingSecurityScopedResource) plus
// persistence. The picker-free re-import shortcut is not offered on iOS.
actual suspend fun getLastImportFileName(): String? = null

actual suspend fun readLastImportFile(): ByteArray? = null
