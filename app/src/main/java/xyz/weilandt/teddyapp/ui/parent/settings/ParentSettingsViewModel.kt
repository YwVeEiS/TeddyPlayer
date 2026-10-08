package xyz.weilandt.teddyapp.ui.parent.settings

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import xyz.weilandt.teddyapp.core.mvi.MviViewModel
import xyz.weilandt.teddyapp.domain.model.ServerUrl
import xyz.weilandt.teddyapp.domain.repository.DownloadRepository
import xyz.weilandt.teddyapp.domain.repository.SettingsRepository
import xyz.weilandt.teddyapp.domain.repository.ToniesRepository

class ParentSettingsViewModel(
    private val settings: SettingsRepository,
    private val tonies: ToniesRepository,
    private val downloads: DownloadRepository,
) : MviViewModel<ParentSettingsState, ParentSettingsIntent, ParentSettingsResult, ParentSettingsEffect>(
    ParentSettingsState()
) {

    init {
        settings.serverUrl
            .onEach { dispatch(ParentSettingsResult.SavedUrl(it)) }
            .launchIn(viewModelScope)

        combine(tonies.observeTonies(), downloads.downloads, downloads.usedBytes, ParentSettingsResult::Data)
            .onEach(::dispatch)
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: ParentSettingsIntent) {
        when (intent) {
            is ParentSettingsIntent.UrlChanged -> dispatch(ParentSettingsResult.UrlInput(intent.url))
            ParentSettingsIntent.SaveUrl -> saveUrl()
            ParentSettingsIntent.Refresh -> refresh()
            is ParentSettingsIntent.DeleteDownload -> downloads.remove(intent.tonieId)
            ParentSettingsIntent.DeleteAllRequested -> dispatch(ParentSettingsResult.ConfirmDeleteAll(true))
            ParentSettingsIntent.DeleteAllDismissed -> dispatch(ParentSettingsResult.ConfirmDeleteAll(false))
            ParentSettingsIntent.DeleteAllConfirmed -> {
                downloads.removeAll()
                dispatch(ParentSettingsResult.ConfirmDeleteAll(false))
            }
            ParentSettingsIntent.Back -> emit(ParentSettingsEffect.NavigateBack)
        }
    }

    private fun saveUrl() {
        val url = ServerUrl.normalize(state.value.serverUrlInput)
        if (url == null) {
            dispatch(ParentSettingsResult.Connection(ConnectionTest.InvalidUrl))
            return
        }
        viewModelScope.launch {
            dispatch(ParentSettingsResult.Connection(ConnectionTest.Testing))
            val result = tonies.testConnection(url)
            if (result.isSuccess) {
                dispatch(ParentSettingsResult.UrlInput(url))
                settings.setServerUrl(url)
                dispatch(ParentSettingsResult.Connection(ConnectionTest.Success(result.getOrDefault(0))))
                refresh()
            } else {
                dispatch(ParentSettingsResult.Connection(ConnectionTest.Failed))
            }
        }
    }

    private fun refresh() {
        if (state.value.isRefreshing) return
        viewModelScope.launch {
            dispatch(ParentSettingsResult.Refreshing(true))
            val result = tonies.refresh()
            dispatch(ParentSettingsResult.Refreshing(false))
            dispatch(
                ParentSettingsResult.Connection(
                    result.fold({ ConnectionTest.Success(it) }, { ConnectionTest.Failed })
                )
            )
        }
    }

    override fun reduce(state: ParentSettingsState, result: ParentSettingsResult): ParentSettingsState =
        ParentSettingsReducer.reduce(state, result)
}
