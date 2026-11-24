package app.hohe.keyboard.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import app.hohe.keyboard.core.settings.KeyboardSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

/**
 * [SettingsStore] backed by Preferences DataStore (file `datastore/settings.preferences_pb`,
 * backed up, see `res/xml/backup_rules.xml`). [settings] starts with the defaults and follows the
 * file; [update] and [reset] are atomic DataStore edits and are visible in [settings] on return.
 */
class DataStoreSettingsStore internal constructor(
    private val dataStore: DataStore<Preferences>,
    scope: CoroutineScope,
) : SettingsStore {
    private val state = MutableStateFlow(KeyboardSettings())
    override val settings: StateFlow<KeyboardSettings> = state.asStateFlow()

    init {
        scope.launch {
            dataStore.data
                .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
                .collect { state.value = SettingsCodec.decode(it) }
        }
    }

    override suspend fun update(transform: (KeyboardSettings) -> KeyboardSettings) {
        val written =
            dataStore.edit { prefs ->
                val next = transform(SettingsCodec.decode(prefs)).sanitized()
                SettingsCodec.encode(next, prefs)
            }
        state.value = SettingsCodec.decode(written)
    }

    override suspend fun reset() {
        val written = dataStore.edit { it.clear() }
        state.value = SettingsCodec.decode(written)
    }

    companion object {
        /** DataStore name; the file is `files/datastore/settings.preferences_pb`. */
        const val FILE_NAME = "settings"

        /** Path relative to `filesDir`, as listed in the backup rules. */
        const val BACKUP_PATH = "datastore/$FILE_NAME.preferences_pb"

        internal fun create(context: Context): DataStoreSettingsStore {
            val appContext = context.applicationContext
            return create(
                file = { appContext.preferencesDataStoreFile(FILE_NAME) },
                scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
            )
        }

        internal fun create(
            file: () -> File,
            scope: CoroutineScope,
        ): DataStoreSettingsStore {
            val store =
                PreferenceDataStoreFactory.create(
                    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
                    scope = scope,
                    produceFile = file,
                )
            return DataStoreSettingsStore(store, scope)
        }
    }
}
