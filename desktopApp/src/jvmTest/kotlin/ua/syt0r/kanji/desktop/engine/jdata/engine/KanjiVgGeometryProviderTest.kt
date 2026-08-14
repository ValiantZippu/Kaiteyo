package ua.syt0r.kanji.desktop.engine.jdata.engine

import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Tests for [KanjiVgGeometryProvider] against real on-disk fixtures:
 * hex-codepoint files (kanji/), kana files (kana/), literal-name layout,
 * the aggregate kanjivg.xml document, stroke ordering, the stroke-number
 * glyph skip and hard-failure handling (malformed XML must not throw).
 */
class KanjiVgGeometryProviderTest {

    private lateinit var root: File

    @BeforeTest
    fun setUp() {
        root = Files.createTempDirectory("kanjivg-test").toFile()
    }

    @AfterTest
    fun tearDown() {
        root.deleteRecursively()
    }

    private fun write(relative: String, content: String) {
        val file = File(root, relative)
        file.parentFile.mkdirs()
        file.writeText(content)
    }

    // 食 = U+98DF, 一 = U+4E00, あ = U+3042, 邪 = U+90AA
    private val svgOpen = "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 1092 1092\">"

    @Test
    fun perFileHexLayoutOrdersByNumberAttributeAndSkipsNumberGlyphs() {
        write(
            "kanji/98df.svg",
            """
            $svgOpen
              <g id="kvg:StrokeNumbers_98df" style="font-size:20px"><text x="100" y="100">1</text></g>
              <path id="kvg:98df-s2" number="2" d="M 200 300 L 800 300"/>
              <path id="kvg:98df-s1" number="1" d="M 300 200 L 300 800"/>
            </svg>
            """.trimIndent()
        )
        val provider = KanjiVgGeometryProvider(root)
        val strokes = provider.strokesFor("食")
        assertEquals(2, strokes.size)
        assertTrue(strokes[0].startsWith("M 300"), "stroke 1 should be the number=1 path: ${strokes[0]}")
        assertTrue(strokes[1].startsWith("M 200"), "stroke 2 should be the number=2 path: ${strokes[1]}")
        assertTrue(provider.has("食"))
    }

    @Test
    fun perFileLayoutFallsBackToPathIdSuffixWithoutNumberAttribute() {
        write(
            "kanji/4e00.svg",
            """
            $svgOpen
              <path id="kvg:4e00-s2" d="M 0 0 L 100 0"/>
              <path id="kvg:4e00-s1" d="M 0 0 L 0 100"/>
            </svg>
            """.trimIndent()
        )
        val provider = KanjiVgGeometryProvider(root)
        val strokes = provider.strokesFor("一")
        assertEquals(2, strokes.size)
        assertTrue(strokes[0].contains("0 100"), "s1 first: ${strokes[0]}")
    }

    @Test
    fun kanaFolderResolvesHexCodepoints() {
        write(
            "kana/3042.svg",
            """
            $svgOpen
              <path number="1" d="M 100 100 L 900 100"/>
              <path number="2" d="M 500 100 L 500 900"/>
            </svg>
            """.trimIndent()
        )
        val provider = KanjiVgGeometryProvider(root)
        val strokes = provider.strokesFor("あ")
        assertEquals(2, strokes.size)
    }

    @Test
    fun literalNameLayoutResolves() {
        write(
            "kanji/食.svg",
            """
            $svgOpen
              <path number="1" d="M 50 50 L 950 50"/>
            </svg>
            """.trimIndent()
        )
        val provider = KanjiVgGeometryProvider(root)
        val strokes = provider.strokesFor("食")
        assertEquals(1, strokes.size)
    }

    @Test
    fun aggregateDocumentIsFilteredPerCharacterAndOrdered() {
        write(
            "kanjivg.xml",
            """
            <kanjivg>
              <kanji id="kvg:kanji_4e00">
                <g id="kvg:4e00-g1"><path number="2" d="M 100 100 L 900 100"/></g>
                <g id="kvg:4e00-g2"><path number="1" d="M 100 500 L 900 500"/></g>
              </kanji>
              <kanji id="kvg:kanji_98df">
                <g id="kvg:98df-g1"><path number="1" d="M 50 50 L 500 50"/></g>
              </kanji>
            </kanjivg>
            """.trimIndent()
        )
        val provider = KanjiVgGeometryProvider(root)

        val ichi = provider.strokesFor("一")
        assertEquals(2, ichi.size)
        assertTrue(ichi[0].contains("100 500"), "number=1 stroke first: ${ichi[0]}")

        val syoku = provider.strokesFor("食")
        assertEquals(1, syoku.size)
        assertTrue(syoku[0].startsWith("M 50"))
    }

    @Test
    fun missingCharacterReturnsEmptyAndIsCached() {
        write(
            "kanjivg.xml",
            "<kanjivg><kanji id=\"kvg:kanji_4e00\"><path number=\"1\" d=\"M 0 0 L 100 0\"/></kanji></kanjivg>"
        )
        val provider = KanjiVgGeometryProvider(root)
        // 邪 = U+90AA is not in the aggregate and has no per-file document.
        assertTrue(provider.strokesFor("邪").isEmpty())
        assertTrue(provider.strokesFor("邪").isEmpty()) // cached empty result
        assertTrue(!provider.has("邪"))
    }

    @Test
    fun malformedXmlDoesNotThrow() {
        write("kanji/4e00.svg", "this is definitely not svg <<< >")
        val provider = KanjiVgGeometryProvider(root)
        assertEquals(emptyList(), provider.strokesFor("一"))
    }

    @Test
    fun nonDirectoryRootIsRejected() {
        val file = File(root, "not-a-dir.txt").apply { writeText("x") }
        assertFailsWith<IllegalArgumentException> { KanjiVgGeometryProvider(file) }
    }
}
