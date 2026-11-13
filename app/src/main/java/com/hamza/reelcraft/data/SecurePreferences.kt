package com.hamza.reelcraft.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecurePreferences(context: Context) {
    private val sharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "reelcraft_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        context.getSharedPreferences("reelcraft_fallback_prefs", Context.MODE_PRIVATE)
    }

    fun saveApiKeys(keys: Set<String>) {
        sharedPreferences.edit().putStringSet("saved_api_keys", keys).apply()
    }

    fun getApiKeys(): Set<String> {
        return sharedPreferences.getStringSet("saved_api_keys", emptySet()) ?: emptySet()
    }

    fun saveActiveApiKey(key: String) {
        sharedPreferences.edit().putString("active_api_key", key).apply()
    }

    fun getActiveApiKey(): String {
        return sharedPreferences.getString("active_api_key", "") ?: ""
    }
}
