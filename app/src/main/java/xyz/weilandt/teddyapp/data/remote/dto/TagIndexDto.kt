package xyz.weilandt.teddyapp.data.remote.dto

import kotlinx.serialization.Serializable

/** Response of `GET /api/getTagIndex`. */
@Serializable
data class TagIndexDto(
    val tags: List<TagDto> = emptyList(),
)

@Serializable
data class TagDto(
    val ruid: String,
    val exists: Boolean = false,
    val hide: Boolean = false,
    val source: String = "",
    val audioUrl: String = "",
    val trackSeconds: List<Long> = emptyList(),
    val tonieInfo: TonieInfoDto? = null,
)

@Serializable
data class TonieInfoDto(
    val tracks: List<String> = emptyList(),
    val model: String = "",
    val series: String = "",
    val episode: String = "",
    val picture: String = "",
)
