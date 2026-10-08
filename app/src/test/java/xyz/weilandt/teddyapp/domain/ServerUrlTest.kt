package xyz.weilandt.teddyapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import xyz.weilandt.teddyapp.domain.model.ServerUrl

class ServerUrlTest {

    @Test
    fun `normalize adds scheme and strips web suffix`() {
        assertEquals("http://192.168.1.50", ServerUrl.normalize("192.168.1.50/web/"))
        assertEquals("http://192.168.1.50", ServerUrl.normalize(" http://192.168.1.50/web "))
        assertEquals("https://teddy.example.org", ServerUrl.normalize("https://teddy.example.org/"))
        assertEquals("http://host:8080/teddy", ServerUrl.normalize("host:8080/teddy"))
    }

    @Test
    fun `normalize rejects blank input`() {
        assertNull(ServerUrl.normalize("   "))
    }

    @Test
    fun `resolve joins paths and keeps absolute urls`() {
        assertEquals("http://h/api/x", ServerUrl.resolve("http://h/", "/api/x"))
        assertEquals("http://h/api/x", ServerUrl.resolve("http://h", "api/x"))
        assertEquals("https://cdn/img.png", ServerUrl.resolve("http://h", "https://cdn/img.png"))
    }
}
