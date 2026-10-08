package xyz.weilandt.teddyapp.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import xyz.weilandt.teddyapp.data.remote.dto.TagIndexDto
import xyz.weilandt.teddyapp.domain.model.ServerUrl

class TeddyCloudApi(private val client: HttpClient) {

    suspend fun getTagIndex(baseUrl: String): TagIndexDto =
        client.get(ServerUrl.resolve(baseUrl, "/api/getTagIndex")).body()
}
