package ua.syt0r.kanji.presentation.common.ui.kaiteyo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.syt0r.kanji.core.knowledge.ContentConfidence
import ua.syt0r.kanji.core.knowledge.ContentSourceType
import ua.syt0r.kanji.core.knowledge.KanjiMnemonic
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors

// ============================================================
// KAITEYO MNEMONIC CARD — Connected Memory System
//
// Features:
//   · View curated / authoritative / user-created mnemonics
//   · Inline editing of user mnemonic notes
//   · Component keyword tags
//   · Source badge (Curated, User, Community, AI)
//   · Full theme support (OLED, Sepia, Light, Dark)
// ============================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KaiteyoMnemonicCard(
    character: String,
    mnemonics: List<KanjiMnemonic>,
    userNote: String? = null,
    onSaveUserMnemonic: ((String) -> Unit)? = null,
    onComponentClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current

    var isEditing by remember { mutableStateOf(false) }
    var editedText by remember(userNote) { mutableStateOf(userNote.orEmpty()) }

    val activeMnemonic = mnemonics.firstOrNull { it.isActive } ?: mnemonics.firstOrNull()

    KaiteyoCard(
        modifier = modifier,
        header = "Mnemonic & Recall",
        subtitle = "Story memory aids and keyword associations"
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Main Mnemonic Display or Editor
            if (isEditing) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(KaiteyoCardShape)
                        .background(surfaceColors.surfaceInteractive.copy(alpha = 0.5f))
                        .border(1.dp, accent.primary.copy(alpha = 0.5f), KaiteyoCardShape)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Edit personal mnemonic note for $character:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = surfaceColors.textMuted
                    )

                    BasicTextField(
                        value = editedText,
                        onValueChange = { editedText = it },
                        textStyle = TextStyle(
                            fontSize = 13.sp,
                            color = surfaceColors.textPrimary,
                            lineHeight = 18.sp
                        ),
                        cursorBrush = SolidColor(accent.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { isEditing = false },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Cancel",
                                tint = surfaceColors.textMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        IconButton(
                            onClick = {
                                onSaveUserMnemonic?.invoke(editedText)
                                isEditing = false
                            },
                            modifier = Modifier
                                .size(30.dp)
                                .background(accent.primary, CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Save",
                                tint = surfaceColors.textInverse,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            } else {
                // Read Mode
                val displayText = when {
                    !userNote.isNullOrBlank() -> userNote
                    activeMnemonic != null -> activeMnemonic.text
                    else -> buildString {
                        append("Picture the components of $character working together. ")
                        append("Each part contributes to the meaning — find the connection that sticks.")
                    }
                }

                val sourceBadgeText = when {
                    !userNote.isNullOrBlank() -> "✎ Your Note"
                    activeMnemonic?.source == ContentSourceType.Authoritative -> "★ Authoritative"
                    activeMnemonic?.source == ContentSourceType.UserGenerated -> "↻ Community"
                    activeMnemonic?.source == ContentSourceType.AiGenerated -> "◉ AI Generated"
                    else -> "💡 Memory Aid"
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(KaiteyoCardShape)
                        .background(surfaceColors.surfaceInteractive.copy(alpha = 0.35f))
                        .border(1.dp, surfaceColors.surfaceInteractive.copy(alpha = 0.6f), KaiteyoCardShape)
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = accent.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            KaiteyoBadge(
                                text = sourceBadgeText,
                                containerColor = accent.primary.copy(alpha = 0.12f),
                                contentColor = accent.primary
                            )
                            Spacer(Modifier.weight(1f))
                            if (onSaveUserMnemonic != null) {
                                IconButton(
                                    onClick = {
                                        editedText = userNote ?: activeMnemonic?.text.orEmpty()
                                        isEditing = true
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit Mnemonic",
                                        tint = surfaceColors.textMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = displayText,
                            fontSize = 13.sp,
                            color = surfaceColors.textPrimary,
                            lineHeight = 19.sp
                        )
                    }
                }
            }

            // Keyword and component associative tags
            if (activeMnemonic != null && activeMnemonic.components.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Related Parts in Story",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = surfaceColors.textMuted
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        activeMnemonic.components.forEach { comp ->
                            Box(
                                modifier = Modifier
                                    .clip(KaiteyoPillShape)
                                    .background(accent.secondary.copy(alpha = 0.12f))
                                    .border(1.dp, accent.secondary.copy(alpha = 0.3f), KaiteyoPillShape)
                                    .clickable { onComponentClick?.invoke(comp) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = comp,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = surfaceColors.textPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
