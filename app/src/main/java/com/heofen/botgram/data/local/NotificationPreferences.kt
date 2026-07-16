package com.heofen.botgram.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/** Хранит per-chat настройки уведомлений и отдаёт их как `Flow` для UI. */
class NotificationPreferences(context: Context) {
    private val prefs: SharedPreferences = context.applicationContext
        .getSharedPreferences("botgram_notifications", Context.MODE_PRIVATE)

    /** Проверяет, выключены ли уведомления у конкретного чата. */
    fun isMuted(chatId: Long): Boolean =
        chatId.toString() in prefs.getStringSet(KEY_MUTED_CHATS, emptySet()).orEmpty()

    fun setMuted(chatId: Long, muted: Boolean) {
        val current = prefs.getStringSet(KEY_MUTED_CHATS, emptySet()).orEmpty().toMutableSet()
        val key = chatId.toString()
        val changed = if (muted) current.add(key) else current.remove(key)
        if (changed) {
            // Создаём новую коллекцию, иначе SharedPreferences сравнит ссылки и не уведомит слушателей.
            prefs.edit { putStringSet(KEY_MUTED_CHATS, current.toSet()) }
        }
    }

    /** Поток состояния mute для конкретного чата. */
    fun observeMuted(chatId: Long): Flow<Boolean> = callbackFlow {
        val emit = { trySend(isMuted(chatId)).let {} }
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_MUTED_CHATS) emit()
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        emit()
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    companion object {
        private const val KEY_MUTED_CHATS = "muted_chats"
    }
}
