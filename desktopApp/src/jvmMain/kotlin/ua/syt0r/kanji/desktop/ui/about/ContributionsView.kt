package ua.syt0r.kanji.desktop.ui.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.designsystem.DsBadge
import ua.syt0r.kanji.desktop.designsystem.DsButton
import ua.syt0r.kanji.desktop.designsystem.DsButtonKind
import ua.syt0r.kanji.desktop.designsystem.DsCard
import ua.syt0r.kanji.desktop.designsystem.DsRadius
import ua.syt0r.kanji.desktop.designsystem.DsSectionHeader
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.accent
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import ua.syt0r.kanji.desktop.engine.history.ActivityCategory
import ua.syt0r.kanji.desktop.model.ToastKind

// ============================================
// CONTRIBUTIONS
// Credits, license, and ways to support the
// project — framed as "Contributions" rather
// than "Donations" to emphasize community
// participation over transactions.
// ============================================

@Composable
fun ContributionsView(state: AppState) {
    val sc = surfaceColors()

    Column(
        Modifier.fillMaxSize().padding(DsSpacing.Lg).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(DsSpacing.Lg)
    ) {
        // Hero
        DsCard {
            Column(
                Modifier.padding(DsSpacing.Xl).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(DsRadius.Xl))
                        .background(accent().primary)
                ) {
                    // Placeholder logo - just a "K" for now
                }
                Text("Kaiteyo", color = sc.textPrimary, fontSize = DsType.Display, fontWeight = FontWeight.Bold)
                Text("Kanji study, rethought.", color = sc.textMuted, fontSize = DsType.BodyLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                    DsBadge(text = "v1.0.0", tint = sc.textMuted)
                    DsBadge(text = "Kotlin + Compose", tint = Color(0xFF7BC8FF))
                    DsBadge(text = "Open Source", tint = Color(0xFFC2FC8B))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                    DsButton(
                        text = "GitHub Repository",
                        icon = Icons.Default.OpenInNew,
                        kind = DsButtonKind.Ghost,
                        onClick = { /* open browser */ }
                    )
                    DsButton(
                        text = "Report an Issue",
                        icon = Icons.Default.Info,
                        kind = DsButtonKind.Ghost,
                        onClick = { /* open browser */ }
                    )
                }
            }
        }

        // Credits
        DsSectionHeader(
            title = "Credits & Acknowledgments",
            subtitle = "Built on the shoulders of open-source giants."
        ) {
            DsCard {
                Column(Modifier.padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                    creditRow("Kotlin", "The language that makes this possible", Icons.Default.Code)
                    creditRow("JetBrains Compose Multiplatform", "UI framework for desktop and beyond", Icons.Default.Palette)
                    creditRow("kotlinx.serialization", "Type-safe JSON & data persistence", Icons.Default.Code)
                    creditRow("kotlinx.datetime", "Modern date/time handling", Icons.Default.Info)
                    creditRow("kotlinx.coroutines", "Structured concurrency", Icons.Default.Code)
                    creditRow("SQLDelight", "Type-safe SQL for Kotlin", Icons.Default.Code)
                    creditRow("sqlite-jdbc", "Embedded database driver", Icons.Default.Code)
                    creditRow("KanjiVG / KanjiDic2", "Kanji stroke & dictionary data (CC-BY-SA)", Icons.Default.Info)
                    creditRow("WaniKani API", "SRS algorithm inspiration", Icons.Default.Star)
                    creditRow("Anki", "Scheduling model reference", Icons.Default.Favorite)
                }
            }
        }

        // License
        DsSectionHeader(
            title = "License",
            subtitle = "Kaiteyo is free and open-source software."
        ) {
            DsCard {
                Column(Modifier.padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                    Text("MIT License", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = """Copyright (c) 2024-2026 syt0r

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.""",
                        color = sc.textSecondary,
                        fontSize = DsType.Body
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                        DsButton(
                            text = "View on GitHub",
                            icon = Icons.Default.OpenInNew,
                            kind = DsButtonKind.Ghost,
                            compact = true,
                            onClick = { /* open LICENSE file */ }
                        )
                    }
                }
            }
        }

        // Contribute
        DsSectionHeader(
            title = "Contribute",
            subtitle = "Kaiteyo thrives on community contributions — code, translations, ideas, feedback."
        ) {
            DsCard {
                Column(Modifier.padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                    contributeRow(
                        icon = Icons.Default.Code,
                        title = "Code Contributions",
                        desc = "Fix bugs, add features, improve performance. See CONTRIBUTING.md on GitHub.",
                        actionText = "Open Issues",
                        onAction = { /* open issues */ }
                    )
                    contributeRow(
                        icon = Icons.Default.Link,
                        title = "Translations & Localization",
                        desc = "Help bring Kaiteyo to Japanese, Chinese, Korean, and other languages.",
                        actionText = "Join Localization",
                        onAction = { /* open localization */ }
                    )
                    contributeRow(
                        icon = Icons.Default.Star,
                        title = "Spread the Word",
                        desc = "Star the repo, share with fellow learners, write a blog post or review.",
                        actionText = "Star on GitHub",
                        onAction = { /* star repo */ }
                    )
                    contributeRow(
                        icon = Icons.Default.Favorite,
                        title = "Financial Support",
                        desc = "If Kaiteyo helps your studies, consider sponsoring development. No obligations.",
                        actionText = "Sponsor",
                        onAction = { /* sponsor link */ }
                    )
                }
            }
        }

        // Team
        DsSectionHeader(
            title = "Core Team",
            subtitle = "A small group of kanji enthusiasts."
        ) {
            DsCard {
                Column(Modifier.padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                    teamMember("syt0r", "Founder & Lead Developer", "Architecture, core engine, desktop UI")
                    teamMember("Community", "Contributors & Testers", "Bug reports, feature ideas, translations")
                }
            }
        }

        // Footer
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(DsSpacing.Xs)
        ) {
            Text("Made with ❤ for kanji learners everywhere", color = sc.textMuted, fontSize = DsType.Caption)
            Text("Kaiteyo is not affiliated with WaniKani, Anki, or any other platform.", color = sc.textMuted, fontSize = DsType.Caption)
        }
    }
}

@Composable
private fun creditRow(name: String, desc: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    val sc = surfaceColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = DsSpacing.Xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)
    ) {
        androidx.compose.material3.Icon(icon, contentDescription = null, tint = sc.textMuted, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f)) {
            Text(name, color = sc.textPrimary, fontSize = DsType.Body, fontWeight = FontWeight.Medium)
            Text(desc, color = sc.textMuted, fontSize = DsType.Caption)
        }
    }
}

@Composable
private fun contributeRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String,
    actionText: String,
    onAction: () -> Unit
) {
    val sc = surfaceColors()
    val ac = ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = DsSpacing.Xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)
    ) {
        androidx.compose.material3.Icon(icon, contentDescription = null, tint = ac.primary, modifier = Modifier.size(24.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = sc.textPrimary, fontSize = DsType.Body, fontWeight = FontWeight.Medium)
            Text(desc, color = sc.textMuted, fontSize = DsType.Caption)
        }
        DsButton(
            text = actionText,
            kind = DsButtonKind.Ghost,
            compact = true,
            onClick = onAction
        )
    }
}

@Composable
private fun teamMember(name: String, role: String, details: String) {
    val sc = surfaceColors()
    val ac = accent()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = DsSpacing.Xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(DsRadius.Full)).background(ac.primary.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Text(name.take(1).uppercase(), color = ac.primary, fontSize = DsType.Body, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.weight(1f)) {
            Text(name, color = sc.textPrimary, fontSize = DsType.Body, fontWeight = FontWeight.SemiBold)
            Text(role, color = sc.textSecondary, fontSize = DsType.Caption)
            Text(details, color = sc.textMuted, fontSize = DsType.Caption)
        }
    }
}