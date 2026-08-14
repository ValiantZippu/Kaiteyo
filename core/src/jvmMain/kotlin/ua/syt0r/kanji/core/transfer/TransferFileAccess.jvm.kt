package ua.syt0r.kanji.core.transfer

import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

actual suspend fun pickImportFile(description: String, vararg extensions: String): ByteArray? {
    val chooser = JFileChooser()
    chooser.fileFilter = FileNameExtensionFilter(description, *extensions.map { it.removePrefix(".") }.toTypedArray())
    if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return null
    val file = chooser.selectedFile ?: return null
    return runCatching { file.readBytes() }.getOrNull()
}

actual suspend fun saveExportFile(
    bytes: ByteArray,
    suggestedName: String,
    description: String,
    vararg extensions: String
): Boolean {
    val chooser = JFileChooser()
    chooser.fileFilter = FileNameExtensionFilter(description, *extensions.map { it.removePrefix(".") }.toTypedArray())
    chooser.selectedFile = File(suggestedName)
    if (chooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) return false

    var target = chooser.selectedFile ?: return false
    val primaryExtension = extensions.firstOrNull()?.removePrefix(".")
    if (!primaryExtension.isNullOrBlank() && !target.name.endsWith(".$primaryExtension", ignoreCase = true)) {
        target = File(target.absolutePath + ".$primaryExtension")
    }
    return runCatching { target.writeBytes(bytes) }.isSuccess
}

// The desktop chooser does not remember the last picked file, so the
// picker-free re-import shortcut is not offered on the JVM.
actual suspend fun getLastImportFileName(): String? = null

actual suspend fun readLastImportFile(): ByteArray? = null
