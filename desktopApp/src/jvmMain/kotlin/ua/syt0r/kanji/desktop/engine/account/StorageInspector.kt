package ua.syt0r.kanji.desktop.engine.account

import java.io.File

// ============================================
// STORAGE INSPECTOR
// Walks ~/.kaiteyo and buckets every directory
// into a user-facing storage category so the
// account dashboard can show a real, live
// breakdown of what Kaiteyo keeps on disk.
// ============================================

object StorageInspector {

    /** Folder name (lowercase) → display category. */
    private val FOLDER_CATEGORY = listOf(
        "library" to "Database",
        "collections" to "Database",
        "cards" to "Database",
        "database" to "Database",
        "user-data" to "Database",
        "dictionary" to "Dictionaries",
        "plugins" to "Plugins",
        "themes" to "Themes",
        "backups" to "Backups",
        "account" to "Account data",
        "media" to "Media",
        "images" to "Media",
        "audio" to "Media",
        "screenshots" to "Media",
        "mining" to "Media",
        "ocr" to "Media",
        "cache" to "Cache",
        "logs" to "Logs"
    )

    private val CATEGORY_ORDER = listOf(
        "Database", "Media", "Dictionaries", "Plugins", "Themes",
        "Backups", "Cache", "Account data", "Logs", "Other"
    )

    fun inspect(root: File): StorageBreakdown {
        if (!root.exists()) return StorageBreakdown(emptyList(), 0L)

        val filesByCategory = mutableMapOf<String, MutableList<File>>()
        val looseFiles = mutableListOf<File>()

        root.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                val label = FOLDER_CATEGORY.firstOrNull { it.first == file.name.lowercase() }?.second ?: "Other"
                filesByCategory.getOrPut(label) { mutableListOf() }.add(file)
            } else {
                looseFiles.add(file)
            }
        }

        val categories = filesByCategory.map { (label, dirs) ->
            StorageCategory(
                key = label.lowercase().replace(' ', '-'),
                label = label,
                bytes = dirs.sumOf { dirSize(it) },
                fileCount = dirs.sumOf { fileCount(it) }
            )
        }.toMutableList()

        if (looseFiles.isNotEmpty()) {
            categories.add(
                StorageCategory(
                    key = "other",
                    label = "Other",
                    bytes = looseFiles.sumOf { it.length() },
                    fileCount = looseFiles.size
                )
            )
        }

        val sorted = categories.sortedBy { CATEGORY_ORDER.indexOf(it.label).let { idx -> if (idx < 0) Int.MAX_VALUE else idx } }
        return StorageBreakdown(
            categories = sorted,
            totalBytes = sorted.sumOf { it.bytes }
        )
    }

    fun dirSize(dir: File): Long =
        if (dir.exists()) dir.walkTopDown().filter { it.isFile }.sumOf { it.length() } else 0L

    fun fileCount(dir: File): Int =
        if (dir.exists()) dir.walkTopDown().filter { it.isFile }.count() else 0

    fun formatBytes(bytes: Long): String = when {
        bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
        bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
        bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
        else -> "$bytes B"
    }
}
