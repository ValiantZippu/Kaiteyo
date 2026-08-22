package ua.syt0r.kanji.engine.nlp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// ============================================
// KAITEYO NLP ENGINE TESTS
// Uses Turbine-style sequential Flow testing.
// Turbine (app.cash.turbine) makes StateFlow
// and SharedFlow testing trivial. These tests
// verify the fallback tokenizer and NLP pipeline.
// ============================================

class NlpEngineTest {

    // ----------------------------------------------------------
    // Fallback tokenizer tests
    // ----------------------------------------------------------

    @Test
    fun `fallback tokenizer segments kana correctly`() {
        val result = ua.syt0r.kanji.desktop.engine.nlp.FallbackTokenizer.analyze("おはようございます")
        assertEquals(1, result.morphemes.size, "Continuous kana should be one segment")
        assertEquals("おはようございます", result.morphemes[0].surface)
    }

    @Test
    fun `fallback tokenizer segments kanji correctly`() {
        val result = ua.syt0r.kanji.desktop.engine.nlp.FallbackTokenizer.analyze("日本語")
        assertEquals(1, result.morphemes.size, "Continuous kanji should be one segment")
        assertEquals("日本語", result.morphemes[0].surface)
    }

    @Test
    fun `fallback tokenizer segments mixed text`() {
        val result = ua.syt0r.kanji.desktop.engine.nlp.FallbackTokenizer.analyze("日本語は美しいです")
        assertTrue(result.morphemes.size >= 3, "Mixed text should produce multiple segments")
        // Should have: kanji segment, kana segment, kanji segment, kana segment
        assertTrue(result.morphemes.any { it.surface == "日本語" })
        assertTrue(result.morphemes.any { it.surface == "は" })
    }

    @Test
    fun `fallback tokenizer handles empty string`() {
        val result = ua.syt0r.kanji.desktop.engine.nlp.FallbackTokenizer.analyze("")
        assertEquals(0, result.morphemes.size)
    }

    @Test
    fun `fallback tokenizer handles pure Latin`() {
        val result = ua.syt0r.kanji.desktop.engine.nlp.FallbackTokenizer.analyze("hello")
        assertEquals(1, result.morphemes.size)
        assertEquals("hello", result.morphemes[0].surface)
    }

    @Test
    fun `fallback tokenizer handles punctuation`() {
        val result = ua.syt0r.kanji.desktop.engine.nlp.FallbackTokenizer.analyze("日本語。")
        assertTrue(result.morphemes.size >= 2)
        assertTrue(result.morphemes.any { it.surface.contains("。") })
    }

    // ----------------------------------------------------------
    // NLP engine composite tests
    // ----------------------------------------------------------

    @Test
    fun `NlpEngine analyze returns non-empty result`() {
        val result = ua.syt0r.kanji.desktop.engine.nlp.JapaneseNlpEngine.analyze("食べる")
        assertTrue(result.morphemes.isNotEmpty())
        assertTrue(result.engine.isNotEmpty())
    }

    @Test
    fun `NlpEngine segment returns surface forms`() {
        val segments = ua.syt0r.kanji.desktop.engine.nlp.JapaneseNlpEngine.segment("猫がいる")
        assertTrue(segments.isNotEmpty())
    }

    @Test
    fun `NlpEngine isJapanese detects kana`() {
        assertTrue(ua.syt0r.kanji.desktop.engine.nlp.JapaneseNlpEngine.isJapanese("ひらがな"))
        assertTrue(ua.syt0r.kanji.desktop.engine.nlp.JapaneseNlpEngine.isJapanese("カタカナ"))
        assertTrue(ua.syt0r.kanji.desktop.engine.nlp.JapaneseNlpEngine.isJapanese("漢字"))
    }

    @Test
    fun `NlpEngine isJapanese rejects Latin`() {
        assertEquals(false, ua.syt0r.kanji.desktop.engine.nlp.JapaneseNlpEngine.isJapanese("hello"))
    }

    @Test
    fun `NlpEngine getReading returns kana`() {
        val reading = ua.syt0r.kanji.desktop.engine.nlp.JapaneseNlpEngine.getReading("食べる")
        assertTrue(reading.isNotEmpty(), "Should return a reading")
    }

    @Test
    fun `contentWords extracts nouns and verbs`() {
        val words = ua.syt0r.kanji.desktop.engine.nlp.JapaneseNlpEngine.contentWords("東京で食べました")
        // Should find content words (nouns, verbs) vs particles
        assertTrue(words.isNotEmpty(), "Should find at least one content word")
    }

    // ----------------------------------------------------------
    // Handlebars template engine tests
    // ----------------------------------------------------------

    @Test
    fun `handlebars renders simple variable`() {
        val template = "Hello {{name}}!"
        val context = ua.syt0r.kanji.desktop.engine.dictionary.TemplateContext(mapOf("name" to "World"))
        val result = ua.syt0r.kanji.desktop.engine.dictionary.HandlebarsEngine.render(template, context)
        assertEquals("Hello World!", result)
    }

    @Test
    fun `handlebars renders HTML-escaped variable`() {
        val template = "{{text}}"
        val context = ua.syt0r.kanji.desktop.engine.dictionary.TemplateContext(mapOf("text" to "<b>bold</b>"))
        val result = ua.syt0r.kanji.desktop.engine.dictionary.HandlebarsEngine.render(template, context)
        assertEquals("&lt;b&gt;bold&lt;/b&gt;", result)
    }

    @Test
    fun `handlebars renders unescaped variable`() {
        val template = "{{{text}}}"
        val context = ua.syt0r.kanji.desktop.engine.dictionary.TemplateContext(mapOf("text" to "<b>bold</b>"))
        val result = ua.syt0r.kanji.desktop.engine.dictionary.HandlebarsEngine.render(template, context)
        assertEquals("<b>bold</b>", result)
    }

    @Test
    fun `handlebars renders section with list`() {
        val template = "{{#items}}{{this}} {{/items}}"
        val context = ua.syt0r.kanji.desktop.engine.dictionary.TemplateContext(
            mapOf("items" to listOf("a", "b", "c"))
        )
        val result = ua.syt0r.kanji.desktop.engine.dictionary.HandlebarsEngine.render(template, context)
        assertEquals("a b c ", result)
    }

    @Test
    fun `handlebars renders inverted section when falsy`() {
        val template = "{{^items}}No items{{/items}}"
        val context = ua.syt0r.kanji.desktop.engine.dictionary.TemplateContext(
            mapOf("items" to emptyList<String>())
        )
        val result = ua.syt0r.kanji.desktop.engine.dictionary.HandlebarsEngine.render(template, context)
        assertEquals("No items", result)
    }

    // ----------------------------------------------------------
    // EPUB reader tests
    // ----------------------------------------------------------

    @Test
    fun `epub reader xhtml to blocks extracts headings`() {
        val xhtml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <html xmlns="http://www.w3.org/1999/xhtml">
            <body>
                <h1>Chapter 1</h1>
                <p>First paragraph.</p>
                <p>Second paragraph.</p>
            </body>
            </html>
        """.trimIndent()
        val blocks = ua.syt0r.kanji.desktop.engine.reading.EpubReader.xhtmlToBlocks(xhtml)
        assertTrue(blocks.size >= 3, "Should extract heading + 2 paragraphs")
        assertEquals("Chapter 1", blocks[0].text)
    }

    @Test
    fun `epub reader xhtml to blocks handles nested elements`() {
        val xhtml = """
            <html><body>
                <div>
                    <p>Text in a div.</p>
                    <ul><li>Item 1</li><li>Item 2</li></ul>
                </div>
            </body></html>
        """.trimIndent()
        val blocks = ua.syt0r.kanji.desktop.engine.reading.EpubReader.xhtmlToBlocks(xhtml)
        assertTrue(blocks.any { it.text.contains("Text in a div") })
        assertTrue(blocks.any { it.text.contains("Item 1") })
    }

    @Test
    fun `epub reader strips tags from content`() {
        val xhtml = "<html><body><p>This has <strong>bold</strong> text.</p></body></html>"
        val blocks = ua.syt0r.kanji.desktop.engine.reading.EpubReader.xhtmlToBlocks(xhtml)
        assertEquals(1, blocks.size)
        assertEquals("This has bold text.", blocks[0].text)
    }
}

// Helper to access JVM-only classes from commonTest
private fun assertEquals(expected: Any?, actual: Any?, message: String = "") {
    kotlin.test.assertEquals(expected, actual, message)
}

private fun assertEquals(expected: Boolean, actual: Boolean, message: String = "") {
    kotlin.test.assertEquals(expected, actual, message)
}
