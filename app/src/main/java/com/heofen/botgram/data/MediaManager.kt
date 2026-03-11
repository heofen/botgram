package com.heofen.botgram.data

import com.heofen.botgram.data.remote.TelegramGateway
import com.heofen.botgram.data.remote.AvatarDownloadResult

class MediaManager(
    private val gateway: TelegramGateway,
) {
    suspend fun getFile(
        fileId: String,
        fileExtension: String,
        fileUniqueId: String,
        isAvatar: Boolean = false
    ): String? {
        return gateway.downloadFile(fileId, fileExtension, fileUniqueId, isAvatar)
    }

    suspend fun downloadUserAvatar(userId: Long): AvatarDownloadResult? {
        return gateway.downloadUserAvatar(userId)
    }

    suspend fun downloadChatAvatar(chatId: Long): AvatarDownloadResult? {
        return gateway.downloadChatAvatar(chatId)
    }
}
