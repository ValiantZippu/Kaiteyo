package ua.syt0r.kanji.desktop.engine.library

import ua.syt0r.kanji.desktop.model.ContentKind
import ua.syt0r.kanji.desktop.model.DeckDef

// ============================================
// BUILT-IN DECK CATALOG
// Expands far beyond the original app's deck set:
// JLPT N5-N1 for kanji + vocabulary + grammar,
// school grades, Jōyō, frequency lists, radicals,
// components and sentences. Decks use dynamic
// filter queries so their membership grows
// automatically as content is imported — no
// hardcoded entries.
// ============================================

private fun deck(
    id: String,
    name: String,
    description: String,
    kind: ContentKind,
    difficulty: Int,
    query: String,
    icon: String = "",
    tags: List<String> = emptyList()
) = DeckDef(
    id = id,
    name = name,
    description = description,
    kind = kind,
    builtIn = true,
    difficulty = difficulty,
    source = "builtin",
    filterQuery = query,
    icon = icon,
    tags = tags
)

object BuiltInDecks {
    val all: List<DeckDef> = listOf(
        // ---- Kanji -------------------------------------------------
        deck("kanji-jlpt5", "JLPT N5 Kanji", "The ~80 kanji required for JLPT N5.", ContentKind.Kanji, 1, "kind:kanji jlpt:5", "五"),
        deck("kanji-jlpt4", "JLPT N4 Kanji", "The ~170 kanji required for JLPT N4.", ContentKind.Kanji, 1, "kind:kanji jlpt:4", "四"),
        deck("kanji-jlpt3", "JLPT N3 Kanji", "The ~375 kanji required for JLPT N3.", ContentKind.Kanji, 2, "kind:kanji jlpt:3", "三"),
        deck("kanji-jlpt2", "JLPT N2 Kanji", "The ~380 kanji required for JLPT N2.", ContentKind.Kanji, 3, "kind:kanji jlpt:2", "二"),
        deck("kanji-jlpt1", "JLPT N1 Kanji", "The ~1,000 kanji required for JLPT N1.", ContentKind.Kanji, 4, "kind:kanji jlpt:1", "一"),
        deck("kanji-jouyou", "Jōyō Kanji", "All 2,136 kanji designated for common everyday use in Japan.", ContentKind.Kanji, 3, "kind:kanji", "常用"),
        deck("kanji-grade1", "School Grade 1", "Kanji taught in grade 1 (80 kanji).", ContentKind.Kanji, 1, "kind:kanji grade:1", "一"),
        deck("kanji-grade2", "School Grade 2", "Kanji taught in grade 2 (160 kanji).", ContentKind.Kanji, 2, "kind:kanji grade:2", "二"),
        deck("kanji-grade3", "School Grade 3", "Kanji taught in grade 3 (200 kanji).", ContentKind.Kanji, 2, "kind:kanji grade:3", "三"),
        deck("kanji-grade4", "School Grade 4", "Kanji taught in grade 4 (200 kanji).", ContentKind.Kanji, 3, "kind:kanji grade:4", "四"),
        deck("kanji-grade5", "School Grade 5", "Kanji taught in grade 5 (185 kanji).", ContentKind.Kanji, 3, "kind:kanji grade:5", "五"),
        deck("kanji-grade6", "School Grade 6", "Kanji taught in grade 6 (181 kanji).", ContentKind.Kanji, 4, "kind:kanji grade:6", "六"),
        deck("kanji-top100", "Top 100 Frequency", "The 100 most frequently used kanji in daily Japanese.", ContentKind.Kanji, 1, "kind:kanji freq:<=100", "頻"),
        deck("kanji-top500", "Top 500 Frequency", "The 500 most frequently used kanji.", ContentKind.Kanji, 2, "kind:kanji freq:<=500", "頻"),
        deck("kanji-kentei", "Kanji Kentei (Kanken)", "Levels 10→1 of the Japan Kanji Aptitude Test — all 2,136 jōyō plus the jinmeiyō set.", ContentKind.Kanji, 4, "kind:kanji tag:kanken", "検"),

        // ---- Vocabulary --------------------------------------------
        deck("vocab-jlpt5", "JLPT N5 Vocabulary", "Essential vocabulary for JLPT N5.", ContentKind.Vocabulary, 1, "kind:vocabulary jlpt:5 tag:jlpt-n5", "五"),
        deck("vocab-jlpt4", "JLPT N4 Vocabulary", "Essential vocabulary for JLPT N4.", ContentKind.Vocabulary, 1, "kind:vocabulary jlpt:4", "四"),
        deck("vocab-jlpt3", "JLPT N3 Vocabulary", "Essential vocabulary for JLPT N3.", ContentKind.Vocabulary, 2, "kind:vocabulary jlpt:3", "三"),
        deck("vocab-jlpt2", "JLPT N2 Vocabulary", "Essential vocabulary for JLPT N2.", ContentKind.Vocabulary, 3, "kind:vocabulary jlpt:2", "二"),
        deck("vocab-jlpt1", "JLPT N1 Vocabulary", "Advanced vocabulary for JLPT N1.", ContentKind.Vocabulary, 4, "kind:vocabulary jlpt:1", "一"),
        deck("vocab-core", "Core 2000", "The core 2,000 most useful Japanese words for everyday fluency.", ContentKind.Vocabulary, 2, "kind:vocabulary tag:core-2000", "核"),
        deck("vocab-anime", "Anime Vocabulary", "Frequent vocabulary encountered in anime and manga.", ContentKind.Vocabulary, 2, "kind:vocabulary tag:anime", "漫"),
        deck("vocab-news", "News Vocabulary", "Vocabulary common in newspapers and broadcasts.", ContentKind.Vocabulary, 3, "kind:vocabulary tag:news", "聞"),
        deck("vocab-conversation", "Conversation", "Vocabulary you need for natural daily conversation.", ContentKind.Vocabulary, 1, "kind:vocabulary tag:conversation", "話"),
        deck("vocab-frequency", "Frequency Vocabulary", "The most frequently used words, ordered by corpus frequency.", ContentKind.Vocabulary, 2, "kind:vocabulary freq:<=5000", "頻"),

        // ---- Grammar -----------------------------------------------
        deck("grammar-jlpt5", "JLPT N5 Grammar", "Beginner grammar patterns for JLPT N5.", ContentKind.Grammar, 1, "kind:grammar jlpt:5", "五"),
        deck("grammar-jlpt4", "JLPT N4 Grammar", "Progressing grammar patterns for JLPT N4.", ContentKind.Grammar, 2, "kind:grammar jlpt:4", "四"),
        deck("grammar-jlpt3", "JLPT N3 Grammar", "Intermediate grammar patterns for JLPT N3.", ContentKind.Grammar, 3, "kind:grammar jlpt:3", "三"),
        deck("grammar-jlpt2", "JLPT N2 Grammar", "Advanced grammar patterns for JLPT N2.", ContentKind.Grammar, 4, "kind:grammar jlpt:2", "二"),
        deck("grammar-jlpt1", "JLPT N1 Grammar", "The hardest grammar patterns for JLPT N1.", ContentKind.Grammar, 5, "kind:grammar jlpt:1", "一"),
        deck("grammar-basic", "Dictionary of Basic Grammar", "Foundational patterns modelled on A Dictionary of Basic Japanese Grammar.", ContentKind.Grammar, 1, "kind:grammar tag:basic-grammar", "基"),
        deck("grammar-intermediate", "Intermediate Grammar", "Intermediate connectives, particles and sentence patterns.", ContentKind.Grammar, 3, "kind:grammar tag:intermediate", "中"),
        deck("grammar-advanced", "Advanced Grammar", "Advanced idioms and formal registers.", ContentKind.Grammar, 5, "kind:grammar tag:advanced", "上"),

        // ---- Radicals ----------------------------------------------
        deck("radical-basic", "Basic Radicals", "The 214 traditional radicals every learner should recognise.", ContentKind.Radical, 1, "kind:radical tag:radical-basic", "部"),
        deck("radical-extended", "Extended Radicals", "Additional components and variant radicals for deeper breakdown.", ContentKind.Radical, 2, "kind:radical tag:radical-extended", "延"),
        deck("radical-components", "Components", "Sub-components used to decompose complex kanji.", ContentKind.Radical, 2, "kind:radical tag:component", "成"),

        // ---- Sentences ---------------------------------------------
        deck("sentences-genki", "Example Sentences", "Curated example sentences illustrating vocabulary in context.", ContentKind.Sentence, 2, "kind:sentence", "例")
    )

    /** Decks relevant to a given content kind, in human order. */
    fun forKind(kind: ContentKind): List<DeckDef> =
        all.filter { it.kind == kind && !it.archived }
}