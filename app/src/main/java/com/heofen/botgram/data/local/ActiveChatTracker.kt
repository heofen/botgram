package com.heofen.botgram.data.local

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Хранит идентификатор чата, который сейчас открыт пользователем.
 *
 * Используется, чтобы не показывать уведомление о сообщении в чате,
 * который уже находится на экране.
 */
class ActiveChatTracker {
    private val _activeChatId = MutableStateFlow<Long?>(null)
    val activeChatId: StateFlow<Long?> = _activeChatId.asStateFlow()

    fun setActive(chatId: Long?) {
        _activeChatId.value = chatId
    }

    fun current(): Long? = _activeChatId.value
}
