package com.heofen.botgram.data.remote.telegramapi

import android.content.Context
import android.util.Log
import com.heofen.botgram.ChatType
import com.heofen.botgram.MessageType
import com.heofen.botgram.data.remote.AvatarFetchResult
import com.heofen.botgram.data.remote.OutgoingVisualMedia
import com.heofen.botgram.data.remote.PublicProfileBioResult
import com.heofen.botgram.data.remote.TelegramChat
import com.heofen.botgram.data.remote.TelegramChatMember
import com.heofen.botgram.data.remote.TelegramGateway
import com.heofen.botgram.data.remote.TelegramIncomingMessage
import com.heofen.botgram.data.remote.TelegramUpdate
import com.heofen.botgram.data.remote.TelegramUser
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import java.io.File
import kotlin.coroutines.cancellation.CancellationException

/** HTTP-реализация `TelegramGateway`, основанная on `TelegramBotApiClient`. */
class HttpTelegramGateway(
    private val context: Context,
    token: String
) : TelegramGateway {

    private val apiClient = TelegramBotApiClient(token = token)
    private var nextUpdateOffset: Long? = null

    /** Держит long polling цикл и конвертирует DTO Telegram в доменные обновления. */
    override suspend fun collectUpdates(onUpdate: suspend (TelegramUpdate) -> Unit) {
        while (currentCoroutineContext().isActive) {
            val updates = apiClient.getUpdates(offset = nextUpdateOffset, timeout = 50)

            for (update in updates) {
                val telegramUpdate = when {
                    update.message != null -> TelegramUpdate.NewMessage(
                        updateId = update.updateId,
                        message = update.message.toIncomingMessage()
                    )
                    update.editedMessage != null -> TelegramUpdate.EditedMessage(
                        updateId = update.updateId,
                        message = update.editedMessage.toIncomingMessage()
                    )
                    else -> TelegramUpdate.Unsupported(updateId = update.updateId)
                }

                onUpdate(telegramUpdate)
                nextUpdateOffset = update.updateId + 1
            }
        }
    }

    override suspend fun sendTextMessage(
        chatId: Long,
        text: String,
        replyToMessageId: Long?
    ): TelegramIncomingMessage {
        return apiClient.sendMessage(
            chatId = chatId,
            text = text,
            replyToMessageId = replyToMessageId
        )
            .toIncomingMessage()
    }

    override suspend fun sendLocationMessage(
        chatId: Long,
        latitude: Double,
        longitude: Double,
        replyToMessageId: Long?
    ): TelegramIncomingMessage {
        return apiClient.sendLocation(
            chatId = chatId,
            latitude = latitude,
            longitude = longitude,
            replyToMessageId = replyToMessageId
        ).toIncomingMessage()
    }

    override suspend fun sendDocumentMessage(
        chatId: Long,
        file: File,
        mimeType: String,
        caption: String?,
        replyToMessageId: Long?
    ): TelegramIncomingMessage {
        return apiClient.sendDocument(
            chatId = chatId,
            file = file,
            mimeType = mimeType,
            caption = caption,
            replyToMessageId = replyToMessageId
        ).toIncomingMessage()
    }

    override suspend fun sendPhotoMessage(
        chatId: Long,
        file: File,
        mimeType: String,
        caption: String?,
        replyToMessageId: Long?
    ): TelegramIncomingMessage {
        return apiClient.sendPhoto(
            chatId = chatId,
            file = file,
            mimeType = mimeType,
            caption = caption,
            replyToMessageId = replyToMessageId
        ).toIncomingMessage()
    }

    override suspend fun sendVideoMessage(
        chatId: Long,
        file: File,
        mimeType: String,
        caption: String?,
        replyToMessageId: Long?
    ): TelegramIncomingMessage {
        return apiClient.sendVideo(
            chatId = chatId,
            file = file,
            mimeType = mimeType,
            caption = caption,
            replyToMessageId = replyToMessageId
        ).toIncomingMessage()
    }

    override suspend fun sendVisualMediaGroup(
        chatId: Long,
        media: List<OutgoingVisualMedia>,
        caption: String?,
        replyToMessageId: Long?
    ): List<TelegramIncomingMessage> {
        return apiClient.sendMediaGroup(
            chatId = chatId,
            media = media,
            caption = caption,
            replyToMessageId = replyToMessageId
        ).map(MessageDto::toIncomingMessage)
    }

    override suspend fun deleteMessage(chatId: Long, messageId: Long): Boolean {
        return apiClient.deleteMessage(chatId = chatId, messageId = messageId)
    }

    override suspend fun downloadFile(
        fileId: String,
        fileExtension: String,
        fileUniqueId: String,
        isAvatar: Boolean
    ): String? {
        return try {
            val folder = if (isAvatar) "avatars" else "media"
            val filename = "$fileUniqueId.$fileExtension"
            val file = File(context.cacheDir, "$folder/$filename")

            file.parentFile?.mkdirs()
            if (file.exists() && file.length() > 0) {
                return file.absolutePath
            }

            val fileInfo = apiClient.getFile(fileId)
            val remotePath = fileInfo.filePath ?: return null
            val downloaded = apiClient.downloadFile(remotePath, file)

            if (downloaded && file.exists() && file.length() > 0) file.absolutePath else null
        } catch (e: CancellationException) {
            throw e
        } catch (e: TelegramApiException) {
            if (e.statusCode == 400) {
                Log.w("TelegramGateway", "File unavailable for $fileId: ${e.description ?: e.message}")
                null
            } else {
                Log.e("TelegramGateway", "Download error", e)
                null
            }
        } catch (e: Exception) {
            Log.e("TelegramGateway", "Download error", e)
            null
        }
    }

    override suspend fun downloadUserAvatar(userId: Long): AvatarFetchResult? {
        return try {
            val photos = apiClient.getUserProfilePhotos(userId = userId, limit = 1)
            val bestPhoto = photos.photos.firstOrNull()?.lastOrNull()
                ?: return AvatarFetchResult.Missing
            val localPath = downloadFile(
                fileId = bestPhoto.fileId,
                fileExtension = "jpg",
                fileUniqueId = bestPhoto.fileUniqueId,
                isAvatar = true
            )

            AvatarFetchResult.Available(
                fileId = bestPhoto.fileId,
                fileUniqueId = bestPhoto.fileUniqueId,
                localPath = localPath
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: TelegramApiException) {
            if (e.statusCode == 400) {
                Log.w("TelegramGateway", "Avatar unavailable for $userId: ${e.description ?: e.message}")
                null
            } else {
                Log.e("TelegramGateway", "Avatar fetch error for $userId", e)
                null
            }
        } catch (e: Exception) {
            Log.e("TelegramGateway", "Avatar fetch error for $userId", e)
            null
        }
    }

    override suspend fun downloadChatAvatar(chatId: Long): AvatarFetchResult? {
        return try {
            val chat = apiClient.getChat(chatId)
            val photo = chat.photo ?: return AvatarFetchResult.Missing
            val localPath = downloadFile(
                fileId = photo.bigFileId,
                fileExtension = "jpg",
                fileUniqueId = photo.bigFileUniqueId,
                isAvatar = true
            )

            AvatarFetchResult.Available(
                fileId = photo.bigFileId,
                fileUniqueId = photo.bigFileUniqueId,
                localPath = localPath
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("TelegramGateway", "Chat avatar fetch error for $chatId", e)
            null
        }
    }

    override suspend fun fetchUserBio(username: String): PublicProfileBioResult {
        val normalizedUsername = username.removePrefix("@").trim()
            .takeIf { telegramUsernameRegex.matches(it) }
            ?: return PublicProfileBioResult.Failure

        return try {
            val html = apiClient.getText("https://t.me/$normalizedUsername")
            PublicProfileBioResult.Success(extractTelegramProfileBioFromHtml(html))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("TelegramGateway", "Public profile fetch error for @$normalizedUsername", e)
            PublicProfileBioResult.Failure
        }
    }

    override suspend fun getChat(chatId: Long): TelegramChat? {
        return try {
            apiClient.getChat(chatId).toTelegramChat()
        } catch (e: Exception) {
            Log.e("TelegramGateway", "Error getting chat info for $chatId", e)
            null
        }
    }

    override suspend fun getChatMemberCount(chatId: Long): Int? {
        return try {
            apiClient.getChatMemberCount(chatId)
        } catch (e: Exception) {
            Log.e("TelegramGateway", "Error getting chat member count for $chatId", e)
            null
        }
    }

    override suspend fun getChatAdministrators(chatId: Long): List<TelegramChatMember>? {
        return try {
            apiClient.getChatAdministrators(chatId).map { it.toTelegramChatMember() }
        } catch (e: Exception) {
            Log.e("TelegramGateway", "Error getting chat administrators for $chatId", e)
            null
        }
    }

    override fun close() {
        apiClient.close()
    }
}

private val telegramUsernameRegex = Regex("^[A-Za-z0-9_]{5,32}$")

/** Преобразует DTO участника чата в нормализованную модель. */
private fun ChatMemberDto.toTelegramChatMember(): TelegramChatMember {
    return TelegramChatMember(
        user = user.toTelegramUser(),
        status = status,
        customTitle = customTitle
    )
}

/** Преобразует DTO пользователя в нормализованную модель. */
private fun UserDto.toTelegramUser(): TelegramUser {
    return TelegramUser(
        id = id,
        firstName = firstName,
        lastName = lastName,
        username = username,
        languageCode = languageCode,
        bio = null,
        canWriteMsgToPm = false
    )
}

/** Преобразует DTO чата в нормализованную модель. */
private fun ChatDto.toTelegramChat(): TelegramChat {
    return TelegramChat(
        id = id,
        type = toChatType(),
        title = title,
        firstName = firstName,
        lastName = lastName,
        username = username,
        description = description,
        lastMessageType = null,
        lastMessageText = null,
        lastMessageTime = null,
        lastMessageSenderId = null
    )
}

/** Преобразует сырое сообщение Telegram Bot API в внутреннюю модель приложения. */
private fun MessageDto.toIncomingMessage(): TelegramIncomingMessage {
    val sender = from

    return TelegramIncomingMessage(
        messageId = messageId,
        chatId = chat.id,
        topicId = messageThreadId,
        senderId = sender?.id,
        type = determineMessageType(),
        timestamp = date * 1000,
        text = text,
        caption = caption,
        replyMsgId = replyToMessage?.messageId,
        replyMsgTopicId = replyToMessage?.messageThreadId,
        fileName = extractFileName(),
        fileExtension = extractFileExtension(),
        fileId = extractFileId(),
        fileUniqueId = extractFileUniqueId(),
        fileSize = extractFileSize(),
        width = extractMediaWidth(),
        height = extractMediaHeight(),
        duration = extractMediaDuration(),
        thumbnailFileId = extractThumbnailFileId(),
        latitude = location?.latitude,
        longitude = location?.longitude,
        isEdited = editDate != null,
        editedAt = editDate?.times(1000),
        mediaGroupId = mediaGroupId,
        chatAvatarChanged = newChatPhoto != null,
        chatAvatarRemoved = deleteChatPhoto,
        chat = chat.toTelegramChat(),
        sender = sender?.let {
            TelegramUser(
                id = it.id,
                firstName = it.firstName,
                lastName = it.lastName,
                username = it.username,
                languageCode = it.languageCode,
                bio = null,
                canWriteMsgToPm = chat.type == "private"
            )
        }
    )
}

/** Нормализует строковый тип чата Telegram в enum приложения. */
private fun ChatDto.toChatType(): ChatType {
    return when (type) {
        "private" -> ChatType.PRIVATE
        "group" -> ChatType.GROUP
        "supergroup" -> ChatType.SUPERGROUP
        "channel" -> ChatType.CHANNEL
        else -> ChatType.PRIVATE
    }
}

/** Определяет тип сообщения по заполненным полям DTO. */
private fun MessageDto.determineMessageType(): MessageType {
    return when {
        text != null -> MessageType.TEXT
        photo != null -> MessageType.PHOTO
        video != null -> MessageType.VIDEO
        animation != null -> MessageType.ANIMATION
        audio != null -> MessageType.AUDIO
        voice != null -> MessageType.VOICE
        videoNote != null -> MessageType.VIDEO_NOTE
        document != null -> MessageType.DOCUMENT
        sticker != null -> when {
            sticker.isAnimated -> MessageType.ANIMATED_STICKER
            sticker.isVideo -> MessageType.VIDEO_STICKER
            else -> MessageType.STICKER
        }
        contact != null -> MessageType.CONTACT
        location != null -> MessageType.LOCATION
        else -> MessageType.TEXT
    }
}

/** Извлекает расширение файла из доступных полей медиа-вложения. */
private fun MessageDto.extractFileExtension(): String? {
    return when {
        document != null -> document.fileName?.substringAfterLast('.', "")
            ?.takeIf { it.isNotBlank() }
            ?: getMimeTypeExtension(document.mimeType)
        audio != null -> audio.fileName?.substringAfterLast('.', "")?.takeIf { it.isNotBlank() } ?: "mp3"
        photo != null -> "jpg"
        video != null -> "mp4"
        animation != null -> animation.fileName?.substringAfterLast('.', "")
            ?.takeIf { it.isNotBlank() }
            ?: getMimeTypeExtension(animation.mimeType)
        voice != null -> "ogg"
        videoNote != null -> "mp4"
        sticker != null -> when {
            sticker.isAnimated -> "tgs"
            sticker.isVideo -> "webm"
            else -> "webp"
        }
        else -> null
    }
}

/** Возвращает расширение по MIME-типу для распространённых форматов. */
private fun getMimeTypeExtension(mimeType: String?): String {
    return when (mimeType) {
        "image/jpeg" -> "jpg"
        "image/png" -> "png"
        "video/mp4" -> "mp4"
        "audio/mpeg" -> "mp3"
        "audio/ogg" -> "ogg"
        "application/pdf" -> "pdf"
        "text/plain" -> "txt"
        else -> "bin"
    }
}

/** Извлекает наиболее уместное имя файла для вложения. */
private fun MessageDto.extractFileName(): String? {
    return when {
        document != null -> document.fileName
        audio != null -> audio.fileName
        animation != null -> animation.fileName
        else -> null
    }
}

/** Возвращает `fileId` миниатюры, если вложение её поддерживает. */
private fun MessageDto.extractThumbnailFileId(): String? {
    return when {
        photo != null -> photo.minByOrNull { it.width }?.fileId
        video != null -> video.thumbnail?.fileId
        animation != null -> animation.thumbnail?.fileId
        document != null -> document.thumbnail?.fileId
        audio != null -> audio.thumbnail?.fileId
        videoNote != null -> videoNote.thumbnail?.fileId
        sticker != null -> sticker.thumbnail?.fileId
        else -> null
    }
}

/** Извлекает длительность медиа-сообщения в секундах. */
private fun MessageDto.extractMediaDuration(): Long? {
    return when {
        video != null -> video.duration
        videoNote != null -> videoNote.duration
        animation != null -> animation.duration
        audio != null -> audio.duration
        voice != null -> voice.duration
        else -> null
    }
}

/** Извлекает ширину медиа. */
private fun MessageDto.extractMediaWidth(): Int? {
    return when {
        photo != null -> photo.maxByOrNull { it.width }?.width
        video != null -> video.width
        animation != null -> animation.width
        videoNote != null -> videoNote.width
        sticker != null -> sticker.width
        else -> null
    }
}

/** Извлекает высоту медиа. */
private fun MessageDto.extractMediaHeight(): Int? {
    return when {
        photo != null -> photo.maxByOrNull { it.width }?.height
        video != null -> video.height
        animation != null -> animation.height
        videoNote != null -> videoNote.height
        sticker != null -> sticker.height
        else -> null
    }
}

/** Извлекает основной `fileId` вложения. */
private fun MessageDto.extractFileId(): String? {
    return when {
        photo != null -> photo.maxByOrNull { it.width }?.fileId
        video != null -> video.fileId
        animation != null -> animation.fileId
        audio != null -> audio.fileId
        voice != null -> voice.fileId
        videoNote != null -> videoNote.fileId
        document != null -> document.fileId
        sticker != null -> sticker.fileId
        else -> null
    }
}

/** Извлекает `fileUniqueId`, используемый для кеширования. */
private fun MessageDto.extractFileUniqueId(): String? {
    return when {
        photo != null -> photo.maxByOrNull { it.width }?.fileUniqueId
        video != null -> video.fileUniqueId
        animation != null -> animation.fileUniqueId
        audio != null -> audio.fileUniqueId
        voice != null -> voice.fileUniqueId
        videoNote != null -> videoNote.fileUniqueId
        document != null -> document.fileUniqueId
        sticker != null -> sticker.fileUniqueId
        else -> null
    }
}

/** Извлекает размер файла вложения. */
private fun MessageDto.extractFileSize(): Long? {
    return when {
        photo != null -> photo.maxByOrNull { it.width }?.fileSize
        video != null -> video.fileSize
        animation != null -> animation.fileSize
        audio != null -> audio.fileSize
        voice != null -> voice.fileSize
        videoNote != null -> videoNote.fileSize
        document != null -> document.fileSize
        sticker != null -> sticker.fileSize
        else -> null
    }
}
