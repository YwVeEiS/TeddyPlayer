package xyz.weilandt.teddyapp.ui.library

import xyz.weilandt.teddyapp.domain.model.DownloadStatus

object LibraryReducer {

    fun reduce(state: LibraryState, result: LibraryResult): LibraryState = when (result) {
        is LibraryResult.TonieData -> state.copy(
            hasLoadedCache = true,
            items = result.tonies.map { tonie ->
                LibraryItem(tonie, result.downloads[tonie.id]?.status ?: DownloadStatus.None)
            },
        )
        is LibraryResult.OnlineChanged -> state.copy(isOnline = result.isOnline)
        is LibraryResult.ServerConfiguredChanged -> state.copy(needsServerSetup = !result.configured)
        is LibraryResult.ShowTitlesChanged -> state.copy(showTitles = result.show)
        is LibraryResult.PlaybackChanged -> state.copy(playback = result.snapshot)
        LibraryResult.RefreshStarted -> state.copy(isRefreshing = true)
        is LibraryResult.RefreshFinished -> state.copy(
            isRefreshing = false,
            lastRefreshFailed = !result.success,
        )
    }
}
