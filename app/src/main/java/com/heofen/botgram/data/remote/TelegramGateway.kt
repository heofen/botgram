package com.heofen.botgram.data.remote

import java.io.File

/** Локальный медиафайл, подготовленный к отправке как фото или видео. */
data class OutgoingVisualMedia(
    val file: File,
    val mimeType: String
)

/** Состояние аватара после запроса в Telegram. */
sealed interface AvatarFetchResult {
    /** У сущности есть аватар, идентификаторы известны. */
    data class Available(
        val fileId: String,
        val fileUniqueId: String,
        val localPath: String?
    ) : AvatarFetchResult

    /** Аватар у сущности отсутствует. */
    object Missing : AvatarFetchResult
}

/** Результат чтения публичного описания профиля по `t.me/<username>`. */
sealed interface PublicProfileBioResult {
    /** Запрос выполнен успешно, `bio` может отсутствовать. */
    data class Success(val bio: String?) : PublicProfileBioResult

    /** Запрос не удалось выполнить или разобрать. */
    object Failure : PublicProfileBioResult
}

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

    /** Отправляет произвольный файл как документ. */
    suspend fun sendDocumentMessage(
        chatId: Long,
        file: File,
        mimeType: String,
        caption: String? = null,
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
    suspend fun downloadUserAvatar(userId: Long): AvatarFetchResult?

    /** Загружает аватар чата. */
    suspend fun downloadChatAvatar(chatId: Long): AvatarFetchResult?

    /** Читает публичное описание пользователя со страницы `t.me/<username>`. */
    suspend fun fetchUserBio(username: String): PublicProfileBioResult

    /** Получает полную информацию о чате. */
    suspend fun getChat(chatId: Long): TelegramChat?

    /** Освобождает ресурсы сетевого клиента. */
    fun close()
}
