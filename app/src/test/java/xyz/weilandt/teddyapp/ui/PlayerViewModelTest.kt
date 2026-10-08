package xyz.weilandt.teddyapp.ui

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import xyz.weilandt.teddyapp.domain.model.PlaybackSnapshot
import xyz.weilandt.teddyapp.fakes.FakePlaybackController
import xyz.weilandt.teddyapp.fakes.FakeSettingsRepository
import xyz.weilandt.teddyapp.fakes.FakeToniesRepository
import xyz.weilandt.teddyapp.fakes.MainDispatcherRule
import xyz.weilandt.teddyapp.fakes.tonie
import xyz.weilandt.teddyapp.ui.player.PlayerEffect
import xyz.weilandt.teddyapp.ui.player.PlayerIntent
import xyz.weilandt.teddyapp.ui.player.PlayerViewModel

class PlayerViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val a = tonie("a", chapters = listOf(0L, 10_000L, 20_000L))
    private val playback = FakePlaybackController()
    private val settings = FakeSettingsRepository()
    private val vm by lazy { PlayerViewModel(playback, FakeToniesRepository(listOf(a)), settings) }

    @Test
    fun `state shows nothing while nothing plays`() {
        assertNull(vm.state.value.tonie)
    }

    @Test
    fun `state reflects playback with chapters`() {
        playback.state.value = PlaybackSnapshot(
            tonieId = "a",
            isPlaying = true,
            positionMs = 15_000L,
            durationMs = 30_000L,
            chapterStartsMs = a.chapterStartsMs,
        )
        val state = vm.state.value
        assertEquals(a, state.tonie)
        assertTrue(state.isPlaying)
        assertEquals(3, state.chapterCount)
        assertEquals(1, state.currentChapter)
        assertEquals(0.5f, state.chapterProgress, 0.001f)
        assertTrue(state.canGoNext)
    }

    @Test
    fun `titles setting is reflected in state`() {
        assertFalse(vm.state.value.showTitles)
        settings.titles.value = true
        assertTrue(vm.state.value.showTitles)
    }

    @Test
    fun `cannot go next in last chapter`() {
        playback.state.value = PlaybackSnapshot(
            tonieId = "a", positionMs = 25_000L, durationMs = 30_000L, chapterStartsMs = a.chapterStartsMs,
        )
        assertFalse(vm.state.value.canGoNext)
    }

    @Test
    fun `controls are forwarded to playback`() = runTest {
        vm.onIntent(PlayerIntent.TogglePlayPause)
        vm.onIntent(PlayerIntent.NextChapter)
        vm.onIntent(PlayerIntent.PreviousChapter)
        vm.onIntent(PlayerIntent.ChapterSelected(2))
        assertEquals(listOf("toggle", "next", "previous", "chapter:2"), playback.calls)

        vm.effects.test {
            vm.onIntent(PlayerIntent.Close)
            assertEquals(PlayerEffect.NavigateBack, awaitItem())
        }
    }
}
