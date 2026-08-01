package ua.syt0r.kanji.presentation.screen.main.screen.decks

// KAITEYO ACHIEVEMENT DEFS - Learning & Writing

fun buildLearningAchievements() = listOf(
    Achievement("first-review", "First Review", "Complete your first card review",
        "\u2705", AchievementCategory.Learning, AchievementDifficulty.Bronze, xpReward = 5),
    Achievement("reviews-100", "Getting Started", "Complete 100 card reviews",
        "\uD83D\uDCDD", AchievementCategory.Learning, AchievementDifficulty.Bronze, xpReward = 10, steps = 100),
    Achievement("reviews-500", "Dedicated Learner", "Complete 500 card reviews",
        "\uD83D\uDCDD", AchievementCategory.Learning, AchievementDifficulty.Silver, xpReward = 25, steps = 500),
    Achievement("reviews-1000", "Review Veteran", "Complete 1,000 card reviews",
        "\uD83C\uDFAF", AchievementCategory.Learning, AchievementDifficulty.Silver, xpReward = 50, steps = 1000),
    Achievement("reviews-5000", "Review Machine", "Complete 5,000 card reviews",
        "\u26A1", AchievementCategory.Learning, AchievementDifficulty.Gold, xpReward = 100, steps = 5000,
        prerequisites = listOf("reviews-1000")),
    Achievement("reviews-10000", "Review Legend", "Complete 10,000 card reviews",
        "\uD83D\uDD25", AchievementCategory.Learning, AchievementDifficulty.Gold, xpReward = 200, steps = 10000),
    Achievement("reviews-50000", "Unstoppable", "Complete 50,000 card reviews",
        "\uD83D\uDCAA", AchievementCategory.Learning, AchievementDifficulty.Platinum, xpReward = 500, steps = 50000),
    Achievement("reviews-100000", "Century of Reviews", "Complete 100,000 card reviews",
        "\uD83C\uDFC5", AchievementCategory.Learning, AchievementDifficulty.Diamond, AchievementRarity.Epic,
        xpReward = 1000, steps = 100000)
)

fun buildWritingAchievements() = listOf(
    Achievement("first-kanji", "First Stroke", "Write your first kanji character",
        "\u270F\uFE0F", AchievementCategory.Writing, AchievementDifficulty.Bronze, xpReward = 5),
    Achievement("kanji-50", "Beginning Calligrapher", "Write 50 kanji characters",
        "\uD83D\uDD8A\uFE0F", AchievementCategory.Writing, AchievementDifficulty.Bronze, xpReward = 10, steps = 50),
    Achievement("kanji-100", "Stroke Artist", "Write 100 kanji characters",
        "\uD83C\uDFA8", AchievementCategory.Writing, AchievementDifficulty.Silver, xpReward = 25, steps = 100),
    Achievement("kanji-500", "Ink Master", "Write 500 kanji characters",
        "\uD83D\uDD8C\uFE0F", AchievementCategory.Writing, AchievementDifficulty.Silver, xpReward = 50, steps = 500),
    Achievement("kanji-1000", "Calligraphy Apprentice", "Write 1,000 kanji characters",
        "\uD83D\uDCDC", AchievementCategory.Writing, AchievementDifficulty.Gold, xpReward = 100, steps = 1000),
    Achievement("kanji-5000", "Calligraphy Master", "Write 5,000 kanji characters",
        "\uD83C\uDFEF", AchievementCategory.Writing, AchievementDifficulty.Platinum, xpReward = 500, steps = 5000),
    Achievement("perfect-stroke", "Perfect Stroke", "Achieve a perfect stroke evaluation",
        "\u2B50", AchievementCategory.Writing, AchievementDifficulty.Silver, AchievementRarity.Rare,
        xpReward = 50, isHidden = true),
    Achievement("stroke-streak-10", "Steady Hand", "Get 10 consecutive perfect strokes",
        "\uD83C\uDFAF", AchievementCategory.Writing, AchievementDifficulty.Gold, AchievementRarity.Epic,
        xpReward = 100, steps = 10, isHidden = true)
)
