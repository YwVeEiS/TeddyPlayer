package xyz.weilandt.teddyapp.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import xyz.weilandt.teddyapp.domain.model.Tonie

@Entity(tableName = "tonies")
data class TonieEntity(
    @PrimaryKey val id: String,
    val title: String,
    val series: String,
    val coverUrl: String?,
    val audioPath: String,
    /** Kommagetrennte Kapitelstarts in ms. */
    val chapterStarts: String,
) {
    fun toDomain() = Tonie(
        id = id,
        title = title,
        series = series,
        coverUrl = coverUrl,
        audioPath = audioPath,
        chapterStartsMs = chapterStarts.split(',').mapNotNull { it.toLongOrNull() }.ifEmpty { listOf(0L) },
    )

    companion object {
        fun from(tonie: Tonie) = TonieEntity(
            id = tonie.id,
            title = tonie.title,
            series = tonie.series,
            coverUrl = tonie.coverUrl,
            audioPath = tonie.audioPath,
            chapterStarts = tonie.chapterStartsMs.joinToString(","),
        )
    }
}

@Entity(tableName = "playback_progress")
data class PlaybackProgressEntity(
    @PrimaryKey val tonieId: String,
    val positionMs: Long,
    val lastPlayedAt: Long,
)

@Dao
interface TonieDao {
    @Query(
        """
        SELECT t.* FROM tonies t
        LEFT JOIN playback_progress p ON p.tonieId = t.id
        ORDER BY p.lastPlayedAt IS NULL, p.lastPlayedAt DESC,
                 t.series COLLATE NOCASE, t.title COLLATE NOCASE
        """
    )
    fun observeAll(): Flow<List<TonieEntity>>

    @Query("SELECT * FROM tonies WHERE id = :id")
    suspend fun get(id: String): TonieEntity?

    @Query("DELETE FROM tonies")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tonies: List<TonieEntity>)

    @Transaction
    suspend fun replaceAll(tonies: List<TonieEntity>) {
        deleteAll()
        insertAll(tonies)
    }
}

@Dao
interface PlaybackProgressDao {
    @Query("SELECT * FROM playback_progress WHERE tonieId = :tonieId")
    suspend fun get(tonieId: String): PlaybackProgressEntity?

    @Upsert
    suspend fun upsert(entity: PlaybackProgressEntity)
}

@Database(
    entities = [TonieEntity::class, PlaybackProgressEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class TeddyDatabase : RoomDatabase() {
    abstract fun tonieDao(): TonieDao
    abstract fun playbackProgressDao(): PlaybackProgressDao
}
