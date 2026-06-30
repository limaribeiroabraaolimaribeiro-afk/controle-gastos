package com.controlpro.banklistener.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.controlpro.banklistener.data.model.DefaultBankApps
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_prefs")

class AppPreferences(private val context: Context) {

    companion object {
        val AUTO_MODE = booleanPreferencesKey("auto_mode")
        val ENABLED_PACKAGES = stringSetPreferencesKey("enabled_packages")
        val MIN_CONFIDENCE = stringPreferencesKey("min_confidence")
    }

    val autoModeFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[AUTO_MODE] ?: false
    }

    val enabledPackagesFlow: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[ENABLED_PACKAGES] ?: DefaultBankApps.list.map { it.packageName }.toSet()
    }

    val minConfidenceFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        (prefs[MIN_CONFIDENCE] ?: "85").toIntOrNull() ?: 85
    }

    suspend fun setAutoMode(enabled: Boolean) {
        context.dataStore.edit { it[AUTO_MODE] = enabled }
    }

    suspend fun setEnabledPackages(packages: Set<String>) {
        context.dataStore.edit { it[ENABLED_PACKAGES] = packages }
    }

    suspend fun togglePackage(packageName: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[ENABLED_PACKAGES] ?: DefaultBankApps.list.map { it.packageName }.toSet()
            prefs[ENABLED_PACKAGES] = if (packageName in current) {
                current - packageName
            } else {
                current + packageName
            }
        }
    }
}
