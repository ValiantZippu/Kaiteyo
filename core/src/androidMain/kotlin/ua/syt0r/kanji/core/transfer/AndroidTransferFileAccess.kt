package ua.syt0r.kanji.core.transfer

/**
 * Registry that the Android [pickImportFile]/[saveExportFile] actuals
 * delegate to.
 *
 * A top-level suspend function has no Activity to launch the Storage
 * Access Framework intents from, so the Compose host
 * ([ua.syt0r.kanji.presentation.AndroidTransferFilePickerHost], installed
 * from `KaiteyoActivity`) registers its Activity Result launchers here.
 * Until a provider is installed — in previews, tests, or before the host
 * composes — the actuals degrade to null/false exactly like the old stub.
 */
object AndroidTransferFileAccess {

    interface Provider {
        suspend fun pickImportFile(description: String, extensions: Array<out String>): ByteArray?
        suspend fun saveExportFile(
            bytes: ByteArray,
            suggestedName: String,
            description: String,
            extensions: Array<out String>
        ): Boolean
        suspend fun getLastImportFileName(): String?
        suspend fun readLastImportFile(): ByteArray?
    }

    @Volatile
    private var provider: Provider? = null

    fun install(provider: Provider) {
        this.provider = provider
    }

    fun clear() {
        provider = null
    }

    suspend fun pickImportFile(description: String, extensions: Array<out String>): ByteArray? =
        provider?.pickImportFile(description, extensions)

    suspend fun saveExportFile(
        bytes: ByteArray,
        suggestedName: String,
        description: String,
        extensions: Array<out String>
    ): Boolean = provider?.saveExportFile(bytes, suggestedName, description, extensions) ?: false

    suspend fun getLastImportFileName(): String? = provider?.getLastImportFileName()

    suspend fun readLastImportFile(): ByteArray? = provider?.readLastImportFile()
}
