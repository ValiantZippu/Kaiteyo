package ua.syt0r.kanji.desktop.engine.learning

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import ua.syt0r.kanji.desktop.model.ReviewRating
import kotlin.random.Random

// ============================================
// EXAM ENGINE
// A real exam system, not a five-random-cards
// quiz. Exams are generated from actual study
// state: selected decks, JLPT levels, weak areas
// and mistakes. Every question references a real
// note; every answer is evaluated; every result
// persists and feeds exam analytics.
//
//   * Skill exams   — kanji / vocab / radical /
//                     grammar, recognition +
//                     production directions
//   * JLPT simulation — a timed, sectioned exam
//                     (Vocabulary · Grammar ·
//                     Reading) mirroring the real
//                     JLPT structure and pacing
//   * Weekly        — what you actually studied
//                     this week
//   * Mistakes      — your real recorded mistakes
// ============================================

/** The kinds of exams the engine can generate. */
enum class ExamType(val label: String) {
    KanjiRecognition("Kanji recognition"),
    KanjiReading("Kanji reading"),
    KanjiMeaning("Kanji meaning"),
    VocabMeaning("Vocabulary meaning"),
    VocabReading("Vocabulary reading"),
    VocabProduction("Vocabulary production"),
    RadicalRecognition("Radical recognition"),
    GrammarStructure("Grammar structure"),
    GrammarUsage("Grammar usage"),
    MixedJlpt("Mixed JLPT-style"),
    JlptSimulation("JLPT simulation"),
    GeneratorMixed("Kanji workshop"),
    Mistakes("Mistakes review"),
    Weekly("Weekly assessment")
}

enum class ExamQuestionType(val label: String) {
    MultipleChoiceMeaning("recognition:meaning"),
    MultipleChoiceReading("recognition:reading"),
    TypedReading("production:reading"),
    TypedExpression("production:writing"),
    MultipleSelect("recognition:multiple"),
    Matching("recognition:matching"),
    /** Which pattern expresses this meaning (grammar production). */
    PatternSelection("production:grammar:pattern"),
    /** Fill the blank in a real example sentence (cloze). */
    SentenceCompletion("recognition:reading:cloze")
}

/** A generated question. Answers are normalized for fair evaluation. */
data class ExamQuestion(
    val id: String,
    val cardId: String,
    val noteId: String,
    val questionType: ExamQuestionType,
    val prompt: String,
    val correctAnswer: String,
    val options: List<String> = emptyList(), // multiple choice / select
    val jlpt: Int? = null,
    val deckId: String = ""
)

/** A timed section within an exam — the JLPT simulation uses several. */
data class ExamSection(
    val id: String,
    val label: String,
    val questions: List<ExamQuestion>,
    val timeLimitMs: Long = 0,
    val intro: String = ""
)

data class ExamDraft(
    val type: ExamType,
    val title: String,
    val sections: List<ExamSection>,
    val deckId: String = "",
    val jlpt: Int? = null,
    val weekly: Boolean = false,
    val timeLimitMs: Long = 0
) {
    /** Flattened questions — most UI reads the exam as one list. */
    val questions: List<ExamQuestion> get() = sections.flatMap { it.questions }

    fun sectionOf(questionId: String): String =
        sections.firstOrNull { s -> s.questions.any { it.id == questionId } }?.label ?: ""
}

data class ExamAnswer(
    val questionId: String,
    val answer: String = "",
    val confidence: Int = 0,
    val skipped: Boolean = false,
    val responseTimeMs: Long = 0
)

class ExamEngine(
    private val store: LearningStore,
    private val eventLog: ua.syt0r.kanji.desktop.engine.events.EventLog? = null
) {

    private val random = Random(System.nanoTime())

    // ------------------------------------------------------------
    // Generation
    // ------------------------------------------------------------

    /**
     * Generate an exam from real notes. [source] selects the pool:
     * deck id, JLPT level, "mistakes", or "week" (recently studied).
     */
    fun generate(
        type: ExamType,
        questionCount: Int = 20,
        deckId: String = "",
        jlpt: Int? = null,
        includeNew: Boolean = true,
        includeMature: Boolean = true,
        timeLimitMs: Long = 0,
        weekly: Boolean = false,
        now: Instant = Clock.System.now()
    ): ExamDraft? {
        // JLPT simulation is a sectioned exam — build it directly.
        if (type == ExamType.JlptSimulation) {
            return buildJlptSimulation(jlpt = jlpt, deckId = deckId, now = now)
        }
        // The generator-mix exam is built from the standalone question
        // generators (meaning/reading matching, single-kanji cloze, free
        // response) over the real vocab pool — question shapes the engine's
        // own generator doesn't produce.
        if (type == ExamType.GeneratorMixed) {
            return buildGeneratorMixed(deckId = deckId, jlpt = jlpt, questionCount = questionCount, timeLimitMs = timeLimitMs)
        }
        val pool = selectPool(type, deckId, jlpt, includeNew, includeMature, now)
        if (pool.isEmpty()) return null

        val shuffled = pool.shuffled(random).take(questionCount)
        val questions = shuffled.mapNotNull { entry ->
            generateQuestion(type, entry) ?: return@mapNotNull null
        }
        if (questions.isEmpty()) return null

        return ExamDraft(
            type = type,
            title = titleFor(type, deckId, jlpt, weekly),
            sections = listOf(
                ExamSection(
                    id = "s1",
                    label = sectionLabelFor(type),
                    questions = questions,
                    timeLimitMs = timeLimitMs
                )
            ),
            deckId = deckId,
            jlpt = jlpt,
            weekly = weekly,
            timeLimitMs = timeLimitMs
        )
    }

    /** Weekly assessment — generated from what was actually studied this week. */
    fun generateWeekly(now: Instant = Clock.System.now()): ExamDraft? {
        val tz = TimeZone.currentSystemDefault()
        val today = now.toLocalDateTime(tz).date
        val weekStart = today.minus(6, DateTimeUnit.DAY)
        val studiedThisWeek = store.reviewEvents.filter {
            it.reviewedAt.toLocalDateTime(tz).date >= weekStart
        }
        val noteIds = studiedThisWeek.map { it.noteId }.distinct().take(40)
        if (noteIds.isEmpty()) return null
        val notes = store.notes.filter { it.id in noteIds }
        if (notes.isEmpty()) return null

        val questions = notes.shuffled(random).take(30).mapNotNull { note ->
            val card = store.cards.firstOrNull { it.noteId == note.id }
            generateQuestionFor(note, card, ExamQuestionType.MultipleChoiceMeaning) ?:
                generateQuestionFor(note, card, ExamQuestionType.TypedReading)
        }
        if (questions.isEmpty()) return null
        return ExamDraft(
            type = ExamType.Weekly,
            title = "Weekly assessment — ${today.month.name.lowercase().replaceFirstChar { it.uppercase() }} week",
            sections = listOf(
                ExamSection(id = "s1", label = "This week's study", questions = questions, timeLimitMs = 30 * 60_000L)
            ),
            weekly = true,
            timeLimitMs = 30 * 60_000L
        )
    }

    /**
     * A full JLPT simulation: three timed sections mirroring the real exam
     * structure — 文字・語彙 (vocabulary), 文法 (grammar), 読解 (reading).
     * Every question comes from real notes at the requested level.
     */
    private fun buildJlptSimulation(jlpt: Int?, deckId: String, now: Instant): ExamDraft? {
        val base = jlptNotes(jlpt, deckId)
        val vocabNotes = base.filter { it.kind == LearningItemKind.Kanji || it.kind == LearningItemKind.Vocabulary }
        val grammarNotes = base.filter { it.kind == LearningItemKind.Grammar }
        // Reading section uses cloze sentences from vocab + grammar examples.
        val readingPool = (vocabNotes + grammarNotes).filter {
            it.examples.any { ex -> it.expression.isNotBlank() && ex.contains(it.expression) }
        }

        val sections = mutableListOf<ExamSection>()

        if (vocabNotes.isNotEmpty()) {
            val count = (vocabNotes.size).coerceAtMost(12)
            val questions = vocabNotes.shuffled(random).take(count).mapNotNull { note ->
                val card = store.cards.firstOrNull { it.noteId == note.id }
                when (random.nextInt(10)) {
                    in 0..4 -> generateQuestionFor(note, card, ExamQuestionType.MultipleChoiceMeaning)
                    in 5..6 -> generateQuestionFor(note, card, ExamQuestionType.MultipleChoiceReading)
                    in 7..8 -> generateQuestionFor(note, card, ExamQuestionType.TypedReading)
                    else -> generateQuestionFor(note, card, ExamQuestionType.TypedExpression)
                }
            }
            if (questions.isNotEmpty()) {
                sections.add(
                    ExamSection(
                        id = "vocab",
                        label = "Vocabulary",
                        questions = questions,
                        timeLimitMs = (questions.size * 40_000L).coerceAtLeast(2 * 60_000L),
                        intro = "文字・語彙 — kanji and vocabulary. ~40s per question."
                    )
                )
            }
        }

        if (grammarNotes.isNotEmpty()) {
            val count = (grammarNotes.size).coerceAtMost(8)
            val questions = grammarNotes.shuffled(random).take(count).mapNotNull { note ->
                val card = store.cards.firstOrNull { it.noteId == note.id }
                when (random.nextInt(2)) {
                    0 -> generateQuestionFor(note, card, ExamQuestionType.PatternSelection)
                    else -> generateQuestionFor(note, card, ExamQuestionType.SentenceCompletion)
                }
            }
            if (questions.isNotEmpty()) {
                sections.add(
                    ExamSection(
                        id = "grammar",
                        label = "Grammar",
                        questions = questions,
                        timeLimitMs = (questions.size * 50_000L).coerceAtLeast(2 * 60_000L),
                        intro = "文法 — pattern meaning and sentence completion. ~50s per question."
                    )
                )
            }
        }

        if (readingPool.isNotEmpty()) {
            val count = (readingPool.size).coerceAtMost(5)
            val questions = readingPool.shuffled(random).take(count).mapNotNull { note ->
                val card = store.cards.firstOrNull { it.noteId == note.id }
                generateQuestionFor(note, card, ExamQuestionType.SentenceCompletion)
            }
            if (questions.isNotEmpty()) {
                sections.add(
                    ExamSection(
                        id = "reading",
                        label = "Reading",
                        questions = questions,
                        timeLimitMs = (questions.size * 75_000L).coerceAtLeast(2 * 60_000L),
                        intro = "読解 — complete the sentence in context. ~75s per question."
                    )
                )
            }
        }

        if (sections.isEmpty()) return null
        val total = sections.sumOf { it.questions.size }
        return ExamDraft(
            type = ExamType.JlptSimulation,
            title = if (jlpt != null) "JLPT N$jlpt simulation" else "JLPT simulation (mixed levels)",
            sections = sections,
            deckId = deckId,
            jlpt = jlpt,
            timeLimitMs = total * 50_000L
        )
    }

    /** Notes available for a JLPT exam at the requested level / deck. */
    private fun jlptNotes(jlpt: Int?, deckId: String): List<LearningNote> {
        var notes: List<LearningNote> = store.notes.toList()
        if (jlpt != null) notes = notes.filter { it.jlpt == jlpt }
        if (deckId.isNotBlank()) {
            val ids = store.cardsForDeck(deckId).map { it.noteId }.toSet()
            notes = notes.filter { it.id in ids }
        }
        return notes
    }

    private fun selectPool(
        type: ExamType,
        deckId: String,
        jlpt: Int?,
        includeNew: Boolean,
        includeMature: Boolean,
        now: Instant
    ): List<Pair<NoteCard?, LearningNote>> {
        var cards = when {
            deckId.isNotBlank() -> store.cardsForDeck(deckId)
            else -> store.cards.toList()
        }
        if (!includeNew) cards = cards.filter { !it.isNew }
        if (!includeMature && type != ExamType.Mistakes) cards = cards.filter { it.stage != LearningStage.Mature }
        if (jlpt != null) {
            val noteIds = store.notes.filter { it.jlpt == jlpt }.map { it.id }.toSet()
            cards = cards.filter { it.noteId in noteIds }
        }
        // Restrict to the card types the exam tests.
        cards = cards.filter { it.cardType in cardTypesFor(type) }

        // Grammar exams work on grammar notes even before cards exist for them
        // — a note is enough to build pattern / cloze questions from. Cards are
        // optional for question generation, so these enter the pool as (null, note).
        var extraNotes: List<LearningNote> = emptyList()
        if (type == ExamType.GrammarStructure || type == ExamType.GrammarUsage) {
            val existing = cards.map { it.noteId }.toSet()
            val deckNoteIds = store.cardsForDeck(deckId).map { it.noteId }.toSet()
            extraNotes = store.notes
                .filter { it.kind == LearningItemKind.Grammar }
                .filter { it.id !in existing }
                .filter { jlpt == null || it.jlpt == jlpt }
                .filter { deckId.isBlank() || it.id in deckNoteIds }
        }

        // Mistake exams are built from actual failures, not the whole deck.
        if (type == ExamType.Mistakes) {
            val snapshot = StatisticsRepository.mistakeSnapshot(store)
            val ids = (snapshot.againCardIds + snapshot.writingNoteIds + snapshot.examWrongCardIds + snapshot.lapsedCardIds)
                .distinct().take(60)
            cards = cards.filter { it.id in ids }
        }
        // Weekly exams use recently studied notes.
        if (type == ExamType.Weekly) {
            val weekStart = now.minus(7L, DateTimeUnit.DAY, TimeZone.currentSystemDefault())
            val studiedIds = store.reviewEvents.filter { it.reviewedAt >= weekStart }
                .map { it.noteId }.distinct()
            cards = cards.filter { it.noteId in studiedIds }
        }
        val pairs = cards.mapNotNull { card -> store.note(card.noteId)?.let { card to it } }
            .distinctBy { it.first.id }
        val extras = extraNotes.map { note -> null to note }
        return (pairs + extras).distinctBy { it.second.id }
    }

    private fun cardTypesFor(type: ExamType): List<CardType> = when (type) {
        ExamType.KanjiRecognition -> listOf(CardType.Recognition, CardType.Meaning)
        ExamType.KanjiReading -> listOf(CardType.Reading)
        ExamType.KanjiMeaning -> listOf(CardType.Meaning, CardType.Recognition)
        ExamType.VocabMeaning -> listOf(CardType.Meaning, CardType.Recognition)
        ExamType.VocabReading -> listOf(CardType.Reading)
        ExamType.VocabProduction -> listOf(CardType.Production, CardType.Writing)
        ExamType.RadicalRecognition -> listOf(CardType.Recognition, CardType.Meaning)
        ExamType.GrammarStructure -> listOf(CardType.Pattern, CardType.Meaning, CardType.Cloze)
        ExamType.GrammarUsage -> listOf(CardType.Cloze, CardType.Pattern, CardType.Meaning)
        ExamType.MixedJlpt, ExamType.Mistakes, ExamType.Weekly, ExamType.JlptSimulation, ExamType.GeneratorMixed -> CardType.entries.toList()
    }

    private fun generateQuestion(type: ExamType, entry: Pair<NoteCard?, LearningNote>): ExamQuestion? {
        val (card, note) = entry
        return when (type) {
            ExamType.KanjiRecognition, ExamType.KanjiMeaning,
            ExamType.VocabMeaning, ExamType.RadicalRecognition -> generateQuestionFor(note, card, ExamQuestionType.MultipleChoiceMeaning)
            ExamType.KanjiReading, ExamType.VocabReading -> generateQuestionFor(note, card, ExamQuestionType.TypedReading)
            ExamType.VocabProduction -> generateQuestionFor(note, card, ExamQuestionType.TypedExpression)
            ExamType.GrammarStructure -> when (random.nextInt(2)) {
                0 -> generateQuestionFor(note, card, ExamQuestionType.PatternSelection)
                else -> generateQuestionFor(note, card, ExamQuestionType.MultipleChoiceMeaning)
            }
            ExamType.GrammarUsage -> when (random.nextInt(3)) {
                0 -> generateQuestionFor(note, card, ExamQuestionType.SentenceCompletion)
                1 -> generateQuestionFor(note, card, ExamQuestionType.PatternSelection)
                else -> generateQuestionFor(note, card, ExamQuestionType.MultipleChoiceMeaning)
            }
            ExamType.MixedJlpt -> when (random.nextInt(5)) {
                0 -> generateQuestionFor(note, card, ExamQuestionType.TypedReading)
                1 -> generateQuestionFor(note, card, ExamQuestionType.TypedExpression)
                2 -> generateQuestionFor(note, card, ExamQuestionType.PatternSelection)
                else -> generateQuestionFor(note, card, ExamQuestionType.MultipleChoiceMeaning)
            }
            ExamType.Mistakes, ExamType.Weekly, ExamType.JlptSimulation, ExamType.GeneratorMixed -> when (random.nextInt(3)) {
                0 -> generateQuestionFor(note, card, ExamQuestionType.MultipleChoiceMeaning)
                1 -> generateQuestionFor(note, card, ExamQuestionType.TypedReading)
                else -> generateQuestionFor(note, card, ExamQuestionType.TypedExpression)
            }
        }
    }

    /**
     * Build the "Kanji workshop" exam from the standalone question
     * generators: meaning matching, reading matching, single-kanji cloze and
     * free response over the real vocab pool at the requested scope. Ordering
     * questions are excluded — they need a sequence UI the single-answer
     * runner doesn't have yet (they remain available through the generators
     * for a future dedicated surface).
     */
    private fun buildGeneratorMixed(
        deckId: String,
        jlpt: Int?,
        questionCount: Int,
        timeLimitMs: Long
    ): ExamDraft? {
        val notes = jlptNotes(jlpt, deckId)
            .filter { it.kind == LearningItemKind.Kanji || it.kind == LearningItemKind.Vocabulary }
            .take(questionCount + 12)
        if (notes.isEmpty()) return null

        val items = notes.map { note ->
            ExamVocabItem(
                expression = note.expression,
                reading = note.allReadings.firstOrNull().orEmpty(),
                meaning = note.meanings.joinToString("; ")
            )
        }
        val generated = ExamQuestionGenerators.mixedExam(items)
        val questions = generated
            .filter { it.type != GeneratorQuestionType.Ordering }
            .mapNotNull(::convertGenerator)
            .take(questionCount)
        if (questions.isEmpty()) return null

        return ExamDraft(
            type = ExamType.GeneratorMixed,
            title = "Kanji workshop" + if (jlpt != null) " · JLPT N$jlpt" else "",
            sections = listOf(
                ExamSection(
                    id = "workshop",
                    label = "Kanji workshop",
                    questions = questions,
                    timeLimitMs = timeLimitMs
                )
            ),
            deckId = deckId,
            jlpt = jlpt,
            timeLimitMs = timeLimitMs
        )
    }

    /** Map a generator question onto the engine's evaluatable model. */
    private fun convertGenerator(q: GeneratorQuestion): ExamQuestion? {
        val correctAnswer = q.correct.firstOrNull() ?: return null
        val engineType = when (q.type) {
            // Matching is used by both meaning and reading generators; the
            // context discriminates: reading matching carries the English
            // meaning (ASCII), meaning matching carries the kana reading.
            GeneratorQuestionType.Matching ->
                if (q.context.isNotBlank() && q.context.none { it.code > 127 }) ExamQuestionType.MultipleChoiceReading
                else ExamQuestionType.MultipleChoiceMeaning
            GeneratorQuestionType.Cloze -> ExamQuestionType.SentenceCompletion
            GeneratorQuestionType.Ordering -> ExamQuestionType.MultipleChoiceReading
            GeneratorQuestionType.FreeResponse -> ExamQuestionType.TypedExpression
            GeneratorQuestionType.Timed -> ExamQuestionType.MultipleChoiceMeaning
        }
        return ExamQuestion(
            id = LearningIds.eventId("q"),
            cardId = "",
            noteId = "",
            questionType = engineType,
            prompt = q.prompt,
            correctAnswer = correctAnswer,
            options = q.options,
            jlpt = null,
            deckId = ""
        )
    }

    /**
     * Build one question. Multiple-choice distractors are real notes of the
     * same kind (same JLPT band when possible) so the exam is fair and the
     * options are never garbage strings.
     */
    private fun generateQuestionFor(
        note: LearningNote,
        card: NoteCard?,
        questionType: ExamQuestionType
    ): ExamQuestion? {
        val id = LearningIds.eventId("q")
        val cardId = card?.id ?: ""
        return when (questionType) {
            ExamQuestionType.MultipleChoiceMeaning -> {
                val prompt = "What does ${note.expression}${note.reading.takeIf { it.isNotBlank() }?.let { " ($it)" } ?: ""} mean?"
                val correct = note.meanings.firstOrNull() ?: return null
                val distractors = distractorMeanings(note)
                ExamQuestion(
                    id = id, cardId = cardId, noteId = note.id,
                    questionType = questionType, prompt = prompt,
                    correctAnswer = correct, options = (listOf(correct) + distractors).shuffled(random),
                    jlpt = note.jlpt, deckId = card?.deckId.orEmpty()
                )
            }
            ExamQuestionType.MultipleChoiceReading -> {
                val reading = note.allReadings.firstOrNull() ?: return null
                val prompt = "How do you read ${note.expression}?"
                val distractors = distractorReadings(note)
                ExamQuestion(
                    id = id, cardId = cardId, noteId = note.id,
                    questionType = questionType, prompt = prompt,
                    correctAnswer = reading, options = (listOf(reading) + distractors).shuffled(random),
                    jlpt = note.jlpt, deckId = card?.deckId.orEmpty()
                )
            }
            ExamQuestionType.TypedReading -> {
                val reading = note.allReadings.firstOrNull() ?: return null
                val prompt = "Type the reading of ${note.expression} (kana)."
                ExamQuestion(
                    id = id, cardId = cardId, noteId = note.id,
                    questionType = questionType, prompt = prompt,
                    correctAnswer = reading,
                    jlpt = note.jlpt, deckId = card?.deckId.orEmpty()
                )
            }
            ExamQuestionType.TypedExpression -> {
                val prompt = "Write the ${note.kind.label.lowercase()} for: ${note.meanings.firstOrNull() ?: return null}"
                ExamQuestion(
                    id = id, cardId = cardId, noteId = note.id,
                    questionType = questionType, prompt = prompt,
                    correctAnswer = note.expression,
                    jlpt = note.jlpt, deckId = card?.deckId.orEmpty()
                )
            }
            ExamQuestionType.PatternSelection -> {
                val meaning = note.meanings.firstOrNull() ?: return null
                val prompt = "Which pattern matches this meaning?\n${meaning.take(140)}"
                val distractors = distractorExpressions(note)
                ExamQuestion(
                    id = id, cardId = cardId, noteId = note.id,
                    questionType = questionType, prompt = prompt,
                    correctAnswer = note.expression,
                    options = (listOf(note.expression) + distractors).shuffled(random),
                    jlpt = note.jlpt, deckId = card?.deckId.orEmpty()
                )
            }
            ExamQuestionType.SentenceCompletion -> {
                if (note.expression.length < 2) return null
                val example = note.examples.firstOrNull { it.contains(note.expression) } ?: return null
                val blanked = example.replaceFirst(note.expression, "＿＿＿")
                val distractors = distractorExpressions(note)
                ExamQuestion(
                    id = id, cardId = cardId, noteId = note.id,
                    questionType = questionType, prompt = "Complete the sentence:\n$blanked",
                    correctAnswer = note.expression,
                    options = (listOf(note.expression) + distractors).shuffled(random),
                    jlpt = note.jlpt, deckId = card?.deckId.orEmpty()
                )
            }
            ExamQuestionType.MultipleSelect -> {
                val prompt = "Select all correct meanings of ${note.expression}:"
                val correct = note.meanings.take(2).ifEmpty { return null }
                val distractors = distractorMeanings(note).take(4)
                ExamQuestion(
                    id = id, cardId = cardId, noteId = note.id,
                    questionType = questionType, prompt = prompt,
                    correctAnswer = correct.joinToString("|"),
                    options = (correct + distractors).distinct().shuffled(random),
                    jlpt = note.jlpt, deckId = card?.deckId.orEmpty()
                )
            }
            ExamQuestionType.Matching -> {
                // Matching pairs aren't generated per-note; fall back to reading.
                return generateQuestionFor(note, card, ExamQuestionType.TypedReading)
            }
        }
    }

    /** Real meanings from other notes of the same kind (same JLPT when possible). */
    private fun distractorMeanings(note: LearningNote, count: Int = 3): List<String> {
        val candidates = store.notes
            .filter { it.kind == note.kind && it.id != note.id }
            .filter { it.jlpt == note.jlpt || it.jlpt == null }
            .flatMap { it.meanings }
            .filter { it.isNotBlank() && it != note.meanings.firstOrNull() }
            .distinct()
        return candidates.shuffled(random).take(count)
    }

    /** Real readings from other notes (distractors must be plausible kana). */
    private fun distractorReadings(note: LearningNote, count: Int = 3): List<String> {
        val candidates = store.notes
            .filter { it.kind == note.kind && it.id != note.id }
            .flatMap { it.allReadings }
            .filter { it.isNotBlank() && it != note.allReadings.firstOrNull() }
            .filter { it.matches(Regex("[\\u3040-\\u30FF]+")) }
            .distinct()
        return candidates.shuffled(random).take(count)
    }

    /** Other expressions of the same kind — pattern / word distractors. */
    private fun distractorExpressions(note: LearningNote, count: Int = 3): List<String> {
        val candidates = store.notes
            .filter { it.kind == note.kind && it.id != note.id }
            .filter { it.jlpt == note.jlpt || it.jlpt == null }
            .map { it.expression }
            .filter { it.isNotBlank() && it != note.expression }
            .distinct()
        return candidates.shuffled(random).take(count)
    }

    // ------------------------------------------------------------
    // Evaluation
    // ------------------------------------------------------------

    /** Normalize an answer for fair comparison (strip spaces/punctuation). */
    fun normalize(text: String): String =
        text.trim().lowercase().replace(Regex("[\\s・。、,，.!！?？()（）\\[\\]「」『』]"), "")

    /**
     * Evaluate answers against the draft. Returns a completed ExamResult with
     * per-question detail and persists it to the store.
     */
    fun evaluate(
        draft: ExamDraft,
        answers: Map<String, ExamAnswer>,
        startedAt: Instant,
        now: Instant = Clock.System.now(),
        skippedConfidence: Int = 0
    ): ExamResult {
        val results = draft.questions.map { q ->
            val answer = answers[q.id]
            val answered = answer?.answer?.trim().orEmpty()
            val correct = when {
                answer?.skipped == true || answered.isEmpty() -> false
                q.options.isNotEmpty() && q.questionType != ExamQuestionType.MultipleSelect ->
                    answered == q.correctAnswer
                q.questionType == ExamQuestionType.MultipleSelect -> {
                    val selected = answered.split("|").map { it.trim() }
                    val expected = q.correctAnswer.split("|").toSet()
                    selected.isNotEmpty() && selected.toSet() == expected
                }
                else -> normalize(answered) == normalize(q.correctAnswer)
            }
            ExamQuestionResult(
                questionId = q.id,
                cardId = q.cardId,
                noteId = q.noteId,
                questionType = q.questionType.label,
                correct = correct,
                answer = answered,
                correctAnswer = q.correctAnswer,
                confidence = answer?.confidence ?: skippedConfidence,
                responseTimeMs = answer?.responseTimeMs ?: 0,
                jlpt = q.jlpt,
                category = q.deckId,
                section = draft.sectionOf(q.id)
            )
        }
        val result = ExamResult(
            id = LearningIds.eventId("exam"),
            title = draft.title,
            examType = draft.type.name,
            startedAt = startedAt,
            finishedAt = now,
            questionCount = draft.questions.size,
            correctCount = results.count { it.correct },
            skippedCount = results.count { it.answer.isBlank() },
            timeLimitMs = draft.timeLimitMs,
            deckId = draft.deckId,
            jlpt = draft.jlpt,
            weekly = draft.weekly,
            questions = results
        )
        store.recordExam(result)

        // Append the exam fact to the domain event log (EVENT_CATALOG).
        eventLog?.record(
            ua.syt0r.kanji.desktop.engine.events.EventType.ExamCompleted,
            source = "exam",
            payload = mapOf(
                "examId" to result.id,
                "title" to result.title,
                "examType" to result.examType,
                "score" to result.percentage.toString(),
                "correct" to result.correctCount.toString(),
                "questions" to result.questionCount.toString(),
                "timeMs" to result.timeLimitMs.toString()
            )
        )

        // Exams also feed the review log — answers are real study activity,
        // so they update SRS for the tested cards exactly like normal reviews.
        val events = mutableListOf<LearningReviewEvent>()
        draft.questions.forEach { q ->
            val r = results.first { it.questionId == q.id }
            val card = store.card(q.cardId) ?: return@forEach
            val rating = when {
                r.correct && r.confidence >= 3 -> ReviewRating.Easy
                r.correct -> ReviewRating.Good
                else -> ReviewRating.Again
            }
            val grade = StudyEngine(store).grade(
                item = StudyQueueItem(card, store.note(q.noteId) ?: return@forEach),
                rating = rating,
                activityType = StudyActivityType.Exam,
                mistakes = if (r.correct) emptyList() else listOf("Exam: ${q.questionType.label}"),
                examId = result.id,
                mode = card.cardType.toStudyMode(),
                now = now
            )
            events.add(grade.event)
        }
        return result
    }

    // ------------------------------------------------------------
    // Titles
    // ------------------------------------------------------------
    private fun titleFor(type: ExamType, deckId: String, jlpt: Int?, weekly: Boolean): String {
        if (weekly) return "Weekly assessment"
        if (type == ExamType.GeneratorMixed) {
            return "Kanji workshop" + if (jlpt != null) " · JLPT N$jlpt" else ""
        }
        val scope = when {
            jlpt != null -> "JLPT N$jlpt"
            deckId.isNotBlank() -> "Deck"
            else -> "All content"
        }
        return "${type.label} · $scope"
    }

    private fun sectionLabelFor(type: ExamType): String = when (type) {
        ExamType.GrammarStructure, ExamType.GrammarUsage -> "Grammar"
        ExamType.GeneratorMixed -> "Kanji workshop"
        else -> type.label
    }

    // ------------------------------------------------------------
    // Analytics conveniences (thin wrappers over the repository)
    // ------------------------------------------------------------
    fun history(limit: Int = 30): List<ExamResult> = StatisticsRepository.examHistory(store, limit)
    fun aggregates(): ExamAggregates = StatisticsRepository.examAggregates(store)
}
