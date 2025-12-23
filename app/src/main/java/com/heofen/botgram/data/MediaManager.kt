package com.heofen.botgram.data

import android.content.Context
import android.util.Log
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.chat.get.getChat
import dev.inmo.tgbotapi.extensions.api.files.downloadFile
import dev.inmo.tgbotapi.extensions.api.get.getUserProfilePhotos
import dev.inmo.tgbotapi.requests.abstracts.FileId
import dev.inmo.tgbotapi.requests.get.GetFile
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.UserId
import dev.inmo.tgbotapi.types.chat.ExtendedPrivateChat
import dev.inmo.tgbotapi.types.chat.ExtendedPublicChat
import java.io.File
import dev.inmo.tgbotapi.types.chat.ExtendedSupergroupChat
import dev.inmo.tgbotapi.types.chat.ExtendedChannelChat
import dev.inmo.tgbotapi.types.chat.ExtendedGroupChat

class MediaManager(
    private val context: Context,
    private val bot: TelegramBot,
) {
    suspend fun getFile(
        fileId: String,
        fileExtension: String,
        fileUniqueId: String,
        isAvatar: Boolean = false
    ): String? {
        try {
            val folder = if (isAvatar) "avatars" else "media"
            val filename = "$fileUniqueId.$fileExtension"
            val file = File(context.cacheDir, "$folder/$filename")

            file.parentFile?.mkdirs()

            if (file.exists() && file.length() > 0) {
                return file.absolutePath
            }

            Log.d("MediaManager", "getFile start: id=$fileId uid=$fileUniqueId ext=$fileExtension")
            val fileInfo = bot.execute(GetFile(FileId(fileId)))
            Log.d("MediaManager", "getFile got fileInfo: $fileInfo")
            bot.downloadFile(fileInfo, file)
            Log.d("MediaManager", "getFile done: exists=${file.exists()} size=${file.length()} path=${file.absolutePath}")

            return if (file.exists() && file.length() > 0) {
                file.absolutePath
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("MediaManager", "Download error: $e")
            return null
        }
    }

    suspend fun downloadUserAvatar(userId: Long): Pair<String?, String?> {
        try {
            val photos = bot.getUserProfilePhotos(UserId(RawChatId(userId)), limit = 1)

            if (photos.count == 0 || photos.photos.isEmpty()) {
                return null to null
            }

            val bestPhoto = photos.photos[0].last()

            val fileId = bestPhoto.fileId.fileId
            val fileUniqueId = bestPhoto.fileUniqueId.toString()

            val localPath = getFile(fileId, "jpg", fileUniqueId, isAvatar = true)

            return fileId to localPath
        } catch (e: Exception) {
            Log.e("MediaManager", "Avatar fetch error for $userId: $e")
            return null to null
        }
    }

    suspend fun downloadChatAvatar(chatId: Long): Pair<String?, String?> {
        try {
            val chat = bot.getChat(ChatId(RawChatId(chatId)))

            val photo = when (chat) {
                is ExtendedPrivateChat -> chat.chatPhoto
                is ExtendedPublicChat -> chat.chatPhoto
                else -> null
            }

            if (photo == null) return null to null

            val fileId = photo.bigFileId
            val fileUniqueId = photo.bigFileId

            val localPath = getFile(fileId, "jpg", fileUniqueId, isAvatar = true)

            return fileId to localPath
        } catch (e: Exception) {
            Log.e("MediaManager", "Chat avatar fetch error for $chatId: $e")
            return null to null
        }
    }
}