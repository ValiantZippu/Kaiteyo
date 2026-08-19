package ua.syt0r.kanji.core.knowledge

// ============================================================
// PERFORMANCE UTILITIES
// ------------------------------------------------------------
// Shared utilities for debouncing, lazy pagination, and
// incremental loading. These are used by the search engine,
// graph expansion, and list screens to keep the UI responsive
// with large datasets.
// ============================================================

/**
 * Normalizes a frequency rank to a 0..1 float for display
 * (progress bars, heatmap intensity, etc).
 * Rank 1 = 1.0, rank [maxRank] = 0.0.
 */
fun frequencyNormalized(rank: Int?, maxRank: Int = 5000): Float {
    if (rank == null || rank <= 0) return 0f
    return ((maxRank - rank).coerceAtLeast(0).toFloat() / maxRank).coerceIn(0f, 1f)
}
