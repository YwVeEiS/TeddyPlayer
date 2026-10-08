package xyz.weilandt.teddyapp.ui.setup

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import xyz.weilandt.teddyapp.core.mvi.MviViewModel
import xyz.weilandt.teddyapp.domain.model.ServerUrl
import xyz.weilandt.teddyapp.domain.repository.SettingsRepository
import xyz.weilandt.teddyapp.domain.repository.ToniesRepository
import xyz.weilandt.teddyapp.ui.parent.settings.ConnectionTest

/**
 * Ersteinrichtung: Adresse prüfen und speichern. Der Dialog verschwindet von selbst,
 * sobald [SettingsRepository.isServerConfigured] `true` meldet – daher keine Effects.
 */
class ServerSetupViewModel(
    private val settings: SettingsRepository,
    private val tonies: ToniesRepository,
) : MviViewModel<ServerSetupState, ServerSetupIntent, ServerSetupResult, Nothing>(ServerSetupState()) {

    override fun onIntent(intent: ServerSetupIntent) {
        when (intent) {
            is ServerSetupIntent.UrlChanged -> dispatch(ServerSetupResult.UrlInput(intent.url))
            ServerSetupIntent.Connect -> connect()
            ServerSetupIntent.SaveAnyway -> normalizedUrl()?.let { url ->
                viewModelScope.launch { settings.setServerUrl(url) }
            }
        }
    }

    private fun connect() {
        if (state.value.isTesting) return
        val url = normalizedUrl() ?: return
        viewModelScope.launch {
            dispatch(ServerSetupResult.Connection(ConnectionTest.Testing))
            val result = tonies.testConnection(url)
            if (result.isSuccess) {
                dispatch(ServerSetupResult.Connection(ConnectionTest.Success(result.getOrDefault(0))))
                settings.setServerUrl(url)
            } else {
                dispatch(ServerSetupResult.Connection(ConnectionTest.Failed))
            }
        }
    }

    private fun normalizedUrl(): String? {
        val url = ServerUrl.normalize(state.value.urlInput)
        if (url == null) dispatch(ServerSetupResult.Connection(ConnectionTest.InvalidUrl))
        return url
    }

    override fun reduce(state: ServerSetupState, result: ServerSetupResult): ServerSetupState =
        ServerSetupReducer.reduce(state, result)
}
