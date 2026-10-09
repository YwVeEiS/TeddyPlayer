package xyz.weilandt.teddyapp.domain.model

/** Pure chapter logic, used by the player service and the UI. */
object Chapters {

    /** Beyond this position, "previous" jumps to the chapter start instead of the previous chapter. */
    const val RESTART_THRESHOLD_MS = 3_000L

    fun indexAt(starts: List<Long>, positionMs: Long): Int {
        if (starts.isEmpty()) return 0
        val idx = starts.indexOfLast { it <= positionMs }
        return idx.coerceAtLeast(0)
    }

    /** Target for "next", or `null` when already in the last chapter. */
    fun nextStart(starts: List<Long>, positionMs: Long): Long? {
        val idx = indexAt(starts, positionMs)
        return starts.getOrNull(idx + 1)
    }

    /** Target for "previous": chapter start, or the previous chapter if we only just started. */
    fun previousTarget(starts: List<Long>, positionMs: Long): Long {
        if (starts.isEmpty()) return 0L
        val idx = indexAt(starts, positionMs)
        val currentStart = starts[idx]
        return if (positionMs - currentStart > RESTART_THRESHOLD_MS || idx == 0) {
            currentStart
        } else {
            starts[idx - 1]
        }
    }

    /** Progress within the current chapter (0..1). */
    fun progressInChapter(starts: List<Long>, positionMs: Long, durationMs: Long): Float {
        val idx = indexAt(starts, positionMs)
        val start = starts.getOrElse(idx) { 0L }
        // The total duration is only needed for the last chapter (often unknown at first for Ogg)
        val end = starts.getOrNull(idx + 1) ?: durationMs.takeIf { it > 0L } ?: return 0f
        val length = end - start
        if (length <= 0L) return 0f
        return ((positionMs - start).toFloat() / length).coerceIn(0f, 1f)
    }
}
