package com.heofen.botgram.data.repository

import android.util.Log
import com.heofen.botgram.data.MediaManager
import com.heofen.botgram.data.remote.TelegramIncomingMessage
import com.heofen.botgram.data.remote.TelegramGateway
import com.heofen.botgram.data.sync.MessageSyncStore
import com.heofen.botgram.database.dao.MessageDao
import com.heofen.botgram.database.tables.Message
import com.heofen.botgram.utils.toDbMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File

class MessageRepository(
    private val messageDao: MessageDao,
    private val gateway: TelegramGateway,
    private val mediaManager: MediaManager
) : MessageSyncStore {

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

    override suspend fun getLastMessage(chatId: Long): Message? =
        messageDao.getLastMessage(chatId)

    override suspend fun upsertRemoteMessage(
        message: TelegramIncomingMessage,
        isOutgoing: Boolean,
        readStatus: Boolean
    ): Message {
        val existing = getMessage(message.chatId, message.messageId)
        val incoming = message.toDbMessage(
            isOutgoing = existing?.isOutgoing ?: isOutgoing,
            readStatus = existing?.readStatus ?: readStatus
        )
        val merged = mergeRemoteMessageWithLocalState(
            incoming = incoming,
            existing = existing
        )
        messageDao.insert(merged)
        return merged
    }

    suspend fun deleteOldMessages(chatId: Long, beforeTimestamp: Long) =
        messageDao.deleteOldMessages(chatId, beforeTimestamp)

    suspend fun deleteMessageForMe(chatId: Long, messageId: Long) =
        messageDao.deleteMessage(chatId, messageId)

    suspend fun deleteMessageForEveryone(chatId: Long, messageId: Long): Boolean {
        return try {
            val deleted = gateway.deleteMessage(chatId = chatId, messageId = messageId)
            if (deleted) {
                messageDao.deleteMessage(chatId, messageId)
            }
            deleted
        } catch (e: Exception) {
            Log.e("MessageRepository", "Error deleting message for everyone: ${e.message}", e)
            false
        }
    }

    suspend fun fileExists(fileUniqueId: String): Boolean =
        messageDao.fileExists(fileUniqueId)

    suspend fun sendTextMessage(
        chatId: Long,
        text: String,
        replyToMessageId: Long? = null
    ): Message? {
        return try {
            val sentMessage = gateway.sendTextMessage(
                chatId = chatId,
                text = text,
                replyToMessageId = replyToMessageId
            )
            val dbMessage = sentMessage.toDbMessage(isOutgoing = true, readStatus = true)
            messageDao.insert(dbMessage)
            Log.i("MessageRepository", "Message sent: ${sentMessage.messageId}")
            dbMessage
        } catch (e: Exception) {
            Log.e("MessageRepository", "Error sending message: ${e.message}")
            null
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

internal fun mergeRemoteMessageWithLocalState(
    incoming: Message,
    existing: Message?
): Message {
    if (existing == null) return incoming

    val preserveLocalFilePath = incoming.fileUniqueId == null || incoming.fileUniqueId == existing.fileUniqueId

    return incoming.copy(
        topicId = incoming.topicId ?: existing.topicId,
        senderId = incoming.senderId ?: existing.senderId,
        replyMsgId = incoming.replyMsgId ?: existing.replyMsgId,
        replyMsgTopicId = incoming.replyMsgTopicId ?: existing.replyMsgTopicId,
        fileName = incoming.fileName ?: existing.fileName,
        fileExtension = incoming.fileExtension ?: existing.fileExtension,
        fileId = incoming.fileId ?: existing.fileId,
        fileUniqueId = incoming.fileUniqueId ?: existing.fileUniqueId,
        fileLocalPath = if (preserveLocalFilePath) existing.fileLocalPath ?: incoming.fileLocalPath else incoming.fileLocalPath,
        fileSize = incoming.fileSize ?: existing.fileSize,
        width = incoming.width ?: existing.width,
        height = incoming.height ?: existing.height,
        duration = incoming.duration ?: existing.duration,
        thumbnailFileId = incoming.thumbnailFileId ?: existing.thumbnailFileId,
        isEdited = incoming.isEdited || existing.isEdited,
        editedAt = incoming.editedAt ?: existing.editedAt,
        mediaGroupId = incoming.mediaGroupId ?: existing.mediaGroupId,
        readStatus = existing.readStatus || incoming.readStatus,
        isOutgoing = existing.isOutgoing
    )
}
