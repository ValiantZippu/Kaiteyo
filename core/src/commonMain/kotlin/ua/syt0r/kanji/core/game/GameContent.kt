package ua.syt0r.kanji.core.game

// ============================================================
// GAME CONTENT — the built-in curriculum.
//
// Courses reference the real Kaiteyo dataset through
// [KanjiSource]: JLPT and school-grade courses resolve to the
// exact kanji classified "n5"…"n1" / "o1"…"o6" in the bundled
// dictionary, and the frequency courses resolve to the top-N
// kanji by the bundled frequency ranking. Vocabulary nodes
// reference the same classifications used by the JLPT import
// decks. Nothing here is hardcoded card data — the app owns
// the content, the game just organizes a path through it.
// ============================================================

val kaiteyoWorld = GameWorld(
    id = "kaiteyo-world",
    title = "Kaiteyo World",
    subtitle = "A path through the language, built from your own study.",
    courses = listOf(
        GameCourse(
            id = "foundations",
            title = "Foundations",
            subtitle = "Kana, numbers and the shapes of everyday life.",
            icon = "🔤",
            nodes = listOf(
                GameNode(
                    id = "foundations-hiragana",
                    kind = GameNodeKind.Kana,
                    title = "Hiragana",
                    subtitle = "あ い う え お · 46 characters",
                    description = "The first writing system. Master the shapes, sounds and stroke order of every hiragana character. Mark this complete once you can read and write them without hesitation.",
                    xp = 150
                ),
                GameNode(
                    id = "foundations-katakana",
                    kind = GameNodeKind.Kana,
                    title = "Katakana",
                    subtitle = "ア イ ウ エ オ · 46 characters",
                    description = "Used for loanwords, names and emphasis. Study them the same way you did hiragana, then mark this complete.",
                    prerequisites = listOf("foundations-hiragana"),
                    xp = 150
                ),
                GameNode(
                    id = "foundations-numbers",
                    kind = GameNodeKind.Kanji,
                    title = "Numbers",
                    subtitle = "一二三四五六七八九十百千万",
                    description = "The kanji you will meet every single day. Master all twelve and the counting system starts to feel natural.",
                    kanji = listOf("一", "二", "三", "四", "五", "六", "七", "八", "九", "十", "百", "千", "万"),
                    xp = 150
                ),
                GameNode(
                    id = "foundations-essentials",
                    kind = GameNodeKind.Kanji,
                    title = "Essential Shapes",
                    subtitle = "上下左右中大小出入",
                    description = "Direction and position words appear constantly in vocabulary. Master these ten and a surprising amount of reading unlocks.",
                    kanji = listOf("上", "下", "左", "右", "中", "大", "小", "出", "入", "人"),
                    xp = 150
                ),
                GameNode(
                    id = "foundations-nature",
                    kind = GameNodeKind.Kanji,
                    title = "Nature",
                    subtitle = "日月火水木金土山川雨",
                    description = "The classic elements and the world around you — from the sun and moon to rivers and rain.",
                    kanji = listOf("日", "月", "火", "水", "木", "金", "土", "山", "川", "雨", "天", "気"),
                    xp = 150
                )
            )
        ),
        GameCourse(
            id = "jlpt",
            title = "JLPT Path",
            subtitle = "N5 → N1, the standard progression.",
            icon = "🗾",
            nodes = listOf(
                GameNode(
                    id = "jlpt-n5-kanji",
                    kind = GameNodeKind.Kanji,
                    title = "N5 Kanji",
                    subtitle = "The first ~80 kanji of the JLPT",
                    description = "Everything classified N5 in Kaiteyo's dictionary. These are the kanji behind most beginner vocabulary.",
                    kanjiSource = KanjiSource.Classification("n5"),
                    xp = 200
                ),
                GameNode(
                    id = "jlpt-n5-vocab",
                    kind = GameNodeKind.Vocabulary,
                    title = "N5 Vocabulary",
                    subtitle = "The core N5 word list",
                    description = "The words every beginner needs. Add the N5 vocabulary deck in Library, study it, then mark this complete.",
                    vocabClassification = "n5",
                    prerequisites = listOf("jlpt-n5-kanji"),
                    xp = 200
                ),
                GameNode(
                    id = "jlpt-n4-kanji",
                    kind = GameNodeKind.Kanji,
                    title = "N4 Kanji",
                    subtitle = "~170 more kanji",
                    description = "The N4 kanji set. Your reading starts leaving textbook sentences behind.",
                    kanjiSource = KanjiSource.Classification("n4"),
                    prerequisites = listOf("jlpt-n5-vocab"),
                    xp = 200
                ),
                GameNode(
                    id = "jlpt-n4-vocab",
                    kind = GameNodeKind.Vocabulary,
                    title = "N4 Vocabulary",
                    subtitle = "The N4 word list",
                    description = "Add the N4 vocabulary deck, study it, then mark this complete.",
                    vocabClassification = "n4",
                    prerequisites = listOf("jlpt-n4-kanji"),
                    xp = 200
                ),
                GameNode(
                    id = "jlpt-n3-kanji",
                    kind = GameNodeKind.Kanji,
                    title = "N3 Kanji",
                    subtitle = "~370 kanji — the bridge",
                    description = "The jump to intermediate. Real articles and anime become approachable around here.",
                    kanjiSource = KanjiSource.Classification("n3"),
                    prerequisites = listOf("jlpt-n4-vocab"),
                    xp = 250
                ),
                GameNode(
                    id = "jlpt-n3-vocab",
                    kind = GameNodeKind.Vocabulary,
                    title = "N3 Vocabulary",
                    subtitle = "The N3 word list",
                    description = "Add the N3 vocabulary deck, study it, then mark this complete.",
                    vocabClassification = "n3",
                    prerequisites = listOf("jlpt-n3-kanji"),
                    xp = 250
                ),
                GameNode(
                    id = "jlpt-n2-kanji",
                    kind = GameNodeKind.Kanji,
                    title = "N2 Kanji",
                    subtitle = "~370 more kanji",
                    description = "The threshold for most university programs and serious media consumption.",
                    kanjiSource = KanjiSource.Classification("n2"),
                    prerequisites = listOf("jlpt-n3-vocab"),
                    xp = 300
                ),
                GameNode(
                    id = "jlpt-n1-kanji",
                    kind = GameNodeKind.Kanji,
                    title = "N1 Kanji",
                    subtitle = "The full ~1,200 set",
                    description = "The end of the JLPT road. Nearly all joyo kanji, mastered in context.",
                    kanjiSource = KanjiSource.Classification("n1"),
                    prerequisites = listOf("jlpt-n2-kanji"),
                    xp = 400
                )
            )
        ),
        GameCourse(
            id = "grades",
            title = "School Grades",
            subtitle = "The Japanese curriculum, grade by grade.",
            icon = "🏫",
            nodes = (1..6).map { grade ->
                GameNode(
                    id = "grade-$grade",
                    kind = GameNodeKind.Kanji,
                    title = when (grade) {
                        1 -> "Grade 1"
                        2 -> "Grade 2"
                        3 -> "Grade 3"
                        4 -> "Grade 4"
                        5 -> "Grade 5"
                        else -> "Grade 6"
                    },
                    subtitle = "The ${grade}${ordinalSuffix(grade)} school-year kanji",
                    description = "Japanese schoolchildren learn joyo kanji across six years of primary school. This is year $grade of that progression.",
                    kanjiSource = KanjiSource.Classification("o$grade"),
                    prerequisites = if (grade == 1) emptyList() else listOf("grade-${grade - 1}"),
                    xp = 100 + grade * 50
                )
            }
        ),
        GameCourse(
            id = "frequency",
            title = "Core Frequency",
            subtitle = "The most-used kanji in real Japanese.",
            icon = "⚡",
            nodes = listOf(
                GameNode(
                    id = "freq-top100",
                    kind = GameNodeKind.Kanji,
                    title = "Top 100",
                    subtitle = "The most frequent kanji in the language",
                    description = "By frequency of actual use — the kanji that carry everyday Japanese.",
                    kanjiSource = KanjiSource.TopFrequency(100),
                    xp = 200
                ),
                GameNode(
                    id = "freq-top500",
                    kind = GameNodeKind.Kanji,
                    title = "Top 500",
                    subtitle = "The next 400 by frequency",
                    description = "With the top 500 under your belt you can read the majority of printed Japanese.",
                    kanjiSource = KanjiSource.TopFrequency(500),
                    prerequisites = listOf("freq-top100"),
                    xp = 300
                ),
                GameNode(
                    id = "freq-top1000",
                    kind = GameNodeKind.Kanji,
                    title = "Top 1000",
                    subtitle = "The next 500 by frequency",
                    description = "Solid reading fluency territory. Keep adding these to your decks and reviewing.",
                    kanjiSource = KanjiSource.TopFrequency(1000),
                    prerequisites = listOf("freq-top500"),
                    xp = 400
                ),
                GameNode(
                    id = "freq-top2000",
                    kind = GameNodeKind.Kanji,
                    title = "Top 2000",
                    subtitle = "The long tail of common use",
                    description = "A serious, rare-level kanji reading ability. This is where patience pays off.",
                    kanjiSource = KanjiSource.TopFrequency(2000),
                    prerequisites = listOf("freq-top1000"),
                    xp = 500
                )
            )
        )
    )
)

private fun ordinalSuffix(n: Int): String = when (n) {
    1 -> "st"
    2 -> "nd"
    3 -> "rd"
    else -> "th"
}
