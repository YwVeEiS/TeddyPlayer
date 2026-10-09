package xyz.weilandt.teddyapp.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import xyz.weilandt.teddyapp.R
import xyz.weilandt.teddyapp.domain.model.DownloadStatus
import xyz.weilandt.teddyapp.domain.model.PlaybackSnapshot
import xyz.weilandt.teddyapp.ui.components.BigIconButton
import xyz.weilandt.teddyapp.ui.components.HoldToOpenButton
import xyz.weilandt.teddyapp.ui.components.MiniPlayer
import xyz.weilandt.teddyapp.ui.components.TonieTile
import xyz.weilandt.teddyapp.ui.preview.SampleData
import xyz.weilandt.teddyapp.ui.preview.TABLET_PORTRAIT
import xyz.weilandt.teddyapp.ui.setup.ServerSetupRoute
import xyz.weilandt.teddyapp.ui.theme.TeddyColors
import xyz.weilandt.teddyapp.ui.theme.TeddyTheme

@Composable
fun LibraryRoute(
    onOpenPlayer: () -> Unit,
    onOpenParentGate: () -> Unit,
    viewModel: LibraryViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val shakeTriggers = remember { mutableStateMapOf<String, Int>() }
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                LibraryEffect.NavigateToPlayer -> onOpenPlayer()
                LibraryEffect.NavigateToParentGate -> onOpenParentGate()
                is LibraryEffect.ShakeTonie -> {
                    haptics.performHapticFeedback(HapticFeedbackType.Reject)
                    shakeTriggers[effect.tonieId] = (shakeTriggers[effect.tonieId] ?: 0) + 1
                }
            }
        }
    }

    LibraryScreen(state = state, shakeTriggers = shakeTriggers, onIntent = viewModel::onIntent)

    if (state.needsServerSetup) {
        ServerSetupRoute()
    }
}

@Composable
fun LibraryScreen(
    state: LibraryState,
    onIntent: (LibraryIntent) -> Unit,
    shakeTriggers: Map<String, Int> = emptyMap(),
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TeddyColors.Background)
            .systemBarsPadding(),
    ) {
        TopRow(isOnline = state.isOnline, onOpenParentArea = { onIntent(LibraryIntent.OpenParentArea) })

        Box(Modifier.weight(1f)) {
            when (state.content) {
                LibraryContent.Loading -> CenteredLoading()
                LibraryContent.Error -> CenteredMessage(Icons.Rounded.CloudOff) { onIntent(LibraryIntent.Retry) }
                LibraryContent.Empty -> CenteredMessage(Icons.Rounded.Inbox) { onIntent(LibraryIntent.Retry) }
                LibraryContent.Content -> TonieGrid(state, shakeTriggers, onIntent)
            }
        }

        state.nowPlaying?.let { nowPlaying ->
            MiniPlayer(
                tonie = nowPlaying.tonie,
                isPlaying = nowPlaying.isPlaying,
                isBuffering = nowPlaying.isBuffering,
                progress = nowPlaying.progress,
                onOpen = { onIntent(LibraryIntent.OpenPlayer) },
                onTogglePlayPause = { onIntent(LibraryIntent.TogglePlayPause) },
                showTitle = state.showTitles,
            )
        }
    }
}

@Composable
private fun TopRow(isOnline: Boolean, onOpenParentArea: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!isOnline) {
            Icon(
                imageVector = Icons.Rounded.CloudOff,
                contentDescription = stringResource(R.string.offline),
                tint = TeddyColors.Muted,
                modifier = Modifier.size(32.dp),
            )
        }
        Spacer(Modifier.weight(1f))
        HoldToOpenButton(onTriggered = onOpenParentArea)
    }
}

@Composable
private fun TonieGrid(
    state: LibraryState,
    shakeTriggers: Map<String, Int>,
    onIntent: (LibraryIntent) -> Unit,
) {
    val playingId = state.playback.tonieId
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Phone: always 2 large tiles; tablet: as many columns as fit (portrait ~3, landscape ~6)
        val isCompact = maxWidth < 600.dp
        val spacing = if (isCompact) 16.dp else 24.dp
        LazyVerticalGrid(
            columns = if (isCompact) GridCells.Fixed(2) else GridCells.Adaptive(minSize = 180.dp),
            contentPadding = PaddingValues(spacing),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalArrangement = Arrangement.spacedBy(spacing),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(state.items, key = { it.tonie.id }) { item ->
                TonieTile(
                    tonie = item.tonie,
                    downloadStatus = item.download,
                    isAvailable = state.isAvailable(item),
                    isNowPlaying = item.tonie.id == playingId,
                    shakeTrigger = shakeTriggers[item.tonie.id] ?: 0,
                    showTitle = state.showTitles,
                    onClick = { onIntent(LibraryIntent.TonieClicked(item.tonie.id)) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

@Composable
private fun CenteredLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            modifier = Modifier.size(96.dp),
            color = TeddyColors.Primary,
            strokeWidth = 8.dp,
        )
    }
}

@Composable
private fun CenteredMessage(icon: ImageVector, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = TeddyColors.Muted, modifier = Modifier.size(140.dp))
        Spacer(Modifier.height(32.dp))
        BigIconButton(
            icon = Icons.Rounded.Refresh,
            contentDescription = stringResource(R.string.retry),
            onClick = onRetry,
            containerColor = TeddyColors.Secondary,
        )
    }
}

// region Previews

internal class LibraryStateProvider : PreviewParameterProvider<LibraryState> {
    private val items = SampleData.tonies.mapIndexed { index, tonie ->
        LibraryItem(
            tonie,
            when (index) {
                0 -> DownloadStatus.Completed
                1 -> DownloadStatus.Downloading(0.45f)
                3 -> DownloadStatus.Completed
                else -> DownloadStatus.None
            },
        )
    }
    private val playing = PlaybackSnapshot(
        tonieId = SampleData.bobo.id,
        isPlaying = true,
        positionMs = 400_000L,
        durationMs = 1_500_000L,
        chapterStartsMs = SampleData.bobo.chapterStartsMs,
    )

    override val values = sequenceOf(
        LibraryState(hasLoadedCache = false),
        LibraryState(hasLoadedCache = true, items = items),
        LibraryState(hasLoadedCache = true, items = items, playback = playing),
        LibraryState(hasLoadedCache = true, items = items, playback = playing, showTitles = true),
        LibraryState(hasLoadedCache = true, needsServerSetup = true),
        LibraryState(hasLoadedCache = true, items = items, isOnline = false, playback = playing.copy(isPlaying = false)),
        LibraryState(hasLoadedCache = true, items = emptyList()),
        LibraryState(hasLoadedCache = true, items = emptyList(), lastRefreshFailed = true, isOnline = false),
    )
}

@Preview(name = "Handy", showBackground = true, widthDp = 400, heightDp = 860)
@Preview(name = "Tablet hoch", showBackground = true, device = TABLET_PORTRAIT)
@Preview(name = "Tablet quer", showBackground = true, device = Devices.PIXEL_TABLET)
@Composable
private fun LibraryScreenPreview(@PreviewParameter(LibraryStateProvider::class) state: LibraryState) = TeddyTheme {
    LibraryScreen(state = state, onIntent = {})
}

// endregion
