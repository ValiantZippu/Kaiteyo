package ua.syt0r.kanji.desktopApp

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.syt0r.kanji.presentation.common.theme.AllAccentSchemes
import ua.syt0r.kanji.presentation.common.theme.AnimationSpeed
import ua.syt0r.kanji.presentation.common.theme.BaseMode
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoThemeState
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.theme.SidebarMode
import ua.syt0r.kanji.presentation.common.theme.SidebarPosition
import ua.syt0r.kanji.presentation.common.theme.surfaceForBaseMode
import kotlin.math.roundToInt

// ============================================
// KAITEYO ONBOARDING WIZARD v1.0
// First-launch experience with 8 guided steps
// Sets up: Theme, Accent, Scaling, Font Size, Sidebar, Animations, Finish
// ============================================

@Composable
fun OnboardingWizard(onComplete: () -> Unit) {
    val themeState = LocalKaiteyoThemeState.current
    val currentAccent = LocalKaiteyoAccent.current
    val surfaceColors = LocalSurfaceColors.current
    var currentStep by remember { mutableIntStateOf(0) }
    val totalSteps = 8

    val accent by animateColorAsState(
        targetValue = currentAccent.primary,
        animationSpec = tween(600), label = "onboardAccent"
    )

    Box(
        modifier = Modifier.fillMaxSize()
            .background(surfaceColors.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(28.dp)
        ) {
            // Header with branding and step progress
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier.size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accent),
                    contentAlignment = Alignment.Center
                ) { Text("K", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.width(10.dp))
                Text("Welcome to Kaiteyo",
                    color = surfaceColors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("Step ${currentStep + 1}/$totalSteps",
                    color = surfaceColors.textMuted, fontSize = 12.sp)
            }
            Spacer(Modifier.height(12.dp))

            // Progress bar
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                for (i in 0 until totalSteps) {
                    val isActive = i <= currentStep
                    val stepColor by animateColorAsState(
                        targetValue = if (isActive) accent else surfaceColors.border.copy(alpha = 0.25f),
                        animationSpec = tween(300), label = "progBar"
                    )
                    Box(
                        modifier = Modifier.weight(1f).height(3.dp)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(stepColor)
                    )
                }
            }
            Spacer(Modifier.height(20.dp))

            // Main content with animated transitions
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    fadeIn(tween(350)) + slideInHorizontally { it * 80 } togetherWith
                    fadeOut(tween(250)) + slideOutHorizontally { -it * 40 }
                },
                label = "onboardStep",
                modifier = Modifier.weight(1f)
            ) { step ->
                Box(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    when (step) {
                        0 -> OnboardingWelcome(accent)
                        1 -> OnboardingTheme(accent)
                        2 -> OnboardingAccent(accent)
                        3 -> OnboardingScaling(accent)
                        4 -> OnboardingFontSize(accent)
                        5 -> OnboardingSidebar(accent)
                        6 -> OnboardingAnimations(accent)
                        7 -> OnboardingFinish(accent)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = surfaceColors.border.copy(alpha = 0.15f))
            Spacer(Modifier.height(12.dp))

            // Navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStep > 0) {
                    OutlinedButton(
                        onClick = { currentStep-- },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = surfaceColors.textPrimary)
                    ) { Text("Back") }
                } else {
                    Spacer(Modifier.width(1.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (currentStep < totalSteps - 1) {
                        OutlinedButton(
                            onClick = { currentStep = totalSteps - 1 },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = surfaceColors.textMuted)
                        ) { Text("Skip All") }
                    }
                    Button(
                        onClick = {
                            if (currentStep < totalSteps - 1) currentStep++
                            else onComplete()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White)
                    ) {
                        Text(if (currentStep < totalSteps - 1) "Continue" else "Get Started")
                    }
                }
            }
        }
    }
}

// ============================================
// STEP 1: Welcome
// ============================================

@Composable
private fun OnboardingWelcome(accent: Color) {
    val surfaceColors = LocalSurfaceColors.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        // Animated logo
        Box(
            modifier = Modifier.size(100.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(accent),
            contentAlignment = Alignment.Center
        ) {
            Text("K", color = Color.White, fontSize = 52.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(20.dp))
        Text("Let's Get You Set Up",
            color = surfaceColors.textPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("We'll help you configure Kaiteyo in just a few steps.\nYou can always change these later in Settings.",
            color = surfaceColors.textMuted, fontSize = 15.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(0.7f),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            QuickInfo("\uD83C\uDFA8", "Themes")
            QuickInfo("\uD83D\uDCCF", "Layout")
            QuickInfo("\u2728", "Animations")
        }
    }
}

@Composable
private fun QuickInfo(icon: String, label: String) {
    val surfaceColors = LocalSurfaceColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 24.sp)
        Spacer(Modifier.height(4.dp))
        Text(label, color = surfaceColors.textMuted, fontSize = 12.sp)
    }
}

// ============================================
// STEP 2: Choose Theme
// ============================================

@Composable
private fun OnboardingTheme(accent: Color) {
    val themeState = LocalKaiteyoThemeState.current
    val surfaceColors = LocalSurfaceColors.current

    Column {
        Text("Choose Your Theme",
            color = surfaceColors.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("Select a base appearance",
            color = surfaceColors.textMuted, fontSize = 14.sp)
        Spacer(Modifier.height(20.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BaseMode.entries.forEach { mode ->
                val isSelected = themeState.baseMode == mode
                val surf = surfaceForBaseMode(mode)
                val cardBg by animateColorAsState(
                    targetValue = if (isSelected) surf.background else surf.surface,
                    animationSpec = tween(300), label = "themeCard"
                )
                Column(
                    modifier = Modifier.weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(cardBg)
                        .border(2.dp, if (isSelected) accent else surf.border.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                        .clickable { themeState.baseMode = mode }
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(mode.displayName, color = surf.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(
                            if (isSelected) accent else surf.textPrimary.copy(alpha = 0.15f)
                        ),
                        contentAlignment = Alignment.Center
                    ) { Text("A", color = if (isSelected) Color.White else surf.textMuted, fontSize = 20.sp) }
                    Spacer(Modifier.height(8.dp))
                    // Mini preview
                    Box(
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                            .background(surf.border.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth(0.6f).fillMaxHeight()
                                .clip(RoundedCornerShape(2.dp)).background(if (isSelected) accent else surf.textPrimary.copy(alpha = 0.2f))
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth().height(20.dp).clip(RoundedCornerShape(6.dp))
                            .background(surf.surfaceElevated)
                    )
                }
            }
        }
    }
}

// ============================================
// STEP 3: Accent Theme (with live preview)
// ============================================

@Composable
private fun OnboardingAccent(accent: Color) {
    val themeState = LocalKaiteyoThemeState.current
    val surfaceColors = LocalSurfaceColors.current
    val isSepia = themeState.baseMode == BaseMode.Sepia

    Column {
        Text("Accent Color",
            color = surfaceColors.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(if (isSepia) "Not available in Sepia mode" else "Choose your accent color",
            color = surfaceColors.textMuted, fontSize = 14.sp)

        if (isSepia) {
            Spacer(Modifier.height(16.dp))
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(surfaceColors.surface).padding(20.dp)) {
                Text("Accent themes are disabled in Sepia reading mode.\nSwitch to another base theme to customize accents.",
                    color = surfaceColors.textMuted, fontSize = 13.sp, textAlign = TextAlign.Center)
            }
            return
        }

        Spacer(Modifier.height(16.dp))
        AllAccentSchemes.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { scheme ->
                    val isSelected = themeState.accentScheme.name == scheme.name
                    Box(
                        modifier = Modifier.weight(1f).padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) accent.copy(alpha = 0.12f) else surfaceColors.surface)
                            .border(1.5.dp, if (isSelected) accent else surfaceColors.border.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                            .clickable { themeState.accentScheme = scheme }
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                scheme.previewColors.forEach { c ->
                                    Box(Modifier.size(10.dp).clip(CircleShape).background(c))
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(scheme.name, color = if (isSelected) accent else surfaceColors.textPrimary,
                                fontSize = 9.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        // Live preview mini
        Box(
            modifier = Modifier.fillMaxWidth().height(80.dp).clip(RoundedCornerShape(12.dp))
                .background(surfaceColors.surface).padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize()) {
                Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(accent))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Box(Modifier.fillMaxWidth(0.5f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(accent.copy(alpha = 0.3f)))
                    Spacer(Modifier.height(4.dp))
                    Box(Modifier.fillMaxWidth(0.3f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(surfaceColors.textMuted.copy(alpha = 0.3f)))
                }
                Box(Modifier.size(24.dp).clip(RoundedCornerShape(6.dp)).background(accent.copy(alpha = 0.2f)))
            }
        }
    }
}

// ============================================
// STEP 4: UI Scaling
// ============================================

@Composable
private fun OnboardingScaling(accent: Color) {
    val surfaceColors = LocalSurfaceColors.current
    var scaleValue by remember { mutableFloatStateOf(100f) }

    Column {
        Text("UI Scaling",
            color = surfaceColors.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("Adjust the size of interface elements",
            color = surfaceColors.textMuted, fontSize = 14.sp)
        Spacer(Modifier.height(24.dp))

        Text("Scale: ${scaleValue.roundToInt()}%",
            color = surfaceColors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))

        Slider(value = scaleValue, onValueChange = { scaleValue = it },
            valueRange = 80f..200f, modifier = Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Smaller (80%)", color = surfaceColors.textMuted, fontSize = 10.sp)
            Text("Larger (200%)", color = surfaceColors.textMuted, fontSize = 10.sp)
        }
        Spacer(Modifier.height(24.dp))

        // Live scale preview
        val previewScale = scaleValue / 100f
        Box(
            modifier = Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(12.dp))
                .background(surfaceColors.surface).padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier.size((40 * previewScale).dp).clip(RoundedCornerShape((8 * previewScale).dp)).background(accent),
                    contentAlignment = Alignment.Center
                ) { Text("K", color = Color.White, fontSize = (18 * previewScale).sp) }
                Spacer(Modifier.width((12 * previewScale).dp))
                Column {
                    Text("Sample Text", color = surfaceColors.textPrimary, fontSize = (14 * previewScale).sp)
                    Text("At ${scaleValue.roundToInt()}% scale", color = surfaceColors.textMuted, fontSize = (11 * previewScale).sp)
                }
            }
        }
    }
}

// ============================================
// STEP 5: Font Size
// ============================================

@Composable
private fun OnboardingFontSize(accent: Color) {
    val surfaceColors = LocalSurfaceColors.current
    var fontSizeLevel by remember { mutableIntStateOf(1) }
    val sizeNames = listOf("Small", "Medium", "Large", "Extra Large")
    val sizeValues = listOf(12, 14, 16, 18)

    Column {
        Text("Font Size",
            color = surfaceColors.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("Choose your preferred reading size",
            color = surfaceColors.textMuted, fontSize = 14.sp)
        Spacer(Modifier.height(24.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            sizeNames.forEachIndexed { i, name ->
                val isSelected = fontSizeLevel == i
                Box(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) accent.copy(alpha = 0.15f) else surfaceColors.surface)
                        .border(1.5.dp, if (isSelected) accent else surfaceColors.border.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                        .clickable { fontSizeLevel = i }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(name, color = if (isSelected) accent else surfaceColors.textPrimary,
                        fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium)
                }
            }
        }
        Spacer(Modifier.height(24.dp))

        // Preview
        val currentSize = sizeValues[fontSizeLevel]
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(surfaceColors.surface).padding(16.dp)
        ) {
            Text("The quick brown fox jumps over the lazy dog.\n\u3053\u3093\u306B\u3061\u306F\u4E16\u754C\u3002\u5B66\u3076\u3053\u3068\u306F\u5F37\u307F\u3060\u3002",
                color = surfaceColors.textPrimary, fontSize = currentSize.sp, lineHeight = (currentSize * 1.5).sp)
        }
        Spacer(Modifier.height(12.dp))
        Text("This is how your study content will appear.",
            color = surfaceColors.textMuted, fontSize = 12.sp)
    }
}

// ============================================
// STEP 6: Sidebar Layout
// ============================================

@Composable
private fun OnboardingSidebar(accent: Color) {
    val themeState = LocalKaiteyoThemeState.current
    val surfaceColors = LocalSurfaceColors.current

    Column {
        Text("Sidebar Layout",
            color = surfaceColors.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("Choose how your navigation sidebar behaves",
            color = surfaceColors.textMuted, fontSize = 14.sp)
        Spacer(Modifier.height(20.dp))

        // Position selection
        Text("Position", color = surfaceColors.textSecondary, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SidebarPosition.entries.forEach { pos ->
                val isSelected = themeState.layoutConfig.sidebarPosition == pos
                Box(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) accent.copy(alpha = 0.12f) else surfaceColors.surface)
                        .border(1.5.dp, if (isSelected) accent else surfaceColors.border.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                        .clickable { themeState.layoutConfig = themeState.layoutConfig.copy(sidebarPosition = pos) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val icon = when (pos) {
                            SidebarPosition.Left -> "\u25B6"
                            SidebarPosition.Right -> "\u25C0"
                            SidebarPosition.Top -> "\u25BC"
                            SidebarPosition.Bottom -> "\u25B2"
                        }
                        Text(icon, color = if (isSelected) accent else surfaceColors.textSecondary, fontSize = 18.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(pos.displayName, color = if (isSelected) accent else surfaceColors.textPrimary,
                            fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))

        // Mode selection
        Text("Mode", color = surfaceColors.textSecondary, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SidebarMode.entries.forEach { mode ->
                val isSelected = themeState.layoutConfig.sidebarMode == mode
                Box(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) accent.copy(alpha = 0.1f) else Color.Transparent)
                        .clickable { themeState.layoutConfig = themeState.layoutConfig.copy(sidebarMode = mode) }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(mode.displayName, color = if (isSelected) accent else surfaceColors.textSecondary,
                        fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        // Mini preview
        val previewPos = themeState.layoutConfig.sidebarPosition
        Box(
            modifier = Modifier.fillMaxWidth().height(80.dp).clip(RoundedCornerShape(12.dp))
                .background(surfaceColors.surface).padding(8.dp)
        ) {
            val sideBarMod = when (previewPos) {
                SidebarPosition.Left -> Modifier.align(Alignment.CenterStart).width(20.dp).fillMaxHeight()
                SidebarPosition.Right -> Modifier.align(Alignment.CenterEnd).width(20.dp).fillMaxHeight()
                SidebarPosition.Top -> Modifier.align(Alignment.TopCenter).fillMaxWidth().height(16.dp)
                SidebarPosition.Bottom -> Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(16.dp)
            }
            Box(
                modifier = Modifier.then(sideBarMod).clip(RoundedCornerShape(6.dp)).background(accent)
            )
        }
    }
}

// ============================================
// STEP 7: Animation Preset
// ============================================

@Composable
private fun OnboardingAnimations(accent: Color) {
    val themeState = LocalKaiteyoThemeState.current
    val surfaceColors = LocalSurfaceColors.current

    Column {
        Text("Animation Style",
            color = surfaceColors.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("Choose how animations feel",
            color = surfaceColors.textMuted, fontSize = 14.sp)
        Spacer(Modifier.height(20.dp))

        val presets = listOf(
            "None" to AnimationSpeed.Instant to "\u274C No motion — best for accessibility",
            "Minimal" to AnimationSpeed.Fast to "\u26A1 Quick fades and slides",
            "Balanced" to AnimationSpeed.Normal to "\u2699\uFE0F Smooth default animations",
            "Smooth" to AnimationSpeed.Slow to "\uD83C\uDFB2 Fluid spring-based transitions",
            "Cinematic" to AnimationSpeed.Slow to "\uD83C\uDFAC Dramatic, premium feel"
        )

        presets.forEach { (pair, desc) ->
            val (name, speed) = pair
            val isSelected = themeState.animationConfig.speed == speed
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) accent.copy(alpha = 0.1f) else surfaceColors.surface)
                    .border(1.dp, if (isSelected) accent else surfaceColors.border.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                    .clickable { themeState.animationConfig = themeState.animationConfig.copy(speed = speed) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(18.dp).clip(CircleShape)
                        .background(if (isSelected) accent else surfaceColors.border.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) Text("\u2713", color = Color.White, fontSize = 10.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(name, color = if (isSelected) accent else surfaceColors.textPrimary,
                        fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium)
                    Text(desc, color = surfaceColors.textMuted, fontSize = 10.sp)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("You can change this anytime in Settings > Motion Studio.",
            color = surfaceColors.textMuted, fontSize = 11.sp)
    }
}

// ============================================
// STEP 8: Finish
// ============================================

@Composable
private fun OnboardingFinish(accent: Color) {
    val surfaceColors = LocalSurfaceColors.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))

        Box(
            modifier = Modifier.size(96.dp).clip(RoundedCornerShape(24.dp)).background(accent),
            contentAlignment = Alignment.Center
        ) {
            Text("K", color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(20.dp))
        Text("You're All Set!",
            color = surfaceColors.textPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Kaiteyo is ready to use. Here's what you can do next:",
            color = surfaceColors.textMuted, fontSize = 14.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))

        Row(Modifier.fillMaxWidth(0.8f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NextStepCard("\uD83D\uDCDA", "Browse Decks", "Explore kanji decks and start learning", accent, Modifier.weight(1f))
            NextStepCard("\uD83D\uDCCA", "View Stats", "Check your progress and streaks", accent, Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(0.8f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NextStepCard("\u2728", "Customize", "Fine-tune themes in Theme Studio", accent, Modifier.weight(1f))
            NextStepCard("\u2699\uFE0F", "Settings", "Adjust all preferences", accent, Modifier.weight(1f))
        }

        Spacer(Modifier.height(24.dp))
        Text("Happy learning! \uD83C\uDF38",
            color = surfaceColors.textMuted.copy(alpha = 0.7f), fontSize = 14.sp)
    }
}

@Composable
private fun NextStepCard(icon: String, title: String, desc: String, accent: Color,
                         modifier: Modifier = Modifier) {
    val surfaceColors = LocalSurfaceColors.current
    Column(
        modifier = modifier.clip(RoundedCornerShape(12.dp))
            .background(surfaceColors.surface)
            .border(1.dp, surfaceColors.border.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(icon, fontSize = 20.sp)
        Spacer(Modifier.height(6.dp))
        Text(title, color = surfaceColors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(2.dp))
        Text(desc, color = surfaceColors.textMuted, fontSize = 10.sp)
    }
}
