package com.heofen.botgram.data.local

import android.content.Context
import androidx.core.content.edit

/** Обёртка над `SharedPreferences` для хранения bot token. */
class TokenManager(context: Context) {
    private val prefs = context.getSharedPreferences("botgram_prefs", Context.MODE_PRIVATE)

    /** Сохраняет токен в локальное хранилище в нормализованном виде. */
    fun saveToken(token: String) {
        prefs.edit { putString("bot_token", token.trim()) }
    }

    /** Возвращает сохранённый токен или `null`, если его ещё нет. */
    fun getToken(): String? {
        return prefs.getString("bot_token", null)
    }

    /** Удаляет токен из локального хранилища. */
    fun clearToken() {
        prefs.edit { remove("bot_token") }
    }
}
