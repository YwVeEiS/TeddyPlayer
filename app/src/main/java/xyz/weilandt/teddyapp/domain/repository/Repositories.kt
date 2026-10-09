package xyz.weilandt.teddyapp.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import xyz.weilandt.teddyapp.domain.model.DownloadInfo
import xyz.weilandt.teddyapp.domain.model.PlaybackSnapshot
import xyz.weilandt.teddyapp.domain.model.Tonie

interface ToniesRepository {
    /** Tonies from the local cache: recently played first, then by series and title, those without cover last. */
    fun observeTonies(): Flow<List<Tonie>>

    /** `null` = not known yet, otherwise the result of the last server request. */
    val isServerReachable: StateFlow<Boolean?>

    /** Loads the list from the server and updates the cache. Returns the number of tonies. */
    suspend fun refresh(): Result<Int>

    /** Checks a server URL without saving anything. Returns the number of tonies. */
    suspend fun testConnection(baseUrl: String): Result<Int>

    suspend fun getTonie(id: String): Tonie?
}

interface PlaybackProgressRepository {
    suspend fun getPosition(tonieId: String): Long
    suspend fun savePosition(tonieId: String, positionMs: Long)
    suspend fun markPlayed(tonieId: String)
}

interface SettingsRepository {
    val serverUrl: Flow<String>

    /** `false` until parents have saved an address during the initial setup. */
    val isServerConfigured: Flow<Boolean>

    /** Show titles below covers (for children who can already read). */
    val showTitles: Flow<Boolean>

    suspend fun setServerUrl(url: String)
    suspend fun setShowTitles(show: Boolean)
}

interface DownloadRepository {
    val downloads: StateFlow<Map<String, DownloadInfo>>
    val usedBytes: StateFlow<Long>
    suspend fun download(tonie: Tonie)
    fun remove(tonieId: String)
    fun removeAll()

    /** Resume interrupted downloads (e.g. after an app restart). */
    fun resumePending()
}

interface PlaybackController {
    val state: StateFlow<PlaybackSnapshot>
    fun play(tonie: Tonie)
    fun togglePlayPause()
    fun nextChapter()
    fun previousChapter()
    fun seekToChapter(index: Int)
    fun stop()
}

interface NetworkMonitor {
    val isNetworkAvailable: Flow<Boolean>
}
