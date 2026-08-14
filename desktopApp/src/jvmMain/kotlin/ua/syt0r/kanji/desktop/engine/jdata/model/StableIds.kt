package ua.syt0r.kanji.desktop.engine.jdata.model

import ua.syt0r.kanji.desktop.engine.jdata.normalize.Normalizer

// ============================================================
// STABLE IDS
// Entity IDs must stay identical across regenerated databases as
// long as the underlying entity is unchanged. Kaiteyo cards,
// third-party apps, imports and mining records can all reference
// the same language entry by ID, so IDs are derived deterministically
// from the entity's canonical identity — never from random values.
//
// Human-readable IDs (kanji/kana/radical/component/stroke-set) keep
// the character itself; vocabulary IDs hash the (expression, reading)
// pair so regenerating a database yields the same ID for the same
// word while different readings stay distinct.
// ============================================================

object StableIds {

    /** FNV-1a 64-bit hash — deterministic, cheap, no allocation-heavy machinery. */
    fun fnv1a(input: String): Long {
        var hash = -3750763034362895579L // FNV offset basis
        input.forEach { c ->
            hash = hash xor c.code.toLong()
            hash *= 1099511628211L      // FNV prime
        }
        return hash
    }

    /** Lowercase hex of the low 56 bits, zero-padded — stable and compact. */
    fun hash(input: String): String =
        (fnv1a(input).toULong() and 0x0000_00FF_FFFF_FFFFUL)
            .toString(16)
            .padStart(14, '0')

    fun kanji(character: String): String = "k:$character"
    fun kana(character: String): String = "kana:$character"
    fun radical(character: String): String = "r:$character"
    fun component(character: String): String = "c:$character"
    fun strokeSet(character: String): String = "s:$character"

    fun vocab(expression: String, reading: String): String =
        "v:${hash("$expression\u0000${Normalizer.readingKey(reading)}")}"

    fun reading(ownerId: String, kana: String): String = "$ownerId/reading:${hash(kana)}"
    fun sense(ownerId: String, index: Int): String = "$ownerId/sense:$index"
    fun frequency(ownerId: String, source: String): String = "$ownerId/freq:${hash(source)}"
    fun jlpt(level: Int): String = "jlpt:n$level"

    fun relation(fromId: String, toId: String, kind: String): String =
        "rel:${hash("$fromId\u0000$toId\u0000$kind")}"

    fun radicalCharacter(radicalId: String): String =
        if (radicalId.startsWith("r:")) radicalId.removePrefix("r:") else radicalId

    fun componentCharacter(componentId: String): String =
        if (componentId.startsWith("c:")) componentId.removePrefix("c:") else componentId

    fun strokeSetCharacter(strokeSetId: String): String =
        if (strokeSetId.startsWith("s:")) strokeSetId.removePrefix("s:") else strokeSetId
}
