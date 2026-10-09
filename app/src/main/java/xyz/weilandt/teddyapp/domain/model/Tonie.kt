package xyz.weilandt.teddyapp.domain.model

/**
 * A playable tonie. Several tags on the server sharing the same audio file are merged
 * into one tonie.
 *
 * @param id stable ID, also used as download and cache key
 * @param audioPath path relative to the server URL (e.g. `/content/download/…`)
 * @param coverUrl absolute URL, or `null` if there is no cover
 * @param chapterStartsMs chapter start times, beginning with 0
 * @param tagIds all tag IDs (ruid) with this content – for NFC detection
 */
data class Tonie(
    val id: String,
    val title: String,
    val series: String,
    val coverUrl: String?,
    val audioPath: String,
    val chapterStartsMs: List<Long>,
    val tagIds: List<String> = emptyList(),
) {
    /** Short title shown below the cover. */
    val shortTitle: String
        get() = title.ifBlank { series }

    /** Name for parents/TalkBack – children only see the cover. */
    val displayName: String
        get() = when {
            series.isBlank() -> title
            title.isBlank() || title == series -> series
            else -> "$series – $title"
        }
}
