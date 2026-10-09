package xyz.weilandt.teddyapp.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.weilandt.teddyapp.data.remote.TonieMapper
import xyz.weilandt.teddyapp.data.remote.dto.TagDto
import xyz.weilandt.teddyapp.data.remote.dto.TagIndexDto
import xyz.weilandt.teddyapp.data.remote.dto.TonieInfoDto

class TonieMapperTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val base = "http://teddy"

    /** Real response of a TeddyCloud server (93 tags, anonymized IDs). */
    private val fixture: TagIndexDto by lazy {
        val text = javaClass.classLoader!!.getResource("tag_index.json")!!.readText()
        json.decodeFromString(text)
    }

    @Test
    fun `real server response is filtered and deduplicated`() {
        val tonies = TonieMapper.map(fixture, base)

        assertEquals(93, fixture.tags.size)
        assertEquals(68, tonies.size)
        assertEquals("ids are unique", tonies.size, tonies.map { it.id }.toSet().size)
        assertTrue(tonies.none { it.series.startsWith("System sounds") })
        assertTrue(tonies.all { it.chapterStartsMs.first() == 0L })
    }

    @Test
    fun `duplicates keep the smallest ruid`() {
        val tonies = TonieMapper.map(fixture, base)
        // "Bobo auf großer Reise" is on four tags with the same audio file
        val bobo = tonies.filter { it.title == "Bobo auf großer Reise und weitere Folgen" }
        assertEquals(1, bobo.size)
        assertEquals("8732bcd8500304e0", bobo.single().id)
    }

    @Test
    fun `tags without audio, streams, hidden and system tags are skipped`() {
        val tags = listOf(
            tag("a", exists = false),
            tag("b", source = "http://radio/stream.mp3"),
            tag("c", hide = true),
            tag("0000000000000000"),
            tag("d", audioUrl = ""),
            tag("e", series = "System sounds en-gb"),
            tag("ok", source = ""),
            tag("ok2", source = "lib://by/audioID/1.taf"),
        )
        val ids = TonieMapper.map(TagIndexDto(tags), base).map { it.id }
        assertEquals(listOf("ok", "ok2"), ids)
    }

    @Test
    fun `cover urls are resolved and unknown pictures dropped`() {
        val tonies = TonieMapper.map(
            TagIndexDto(
                listOf(
                    tag("a", source = "lib://a.taf", picture = "/img_unknown.png"),
                    tag("b", source = "lib://b.taf", picture = "/cache/b.png"),
                    tag("c", source = "lib://c.taf", picture = "https://cdn/c.png"),
                )
            ),
            base,
        ).associateBy { it.id }
        assertNull(tonies.getValue("a").coverUrl)
        assertEquals("http://teddy/cache/b.png", tonies.getValue("b").coverUrl)
        assertEquals("https://cdn/c.png", tonies.getValue("c").coverUrl)
    }

    @Test
    fun `custom tonies get a title from the file name`() {
        val tonie = TonieMapper.map(
            TagIndexDto(listOf(tag("x", source = "lib://Der verschwundene Apfelkuchen.taf", episode = "", series = ""))),
            base,
        ).single()
        assertEquals("Der verschwundene Apfelkuchen", tonie.title)
        assertEquals("Der verschwundene Apfelkuchen", tonie.displayName)
    }

    @Test
    fun `numeric audio ids are not used as title`() {
        assertEquals("", TonieMapper.titleFromSource("lib://by/audioID/1731691506.taf"))
        assertFalse(TonieMapper.isPlayable(tag("x", source = "https://stream")))
    }

    @Test
    fun `first chapter always starts at zero`() {
        val tonies = TonieMapper.map(
            TagIndexDto(
                listOf(
                    tag("x", source = "lib://x.taf", trackSeconds = listOf(1, 18, 94)),
                    tag("y", source = "lib://y.taf", trackSeconds = emptyList()),
                )
            ),
            base,
        ).associateBy { it.id }
        assertEquals(listOf(0L, 18_000L, 94_000L), tonies.getValue("x").chapterStartsMs)
        assertEquals(listOf(0L), tonies.getValue("y").chapterStartsMs)
    }

    @Test
    fun `chapter seconds become sorted milliseconds`() {
        val tonie = TonieMapper.map(
            TagIndexDto(listOf(tag("x", source = "lib://x.taf", trackSeconds = listOf(0, 90, 45, 45)))),
            base,
        ).single()
        assertEquals(listOf(0L, 45_000L, 90_000L), tonie.chapterStartsMs)
    }

    private fun tag(
        ruid: String,
        exists: Boolean = true,
        hide: Boolean = false,
        source: String = "lib://$ruid.taf",
        audioUrl: String = "/content/download/$ruid",
        series: String = "Serie",
        episode: String = "Folge $ruid",
        picture: String = "",
        trackSeconds: List<Long> = listOf(0),
    ) = TagDto(
        ruid = ruid,
        exists = exists,
        hide = hide,
        source = source,
        audioUrl = audioUrl,
        trackSeconds = trackSeconds,
        tonieInfo = TonieInfoDto(series = series, episode = episode, picture = picture),
    )
}
