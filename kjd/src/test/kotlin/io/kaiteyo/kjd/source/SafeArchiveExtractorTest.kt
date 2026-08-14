package io.kaiteyo.kjd.source

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Verifies [SafeArchiveExtractor] against malicious and well-formed archives:
 * traversal entries, absolute paths, backslash traversal, oversized entries
 * and nested directory layouts.
 */
class SafeArchiveExtractorTest {

    private fun zipWith(vararg entries: Pair<String, String>, bytes: Map<String, ByteArray> = emptyMap()): File {
        val zip = File.createTempFile("kjd-zip-", ".zip")
        zip.deleteOnExit()
        ZipOutputStream(zip.outputStream()).use { out ->
            entries.forEach { (name, content) ->
                out.putNextEntry(ZipEntry(name))
                out.write(content.toByteArray())
                out.closeEntry()
            }
            bytes.forEach { (name, content) ->
                out.putNextEntry(ZipEntry(name))
                out.write(content)
                out.closeEntry()
            }
        }
        return zip
    }

    @Test
    fun extractsWellFormedEntries() {
        val zip = zipWith("a.xml" to "<a/>", "dir/b.svg" to "<svg/>", "dir/deep/c.txt" to "text")
        val dest = File.createTempDir()
        dest.deleteOnExit()

        val result = SafeArchiveExtractor.extractZip(zip, dest)

        assertTrue(File(dest, "a.xml").isFile)
        assertTrue(File(dest, "dir/b.svg").isFile)
        assertTrue(File(dest, "dir/deep/c.txt").isFile)
        assertEquals(3, result.extracted.size)
        assertTrue(result.rejected.isEmpty())
        dest.deleteRecursively()
    }

    @Test
    fun rejectsPathTraversal() {
        val zip = zipWith("../evil.xml" to "<evil/>", "good.xml" to "<good/>")
        val dest = File.createTempDir()
        dest.deleteOnExit()

        val result = SafeArchiveExtractor.extractZip(zip, dest)

        assertTrue(File(dest, "good.xml").isFile)
        assertFalse(File(dest, "evil.xml").exists())
        assertFalse(File(dest.parentFile, "evil.xml").exists(), "must not write outside destination")
        assertTrue(result.rejected.any { it.name == "../evil.xml" })
        dest.deleteRecursively()
    }

    @Test
    fun rejectsAbsolutePaths() {
        val zip = zipWith("/etc/passwd" to "root", "ok.txt" to "fine")
        val dest = File.createTempDir()
        dest.deleteOnExit()

        val result = SafeArchiveExtractor.extractZip(zip, dest)

        assertTrue(File(dest, "ok.txt").isFile)
        assertFalse(File("/etc/passwd").readText().contains("root"), "must never overwrite an absolute path")
        assertTrue(result.rejected.any { it.name == "/etc/passwd" })
        dest.deleteRecursively()
    }

    @Test
    fun rejectsBackslashTraversal() {
        val zip = zipWith("..\\evil.txt" to "bad", "safe.txt" to "ok")
        val dest = File.createTempDir()
        dest.deleteOnExit()

        val result = SafeArchiveExtractor.extractZip(zip, dest)

        assertTrue(File(dest, "safe.txt").isFile)
        assertFalse(File(dest, "evil.txt").exists())
        assertFalse(File(dest.parentFile, "evil.txt").exists())
        assertTrue(result.rejected.any { it.name == "..\\evil.txt" })
        dest.deleteRecursively()
    }

    @Test
    fun rejectsOversizedEntries() {
        val zip = zipWith(bytes = mapOf("big.bin" to ByteArray(1024)))
        val dest = File.createTempDir()
        dest.deleteOnExit()

        val result = SafeArchiveExtractor.extractZip(zip, dest, maxEntryBytes = 512)

        assertFalse(File(dest, "big.bin").exists())
        assertTrue(result.rejected.any { it.name == "big.bin" && it.reason.contains("size limit") })
        dest.deleteRecursively()
    }

    @Test
    fun nestedDirectoryEntryIsSkipped() {
        val zip = zipWith("folder/" to "", "folder/file.txt" to "x")
        val dest = File.createTempDir()
        dest.deleteOnExit()

        val result = SafeArchiveExtractor.extractZip(zip, dest)

        // Only the real file is extracted; the bare directory entry is skipped.
        assertTrue(File(dest, "folder/file.txt").isFile)
        assertEquals(listOf("folder/file.txt"), result.extracted)
        dest.deleteRecursively()
    }
}

private fun File.createTempDir(): File {
    val dir = File.createTempFile("kjd-dest-", "")
    dir.delete()
    dir.mkdirs()
    return dir
}
