package com.atlasv.android.amplify.simpleappsync.ext

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.firstOrNull

/**
 * weiping@atlasv.com
 * 2022/12/22
 */

val Context.amplifySettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "amplify_settings")

object AmplifyExtSettings {
    private const val KEY_LAST_LOCALE = "last_locale"
    private const val KEY_SYNC_DB_VERSION = "sync_db_version"
    private const val KEY_SYNC_TIMESTAMP = "sync_timestamp"
    suspend fun getLastModelLocale(context: Context): String? {
        return context.amplifySettingsDataStore.data.firstOrNull()?.get(stringPreferencesKey(KEY_LAST_LOCALE))
    }

    suspend fun getLastSyncDbVersion(context: Context): String? {
        return context.amplifySettingsDataStore.data.firstOrNull()?.get(stringPreferencesKey(KEY_SYNC_DB_VERSION))
    }

    private suspend fun saveLastSyncDbVersion(context: Context, dbVersion: String) {
        context.amplifySettingsDataStore.edit {
            it[stringPreferencesKey(KEY_SYNC_DB_VERSION)] = dbVersion
        }
    }

    suspend fun getLastSyncTimestamp(context: Context): Long {
        return context.amplifySettingsDataStore.data.firstOrNull()?.get(longPreferencesKey(KEY_SYNC_TIMESTAMP)) ?: 0
    }

    private suspend fun saveLastSyncTimestamp(context: Context, timestamp: Long?) {
        timestamp ?: return
        context.amplifySettingsDataStore.edit {
            it[longPreferencesKey(KEY_SYNC_TIMESTAMP)] = timestamp
        }
    }

    suspend fun saveLastSync(context: Context, dbVersion: String, timestamp: Long?, locale: String) {
        saveLastSyncDbVersion(context, dbVersion)
        saveLastSyncTimestamp(context, timestamp)
        saveLastModelLocale(context, locale)
    }

    private suspend fun saveLastModelLocale(context: Context, locale: String) {
        context.amplifySettingsDataStore.edit {
            it[stringPreferencesKey(KEY_LAST_LOCALE)] = locale
        }
    }
}