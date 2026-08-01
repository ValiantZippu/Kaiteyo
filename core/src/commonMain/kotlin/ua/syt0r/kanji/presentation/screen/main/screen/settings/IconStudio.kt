package ua.syt0r.kanji.presentation.screen.main.screen.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoThemeState
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.theme.surfaceForBaseMode

// ============================================
// KAITEYO ICON STUDIO v1.2
// Change application icons with built-in or custom
// Supports: Default, Signature, Cotton Candy, Ocean,
// Forest, Lavender, Monochrome, Minimal, Outlined
// Import: PNG, SVG, ICO, ICNS
// ============================================

data class AppIconOption(
    val name: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val isBuiltIn: Boolean = true
)

private val builtInIcons = listOf(
    AppIconOption("Default", Color(0xFFC2FC8B), Color(0xFFFEAB57)),
    AppIconOption("Signature", Color(0xFFC2FC8B), Color(0xFF050505)),
    AppIconOption("Cotton Candy", Color(0xFFD4A5F0), Color(0xFFFFB5C5)),
    AppIconOption("Ocean", Color(0xFF00D4AA), Color(0xFF00A8FF)),
    AppIconOption("Forest", Color(0xFF81C784), Color(0xFFA5D6A7)),
    AppIconOption("Lavender", Color(0xFFB39DDB), Color(0xFFCE93D8)),
    AppIconOption("Monochrome", Color(0xFFE0E0E0), Color(0xFF9E9E9E)),
    AppIconOption("Minimal", Color(0xFFC2FC8B), Color(0xFF000000)),
    AppIconOption("Outlined", Color(0x00000000), Color(0xFFC2FC8B)),
)

@Composable
fun IconStudio() {
    val themeState = LocalKaiteyoThemeState.current
    val surfaceColors = LocalSurfaceColors.current
    val currentAccent = LocalKaiteyoAccent.current
    var selectedIcon by remember { mutableStateOf(builtInIcons.first()) }
    var showImportDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("Icon Studio", style = MaterialTheme.typography.titleLarge,
            color = surfaceColors.textPrimary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Customize your application icon", style = MaterialTheme.typography.bodySmall,
            color = surfaceColors.textMuted)
        Spacer(modifier = Modifier.height(20.dp))

        // Current icon preview
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(if (selectedIcon.name == "Outlined") Color.Transparent else selectedIcon.primaryColor)
                .border(
                    2.dp,
                    if (selectedIcon.name == "Outlined") selectedIcon.secondaryColor else Color.Transparent,
                    RoundedCornerShape(22.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(selectedIcon.secondaryColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text("K", color = selectedIcon.secondaryColor, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(selectedIcon.name, color = surfaceColors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Text("120×120 preview", color = surfaceColors.textMuted, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(24.dp))

        // Built-in icons grid
        Text("Built-in Icons", style = MaterialTheme.typography.titleMedium,
            color = surfaceColors.textPrimary, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(12.dp))

        builtInIcons.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { icon ->
                    val isSelected = selectedIcon.name == icon.name
                    val cardBg by animateColorAsState(
                        targetValue = if (isSelected) currentAccent.primary.copy(alpha = 0.12f)
                            else surfaceColors.surface,
                        animationSpec = tween(200), label = "iconCardBg"
                    )
                    val cardBorder by animateColorAsState(
                        targetValue = if (isSelected) currentAccent.primary
                            else surfaceColors.border.copy(alpha = 0.2f),
                        animationSpec = tween(200), label = "iconCardBorder"
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(cardBg)
                            .border(1.5.dp, cardBorder, RoundedCornerShape(12.dp))
                            .clickable { selectedIcon = icon }
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (icon.name == "Outlined") Color.Transparent else icon.primaryColor)
                                .border(
                                    if (icon.name == "Outlined") 1.5.dp else 0.dp,
                                    icon.secondaryColor,
                                    RoundedCornerShape(10.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(icon.secondaryColor.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("K", color = icon.secondaryColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(icon.name, color = if (isSelected) currentAccent.primary else surfaceColors.textPrimary,
                            fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            textAlign = TextAlign.Center)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))
        Divider(color = surfaceColors.border.copy(alpha = 0.3f))
        Spacer(modifier = Modifier.height(16.dp))

        // Import section
        Text("Import Custom Icon", style = MaterialTheme.typography.titleMedium,
            color = surfaceColors.textPrimary, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Supported: PNG, SVG, ICO, ICNS", style = MaterialTheme.typography.bodySmall,
            color = surfaceColors.textMuted)
        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(surfaceColors.surfaceInteractive.copy(alpha = 0.5f))
                .border(1.dp, surfaceColors.border.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .clickable { showImportDialog = true },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(currentAccent.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("+", color = currentAccent.primary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Drop icon file or click to browse", color = surfaceColors.textMuted, fontSize = 13.sp)
                Text("PNG · SVG · ICO · ICNS", color = currentAccent.primary, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Icon sizes info
        Text("Icon Sizes", style = MaterialTheme.typography.titleMedium,
            color = surfaceColors.textPrimary, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        listOf(
            "Windows" to "32×32, 64×64, 256×256 (ICO)",
            "macOS" to "16×16, 32×32, 128×128, 256×256, 512×512 (ICNS)",
            "Linux" to "256×256 (PNG)",
            "Android" to "Adaptive icon (48×48 to 192×192)",
            "iOS" to "20×20 to 1024×1024"
        ).forEach { (platform, sizes) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(platform, color = surfaceColors.textSecondary, fontSize = 12.sp)
                Text(sizes, color = surfaceColors.textMuted, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Apply button
        Button(
            onClick = { /* Apply icon */ },
            colors = ButtonDefaults.buttonColors(
                containerColor = currentAccent.primary,
                contentColor = currentAccent.onPrimary
            ),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Apply Icon", fontWeight = FontWeight.SemiBold)
        }
    }
}