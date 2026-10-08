package xyz.weilandt.teddyapp.ui.library

import xyz.weilandt.teddyapp.domain.model.DownloadInfo
import xyz.weilandt.teddyapp.domain.model.DownloadStatus
import xyz.weilandt.teddyapp.domain.model.PlaybackSnapshot
import xyz.weilandt.teddyapp.domain.model.Tonie

data class LibraryItem(
    val tonie: Tonie,
    val download: DownloadStatus,
)

data class NowPlaying(
    val tonie: Tonie,
    val isPlaying: Boolean,
    val isBuffering: Boolean,
    val progress: Float,
)

enum class LibraryContent { Loading, Content, Empty, Error }

data class LibraryState(
    val items: List<LibraryItem> = emptyList(),
    val hasLoadedCache: Boolean = false,
    val isRefreshing: Boolean = false,
    val lastRefreshFailed: Boolean = false,
    val isOnline: Boolean = true,
    val playback: PlaybackSnapshot = PlaybackSnapshot(),
) {
    val content: LibraryContent
        get() = when {
            items.isNotEmpty() -> LibraryContent.Content
            !hasLoadedCache || isRefreshing -> LibraryContent.Loading
            lastRefreshFailed || !isOnline -> LibraryContent.Error
            else -> LibraryContent.Empty
        }

    val nowPlaying: NowPlaying?
        get() {
            val id = playback.tonieId ?: return null
            val tonie = items.firstOrNull { it.tonie.id == id }?.tonie ?: return null
            val progress = if (playback.durationMs > 0) {
                (playback.positionMs.toFloat() / playback.durationMs).coerceIn(0f, 1f)
            } else 0f
            return NowPlaying(tonie, playback.isPlaying, playback.isBuffering, progress)
        }

    /** Offline nur abspielbar, was komplett heruntergeladen ist. */
    fun isAvailable(item: LibraryItem): Boolean = isOnline || item.download == DownloadStatus.Completed
}

sealed interface LibraryIntent {
    data class TonieClicked(val tonieId: String) : LibraryIntent
    data object TogglePlayPause : LibraryIntent
    data object OpenPlayer : LibraryIntent
    data object Retry : LibraryIntent
    data object OpenParentArea : LibraryIntent
}

sealed interface LibraryResult {
    data class TonieData(val tonies: List<Tonie>, val downloads: Map<String, DownloadInfo>) : LibraryResult
    data class OnlineChanged(val isOnline: Boolean) : LibraryResult
    data class PlaybackChanged(val snapshot: PlaybackSnapshot) : LibraryResult
    data object RefreshStarted : LibraryResult
    data class RefreshFinished(val success: Boolean) : LibraryResult
}

sealed interface LibraryEffect {
    data object NavigateToPlayer : LibraryEffect
    data class ShakeTonie(val tonieId: String) : LibraryEffect
    data object NavigateToParentGate : LibraryEffect
}
