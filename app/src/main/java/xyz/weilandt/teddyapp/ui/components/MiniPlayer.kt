package xyz.weilandt.teddyapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import xyz.weilandt.teddyapp.domain.model.Tonie
import xyz.weilandt.teddyapp.ui.preview.SampleData
import xyz.weilandt.teddyapp.ui.theme.TeddyColors
import xyz.weilandt.teddyapp.ui.theme.TeddyTheme

/** Leiste am unteren Rand: Tippen aufs Cover öffnet den Player, großer Play/Pause-Knopf. */
@Composable
fun MiniPlayer(
    tonie: Tonie,
    isPlaying: Boolean,
    isBuffering: Boolean,
    progress: Float,
    onOpen: () -> Unit,
    onTogglePlayPause: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        shadowElevation = 12.dp,
        color = TeddyColors.Surface,
    ) {
        Column {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = TeddyColors.Primary,
                trackColor = TeddyColors.Muted.copy(alpha = 0.3f),
            )
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TonieCover(
                    tonieId = tonie.id,
                    coverUrl = tonie.coverUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(72.dp)
                        .clickable(onClick = onOpen)
                        .semantics { contentDescription = "Player öffnen" },
                )
                Box(Modifier.weight(1f))
                Box(contentAlignment = Alignment.Center) {
                    BigIconButton(
                        icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Abspielen",
                        onClick = onTogglePlayPause,
                        size = 72.dp,
                    )
                    if (isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(80.dp),
                            color = TeddyColors.Secondary,
                            strokeWidth = 4.dp,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MiniPlayerPlayingPreview() = TeddyTheme {
    MiniPlayer(SampleData.bobo, isPlaying = true, isBuffering = false, progress = 0.4f, onOpen = {}, onTogglePlayPause = {})
}

@Preview(showBackground = true)
@Composable
private fun MiniPlayerPausedPreview() = TeddyTheme {
    MiniPlayer(SampleData.conni, isPlaying = false, isBuffering = false, progress = 0.7f, onOpen = {}, onTogglePlayPause = {})
}

@Preview(showBackground = true)
@Composable
private fun MiniPlayerBufferingPreview() = TeddyTheme {
    MiniPlayer(SampleData.pikachu, isPlaying = false, isBuffering = true, progress = 0f, onOpen = {}, onTogglePlayPause = {})
}
