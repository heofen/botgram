package com.heofen.botgram.data.remote

data class AvatarDownloadResult(
    val fileId: String?,
    val fileUniqueId: String?,
    val localPath: String?
)

interface TelegramGateway {
    suspend fun collectUpdates(onMessage: suspend (TelegramIncomingMessage) -> Unit)

    suspend fun sendTextMessage(chatId: Long, text: String): TelegramIncomingMessage

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
