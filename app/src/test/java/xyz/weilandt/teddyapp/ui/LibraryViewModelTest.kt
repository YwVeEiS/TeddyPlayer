package xyz.weilandt.teddyapp.ui

import app.cash.turbine.test
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import xyz.weilandt.teddyapp.domain.model.DownloadStatus
import xyz.weilandt.teddyapp.domain.model.PlaybackSnapshot
import xyz.weilandt.teddyapp.fakes.FakeDownloadRepository
import xyz.weilandt.teddyapp.fakes.FakeNetworkMonitor
import xyz.weilandt.teddyapp.fakes.FakePlaybackController
import xyz.weilandt.teddyapp.fakes.FakeSettingsRepository
import xyz.weilandt.teddyapp.fakes.FakeToniesRepository
import xyz.weilandt.teddyapp.fakes.MainDispatcherRule
import xyz.weilandt.teddyapp.fakes.runViewModelTest
import xyz.weilandt.teddyapp.fakes.tonie
import xyz.weilandt.teddyapp.ui.library.LibraryContent
import xyz.weilandt.teddyapp.ui.library.LibraryEffect
import xyz.weilandt.teddyapp.ui.library.LibraryIntent
import xyz.weilandt.teddyapp.ui.library.LibraryItem
import xyz.weilandt.teddyapp.ui.library.LibraryReducer
import xyz.weilandt.teddyapp.ui.library.LibraryResult
import xyz.weilandt.teddyapp.ui.library.LibraryState
import xyz.weilandt.teddyapp.ui.library.LibraryViewModel

class LibraryViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val a = tonie("a")
    private val b = tonie("b")
    private val tonies = FakeToniesRepository(listOf(a, b))
    private val downloads = FakeDownloadRepository()
    private val playback = FakePlaybackController()
    private val network = FakeNetworkMonitor()
    private val settings = FakeSettingsRepository()

    private fun libraryTest(block: suspend TestScope.(LibraryViewModel) -> Unit) =
        runViewModelTest({ LibraryViewModel(tonies, downloads, playback, network, settings) }, block)

    @Test
    fun `loads tonies and refreshes on start`() = libraryTest { vm ->
        assertEquals(LibraryContent.Content, vm.state.value.content)
        assertEquals(listOf("a", "b"), vm.state.value.items.map { it.tonie.id })
        assertEquals(1, tonies.refreshCount)
        assertTrue(vm.state.value.isOnline)
    }

    @Test
    fun `clicking available tonie plays it and opens player`() = libraryTest { vm ->
        vm.effects.test {
            vm.onIntent(LibraryIntent.TonieClicked("a"))
            assertEquals(LibraryEffect.NavigateToPlayer, awaitItem())
        }
        assertEquals(a, playback.played)
    }

    @Test
    fun `offline only downloaded tonies are playable, others shake`() {
        tonies.refreshResult = Result.failure(Exception("offline"))
        downloads.set("b", DownloadStatus.Completed)
        libraryTest { vm ->
            assertFalse(vm.state.value.isOnline)
            vm.effects.test {
                vm.onIntent(LibraryIntent.TonieClicked("a"))
                assertEquals(LibraryEffect.ShakeTonie("a"), awaitItem())
                vm.onIntent(LibraryIntent.TonieClicked("b"))
                assertEquals(LibraryEffect.NavigateToPlayer, awaitItem())
            }
            assertEquals(b, playback.played)
        }
    }

    @Test
    fun `retries periodically while server is unreachable`() {
        tonies.refreshResult = Result.failure(Exception("offline"))
        libraryTest {
            assertEquals(1, tonies.refreshCount)

            advanceTimeBy(LibraryViewModel.RETRY_INTERVAL_MS + 1)
            assertEquals(2, tonies.refreshCount)

            tonies.refreshResult = Result.success(2)
            advanceTimeBy(LibraryViewModel.RETRY_INTERVAL_MS + 1)
            assertEquals(3, tonies.refreshCount)

            advanceTimeBy(LibraryViewModel.RETRY_INTERVAL_MS * 3)
            assertEquals(3, tonies.refreshCount)
        }
    }

    @Test
    fun `no network means offline without refresh`() {
        network.available.value = false
        libraryTest { vm ->
            assertFalse(vm.state.value.isOnline)
            assertEquals(0, tonies.refreshCount)

            network.available.value = true
            assertEquals(1, tonies.refreshCount)
            assertTrue(vm.state.value.isOnline)
        }
    }

    @Test
    fun `first start asks for server and loads only after setup`() {
        settings.configured.value = false
        libraryTest { vm ->
            assertTrue(vm.state.value.needsServerSetup)
            assertEquals(0, tonies.refreshCount)

            vm.onIntent(LibraryIntent.Retry)
            assertEquals(0, tonies.refreshCount)

            settings.setServerUrl("http://tc")
            assertFalse(vm.state.value.needsServerSetup)
            assertEquals(1, tonies.refreshCount)
        }
    }

    @Test
    fun `titles setting is reflected in state`() = libraryTest { vm ->
        assertFalse(vm.state.value.showTitles)
        settings.titles.value = true
        assertTrue(vm.state.value.showTitles)
    }

    @Test
    fun `mini player progress falls back to chapters when duration is unknown`() = libraryTest { vm ->
        playback.state.value = PlaybackSnapshot(
            tonieId = "a",
            positionMs = 15_000L,
            durationMs = 0L,
            chapterStartsMs = a.chapterStartsMs,
        )
        // Kapitel 2 von 3, zur Hälfte gehört → (1 + 0.5) / 3
        assertEquals(0.5f, vm.state.value.nowPlaying!!.progress, 0.001f)
    }

    @Test
    fun `download status and playback appear in state`() = libraryTest { vm ->
        downloads.set("a", DownloadStatus.Downloading(0.5f))
        playback.state.value = PlaybackSnapshot(tonieId = "a", isPlaying = true, positionMs = 50, durationMs = 100)

        val state = vm.state.value
        assertEquals(DownloadStatus.Downloading(0.5f), state.items.first { it.tonie.id == "a" }.download)
        assertEquals(a, state.nowPlaying?.tonie)
        assertEquals(0.5f, state.nowPlaying!!.progress, 0.001f)
    }

    @Test
    fun `mini player and parent intents are forwarded`() = libraryTest { vm ->
        vm.effects.test {
            vm.onIntent(LibraryIntent.OpenPlayer)
            assertEquals(LibraryEffect.NavigateToPlayer, awaitItem())
            vm.onIntent(LibraryIntent.OpenParentArea)
            assertEquals(LibraryEffect.NavigateToParentGate, awaitItem())
        }
        vm.onIntent(LibraryIntent.TogglePlayPause)
        assertEquals(listOf("toggle"), playback.calls)
    }

    // region Reducer

    @Test
    fun `content is loading until cache arrives`() {
        assertEquals(LibraryContent.Loading, LibraryState().content)
    }

    @Test
    fun `content stays loading while server setup is pending`() {
        val state = LibraryState(hasLoadedCache = true, needsServerSetup = true, lastRefreshFailed = true)
        assertEquals(LibraryContent.Loading, state.content)
    }

    @Test
    fun `empty cache after failed refresh is an error`() {
        val state = listOf(
            LibraryResult.TonieData(emptyList(), emptyMap()),
            LibraryResult.RefreshStarted,
            LibraryResult.RefreshFinished(success = false),
        ).fold(LibraryState(), LibraryReducer::reduce)
        assertEquals(LibraryContent.Error, state.content)
    }

    @Test
    fun `empty cache after successful refresh is empty`() {
        val state = listOf(
            LibraryResult.TonieData(emptyList(), emptyMap()),
            LibraryResult.RefreshFinished(success = true),
        ).fold(LibraryState(), LibraryReducer::reduce)
        assertEquals(LibraryContent.Empty, state.content)
    }

    @Test
    fun `now playing is null for unknown tonie`() {
        val state = LibraryState(
            items = listOf(LibraryItem(a, DownloadStatus.None)),
            playback = PlaybackSnapshot(tonieId = "unknown"),
        )
        assertNull(state.nowPlaying)
    }

    // endregion
}
