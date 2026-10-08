package xyz.weilandt.teddyapp.domain.model

/** Momentaufnahme der Wiedergabe, wie sie die UI braucht. */
data class PlaybackSnapshot(
    val tonieId: String? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val chapterStartsMs: List<Long> = emptyList(),
    val hasError: Boolean = false,
) {
    val currentChapter: Int get() = Chapters.indexAt(chapterStartsMs, positionMs)
    val chapterCount: Int get() = chapterStartsMs.size.coerceAtLeast(1)
    val chapterProgress: Float get() = Chapters.progressInChapter(chapterStartsMs, positionMs, durationMs)
}
