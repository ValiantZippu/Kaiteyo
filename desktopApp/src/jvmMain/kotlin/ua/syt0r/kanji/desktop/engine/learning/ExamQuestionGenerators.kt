package ua.syt0r.kanji.desktop.engine.learning

import kotlin.random.Random

// ============================================
// KAITEYO EXAMS — QUESTION GENERATORS
// Deterministic question generators over real
// vocabulary data (expression/reading/meaning).
// Distractors come from the same pool — never
// invented. Each generator is a pure function
// of the pool + a seed, so exams are
// reproducible and testable.
// ============================================

/** A vocabulary item an exam question can be built from. */
data class ExamVocabItem(
    val expression: String,
    val reading: String = "",
    val meaning: String = ""
)

// NOTE: named GeneratorQuestionType / GeneratorQuestion to avoid colliding
// with the ExamEngine's ExamQuestionType / ExamQuestion (same package). These
// generators produce question *shapes*; ExamEngine.convert maps them into the
// engine's evaluatable ExamQuestion model.
enum class GeneratorQuestionType {
    Cloze,        // complete a word with one kanji blanked
    Matching,     // pick the correct meaning/reading among options
    Ordering,     // arrange the kanji of a compound word
    FreeResponse, // type the word from its meaning
    Timed         // any question with a time limit
}

data class GeneratorQuestion(
    val type: GeneratorQuestionType,
    val prompt: String,
    val correct: List<String>,
    val options: List<String> = emptyList(),
    val context: String = "",
    val timeLimitSeconds: Int? = null
)

object ExamQuestionGenerators {

    // ------------------------------------------------------------
    // Matching (multiple choice: expression → meaning / reading)
    // ------------------------------------------------------------

    /** Pick the correct meaning for an expression among [optionCount] options. */
    fun meaningMatching(items: List<ExamVocabItem>, optionCount: Int = 4, seed: Int = 7): List<GeneratorQuestion> {
        val rng = Random(seed)
        return items.map { item ->
            val distractors = items
                .filter { it.meaning.isNotBlank() && it.meaning != item.meaning }
                .map { it.meaning }
                .distinct()
                .shuffled(rng)
                .take(optionCount - 1)
            GeneratorQuestion(
                type = GeneratorQuestionType.Matching,
                prompt = item.expression,
                correct = listOf(item.meaning),
                options = (distractors + item.meaning).shuffled(rng),
                context = item.reading
            )
        }
    }

    /** Pick the correct reading (kana) for an expression among options. */
    fun readingMatching(items: List<ExamVocabItem>, optionCount: Int = 4, seed: Int = 11): List<GeneratorQuestion> {
        val withReadings = items.filter { it.reading.isNotBlank() }
        val rng = Random(seed)
        return withReadings.map { item ->
            val distractors = withReadings
                .filter { it.reading != item.reading }
                .map { it.reading }
                .distinct()
                .shuffled(rng)
                .take(optionCount - 1)
            GeneratorQuestion(
                type = GeneratorQuestionType.Matching,
                prompt = item.expression,
                correct = listOf(item.reading),
                options = (distractors + item.reading).shuffled(rng),
                context = item.meaning
            )
        }
    }

    // ------------------------------------------------------------
    // Cloze (one kanji blanked in a compound word)
    // ------------------------------------------------------------

    /** Complete the word: 学校 → "学＿" with 校 among the options. */
    fun kanjiCloze(items: List<ExamVocabItem>, seed: Int = 13): List<GeneratorQuestion> {
        val compounds = items.filter { it.expression.length >= 2 && it.expression.any(::isKanji) }
        val kanjiPool = compounds.flatMap { it.expression.filter(::isKanji).toList() }.distinct()
        val rng = Random(seed)
        return compounds.map { item ->
            val kanji = item.expression.filter(::isKanji).toList()
            val blankIndex = rng.nextInt(kanji.size)
            val blanked = kanji.toMutableList()
            val answer = blanked[blankIndex]
            blanked[blankIndex] = '＿'
            val prompt = buildString {
                var ki = 0
                item.expression.forEach { c ->
                    if (isKanji(c)) {
                        append(if (ki == blankIndex) '＿' else c)
                        ki++
                    } else {
                        append(c)
                    }
                }
            }
            val distractors = kanjiPool.filter { it != answer }.shuffled(rng).take(3)
            GeneratorQuestion(
                type = GeneratorQuestionType.Cloze,
                prompt = prompt,
                correct = listOf(answer.toString()),
                options = (distractors.map { it.toString() } + answer.toString()).distinct().shuffled(rng),
                context = item.meaning
            )
        }
    }

    // ------------------------------------------------------------
    // Ordering (arrange the kanji of a compound word)
    // ------------------------------------------------------------

    /** Arrange the shuffled kanji of a compound word into the correct order. */
    fun compoundOrdering(items: List<ExamVocabItem>, seed: Int = 17): List<GeneratorQuestion> {
        val rng = Random(seed)
        return items
            .filter { it.expression.length >= 2 && it.expression.count(::isKanji) >= 2 }
            .map { item ->
                val kanji = item.expression.filter(::isKanji).toList()
                val shuffled = kanji.shuffled(rng)
                val optionKanji = if (shuffled == kanji) kanji.reversed() else shuffled
                GeneratorQuestion(
                    type = GeneratorQuestionType.Ordering,
                    prompt = "Arrange the kanji to spell “${item.meaning}”",
                    correct = kanji.map { it.toString() },
                    options = optionKanji.map { it.toString() },
                    context = item.expression
                )
            }
    }

    // ------------------------------------------------------------
    // Free response (type the word from its meaning)
    // ------------------------------------------------------------

    /** Type the expression (exact or its reading) that matches the meaning. */
    fun freeResponse(items: List<ExamVocabItem>): List<GeneratorQuestion> =
        items.filter { it.meaning.isNotBlank() }.map { item ->
            GeneratorQuestion(
                type = GeneratorQuestionType.FreeResponse,
                prompt = item.meaning,
                correct = listOfNotNull(item.expression, item.reading.takeIf { it.isNotBlank() && it != item.expression }),
                context = "Type the Japanese word"
            )
        }

    // ------------------------------------------------------------
    // Timed wrapper
    // ------------------------------------------------------------

    /** Stamp a time limit onto any generated questions (timed exam mode). */
    fun timed(questions: List<GeneratorQuestion>, secondsPerQuestion: Int): List<GeneratorQuestion> =
        questions.map { it.copy(type = GeneratorQuestionType.Timed, timeLimitSeconds = secondsPerQuestion) }

    // ------------------------------------------------------------
    // Composite builders
    // ------------------------------------------------------------

    /** A balanced mixed exam: matching + reading + cloze + ordering + free response. */
    fun mixedExam(items: List<ExamVocabItem>, seed: Int = 23, timeLimitSeconds: Int? = null): List<GeneratorQuestion> {
        val generated = buildList {
            addAll(meaningMatching(items, seed = seed))
            addAll(readingMatching(items, seed = seed + 1))
            addAll(kanjiCloze(items, seed = seed + 2))
            addAll(compoundOrdering(items, seed = seed + 3))
            addAll(freeResponse(items))
        }
        return if (timeLimitSeconds != null) timed(generated, timeLimitSeconds) else generated
    }

    private fun isKanji(c: Char): Boolean =
        c.code in 0x4E00..0x9FFF || c.code in 0x3400..0x4DBF || c.code in 0xF900..0xFAFF
}
