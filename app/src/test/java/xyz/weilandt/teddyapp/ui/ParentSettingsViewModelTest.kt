package xyz.weilandt.teddyapp.ui

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import xyz.weilandt.teddyapp.domain.model.DownloadStatus
import xyz.weilandt.teddyapp.fakes.FakeDownloadRepository
import xyz.weilandt.teddyapp.fakes.FakeSettingsRepository
import xyz.weilandt.teddyapp.fakes.FakeToniesRepository
import xyz.weilandt.teddyapp.fakes.MainDispatcherRule
import xyz.weilandt.teddyapp.fakes.tonie
import xyz.weilandt.teddyapp.ui.parent.settings.ConnectionTest
import xyz.weilandt.teddyapp.ui.parent.settings.ParentSettingsEffect
import xyz.weilandt.teddyapp.ui.parent.settings.ParentSettingsIntent
import xyz.weilandt.teddyapp.ui.parent.settings.ParentSettingsViewModel

class ParentSettingsViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val settings = FakeSettingsRepository("http://old")
    private val tonies = FakeToniesRepository(listOf(tonie("a"), tonie("b")))
    private val downloads = FakeDownloadRepository()
    private val vm by lazy { ParentSettingsViewModel(settings, tonies, downloads) }

    @Test
    fun `shows saved url and download list`() {
        downloads.set("a", DownloadStatus.Completed, bytes = 100)
        downloads.set("unknown", DownloadStatus.Completed, bytes = 5)
        downloads.usedBytes.value = 105

        val state = vm.state.value
        assertEquals("http://old", state.serverUrlInput)
        assertFalse(state.isUrlChanged)
        assertEquals(2, state.tonieCount)
        assertEquals(listOf("a"), state.downloads.map { it.tonie.id })
        assertEquals(105, state.usedBytes)
    }

    @Test
    fun `saving a reachable url normalizes, stores and refreshes`() {
        tonies.testResult = Result.success(7)
        tonies.refreshResult = Result.success(7)
        vm.onIntent(ParentSettingsIntent.UrlChanged("10.0.0.5/web/"))
        assertTrue(vm.state.value.isUrlChanged)

        vm.onIntent(ParentSettingsIntent.SaveUrl)

        assertEquals(listOf("http://10.0.0.5"), tonies.testedUrls)
        assertEquals("http://10.0.0.5", settings.url.value)
        assertEquals(1, tonies.refreshCount)
        assertEquals(ConnectionTest.Success(7), vm.state.value.connection)
        assertFalse(vm.state.value.isUrlChanged)
    }

    @Test
    fun `unreachable url is not saved`() {
        tonies.testResult = Result.failure(Exception())
        vm.onIntent(ParentSettingsIntent.UrlChanged("10.0.0.9"))
        vm.onIntent(ParentSettingsIntent.SaveUrl)

        assertEquals("http://old", settings.url.value)
        assertEquals(ConnectionTest.Failed, vm.state.value.connection)
    }

    @Test
    fun `blank url is rejected`() {
        vm.onIntent(ParentSettingsIntent.UrlChanged("  "))
        vm.onIntent(ParentSettingsIntent.SaveUrl)
        assertEquals(ConnectionTest.InvalidUrl, vm.state.value.connection)
        assertTrue(tonies.testedUrls.isEmpty())
    }

    @Test
    fun `delete all needs confirmation`() {
        vm.onIntent(ParentSettingsIntent.DeleteAllRequested)
        assertTrue(vm.state.value.confirmDeleteAll)
        assertFalse(downloads.removedAll)

        vm.onIntent(ParentSettingsIntent.DeleteAllConfirmed)
        assertTrue(downloads.removedAll)
        assertFalse(vm.state.value.confirmDeleteAll)
    }

    @Test
    fun `titles can be switched on and off`() {
        vm.onIntent(ParentSettingsIntent.ShowTitlesChanged(true))
        assertTrue(settings.titles.value)
        assertTrue(vm.state.value.showTitles)

        vm.onIntent(ParentSettingsIntent.ShowTitlesChanged(false))
        assertFalse(vm.state.value.showTitles)
    }

    @Test
    fun `single download can be removed`() {
        vm.onIntent(ParentSettingsIntent.DeleteDownload("a"))
        assertEquals(listOf("a"), downloads.removed)
    }

    @Test
    fun `back navigates`() = runTest {
        vm.effects.test {
            vm.onIntent(ParentSettingsIntent.Back)
            assertEquals(ParentSettingsEffect.NavigateBack, awaitItem())
        }
    }
}
