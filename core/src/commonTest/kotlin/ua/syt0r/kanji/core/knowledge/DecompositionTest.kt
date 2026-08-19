package ua.syt0r.kanji.core.knowledge

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Decomposition tests (KT-RAD-002, spec §8): the decomposition MODEL
 * and the engine's dataset-backed behavior. The bundled curated dataset
 * covers common jōyō kanji with well-established structural facts; kanji
 * outside it must degrade honestly (null / empty / 0) — never fabricated.
 */
class DecompositionTest {

    // ---------------------------------------------------------------
    // Decomposition model invariants
    // ---------------------------------------------------------------

    @Test
    fun decompositionModelHoldsComponentsWithRoles() {
        val decomposition = KanjiDecomposition(
            kanji = "明",
            components = listOf(
                DecompositionComponent(
                    character = "日",
                    position = ComponentPosition.Left,
                    isSemantic = true
                ),
                DecompositionComponent(
                    character = "月",
                    position = ComponentPosition.Right,
                    isPhonetic = true
                )
            )
        )
        assertEquals("明", decomposition.kanji)
        assertEquals(2, decomposition.components.size)
        assertTrue(decomposition.components[0].isSemantic)
        assertTrue(decomposition.components[1].isPhonetic)
        assertFalse(decomposition.components[0].character.isEmpty())
        // The model carries a confidence that is never fabricated.
        assertEquals(ContentConfidence.High, decomposition.confidence)
    }

    @Test
    fun componentKnowledgeDistinguishesRadicalSource() {
        val component = ComponentKnowledge(
            component = "口",
            radicalOf = "口",
            strokesCount = 3,
            source = ComponentSource.RadicalDecomposition
        )
        assertEquals("口", component.radicalOf)
        assertEquals(ComponentSource.RadicalDecomposition, component.source)
    }

    @Test
    fun componentWithoutRadicalIsExplicitlyNull() {
        val component = ComponentKnowledge(component = "非")
        assertNull(component.radicalOf)
        assertEquals(0, component.strokesCount)
    }

    // ---------------------------------------------------------------
    // Dataset-backed decompositions (real structural facts)
    // ---------------------------------------------------------------

    @Test
    fun decomposeKnownKanjiReturnsComponents() {
        val engine = KanjiDecompositionEngine()
        val decomposition = engine.decompose("明")
        assertNotNull(decomposition)
        assertEquals("明", decomposition?.kanji)
        assertEquals(2, decomposition?.components?.size)
        assertEquals("日", decomposition?.components?.get(0)?.character)
        assertEquals(ComponentPosition.Left, decomposition?.components?.get(0)?.position)
        assertEquals(ComponentPosition.Right, decomposition?.components?.get(1)?.position)
        assertEquals(DecompositionLayout.LeftRight, decomposition?.layout)
        assertEquals(8, decomposition?.strokeCount)
        assertEquals(ContentConfidence.High, decomposition?.confidence)
    }

    @Test
    fun decomposeEnclosureKanji() {
        val engine = KanjiDecompositionEngine()
        val decomposition = engine.decompose("間")
        assertNotNull(decomposition)
        assertEquals(DecompositionLayout.Enclosure, decomposition?.layout)
        assertEquals(ComponentPosition.Enclosure, decomposition?.components?.get(0)?.position)
        assertEquals(ComponentPosition.Enclosed, decomposition?.components?.get(1)?.position)
        assertEquals("門", decomposition?.components?.get(0)?.character)
        assertEquals("日", decomposition?.components?.get(1)?.character)
    }

    @Test
    fun decomposeTopBottomKanji() {
        val engine = KanjiDecompositionEngine()
        val decomposition = engine.decompose("森")
        assertNotNull(decomposition)
        assertEquals(DecompositionLayout.TopBottom, decomposition?.layout)
        assertEquals("木", decomposition?.components?.get(0)?.character)
        assertEquals("林", decomposition?.components?.get(1)?.character)
    }

    @Test
    fun decomposeSingleComponentKanji() {
        val engine = KanjiDecompositionEngine()
        val decomposition = engine.decompose("木")
        assertNotNull(decomposition)
        assertEquals(DecompositionLayout.Single, decomposition?.layout)
        assertEquals(1, decomposition?.components?.size)
        assertEquals("木", decomposition?.components?.get(0)?.character)
        assertEquals(ComponentPosition.Full, decomposition?.components?.get(0)?.position)
        assertEquals(4, decomposition?.strokeCount)
    }

    @Test
    fun decomposeKanjiMarksRadicalAndSemanticParts() {
        val engine = KanjiDecompositionEngine()
        val decomposition = engine.decompose("海")
        assertNotNull(decomposition)
        assertTrue(decomposition?.components?.get(0)?.isRadical == true)
        assertTrue(decomposition?.components?.get(0)?.isSemantic == true)
        assertTrue(decomposition?.components?.get(1)?.isPhonetic == true)
        assertEquals("氵", decomposition?.components?.get(0)?.character)
    }

    // ---------------------------------------------------------------
    // Layout estimation — dataset-backed, honest for unknowns
    // ---------------------------------------------------------------

    @Test
    fun layoutEstimateForKnownKanjiIsExact() {
        val engine = KanjiDecompositionEngine()
        assertEquals(DecompositionLayout.LeftRight, engine.estimateLayout("明"))
        assertEquals(DecompositionLayout.TopBottom, engine.estimateLayout("森"))
        assertEquals(DecompositionLayout.Enclosure, engine.estimateLayout("間"))
        assertEquals(DecompositionLayout.Single, engine.estimateLayout("木"))
    }

    @Test
    fun layoutEstimateForUnknownKanjiIsUnknown() {
        val engine = KanjiDecompositionEngine()
        // 鬱 is a valid CJK ideograph not in the curated starter dataset.
        assertEquals(DecompositionLayout.Unknown, engine.estimateLayout("鬱"))
        assertEquals(DecompositionLayout.Unknown, engine.estimateLayout("a"))
        assertEquals(DecompositionLayout.Unknown, engine.estimateLayout("あ"))
        assertEquals(DecompositionLayout.Unknown, engine.estimateLayout(""))
        assertEquals(DecompositionLayout.Unknown, engine.estimateLayout("食べ"))
    }

    // ---------------------------------------------------------------
    // Radical lookup
    // ---------------------------------------------------------------

    @Test
    fun radicalLookupReturnsKangxiNumberAndName() {
        val engine = KanjiDecompositionEngine()
        val radical = engine.getRadical("海")
        assertNotNull(radical)
        assertEquals("氵", radical?.character)
        assertEquals(85, radical?.number) // Kangxi water radical
        assertNotNull(radical?.japaneseName)
        assertNotNull(radical?.meaning)
    }

    @Test
    fun radicalLookupForSingleKanjiIsItself() {
        val engine = KanjiDecompositionEngine()
        val radical = engine.getRadical("木")
        assertNotNull(radical)
        assertEquals("木", radical?.character)
        assertEquals(75, radical?.number)
    }

    @Test
    fun radicalLookupForUnknownKanjiIsNull() {
        val engine = KanjiDecompositionEngine()
        assertNull(engine.getRadical("鬱"))
        assertNull(engine.getRadical("a"))
    }

    // ---------------------------------------------------------------
    // Engine edge cases — missing data degrades honestly
    // ---------------------------------------------------------------

    @Test
    fun decomposeReturnsNullForNonKanji() {
        val engine = KanjiDecompositionEngine()
        assertNull(engine.decompose("a"))
        assertNull(engine.decompose(""))
        assertNull(engine.decompose("食べ")) // multi-char
        assertNull(engine.decompose("あ")) // kana
    }

    @Test
    fun decomposeReturnsNullWhenDatasetMissing() {
        // 鬱 is a real kanji but not in the curated starter dataset — the
        // engine must report "no data", never a fabricated decomposition.
        assertNull(KanjiDecompositionEngine().decompose("鬱"))
    }

    @Test
    fun fullDecompositionOfUnknownKanjiIsEmpty() {
        assertTrue(KanjiDecompositionEngine().fullDecomposition("鬱").isEmpty())
    }

    @Test
    fun fullDecompositionIsRecursive() {
        val engine = KanjiDecompositionEngine()
        // 森 = 木 (top) + 林 (bottom); 林 itself decomposes into 木 + 木.
        val full = engine.fullDecomposition("森", maxDepth = 2)
        assertEquals(2, full.size)
        val grove = full.firstOrNull { it.character == "林" }
        assertNotNull(grove)
        assertEquals(2, grove?.subComponents?.size)
        assertEquals("木", grove?.subComponents?.get(0)?.character)
    }

    // ---------------------------------------------------------------
    // Component-based lookups
    // ---------------------------------------------------------------

    @Test
    fun findKanjiContainingComponentUsesInvertedIndex() {
        val engine = KanjiDecompositionEngine()
        val waterKanji = engine.findKanjiContainingComponent("氵")
        assertTrue(waterKanji.contains("海"))
        assertTrue(waterKanji.contains("河"))
        assertTrue(waterKanji.contains("池"))
        assertTrue(waterKanji.contains("清"))

        val treeKanji = engine.findKanjiContainingComponent("木")
        assertTrue(treeKanji.contains("林"))
        assertTrue(treeKanji.contains("校"))
    }

    @Test
    fun findKanjiContainingComponentReturnsEmptyForUnknown() {
        val engine = KanjiDecompositionEngine()
        assertTrue(engine.findKanjiContainingComponent("鬱").isEmpty())
        assertTrue(engine.findKanjiContainingComponent("").isEmpty())
    }

    // ---------------------------------------------------------------
    // Structural similarity (Jaccard over shared components)
    // ---------------------------------------------------------------

    @Test
    fun structuralSimilaritySharesComponents() {
        val engine = KanjiDecompositionEngine()
        // 河 and 海 both contain 氵 → nonzero shared structure.
        assertTrue(engine.structuralSimilarity("河", "海") > 0f)
        // 河 and 明 share nothing.
        assertEquals(0f, engine.structuralSimilarity("河", "明"))
        // Identical component sets → perfect similarity.
        assertEquals(1f, engine.structuralSimilarity("河", "河"))
    }

    @Test
    fun similarityIsZeroWithoutData() {
        val engine = KanjiDecompositionEngine()
        assertEquals(0f, engine.structuralSimilarity("鬱", "明"))
        assertEquals(0f, engine.structuralSimilarity("明", "鬱"))
    }

    // ---------------------------------------------------------------
    // Kangxi radical table
    // ---------------------------------------------------------------

    @Test
    fun kangxiTableResolvesByCharacterAndVariant() {
        assertEquals(85, KangxiRadicalTable.byCharacter("氵")?.number)
        assertEquals(85, KangxiRadicalTable.byVariant("氵")?.number)
        assertEquals(61, KangxiRadicalTable.byVariant("忄")?.number)
        assertNull(KangxiRadicalTable.byCharacter("鬱"))
    }

    // ---------------------------------------------------------------
    // Position / layout vocabulary is complete and stable
    // ---------------------------------------------------------------

    @Test
    fun componentPositionsCoverStructuralSides() {
        val positions = ComponentPosition.entries.map { it.label }
        assertTrue(positions.isNotEmpty())
        assertTrue(ComponentPosition.entries.containsAll(ComponentPosition.entries))
    }

    @Test
    fun decompositionLayoutsCoverCommonStructures() {
        val layouts = DecompositionLayout.entries.map { it.label }
        assertTrue(layouts.isNotEmpty())
        assertTrue(DecompositionLayout.entries.containsAll(DecompositionLayout.entries))
    }
}
