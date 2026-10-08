package xyz.weilandt.teddyapp.ui.setup

import xyz.weilandt.teddyapp.domain.model.ServerUrl
import xyz.weilandt.teddyapp.ui.parent.settings.ConnectionTest

data class ServerSetupState(
    val urlInput: String = ServerUrl.DEFAULT,
    val connection: ConnectionTest = ConnectionTest.Idle,
) {
    val isTesting: Boolean get() = connection == ConnectionTest.Testing

    /** Nach einem Fehlschlag darf trotzdem gespeichert werden (z. B. Einrichtung unterwegs). */
    val canSaveAnyway: Boolean get() = connection == ConnectionTest.Failed
}

sealed interface ServerSetupIntent {
    data class UrlChanged(val url: String) : ServerSetupIntent
    data object Connect : ServerSetupIntent
    data object SaveAnyway : ServerSetupIntent
}

sealed interface ServerSetupResult {
    data class UrlInput(val url: String) : ServerSetupResult
    data class Connection(val test: ConnectionTest) : ServerSetupResult
}

object ServerSetupReducer {
    fun reduce(state: ServerSetupState, result: ServerSetupResult): ServerSetupState = when (result) {
        is ServerSetupResult.UrlInput -> state.copy(urlInput = result.url, connection = ConnectionTest.Idle)
        is ServerSetupResult.Connection -> state.copy(connection = result.test)
    }
}
