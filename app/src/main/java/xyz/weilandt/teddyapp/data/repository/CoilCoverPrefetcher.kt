package xyz.weilandt.teddyapp.data.repository

import android.content.Context
import coil3.ImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest

/** Lädt alle Cover in den Disk-Cache, damit sie auch ohne Netz angezeigt werden. */
class CoilCoverPrefetcher(
    private val context: Context,
    private val imageLoader: ImageLoader,
) : CoverPrefetcher {

    override fun prefetch(urls: List<String>) {
        urls.distinct().forEach { url ->
            imageLoader.enqueue(
                ImageRequest.Builder(context)
                    .data(url)
                    .memoryCachePolicy(CachePolicy.DISABLED)
                    .build()
            )
        }
    }
}
