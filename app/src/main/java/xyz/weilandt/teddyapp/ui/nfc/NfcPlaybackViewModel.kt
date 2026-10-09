package xyz.weilandt.teddyapp.ui.nfc

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import xyz.weilandt.teddyapp.core.mvi.MviViewModel
import xyz.weilandt.teddyapp.domain.model.DownloadStatus
import xyz.weilandt.teddyapp.domain.model.TagIds
import xyz.weilandt.teddyapp.domain.model.Tonie
import xyz.weilandt.teddyapp.domain.repository.DownloadRepository
import xyz.weilandt.teddyapp.domain.repository.NetworkMonitor
import xyz.weilandt.teddyapp.domain.repository.PlaybackController
import xyz.weilandt.teddyapp.domain.repository.TonieTagReader
import xyz.weilandt.teddyapp.domain.repository.ToniesRepository

/**
 * Hold a figure against the device → the tonie starts and the player opens – on every screen.
 * Lives at navigation level so it is active everywhere.
 */
class NfcPlaybackViewModel(
    private val tonies: ToniesRepository,
    private val downloads: DownloadRepository,
    private val playback: PlaybackController,
    private val network: NetworkMonitor,
    tagReader: TonieTagReader,
    private val clock: () -> Long = System::currentTimeMillis,
) : MviViewModel<NfcPlaybackState, NfcPlaybackIntent, NfcPlaybackResult, NfcPlaybackEffect>(NfcPlaybackState()) {

    private var lastTag: String? = null
    private var lastTagAt = 0L
    private var feedbackJob: Job? = null

    init {
        tagReader.tagIds
            .onEach { onIntent(NfcPlaybackIntent.TagDetected(it)) }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: NfcPlaybackIntent) {
        when (intent) {
            is NfcPlaybackIntent.TagDetected -> onTag(intent.uidHex.lowercase())
        }
    }

    private fun onTag(uidHex: String) {
        // If the figure stays on the device, Android may report it several times
        val now = clock()
        if (uidHex == lastTag && now - lastTagAt < REPEAT_WINDOW_MS) return
        lastTag = uidHex
        lastTagAt = now

        viewModelScope.launch {
            val tonie = TagIds.candidates(uidHex).firstNotNullOfOrNull { tonies.findByTagId(it) }
            when {
                tonie == null -> reject(NfcFeedback.Unknown)
                !isPlayable(tonie) -> reject(NfcFeedback.Unavailable)
                else -> {
                    showFeedback(null)
                    playback.play(tonie)
                    emit(NfcPlaybackEffect.NavigateToPlayer)
                }
            }
        }
    }

    /** Same as the grid: offline, only fully downloaded tonies. */
    private suspend fun isPlayable(tonie: Tonie): Boolean {
        if (downloads.downloads.value[tonie.id]?.status == DownloadStatus.Completed) return true
        return network.isNetworkAvailable.first() && tonies.isServerReachable.value != false
    }

    private fun reject(feedback: NfcFeedback) {
        emit(NfcPlaybackEffect.Rejected)
        showFeedback(feedback)
    }

    private fun showFeedback(feedback: NfcFeedback?) {
        feedbackJob?.cancel()
        dispatch(NfcPlaybackResult.Feedback(feedback))
        if (feedback == null) return
        feedbackJob = viewModelScope.launch {
            delay(FEEDBACK_DURATION_MS)
            dispatch(NfcPlaybackResult.Feedback(null))
        }
    }

    override fun reduce(state: NfcPlaybackState, result: NfcPlaybackResult): NfcPlaybackState =
        NfcPlaybackReducer.reduce(state, result)

    companion object {
        const val REPEAT_WINDOW_MS = 2_000L
        const val FEEDBACK_DURATION_MS = 2_000L
    }
}
