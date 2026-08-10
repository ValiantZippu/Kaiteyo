package ua.syt0r.kanji.presentation.screen.main.screen.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.syt0r.kanji.presentation.common.theme.AllAccentSchemes
import ua.syt0r.kanji.presentation.common.theme.AnimationConfig
import ua.syt0r.kanji.presentation.common.theme.AnimationSpeed
import ua.syt0r.kanji.presentation.common.theme.BaseMode
import ua.syt0r.kanji.presentation.common.theme.CornerRadiusStyle
import ua.syt0r.kanji.presentation.common.theme.GlowConfig
import ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme
import ua.syt0r.kanji.presentation.common.theme.KaiteyoGradient
import ua.syt0r.kanji.presentation.common.theme.LayoutConfig
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoThemeState
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.theme.PageTransitionType
import ua.syt0r.kanji.presentation.common.theme.RadiusConfig
import ua.syt0r.kanji.presentation.common.theme.UIDensity
import ua.syt0r.kanji.presentation.common.nav.LocalNavigationSettings
import ua.syt0r.kanji.presentation.common.nav.NavigationSettingsOverlay
import ua.syt0r.kanji.presentation.common.nav.rememberFormFactor
import ua.syt0r.kanji.presentation.common.resources.string.resolveString
import ua.syt0r.kanji.presentation.common.theme.gradientForAccent
import ua.syt0r.kanji.presentation.common.theme.surfaceForBaseMode
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

// ============================================
// KAITEYO THEME STUDIO v2.0
// Premium professional theme customization suite
// Color wheel · Gradient editor · Live preview · Icon packs · Export/Import
// ============================================

@Composable
fun ThemeStudio() {
    val themeState = LocalKaiteyoThemeState.current
    val currentAccent = LocalKaiteyoAccent.current
    val surfaceColors = LocalSurfaceColors.current
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    Row(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Column(modifier = Modifier.width(480.dp).fillMaxHeight()) {
            Text("Theme Studio", style = MaterialTheme.typography.titleLarge,
                color = surfaceColors.textPrimary, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ThemeStudioTab.entries.forEach { tab ->
                    val isSelected = selectedTabIndex == ThemeStudioTab.entries.indexOf(tab)
                    val tabBg by animateColorAsState(
                        targetValue = if (isSelected) currentAccent.primary.copy(alpha = 0.15f) else Color.Transparent,
                        animationSpec = tween(200), label = "tabBg")
                    val tabText by animateColorAsState(
                        targetValue = if (isSelected) currentAccent.primary else surfaceColors.textSecondary,
                        animationSpec = tween(200), label = "tabText")
                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(tabBg)
                        .clickable { selectedTabIndex = ThemeStudioTab.entries.indexOf(tab) }.padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center) {
                        Text(tab.displayName, color = tabText, fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = surfaceColors.border.copy(alpha = 0.3f))
            Spacer(Modifier.height(8.dp))
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                when (selectedTabIndex) {
                    0 -> BaseThemeTab()
                    1 -> AccentThemeTab()
                    2 -> CustomColorTab()
                    3 -> GradientEditorTab()
                    4 -> MotionTab()
                    5 -> LayoutTab()
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        HorizontalDivider(modifier = Modifier.fillMaxHeight().width(1.dp),
            color = surfaceColors.border.copy(alpha = 0.2f))
        Spacer(Modifier.width(12.dp))
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            LivePreviewPanel()
        }
    }
}

// ThemeStudioTab defined in ThemeStudioTab.kt

// ============================================
// TAB 1: Base Theme Selector
// ============================================

@Composable
private fun BaseThemeTab() {
    val themeState = LocalKaiteyoThemeState.current
    val surfaceColors = LocalSurfaceColors.current
    val currentAccent = LocalKaiteyoAccent.current

    Text("Base Themes", style = MaterialTheme.typography.titleMedium,
        color = surfaceColors.textPrimary, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(4.dp))
    Text("Choose your foundation", style = MaterialTheme.typography.bodySmall, color = surfaceColors.textMuted)
    Spacer(Modifier.height(12.dp))

    BaseMode.entries.forEach { mode ->
        val isSelected = themeState.baseMode == mode
        val isSepia = mode == BaseMode.Sepia
        val cardBg by animateColorAsState(
            targetValue = if (isSelected) currentAccent.primary.copy(alpha = 0.12f) else surfaceColors.surface,
            animationSpec = tween(200), label = "baseCardBg")
        val cardBorder by animateColorAsState(
            targetValue = if (isSelected) currentAccent.primary else surfaceColors.border.copy(alpha = 0.2f),
            animationSpec = tween(200), label = "baseCardBorder")
        val surf = surfaceForBaseMode(mode)

        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp)).background(cardBg)
            .border(1.5.dp, cardBorder, RoundedCornerShape(12.dp))
            .clickable { themeState.baseMode = mode }.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(surf.background)
                .border(0.5.dp, surf.border.copy(alpha = 0.3f), RoundedCornerShape(8.dp)))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(mode.displayName, color = if (isSelected) currentAccent.primary else surfaceColors.textPrimary,
                    fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium)
                Text(if (isSepia) "Warm paper · No accent themes · Distraction-free"
                    else "Full accent support · ${surf.background.toArgb().toString(16)} bg",
                    color = surfaceColors.textMuted, fontSize = 11.sp)
            }
            if (isSelected) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(currentAccent.primary))
            }
        }
    }
}

// ============================================
// TAB 2: Accent Theme Selector
// ============================================

@Composable
private fun AccentThemeTab() {
    val themeState = LocalKaiteyoThemeState.current
    val surfaceColors = LocalSurfaceColors.current
    val currentAccent = LocalKaiteyoAccent.current
    val isSepia = themeState.baseMode == BaseMode.Sepia

    Text("Accent Themes", style = MaterialTheme.typography.titleMedium,
        color = surfaceColors.textPrimary, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(4.dp))

    if (isSepia) {
        Text("Accent themes are disabled in Sepia reading mode for distraction-free focus.",
            color = surfaceColors.textMuted, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
            .background(surfaceColors.surface).padding(16.dp), contentAlignment = Alignment.Center) {
            Text("Switch to OLED, Dark, or Light to use accent themes.",
                color = surfaceColors.textMuted, fontSize = 12.sp)
        }
        return
    }

    Text("Choose your color identity", style = MaterialTheme.typography.bodySmall, color = surfaceColors.textMuted)
    Spacer(Modifier.height(12.dp))

    AllAccentSchemes.chunked(2).forEach { row ->
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            row.forEach { scheme ->
                val isSelected = currentAccent.name == scheme.name
                val cardBg by animateColorAsState(
                    targetValue = if (isSelected) currentAccent.primary.copy(alpha = 0.12f) else surfaceColors.surface,
                    animationSpec = tween(200), label = "accentCardBg")
                val cardBorder by animateColorAsState(
                    targetValue = if (isSelected) currentAccent.primary else surfaceColors.border.copy(alpha = 0.2f),
                    animationSpec = tween(200), label = "accentCardBorder")
                Column(modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(cardBg)
                    .border(1.dp, cardBorder, RoundedCornerShape(10.dp))
                    .clickable { themeState.accentScheme = scheme }.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            scheme.previewColors.forEach { color ->
                                Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(color))
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(scheme.name, color = if (isSelected) currentAccent.primary else surfaceColors.textPrimary,
                            fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium)
                    }
                    Spacer(Modifier.height(6.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(1.5.dp))
                        .background(Brush.horizontalGradient(listOf(
                            scheme.gradientStart ?: scheme.primary,
                            scheme.gradientEnd ?: scheme.secondary))))
                }
            }
        }
        Spacer(Modifier.height(6.dp))
    }
}

// ============================================
// TAB 3: Custom Color Creator
// Full color wheel + HSV/RGB/HEX/HSL + Recent/Saved + Apply
// ============================================

@Composable
private fun CustomColorTab() {
    val themeState = LocalKaiteyoThemeState.current
    val surfaceColors = LocalSurfaceColors.current
    val currentAccent = LocalKaiteyoAccent.current

    Text("Custom Color Creator", style = MaterialTheme.typography.titleMedium,
        color = surfaceColors.textPrimary, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(4.dp))
    Text("Fine-tune every color in your theme", style = MaterialTheme.typography.bodySmall, color = surfaceColors.textMuted)
    Spacer(Modifier.height(12.dp))

    var selectedTarget by remember { mutableStateOf("Primary") }
    val targets = listOf("Primary", "Secondary", "Tertiary", "Background", "Surface", "Text",
        "SurfaceVar", "Outline", "Success", "Warning", "Error")
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        LazyColumn(modifier = Modifier.weight(1f).height(120.dp)) {
            items(targets) { target ->
                val isSelected = selectedTarget == target
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) currentAccent.primary.copy(alpha = 0.15f) else Color.Transparent)
                    .clickable { selectedTarget = target }.padding(vertical = 4.dp, horizontal = 6.dp)) {
                    Text(target, color = if (isSelected) currentAccent.primary else surfaceColors.textSecondary,
                        fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }
    }
    Spacer(Modifier.height(12.dp))

    var mode by remember { mutableStateOf("RGB") }
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf("RGB", "HSL", "HSV", "HEX").forEach { m ->
            val isSelected = mode == m
            Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(6.dp))
                .background(if (isSelected) currentAccent.primary.copy(alpha = 0.12f) else Color.Transparent)
                .clickable { mode = m }.padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
                Text(m, color = if (isSelected) currentAccent.primary else surfaceColors.textSecondary,
                    fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
            }
        }
    }
    Spacer(Modifier.height(12.dp))

    var currentColor by remember { mutableStateOf(Color(0xFF4FC3F7)) }

    // Color Wheel
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        ColorWheelPicker(
            selectedColor = currentColor,
            onColorChanged = { currentColor = it },
            modifier = Modifier.size(180.dp)
        )
    }
    Spacer(Modifier.height(12.dp))

    when (mode) {
        "RGB" -> RgbEditor(currentColor) { currentColor = it }
        "HSL" -> HslEditor(currentColor) { currentColor = it }
        "HSV" -> HsvEditor(currentColor) { currentColor = it }
        "HEX" -> HexEditor(currentColor) { currentColor = it }
    }

    Spacer(Modifier.height(12.dp))

    // Recent & Saved Colors
    var savedColors by remember { mutableStateOf(listOf<Color>()) }
    if (savedColors.isNotEmpty()) {
        Text("Saved Colors", color = surfaceColors.textMuted, fontSize = 11.sp)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            savedColors.take(8).forEach { color ->
                Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(color)
                    .border(1.dp, surfaceColors.border.copy(alpha = 0.3f), CircleShape)
                    .clickable { currentColor = color })
            }
        }
        Spacer(Modifier.height(8.dp))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { savedColors = (savedColors + currentColor).distinct().take(20) },
            colors = ButtonDefaults.buttonColors(containerColor = currentAccent.primary),
            modifier = Modifier.weight(1f)) { Text("Save", fontSize = 11.sp) }
        Button(onClick = { savedColors = emptyList() },
            colors = ButtonDefaults.buttonColors(containerColor = surfaceColors.surface),
            modifier = Modifier.weight(1f)) { Text("Clear", fontSize = 11.sp) }
    }

    Spacer(Modifier.height(12.dp))
    Button(onClick = {
        val currentScheme = themeState.accentScheme
        val newScheme = when (selectedTarget) {
            "Primary" -> currentScheme.copy(primary = currentColor)
            "Secondary" -> currentScheme.copy(secondary = currentColor)
            "Tertiary" -> currentScheme.copy(tertiary = currentColor)
            else -> currentScheme
        }
        themeState.accentScheme = newScheme
    }, colors = ButtonDefaults.buttonColors(containerColor = currentAccent.primary, contentColor = currentAccent.onPrimary),
        modifier = Modifier.fillMaxWidth()) {
        Text("Apply to $selectedTarget")
    }
}

// ============================================
// COLOR WHEEL PICKER — Interactive HSV wheel
// ============================================

@Composable
private fun ColorWheelPicker(
    selectedColor: Color,
    onColorChanged: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    var isDragging by remember { mutableStateOf(false) }

    Canvas(modifier = modifier
        .clip(CircleShape)
        .border(2.dp, surfaceColors.border.copy(alpha = 0.2f), CircleShape)
        .pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { isDragging = true },
                onDragEnd = { isDragging = false },
                onDragCancel = { isDragging = false },
                onDrag = { change, _ ->
                    change.consume()
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val dx = change.position.x - center.x
                    val dy = change.position.y - center.y
                    val distance = sqrt(dx * dx + dy * dy)
                    val radius = size.width / 2f
                    if (distance <= radius) {
                        val hue = (atan2(dy, dx) * 180f / PI.toFloat() + 90f + 360f) % 360f
                        val saturation = (distance / radius).coerceIn(0f, 1f)
                        onColorChanged(Color.hsv(hue, saturation, 1f))
                    }
                }
            )
        }
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val outerRadius = size.width / 2f
        for (y in 0 until size.height.toInt()) {
            for (x in 0 until size.width.toInt()) {
                val dx = x - center.x
                val dy = y - center.y
                val distance = sqrt(dx * dx + dy * dy)
                if (distance <= outerRadius) {
                    val hue = (atan2(dy, dx) * 180f / PI.toFloat() + 90f + 360f) % 360f
                    val saturation = (distance / outerRadius).coerceIn(0f, 1f)
                    drawCircle(
                        color = Color.hsv(hue, saturation, 1f).copy(alpha = 0.5f),
                        radius = outerRadius,
                        center = center
                    )
                }
            }
        }
        val selHue = selectedColor.hue
        val selSat = selectedColor.saturation
        val selRadius = selSat * outerRadius
        val selAngle = (selHue - 90f) * PI.toFloat() / 180f
        val indicatorX = center.x + selRadius * cos(selAngle)
        val indicatorY = center.y + selRadius * sin(selAngle)
        drawCircle(color = Color.White, radius = 6f, center = Offset(indicatorX, indicatorY))
        drawCircle(color = Color.Black.copy(alpha = 0.3f), radius = 6f,
            center = Offset(indicatorX, indicatorY), style = Stroke(width = 1.5f))
    }
}

// ============================================
// RGB Editor
// ============================================

@Composable
private fun RgbEditor(color: Color, onChange: (Color) -> Unit) {
    val surfaceColors = LocalSurfaceColors.current
    var r by remember(color) { mutableFloatStateOf(color.red * 255f) }
    var g by remember(color) { mutableFloatStateOf(color.green * 255f) }
    var b by remember(color) { mutableFloatStateOf(color.blue * 255f) }

    LaunchedEffect(r, g, b) { onChange(Color(r / 255f, g / 255f, b / 255f, color.alpha)) }

    ColorSlider("R", (r / 255f).coerceIn(0f, 1f), Color.Red.copy(alpha = 0.3f)) { r = (it * 255f).coerceIn(0f, 255f) }
    ColorSlider("G", (g / 255f).coerceIn(0f, 1f), Color.Green.copy(alpha = 0.3f)) { g = (it * 255f).coerceIn(0f, 255f) }
    ColorSlider("B", (b / 255f).coerceIn(0f, 1f), Color.Blue.copy(alpha = 0.3f)) { b = (it * 255f).coerceIn(0f, 255f) }
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Text("RGB(${(r).roundToInt()}, ${(g).roundToInt()}, ${(b).roundToInt()})",
            color = surfaceColors.textMuted, fontSize = 11.sp)
        Text(formatFloat(color.alpha, 2), color = surfaceColors.textMuted, fontSize = 11.sp)
    }
}

// ============================================
// HSL Editor
// ============================================

@Composable
private fun HslEditor(color: Color, onChange: (Color) -> Unit) {
    val surfaceColors = LocalSurfaceColors.current
    var hue by remember(color) { mutableFloatStateOf(color.hue.coerceIn(0f, 360f)) }
    var sat by remember(color) { mutableFloatStateOf(color.saturation.coerceIn(0f, 1f)) }
    var light by remember(color) { mutableFloatStateOf(color.lightness.coerceIn(0f, 1f)) }

    LaunchedEffect(hue, sat, light) { onChange(Color.hsl(hue, sat, light, color.alpha)) }

    SliderWithLabel("Hue", hue, 0f..360f, "°") { hue = it }
    SliderWithLabel("Saturation", sat, 0f..1f, "%") { sat = it }
    SliderWithLabel("Lightness", light, 0f..1f, "%") { light = it }
    Text("HSL(${hue.toInt()}°, ${(sat * 100).toInt()}%, ${(light * 100).toInt()}%)",
        color = surfaceColors.textMuted, fontSize = 11.sp)
}

// ============================================
// HSV Editor
// ============================================

@Composable
private fun HsvEditor(color: Color, onChange: (Color) -> Unit) {
    val surfaceColors = LocalSurfaceColors.current
    var hue by remember(color) { mutableFloatStateOf(color.hue.coerceIn(0f, 360f)) }
    var sat by remember(color) { mutableFloatStateOf(color.saturation.coerceIn(0f, 1f)) }
    var value by remember(color) { mutableFloatStateOf(1f) }

    LaunchedEffect(hue, sat, value) { onChange(Color.hsv(hue, sat, value, color.alpha)) }

    SliderWithLabel("Hue", hue, 0f..360f, "°") { hue = it }
    SliderWithLabel("Saturation", sat, 0f..1f, "%") { sat = it }
    SliderWithLabel("Value", value, 0f..1f, "%") { value = it }
    Text("HSV(${hue.toInt()}°, ${(sat * 100).toInt()}%, ${(value * 100).toInt()}%)",
        color = surfaceColors.textMuted, fontSize = 11.sp)
}

// ============================================
// HEX Editor
// ============================================

@Composable
private fun HexEditor(color: Color, onChange: (Color) -> Unit) {
    val surfaceColors = LocalSurfaceColors.current
    var hexValue by remember(color) { mutableStateOf(colorToHex(color).removePrefix("#")) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("#", color = surfaceColors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(4.dp))
        Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(surfaceColors.surfaceInteractive)
            .border(1.dp, surfaceColors.border.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)) {
            BasicTextField(value = TextFieldValue(hexValue),
                onValueChange = {
                    hexValue = it.text.take(6).filter { c -> c.isDigit() || c.uppercase() in "ABCDEF" }.uppercase()
                    if (hexValue.length == 6) {
                        val r = hexValue.substring(0, 2).toIntOrNull(16) ?: 0
                        val g = hexValue.substring(2, 4).toIntOrNull(16) ?: 0
                        val b = hexValue.substring(4, 6).toIntOrNull(16) ?: 0
                        onChange(Color(r / 255f, g / 255f, b / 255f, color.alpha))
                    }
                },
                textStyle = TextStyle(color = surfaceColors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium),
                singleLine = true, modifier = Modifier.width(100.dp))
        }
    }
}

// ============================================
// TAB 4: Gradient Editor
// ============================================

@Composable
private fun GradientEditorTab() {
    val themeState = LocalKaiteyoThemeState.current
    val surfaceColors = LocalSurfaceColors.current
    val currentAccent = LocalKaiteyoAccent.current

    Text("Gradient Editor", style = MaterialTheme.typography.titleMedium,
        color = surfaceColors.textPrimary, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(4.dp))
    Text("Design custom gradients for your theme", style = MaterialTheme.typography.bodySmall, color = surfaceColors.textMuted)
    Spacer(Modifier.height(12.dp))

    var gradientType by remember { mutableStateOf("Linear") }
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf("Linear", "Radial", "Angular").forEach { type ->
            val isSelected = gradientType == type
            Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                .background(if (isSelected) currentAccent.primary.copy(alpha = 0.15f) else surfaceColors.surface)
                .border(1.dp, if (isSelected) currentAccent.primary else surfaceColors.border.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                .clickable { gradientType = type }.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                Text(type, color = if (isSelected) currentAccent.primary else surfaceColors.textSecondary,
                    fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
            }
        }
    }
    Spacer(Modifier.height(12.dp))

    var stopCount by remember { mutableIntStateOf(2) }
    Text("Gradient Stops: $stopCount", color = surfaceColors.textPrimary, fontSize = 13.sp)
    Slider(value = stopCount.toFloat(), onValueChange = { stopCount = it.roundToInt().coerceIn(2, 8) },
        valueRange = 2f..8f, steps = 6, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(8.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(stopCount) { index ->
            Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(currentAccent.primary)
                .border(1.dp, surfaceColors.border.copy(alpha = 0.3f), CircleShape)
                .clickable { /* Open color picker for this stop */ },
                contentAlignment = Alignment.Center) {
                Text("${index + 1}", color = Color.White, fontSize = 9.sp)
            }
        }
    }
    Spacer(Modifier.height(12.dp))

    var angle by remember { mutableFloatStateOf(45f) }
    if (gradientType == "Linear" || gradientType == "Angular") {
        SliderWithLabel("Angle", angle, 0f..360f, "°") { angle = it }
    }
    Spacer(Modifier.height(8.dp))

    var intensity by remember { mutableFloatStateOf(1f) }
    SliderWithLabel("Intensity", intensity, 0f..2f, "x") { intensity = it }
    Spacer(Modifier.height(8.dp))

    var opacity by remember { mutableFloatStateOf(1f) }
    SliderWithLabel("Opacity", opacity, 0f..1f, "%") { opacity = it }
    Spacer(Modifier.height(16.dp))

    // Gradient Preview
    val gradColors = List(stopCount) { currentAccent.primary.copy(alpha = opacity) }
    Box(modifier = Modifier.fillMaxWidth().height(60.dp).clip(RoundedCornerShape(12.dp))
        .background(
            when (gradientType) {
                "Linear" -> Brush.linearGradient(gradColors, start = Offset.Zero, end = Offset(1f, 1f))
                "Radial" -> Brush.radialGradient(gradColors)
                "Angular" -> Brush.sweepGradient(gradColors)
                else -> Brush.horizontalGradient(gradColors)
            }
        ).border(1.dp, surfaceColors.border.copy(alpha = 0.2f), RoundedCornerShape(12.dp)))
    Spacer(Modifier.height(12.dp))

    Button(onClick = {
        val gStart = gradColors.firstOrNull() ?: currentAccent.primary
        val gEnd = gradColors.lastOrNull() ?: currentAccent.secondary
        themeState.accentScheme = themeState.accentScheme.copy(
            gradientStart = gStart, gradientEnd = gEnd)
    }, colors = ButtonDefaults.buttonColors(containerColor = currentAccent.primary),
        modifier = Modifier.fillMaxWidth()) { Text("Apply Gradient to Theme") }
}

// ============================================
// TAB 5: Motion Studio
// ============================================

@Composable
private fun MotionTab() {
    val themeState = LocalKaiteyoThemeState.current
    val surfaceColors = LocalSurfaceColors.current
    val currentAccent = LocalKaiteyoAccent.current

    Text("Motion Studio", style = MaterialTheme.typography.titleMedium,
        color = surfaceColors.textPrimary, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(4.dp))
    Text("Control the feel of every interaction", style = MaterialTheme.typography.bodySmall, color = surfaceColors.textMuted)
    Spacer(Modifier.height(12.dp))

    Text("Animation Preset", style = MaterialTheme.typography.bodyMedium, color = surfaceColors.textSecondary, fontWeight = FontWeight.Medium)
    Spacer(Modifier.height(6.dp))
    val presets = listOf("None" to AnimationSpeed.Instant, "Minimal" to AnimationSpeed.Fast,
        "Balanced" to AnimationSpeed.Normal, "Smooth" to AnimationSpeed.Slow, "Cinematic" to AnimationSpeed.Slow)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        presets.forEach { (name, speed) ->
            val isSelected = themeState.animationConfig.speed == speed
            Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                .background(if (isSelected) currentAccent.primary.copy(alpha = 0.15f) else surfaceColors.surface)
                .border(1.dp, if (isSelected) currentAccent.primary else surfaceColors.border.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                .clickable { themeState.animationConfig = themeState.animationConfig.copy(speed = speed) }
                .padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                Text(name, color = if (isSelected) currentAccent.primary else surfaceColors.textSecondary,
                    fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    textAlign = TextAlign.Center)
            }
        }
    }
    Spacer(Modifier.height(16.dp))

    Text("Spring Physics", style = MaterialTheme.typography.bodyMedium, color = surfaceColors.textSecondary, fontWeight = FontWeight.Medium)
    Spacer(Modifier.height(6.dp))
    SliderWithLabel("Damping", themeState.animationConfig.springDamping, 0.1f..2f, "") { v ->
        themeState.animationConfig = themeState.animationConfig.copy(springDamping = v)
    }
    SliderWithLabel("Stiffness", themeState.animationConfig.springStiffness, 100f..1000f, "") { v ->
        themeState.animationConfig = themeState.animationConfig.copy(springStiffness = v)
    }
    Spacer(Modifier.height(12.dp))

    Text("Page Transition", style = MaterialTheme.typography.bodyMedium, color = surfaceColors.textSecondary, fontWeight = FontWeight.Medium)
    Spacer(Modifier.height(6.dp))
    PageTransitionType.entries.forEach { transition ->
        val isSelected = themeState.animationConfig.pageTransition == transition
        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) currentAccent.primary.copy(alpha = 0.1f) else Color.Transparent)
            .clickable { themeState.animationConfig = themeState.animationConfig.copy(pageTransition = transition) }
            .padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(16.dp).clip(CircleShape)
                .background(if (isSelected) currentAccent.primary else surfaceColors.border))
            Spacer(Modifier.width(10.dp))
            Text(transition.displayName, color = if (isSelected) currentAccent.primary else surfaceColors.textPrimary, fontSize = 13.sp)
        }
    }
    Spacer(Modifier.height(12.dp))

    SliderWithLabel("Duration (ms)", themeState.animationConfig.defaultDuration.toFloat(), 50f..800f, "ms") { v ->
        themeState.animationConfig = themeState.animationConfig.copy(defaultDuration = v.toInt())
    }
    Spacer(Modifier.height(12.dp))

    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
        .clickable { themeState.animationConfig = themeState.animationConfig.copy(reducedMotion = !themeState.animationConfig.reducedMotion) }
        .padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(20.dp).clip(RoundedCornerShape(4.dp))
            .background(if (themeState.animationConfig.reducedMotion) currentAccent.primary else surfaceColors.border),
            contentAlignment = Alignment.Center) {
            if (themeState.animationConfig.reducedMotion) Text("\u2713", color = currentAccent.onPrimary, fontSize = 10.sp)
        }
        Spacer(Modifier.width(10.dp))
        Text("Reduced Motion", color = surfaceColors.textPrimary, fontSize = 13.sp)
        Spacer(Modifier.weight(1f))
        if (themeState.animationConfig.reducedMotion) {
            Text("On", color = currentAccent.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ============================================
// TAB 6: Layout Studio
// ============================================

@Composable
private fun LayoutTab() {
    val themeState = LocalKaiteyoThemeState.current
    val surfaceColors = LocalSurfaceColors.current
    val currentAccent = LocalKaiteyoAccent.current
    var navSettingsOpen by remember { mutableStateOf(false) }

    Text("Layout Studio", style = MaterialTheme.typography.titleMedium,
        color = surfaceColors.textPrimary, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(4.dp))
    Text("Customize the spatial experience", style = MaterialTheme.typography.bodySmall, color = surfaceColors.textMuted)
    Spacer(Modifier.height(12.dp))

    Text("UI Density", style = MaterialTheme.typography.bodyMedium, color = surfaceColors.textSecondary, fontWeight = FontWeight.Medium)
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        UIDensity.entries.forEach { density ->
            val isSelected = themeState.layoutConfig.density == density
            Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                .background(if (isSelected) currentAccent.primary.copy(alpha = 0.15f) else surfaceColors.surface)
                .border(1.dp, if (isSelected) currentAccent.primary else surfaceColors.border.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                .clickable { themeState.layoutConfig = themeState.layoutConfig.copy(density = density) }
                .padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                Text(density.displayName, color = if (isSelected) currentAccent.primary else surfaceColors.textSecondary,
                    fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
            }
        }
    }
    Spacer(Modifier.height(16.dp))

    Text("Corner Radius", style = MaterialTheme.typography.bodyMedium, color = surfaceColors.textSecondary, fontWeight = FontWeight.Medium)
    Spacer(Modifier.height(6.dp))
    CornerRadiusSelector(themeState.radiusConfig) { themeState.radiusConfig = it }
    Spacer(Modifier.height(12.dp))
    SliderWithLabel("Custom Radius", themeState.radiusConfig.customRadius ?: 12f, 0f..48f, "dp") { v ->
        themeState.radiusConfig = themeState.radiusConfig.copy(customRadius = v)
    }
    Spacer(Modifier.height(16.dp))

    Text("Navigation", style = MaterialTheme.typography.bodyMedium, color = surfaceColors.textSecondary, fontWeight = FontWeight.Medium)
    Spacer(Modifier.height(6.dp))
    Text("Mode, placement, floating launcher and phone layout are configured in the adaptive navigation settings.",
        color = surfaceColors.textMuted, fontSize = 12.sp)
    Spacer(Modifier.height(8.dp))
    val navSettings = LocalNavigationSettings.current
    if (navSettings != null) {
        Button(
            onClick = { navSettingsOpen = true },
            colors = ButtonDefaults.buttonColors(
                containerColor = currentAccent.primary,
                contentColor = currentAccent.onPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(resolveString { nav.openNavigationSettingsLabel }, fontSize = 11.sp)
        }
    }
    Spacer(Modifier.height(16.dp))

    Text("Glow Effects", style = MaterialTheme.typography.bodyMedium, color = surfaceColors.textSecondary, fontWeight = FontWeight.Medium)
    Spacer(Modifier.height(6.dp))
    SliderWithLabel("Intensity", themeState.glowConfig.intensity, 0f..2f, "x") { v ->
        themeState.glowConfig = themeState.glowConfig.copy(intensity = v)
    }
    SliderWithLabel("Radius", themeState.glowConfig.radius, 0f..2f, "x") { v ->
        themeState.glowConfig = themeState.glowConfig.copy(radius = v)
    }
    SliderWithLabel("Opacity", themeState.glowConfig.opacity, 0f..1f, "%") { v ->
        themeState.glowConfig = themeState.glowConfig.copy(opacity = v)
    }
    Spacer(Modifier.height(12.dp))

    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
        .clickable { themeState.layoutConfig = themeState.layoutConfig.copy(transparencyEnabled = !themeState.layoutConfig.transparencyEnabled) }
        .padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(20.dp).clip(RoundedCornerShape(4.dp))
            .background(if (themeState.layoutConfig.transparencyEnabled) currentAccent.primary else surfaceColors.border),
            contentAlignment = Alignment.Center) {
            if (themeState.layoutConfig.transparencyEnabled) Text("\u2713", color = currentAccent.onPrimary, fontSize = 10.sp)
        }
        Spacer(Modifier.width(10.dp))
        Text("Enable Transparency", color = surfaceColors.textPrimary, fontSize = 13.sp)
    }
    if (themeState.layoutConfig.transparencyEnabled) {
        Spacer(Modifier.height(4.dp))
        SliderWithLabel("Glass Opacity", themeState.layoutConfig.glassOpacity, 0.1f..1f, "%") { v ->
            themeState.layoutConfig = themeState.layoutConfig.copy(glassOpacity = v)
        }
    }
    Spacer(Modifier.height(12.dp))

    HorizontalDivider(color = surfaceColors.border.copy(alpha = 0.3f))
    Spacer(Modifier.height(12.dp))
    Text("Theme Management", style = MaterialTheme.typography.bodyMedium, color = surfaceColors.textSecondary, fontWeight = FontWeight.Medium)
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("Export", "Import", "Reset").forEach { action ->
            Button(onClick = { /* TODO: file I/O integration */ },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (action == "Reset") MaterialTheme.colorScheme.error else currentAccent.primary),
                modifier = Modifier.weight(1f)) { Text(action, fontSize = 11.sp) }
        }
    }

    if (navSettingsOpen) {
        val navSettingsState = LocalNavigationSettings.current
        if (navSettingsState != null) {
            NavigationSettingsOverlay(
                navSettings = navSettingsState,
                formFactor = rememberFormFactor(),
                onDismiss = { navSettingsOpen = false }
            )
        }
    }
}

// ============================================
// CORNER RADIUS SELECTOR
// ============================================

@Composable
private fun CornerRadiusSelector(current: RadiusConfig, onSelect: (RadiusConfig) -> Unit) {
    val surfaceColors = LocalSurfaceColors.current
    val currentAccent = LocalKaiteyoAccent.current
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        CornerRadiusStyle.entries.forEach { style ->
            val isSelected = current.style == style
            Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                .background(if (isSelected) currentAccent.primary.copy(alpha = 0.15f) else surfaceColors.surface)
                .border(1.dp, if (isSelected) currentAccent.primary else surfaceColors.border.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                .clickable { onSelect(current.copy(style = style)) }.padding(vertical = 8.dp),
                contentAlignment = Alignment.Center) {
                Text(style.displayName, color = if (isSelected) currentAccent.primary else surfaceColors.textSecondary,
                    fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
            }
        }
    }
}

// ============================================
// LIVE PREVIEW PANEL
// Full UI preview with sidebar, stats, cards, buttons, progress
// ============================================

@Composable
private fun LivePreviewPanel() {
    val themeState = LocalKaiteyoThemeState.current
    val surfaceColors = LocalSurfaceColors.current
    val currentAccent = LocalKaiteyoAccent.current
    val previewSurface = surfaceForBaseMode(themeState.baseMode)

    Column(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)).background(previewSurface.background).padding(16.dp)) {
        Text("Live Preview", style = MaterialTheme.typography.titleMedium, color = previewSurface.textPrimary, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        Text("Changes apply in real-time", style = MaterialTheme.typography.bodySmall, color = previewSurface.textMuted)
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)).background(previewSurface.surface).padding(8.dp)) {
            // Mini sidebar
            Column(modifier = Modifier.width(80.dp).fillMaxHeight().clip(RoundedCornerShape(8.dp))
                .background(previewSurface.surfaceElevated).padding(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.fillMaxWidth().height(20.dp).clip(RoundedCornerShape(4.dp))
                    .background(currentAccent.primary.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                    Text("K", color = currentAccent.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(4.dp))
                listOf("\u25C6", "\u25C7", "\u25C7", "\u25C7").forEachIndexed { i, icon ->
                    val isActive = i == 0
                    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp))
                        .background(if (isActive) currentAccent.primary.copy(alpha = 0.1f) else Color.Transparent)
                        .padding(horizontal = 4.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(icon, color = if (isActive) currentAccent.primary else previewSurface.textMuted, fontSize = 8.sp)
                        Spacer(Modifier.width(4.dp))
                        Text(if (i == 0) "Home" else "Item", color = if (isActive) currentAccent.primary else previewSurface.textMuted, fontSize = 7.sp)
                    }
                }
                Spacer(Modifier.weight(1f))
                Box(modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(1.5.dp)).background(previewSurface.border)) {
                    Box(modifier = Modifier.fillMaxWidth(0.4f).height(3.dp).clip(RoundedCornerShape(1.5.dp)).background(currentAccent.primary))
                }
            }
            Spacer(Modifier.width(8.dp))
            // Content area
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Dashboard", color = previewSurface.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Study overview", color = previewSurface.textMuted, fontSize = 8.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Box(modifier = Modifier.size(16.dp).clip(RoundedCornerShape(3.dp)).background(previewSurface.surfaceElevated),
                            contentAlignment = Alignment.Center) { Text("\u2699", fontSize = 7.sp, color = previewSurface.textMuted) }
                        Box(modifier = Modifier.size(16.dp).clip(RoundedCornerShape(3.dp)).background(previewSurface.surfaceElevated),
                            contentAlignment = Alignment.Center) { Text("\u2605", fontSize = 7.sp, color = previewSurface.textMuted) }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("23", "156", "89").forEachIndexed { i, value ->
                        Column(modifier = Modifier.weight(1f).clip(RoundedCornerShape(6.dp)).background(previewSurface.surfaceElevated).padding(6.dp)) {
                            Text(value, color = currentAccent.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(listOf("Learning", "Review", "Mastered")[i], color = previewSurface.textMuted, fontSize = 7.sp)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Box(modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(6.dp)).background(previewSurface.surfaceElevated).padding(8.dp)) {
                    Column {
                        Text("Continue Learning", color = previewSurface.textPrimary, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        listOf(0.45f, 0.25f, 0.15f).forEach { progress ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(listOf("N5", "N4", "Vocab")[listOf(0.45f, 0.25f, 0.15f).indexOf(progress)],
                                    color = previewSurface.textMuted, fontSize = 7.sp, modifier = Modifier.width(28.dp))
                                Box(modifier = Modifier.weight(1f).height(3.dp).clip(RoundedCornerShape(1.5.dp)).background(previewSurface.border)) {
                                    Box(modifier = Modifier.fillMaxWidth(progress).height(3.dp).clip(RoundedCornerShape(1.5.dp)).background(currentAccent.primary))
                                }
                                Spacer(Modifier.width(4.dp))
                                Text("${(progress * 100).toInt()}%", color = previewSurface.textMuted, fontSize = 7.sp)
                            }
                            Spacer(Modifier.height(2.dp))
                        }
                        Spacer(Modifier.weight(1f))
                        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(5.dp)).background(currentAccent.primary).padding(vertical = 5.dp),
                            contentAlignment = Alignment.Center) {
                            Text("Start Review", color = currentAccent.onPrimary, fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

// ============================================
// REUSABLE COMPONENTS
// ============================================

@Composable
private fun SliderWithLabel(
    label: String, value: Float, range: ClosedFloatingPointRange<Float>,
    suffix: String, onValueChange: (Float) -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    Text("$label: ${formatFloat(value, 1)}$suffix", color = surfaceColors.textPrimary, fontSize = 13.sp)
    Slider(value = value, onValueChange = onValueChange, valueRange = range, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun ColorSlider(label: String, value: Float, trackColor: Color, onValueChange: (Float) -> Unit) {
    val surfaceColors = LocalSurfaceColors.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, color = surfaceColors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.width(20.dp))
        Slider(value = value.coerceIn(0f, 1f), onValueChange = onValueChange,
            valueRange = 0f..1f, modifier = Modifier.weight(1f))
        Text("${(value * 255).toInt()}", color = surfaceColors.textMuted, fontSize = 11.sp,
            modifier = Modifier.width(30.dp), textAlign = TextAlign.End)
    }
}

// ============================================
// UTILITY FUNCTIONS — KMP-safe (no String.format!)
// ============================================

private fun colorToHex(color: Color): String {
    val r = (color.red * 255).toInt(); val g = (color.green * 255).toInt(); val b = (color.blue * 255).toInt()
    return "#${r.toString(16).padStart(2, '0').uppercase()}${g.toString(16).padStart(2, '0').uppercase()}${b.toString(16).padStart(2, '0').uppercase()}"
}

private fun Color.toArgb(): Int {
    val r = (red * 255).toInt().coerceIn(0, 255)
    val g = (green * 255).toInt().coerceIn(0, 255)
    val b = (blue * 255).toInt().coerceIn(0, 255)
    return (255 shl 24) or (r shl 16) or (g shl 8) or b
}

private val Color.hue: Float get() {
    val r = red; val g = green; val b = blue
    val max = maxOf(r, g, b); val min = minOf(r, g, b)
    val delta = max - min
    if (delta == 0f) return 0f
    var hue = when (max) {
        r -> ((g - b) / delta) % 6f
        g -> ((b - r) / delta) + 2f
        else -> ((r - g) / delta) + 4f
    }
    return ((hue * 60f) % 360f + 360f) % 360f
}

private val Color.saturation: Float get() {
    val max = maxOf(red, green, blue); val min = minOf(red, green, blue)
    val l = (max + min) / 2f
    if (max == min) return 0f
    return if (l <= 0.5f) (max - min) / (max + min) else (max - min) / (2f - max - min)
}

private val Color.lightness: Float get() = (maxOf(red, green, blue) + minOf(red, green, blue)) / 2f

private fun formatFloat(value: Float, decimals: Int): String {
    val factor = when (decimals) { 0 -> 1; 1 -> 10; 2 -> 100; else -> 1000 }
    val rounded = (value * factor).roundToInt()
    val intPart = rounded / factor
    val decPart = (rounded % factor).let { if (it < 0) -it else it }
    return if (decimals > 0) "$intPart.${decPart.toString().padStart(decimals, '0')}" else "$intPart"
}