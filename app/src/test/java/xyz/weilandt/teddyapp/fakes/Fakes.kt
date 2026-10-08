package xyz.weilandt.teddyapp.fakes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.Dispatchers
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import xyz.weilandt.teddyapp.data.local.TonieDao
import xyz.weilandt.teddyapp.data.local.TonieEntity
import xyz.weilandt.teddyapp.domain.model.DownloadInfo
import xyz.weilandt.teddyapp.domain.model.DownloadStatus
import xyz.weilandt.teddyapp.domain.model.PlaybackSnapshot
import xyz.weilandt.teddyapp.domain.model.Tonie
import xyz.weilandt.teddyapp.domain.repository.DownloadRepository
import xyz.weilandt.teddyapp.domain.repository.NetworkMonitor
import xyz.weilandt.teddyapp.domain.repository.PlaybackController
import xyz.weilandt.teddyapp.domain.repository.SettingsRepository
import xyz.weilandt.teddyapp.domain.repository.ToniesRepository

class MainDispatcherRule(
    val dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}

/**
 * Führt einen Test mit einem ViewModel aus und beendet danach dessen viewModelScope –
 * sonst würden Endlosschleifen (z. B. Retry) das Ende von runTest blockieren.
 */
inline fun <reified VM : ViewModel> runViewModelTest(
    crossinline create: () -> VM,
    crossinline block: suspend TestScope.(VM) -> Unit,
) = runTest {
    val store = ViewModelStore()
    val vm = ViewModelProvider.create(store, viewModelFactory { initializer { create() } })[VM::class]
    try {
        block(vm)
    } finally {
        store.clear()
    }
}

fun tonie(id: String, series: String = "Serie $id", chapters: List<Long> = listOf(0L, 10_000L, 20_000L)) = Tonie(
    id = id,
    title = "Titel $id",
    series = series,
    coverUrl = null,
    audioPath = "/content/download/$id",
    chapterStartsMs = chapters,
)

class FakeToniesRepository(initial: List<Tonie> = emptyList()) : ToniesRepository {
    val tonies = MutableStateFlow(initial)
    override val isServerReachable = MutableStateFlow<Boolean?>(null)
    var refreshResult: Result<Int> = Result.success(initial.size)
    var testResult: Result<Int> = Result.success(initial.size)
    var refreshCount = 0
    val testedUrls = mutableListOf<String>()

    override fun observeTonies(): Flow<List<Tonie>> = tonies

    override suspend fun refresh(): Result<Int> {
        refreshCount++
        isServerReachable.value = refreshResult.isSuccess
        return refreshResult
    }

    override suspend fun testConnection(baseUrl: String): Result<Int> {
        testedUrls += baseUrl
        return testResult
    }

    override suspend fun getTonie(id: String): Tonie? = tonies.value.firstOrNull { it.id == id }
}

class FakeDownloadRepository : DownloadRepository {
    override val downloads = MutableStateFlow<Map<String, DownloadInfo>>(emptyMap())
    override val usedBytes = MutableStateFlow(0L)
    val removed = mutableListOf<String>()
    var removedAll = false

    fun set(id: String, status: DownloadStatus, bytes: Long = 0L) {
        downloads.value = downloads.value + (id to DownloadInfo(id, status, bytes))
    }

    override suspend fun download(tonie: Tonie) = set(tonie.id, DownloadStatus.Queued)
    override fun remove(tonieId: String) { removed += tonieId }
    override fun removeAll() { removedAll = true }
    override fun resumePending() = Unit
}

class FakePlaybackController : PlaybackController {
    override val state = MutableStateFlow(PlaybackSnapshot())
    val calls = mutableListOf<String>()
    var played: Tonie? = null

    override fun play(tonie: Tonie) {
        played = tonie
        calls += "play:${tonie.id}"
    }
    override fun togglePlayPause() { calls += "toggle" }
    override fun nextChapter() { calls += "next" }
    override fun previousChapter() { calls += "previous" }
    override fun seekToChapter(index: Int) { calls += "chapter:$index" }
    override fun stop() { calls += "stop" }
}

class FakeNetworkMonitor(available: Boolean = true) : NetworkMonitor {
    val available = MutableStateFlow(available)
    override val isNetworkAvailable: Flow<Boolean> = this.available
}

class FakeSettingsRepository(url: String = "http://server", configured: Boolean = true) : SettingsRepository {
    val url = MutableStateFlow(url)
    val configured = MutableStateFlow(configured)
    val titles = MutableStateFlow(false)
    override val serverUrl: Flow<String> = this.url
    override val isServerConfigured: Flow<Boolean> = this.configured
    override val showTitles: Flow<Boolean> = titles
    override suspend fun setServerUrl(url: String) {
        this.url.value = url
        configured.value = true
    }
    override suspend fun setShowTitles(show: Boolean) { titles.value = show }
}

class FakeTonieDao : TonieDao {
    val entities = MutableStateFlow<List<TonieEntity>>(emptyList())
    override fun observeAll(): Flow<List<TonieEntity>> = entities
    override suspend fun get(id: String): TonieEntity? = entities.value.firstOrNull { it.id == id }
    override suspend fun deleteAll() { entities.value = emptyList() }
    override suspend fun insertAll(tonies: List<TonieEntity>) { entities.value = entities.value + tonies }
    override suspend fun replaceAll(tonies: List<TonieEntity>) { entities.value = tonies }
}
