package com.heofen.botgram.data

import com.heofen.botgram.data.remote.TelegramGateway
import com.heofen.botgram.data.remote.AvatarFetchResult

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
    suspend fun downloadUserAvatar(userId: Long): AvatarFetchResult? {
        return gateway.downloadUserAvatar(userId)
    }

    /** Загружает аватар чата. */
    suspend fun downloadChatAvatar(chatId: Long): AvatarFetchResult? {
        return gateway.downloadChatAvatar(chatId)
    }
}
