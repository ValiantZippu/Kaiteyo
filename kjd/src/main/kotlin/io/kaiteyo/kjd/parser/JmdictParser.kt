package io.kaiteyo.kjd.parser

import io.kaiteyo.kjd.source.SourceMetadata
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Raw vocabulary record extracted from JMdict XML.
 *
 * JMdict entries have a stable `<ent_seq>` and contain:
 *   - `<k_ele>`: kanji (expression) elements with `<keb>` + restrictions
 *   - `<r_ele>`: reading elements with `<reb>` (+ `no_kanji` marker, restrictions)
 *   - `<sense>`: senses with `<pos>` (parts of speech), `<gloss>`,
 *     `<field>`, `<misc>`, `<stagr>/<stagk>` (restrictions), `<dial>`
 */
data class RawJmdictEntry(
    val entSeq: Long,
    val kanji: List<RawJmdictKanjiElement> = emptyList(),
    val readings: List<RawJmdictReadingElement> = emptyList(),
    val senses: List<RawJmdictSense> = emptyList()
)

data class RawJmdictKanjiElement(
    val keb: String,
    val info: List<String> = emptyList(),
    val priority: List<String> = emptyList()
)

data class RawJmdictReadingElement(
    val reb: String,
    val noKanji: Boolean = false,
    val info: List<String> = emptyList(),
    val priority: List<String> = emptyList()
)

data class RawJmdictSense(
    val pos: List<String> = emptyList(),
    val field: List<String> = emptyList(),
    val misc: List<String> = emptyList(),
    val dialect: List<String> = emptyList(),
    val glosses: List<RawJmdictGloss> = emptyList(),
    val stagk: List<String> = emptyList(),
    val stagr: List<String> = emptyList(),
    val crossReference: List<String> = emptyList(),
    val antReference: List<String> = emptyList(),
    val example: List<String> = emptyList()
)

data class RawJmdictGloss(
    val value: String,
    val language: String = "en"
)

/**
 * Parses a JMdict XML file. Robust to missing optional fields; an entry with
 * neither kanji nor reading elements is rejected as unparseable.
 */
class JmdictParser : SourceParser<RawJmdictEntry> {

    override val sourceId: String = "jmdict"

    override fun parse(file: File, metadata: SourceMetadata): ParseResult<RawJmdictEntry> {
        val parsed = mutableListOf<RawJmdictEntry>()
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
                    if (entSeq == null) {
                        rejected.add(ParseFailure(recordId = null, reason = "Missing ent_seq"))
                        continue
                    }

                    val kanji = mutableListOf<RawJmdictKanjiElement>()
                    val kElems = node.getElementsByTagName("k_ele")
                    for (k in 0 until kElems.length) {
                        val kEl = kElems.item(k) as? org.w3c.dom.Element ?: continue
                        val keb = kEl.getElementsByTagName("keb").item(0)?.textContent?.trim().orEmpty()
                        if (keb.isEmpty()) continue
                        kanji.add(
                            RawJmdictKanjiElement(
                                keb = keb,
                                info = kEl.childTexts("ke_inf"),
                                priority = kEl.childTexts("ke_pri")
                            )
                        )
                    }

                    val readings = mutableListOf<RawJmdictReadingElement>()
                    val rElems = node.getElementsByTagName("r_ele")
                    for (r in 0 until rElems.length) {
                        val rEl = rElems.item(r) as? org.w3c.dom.Element ?: continue
                        val reb = rEl.getElementsByTagName("reb").item(0)?.textContent?.trim().orEmpty()
                        if (reb.isEmpty()) continue
                        readings.add(
                            RawJmdictReadingElement(
                                reb = reb,
                                noKanji = rEl.getElementsByTagName("no_kanji").length > 0,
                                info = rEl.childTexts("re_inf"),
                                priority = rEl.childTexts("re_pri")
                            )
                        )
                    }

                    if (kanji.isEmpty() && readings.isEmpty()) {
                        rejected.add(ParseFailure(recordId = entSeq.toString(), reason = "No kanji or reading elements"))
                        continue
                    }

                    val senses = mutableListOf<RawJmdictSense>()
                    val senseNodes = node.getElementsByTagName("sense")
                    for (s in 0 until senseNodes.length) {
                        val senseEl = senseNodes.item(s) as? org.w3c.dom.Element ?: continue
                        val glosses = senseEl.childElements("gloss").mapNotNull { glossEl ->
                            val value = glossEl.textContent.trim()
                            if (value.isEmpty()) null
                            else RawJmdictGloss(
                                value = value,
                                language = glossEl.getAttribute("xml:lang").ifBlank { "en" }
                            )
                        }
                        senses.add(
                            RawJmdictSense(
                                pos = senseEl.childTexts("pos"),
                                field = senseEl.childTexts("field"),
                                misc = senseEl.childTexts("misc"),
                                dialect = senseEl.childTexts("dial"),
                                glosses = glosses,
                                stagk = senseEl.childTexts("stagk"),
                                stagr = senseEl.childTexts("stagr"),
                                crossReference = senseEl.childTexts("xref"),
                                antReference = senseEl.childTexts("ant"),
                                example = senseEl.childTexts("example")
                            )
                        )
                    }

                    parsed.add(
                        RawJmdictEntry(
                            entSeq = entSeq,
                            kanji = kanji,
                            readings = readings,
                            senses = senses
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
}

/** Helper: collect the trimmed text content of every direct child with a tag name. */
private fun org.w3c.dom.Element.childTexts(tag: String): List<String> {
    val result = mutableListOf<String>()
    val nodes = getElementsByTagName(tag)
    for (i in 0 until nodes.length) {
        val text = nodes.item(i).textContent.trim()
        if (text.isNotEmpty()) result.add(text)
    }
    return result
}

/** Helper: collect every direct child element with a tag name. */
private fun org.w3c.dom.Element.childElements(tag: String): List<org.w3c.dom.Element> {
    val result = mutableListOf<org.w3c.dom.Element>()
    val nodes = getElementsByTagName(tag)
    for (i in 0 until nodes.length) {
        val el = nodes.item(i) as? org.w3c.dom.Element ?: continue
        result.add(el)
    }
    return result
}
