package ua.syt0r.kanji.desktop.engine.reading

import java.io.File

// ============================================
// KAITEYO READING ENGINE — PARSERS
// Normalize TXT / Markdown / HTML / EPUB sources into
// a flat list of ReadingBlocks the reader can render.
// Each parser is a pure function from raw text to blocks;
// parsing is synchronous and cheap enough for typical
// local documents. EPUB is fully supported via EpubReader.
// ============================================

object ReadingParsers {

    /** Detect the document kind from a file name (extension wins). */
    fun detectKind(fileName: String): ReadingDocumentKind = when {
        fileName.endsWith(".md", ignoreCase = true) ||
            fileName.endsWith(".markdown", ignoreCase = true) -> ReadingDocumentKind.Markdown
        fileName.endsWith(".html", ignoreCase = true) ||
            fileName.endsWith(".htm", ignoreCase = true) -> ReadingDocumentKind.Html
        fileName.endsWith(".epub", ignoreCase = true) -> ReadingDocumentKind.Epub
        else -> ReadingDocumentKind.Text
    }

    /**
     * Parse a file's contents into blocks. Returns an error result when the
     * format is recognized but unsupported (EPUB) or the file is empty.
     */
    fun parse(file: File, content: String): ReadingOpenResult {
        val kind = detectKind(file.name)

        // EPUB: delegate to EpubReader which parses the ZIP archive directly
        if (kind == ReadingDocumentKind.Epub) {
            return parseEpub(file)
        }

        if (content.isBlank()) {
            return ReadingOpenResult(error = "The file is empty.")
        }
        val blocks = parseBlocks(content, kind)
        if (blocks.isEmpty()) {
            return ReadingOpenResult(error = "No readable content found in this file.")
        }
        return ReadingOpenResult(
            document = ReadingDocument(
                id = stableDocumentId(file),
                title = documentTitle(file.name),
                sourcePath = file.absolutePath,
                kind = kind,
                sizeBytes = file.length(),
                blockCount = blocks.size
            )
        )
    }

    /**
     * Parse an EPUB file using EpubReader. Flattens all chapters into a
     * single list of ReadingBlocks (headings + paragraphs + lists), which
     * the reader can render with chapter headings as section separators.
     */
    private fun parseEpub(file: File): ReadingOpenResult {
        return runCatching {
            val book = EpubReader.parse(file)
            if (book.totalBlocks == 0) {
                return ReadingOpenResult(error = "No readable content found in EPUB.")
            }
            // Flatten all chapters into a single block list, inserting
            // chapter titles as Heading blocks for navigation.
            val blocks = mutableListOf<ReadingBlock>()
            for (chapter in book.chapters) {
                if (chapter.title.isNotBlank()) {
                    blocks.add(ReadingBlock(blocks.size, ReadingBlockKind.Heading, chapter.title))
                }
                for (block in chapter.blocks) {
                    blocks.add(block.copy(index = blocks.size))
                }
            }
            ReadingOpenResult(
                document = ReadingDocument(
                    id = stableDocumentId(file),
                    title = book.metadata.title.ifBlank { documentTitle(file.name) },
                    sourcePath = file.absolutePath,
                    kind = ReadingDocumentKind.Epub,
                    sizeBytes = file.length(),
                    blockCount = blocks.size
                )
            )
        }.getOrElse { e ->
            ReadingOpenResult(error = "Failed to parse EPUB: ${e.message}")
        }
    }

    /** Parse raw text without a file (used by tests / clipboard import). */
    fun parseText(title: String, content: String, kind: ReadingDocumentKind = ReadingDocumentKind.Text): ReadingOpenResult {
        if (content.isBlank()) return ReadingOpenResult(error = "The text is empty.")
        if (kind == ReadingDocumentKind.Epub) return ReadingOpenResult(error = "EPUB requires a file path — use parse(file, content) instead.")
        val blocks = parseBlocks(content, kind)
        if (blocks.isEmpty()) return ReadingOpenResult(error = "No readable content found.")
        return ReadingOpenResult(
            document = ReadingDocument(
                id = "doc-${title.hashCode().toUInt().toString(16)}-${content.hashCode().toUInt().toString(16)}",
                title = title,
                sourcePath = "",
                kind = kind,
                blockCount = blocks.size
            )
        )
    }

    /** Parse content into blocks for a given kind (the reader consumes this). */
    fun parseBlocks(content: String, kind: ReadingDocumentKind): List<ReadingBlock> = when (kind) {
        ReadingDocumentKind.Epub -> emptyList() // Handled by parseEpub(); never reached here
        ReadingDocumentKind.Markdown -> parseMarkdown(content)
        ReadingDocumentKind.Html -> parseHtml(content)
        ReadingDocumentKind.Text -> parsePlainText(content)
    }

    // ------------------------------------------------------------
    // Plain text
    // ------------------------------------------------------------

    fun parsePlainText(content: String): List<ReadingBlock> {
        val blocks = mutableListOf<ReadingBlock>()
        var buffer = StringBuilder()
        fun flush() {
            val text = buffer.toString().trim()
            if (text.isNotEmpty()) {
                blocks.add(ReadingBlock(blocks.size, ReadingBlockKind.Paragraph, text))
            }
            buffer = StringBuilder()
        }
        content.lines().forEach { raw ->
            val line = raw.trimEnd()
            when {
                line.isBlank() -> flush()
                else -> {
                    if (buffer.isNotEmpty()) buffer.append(' ')
                    buffer.append(line.trim())
                }
            }
        }
        flush()
        return blocks
    }

    // ------------------------------------------------------------
    // Markdown (lightweight: headings, lists, quotes, code, links)
    // ------------------------------------------------------------

    fun parseMarkdown(content: String): List<ReadingBlock> {
        val blocks = mutableListOf<ReadingBlock>()
        var inCodeFence = false
        content.lines().forEach { raw ->
            val line = raw.trimEnd()
            when {
                line.trimStart().startsWith("```") -> {
                    inCodeFence = !inCodeFence
                    if (inCodeFence) {
                        blocks.add(ReadingBlock(blocks.size, ReadingBlockKind.Code, "```"))
                    } else {
                        blocks.add(ReadingBlock(blocks.size, ReadingBlockKind.Code, "```"))
                    }
                }

                inCodeFence -> {
                    blocks.add(ReadingBlock(blocks.size, ReadingBlockKind.Code, line))
                }

                line.isBlank() -> {
                    // Skip — paragraphs accumulate below.
                }

                line.trimStart().startsWith("#") -> {
                    val heading = line.trimStart().trimStart('#').trim()
                    if (heading.isNotEmpty()) {
                        blocks.add(ReadingBlock(blocks.size, ReadingBlockKind.Heading, stripInlineMarkdown(heading)))
                    }
                }

                line.trimStart().startsWith(">") -> {
                    val quote = line.trimStart().trimStart('>').trim()
                    if (quote.isNotEmpty()) {
                        blocks.add(ReadingBlock(blocks.size, ReadingBlockKind.Quote, stripInlineMarkdown(quote)))
                    }
                }

                line.trimStart().matches(Regex("[-_*]{3,}")) -> {
                    blocks.add(ReadingBlock(blocks.size, ReadingBlockKind.Divider, "———"))
                }

                line.trimStart().startsWith("-") -> {
                    val item = line.trimStart().removePrefix("-").trim()
                    if (item.isNotEmpty()) {
                        blocks.add(ReadingBlock(blocks.size, ReadingBlockKind.ListItem, stripInlineMarkdown(item)))
                    }
                }

                line.trimStart().startsWith("*") && !line.trimStart().startsWith("**") -> {
                    val item = line.trimStart().removePrefix("*").trim()
                    if (item.isNotEmpty()) {
                        blocks.add(ReadingBlock(blocks.size, ReadingBlockKind.ListItem, stripInlineMarkdown(item)))
                    }
                }

                else -> {
                    val text = stripInlineMarkdown(line.trim())
                    if (text.isNotEmpty()) {
                        // Merge consecutive paragraph lines into one block.
                        val last = blocks.lastOrNull()
                        if (last != null && last.kind == ReadingBlockKind.Paragraph && last.text.isNotBlank()) {
                            blocks[blocks.lastIndex] = last.copy(text = "${last.text} $text")
                        } else {
                            blocks.add(ReadingBlock(blocks.size, ReadingBlockKind.Paragraph, text))
                        }
                    }
                }
            }
        }
        return blocks
    }

    // ------------------------------------------------------------
    // HTML (tag-stripping reader extraction)
    // ------------------------------------------------------------

    fun parseHtml(content: String): List<ReadingBlock> {
        // Drop scripts/styles/comments — they are never reading content.
        var html = content
            .replace(Regex("(?is)<(script|style|head|noscript|template)[^>]*>.*?</\\1>"), " ")
            .replace(Regex("(?s)<!--.*?-->"), " ")

        val blocks = mutableListOf<ReadingBlock>()
        // Block-level tags become paragraph breaks.
        html = html.replace(Regex("(?i)</(p|div|section|article|li|blockquote|h[1-6]|br|tr)>"), "\n")
        html = html.replace(Regex("(?i)<(p|div|section|article|li|blockquote|h[1-6]|tr)[^>]*>"), "\n")

        // Headings keep their block kind.
        val headingRegex = Regex("(?i)<h([1-6])[^>]*>(.*?)</h\\1>")
        html = headingRegex.replace(html) { match ->
            "\n[[H]] ${stripHtml(match.groupValues[2])} [[/H]]\n"
        }

        html.lines().forEach { raw ->
            val line = raw.trim()
            if (line.isBlank()) return@forEach
            when {
                line.startsWith("[[H]]") && line.endsWith("[[/H]]") -> {
                    val heading = line.removePrefix("[[H]]").removeSuffix("[[/H]]").trim()
                    if (heading.isNotEmpty()) {
                        blocks.add(ReadingBlock(blocks.size, ReadingBlockKind.Heading, stripHtml(heading)))
                    }
                }

                line.trimStart().startsWith("&bull;") || line.trimStart().startsWith("•") -> {
                    val item = stripHtml(line.trimStart().removePrefix("&bull;").removePrefix("•").trim())
                    if (item.isNotEmpty()) {
                        blocks.add(ReadingBlock(blocks.size, ReadingBlockKind.ListItem, item))
                    }
                }

                else -> {
                    val text = stripHtml(line)
                    if (text.isNotEmpty()) {
                        val last = blocks.lastOrNull()
                        if (last != null && last.kind == ReadingBlockKind.Paragraph && last.text.isNotBlank()) {
                            blocks[blocks.lastIndex] = last.copy(text = "${last.text} $text")
                        } else {
                            blocks.add(ReadingBlock(blocks.size, ReadingBlockKind.Paragraph, text))
                        }
                    }
                }
            }
        }
        return blocks
    }

    // ------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------

    private fun stripHtml(raw: String): String = raw
        .replace(Regex("<[^>]*>"), "")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&mdash;", "—")
        .replace("&ndash;", "–")
        .replace("&hellip;", "…")
        .trim()

    private fun stripInlineMarkdown(raw: String): String = raw
        // Links: [text](url) → text
        .replace(Regex("\\[([^\\]]*)\\]\\([^)]*\\)"), "$1")
        // Images: ![alt](url) → alt
        .replace(Regex("!\\[([^\\]]*)\\]\\([^)]*\\)"), "$1")
        // Inline code / emphasis markers.
        .replace("`", "")
        .replace("**", "")
        .replace("__", "")
        .replace(Regex("(?m)^\\s*#+\\s*"), "")
        .trim()

    /** Stable id for a file: path hash + size + mtime so content changes re-key. */
    fun stableDocumentId(file: File): String =
        "doc-${file.absolutePath.hashCode().toUInt().toString(16)}-${file.length().hashCode().toUInt().toString(16)}"

    /** Human title from a file name, extension stripped. */
    fun documentTitle(fileName: String): String {
        val base = fileName.substringBeforeLast('.')
        return base.replace('_', ' ').replace('-', ' ').trim().ifBlank { fileName }
    }
}
