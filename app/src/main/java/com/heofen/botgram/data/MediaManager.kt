package com.heofen.botgram.data

import com.heofen.botgram.data.remote.TelegramGateway
import com.heofen.botgram.data.remote.AvatarDownloadResult

/** Централизует скачивание файлов и аватаров через `TelegramGateway`. */
class MediaManager(
    private val gateway: TelegramGateway,
) {
    /** Скачивает файл Telegram и возвращает путь в локальном кеше. */
    suspend fun getFile(
        fileId: String,
        fileExtension: String,
        fileUniqueId: String,
        isAvatar: Boolean = false
    ): String? {
        return gateway.downloadFile(fileId, fileExtension, fileUniqueId, isAvatar)
    }

    /** Загружает аватар пользователя. */
    suspend fun downloadUserAvatar(userId: Long): AvatarDownloadResult? {
        return gateway.downloadUserAvatar(userId)
    }

    /** Загружает аватар чата. */
    suspend fun downloadChatAvatar(chatId: Long): AvatarDownloadResult? {
        return gateway.downloadChatAvatar(chatId)
    }
}
