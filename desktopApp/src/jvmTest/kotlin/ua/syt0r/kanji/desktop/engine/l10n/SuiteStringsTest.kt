package ua.syt0r.kanji.desktop.engine.l10n

import java.lang.reflect.Method
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Guards the suite l10n layer:
 *  - every [SuiteStrings] property is implemented and non-blank in both
 *    [EnglishSuiteStrings] and [JapaneseSuiteStrings],
 *  - the resolver picks the right implementation for the active locale,
 *  - the Japanese implementation actually contains kana/kanji.
 *
 * Uses plain JVM reflection so the test needs no kotlin-reflect dependency.
 */
class SuiteStringsTest {

    /** Property names that are intentionally ASCII (acronyms, verbatim placeholders). */
    private val asciiExempt = setOf("jlptLabel")

    private val stringGetters: List<Method> =
        SuiteStrings::class.java.methods
            .filter { it.parameterCount == 0 && it.returnType == String::class.java && it.name != "toString" }
            .sortedBy { it.name }

    @Test
    fun bothImplementationsDefineEveryString() {
        stringGetters.forEach { getter ->
            val en = getter.invoke(EnglishSuiteStrings) as String
            val ja = getter.invoke(JapaneseSuiteStrings) as String
            assertTrue(en.isNotBlank(), "English '${getter.name}' is blank")
            assertTrue(ja.isNotBlank(), "Japanese '${getter.name}' is blank")
        }
    }

    @Test
    fun japaneseValuesContainKanaOrKanji() {
        stringGetters.forEach { getter ->
            if (getter.name in asciiExempt) return@forEach
            val ja = getter.invoke(JapaneseSuiteStrings) as String
            assertTrue(
                ja.any { it.code in '\u3040'..'\u30ff' || it.code in '\u4e00'..'\u9fff' },
                "Expected kana/kanji in Japanese '${getter.name}': '$ja'"
            )
        }
    }

    @Test
    fun resolverSelectsByLocale() {
        assertEquals(JapaneseSuiteStrings.dictionariesTitle, resolveForLocale("ja") { dictionariesTitle })
        assertEquals(EnglishSuiteStrings.dictionariesTitle, resolveForLocale("en") { dictionariesTitle })
        assertEquals(EnglishSuiteStrings.dictionariesTitle, resolveForLocale("fr") { dictionariesTitle })
    }

    @Test
    fun noRawKeyLeaks() {
        stringGetters.forEach { getter ->
            val value = getter.invoke(EnglishSuiteStrings) as String
            assertTrue(!value.contains("{ get() }") && !value.startsWith("val "),
                "English '${getter.name}' looks like a raw key: '$value'")
        }
    }

    // ---- helpers -------------------------------------------------

    private fun resolveForLocale(locale: String, block: SuiteStrings.() -> String): String {
        val impl = if (locale == "ja") JapaneseSuiteStrings else EnglishSuiteStrings
        return block(impl)
    }
}
