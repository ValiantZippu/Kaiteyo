package ua.syt0r.kanji.desktop.game.ui.menus

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ua.syt0r.kanji.desktop.designsystem.DsRadius
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.game.GameSession
import ua.syt0r.kanji.desktop.game.photography.Photo
import ua.syt0r.kanji.desktop.game.photography.PhotoCategory

/**
 * The album (spec §45): every capture with its tagged vocabulary — each
 * discovery shows Japanese, reading and meaning.
 */
@Composable
fun AlbumView(session: GameSession) {
    val album = session.album
    Column(verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
        if (album.photos.isEmpty()) {
            Text(
                text = "No photos yet — press C and capture the town.",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = DsType.Body
            )
            return
        }
        PhotoCategory.entries.forEach { category ->
            val photos = album.byCategory(category)
            if (photos.isEmpty()) return@forEach
            Text(
                text = category.label,
                color = Color(0xFFFFD54F),
                fontSize = DsType.Label,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = DsSpacing.Sm)
            )
            photos.forEach { photo -> PhotoCard(session, photo) }
        }
    }
}

@Composable
private fun PhotoCard(session: GameSession, photo: Photo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DsRadius.Md))
            .background(Color.White.copy(alpha = 0.07f))
            .clickable { session.state.photoDetail = photo.id }
            .padding(DsSpacing.Md),
        verticalArrangement = Arrangement.spacedBy(DsSpacing.Xs)
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = photo.title,
                color = Color.White,
                fontSize = DsType.Body,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${photo.tags.size} words",
                color = Color(0xFF90CAF9),
                fontSize = DsType.Caption
            )
        }
        photo.tags.forEach { tag ->
            Text(
                text = "${tag.headword}  ${tag.reading}  —  ${tag.meaning}",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = DsType.Body
            )
        }
    }
}
