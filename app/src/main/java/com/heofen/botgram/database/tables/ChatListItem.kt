package com.heofen.botgram.database.tables

import androidx.room.Embedded

/** Элемент списка чатов: сам чат плюс последнее сообщение для превью. */
data class ChatListItem(
    @Embedded
    val chat: Chat,
    @Embedded(prefix = "lastMessage_")
    val lastMessage: Message?
)
