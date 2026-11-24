package app.hohe.keyboard.data

import android.content.Context

/**
 * Process-wide entry point for `:data`. Both stores are created lazily on first use with the
 * application context and live for the rest of the process. DataStore allows only one instance
 * per file, so always obtain stores here, never by constructing them.
 */
object HoheData {
    @Volatile private var settingsStore: SettingsStore? = null

    @Volatile private var personalStore: PersonalDataStore? = null

    fun settings(context: Context): SettingsStore =
        settingsStore ?: synchronized(this) {
            settingsStore ?: DataStoreSettingsStore.create(context.applicationContext).also { settingsStore = it }
        }

    fun personal(context: Context): PersonalDataStore =
        personalStore ?: synchronized(this) {
            personalStore ?: HohePersonalDataStore.create(context.applicationContext).also { personalStore = it }
        }
}
