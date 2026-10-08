package xyz.weilandt.teddyapp.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import xyz.weilandt.teddyapp.domain.model.ServerUrl
import xyz.weilandt.teddyapp.domain.repository.SettingsRepository

class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val serverUrl: Flow<String> = dataStore.data
        .map { it[KEY_SERVER_URL] ?: ServerUrl.DEFAULT }
        .distinctUntilChanged()

    override suspend fun setServerUrl(url: String) {
        val normalized = ServerUrl.normalize(url) ?: return
        dataStore.edit { it[KEY_SERVER_URL] = normalized }
    }

    private companion object {
        val KEY_SERVER_URL = stringPreferencesKey("server_url")
    }
}
