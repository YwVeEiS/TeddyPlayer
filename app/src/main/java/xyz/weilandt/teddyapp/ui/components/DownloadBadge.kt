package xyz.weilandt.teddyapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import xyz.weilandt.teddyapp.domain.model.DownloadStatus
import xyz.weilandt.teddyapp.ui.theme.TeddyColors
import xyz.weilandt.teddyapp.ui.theme.TeddyTheme

/**
 * Small round badge on a tonie: downloading (ring), available offline (check mark),
 * unavailable (crossed-out cloud) or error.
 */
@Composable
fun DownloadBadge(
    status: DownloadStatus,
    isUnavailable: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
) {
    if (status == DownloadStatus.None && !isUnavailable) return
    Box(
        modifier = modifier
            .size(size)
            .shadow(2.dp, CircleShape)
            .background(Color.White, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        val iconModifier = Modifier.fillMaxSize(0.65f)
        when {
            isUnavailable -> Icon(Icons.Rounded.CloudOff, null, iconModifier, tint = TeddyColors.Muted)
            status is DownloadStatus.Downloading -> {
                val progress = status.progress
                if (progress != null) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.padding(3.dp).fillMaxSize(),
                        strokeWidth = 3.dp,
                        color = TeddyColors.Secondary,
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(3.dp).fillMaxSize(),
                        strokeWidth = 3.dp,
                        color = TeddyColors.Secondary,
                    )
                }
                Icon(Icons.Rounded.Download, null, Modifier.fillMaxSize(0.45f), tint = TeddyColors.Secondary)
            }
            status == DownloadStatus.Queued ->
                Icon(Icons.Rounded.Download, null, iconModifier, tint = TeddyColors.Muted)
            status == DownloadStatus.Completed ->
                Icon(Icons.Rounded.DownloadDone, null, iconModifier, tint = TeddyColors.Success)
            status == DownloadStatus.Failed ->
                Icon(Icons.Rounded.ErrorOutline, null, iconModifier, tint = TeddyColors.Warning)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DownloadBadgeAllStatesPreview() = TeddyTheme {
    Row(Modifier.padding(8.dp)) {
        listOf(
            DownloadStatus.Queued,
            DownloadStatus.Downloading(0.4f),
            DownloadStatus.Downloading(null),
            DownloadStatus.Completed,
            DownloadStatus.Failed,
        ).forEach { DownloadBadge(it, isUnavailable = false, modifier = Modifier.padding(4.dp)) }
        DownloadBadge(DownloadStatus.None, isUnavailable = true, modifier = Modifier.padding(4.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun DownloadBadgeDownloadingPreview() = TeddyTheme {
    DownloadBadge(DownloadStatus.Downloading(0.7f), isUnavailable = false, size = 64.dp)
}

@Preview(showBackground = true)
@Composable
private fun DownloadBadgeCompletedPreview() = TeddyTheme {
    DownloadBadge(DownloadStatus.Completed, isUnavailable = false, size = 64.dp)
}

@Preview(showBackground = true)
@Composable
private fun DownloadBadgeUnavailablePreview() = TeddyTheme {
    DownloadBadge(DownloadStatus.None, isUnavailable = true, size = 64.dp)
}
