package ua.syt0r.kanji.desktop.ui.about

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Translate
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.designsystem.DsBadge
import ua.syt0r.kanji.desktop.designsystem.DsButton
import ua.syt0r.kanji.desktop.designsystem.DsButtonKind
import ua.syt0r.kanji.desktop.designsystem.DsCard
import ua.syt0r.kanji.desktop.designsystem.DsChip
import ua.syt0r.kanji.desktop.designsystem.DsRadius
import ua.syt0r.kanji.desktop.designsystem.DsSectionHeader
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.accent
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import ua.syt0r.kanji.presentation.common.resources.brand.BrandMark

// ============================================
// ABOUT
// Version, contributors, acknowledgements,
// licenses, third-party software, roadmap,
// GitHub, documentation and credits — in one
// modern, linkable overview.
// ============================================

private const val GITHUB_URL = "https://github.com/ValiantZippu/Kaiteyo"
private const val DOCS_URL = "https://github.com/ValiantZippu/Kaiteyo#readme"
private const val ISSUES_URL = "https://github.com/ValiantZippu/Kaiteyo/issues"

private fun openUrl(url: String) {
    runCatching { java.awt.Desktop.getDesktop().browse(java.net.URI(url)) }
}

@Composable
fun ContributionsView(state: AppState) {
    val sc = surfaceColors()
    var tab by remember { mutableStateOf(0) }
    val tabs = listOf("Overview", "Roadmap", "Credits & Licenses")

    Column(
        Modifier.fillMaxSize().padding(DsSpacing.Lg).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(DsSpacing.Lg)
    ) {
        HeroCard()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            ua.syt0r.kanji.desktop.designsystem.DsTabRow(
                tabs = tabs,
                selectedIndex = tab,
                onSelect = { tab = it },
                modifier = Modifier.width(460.dp)
            )
        }

        when (tab) {
            0 -> OverviewTab(state)
            1 -> RoadmapTab()
            else -> CreditsTab()
        }

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
private fun HeroCard() {
    val sc = surfaceColors()
    val ac = accent()

    DsCard(elevated = true) {
        Column(
            Modifier.padding(DsSpacing.Xl).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)
        ) {
            // The real Kaiteyo mark — centralized brand asset, not a "K".
            BrandMark(modifier = Modifier.size(80.dp))
            Text("Kaiteyo", color = sc.textPrimary, fontSize = DsType.Display, fontWeight = FontWeight.Bold)
            Text("Kanji study, rethought.", color = sc.textMuted, fontSize = DsType.BodyLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                DsBadge(text = "Version 1.0.0", tint = sc.textPrimary)
                DsBadge(text = "Kotlin + Compose Multiplatform", tint = Color(0xFF7BC8FF))
                DsBadge(text = "MIT License", tint = Color(0xFFC2FC8B))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                DsButton(
                    text = "GitHub",
                    icon = Icons.Default.OpenInNew,
                    kind = DsButtonKind.Ghost,
                    onClick = { openUrl(GITHUB_URL) }
                )
                DsButton(
                    text = "Documentation",
                    icon = Icons.Default.MenuBook,
                    kind = DsButtonKind.Ghost,
                    onClick = { openUrl(DOCS_URL) }
                )
                DsButton(
                    text = "Report an issue",
                    icon = Icons.Default.BugReport,
                    kind = DsButtonKind.Ghost,
                    onClick = { openUrl(ISSUES_URL) }
                )
            }
        }
    }
}

// ============================================
// OVERVIEW TAB
// ============================================

@Composable
private fun OverviewTab(state: AppState) {
    val sc = surfaceColors()

    Column(verticalArrangement = Arrangement.spacedBy(DsSpacing.Lg)) {
        // Resources
        Column(verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
            DsSectionHeader(
                title = "Resources",
                subtitle = "Everything you need to get the most out of Kaiteyo"
            )
            DsCard {
                Column(Modifier.padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                    resourceRow(Icons.Default.Link, "Source code", "Browse, fork and build Kaiteyo from source", GITHUB_URL)
                    resourceRow(Icons.Default.MenuBook, "Documentation", "Setup, workflows and the study system explained", DOCS_URL)
                    resourceRow(Icons.Default.BugReport, "Issue tracker", "Report bugs and request features", ISSUES_URL)
                    resourceRow(Icons.Default.Language, "Project website", "Learn more about the project", GITHUB_URL)
                }
            }
        }

        // Project stats
        Column(verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
            DsSectionHeader(title = "At a glance", subtitle = "Live data from this workspace")
            DsCard {
                Row(
                    Modifier.padding(DsSpacing.Xl).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)
                ) {
                    GlanceStat("Cards", state.cards.size.toString(), Modifier.weight(1f))
                    GlanceStat("Decks", state.library.decks.size.toString(), Modifier.weight(1f))
                    GlanceStat("Study time", state.formatDuration(state.totalStudyTime()), Modifier.weight(1f))
                    GlanceStat("Reviews", state.totalReviews().toString(), Modifier.weight(1f))
                    GlanceStat("Plugins", state.pluginRegistry.installed.size.toString(), Modifier.weight(1f))
                }
            }
        }

        // Contributing
        Column(verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
            DsSectionHeader(
                title = "Get involved",
                subtitle = "Kaiteyo thrives on community contributions"
            )
            DsCard {
                Column(Modifier.padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                    contributionRow(Icons.Default.Code, "Code contributions", "Fix bugs, add features, improve performance.", "Open issues", ISSUES_URL)
                    contributionRow(Icons.Default.Translate, "Translations", "Bring Kaiteyo to more languages.", "Translate", DOCS_URL)
                    contributionRow(Icons.Default.Star, "Spread the word", "Star the repo and share with fellow learners.", "Star on GitHub", GITHUB_URL)
                }
            }
        }
    }
}

@Composable
private fun resourceRow(icon: ImageVector, title: String, desc: String, url: String) {
    val sc = surfaceColors()
    val ac = accent()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DsRadius.Md))
            .background(sc.surfaceInteractive.copy(alpha = 0.4f))
            .clickable { openUrl(url) }
            .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)
    ) {
        Icon(icon, contentDescription = null, tint = ac.primary, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = sc.textPrimary, fontSize = DsType.Body, fontWeight = FontWeight.Medium)
            Text(desc, color = sc.textMuted, fontSize = DsType.Caption)
        }
        Icon(Icons.Default.OpenInNew, contentDescription = null, tint = sc.textMuted, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun contributionRow(icon: ImageVector, title: String, desc: String, action: String, url: String) {
    val sc = surfaceColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = DsSpacing.Xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)
    ) {
        Icon(icon, contentDescription = null, tint = accent().primary, modifier = Modifier.size(24.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = sc.textPrimary, fontSize = DsType.Body, fontWeight = FontWeight.Medium)
            Text(desc, color = sc.textMuted, fontSize = DsType.Caption)
        }
        DsButton(
            text = action,
            kind = DsButtonKind.Ghost,
            compact = true,
            onClick = { openUrl(url) }
        )
    }
}

@Composable
private fun GlanceStat(label: String, value: String, modifier: Modifier = Modifier) {
    val sc = surfaceColors()
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = sc.textPrimary, fontSize = DsType.Title, fontWeight = FontWeight.Bold)
        Text(label, color = sc.textMuted, fontSize = DsType.Caption)
    }
}

// ============================================
// ROADMAP TAB
// ============================================

private data class RoadmapItem(val version: String, val title: String, val detail: String, val status: String)

private val roadmap = listOf(
    RoadmapItem("1.0", "Production launch", "Polished desktop workspace with the full study ecosystem: library decks, per-mode SRS, mining, OCR, media and browser integration.", "Shipped"),
    RoadmapItem("1.1", "Sync providers", "Git, Google Drive, Dropbox and WebDAV transports on top of the existing sync engine.", "In progress"),
    RoadmapItem("1.2", "Mobile companion", "Phone-optimized tab bar and study sessions with cross-device progress sync.", "Planned"),
    RoadmapItem("1.3", "Community plugins", "Marketplace distribution for plugins, plus plugin-authored study modes.", "Planned"),
    RoadmapItem("2.0", "Spaced listening", "Audio-first review lanes with pitch accent drill-downs for vocabulary decks.", "Research")
)

@Composable
private fun RoadmapTab() {
    val sc = surfaceColors()
    Column(verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
        DsSectionHeader(
            title = "Roadmap",
            subtitle = "Where Kaiteyo is headed next"
        )
        roadmap.forEach { item ->
            DsCard {
                Row(
                    Modifier.padding(DsSpacing.Xl).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(DsRadius.Md))
                            .background(accent().primary.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.RocketLaunch,
                            contentDescription = null,
                            tint = accent().primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(DsSpacing.Md))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                            Text(
                                text = "${item.version} · ${item.title}",
                                color = sc.textPrimary,
                                fontSize = DsType.BodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            DsBadge(
                                text = item.status,
                                tint = when (item.status) {
                                    "Shipped" -> Color(0xFFC2FC8B)
                                    "In progress" -> Color(0xFFFFD93D)
                                    else -> sc.textMuted
                                }
                            )
                        }
                        Text(item.detail, color = sc.textMuted, fontSize = DsType.Body)
                    }
                }
            }
        }
    }
}

// ============================================
// CREDITS & LICENSES TAB
// ============================================

private data class CreditItem(val name: String, val role: String, val license: String)

private val thirdParty = listOf(
    CreditItem("Kotlin", "Language", "Apache-2.0"),
    CreditItem("Compose Multiplatform", "UI framework", "Apache-2.0"),
    CreditItem("Ktor", "HTTP server (local API)", "Apache-2.0"),
    CreditItem("kotlinx.coroutines", "Structured concurrency", "Apache-2.0"),
    CreditItem("kotlinx.serialization", "Type-safe persistence", "Apache-2.0"),
    CreditItem("kotlinx.datetime", "Date & time handling", "Apache-2.0"),
    CreditItem("SQLDelight", "Type-safe SQL", "Apache-2.0"),
    CreditItem("sqlite-jdbc", "Embedded database driver", "Apache-2.0"),
    CreditItem("Material Icons", "Iconography", "Apache-2.0")
)

private val dataSources = listOf(
    CreditItem("KanjiVG", "Stroke order data", "CC-BY-SA-3.0"),
    CreditItem("KanjiDic2", "Kanji dictionary data", "CC-BY-SA-3.0"),
    CreditItem("JMdict / JMnedict", "Vocabulary dictionary data", "CC-BY-SA-4.0"),
    CreditItem("KANJIDIC", "Readings and meanings", "CC-BY-SA-3.0")
)

@Composable
private fun CreditsTab() {
    val sc = surfaceColors()

    Column(verticalArrangement = Arrangement.spacedBy(DsSpacing.Lg)) {
        // Contributors
        Column(verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
            DsSectionHeader(title = "Contributors", subtitle = "The people behind Kaiteyo")
            DsCard {
                Column(Modifier.padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                    teamMember("syt0r", "Founder & Lead Developer", "Architecture, core engine, desktop UI")
                    teamMember("Community", "Contributors & Testers", "Bug reports, feature ideas, translations")
                }
            }
        }

        // Acknowledgements
        Column(verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
            DsSectionHeader(
                title = "Acknowledgements",
                subtitle = "Language data that powers the dictionaries and stroke data"
            )
            DsCard {
                Column(Modifier.padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                    dataSources.forEach { creditRow(it) }
                }
            }
        }

        // Third-party software
        Column(verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
            DsSectionHeader(
                title = "Third-party software",
                subtitle = "Open-source libraries Kaiteyo is built on"
            )
            DsCard {
                Column(Modifier.padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                    thirdParty.forEach { creditRow(it) }
                }
            }
        }

        // Licenses
        Column(verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
            DsSectionHeader(
                title = "Licenses",
                subtitle = "Kaiteyo is free and open-source software"
            )
            DsCard {
                Column(Modifier.padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                        Text("MIT License", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
                        DsChip(text = "View full license on GitHub", selected = false, onClick = { openUrl(GITHUB_URL) })
                    }
                    Text(
                        text = "Copyright (c) 2024-2026 syt0r. Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files, to deal in the Software without restriction. The Software is provided \"AS IS\", without warranty of any kind. See the full MIT license text on GitHub.",
                        color = sc.textSecondary,
                        fontSize = DsType.Body
                    )
                    Text(
                        text = "Dictionary data (KanjiVG, KanjiDic2, JMdict, KANJIDIC) is provided under Creative Commons licenses as noted above; all such content remains the property of its respective authors.",
                        color = sc.textMuted,
                        fontSize = DsType.Caption
                    )
                }
            }
        }
    }
}

@Composable
private fun creditRow(credit: CreditItem) {
    val sc = surfaceColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = DsSpacing.Xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)
    ) {
        Icon(Icons.Default.Favorite, contentDescription = null, tint = accent().primary.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
        Column(Modifier.weight(1f)) {
            Text(credit.name, color = sc.textPrimary, fontSize = DsType.Body, fontWeight = FontWeight.Medium)
            Text(credit.role, color = sc.textMuted, fontSize = DsType.Caption)
        }
        DsBadge(text = credit.license, tint = sc.textMuted)
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
