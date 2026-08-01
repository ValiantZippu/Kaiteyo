package ua.syt0r.kanji.presentation.screen.main.screen.decks

// KAITEYO ACHIEVEMENT DEFS - Vocabulary & Kanji

fun buildVocabularyAchievements() = listOf(
    Achievement("words-10", "First Words", "Learn 10 vocabulary words",
        "\uD83D\uDDE3\uFE0F", AchievementCategory.Vocabulary, AchievementDifficulty.Bronze, xpReward = 5, steps = 10),
    Achievement("words-50", "Growing Vocabulary", "Learn 50 vocabulary words",
        "\uD83D\uDCDD", AchievementCategory.Vocabulary, AchievementDifficulty.Bronze, xpReward = 10, steps = 50),
    Achievement("words-100", "Hundred Words Club", "Learn 100 vocabulary words",
        "\uD83C\uDFAF", AchievementCategory.Vocabulary, AchievementDifficulty.Silver, xpReward = 25, steps = 100),
    Achievement("words-500", "Lexicon Builder", "Learn 500 vocabulary words",
        "\uD83D\uDCDA", AchievementCategory.Vocabulary, AchievementDifficulty.Silver, xpReward = 50, steps = 500),
    Achievement("words-1000", "Thousand Words", "Learn 1,000 vocabulary words",
        "\uD83D\uDCAA", AchievementCategory.Vocabulary, AchievementDifficulty.Gold, xpReward = 100, steps = 1000),
    Achievement("words-2000", "Word Collector", "Learn 2,000 vocabulary words",
        "\uD83D\uDD25", AchievementCategory.Vocabulary, AchievementDifficulty.Gold, xpReward = 200, steps = 2000),
    Achievement("words-5000", "Polyglot in Training", "Learn 5,000 vocabulary words",
        "\uD83C\uDF1F", AchievementCategory.Vocabulary, AchievementDifficulty.Platinum, xpReward = 500, steps = 5000),
    Achievement("words-10000", "Vocabulary Virtuoso", "Learn 10,000 vocabulary words",
        "\uD83D\uDC8E", AchievementCategory.Vocabulary, AchievementDifficulty.Diamond, AchievementRarity.Epic,
        xpReward = 1000, steps = 10000)
)

fun buildKanjiAchievements() = listOf(
    Achievement("jlpt-n5-complete", "JLPT N5 Complete", "Master all JLPT N5 kanji",
        "\uD83C\uDF35", AchievementCategory.Kanji, AchievementDifficulty.Bronze, xpReward = 50),
    Achievement("jlpt-n4-complete", "JLPT N4 Complete", "Master all JLPT N4 kanji",
        "\uD83C\uDF36", AchievementCategory.Kanji, AchievementDifficulty.Silver, xpReward = 100),
    Achievement("jlpt-n3-complete", "JLPT N3 Complete", "Master all JLPT N3 kanji",
        "\uD83C\uDF39", AchievementCategory.Kanji, AchievementDifficulty.Silver, xpReward = 200),
    Achievement("jlpt-n2-complete", "JLPT N2 Complete", "Master all JLPT N2 kanji",
        "\uD83C\uDF3A", AchievementCategory.Kanji, AchievementDifficulty.Gold, AchievementRarity.Rare, xpReward = 500),
    Achievement("jlpt-n1-complete", "JLPT N1 Complete", "Master all JLPT N1 kanji",
        "\uD83C\uDF37", AchievementCategory.Kanji, AchievementDifficulty.Platinum, AchievementRarity.Epic, xpReward = 1000),
    Achievement("kanji-all-joyo", "Joyu Master", "Master all 2,136 Joyu kanji",
        "\uD83C\uDF8C", AchievementCategory.Kanji, AchievementDifficulty.Diamond, AchievementRarity.Legendary,
        xpReward = 5000, steps = 2136, isSecret = true)
)
