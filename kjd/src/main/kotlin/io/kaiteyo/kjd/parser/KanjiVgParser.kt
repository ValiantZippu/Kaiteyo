package io.kaiteyo.kjd.parser

import io.kaiteyo.kjd.model.BoundingBox
import io.kaiteyo.kjd.model.EntityId
import io.kaiteyo.kjd.model.Stroke
import io.kaiteyo.kjd.model.StrokeDirection
import io.kaiteyo.kjd.source.SourceMetadata
import java.io.File
import java.util.regex.Pattern
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Raw kanji stroke record extracted from a KanjiVG SVG.
 *
 * KanjiVG files encode each kanji as an SVG with one `<path>` per stroke and
 * a number attribute ("1", "2", ...) giving the stroke order. The coordinate
 * space is 1092x1092. The parser extracts the minimal structured geometry;
 * heavy path analysis (direction, bounding boxes) is derived at a later stage.
 */
data class RawKanjiVgCharacter(
    val kanji: String,
    val strokes: List<RawVgStroke>,
    /** Structural parts extracted from `kvg:element_*` groups (components). */
    val parts: List<String> = emptyList(),
    val radNumber: Int? = null,
    val radical: String? = null
)

data class RawVgStroke(
    val index: Int,
    val path: String,
    val elementId: String
)

/**
 * Parses KanjiVG XML (either a single `<kanji>` document or a `<kanjidic2>`
 *-style bundle is not expected here — KanjiVG ships one SVG per character,
 * typically inside a zip that the caller extracts first).
 *
 * The parser is intentionally forgiving: missing strokes produce a failure
 * record, not a crash.
 */
class KanjiVgParser : SourceParser<RawKanjiVgCharacter> {

    override val sourceId: String = "kanjivg"

    override fun parse(file: File, metadata: SourceMetadata): ParseResult<RawKanjiVgCharacter> {
        val parsed = mutableListOf<RawKanjiVgCharacter>()
        val rejected = mutableListOf<ParseFailure>()

        try {
            val dbf = DocumentBuilderFactory.newInstance().apply {
                setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
                isNamespaceAware = false
            }
            val doc = dbf.newDocumentBuilder().parse(file)
            val kanjiNodes = doc.getElementsByTagName("kanji")

            for (i in 0 until kanjiNodes.length) {
                val node = kanjiNodes.item(i)
                val element = node as? org.w3c.dom.Element ?: continue
                val kanji = element.getAttribute("id")
                    .removePrefix("kvg:")
                    .removePrefix("kanji_")
                    .takeIf { it.isNotBlank() && it.length <= 8 }

                if (kanji == null || kanji.isBlank()) {
                    rejected.add(ParseFailure(recordId = element.getAttribute("id"), reason = "Missing or invalid kanji id"))
                    continue
                }

                val strokes = mutableListOf<RawVgStroke>()
                val paths = element.getElementsByTagName("path")
                for (p in 0 until paths.length) {
                    val pathEl = paths.item(p) as? org.w3c.dom.Element ?: continue
                    val number = pathEl.getAttribute("number")
                    val index = number.toIntOrNull() ?: continue
                    val d = pathEl.getAttribute("d")
                    if (d.isBlank()) continue
                    strokes.add(RawVgStroke(index = index, path = d, elementId = pathEl.getAttribute("id")))
                }

                if (strokes.isEmpty()) {
                    rejected.add(ParseFailure(recordId = kanji, reason = "No numbered stroke paths found"))
                    continue
                }

                strokes.sortBy { it.index }
                parsed.add(
                    RawKanjiVgCharacter(
                        kanji = kanji,
                        strokes = strokes,
                        parts = extractParts(element),
                        radNumber = element.getAttribute("rad_number").toIntOrNull(),
                        radical = element.getAttribute("radical").takeIf { it.isNotBlank() }
                    )
                )
            }
        } catch (t: Throwable) {
            rejected.add(ParseFailure(recordId = file.name, reason = "Document parse failed: ${t.summary()}", exception = t))
        }

        return ParseResult(metadata, parsed, rejected)
    }

    companion object {

        /**
         * KanjiVG marks structural parts with `kvg:element_<char>` group ids
         * (optionally numbered variants like `kvg:element_人1`). Extract the
         * distinct part characters, de-duplicated in document order.
         */
        internal fun extractParts(root: org.w3c.dom.Element): List<String> {
            val parts = LinkedHashSet<String>()
            val groups = root.getElementsByTagName("g")
            for (i in 0 until groups.length) {
                val group = groups.item(i) as? org.w3c.dom.Element ?: continue
                val id = group.getAttribute("id")
                if (!id.startsWith("kvg:element_")) continue
                val raw = id.removePrefix("kvg:element_")
                // Strip variant numbers (kanji never contain ASCII digits).
                val part = raw.trimEnd { it in '0'..'9' }
                if (part.isNotEmpty() && part.length <= 4) parts.add(part)
            }
            return parts.toList()
        }

        /** Valid stroke-order attribute values are integers. */
        private val strokeNumberPattern: Pattern = Pattern.compile("^\\d+$")

        fun isStrokeNumber(value: String): Boolean = strokeNumberPattern.matcher(value).matches()

        /**
         * Convert a raw stroke into a canonical [Stroke], deriving a bounding
         * box from the numeric tokens in the path data. Numeric tokens in
         * KanjiVG are in a 1092x1092 space.
         */
        fun toCanonicalStroke(raw: RawVgStroke, kanjiId: EntityId, sources: List<io.kaiteyo.kjd.model.SourceRef>): Stroke {
            val box = deriveBoundingBox(raw.path)
            return Stroke(
                id = EntityId("stroke:${kanjiId.value}:${raw.index}"),
                index = raw.index,
                characterId = kanjiId,
                path = raw.path,
                boundingBox = box,
                direction = deriveDirection(raw.path),
                sources = sources
            )
        }

        private fun deriveBoundingBox(path: String): BoundingBox? {
            val tokens = PathTokens.extract(path) ?: return null
            val xs = mutableListOf<Float>()
            val ys = mutableListOf<Float>()
            for (token in tokens) {
                val x = token.x ?: continue
                val y = token.y ?: continue
                xs.add(x); ys.add(y)
            }
            if (xs.isEmpty()) return null
            return BoundingBox(
                minX = xs.minOrNull() ?: 0f,
                minY = ys.minOrNull() ?: 0f,
                maxX = xs.maxOrNull() ?: 0f,
                maxY = ys.maxOrNull() ?: 0f
            )
        }

        private fun deriveDirection(path: String): StrokeDirection? {
            val tokens = PathTokens.extract(path) ?: return null
            if (tokens.size < 2) return null
            val first = tokens.first()
            val last = tokens.last()
            val dx = (last.x ?: 0f) - (first.x ?: 0f)
            val dy = (last.y ?: 0f) - (first.y ?: 0f)
            val adx = kotlin.math.abs(dx)
            val ady = kotlin.math.abs(dy)
            return when {
                adx < 1f && dy < 0 -> StrokeDirection.TopToBottom
                adx < 1f && dy > 0 -> StrokeDirection.BottomToTop
                ady < 1f && dx > 0 -> StrokeDirection.LeftToRight
                ady < 1f && dx < 0 -> StrokeDirection.RightToLeft
                adx > ady * 1.5f && dx > 0 -> StrokeDirection.DiagonalDownRight
                adx > ady * 1.5f && dx < 0 -> StrokeDirection.DiagonalDownLeft
                dy < 0 -> StrokeDirection.TopToBottom
                else -> StrokeDirection.Unknown
            }
        }
    }
}

/**
 * Lightweight SVG path tokenizer: pulls every `x,y` numeric pair out of an
 * SVG path `d` attribute. Good enough for bounding-box / direction analysis.
 */
internal object PathTokens {
    private val numberPattern = Pattern.compile("-?\\d+(?:\\.\\d+)?")

    fun extract(d: String): List<PathToken>? {
        val matcher = numberPattern.matcher(d)
        val numbers = mutableListOf<Float>()
        while (matcher.find()) {
            numbers.add(matcher.group().toFloat())
        }
        // Pairs of coordinates. Some paths include commands like M/l/c — those
        // produce extra numbers; we approximate by taking all pairs.
        val tokens = mutableListOf<PathToken>()
        var i = 0
        while (i + 1 < numbers.size) {
            tokens.add(PathToken(numbers[i], numbers[i + 1]))
            i += 2
        }
        return tokens.ifEmpty { null }
    }
}

internal data class PathToken(val x: Float?, val y: Float?)
