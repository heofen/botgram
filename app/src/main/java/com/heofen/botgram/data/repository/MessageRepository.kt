package com.heofen.botgram.data.repository

import com.heofen.botgram.database.dao.MessageDao
import com.heofen.botgram.database.tables.Message
import kotlinx.coroutines.flow.Flow

class MessageRepository(private val messageDao: MessageDao) {
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
}
