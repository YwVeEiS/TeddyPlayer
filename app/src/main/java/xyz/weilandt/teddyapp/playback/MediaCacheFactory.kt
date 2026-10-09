package xyz.weilandt.teddyapp.playback

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.offline.DownloadManager
import java.io.File
import java.util.concurrent.Executors

/**
 * Shared audio cache for player and downloads – the "Toniebox principle":
 * while playing, everything streamed ends up in the cache and the [DownloadManager]
 * fetches the rest in parallel. Afterwards the tonie plays fully offline. Nothing is evicted automatically.
 */
@OptIn(UnstableApi::class)
object MediaCacheFactory {

    fun databaseProvider(context: Context) = StandaloneDatabaseProvider(context)

    fun cache(context: Context, databaseProvider: StandaloneDatabaseProvider) =
        SimpleCache(File(context.filesDir, "audio"), NoOpCacheEvictor(), databaseProvider)

    fun httpDataSourceFactory(): DefaultHttpDataSource.Factory =
        DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(8_000)
            .setReadTimeoutMs(20_000)
            .setAllowCrossProtocolRedirects(true)

    fun playbackDataSourceFactory(
        context: Context,
        cache: SimpleCache,
        http: DefaultHttpDataSource.Factory,
    ): CacheDataSource.Factory =
        CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(DefaultDataSource.Factory(context, http))
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

    fun downloadManager(
        context: Context,
        databaseProvider: StandaloneDatabaseProvider,
        cache: SimpleCache,
        http: DefaultHttpDataSource.Factory,
    ): DownloadManager =
        DownloadManager(context, databaseProvider, cache, http, Executors.newFixedThreadPool(2))
            .apply { maxParallelDownloads = 2 }
}
