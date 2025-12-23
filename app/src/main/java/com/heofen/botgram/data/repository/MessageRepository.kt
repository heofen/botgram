package com.heofen.botgram.data.repository

import android.util.Log
import com.heofen.botgram.data.MediaManager
import com.heofen.botgram.database.dao.MessageDao
import com.heofen.botgram.database.tables.Message
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.RawChatId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File

class MessageRepository(
    private val messageDao: MessageDao,
    private val bot: TelegramBot,
    private val mediaManager: MediaManager
) {

    private val downloadSemaphore = Semaphore(permits = 3)

    fun getChatMessages(chatId: Long): Flow<List<Message>> =
        messageDao.getChatMessages(chatId)

    fun getMediaGroup(groupId: String): Flow<List<Message>> =
        messageDao.getMediaGroup(groupId)

    suspend fun getMessage(chatId: Long, messageId: Long): Message? =
        messageDao.getMessage(chatId, messageId)

    suspend fun insertMessage(message: Message) =
        messageDao.insert(message)

    suspend fun insertAll(messages: List<Message>) =
        messageDao.insertAll(messages)

    suspend fun updateMessage(
        chatId: Long,
        messageId: Long,
        text: String?,
        caption: String?,
        isEdited: Boolean,
        editedAt: Long?
    ) = messageDao.updateMessage(chatId, messageId, text, caption, isEdited, editedAt)

    suspend fun updateFilePath(chatId: Long, messageId: Long, localPath: String) =
        messageDao.updateFilePath(chatId, messageId, localPath)

    suspend fun findCachedMedia(fileUniqueId: String): Message? =
        messageDao.findDownloadedByUniqueId(fileUniqueId)

    suspend fun getLastMessage(chatId: Long): Message? =
        messageDao.getLastMessage(chatId)

    suspend fun deleteOldMessages(chatId: Long, beforeTimestamp: Long) =
        messageDao.deleteOldMessages(chatId, beforeTimestamp)

    suspend fun fileExists(fileUniqueId: String): Boolean =
        messageDao.fileExists(fileUniqueId)

    suspend fun sendTextMessage(chatId: Long, text: String) {
        try {
            val sentMessage = bot.sendMessage(
                chatId = ChatId(RawChatId(chatId)),
                text = text
            )
            Log.i("MessageRepository", "Message sent: ${sentMessage.messageId.long}")
        } catch (e: Exception) {
            Log.e("MessageRepository", "Error sending message: ${e.message}")
        }
    }

    suspend fun ensureMediaDownloaded(message: Message) {
        val fileId = message.fileId ?: return
        val fileUniqueId = message.fileUniqueId ?: return
        val ext = message.fileExtension ?: return

        message.fileLocalPath?.let { path ->
            if (File(path).exists()) return
        }

        val cached = findCachedMedia(fileUniqueId)
        cached?.fileLocalPath?.let { cachedPath ->
            if (File(cachedPath).exists()) {
                updateFilePath(message.chatId, message.messageId, cachedPath)
                return
            }
        }

        downloadSemaphore.withPermit {
            try {
                val localPath = mediaManager.getFile(
                    fileId = fileId,
                    fileExtension = ext,
                    fileUniqueId = fileUniqueId,
                    isAvatar = false
                )
                if (localPath != null) {
                    updateFilePath(message.chatId, message.messageId, localPath)
                }
            } catch (e: Exception) {
                Log.e("MessageRepository", "Media download failed: ${e.message}")
            }
        }
    }
}
