package com.heofen.botgram.database.tables

import androidx.annotation.RequiresExtension
import androidx.room.Entity
import com.heofen.botgram.MessageType

@Entity(
    tableName = "messages",
    primaryKeys = ["chatId", "messageId"]
)
data class Message(
    val messageId: Long,
    val chatId: Long,
    val topicId: Long?,
    val senderId: Long?,
    val type: MessageType,
    val timestamp: Long,

    val text: String?,
    val caption: String?,

    val replyMsgId: Long?,
    val replyMsgTopicId: Long?,
    // val replyChatId: Long?,

    val fileName: String?,
    val fileExtension: String?,
    val fileId: String?,
    val fileUniqueId: String?,
    val fileLocalPath: String?,
    val fileSize: Long?,

    val width: Int?,
    val height: Int?,
    val duration: Long?,
    val thumbnailFileId: String?,

    val isEdited: Boolean = false,
    val editedAt: Long? = null,

    val mediaGroupId: String?,

    val isOutgoing: Boolean = false
)
