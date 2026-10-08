package xyz.weilandt.teddyapp.di

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.media3.datasource.cache.SimpleCache
import androidx.room.Room
import coil3.SingletonImageLoader
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import xyz.weilandt.teddyapp.data.local.TeddyDatabase
import xyz.weilandt.teddyapp.data.network.AndroidNetworkMonitor
import xyz.weilandt.teddyapp.data.remote.TeddyCloudApi
import xyz.weilandt.teddyapp.data.repository.CoilCoverPrefetcher
import xyz.weilandt.teddyapp.data.repository.CoverPrefetcher
import xyz.weilandt.teddyapp.data.repository.PlaybackProgressRepositoryImpl
import xyz.weilandt.teddyapp.data.repository.ToniesRepositoryImpl
import xyz.weilandt.teddyapp.data.settings.DataStoreSettingsRepository
import xyz.weilandt.teddyapp.domain.repository.DownloadRepository
import xyz.weilandt.teddyapp.domain.repository.NetworkMonitor
import xyz.weilandt.teddyapp.domain.repository.PlaybackController
import xyz.weilandt.teddyapp.domain.repository.PlaybackProgressRepository
import xyz.weilandt.teddyapp.domain.repository.SettingsRepository
import xyz.weilandt.teddyapp.domain.repository.ToniesRepository
import xyz.weilandt.teddyapp.playback.Media3DownloadRepository
import xyz.weilandt.teddyapp.playback.MediaCacheFactory
import xyz.weilandt.teddyapp.playback.MediaPlaybackController
import xyz.weilandt.teddyapp.ui.library.LibraryViewModel
import xyz.weilandt.teddyapp.ui.parent.gate.ParentGateViewModel
import xyz.weilandt.teddyapp.ui.parent.settings.ParentSettingsViewModel
import xyz.weilandt.teddyapp.ui.player.PlayerViewModel

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

private val APP_SCOPE = named("appScope")

val dataModule = module {
    single(APP_SCOPE) { CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate) }

    single {
        HttpClient(OkHttp) {
            expectSuccess = true
            install(ContentNegotiation) {
                // TeddyCloud liefert JSON als "text/json" – daher für alle Typen registrieren
                json(Json { ignoreUnknownKeys = true; coerceInputValues = true }, contentType = ContentType.Any)
            }
            install(HttpTimeout) {
                connectTimeoutMillis = 5_000
                requestTimeoutMillis = 20_000
            }
        }
    }
    single { TeddyCloudApi(get()) }

    single {
        Room.databaseBuilder(androidContext(), TeddyDatabase::class.java, "teddy.db").build()
    }
    single { get<TeddyDatabase>().tonieDao() }
    single { get<TeddyDatabase>().playbackProgressDao() }

    single<SettingsRepository> { DataStoreSettingsRepository(androidContext().settingsDataStore) }
    single<CoverPrefetcher> { CoilCoverPrefetcher(androidContext(), SingletonImageLoader.get(androidContext())) }
    single<ToniesRepository> { ToniesRepositoryImpl(get(), get(), get(), get()) }
    single<PlaybackProgressRepository> { PlaybackProgressRepositoryImpl(get()) }
    single<NetworkMonitor> { AndroidNetworkMonitor(androidContext()) }
}

val playbackModule = module {
    single { MediaCacheFactory.databaseProvider(androidContext()) }
    single { MediaCacheFactory.cache(androidContext(), get()) }
    single { MediaCacheFactory.httpDataSourceFactory() }
    single { MediaCacheFactory.playbackDataSourceFactory(androidContext(), get(), get()) }
    single { MediaCacheFactory.downloadManager(androidContext(), get(), get(), get()) }
    single<DownloadRepository> {
        Media3DownloadRepository(androidContext(), get(), get<SimpleCache>(), get(), get(APP_SCOPE))
    }
    single<PlaybackController> {
        MediaPlaybackController(androidContext(), get(), get(), get(), get(APP_SCOPE))
    }
}

val uiModule = module {
    viewModelOf(::LibraryViewModel)
    viewModelOf(::PlayerViewModel)
    viewModel { ParentGateViewModel() }
    viewModelOf(::ParentSettingsViewModel)
}

val appModules = listOf(dataModule, playbackModule, uiModule)
