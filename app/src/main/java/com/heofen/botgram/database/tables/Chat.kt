package com.heofen.botgram.database.tables

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.heofen.botgram.ChatType

@Entity(tableName = "chats")
data class Chat(
    @PrimaryKey
    val id: Long,
    val type: ChatType,
    val title: String,

    val lastMessageText: String?,
    val lastMessageTime: Long,
    val unreadCount: Int = 0,

    val avatarFileId: String?,
    val avatarFileUniqueId: String?,
    val avatarLocalPath: String?,
    val avatarUpdatedAt: Long?
)