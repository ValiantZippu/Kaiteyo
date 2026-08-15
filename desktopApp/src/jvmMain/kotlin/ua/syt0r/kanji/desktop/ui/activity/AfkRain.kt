package ua.syt0r.kanji.desktop.ui.activity

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.designsystem.accent
import ua.syt0r.kanji.desktop.engine.activity.ActivitySettings
import ua.syt0r.kanji.presentation.common.theme.LocalAnimationConfig
import kotlin.math.abs
import kotlin.math.sin

// ============================================
// AFK RAIN — ambient "away" effect
//
// When the user goes AFK, kanji and vocabulary
// from the real card pool fall like rain. Pure
// decoration: it never touches learning state.
//
// Performance model: one looping `phase` value
// (0..1) is advanced per frame; every particle's
// position is a pure deterministic function of
// (index, phase) — no per-particle state, no
// per-frame allocation, bounded particle count.
// Respects reduced motion and auto-stops when
// the user returns or the window is unfocused.
// ============================================

private const val MAX_PARTICLES = 48

@Composable
fun AfkRain(state: AppState, modifier: Modifier = Modifier) {
    val anim = LocalAnimationConfig.current
    val reducedMotion = anim.reducedMotion ||
        state.navReducedMotion ||
        state.settings.getBool("appearance.reduced-motion")
    val enabled = state.settings.getBool(ActivitySettings.RainEnabled, true)

    var phase by remember { mutableFloatStateOf(0f) }
    var running by remember { mutableStateOf(false) }

    // Re-evaluate the AFK state every second while mounted: `tick` forces a
    // recomposition, which re-reads isAfk() and refreshes the effect keys —
    // so the rain starts the moment the user lapses and stops the moment
    // they return. One bounded coroutine, never a per-frame poll.
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            tick++
        }
    }
    // Read `tick` in composition so the write above actually recomposes.
    tick
    val isAfk = state.activity.isAfk()

    // The effect's lifecycle is fully keyed on the conditions — when any of
    // them flips, the loop stops and `running` resets. No orphaned timers.
    // Focus gating is implicit: when the window loses focus no signals
    // arrive, so the tracker goes AFK and the effect self-limits; the first
    // interaction on return reopens the engagement and stops the rain.
    LaunchedEffect(enabled, reducedMotion, isAfk) {
        val shouldRun = enabled && !reducedMotion && isAfk
        if (!shouldRun) {
            running = false
            phase = 0f
            return@LaunchedEffect
        }
        running = true
        val durationMs = state.settings.getInt(ActivitySettings.RainDurationSeconds, 45)
            .coerceIn(5, 600) * 1000L
        val startNanos = System.nanoTime()
        while (isActive) {
            // Stop early the moment the user returns (a signal reopens the
            // engagement) so the rain never outlives the absence.
            if (!state.activity.isAfk()) break
            val elapsed = (System.nanoTime() - startNanos) / 1_000_000f
            if (elapsed > durationMs) break
            phase = (elapsed / 1000f) % 1f
            delay(16)
        }
        running = false
        phase = 0f
    }

    if (running) {
        val glyphs = remember(state.cards.size) { buildRainGlyphs(state) }
        if (glyphs.isNotEmpty()) {
            RainCanvas(
                glyphs = glyphs,
                phase = phase,
                opacity = state.settings.getFloat(ActivitySettings.RainOpacity, 0.35f)
                    .coerceIn(0.05f, 0.9f),
                density = state.settings.getString(ActivitySettings.RainDensity, "medium"),
                speed = state.settings.getString(ActivitySettings.RainSpeed, "normal"),
                modifier = modifier
            )
        }
    }
}

/**
 * Sample the real card pool: kanji glyphs (single CJK characters) and/or
 * vocabulary surface forms. Deterministic sample so the rain always shows
 * content the user is actually studying — never random nonsense.
 */
private fun buildRainGlyphs(state: AppState): List<String> {
    val content = state.settings.getString(ActivitySettings.RainContent, "both")
    val kanji = state.cards.asSequence()
        .map { it.character.trim() }
        .filter { it.length == 1 && it[0].code in 0x4E00..0x9FFF }
        .distinct()
        .toList()
    val vocab = state.cards.asSequence()
        .map { it.character.trim() }
        .filter { it.isNotEmpty() && it.any { c -> c.code in 0x3040..0x30FF } }
        .distinct()
        .toList()

    val glyphs = when (content) {
        "kanji" -> kanji
        "vocabulary" -> vocab
        else -> (kanji + vocab).distinct()
    }
    return glyphs.take(MAX_PARTICLES)
}

/** Deterministic pseudo-random from a seed — stable across frames. */
private fun seeded(seed: Int, salt: Int): Float =
    abs(sin(seed * 12.9898f + salt * 78.233f).toDouble()).toFloat()

@Composable
private fun RainCanvas(
    glyphs: List<String>,
    phase: Float,
    opacity: Float,
    density: String,
    speed: String,
    modifier: Modifier = Modifier
) {
    val densityMul = when (density) {
        "low" -> 0.5f
        "high" -> 1.6f
        else -> 1f
    }
    val speedMul = when (speed) {
        "slow" -> 0.55f
        "fast" -> 1.8f
        else -> 1f
    }
    val ac = accent()
    val measurer = rememberTextMeasurer()

    BoxWithConstraints(modifier.fillMaxSize()) {
        val w = maxWidth
        val h = maxHeight
        val count = (glyphs.size * densityMul).toInt().coerceIn(12, MAX_PARTICLES)

        Canvas(Modifier.fillMaxSize()) {
            val textStyle = TextStyle(
                color = ac.primary,
                fontWeight = FontWeight.Medium
            )
            for (i in 0 until count) {
                val glyph = glyphs[i % glyphs.size]
                val seed = i * 7919 + 104729
                val fontSize = 10f + seeded(seed, 1) * 18f
                val textLayout = measurer.measure(
                    text = glyph,
                    style = textStyle.copy(fontSize = fontSize.sp)
                )
                val charW = textLayout.size.width.toFloat()
                val charH = textLayout.size.height.toFloat()

                // Stagger each particle's phase so the rain is continuous.
                val effPhase = (phase + seeded(seed, 2) * 0.9f) % 1f
                val x = seeded(seed, 3) * w.value * 1.15f - charW / 2
                // Wrap: each particle falls a full screen-height per loop.
                val y = effPhase * (h.value + charH * 2f) - charH
                val sway = sin(phase * 6.283f + seeded(seed, 4) * 6.283f) * 6f
                val alphaMul = (0.25f + 0.75f * (1f - effPhase)) * opacity
                drawText(
                    textLayoutResult = textLayout,
                    topLeft = Offset(x + sway, y),
                    alpha = alphaMul
                )
            }
        }
    }
}
