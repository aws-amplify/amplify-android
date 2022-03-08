package com.amplifyframework.auth.cognito.data

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV
import androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
import androidx.security.crypto.MasterKeys

class EncryptedKeyValueRepository(
    private val context: Context,
    private val sharedPreferencesName: String,
) : KeyValueRepository {

    override fun put(dataKey: String, value: String?) {
        with(getSharedPreferences().edit()) {
            putString(dataKey, value)
            apply()
        }
    }

    override fun get(dataKey: String): String? = getSharedPreferences().getString(dataKey, null)

    override fun remove(dataKey: String) {
        with(getSharedPreferences().edit()) {
            remove(dataKey)
            apply()
        }
    }

    @VisibleForTesting
    fun getSharedPreferences(): SharedPreferences {
        return EncryptedSharedPreferences.create(
            sharedPreferencesName,
            MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
            context,
            AES256_SIV,
            AES256_GCM
        )
    }
}