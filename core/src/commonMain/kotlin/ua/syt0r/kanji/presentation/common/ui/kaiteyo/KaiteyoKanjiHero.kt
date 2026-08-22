package ua.syt0r.kanji.presentation.common.ui.kaiteyo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.ui.kanji.KanjiBackground
import ua.syt0r.kanji.presentation.screen.main.screen.info.LetterInfoData

/**
 * Clean kanji hero — large character display with badge, JLPT tag, meanings, and copy.
 * No stroke animation (kept separate in AnimatedKanji if needed).
 */
@Composable
fun KaiteyoKanjiHero(
    data: LetterInfoData.Kanji,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current

    KaiteyoCard(modifier = modifier, contentPadding = PaddingValues(16.dp, 14.dp, 16.dp, 16.dp)) {
        // Badges row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            KaiteyoBadge(
                text = "漢字",
                containerColor = accent.primary.copy(alpha = 0.14f),
                contentColor = accent.primary
            )
            data.jlptLevel?.let { jlpt ->
                KaiteyoBadge(
                    text = "JLPT N$jlpt",
                    containerColor = accent.secondary.copy(alpha = 0.14f),
                    contentColor = accent.secondary
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Large kanji character
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.4f)
                .clip(RoundedCornerShape(12.dp))
                .background(surfaceColors.surfaceInteractive.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            KanjiBackground(Modifier.fillMaxSize())
            Text(
                text = data.character,
                fontSize = 80.sp,
                fontWeight = FontWeight.Light,
                color = accent.primary,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(12.dp))

        // Meanings
        Text(
            text = data.meanings.joinToString(", "),
            fontSize = 14.sp,
            color = surfaceColors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(10.dp))

        // Copy button
        KaiteyoActionButton(
            label = "📋 Copy",
            onClick = onCopy,
            container = accent.primary.copy(alpha = 0.14f),
            content = accent.primary,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
