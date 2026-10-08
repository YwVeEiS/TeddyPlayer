package xyz.weilandt.teddyapp.ui.player

import xyz.weilandt.teddyapp.domain.model.PlaybackSnapshot
import xyz.weilandt.teddyapp.domain.model.Tonie

data class PlayerState(
    val tonie: Tonie? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val hasError: Boolean = false,
    val chapterCount: Int = 1,
    val currentChapter: Int = 0,
    val chapterProgress: Float = 0f,
    val showTitles: Boolean = false,
) {
    val canGoNext: Boolean get() = currentChapter < chapterCount - 1
}

sealed interface PlayerIntent {
    data object TogglePlayPause : PlayerIntent
    data object NextChapter : PlayerIntent
    data object PreviousChapter : PlayerIntent
    data class ChapterSelected(val index: Int) : PlayerIntent
    data object Close : PlayerIntent
}

sealed interface PlayerResult {
    data class Playback(val snapshot: PlaybackSnapshot, val tonie: Tonie?) : PlayerResult
    data class ShowTitles(val show: Boolean) : PlayerResult
}

sealed interface PlayerEffect {
    data object NavigateBack : PlayerEffect
}

object PlayerReducer {
    fun reduce(state: PlayerState, result: PlayerResult): PlayerState = when (result) {
        is PlayerResult.Playback -> {
            val s = result.snapshot
            state.copy(
                tonie = result.tonie,
                isPlaying = s.isPlaying,
                isBuffering = s.isBuffering,
                hasError = s.hasError,
                chapterCount = s.chapterCount,
                currentChapter = s.currentChapter,
                chapterProgress = s.chapterProgress,
            )
        }
        is PlayerResult.ShowTitles -> state.copy(showTitles = result.show)
    }
}
