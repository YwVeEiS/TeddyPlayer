package xyz.weilandt.teddyapp.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import xyz.weilandt.teddyapp.domain.model.DownloadStatus
import xyz.weilandt.teddyapp.domain.model.Tonie
import xyz.weilandt.teddyapp.ui.preview.SampleData
import xyz.weilandt.teddyapp.ui.theme.TeddyColors
import xyz.weilandt.teddyapp.ui.theme.TeddyTheme

private val TileShape = RoundedCornerShape(28.dp)

/**
 * Eine Kachel im Grid.
 *
 * @param shakeTrigger jede Änderung lässt die Kachel wackeln ("geht gerade nicht")
 */
@Composable
fun TonieTile(
    tonie: Tonie,
    downloadStatus: DownloadStatus,
    isAvailable: Boolean,
    isNowPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shakeTrigger: Int = 0,
) {
    val shake = remember { Animatable(0f) }
    LaunchedEffect(shakeTrigger) {
        if (shakeTrigger == 0) return@LaunchedEffect
        shake.animateTo(0f, keyframes {
            durationMillis = 400
            -16f at 50
            16f at 120
            -12f at 190
            12f at 260
            -6f at 330
        })
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .graphicsLayer { translationX = shake.value }
            .shadow(if (isAvailable) 6.dp else 0.dp, TileShape)
            .clip(TileShape)
            .background(TeddyColors.Surface)
            .then(if (isNowPlaying) Modifier.border(4.dp, TeddyColors.Primary, TileShape) else Modifier)
            .clickable(onClick = onClick)
            .semantics { contentDescription = tonie.displayName },
    ) {
        TonieCover(
            tonieId = tonie.id,
            coverUrl = tonie.coverUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .then(if (isAvailable) Modifier else Modifier.alpha(0.4f)),
            grayscale = !isAvailable,
        )
        DownloadBadge(
            status = downloadStatus,
            isUnavailable = !isAvailable,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
        )
        if (isNowPlaying) {
            Icon(
                imageVector = Icons.Rounded.GraphicEq,
                contentDescription = null,
                tint = TeddyColors.Primary,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 180)
@Composable
private fun TonieTileAvailablePreview() = TeddyTheme {
    TonieTile(SampleData.bobo, DownloadStatus.None, isAvailable = true, isNowPlaying = false, onClick = {}, modifier = Modifier.padding(8.dp))
}

@Preview(showBackground = true, widthDp = 180)
@Composable
private fun TonieTileDownloadingPreview() = TeddyTheme {
    TonieTile(SampleData.conni, DownloadStatus.Downloading(0.6f), isAvailable = true, isNowPlaying = true, onClick = {}, modifier = Modifier.padding(8.dp))
}

@Preview(showBackground = true, widthDp = 180)
@Composable
private fun TonieTileDownloadedPreview() = TeddyTheme {
    TonieTile(SampleData.pikachu, DownloadStatus.Completed, isAvailable = true, isNowPlaying = false, onClick = {}, modifier = Modifier.padding(8.dp))
}

@Preview(showBackground = true, widthDp = 180)
@Composable
private fun TonieTileUnavailablePreview() = TeddyTheme {
    TonieTile(SampleData.frozen, DownloadStatus.None, isAvailable = false, isNowPlaying = false, onClick = {}, modifier = Modifier.padding(8.dp))
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun TonieTileNowPlayingPreview() = TeddyTheme {
    Row {
        TonieTile(SampleData.sandman, DownloadStatus.Completed, isAvailable = true, isNowPlaying = true, onClick = {}, modifier = Modifier.width(170.dp).padding(8.dp))
        TonieTile(SampleData.custom, DownloadStatus.Failed, isAvailable = true, isNowPlaying = false, onClick = {}, modifier = Modifier.width(170.dp).padding(8.dp))
    }
}
