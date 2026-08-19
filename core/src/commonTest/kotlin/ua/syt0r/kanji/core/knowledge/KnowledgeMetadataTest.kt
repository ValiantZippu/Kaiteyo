package ua.syt0r.kanji.core.knowledge

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Keyword + dataset provenance tests (KT-DATA-002/003, spec §13, §46):
 * keywords are orientation tools with honest nullability; provenance
 * records real metadata and never fabricates a license or version.
 */
class KnowledgeMetadataTest {

    // ---------------------------------------------------------------
    // Keyword system
    // ---------------------------------------------------------------

    @Test
    fun keywordSetBuildsAllKeywords() {
        val set = KanjiKeywordSet(
            character = "食",
            primary = "eat",
            alternates = listOf("food", "meal"),
            learnerMeaning = "to eat",
            literalMeaning = "food in the mouth",
            componentKeyword = "mouth"
        )
        assertEquals(5, set.allKeywords.size)
        assertTrue(set.matches("meal"))
        assertTrue(set.matches("EAT"))
        assertFalse(set.matches("sleep"))
    }

    @Test
    fun keywordSetNullFieldsAreUnavailableNotGuessed() {
        val minimal = KanjiKeywordSet(character = "食", primary = "eat")
        assertNull(minimal.learnerMeaning)
        assertNull(minimal.literalMeaning)
        assertEquals(listOf("eat"), minimal.allKeywords)
    }

    @Test
    fun keywordRegistryFallbackUsesRealMeaning() {
        val registry = KeywordRegistry()
        registry.register(
            KanjiKeywordSet(
                character = "食",
                primary = "eat",
                alternates = listOf("meal")
            )
        )
        assertEquals(
            "eat",
            registry.forCharacter("食")?.primary
        )
        assertEquals(
            listOf("meal"),
            registry.forCharacter("食")?.alternates
        )
    }

    @Test
    fun keywordRegistryFallbackWithoutMeaningIsNull() {
        val registry = KeywordRegistry()
        assertNull(registry.forCharacter("無"))
        assertNull(registry.fallbackFor("無", null))
    }

    @Test
    fun keywordRegistryFallbackWithMeaningBuildsSingleSet() {
        val registry = KeywordRegistry()
        val fallback = registry.fallbackFor("無", "nothing")
        assertEquals("nothing", fallback?.primary)
        assertEquals(emptyList(), fallback?.alternates)
    }

    // ---------------------------------------------------------------
    // Dataset provenance
    // ---------------------------------------------------------------

    @Test
    fun provenanceRecordsHonestMetadata() {
        val kanjidic = DatasetProvenance(
            id = "kanjidic2",
            name = "KANJIDIC2",
            version = "2024-01",
            license = "CC BY-SA 3.0",
            sourceUrl = "https://www.edrdg.org/kanjidic/kanjidic2.xml.gz",
            recordCounts = mapOf("kanji" to 13_108L)
        )
        assertEquals("kanjidic2", kanjidic.id)
        assertEquals(13_108L, kanjidic.recordCounts["kanji"])
    }

    @Test
    fun provenanceMissingLicenseStaysNull() {
        val unknown = DatasetProvenance(id = "x", name = "Unknown dataset")
        assertNull(unknown.license)
        assertNull(unknown.version)
        assertNull(unknown.importedOn)
    }

    @Test
    fun provenanceRegistryRegistersAndQueries() {
        val registry = DatasetProvenanceRegistry()
        assertTrue(registry.isEmpty)
        registry.register(
            DatasetProvenance(id = "kanjidic2", name = "KANJIDIC2")
        )
        registry.register(
            DatasetProvenance(id = "jmdict", name = "JMdict")
        )
        assertFalse(registry.isEmpty)
        assertEquals("KANJIDIC2", registry.forId("kanjidic2")?.name)
        assertEquals(2, registry.all.size)
        assertNull(registry.forId("tatoeba"))
    }
}
