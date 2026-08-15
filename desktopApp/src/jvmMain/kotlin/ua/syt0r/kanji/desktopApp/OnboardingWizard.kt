package ua.syt0r.kanji.desktopApp

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.ViewSidebar
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.appstate.NavLayout
import ua.syt0r.kanji.desktop.appstate.NavPosition
import ua.syt0r.kanji.desktop.engine.theming.ThemeMapper
import ua.syt0r.kanji.desktop.engine.theming.colorToHex
import ua.syt0r.kanji.desktop.engine.theming.hexToColor
import ua.syt0r.kanji.desktop.designsystem.accent
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import ua.syt0r.kanji.presentation.common.resources.brand.BrandMark
import ua.syt0r.kanji.presentation.common.theme.AllAccentSchemes
import ua.syt0r.kanji.presentation.common.theme.AnimationSpeed
import ua.syt0r.kanji.presentation.common.theme.BaseMode
import ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme
import ua.syt0r.kanji.presentation.common.theme.LocalAnimationConfig
import ua.syt0r.kanji.presentation.common.theme.surfaceForBaseMode
import kotlin.math.roundToInt

// ============================================
// KAITEYO ONBOARDING WIZARD v2.0
// Premium first-launch experience with 8 steps:
// Welcome → Theme → Accent → Scaling → Font →
// Navigation → Motion → Finish.
// Everything is live: selections write straight
// into the ThemeManager / settings, so the app
// behind the wizard re-themes in real time.
// Shows only on first launch; re-openable from
// Settings → General → "Show onboarding again".
// ============================================

private const val TOTAL_STEPS = 8
private const val LAST_STEP = TOTAL_STEPS - 1

/** Local 4dp-grid spacing kept stable while the live display-scale preview changes. */
private object Wiz {
    val s1 = 4.dp
    val s2 = 8.dp
    val s3 = 12.dp
    val s4 = 16.dp
    val s5 = 20.dp
    val s6 = 24.dp
    val s8 = 32.dp

    val r1 = 8.dp
    val r2 = 12.dp
    val r3 = 16.dp
    val r4 = 20.dp
    val r5 = 24.dp
}

private data class WizardStep(
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

private val wizardSteps = listOf(
    WizardStep("Welcome", "A quick tour of your new study space", Icons.Default.AutoAwesome),
    WizardStep("Choose your theme", "Pick a base appearance", Icons.Default.DarkMode),
    WizardStep("Accent color", "Make Kaiteyo feel like yours", Icons.Default.Palette),
    WizardStep("UI scaling", "Tune the size of interface elements", Icons.Default.ZoomIn),
    WizardStep("Font size", "Choose your reading comfort", Icons.Default.TextFields),
    WizardStep("Navigation", "Sidebar position and layout", Icons.Default.ViewSidebar),
    WizardStep("Motion", "How animations feel", Icons.Default.Animation),
    WizardStep("You're all set", "Everything is ready", Icons.Default.CheckCircle)
)

@Composable
fun OnboardingWizard(
    state: AppState,
    onComplete: () -> Unit
) {
    val sc = surfaceColors()
    val ac = accent()
    val anim = LocalAnimationConfig.current

    var currentStep by remember { mutableIntStateOf(0) }
    var direction by remember { mutableIntStateOf(1) }

    fun goTo(step: Int) {
        direction = if (step > currentStep) 1 else -1
        currentStep = step
    }

    // Entrance: the whole wizard fades + scales in.
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val entranceScale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.96f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 220f),
        label = "wizardEntranceScale"
    )
    val entranceAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(if (anim.reducedMotion) 0 else 280),
        label = "wizardEntranceAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(sc.background)
            .graphicsLayer {
                scaleX = entranceScale
                scaleY = entranceScale
                alpha = entranceAlpha
            }
    ) {
        AmbientGlow(ac)

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxHeight()
                .widthIn(max = 800.dp)
                .padding(horizontal = Wiz.s8, vertical = Wiz.s6)
        ) {
            WizardHeader(currentStep, ac)
            Spacer(Modifier.height(Wiz.s5))
            WizardProgress(currentStep, ac)
            Spacer(Modifier.height(Wiz.s6))

            val stepDuration = if (anim.reducedMotion) 0 else 240
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    val forward = direction > 0
                    val enterOffset = if (forward) 40 else -40
                    val exitOffset = if (forward) -24 else 24
                    (fadeIn(tween(stepDuration)) +
                        slideInHorizontally(tween(stepDuration)) { it / 10 + enterOffset }) togetherWith
                        (fadeOut(tween(stepDuration)) +
                            slideOutHorizontally(tween(stepDuration)) { -it / 10 + exitOffset })
                },
                label = "onboardStep",
                modifier = Modifier.weight(1f)
            ) { step ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    StepHeader(step)
                    Spacer(Modifier.height(Wiz.s5))
                    when (step) {
                        0 -> StepWelcome(ac)
                        1 -> StepTheme(state)
                        2 -> StepAccent(state)
                        3 -> StepScaling(state, ac)
                        4 -> StepFontSize(state, ac)
                        5 -> StepNavigation(state, ac)
                        6 -> StepMotion(state, ac)
                        7 -> StepFinish(ac)
                    }
                    Spacer(Modifier.height(Wiz.s4))
                }
            }

            Spacer(Modifier.height(Wiz.s4))
            HorizontalDivider(color = sc.border.copy(alpha = 0.3f))
            Spacer(Modifier.height(Wiz.s3))

            WizardFooter(
                step = currentStep,
                onBack = { goTo(currentStep - 1) },
                onSkipAll = { goTo(LAST_STEP) },
                onContinue = {
                    if (currentStep < LAST_STEP) goTo(currentStep + 1)
                    else {
                        state.completeOnboarding()
                        onComplete()
                    }
                }
            )
        }
    }
}

// ============================================
// SHELL PIECES
// ============================================

@Composable
private fun AmbientGlow(ac: KaiteyoAccentScheme) {
    val primary = ac.primary
    val secondary = ac.secondary
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(primary.copy(alpha = 0.08f), Color.Transparent),
                    center = Offset(0.95f, 0.05f),
                    radius = 1200f
                )
            )
    )
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(secondary.copy(alpha = 0.06f), Color.Transparent),
                    center = Offset(0.05f, 0.95f),
                    radius = 1200f
                )
            )
    )
}

@Composable
private fun WizardHeader(step: Int, ac: KaiteyoAccentScheme) {
    val sc = surfaceColors()
    Row(verticalAlignment = Alignment.CenterVertically) {
        WizardLogo(size = 34.dp, pulse = false)
        Spacer(Modifier.width(Wiz.s3))
        Column(Modifier.weight(1f)) {
            Text(
                text = "KAITEYO",
                color = sc.textMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Text(
                text = "First-run setup",
                color = sc.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(ac.primary.copy(alpha = 0.12f))
                .padding(horizontal = Wiz.s3, vertical = Wiz.s1)
        ) {
            Text(
                text = "Step ${step + 1} / $TOTAL_STEPS",
                color = ac.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun WizardProgress(currentStep: Int, ac: KaiteyoAccentScheme) {
    val sc = surfaceColors()
    val reducedMotion = LocalAnimationConfig.current.reducedMotion
    val pulse = if (reducedMotion) {
        1f
    } else {
        val infinite = rememberInfiniteTransition(label = "wizardProgress")
        val animatedPulse by infinite.animateFloat(
            initialValue = 0.55f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
            label = "wizardProgressPulse"
        )
        animatedPulse
    }
    Row(horizontalArrangement = Arrangement.spacedBy(Wiz.s1), modifier = Modifier.fillMaxWidth()) {
        for (i in 0 until TOTAL_STEPS) {
            val state = when {
                i < currentStep -> 2 // done
                i == currentStep -> 1 // active
                else -> 0 // upcoming
            }
            val bg by animateColorAsState(
                targetValue = when (state) {
                    2 -> ac.primary
                    1 -> ac.primary.copy(alpha = 0.55f + 0.45f * pulse)
                    else -> sc.border.copy(alpha = 0.3f)
                },
                animationSpec = tween(260),
                label = "wizProgressSeg"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(bg)
            )
        }
    }
}

@Composable
private fun StepHeader(step: Int) {
    val sc = surfaceColors()
    val meta = wizardSteps[step]
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(Wiz.r2))
                .background(accent().primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = meta.icon,
                contentDescription = null,
                tint = accent().primary,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(Wiz.s3))
        Column {
            Text(
                text = meta.title,
                color = sc.textPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = meta.subtitle,
                color = sc.textMuted,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun WizardFooter(
    step: Int,
    onBack: () -> Unit,
    onSkipAll: () -> Unit,
    onContinue: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (step > 0) {
            WizardGhostButton(text = "Back", icon = Icons.Default.ArrowBack, onClick = onBack)
        } else {
            Spacer(Modifier.width(1.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Wiz.s3), verticalAlignment = Alignment.CenterVertically) {
            if (step < LAST_STEP) {
                Text(
                    text = "Skip all",
                    color = surfaceColors().textMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(Wiz.r1))
                        .clickable(onClick = onSkipAll)
                        .padding(horizontal = Wiz.s3, vertical = Wiz.s2)
                )
            }
            WizardPrimaryButton(
                text = if (step < LAST_STEP) "Continue" else "Get Started",
                icon = Icons.Default.ArrowForward,
                onClick = onContinue
            )
        }
    }
}

@Composable
private fun WizardPrimaryButton(
    text: String,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    val ac = accent()
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else if (hovered) 1.03f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 420f),
        label = "wizPrimaryScale"
    )
    Row(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(listOf(ac.primary, ac.secondary ?: ac.primary)),
                alpha = if (hovered) 1f else 0.92f
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .hoverable(interaction)
            .padding(horizontal = Wiz.s5, vertical = Wiz.s3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Wiz.s2)
    ) {
        Text(
            text = text,
            color = ac.onPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ac.onPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun WizardGhostButton(text: String, icon: ImageVector, onClick: () -> Unit) {
    val sc = surfaceColors()
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(Wiz.r2))
            .background(if (hovered) sc.surfaceInteractive else Color.Transparent)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .hoverable(interaction)
            .padding(horizontal = Wiz.s4, vertical = Wiz.s3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Wiz.s2)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = sc.textSecondary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            color = sc.textSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/** Shared selectable option card with animated frame + check badge. */
@Composable
private fun WizardSelectCard(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val sc = surfaceColors()
    val ac = accent()
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()
    val bg by animateColorAsState(
        targetValue = when {
            selected -> ac.primary.copy(alpha = 0.14f)
            hovered -> sc.surfaceInteractive
            else -> sc.surface
        },
        animationSpec = tween(180),
        label = "wizCardBg"
    )
    val border by animateColorAsState(
        targetValue = if (selected) ac.primary else sc.border.copy(alpha = 0.4f),
        animationSpec = tween(180),
        label = "wizCardBorder"
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else if (hovered) 1.015f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 420f),
        label = "wizCardScale"
    )
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(bg)
            .border(1.5.dp, border, shape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .hoverable(interaction)
    ) {
        Column(Modifier.padding(Wiz.s4)) { content() }
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn(tween(150)) +
                scaleIn(initialScale = 0.4f, animationSpec = spring(dampingRatio = 0.5f, stiffness = 600f)),
            exit = fadeOut(tween(120)) + scaleOut(animationSpec = tween(120)),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(Wiz.s2)
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(ac.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = ac.onPrimary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun WizardLogo(size: Dp, pulse: Boolean) {
    val ac = accent()
    val reducedMotion = LocalAnimationConfig.current.reducedMotion
    val glow = if (pulse && !reducedMotion) {
        val infinite = rememberInfiniteTransition(label = "wizardLogo")
        val animatedGlow by infinite.animateFloat(
            initialValue = 0.7f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
            label = "wizardLogoGlow"
        )
        animatedGlow
    } else {
        1f
    }
    val scale = if (pulse) glow else 1f
    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        contentAlignment = Alignment.Center
    ) {
        // The real Kaiteyo mark — centralized brand asset, not a "K".
        // The mark carries its own brand tile, so no synthetic gradient
        // backdrop is needed behind it.
        BrandMark(modifier = Modifier.fillMaxSize(), contentDescription = "Kaiteyo")
    }
    // Soft glow ring behind the logo.
    if (pulse) {
        Box(
            modifier = Modifier
                .size(size * 1.7f)
                .graphicsLayer { alpha = glow * 0.5f }
                .clip(CircleShape)
                .background(ac.primary.copy(alpha = 0.14f))
        )
    }
}

// ============================================
// STEP 1 — WELCOME
// ============================================

@Composable
private fun StepWelcome(ac: KaiteyoAccentScheme) {
    val sc = surfaceColors()
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(Wiz.s6))
        Box(contentAlignment = Alignment.Center) {
            WizardLogo(size = 96.dp, pulse = true)
        }
        Spacer(Modifier.height(Wiz.s6))
        Text(
            text = "Let's get you set up",
            color = sc.textPrimary,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(Wiz.s2))
        Text(
            text = "Kaiteyo is your premium Japanese study space.\nConfigure it in just a few steps — everything can change later in Settings.",
            color = sc.textMuted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
        Spacer(Modifier.height(Wiz.s8))

        Row(
            modifier = Modifier.fillMaxWidth(0.8f),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            WelcomePill(Icons.Default.DarkMode, "Theme")
            WelcomePill(Icons.Default.ViewSidebar, "Layout")
            WelcomePill(Icons.Default.Animation, "Motion")
        }
        Spacer(Modifier.height(Wiz.s8))
        Text(
            text = "Press Continue to start, or Skip all to jump straight in.",
            color = sc.textMuted.copy(alpha = 0.7f),
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun WelcomePill(icon: ImageVector, label: String) {
    val sc = surfaceColors()
    val ac = accent()
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(Wiz.r3))
                .background(ac.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ac.primary,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.height(Wiz.s2))
        Text(text = label, color = sc.textMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

// ============================================
// STEP 2 — THEME
// ============================================

@Composable
private fun StepTheme(state: AppState) {
    val current = ThemeMapper.baseMode(state.themeManager.activeTheme)
    val sc = surfaceColors()
    Column(verticalArrangement = Arrangement.spacedBy(Wiz.s3)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Wiz.s3)) {
            BaseMode.entries.chunked(2).forEach { pair ->
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Wiz.s3)) {
                    pair.forEach { mode ->
                        val surf = surfaceForBaseMode(mode)
                        WizardSelectCard(
                            selected = current == mode,
                            onClick = {
                                if (current != mode) {
                                    state.themeManager.updateActive {
                                        it.copy(baseMode = mode.name.lowercase())
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ThemeModePreview(mode, surf, ac = accent())
                            Spacer(Modifier.height(Wiz.s3))
                            Text(
                                text = mode.displayName,
                                color = if (current == mode) accent().primary else sc.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(Wiz.s1))
        Text(
            text = "Theme changes apply instantly — the whole app previews in real time.",
            color = surfaceColors().textMuted,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun ThemeModePreview(mode: BaseMode, surf: ua.syt0r.kanji.presentation.common.theme.SurfaceColors, ac: KaiteyoAccentScheme) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(Wiz.r2))
            .background(surf.background)
            .border(1.dp, surf.border.copy(alpha = 0.4f), RoundedCornerShape(Wiz.r2))
    ) {
        // Sidebar strip
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .width(16.dp)
                .background(surf.surfaceElevated)
        )
        // Content card
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .width(52.dp)
                .height(26.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(surf.surface)
                .border(1.dp, surf.border.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(5.dp)
                    .width(20.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(ac.primary)
            )
        }
        // Accent dot
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(Wiz.s2)
                .size(8.dp)
                .clip(CircleShape)
                .background(ac.primary)
        )
    }
}

// ============================================
// STEP 3 — ACCENT
// ============================================

@Composable
private fun StepAccent(state: AppState) {
    val sc = surfaceColors()
    val theme = state.themeManager.activeTheme
    val isSepia = ThemeMapper.baseMode(theme) == BaseMode.Sepia

    if (isSepia) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Wiz.r2))
                .background(sc.surface)
                .padding(Wiz.s6)
        ) {
            Text(
                text = "Accent themes are disabled in Sepia reading mode.\nSwitch to another base theme to customize accents.",
                color = sc.textMuted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        return
    }

    // Detect which scheme most closely matches the current theme.
    val currentPrimary = colorToHex(hexToColor(theme.colors.primary))
    val selectedIndex = AllAccentSchemes.indexOfFirst { colorToHex(it.primary) == currentPrimary }
        .let { if (it >= 0) it else 0 }

    Column(verticalArrangement = Arrangement.spacedBy(Wiz.s3)) {
        AllAccentSchemes.chunked(4).forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Wiz.s3)
            ) {
                row.forEach { scheme ->
                    val index = AllAccentSchemes.indexOf(scheme)
                    WizardSelectCard(
                        selected = selectedIndex == index,
                        onClick = { applyAccentColors(state, scheme) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                scheme.previewColors.forEach { c ->
                                    Box(
                                        Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(c)
                                    )
                                }
                            }
                            Spacer(Modifier.height(Wiz.s2))
                            Text(
                                text = scheme.name,
                                color = if (selectedIndex == index) accent().primary else sc.textPrimary,
                                fontSize = 10.sp,
                                fontWeight = if (selectedIndex == index) FontWeight.SemiBold else FontWeight.Normal,
                                textAlign = TextAlign.Center,
                                maxLines = 2
                            )
                        }
                    }
                }
                repeat(4 - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
        Spacer(Modifier.height(Wiz.s1))
        // Live preview strip
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Wiz.r3))
                .background(sc.surface)
                .padding(Wiz.s4)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(Wiz.r2))
                        .background(accent().primary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Palette,
                        contentDescription = null,
                        tint = accent().primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(Wiz.s3))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Live preview",
                        color = sc.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Buttons, links and highlights follow your accent.",
                        color = sc.textMuted,
                        fontSize = 11.sp
                    )
                }
                WizardMiniButton()
            }
        }
    }
}

private fun applyAccentColors(state: AppState, scheme: KaiteyoAccentScheme) {
    state.themeManager.updateActiveColors { c ->
        c.copy(
            primary = colorToHex(scheme.primary),
            primaryDark = colorToHex(scheme.primaryDark),
            secondary = colorToHex(scheme.secondary),
            secondaryDark = colorToHex(scheme.secondaryDark),
            tertiary = scheme.tertiary?.let { colorToHex(it) } ?: c.tertiary,
            onPrimary = colorToHex(scheme.onPrimary),
            onSecondary = colorToHex(scheme.onSecondary)
        )
    }
}

@Composable
private fun WizardMiniButton() {
    val ac = accent()
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(ac.primary)
            .padding(horizontal = Wiz.s3, vertical = 6.dp)
    ) {
        Text(
            text = "Button",
            color = ac.onPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ============================================
// STEP 4 — UI SCALING
// ============================================

@Composable
private fun StepScaling(state: AppState, ac: KaiteyoAccentScheme) {
    val sc = surfaceColors()
    var scaleValue by remember {
        mutableFloatStateOf((state.themeManager.activeTheme.scaling.displayScale * 100f).roundToInt().toFloat())
    }

    Column {
        Text(
            text = "Scale: ${scaleValue.roundToInt()}%",
            color = sc.textPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(Wiz.s4))
        Slider(
            value = scaleValue,
            onValueChange = { newValue ->
                scaleValue = newValue
                state.themeManager.updateActiveScaling { it.copy(displayScale = newValue / 100f) }
            },
            valueRange = 80f..160f,
            colors = SliderDefaults.colors(
                thumbColor = ac.primary,
                activeTrackColor = ac.primary,
                inactiveTrackColor = sc.border
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Smaller (80%)", color = sc.textMuted, fontSize = 11.sp)
            Text("Larger (160%)", color = sc.textMuted, fontSize = 11.sp)
        }
        Spacer(Modifier.height(Wiz.s6))

        // Live preview
        val preview = scaleValue / 100f
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Wiz.r3))
                .background(sc.surface)
                .padding(Wiz.s4)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // The real Kaiteyo mark — centralized brand asset, not a "K".
                BrandMark(
                    modifier = Modifier.size((40 * preview).dp),
                    contentDescription = null
                )
                Spacer(Modifier.width((16 * preview).dp))
                Column {
                    Text(
                        text = "Sample text",
                        color = sc.textPrimary,
                        fontSize = (15 * preview).sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Everything scales together",
                        color = sc.textMuted,
                        fontSize = (11 * preview).sp
                    )
                }
            }
        }
    }
}

// ============================================
// STEP 5 — FONT SIZE
// ============================================

@Composable
private fun StepFontSize(state: AppState, ac: KaiteyoAccentScheme) {
    val sc = surfaceColors()
    val sizeNames = listOf("Small", "Medium", "Large", "Extra large")
    val sizeValues = listOf(12f, 14f, 16f, 18f)
    var level by remember {
        val current = state.themeManager.activeTheme.typography.fontSize
        mutableIntStateOf(sizeValues.indexOfFirst { (it / 14f) >= current }.let { if (it >= 0) it else 1 })
    }

    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Wiz.s2)) {
            sizeNames.forEachIndexed { i, name ->
                WizardSelectCard(
                    selected = level == i,
                    onClick = {
                        level = i
                        state.themeManager.updateActiveTypography {
                            it.copy(fontSize = sizeValues[i] / 14f)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "あ",
                            color = if (level == i) ac.primary else sc.textPrimary,
                            fontSize = (14 + i * 3).sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(Wiz.s1))
                        Text(
                            text = name,
                            color = if (level == i) ac.primary else sc.textMuted,
                            fontSize = 11.sp,
                            fontWeight = if (level == i) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(Wiz.s6))

        val currentSize = sizeValues[level]
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Wiz.r3))
                .background(sc.surface)
                .padding(Wiz.s5)
        ) {
            Column {
                Text(
                    text = "The quick brown fox jumps over the lazy dog.",
                    color = sc.textPrimary,
                    fontSize = currentSize.sp,
                    lineHeight = (currentSize * 1.5).sp
                )
                Spacer(Modifier.height(Wiz.s2))
                Text(
                    text = "こんにちは世界。学ぶことは強みだ。",
                    color = sc.textPrimary,
                    fontSize = currentSize.sp,
                    lineHeight = (currentSize * 1.6).sp
                )
            }
        }
        Spacer(Modifier.height(Wiz.s3))
        Text(
            text = "This is how your study content will appear.",
            color = sc.textMuted,
            fontSize = 12.sp
        )
    }
}

// ============================================
// STEP 6 — NAVIGATION
// ============================================

@Composable
private fun StepNavigation(state: AppState, ac: KaiteyoAccentScheme) {
    val sc = surfaceColors()

    Column(verticalArrangement = Arrangement.spacedBy(Wiz.s4)) {
        Text(
            text = "Position",
            color = sc.textSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Wiz.s2)) {
            NavPosition.entries.forEach { pos ->
                val selected = state.navPosition == pos
                WizardSelectCard(
                    selected = selected,
                    onClick = { state.updateNavPosition(pos) },
                    modifier = Modifier.weight(1f)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        val icon = when (pos) {
                            NavPosition.Left -> Icons.Default.ArrowBack
                            NavPosition.Right -> Icons.Default.ArrowForward
                            NavPosition.Top -> Icons.Default.ArrowForward
                            NavPosition.Bottom -> Icons.Default.ArrowBack
                        }
                        val rotation = when (pos) {
                            NavPosition.Top -> -90f
                            NavPosition.Bottom -> 90f
                            else -> 0f
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (selected) ac.primary else sc.textSecondary,
                            modifier = Modifier
                                .size(18.dp)
                                .graphicsLayer { rotationZ = rotation }
                        )
                        Spacer(Modifier.height(Wiz.s1))
                        Text(
                            text = pos.label,
                            color = if (selected) ac.primary else sc.textPrimary,
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(Wiz.s1))
        Text(
            text = "Mode",
            color = sc.textSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Wiz.s2)) {
            NavLayout.entries.forEach { layout ->
                val selected = state.navLayout == layout
                WizardSelectCard(
                    selected = selected,
                    onClick = { state.updateNavLayout(layout) },
                    modifier = Modifier.weight(1f)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = layout.label,
                            color = if (selected) ac.primary else sc.textPrimary,
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                    }
                }
            }
        }

        // Mini window preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(88.dp)
                .clip(RoundedCornerShape(Wiz.r3))
                .background(sc.surface)
                .padding(Wiz.s3)
        ) {
            val pos = state.navPosition
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(Wiz.r2))
                    .background(sc.background)
            ) {
                val dockMod = when (pos) {
                    NavPosition.Left -> Modifier.align(Alignment.CenterStart).width(18.dp).fillMaxHeight()
                    NavPosition.Right -> Modifier.align(Alignment.CenterEnd).width(18.dp).fillMaxHeight()
                    NavPosition.Top -> Modifier.align(Alignment.TopCenter).fillMaxWidth().height(14.dp)
                    NavPosition.Bottom -> Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(14.dp)
                }
                Box(
                    modifier = Modifier
                        .then(dockMod)
                        .clip(RoundedCornerShape(5.dp))
                        .background(ac.primary)
                )
                // Content card
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .width(56.dp)
                        .height(30.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(sc.surface)
                        .border(1.dp, sc.border.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                )
            }
        }
    }
}

// ============================================
// STEP 7 — MOTION
// ============================================

@Composable
private fun StepMotion(state: AppState, ac: KaiteyoAccentScheme) {
    val sc = surfaceColors()
    val theme = state.themeManager.activeTheme

    data class MotionPreset(val name: String, val speed: AnimationSpeed, val description: String, val cinematic: Boolean = false)

    val presets = listOf(
        MotionPreset("Off", AnimationSpeed.Instant, "No motion — best for accessibility"),
        MotionPreset("Minimal", AnimationSpeed.Fast, "Quick fades and slides"),
        MotionPreset("Balanced", AnimationSpeed.Normal, "Smooth default animations"),
        MotionPreset("Smooth", AnimationSpeed.Slow, "Fluid spring-based transitions"),
        MotionPreset("Cinematic", AnimationSpeed.Slow, "Dramatic, premium feel", cinematic = true)
    )

    fun isSelected(preset: MotionPreset): Boolean {
        val speedMatches = if (preset.speed == AnimationSpeed.Instant) {
            theme.animation.reducedMotion
        } else {
            !theme.animation.reducedMotion && theme.animation.speed == preset.speed.multiplier
        }
        return speedMatches && preset.cinematic == (theme.animation.durationMs >= 420)
    }

    Column(verticalArrangement = Arrangement.spacedBy(Wiz.s2)) {
        presets.forEach { preset ->
            val selected = isSelected(preset)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Wiz.r2))
                    .background(if (selected) ac.primary.copy(alpha = 0.1f) else sc.surface)
                    .border(1.dp, if (selected) ac.primary else sc.border.copy(alpha = 0.35f), RoundedCornerShape(Wiz.r2))
                    .clickable {
                        state.themeManager.updateActiveAnimation {
                            it.copy(
                                speed = preset.speed.multiplier,
                                reducedMotion = preset.speed == AnimationSpeed.Instant,
                                durationMs = if (preset.cinematic) 460 else 300
                            )
                        }
                    }
                    .padding(horizontal = Wiz.s4, vertical = Wiz.s3),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(if (selected) ac.primary else sc.border.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = selected,
                        enter = scaleIn(animationSpec = spring(dampingRatio = 0.5f, stiffness = 600f)),
                        exit = scaleOut(animationSpec = tween(120))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = ac.onPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
                Spacer(Modifier.width(Wiz.s3))
                Column {
                    Text(
                        text = preset.name,
                        color = if (selected) ac.primary else sc.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                    )
                    Text(
                        text = preset.description,
                        color = sc.textMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }
        Spacer(Modifier.height(Wiz.s1))
        Text(
            text = "You can fine-tune everything later in Theme Studio.",
            color = sc.textMuted,
            fontSize = 12.sp
        )
    }
}

// ============================================
// STEP 8 — FINISH
// ============================================

@Composable
private fun StepFinish(ac: KaiteyoAccentScheme) {
    val sc = surfaceColors()
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(Wiz.s4))
        Box(contentAlignment = Alignment.Center) {
            WizardLogo(size = 88.dp, pulse = true)
        }
        Spacer(Modifier.height(Wiz.s5))
        Text(
            text = "You're all set!",
            color = sc.textPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(Wiz.s2))
        Text(
            text = "Kaiteyo is ready to use. Here's what you can explore:",
            color = sc.textMuted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(Wiz.s6))

        Row(
            Modifier.fillMaxWidth(0.85f),
            horizontalArrangement = Arrangement.spacedBy(Wiz.s3)
        ) {
            FinishCard(Icons.Default.School, "Browse decks", "Explore kanji decks and start learning", Modifier.weight(1f))
            FinishCard(Icons.Default.Insights, "View stats", "Check your progress and streaks", Modifier.weight(1f))
        }
        Spacer(Modifier.height(Wiz.s3))
        Row(
            Modifier.fillMaxWidth(0.85f),
            horizontalArrangement = Arrangement.spacedBy(Wiz.s3)
        ) {
            FinishCard(Icons.Default.Palette, "Customize", "Fine-tune themes in Theme Studio", Modifier.weight(1f))
            FinishCard(Icons.Default.Settings, "Settings", "Adjust all preferences", Modifier.weight(1f))
        }

        Spacer(Modifier.height(Wiz.s6))
        Text(
            text = "Happy learning! \uD83C\uDF38",
            color = sc.textMuted.copy(alpha = 0.7f),
            fontSize = 13.sp
        )
    }
}

@Composable
private fun FinishCard(icon: ImageVector, title: String, desc: String, modifier: Modifier = Modifier) {
    val sc = surfaceColors()
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val scale by animateFloatAsState(
        targetValue = if (hovered) 1.02f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 420f),
        label = "finishCardScale"
    )
    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(Wiz.r3))
            .background(if (hovered) sc.surfaceInteractive else sc.surface)
            .border(1.dp, sc.border.copy(alpha = if (hovered) 0.6f else 0.3f), RoundedCornerShape(Wiz.r3))
            .hoverable(interaction)
            .padding(Wiz.s4)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent().primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.height(Wiz.s2))
        Text(
            text = title,
            color = sc.textPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = desc,
            color = sc.textMuted,
            fontSize = 11.sp
        )
    }
}
