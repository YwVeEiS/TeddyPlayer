package xyz.weilandt.teddyapp.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import xyz.weilandt.teddyapp.domain.model.Chapters
import xyz.weilandt.teddyapp.domain.model.PlaybackSnapshot
import xyz.weilandt.teddyapp.domain.model.ServerUrl
import xyz.weilandt.teddyapp.domain.model.Tonie
import xyz.weilandt.teddyapp.domain.repository.DownloadRepository
import xyz.weilandt.teddyapp.domain.repository.PlaybackController
import xyz.weilandt.teddyapp.domain.repository.PlaybackProgressRepository
import xyz.weilandt.teddyapp.domain.repository.SettingsRepository

/**
 * Connects the app to the [PlaybackService] via a [MediaController],
 * remembers the position per tonie and triggers the download when playing.
 *
 * @param scope must run on the main thread (MediaController requirement)
 */
class MediaPlaybackController(
    private val context: Context,
    private val progress: PlaybackProgressRepository,
    private val downloads: DownloadRepository,
    private val settings: SettingsRepository,
    private val scope: CoroutineScope,
) : PlaybackController {

    private val _state = MutableStateFlow(PlaybackSnapshot())
    override val state: StateFlow<PlaybackSnapshot> = _state.asStateFlow()

    private var tickerJob: Job? = null
    private var lastSavedAt = 0L

    private val controller = scope.async(start = CoroutineStart.LAZY) {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        MediaController.Builder(context, token).buildAsync().await().also { it.addListener(listener) }
    }

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            publish(player)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) startTicker() else scope.launch { saveCurrentPosition() }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                // Stop at the end; the next tap starts the tonie from the beginning.
                val id = _state.value.tonieId ?: return
                scope.launch { progress.savePosition(id, 0L) }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            _state.value = _state.value.copy(hasError = true, isPlaying = false, isBuffering = false)
        }
    }

    override fun play(tonie: Tonie) {
        scope.launch {
            val c = controller.await()
            val sameTonie = c.currentMediaItem?.mediaId == tonie.id
            val canResume = sameTonie && c.playerError == null &&
                c.playbackState != Player.STATE_ENDED && c.playbackState != Player.STATE_IDLE
            if (canResume) {
                c.play()
            } else {
                saveCurrentPosition()
                val baseUrl = settings.serverUrl.first()
                val startAt = progress.getPosition(tonie.id)
                c.setMediaItem(mediaItem(tonie, baseUrl), startAt)
                c.prepare()
                c.play()
            }
            progress.markPlayed(tonie.id)
            downloads.download(tonie)
        }
    }

    override fun togglePlayPause() = withController { c ->
        when {
            c.isPlaying -> c.pause()
            c.playbackState == Player.STATE_ENDED -> {
                c.seekTo(0L)
                c.play()
            }
            c.playbackState == Player.STATE_IDLE -> {
                c.prepare()
                c.play()
            }
            else -> c.play()
        }
    }

    // Deliberately seekTo instead of seekToNext: the MediaController doesn't forward
    // seekToNext to the session for a single file.
    override fun nextChapter() = withController { c ->
        Chapters.nextStart(_state.value.chapterStartsMs, c.currentPosition)?.let(c::seekTo)
    }

    override fun previousChapter() = withController { c ->
        c.seekTo(Chapters.previousTarget(_state.value.chapterStartsMs, c.currentPosition))
    }

    override fun seekToChapter(index: Int) = withController { c ->
        _state.value.chapterStartsMs.getOrNull(index)?.let(c::seekTo)
    }

    override fun stop() = withController { c ->
        scope.launch { saveCurrentPosition() }
        c.stop()
        c.clearMediaItems()
    }

    private fun withController(block: (MediaController) -> Unit) {
        scope.launch { block(controller.await()) }
    }

    private fun publish(player: Player) {
        val item = player.currentMediaItem
        _state.value = PlaybackSnapshot(
            tonieId = item?.mediaId,
            isPlaying = player.isPlaying,
            isBuffering = player.playbackState == Player.STATE_BUFFERING,
            positionMs = player.currentPosition.coerceAtLeast(0L),
            durationMs = player.duration.takeIf { it > 0L } ?: 0L,
            chapterStartsMs = item?.mediaMetadata?.extras
                ?.getLongArray(ChapterPlayer.KEY_CHAPTERS)?.toList().orEmpty(),
            hasError = player.playerError != null,
        )
    }

    private fun startTicker() {
        if (tickerJob?.isActive == true) return
        tickerJob = scope.launch {
            val c = controller.await()
            while (isActive && c.isPlaying) {
                publish(c)
                val now = System.currentTimeMillis()
                if (now - lastSavedAt >= SAVE_INTERVAL_MS) saveCurrentPosition()
                delay(TICK_MS)
            }
        }
    }

    private suspend fun saveCurrentPosition() {
        if (!controller.isCompleted) return
        val c = controller.await()
        val id = c.currentMediaItem?.mediaId ?: return
        if (c.playbackState == Player.STATE_ENDED) return
        lastSavedAt = System.currentTimeMillis()
        progress.savePosition(id, c.currentPosition.coerceAtLeast(0L))
    }

    private fun mediaItem(tonie: Tonie, baseUrl: String): MediaItem {
        val uri = ServerUrl.resolve(baseUrl, tonie.audioPath).toUri()
        return MediaItem.Builder()
            .setMediaId(tonie.id)
            .setUri(uri)
            .setCustomCacheKey(tonie.id)
            .setMimeType(MimeTypes.AUDIO_OGG)
            .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(uri).build())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(tonie.title.ifBlank { tonie.series })
                    .setArtist(tonie.series)
                    .setArtworkUri(tonie.coverUrl?.toUri())
                    .setExtras(bundleOf(ChapterPlayer.KEY_CHAPTERS to tonie.chapterStartsMs.toLongArray()))
                    .build()
            )
            .build()
    }

    private companion object {
        const val TICK_MS = 500L
        const val SAVE_INTERVAL_MS = 5_000L
    }
}
