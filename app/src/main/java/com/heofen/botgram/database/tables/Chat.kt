package com.heofen.botgram.database.tables

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.heofen.botgram.ChatType
import com.heofen.botgram.MessageType

@Entity(tableName = "chats")
data class Chat(
    @PrimaryKey
    val id: Long,
    val type: ChatType,
    val title: String?,
    val firstName: String?,
    val lastName: String?,
    val username: String?,

    val lastMessageType: MessageType?,
    val lastMessageText: String?,
    val lastMessageTime: Long?,
    val lastMessageSenderId: Long?,

//    val isMuted: Boolean = false,

    val avatarFileId: String?,
    val avatarFileUniqueId: String?,
    val avatarLocalPath: String?,
)

// TODO("для lastMessage добавить sender id")
