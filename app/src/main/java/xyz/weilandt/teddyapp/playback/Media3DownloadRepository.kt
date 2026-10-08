package xyz.weilandt.teddyapp.playback

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.Cache
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.weilandt.teddyapp.domain.model.DownloadInfo
import xyz.weilandt.teddyapp.domain.model.DownloadStatus
import xyz.weilandt.teddyapp.domain.model.ServerUrl
import xyz.weilandt.teddyapp.domain.model.Tonie
import xyz.weilandt.teddyapp.domain.repository.DownloadRepository
import xyz.weilandt.teddyapp.domain.repository.SettingsRepository

/** Muss auf dem Main-Thread erzeugt werden, da der [DownloadManager] dort lebt. */
@OptIn(UnstableApi::class)
class Media3DownloadRepository(
    private val context: Context,
    private val downloadManager: DownloadManager,
    private val cache: Cache,
    private val settings: SettingsRepository,
    private val scope: CoroutineScope,
) : DownloadRepository {

    private val _downloads = MutableStateFlow<Map<String, DownloadInfo>>(emptyMap())
    override val downloads: StateFlow<Map<String, DownloadInfo>> = _downloads.asStateFlow()

    private val _usedBytes = MutableStateFlow(0L)
    override val usedBytes: StateFlow<Long> = _usedBytes.asStateFlow()

    private var progressJob: Job? = null

    init {
        downloadManager.addListener(object : DownloadManager.Listener {
            override fun onInitialized(downloadManager: DownloadManager) {
                loadAll()
            }

            override fun onDownloadChanged(
                downloadManager: DownloadManager,
                download: Download,
                finalException: Exception?,
            ) {
                _downloads.update { it + (download.request.id to download.toInfo()) }
                updateUsedBytes()
                ensureProgressPolling()
            }

            override fun onDownloadRemoved(downloadManager: DownloadManager, download: Download) {
                _downloads.update { it - download.request.id }
                updateUsedBytes()
            }
        })
        if (downloadManager.isInitialized) loadAll()
    }

    override suspend fun download(tonie: Tonie) {
        when (_downloads.value[tonie.id]?.status) {
            DownloadStatus.Completed, DownloadStatus.Queued, is DownloadStatus.Downloading -> return
            else -> Unit
        }
        val uri = ServerUrl.resolve(settings.serverUrl.first(), tonie.audioPath)
        val request = DownloadRequest.Builder(tonie.id, android.net.Uri.parse(uri))
            .setCustomCacheKey(tonie.id)
            .setMimeType(MimeTypes.AUDIO_OGG)
            .setData(tonie.displayName.toByteArray())
            .build()
        DownloadService.sendAddDownload(context, TeddyDownloadService::class.java, request, false)
    }

    override fun remove(tonieId: String) {
        DownloadService.sendRemoveDownload(context, TeddyDownloadService::class.java, tonieId, false)
    }

    override fun removeAll() {
        DownloadService.sendRemoveAllDownloads(context, TeddyDownloadService::class.java, false)
    }

    private fun loadAll() {
        scope.launch {
            val all = withContext(Dispatchers.IO) {
                buildMap {
                    downloadManager.downloadIndex.getDownloads().use { cursor ->
                        while (cursor.moveToNext()) {
                            val download = cursor.download
                            put(download.request.id, download.toInfo())
                        }
                    }
                }
            }
            _downloads.value = all
            updateUsedBytes()
            ensureProgressPolling()
        }
    }

    /** Der DownloadManager meldet keinen Fortschritt – solange etwas lädt, fragen wir ihn ab. */
    private fun ensureProgressPolling() {
        if (progressJob?.isActive == true) return
        if (downloadManager.currentDownloads.none { it.state == Download.STATE_DOWNLOADING }) return
        progressJob = scope.launch {
            while (isActive) {
                val running = downloadManager.currentDownloads
                if (running.none { it.state == Download.STATE_DOWNLOADING }) break
                _downloads.update { current ->
                    current + running.associate { it.request.id to it.toInfo() }
                }
                updateUsedBytes()
                delay(PROGRESS_INTERVAL_MS)
            }
        }
    }

    private fun updateUsedBytes() {
        scope.launch(Dispatchers.IO) { _usedBytes.value = cache.cacheSpace }
    }

    private fun Download.toInfo() = DownloadInfo(
        tonieId = request.id,
        status = when (state) {
            Download.STATE_COMPLETED -> DownloadStatus.Completed
            Download.STATE_DOWNLOADING -> DownloadStatus.Downloading(
                percentDownloaded.takeIf { it >= 0f }?.div(100f)
            )
            Download.STATE_QUEUED, Download.STATE_RESTARTING -> DownloadStatus.Queued
            Download.STATE_FAILED -> DownloadStatus.Failed
            else -> DownloadStatus.None
        },
        bytesDownloaded = bytesDownloaded,
    )

    private companion object {
        const val PROGRESS_INTERVAL_MS = 500L
    }
}
