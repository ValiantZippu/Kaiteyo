package ua.syt0r.kanji.desktop.engine.jdata.engine

import ua.syt0r.kanji.desktop.engine.jdata.model.EntityType
import ua.syt0r.kanji.desktop.engine.jdata.model.PlatformData
import ua.syt0r.kanji.desktop.engine.jdata.model.RelationEdge
import ua.syt0r.kanji.desktop.engine.jdata.model.StableIds
import ua.syt0r.kanji.desktop.engine.jdata.normalize.Normalizer

// ============================================================
// RELATIONSHIP GRAPH
// Every cross-entity relationship is a typed, stable-ID edge:
// kanji↔vocab, vocab→reading, vocab→sense, character→strokeSet,
// kanji→radical, kanji→component, kanji→JLPT, vocab→JLPT,
// vocab→frequency, kanji→frequency. Consumers can navigate from
// any entity to everything that references it — without copying
// canonical records into their own data.
// ============================================================

object RelationshipGraph {

    /**
     * Builds the full edge set. Deterministic: edges are sorted by
     * (fromId, toId, kind) and deduplicated by their stable ID.
     */
    fun build(data: PlatformData): List<RelationEdge> {
        val edges = linkedMapOf<String, RelationEdge>()
        fun add(fromType: EntityType, fromId: String, toType: EntityType, toId: String, kind: String) {
            val id = StableIds.relation(fromId, toId, kind)
            edges.putIfAbsent(id, RelationEdge(id, fromType, fromId, toType, toId, kind))
        }

        // Kanji → radical / component
        data.kanji.values.forEach { kanji ->
            kanji.radicalId?.let { radicalId ->
                add(EntityType.KANJI, kanji.id, EntityType.RADICAL, radicalId, "radical")
                add(EntityType.KANJI, kanji.id, EntityType.COMPONENT, StableIds.component(StableIds.radicalCharacter(radicalId)), "component")
            }
        }

        // Kanji ↔ vocabulary (both directions)
        data.vocab.values.forEach { vocab ->
            Normalizer.kanjiCharacters(vocab.expression).forEach { ch ->
                val kanjiId = StableIds.kanji(ch)
                if (kanjiId in data.kanji) {
                    add(EntityType.KANJI, kanjiId, EntityType.VOCAB, vocab.id, "appears-in")
                    add(EntityType.VOCAB, vocab.id, EntityType.KANJI, kanjiId, "contains")
                }
            }
        }

        // Vocabulary → reading / sense
        data.vocab.values.forEach { vocab ->
            vocab.readings.forEach { reading ->
                add(EntityType.VOCAB, vocab.id, EntityType.READING, StableIds.reading(vocab.id, reading.kana), "reading")
            }
            vocab.senses.forEachIndexed { index, _ ->
                add(EntityType.VOCAB, vocab.id, EntityType.SENSE, StableIds.sense(vocab.id, index), "sense")
            }
        }

        // Character → stroke set (kanji + kana) — only when a stroke set exists,
        // so validation never sees dangling edges for characters without stroke data.
        data.kanji.values.forEach { entry ->
            val strokeSetId = StableIds.strokeSet(entry.character)
            if (strokeSetId in data.strokeSets) {
                add(EntityType.KANJI, entry.id, EntityType.STROKE_SET, strokeSetId, "strokes")
            }
        }
        data.kana.values.forEach { entry ->
            val strokeSetId = StableIds.strokeSet(entry.character)
            if (strokeSetId in data.strokeSets) {
                add(EntityType.KANA, entry.id, EntityType.STROKE_SET, strokeSetId, "strokes")
            }
        }

        // JLPT links
        data.kanji.values.forEach { kanji ->
            kanji.jlpt?.let { add(EntityType.KANJI, kanji.id, EntityType.JLPT, StableIds.jlpt(it), "jlpt") }
        }
        data.vocab.values.forEach { vocab ->
            vocab.jlpt?.let { add(EntityType.VOCAB, vocab.id, EntityType.JLPT, StableIds.jlpt(it), "jlpt") }
        }

        // Frequency links
        data.vocab.values.forEach { vocab ->
            vocab.frequencies.forEach { frequency ->
                add(EntityType.VOCAB, vocab.id, EntityType.FREQUENCY, StableIds.frequency(vocab.id, frequency.source), "frequency")
            }
        }
        data.kanji.values.forEach { kanji ->
            kanji.frequencyRank?.let {
                add(EntityType.KANJI, kanji.id, EntityType.FREQUENCY, StableIds.frequency(kanji.id, "kanji-frequency"), "frequency")
            }
        }

        return edges.values.sortedWith(compareBy<RelationEdge> { it.fromId }.thenBy { it.toId }.thenBy { it.kind })
    }

    /** Outgoing edges of an entity. */
    fun edgesFrom(relations: List<RelationEdge>, entityId: String): List<RelationEdge> =
        relations.filter { it.fromId == entityId }

    /** Incoming edges pointing at an entity. */
    fun edgesTo(relations: List<RelationEdge>, entityId: String): List<RelationEdge> =
        relations.filter { it.toId == entityId }

    /** Neighbours of one kind reachable from an entity (both directions). */
    fun neighbours(relations: List<RelationEdge>, entityId: String, kind: String): List<String> =
        relations.filter { it.kind == kind && (it.fromId == entityId || it.toId == entityId) }
            .map { if (it.fromId == entityId) it.toId else it.fromId }
            .distinct()
}
