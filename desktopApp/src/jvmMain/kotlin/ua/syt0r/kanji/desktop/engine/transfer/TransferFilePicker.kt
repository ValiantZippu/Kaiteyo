package ua.syt0r.kanji.desktop.engine.transfer

import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

// ============================================
// NATIVE FILE PICKER
// Thin wrappers around Swing's JFileChooser so the
// transfer views can save to / load from real files
// instead of only the clipboard.
// ============================================

object TransferFilePicker {

    fun save(bytes: ByteArray, fileName: String, description: String, vararg extensions: String): Boolean {
        val chooser = JFileChooser()
        chooser.configureFilter(description, extensions)
        chooser.selectedFile = File(fileName)
        if (chooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) return false

        var target = chooser.selectedFile ?: return false
        val primaryExtension = extensions.firstOrNull()?.removePrefix(".")
        if (!primaryExtension.isNullOrBlank() && !target.name.endsWith(".$primaryExtension", ignoreCase = true)) {
            target = File(target.absolutePath + ".$primaryExtension")
        }
        return runCatching { target.writeBytes(bytes) }.isSuccess
    }

    fun open(description: String, vararg extensions: String): ByteArray? {
        val chooser = JFileChooser()
        chooser.configureFilter(description, extensions)
        if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return null
        val file = chooser.selectedFile ?: return null
        return runCatching { file.readBytes() }.getOrNull()
    }

    private fun JFileChooser.configureFilter(description: String, extensions: Array<out String>) {
        val accepted = extensions.map { it.removePrefix(".") }.toTypedArray()
        if (accepted.isEmpty()) return
        fileFilter = FileNameExtensionFilter(description, *accepted)
    }
}
