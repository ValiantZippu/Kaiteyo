package ua.syt0r.kanji.desktop.engine.curriculum

// ============================================
// KAITEYO CURRICULUM — BUILT-IN COURSES
// Starter courses reference the real built-in
// deck ids (KanaCatalog + BuiltInDecks) — never
// fabricated data. Objectives resolve through
// the data source, so a course measures actual
// card/review counts.
// ============================================

object BuiltInCurriculum {

    private fun objective(
        id: String,
        kind: CurriculumObjectiveKind,
        target: Int,
        label: String,
        deckId: String? = null
    ) = CurriculumObjective(id = id, kind = kind, target = target, label = label, deckId = deckId)

    private fun lesson(
        id: String,
        title: String,
        description: String,
        vararg objectives: CurriculumObjective
    ) = CurriculumLesson(id = id, title = title, description = description, objectives = objectives.toList())

    val all: List<CurriculumCourse> = listOf(
        // --------------------------------------------------------
        // Kana foundation — the reading basics
        // --------------------------------------------------------
        CurriculumCourse(
            id = "kana-foundation",
            title = "Kana Foundation",
            description = "Hiragana and katakana — the reading basics. Learn every character, then review them until they stick.",
            lessons = listOf(
                lesson(
                    "hiragana",
                    "Hiragana",
                    "Learn all 46 base hiragana.",
                    objective("hira-new", CurriculumObjectiveKind.NewCardCount, 46, "Learn the 46 base hiragana", "kana-hiragana"),
                    objective("hira-review", CurriculumObjectiveKind.ReviewCount, 20, "Review 20 hiragana", "kana-hiragana")
                ),
                lesson(
                    "katakana",
                    "Katakana",
                    "Learn all 46 base katakana.",
                    objective("kata-new", CurriculumObjectiveKind.NewCardCount, 46, "Learn the 46 base katakana", "kana-katakana"),
                    objective("kata-review", CurriculumObjectiveKind.ReviewCount, 20, "Review 20 katakana", "kana-katakana")
                ),
                lesson(
                    "advanced-kana",
                    "Dakuten, handakuten and yōon",
                    "The modified kana: voiced sounds, semi-voiced sounds and contracted syllables.",
                    objective("adv-new", CurriculumObjectiveKind.NewCardCount, 25, "Learn the modified kana", "kana-hiragana-advanced"),
                    objective("adv-kata", CurriculumObjectiveKind.NewCardCount, 25, "Learn modified katakana", "kana-katakana-advanced")
                ),
                lesson(
                    "kana-review",
                    "Kana review",
                    "Consolidate both syllabaries.",
                    objective("kana-full-new", CurriculumObjectiveKind.NewCardCount, 100, "Grow the full kana deck", "kana-full"),
                    objective("kana-full-review", CurriculumObjectiveKind.ReviewCount, 40, "Review 40 kana", "kana-full")
                )
            )
        ),

        // --------------------------------------------------------
        // JLPT N5 path — first certification milestone
        // --------------------------------------------------------
        CurriculumCourse(
            id = "jlpt-n5",
            title = "JLPT N5 Path",
            description = "The ~80 kanji, core vocabulary and beginner grammar required for JLPT N5.",
            lessons = listOf(
                lesson(
                    "n5-kanji",
                    "N5 Kanji",
                    "The ~80 kanji required for JLPT N5.",
                    objective("n5-kanji-new", CurriculumObjectiveKind.NewCardCount, 80, "Learn the N5 kanji", "kanji-jlpt5"),
                    objective("n5-kanji-review", CurriculumObjectiveKind.ReviewCount, 40, "Review 40 N5 kanji", "kanji-jlpt5")
                ),
                lesson(
                    "n5-vocab",
                    "N5 Vocabulary",
                    "Essential vocabulary for JLPT N5.",
                    objective("n5-vocab-new", CurriculumObjectiveKind.NewCardCount, 150, "Learn 150 N5 words", "vocab-jlpt5"),
                    objective("n5-vocab-review", CurriculumObjectiveKind.ReviewCount, 60, "Review 60 N5 words", "vocab-jlpt5")
                ),
                lesson(
                    "n5-grammar",
                    "N5 Grammar",
                    "Beginner grammar patterns for JLPT N5.",
                    objective("n5-grammar-new", CurriculumObjectiveKind.NewCardCount, 20, "Learn 20 N5 grammar patterns", "grammar-jlpt5"),
                    objective("n5-grammar-review", CurriculumObjectiveKind.ReviewCount, 10, "Review 10 grammar patterns", "grammar-jlpt5")
                ),
                lesson(
                    "n5-milestone",
                    "N5 Milestone",
                    "A steady review habit — the exam reward is a habit, not a grind.",
                    objective("n5-milestone-reviews", CurriculumObjectiveKind.TotalReviewCount, 200, "Complete 200 total reviews")
                )
            )
        ),

        // --------------------------------------------------------
        // JLPT N4 path
        // --------------------------------------------------------
        CurriculumCourse(
            id = "jlpt-n4",
            title = "JLPT N4 Path",
            description = "The ~170 kanji, intermediate vocabulary and progressing grammar for JLPT N4.",
            lessons = listOf(
                lesson(
                    "n4-kanji",
                    "N4 Kanji",
                    "The ~170 kanji required for JLPT N4.",
                    objective("n4-kanji-new", CurriculumObjectiveKind.NewCardCount, 170, "Learn the N4 kanji", "kanji-jlpt4"),
                    objective("n4-kanji-review", CurriculumObjectiveKind.ReviewCount, 80, "Review 80 N4 kanji", "kanji-jlpt4")
                ),
                lesson(
                    "n4-vocab",
                    "N4 Vocabulary",
                    "Essential vocabulary for JLPT N4.",
                    objective("n4-vocab-new", CurriculumObjectiveKind.NewCardCount, 200, "Learn 200 N4 words", "vocab-jlpt4"),
                    objective("n4-vocab-review", CurriculumObjectiveKind.ReviewCount, 80, "Review 80 N4 words", "vocab-jlpt4")
                ),
                lesson(
                    "n4-grammar",
                    "N4 Grammar",
                    "Progressing grammar patterns for JLPT N4.",
                    objective("n4-grammar-new", CurriculumObjectiveKind.NewCardCount, 30, "Learn 30 N4 grammar patterns", "grammar-jlpt4"),
                    objective("n4-grammar-review", CurriculumObjectiveKind.ReviewCount, 15, "Review 15 grammar patterns", "grammar-jlpt4")
                ),
                lesson(
                    "n4-milestone",
                    "N4 Milestone",
                    "Sustained review volume.",
                    objective("n4-milestone-reviews", CurriculumObjectiveKind.TotalReviewCount, 400, "Complete 400 total reviews")
                )
            )
        )
    )
}
