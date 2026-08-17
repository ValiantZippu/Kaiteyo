package ua.syt0r.kanji.desktop.game.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ua.syt0r.kanji.desktop.designsystem.DsIconButton
import ua.syt0r.kanji.desktop.designsystem.DsRadius
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.game.GameSession

/**
 * The photo viewfinder (spec §44): a minimal frame — capture, zoom, exit.
 * Nothing else on screen; the scene stays the star.
 */
@Composable
fun PhotoModeOverlay(session: GameSession) {
    if (!session.state.photoMode) return

    Box(Modifier.fillMaxSize()) {
        // Viewfinder frame, centred on the photo focus.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 90.dp, vertical = 70.dp)
                .border(2.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(DsRadius.Lg))
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = DsSpacing.Lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "PHOTO MODE — move mouse to frame · click to capture",
                color = Color.White,
                fontSize = DsType.Label,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(DsRadius.Sm))
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(horizontal = DsSpacing.Md, vertical = 4.dp)
            )
        }

        // Bottom controls
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = DsSpacing.Xl),
            horizontalArrangement = Arrangement.spacedBy(DsSpacing.Lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DsIconButton(
                icon = Icons.Default.Close,
                onClick = { session.togglePhotoMode() },
                contentDescription = "Exit photo mode (Esc)",
                tint = Color.White
            )
            // Capture button
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.85f))
                    .border(3.dp, Color.Black.copy(alpha = 0.3f), RoundedCornerShape(50))
                    .clickable { session.capturePhotoPublic() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = "Capture",
                    tint = Color.Black,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}
