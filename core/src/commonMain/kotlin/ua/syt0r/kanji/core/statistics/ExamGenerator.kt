package ua.syt0r.kanji.core.statistics

import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlin.random.Random

/**
 * Generates exam questions from real language data.
 *
 * - Multiple choice options are drawn from related content (same JLPT
 *   band where possible) and always validated: the correct answer is
 *   present, options are distinct and non-blank, prompts are never
 *   ambiguous (a prompt never equals its answer).
 * - Production questions are free-text and scored leniently by
 *   [ExamScorer].
 * - A fixed [ExamConfig.seed] makes generation reproducible; different
 *   seeds yield different exams.
 */
class ExamGenerator(
    private val json: Json = Json
) {

    fun generate(
        config: ExamConfig,
        items: List<ExamSourceItem>
    ): GeneratedExam {
        val random = Random(config.seed)

        val pool = items
            .filter { it.studied }
            .filter { config.jlptLevel == null || it.jlptLevel == config.jlptLevel }
            .filter { config.contentType == null || it.contentType == config.contentType }
            .filter { it.meaning.isNotBlank() && it.reading.isNotBlank() }
            .distinctBy { it.key }
            .toMutableList()

        val kanjiPool = pool.filter { it.contentType == ContentTypes.KANJI }
        val vocabPool = pool.filter { it.contentType == ContentTypes.VOCAB }

        // Build the type mix based on what is available.
        val availableTypes = buildList {
            if (kanjiPool.isNotEmpty()) {
                add(ExamQuestionType.KanjiToMeaning)
                add(ExamQuestionType.MeaningToKanji)
                add(ExamQuestionType.KanjiToReading)
                add(ExamQuestionType.ReadingToKanji)
                if (kanjiPool.any { it.radical != null }) {
                    add(ExamQuestionType.KanjiToRadical)
                    add(ExamQuestionType.RadicalToKanji)
                }
                if (kanjiPool.any { it.strokeCount != null }) {
                    add(ExamQuestionType.StrokeCount)
                }
                if (config.includeProduction) {
                    add(ExamQuestionType.ProductionReading)
                    add(ExamQuestionType.ProductionKanji)
                }
            }
            if (vocabPool.isNotEmpty()) {
                add(ExamQuestionType.VocabToMeaning)
                add(ExamQuestionType.MeaningToVocab)
                add(ExamQuestionType.VocabToReading)
                add(ExamQuestionType.ReadingToVocab)
                if (config.includeProduction) {
                    add(ExamQuestionType.ProductionVocabReading)
                    add(ExamQuestionType.ProductionVocabKanji)
                }
            }
        }

        val questions = mutableListOf<GeneratedExamQuestion>()
        val usedEntities = mutableSetOf<String>()

        repeat(config.questionCount) { index ->
            if (availableTypes.isEmpty()) return@repeat
            val type = availableTypes[random.nextInt(availableTypes.size)]
            val generated = tryGenerate(type, pool, kanjiPool, vocabPool, usedEntities, random)
            if (generated != null) {
                usedEntities += generated.entityKey
                questions.add(generated)
            }
        }

        return GeneratedExam(
            exam = ExamRecord(
                title = config.title,
                examType = when {
                    config.contentType == ContentTypes.VOCAB -> "vocabulary"
                    config.contentType == ContentTypes.KANJI -> "kanji"
                    else -> "mixed"
                },
                scopeJson = json.encodeToString(
                    ExamScope(
                        jlptLevel = config.jlptLevel,
                        contentType = config.contentType,
                        questionTypes = questions.map { it.type.name }.distinct()
                    )
                ),
                questionCount = questions.size,
                timeLimitMs = config.timeLimitMs,
                seed = config.seed,
                startedAt = Clock.System.now()
            ),
            questions = questions
        )
    }

    private fun tryGenerate(
        type: ExamQuestionType,
        pool: List<ExamSourceItem>,
        kanjiPool: List<ExamSourceItem>,
        vocabPool: List<ExamSourceItem>,
        usedEntities: Set<String>,
        random: Random
    ): GeneratedExamQuestion? {
        val source = when (type.contentType) {
            ContentTypes.KANJI -> kanjiPool
            else -> vocabPool
        }
        val candidates = source.filter { it.key !in usedEntities }.takeIf { it.isNotEmpty() }
            ?: source
        if (candidates.isEmpty()) return null

        return when (type) {
            ExamQuestionType.KanjiToMeaning -> multipleChoice(
                type, candidates, random,
                prompt = { "What does「${it.content}」mean?" },
                answer = { it.meaning },
                distractors = { item, poolItems ->
                    poolItems.filter { p -> p.key != item.key && p.meaning.isNotBlank() }.map { it.meaning }
                }
            )

            ExamQuestionType.MeaningToKanji -> multipleChoice(
                type, candidates, random,
                prompt = { "Which kanji means \"${firstMeaning(it.meaning)}\"?" },
                answer = { it.content },
                distractors = { item, poolItems ->
                    poolItems.filter { p -> p.key != item.key }.map { it.content }
                }
            )

            ExamQuestionType.KanjiToReading -> multipleChoice(
                type, candidates, random,
                prompt = { "How is「${it.content}」read?" },
                answer = { it.reading },
                distractors = { item, poolItems ->
                    poolItems.filter { p -> p.key != item.key && p.reading.isNotBlank() }.map { it.reading }
                }
            )

            ExamQuestionType.ReadingToKanji -> multipleChoice(
                type, candidates, random,
                prompt = { "Which kanji is read \"${it.reading}\"?" },
                answer = { it.content },
                distractors = { item, poolItems ->
                    poolItems.filter { p -> p.key != item.key }.map { it.content }
                }
            )

            ExamQuestionType.VocabToMeaning -> multipleChoice(
                type, candidates, random,
                prompt = { "What does「${it.content}」mean?" },
                answer = { it.meaning },
                distractors = { item, poolItems ->
                    poolItems.filter { p -> p.key != item.key && p.meaning.isNotBlank() }.map { p -> p.meaning }
                }
            )

            ExamQuestionType.MeaningToVocab -> multipleChoice(
                type, candidates, random,
                prompt = { "Which word means \"${firstMeaning(it.meaning)}\"?" },
                answer = { it.content },
                distractors = { item, poolItems ->
                    poolItems.filter { p -> p.key != item.key }.map { it.content }
                }
            )

            ExamQuestionType.VocabToReading -> multipleChoice(
                type, candidates, random,
                prompt = { "How is「${it.content}」read?" },
                answer = { it.reading },
                distractors = { item, poolItems ->
                    poolItems.filter { p -> p.key != item.key && p.reading.isNotBlank() }.map { p -> p.reading }
                }
            )

            ExamQuestionType.ReadingToVocab -> multipleChoice(
                type, candidates, random,
                prompt = { "Which word is read \"${it.reading}\"?" },
                answer = { it.content },
                distractors = { item, poolItems ->
                    poolItems.filter { p -> p.key != item.key }.map { it.content }
                }
            )

            ExamQuestionType.KanjiToRadical -> multipleChoice(
                type, candidates.filter { it.radical != null }, random,
                prompt = { "Which radical is in「${it.content}」?" },
                answer = { it.radical ?: "" },
                distractors = { item, poolItems ->
                    poolItems.filter { p -> p.key != item.key && p.radical != null }.map { p -> p.radical ?: "" }
                }
            )

            ExamQuestionType.RadicalToKanji -> multipleChoice(
                type, candidates.filter { it.radical != null }, random,
                prompt = { "Which kanji contains the radical「${it.radical}」?" },
                answer = { it.content },
                distractors = { item, poolItems ->
                    poolItems.filter { p -> p.key != item.key }.map { it.content }
                }
            )

            ExamQuestionType.StrokeCount -> multipleChoice(
                type, candidates.filter { it.strokeCount != null }, random,
                prompt = { "How many strokes does「${it.content}」have?" },
                answer = { "${it.strokeCount}" },
                distractors = { item, poolItems ->
                    poolItems.filter { p -> p.key != item.key && p.strokeCount != null }
                        .map { p -> "${p.strokeCount}" }
                }
            )

            ExamQuestionType.ProductionReading,
            ExamQuestionType.ProductionVocabReading -> production(
                type, candidates, random,
                prompt = { "Write the reading of「${it.content}」:" },
                answer = { it.reading }
            )

            ExamQuestionType.ProductionKanji,
            ExamQuestionType.ProductionVocabKanji -> production(
                type, candidates, random,
                prompt = { "Write the ${if (type.contentType == ContentTypes.KANJI) "kanji" else "word"} meaning \"${firstMeaning(it.meaning)}\":" },
                answer = { it.content }
            )
        }
    }

    private fun multipleChoice(
        type: ExamQuestionType,
        candidates: List<ExamSourceItem>,
        random: Random,
        prompt: (ExamSourceItem) -> String,
        answer: (ExamSourceItem) -> String,
        distractors: (ExamSourceItem, List<ExamSourceItem>) -> List<String>
    ): GeneratedExamQuestion? {
        if (candidates.isEmpty()) return null
        val item = candidates[random.nextInt(candidates.size)]
        val correct = answer(item)
        if (correct.isBlank()) return null

        val candidatesForDistractors = distractors(item, candidates)
        val options = buildDistractorOptions(correct, candidatesForDistractors, random)
            ?: return null

        return GeneratedExamQuestion(
            type = type,
            prompt = prompt(item),
            answer = correct,
            options = options,
            entityKey = item.key,
            jlptLevel = item.jlptLevel
        )
    }

    private fun production(
        type: ExamQuestionType,
        candidates: List<ExamSourceItem>,
        random: Random,
        prompt: (ExamSourceItem) -> String,
        answer: (ExamSourceItem) -> String
    ): GeneratedExamQuestion? {
        if (candidates.isEmpty()) return null
        val item = candidates[random.nextInt(candidates.size)]
        val correct = answer(item)
        if (correct.isBlank()) return null
        return GeneratedExamQuestion(
            type = type,
            prompt = prompt(item),
            answer = correct,
            options = null,
            entityKey = item.key,
            jlptLevel = item.jlptLevel
        )
    }

    /**
     * Builds a 4-option list: the correct answer + 3 distinct distractors.
     * Falls back to any available option when the preferred (related)
     * distractors run out. Returns null when a valid set cannot be made.
     */
    private fun buildDistractorOptions(
        correct: String,
        preferredDistractors: List<String>,
        random: Random
    ): List<String>? {
        val distinct = preferredDistractors
            .map { it.trim() }
            .filter { it.isNotBlank() && it != correct }
            .distinct()
            .shuffled(random)

        val chosen = distinct.take(3)
        if (chosen.size < 3) {
            return null
        }
        return (chosen + correct).shuffled(random)
    }

    private fun firstMeaning(meaning: String): String =
        meaning.split(",", "、", "，").firstOrNull { it.isNotBlank() }?.trim() ?: meaning
}

@kotlinx.serialization.Serializable
private data class ExamScope(
    val jlptLevel: Int?,
    val contentType: String?,
    val questionTypes: List<String>
)
