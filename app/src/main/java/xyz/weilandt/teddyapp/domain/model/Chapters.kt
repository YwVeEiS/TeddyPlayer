package xyz.weilandt.teddyapp.domain.model

/** Reine Kapitel-Logik, genutzt vom Player-Service und der UI. */
object Chapters {

    /** Ab dieser Position springt "zurück" an den Kapitelanfang statt ins vorige Kapitel. */
    const val RESTART_THRESHOLD_MS = 3_000L

    fun indexAt(starts: List<Long>, positionMs: Long): Int {
        if (starts.isEmpty()) return 0
        val idx = starts.indexOfLast { it <= positionMs }
        return idx.coerceAtLeast(0)
    }

    /** Ziel für "vor" oder `null`, wenn bereits im letzten Kapitel. */
    fun nextStart(starts: List<Long>, positionMs: Long): Long? {
        val idx = indexAt(starts, positionMs)
        return starts.getOrNull(idx + 1)
    }

    /** Ziel für "zurück": Kapitelanfang, oder vorheriges Kapitel, wenn wir gerade erst begonnen haben. */
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

    /** Fortschritt innerhalb des aktuellen Kapitels (0..1). */
    fun progressInChapter(starts: List<Long>, positionMs: Long, durationMs: Long): Float {
        val idx = indexAt(starts, positionMs)
        val start = starts.getOrElse(idx) { 0L }
        // Die Gesamtdauer wird nur fürs letzte Kapitel gebraucht (bei Ogg anfangs oft unbekannt)
        val end = starts.getOrNull(idx + 1) ?: durationMs.takeIf { it > 0L } ?: return 0f
        val length = end - start
        if (length <= 0L) return 0f
        return ((positionMs - start).toFloat() / length).coerceIn(0f, 1f)
    }
}
