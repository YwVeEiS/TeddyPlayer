package xyz.weilandt.teddyapp.ui.player

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import xyz.weilandt.teddyapp.core.mvi.MviViewModel
import xyz.weilandt.teddyapp.domain.repository.PlaybackController
import xyz.weilandt.teddyapp.domain.repository.ToniesRepository

class PlayerViewModel(
    private val playback: PlaybackController,
    tonies: ToniesRepository,
) : MviViewModel<PlayerState, PlayerIntent, PlayerResult, PlayerEffect>(PlayerState()) {

    init {
        combine(playback.state, tonies.observeTonies()) { snapshot, list ->
            PlayerResult.Playback(snapshot, list.firstOrNull { it.id == snapshot.tonieId })
        }
            .onEach(::dispatch)
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: PlayerIntent) {
        when (intent) {
            PlayerIntent.TogglePlayPause -> playback.togglePlayPause()
            PlayerIntent.NextChapter -> playback.nextChapter()
            PlayerIntent.PreviousChapter -> playback.previousChapter()
            is PlayerIntent.ChapterSelected -> playback.seekToChapter(intent.index)
            PlayerIntent.Close -> emit(PlayerEffect.NavigateBack)
        }
    }

    override fun reduce(state: PlayerState, result: PlayerResult): PlayerState =
        PlayerReducer.reduce(state, result)
}
