package xyz.weilandt.teddyapp.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import xyz.weilandt.teddyapp.data.local.TonieDao
import xyz.weilandt.teddyapp.data.local.TonieEntity
import xyz.weilandt.teddyapp.data.remote.TeddyCloudApi
import xyz.weilandt.teddyapp.data.remote.TonieMapper
import xyz.weilandt.teddyapp.domain.model.Tonie
import xyz.weilandt.teddyapp.domain.repository.SettingsRepository
import xyz.weilandt.teddyapp.domain.repository.ToniesRepository

/** Wird nach erfolgreichem Laden aufgerufen, z. B. um Cover für offline vorzuladen. */
fun interface CoverPrefetcher {
    fun prefetch(urls: List<String>)
}

class ToniesRepositoryImpl(
    private val api: TeddyCloudApi,
    private val dao: TonieDao,
    private val settings: SettingsRepository,
    private val coverPrefetcher: CoverPrefetcher,
) : ToniesRepository {

    private val _isServerReachable = MutableStateFlow<Boolean?>(null)
    override val isServerReachable: StateFlow<Boolean?> = _isServerReachable.asStateFlow()

    override fun observeTonies(): Flow<List<Tonie>> =
        dao.observeAll().map { list -> list.map(TonieEntity::toDomain) }

    override suspend fun refresh(): Result<Int> {
        val baseUrl = settings.serverUrl.first()
        return fetch(baseUrl)
            .onSuccess { tonies ->
                dao.replaceAll(tonies.map(TonieEntity::from))
                coverPrefetcher.prefetch(tonies.mapNotNull { it.coverUrl })
            }
            .also { _isServerReachable.value = it.isSuccess }
            .map { it.size }
    }

    override suspend fun testConnection(baseUrl: String): Result<Int> =
        fetch(baseUrl).map { it.size }

    override suspend fun getTonie(id: String): Tonie? = dao.get(id)?.toDomain()

    override suspend fun findByTagId(tagId: String): Tonie? = dao.findByTagId(tagId.lowercase())?.toDomain()

    private suspend fun fetch(baseUrl: String): Result<List<Tonie>> =
        try {
            Result.success(TonieMapper.map(api.getTagIndex(baseUrl), baseUrl))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
}
