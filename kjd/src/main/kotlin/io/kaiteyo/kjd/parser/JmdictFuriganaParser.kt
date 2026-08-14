package io.kaiteyo.kjd.parser

import io.kaiteyo.kjd.model.FuriganaSegment
import io.kaiteyo.kjd.source.SourceMetadata
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Raw furigana record extracted from JmdictFurigana (also known as
 * "JMdictFurigana" / jmdict_furigana).
 *
 * The source provides per-entry, per-reading furigana annotations. The XML
 * (or JSON) format describes the expression and its reading with segment
 * annotations. The canonical [FuriganaSegment] model is:
 *
 *   食べる → 食 → た, べ → (none), る → (none)
 *
 * which consumers render as "食[た]べる" themselves.
 */
data class RawFuriganaRecord(
    val entSeq: Long,
    val expression: String,
    val reading: String,
    val segments: List<FuriganaSegment>
)

/**
 * Parses the JmdictFurigana XML. The XML uses a `<k_ele>/<r_ele>` shape with
 * `<ruby>` groups containing `<rt>` text and `<rb>` base text per segment.
 * A JSON variant (`jmdict_furigana.json`) is handled by [JmdictFuriganaJsonParser].
 */
class JmdictFuriganaParser : SourceParser<RawFuriganaRecord> {

    override val sourceId: String = "jmdict-furigana"

    override fun parse(file: File, metadata: SourceMetadata): ParseResult<RawFuriganaRecord> {
        val parsed = mutableListOf<RawFuriganaRecord>()
        val rejected = mutableListOf<ParseFailure>()

        try {
            val dbf = DocumentBuilderFactory.newInstance().apply {
                setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
                isNamespaceAware = false
            }
            val doc = dbf.newDocumentBuilder().parse(file)
            val entries = doc.getElementsByTagName("entry")

            for (i in 0 until entries.length) {
                val node = entries.item(i) as? org.w3c.dom.Element ?: continue
                try {
                    val entSeq = node.getElementsByTagName("ent_seq").item(0)
                        ?.textContent?.trim()?.toLongOrNull()
                    if (entSeq == null) continue

                    val kEl = node.getElementsByTagName("k_ele").item(0) as? org.w3c.dom.Element
                    val rEl = node.getElementsByTagName("r_ele").item(0) as? org.w3c.dom.Element

                    val expression = kEl?.getElementsByTagName("keb")?.item(0)?.textContent?.trim()
                        ?: rEl?.getElementsByTagName("reb")?.item(0)?.textContent?.trim()
                        ?: ""
                    val reading = rEl?.getElementsByTagName("reb")?.item(0)?.textContent?.trim()
                        ?: expression
                    if (expression.isEmpty()) {
                        rejected.add(ParseFailure(recordId = entSeq.toString(), reason = "Missing expression"))
                        continue
                    }

                    val segments = mutableListOf<FuriganaSegment>()
                    val ruby = kEl ?: rEl
                    if (ruby != null) {
                        val rtNodes = ruby.getElementsByTagName("rt")
                        for (r in 0 until rtNodes.length) {
                            val rt = rtNodes.item(r) as? org.w3c.dom.Element ?: continue
                            val text = rt.textContent.trim()
                            if (text.isEmpty()) continue
                            // Segment with reading (kanji segment).
                            segments.add(FuriganaSegment(text = text, reading = text))
                        }
                    }

                    parsed.add(
                        RawFuriganaRecord(
                            entSeq = entSeq,
                            expression = expression,
                            reading = reading,
                            segments = segments
                        )
                    )
                } catch (t: Throwable) {
                    rejected.add(ParseFailure(recordId = null, reason = "Record parse failed: ${t.summary()}", exception = t))
                }
            }
        } catch (t: Throwable) {
            rejected.add(ParseFailure(recordId = file.name, reason = "Document parse failed: ${t.summary()}", exception = t))
        }

        return ParseResult(metadata, parsed, rejected)
    }

    companion object {
        /**
         * Derive segments from an expression and its full reading, splitting
         * kana from kanji. This is the fallback algorithm used when the source
         * only provides expression + reading (e.g. the JSON variant).
         *
         * 食べる/たべる → [食→た, べ→null, る→null]
         */
        fun deriveSegments(expression: String, reading: String): List<FuriganaSegment> {
            if (expression == reading) {
                return listOf(FuriganaSegment(text = expression, reading = null))
            }
            val segments = mutableListOf<FuriganaSegment>()
            val chars = expression.toCharArray()
            var readingIdx = 0
            var kanaBuffer = StringBuilder()

            fun flushKana() {
                if (kanaBuffer.isNotEmpty()) {
                    segments.add(FuriganaSegment(text = kanaBuffer.toString(), reading = null))
                    kanaBuffer = StringBuilder()
                }
            }

            for (char in chars) {
                if (isKana(char)) {
                    kanaBuffer.append(char)
                    readingIdx++
                } else {
                    flushKana()
                    // Consume the reading up to the next kana in the reading string.
                    val readingLen = if (readingIdx < reading.length) reading.length - readingIdx else 0
                    if (readingLen == 0) continue
                    segments.add(FuriganaSegment(text = char.toString(), reading = reading.substring(readingIdx, readingIdx + readingLen).take(1)))
                    readingIdx++
                }
            }
            flushKana()
            return segments
        }

        private fun isKana(c: Char): Boolean {
            val code = c.code
            return (code in 0x3040..0x309F) || (code in 0x30A0..0x30FF) || c == 'ー'
        }
    }
}
