package ua.syt0r.kanji.desktopApp

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.syt0r.kanji.presentation.common.resources.brand.BrandMark
import ua.syt0r.kanji.presentation.common.theme.AllAccentSchemes
import ua.syt0r.kanji.presentation.common.theme.AnimationSpeed
import ua.syt0r.kanji.presentation.common.theme.BaseMode
import ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoThemeState
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.theme.surfaceForBaseMode
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// ============================================
// KAITEYO BRANDED INSTALLER
// Premium installation wizard with themed experience
// 8 screens: Welcome → Location → Components → Theme → Accent → Accessibility → Progress → Completion
// ============================================

private const val VERSION = "1.0.0"

@Composable
fun BrandedInstaller(
    onComplete: () -> Unit,
    onClose: () -> Unit
) {
    val themeState = LocalKaiteyoThemeState.current
    val currentAccent = LocalKaiteyoAccent.current
    val surfaceColors = LocalSurfaceColors.current
    var currentStep by remember { mutableIntStateOf(0) }
    val totalSteps = 8

    val accent by animateColorAsState(
        targetValue = currentAccent.primary,
        animationSpec = tween(800), label = "installerAccent"
    )

    Box(
        modifier = Modifier.fillMaxSize()
            .background(surfaceColors.background)
    ) {
        // Background decorative gradient
        Box(
            modifier = Modifier.fillMaxSize()
                .drawBehind {
                    drawRect(
                        Brush.radialGradient(
                            colors = listOf(
                                accent.copy(alpha = 0.06f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.7f, size.height * 0.2f),
                            radius = size.width * 0.6f
                        )
                    )
                }
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp)
        ) {
            // Top bar with logo and progress
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // The real Kaiteyo mark — centralized brand asset, not a "K".
                BrandMark(modifier = Modifier.size(36.dp), contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Kaiteyo Installer", color = surfaceColors.textPrimary,
                        fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Version $VERSION", color = surfaceColors.textMuted, fontSize = 11.sp)
                }
                Spacer(Modifier.weight(1f))
                // Step indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until totalSteps) {
                        val isActive = i <= currentStep
                        val stepColor by animateColorAsState(
                            targetValue = if (isActive) accent else surfaceColors.border.copy(alpha = 0.3f),
                            animationSpec = tween(400), label = "stepColor"
                        )
                        Box(
                            modifier = Modifier
                                .size(if (i == currentStep) 10.dp else 6.dp)
                                .clip(CircleShape)
                                .background(stepColor)
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = surfaceColors.border.copy(alpha = 0.2f))
            Spacer(Modifier.height(24.dp))

            // Main content area with animated transitions
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = { fadeIn(tween(300)) + slideInHorizontally { it * 100 } togetherWith fadeOut(tween(200)) + slideOutHorizontally { -it * 50 } },
                label = "installerStep",
                modifier = Modifier.weight(1f)
            ) { step ->
                Box(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    when (step) {
                        0 -> WelcomeStep(accent)
                        1 -> InstallationLocation(accent)
                        2 -> ComponentsStep(accent)
                        3 -> ThemePreviewStep(accent)
                        4 -> AccentSelectionStep(accent)
                        5 -> AccessibilityStep(accent)
                        6 -> ProgressStep(accent)
                        7 -> CompletionStep(accent, onComplete)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = surfaceColors.border.copy(alpha = 0.2f))
            Spacer(Modifier.height(16.dp))

            // Navigation buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { if (currentStep > 0) currentStep-- else onClose() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = surfaceColors.textPrimary)
                ) {
                    Text(if (currentStep == 0) "Cancel" else "Back")
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (currentStep < totalSteps - 1) {
                        OutlinedButton(
                            onClick = { onClose() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = surfaceColors.textMuted)
                        ) { Text("Skip") }
                    }
                    Button(
                        onClick = {
                            if (currentStep < totalSteps - 1) currentStep++
                            else onComplete()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White)
                    ) {
                        Text(
                            if (currentStep < totalSteps - 1) "Next"
                            else if (currentStep == totalSteps - 1) "Finish"
                            else "Install"
                        )
                    }
                }
            }
        }
    }
}

// ============================================
// STEP 1: Welcome Screen
// ============================================

@Composable
private fun WelcomeStep(accent: Color) {
    val surfaceColors = LocalSurfaceColors.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))
        // The real Kaiteyo mark — centralized brand asset, not a "K".
        BrandMark(modifier = Modifier.size(96.dp), contentDescription = "Kaiteyo")
        Spacer(Modifier.height(24.dp))
        Text("Welcome to Kaiteyo",
            color = surfaceColors.textPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Your intelligent kanji learning companion",
            color = surfaceColors.textMuted, fontSize = 16.sp)
        Spacer(Modifier.height(32.dp))
        Box(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(surfaceColors.surface)
                .padding(20.dp)
        ) {
            Column {
                FeatureRow("\u2728", "Smart Spaced Repetition with Anki algorithms")
                Spacer(Modifier.height(12.dp))
                FeatureRow("\uD83C\uDFA8", "Beautiful customizable themes")
                Spacer(Modifier.height(12.dp))
                FeatureRow("\uD83D\uDD0D", "Full-text search across all kanji and decks")
                Spacer(Modifier.height(12.dp))
                FeatureRow("\uD83D\uDCF1", "Cross-platform (Windows, macOS, Linux, Android, iOS)")
                Spacer(Modifier.height(12.dp))
                FeatureRow("\uD83D\uDCCA", "Detailed statistics and progress tracking")
                Spacer(Modifier.height(12.dp))
                FeatureRow("\uD83C\uDFAF", "5000+ kanji with stroke order and examples")
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Version $VERSION \u00B7 2024 Kaiteyo",
            color = surfaceColors.textMuted.copy(alpha = 0.6f), fontSize = 11.sp)
    }
}

@Composable
private fun FeatureRow(icon: String, text: String) {
    val surfaceColors = LocalSurfaceColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 16.sp)
        Spacer(Modifier.width(12.dp))
        Text(text, color = surfaceColors.textSecondary, fontSize = 13.sp)
    }
}

// ============================================
// STEP 2: Installation Location
// ============================================

@Composable
private fun InstallationLocation(accent: Color) {
    val surfaceColors = LocalSurfaceColors.current
    var installPath by remember { mutableStateOf("C:\\Program Files\\Kaiteyo") }
    var dataPath by remember { mutableStateOf("C:\\Users\\Admin\\AppData\\Local\\Kaiteyo") }
    var diskSpaceInfo by remember { mutableStateOf("2.4 GB available on C:") }

    Column {
        Text("Installation Location",
            color = surfaceColors.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("Choose where to install Kaiteyo",
            color = surfaceColors.textMuted, fontSize = 14.sp)
        Spacer(Modifier.height(24.dp))

        Text("Install Location", color = surfaceColors.textSecondary, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(surfaceColors.surfaceInteractive)
                .border(1.dp, surfaceColors.border.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                .clickable { /* Browse folder */ }
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(installPath, color = surfaceColors.textPrimary, fontSize = 14.sp,
                    modifier = Modifier.weight(1f))
                Text("Browse", color = accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(diskSpaceInfo, color = surfaceColors.textMuted, fontSize = 11.sp)
        Spacer(Modifier.height(20.dp))

        Text("Data Location (appdata)", color = surfaceColors.textSecondary, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(surfaceColors.surfaceInteractive)
                .border(1.dp, surfaceColors.border.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                .clickable { /* Browse folder */ }
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(dataPath, color = surfaceColors.textPrimary, fontSize = 14.sp,
                    modifier = Modifier.weight(1f))
                Text("Browse", color = accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(20.dp))

        Text("Required Space", color = surfaceColors.textSecondary, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            RequirementBar("Application", "450 MB", 0.6f, accent, Modifier.weight(1f))
        }
    }
}

@Composable
private fun RequirementBar(label: String, size: String, fraction: Float, accent: Color,
                           modifier: Modifier = Modifier) {
    val surfaceColors = LocalSurfaceColors.current
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = surfaceColors.textMuted, fontSize = 12.sp)
            Text(size, color = surfaceColors.textPrimary, fontSize = 12.sp)
        }
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
            .background(surfaceColors.border.copy(alpha = 0.3f))) {
            Box(Modifier.fillMaxWidth(fraction).fillMaxHeight()
                .clip(RoundedCornerShape(2.dp)).background(accent))
        }
    }
}

// ============================================
// STEP 3: Components Selection
// ============================================

@Composable
private fun ComponentsStep(accent: Color) {
    val surfaceColors = LocalSurfaceColors.current
    var shortcuts by remember { mutableStateOf(true) }
    var startup by remember { mutableStateOf(false) }
    var fileAssoc by remember { mutableStateOf(true) }
    var desktopIcon by remember { mutableStateOf(true) }
    var autoUpdate by remember { mutableStateOf(true) }

    Column {
        Text("Choose Components",
            color = surfaceColors.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("Select optional features to install",
            color = surfaceColors.textMuted, fontSize = 14.sp)
        Spacer(Modifier.height(24.dp))

        ComponentToggle("Create Desktop Shortcut", "Add Kaiteyo icon to your desktop",
            desktopIcon, accent) { desktopIcon = it }
        Spacer(Modifier.height(4.dp))
        ComponentToggle("Add to Start Menu", "Pin Kaiteyo to the Start Menu for quick access",
            shortcuts, accent) { shortcuts = it }
        Spacer(Modifier.height(4.dp))
        ComponentToggle("Launch on Startup", "Kaiteyo automatically launches when you log in",
            startup, accent) { startup = it }
        Spacer(Modifier.height(4.dp))
        ComponentToggle("Associate .kaiteyo Files", "Open Kaiteyo data files with this program",
            fileAssoc, accent) { fileAssoc = it }
        Spacer(Modifier.height(4.dp))
        ComponentToggle("Enable Auto-Update", "Automatically check for and install updates",
            autoUpdate, accent) { autoUpdate = it }

        Spacer(Modifier.height(24.dp))
        Text("Installation Size: ~450 MB",
            color = surfaceColors.textMuted, fontSize = 12.sp)
    }
}

@Composable
private fun ComponentToggle(
    title: String, description: String, checked: Boolean,
    accent: Color, onChecked: (Boolean) -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(surfaceColors.surface)
            .clickable { onChecked(!checked) }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onChecked,
            colors = CheckboxDefaults.colors(checkedColor = accent, checkmarkColor = Color.White)
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, color = surfaceColors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(description, color = surfaceColors.textMuted, fontSize = 11.sp)
        }
    }
}

// ============================================
// STEP 4: Theme Preview
// ============================================

@Composable
private fun ThemePreviewStep(accent: Color) {
    val themeState = LocalKaiteyoThemeState.current
    val surfaceColors = LocalSurfaceColors.current

    Column {
        Text("Choose Your Theme",
            color = surfaceColors.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("Select a base theme to start with",
            color = surfaceColors.textMuted, fontSize = 14.sp)
        Spacer(Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BaseMode.entries.forEach { mode ->
                val isSelected = themeState.baseMode == mode
                val surf = surfaceForBaseMode(mode)
                Column(
                    modifier = Modifier.weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(surf.background)
                        .border(2.dp, if (isSelected) accent else surf.border.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .clickable { themeState.baseMode = mode }
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(mode.displayName, color = surf.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    // The real Kaiteyo mark — centralized brand asset, not a "K".
                    BrandMark(modifier = Modifier.size(40.dp), contentDescription = null)
                    Spacer(Modifier.height(8.dp))
                    Text("Preview", color = surf.textMuted, fontSize = 9.sp)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        // Live mini-preview
        val previewSurface = surfaceForBaseMode(themeState.baseMode)
        Box(
            modifier = Modifier.fillMaxWidth().height(120.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(previewSurface.surface)
                .padding(12.dp)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.width(40.dp).fillMaxHeight()
                        .clip(RoundedCornerShape(6.dp))
                        .background(previewSurface.surfaceElevated),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    Box(Modifier.size(16.dp).clip(CircleShape).background(accent))
                    Box(Modifier.size(12.dp).clip(CircleShape).background(previewSurface.textMuted.copy(alpha = 0.3f)))
                    Box(Modifier.size(12.dp).clip(CircleShape).background(previewSurface.textMuted.copy(alpha = 0.3f)))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Box(
                        Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(previewSurface.textPrimary.copy(alpha = 0.2f))
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(3) {
                            Box(
                                Modifier.weight(1f).height(24.dp).clip(RoundedCornerShape(6.dp)).background(previewSurface.surfaceElevated)
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier.fillMaxWidth().height(30.dp).clip(RoundedCornerShape(6.dp)).background(previewSurface.surfaceElevated)
                    )
                }
            }
        }
    }
}

// ============================================
// STEP 5: Accent Selection
// ============================================

@Composable
private fun AccentSelectionStep(accent: Color) {
    val themeState = LocalKaiteyoThemeState.current
    val surfaceColors = LocalSurfaceColors.current
    val isSepia = themeState.baseMode == BaseMode.Sepia

    Column {
        Text("Accent Color",
            color = surfaceColors.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("Pick your accent color theme",
            color = surfaceColors.textMuted, fontSize = 14.sp)

        if (isSepia) {
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(surfaceColors.surface).padding(16.dp)
            ) {
                Text("Accent themes are disabled in Sepia mode. Switch to another base theme to customize accents.",
                    color = surfaceColors.textMuted, fontSize = 12.sp)
            }
            return
        }

        Spacer(Modifier.height(16.dp))
        AllAccentSchemes.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { scheme ->
                    val isSelected = themeState.accentScheme.name == scheme.name
                    Box(
                        modifier = Modifier.weight(1f)
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) accent.copy(alpha = 0.15f) else surfaceColors.surface)
                            .border(1.5.dp, if (isSelected) accent else surfaceColors.border.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                            .clickable { themeState.accentScheme = scheme }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                scheme.previewColors.forEach { c ->
                                    Box(Modifier.size(12.dp).clip(CircleShape).background(c))
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(scheme.name, color = if (isSelected) accent else surfaceColors.textPrimary,
                                fontSize = 9.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
                        }
                    }
                }
            }
        }
    }
}

// ============================================
// STEP 6: Accessibility
// ============================================

@Composable
private fun AccessibilityStep(accent: Color) {
    val surfaceColors = LocalSurfaceColors.current
    var uiScale by remember { mutableFloatStateOf(100f) }
    var fontSize by remember { mutableStateOf("Medium") }
    var highContrast by remember { mutableStateOf(false) }
    var animations by remember { mutableStateOf(true) }

    Column {
        Text("Accessibility Settings",
            color = surfaceColors.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("Customize for your needs",
            color = surfaceColors.textMuted, fontSize = 14.sp)
        Spacer(Modifier.height(24.dp))

        Text("UI Scale: ${uiScale.roundToInt()}%", color = surfaceColors.textSecondary, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(6.dp))
        Slider(value = uiScale, onValueChange = { uiScale = it }, valueRange = 80f..200f,
            modifier = Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("80%", color = surfaceColors.textMuted, fontSize = 10.sp)
            Text("200%", color = surfaceColors.textMuted, fontSize = 10.sp)
        }
        Spacer(Modifier.height(20.dp))

        Text("Font Size", color = surfaceColors.textSecondary, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Small", "Medium", "Large", "Extra Large").forEach { size ->
                val isSelected = fontSize == size
                Box(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) accent.copy(alpha = 0.15f) else surfaceColors.surface)
                        .border(1.dp, if (isSelected) accent else surfaceColors.border.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .clickable { fontSize = size }.padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) { Text(size, color = if (isSelected) accent else surfaceColors.textSecondary, fontSize = 11.sp) }
            }
        }
        Spacer(Modifier.height(20.dp))

        ComponentToggle("High Contrast Mode", "Enhance visual contrast for better readability",
            highContrast, accent) { highContrast = it }
        Spacer(Modifier.height(4.dp))
        ComponentToggle("Enable Animations", "Smooth transitions and effects",
            animations, accent) { animations = it }
    }
}

// ============================================
// STEP 7: Progress / Installation
// ============================================

@Composable
private fun ProgressStep(accent: Color) {
    val surfaceColors = LocalSurfaceColors.current
    var progress by remember { mutableFloatStateOf(0f) }
    val infiniteTransition = rememberInfiniteTransition(label = "progressAnim")
    val animProgress by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "installProgress"
    )

    // Simulated progress
    progress = (animProgress * 0.7f + 0.1f).coerceAtMost(0.95f)

    val progressText = when {
        progress < 0.3f -> "Preparing installation..."
        progress < 0.5f -> "Copying application files..."
        progress < 0.7f -> "Configuring settings..."
        progress < 0.9f -> "Optimizing for your system..."
        else -> "Almost done..."
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // The real Kaiteyo mark — centralized brand asset, not a "K".
        BrandMark(modifier = Modifier.size(80.dp), contentDescription = "Kaiteyo")
        Spacer(Modifier.height(24.dp))
        Text("Installing Kaiteyo...",
            color = surfaceColors.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(progressText,
            color = surfaceColors.textMuted, fontSize = 14.sp)
        Spacer(Modifier.height(24.dp))

        // Animated progress bar
        Box(
            modifier = Modifier.fillMaxWidth(0.6f).height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(surfaceColors.border.copy(alpha = 0.3f))
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(progress).fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        Brush.horizontalGradient(listOf(accent, accent.copy(red = (accent.red * 0.8f).coerceAtMost(1f))))
                    )
            )
        }
        Spacer(Modifier.height(8.dp))
        Text("${(progress * 100).toInt()}%",
            color = accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)

        Spacer(Modifier.height(24.dp))
        // Step list
        Column(horizontalAlignment = Alignment.Start) {
            listOf("Extracting files", "Setting up components", "Configuring themes",
                "Creating shortcuts", "Finalizing").forEachIndexed { i, step ->
                val done = progress > (i + 1) * 0.18f
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        if (done) "\u2713" else "\u25CB",
                        color = if (done) accent else surfaceColors.textMuted,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(step, color = if (done) surfaceColors.textPrimary else surfaceColors.textMuted, fontSize = 13.sp)
                }
            }
        }
    }
}

// ============================================
// STEP 8: Completion Screen
// ============================================

@Composable
private fun CompletionStep(accent: Color, onComplete: () -> Unit) {
    val surfaceColors = LocalSurfaceColors.current
    var launchNow by remember { mutableStateOf(true) }
    var deleteInstaller by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Success animation
        Box(
            modifier = Modifier.size(80.dp).clip(CircleShape).background(accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(accent),
                contentAlignment = Alignment.Center
            ) { Text("\u2713", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold) }
        }
        Spacer(Modifier.height(20.dp))
        Text("Installation Complete!",
            color = surfaceColors.textPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Kaiteyo has been successfully installed on your system.",
            color = surfaceColors.textMuted, fontSize = 14.sp)
        Spacer(Modifier.height(32.dp))

        Box(
            modifier = Modifier.fillMaxWidth(0.7f)
                .clip(RoundedCornerShape(12.dp))
                .background(surfaceColors.surface)
                .padding(16.dp)
        ) {
            Column {
                ComponentToggle("Launch Kaiteyo now", "Start the application immediately",
                    launchNow, accent) { launchNow = it }
                Spacer(Modifier.height(8.dp))
                ComponentToggle("Delete installer files", "Remove temporary installation files",
                    deleteInstaller, accent) { deleteInstaller = it }
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = surfaceColors.border.copy(alpha = 0.2f))
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionButton("\uD83D\uDCD6", "View Release Notes", accent, Modifier.weight(1f))
                    ActionButton("\uD83C\uDF10", "Visit GitHub", accent, Modifier.weight(1f))
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("Thank you for choosing Kaiteyo!",
            color = surfaceColors.textMuted.copy(alpha = 0.7f), fontSize = 12.sp)
    }
}

@Composable
private fun ActionButton(icon: String, label: String, accent: Color, modifier: Modifier = Modifier) {
    val surfaceColors = LocalSurfaceColors.current
    Row(
        modifier = modifier.clip(RoundedCornerShape(8.dp))
            .background(surfaceColors.surfaceInteractive)
            .clickable { }.padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(icon, fontSize = 12.sp)
        Spacer(Modifier.width(6.dp))
        Text(label, color = surfaceColors.textPrimary, fontSize = 11.sp)
    }
}
