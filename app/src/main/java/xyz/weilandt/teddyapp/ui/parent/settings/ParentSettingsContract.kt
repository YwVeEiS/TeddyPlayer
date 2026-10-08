package xyz.weilandt.teddyapp.ui.parent.settings

import xyz.weilandt.teddyapp.domain.model.DownloadInfo
import xyz.weilandt.teddyapp.domain.model.DownloadStatus
import xyz.weilandt.teddyapp.domain.model.Tonie

sealed interface ConnectionTest {
    data object Idle : ConnectionTest
    data object Testing : ConnectionTest
    data class Success(val tonieCount: Int) : ConnectionTest
    data object Failed : ConnectionTest
    data object InvalidUrl : ConnectionTest
}

data class DownloadEntry(
    val tonie: Tonie,
    val status: DownloadStatus,
    val bytes: Long,
)

data class ParentSettingsState(
    val serverUrlInput: String = "",
    val savedServerUrl: String = "",
    val connection: ConnectionTest = ConnectionTest.Idle,
    val isRefreshing: Boolean = false,
    val tonieCount: Int = 0,
    val downloads: List<DownloadEntry> = emptyList(),
    val usedBytes: Long = 0L,
    val confirmDeleteAll: Boolean = false,
) {
    val isUrlChanged: Boolean get() = serverUrlInput.trim() != savedServerUrl
}

sealed interface ParentSettingsIntent {
    data class UrlChanged(val url: String) : ParentSettingsIntent
    data object SaveUrl : ParentSettingsIntent
    data object Refresh : ParentSettingsIntent
    data class DeleteDownload(val tonieId: String) : ParentSettingsIntent
    data object DeleteAllRequested : ParentSettingsIntent
    data object DeleteAllConfirmed : ParentSettingsIntent
    data object DeleteAllDismissed : ParentSettingsIntent
    data object Back : ParentSettingsIntent
}

sealed interface ParentSettingsResult {
    data class SavedUrl(val url: String) : ParentSettingsResult
    data class UrlInput(val url: String) : ParentSettingsResult
    data class Connection(val test: ConnectionTest) : ParentSettingsResult
    data class Refreshing(val active: Boolean) : ParentSettingsResult
    data class Data(
        val tonies: List<Tonie>,
        val downloads: Map<String, DownloadInfo>,
        val usedBytes: Long,
    ) : ParentSettingsResult
    data class ConfirmDeleteAll(val visible: Boolean) : ParentSettingsResult
}

sealed interface ParentSettingsEffect {
    data object NavigateBack : ParentSettingsEffect
}

object ParentSettingsReducer {
    fun reduce(state: ParentSettingsState, result: ParentSettingsResult): ParentSettingsState = when (result) {
        is ParentSettingsResult.SavedUrl -> state.copy(
            savedServerUrl = result.url,
            // Eingabe nur übernehmen, solange der Nutzer noch nichts Eigenes getippt hat
            serverUrlInput = if (state.serverUrlInput.isEmpty() || !state.isUrlChanged) result.url else state.serverUrlInput,
        )
        is ParentSettingsResult.UrlInput -> state.copy(serverUrlInput = result.url, connection = ConnectionTest.Idle)
        is ParentSettingsResult.Connection -> state.copy(connection = result.test)
        is ParentSettingsResult.Refreshing -> state.copy(isRefreshing = result.active)
        is ParentSettingsResult.Data -> {
            val byId = result.tonies.associateBy { it.id }
            state.copy(
                tonieCount = result.tonies.size,
                usedBytes = result.usedBytes,
                downloads = result.downloads.values
                    .mapNotNull { info ->
                        byId[info.tonieId]?.let { DownloadEntry(it, info.status, info.bytesDownloaded) }
                    }
                    .sortedByDescending { it.bytes },
            )
        }
        is ParentSettingsResult.ConfirmDeleteAll -> state.copy(confirmDeleteAll = result.visible)
    }
}
