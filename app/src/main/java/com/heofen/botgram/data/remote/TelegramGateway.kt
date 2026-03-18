package com.heofen.botgram.data.remote

import java.io.File

data class OutgoingVisualMedia(
    val file: File,
    val mimeType: String
)

data class AvatarDownloadResult(
    val fileId: String?,
    val fileUniqueId: String?,
    val localPath: String?
)

interface TelegramGateway {
    suspend fun collectUpdates(onUpdate: suspend (TelegramUpdate) -> Unit)

    suspend fun sendTextMessage(
        chatId: Long,
        text: String,
        replyToMessageId: Long? = null
    ): TelegramIncomingMessage

    suspend fun sendLocationMessage(
        chatId: Long,
        latitude: Double,
        longitude: Double,
        replyToMessageId: Long? = null
    ): TelegramIncomingMessage

    suspend fun sendPhotoMessage(
        chatId: Long,
        file: File,
        mimeType: String,
        caption: String? = null,
        replyToMessageId: Long? = null
    ): TelegramIncomingMessage

    suspend fun sendVideoMessage(
        chatId: Long,
        file: File,
        mimeType: String,
        caption: String? = null,
        replyToMessageId: Long? = null
    ): TelegramIncomingMessage

    suspend fun sendVisualMediaGroup(
        chatId: Long,
        media: List<OutgoingVisualMedia>,
        caption: String? = null,
        replyToMessageId: Long? = null
    ): List<TelegramIncomingMessage>

    suspend fun deleteMessage(chatId: Long, messageId: Long): Boolean

    suspend fun downloadFile(
        fileId: String,
        fileExtension: String,
        fileUniqueId: String,
        isAvatar: Boolean = false
    ): String?

    suspend fun downloadUserAvatar(userId: Long): AvatarDownloadResult?

    suspend fun downloadChatAvatar(chatId: Long): AvatarDownloadResult?

    fun close()
}
