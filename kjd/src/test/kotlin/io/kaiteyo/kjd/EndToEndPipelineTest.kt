package io.kaiteyo.kjd

import io.kaiteyo.kjd.api.JapaneseDatabase
import io.kaiteyo.kjd.pipeline.KjdPipeline
import io.kaiteyo.kjd.pipeline.PipelineConfig
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * End-to-end test: a small fixture tree of raw sources runs through the full
 * pipeline (parse → normalize → resolve → validate → generate) and the
 * resulting database is queried through the public API.
 */
class EndToEndPipelineTest {

    private fun fixtureRoot(): File {
        val root = File(System.getProperty("java.io.tmpdir"), "kjd-fixture-${System.nanoTime()}")
        root.mkdirs()

        // KanjiVG (one SVG).
        File(root, "sources/kanjivg/raw/食.svg").apply {
            parentFile.mkdirs()
            writeText(
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <kanji id="kvg:kanji_食">
                  <g id="kvg:StrokePaths_食">
                    <path d="M47,24c0.7,2.2-0.5,4.9-2,7" id="kvg:1" number="1"/>
                    <path d="M53,30.8c8.9,3.5,22.3,9.4" id="kvg:2" number="2"/>
                    <path d="M49,19c1,1,1.8,2.5,1.8,3.5" id="kvg:3" number="3"/>
                  </g>
                </kanji>
                """.trimIndent()
            )
        }

        // KANJIDIC (one character).
        File(root, "sources/kanjidic/raw/kanjidic2.xml").apply {
            parentFile.mkdirs()
            writeText(
                """
                <?xml version="1.0"?>
                <kanjidic2>
                  <character>
                    <literal>食</literal>
                    <radical><rad_number>184</rad_number></radical>
                    <misc><grade>2</grade><stroke_count>9</stroke_count><jlpt>4</jlpt></misc>
                    <reading r_type="ja_on">ショク</reading>
                    <reading r_type="ja_kun">く.う</reading>
                    <meaning>eat</meaning>
                  </character>
                </kanjidic2>
                """.trimIndent()
            )
        }

        // JMdict (one entry).
        File(root, "sources/jmdict/raw/jmdict.xml").apply {
            parentFile.mkdirs()
            writeText(
                """
                <?xml version="1.0"?>
                <JMdict>
                  <entry>
                    <ent_seq>1000990</ent_seq>
                    <k_ele><keb>食べる</keb></k_ele>
                    <r_ele><reb>たべる</reb></r_ele>
                    <sense><pos>Ichidan verb</pos><gloss>to eat</gloss></sense>
                  </entry>
                </JMdict>
                """.trimIndent()
            )
        }

        // JLPT + frequency.
        File(root, "sources/tanos-jlpt/raw/jlpt-n5-kanji.txt").apply {
            parentFile.mkdirs()
            writeText("食\n水\n山\n")
        }
        File(root, "sources/leeds-frequency/raw/japanese.txt").apply {
            parentFile.mkdirs()
            writeText("1 の\n2 食べる\n")
        }

        root.deleteOnExit()
        return root
    }

    @Test
    fun fullPipelineProducesQueryableDatabase() {
        val root = fixtureRoot()
        val dbFile = File(root, "out/kjd-test.db")
        val reportDir = File(root, "out/report")

        val report = KjdPipeline().run(
            PipelineConfig(
                sourcesDir = root,
                outputDatabase = dbFile,
                outputReportDir = reportDir,
                exportArtifacts = true,
                exportDirectory = File(root, "out")
            )
        )

        assertTrue(dbFile.exists())
        assertTrue(report.kanjiCount >= 1)
        assertTrue(report.vocabularyCount >= 1)

        // Public API queries.
        JapaneseDatabase.open(dbFile).use { db ->
            assertEquals(KjdVersion.SCHEMA_VERSION, db.schemaVersion())
            assertEquals(KjdVersion.GENERATOR_VERSION, db.generatorVersion())

            // Kanji lookup + stroke data.
            val kanji = db.lookupKanji("食")
            assertNotNull(kanji)
            assertEquals("ショク", kanji.onReadings.first().value)
            assertEquals("eat", kanji.meanings.first().value)
            assertTrue(kanji.jlpt.any { it.level == 4 })
            assertEquals(3, db.strokesFor("食").size)

            // Vocabulary lookup + senses + furigana.
            val vocab = db.lookupVocabulary("食べる")
            assertNotNull(vocab)
            assertEquals("たべる", vocab.readings.first().value)
            assertEquals("to eat", vocab.senses.first().glosses.first().value)
            assertTrue(vocab.furigana.any { it.text == "食" && it.reading == "た" })
            assertEquals(5, vocab.jlpt.first().level)

            // Search.
            val byExpression = db.search("食べる")
            assertTrue(byExpression.any { it.displayText == "食べる" })
            val byReading = db.search("たべる")
            assertTrue(byReading.any { it.displayText == "食べる" })
            val byMeaning = db.search("eat")
            assertTrue(byMeaning.isNotEmpty())
        }
    }
}
