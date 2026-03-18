package com.heofen.botgram.data.remote

import java.io.File

/** Локальный медиафайл, подготовленный к отправке как фото или видео. */
data class OutgoingVisualMedia(
    val file: File,
    val mimeType: String
)

/** Результат загрузки аватара с Telegram-идентификаторами и локальным путём. */
data class AvatarDownloadResult(
    val fileId: String?,
    val fileUniqueId: String?,
    val localPath: String?
)

/**
 * Абстракция над транспортом Telegram.
 *
 * Остальные слои используют этот интерфейс и не знают о деталях HTTP-реализации.
 */
interface TelegramGateway {
    /** Непрерывно получает обновления и отдаёт их вызывающему коду. */
    suspend fun collectUpdates(onUpdate: suspend (TelegramUpdate) -> Unit)

    /** Отправляет текстовое сообщение. */
    suspend fun sendTextMessage(
        chatId: Long,
        text: String,
        replyToMessageId: Long? = null
    ): TelegramIncomingMessage

    /** Отправляет сообщение с геолокацией. */
    suspend fun sendLocationMessage(
        chatId: Long,
        latitude: Double,
        longitude: Double,
        replyToMessageId: Long? = null
    ): TelegramIncomingMessage

    /** Отправляет одиночную фотографию. */
    suspend fun sendPhotoMessage(
        chatId: Long,
        file: File,
        mimeType: String,
        caption: String? = null,
        replyToMessageId: Long? = null
    ): TelegramIncomingMessage

    /** Отправляет одиночное видео. */
    suspend fun sendVideoMessage(
        chatId: Long,
        file: File,
        mimeType: String,
        caption: String? = null,
        replyToMessageId: Long? = null
    ): TelegramIncomingMessage

    /** Отправляет альбом из фото и/или видео. */
    suspend fun sendVisualMediaGroup(
        chatId: Long,
        media: List<OutgoingVisualMedia>,
        caption: String? = null,
        replyToMessageId: Long? = null
    ): List<TelegramIncomingMessage>

    /** Пытается удалить сообщение через Telegram API. */
    suspend fun deleteMessage(chatId: Long, messageId: Long): Boolean

    /** Скачивает произвольный файл Telegram и возвращает путь к нему в кеше. */
    suspend fun downloadFile(
        fileId: String,
        fileExtension: String,
        fileUniqueId: String,
        isAvatar: Boolean = false
    ): String?

    /** Загружает аватар пользователя. */
    suspend fun downloadUserAvatar(userId: Long): AvatarDownloadResult?

    /** Загружает аватар чата. */
    suspend fun downloadChatAvatar(chatId: Long): AvatarDownloadResult?

    /** Освобождает ресурсы сетевого клиента. */
    fun close()
}
