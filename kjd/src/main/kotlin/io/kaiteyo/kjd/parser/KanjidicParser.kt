package io.kaiteyo.kjd.parser

import io.kaiteyo.kjd.source.SourceMetadata
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Raw kanji record extracted from KANJIDIC/kanjidic2 XML.
 *
 * The kanjidic2 format describes each kanji with:
 *   - `<literal>`: the character
 *   - `<reading r_type="ja_on|ja_kun|...">`: readings
 *   - `<meaning>`: meanings (optionally `m_lang` for non-English)
 *   - `<grade>`: school grade (1-8)
 *   - `<freq>`: newspaper frequency rank
 *   - `<stroke_count>`: stroke count
 *   - `<radical rad_number="..">`: primary radical
 *   - `<jlpt>`: JLPT level (legacy field)
 */
data class RawKanjidicCharacter(
    val kanji: String,
    val onReadings: List<String> = emptyList(),
    val kunReadings: List<String> = emptyList(),
    val otherReadings: List<String> = emptyList(),
    val meanings: List<RawMeaning> = emptyList(),
    val grade: Int? = null,
    val jlpt: Int? = null,
    val frequency: Int? = null,
    val strokeCount: Int? = null,
    val radNumber: Int? = null,
    val radicalName: String? = null
)

data class RawMeaning(
    val value: String,
    val language: String = "en"
)

/**
 * Parses a KANJIDIC2 XML file. The document root is `<kanjidic2>` and each
 * record is `<character>`. Fatal structural problems are reported as
 * failures; individual malformed records never abort the whole parse.
 */
class KanjidicParser : SourceParser<RawKanjidicCharacter> {

    override val sourceId: String = "kanjidic"

    override fun parse(file: File, metadata: SourceMetadata): ParseResult<RawKanjidicCharacter> {
        val parsed = mutableListOf<RawKanjidicCharacter>()
        val rejected = mutableListOf<ParseFailure>()

        try {
            val dbf = DocumentBuilderFactory.newInstance().apply {
                setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
                isNamespaceAware = false
            }
            val doc = dbf.newDocumentBuilder().parse(file)
            val characters = doc.getElementsByTagName("character")

            for (i in 0 until characters.length) {
                val node = characters.item(i) as? org.w3c.dom.Element ?: continue
                try {
                    val literal = node.getElementsByTagName("literal").item(0)?.textContent?.trim()
                    if (literal.isNullOrBlank() || literal.length != 1) {
                        rejected.add(ParseFailure(recordId = literal, reason = "Invalid literal"))
                        continue
                    }

                    val on = mutableListOf<String>()
                    val kun = mutableListOf<String>()
                    val other = mutableListOf<String>()
                    val readings = node.getElementsByTagName("reading")
                    for (r in 0 until readings.length) {
                        val readingEl = readings.item(r) as? org.w3c.dom.Element ?: continue
                        val value = readingEl.textContent.trim()
                        if (value.isEmpty()) continue
                        when (readingEl.getAttribute("r_type")) {
                            "ja_on" -> on.add(value)
                            "ja_kun" -> kun.add(value)
                            else -> other.add(value)
                        }
                    }

                    val meanings = mutableListOf<RawMeaning>()
                    val meaningNodes = node.getElementsByTagName("meaning")
                    for (m in 0 until meaningNodes.length) {
                        val meaningEl = meaningNodes.item(m) as? org.w3c.dom.Element ?: continue
                        val lang = meaningEl.getAttribute("m_lang").ifBlank { "en" }
                        val value = meaningEl.textContent.trim()
                        if (value.isNotEmpty()) meanings.add(RawMeaning(value, lang))
                    }

                    val grade = node.getElementsByTagName("grade").item(0)?.textContent?.trim()?.toIntOrNull()
                    val jlpt = node.getElementsByTagName("jlpt").item(0)?.textContent?.trim()?.toIntOrNull()
                    val freq = node.getElementsByTagName("freq").item(0)?.textContent?.trim()?.toIntOrNull()
                    val strokesEl = node.getElementsByTagName("stroke_count").item(0) as? org.w3c.dom.Element
                    val strokeCount = strokesEl?.textContent?.trim()?.toIntOrNull()
                        ?: (node.getElementsByTagName("stroke_count").item(0)?.textContent?.trim()?.toIntOrNull())
                    val radicalEl = node.getElementsByTagName("radical").item(0) as? org.w3c.dom.Element
                    val radNumber = radicalEl?.getAttribute("rad_number")?.toIntOrNull()
                    val radicalName = radicalEl?.textContent?.trim()

                    parsed.add(
                        RawKanjidicCharacter(
                            kanji = literal,
                            onReadings = on,
                            kunReadings = kun,
                            otherReadings = other,
                            meanings = meanings,
                            grade = grade,
                            jlpt = jlpt,
                            frequency = freq,
                            strokeCount = strokeCount,
                            radNumber = radNumber,
                            radicalName = radicalName
                        )
                    )
                } catch (t: Throwable) {
                    rejected.add(ParseFailure(recordId = node.getAttribute("id"), reason = "Record parse failed: ${t.summary()}", exception = t))
                }
            }
        } catch (t: Throwable) {
            rejected.add(ParseFailure(recordId = file.name, reason = "Document parse failed: ${t.summary()}", exception = t))
        }

        return ParseResult(metadata, parsed, rejected)
    }
}
