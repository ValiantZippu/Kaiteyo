package io.kaiteyo.kjd.parser

import io.kaiteyo.kjd.source.BuiltinSources
import io.kaiteyo.kjd.source.SourceIds
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ParsersTest {

    private fun tempFile(name: String, content: String): File {
        val dir = File(System.getProperty("java.io.tmpdir"), "kjd-test-${System.nanoTime()}")
        dir.mkdirs()
        val file = File(dir, name)
        file.writeText(content)
        file.deleteOnExit()
        return file
    }

    @Test
    fun kanjidicParsesReadingsMeaningsAndGrade() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <kanjidic2>
              <header><file_version>1</file_version></header>
              <character>
                <literal>食</literal>
                <codepoint><cp_value cp_type="ucs">98DF</cp_value></codepoint>
                <radical><rad_number>184</rad_number><rad_type>classical</rad_type></radical>
                <misc><grade>2</grade><stroke_count>9</stroke_count><freq>79</freq><jlpt>4</jlpt></misc>
                <reading r_type="ja_on">ショク</reading>
                <reading r_type="ja_on">ジキ</reading>
                <reading r_type="ja_kun">く.う</reading>
                <meaning>eat</meaning>
                <meaning m_lang="es">comer</meaning>
              </character>
            </kanjidic2>
        """.trimIndent()
        val result = KanjidicParser().parse(tempFile("kanjidic2.xml", xml), BuiltinSources.byId(SourceIds.KANJIDIC))

        assertEquals(1, result.parsed.size)
        val kanji = result.parsed.first()
        assertEquals("食", kanji.kanji)
        assertEquals(listOf("ショク", "ジキ"), kanji.onReadings)
        assertEquals(listOf("く.う"), kanji.kunReadings)
        assertEquals(2, kanji.grade)
        assertEquals(9, kanji.strokeCount)
        assertEquals(79, kanji.frequency)
        assertEquals(4, kanji.jlpt)
        assertEquals(184, kanji.radNumber)
        assertEquals("eat", kanji.meanings.first { it.language == "en" }.value)
        assertEquals("comer", kanji.meanings.first { it.language == "es" }.value)
    }

    @Test
    fun jmdictParsesEntriesSensesAndGlosses() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <JMdict>
              <entry>
                <ent_seq>1000990</ent_seq>
                <k_ele><keb>食べる</keb><ke_pri>ichi1</ke_pri></k_ele>
                <r_ele><reb>たべる</reb><re_pri>ichi1</re_pri></r_ele>
                <sense>
                  <pos>Ichidan verb</pos>
                  <gloss>to eat</gloss>
                  <gloss xml:lang="de">essen</gloss>
                </sense>
                <sense>
                  <pos>Ichidan verb</pos>
                  <gloss>to live on (food)</gloss>
                </sense>
              </entry>
            </JMdict>
        """.trimIndent()
        val result = JmdictParser().parse(tempFile("jmdict.xml", xml), BuiltinSources.byId(SourceIds.JMDICT))

        assertEquals(1, result.parsed.size)
        val entry = result.parsed.first()
        assertEquals(1000990L, entry.entSeq)
        assertEquals(listOf("食べる"), entry.kanji.map { it.keb })
        assertEquals(listOf("たべる"), entry.readings.map { it.reb })
        assertEquals(2, entry.senses.size)
        assertEquals(listOf("Ichidan verb"), entry.senses.first().pos)
        assertEquals("to eat", entry.senses.first().glosses.first { it.language == "en" }.value)
        assertEquals("essen", entry.senses.first().glosses.first { it.language == "de" }.value)
    }

    @Test
    fun jmdictFuriganaParserDerivesSegments() {
        val segments = JmdictFuriganaParser.deriveSegments("食べる", "たべる")
        // 食 → た, べ → (no annotation), る → (no annotation)
        assertTrue(segments.isNotEmpty())
        val kanjiSegment = segments.firstOrNull { it.text == "食" }
        assertNotNull(kanjiSegment)
        assertEquals("た", kanjiSegment.reading)
    }

    @Test
    fun kanjiVgParsesStrokePaths() {
        val svg = """
            <?xml version="1.0" encoding="UTF-8"?>
            <kanji id="kvg:kanji_食">
              <g id="kvg:StrokePaths_食" style="fill:none;stroke:#000000;stroke-width:5;stroke-linecap:round;stroke-linejoin:round;">
                <path d="M47,24c0.7,2.2-0.5,4.9-2,7C37.5,43.2,27,58.7,13.3,73" id="kvg:1" number="1"/>
                <path d="M53,30.8c8.9,3.5,22.3,9.4,27.8,15.1c2.1,2.1,2.8,3.7,2.4,7.3" id="kvg:2" number="2"/>
              </g>
            </kanji>
        """.trimIndent()
        val result = KanjiVgParser().parse(tempFile("食.svg", svg), BuiltinSources.byId(SourceIds.KANJIVG))

        assertEquals(1, result.parsed.size)
        val character = result.parsed.first()
        assertEquals("食", character.kanji)
        assertEquals(2, character.strokes.size)
        assertEquals(listOf(1, 2), character.strokes.map { it.index })
        assertTrue(character.strokes.all { it.path.startsWith("M") })
    }

    @Test
    fun tanosJlptParsesLevels() {
        val content = "n5\n食\n水\n山"
        val result = TanosJlptParser().parse(tempFile("jlpt-n5-kanji.txt", content), BuiltinSources.byId(SourceIds.TANOS_JLPT))
        assertEquals(3, result.parsed.size)
        assertTrue(result.parsed.all { it.level == 5 })
        assertEquals("食", result.parsed.first().item)
    }

    @Test
    fun leedsFrequencyParsesRanks() {
        val content = "1 の\n2 に\n3 する"
        val result = LeedsFrequencyParser().parse(tempFile("japanese.txt", content), BuiltinSources.byId(SourceIds.LEEDS_FREQUENCY))
        assertEquals(3, result.parsed.size)
        assertEquals(1, result.parsed.first().rank)
        assertEquals("の", result.parsed.first().item)
    }

    @Test
    fun yomichanJlptVocabParsesTerms() {
        val json = """[["食べる","たべる",["jlpt-n5","v1"],"to eat"]]"""
        val result = YomichanJlptVocabParser().parse(tempFile("jlpt-vocab.json", json), BuiltinSources.byId(SourceIds.YOMICHAN_JLPT_VOCAB))
        assertEquals(1, result.parsed.size)
        assertEquals("食べる", result.parsed.first().expression)
        assertEquals(5, result.parsed.first().level)
    }
}
