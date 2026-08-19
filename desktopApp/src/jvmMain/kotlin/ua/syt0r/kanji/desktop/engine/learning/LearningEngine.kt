package ua.syt0r.kanji.desktop.engine.learning

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import ua.syt0r.kanji.desktop.model.DesktopCard
import ua.syt0r.kanji.desktop.model.ReviewRating

// ============================================
// LEARNING ENGINE
// The facade every new screen talks to. Owns the
// unified store and exposes the study, exam,
// mistake, statistics and import/export systems
// behind one object. Also bridges the legacy
// DesktopCard pool so old and new systems share
// the same underlying learning data.
// ============================================

class LearningEngine(
    private val store: LearningStore = LearningStore(),
    eventLog: ua.syt0r.kanji.desktop.engine.events.EventLog? = null
) {
    val study = StudyEngine(store)
    val exams = ExamEngine(store, eventLog)
    val mistakes = MistakeEngine(store)
    val stats = StatisticsRepository
    val goals = GoalsRepository

    val notes get() = store.notes
    val cards get() = store.cards
    val reviewEvents get() = store.reviewEvents
    val writingAttempts get() = store.writingAttempts
    val examResults get() = store.examResults
    val sessions get() = store.sessions
    val revision get() = store.revision

    // ------------------------------------------------------------
    // Legacy bridge
    // ------------------------------------------------------------

    /**
     * Synchronize the unified store with the legacy DesktopCard pool.
     * Notes are deduplicated by expression; cards keep their SRS state
     * and are never reset. Safe to call on every launch — unchanged
     * cards are no-ops, and removed cards are dropped.
     */
    fun syncFromLegacy(cards: List<DesktopCard>, now: Instant = Clock.System.now()) {
        val legacyIds = cards.map { it.id }.toSet()
        store.cards.filterNot { it.id in legacyIds }.forEach { store.removeCard(it.id) }

        cards.forEach { legacy ->
            val kind = legacy.contentKind.toLearningItemKind()
            val note = LearningNote(
                id = LearningIds.noteId(kind, legacy.character, legacy.onReadings.firstOrNull().orEmpty()),
                kind = kind,
                expression = legacy.character,
                meanings = legacy.meaning.split("; ").map { it.trim() }.filter { it.isNotBlank() },
                reading = legacy.onReadings.firstOrNull().orEmpty(),
                onReadings = legacy.onReadings,
                kunReadings = legacy.kunReadings,
                radicals = legacy.radicals,
                components = legacy.components,
                strokeCount = legacy.strokeCount,
                jlpt = legacy.jlpt,
                grade = legacy.grade,
                frequency = legacy.frequency,
                tags = legacy.tags,
                examples = legacy.note.takeIf { it.isNotBlank() }?.let { listOf(it) }.orEmpty(),
                source = NoteSource(NoteSourceType.Builtin),
                createdAt = legacy.createdAt,
                updatedAt = now
            )
            store.upsertNote(note)

            // Generate the default card set for the deck this legacy card
            // belonged to, then fold the legacy SRS state onto the matching
            // generated card so state is never lost.
            val deckId = legacy.deckId
            val config = store.deckConfig(deckId)
            val generated = CardGenerator.generateForDeck(note, deckId, config, existing = store.cardsForDeck(deckId).filter { it.noteId == note.id }, now = now)
            val primary = generated.firstOrNull() ?: return@forEach
            val withState = primary.copy(
                status = legacy.status,
                intervalDays = legacy.intervalDays,
                dueAt = legacy.dueAt,
                lapses = legacy.lapses,
                reps = legacy.reps,
                ease = legacy.ease,
                accuracy = legacy.accuracy,
                lastReviewedAt = legacy.lastReviewedAt
            )
            store.upsertCard(withState)
            generated.filter { it.id != primary.id }.forEach { store.upsertCard(it) }
        }
        store.save()
    }

    /**
     * Record a review that happened through the legacy flow (AppState review
     * sessions) so statistics stay complete without double-scheduling SRS.
     * Appends an immutable event; the legacy path already updated SRS.
     */
    fun recordLegacyReview(
        legacyCard: DesktopCard,
        rating: ReviewRating,
        responseTimeMs: Long = 0,
        activityType: StudyActivityType = StudyActivityType.Review,
        now: Instant = Clock.System.now()
    ) {
        val kind = legacyCard.contentKind.toLearningItemKind()
        var note = store.noteByExpression(kind, legacyCard.character)
        if (note == null) {
            syncFromLegacy(listOf(legacyCard), now)
            note = store.noteByExpression(kind, legacyCard.character)
        }
        if (note == null) return

        val card = store.cards.firstOrNull { it.noteId == note.id } ?: return
        store.recordReview(
            LearningReviewEvent(
                id = LearningIds.eventId("review"),
                cardId = card.id,
                noteId = note.id,
                deckId = card.deckId,
                cardType = card.cardType,
                activityType = activityType,
                rating = rating,
                reviewedAt = now,
                responseTimeMs = responseTimeMs,
                statusBefore = card.status,
                statusAfter = legacyCard.status,
                intervalBefore = card.intervalDays,
                intervalAfter = legacyCard.intervalDays,
                wasNew = legacyCard.status == ua.syt0r.kanji.desktop.model.SrsStatus.New,
                lapsesAfter = legacyCard.lapses
            )
        )
    }

    /**
     * Record a writing attempt from the legacy writing flow. Accuracy is the
     * self-evaluated result (Again = miss). [strokes] carries the real
     * per-stroke evaluation produced by the stroke evaluator when the canvas
     * captured the attempt; statistics derive from both.
     */
    fun recordWritingAttempt(
        card: DesktopCard,
        expected: String,
        accuracy: Float,
        mistakeCount: Int = 0,
        completed: Boolean = true,
        durationMs: Long = 0,
        strokes: List<ua.syt0r.kanji.desktop.engine.learning.StrokeAttempt> = emptyList(),
        now: Instant = Clock.System.now()
    ) {
        val kind = card.contentKind.toLearningItemKind()
        var note = store.noteByExpression(kind, expected)
        if (note == null) {
            syncFromLegacy(listOf(card), now)
            note = store.noteByExpression(kind, expected)
        }
        if (note == null) return
        val generatedCard = store.cards.firstOrNull { it.noteId == note.id && it.cardType == CardType.Writing }
            ?: store.cards.firstOrNull { it.noteId == note.id }
            ?: return
        store.recordWriting(
            WritingAttemptEvent(
                id = LearningIds.eventId("writing"),
                cardId = generatedCard.id,
                noteId = note.id,
                deckId = generatedCard.deckId,
                attempted = expected,
                expected = expected,
                strokes = strokes,
                accuracy = accuracy.coerceIn(0f, 1f),
                mistakeCount = mistakeCount,
                completed = completed,
                durationMs = durationMs,
                attemptedAt = now
            )
        )
    }

    /**
     * Convenience for the writing canvas: evaluate the captured freehand
     * strokes against the expected character and record the attempt with the
     * real per-stroke data. Returns the evaluation so the UI can show it.
     */
    fun recordEvaluatedWriting(
        card: DesktopCard,
        expected: String,
        drawnStrokes: List<List<ua.syt0r.kanji.desktop.engine.stroke_evaluator.StrokePoint>>,
        canvasWidth: Float,
        canvasHeight: Float,
        selfRating: ReviewRating,
        durationMs: Long = 0,
        now: Instant = Clock.System.now(),
        evaluator: ua.syt0r.kanji.desktop.engine.stroke_evaluator.WritingEvaluator? = null
    ): ua.syt0r.kanji.desktop.engine.stroke_evaluator.StrokeEvaluationResult? {
        // The facade routes through the real KanjiVG stack when a licensed
        // dataset directory is present; otherwise it uses the built-in
        // common-kanji dataset. Either way the result shape is the same.
        val result = if (evaluator != null) {
            val evaluated = evaluator.evaluate(
                expression = expected,
                drawnStrokes = drawnStrokes,
                canvasWidth = canvasWidth.toDouble(),
                canvasHeight = canvasHeight.toDouble()
            )
            if (evaluated.supported) {
                ua.syt0r.kanji.desktop.engine.stroke_evaluator.StrokeEvaluationResult(
                    expression = expected,
                    strokeEvaluations = evaluated.strokes,
                    accuracy = evaluated.accuracy,
                    supported = true
                )
            } else null
        } else {
            ua.syt0r.kanji.desktop.engine.stroke_evaluator.StrokeEvaluator.evaluate(
                expression = expected,
                drawnStrokes = drawnStrokes,
                canvasWidth = canvasWidth.toDouble(),
                canvasHeight = canvasHeight.toDouble()
            ).takeIf { it.supported }
        }
        if (result == null) {
            // No canonical data for this character — record a plain attempt
            // with the self-graded accuracy instead of fabricating strokes.
            recordWritingAttempt(
                card = card,
                expected = expected,
                accuracy = if (selfRating != ReviewRating.Again) 1f else 0.3f,
                mistakeCount = if (selfRating == ReviewRating.Again) 1 else 0,
                completed = true,
                durationMs = durationMs,
                now = now
            )
            return null
        }
        val accuracy = if (selfRating == ReviewRating.Again) {
            (result.accuracy * 0.4f).coerceAtMost(0.5f)
        } else {
            result.accuracy
        }
        recordWritingAttempt(
            card = card,
            expected = expected,
            accuracy = accuracy,
            mistakeCount = result.strokeEvaluations.count { !it.correct },
            completed = true,
            durationMs = durationMs,
            strokes = result.strokeEvaluations.map { ev ->
                ua.syt0r.kanji.desktop.engine.learning.StrokeAttempt(
                    strokeIndex = ev.strokeIndex,
                    correct = ev.correct,
                    deviation = ev.deviation,
                    mistake = when (ev.mistake) {
                        ua.syt0r.kanji.desktop.engine.stroke_evaluator.StrokeMistake.None -> ""
                        ua.syt0r.kanji.desktop.engine.stroke_evaluator.StrokeMistake.Shape -> "shape"
                        ua.syt0r.kanji.desktop.engine.stroke_evaluator.StrokeMistake.Direction -> "direction"
                        ua.syt0r.kanji.desktop.engine.stroke_evaluator.StrokeMistake.ShapeAndDirection -> "shape+direction"
                    }
                )
            },
            now = now
        )
        return result
    }

    // ------------------------------------------------------------
    // Deck convenience
    // ------------------------------------------------------------

    /** Materialize legacy cards for a deck (bridges to the old views). */
    fun legacyCardsForDeck(deckId: String): List<DesktopCard> =
        store.cardsForDeck(deckId).mapNotNull { store.toDesktopCard(it) }

    /** The persisted study config for a deck (created with defaults on first access). */
    fun deckStudyConfig(deckId: String): DeckStudyConfig = store.deckConfig(deckId)

    /** Persist a deck's study settings (limits, steps, intervals, card types). */
    fun saveDeckStudyConfig(config: DeckStudyConfig) = store.setDeckConfig(config)

    // ------------------------------------------------------------
    // Mistakes convenience
    // ------------------------------------------------------------
    fun mistakeQueue(limit: Int = 200): List<MistakeItem> = mistakes.queue(limit)

    fun mistakeBreakdown(): Map<MistakeCategory, Int> = mistakes.breakdown()

    /** Materialize mistake cards as legacy DesktopCards so the review flow can study them. */
    fun mistakeCards(limit: Int = 50): List<DesktopCard> =
        mistakes.asStudyQueue(limit).mapNotNull { store.toDesktopCard(it.card) }

    // ------------------------------------------------------------
    // Import / export convenience (full learning-data fidelity)
    // ------------------------------------------------------------
    fun exportSnapshotJson(): String = ImportExportEngine.exportSnapshot(store)

    fun exportCsv(): String = ImportExportEngine.exportCsv(store)

    fun exportTsv(): String = ImportExportEngine.exportTsv(store)

    fun importJson(text: String): ImportResult = ImportExportEngine.importJson(store, text)

    fun importCsv(text: String): ImportResult = ImportExportEngine.importCsv(store, text)

    fun importTsv(text: String): ImportResult = ImportExportEngine.importTsv(store, text)

    /** Materialize every unified card as a legacy DesktopCard (keeps views in sync after import). */
    fun allLegacyCards(): List<DesktopCard> = store.toDesktopCards()

    /**
     * Generate the default card set for notes that have none yet in the given
     * deck (used after CSV/TSV imports, which only carry notes).
     */
    fun ensureCards(deckId: String = "default") {
        val config = store.deckConfig(deckId)
        val notesWithoutCards = store.notes.filter { store.cardsFor(it.id).isEmpty() }
        if (notesWithoutCards.isEmpty()) return
        val generated = CardGenerator.generateBatch(notesWithoutCards, deckId, config)
        store.upsertCards(generated)
        store.save()
    }

    fun deckTotals(deckId: String): DeckLearningTotals = study.deckTotals(deckId)

    fun jlptCoverage(): List<JlptCoverage> = stats.jlptCoverage(store)

    fun characterProgress(): CharacterProgress = stats.characterProgress(store)

    fun writingStats(limit: Int = 10): List<WritingStatRow> = stats.writingStats(store, limit)

    fun weakestKanji(limit: Int = 8): List<WritingStatRow> = stats.weakestKanji(store, limit)

    /** Recent writing attempts with per-stroke detail — the writing history feed. */
    fun recentWritingAttempts(limit: Int = 20): List<WritingAttemptEvent> =
        store.writingAttempts.sortedByDescending { it.attemptedAt }.take(limit)

    /** Per-character writing accuracy history (for trends): most recent N per character. */
    fun writingAccuracyTrend(character: String, limit: Int = 30): List<Pair<Instant, Float>> =
        store.writingAttempts
            .filter { it.expected == character }
            .sortedBy { it.attemptedAt }
            .takeLast(limit)
            .map { it.attemptedAt to it.accuracy }

    fun cardHistory(cardId: String, limit: Int = 100): List<CardHistoryEntry> =
        stats.cardHistory(store, cardId, limit)

    fun examHistory(limit: Int = 30): List<ExamResult> = stats.examHistory(store, limit)

    fun examAggregates(): ExamAggregates = stats.examAggregates(store)

    fun examAccuracyByType(): Map<String, Float> = stats.accuracyByType(store)

    fun examAccuracyByJlpt(): Map<Int, Float> = stats.accuracyByJlpt(store)

    fun examAccuracyBySection(): Map<String, Float> = stats.accuracyBySection(store)

    fun examAccuracyByExamType(): Map<String, Float> = stats.accuracyByExamType(store)

    fun examTrend(limit: Int = 30): List<Pair<String, Int>> = stats.examTrend(store, limit)

    fun studyVsExamGap(): StudyVsExamGap = stats.studyVsExamGap(store)

    fun forecast(days: Int = 30): List<ForecastPoint> = stats.forecast(store, days)

    fun dueToday(): Int = stats.dueToday(store)

    fun mistakeSnapshot(): MistakeSnapshot = stats.mistakeSnapshot(store)

    fun periodStats(period: StatsPeriod): PeriodStats = stats.periodStats(store, period)

    fun goalProgress(): List<GoalProgress> = GoalsRepository.allProgress(store)

    fun streaks(): StreakInfo = stats.streaks(store)

    fun totalStudyTimeMs(): Long = stats.totalStudyTimeMs(store)

    val isEmpty: Boolean get() = store.isEmpty

    // ------------------------------------------------------------
    // Unified search — one engine over notes + cards + decks
    // ------------------------------------------------------------

    /**
     * Search every learning item in the unified store: kanji, vocabulary,
     * radicals, grammar and custom notes, matched by expression, reading,
     * meaning, romaji and JLPT/tag filters. Results carry their kind, JLPT
     * level and current learning stage so the Library can render them.
     */
    fun search(
        query: String,
        kinds: Set<LearningItemKind> = LearningItemKind.entries.toSet(),
        jlpt: Int? = null,
        maxResults: Int = 50
    ): List<UnifiedSearchResult> {
        val q = query.trim().lowercase()
        val byKind = if (kinds.isEmpty()) LearningItemKind.entries.toSet() else kinds

        // Structured filters: kind:kanji jlpt:5 tag:foo state:mature
        val filterKinds = Regex("kind:(\\w+)").findAll(q).mapNotNull { m ->
            LearningItemKind.entries.firstOrNull { it.name.equals(m.groupValues[1], ignoreCase = true) }
        }.toSet()
        val filterJlpt = Regex("jlpt:(\\d)").findAll(q).map { it.groupValues[1].toIntOrNull() }.filterNotNull().firstOrNull()
        val tag = Regex("tag:(\\S+)").findAll(q).map { it.groupValues[1].lowercase() }.firstOrNull()
        val stateFilter = Regex("state:(\\w+)").findAll(q).map { it.groupValues[1].lowercase() }.firstOrNull()
        val effectiveKinds = if (filterKinds.isEmpty()) byKind else byKind intersect filterKinds
        val effectiveJlpt = jlpt ?: filterJlpt

        val plain = q.replace(Regex("kind:\\S+"), " ")
            .replace(Regex("jlpt:\\S+"), " ")
            .replace(Regex("tag:\\S+"), " ")
            .replace(Regex("state:\\S+"), " ")
            .trim()

        val scored = store.notes.asSequence()
            .filter { it.kind in effectiveKinds }
            .filter { effectiveJlpt == null || it.jlpt == effectiveJlpt }
            .filter { tag == null || it.tags.any { t -> t.lowercase().contains(tag) } }
            .filter { stateFilter == null || store.cardsFor(it.id).any { c -> c.stage.name.equals(stateFilter, ignoreCase = true) } }
            .map { note -> note to scoreNote(note, plain) }
            .filter { plain.isBlank() || it.second > 0 }
            .sortedByDescending { it.second }
            .take(maxResults)
            .toList()

        return scored.map { (note, score) ->
            val cards = store.cardsFor(note.id)
            UnifiedSearchResult(
                noteId = note.id,
                kind = note.kind,
                expression = note.expression,
                reading = note.reading,
                meanings = note.meanings,
                jlpt = note.jlpt,
                tags = note.tags,
                score = score,
                stage = cards.maxOfOrNull { it.stage } ?: LearningStage.Introduced,
                due = cards.count { it.isDue },
                deckIds = cards.map { it.deckId }.distinct()
            )
        }
    }

    private fun scoreNote(note: LearningNote, query: String): Int {
        if (query.isBlank()) return 1
        var score = 0
        val expression = note.expression.lowercase()
        val reading = note.reading.lowercase()
        if (expression == query) score += 100
        else if (expression.startsWith(query)) score += 60
        else if (expression.contains(query)) score += 40
        if (reading == query) score += 55
        else if (reading.startsWith(query)) score += 30
        else if (reading.contains(query)) score += 20
        note.meanings.forEach { m ->
            val ml = m.lowercase()
            if (ml == query) score += 45
            else if (ml.startsWith(query)) score += 25
            else if (ml.contains(query)) score += 12
        }
        note.onReadings.forEach { r -> if (r.lowercase().contains(query)) score += 10 }
        note.kunReadings.forEach { r -> if (r.lowercase().contains(query)) score += 10 }
        note.tags.forEach { t -> if (t.lowercase().contains(query)) score += 5 }
        return score
    }

    /**
     * Compact per-note search result. Everything is real store data — the
     * stage/due/deck fields let UI render progress without extra lookups.
     */
    data class UnifiedSearchResult(
        val noteId: String,
        val kind: LearningItemKind,
        val expression: String,
        val reading: String,
        val meanings: List<String>,
        val jlpt: Int?,
        val tags: List<String>,
        val score: Int,
        val stage: LearningStage,
        val due: Int,
        val deckIds: List<String>
    )
}

/** Maps a legacy deck/content kind to the unified learning kind. */
fun ua.syt0r.kanji.desktop.model.ContentKind.toLearningItemKind(): LearningItemKind = when (this) {
    ua.syt0r.kanji.desktop.model.ContentKind.Kanji -> LearningItemKind.Kanji
    ua.syt0r.kanji.desktop.model.ContentKind.Vocabulary -> LearningItemKind.Vocabulary
    ua.syt0r.kanji.desktop.model.ContentKind.Kana -> LearningItemKind.Kana
    ua.syt0r.kanji.desktop.model.ContentKind.Radical -> LearningItemKind.Radical
    ua.syt0r.kanji.desktop.model.ContentKind.Grammar -> LearningItemKind.Grammar
    else -> LearningItemKind.Custom
}
