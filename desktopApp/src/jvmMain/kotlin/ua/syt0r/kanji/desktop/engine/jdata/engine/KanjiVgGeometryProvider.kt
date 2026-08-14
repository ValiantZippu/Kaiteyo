package ua.syt0r.kanji.desktop.engine.jdata.engine

import ua.syt0r.kanji.desktop.engine.jdata.source.SourceDefinition
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.w3c.dom.Node

// ============================================================
// KANJIVG GEOMETRY PROVIDER
// Supplies real per-stroke SVG path data from a KanjiVG dataset
// directory (https://kanjivg.tagaini.net/, CC BY-SA 3.0). The
// dataset is NOT bundled with the app — the user (or a build
// step) provides an extracted copy:
//
//   <root>/kanji/4e00.svg      standard KanjiVG layout (hex codepoint)
//   <root>/kana/3042.svg       kana folder (hex codepoint)
//   <root>/kanji/食.svg        literal-name layout (used by some tools)
//   <root>/kanjivg.xml         aggregate document with <kanji> elements
//
// Stroke order comes from the SVG itself (the `number` attribute or
// the `-sN` suffix on path ids — both emitted by KanjiVG). Parsing is
// hardened against untrusted XML (DOCTYPE disallowed, external entities
// off, secure processing on). Results are cached per character; the
// aggregate document is parsed exactly once.
// ============================================================

object KanjiVgSource {
    const val SourceId = "kanjivg"

    /** License metadata — KanjiVG is CC BY-SA 3.0, never claimed as Kaiteyo's own. */
    val Definition = SourceDefinition(
        id = SourceId,
        name = "KanjiVG",
        version = "",
        homepage = "https://kanjivg.tagaini.net/",
        licenseName = "CC BY-SA 3.0",
        licenseUrl = "https://creativecommons.org/licenses/by-sa/3.0/",
        retrievalDate = "",
        format = "svg",
        priority = 500,
        tags = listOf("license:CC BY-SA 3.0", "homepage:https://kanjivg.tagaini.net/")
    )
}

class KanjiVgGeometryProvider(
    private val rootDirectory: File
) : StrokeGeometryProvider {

    private val cache = ConcurrentHashMap<String, List<String>>()
    private val aggregateCache = ConcurrentHashMap<String, Map<String, List<String>>>()

    init {
        require(rootDirectory.isDirectory) {
            "KanjiVG root is not a directory: ${rootDirectory.absolutePath}"
        }
    }

    /** Stroke path data in stroke order for [character]; empty when unavailable. */
    override fun strokesFor(character: String): List<String> =
        cache.computeIfAbsent(character) {
            val file = resolveFile(character) ?: return@computeIfAbsent emptyList()
            if (file.name.equals("kanjivg.xml", ignoreCase = true)) {
                parseAggregate(file)[keyFor(character)] ?: emptyList()
            } else {
                parseSingleSvg(file)
            }
        }

    /** True when a KanjiVG document for the character is present on disk. */
    fun has(character: String): Boolean = strokesFor(character).isNotEmpty()

    val sourceId: String get() = KanjiVgSource.SourceId

    // ------------------------------------------------------------
    // File resolution
    // ------------------------------------------------------------

    private fun resolveFile(character: String): File? {
        val hex = character[0].code.toString(16)
        val hexPadded4 = hex.padStart(4, '0')
        val hexPadded5 = hex.padStart(5, '0')
        val candidates = listOf(
            File(rootDirectory, "kanji/$hex.svg"),
            File(rootDirectory, "kanji/$hexPadded4.svg"),
            File(rootDirectory, "kanji/$hexPadded5.svg"),
            File(rootDirectory, "kana/$hex.svg"),
            File(rootDirectory, "kana/$hexPadded4.svg"),
            File(rootDirectory, "kanji/${character}.svg"),
            File(rootDirectory, "kana/${character}.svg"),
            File(rootDirectory, "$hex.svg"),
            File(rootDirectory, "$hexPadded4.svg"),
            File(rootDirectory, "${character}.svg")
        )
        return candidates.firstOrNull { it.isFile }
            ?: File(rootDirectory, "kanjivg.xml").takeIf { it.isFile }
    }

    /** Aggregate lookup key: hex codepoint, or the literal for ID-style names. */
    private fun keyFor(character: String): String = character[0].code.toString(16)

    // ------------------------------------------------------------
    // Parsing — single-character SVGs
    // ------------------------------------------------------------

    private fun parseSingleSvg(file: File): List<String> {
        val dbf = secureFactory() ?: return emptyList()
        return try {
            val doc = dbf.newDocumentBuilder().parse(file)
            val paths = doc.getElementsByTagName("path")
            val strokes = mutableListOf<Pair<Int, String>>()
            for (i in 0 until paths.length) {
                val el = paths.item(i) as? Element ?: continue
                if (isNumberGlyph(el)) continue
                val d = el.getAttribute("d").trim()
                if (d.isEmpty()) continue
                strokes.add(strokeIndex(el, strokes.size) to d)
            }
            strokes.sortedBy { it.first }.map { it.second }
        } catch (t: Throwable) {
            // A single malformed SVG must not take down the whole provider.
            emptyList()
        }
    }

    // ------------------------------------------------------------
    // Parsing — aggregate <kanji> document (parsed exactly once)
    // ------------------------------------------------------------

    private fun parseAggregate(file: File): Map<String, List<String>> =
        aggregateCache.computeIfAbsent(file.absolutePath) {
            val dbf = secureFactory() ?: return@computeIfAbsent emptyMap()
            val result = mutableMapOf<String, List<String>>()
            try {
                val doc = dbf.newDocumentBuilder().parse(file)
                val kanjiNodes = doc.getElementsByTagName("kanji")
                for (i in 0 until kanjiNodes.length) {
                    val el = kanjiNodes.item(i) as? Element ?: continue
                    val rawId = el.getAttribute("id")
                    val char = rawId.removePrefix("kvg:").removePrefix("kanji_").trim()
                    if (char.isEmpty()) continue
                    val strokes = mutableListOf<Pair<Int, String>>()
                    val paths = el.getElementsByTagName("path")
                    for (p in 0 until paths.length) {
                        val pathEl = paths.item(p) as? Element ?: continue
                        val d = pathEl.getAttribute("d").trim()
                        if (d.isEmpty()) continue
                        strokes.add(strokeIndex(pathEl, strokes.size) to d)
                    }
                    if (strokes.isNotEmpty()) {
                        val ordered = strokes.sortedBy { it.first }.map { it.second }
                        result[char.lowercase()] = ordered
                        // Hex key only for literal ids (length 1): for hex-string ids
                        // like "4e00", char.code would be the code of '4' ("34"), wrong.
                        if (char.length == 1) result[char[0].code.toString(16)] = ordered
                    }
                }
            } catch (t: Throwable) {
                result.clear()
            }
            result
        }

    // ------------------------------------------------------------
    // Shared helpers
    // ------------------------------------------------------------

    private fun secureFactory(): DocumentBuilderFactory? =
        try {
            DocumentBuilderFactory.newInstance().apply {
                setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
                setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
                setFeature("http://xml.org/sax/features/external-general-entities", false)
                setFeature("http://xml.org/sax/features/external-parameter-entities", false)
                isXIncludeAware = false
                isExpandEntityReferences = false
            }
        } catch (t: Throwable) {
            null
        }

    private fun strokeIndex(element: Element, position: Int): Int {
        element.getAttribute("number").toIntOrNull()?.let { return it }
        val idSuffix = Regex("-s(\\d+)").find(element.getAttribute("id"))?.groupValues?.get(1)
        idSuffix?.toIntOrNull()?.let { return it }
        return position
    }

    /** KanjiVG renders the little stroke-order *number* glyphs as paths too — skip them. */
    private fun isNumberGlyph(element: Element): Boolean {
        var node: Node? = element
        while (node != null) {
            if (node is Element && node.getAttribute("id").contains("StrokeNumbers")) return true
            node = node.parentNode
        }
        return false
    }
}
