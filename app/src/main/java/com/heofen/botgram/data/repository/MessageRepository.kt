package com.heofen.botgram.data.repository

import com.heofen.botgram.database.dao.MessageDao
import com.heofen.botgram.database.tables.Message
import kotlinx.coroutines.flow.Flow

class MessageRepository(private val messageDao: MessageDao) {
    fun getChatMessages(chatId: Long): Flow<List<Message>> =
        messageDao.getChatMessages(chatId)

    suspend fun insertMessage(message: Message) =
        messageDao.insert(message)

    suspend fun findCachedMedia(fileUniqueId: String): Message? =
        messageDao.findDownloadedByUniqueId(fileUniqueId)
}