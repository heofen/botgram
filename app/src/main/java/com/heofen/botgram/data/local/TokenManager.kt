package com.heofen.botgram.data.local

import android.content.Context
import androidx.core.content.edit

class TokenManager(context: Context) {
    private val prefs = context.getSharedPreferences("botgram_prefs", Context.MODE_PRIVATE)

    fun saveToken(token: String) {
        prefs.edit { putString("bot_token", token.trim()) }
    }

    fun getToken(): String? {
        return prefs.getString("bot_token", null)
    }

    fun clearToken() {
        prefs.edit { remove("bot_token") }
    }
}