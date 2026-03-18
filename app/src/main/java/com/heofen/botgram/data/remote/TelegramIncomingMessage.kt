package com.heofen.botgram.data.remote

import com.heofen.botgram.ChatType
import com.heofen.botgram.MessageType

/** Нормализованная модель сообщения, независимая от сырых DTO Telegram API. */
data class TelegramIncomingMessage(
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
    val fileName: String?,
    val fileExtension: String?,
    val fileId: String?,
    val fileUniqueId: String?,
    val fileSize: Long?,
    val width: Int?,
    val height: Int?,
    val duration: Long?,
    val thumbnailFileId: String?,
    val latitude: Double?,
    val longitude: Double?,
    val isEdited: Boolean,
    val editedAt: Long?,
    val mediaGroupId: String?,
    val chat: TelegramChat,
    val sender: TelegramUser?
)

/** Нормализованная модель чата, полученная из Telegram API. */
data class TelegramChat(
    val id: Long,
    val type: ChatType,
    val title: String?,
    val firstName: String?,
    val lastName: String?,
    val username: String?,
    val lastMessageType: MessageType?,
    val lastMessageText: String?,
    val lastMessageTime: Long?,
    val lastMessageSenderId: Long?
)

/** Нормализованная модель пользователя Telegram. */
data class TelegramUser(
    val id: Long,
    val firstName: String,
    val lastName: String?,
    val bio: String?,
    val canWriteMsgToPm: Boolean
)
