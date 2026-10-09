package xyz.weilandt.teddyapp.data.remote

import xyz.weilandt.teddyapp.data.remote.dto.TagDto
import xyz.weilandt.teddyapp.data.remote.dto.TagIndexDto
import xyz.weilandt.teddyapp.domain.model.ServerUrl
import xyz.weilandt.teddyapp.domain.model.Tonie

/**
 * Turns the tag index into the tonie list shown to children:
 * - only tags whose audio is stored on the server
 * - no streams, no system sounds, no hidden tags
 * - tags sharing the same audio file are merged
 */
object TonieMapper {

    private const val SYSTEM_RUID = "0000000000000000"
    private const val UNKNOWN_PICTURE = "img_unknown"

    fun map(dto: TagIndexDto, baseUrl: String): List<Tonie> =
        dto.tags
            .filter(::isPlayable)
            .groupBy(::audioKey)
            .values
            // stable: always the same tag of a group, regardless of server order
            .map { group -> group.minBy { it.ruid } }
            .map { toTonie(it, baseUrl) }

    internal fun isPlayable(tag: TagDto): Boolean {
        if (!tag.exists || tag.hide) return false
        if (tag.audioUrl.isBlank()) return false
        if (tag.ruid == SYSTEM_RUID) return false
        if (tag.tonieInfo?.series?.startsWith("System sounds", ignoreCase = true) == true) return false
        // Empty source = original content on the server, lib:// = library, everything else is a stream
        return tag.source.isEmpty() || tag.source.startsWith("lib://")
    }

    private fun audioKey(tag: TagDto): String = tag.source.ifEmpty { tag.ruid }

    private fun toTonie(tag: TagDto, baseUrl: String): Tonie {
        val info = tag.tonieInfo
        val series = info?.series.orEmpty().trim()
        val title = info?.episode.orEmpty().trim()
            .ifEmpty { titleFromSource(tag.source) }
            .ifEmpty { info?.model.orEmpty().trim() }
        return Tonie(
            id = tag.ruid,
            title = title,
            series = series,
            coverUrl = coverUrl(info?.picture, baseUrl),
            audioPath = tag.audioUrl,
            chapterStartsMs = chapterStarts(tag.trackSeconds),
        )
    }

    /** Seconds → ms; the first chapter always starts at 0 (some files start at 1 s). */
    private fun chapterStarts(trackSeconds: List<Long>): List<Long> {
        val starts = trackSeconds.distinct().sorted().map { it * 1000L }
        return listOf(0L) + starts.drop(1).filter { it > 0L }
    }

    private fun coverUrl(picture: String?, baseUrl: String): String? {
        if (picture.isNullOrBlank() || picture.contains(UNKNOWN_PICTURE)) return null
        return ServerUrl.resolve(baseUrl, picture)
    }

    /** `lib://Der verschwundene Apfelkuchen.taf` → `Der verschwundene Apfelkuchen` */
    internal fun titleFromSource(source: String): String {
        if (source.isEmpty()) return ""
        val name = source.substringAfterLast('/').substringBeforeLast(".taf")
        // Plain audio IDs like "1731691506" are not meaningful names
        return if (name.all { it.isDigit() }) "" else name.replace('_', ' ').trim()
    }
}
