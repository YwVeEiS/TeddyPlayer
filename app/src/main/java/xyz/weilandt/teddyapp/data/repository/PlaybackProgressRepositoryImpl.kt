package xyz.weilandt.teddyapp.data.repository

import xyz.weilandt.teddyapp.data.local.PlaybackProgressDao
import xyz.weilandt.teddyapp.data.local.PlaybackProgressEntity
import xyz.weilandt.teddyapp.domain.repository.PlaybackProgressRepository

class PlaybackProgressRepositoryImpl(
    private val dao: PlaybackProgressDao,
    private val clock: () -> Long = System::currentTimeMillis,
) : PlaybackProgressRepository {

    override suspend fun getPosition(tonieId: String): Long = dao.get(tonieId)?.positionMs ?: 0L

    override suspend fun savePosition(tonieId: String, positionMs: Long) {
        val lastPlayed = dao.get(tonieId)?.lastPlayedAt ?: clock()
        dao.upsert(PlaybackProgressEntity(tonieId, positionMs, lastPlayed))
    }

    override suspend fun markPlayed(tonieId: String) {
        val position = dao.get(tonieId)?.positionMs ?: 0L
        dao.upsert(PlaybackProgressEntity(tonieId, position, clock()))
    }
}
