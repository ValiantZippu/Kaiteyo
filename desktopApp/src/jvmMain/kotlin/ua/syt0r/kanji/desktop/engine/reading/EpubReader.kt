package ua.syt0r.kanji.desktop.engine.reading

import java.io.File
import java.io.InputStream
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Document
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import java.io.StringReader
import java.io.StringWriter
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

// ============================================
// KAITEYO EPUB READER ENGINE
// Parses EPUB 2/3 archives: extracts the OPF
// manifest, spine order, and converts each XHTML
// content document into ReadingBlocks. Pure JVM,
// no external dependencies beyond the JDK.
// ============================================

/** Parsed metadata from the EPUB's OPF file. */
data class EpubMetadata(
    val title: String = "",
    val creator: String = "",
    val language: String = "",
    val publisher: String = "",
    val description: String = "",
    val uniqueIdentifier: String = ""
)

/** A single chapter extracted from the EPUB spine. */
data class EpubChapter(
    val index: Int,
    val href: String,
    val title: String,
    val blocks: List<ReadingBlock>
)

/** Full EPUB parse result. */
data class EpubBook(
    val metadata: EpubMetadata,
    val chapters: List<EpubChapter>,
    val totalBlocks: Int
)

object EpubReader {

    private val xmlFactory = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = false
        isIgnoringElementContentWhitespace = true
    }

    // ----------------------------------------------------------
    // Public API
    // ----------------------------------------------------------

    /** Parse an EPUB file into an [EpubBook]. */
    fun parse(file: File): EpubBook {
        if (!file.exists()) error("File not found: ${file.absolutePath}")
        return parse { ZipFile(file) }
    }

    /** Parse from an input stream (e.g. drag-and-drop). */
    fun parse(stream: InputStream): EpubBook {
        return parse {
            val tmp = File.createTempFile("kaiteyo-epub", ".epub")
            tmp.deleteOnExit()
            tmp.outputStream().use { out -> stream.copyTo(out) }
            ZipFile(tmp)
        }
    }

    // ----------------------------------------------------------
    // Internal: ZIP → OPF → spine → XHTML → blocks
    // ----------------------------------------------------------

    private fun parse(openZip: () -> ZipFile): EpubBook {
        val zip = openZip()
        try {
            val containerXml = readEntry(zip, "META-INF/container.xml")
                ?: error("Not a valid EPUB: missing container.xml")
            val opfPath = parseContainerXml(containerXml)
                ?: error("Not a valid EPUB: no rootfile in container.xml")

            val opfDir = opfPath.substringBeforeLast('/', "")
            val opfXml = readEntry(zip, opfPath)
                ?: error("Cannot read OPF: $opfPath")

            val opfDoc = parseXml(opfXml)
            val metadata = parseOpfMetadata(opfDoc)
            val manifest = parseOpfManifest(opfDoc)
            val spine = parseOpfSpine(opfDoc)

            val chapters = spine.mapIndexed { index, itemHref ->
                val resolvedHref = resolveHref(opfDir, itemHref)
                val xhtml = readEntry(zip, resolvedHref)
                val blocks = if (xhtml != null) {
                    xhtmlToBlocks(xhtml)
                } else {
                    listOf(ReadingBlock(index, ReadingBlockKind.Paragraph, "[Content not available]"))
                }
                val title = extractChapterTitle(xhtml)
                EpubChapter(index, itemHref, title, blocks)
            }

            val totalBlocks = chapters.sumOf { it.blocks.size }
            return EpubBook(metadata, chapters, totalBlocks)
        } finally {
            zip.close()
        }
    }

    // ----------------------------------------------------------
    // container.xml → OPF path
    // ----------------------------------------------------------

    private fun parseContainerXml(xml: String): String? {
        val doc = parseXml(xml)
        val rootfiles = doc.getElementsByTagName("rootfile")
        for (i in 0 until rootfiles.length) {
            val node = rootfiles.item(i)
            if (node.attributes?.getNamedItem("media-type")?.nodeValue
                    ?.contains("oebps-package") == true ||
                node.attributes?.getNamedItem("media-type")?.nodeValue
                    ?.contains("opendocument") == true
            ) {
                return node.attributes?.getNamedItem("full-path")?.nodeValue
            }
        }
        // Fallback: just find any rootfile
        for (i in 0 until rootfiles.length) {
            return rootfiles.item(i).attributes?.getNamedItem("full-path")?.nodeValue
        }
        return null
    }

    // ----------------------------------------------------------
    // OPF parsing
    // ----------------------------------------------------------

    private fun parseOpfMetadata(doc: Document): EpubMetadata {
        val getText = { tagName: String ->
            val nodes = doc.getElementsByTagName(tagName)
            if (nodes.length > 0) nodes.item(0).textContent?.trim() ?: "" else ""
        }
        return EpubMetadata(
            title = getText("dc:title").ifBlank { getText("title") },
            creator = getText("dc:creator").ifBlank { getText("creator") },
            language = getText("dc:language").ifBlank { getText("language") },
            publisher = getText("dc:publisher").ifBlank { getText("publisher") },
            description = getText("dc:description").ifBlank { getText("description") },
            uniqueIdentifier = getText("dc:identifier").ifBlank { getText("identifier") }
        )
    }

    /** href → media-type map from <manifest>. */
    private fun parseOpfManifest(doc: Document): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val items = doc.getElementsByTagName("item")
        for (i in 0 until items.length) {
            val node = items.item(i)
            val href = node.attributes?.getNamedItem("href")?.nodeValue ?: continue
            val mediaType = node.attributes?.getNamedItem("media-type")?.nodeValue ?: "application/octet-stream"
            map[href] = mediaType
        }
        return map
    }

    /** Ordered list of item hrefs from <spine>. */
    private fun parseOpfSpine(doc: Document): List<String> {
        val result = mutableListOf<String>()
        val spineRefs = doc.getElementsByTagName("itemref")
        val manifest = parseOpfManifest(doc)
        // We need to map idref → href via manifest items
        val idToHref = mutableMapOf<String, String>()
        val items = doc.getElementsByTagName("item")
        for (i in 0 until items.length) {
            val node = items.item(i)
            val id = node.attributes?.getNamedItem("id")?.nodeValue ?: continue
            val href = node.attributes?.getNamedItem("href")?.nodeValue ?: continue
            idToHref[id] = href
        }
        for (i in 0 until spineRefs.length) {
            val idref = spineRefs.item(i).attributes?.getNamedItem("idref")?.nodeValue ?: continue
            val href = idToHref[idref] ?: idref
            result.add(href)
        }
        return result
    }

    // ----------------------------------------------------------
    // XHTML → ReadingBlock conversion
    // ----------------------------------------------------------

    fun xhtmlToBlocks(xhtml: String): List<ReadingBlock> {
        // Strip the XML declaration and doctype for cleaner parsing
        val cleaned = xhtml
            .replace(Regex("<\\?xml[^>]*\\?>"), "")
            .replace(Regex("<!DOCTYPE[^>]*>"), "")

        val doc = parseXml(cleaned)
        val body = doc.getElementsByTagName("body")
        if (body.length == 0) {
            // Fallback: strip all tags from the full document
            return textToBlocks(stripAllTags(cleaned))
        }

        val blocks = mutableListOf<ReadingBlock>()
        extractBlocksFromNode(body.item(0), blocks)
        return blocks
    }

    private fun extractBlocksFromNode(node: Node, blocks: MutableList<ReadingBlock>) {
        if (node.nodeType != Node.ELEMENT_NODE && node.nodeType != Node.DOCUMENT_NODE) return

        val tag = node.localName?.lowercase() ?: node.nodeName.lowercase() ?: return

        when (tag) {
            "h1", "h2", "h3", "h4", "h5", "h6" -> {
                val text = node.textContent?.trim() ?: ""
                if (text.isNotBlank()) {
                    blocks.add(ReadingBlock(blocks.size, ReadingBlockKind.Heading, text))
                }
            }
            "p" -> {
                val text = node.textContent?.trim() ?: ""
                if (text.isNotBlank()) {
                    blocks.add(ReadingBlock(blocks.size, ReadingBlockKind.Paragraph, text))
                }
            }
            "li" -> {
                val text = node.textContent?.trim() ?: ""
                if (text.isNotBlank()) {
                    blocks.add(ReadingBlock(blocks.size, ReadingBlockKind.ListItem, text))
                }
            }
            "blockquote" -> {
                val text = node.textContent?.trim() ?: ""
                if (text.isNotBlank()) {
                    blocks.add(ReadingBlock(blocks.size, ReadingBlockKind.Quote, text))
                }
            }
            "pre", "code" -> {
                val text = node.textContent ?: ""
                if (text.isNotBlank()) {
                    blocks.add(ReadingBlock(blocks.size, ReadingBlockKind.Code, text.trim()))
                }
            }
            "hr" -> {
                blocks.add(ReadingBlock(blocks.size, ReadingBlockKind.Divider, "———"))
            }
            "br" -> {
                // Only add a break if not already followed by a block element
            }
            else -> {
                // Recurse into child elements
                val children = node.childNodes
                for (i in 0 until children.length) {
                    extractBlocksFromNode(children.item(i), blocks)
                }
            }
        }
    }

    // ----------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------

    private fun extractChapterTitle(xhtml: String?): String {
        if (xhtml == null) return ""
        // Look for first heading tag
        val headingMatch = Regex("<h[1-6][^>]*>(.*?)</h[1-6]>", RegexOption.DOT_MATCHES_ALL)
            .find(xhtml) ?: return ""
        return stripAllTags(headingMatch.groupValues[1]).trim()
    }

    private fun readEntry(zip: ZipFile, path: String): String? {
        return try {
            val entry = zip.getEntry(path) ?: zip.entries().asSequence().firstOrNull {
                it.name.equals(path, ignoreCase = true) || it.name.endsWith("/$path")
            } ?: return null
            zip.getInputStream(entry).bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            null
        }
    }

    private fun resolveHref(baseDir: String, href: String): String {
        if (baseDir.isBlank()) return href
        if (href.startsWith("/")) return href.removePrefix("/")
        return "$baseDir/$href"
    }

    private fun parseXml(xml: String): Document {
        return xmlFactory.newDocumentBuilder().parse(
            InputSource(StringReader(xml))
        )
    }

    private fun textToBlocks(text: String): List<ReadingBlock> {
        val blocks = mutableListOf<ReadingBlock>()
        var buffer = StringBuilder()
        fun flush() {
            val t = buffer.toString().trim()
            if (t.isNotBlank()) {
                blocks.add(ReadingBlock(blocks.size, ReadingBlockKind.Paragraph, t))
            }
            buffer = StringBuilder()
        }
        text.lines().forEach { line ->
            if (line.isBlank()) flush()
            else {
                if (buffer.isNotEmpty()) buffer.append(' ')
                buffer.append(line.trim())
            }
        }
        flush()
        return blocks
    }

    private fun stripAllTags(html: String): String = html
        .replace(Regex("<[^>]*>"), " ")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace(Regex("\\s+"), " ")
        .trim()
}
