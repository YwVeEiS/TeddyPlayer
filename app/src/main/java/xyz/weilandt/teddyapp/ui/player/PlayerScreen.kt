package xyz.weilandt.teddyapp.ui.player

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import xyz.weilandt.teddyapp.ui.components.BigIconButton
import xyz.weilandt.teddyapp.ui.components.ChapterDots
import xyz.weilandt.teddyapp.ui.components.TonieCover
import xyz.weilandt.teddyapp.ui.preview.SampleData
import xyz.weilandt.teddyapp.ui.theme.TeddyColors
import xyz.weilandt.teddyapp.ui.theme.TeddyTheme

@Composable
fun PlayerRoute(
    onBack: () -> Unit,
    viewModel: PlayerViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                PlayerEffect.NavigateBack -> onBack()
            }
        }
    }
    PlayerScreen(state = state, onIntent = viewModel::onIntent)
}

@Composable
fun PlayerScreen(
    state: PlayerState,
    onIntent: (PlayerIntent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TeddyColors.Background)
            .systemBarsPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth()) {
            BigIconButton(
                icon = Icons.Rounded.Home,
                contentDescription = "Zurück zur Übersicht",
                onClick = { onIntent(PlayerIntent.Close) },
                size = 64.dp,
                containerColor = TeddyColors.Secondary,
            )
        }

        val tonie = state.tonie
        if (tonie == null) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.MusicOff, contentDescription = null, tint = TeddyColors.Muted, modifier = Modifier.size(140.dp))
            }
            return@Column
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            TonieCover(
                tonieId = tonie.id,
                coverUrl = tonie.coverUrl,
                contentDescription = tonie.displayName,
                modifier = Modifier.fillMaxSize(),
                grayscale = state.hasError,
            )
            if (state.hasError) {
                Icon(Icons.Rounded.CloudOff, contentDescription = "Fehler", tint = TeddyColors.Warning, modifier = Modifier.size(120.dp))
            }
        }

        if (state.showTitles) {
            Text(
                text = tonie.shortTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (tonie.series.isNotBlank() && tonie.series != tonie.shortTitle) {
                Text(
                    text = tonie.series,
                    style = MaterialTheme.typography.titleMedium,
                    color = TeddyColors.Muted,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        ChapterDots(
            chapterCount = state.chapterCount,
            currentChapter = state.currentChapter,
            progressInChapter = state.chapterProgress,
            onChapterClick = { onIntent(PlayerIntent.ChapterSelected(it)) },
        )

        Spacer(Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BigIconButton(
                icon = Icons.Rounded.SkipPrevious,
                contentDescription = "Kapitel zurück",
                onClick = { onIntent(PlayerIntent.PreviousChapter) },
                containerColor = TeddyColors.Tertiary,
                enabled = !state.hasError,
            )
            Box(contentAlignment = Alignment.Center) {
                BigIconButton(
                    icon = when {
                        state.hasError -> Icons.Rounded.Replay
                        state.isPlaying -> Icons.Rounded.Pause
                        else -> Icons.Rounded.PlayArrow
                    },
                    contentDescription = if (state.isPlaying) "Pause" else "Abspielen",
                    onClick = { onIntent(PlayerIntent.TogglePlayPause) },
                    size = 128.dp,
                )
                if (state.isBuffering) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(140.dp),
                        color = TeddyColors.Secondary,
                        strokeWidth = 6.dp,
                    )
                }
            }
            BigIconButton(
                icon = Icons.Rounded.SkipNext,
                contentDescription = "Kapitel vor",
                onClick = { onIntent(PlayerIntent.NextChapter) },
                containerColor = TeddyColors.Tertiary,
                enabled = state.canGoNext && !state.hasError,
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

// region Previews

internal class PlayerStateProvider : PreviewParameterProvider<PlayerState> {
    private val base = PlayerState(
        tonie = SampleData.bobo,
        chapterCount = 5,
        currentChapter = 2,
        chapterProgress = 0.4f,
    )

    override val values = sequenceOf(
        base.copy(isPlaying = true),
        base.copy(isPlaying = false),
        base.copy(isBuffering = true),
        base.copy(hasError = true),
        base.copy(isPlaying = true, currentChapter = 4, chapterProgress = 0.9f),
        base.copy(isPlaying = true, showTitles = true),
        PlayerState(),
    )
}

@Preview(showBackground = true, widthDp = 400, heightDp = 860)
@Composable
private fun PlayerScreenPreview(@PreviewParameter(PlayerStateProvider::class) state: PlayerState) = TeddyTheme {
    PlayerScreen(state = state, onIntent = {})
}

// endregion
