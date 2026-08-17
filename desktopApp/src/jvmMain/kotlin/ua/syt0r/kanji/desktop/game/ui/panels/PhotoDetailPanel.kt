package ua.syt0r.kanji.desktop.game.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ua.syt0r.kanji.desktop.designsystem.DsRadius
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.game.GameSession
import ua.syt0r.kanji.desktop.game.bridge.BridgePhoto
import ua.syt0r.kanji.desktop.game.bridge.BridgePhotoTag
import ua.syt0r.kanji.desktop.game.photography.Photo

/**
 * A photo in the album, opened: its vocabulary tags (each tag is a real
 * discovery), the region it was taken in, and actions — save the photo to
 * disk (JSON sidecar through the bridge) or delete it from the album.
 * Photography is a collection mechanic, not an inventory chore (spec §46).
 */
@Composable
fun PhotoDetailPanel(session: GameSession) {
    val photoId = session.state.photoDetail ?: return
    val photo = session.album.photos.firstOrNull { it.id == photoId } ?: return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .clip(RoundedCornerShape(DsRadius.Lg))
                .background(Color(0xFF1B2233).copy(alpha = 0.97f))
                .padding(DsSpacing.Lg),
            verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📷  ${photo.title}",
                    color = Color.White,
                    fontSize = DsType.Body,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = photo.category.label,
                    color = Color(0xFFFFD54F),
                    fontSize = DsType.Caption
                )
            }
            Text(
                text = "Taken in ${photo.regionId} · ${photo.takenAt.take(10)}",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = DsType.Caption
            )

            if (photo.tags.isEmpty()) {
                Text(
                    text = "No vocabulary tagged in this frame.",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = DsType.Caption
                )
            } else {
                Text(
                    text = "Vocabulary in this photo:",
                    color = Color(0xFF90CAF9),
                    fontSize = DsType.Label,
                    fontWeight = FontWeight.Medium
                )
                photo.tags.forEach { tag -> TagRow(tag.headword, tag.reading, tag.meaning) }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
            ) {
                ActionButton("Save to disk", Color(0xFF2E7D32)) { session.savePhotoToDisk(photo) }
                ActionButton("Delete", Color(0xFFB71C1C)) { session.deletePhoto(photo.id) }
                ActionButton("Close", Color.White.copy(alpha = 0.1f), Modifier.weight(1f)) {
                    session.state.photoDetail = null
                }
            }
        }
    }
}

@Composable
private fun TagRow(headword: String, reading: String, meaning: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DsRadius.Md))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = "$headword  $reading", color = Color.White, fontSize = DsType.Body)
        Text(text = meaning, color = Color.White.copy(alpha = 0.65f), fontSize = DsType.Caption)
    }
}

@Composable
private fun ActionButton(label: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Text(
        text = label,
        color = Color.White,
        fontSize = DsType.Label,
        modifier = modifier
            .clip(RoundedCornerShape(DsRadius.Md))
            .background(color.copy(alpha = 0.85f))
            .clickable { onClick() }
            .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm)
    )
}
