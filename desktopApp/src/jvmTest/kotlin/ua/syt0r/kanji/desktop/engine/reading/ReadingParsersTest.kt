package ua.syt0r.kanji.desktop.engine.reading

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReadingParsersTest {

    // ------------------------------------------------------------
    // Format detection
    // ------------------------------------------------------------

    @Test
    fun detectsKindByExtension() {
        assertEquals(ReadingDocumentKind.Text, ReadingParsers.detectKind("notes.txt"))
        assertEquals(ReadingDocumentKind.Markdown, ReadingParsers.detectKind("readme.md"))
        assertEquals(ReadingDocumentKind.Markdown, ReadingParsers.detectKind("guide.markdown"))
        assertEquals(ReadingDocumentKind.Html, ReadingParsers.detectKind("page.html"))
        assertEquals(ReadingDocumentKind.Html, ReadingParsers.detectKind("page.htm"))
        assertEquals(ReadingDocumentKind.Epub, ReadingParsers.detectKind("book.epub"))
        // Unknown extensions fall back to plain text.
        assertEquals(ReadingDocumentKind.Text, ReadingParsers.detectKind("notes.log"))
    }

    // ------------------------------------------------------------
    // Plain text
    // ------------------------------------------------------------

    @Test
    fun parsesPlainTextParagraphs() {
        val text = """
            これは最初の段落です。日本語で書かれています。

            これは二つ目の段落です。
            長い文は一つの段落にまとめられます。
        """.trimIndent()
        val blocks = ReadingParsers.parseBlocks(text, ReadingDocumentKind.Text)
        assertEquals(2, blocks.size)
        assertEquals(ReadingBlockKind.Paragraph, blocks[0].kind)
        assertTrue(blocks[0].text.contains("最初の段落"))
        // Consecutive lines merge into a single paragraph.
        assertTrue(blocks[1].text.contains("長い文"))
        assertTrue(blocks[1].text.contains("一つの段落にまとめられます"))
    }

    @Test
    fun emptyTextYieldsErrorResult() {
        val result = ReadingParsers.parseText("empty", "   \n  ", ReadingDocumentKind.Text)
        assertFalse(result.isSuccess)
        assertNull(result.document)
        assertNotNull(result.error)
    }

    // ------------------------------------------------------------
    // Markdown
    // ------------------------------------------------------------

    @Test
    fun parsesMarkdownHeadingsListsAndLinks() {
        val md = """
            # はじめに

            [リンク](https://example.com) を含む本文です。

            - 一つ目
            - 二つ目

            > 引用文です。
        """.trimIndent()
        val blocks = ReadingParsers.parseBlocks(md, ReadingDocumentKind.Markdown)
        assertEquals(ReadingBlockKind.Heading, blocks.first { it.kind == ReadingBlockKind.Heading }.kind)
        val heading = blocks.first { it.kind == ReadingBlockKind.Heading }
        assertEquals("はじめに", heading.text)
        // Links are stripped to their label text.
        assertTrue(blocks.any { it.text.contains("リンク") && it.text.contains("を含む本文です") })
        val listItems = blocks.filter { it.kind == ReadingBlockKind.ListItem }
        assertEquals(2, listItems.size)
        assertEquals("一つ目", listItems[0].text)
        val quote = blocks.first { it.kind == ReadingBlockKind.Quote }
        assertEquals("引用文です。", quote.text)
    }

    // ------------------------------------------------------------
    // HTML
    // ------------------------------------------------------------

    @Test
    fun parsesHtmlStripsTagsAndKeepsHeadings() {
        val html = """
            <html>
              <head><title>Ignore me</title></head>
              <body>
                <h1>見出し</h1>
                <p>最初の&lt;p&gt;です。</p>
                <p>二つ目の段落です。</p>
                <script>alert('never rendered')</script>
              </body>
            </html>
        """.trimIndent()
        val blocks = ReadingParsers.parseBlocks(html, ReadingDocumentKind.Html)
        val heading = blocks.first { it.kind == ReadingBlockKind.Heading }
        assertEquals("見出し", heading.text)
        assertTrue(blocks.none { it.text.contains("alert") })
        assertTrue(blocks.none { it.text.contains("Ignore me") })
        // Entities are decoded: &lt;p&gt; → <p>.
        assertTrue(blocks.any { it.text.contains("<p>です") })
        // Two paragraphs stay separate.
        val paragraphs = blocks.filter { it.kind == ReadingBlockKind.Paragraph }
        assertTrue(paragraphs.size >= 2)
    }

    @Test
    fun htmlCommentsAndStyleAreDropped() {
        val html = """
            <style>.fancy { color: red; }</style>
            <!-- hidden note -->
            <p>表示されるテキスト</p>
        """.trimIndent()
        val blocks = ReadingParsers.parseBlocks(html, ReadingDocumentKind.Html)
        assertTrue(blocks.none { it.text.contains("fancy") })
        assertTrue(blocks.none { it.text.contains("hidden note") })
        assertTrue(blocks.any { it.text.contains("表示されるテキスト") })
    }

    // ------------------------------------------------------------
    // EPUB + file-based parse
    // ------------------------------------------------------------

    @Test
    fun epubReturnsPlannedError() {
        val result = ReadingParsers.parseText("book", "some epub bytes", ReadingDocumentKind.Epub)
        assertFalse(result.isSuccess)
        assertNotNull(result.error)
        assertTrue(result.error!!.contains("planned", ignoreCase = true))
    }

    @Test
    fun parseFileBuildsDocumentWithKindAndBlocks() {
        val file = File.createTempFile("kaiteyo-reading-test", ".md").apply {
            writeText("# Title\n\nSome Japanese text 日本語です。\n\n- item")
            deleteOnExit()
        }
        val result = ReadingParsers.parse(file, file.readText())
        assertTrue(result.isSuccess)
        val doc = result.document
        assertNotNull(doc)
        assertEquals(ReadingDocumentKind.Markdown, doc.kind)
        assertEquals("Title", doc.title)
        assertEquals(file.absolutePath, doc.sourcePath)
        assertTrue(doc.blockCount > 0)
    }
}
