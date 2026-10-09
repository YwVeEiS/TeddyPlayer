package xyz.weilandt.teddyapp.ui.setup

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import xyz.weilandt.teddyapp.core.mvi.MviViewModel
import xyz.weilandt.teddyapp.domain.model.ServerUrl
import xyz.weilandt.teddyapp.domain.repository.SettingsRepository
import xyz.weilandt.teddyapp.domain.repository.ToniesRepository
import xyz.weilandt.teddyapp.ui.parent.settings.ConnectionTest

/**
 * Initial setup: check and save the address. The dialog disappears by itself
 * as soon as [SettingsRepository.isServerConfigured] reports `true` – hence no effects.
 */
class ServerSetupViewModel(
    private val settings: SettingsRepository,
    private val tonies: ToniesRepository,
) : MviViewModel<ServerSetupState, ServerSetupIntent, ServerSetupResult, Nothing>(ServerSetupState()) {

    override fun onIntent(intent: ServerSetupIntent) {
        when (intent) {
            is ServerSetupIntent.UrlChanged -> dispatch(ServerSetupResult.UrlInput(intent.url))
            is ServerSetupIntent.ShowTitlesChanged -> dispatch(ServerSetupResult.ShowTitles(intent.show))
            ServerSetupIntent.Connect -> connect()
            ServerSetupIntent.SaveAnyway -> normalizedUrl()?.let { url ->
                viewModelScope.launch { save(url) }
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
                save(url)
            } else {
                dispatch(ServerSetupResult.Connection(ConnectionTest.Failed))
            }
        }
    }

    /** Save titles first – saving the address closes the dialog. */
    private suspend fun save(url: String) {
        settings.setShowTitles(state.value.showTitles)
        settings.setServerUrl(url)
    }

    private fun normalizedUrl(): String? {
        val url = ServerUrl.normalize(state.value.urlInput)
        if (url == null) dispatch(ServerSetupResult.Connection(ConnectionTest.InvalidUrl))
        return url
    }

    override fun reduce(state: ServerSetupState, result: ServerSetupResult): ServerSetupState =
        ServerSetupReducer.reduce(state, result)
}
