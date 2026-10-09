package xyz.weilandt.teddyapp.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import xyz.weilandt.teddyapp.domain.model.TagIds

class TagIdsTest {

    @Test
    fun `bytes become lowercase hex`() {
        val bytes = byteArrayOf(0x4a, 0xcc.toByte(), 0xd8.toByte(), 0x9d.toByte(), 0x50, 0x03, 0x04, 0xe0.toByte())
        assertEquals("4accd89d500304e0", TagIds.toHex(bytes))
    }

    @Test
    fun `candidates contain delivered and reversed byte order`() {
        assertEquals(
            listOf("4accd89d500304e0", "e00403509dd8cc4a"),
            TagIds.candidates("4ACCD89D500304E0"),
        )
    }
}
