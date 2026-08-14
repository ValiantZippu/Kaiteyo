package ua.syt0r.kanji.core.transfer

/**
 * Android file picking for the import/export screen, backed by the Storage
 * Access Framework via [AndroidTransferFileAccess]. The Activity Result
 * launchers are registered by
 * [ua.syt0r.kanji.presentation.AndroidTransferFilePickerHost] (installed
 * from `KaiteyoActivity`); until that host is installed — in previews or
 * tests — the actuals degrade to null/false so the UI reports the operation
 * as canceled/unsupported instead of doing nothing.
 */
actual suspend fun pickImportFile(description: String, vararg extensions: String): ByteArray? =
    AndroidTransferFileAccess.pickImportFile(description, extensions)

actual suspend fun saveExportFile(
    bytes: ByteArray,
    suggestedName: String,
    description: String,
    vararg extensions: String
): Boolean = AndroidTransferFileAccess.saveExportFile(bytes, suggestedName, description, extensions)

actual suspend fun getLastImportFileName(): String? = AndroidTransferFileAccess.getLastImportFileName()

actual suspend fun readLastImportFile(): ByteArray? = AndroidTransferFileAccess.readLastImportFile()
