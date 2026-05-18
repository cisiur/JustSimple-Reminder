package com.justsimple.reminder.domain.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val KEY_USE_24H = booleanPreferencesKey("use_24h_format")
    }

    val use24hFormat: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_USE_24H] ?: false
    }

    suspend fun setUse24hFormat(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_USE_24H] = enabled }
    }
}

