package com.heofen.botgram.data.repository

import com.heofen.botgram.MessageType
import com.heofen.botgram.database.dao.ChatDao
import com.heofen.botgram.database.tables.Chat
import kotlinx.coroutines.flow.Flow

class ChatRepository(private val chatDao: ChatDao) {
    fun getAllChats(): Flow<List<Chat>> = chatDao.getAllChats()

    suspend fun getById(id: Long): Chat? = chatDao.getById(id)

    suspend fun chatExists(id: Long): Boolean = chatDao.chatExists(id)

    suspend fun insertChat(chat: Chat) = chatDao.insert(chat)

    suspend fun upsertChat(chat: Chat) = chatDao.upsert(chat)

    suspend fun updateLastMessage(chatId: Long, type: MessageType, text: String?, time: Long) =
        chatDao.updateLastMessage(chatId, type, text, time)

    suspend fun incrementUnread(chatId: Long) = chatDao.incrementUnread(chatId)

    suspend fun resetUnread(chatId: Long) = chatDao.resetUnread(chatId)


//    suspend fun updateMuted(chatId: Long, isMuted: Boolean) =
//        chatDao.updateMuted(chatId, isMuted)

    suspend fun updateAvatar(
        chatId: Long,
        fileId: String?,
        fileUniqueId: String?,
        localPath: String?,
    ) = chatDao.updateAvatar(chatId, fileId, fileUniqueId, localPath)
}
