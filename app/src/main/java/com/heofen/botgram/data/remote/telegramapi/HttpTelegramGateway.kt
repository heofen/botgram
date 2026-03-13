package com.heofen.botgram.data.remote.telegramapi

import android.content.Context
import android.util.Log
import com.heofen.botgram.ChatType
import com.heofen.botgram.MessageType
import com.heofen.botgram.data.remote.AvatarDownloadResult
import com.heofen.botgram.data.remote.TelegramChat
import com.heofen.botgram.data.remote.TelegramGateway
import com.heofen.botgram.data.remote.TelegramIncomingMessage
import com.heofen.botgram.data.remote.TelegramUser
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import java.io.File
import kotlin.coroutines.cancellation.CancellationException

class HttpTelegramGateway(
    private val context: Context,
    token: String
) : TelegramGateway {

    private val apiClient = TelegramBotApiClient(token = token)
    private var nextUpdateOffset: Long? = null

    override suspend fun collectUpdates(onMessage: suspend (TelegramIncomingMessage) -> Unit) {
        while (currentCoroutineContext().isActive) {
            val updates = apiClient.getUpdates(offset = nextUpdateOffset, timeout = 50)

            for (update in updates) {
                val message = update.message ?: update.editedMessage ?: continue
                onMessage(message.toIncomingMessage())
                nextUpdateOffset = update.updateId + 1
            }
        }
    }

    override suspend fun sendTextMessage(chatId: Long, text: String): TelegramIncomingMessage {
        return apiClient.sendMessage(chatId = chatId, text = text)
            .toIncomingMessage()
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

    override suspend fun downloadUserAvatar(userId: Long): AvatarDownloadResult? {
        return try {
            val photos = apiClient.getUserProfilePhotos(userId = userId, limit = 1)
            val bestPhoto = photos.photos.firstOrNull()?.lastOrNull() ?: return null
            val localPath = downloadFile(
                fileId = bestPhoto.fileId,
                fileExtension = "jpg",
                fileUniqueId = bestPhoto.fileUniqueId,
                isAvatar = true
            )

            AvatarDownloadResult(
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

    override suspend fun downloadChatAvatar(chatId: Long): AvatarDownloadResult? {
        return try {
            val chat = apiClient.getChat(chatId)
            val photo = chat.photo ?: return null
            val localPath = downloadFile(
                fileId = photo.bigFileId,
                fileExtension = "jpg",
                fileUniqueId = photo.bigFileUniqueId,
                isAvatar = true
            )

            AvatarDownloadResult(
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

    override fun close() {
        apiClient.close()
    }
}

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
        isEdited = editDate != null,
        editedAt = editDate?.times(1000),
        mediaGroupId = mediaGroupId,
        chat = TelegramChat(
            id = chat.id,
            type = chat.toChatType(),
            title = chat.title,
            firstName = if (chat.type == "private") chat.firstName ?: sender?.firstName else null,
            lastName = if (chat.type == "private") chat.lastName ?: sender?.lastName else null,
            username = chat.username,
            lastMessageType = determineMessageType(),
            lastMessageText = text ?: caption,
            lastMessageTime = date * 1000,
            lastMessageSenderId = sender?.id
        ),
        sender = sender?.let {
            TelegramUser(
                id = it.id,
                firstName = it.firstName,
                lastName = it.lastName,
                bio = null,
                canWriteMsgToPm = chat.type == "private"
            )
        }
    )
}

private fun ChatDto.toChatType(): ChatType {
    return when (type) {
        "private" -> ChatType.PRIVATE
        "group" -> ChatType.GROUP
        "supergroup" -> ChatType.SUPERGROUP
        "channel" -> ChatType.CHANNEL
        else -> ChatType.PRIVATE
    }
}

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

private fun MessageDto.extractFileName(): String? {
    return when {
        document != null -> document.fileName
        audio != null -> audio.fileName
        animation != null -> animation.fileName
        else -> null
    }
}

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
