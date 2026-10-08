package xyz.weilandt.teddyapp.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.weilandt.teddyapp.data.remote.TeddyCloudApi
import xyz.weilandt.teddyapp.data.repository.ToniesRepositoryImpl
import xyz.weilandt.teddyapp.fakes.FakeSettingsRepository
import xyz.weilandt.teddyapp.fakes.FakeTonieDao

class ToniesRepositoryImplTest {

    private val fixture = javaClass.classLoader!!.getResource("tag_index.json")!!.readText()
    private val requestedUrls = mutableListOf<String>()
    private val prefetched = mutableListOf<String>()
    private var failRequests = false

    private val client = HttpClient(MockEngine { request ->
        requestedUrls += request.url.toString()
        if (failRequests) {
            respond("", HttpStatusCode.ServiceUnavailable)
        } else {
            respond(fixture, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "text/json"))
        }
    }) {
        expectSuccess = true
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }, contentType = ContentType.Any) }
    }

    private val dao = FakeTonieDao()
    private val settings = FakeSettingsRepository("http://teddy")
    private val repository = ToniesRepositoryImpl(TeddyCloudApi(client), dao, settings) { prefetched += it }

    @Test
    fun `refresh stores tonies and marks server reachable`() = runTest {
        assertNull(repository.isServerReachable.value)

        val result = repository.refresh()

        assertEquals(68, result.getOrThrow())
        assertEquals(68, repository.observeTonies().first().size)
        assertEquals(true, repository.isServerReachable.value)
        assertEquals("http://teddy/api/getTagIndex", requestedUrls.single())
        assertTrue(prefetched.isNotEmpty())
    }

    @Test
    fun `failed refresh keeps cached tonies and marks server unreachable`() = runTest {
        repository.refresh()
        failRequests = true

        val result = repository.refresh()

        assertTrue(result.isFailure)
        assertEquals(false, repository.isServerReachable.value)
        assertEquals(68, repository.observeTonies().first().size)
    }

    @Test
    fun `testConnection uses given url and does not store anything`() = runTest {
        val result = repository.testConnection("http://other")

        assertEquals(68, result.getOrThrow())
        assertEquals("http://other/api/getTagIndex", requestedUrls.single())
        assertTrue(dao.entities.value.isEmpty())
        assertNull(repository.isServerReachable.value)
    }

    @Test
    fun `getTonie returns cached tonie`() = runTest {
        repository.refresh()
        val tonie = repository.getTonie("8732bcd8500304e0")
        assertEquals("Bobo Siebenschläfer", tonie?.series)
        assertFalse(tonie?.chapterStartsMs.isNullOrEmpty())
    }
}
