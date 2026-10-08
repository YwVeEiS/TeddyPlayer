package xyz.weilandt.teddyapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import xyz.weilandt.teddyapp.domain.model.Chapters

class ChaptersTest {

    private val starts = listOf(0L, 60_000L, 120_000L, 180_000L)

    @Test
    fun `indexAt finds current chapter`() {
        assertEquals(0, Chapters.indexAt(starts, 0L))
        assertEquals(0, Chapters.indexAt(starts, 59_999L))
        assertEquals(1, Chapters.indexAt(starts, 60_000L))
        assertEquals(3, Chapters.indexAt(starts, 500_000L))
        assertEquals(0, Chapters.indexAt(emptyList(), 10_000L))
    }

    @Test
    fun `nextStart jumps to following chapter or null at the end`() {
        assertEquals(60_000L, Chapters.nextStart(starts, 5_000L))
        assertEquals(180_000L, Chapters.nextStart(starts, 150_000L))
        assertNull(Chapters.nextStart(starts, 190_000L))
    }

    @Test
    fun `previousTarget restarts chapter after threshold`() {
        assertEquals(60_000L, Chapters.previousTarget(starts, 70_000L))
    }

    @Test
    fun `previousTarget goes to previous chapter right after chapter start`() {
        assertEquals(0L, Chapters.previousTarget(starts, 61_000L))
    }

    @Test
    fun `previousTarget stays at zero in first chapter`() {
        assertEquals(0L, Chapters.previousTarget(starts, 1_000L))
        assertEquals(0L, Chapters.previousTarget(emptyList(), 1_000L))
    }

    @Test
    fun `progressInChapter uses next chapter start or duration as end`() {
        assertEquals(0.5f, Chapters.progressInChapter(starts, 90_000L, 240_000L), 0.001f)
        assertEquals(0.5f, Chapters.progressInChapter(starts, 210_000L, 240_000L), 0.001f)
        assertEquals(0f, Chapters.progressInChapter(starts, 10_000L, 0L), 0.001f)
    }
}
