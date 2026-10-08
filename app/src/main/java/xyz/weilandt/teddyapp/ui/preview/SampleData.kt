package xyz.weilandt.teddyapp.ui.preview

import xyz.weilandt.teddyapp.domain.model.Tonie

/** Beispieldaten für Previews (ohne Cover-URL, damit Platzhalter sichtbar sind). */
object SampleData {
    val bobo = Tonie(
        id = "bobo",
        title = "Bobo auf großer Reise",
        series = "Bobo Siebenschläfer",
        coverUrl = null,
        audioPath = "/content/download/A/B",
        chapterStartsMs = listOf(0L, 300_000L, 600_000L, 900_000L, 1_200_000L),
    )
    val conni = bobo.copy(id = "conni", title = "Conni backt Pizza", series = "Conni")
    val pikachu = bobo.copy(id = "pikachu", title = "Pikachu", series = "Pokémon")
    val frozen = bobo.copy(id = "frozen", title = "Die Eiskönigin", series = "Disney")
    val sandman = bobo.copy(id = "sandman", title = "Abends im Walde", series = "Unser Sandmännchen")
    val custom = bobo.copy(id = "custom", title = "Der verschwundene Apfelkuchen", series = "")

    val tonies = listOf(bobo, conni, pikachu, frozen, sandman, custom)
}
