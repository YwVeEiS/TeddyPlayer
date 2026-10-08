package xyz.weilandt.teddyapp.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import xyz.weilandt.teddyapp.domain.model.ServerUrl
import xyz.weilandt.teddyapp.fakes.FakeSettingsRepository
import xyz.weilandt.teddyapp.fakes.FakeToniesRepository
import xyz.weilandt.teddyapp.fakes.MainDispatcherRule
import xyz.weilandt.teddyapp.ui.parent.settings.ConnectionTest
import xyz.weilandt.teddyapp.ui.setup.ServerSetupIntent
import xyz.weilandt.teddyapp.ui.setup.ServerSetupViewModel

class ServerSetupViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val settings = FakeSettingsRepository(url = ServerUrl.DEFAULT, configured = false)
    private val tonies = FakeToniesRepository()
    private val vm by lazy { ServerSetupViewModel(settings, tonies) }

    @Test
    fun `input is prefilled with http tc`() {
        assertEquals("http://tc", vm.state.value.urlInput)
        assertEquals(ConnectionTest.Idle, vm.state.value.connection)
    }

    @Test
    fun `reachable server is saved`() {
        tonies.testResult = Result.success(68)
        vm.onIntent(ServerSetupIntent.UrlChanged("tc/web"))
        vm.onIntent(ServerSetupIntent.Connect)

        assertEquals(listOf("http://tc"), tonies.testedUrls)
        assertEquals("http://tc", settings.url.value)
        assertTrue(settings.configured.value)
        assertEquals(ConnectionTest.Success(68), vm.state.value.connection)
    }

    @Test
    fun `unreachable server is not saved but can be saved anyway`() {
        tonies.testResult = Result.failure(Exception())
        vm.onIntent(ServerSetupIntent.UrlChanged("10.0.0.7"))
        vm.onIntent(ServerSetupIntent.Connect)

        assertFalse(settings.configured.value)
        assertTrue(vm.state.value.canSaveAnyway)

        vm.onIntent(ServerSetupIntent.SaveAnyway)
        assertTrue(settings.configured.value)
        assertEquals("http://10.0.0.7", settings.url.value)
    }

    @Test
    fun `blank input is rejected`() {
        vm.onIntent(ServerSetupIntent.UrlChanged(" "))
        vm.onIntent(ServerSetupIntent.Connect)
        assertEquals(ConnectionTest.InvalidUrl, vm.state.value.connection)
        assertTrue(tonies.testedUrls.isEmpty())
        assertFalse(settings.configured.value)
    }

    @Test
    fun `editing resets the status`() {
        tonies.testResult = Result.failure(Exception())
        vm.onIntent(ServerSetupIntent.Connect)
        vm.onIntent(ServerSetupIntent.UrlChanged("http://tc2"))
        assertEquals(ConnectionTest.Idle, vm.state.value.connection)
    }
}
