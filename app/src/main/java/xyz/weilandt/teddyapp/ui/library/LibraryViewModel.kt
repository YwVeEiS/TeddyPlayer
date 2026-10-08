package xyz.weilandt.teddyapp.ui.library

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import xyz.weilandt.teddyapp.core.mvi.MviViewModel
import xyz.weilandt.teddyapp.domain.repository.DownloadRepository
import xyz.weilandt.teddyapp.domain.repository.NetworkMonitor
import xyz.weilandt.teddyapp.domain.repository.PlaybackController
import xyz.weilandt.teddyapp.domain.repository.SettingsRepository
import xyz.weilandt.teddyapp.domain.repository.ToniesRepository

class LibraryViewModel(
    private val tonies: ToniesRepository,
    private val downloads: DownloadRepository,
    private val playback: PlaybackController,
    network: NetworkMonitor,
    settings: SettingsRepository,
) : MviViewModel<LibraryState, LibraryIntent, LibraryResult, LibraryEffect>(LibraryState()) {

    private var refreshJob: Job? = null

    init {
        combine(tonies.observeTonies(), downloads.downloads, LibraryResult::TonieData)
            .onEach(::dispatch)
            .launchIn(viewModelScope)

        combine(network.isNetworkAvailable, tonies.isServerReachable) { net, server -> net && server != false }
            .distinctUntilChanged()
            .onEach { dispatch(LibraryResult.OnlineChanged(it)) }
            .launchIn(viewModelScope)

        playback.state
            .onEach { dispatch(LibraryResult.PlaybackChanged(it)) }
            .launchIn(viewModelScope)

        settings.isServerConfigured
            .onEach { dispatch(LibraryResult.ServerConfiguredChanged(it)) }
            .launchIn(viewModelScope)

        settings.showTitles
            .onEach { dispatch(LibraryResult.ShowTitlesChanged(it)) }
            .launchIn(viewModelScope)

        // Sobald Netz da und der Server eingerichtet ist: laden.
        // Ist der Server nicht erreichbar, regelmäßig erneut versuchen.
        viewModelScope.launch {
            combine(network.isNetworkAvailable, settings.isServerConfigured) { available, configured ->
                available && configured
            }.distinctUntilChanged().collectLatest { canLoad ->
                if (!canLoad) return@collectLatest
                refresh()?.join()
                while (tonies.isServerReachable.value == false) {
                    delay(RETRY_INTERVAL_MS)
                    refresh()?.join()
                }
            }
        }
    }

    override fun onIntent(intent: LibraryIntent) {
        when (intent) {
            is LibraryIntent.TonieClicked -> onTonieClicked(intent.tonieId)
            LibraryIntent.TogglePlayPause -> playback.togglePlayPause()
            LibraryIntent.OpenPlayer -> emit(LibraryEffect.NavigateToPlayer)
            LibraryIntent.Retry -> if (!state.value.needsServerSetup) refresh()
            LibraryIntent.OpenParentArea -> emit(LibraryEffect.NavigateToParentGate)
        }
    }

    private fun onTonieClicked(id: String) {
        val current = state.value
        val item = current.items.firstOrNull { it.tonie.id == id } ?: return
        if (!current.isAvailable(item)) {
            emit(LibraryEffect.ShakeTonie(id))
            return
        }
        playback.play(item.tonie)
        emit(LibraryEffect.NavigateToPlayer)
    }

    private fun refresh(): Job? {
        if (refreshJob?.isActive == true) return refreshJob
        refreshJob = viewModelScope.launch {
            dispatch(LibraryResult.RefreshStarted)
            val result = tonies.refresh()
            dispatch(LibraryResult.RefreshFinished(result.isSuccess))
        }
        return refreshJob
    }

    override fun reduce(state: LibraryState, result: LibraryResult): LibraryState =
        LibraryReducer.reduce(state, result)

    companion object {
        const val RETRY_INTERVAL_MS = 30_000L
    }
}
