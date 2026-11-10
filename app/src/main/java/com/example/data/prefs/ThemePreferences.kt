package com.example.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "mera_insta_prefs")

class ThemePreferences(private val context: Context) {
    private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
    private val PERMISSIONS_DONE_KEY = booleanPreferencesKey("permissions_wizard_done")

    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[DARK_MODE_KEY] ?: false
    }

    val isPermissionsWizardDone: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PERMISSIONS_DONE_KEY] ?: false
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[DARK_MODE_KEY] = enabled
        }
    }

    suspend fun setPermissionsWizardDone(done: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PERMISSIONS_DONE_KEY] = done
        }
    }
}
