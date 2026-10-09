package xyz.weilandt.teddyapp

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.request.crossfade
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import xyz.weilandt.teddyapp.di.appModules
import xyz.weilandt.teddyapp.domain.repository.DownloadRepository

class TeddyPlayerApp : Application(), SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@TeddyPlayerApp)
            modules(appModules)
        }
        // The DownloadManager must be created on the main thread and load its state early.
        get<DownloadRepository>()
    }

    /** Covers live in filesDir instead of cacheDir so Android doesn't evict them while offline. */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .diskCache {
                DiskCache.Builder()
                    .directory(filesDir.resolve("covers"))
                    .maxSizeBytes(COVER_CACHE_BYTES)
                    .build()
            }
            .crossfade(true)
            .build()

    private companion object {
        const val COVER_CACHE_BYTES = 250L * 1024 * 1024
    }
}
