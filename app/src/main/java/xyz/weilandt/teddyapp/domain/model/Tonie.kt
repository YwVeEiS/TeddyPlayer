package xyz.weilandt.teddyapp.domain.model

/**
 * Ein abspielbarer Tonie. Mehrere Tags auf dem Server mit derselben Audiodatei
 * werden zu einem Tonie zusammengefasst.
 *
 * @param id stabile ID, zugleich Download- und Cache-Schlüssel
 * @param audioPath Pfad relativ zur Server-URL (z. B. `/content/download/…`)
 * @param coverUrl absolute URL oder `null`, wenn es kein Cover gibt
 * @param chapterStartsMs Startzeitpunkte der Kapitel, beginnend mit 0
 * @param tagIds alle Tag-IDs (ruid) mit diesem Inhalt – für das Erkennen per NFC
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
    /** Kurzer Titel für die Anzeige unter dem Cover. */
    val shortTitle: String
        get() = title.ifBlank { series }

    /** Name für Eltern/TalkBack – Kinder sehen nur das Cover. */
    val displayName: String
        get() = when {
            series.isBlank() -> title
            title.isBlank() || title == series -> series
            else -> "$series – $title"
        }
}
