package io.kaiteyo.kjd.source

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * Result of a safe archive extraction.
 */
data class ExtractionResult(
    /** Files actually written (relative paths). */
    val extracted: List<String>,
    /** Entries skipped because they were unsafe or exceeded limits. */
    val rejected: List<RejectedEntry>
) {
    val isEmpty: Boolean get() = extracted.isEmpty()
}

data class RejectedEntry(
    val name: String,
    val reason: String
)

/**
 * Safe ZIP extraction for untrusted archives (source downloads, user imports).
 *
 * Protection guarantees:
 *   - rejects path traversal (`..`) and absolute paths,
 *   - rejects symlink entries (extracted archives must never create links),
 *   - rejects Windows drive prefixes and backslash path separators,
 *   - enforces per-entry and total size limits,
 *   - never follows or creates anything outside [destination].
 *
 * Extraction failures are collected per entry — one malicious entry never
 * aborts the whole import.
 */
object SafeArchiveExtractor {

    /** Defaults: 1 GiB per archive, 256 MiB per entry, 100k entries. */
    const val DEFAULT_MAX_TOTAL_BYTES: Long = 1L shl 30
    const val DEFAULT_MAX_ENTRY_BYTES: Long = 256L shl 20
    const val DEFAULT_MAX_ENTRIES: Int = 100_000

    fun extractZip(
        archive: File,
        destination: File,
        maxTotalBytes: Long = DEFAULT_MAX_TOTAL_BYTES,
        maxEntryBytes: Long = DEFAULT_MAX_ENTRY_BYTES,
        maxEntries: Int = DEFAULT_MAX_ENTRIES
    ): ExtractionResult {
        val root = destination.toPath().toAbsolutePath().normalize()
        Files.createDirectories(root)

        val extracted = mutableListOf<String>()
        val rejected = mutableListOf<RejectedEntry>()
        var totalBytes = 0L
        var entryCount = 0

        ZipFile(archive).use { zip ->
            for (entry in zip.entries()) {
                if (entryCount >= maxEntries) {
                    rejected.add(RejectedEntry(entry.name, "entry count exceeds limit $maxEntries"))
                    break
                }
                entryCount++

                val relative = safeRelativeName(entry)
                if (relative == null) {
                    rejected.add(RejectedEntry(entry.name, "unsafe path (traversal/absolute)"))
                    continue
                }
                if (isSymlink(entry)) {
                    rejected.add(RejectedEntry(entry.name, "symlink entries are not allowed"))
                    continue
                }
                if (entry.isDirectory) {
                    // Create only parent directories of real files below.
                    continue
                }
                if (entry.size > maxEntryBytes) {
                    rejected.add(RejectedEntry(entry.name, "entry exceeds size limit $maxEntryBytes bytes"))
                    continue
                }
                if (totalBytes + entry.size > maxTotalBytes) {
                    rejected.add(RejectedEntry(entry.name, "archive exceeds total size limit $maxTotalBytes bytes"))
                    continue
                }

                val target = root.resolve(relative).normalize()
                if (!target.startsWith(root)) {
                    rejected.add(RejectedEntry(entry.name, "escapes destination directory"))
                    continue
                }

                target.parent?.let { Files.createDirectories(it) }
                try {
                    zip.getInputStream(entry).use { input ->
                        Files.newOutputStream(target).use { output -> input.copyTo(output) }
                    }
                    totalBytes += entry.size
                    extracted.add(relative)
                } catch (t: IOException) {
                    rejected.add(RejectedEntry(entry.name, "write failed: ${t.message ?: t.javaClass.simpleName}"))
                }
            }
        }
        return ExtractionResult(extracted, rejected)
    }

    /** Validates an entry name and returns a safe relative name, or null. */
    private fun safeRelativeName(entry: ZipEntry): String? {
        val name = entry.name.replace('\\', '/')
        if (name.isEmpty()) return null
        if (name.startsWith("/")) return null // absolute path
        if (name.length >= 2 && name[1] == ':') return null // Windows drive prefix
        val parts = name.split('/')
        if (parts.any { it == ".." }) return null // path traversal
        // Reject NUL bytes and control characters that confuse filesystems.
        if (name.any { it.code == 0 || it.code < 0x20 }) return null
        return parts.filter { it.isNotEmpty() }.joinToString("/")
    }

    /** Zip entries can carry Unix mode bits; reject anything that is a symlink. */
    private fun isSymlink(entry: ZipEntry): Boolean {
        val mode = entry.extra?.let { findUnixMode(it) } ?: 0
        return mode and 0xF000 == 0xA000 // S_IFLNK
    }

    private fun findUnixMode(extra: ByteArray): Int {
        // Look for the Unix extra field (0x7875) containing uid/gid/mode.
        var i = 0
        while (i + 3 < extra.size) {
            val id = (extra[i].toInt() and 0xFF) or ((extra[i + 1].toInt() and 0xFF) shl 8)
            val size = (extra[i + 2].toInt() and 0xFF) or ((extra[i + 3].toInt() and 0xFF) shl 8)
            if (id == 0x7875 && i + 4 + size <= extra.size) {
                // Unix extra: version(1) uidSize(1) uid uidSize gid gidSize mode(2)
                val data = extra.copyOfRange(i + 4, i + 4 + size)
                if (data.size >= 6) {
                    val uidSize = data[1].toInt() and 0xFF
                    val gidSize = data[2 + uidSize].toInt() and 0xFF
                    val modeOffset = 3 + uidSize + gidSize
                    if (modeOffset + 1 < data.size) {
                        return (data[modeOffset].toInt() and 0xFF) or
                            ((data[modeOffset + 1].toInt() and 0xFF) shl 8)
                    }
                }
            }
            i += 4 + size
        }
        return 0
    }
}
