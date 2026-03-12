package com.heofen.botgram.database.tables

import androidx.room.Embedded

data class ChatListItem(
    @Embedded
    val chat: Chat,
    @Embedded(prefix = "lastMessage_")
    val lastMessage: Message?
)
