package xyz.weilandt.teddyapp.ui

import app.cash.turbine.test
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import xyz.weilandt.teddyapp.domain.model.DownloadStatus
import xyz.weilandt.teddyapp.fakes.FakeDownloadRepository
import xyz.weilandt.teddyapp.fakes.FakeNetworkMonitor
import xyz.weilandt.teddyapp.fakes.FakePlaybackController
import xyz.weilandt.teddyapp.fakes.FakeTagReader
import xyz.weilandt.teddyapp.fakes.FakeToniesRepository
import xyz.weilandt.teddyapp.fakes.MainDispatcherRule
import xyz.weilandt.teddyapp.fakes.runViewModelTest
import xyz.weilandt.teddyapp.fakes.tonie
import xyz.weilandt.teddyapp.ui.nfc.NfcFeedback
import xyz.weilandt.teddyapp.ui.nfc.NfcPlaybackEffect
import xyz.weilandt.teddyapp.ui.nfc.NfcPlaybackViewModel

class NfcPlaybackViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    // Bobo is on two figures; the second one is a duplicate
    private val bobo = tonie("4accd89d500304e0").copy(tagIds = listOf("4accd89d500304e0", "aabbccdd500304e0"))
    private val tonies = FakeToniesRepository(listOf(bobo)).apply { isServerReachable.value = true }
    private val downloads = FakeDownloadRepository()
    private val playback = FakePlaybackController()
    private val network = FakeNetworkMonitor()
    private val reader = FakeTagReader()
    private var now = 0L

    private fun nfcTest(block: suspend TestScope.(NfcPlaybackViewModel) -> Unit) =
        runViewModelTest({ NfcPlaybackViewModel(tonies, downloads, playback, network, reader) { now } }, block)

    @Test
    fun `known figure plays and opens player`() = nfcTest { vm ->
        vm.effects.test {
            reader.tagIds.emit("4accd89d500304e0")
            assertEquals(NfcPlaybackEffect.NavigateToPlayer, awaitItem())
        }
        assertEquals(bobo, playback.played)
        assertNull(vm.state.value.feedback)
    }

    @Test
    fun `reversed byte order and duplicate figures are recognized`() = nfcTest { vm ->
        vm.effects.test {
            reader.tagIds.emit("E00403509DD8CC4A")
            assertEquals(NfcPlaybackEffect.NavigateToPlayer, awaitItem())
            now += 5_000
            reader.tagIds.emit("aabbccdd500304e0")
            assertEquals(NfcPlaybackEffect.NavigateToPlayer, awaitItem())
        }
        assertEquals(listOf("play:${bobo.id}", "play:${bobo.id}"), playback.calls)
    }

    @Test
    fun `unknown figure is rejected with question mark`() = nfcTest { vm ->
        vm.effects.test {
            reader.tagIds.emit("ffffffff500304e0")
            assertEquals(NfcPlaybackEffect.Rejected, awaitItem())
        }
        assertEquals(NfcFeedback.Unknown, vm.state.value.feedback)
        assertNull(playback.played)

        advanceTimeBy(NfcPlaybackViewModel.FEEDBACK_DURATION_MS + 1)
        assertNull(vm.state.value.feedback)
    }

    @Test
    fun `offline only downloaded tonies play`() = nfcTest { vm ->
        tonies.isServerReachable.value = false
        vm.effects.test {
            reader.tagIds.emit("4accd89d500304e0")
            assertEquals(NfcPlaybackEffect.Rejected, awaitItem())
            assertEquals(NfcFeedback.Unavailable, vm.state.value.feedback)

            downloads.set(bobo.id, DownloadStatus.Completed)
            now += 5_000
            reader.tagIds.emit("4accd89d500304e0")
            assertEquals(NfcPlaybackEffect.NavigateToPlayer, awaitItem())
        }
        assertEquals(bobo, playback.played)
    }

    @Test
    fun `figure held to the phone is only handled once`() = nfcTest { vm ->
        vm.effects.test {
            reader.tagIds.emit("4accd89d500304e0")
            assertEquals(NfcPlaybackEffect.NavigateToPlayer, awaitItem())
            now += 500
            reader.tagIds.emit("4accd89d500304e0")
            expectNoEvents()
        }
        assertEquals(1, playback.calls.size)
    }

    @Test
    fun `no network means offline`() = nfcTest { vm ->
        network.available.value = false
        vm.effects.test {
            reader.tagIds.emit("4accd89d500304e0")
            assertEquals(NfcPlaybackEffect.Rejected, awaitItem())
        }
        assertTrue(playback.calls.isEmpty())
    }
}
