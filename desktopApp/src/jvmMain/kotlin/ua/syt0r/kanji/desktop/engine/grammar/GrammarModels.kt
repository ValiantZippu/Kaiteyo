package ua.syt0r.kanji.desktop.engine.grammar

import kotlinx.serialization.Serializable

// ============================================
// KAITEYO GRAMMAR — MODELS
// Grammar entries (pattern + meaning + forms +
// examples) with optional conjugation edges.
// The model is dataset-agnostic: it renders
// whatever openly licensed grammar dataset is
// adopted (see docs/data/SOURCES.md — the
// dataset gate), plus the curated reference
// facts shipped with the suite. No entry is
// ever fabricated.
// ============================================

@Serializable
data class GrammarEntry(
    val id: String,
    val pattern: String,
    val meaning: String,
    val jlpt: Int? = null,
    val forms: List<GrammarForm> = emptyList(),
    val examples: List<GrammarExample> = emptyList(),
    val tags: List<String> = emptyList(),
    /** Provenance of the entry — dataset/source id, never blank for real data. */
    val source: String
)

@Serializable
data class GrammarForm(
    val label: String,
    val pattern: String
)

@Serializable
data class GrammarExample(
    val japanese: String,
    val english: String
)

/** A conjugation relation between two grammar patterns. */
@Serializable
data class GrammarConjugationEdge(
    val from: String,
    val to: String,
    val kind: String // e.g. "te-form", "past", "negative", "potential"
)
