package ua.syt0r.kanji.desktop.engine.reading

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReadingEngineTest {

    private fun engine(): ReadingEngine = ReadingEngine()

    private fun tempTxt(content: String): File =
        File.createTempFile("kaiteyo-reading-engine", ".txt").apply {
            writeText(content)
            deleteOnExit()
        }

    private val sampleText: String
        get() = """
            学校に行きます。

            朝ごはんを食べました。

            これは三つ目の段落です。
        """.trimIndent()

    // ------------------------------------------------------------
    // Opening
    // ------------------------------------------------------------

    @Test
    fun openFileRegistersAndActivatesDocument() {
        val e = engine()
        val file = tempTxt(sampleText)
        val doc = e.openFile(file)
        assertNotNull(doc)
        assertEquals(3, doc.blockCount)
        assertEquals(doc.id, e.activeDocumentId)
        assertEquals(1, e.documents.size)
        assertTrue(e.documents[0].title.startsWith("kaiteyo-reading-engine"))
    }

    @Test
    fun openFileWithMissingPathSetsError() {
        val e = engine()
        val doc = e.openFile(File("/nonexistent/kaiteyo-reading-file.txt"))
        assertNull(doc)
        assertNotNull(e.lastError)
        assertFalse(e.documents.any { it.sourcePath.contains("nonexistent") })
    }

    @Test
    fun epubFileReportsPlannedError() {
        val e = engine()
        val file = File.createTempFile("book", ".epub").apply { writeText("not really epub") }
        val doc = e.openFile(file)
        assertNull(doc)
        assertNotNull(e.lastError)
        assertTrue(e.lastError!!.contains("planned", ignoreCase = true))
    }

    // ------------------------------------------------------------
    // Position + progress
    // ------------------------------------------------------------

    @Test
    fun setPositionUpdatesProgress() {
        val e = engine()
        val doc = e.openFile(tempTxt(sampleText))!!
        assertEquals(0f, doc.progress)
        e.setPosition(2)
        val updated = e.activeDocument!!
        assertEquals(2, updated.position.blockIndex)
        assertEquals(1f, updated.progress)
    }

    @Test
    fun setPositionByFractionClampsToBlockCount() {
        val e = engine()
        e.openFile(tempTxt(sampleText))
        e.setPositionByFraction(0.5f)
        val doc = e.activeDocument!!
        assertTrue(doc.position.blockIndex in 0 until doc.blockCount)
    }

    // ------------------------------------------------------------
    // Bookmarks
    // ------------------------------------------------------------

    @Test
    fun toggleBookmarkAddsThenRemoves() {
        val e = engine()
        e.openFile(tempTxt(sampleText))
        assertEquals(0, e.activeDocument!!.bookmarkCount)
        e.toggleBookmark(1, label = "Halfway")
        assertEquals(1, e.activeDocument!!.bookmarkCount)
        assertEquals("Halfway", e.activeDocument!!.bookmarks.first().label)
        e.toggleBookmark(1)
        assertEquals(0, e.activeDocument!!.bookmarkCount)
    }

    @Test
    fun removeBookmarkById() {
        val e = engine()
        e.openFile(tempTxt(sampleText))
        e.toggleBookmark(0)
        val id = e.activeDocument!!.bookmarks.first().id
        e.removeBookmark(id)
        assertEquals(0, e.activeDocument!!.bookmarkCount)
    }

    // ------------------------------------------------------------
    // Highlights
    // ------------------------------------------------------------

    @Test
    fun toggleHighlightAddsThenRemoves() {
        val e = engine()
        e.openFile(tempTxt(sampleText))
        e.toggleHighlight(0, 0, 3, "学校")
        assertEquals(1, e.activeDocument!!.highlightCount)
        assertEquals("学校", e.activeDocument!!.highlights.first().text)
        e.toggleHighlight(0, 0, 3, "学校")
        assertEquals(0, e.activeDocument!!.highlightCount)
    }

    @Test
    fun highlightWithInvalidRangeIsIgnored() {
        val e = engine()
        e.openFile(tempTxt(sampleText))
        e.toggleHighlight(0, 5, 5, "")
        assertEquals(0, e.activeDocument!!.highlightCount)
    }

    @Test
    fun highlightsInBlockFiltersByBlock() {
        val e = engine()
        e.openFile(tempTxt(sampleText))
        e.toggleHighlight(0, 0, 2, "学校")
        e.toggleHighlight(2, 0, 2, "これ")
        assertEquals(1, e.highlightsInBlock(0).size)
        assertEquals(1, e.highlightsInBlock(2).size)
        assertEquals(0, e.highlightsInBlock(1).size)
    }

    // ------------------------------------------------------------
    // History
    // ------------------------------------------------------------

    @Test
    fun openingRecordsOneHistoryEntryPerDocument() {
        val e = engine()
        e.openFile(tempTxt(sampleText))
        e.closeDocument()
        e.openFile(tempTxt(sampleText))
        assertEquals(1, e.history.size)
        assertEquals(e.activeDocument!!.id, e.history.first().documentId)
    }

    @Test
    fun historyProgressTracksReading() {
        val e = engine()
        e.openFile(tempTxt(sampleText))
        e.setPosition(2)
        e.updateHistoryProgress()
        assertEquals(1f, e.history.first().maxProgress)
    }

    // ------------------------------------------------------------
    // Removal + clipboard import
    // ------------------------------------------------------------

    @Test
    fun removeDocumentClearsHistory() {
        val e = engine()
        val doc = e.openFile(tempTxt(sampleText))!!
        e.removeDocument(doc.id)
        assertEquals(0, e.documents.size)
        assertEquals(0, e.history.size)
        assertNull(e.activeDocumentId)
    }

    @Test
    fun importClipboardCreatesDocument() {
        val e = engine()
        val doc = e.importClipboard("Pasted text", "クリップボードからの日本語テキストです。")
        assertNotNull(doc)
        assertEquals(1, doc.blockCount)
        assertEquals(doc.id, e.activeDocumentId)
    }

    @Test
    fun closingDocumentKeepsItInLibrary() {
        val e = engine()
        val doc = e.openFile(tempTxt(sampleText))!!
        e.closeDocument()
        assertNull(e.activeDocumentId)
        assertEquals(1, e.documents.size)
        assertEquals(doc.id, e.documents[0].id)
    }
}
