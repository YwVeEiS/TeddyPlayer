package xyz.weilandt.teddyapp.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import xyz.weilandt.teddyapp.domain.model.DownloadInfo
import xyz.weilandt.teddyapp.domain.model.PlaybackSnapshot
import xyz.weilandt.teddyapp.domain.model.Tonie

interface ToniesRepository {
    /** Tonies aus dem lokalen Cache: zuletzt gehörte zuerst, danach nach Serie und Titel, ohne Cover am Ende. */
    fun observeTonies(): Flow<List<Tonie>>

    /** `null` = noch unbekannt, sonst Ergebnis der letzten Server-Anfrage. */
    val isServerReachable: StateFlow<Boolean?>

    /** Lädt die Liste vom Server und aktualisiert den Cache. Liefert die Anzahl der Tonies. */
    suspend fun refresh(): Result<Int>

    /** Prüft eine Server-URL, ohne etwas zu speichern. Liefert die Anzahl der Tonies. */
    suspend fun testConnection(baseUrl: String): Result<Int>

    suspend fun getTonie(id: String): Tonie?

    /** Sucht den Tonie zu einer Figur (Tag-ID/ruid, kleingeschrieben). */
    suspend fun findByTagId(tagId: String): Tonie?
}

/** Liefert die IDs von Tonie-Figuren, die ans Gerät gehalten werden. */
interface TonieTagReader {
    val tagIds: Flow<String>
}

interface PlaybackProgressRepository {
    suspend fun getPosition(tonieId: String): Long
    suspend fun savePosition(tonieId: String, positionMs: Long)
    suspend fun markPlayed(tonieId: String)
}

interface SettingsRepository {
    val serverUrl: Flow<String>

    /** `false` bis Eltern bei der Ersteinrichtung eine Adresse gespeichert haben. */
    val isServerConfigured: Flow<Boolean>

    /** Titel unter Covern anzeigen (für Kinder, die schon lesen können). */
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

    /** Unterbrochene Downloads fortsetzen (z. B. nach App-Neustart). */
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
