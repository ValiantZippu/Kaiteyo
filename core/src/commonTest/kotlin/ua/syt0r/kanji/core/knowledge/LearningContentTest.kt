package ua.syt0r.kanji.core.knowledge

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class LearningContentTest {

    @Test
    fun `formula is derived from real components`() {
        val formula = FormulaBuilder.formula(
            character = "漢",
            components = listOf(
                ComponentKnowledge(component = "氵", radicalOf = "氵", strokesCount = 3),
                ComponentKnowledge(component = "又", radicalOf = "又", strokesCount = 2)
            )
        )
        assertNotNull(formula)
        assertEquals("氵 + 又 → 漢", formula.body)
        assertEquals(ContentSourceType.Derived, formula.sourceType)
        assertEquals(ContentConfidence.High, formula.confidence)
    }

    @Test
    fun `no components means no formula`() {
        assertNull(FormulaBuilder.formula("漢", emptyList()))
    }

    @Test
    fun `registry groups content by entity`() {
        val registry = LearningContentRegistry()
        registry.register(
            FormulaBuilder.formula(
                character = "漢",
                components = listOf(ComponentKnowledge("氵", "氵", 3))
            )!!
        )
        registry.register(
            FormulaBuilder.formula(
                character = "海",
                components = listOf(ComponentKnowledge("氵", "氵", 3))
            )!!
        )
        assertEquals(1, registry.forEntity("kanji:漢").size)
        assertEquals(2, registry.all().size)
    }

    @Test
    fun `ai content carries its source label`() {
        val content = LearningContent(
            id = "mnemonic:漢:ai-1",
            entityKey = "kanji:漢",
            type = LearningContentType.Mnemonic,
            title = "AI suggestion",
            body = "A suggested mnemonic.",
            sourceType = ContentSourceType.AiGenerated,
            confidence = ContentConfidence.Medium
        )
        assertEquals("AI-generated", content.sourceType.label)
        assertEquals(ContentConfidence.Medium, content.confidence)
    }
}
