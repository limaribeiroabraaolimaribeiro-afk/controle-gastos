package com.controlpro.banklistener.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecurePreferences(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "bank_listener_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun putString(key: String, value: String) = prefs.edit().putString(key, value).apply()
    fun getString(key: String): String? = prefs.getString(key, null)
    fun remove(key: String) = prefs.edit().remove(key).apply()
    fun clear() = prefs.edit().clear().apply()

    companion object {
        const val KEY_ACCESS_TOKEN = "supabase_access_token"
        const val KEY_REFRESH_TOKEN = "supabase_refresh_token"
        const val KEY_USER_ID = "supabase_user_id"
        const val KEY_USER_EMAIL = "supabase_user_email"
    }
}
